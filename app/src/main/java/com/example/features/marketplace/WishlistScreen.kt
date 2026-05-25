package com.example.features.marketplace

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

enum class WishlistSortOption(val label: String) {
    NewestSaved("Newest Saved"),
    PriceLowToHigh("Price: Low to High"),
    PriceHighToLow("Price: High to Low")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WishlistScreen(
    onNavigateBack: () -> Unit,
    onProductSelected: (String) -> Unit,
    onGoToCart: () -> Unit
) {
    val wishlist = SharedWishlistState.wishlistItems
    var activeSortOption by remember { mutableStateOf(WishlistSortOption.NewestSaved) }
    
    // Coroutine scope for transient popups
    val coroutineScope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Quick in-cart local indicator mapping for beautiful interactive micro-feedbacks
    val recentlyAddedIds = remember { mutableStateListOf<String>() }

    // Sorted items computation
    val sortedItems = remember(wishlist.size, activeSortOption) {
        when (activeSortOption) {
            WishlistSortOption.NewestSaved -> {
                // Return items in saved order (newest saved normally at the end or reversed)
                wishlist.reversed()
            }
            WishlistSortOption.PriceLowToHigh -> {
                wishlist.sortedBy { it.price }
            }
            WishlistSortOption.PriceHighToLow -> {
                wishlist.sortedByDescending { it.price }
            }
        }
    }

    // Queue value calculation to simulate high-retention "shopping queue"
    val queueTotalValue = remember(wishlist.size) {
        wishlist.sumOf { it.price }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            Surface(
                color = Color.White,
                tonalElevation = 0.dp,
                shadowElevation = 0.dp,
                border = BorderStroke(0.5.dp, BrandSoftGray)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandBackground)
                            .testTag("wishlist_back_button")
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = BrandTextPrimary
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "SHOPPING QUEUE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 1.2.sp
                        )
                        Text(
                            text = "Saved Wishlist",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandTextPrimary
                        )
                    }

