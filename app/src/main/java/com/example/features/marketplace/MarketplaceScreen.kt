package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import kotlinx.coroutines.delay
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.AddCircle
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.BrandButton
import com.example.components.BrandCard
import com.example.components.BrandTextField
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandError
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.List
import androidx.compose.ui.platform.testTag

private data class PromoBanner(
    val backgroundColor: Color,
    val title: String,
    val subtitle: String
)

@OptIn(ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun MarketplaceScreen(
    onProductSelected: (String) -> Unit,
    onStoreSelected: (String) -> Unit,
    onSignOut: () -> Unit,
    onCartSelected: () -> Unit,
    onSearchSelected: () -> Unit,
    onWishlistSelected: () -> Unit,
    onCreateStoreSelected: () -> Unit
) {
    var showBottomSheet by remember { mutableStateOf(false) }

    val promoBanners = remember {
        listOf(
            PromoBanner(Color(0xFF1DB954), "عروض نهاية الأسبوع", "خصم يصل لـ 50%"),
            PromoBanner(Color(0xFF1A1A2E), "متاجر جديدة", "اكتشف أحدث المتاجر"),
            PromoBanner(Color(0xFF0D1F0D), "توصيل مجاني", "على الطلبات فوق $30")
        )
    }

    val pagerState = rememberPagerState(pageCount = { promoBanners.size })

    LaunchedEffect(pagerState.currentPage) {
        delay(3000)
        val nextPage = (pagerState.currentPage + 1) % promoBanners.size
        pagerState.animateScrollToPage(nextPage)
    }

    val filteredHomeProducts = remember(
        SharedFilterState.selectedCategoryFilter,
        SharedFilterState.maxPriceRange,
        SharedFilterState.minRatingFilter,
        SharedFilterState.deliveryFilterSameDayOnly
    ) {
        productCatalog.filter { product ->
            val matchesCategory = SharedFilterState.selectedCategoryFilter == "All" || product.category == SharedFilterState.selectedCategoryFilter
            val matchesPrice = product.price <= SharedFilterState.maxPriceRange
            val matchesRating = product.rating >= SharedFilterState.minRatingFilter
            val matchesDelivery = !SharedFilterState.deliveryFilterSameDayOnly || product.deliveryTime == "Same Day"
            matchesCategory && matchesPrice && matchesRating && matchesDelivery
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color.White
        ) {
            FilterBottomSheetContent(
                onApply = { showBottomSheet = false }
            )
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                // Editorial Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "WasetPlus",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            letterSpacing = 0.5.sp
                        )
                        Text(
                            text = "Connected Multi-Vendor Feed",
                            fontSize = 12.sp,
                            color = BrandTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        // High-retention Wishlist trigger button (Heart toggle) with badge
                        IconButton(
                            onClick = onWishlistSelected,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandBackground)
                                .testTag("home_wishlist_button")
                        ) {
                            BadgedBox(
                                badge = {
                                    if (SharedWishlistState.wishlistItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = Color.Red,
                                            contentColor = Color.White
                                        ) {
                                            Text(text = "${SharedWishlistState.wishlistItems.size}", fontSize = 10.sp, fontWeight = FontWeight.Bold)
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Favorite,
                                    contentDescription = "Show Wishlist Queue",
                                    tint = Color.Red,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = onCartSelected,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Show Shopping Cart",
                                tint = BrandTextPrimary
                            )
                        }

                        IconButton(
                            onClick = onCreateStoreSelected,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandBackground)
                                .testTag("home_create_store_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.AddCircle,
                                contentDescription = "Create Store Flow",
                                tint = BrandPrimary
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Primary Custom Styled Search Bar alongside Outlined Filters Button
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(Color.White, RoundedCornerShape(16.dp))
                            .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
                            .clickable { onSearchSelected() }
                            .padding(horizontal = 16.dp),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = "Search Trigger Icon",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(
                                text = "Search stores, items...",
                                fontSize = 14.sp,
                                color = BrandTextMuted,
                                fontWeight = FontWeight.Light,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    OutlinedButton(
                        onClick = { showBottomSheet = true },
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = Color(0xFF1DB954)
                        ),
                        border = BorderStroke(1.dp, Color(0xFF1DB954)),
                        shape = RoundedCornerShape(16.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp),
                        modifier = Modifier
                            .height(56.dp)
                            .testTag("filters_home_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.List,
                                    contentDescription = "Filters Icon",
                                    tint = Color(0xFF1DB954),
                                    modifier = Modifier.size(20.dp)
                                )
                                Text(
                                    text = "فلاتر",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF1DB954)
                                )
                            }
                            
                            if (SharedFilterState.isActive) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .offset(x = 6.dp, y = (-6).dp)
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF1DB954))
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
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.Top
        ) {
            // Professional Banner Carousel (3 Banners, height: 180dp, rounded corners 16dp, auto-scrolling)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) { page ->
                    val banner = promoBanners[page]
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(horizontal = 24.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(banner.backgroundColor)
                    ) {
                        // Luxurious ambient geometric design ornaments for professional look
                        Box(
                            modifier = Modifier
                                .size(130.dp)
                                .align(Alignment.TopEnd)
                                .offset(x = 20.dp, y = (-20).dp)
                                .background(Color.White.copy(alpha = 0.08f), CircleShape)
                        )
                        Box(
                            modifier = Modifier
                                .size(170.dp)
                                .align(Alignment.BottomStart)
                                .offset(x = (-30).dp, y = 30.dp)
                                .background(Color.White.copy(alpha = 0.04f), CircleShape)
                        )

                        // Rich Editorial Content (Right-aligned / RTL Arabic typography)
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(24.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.End
                        ) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color.White.copy(alpha = 0.15f))
                                    .padding(horizontal = 10.dp, vertical = 5.dp)
                            ) {
                                Text(
                                    text = "WasetPlus عروض",
                                    color = Color.White.copy(alpha = 0.95f),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 1.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = banner.title,
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold,
                                textAlign = TextAlign.Right
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = banner.subtitle,
                                color = Color.White.copy(alpha = 0.85f),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium,
                                textAlign = TextAlign.Right
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Pagination Indicator Dots matching active dot = #1DB954 exactly
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(promoBanners.size) { index ->
                        val isSelected = pagerState.currentPage == index
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 4.dp)
                                .size(if (isSelected) 10.dp else 8.dp)
                                .clip(CircleShape)
                                .background(
                                    if (isSelected) Color(0xFF1DB954) else Color(0xFFECECEC)
                                )
                        )
                    }
                }
            }

            // Promotional Design Hero Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(BrandPrimary.copy(alpha = 0.08f))
                    .border(1.dp, BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(24.dp))
                    .padding(24.dp)
            ) {
                Column {
                    Text(
                        text = "WasetPlus Protection",
                        color = BrandPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "100% Escrow Protection Guarantee",
                        color = BrandTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Funds are safely locked under cloud service escrow until products are inspected and verified.",
                        color = BrandTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // Real Production-Ready Architecture Skeletons & Status Card
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = "Active Featured Products (Scrollable)",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                // High-End Selectable Product Grid / Row
                if (filteredHomeProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text("🔍", fontSize = 48.sp)
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "لا توجد منتجات مطابقة للفلاتر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { SharedFilterState.reset() }
                            ) {
                                Text(
                                    text = "إعادة تعيين الفلاتر",
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                } else {
                    filteredHomeProducts.chunked(2).forEach { rowProducts ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            rowProducts.forEach { product ->
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(16.dp))
                                        .background(Color.White)
                                        .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
                                        .clickable { onProductSelected(product.id) }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                        AsyncImage(
                                            model = product.imageUrl,
                                            contentDescription = product.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        // Heart Favorite button for high conversion wishlist adding directly from home feed
                                         val isWishlisted = SharedWishlistState.isWishlisted(product)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.TopEnd)
                                                 .padding(8.dp)
                                                 .size(28.dp)
                                                 .clip(CircleShape)
                                                 .background(Color.White.copy(alpha = 0.9f))
                                                 .clickable {
                                                     SharedWishlistState.toggleWishlist(product)
                                                 }
                                                 .testTag("home_wishlist_toggle_${product.id}"),
                                             contentAlignment = Alignment.Center
                                         ) {
                                             Icon(
                                                 imageVector = if (isWishlisted) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                                 contentDescription = "Toggle Wishlist",
                                                 tint = if (isWishlisted) Color.Red else BrandTextMuted,
                                                 modifier = Modifier.size(14.dp)
                                             )
                                         }

                                         if (product.originalPrice != null) {
                                            val savings = ((product.originalPrice - product.price) / product.originalPrice * 100).toInt()
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFE8F5E9))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "$savings% OFF",
                                                    color = BrandPrimary,
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        } else if (product.id == "woolen_trench_coat") {
                                            Box(
                                                modifier = Modifier
                                                    .padding(8.dp)
                                                    .clip(RoundedCornerShape(6.dp))
                                                    .background(Color(0xFFFFEBEE))
                                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                                            ) {
                                                Text(
                                                    text = "LIMITED",
                                                    color = Color(0xFFC62828),
                                                    fontSize = 10.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = product.name,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = product.storeName,
                                            fontSize = 11.sp,
                                            color = BrandTextMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Text(
                                                text = "$${String.format("%.2f", product.price)}",
                                                fontSize = 15.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = BrandPrimary
                                            )
                                            Text(
                                                text = "★ ${product.rating}",
                                                fontSize = 11.sp,
                                                color = Color(0xFFFFB300),
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }
                            if (rowProducts.size == 1) {
                                Spacer(modifier = Modifier.weight(1f))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Large Design Card instructing how live dynamic products populate.
                BrandCard(
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(64.dp)
                                .clip(CircleShape)
                                .background(BrandBackground),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "☁️",
                                fontSize = 28.sp
                            )
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        Text(
                            text = "Cloud Database Connected",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = "Real-time updates via Cloud Firestore feeds. Once you upload catalogs to 'products' or 'categories' database collections, listings will sync instantly in this interface.",
                            fontSize = 14.sp,
                            color = BrandTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        BrandButton(
                            text = "Launch New Store",
                            onClick = { onStoreSelected("new_store") }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
