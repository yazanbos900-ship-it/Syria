package com.example.features.chat

import com.google.firebase.Timestamp

data class ChatRoom(
    val chatId: String = "",
    val participants: List<String> = emptyList(),
    val buyerUid: String = "",
    val sellerUid: String = "",
    val buyerName: String = "",
    val sellerName: String = "",
    val productId: String = "",
    val productTitle: String = "",
    val productImage: String = "",
    val lastMessage: String = "",
    val lastMessageTime: Timestamp? = null,
    val lastMessageSenderId: String = "",
    val unreadCount_buyerUid: Int = 0,
    val unreadCount_sellerUid: Int = 0,
    val createdAt: Timestamp? = null
) {
    // Utility to get other party's UID
    fun getOtherPartyUid(currentUid: String): String {
        return if (buyerUid == currentUid) sellerUid else buyerUid
    }
    
    // Utility to get other party's Name
    fun getOtherPartyName(currentUid: String): String {
        return if (buyerUid == currentUid) sellerName else buyerName
    }
    
    // Utility to check unread count for current user
    fun getUnreadCountForUser(currentUid: String): Int {
        return if (buyerUid == currentUid) unreadCount_buyerUid else unreadCount_sellerUid
    }
}

data class ChatMessage(
    val messageId: String = "",
    val senderId: String = "",
    val senderName: String = "",
    val text: String = "",
    val imageUrl: String? = null,
    val type: String = "text", // "text" | "image" | "offer" | "system"
    val offerPrice: Double? = null,
    val offerStatus: String = "pending", // "pending" | "accepted" | "rejected"
    val timestamp: Timestamp? = null,
    val isRead: Boolean = false
)

sealed interface ChatListUiState {
    object Loading : ChatListUiState
    data class Success(val chats: List<ChatRoom>) : ChatListUiState
    data class Error(val message: String) : ChatListUiState
}

sealed interface ChatUiState {
    object Loading : ChatUiState
    data class Success(
        val chatRoom: ChatRoom,
        val messages: List<ChatMessage>,
        val isTypingOpponent: Boolean,
        val currentUserId: String
    ) : ChatUiState
    data class Error(val message: String) : ChatUiState
}
