package com.example.features.marketplace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.components.*
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.core.utils.CurrencyManager
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Unified Spacing, Typography & Color Tokens
private val DarkBg: Color get() = BrandBackground
private val DarkCard: Color get() = BrandSurface
private val PrimaryGreen: Color get() = BrandPrimary
private val TextWhite: Color get() = BrandTextPrimary
private val TextGray: Color get() = BrandTextMuted
private val BorderColor: Color get() = BrandSoftGray

enum class SellerTab(
    val titleAr: String,
    val titleEn: String,
    val icon: ImageVector
) {
    DASHBOARD("لوحة التحكم", "Dashboard", Icons.Default.Dashboard),
    ORDERS("الطلبات", "Orders", Icons.Default.ReceiptLong),
    PRODUCTS("منتجاتي", "My Products", Icons.Default.Inventory2),
    SUBSCRIPTIONS("الاشتراكات", "Subscriptions", Icons.Default.CardMembership),
    SETTINGS("الإعدادات", "Settings", Icons.Default.Settings)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(
    onBack: () -> Unit,
    onEditProduct: (Product) -> Unit = {},
    onNavigateToManageJobs: () -> Unit = {}
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)

    val viewModel: StoreManagementViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StoreManagementViewModel(
                ServiceLocator.storeRepository,
                ServiceLocator.productRepository,
                ServiceLocator.authRepository
            ) as T
        }
    })

    val state by viewModel.state.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(SellerTab.DASHBOARD) }

    // Dialog triggering states
    var showAddEditProductDialog by remember { mutableStateOf(false) }
    var productToEdit by remember { mutableStateOf<Product?>(null) }

    LaunchedEffect(Unit) {
        viewModel.loadStoreAndProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isArabic) "مركز إدارة البائع" else "Seller Hub Center", 
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        color = TextWhite
                    ) 
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("seller_hub_back")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack, 
                            contentDescription = "Back",
                            tint = TextWhite
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToManageJobs) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.Work,
                            contentDescription = "Manage Jobs",
                            tint = TextGray
                        )
                    }
                    // Quick Action Link Switching to Settings Tab
                    IconButton(onClick = { selectedTab = SellerTab.SETTINGS }) {
                        Icon(
                            imageVector = Icons.Default.Settings, 
                            contentDescription = "Quick Settings",
                            tint = if (selectedTab == SellerTab.SETTINGS) PrimaryGreen else TextGray
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = DarkCard)
            )
        },
        containerColor = DarkBg
    ) { innerPadding ->
        if (state.isLoading && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else if (state.error != null && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding).padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Icon(Icons.Default.ErrorOutline, null, tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(64.dp))
                    Text(state.error!!, color = TextWhite, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
                    Button(
                        onClick = { viewModel.loadStoreAndProducts() },
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                    ) {
                        Text(if (isArabic) "إعادة المحاولة" else "Retry")
                    }
                }
            }
        } else {
            val store = state.store ?: return@Scaffold
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // High-Fidelity Custom Scrollable / Adaptive TabBar Row
                TabRow(
                    selectedTabIndex = selectedTab.ordinal,
                    containerColor = DarkCard,
                    contentColor = PrimaryGreen,
                    indicator = { tabPositions ->
                        TabRowDefaults.SecondaryIndicator(
                            Modifier.tabIndicatorOffset(tabPositions[selectedTab.ordinal]),
                            color = PrimaryGreen
                        )
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    SellerTab.values().forEach { tab ->
                        val isSelected = selectedTab == tab
                        Tab(
                            selected = isSelected,
                            onClick = { selectedTab = tab },
                            text = { 
                                Text(
                                    text = if (isArabic) tab.titleAr else tab.titleEn,
                                    fontWeight = if (isSelected) FontWeight.ExtraBold else FontWeight.Medium,
                                    fontSize = 12.sp
                                ) 
                            },
                            icon = { 
                                Icon(
                                    imageVector = tab.icon, 
                                    contentDescription = null,
                                    modifier = Modifier.size(18.dp)
                                ) 
                            },
                            selectedContentColor = PrimaryGreen,
                            unselectedContentColor = TextGray
                        )
                    }
                }

                // Selected Tab Body Layout Switcher
                Box(modifier = Modifier.weight(1f)) {
                    when (selectedTab) {
                        SellerTab.DASHBOARD -> {
                            DashboardSection(
                                store = store,
                                productsCount = state.products.size,
                                isArabic = isArabic,
                                purchaseIntents = state.purchaseIntents,
                                onNavigateToProducts = { selectedTab = SellerTab.PRODUCTS },
                                onNavigateToPlans = { selectedTab = SellerTab.SUBSCRIPTIONS },
                                onNavigateToOrders = { selectedTab = SellerTab.ORDERS }
                            )
                        }
                        SellerTab.ORDERS -> {
                            SellerOrdersSection(
                                isArabic = isArabic,
                                storeId = store.id
                            )
                        }
                        SellerTab.PRODUCTS -> {
                            ProductsSection(
                                products = state.products,
                                rate = store.exchangeRate,
                                isArabic = isArabic,
                                onAddProduct = {
                                    productToEdit = null
                                    showAddEditProductDialog = true
                                },
                                onEditProduct = { product ->
                                    productToEdit = product
                                    showAddEditProductDialog = true
                                },
                                onDeleteProduct = { productId ->
                                    viewModel.deleteProduct(productId)
                                }
                            )
                        }
                        SellerTab.SUBSCRIPTIONS -> {
                            SubscriptionsSection(
                                store = store,
                                isArabic = isArabic,
                                onRequestPlan = { tier ->
                                    viewModel.requestSubscription(tier)
                                }
                            )
                        }
                        SellerTab.SETTINGS -> {
                            SettingsSection(
                                store = store,
                                isArabic = isArabic,
                                onSave = { name, desc, rate, catId, logo, banner, currency ->
                                    viewModel.updateStore(name, desc, rate, catId, logo, banner, currency)
                                },
                                isLoading = state.isLoading
                            )
                        }
                    }
                }
            }
        }
    }

    // Single unified form dialog for BOTH Add and Edit product operations.
    if (showAddEditProductDialog && state.store != null) {
        val store = state.store!!
        AddEditProductDialog(
            product = productToEdit,
            usdExchangeRate = store.exchangeRate,
            defaultStoreCurrency = store.storeCurrency,
            isArabic = isArabic,
            onDismiss = { showAddEditProductDialog = false },
            onSave = { title, price, desc, categoryId, imageUris, currency, cond ->
                if (productToEdit == null) {
                    // Adding
                    viewModel.addProduct(title, price, desc, imageUris, categoryId, currency, cond)
                } else {
                    // Editing
                    val updated = productToEdit!!.copy(
                        title = title,
                        price = price,
                        description = desc,
                        categoryId = categoryId,
                        imageUrls = imageUris,
                        currency = currency,
                        condition = cond
                    )
                    viewModel.updateProduct(updated)
                }
                showAddEditProductDialog = false
            }
        )
    }
}

