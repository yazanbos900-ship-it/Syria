package com.example.features.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.core.utils.LanguageManager
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandTextMuted
import com.google.firebase.Timestamp

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatListScreen(
    onNavigateToChat: (String) -> Unit,
    viewModel: ChatListViewModel = viewModel()
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)

    val uiState by viewModel.uiState.collectAsState()
    val currentUserId by viewModel.currentUserId.collectAsState()
    val totalUnreadCount by viewModel.totalUnreadCount.collectAsState()

    var searchQuery by remember { mutableStateOf("") }

    Scaffold(
        containerColor = Color(0xFF0A0B0D), // Dark background #0A0B0D
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isArabic) "المحادثات" else "Chats",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                        if (totalUnreadCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(Color(0xFF1DB954)) // Green unread badge
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = totalUnreadCount.toString(),
                                    color = Color.White,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0B0D)
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(Color(0xFF0A0B0D))
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                placeholder = {
                    Text(
                        text = if (isArabic) "البحث في المحادثات..." else "Search chats...",
                        color = BrandTextMuted,
                        fontSize = 14.sp
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search",
                        tint = BrandTextMuted
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedBorderColor = Color(0xFF1DB954),
                    unfocusedBorderColor = BrandSoftGray,
                    focusedContainerColor = Color(0xFF17191E),
                    unfocusedContainerColor = Color(0xFF17191E)
                ),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("chat_list_search_input")
            )

            Spacer(modifier = Modifier.height(8.dp))

            when (val state = uiState) {
                is ChatListUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1DB954))
                    }
                }
                is ChatListUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .weight(1f)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = state.message,
                            color = Color.Red,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
                is ChatListUiState.Success -> {
                    val filteredChats = state.chats.filter { chat ->
                        val otherPartyName = chat.getOtherPartyName(currentUserId)
                        val productTitle = chat.productTitle
                        otherPartyName.contains(searchQuery, ignoreCase = true) ||
                                productTitle.contains(searchQuery, ignoreCase = true)
                    }

                    if (filteredChats.isEmpty()) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center,
                                modifier = Modifier.padding(32.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ChatBubbleOutline,
                                    contentDescription = null,
                                    tint = BrandTextMuted,
                                    modifier = Modifier.size(72.dp)
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text(
                                    text = if (isArabic) "لا توجد محادثات بعد" else "No chats yet",
                                    color = Color.White,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = if (isArabic) "ابدأ بالتواصل مع البائعين وشراء المنتجات!" else "Start communicating with sellers now!",
                                    color = BrandTextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Light
                                )
                            }
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxSize()
                                .weight(1f),
                            contentPadding = PaddingValues(bottom = 16.dp)
                        ) {
                            items(filteredChats, key = { it.chatId }) { chat ->
                                val opponentName = chat.getOtherPartyName(currentUserId)
                                val unread = chat.getUnreadCountForUser(currentUserId)

                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToChat(chat.chatId) }
                                        .padding(horizontal = 16.dp, vertical = 12.dp)
                                        .testTag("chat_item_${chat.chatId}")
                                ) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        // Product Thumbnail
                                        Box(
                                            modifier = Modifier
                                                .size(48.dp)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(Color(0xFF17191E))
                                        ) {
                                            AsyncImage(
                                                model = chat.productImage,
                                                contentDescription = chat.productTitle,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        }

                                        Spacer(modifier = Modifier.width(14.dp))

                                        // Chat Metadata Column
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                // Name
                                                Text(
                                                    text = opponentName,
                                                    color = Color.White,
                                                    fontSize = 15.sp,
                                                    fontWeight = FontWeight.Bold,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
                                                )

                                                // Relative Time
                                                Text(
                                                    text = getRelativeTime(chat.lastMessageTime, isArabic),
                                                    color = BrandTextMuted,
                                                    fontSize = 11.sp
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(2.dp))

                                            // Product Name
                                            Text(
                                                text = chat.productTitle,
                                                color = Color(0xFF2196F3), // Accent color to distinguish
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Medium,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            Spacer(modifier = Modifier.height(4.dp))

                                            // Last message
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Text(
                                                    text = chat.lastMessage,
                                                    color = if (unread > 0) Color.White else BrandTextMuted,
                                                    fontSize = 13.sp,
                                                    fontWeight = if (unread > 0) FontWeight.Bold else FontWeight.Normal,
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis,
                                                    modifier = Modifier.weight(1f)
                                                )

                                                if (unread > 0) {
                                                    Spacer(modifier = Modifier.width(8.dp))
                                                    Box(
                                                        modifier = Modifier
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color(0xFF1DB954)),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = unread.toString(),
                                                            color = Color.White,
                                                            fontSize = 10.sp,
                                                            fontWeight = FontWeight.Bold
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(12.dp))
                                    HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

fun getRelativeTime(timestamp: Timestamp?, isArabic: Boolean): String {
    if (timestamp == null) return ""
    val diff = System.currentTimeMillis() - timestamp.toDate().time
    val seconds = diff / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val days = hours / 24

    return if (isArabic) {
        when {
            seconds < 60 -> "الآن"
            minutes < 60 -> "منذ $minutes د"
            hours < 24 -> "منذ $hours س"
            days == 1L -> "أمس"
            days < 7 -> "منذ $days يوم"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
        }
    } else {
        when {
            seconds < 60 -> "Just now"
            minutes < 60 -> "$minutes m ago"
            hours < 24 -> "$hours h ago"
            days == 1L -> "Yesterday"
            days < 7 -> "$days d ago"
            else -> {
                val sdf = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                sdf.format(timestamp.toDate())
            }
        }
    }
}
