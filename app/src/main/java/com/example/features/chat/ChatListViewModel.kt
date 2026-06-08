package com.example.features.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ChatListViewModel : ViewModel() {

    private val _uiState = MutableStateFlow<ChatListUiState>(ChatListUiState.Loading)
    val uiState: StateFlow<ChatListUiState> = _uiState.asStateFlow()

    private val _currentUserId = MutableStateFlow("")
    val currentUserId: StateFlow<String> = _currentUserId.asStateFlow()

    private val _totalUnreadCount = MutableStateFlow(0)
    val totalUnreadCount: StateFlow<Int> = _totalUnreadCount.asStateFlow()

    private val db = FirebaseFirestore.getInstance()
    private var chatsListener: com.google.firebase.firestore.ListenerRegistration? = null

    init {
        loadUserAndChats()
    }

    private fun loadUserAndChats() {
        viewModelScope.launch {
            val user = ServiceLocator.authRepository.getCurrentUserSession()
            if (user == null) {
                _uiState.value = ChatListUiState.Error("Please log in first")
                return@launch
            }
            
            val myUid = user.id
            _currentUserId.value = myUid

            // Listen to chats in real-time
            try {
                chatsListener = db.collection("chats")
                    .whereArrayContains("participants", myUid)
                    .addSnapshotListener { snapshot, error ->
                        if (error != null) {
                            _uiState.value = ChatListUiState.Error(error.localizedMessage ?: "Failed to listen to chats")
                            return@addSnapshotListener
                        }

                        if (snapshot == null) {
                            _uiState.value = ChatListUiState.Success(emptyList())
                            return@addSnapshotListener
                        }

                        val parsedChats = snapshot.documents.mapNotNull { doc ->
                            doc.toChatRoom(myUid)
                        }.sortedByDescending { chat ->
                            chat.lastMessageTime?.seconds ?: 0L
                        }

                        _uiState.value = ChatListUiState.Success(parsedChats)

                        // Calculate total unread count across all active chats for this user
                        val totalUnreads = parsedChats.sumOf { chat ->
                            chat.getUnreadCountForUser(myUid)
                        }
                        _totalUnreadCount.value = totalUnreads
                    }
            } catch (e: Exception) {
                _uiState.value = ChatListUiState.Error(e.localizedMessage ?: "Unexpected error initializing chat list")
            }
        }
    }

    private fun DocumentSnapshot.toChatRoom(myUid: String): ChatRoom? {
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
            
            // Unread counts safely
            val buyerUnreadPath = "unreadCount_$buyerUid"
            val sellerUnreadPath = "unreadCount_$sellerUid"
            
            val unreadCount_buyerUid = (get(buyerUnreadPath) as? Number)?.toInt() 
                ?: (get("unreadCount_buyerUid") as? Number)?.toInt() 
                ?: 0
            val unreadCount_sellerUid = (get(sellerUnreadPath) as? Number)?.toInt() 
                ?: (get("unreadCount_sellerUid") as? Number)?.toInt() 
                ?: 0
                
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

    override fun onCleared() {
        super.onCleared()
        chatsListener?.remove()
    }
}