// ==========================================
// 1. DASHBOARD SUB-SECTION IMPLEMENTATION
// ==========================================
@Composable
fun DashboardSection(
    store: Store,
    productsCount: Int,
    isArabic: Boolean,
    purchaseIntents: List<com.example.domain.model.PurchaseIntent>,
    onNavigateToProducts: () -> Unit,
    onNavigateToPlans: () -> Unit,
    onNavigateToOrders: () -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Core Visual Cover Header Card
        item {
            StoreDashboardHeaderCard(store = store, isArabic = isArabic)
        }

        // Tier and Level Status Pills Banner
        item {
            StoreProgressStatusBanner(store = store, isArabic = isArabic, onNavigateToPlans = onNavigateToPlans)
        }

        // Purchase Requests / Click Analytics Statistics Card
        item {
            PurchaseRequestsStatsCard(purchaseIntents = purchaseIntents, isArabic = isArabic)
        }

        // Analytical Metrics Grid
        item {
            Text(
                text = if (isArabic) "المؤشرات الإحصائية العامة" else "Key Performance Indicators",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = TextWhite,
                modifier = Modifier.padding(top = 4.dp, bottom = 4.dp)
            )
        }

        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricWidgetCard(
                        title = if (isArabic) "إجمالي المنتجات" else "Active Listings",
                        value = productsCount.toString(),
                        subtitle = if (isArabic) "سلعة معروضة" else "items published",
                        icon = Icons.Default.Inventory2,
                        iconTint = PrimaryGreen,
                        modifier = Modifier.weight(1f),
                        onClick = onNavigateToProducts
                    )
                    MetricWidgetCard(
                        title = if (isArabic) "المتابعون للمتجر" else "Store Followers",
                        value = store.followersCount.toString(),
                        subtitle = if (isArabic) "متفاعل حقيقي" else "followers tracking",
                        icon = Icons.Default.People,
                        iconTint = Color(0xFF00CED1),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricWidgetCard(
                        title = if (isArabic) "العملة الافتراضية" else "Active Currency",
                        value = store.storeCurrency,
                        subtitle = if (store.storeCurrency == "USD") "American Dollar ($)" else "Syrian Pound (ل.س)",
                        icon = Icons.Default.MonetizationOn,
                        iconTint = Color(0xFFFFD700),
                        modifier = Modifier.weight(1f)
                    )
                    MetricWidgetCard(
                        title = if (isArabic) "سعر صرف متجرك" else "USD Conversion Rate",
                        value = "${store.exchangeRate.toInt()} ل.س",
                        subtitle = if (isArabic) "لكل 1 دولار أمريكي" else "per 1 USD",
                        icon = Icons.Default.SwapHoriz,
                        iconTint = Color(0xFFFF69B4),
                        modifier = Modifier.weight(1f)
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
                    MetricWidgetCard(
                        title = if (isArabic) "إدارة وفاء الطلبات" else "Fulfillment Orders",
                        value = "→",
                        subtitle = if (isArabic) "إدارة الطلبات والشحن والمبيعات" else "manage invoices & shipments",
                        icon = Icons.Default.ReceiptLong,
                        iconTint = Color(0xFFFFB300),
                        modifier = Modifier.fillMaxWidth(),
                        onClick = onNavigateToOrders
                    )
                }
            }
        }

        // Custom drawn curve line chart representing store impressions
        item {
            DashboardAnalyticsChart(isArabic = isArabic)
        }
    }
}

