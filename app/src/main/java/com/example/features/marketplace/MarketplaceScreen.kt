package com.example.features.marketplace

import com.example.domain.model.getPriceInUSD
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
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Bolt
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
import com.example.domain.usecase.SubmitSubscriptionRequestUseCase
import com.example.domain.usecase.GetSubscriptionRequestsByStoreUseCase
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Work
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
import androidx.compose.material.icons.filled.Paid
import com.example.core.utils.LanguageManager
import com.example.core.utils.CurrencyManager
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
    onManageStoreSelected: (String) -> Unit,
    onAdminSelected: () -> Unit = {},
    onJobsSelected: () -> Unit = {},
    onAllStoresSelected: () -> Unit = {}
) {
    val context = LocalContext.current
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showAccountBottomSheet by remember { mutableStateOf(false) }

    val currentCurrencyState by CurrencyManager.currentCurrency.collectAsStateWithLifecycle()

    val mainViewModel: MarketplaceViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MarketplaceViewModel(
                ServiceLocator.authRepository,
                ServiceLocator.storeRepository,
                SubmitSubscriptionRequestUseCase(ServiceLocator.subscriptionRepository),
                GetSubscriptionRequestsByStoreUseCase(ServiceLocator.subscriptionRepository)
            ) as T
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
            return HomeProductsViewModel(
                ServiceLocator.productRepository,
                ServiceLocator.recommendationRepository,
                ServiceLocator.authRepository
            ) as T
        }
    })
    val featuredState by productViewModel.featuredState.collectAsStateWithLifecycle()
    val newArrivalsState by productViewModel.newArrivalsState.collectAsStateWithLifecycle()
    val trendingState by productViewModel.trendingState.collectAsStateWithLifecycle()
    val recommendationsState by productViewModel.recommendationsState.collectAsStateWithLifecycle()
    val bestRatedState by productViewModel.bestRatedState.collectAsStateWithLifecycle()
    val featuredJobsState by productViewModel.featuredJobsState.collectAsStateWithLifecycle()
    val latestJobsState by productViewModel.latestJobsState.collectAsStateWithLifecycle()
    
    var directAdsList by remember { mutableStateOf<List<com.example.domain.model.Product>>(emptyList()) }
    var isLoadingDirectAds by remember { mutableStateOf(false) }
    var directAdsError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        isLoadingDirectAds = true
        try {
            val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            db.collection("direct_ads")
                .whereEqualTo("status", "active")
                .addSnapshotListener { snapshot, e ->
                    if (e != null) {
                        directAdsError = e.message
                        isLoadingDirectAds = false
                        return@addSnapshotListener
                    }
                    if (snapshot != null) {
                        val ads = snapshot.documents.mapNotNull { doc ->
                            try {
                                val id = doc.id
                                val title = doc.getString("title") ?: ""
                                val description = doc.getString("description") ?: ""
                                val price = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                                val images = (doc.get("images") as? List<String>) ?: (doc.get("imageUrls") as? List<String>) ?: emptyList()
                                val categoryId = doc.getString("categoryId") ?: ""
                                com.example.domain.model.Product(
                                    id = id,
                                    title = title,
                                    description = description,
                                    price = price,
                                    imageUrls = images,
                                    categoryId = categoryId,
                                    storeId = "direct_ad", // set to "direct_ad" to show the Direct Ad badge
                                    rating = 5.0f,
                                    reviewCount = 0,
                                    isAvailable = true,
                                    stockCount = 1,
                                    currency = "USD",
                                    isApproved = true,
                                    isFlagged = false,
                                    createdAt = System.currentTimeMillis()
                                )
                            } catch (ex: Exception) {
                                null
                            }
                        }
                        directAdsList = ads
                    }
                    isLoadingDirectAds = false
                }
        } catch (e: Exception) {
            directAdsError = e.message
            isLoadingDirectAds = false
        }
    }
    
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

    if (showCurrencyDialog) {
        val isAr = LanguageManager.isArabic(context)
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = { Text(if (isAr) "تحديد العملة المعتمدة" else "Select Currency") },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CurrencyManager.setCurrency(context, CurrencyManager.Currency.SYP)
                                showCurrencyDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentCurrencyState == CurrencyManager.Currency.SYP, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isAr) "الليرة السورية (ل.س)" else "Syrian Pound (SYP)")
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CurrencyManager.setCurrency(context, CurrencyManager.Currency.USD)
                                showCurrencyDialog = false
                            }
                            .padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentCurrencyState == CurrencyManager.Currency.USD, onClick = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (isAr) "دولار أمريكي (USD)" else "US Dollar (USD)")
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(if (isAr) "إلغاء" else "Cancel")
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
        featuredState.products,
        SharedFilterState.selectedCategoryFilter,
        SharedFilterState.maxPriceRange,
        SharedFilterState.minRatingFilter,
        SharedFilterState.deliveryFilterSameDayOnly
    ) {
        featuredState.products.filter { product ->
            val matchesCategory = SharedFilterState.selectedCategoryFilter == "All" || product.categoryId == SharedFilterState.selectedCategoryFilter
            val matchesPrice = product.price <= SharedFilterState.maxPriceRange
            val matchesRating = product.rating >= SharedFilterState.minRatingFilter
            val matchesDelivery = true 
            matchesCategory && matchesPrice && matchesRating && matchesDelivery
        }
    }

    val filteredStores = remember(
        storeState.stores,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            storeState.stores
        } else {
            storeState.stores.filter { store ->
                store.categoryId == SharedFilterState.selectedCategoryFilter
            }
        }
    }

    val filteredTopStores = remember(
        storeState.topStores,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            storeState.topStores
        } else {
            storeState.topStores.filter { store ->
                store.categoryId == SharedFilterState.selectedCategoryFilter
            }
        }
    }

    val filteredNewArrivals = remember(
        newArrivalsState.products,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            newArrivalsState.products
        } else {
            newArrivalsState.products.filter { it.categoryId == SharedFilterState.selectedCategoryFilter }
        }
    }

    val filteredTrending = remember(
        trendingState.products,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            trendingState.products
        } else {
            trendingState.products.filter { it.categoryId == SharedFilterState.selectedCategoryFilter }
        }
    }

    val filteredRecommendations = remember(
        recommendationsState.products,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            recommendationsState.products
        } else {
            recommendationsState.products.filter { it.categoryId == SharedFilterState.selectedCategoryFilter }
        }
    }

    val filteredBestRated = remember(
        bestRatedState.products,
        SharedFilterState.selectedCategoryFilter
    ) {
        if (SharedFilterState.selectedCategoryFilter == "All" || 
            SharedFilterState.selectedCategoryFilter.isBlank()) {
            bestRatedState.products
        } else {
            bestRatedState.products.filter { it.categoryId == SharedFilterState.selectedCategoryFilter }
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
                onSelectCurrency = { showCurrencyDialog = true },
                onSignOut = onSignOut,
                onAdminClick = onAdminSelected,
                onDismiss = { showAccountBottomSheet = false }
            )
        }
    }

    LaunchedEffect(mainState.actionMessage) {
        val msg = mainState.actionMessage
        if (msg != null) {
            android.widget.Toast.makeText(context, msg, android.widget.Toast.LENGTH_LONG).show()
            mainViewModel.clearActionMessage()
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

            // Direct Ads Section
            ProductHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "الإعلانات المباشرة" else "Direct Ads",
                products = directAdsList,
                isLoading = isLoadingDirectAds,
                error = directAdsError,
                stores = storeState.stores,
                onProductClick = onProductSelected,
                context = context
            )

            StoresSection(
                stores = filteredTopStores,
                isLoading = storeState.isLoadingTopStores,
                error = storeState.errorTopStores,
                onStoreClick = onStoreSelected,
                onViewAllClick = onAllStoresSelected
            )



            // Featured Jobs Section
            JobHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "الوظائف المميزة" else "Featured Jobs",
                jobs = featuredJobsState.jobs,
                isLoading = featuredJobsState.isLoading,
                error = featuredJobsState.error,
                onJobClick = { onJobsSelected() },
                onViewAllClick = { onJobsSelected() },
                context = context
            )

            // Latest Jobs Section
            JobHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "أحدث الوظائف" else "Latest Jobs",
                jobs = latestJobsState.jobs,
                isLoading = latestJobsState.isLoading,
                error = latestJobsState.error,
                onJobClick = { onJobsSelected() },
                onViewAllClick = { onJobsSelected() },
                context = context
            )


            // 1. New Arrivals Section
            ProductHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "وصل حديثاً" else "New Arrivals",
                products = filteredNewArrivals,
                isLoading = newArrivalsState.isLoading,
                error = newArrivalsState.error,
                stores = storeState.stores,
                onProductClick = onProductSelected,
                context = context
            )

            // 2. Trending Now Section
            ProductHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "الأكثر رواجاً الآن" else "Trending Now",
                products = filteredTrending,
                isLoading = trendingState.isLoading,
                error = trendingState.error,
                stores = storeState.stores,
                onProductClick = onProductSelected,
                context = context
            )

            // 3. Community Recommendations Section
            ProductHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "توصيات مجتمعية" else "Community Recommendations",
                products = filteredRecommendations,
                isLoading = recommendationsState.isLoading,
                error = recommendationsState.error,
                stores = storeState.stores,
                onProductClick = onProductSelected,
                context = context
            )

            // 4. Best Rated Products Section
            ProductHorizontalSection(
                title = if (com.example.core.utils.LanguageManager.isArabic(context)) "المنتجات الأعلى تقييماً" else "Best Rated Products",
                products = filteredBestRated,
                isLoading = bestRatedState.isLoading,
                error = bestRatedState.error,
                stores = storeState.stores,
                onProductClick = onProductSelected,
                context = context
            )

            Spacer(modifier = Modifier.height(16.dp))

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
                                        
                                        val productStore = storeState.stores.find { it.id == product.storeId }
                                        val productRate = productStore?.usdExchangeRate ?: 13500.0
                                        val normalizedPrice = product.getPriceInUSD(productRate)
                                        val marketProduct = MarketProduct(
                                            id = product.id,
                                            name = product.title,
                                            price = normalizedPrice,
                                            originalPrice = normalizedPrice * 1.5,
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
                                            val productStore = storeState.stores.find { it.id == product.storeId }
                                            val productRate = productStore?.usdExchangeRate ?: 13500.0
                                            val formattedPrice = CurrencyManager.formatProductPrice(
                                                product,
                                                productRate,
                                                LanguageManager.isArabic(context)
                                            )
                                            Text(
                                                text = formattedPrice,
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

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StoresSection(
    stores: List<com.example.domain.model.Store>,
    isLoading: Boolean,
    error: String? = null,
    onStoreClick: (String) -> Unit,
    onViewAllClick: () -> Unit
) {
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                androidx.compose.ui.res.stringResource(R.string.top_stores),
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )
            val context = LocalContext.current
            TextButton(onClick = onViewAllClick) {
                Text(if (com.example.core.utils.LanguageManager.isArabic(context)) "جميع المتاجر" else "All Stores", color = BrandPrimary)
            }
        }
        
        if (isLoading) {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                items(4) { StoreStorySkeletonItem() }
            }
        } else if (error != null) {
            Text(text = error, color = BrandError, modifier = Modifier.padding(24.dp))
        } else if (stores.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                Text(androidx.compose.ui.res.stringResource(R.string.top_stores), color = BrandTextMuted)
            }
        } else {
            LazyRow(
                modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp)
            ) {
                items(stores) { store ->
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
            modifier = Modifier.size(72.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
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

            // High priority Badge Overlays on bottom-right of avatar
            if (store.sellerBadge == "Pro Seller") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .background(Color(0xFFFFB300), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Star,
                        contentDescription = "Pro Seller",
                        tint = Color.White,
                        modifier = Modifier.size(10.dp)
                    )
                }
            } else if (store.verificationStatus == "Verified" || store.isVerified || store.sellerBadge == "Verified Seller") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .background(Color(0xFF1DB954), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Verified Store",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            } else if (store.subscriptionTier == "Pro") {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .size(20.dp)
                        .background(Color(0xFF8A2BE2), CircleShape)
                        .border(1.5.dp, Color.White, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bolt,
                        contentDescription = "Pro Tier",
                        tint = Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
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
    onSelectCurrency: () -> Unit,
    onSignOut: () -> Unit,
    onAdminClick: () -> Unit,
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

        // 1.2 Currency Option
        Surface(
            onClick = {
                onDismiss()
                onSelectCurrency()
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
                        imageVector = Icons.Default.Paid,
                        contentDescription = null,
                        tint = BrandPrimary,
                        modifier = Modifier.size(24.dp)
                    )
                    Column {
                        val isAr = com.example.core.utils.LanguageManager.isArabic(context)
                        Text(
                            text = if (isAr) "العملة المفضلة" else "Preferred Currency",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary
                        )
                        val currFlow = CurrencyManager.currentCurrency.value
                        val descStr = when (currFlow) {
                            CurrencyManager.Currency.SYP -> if (isAr) "الليرة السورية (ل.س)" else "Syrian Pound (SYP)"
                            CurrencyManager.Currency.USD -> if (isAr) "دولار أمريكي (USD)" else "US Dollar (USD)"
                        }
                        Text(
                            text = descStr,
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

        if (user?.role == "admin") {
            Surface(
                onClick = {
                    onDismiss()
                    onAdminClick()
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
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.admin_control_panel),
                                fontSize = 14.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = androidx.compose.ui.res.stringResource(R.string.admin_dashboard_sub),
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

@Composable
fun ProductHorizontalSection(
    title: String,
    products: List<com.example.domain.model.Product>,
    isLoading: Boolean,
    error: String?,
    stores: List<com.example.domain.model.Store>,
    onProductClick: (String) -> Unit,
    context: android.content.Context
) {
    val isAr = com.example.core.utils.LanguageManager.isArabic(context)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            modifier = Modifier.padding(horizontal = 24.dp, vertical = 4.dp)
        )

        if (isLoading) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) {
                    ProductSkeletonCard()
                }
            }
        } else if (error != null) {
            Text(
                text = error,
                color = BrandError,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        } else if (products.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "لا توجد منتجات حالياً" else "No products found",
                    color = BrandTextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(products, key = { it.id }) { product ->
                    ProductRowCard(
                        product = product,
                        stores = stores,
                        onProductClick = onProductClick,
                        context = context
                    )
                }
            }
        }
    }
}

@Composable
fun ProductRowCard(
    product: com.example.domain.model.Product,
    stores: List<com.example.domain.model.Store>,
    onProductClick: (String) -> Unit,
    context: android.content.Context
) {
    val isAr = com.example.core.utils.LanguageManager.isArabic(context)
    val isDirectAd = product.storeId == "direct_ad"
    val matchingStore = if (isDirectAd) null else stores.find { it.id == product.storeId }
    val productStoreName = if (isDirectAd) {
        if (isAr) "إعلان مباشر" else "Direct Ad"
    } else {
        matchingStore?.name ?: (if (isAr) "متجر مرخّص" else "Licensed Store")
    }
    val productRate = matchingStore?.usdExchangeRate ?: 13500.0
    val formattedPrice = CurrencyManager.formatProductPrice(
        product,
        productRate,
        isAr
    )

    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
            .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
            .clickable { onProductClick(product.id) }
    ) {
        Box(modifier = Modifier.fillMaxWidth().height(110.dp)) {
            AsyncImage(
                model = product.imageUrls.firstOrNull() ?: "",
                contentDescription = product.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            if (isDirectAd) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color(0xFF2196F3)) // Vibrant blue for direct ad badge
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = if (isAr) "إعلان مباشر" else "Direct Ad",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            val normalizedPrice = product.getPriceInUSD(productRate)
            val marketProduct = MarketProduct(
                id = product.id,
                name = product.title,
                price = normalizedPrice,
                originalPrice = normalizedPrice * 1.5,
                rating = product.rating.toDouble(),
                reviewsCount = product.reviewCount,
                category = product.categoryId,
                storeName = productStoreName,
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

            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(6.dp)
            ) {
                ConditionBadge(condition = product.condition, isAr = isAr)
            }
        }
        Column(modifier = Modifier.padding(10.dp)) {
            Text(
                text = product.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
                modifier = Modifier.padding(top = 2.dp)
            ) {
                Text(
                    text = productStoreName,
                    fontSize = 11.sp,
                    color = BrandTextMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                if (matchingStore != null) {
                    if (matchingStore.sellerBadge == "Pro Seller") {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Pro Seller",
                            tint = Color(0xFFFFB300),
                            modifier = Modifier.size(11.dp)
                        )
                    } else if (matchingStore.verificationStatus == "Verified" || matchingStore.isVerified || matchingStore.sellerBadge == "Verified Seller") {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified Store",
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = formattedPrice,
                    fontSize = 13.sp,
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

@Composable
fun ProductSkeletonCard() {
    Column(
        modifier = Modifier
            .width(160.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
            .border(1.dp, BrandSoftGray, RoundedCornerShape(16.dp))
            .padding(bottom = 12.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(110.dp)
                .background(BrandSoftGray)
        )
        Spacer(modifier = Modifier.height(8.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .height(14.dp)
                .fillMaxWidth()
                .background(BrandSoftGray)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Box(
            modifier = Modifier
                .padding(horizontal = 12.dp)
                .height(10.dp)
                .width(80.dp)
                .background(BrandSoftGray)
        )
    }
}

@Composable
fun JobHorizontalSection(
    title: String,
    jobs: List<com.example.domain.model.Job>,
    isLoading: Boolean,
    error: String?,
    onJobClick: (String) -> Unit,
    onViewAllClick: () -> Unit,
    context: android.content.Context
) {
    val isAr = com.example.core.utils.LanguageManager.isArabic(context)
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )
            
            Text(
                text = if (isAr) "عرض جميع الوظائف" else "View All Jobs",
                fontSize = 13.sp,
                color = BrandPrimary,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.clickable { onViewAllClick() }
            )
        }

        if (isLoading) {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(3) {
                    Box(
                        modifier = Modifier
                            .width(260.dp)
                            .height(130.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(BrandSoftGray.copy(alpha = 0.5f))
                    )
                }
            }
        } else if (error != null) {
            Text(
                text = error,
                color = BrandError,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp)
            )
        } else if (jobs.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = if (isAr) "لا توجد وظائف شاغرة حالياً" else "No jobs found",
                    color = BrandTextMuted,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                contentPadding = PaddingValues(horizontal = 24.dp, vertical = 8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(jobs, key = { it.id }) { job ->
                    JobCardCompact(
                        job = job,
                        onClick = { onJobClick(job.id) }
                    )
                }
            }
        }
    }
}


