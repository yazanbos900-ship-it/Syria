package com.example.features.marketplace

import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.core.utils.CurrencyManager
import com.example.core.utils.LanguageManager
import com.example.domain.model.Product
import com.example.domain.model.RecommendationCriteria
import com.example.domain.model.getPriceInUSD
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.delay

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit,
    onContactSeller: (String) -> Unit = {},
    onSellerProfileClick: (String, String) -> Unit = { _, _ -> }
) {
    val viewModel: ProductDetailViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ProductDetailViewModel(
                com.example.core.di.ServiceLocator.productRepository,
                com.example.core.di.ServiceLocator.storeRepository,
                com.example.core.di.ServiceLocator.recommendationRepository,
                com.example.core.di.ServiceLocator.comparisonRepository,
                com.example.core.di.ServiceLocator.reviewRepository
            ) as T
        }
    })
    
    val state by viewModel.state.collectAsStateWithLifecycle()
    val isAr = LanguageManager.isArabic(LocalContext.current)
    
    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }
    
    val domainProduct = state.product
    var currentUserSession by remember { mutableStateOf<com.example.domain.model.User?>(null) }
    var adOwnerUid by remember { mutableStateOf<String?>(null) }
    var adOwnerName by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(domainProduct) {
        if (domainProduct != null) {
            currentUserSession = com.example.core.di.ServiceLocator.authRepository.getCurrentUserSession()
            if (domainProduct.storeId == "direct_ad") {
                val db = FirebaseFirestore.getInstance()
                db.collection("direct_ads").document(domainProduct.id).get()
                    .addOnSuccessListener { doc ->
                        if (doc.exists()) {
                            adOwnerUid = doc.getString("ownerUid")
                            adOwnerName = doc.getString("ownerUsername")
                        }
                    }
            }
            if (currentUserSession != null) {
                viewModel.loadUserReview(domainProduct.id, currentUserSession!!.id)
            }
        }
    }

    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
        return
    }

    if (state.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = state.error!!, color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onBack) { Text("Back") }
            }
        }
        return
    }

    if (domainProduct == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val storeExchangeRate = state.store?.usdExchangeRate ?: 13500.0
    val priceInStoreCurrency = domainProduct.getPriceInUSD(storeExchangeRate)
    val originalPriceInStoreCurrency = priceInStoreCurrency * 1.5

    val marketProduct = remember(domainProduct.id) {
        productCatalog.find { it.id == domainProduct.id } ?: MarketProduct(
            id = domainProduct.id,
            name = domainProduct.title,
            price = priceInStoreCurrency,
            originalPrice = originalPriceInStoreCurrency,
            rating = domainProduct.rating.toDouble(),
            reviewsCount = domainProduct.reviewCount,
            category = "General",
            storeName = state.store?.name ?: "Store",
            deliveryTime = "Standard",
            dateAdded = "Today",
            imageUrl = domainProduct.imageUrls.firstOrNull() ?: ""
        )
    }

    val isLiked = SharedWishlistState.isWishlisted(marketProduct)
    val images = if (domainProduct.imageUrls.isNotEmpty()) domainProduct.imageUrls else listOf("")
    
    var quantity by remember { mutableIntStateOf(1) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            TopAppBar(
                title = {},
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { SharedWishlistState.toggleWishlist(marketProduct) }) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Wishlist",
                            tint = if (isLiked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = {}) {
                        Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurface)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background.copy(alpha = 0.9f)
                ),
                windowInsets = WindowInsets(0.dp)
            )
        },
        bottomBar = {
            Surface(
                color = MaterialTheme.colorScheme.surface,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Total Price",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = CurrencyManager.formatPrice(priceInStoreCurrency * quantity, storeExchangeRate, isAr),
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                        
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MaterialTheme.colorScheme.secondaryContainer)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = if (domainProduct.isAvailable) "In Stock" else "Out of Stock",
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        val sellerUid = if (domainProduct.storeId == "direct_ad") adOwnerUid else state.store?.ownerId
                        val sellerName = if (domainProduct.storeId == "direct_ad") (adOwnerName ?: "Seller") else (state.store?.ownerUsername ?: state.store?.name ?: "Seller")
                        val isSellerMe = currentUserSession?.id != null && sellerUid != null && (currentUserSession?.id == sellerUid)
                        val context = LocalContext.current
                        var isCheckingChat by remember { mutableStateOf(false) }

                        if (sellerUid != null && !isSellerMe) {
                            // Contact Seller Icon Button
                            OutlinedButton(
                                onClick = {
                                    val currentUid = currentUserSession?.id
                                    if (currentUid == null) {
                                        android.widget.Toast.makeText(context, if (isAr) "يرجى تسجيل الدخول أولاً للاتصال بالبائع" else "Please log in first to contact the seller", android.widget.Toast.LENGTH_SHORT).show()
                                        return@OutlinedButton
                                    }
                                    val targetSellerUid = sellerUid
                                    isCheckingChat = true
                                    val db = FirebaseFirestore.getInstance()

                                    db.collection("chats")
                                        .whereEqualTo("buyerUid", currentUid)
                                        .whereEqualTo("productId", domainProduct.id)
                                        .get()
                                        .addOnSuccessListener { querySnapshot ->
                                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                                val existingChatId = querySnapshot.documents.first().id
                                                isCheckingChat = false
                                                onContactSeller(existingChatId)
                                            } else {
                                                val newChatId = db.collection("chats").document().id
                                                val buyerNameStr = currentUserSession!!.name
                                                val imageToUse = if (domainProduct.imageUrls.isNotEmpty()) domainProduct.imageUrls.first() else ""

                                                val chatData = hashMapOf(
                                                    "chatId" to newChatId,
                                                    "participants" to listOf(currentUid, targetSellerUid),
                                                    "buyerUid" to currentUid,
                                                    "sellerUid" to targetSellerUid,
                                                    "buyerName" to buyerNameStr,
                                                    "sellerName" to sellerName,
                                                    "productId" to domainProduct.id,
                                                    "productTitle" to domainProduct.title,
                                                    "productImage" to imageToUse,
                                                    "lastMessage" to "Conversation started",
                                                    "lastMessageSenderId" to "system",
                                                    "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                    "unreadCount_$currentUid" to 0,
                                                    "unreadCount_$targetSellerUid" to 0,
                                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                )

                                                db.collection("chats").document(newChatId).set(chatData)
                                                    .addOnSuccessListener {
                                                        isCheckingChat = false
                                                        onContactSeller(newChatId)
                                                    }
                                                    .addOnFailureListener {
                                                        isCheckingChat = false
                                                    }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isCheckingChat = false
                                        }
                                },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isCheckingChat
                            ) {
                                if (isCheckingChat) {
                                    CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                                } else {
                                    Icon(Icons.Default.Chat, contentDescription = null, modifier = Modifier.size(20.dp))
                                }
                            }

                            // Redesigned "Buy Now" button: registers purchase intent & starts/opens chat with auto-message
                            Button(
                                onClick = {
                                    val currentUid = currentUserSession?.id
                                    if (currentUid == null) {
                                        android.widget.Toast.makeText(context, if (isAr) "يرجى تسجيل الدخول أولاً لإتمام الشراء" else "Please log in first to proceed with purchase", android.widget.Toast.LENGTH_SHORT).show()
                                        return@Button
                                    }
                                    val targetSellerUid = sellerUid
                                    isCheckingChat = true
                                    val db = FirebaseFirestore.getInstance()

                                    // 1. Record purchase intent for marketplace analytics
                                    val intentId = db.collection("purchase_intents").document().id
                                    val intentData = hashMapOf(
                                        "id" to intentId,
                                        "userId" to currentUid,
                                        "productId" to domainProduct.id,
                                        "productTitle" to domainProduct.title,
                                        "storeId" to (domainProduct.storeId ?: "direct_ad"),
                                        "storeName" to (state.store?.name ?: "Store"),
                                        "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                    )
                                    db.collection("purchase_intents").document(intentId).set(intentData)

                                    // 2. Open or create chat room
                                    db.collection("chats")
                                        .whereEqualTo("buyerUid", currentUid)
                                        .whereEqualTo("productId", domainProduct.id)
                                        .get()
                                        .addOnSuccessListener { querySnapshot ->
                                            if (querySnapshot != null && !querySnapshot.isEmpty) {
                                                val existingChatId = querySnapshot.documents.first().id
                                                isCheckingChat = false
                                                onContactSeller(existingChatId)
                                            } else {
                                                val newChatId = db.collection("chats").document().id
                                                val buyerNameStr = currentUserSession!!.name
                                                val imageToUse = if (domainProduct.imageUrls.isNotEmpty()) domainProduct.imageUrls.first() else ""
                                                val autoMessageText = "مرحباً، أنا مهتم بشراء هذا المنتج. هل ما زال متوفراً؟"

                                                val chatData = hashMapOf(
                                                    "chatId" to newChatId,
                                                    "participants" to listOf(currentUid, targetSellerUid),
                                                    "buyerUid" to currentUid,
                                                    "sellerUid" to targetSellerUid,
                                                    "buyerName" to buyerNameStr,
                                                    "sellerName" to sellerName,
                                                    "productId" to domainProduct.id,
                                                    "productTitle" to domainProduct.title,
                                                    "productImage" to imageToUse,
                                                    "lastMessage" to autoMessageText,
                                                    "lastMessageSenderId" to currentUid,
                                                    "lastMessageTime" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                    "unreadCount_$currentUid" to 0,
                                                    "unreadCount_$targetSellerUid" to 1,
                                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                                )

                                                db.collection("chats").document(newChatId).set(chatData)
                                                    .addOnSuccessListener {
                                                        // Automatically send the first message in the message subcollection
                                                        val messageRef = db.collection("chats").document(newChatId).collection("messages").document()
                                                        val firstMessage = hashMapOf(
                                                            "messageId" to messageRef.id,
                                                            "senderId" to currentUid,
                                                            "senderName" to buyerNameStr,
                                                            "text" to autoMessageText,
                                                            "imageUrl" to null,
                                                            "type" to "text",
                                                            "timestamp" to com.google.firebase.firestore.FieldValue.serverTimestamp(),
                                                            "isRead" to false
                                                        )
                                                        messageRef.set(firstMessage)
                                                            .addOnSuccessListener {
                                                                isCheckingChat = false
                                                                onContactSeller(newChatId)
                                                            }
                                                            .addOnFailureListener {
                                                                isCheckingChat = false
                                                                onContactSeller(newChatId)
                                                            }
                                                    }
                                                    .addOnFailureListener {
                                                        isCheckingChat = false
                                                    }
                                            }
                                        }
                                        .addOnFailureListener {
                                            isCheckingChat = false
                                        }
                                },
                                modifier = Modifier
                                    .weight(3f)
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(16.dp),
                                enabled = !isCheckingChat
                            ) {
                                Text(
                                    text = if (isAr) "شراء الآن (مراسلة)" else "Buy Now (Chat)",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        } else if (isSellerMe) {
                            Button(
                                onClick = {
                                    android.widget.Toast.makeText(context, if (isAr) "هذا منتجك الخاص" else "This is your own product", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(56.dp),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.secondary,
                                    contentColor = MaterialTheme.colorScheme.onSecondary
                                ),
                                shape = RoundedCornerShape(16.dp)
                            ) {
                                Text(
                                    text = if (isAr) "منتجك الخاص" else "Your Product",
                                    fontSize = 16.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
        ) {
            // Product Gallery
            val pagerState = rememberPagerState(pageCount = { images.size })
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(MaterialTheme.colorScheme.surfaceVariant)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    AsyncImage(
                        model = images[page],
                        contentDescription = "Product Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                // Image Indicators
                if (images.size > 1) {
                    Row(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(bottom = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        repeat(images.size) { index ->
                            val isSelected = pagerState.currentPage == index
                            Box(
                                modifier = Modifier
                                    .size(if (isSelected) 10.dp else 8.dp)
                                    .clip(CircleShape)
                                    .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f))
                            )
                        }
                    }
                }
                
                // Discount Badge overlay
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.error)
                        .padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = "33% OFF",
                        color = MaterialTheme.colorScheme.onError,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Product Core Information
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Electronics / Smartwatches",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.SemiBold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = domainProduct.title,
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(12.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Rating",
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "${domainProduct.rating} (${domainProduct.reviewCount} Reviews)",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = CurrencyManager.formatPrice(priceInStoreCurrency, storeExchangeRate, isAr),
                        style = MaterialTheme.typography.headlineLarge,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Black
                    )
                    Text(
                        text = CurrencyManager.formatPrice(originalPriceInStoreCurrency, storeExchangeRate, isAr),
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        textDecoration = TextDecoration.LineThrough
                    )
                }
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))

            // Store or Seller Info Card
            if (domainProduct.sellerType == "DIRECT_SELLER" || state.store != null) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = "Sold By",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Card(
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onSellerProfileClick(
                                    domainProduct.sellerType,
                                    domainProduct.sellerId.ifEmpty { state.store?.id ?: "" }
                                )
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val logoUrl = if (domainProduct.sellerType == "DIRECT_SELLER") "" else state.store?.logoUrl ?: ""
                            val sellerNameDisplay = if (domainProduct.sellerType == "DIRECT_SELLER") adOwnerName ?: "Direct Seller" else state.store?.name ?: "Store"
                            
                            AsyncImage(
                                model = logoUrl,
                                contentDescription = "Store Logo",
                                modifier = Modifier
                                    .size(56.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant),
                                contentScale = ContentScale.Crop
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = sellerNameDisplay,
                                    style = MaterialTheme.typography.titleMedium,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                val currentStore = state.store
                                if (domainProduct.sellerType == "STORE" && currentStore != null) {
                                    StoreBadgesContainer(store = currentStore, horizontalArrangement = Arrangement.Start)
                                    Text("Store ›", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                } else {
                                    Text("Seller ›", fontSize = 12.sp, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.SemiBold)
                                }
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))
            }

            // Description Section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Description",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = domainProduct.description,
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 24.sp
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant, modifier = Modifier.padding(horizontal = 24.dp))

            // Additional Info
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
            ) {
                Text(
                    text = "Specifications",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(16.dp))
                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        SpecRow("Condition", domainProduct.condition.uppercase())
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SpecRow("Available Stock", "${domainProduct.stockCount} items left")
                        HorizontalDivider(modifier = Modifier.padding(vertical = 12.dp), color = MaterialTheme.colorScheme.surfaceVariant)
                        SpecRow("Authenticity", "Verified by Platform")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            
            // Quantity Selector (Internal Page)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp)
            ) {
                Text(
                    text = "Quantity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .wrapContentWidth()
                        .clip(RoundedCornerShape(32.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { if (quantity > 1) quantity-- },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.surface)
                    ) {
                        Text("-", style = MaterialTheme.typography.titleMedium)
                    }
                    Text(
                        text = quantity.toString(),
                        modifier = Modifier.padding(horizontal = 20.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = { if (quantity < domainProduct.stockCount) quantity++ },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.background)
                    ) {
                        Text("+", style = MaterialTheme.typography.titleMedium)
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Reviews Section
            ProductReviewsSection(
                productId = domainProduct.id,
                state = state,
                currentUserSession = currentUserSession,
                viewModel = viewModel
            )
            
            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
fun SpecRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
fun ProductReviewsSection(
    productId: String,
    state: ProductDetailUiState,
    currentUserSession: com.example.domain.model.User?,
    viewModel: ProductDetailViewModel
) {
    var showReviewDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Customer Reviews",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onBackground,
                fontWeight = FontWeight.Bold
            )
            
            if (currentUserSession != null) {
                TextButton(onClick = { showReviewDialog = true }) {
                    Text(
                        text = if (state.userReview == null) "Write a Review" else "Edit Your Review",
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))

        // Rating Summary Card
        val totalReviews = state.product?.reviewCount ?: 0
        val ratingAvg = state.product?.rating ?: 0.0

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(20.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Average Rating
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = String.format(Locale.US, "%.1f", ratingAvg),
                        style = MaterialTheme.typography.displayMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        fontWeight = FontWeight.Black
                    )
                    Row {
                        repeat(5) { i ->
                            Icon(
                                imageVector = if (i < ratingAvg.toInt()) Icons.Default.Star else if (i.toFloat() < ratingAvg.toFloat()) Icons.Default.StarHalf else Icons.Default.StarOutline,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Based on $totalReviews ratings",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                // Distribution Bars
                Column(
                    modifier = Modifier.weight(1.5f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    val reviews = state.reviews
                    val starCounts = IntArray(6) { 0 }
                    reviews.forEach {
                        if (it.rating in 1..5) {
                            starCounts[it.rating]++
                        }
                    }

                    for (i in 5 downTo 1) {
                        val count = starCounts[i]
                        val proportion = if (reviews.isEmpty()) 0f else count.toFloat() / reviews.size.toFloat()
                        
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "$i",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(10.dp)
                            )
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(12.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            LinearProgressIndicator(
                                progress = { proportion },
                                modifier = Modifier
                                    .weight(1f)
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = Color(0xFFFFC107),
                                trackColor = MaterialTheme.colorScheme.surfaceVariant,
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = count.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.width(20.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        val allImages = state.reviews.flatMap { it.images }
        if (allImages.isNotEmpty()) {
            Text(
                text = "Photos from Customers",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(modifier = Modifier.height(12.dp))
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(allImages.size) { index ->
                    var showFullscreen by remember { mutableStateOf(false) }

                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { showFullscreen = true }
                    ) {
                        coil.compose.AsyncImage(
                            model = allImages[index],
                            contentDescription = null,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                        )
                    }

                    if (showFullscreen) {
                        androidx.compose.ui.window.Dialog(
                            onDismissRequest = { showFullscreen = false },
                            properties = androidx.compose.ui.window.DialogProperties(
                                usePlatformDefaultWidth = false,
                                dismissOnBackPress = true,
                                dismissOnClickOutside = true
                            )
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black)
                            ) {
                                val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                                    initialPage = index,
                                    pageCount = { allImages.size }
                                )
                                androidx.compose.foundation.pager.HorizontalPager(
                                    state = pagerState,
                                    modifier = Modifier.fillMaxSize()
                                ) { page ->
                                    coil.compose.AsyncImage(
                                        model = allImages[page],
                                        contentDescription = null,
                                        contentScale = ContentScale.Fit,
                                        modifier = Modifier.fillMaxSize()
                                    )
                                }

                                IconButton(
                                    onClick = { showFullscreen = false },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(16.dp)
                                        .size(48.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Close",
                                        tint = Color.White
                                    )
                                }
                            }
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }

        if (state.reviewsLoading) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp))
            }
        } else if (state.reviews.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "No reviews yet. Be the first to review!",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        } else {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                state.reviews.forEach { review ->
                    ReviewItem(review = review, currentUserId = currentUserSession?.id, onDelete = {
                        viewModel.deleteReview(review.id, productId)
                    })
                }
            }
        }
    }

    if (showReviewDialog) {
        WriteReviewDialog(
            existingReview = state.userReview,
            onDismiss = { showReviewDialog = false },
            onSubmit = { rating, comment, selectedImages ->
                if (currentUserSession != null) {
                    viewModel.submitReview(
                        productId = productId,
                        userId = currentUserSession.id,
                        userName = currentUserSession.name,
                        rating = rating,
                        comment = comment,
                        images = selectedImages,
                        existingReview = state.userReview
                    )
                }
                showReviewDialog = false
            }
        )
    }
}

@Composable
fun ReviewItem(review: com.example.domain.model.Review, currentUserId: String?, onDelete: () -> Unit) {
    val formatter = remember { SimpleDateFormat("MMM dd, yyyy", Locale.US) }
    val dateStr = formatter.format(Date(review.createdAt))

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(MaterialTheme.colorScheme.primaryContainer),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (review.userName.isNotEmpty()) review.userName.take(1).uppercase() else "?",
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = review.userName.ifBlank { "Anonymous user" },
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = dateStr,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                
                if (currentUserId == review.userId) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Review",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Row {
                repeat(5) { i ->
                    Icon(
                        imageVector = if (i < review.rating) Icons.Default.Star else Icons.Default.StarBorder,
                        contentDescription = null,
                        tint = Color(0xFFFFC107),
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
            
            if (review.comment.isNotBlank()) {
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = review.comment,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    lineHeight = 20.sp
                )
            }

            if (review.images.isNotEmpty()) {
                Spacer(modifier = Modifier.height(12.dp))
                androidx.compose.foundation.lazy.LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(review.images.size) { index ->
                        var showFullscreen by remember { mutableStateOf(false) }

                        Box(
                            modifier = Modifier
                                .size(72.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { showFullscreen = true }
                        ) {
                            coil.compose.AsyncImage(
                                model = review.images[index],
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        }

                        if (showFullscreen) {
                            androidx.compose.ui.window.Dialog(
                                onDismissRequest = { showFullscreen = false },
                                properties = androidx.compose.ui.window.DialogProperties(
                                    usePlatformDefaultWidth = false,
                                    dismissOnBackPress = true,
                                    dismissOnClickOutside = true
                                )
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black)
                                ) {
                                    val pagerState = androidx.compose.foundation.pager.rememberPagerState(
                                        initialPage = index,
                                        pageCount = { review.images.size }
                                    )
                                    androidx.compose.foundation.pager.HorizontalPager(
                                        state = pagerState,
                                        modifier = Modifier.fillMaxSize()
                                    ) { page ->
                                        coil.compose.AsyncImage(
                                            model = review.images[page],
                                            contentDescription = null,
                                            contentScale = ContentScale.Fit,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }

                                    IconButton(
                                        onClick = { showFullscreen = false },
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(16.dp)
                                            .size(48.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Close",
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
}

@Composable
fun WriteReviewDialog(
    existingReview: com.example.domain.model.Review?,
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, comment: String, localImages: List<String>) -> Unit
) {
    var rating by remember { mutableStateOf(existingReview?.rating ?: 5) }
    var comment by remember { mutableStateOf(existingReview?.comment ?: "") }
    var selectedImages by remember { mutableStateOf<List<String>>(existingReview?.images ?: emptyList()) }

    val photoLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val urisStrings = uris.map { it.toString() }
        val remainingSpace = 5 - selectedImages.size
        selectedImages = selectedImages + urisStrings.take(remainingSpace)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (existingReview == null) "Write a Review" else "Edit Review",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = "Tap a star to rate",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    horizontalArrangement = Arrangement.Center,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    repeat(5) { i ->
                        val starIndex = i + 1
                        IconButton(onClick = { rating = starIndex }) {
                            Icon(
                                imageVector = if (starIndex <= rating) Icons.Default.Star else Icons.Default.StarBorder,
                                contentDescription = "Rate $starIndex stars",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))
                
                OutlinedTextField(
                    value = comment,
                    onValueChange = { comment = it },
                    label = { Text("Share your experience (optional)") },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(120.dp),
                    shape = RoundedCornerShape(12.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Photos section
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Photos (${selectedImages.size}/5)",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (selectedImages.size < 5) {
                        TextButton(onClick = { photoLauncher.launch("image/*") }) {
                            Icon(Icons.Default.AddPhotoAlternate, contentDescription = null)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Add")
                        }
                    }
                }
                
                if (selectedImages.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(8.dp))
                    androidx.compose.foundation.lazy.LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(selectedImages.size) { index ->
                            val imageUrl = selectedImages[index]
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(8.dp))
                            ) {
                                coil.compose.AsyncImage(
                                    model = imageUrl,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant)
                                )
                                IconButton(
                                    onClick = { 
                                        val mutableList = selectedImages.toMutableList()
                                        mutableList.removeAt(index)
                                        selectedImages = mutableList
                                    },
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(24.dp)
                                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                                ) {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onSubmit(rating, comment.trim(), selectedImages) },
                shape = RoundedCornerShape(8.dp)
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        },
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surface
    )
}
