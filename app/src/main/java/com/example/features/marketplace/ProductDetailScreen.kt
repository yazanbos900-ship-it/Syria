package com.example.features.marketplace

import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import coil.compose.AsyncImage
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.marketplace.ProductDetailViewModel
import kotlinx.coroutines.delay
import kotlin.math.roundToInt

// Realistic data structure for direct high-conversion sales
data class DetailProduct(
    val id: String,
    val name: String,
    val price: Double,
    val originalPrice: Double,
    val discountPercent: Int,
    val rating: Double,
    val reviewsCount: Int,
    val vendorName: String,
    val isVerifiedVendor: Boolean,
    val deliveryPromise: String,
    val badge: String,
    val images: List<String>,
    val description: String,
    val specifications: List<Pair<String, String>>,
    val stockCount: Int
)

val mockProductRepository = listOf(
    DetailProduct(
        id = "apple_watch_ultra_2",
        name = "WasetPlus Precision Active Chrono (Elite Edition)",
        price = 189.0,
        originalPrice = 378.0,
        discountPercent = 50,
        rating = 4.9,
        reviewsCount = 143,
        vendorName = "Bespoke Horology Lab",
        isVerifiedVendor = true,
        deliveryPromise = "Ships today • Free 2-Day Delivery",
        badge = "⚡ SPECIAL 50% OFF FLASH OFFER",
        images = listOf(
            "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1517502884422-41eaaced0168?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=800&q=80"
        ),
        description = "Crafted for connoisseurs of extreme durability and high performance. Features dual-frequency GPS, a state-of-the-art sapphire crystal shield screen, aerospace titanium framing, and an ultra-dense battery capacity built to last up to 72 hours. Certified Escrow Protected with WasetPlus insurance.",
        specifications = listOf(
            "Chassis" to "Aerospace-Grade Titanium",
            "Glass" to "Sapphire Crystal Shield",
            "Telemetry" to "HR, SpO2, Dual-GPS & Altimeter",
            "Battery Life" to "Up to 72 Hrs Under Load",
            "Waterproof" to "IPX8 Certified (Up to 100m)"
        ),
        stockCount = 4
    ),
    DetailProduct(
        id = "woolen_trench_coat",
        name = "Sovereign Tailored Woolen Overcoat",
        price = 249.0,
        originalPrice = 498.0,
        discountPercent = 50,
        rating = 4.8,
        reviewsCount = 92,
        vendorName = "Atelier & Co London",
        isVerifiedVendor = true,
        deliveryPromise = "Ships Tomorrow • Premium Eco-Packaging",
        badge = "🔥 SCARCITY WARNING: FEW UNITS REMAINING",
        images = listOf(
            "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1539571696357-5a69c17a67c6?auto=format&fit=crop&w=800&q=80"
        ),
        description = "Woven from superfine double-faced merino wool yarn, this Sovereign Longline Trench Coat is a masterclass in classic tailoring and fluid modern structure. Hand-finished seams combine warmth and structure without bulky weight. WasetPlus verified genuine seller.",
        specifications = listOf(
            "Material" to "90% Merino Wool, 10% Cashmere",
            "Weave" to "Double-faced premium stitch",
            "Origin" to "Hand-finished in Atelier London",
            "Fit" to "Slightly oversized drape silhouette"
        ),
        stockCount = 3
    ),
    DetailProduct(
        id = "ceramic_vase",
        name = "Nordic Earth Artisanal Terracotta Vase",
        price = 79.0,
        originalPrice = 140.0,
        discountPercent = 43,
        rating = 4.95,
        reviewsCount = 68,
        vendorName = "Studio Terrena Craft",
        isVerifiedVendor = true,
        deliveryPromise = "Ships today • Safe Fragile Insurance",
        badge = "✨ COLLECTORS CHOICE • HEIRLOOM CLASS",
        images = listOf(
            "https://images.unsplash.com/photo-1612196808214-b8e1d6145a8c?auto=format&fit=crop&w=800&q=80",
            "https://images.unsplash.com/photo-1578749556568-bc2c40e68b61?auto=format&fit=crop&w=800&q=80"
        ),
        description = "Turned carefully on the wheel and fired twice in solar-powered kilns, this ceramic work is crafted with heavy stoneware earth. The coarse organic sand texture is finished with a stunning iron silicate wash, creating raw, tactile depth resembling Nordic sea cliffs.",
        specifications = listOf(
            "Clay Type" to "Iron stoneware volcanic clay",
            "Texture" to "Coarse sand with raw silicate wash",
            "Dimensions" to "Height: 28cm, Width: 18cm",
            "Firing Temp" to "Cone 10 oxidized (1280°C)"
        ),
        stockCount = 7
    )
)

