package com.example.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Loading)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private var chatListener: com.google.firebase.firestore.ListenerRegistration? = null
    private var messagesListener: com.google.firebase.firestore.ListenerRegistration? = null

    private var currentChatId: String? = null
    private var currentUid: String? = null
    private var currentOpponentId: String? = null

    private var typingJob: Job? = null

    fun initializeChat(chatId: String) {
        if (currentChatId == chatId) return
        currentChatId = chatId

        viewModelScope.launch {
            val user = ServiceLocator.authRepository.getCurrentUserSession()
            if (user == null) {
                _uiState.value = ChatUiState.Error("Please log in first")
                return@launch
            }
            val myUid = user.id
            currentUid = myUid

            // Mark unread as 0 for this chatId
            resetUnreadCount(chatId, myUid)

            // Listen to chat session metadata
            chatListener = db.collection("chats").document(chatId)
                .addSnapshotListener { chatDoc, error ->
                    if (error != null) {
                        _uiState.value = ChatUiState.Error(error.localizedMessage ?: "Failed to listen to chat metadata")
                        return@addSnapshotListener
                    }

                    if (chatDoc == null || !chatDoc.exists()) {
                        _uiState.value = ChatUiState.Error("Chat does not exist")
                        return@addSnapshotListener
                    }

                    val room = chatDoc.toChatRoom() ?: return@addSnapshotListener
                    val opponentId = room.getOtherPartyUid(myUid)
                    currentOpponentId = opponentId

                    // Retrieve opponent typing status
                    val opponentTypingField = "typing_$opponentId"
                    val isOpponentTyping = chatDoc.getBoolean(opponentTypingField) ?: false

                    // If we are currently viewing this chat, make sure unreads stay 0 on our side (if increments occur)
                    val myUnreadPath = "unreadCount_$myUid"
                    val myUnreads = (chatDoc.get(myUnreadPath) as? Number)?.toInt() ?: 0
                    if (myUnreads > 0) {
                        resetUnreadCount(chatId, myUid)
                    }

                    // Listen to messages once we have opponentId info
                    if (messagesListener == null) {
                        startListeningToMessages(chatId, myUid, opponentId)
                    } else {
                        // Just update metadata in Success state
                        val currentState = _uiState.value
                        if (currentState is ChatUiState.Success) {
                            _uiState.value = currentState.copy(
                                chatRoom = room,
                                isTypingOpponent = isOpponentTyping
                            )
                        }
                    }
                }
        }
    }

    private fun startListeningToMessages(chatId: String, myUid: String, opponentId: String) {
        messagesListener = db.collection("chats").document(chatId)
            .collection("messages")
            .orderBy("timestamp", Query.Direction.ASCENDING)
            .addSnapshotListener { snapshot, error ->
                if (error != null) {
                    _uiState.value = ChatUiState.Error(error.localizedMessage ?: "Failed loading messages")
                    return@addSnapshotListener
                }

                val room = (_uiState.value as? ChatUiState.Success)?.chatRoom
                    ?: ChatRoom(chatId = chatId, buyerUid = myUid, sellerUid = opponentId)

                val list = snapshot?.documents?.mapNotNull { it.toChatMessage() } ?: emptyList()

                // Mark any unread messages from opponent as read now that we are looking at them
                markMessagesAsRead(chatId, opponentId, list)

                val typingStatusField = "typing_$opponentId"
                val opponentTyping = (room.chatId.isNotEmpty() && opponentId.isNotEmpty()) 

                _uiState.value = ChatUiState.Success(
                    chatRoom = room,
                    messages = list,
                    isTypingOpponent = false, // Set initially, updated on chat doc snapshot updates
                    currentUserId = myUid
                )
            }
    }

    fun sendMessage(text: String) {
        val chatId = currentChatId ?: return
        val senderUid = currentUid ?: return
        viewModelScope.launch {
            val userSession = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
            val name = userSession.name
            
            val messageRef = db.collection("chats").document(chatId)
                .collection("messages").document()

            val messageId = messageRef.id
            val rawMessage = hashMapOf(
                "messageId" to messageId,
                "senderId" to senderUid,
                "senderName" to name,
                "text" to text,
                "imageUrl" to null,
                "type" to "text",
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )

            // Submit message and update lastMessage metadata on parent room
            messageRef.set(rawMessage)
            updateParentLastMessage(chatId, text, senderUid)
        }
    }

    fun sendImageMessage(imageUrl: String) {
        val chatId = currentChatId ?: return
        val senderUid = currentUid ?: return
        viewModelScope.launch {
            val userSession = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
            val name = userSession.name
            
            val messageRef = db.collection("chats").document(chatId)
                .collection("messages").document()

            val messageId = messageRef.id
            val rawMessage = hashMapOf(
                "messageId" to messageId,
                "senderId" to senderUid,
                "senderName" to name,
                "text" to "📷 صورة",
                "imageUrl" to imageUrl,
                "type" to "image",
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )

            messageRef.set(rawMessage)
            updateParentLastMessage(chatId, "📷 صورة", senderUid)
        }
    }

    fun sendOfferMessage(price: Double, originalPrice: Double) {
        val chatId = currentChatId ?: return
        val senderUid = currentUid ?: return
        viewModelScope.launch {
            val userSession = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
            val name = userSession.name

            val messageRef = db.collection("chats").document(chatId)
                .collection("messages").document()

            val messageId = messageRef.id
            val rawMessage = hashMapOf(
                "messageId" to messageId,
                "senderId" to senderUid,
                "senderName" to name,
                "text" to "💰 عرض سعر بقيمة $$price",
                "imageUrl" to null,
                "type" to "offer",
                "offerPrice" to price,
                "offerStatus" to "pending",
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to false
            )

            messageRef.set(rawMessage)
            updateParentLastMessage(chatId, "💰 عرض سعر بقيمة $$price", senderUid)
        }
    }

    fun handleOfferAction(messageId: String, action: String, offerPrice: Double) {
        // action is either "accepted" or "rejected"
        val chatId = currentChatId ?: return
        val senderUid = currentUid ?: return

        viewModelScope.launch {
            val messageDoc = db.collection("chats").document(chatId)
                .collection("messages").document(messageId)

            messageDoc.update("offerStatus", action)

            val systemText = if (action == "accepted") {
                "تم قبول العرض بسعر $$offerPrice"
            } else {
                "تم رفض العرض"
            }

            // Create system message
            val sysRef = db.collection("chats").document(chatId)
                .collection("messages").document()
            val sysMsg = hashMapOf(
                "messageId" to sysRef.id,
                "senderId" to "system",
                "senderName" to "system",
                "text" to systemText,
                "imageUrl" to null,
                "type" to "system",
                "timestamp" to FieldValue.serverTimestamp(),
                "isRead" to true
            )
            sysRef.set(sysMsg)

            // Update parent chat LastMessage description
            updateParentLastMessage(chatId, systemText, senderUid)
        }
    }

    // Set self typing status debounced
    fun setTypingStatus(isTyping: Boolean) {
        val chatId = currentChatId ?: return
        val myUid = currentUid ?: return

        typingJob?.cancel()
        
        viewModelScope.launch {
            db.collection("chats").document(chatId)
                .update("typing_$myUid", isTyping)
        }

        if (isTyping) {
            typingJob = viewModelScope.launch {
                delay(3000)
                db.collection("chats").document(chatId)
                    .update("typing_$myUid", false)
            }
        }
    }

    private fun markMessagesAsRead(chatId: String, opponentId: String, messages: List<ChatMessage>) {
        val unreadMessageIds = messages.filter { !it.isRead && it.senderId == opponentId }.map { it.messageId }
        if (unreadMessageIds.isNotEmpty()) {
            viewModelScope.launch {
                val batch = db.batch()
                for (msgId in unreadMessageIds) {
                    val messageRef = db.collection("chats").document(chatId)
                        .collection("messages").document(msgId)
                    batch.update(messageRef, "isRead", true)
                }
                batch.commit()
            }
        }
    }

    private fun resetUnreadCount(chatId: String, myUid: String) {
        viewModelScope.launch {
            val myUnreadPath = "unreadCount_$myUid"
            db.collection("chats").document(chatId)
                .update(myUnreadPath, 0)
        }
    }

    private fun updateParentLastMessage(chatId: String, text: String, senderUid: String) {
        viewModelScope.launch {
            val updateData = mutableMapOf<String, Any>(
                "lastMessage" to text,
                "lastMessageTime" to FieldValue.serverTimestamp(),
                "lastMessageSenderId" to senderUid
            )

            // Increment opponent's unread counter
            val opponentId = currentOpponentId
            if (opponentId != null) {
                val opponentUnreadPath = "unreadCount_$opponentId"
                updateData[opponentUnreadPath] = FieldValue.increment(1)
            }

            db.collection("chats").document(chatId)
                .update(updateData)
        }
    }

    private fun DocumentSnapshot.toChatRoom(): ChatRoom? {
        return try {
            val id = id
            val participants = get("participants") as? List<String> ?: emptyList()
            val buyerUid = getString("buyerUid") ?: ""
            val sellerUid = getString("sellerUid") ?: ""
            val buyerName = getString("buyerName") ?: ""
            val sellerName = getString("sellerName") ?: ""
            val productId = getString("productId") ?: ""
            val productTitle = getString("productTitle") ?: ""
            val productImage = getString("productImage") ?: ""
            val lastMessage = getString("lastMessage") ?: ""
            val lastMessageTime = getTimestamp("lastMessageTime")
            val lastMessageSenderId = getString("lastMessageSenderId") ?: ""
            val unreadCount_buyerUid = (get("unreadCount_$buyerUid") as? Number)?.toInt() ?: 0
            val unreadCount_sellerUid = (get("unreadCount_$sellerUid") as? Number)?.toInt() ?: 0
            val createdAt = getTimestamp("createdAt")

            ChatRoom(
                chatId = id,
                participants = participants,
                buyerUid = buyerUid,
                sellerUid = sellerUid,
                buyerName = buyerName,
                sellerName = sellerName,
                productId = productId,
                productTitle = productTitle,
                productImage = productImage,
                lastMessage = lastMessage,
                lastMessageTime = lastMessageTime,
                lastMessageSenderId = lastMessageSenderId,
                unreadCount_buyerUid = unreadCount_buyerUid,
                unreadCount_sellerUid = unreadCount_sellerUid,
                createdAt = createdAt
            )
        } catch (e: Exception) {
            null
        }
    }

    private fun DocumentSnapshot.toChatMessage(): ChatMessage? {
        return try {
            ChatMessage(
                messageId = id,
                senderId = getString("senderId") ?: "",
                senderName = getString("senderName") ?: "",
                text = getString("text") ?: "",
                imageUrl = getString("imageUrl"),
                type = getString("type") ?: "text",
                offerPrice = getDouble("offerPrice"),
                offerStatus = getString("offerStatus") ?: "pending",
                timestamp = getTimestamp("timestamp"),
                isRead = getBoolean("isRead") ?: false
            )
        } catch (e: Exception) {
            null
        }
    }

    override fun onCleared() {
        super.onCleared()
        chatListener?.remove()
        messagesListener?.remove()
        // Reset our typing indicator in Firestore on exiting
        val chatId = currentChatId
        val myUid = currentUid
        if (chatId != null && myUid != null) {
            db.collection("chats").document(chatId).update("typing_$myUid", false)
        }
    }
}
