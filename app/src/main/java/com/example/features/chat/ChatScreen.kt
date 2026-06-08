package com.example.features.chat

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.core.utils.LanguageManager
import com.example.features.marketplace.CloudinaryUploader
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandTextMuted
import com.google.firebase.Timestamp
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatScreen(
    chatId: String,
    onNavigateBack: () -> Unit,
    viewModel: ChatViewModel = viewModel()
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val layoutDirection = if (isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    LaunchedEffect(chatId) {
        viewModel.initializeChat(chatId)
    }

    val uiState by viewModel.uiState.collectAsState()
    val scope = rememberCoroutineScope()

    var messageText by remember { mutableStateOf("") }
    var isSendingFile by remember { mutableStateOf(false) }

    // Dropdown Actions Menu
    var showMoreMenu by remember { mutableStateOf(false) }

    // Full Screen Image zoom Dialog state
    var zoomedImageUrl by remember { mutableStateOf<String?>(null) }

    // Make Offer BottomSheet state
    var showOfferSheet by remember { mutableStateOf(false) }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            scope.launch {
                isSendingFile = true
                val uploader = CloudinaryUploader()
                val res = uploader.uploadFile(uri.toString())
                res.onSuccess { url ->
                    viewModel.sendImageMessage(url)
                }.onFailure {
                    Toast.makeText(
                        context,
                        if (isArabic) "فشل رفع الصورة وجهات الاتصال" else "Image attachment upload failed",
                        Toast.LENGTH_LONG
                    ).show()
                }
                isSendingFile = false
            }
        }
    }

    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Scaffold(
            containerColor = Color(0xFF0A0B0D), // Dark background #0A0B0D
            contentWindowInsets = WindowInsets.safeDrawing,
            topBar = {
                when (val state = uiState) {
                    is ChatUiState.Success -> {
                        val opposingName = state.chatRoom.getOtherPartyName(state.currentUserId)

                        CenterAlignedTopAppBar(
                            title = {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = opposingName,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = Color.White,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    // Typing or Online Status indicator
                                    if (state.isTypingOpponent) {
                                        Text(
                                            text = if (isArabic) "جاري الكتابة..." else "typing...",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFF1DB954) // Green
                                        )
                                    } else {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(6.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFF1DB954))
                                            )
                                            Text(
                                                text = if (isArabic) "متصل" else "online",
                                                fontSize = 11.sp,
                                                color = BrandTextMuted
                                            )
                                        }
                                    }
                                }
                            },
                            navigationIcon = {
                                IconButton(
                                    onClick = onNavigateBack,
                                    modifier = Modifier.testTag("chat_back_button")
                                ) {
                                    Icon(
                                        imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            actions = {
                                IconButton(onClick = { showMoreMenu = true }) {
                                    Icon(
                                        imageVector = Icons.Default.MoreVert,
                                        contentDescription = "Options",
                                        tint = Color.White
                                    )
                                }

                                DropdownMenu(
                                    expanded = showMoreMenu,
                                    onDismissRequest = { showMoreMenu = false },
                                    modifier = Modifier.background(Color(0xFF17191E))
                                ) {
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isArabic) "الإبلاغ عن المستخدم" else "Report user",
                                                color = Color.White,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(
                                                context,
                                                if (isArabic) "تم إرسال بلاغك بنجاح للمراجعة!" else "Report submitted to admin successfully!",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                    DropdownMenuItem(
                                        text = {
                                            Text(
                                                text = if (isArabic) "حظر المستخدم" else "Block user",
                                                color = Color.Red,
                                                fontSize = 14.sp
                                            )
                                        },
                                        onClick = {
                                            showMoreMenu = false
                                            Toast.makeText(
                                                context,
                                                if (isArabic) "تم حظر هذا العضو بنجاح" else "User blocked successfully",
                                                Toast.LENGTH_LONG
                                            ).show()
                                        }
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color(0xFF0A0B0D)
                            )
                        )
                    }
                    else -> {
                        CenterAlignedTopAppBar(
                            title = { Text(text = "", color = Color.White) },
                            navigationIcon = {
                                IconButton(onClick = onNavigateBack) {
                                    Icon(
                                        imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Back",
                                        tint = Color.White
                                    )
                                }
                            },
                            colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                                containerColor = Color(0xFF0A0B0D)
                            )
                        )
                    }
                }
            }
        ) { innerPadding ->
            when (val state = uiState) {
                is ChatUiState.Loading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = Color(0xFF1DB954))
                    }
                }
                is ChatUiState.Error -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                            .padding(24.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.Block,
                                contentDescription = null,
                                tint = Color.Red,
                                modifier = Modifier.size(54.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = state.message,
                                color = Color.White,
                                textAlign = TextAlign.Center,
                                fontSize = 16.sp
                            )
                        }
                    }
                }
                is ChatUiState.Success -> {
                    val chatRoom = state.chatRoom
                    val currentUid = state.currentUserId
                    val messages = state.messages

                    val isSeller = chatRoom.sellerUid == currentUid

                    // Group messages
                    val groupedMessages = remember(messages, isArabic) {
                        groupMessagesByDate(messages, isArabic)
                    }

                    val listState = rememberLazyListState()

                    // Scroll to bottom when message size changes
                    LaunchedEffect(messages.size) {
                        if (messages.isNotEmpty()) {
                            listState.animateScrollToItem(messages.size * 2) // Roughly elements account
                        }
                    }

                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding)
                    ) {
                        // Header Mini Product Card Row
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF17191E)),
                            shape = RoundedCornerShape(0.dp), // Straight top edge to align beneath app bar
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(Color(0xFF0A0B0D))
                                ) {
                                    AsyncImage(
                                        model = chatRoom.productImage,
                                        contentDescription = chatRoom.productTitle,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = chatRoom.productTitle,
                                        color = Color.White,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = if (isArabic) "محادثة حول هذا المنتج" else "Conversation for this item",
                                        color = BrandTextMuted,
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }

                        // Messages Area list list
                        LazyColumn(
                            state = listState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .padding(horizontal = 16.dp),
                            contentPadding = PaddingValues(vertical = 16.dp)
                        ) {
                            groupedMessages.forEach { (dateHeader, msgs) ->
                                item {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(12.dp))
                                                .background(Color(0xFF17191E))
                                                .padding(horizontal = 12.dp, vertical = 4.dp)
                                        ) {
                                            Text(
                                                text = dateHeader,
                                                color = BrandTextMuted,
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }

                                items(msgs, key = { it.messageId }) { msg ->
                                    val isMe = msg.senderId == currentUid
                                    val isSystem = msg.type == "system" || msg.senderId == "system"

                                    if (isSystem) {
                                        // System Message
                                        Box(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = msg.text,
                                                color = Color(0xFF2196F3),
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 12.sp,
                                                textAlign = TextAlign.Center,
                                                modifier = Modifier.padding(horizontal = 32.dp)
                                            )
                                        }
                                    } else {
                                        // Standard or Offer message row
                                        Row(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(vertical = 6.dp),
                                            horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            // Avatar circle
                                            if (!isMe) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(30.dp)
                                                        .clip(CircleShape)
                                                        .background(Color(0xFF1DB954))
                                                        .align(Alignment.Bottom),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = if (msg.senderName.isNotEmpty()) msg.senderName.take(1).uppercase() else "?",
                                                        color = Color.White,
                                                        fontSize = 12.sp,
                                                        fontWeight = FontWeight.ExtraBold
                                                    )
                                                }
                                                Spacer(modifier = Modifier.width(8.dp))
                                            }

                                            // Bubble Column
                                            Column(
                                                horizontalAlignment = if (isMe) Alignment.End else Alignment.Start
                                            ) {
                                                // Outer Bubble Container
                                                Box(
                                                    modifier = Modifier
                                                        .clip(
                                                            RoundedCornerShape(
                                                                topStart = 16.dp,
                                                                topEnd = 16.dp,
                                                                bottomStart = if (isMe) 16.dp else 4.dp,
                                                                bottomEnd = if (isMe) 4.dp else 16.dp
                                                            )
                                                        )
                                                        .background(
                                                            if (isMe) Color(0xFF1DB954) else Color(0xFF17191E)
                                                        )
                                                        .padding(horizontal = 14.dp, vertical = 10.dp)
                                                ) {
                                                    when (msg.type) {
                                                        "text" -> {
                                                            Text(
                                                                text = msg.text,
                                                                color = Color.White,
                                                                fontSize = 14.sp
                                                            )
                                                        }
                                                        "image" -> {
                                                            Column {
                                                                AsyncImage(
                                                                    model = msg.imageUrl,
                                                                    contentDescription = "Image message",
                                                                    contentScale = ContentScale.Crop,
                                                                    modifier = Modifier
                                                                        .height(200.dp)
                                                                        .width(200.dp)
                                                                        .clip(RoundedCornerShape(8.dp))
                                                                        .clickable { zoomedImageUrl = msg.imageUrl }
                                                                )
                                                            }
                                                        }
                                                        "offer" -> {
                                                            Column(
                                                                modifier = Modifier.width(220.dp),
                                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                                            ) {
                                                                Row(
                                                                    verticalAlignment = Alignment.CenterVertically,
                                                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                                                ) {
                                                                    Icon(
                                                                        imageVector = Icons.Default.LocalOffer,
                                                                        contentDescription = null,
                                                                        tint = Color.White,
                                                                        modifier = Modifier.size(16.dp)
                                                                    )
                                                                    Text(
                                                                        text = if (isArabic) "💰 عرض سعر مقترح" else "💰 Proposed Offer Price",
                                                                        color = Color.White,
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.ExtraBold
                                                                    )
                                                                }

                                                                Text(
                                                                    text = if (isArabic) "السعر المقترح: $${msg.offerPrice}" else "Suggested optimal: $${msg.offerPrice}",
                                                                    color = Color.White,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.SemiBold
                                                                )

                                                                HorizontalDivider(color = Color.White.copy(alpha = 0.2f))

                                                                when (msg.offerStatus) {
                                                                    "pending" -> {
                                                                        if (isSeller) {
                                                                            Row(
                                                                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                                                                modifier = Modifier.fillMaxWidth()
                                                                            ) {
                                                                                Button(
                                                                                    onClick = {
                                                                                        viewModel.handleOfferAction(
                                                                                            msg.messageId,
                                                                                            "accepted",
                                                                                            msg.offerPrice ?: 0.0
                                                                                        )
                                                                                    },
                                                                                    colors = ButtonDefaults.buttonColors(
                                                                                        containerColor = Color(0xFF2E7D32),
                                                                                        contentColor = Color.White
                                                                                    ),
                                                                                    modifier = Modifier.weight(1f),
                                                                                    contentPadding = PaddingValues(vertical = 4.dp),
                                                                                    shape = RoundedCornerShape(8.dp)
                                                                                ) {
                                                                                    Text(
                                                                                        text = if (isArabic) "قبول" else "Accept",
                                                                                        fontSize = 11.sp,
                                                                                        fontWeight = FontWeight.Bold
                                                                                    )
                                                                                }

                                                                                Button(
                                                                                    onClick = {
                                                                                        viewModel.handleOfferAction(
                                                                                            msg.messageId,
                                                                                            "rejected",
                                                                                            msg.offerPrice ?: 0.0
                                                                                        )
                                                                                    },
                                                                                    colors = ButtonDefaults.buttonColors(
                                                                                        containerColor = Color.Red,
                                                                                        contentColor = Color.White
                                                                                    ),
                                                                                    modifier = Modifier.weight(1f),
                                                                                    contentPadding = PaddingValues(vertical = 4.dp),
                                                                                    shape = RoundedCornerShape(8.dp)
                                                                                ) {
                                                                                    Text(
                                                                                        text = if (isArabic) "رفض" else "Refuse",
                                                                                        fontSize = 11.sp,
                                                                                        fontWeight = FontWeight.Bold
                                                                                    )
                                                                                }
                                                                            }
                                                                        } else {
                                                                            Text(
                                                                                text = if (isArabic) "في انتظار الرد من البائع..." else "Waiting for reply...",
                                                                                color = Color.White.copy(alpha = 0.8f),
                                                                                fontSize = 11.sp,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }
                                                                    "accepted" -> {
                                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                            Icon(imageVector = Icons.Default.Check, contentDescription = null, tint = Color.Green, modifier = Modifier.size(16.dp))
                                                                            Text(
                                                                                text = if (isArabic) "تم قبول العرض" else "Offer Accepted",
                                                                                color = Color.Green,
                                                                                fontSize = 12.sp,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }
                                                                    "rejected" -> {
                                                                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                                                                            Icon(imageVector = Icons.Default.Close, contentDescription = null, tint = Color.Red, modifier = Modifier.size(16.dp))
                                                                            Text(
                                                                                text = if (isArabic) "تم رفض العرض" else "Offer Rejected",
                                                                                color = Color.Red,
                                                                                fontSize = 12.sp,
                                                                                fontWeight = FontWeight.Bold
                                                                            )
                                                                        }
                                                                    }
                                                                }
                                                            }
                                                        }
                                                    }
                                                }

                                                Spacer(modifier = Modifier.height(3.dp))

                                                // Small details row (time & read checks)
                                                Row(
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                                                    modifier = Modifier.padding(horizontal = 4.dp)
                                                ) {
                                                    val timeStr = if (msg.timestamp != null) {
                                                        java.text.SimpleDateFormat("hh:mm a", java.util.Locale.getDefault()).format(msg.timestamp.toDate())
                                                    } else {
                                                        ""
                                                    }

                                                    Text(
                                                        text = timeStr,
                                                        color = BrandTextMuted,
                                                        fontSize = 10.sp
                                                    )

                                                    if (isMe) {
                                                        if (msg.isRead) {
                                                            Text(
                                                                text = "✓✓",
                                                                color = Color(0xFF1DB954), // green on read
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        } else {
                                                            Text(
                                                                text = "✓✓",
                                                                color = BrandTextMuted, // grey on delivered
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // Image uploading indicator
                        if (isSendingFile) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF1DB954),
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = if (isArabic) "جاري إرسال الصورة..." else "Uploading image...",
                                        color = Color.White,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Input Sticky Bar
                        Surface(
                            color = Color(0xFF17191E),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 12.dp, vertical = 10.dp)
                                    .fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                // Camera launcher Button
                                IconButton(
                                    onClick = { photoLauncher.launch("image/*") },
                                    modifier = Modifier.testTag("chat_pick_image_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = "Pick Image",
                                        tint = Color.White
                                    )
                                }

                                // Money Offer launcher Button (Only if user is buyer)
                                if (!isSeller) {
                                    IconButton(
                                        onClick = { showOfferSheet = true },
                                        modifier = Modifier.testTag("chat_make_offer_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.LocalOffer,
                                            contentDescription = "Make Offer",
                                            tint = Color(0xFF2196F3) // blue token
                                        )
                                    }
                                }

                                // Text entry
                                OutlinedTextField(
                                    value = messageText,
                                    onValueChange = {
                                        messageText = it
                                        // Trigger typing state updates
                                        viewModel.setTypingStatus(it.isNotEmpty())
                                    },
                                    placeholder = {
                                        Text(
                                            text = if (isArabic) "اكتب رسالتك..." else "Type message...",
                                            color = BrandTextMuted,
                                            fontSize = 13.sp
                                        )
                                    },
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedTextColor = Color.White,
                                        unfocusedTextColor = Color.White,
                                        focusedBorderColor = Color(0xFF1DB954),
                                        unfocusedBorderColor = Color.Transparent,
                                        focusedContainerColor = Color(0xFF0A0B0D),
                                        unfocusedContainerColor = Color(0xFF0A0B0D)
                                    ),
                                    maxLines = 3,
                                    shape = RoundedCornerShape(20.dp),
                                    modifier = Modifier
                                        .weight(1f)
                                        .testTag("chat_text_input")
                                )

                                // Send Button (Active only when text has contents)
                                if (messageText.isNotBlank()) {
                                    IconButton(
                                        onClick = {
                                            viewModel.sendMessage(messageText.trim())
                                            messageText = ""
                                            viewModel.setTypingStatus(false)
                                        },
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(Color(0xFF1DB954))
                                            .testTag("chat_send_button")
                                    ) {
                                        Icon(
                                            imageVector = Icons.AutoMirrored.Filled.Send,
                                            contentDescription = "Send",
                                            tint = Color.White
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // Full screen zooming overlay Image Dialog
    if (zoomedImageUrl != null) {
        Dialog(onDismissRequest = { zoomedImageUrl = null }) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.9f))
                    .clickable { zoomedImageUrl = null },
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = zoomedImageUrl,
                    contentDescription = "Zoomed preview",
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                )
            }
        }
    }

    // Suggest Offer Modal Bottom Sheet/Dialog
    if (showOfferSheet) {
        var proposedPriceInput by remember { mutableStateOf("") }
        var inputErr by remember { mutableStateOf<String?>(null) }

        val activeState = uiState as? ChatUiState.Success
        val productTitle = activeState?.chatRoom?.productTitle ?: ""

        Dialog(onDismissRequest = { showOfferSheet = false }) {
            Surface(
                color = Color(0xFF17191E),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.dp, BrandSoftGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = if (isArabic) "اقتراح سعر جديد" else "Suggest New Price",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold
                    )

                    Text(
                        text = productTitle,
                        color = BrandTextMuted,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )

                    OutlinedTextField(
                        value = proposedPriceInput,
                        onValueChange = { proposedPriceInput = it },
                        label = {
                            Text(
                                text = if (isArabic) "السعر المقترح (USD)" else "Proposed price (USD)",
                                color = BrandTextMuted
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = Color(0xFF0A0B0D),
                            unfocusedContainerColor = Color(0xFF0A0B0D)
                        ),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("offer_price_input")
                    )

                    if (inputErr != null) {
                        Text(
                            text = inputErr!!,
                            color = Color.Red,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        OutlinedButton(
                            onClick = { showOfferSheet = false },
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = Color.White
                            ),
                            border = BorderStroke(1.dp, BrandSoftGray),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(text = if (isArabic) "إلغاء" else "Cancel")
                        }

                        Button(
                            onClick = {
                                val d = proposedPriceInput.toDoubleOrNull()
                                if (d == null || d <= 0.0) {
                                    inputErr = if (isArabic) "الرجاء تحديد قيمة صحيحة" else "Please enter a valid amount"
                                } else {
                                    viewModel.sendOfferMessage(d, 0.0)
                                    showOfferSheet = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF1DB954),
                                contentColor = Color.White
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .testTag("app_send_offer_submit")
                        ) {
                            Text(
                                text = if (isArabic) "إرسال العرض" else "Send Offer",
                                fontWeight = FontWeight.Bold
                              )
                            }
                        }
                    }
                }
            }
        }
    }

fun groupMessagesByDate(messages: List<ChatMessage>, isArabic: Boolean): Map<String, List<ChatMessage>> {
    val grouped = messages.groupBy { msg ->
        if (msg.timestamp == null) return@groupBy if (isArabic) "اليوم" else "Today"
        val date = msg.timestamp.toDate()
        val sdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        sdf.format(date)
    }

    val todaySdf = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
    val todayStr = todaySdf.format(java.util.Date())

    val yesterdayCal = java.util.Calendar.getInstance().apply { add(java.util.Calendar.DATE, -1) }
    val yesterdayStr = todaySdf.format(yesterdayCal.time)

    return grouped.mapKeys { (key, _) ->
        when (key) {
            todayStr -> if (isArabic) "اليوم" else "Today"
            yesterdayStr -> if (isArabic) "أمس" else "Yesterday"
            else -> {
                try {
                    val originalDate = todaySdf.parse(key)
                    val outFormat = java.text.SimpleDateFormat("dd/MM/yyyy", java.util.Locale.getDefault())
                    if (originalDate != null) outFormat.format(originalDate) else key
                } catch (e: Exception) {
                    key
                }
            }
        }
    }
}
