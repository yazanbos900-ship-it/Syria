package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.core.utils.LanguageManager
import com.example.core.utils.CurrencyManager
import com.example.R
import androidx.compose.foundation.BorderStroke
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.core.di.ServiceLocator
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.ui.theme.*
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreDetailScreen(
    storeId: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    onManageStore: () -> Unit
) {
    val viewModel: StoreDetailViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StoreDetailViewModel(
                storeRepo = ServiceLocator.storeRepository,
                productRepo = ServiceLocator.productRepository,
                authRepo = ServiceLocator.authRepository
            ) as T
        }
    })

    val state by viewModel.state.collectAsStateWithLifecycle()

    LaunchedEffect(storeId) {
        viewModel.loadStore(storeId)
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.error!!,
                            color = MaterialTheme.colorScheme.error,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(24.dp)
                        )
                    }
                }
                state.store != null -> {
                    StoreContent(
                        store = state.store!!,
                        products = state.products,
                        onBack = onBack,
                        onProductClick = onProductClick,
                        isOwner = state.store!!.ownerId == state.currentUserId,
                        onManageClick = onManageStore
                    )
                }
            }
        }
    }
}

@Composable
fun StoreContent(
    store: Store,
    products: List<Product>,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit,
    isOwner: Boolean,
    onManageClick: () -> Unit
) {
    val context = LocalContext.current
    // Get Category Info
    val category = SharedFilterState.categoriesList.find { it.id == store.categoryId }
    val isArabic = LanguageManager.isArabic(context)

    var activeJobsCount by remember(store.id) { mutableIntStateOf(0) }
    LaunchedEffect(store.id) {
        try {
            com.example.core.di.ServiceLocator.jobRepository.getJobsByStoreId(store.id).collect { jobs ->
                activeJobsCount = jobs.count { it.status == "active" }
            }
        } catch (e: Exception) {}
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1) Banner & Logo Layout
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
                        .clip(RoundedCornerShape(bottomStart = 24.dp, bottomEnd = 24.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                ) {
                    if (store.bannerUrl != null) {
                        AsyncImage(
                            model = store.bannerUrl,
                            contentDescription = "Store Banner",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier.fillMaxSize().background(
                                androidx.compose.ui.graphics.Brush.verticalGradient(
                                    colors = listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primaryContainer)
                                )
                            )
                        )
                    }

                    // Overlay to ensure back button readability
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.2f))
                    )

                    // Back Button
                    IconButton(
                        onClick = onBack,
                        modifier = Modifier
                            .padding(top = 16.dp, start = 12.dp)
                            .align(Alignment.TopStart)
                            .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                            .size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = stringResource(R.string.back_button),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Logo Overlay (Circular, overlapping banner)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                        .size(100.dp)
                        .offset(y = 50.dp),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.surface,
                    border = BorderStroke(4.dp, MaterialTheme.colorScheme.background)
                ) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize().clip(CircleShape)
                    )
                }
            }
            
            // Spacer to accommodate the overlapping logo
            Spacer(modifier = Modifier.height(60.dp))
        }

        // 2) Header Information Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.onBackground,
                        textAlign = TextAlign.Center
                    )
                    if (store.isVerified) {
                        Spacer(modifier = Modifier.width(6.dp))
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Verified",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(4.dp))
                
                Text(
                    text = "@${store.ownerUsername}",
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center
                )

                if (category != null) {
                    Spacer(modifier = Modifier.height(10.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = category.getName(isArabic),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (store.subscriptionTier != "free" && store.subscriptionTier != "Starter") {
                    Spacer(modifier = Modifier.height(12.dp))
                    StoreSubscriptionBadge(tier = store.subscriptionTier)
                } else {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
        }

        // 3) Stats Row
        item {
            @OptIn(ExperimentalLayoutApi::class)
            FlowRow(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.Center,
                verticalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 4
            ) {
                StoreStatChip(
                    icon = Icons.Default.Star,
                    value = String.format("%.1f", store.rating),
                    label = if (isArabic) "التقييم" else "Rating",
                    tint = Color(0xFFFFC107)
                )
                Spacer(modifier = Modifier.width(8.dp))
                StoreStatChip(
                    icon = Icons.Default.ShoppingCart,
                    value = products.size.toString(),
                    label = if (isArabic) "منتجات" else "Products",
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                StoreStatChip(
                    icon = Icons.Default.Person,
                    value = store.followersCount.toString(),
                    label = if (isArabic) "متابع" else "Followers",
                    tint = MaterialTheme.colorScheme.secondary
                )
                if (activeJobsCount > 0) {
                    Spacer(modifier = Modifier.width(8.dp))
                    StoreStatChip(
                        icon = Icons.Default.Work,
                        value = activeJobsCount.toString(),
                        label = if (isArabic) "وظائف" else "Jobs",
                        tint = MaterialTheme.colorScheme.tertiary
                    )
                }
            }
        }
        
        // 4) Action Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (isOwner) {
                    Button(
                        onClick = onManageClick,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "إدارة المتجر" else "Manage Store", fontWeight = FontWeight.Bold)
                    }
                } else {
                    Button(
                        onClick = { /* Follow Logic */ },
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "متابعة" else "Follow", fontWeight = FontWeight.Bold)
                    }
                    OutlinedButton(
                        onClick = { /* Contact Logic */ },
                        modifier = Modifier.weight(1f),
                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isArabic) "تواصل" else "Contact", color = MaterialTheme.colorScheme.onSurface)
                    }
                }
                
                IconButton(
                    onClick = { /* Share Logic */ },
                    modifier = Modifier.background(MaterialTheme.colorScheme.surfaceVariant, CircleShape)
                ) {
                    Icon(Icons.Default.Share, contentDescription = "Share", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            
            if (!isOwner && activeJobsCount > 0) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 4.dp)
                ) {
                    Button(
                        onClick = { /* View Jobs Logic */ },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.tertiaryContainer, contentColor = MaterialTheme.colorScheme.onTertiaryContainer),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Work, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "عرض الوظائف الشاغرة ($activeJobsCount)" else "View Active Jobs ($activeJobsCount)", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 5) Description
        item {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.about_store_label),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = store.description.takeIf { it.isNotBlank() } ?: androidx.compose.ui.res.stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    lineHeight = 22.sp
                )
            }
        }

        // Divider
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp), color = MaterialTheme.colorScheme.surfaceVariant)
        }

        // 6) Products Section Title
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.products_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp)
            )
        }

        if (products.isEmpty()) {
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 60.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Surface(
                        modifier = Modifier.size(80.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.surface
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.no_products_yet),
                        color = MaterialTheme.colorScheme.onBackground,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.follow_for_updates),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 48.dp, vertical = 4.dp)
                    )
                }
            }
        } else {
            // 7) Products Grid
            val chunkedProducts = products.chunked(2)
            items(chunkedProducts) { rowProducts ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    for (product in rowProducts) {
                        Box(modifier = Modifier.weight(1f)) {
                            ProductCard(
                                product = product,
                                usdExchangeRate = store.usdExchangeRate,
                                onClick = { onProductClick(product.id) }
                            )
                        }
                    }
                    if (rowProducts.size == 1) {
                        Spacer(modifier = Modifier.weight(1f))
                    }
                }
            }
        }
    }
}

@Composable
fun StoreStatChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String,
    tint: Color
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = tint,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(6.dp))
            Column {
                Text(
                    text = value,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Text(
                    text = label,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontSize = 10.sp
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    usdExchangeRate: Double,
    onClick: () -> Unit
) {
    val context = LocalContext.current
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color.Black.copy(alpha = 0.05f))
            ) {
                AsyncImage(
                    model = product.imageUrls.firstOrNull(),
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(8.dp)
                ) {
                    ConditionBadge(condition = product.condition)
                }
            }
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = CurrencyManager.formatPrice(product.price, usdExchangeRate, LanguageManager.isArabic(context)),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