@Composable
fun PurchaseRequestsStatsCard(
    purchaseIntents: List<com.example.domain.model.PurchaseIntent>,
    isArabic: Boolean
) {
    val now = System.currentTimeMillis()
    val dayMillis = 24 * 60 * 60 * 1000L
    val weekMillis = 7 * dayMillis
    val monthMillis = 30 * dayMillis

    val dayRequests = purchaseIntents.count { it.timestamp != null && (now - it.timestamp.toDate().time) <= dayMillis }
    val weekRequests = purchaseIntents.count { it.timestamp != null && (now - it.timestamp.toDate().time) <= weekMillis }
    val monthRequests = purchaseIntents.count { it.timestamp != null && (now - it.timestamp.toDate().time) <= monthMillis }

    val topProducts = purchaseIntents.groupBy { it.productId }
        .mapValues { entry ->
            val title = entry.value.firstOrNull()?.productTitle ?: (if (isArabic) "منتج غير معروف" else "Unknown Product")
            title to entry.value.size
        }
        .values
        .sortedByDescending { it.second }
        .take(5)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            // Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ShowChart,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isArabic) "إحصائيات نقرات الشراء" else "Purchase Request Insights",
                    color = TextWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            // Total Click Indicator
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BorderColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text(
                        text = if (isArabic) "إجمالي طلبات الشراء" else "Total Purchase Requests",
                        color = TextGray,
                        fontSize = 12.sp
                    )
                    Text(
                        text = "${purchaseIntents.size}",
                        color = PrimaryGreen,
                        fontSize = 28.sp,
                        fontWeight = FontWeight.Black
                    )
                }
                Icon(
                    imageVector = Icons.Default.ShoppingCart,
                    contentDescription = null,
                    tint = PrimaryGreen.copy(alpha = 0.8f),
                    modifier = Modifier.size(40.dp)
                )
            }

            // Day / Week / Month Breakdown Row
            Text(
                text = if (isArabic) "معدل الطلب عبر الوقت" else "Request Frequency Over Time",
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Day Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(BorderColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (isArabic) "اليوم" else "Today", color = TextGray, fontSize = 11.sp)
                    Text(text = "$dayRequests", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                // Week Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(BorderColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (isArabic) "هذا الأسبوع" else "This Week", color = TextGray, fontSize = 11.sp)
                    Text(text = "$weekRequests", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
                // Month Box
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .background(BorderColor.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                        .padding(10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(text = if (isArabic) "هذا الشهر" else "This Month", color = TextGray, fontSize = 11.sp)
                    Text(text = "$monthRequests", color = TextWhite, fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }

            // Most Requested Products List
            if (topProducts.isNotEmpty()) {
                Text(
                    text = if (isArabic) "المنتجات الأكثر طلباً" else "Most Requested Products",
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    topProducts.forEachIndexed { index, pair ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(BorderColor.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(24.dp)
                                        .background(PrimaryGreen.copy(alpha = 0.15f), CircleShape),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = "${index + 1}",
                                        color = PrimaryGreen,
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Text(
                                    text = pair.first,
                                    color = TextWhite,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = if (isArabic) "${pair.second} طلبات" else "${pair.second} requests",
                                color = PrimaryGreen,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = if (isArabic) "لا توجد طلبات شراء مسجلة بعد." else "No purchase requests recorded yet.",
                    color = TextGray,
                    fontSize = 11.sp,
                    style = androidx.compose.ui.text.TextStyle(fontStyle = androidx.compose.ui.text.font.FontStyle.Italic)
                )
            }
        }
    }
}

@Composable
fun StoreDashboardHeaderCard(store: Store, isArabic: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(16.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            // Visual Banner Background Frame
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .background(
                        Brush.linearGradient(
                            colors = listOf(PrimaryGreen.copy(alpha = 0.5f), Color(0xFF1A3D2A))
                        )
                    )
            ) {
                store.bannerUrl?.let { banner ->
                    AsyncImage(
                        model = banner,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            // Details section wrapping Logo & Name
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Overlapping Floating Circular Store Logo
                Box(
                    modifier = Modifier
                        .offset(y = (-30).dp)
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(DarkCard)
                        .border(3.dp, DarkCard, CircleShape)
                        .border(1.dp, BorderColor, CircleShape)
                ) {
                    if (store.logoUrl != null) {
                        AsyncImage(
                            model = store.logoUrl,
                            contentDescription = "Store Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.Storefront, null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                        }
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                // Text Metadata Details
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .offset(y = (-10).dp)
                ) {
                    Text(
                        text = store.name,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 18.sp,
                        color = TextWhite,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = store.description.ifBlank { if (isArabic) "لا يوجد وصف محدد للمتجر بعد." else "No profile description provided yet." },
                        fontSize = 11.sp,
                        color = TextGray,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 14.sp
                    )
                }
            }
        }
    }
}

@Composable
fun StoreProgressStatusBanner(
    store: Store,
    isArabic: Boolean,
    onNavigateToPlans: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                // Subscription Pillar Badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val badgeColor = when (store.subscriptionTier) {
                        "Pro" -> Color(0xFFDAA520) // Pro gold
                        "Growth" -> Color(0xFF00CED1) // Growth cyan
                        else -> Color(0xFF808080) // Starter grey
                    }
                    Box(
                        modifier = Modifier
                            .clip(CircleShape)
                            .background(badgeColor)
                            .size(8.dp)
                    )
                    Text(
                        text = String.format(
                            "%s: %s",
                            if (isArabic) "باقة المتجر الحالية" else "Active Subscription",
                            store.subscriptionTier
                        ),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = TextWhite
                    )
                }

                // Verification Pillar Badge
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val (veriText, veriColor) = when (store.verificationStatus) {
                        "Verified" -> (if (isArabic) "تاجر موثق ومعتمد" else "Verified Identity Merchant") to Color(0xFF2E7D32)
                        "Rejected" -> (if (isArabic) "مرفوض - يرجى التحقق" else "Identity Verification Rejected") to Color(0xFFD32F2F)
                        else -> (if (isArabic) "قيد التحقق الأمني" else "Verification Approval Pending") to Color(0xFFEF6C00)
                    }
                    Icon(
                        imageVector = if (store.verificationStatus == "Verified") Icons.Default.CheckCircle else Icons.Default.Info,
                        contentDescription = null,
                        tint = veriColor,
                        modifier = Modifier.size(13.dp)
                    )
                    Text(
                        text = veriText,
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }
            }

            // Quick Plan Upgrader Link
            TextButton(
                onClick = onNavigateToPlans,
                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
            ) {
                Text(
                    text = if (isArabic) "ترقية الباقة" else "Upgrade Plan",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    color = PrimaryGreen
                )
                Icon(
                    imageVector = Icons.Default.ArrowForward,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(14.dp).padding(start = 2.dp)
                )
            }
        }
    }
}

@Composable
fun MetricWidgetCard(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    iconTint: Color,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier.clickable(enabled = onClick != null) { onClick?.invoke() }
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(text = title, fontSize = 11.sp, color = TextGray, fontWeight = FontWeight.Bold)
                Icon(imageVector = icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            }
            Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.ExtraBold, color = TextWhite)
            Text(text = subtitle, fontSize = 10.sp, color = TextGray)
        }
    }
}

