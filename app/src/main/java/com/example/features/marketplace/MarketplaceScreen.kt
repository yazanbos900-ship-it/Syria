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
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
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
import com.example.ui.theme.BrandSurface
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary
import com.example.ui.theme.BrandError
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.ui.platform.testTag

import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.ServiceLocator
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.features.marketplace.HomeStoresViewModel
import com.example.features.marketplace.HomeProductsViewModel
import androidx.compose.foundation.Image
import androidx.compose.ui.res.painterResource
import com.example.features.marketplace.StoreListUiState

import androidx.compose.ui.res.stringResource
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.Palette
import com.example.core.utils.LanguageManager
import com.example.ui.theme.ThemeManager
import com.example.R
import android.app.Activity
import androidx.compose.ui.platform.LocalContext

private data class PromoBanner(
    val backgroundColor: Color,
    val title: String,
    val subtitle: String,
    val imageResId: Int? = null,
    val darkText: Boolean = false
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
    onCreateStoreSelected: () -> Unit,
    onManageStoreSelected: (String) -> Unit
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAccountBottomSheet by remember { mutableStateOf(false) }

    val mainViewModel: MarketplaceViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MarketplaceViewModel(ServiceLocator.authRepository, ServiceLocator.storeRepository) as T
        }
    })
    val mainState by mainViewModel.state.collectAsStateWithLifecycle()

    val storeViewModel: HomeStoresViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return HomeStoresViewModel(ServiceLocator.storeRepository) as T
        }
    })
    val storeState by storeViewModel.state.collectAsStateWithLifecycle()

    val productViewModel: HomeProductsViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return HomeProductsViewModel(ServiceLocator.productRepository) as T
        }
    })
    val productState by productViewModel.state.collectAsStateWithLifecycle()
    
    var showBottomSheet by remember { mutableStateOf(false) }

    val promoBanners = remember {
        listOf(
            PromoBanner(
                backgroundColor = Color(0xFF101114),
                title = "WasetPlus Banner 1",
                subtitle = "",
                imageResId = R.drawable.banner_1,
                darkText = false
            ),
            PromoBanner(
                backgroundColor = Color(0xFFF9FBFD),
                title = "WasetPlus Banner 2",
                subtitle = "",
                imageResId = R.drawable.banner_2,
                darkText = true
            )
        )
    }
    // Wait, I should probably translate these promo banners too in a real app.
    // For now I'll just keep them as they are but localized if I had strings.

    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.language_selection)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguageManager.setLanguage(context, "ar")
                                showLanguageDialog = false
                                (context as? Activity)?.recreate()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = LanguageManager.getLanguage(context) == "ar", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.arabic))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguageManager.setLanguage(context, "en")
                                showLanguageDialog = false
                                (context as? Activity)?.recreate()
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = LanguageManager.getLanguage(context) == "en", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.english))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.cancel))
                }
            }
        )
    }

    if (showThemeDialog) {
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = { Text(androidx.compose.ui.res.stringResource(R.string.theme_selection)) },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeManager.setTheme(context, "light")
                                showThemeDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ThemeManager.themeModeState.value == "light", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.theme_light))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeManager.setTheme(context, "dark")
                                showThemeDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ThemeManager.themeModeState.value == "dark", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.theme_dark))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                ThemeManager.setTheme(context, "system")
                                showThemeDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = ThemeManager.themeModeState.value == "system", onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.theme_system))
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(androidx.compose.ui.res.stringResource(R.string.cancel))
                }
            }
        )
    }

    val pagerState = rememberPagerState(pageCount = { promoBanners.size })

    LaunchedEffect(pagerState.currentPage) {
        delay(3000)
        val nextPage = (pagerState.currentPage + 1) % promoBanners.size
        pagerState.animateScrollToPage(nextPage)
    }

    val filteredHomeProducts = remember(
        productState.products,
        SharedFilterState.selectedCategoryFilter,
        SharedFilterState.maxPriceRange,
        SharedFilterState.minRatingFilter,
        SharedFilterState.deliveryFilterSameDayOnly
    ) {
        productState.products.filter { product ->
            val matchesCategory = SharedFilterState.selectedCategoryFilter == "All" || product.categoryId == SharedFilterState.selectedCategoryFilter
            val matchesPrice = product.price <= SharedFilterState.maxPriceRange
            val matchesRating = product.rating >= SharedFilterState.minRatingFilter
            val matchesDelivery = true 
            matchesCategory && matchesPrice && matchesRating && matchesDelivery
        }
    }

    if (showBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrandSurface
        ) {
            FilterBottomSheetContent(
                onApply = { showBottomSheet = false }
            )
        }
    }

    if (showAccountBottomSheet) {
        ModalBottomSheet(
            onDismissRequest = { showAccountBottomSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrandSurface
        ) {
            AccountBottomSheetContent(
                user = mainState.user,
                hasStore = mainState.hasStore,
                onManageStore = { onManageStoreSelected(mainState.userStoreId!!) },
                onCreateStore = onCreateStoreSelected,
                onSelectLanguage = { showLanguageDialog = true },
                onSelectTheme = { showThemeDialog = true },
                onSignOut = onSignOut,
                onDismiss = { showAccountBottomSheet = false }
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
                    .background(BrandSurface)
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
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
                            text = androidx.compose.ui.res.stringResource(R.string.app_subtitle),
                            fontSize = 12.sp,
                            color = BrandTextMuted,
                            fontWeight = FontWeight.Medium
                        )
                    }

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
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
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.desc_wishlist),
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
                            BadgedBox(
                                badge = {
                                    if (SharedCartState.cartItems.isNotEmpty()) {
                                        Badge(
                                            containerColor = BrandPrimary,
                                            contentColor = Color.White
                                        ) {
                                            Text(
                                                text = "${SharedCartState.cartItems.sumOf { it.quantity }}",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = androidx.compose.ui.res.stringResource(R.string.desc_cart),
                                    tint = BrandTextPrimary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        IconButton(
                            onClick = { showAccountBottomSheet = true },
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(BrandBackground)
                                .testTag("home_profile_options_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Person,
                                contentDescription = "Profile Options",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp)
                            .background(BrandBackground, RoundedCornerShape(16.dp))
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
                                text = androidx.compose.ui.res.stringResource(R.string.search_placeholder),
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
                                    text = androidx.compose.ui.res.stringResource(R.string.nav_marketplace),
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
            StoresSection(
                state = storeState,
                onStoreClick = onStoreSelected,
                onViewAllClick = { /* TODO: navigation to all stores */ }
            )

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    val banner = promoBanners[page]
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp)
                            .aspectRatio(16f / 9f)
                            .clip(RoundedCornerShape(16.dp))
                            .background(banner.backgroundColor)
                    ) {
                        if (banner.imageResId != null) {
                            Image(
                                painter = painterResource(id = banner.imageResId),
                                contentDescription = banner.title,
                                contentScale = ContentScale.Fit,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
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
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

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
                        text = androidx.compose.ui.res.stringResource(R.string.protection_title),
                        color = BrandPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.protection_subtitle),
                        color = BrandTextPrimary,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.protection_desc),
                        color = BrandTextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.Start
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.featured_products),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
                Spacer(modifier = Modifier.height(12.dp))

                if (filteredHomeProducts.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = Icons.Default.SearchOff,
                                contentDescription = null,
                                tint = BrandTextMuted.copy(alpha = 0.5f),
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.no_matching_products),
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            TextButton(
                                onClick = { SharedFilterState.reset() }
                            ) {
                                Text(
                                    text = androidx.compose.ui.res.stringResource(R.string.reset_filters),
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
                                        .background(BrandSurface)
                                        .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
                                        .clickable { onProductSelected(product.id) }
                                ) {
                                    Box(modifier = Modifier.fillMaxWidth().height(140.dp)) {
                                        AsyncImage(
                                            model = product.imageUrls.firstOrNull() ?: "",
                                            contentDescription = product.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                        
                                        val marketProduct = MarketProduct(
                                            id = product.id,
                                            name = product.title,
                                            price = product.price,
                                            originalPrice = product.price * 1.5,
                                            rating = product.rating.toDouble(),
                                            reviewsCount = product.reviewCount,
                                            category = product.categoryId,
                                            storeName = "Store",
                                            deliveryTime = "Standard",
                                            dateAdded = "2026",
                                            imageUrl = product.imageUrls.firstOrNull() ?: ""
                                        )
                                        
                                         val isWishlisted = SharedWishlistState.isWishlisted(marketProduct)
                                         Box(
                                             modifier = Modifier
                                                 .align(Alignment.TopEnd)
                                                 .padding(8.dp)
                                                 .size(28.dp)
                                                 .clip(CircleShape)
                                                 .background(BrandSurface.copy(alpha = 0.9f))
                                                 .clickable {
                                                     SharedWishlistState.toggleWishlist(marketProduct)
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
                                         
                                         val savings = 33
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
                                    }
                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = product.title,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTextPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = "Store",
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
                            text = androidx.compose.ui.res.stringResource(R.string.cloud_connected_title),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        
                        Spacer(modifier = Modifier.height(6.dp))
                        
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.cloud_connected_desc),
                            fontSize = 14.sp,
                            color = BrandTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 20.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )
                        
                        Spacer(modifier = Modifier.height(20.dp))
                        
                        BrandButton(
                            text = if (mainState.hasStore) androidx.compose.ui.res.stringResource(R.string.manage_my_store) else androidx.compose.ui.res.stringResource(R.string.launch_new_store),
                            onClick = {
                                if (mainState.hasStore) {
                                    onManageStoreSelected(mainState.userStoreId!!)
                                } else {
                                    onCreateStoreSelected()
                                }
                            }
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StoresSection(state: StoreListUiState, onStoreClick: (String) -> Unit, onViewAllClick: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                androidx.compose.ui.res.stringResource(R.string.categories_title),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )
            TextButton(onClick = onViewAllClick) {
                Text(androidx.compose.ui.res.stringResource(R.string.go_to_home_button), color = BrandPrimary)
            }
        }
        
        if (state.isLoading) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                items(4) { StoreStorySkeletonItem() }
            }
        } else if (state.error != null) {
            Text(text = state.error, color = BrandError, modifier = Modifier.padding(24.dp))
        } else if (state.stores.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
            Text(androidx.compose.ui.res.stringResource(R.string.top_stores), color = BrandTextMuted)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                items(state.stores) { store ->
                    StoreStoryItem(store = store, onClick = { onStoreClick(store.id) })
                }
            }
        }
    }
}

@Composable
fun StoreStoryItem(store: com.example.domain.model.Store, onClick: () -> Unit) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp).clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .border(2.dp, if (store.status == "active") Color(0xFF1DB954) else Color.Transparent, CircleShape)
                .padding(2.dp)
        ) {
            AsyncImage(
                model = store.logoUrl,
                contentDescription = store.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(CircleShape)
            )
        }
        Text(
            text = store.name,
            fontSize = 12.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 4.dp),
            color = BrandTextPrimary
        )
        Text(
            text = androidx.compose.ui.res.stringResource(R.string.followers_count, store.followersCount),
            fontSize = 10.sp,
            color = BrandTextMuted,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun StoreStorySkeletonItem() {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(80.dp)
    ) {
        Box(
            modifier = Modifier
                .size(72.dp)
                .clip(CircleShape)
                .background(BrandSoftGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(modifier = Modifier.height(12.dp).width(50.dp).background(BrandSoftGray))
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountBottomSheetContent(
    user: com.example.domain.model.User?,
    hasStore: Boolean,
    onManageStore: () -> Unit,
    onCreateStore: () -> Unit,
    onSelectLanguage: () -> Unit,
    onSelectTheme: () -> Unit,
    onSignOut: () -> Unit,
    onDismiss: () -> Unit
) {
    val context = androidx.compose.ui.platform.LocalContext.current
    val currentLang = if (com.example.core.utils.LanguageManager.isArabic(context)) "العربية" else "English"
    
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // User profile header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(BrandBackground)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(BrandPrimary),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = (user?.name?.firstOrNull()?.toString() ?: "U").uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )
            }
            Column {
                Text(
                    text = user?.name ?: "Guest User",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
                Text(
                    text = user?.email ?: "",
                    fontSize = 12.sp,
                    color = BrandTextMuted
                )
            }
        }

        HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)

        // Options List
        // 1. Language Option
        Surface(
            onClick = {
                onDismiss()
                onSelectLanguage()
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Language,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.language_selection),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary
                        )
                        Text(
                            text = currentLang,
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 1.5. Theme Option
        Surface(
            onClick = {
                onDismiss()
                onSelectTheme()
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = Icons.Default.Palette,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = androidx.compose.ui.res.stringResource(R.string.theme_selection),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary
                        )
                        val themeMode = ThemeManager.themeModeState.value
                        val currentThemeLabel = when (themeMode) {
                            "dark" -> androidx.compose.ui.res.stringResource(R.string.theme_dark)
                            "light" -> androidx.compose.ui.res.stringResource(R.string.theme_light)
                            else -> androidx.compose.ui.res.stringResource(R.string.theme_system)
                        }
                        Text(
                            text = currentThemeLabel,
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // 2. Multi-Vendor Store Settings / Launch Center Option
        Surface(
            onClick = {
                onDismiss()
                if (hasStore) onManageStore() else onCreateStore()
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    Icon(
                        imageVector = if (hasStore) Icons.Default.Settings else Icons.Default.AddCircle,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        Text(
                            text = if (hasStore) androidx.compose.ui.res.stringResource(R.string.desc_manage_store) else androidx.compose.ui.res.stringResource(R.string.desc_create_store),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary
                        )
                        Text(
                            text = if (hasStore) androidx.compose.ui.res.stringResource(R.string.manage_my_store) else androidx.compose.ui.res.stringResource(R.string.launch_new_store),
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                    }
                }
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)

        // 3. Log out Option
        Surface(
            onClick = {
                onDismiss()
                onSignOut()
            },
            shape = RoundedCornerShape(12.dp),
            color = Color.Transparent,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 12.dp, horizontal = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.Logout,
                    contentDescription = null,
                    tint = Color.Red,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (com.example.core.utils.LanguageManager.isArabic(context)) "تسجيل الخروج" else "Log Out",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.Red
                )
            }
        }
    }
}