                    // Cart icon with dynamic count indicator as secondary bridge to purchase
                    IconButton(
                        onClick = onGoToCart,
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandBackground)
                            .testTag("wishlist_cart_button")
                    ) {
                        BadgedBox(
                            badge = {
                                if (SharedCartState.cartItems.isNotEmpty()) {
                                    val cartCount = SharedCartState.cartItems.sumOf { it.quantity }
                                    Badge(
                                        containerColor = BrandPrimary,
                                        contentColor = Color.White
                                    ) {
                                        Text(text = "$cartCount", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                    }
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "View Cart",
                                tint = BrandTextPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        },
        bottomBar = {
            // Highly optimized retail "Queue summary" stickiness
            AnimatedVisibility(
                visible = wishlist.isNotEmpty(),
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
            ) {
                Surface(
                    color = Color.White,
                    tonalElevation = 4.dp,
                    shadowElevation = 12.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    border = BorderStroke(0.5.dp, BrandSoftGray)
                ) {
                    Row(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Text(
                                text = "TOTAL QUEUE VALUE",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextMuted,
                                letterSpacing = 0.8.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$${String.format("%.2f", queueTotalValue)}",
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandPrimary
                            )
                            Text(
                                text = "${wishlist.size} items ready to order",
                                fontSize = 11.sp,
                                color = BrandTextMuted
                            )
                        }

                        Button(
                            onClick = {
                                coroutineScope.launch {
                                    // Add all wishlist items to cart
                                    wishlist.forEach { product ->
                                        SharedCartState.addProductToCart(product)
                                    }
                                    snackbarHostState.showSnackbar(
                                        message = "All ${wishlist.size} items moved to your shopping cart!",
                                        duration = SnackbarDuration.Short
                                    )
                                    onGoToCart()
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(14.dp),
                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 14.dp),
                            modifier = Modifier
                                .height(52.dp)
                                .testTag("add_all_to_cart_button")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                )
                                Text(
                                    text = "ORDER QUEUE",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        if (wishlist.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BrandBackground),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.padding(32.dp)
                ) {
                    Text(
                        text = "✨",
                        fontSize = 72.sp,
                        modifier = Modifier.padding(bottom = 16.dp)
                    )
                    
                    Text(
                        text = "Your Wishlist is Empty",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandTextPrimary,
                        textAlign = TextAlign.Center
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    Text(
                        text = "Curate your ideal collection here. Save products you love, track exclusive discounts, and complete checkout instantly. No clutter, pure convenience.",
                        fontSize = 13.sp,
                        color = BrandTextMuted,
                        textAlign = TextAlign.Center,
                        lineHeight = 20.sp
                    )
                    
                    Spacer(modifier = Modifier.height(28.dp))
                    
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(50.dp)
                            .testTag("wishlist_empty_cta")
                    ) {
                        Text(
                            text = "Start exploring products",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(BrandBackground)
            ) {
                // Sorting & Configuration Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${wishlist.size} Saved Items",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextMuted
                    )

                    // Sort pills selector with robust inline UX
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        WishlistSortOption.values().forEach { option ->
                            val isSelected = activeSortOption == option
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) BrandPrimary else Color.White)
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) Color.Transparent else BrandSoftGray,
                                        shape = RoundedCornerShape(20.dp)
                                    )
                                    .clickable { activeSortOption = option }
                                    .padding(horizontal = 10.dp, vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = when (option) {
                                        WishlistSortOption.NewestSaved -> "Newest"
                                        WishlistSortOption.PriceLowToHigh -> "$ Low"
                                        WishlistSortOption.PriceHighToLow -> "$ High"
                                    },
                                    color = if (isSelected) Color.White else BrandTextMuted,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // Wishlist Grid Layout (2 columns)
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier
                        .fillMaxSize()
                        .weight(1f)
                        .testTag("wishlist_grid")
                ) {
                    items(
                        items = sortedItems,
                        key = { it.id }
                    ) { product ->
                        // Clean interactive cards, no heavy shadows
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BrandSoftGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product.id) }
                                .testTag("wishlist_item_card_${product.id}")
                        ) {
                            Column {
                                // Image section with Floating Favorite Heart and delivery badge
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(135.dp)
                                        .background(BrandBackground)
                                ) {
                                    AsyncImage(
                                        model = product.imageUrl,
                                        contentDescription = product.name,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier.fillMaxSize()
                                    )

                                    // Same Day express badge
                                    if (product.deliveryTime == "Same Day") {
                                        Box(
                                            modifier = Modifier
                                                .align(Alignment.BottomStart)
                                                .padding(6.dp)
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFFFF9C4)) // Soft yellow
                                                .padding(horizontal = 5.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "⚡ Same Day",
                                                fontSize = 8.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = Color(0xFFF57F17)
                                            )
                                        }
                                    }

                                    // Floating heart toggle on top right
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .padding(6.dp)
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(Color.White.copy(alpha = 0.9f))
                                            .clickable {
                                                coroutineScope.launch {
                                                    SharedWishlistState.toggleWishlist(product)
                                                    snackbarHostState.showSnackbar(
                                                        message = "Removed '${product.name}' from wishlist",
                                                        duration = SnackbarDuration.Short
                                                    )
                                                }
                                            }
                                            .testTag("remove_heart_button_${product.id}"),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Favorite,
                                            contentDescription = "Remove from Wishlist",
                                            tint = Color.Red,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                // Product metadata segment
                                Column(
                                    modifier = Modifier.padding(10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = product.storeName,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    Text(
                                        text = product.name,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )

                                    // Price comparison indicator (savings highlight)
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = "$${String.format("%.2f", product.price)}",
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = BrandTextPrimary
                                        )

                                        if (product.originalPrice != null) {
                                            Text(
                                                text = "$${String.format("%.2f", product.originalPrice)}",
                                                fontSize = 10.sp,
                                                color = BrandTextMuted,
                                                textDecoration = TextDecoration.LineThrough
                                            )
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(4.dp))

                                    // Quick add-to-cart button on each item
                                    val isAdded = recentlyAddedIds.contains(product.id)

                                    Button(
                                        onClick = {
                                            SharedCartState.addProductToCart(product)
                                            if (!recentlyAddedIds.contains(product.id)) {
                                                recentlyAddedIds.add(product.id)
                                                coroutineScope.launch {
                                                    delay(3000)
                                                    recentlyAddedIds.remove(product.id)
                                                }
                                            }
                                            coroutineScope.launch {
                                                snackbarHostState.showSnackbar(
                                                    message = "Added '${product.name}' to cart!",
                                                    duration = SnackbarDuration.Short
                                                )
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(
                                            containerColor = if (isAdded) Color(0xFFE8F5E9) else BrandPrimary,
                                            contentColor = if (isAdded) BrandPrimary else Color.White
                                        ),
                                        shape = RoundedCornerShape(10.dp),
                                        contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(34.dp)
                                            .testTag("quick_add_cart_${product.id}")
                                    ) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.Center,
                                            modifier = Modifier.fillMaxWidth()
                                        ) {
                                            if (isAdded) {
                                                Icon(
                                                    imageVector = Icons.Default.Check,
                                                    contentDescription = "Added successfully",
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(3.dp))
                                                Text(
                                                    text = "In Cart",
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.ExtraBold
                                                )
                                            } else {
                                                Icon(
                                                    imageVector = Icons.Default.ShoppingCart,
                                                    contentDescription = "Add to Cart",
                                                    modifier = Modifier.size(13.dp)
                                                )
                                                Spacer(modifier = Modifier.width(4.dp))
                                                Text(
                                                    text = "Add to Cart",
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
        }
    }
}