data class PriceComparison(
    val boutiqueName: String,
    val price: Double,
    val rating: String,
    val shippingInfo: String
)

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProductDetailScreen(
    productId: String?,
    onBack: () -> Unit
) {
    val viewModel: ProductDetailViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return ProductDetailViewModel(com.example.core.di.ServiceLocator.productRepository) as T
        }
    })
    
    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(productId) {
        if (productId != null) {
            viewModel.loadProduct(productId)
        }
    }

    // Adapt Product model to DetailProduct or use directly. 
    // Since DetailProduct is used for UI, we can map it.
    val domainProduct = state.product
    
    if (state.isLoading) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = BrandPrimary)
        }
        return
    }

    if (state.error != null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(text = state.error!!, color = Color.Red)
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

    // Mapping logic
    val product = DetailProduct(
        id = domainProduct.id,
        name = domainProduct.title,
        price = domainProduct.price,
        originalPrice = domainProduct.price * 1.5, // Mocking original price
        discountPercent = 33,
        rating = domainProduct.rating.toDouble(),
        reviewsCount = domainProduct.reviewCount,
        vendorName = "Vendor", // We'd need to fetch vendor name too, but let's keep it simple for now
        isVerifiedVendor = true,
        deliveryPromise = "Ships today",
        badge = if (domainProduct.stockCount < 5) "Limited Offer" else "Special Discount",
        images = if (domainProduct.imageUrls.isNotEmpty()) domainProduct.imageUrls else listOf("https://images.unsplash.com/photo-1508685096489-7aacd43bd3b1?auto=format&fit=crop&w=800&q=80"),
        description = domainProduct.description,
        specifications = listOf("Shipping" to "International", "Support" to "24/7"),
        stockCount = domainProduct.stockCount
    )

    val marketProduct = remember(product.id) {
        productCatalog.find { it.id == product.id } ?: MarketProduct(
            id = product.id,
            name = product.name,
            price = product.price,
            originalPrice = product.originalPrice,
            rating = product.rating,
            reviewsCount = product.reviewsCount,
            category = "General",
            storeName = product.vendorName,
            deliveryTime = "Standard",
            dateAdded = "2026-05-25",
            imageUrl = product.images.firstOrNull() ?: ""
        )
    }

    var quantity by remember { mutableStateOf(1) }
    val isLiked = SharedWishlistState.isWishlisted(marketProduct)
    
    // Fake interactive countdown timer for scarcity and higher CTA conversion
    var secondsRemaining by remember { mutableStateOf(522) } // 8 mins 42 secs
    LaunchedEffect(Unit) {
        while (secondsRemaining > 0) {
            delay(1000)
            secondsRemaining--
        }
    }

    val minutes = secondsRemaining / 60
    val seconds = secondsRemaining % 60
    val timerString = String.format("%02d:%02d", minutes, seconds)

    // Pulsing alpha for the limited flash banner
    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.6f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_banner"
    )

    // Layout configuration with sticky bottom bar
    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Elegant Editorial Floating Title Frame
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = BrandTextPrimary
                    )
                }

                Text(
                    text = stringResource(id = R.string.product_design),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    letterSpacing = 1.5.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    IconButton(
                        onClick = { SharedWishlistState.toggleWishlist(marketProduct) },
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandBackground)
                    ) {
                        Icon(
                            imageVector = if (isLiked) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = stringResource(id = R.string.add_to_wishlist),
                            tint = if (isLiked) Color.Red else BrandTextPrimary
                        )
                    }

                    IconButton(
                        onClick = {},
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(BrandBackground)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Share,
                            contentDescription = stringResource(id = R.string.share),
                            tint = BrandTextPrimary
                        )
                    }
                }
            }
        },
        bottomBar = {
            // High-Conversion Sticky Sticky bottom Add to Cart bar
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    // Quick Scarcity + Total Price line
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = stringResource(id = R.string.order_total),
                                fontSize = 10.sp,
                                color = BrandTextMuted,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.sp
                            )
                            val totalPayable = product.price * quantity
                            Text(
                                text = "$${String.format("%.2f", totalPayable)}",
                                fontSize = 24.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }

                        // Sparking badge showing time countdown
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFFFFF3CD))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFD32F2F))
                            )
                            Text(
                                text = stringResource(id = R.string.locked_rate, timerString),
                                color = Color(0xFF856404),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    // Direct Conversion Large Checkout Action
                    Button(
                        onClick = {
                            SharedCartState.addProductToCart(marketProduct)
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = BrandPrimary,
                            contentColor = Color.White
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(58.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(10.dp))
                            Text(
                                text = stringResource(id = R.string.add_to_cart_securely),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.5.sp
                            )
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            
            // Image Carousel with Interactive Pinch-to-Zoom layout
            val pagerState = rememberPagerState(pageCount = { product.images.size })
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(340.dp)
                    .background(Color.White)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxSize()
                ) { page ->
                    // Transformable / scale states for robust zooming
                    var scale by remember { mutableStateOf(1f) }
                    var offset by remember { mutableStateOf(Offset.Zero) }
                    val stateTransform = rememberTransformableState { zoomChange, offsetChange, _ ->
                        scale = (scale * zoomChange).coerceIn(1f, 3.5f)
                        offset += offsetChange
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .transformable(state = stateTransform)
                            .clip(RoundedCornerShape(0.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = product.images[page],
                            contentDescription = "High resolution product preview. Pinch to zoom.",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .graphicsLayer(
                                    scaleX = scale,
                                    scaleY = scale,
                                    translationX = offset.x,
                                    translationY = offset.y
                                )
                        )
                    }
                }

                // Smooth luxury linear overlay looking down
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.15f))
                            )
                        )
                )

                // Page numbering overlay badge
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.65f))
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "${pagerState.currentPage + 1} / ${product.images.size}",
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }

                // Help hint for zooming interaction
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(16.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.White.copy(alpha = 0.85f))
                        .border(1.dp, BrandSoftGray, RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.pinch_to_zoom),
                        color = BrandTextPrimary,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Limited-time offer badge (UI only) - Scarcity & Conversion driving
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color(0xFFE8F5E9)) // Light mint green background
                    .padding(vertical = 10.dp, horizontal = 24.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .background(BrandPrimary)
                                .graphicsLayer(alpha = pulseAlpha)
                        )
                        Text(
                            text = if (product.badge == "Limited Offer") stringResource(id = R.string.limited_offer) else stringResource(id = R.string.special_discount),
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.5.sp
                        )
                    }

                    // Scarcity state (stock)
                    if (product.stockCount <= 5) {
                        Text(
                            text = stringResource(id = R.string.only_left, product.stockCount),
                            color = Color(0xFFC62828), // Dark warning red
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            // Product Core Editorial Details Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp),
                    verticalArrangement = Arrangement.Top
                ) {
                    // Vendor Details with Trust signals
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = product.vendorName,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextMuted
                            )
                            if (product.isVerifiedVendor) {
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified Vendor",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        // Rating pill
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(3.dp),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BrandBackground)
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = null,
                                tint = Color(0xFFFFB300),
                                modifier = Modifier.size(12.dp)
                            )
                            Text(
                                text = "${product.rating}",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = "(${product.reviewsCount})",
                                fontSize = 10.sp,
                                color = BrandTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Title
                    Text(
                        text = product.name,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        lineHeight = 28.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    // Price display (Huge aesthetic hierarchy)
                    Row(
                        verticalAlignment = Alignment.Bottom,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "$${String.format("%.2f", product.price)}",
                            fontSize = 32.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = BrandPrimary
                        )

                        Text(
                            text = "$${String.format("%.2f", product.originalPrice)}",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            textDecoration = TextDecoration.LineThrough,
                            color = BrandTextMuted
                        )

                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(Color(0xFFFFEBEE))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "-${product.discountPercent}% OFF",
                                color = Color(0xFFC62828),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Trust Signals Section
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Trust item 1
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "🛡️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.escrow_policy),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.protected_by),
                                fontSize = 9.sp,
                                color = BrandTextMuted
                            )
                        }
                        
                        // Trust item 2
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "⚡", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.immediate_dispatch),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = product.deliveryPromise,
                                fontSize = 9.sp,
                                color = BrandTextMuted
                            )
                        }

                        // Trust item 3
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(text = "↩️", fontSize = 20.sp)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = stringResource(id = R.string.free_returns),
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.thirty_day_window),
                                fontSize = 9.sp,
                                color = BrandTextMuted
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
                    Spacer(modifier = Modifier.height(16.dp))

                    // Description
                    Text(
                        text = stringResource(id = R.string.story_context),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = product.description,
                        fontSize = 14.sp,
                        color = BrandTextPrimary,
                        lineHeight = 22.sp
                    )
                }
            }

            // Interactive Stepper / Quantity selector Card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = stringResource(id = R.string.quantity_selector),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextMuted,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(id = R.string.choose_dynamic_size),
                            fontSize = 12.sp,
                            color = BrandTextPrimary
                        )
                    }

                    // Luxury Stepper Controller Design without external symbols dependence
                    Row(
                        modifier = Modifier
                            .background(BrandBackground, RoundedCornerShape(12.dp))
                            .border(1.dp, BrandSoftGray, RoundedCornerShape(12.dp))
                            .padding(4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Subtract button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { if (quantity > 1) quantity-- }
                                .border(1.dp, BrandSoftGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "—",
                                color = BrandTextPrimary,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        Text(
                            text = "$quantity",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )

                        // Add button
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color.White)
                                .clickable { if (quantity < product.stockCount) quantity++ }
                                .border(1.dp, BrandSoftGray, RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = "Add Quantity",
                                tint = BrandTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            // Price comparison layout
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 24.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = Color.White)
            ) {
                Column(
                    modifier = Modifier.padding(24.dp)
                ) {
                    Text(
                        text = stringResource(id = R.string.multi_vendor_compare),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextMuted,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))

                    val alternatives = listOf(
                        PriceComparison("Aethelgard Premium Store", product.price + 12.0, "★ 4.6", "Ships free today"),
                        PriceComparison("Meridian Wholesale Inc", product.price + 24.0, "★ 4.4", "Ships in 2 days")
                    )

                    // Current best offer signal
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPrimary.copy(alpha = 0.08f))
                            .border(1.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(12.dp))
                            .padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = product.vendorName,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextPrimary
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(4.dp))
                                        .background(BrandPrimary)
                                        .padding(horizontal = 5.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.best_rate),
                                        color = Color.White,
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Text(
                                text = "Verified Premium Partner",
                                fontSize = 11.sp,
                                color = BrandTextMuted
                            )
                        }
                        
                        Text(
                            text = "$${String.format("%.2f", product.price)}",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    // Second comparisons list
                    alternatives.forEach { alt ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = alt.boutiqueName,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandTextPrimary
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = alt.rating,
                                        fontSize = 11.sp,
                                        color = Color(0xFFC49000),
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = alt.shippingInfo,
                                    fontSize = 11.sp,
                                    color = BrandTextMuted
                                )
                            }
                            
                            Text(
                                text = "$${String.format("%.2f", alt.price)}",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                        }
                        if (alt != alternatives.last()) {
                            HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)
                        }
                    }
                }
            }

            // Related products horizontal scroll section
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 32.dp)
            ) {
                Text(
                    text = "COMMUNAL RECOMMENDATIONS",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextMuted,
                    letterSpacing = 1.sp,
                    modifier = Modifier.padding(horizontal = 24.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    mockProductRepository.forEach { relProduct ->
                        // Show all EXCEPT the current active product is best, or show all for beautiful variety
                        Card(
                            modifier = Modifier
                                .width(160.dp)
                                .clickable {
                                    // Normally navigates to this product ID, we can trigger refreshing or re-route.
                                },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BrandSoftGray),
                            colors = CardDefaults.cardColors(containerColor = Color.White)
                        ) {
                            Column {
                                AsyncImage(
                                    model = relProduct.images.first(),
                                    contentDescription = relProduct.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(115.dp)
                                )
                                Column(
                                    modifier = Modifier.padding(10.dp)
                                ) {
                                    Text(
                                        text = relProduct.name,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = relProduct.vendorName,
                                        fontSize = 10.sp,
                                        color = BrandTextMuted,
                                        maxLines = 1
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Text(
                                            text = "$${relProduct.price}",
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandPrimary
                                        )
                                        Text(
                                            text = "★ ${relProduct.rating}",
                                            fontSize = 10.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = Color(0xFFFFB300)
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
