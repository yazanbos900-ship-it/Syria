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
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.ShoppingCart
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
    onSearchSelected: () -> Unit
) {
    var searchQuery by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("All") }
    
    val categories = listOf("All", "Apparel", "Artisanal", "Bespoke", "Furniture", "Wellness")

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
                        IconButton(
                            onClick = {},
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandBackground)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Notifications,
                                contentDescription = "Active Notifications",
                                tint = BrandTextPrimary
                            )
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
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                // Primary Custom Styled Search Bar (Launches Dedicated Search Screen on tap)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
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
                            text = "Search multi-vendor stores, items...",
                            fontSize = 15.sp,
                            color = BrandTextMuted,
                            fontWeight = FontWeight.Light
                        )
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

            // Horizontal Categories list
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(bottom = 16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 24.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categories.forEach { cat ->
                        val isSelected = selectedCategory == cat
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(if (isSelected) BrandPrimary else BrandBackground)
                                .border(
                                    1.dp,
                                    if (isSelected) Color.Transparent else BrandSoftGray,
                                    RoundedCornerShape(12.dp)
                                )
                                .clickable { selectedCategory = cat }
                                .padding(horizontal = 20.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = cat,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                color = if (isSelected) Color.White else BrandTextPrimary
                            )
                        }
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

                // High-End Selectable Product Rows
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Item 1
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
                            .clickable { onProductSelected("apple_watch_ultra_2") }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            // Image
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?auto=format&fit=crop&w=400&q=80",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // 50% OFF Badge
                            Box(
                                modifier = Modifier
                                    .padding(8.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(0xFFE8F5E9))
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = "50% OFF",
                                    color = BrandPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Precision Active Chrono",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Bespoke Horology Lab",
                                fontSize = 11.sp,
                                color = BrandTextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$189.00",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimary
                                )
                                Text(
                                    text = "★ 4.9",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }

                    // Item 2
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.White)
                            .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
                            .clickable { onProductSelected("woolen_trench_coat") }
                    ) {
                        Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                            // Image
                            AsyncImage(
                                model = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=400&q=80",
                                contentDescription = null,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            // Scarcity alert badge
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
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = "Tailored Overcoat",
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "Atelier & Co",
                                fontSize = 11.sp,
                                color = BrandTextMuted
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$249.00",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimary
                                )
                                Text(
                                    text = "★ 4.8",
                                    fontSize = 11.sp,
                                    color = Color(0xFFFFB300),
                                    fontWeight = FontWeight.Bold
                                )
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