@Composable
fun DashboardAnalyticsChart(isArabic: Boolean) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = if (isArabic) "مؤشرات المشاهدة والتفاعل" else "Store Impressions & Analytics",
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                color = TextWhite
            )
            Text(
                text = if (isArabic) "تقرير تقديري للظهور الأسبوعي لمنتجاتك" else "Aggregated visitor trends over standard weekly metrics",
                fontSize = 10.sp,
                color = TextGray
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            // Beautiful curved line chart using canvas
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val width = size.width
                    val height = size.height
                    
                    // Horizontal background grid separators
                    for (i in 1..3) {
                        val y = height * (i / 4f)
                        drawLine(
                            color = BorderColor.copy(alpha = 0.25f),
                            start = Offset(0f, y),
                            end = Offset(width, y),
                            strokeWidth = 1.dp.toPx()
                        )
                    }

                    // Simulated daily performance data ratios
                    val points = listOf(0.18f, 0.45f, 0.32f, 0.72f, 0.54f, 0.88f, 0.65f)
                    val stepX = width / (points.size - 1)
                    val path = Path()
                    
                    points.forEachIndexed { i, ratio ->
                        val x = i * stepX
                        val y = height - (ratio * height * 0.8f) // buffer safety
                        if (i == 0) {
                            path.moveTo(x, y)
                        } else {
                            val prevX = (i - 1) * stepX
                            val prevY = height - (points[i - 1] * height * 0.8f)
                            path.cubicTo((prevX + x) / 2f, prevY, (prevX + x) / 2f, y, x, y)
                        }
                    }

                    // Stroke Path render
                    drawPath(
                        path = path,
                        color = PrimaryGreen,
                        style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                    )

                    // Glow gradient brush filling area down to X axis
                    val fillPath = Path().apply {
                        addPath(path)
                        lineTo(width, height)
                        lineTo(0f, height)
                        close()
                    }
                    drawPath(
                        path = fillPath,
                        brush = Brush.verticalGradient(
                            colors = listOf(PrimaryGreen.copy(alpha = 0.18f), Color.Transparent)
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // X-Axis Day labels row
            val daysHeading = if (isArabic) {
                listOf("السبت", "الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة")
            } else {
                listOf("Sat", "Sun", "Mon", "Tue", "Wed", "Thu", "Fri")
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                daysHeading.forEach { dayName ->
                    Text(
                        text = dayName,
                        fontSize = 9.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.width(36.dp)
                    )
                }
            }
        }
    }
}


// ==========================================
// 2. PRODUCTS LIST SUB-SECTION IMPLEMENTATION
// ==========================================
@Composable
fun ProductsSection(
    products: List<Product>,
    rate: Double,
    isArabic: Boolean,
    onAddProduct: () -> Unit,
    onEditProduct: (Product) -> Unit,
    onDeleteProduct: (String) -> Unit
) {
    Box(modifier = Modifier.fillMaxSize()) {
        if (products.isEmpty()) {
            // High quality visual empty state
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(80.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.08f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Inventory2,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(36.dp)
                    )
                }
                Spacer(modifier = Modifier.height(20.dp))
                Text(
                    text = if (isArabic) "لا توجد منتجات معروضة حالياً" else "Zero products listed",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (isArabic) "لم تقم بنشر أي سلع في معرض متجرك بعد. أضف أول منتج لك الآن وابدأ في استقبال العروض." else "Your store front is currently empty. List items to let market customers discover them.",
                    fontSize = 13.sp,
                    color = TextGray,
                    textAlign = TextAlign.Center,
                    lineHeight = 18.sp
                )
                Spacer(modifier = Modifier.height(24.dp))
                Button(
                    onClick = onAddProduct,
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                    modifier = Modifier.testTag("empty_state_add_product")
                ) {
                    Icon(Icons.Default.Add, null)
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(if (isArabic) "إضافة منتج جديد" else "Publish First Product", fontWeight = FontWeight.Bold)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = String.format(
                                if (isArabic) "معرض منتجاتي (%d سلعة)" else "My Catalog (%d items)",
                                products.size
                            ),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextWhite
                        )

                        // Compact inline Add btn as tab toolbar
                        IconButton(
                            onClick = onAddProduct,
                            modifier = Modifier
                                .clip(CircleShape)
                                .background(PrimaryGreen),
                            colors = IconButtonDefaults.iconButtonColors(contentColor = Color.White)
                        ) {
                            Icon(Icons.Default.Add, null, modifier = Modifier.size(18.dp))
                        }
                    }
                }

                items(products) { product ->
                    ProductManagementItem(
                        product = product,
                        usdExchangeRate = rate,
                        isArabic = isArabic,
                        onEdit = { onEditProduct(product) },
                        onDelete = { onDeleteProduct(product.id) }
                    )
                }
            }
        }
    }
}


// ==========================================
// 3. SUBSCRIPTIONS PLANS SUB-SECTION
// ==========================================
@Composable
fun SubscriptionsSection(
    store: Store,
    isArabic: Boolean,
    onRequestPlan: (String) -> Unit
) {
    // Beautiful, fully descriptive, inline subscription plans section
    val plansList = listOf(
        SubscriptionPlan(
            id = "Starter",
            nameEn = "Starter Setup",
            nameAr = "الباقة المبتدئة للبدء",
            descEn = "Essential local retail presence setup",
            descAr = "إثبات وجود محلي مميز ومبسط لمتجرك البدائي",
            featuresEn = listOf("Up to 15 active store items listings", "USD and SYP listing conversions", "Standard discovery weight index"),
            featuresAr = listOf("حد أقصى يعادل 15 منتجاً نشطاً", "تحويل وتدوير الأسعار تلقائياً", "معدلات الظهور وخوارزميات البحث العادية"),
            primaryColor = Color(0xFF9E9E9E),
            bgSoftColor = Color(0xFF9E9E9E).copy(alpha = 0.05f)
        ),
        SubscriptionPlan(
            id = "Growth",
            nameEn = "Growth Suite",
            nameAr = "باقة النمو السحابي",
            descEn = "Higher priority search visibility & verifications",
            descAr = "منصة متطورة تدعم أولويات ترويجية قصوى لمتجرك",
            featuresEn = listOf("Unlimited listing capabilities", "Dedicated 'Growth' badge displayed on cards", "Direct priority seller verification audit"),
            featuresAr = listOf("عدد غير محدود لإدراجات المنتجات", "شارة 'نمو' مميزة تظهر للمشترين", "أولوية التحقق الأمني والحصول على الشارة التاجرة"),
            primaryColor = Color(0xFF00CED1),
            bgSoftColor = Color(0xFF00CED1).copy(alpha = 0.05f)
        ),
        SubscriptionPlan(
            id = "Pro",
            nameEn = "Elite PRO Suite",
            nameAr = "الباقة الإحترافية الكاملة",
            descEn = "Premium placement opportunities and full seller rewards",
            descAr = "حزمة تسويقية متكاملة تدفع بالمبيعات للصدارة",
            featuresEn = listOf("Elite Pro verification tag visibility", "Top homepage carousels promo placement", "Double ranking discoverability weighting factor"),
            featuresAr = listOf("شارة 'المحترف النخبة' الذهبية المتميزة", "فرص صدارة العروض بالصفحة الرئيسية", "مضاعفة ظهور منتجاتك للمشترين تلقائياً"),
            primaryColor = Color(0xFFDAA520),
            bgSoftColor = Color(0xFFDAA520).copy(alpha = 0.05f)
        )
    )

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = PrimaryGreen.copy(alpha = 0.08f)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.25f)),
                modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp)
            ) {
                Row(modifier = Modifier.padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.CardMembership, null, tint = PrimaryGreen, modifier = Modifier.size(28.dp))
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = if (isArabic) "حدد العضوية المثالية لنمو عملك" else "Maximize Sales Success",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            color = TextWhite
                        )
                        Text(
                            text = if (isArabic) "اختر خطتك بمرونة وسهولة لتمكين متجرك من الصعود للقمة." else "Seamlessly requests plan upgrades directly into the database hierarchy.",
                            fontSize = 11.sp,
                            color = TextGray
                        )
                    }
                }
            }
        }

        items(plansList) { plan ->
            val isActive = store.subscriptionTier == plan.id
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(
                    1.5.dp, 
                    if (isActive) plan.primaryColor else BorderColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    // Header
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) plan.nameAr else plan.nameEn,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = plan.primaryColor
                            )
                            Text(
                                text = if (isArabic) plan.descAr else plan.descEn,
                                fontSize = 11.sp,
                                color = TextGray
                            )
                        }
                        if (isActive) {
                            Surface(
                                color = plan.primaryColor.copy(alpha = 0.15f),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "النشطة حالياً" else "ACTIVE PLAN",
                                    color = plan.primaryColor,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 10.sp,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    // Features list
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        val features = if (isArabic) plan.featuresAr else plan.featuresEn
                        features.forEach { feature ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.Top,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = null,
                                    tint = plan.primaryColor,
                                    modifier = Modifier.size(14.dp).padding(top = 2.dp)
                                )
                                Text(
                                    text = feature,
                                    fontSize = 11.sp,
                                    color = TextWhite,
                                    lineHeight = 14.sp
                                )
                            }
                        }
                    }

                    // Request action Button
                    if (!isActive) {
                        Button(
                            onClick = { onRequestPlan(plan.id) },
                            shape = RoundedCornerShape(10.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = plan.primaryColor),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(38.dp)
                        ) {
                            Text(
                                text = if (isArabic) "طلب تفعيل هذه الباقة" else "Request Plan Upgrade",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = DarkBg
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 4. SETTINGS SUB-SECTION IMPLEMENTATION
// ==========================================
@Composable
fun SettingsSection(
    store: Store,
    isArabic: Boolean,
    onSave: (name: String, desc: String, rate: Double, catId: String, logo: String?, banner: String?, currency: String) -> Unit,
    isLoading: Boolean
) {
    var name by remember { mutableStateOf(store.name) }
    var desc by remember { mutableStateOf(store.description) }
    var exchangeRateStr by remember { mutableStateOf(store.exchangeRate.toInt().toString()) }
    var defaultCurrency by remember { mutableStateOf(store.storeCurrency) }
    var logoUrl by remember { mutableStateOf(store.logoUrl) }
    var bannerUrl by remember { mutableStateOf(store.bannerUrl) }

    var isUploadingLogo by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }

    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == store.categoryId } ?: categories.firstOrNull()) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploadingLogo = true
                val uploader = CloudinaryUploader()
                uploader.uploadFile(it.toString()).onSuccess { url ->
                    logoUrl = url
                }
                isUploadingLogo = false
            }
        }
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploadingBanner = true
                val uploader = CloudinaryUploader()
                uploader.uploadFile(it.toString()).onSuccess { url ->
                    bannerUrl = url
                }
                isUploadingBanner = false
            }
        }
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Banner and logo configuration block
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "إعدادات الهوية البصرية" else "Visual Brand Identity",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(160.dp)
                    ) {
                        // Banner Frame Changer
                        Card(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(120.dp)
                                .clickable { if (!isUploadingBanner) bannerPicker.launch("image/*") },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, BorderColor)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                AsyncImage(
                                    model = bannerUrl ?: store.bannerUrl,
                                    contentDescription = "Banner",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (isUploadingBanner) {
                                        CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(24.dp))
                                    } else {
                                        Icon(Icons.Default.AddAPhoto, null, tint = Color.White)
                                    }
                                }
                            }
                        }

                        // Logo Changer Overlay
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .size(72.dp)
                                .clip(CircleShape)
                                .background(DarkCard)
                                .border(3.dp, DarkCard, CircleShape)
                                .border(1.dp, BorderColor, CircleShape)
                                .clickable { if (!isUploadingLogo) logoPicker.launch("image/*") },
                            contentAlignment = Alignment.Center
                        ) {
                            AsyncImage(
                                model = logoUrl ?: store.logoUrl,
                                contentDescription = "Logo",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.35f)),
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingLogo) {
                                    CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.AddAPhoto, null, tint = Color.White, modifier = Modifier.size(16.dp))
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isArabic) "انقر فوق المساحة لتغيير الغلاف أو شعار المتجر" else "Tap cards to customize store visuals",
                        fontSize = 11.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // New Currency & Exchange Rate Section Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (isArabic) "إعدادات العملة والسعر" else "Currency & Exchange Settings",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )

                    // Currency Selector (Toggle Chips style)
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "العملة الافتراضية" else "Store Currency",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            listOf("USD" to (if (isArabic) "USD $" else "USD $"), "SYP" to (if (isArabic) "ليرة سورية" else "Syrian Pound")).forEach { (currCode, currLabel) ->
                                val isSelected = defaultCurrency == currCode
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else DarkBg)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) PrimaryGreen else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { defaultCurrency = currCode }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = currLabel,
                                        color = if (isSelected) PrimaryGreen else TextGray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Exchange Rate input with validation
                    var rateErrorStr by remember { mutableStateOf<String?>(null) }
                    
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "سعر الصرف" else "Exchange Rate",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        OutlinedTextField(
                            value = exchangeRateStr,
                            onValueChange = { newValue ->
                                exchangeRateStr = newValue
                                val dValue = newValue.toDoubleOrNull()
                                rateErrorStr = when {
                                    newValue.isEmpty() || dValue == null || dValue == 0.0 -> if (isArabic) "أدخل سعر صرف صحيح" else "Enter a valid exchange rate"
                                    dValue < 1000.0 -> if (isArabic) "السعر منخفض جداً" else "Rate is too low"
                                    dValue > 100000.0 -> if (isArabic) "السعر مرتفع جداً" else "Rate is too high"
                                    else -> null
                                }
                            },
                            placeholder = { Text("1 USD = ؟ ل.س", color = TextGray, fontSize = 13.sp) },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = if (rateErrorStr != null) Color.Red else PrimaryGreen,
                                unfocusedBorderColor = if (rateErrorStr != null) Color.Red else BorderColor,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth().testTag("store_exchange_rate_input"),
                            isError = rateErrorStr != null
                        )
                        if (rateErrorStr != null) {
                            Text(
                                text = rateErrorStr!!,
                                color = Color.Red,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }

                    // Save Button specifically for Currency and Rate Settings: "حفظ إعدادات العملة"
                    Button(
                        onClick = {
                            val dValue = exchangeRateStr.toDoubleOrNull()
                            val validationError = when {
                                exchangeRateStr.isEmpty() || dValue == null || dValue == 0.0 -> if (isArabic) "أدخل سعر صرف صحيح" else "Enter a valid exchange rate"
                                dValue < 1000.0 -> if (isArabic) "السعر منخفض جداً" else "Rate is too low"
                                dValue > 100000.0 -> if (isArabic) "السعر مرتفع جداً" else "Rate is too high"
                                else -> null
                            }
                            rateErrorStr = validationError
                            if (validationError == null && dValue != null) {
                                onSave(name, desc, dValue, selectedCategory?.id ?: store.categoryId, logoUrl, bannerUrl, defaultCurrency)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = !isUploadingLogo && !isUploadingBanner && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_currency_settings_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (isArabic) "حفظ إعدادات العملة" else "Save Currency Settings",
                                color = DarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }

                    Text(
                        text = if (isArabic) "سيُستخدم هذا السعر تلقائياً عند إضافة المنتجات" else "This rate will be automatically used when adding products",
                        fontSize = 11.sp,
                        color = TextGray,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }

        // Standard Store Details form fields
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = DarkCard),
                shape = RoundedCornerShape(16.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text(
                        text = if (isArabic) "الخيارات والبيانات الأساسية" else "Key General Fields",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = TextWhite
                    )

                    // Store Name
                    StoreInputField(
                        label = if (isArabic) "اسم المتجر" else "Store Name",
                        value = name,
                        onValueChange = { name = it },
                        placeholder = if (isArabic) "اسم المتجر" else "Store Name",
                        testTag = "edit_store_name_field"
                    )

                    // Default Store Currency Selection Block
                    Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text(
                            text = if (isArabic) "العملة الافتراضية للمتجر" else "Default Store Currency",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            for (currency in listOf("USD", "SYP")) {
                                val isSelected = defaultCurrency == currency
                                val displayName = if (currency == "USD") "USD ($)" else "SYP (ل.س)"
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else DarkBg)
                                        .border(
                                            1.5.dp,
                                            if (isSelected) PrimaryGreen else BorderColor,
                                            RoundedCornerShape(12.dp)
                                        )
                                        .clickable { defaultCurrency = currency }
                                        .padding(vertical = 12.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = displayName,
                                        color = if (isSelected) PrimaryGreen else TextGray,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                    )
                                }
                            }
                        }
                    }

                    // Exchange Rate
                    StoreInputField(
                        label = if (isArabic) "سعر صرف متجرك مقابل USD" else "Store Exchange Rate (SYP per 1 USD)",
                        value = exchangeRateStr,
                        onValueChange = { exchangeRateStr = it },
                        placeholder = "13500",
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        testTag = "edit_store_rate_field",
                        helperText = if (isArabic) "مثال: 13500" else "e.g., 13500"
                    )

                    // Store Description Multiline Form Field
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isArabic) "حول متجري" else "Store Bio / Description",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        OutlinedTextField(
                            value = desc,
                            onValueChange = { desc = it },
                            placeholder = { Text(if (isArabic) "أدخل نبذة عن تخصص متجرك المعروض..." else "Enter your store specialized identity details...", color = TextGray, fontSize = 13.sp) },
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedTextColor = TextWhite,
                                unfocusedTextColor = TextWhite,
                                focusedBorderColor = PrimaryGreen,
                                unfocusedBorderColor = BorderColor,
                                focusedContainerColor = DarkBg,
                                unfocusedContainerColor = DarkBg
                            ),
                            shape = RoundedCornerShape(12.dp),
                            minLines = 3,
                            maxLines = 5,
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    // Dropdown Category Selector
                    Column(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = if (isArabic) "تصنيف نشاط المتجر الرئيسي" else "Primary Store Specialty",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(DarkBg)
                                .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                                .clickable { showCategoryDialog = true }
                                .padding(horizontal = 16.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = selectedCategory?.getName(isArabic) ?: (if (isArabic) "اختر التصنيف المناسب" else "Select Business Type"),
                                    color = TextWhite,
                                    fontSize = 14.sp,
                                    maxLines = 1,
                                    overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Icon(Icons.Default.ArrowDropDown, null, tint = PrimaryGreen)
                            }
                        }
                    }

                    HorizontalDivider(color = BorderColor.copy(alpha = 0.5f))

                    // Solid Save Trigger Button
                    Button(
                        onClick = {
                            if (name.isNotBlank() && desc.isNotBlank() && selectedCategory != null) {
                                val rate = exchangeRateStr.toDoubleOrNull() ?: 13500.0
                                onSave(name, desc, rate, selectedCategory!!.id, logoUrl, bannerUrl, defaultCurrency)
                            }
                        },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = name.isNotBlank() && desc.isNotBlank() && !isUploadingLogo && !isUploadingBanner && !isLoading,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp)
                            .testTag("save_store_settings_button")
                    ) {
                        if (isLoading) {
                            CircularProgressIndicator(color = DarkBg, modifier = Modifier.size(20.dp))
                        } else {
                            Text(
                                text = if (isArabic) "حفظ التغييرات بالكامل" else "Save Workspace Updates",
                                color = DarkBg,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }
            }
        }
    }

    // Category Selector Dialog
    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isArabic) "اختر تصنيف العمل المناسب" else "Select Main Niche Category",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            SellerCategorySelectionRow(
                                name = category.getName(isArabic),
                                isSelected = selectedCategory?.id == category.id,
                                onClick = {
                                    selectedCategory = category
                                    showCategoryDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}


// ==========================================
// 5. SHARED REUSABLE LAYOUT WIDGETS
// ==========================================
@Composable
fun ProductManagementItem(
    product: Product,
    usdExchangeRate: Double,
    isArabic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(12.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(64.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(BorderColor),
                contentAlignment = Alignment.Center
            ) {
                AsyncImage(
                    model = product.imageUrls.firstOrNull(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            
            Spacer(modifier = Modifier.width(12.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.title, 
                    fontWeight = FontWeight.Bold, 
                    color = TextWhite, 
                    fontSize = 14.sp,
                    maxLines = 1, 
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = CurrencyManager.formatProductPrice(product, usdExchangeRate, isArabic),
                    color = PrimaryGreen,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
                Text(
                    text = String.format(
                        if (isArabic) "المخزون: %d وحدات" else "Stock: %d units", 
                        product.stockCount
                    ), 
                    fontSize = 11.sp, 
                    color = TextGray
                )
            }
            
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = TextWhite.copy(alpha = 0.7f), modifier = Modifier.size(20.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.8f), modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun SellerCategorySelectionRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryGreen.copy(alpha = 0.12f) else Color.Transparent)
            .border(1.dp, if (isSelected) PrimaryGreen else BorderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(12.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = if (isSelected) PrimaryGreen else TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

// Dialog that handles BOTH Product addition & Product edits beautifully!
@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AddEditProductDialog(
    product: Product? = null,
    usdExchangeRate: Double,
    defaultStoreCurrency: String,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onSave: (title: String, price: Double, desc: String, categoryId: String, imageUris: List<String>, currency: String, condition: String) -> Unit
) {
    var title by remember { mutableStateOf(product?.title ?: "") }
    var price by remember { mutableStateOf(product?.price?.let { if (it > 0.0) it.toString() else "" } ?: "") }
    var desc by remember { mutableStateOf(product?.description ?: "") }
    var selectedCurrency by remember { mutableStateOf(product?.currency ?: defaultStoreCurrency) }
    var imageUris by remember { mutableStateOf<List<String>>(product?.imageUrls ?: emptyList()) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var condition by remember { mutableStateOf(product?.condition ?: "new") }

    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == product?.categoryId } ?: categories.firstOrNull()) }
    var showCategorySelectorDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && imageUris.size < 10) {
            scope.launch {
                isUploadingImage = true
                val uploader = CloudinaryUploader()
                uploader.uploadFile(uri.toString()).onSuccess { url ->
                    imageUris = imageUris + url
                }
                isUploadingImage = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Headline Title
                Text(
                    text = if (product == null) {
                        if (isArabic) "إضافة بطاقة منتج جديد" else "Publish New Product"
                    } else {
                        if (isArabic) "تعديل بيانات المنتج" else "Modify Listing Details"
                    },
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Product Title Campo
                StoreInputField(
                    label = if (isArabic) "مسمى المنتج" else "Product Title",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = if (isArabic) "اسم السلعة" else "Item name",
                    testTag = "add_product_title_field"
                )

                // Currency Toggle Selector Block (Single source of truth)
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        text = if (isArabic) "عملة تسعير المنتج" else "Product Pricing Currency",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        for (currency in listOf("USD", "SYP")) {
                            val isSelected = selectedCurrency == currency
                            val displayName = if (currency == "USD") "USD ($)" else "SYP (ل.س)"
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else DarkBg)
                                    .border(
                                        1.5.dp,
                                        if (isSelected) PrimaryGreen else BorderColor,
                                        RoundedCornerShape(12.dp)
                                    )
                                    .clickable { selectedCurrency = currency }
                                    .padding(vertical = 12.dp),
                                contentAlignment = Alignment.Center
                              ) {
                                Text(
                                    text = displayName,
                                    color = if (isSelected) PrimaryGreen else TextGray,
                                    fontSize = 14.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                                )
                            }
                        }
                    }
                }

                // Price Field
                StoreInputField(
                    label = if (isArabic) "سعر البيع المطلق" else "Sale Price Valuation",
                    value = price,
                    onValueChange = { price = it },
                    placeholder = "0.00",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    testTag = "add_product_price_field"
                )

                // Real-time conversion preview calculation wrapper
                val parsedDoublePrice = price.toDoubleOrNull()
                if (parsedDoublePrice != null && parsedDoublePrice > 0.0) {
                    val rate = if (usdExchangeRate <= 0.0) 12500.0 else usdExchangeRate
                    val equivalentAmount = if (defaultStoreCurrency == "USD") parsedDoublePrice * rate else parsedDoublePrice / rate
                    val formattedEquivalent = if (defaultStoreCurrency == "USD") {
                        "≈ ${String.format("%,d", equivalentAmount.toLong())} ل.س"
                    } else {
                        "≈ $ ${String.format("%.2f", equivalentAmount)}"
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(8.dp))
                            .background(PrimaryGreen.copy(alpha = 0.06f))
                            .padding(10.dp)
                    ) {
                        Text(
                            text = formattedEquivalent,
                            fontSize = 13.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Description
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "تفاصيل ومواصفات المنتج" else "Item Profile Details",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text(if (isArabic) "أدخل مواصفات منتجك هنا..." else "Describe your product attributes...", color = TextGray, fontSize = 13.sp) },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            focusedBorderColor = PrimaryGreen,
                            unfocusedBorderColor = BorderColor,
                            focusedContainerColor = DarkBg,
                            unfocusedContainerColor = DarkBg
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 5,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Categories Specialty Dropdown Box
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "تصنيف المنتج" else "Product Category",
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(DarkBg)
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .clickable { showCategorySelectorDialog = true }
                            .padding(horizontal = 16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = selectedCategory?.getName(isArabic) ?: (if (isArabic) "تحميل الفئات..." else "Loading Categories..."),
                                color = TextWhite,
                                fontSize = 14.sp,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(Icons.Default.ArrowDropDown, null, tint = PrimaryGreen)
                        }
                    }
                }

                // Condition selection
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = stringResource(id = R.string.product_condition_label),
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val conditions = listOf(
                            "new" to R.string.condition_new,
                            "open_box" to R.string.condition_open_box,
                            "like_new" to R.string.condition_like_new,
                            "excellent" to R.string.condition_excellent,
                            "good" to R.string.condition_good,
                            "fair" to R.string.condition_fair,
                            "used" to R.string.condition_used,
                            "for_parts" to R.string.condition_for_parts
                        )
                        conditions.forEach { (key, strId) ->
                            val isSelected = condition == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(if (isSelected) PrimaryGreen else DarkBg)
                                    .border(1.dp, if (isSelected) PrimaryGreen else BorderColor, RoundedCornerShape(10.dp))
                                    .clickable { condition = key }
                                    .padding(horizontal = 14.dp, vertical = 10.dp)
                                    .testTag("dialog_condition_chip_$key")
                            ) {
                                Text(
                                    text = stringResource(id = strId),
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color.White else TextGray
                                )
                            }
                        }
                    }
                }

                // Ribbon Custom Adaptive Multi-Image Uploader Grid
                Column(modifier = Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "معرض صور المنتج" else "Item Gallery Images",
                            color = TextWhite,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${imageUris.size}/10",
                            fontSize = 11.sp,
                            color = PrimaryGreen,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    LazyRow(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(DarkBg, RoundedCornerShape(12.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                            .padding(8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Multi Image Selection Plus Block
                        item {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(PrimaryGreen.copy(alpha = 0.05f))
                                    .border(
                                        BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.4f)),
                                        RoundedCornerShape(10.dp)
                                    )
                                    .clickable(enabled = !isUploadingImage && imageUris.size < 10) {
                                        photoPickerLauncher.launch("image/*")
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(20.dp))
                                } else {
                                    Icon(Icons.Default.AddAPhoto, null, tint = PrimaryGreen, modifier = Modifier.size(24.dp))
                                }
                            }
                        }

                        // Listed item slides with removal cross buttons
                        items(imageUris) { url ->
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .border(BorderStroke(1.dp, BorderColor), RoundedCornerShape(10.dp))
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    modifier = Modifier.fillMaxSize(),
                                    contentScale = ContentScale.Crop
                                )
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(18.dp)
                                        .background(Color.Black.copy(alpha = 0.61f), CircleShape)
                                        .clickable { imageUris = imageUris.filter { it != url } },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Remove",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Confirmation / Dismiss Actions block
                Row(
                    modifier = Modifier.fillMaxWidth().padding(top = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text(if (isArabic) "إلغاء الأمر" else "Cancel", color = TextGray)
                    }

                    val priceValNum = price.toDoubleOrNull() ?: 0.0
                    val isFormValid = title.isNotBlank() && priceValNum > 0.0 && selectedCategory != null && imageUris.isNotEmpty() && !isUploadingImage

                    Button(
                        onClick = {
                            if (isFormValid) {
                                onSave(title, priceValNum, desc, selectedCategory!!.id, imageUris, selectedCurrency, condition)
                            }
                        },
                        modifier = Modifier.weight(1.5f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = isFormValid
                    ) {
                        Text(
                            text = if (product == null) {
                                if (isArabic) "إضافة المنتج" else "Publish Product"
                            } else {
                                if (isArabic) "حفظ التعديلات" else "Save Changes"
                            },
                            color = DarkBg,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }

    // Category Selector
    if (showCategorySelectorDialog) {
        Dialog(onDismissRequest = { showCategorySelectorDialog = false }) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = if (isArabic) "اختر تصنيف المنتج المناسب" else "Select Product Category",
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 280.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            SellerCategorySelectionRow(
                                name = category.getName(isArabic),
                                isSelected = selectedCategory?.id == category.id,
                                onClick = {
                                    selectedCategory = category
                                    showCategorySelectorDialog = false
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
