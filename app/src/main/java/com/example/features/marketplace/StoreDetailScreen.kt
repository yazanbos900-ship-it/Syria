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
import androidx.compose.material.icons.filled.ShoppingBasket
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.platform.LocalContext
import com.example.core.utils.LanguageManager
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

// Premium Dynamic Theme Palette
private val DarkBg: Color get() = BrandBackground
private val DarkCard: Color get() = BrandSurface
private val PrimaryGreen: Color get() = BrandPrimary
private val TextWhite: Color get() = BrandTextPrimary
private val TextGray: Color get() = BrandTextMuted
private val BorderColor: Color get() = BrandSoftGray

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
        containerColor = DarkBg,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = PrimaryGreen)
                    }
                }
                state.error != null -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.error!!,
                            color = TextGray,
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

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 32.dp)
    ) {
        // 1) Banner & Logo Layout
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(260.dp)
            ) {
                // Banner
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(200.dp)
                        .background(DarkCard)
                ) {
                    AsyncImage(
                        model = store.bannerUrl,
                        contentDescription = "Store Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )

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
                            contentDescription = androidx.compose.ui.res.stringResource(R.string.back_button),
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Logo Overlay (Circular, bottom-center)
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(bottom = 0.dp)
                        .size(90.dp)
                        .offset(y = (-10).dp),
                    shape = CircleShape,
                    color = DarkCard,
                    border = BorderStroke(3.dp, PrimaryGreen)
                ) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // 2) Header Information Section
        item {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = store.name,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = "@${store.ownerUsername}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    textAlign = TextAlign.Center
                )

                if (category != null) {
                    Spacer(modifier = Modifier.height(12.dp))
                    Surface(
                        color = PrimaryGreen.copy(alpha = 0.1f),
                        shape = RoundedCornerShape(20.dp),
                        border = BorderStroke(1.dp, PrimaryGreen.copy(alpha = 0.5f))
                    ) {
                        Text(
                            text = category.getName(LanguageManager.isArabic(context)),
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                            color = PrimaryGreen,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        // 3) Stats Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${products.size}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = androidx.compose.ui.res.stringResource(R.string.products_label), color = TextGray, fontSize = 12.sp)
                }
                
                VerticalDivider(
                    modifier = Modifier
                        .padding(horizontal = 32.dp)
                        .height(24.dp),
                    color = BorderColor
                )

                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(text = "${store.followersCount}", color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    Text(text = androidx.compose.ui.res.stringResource(R.string.follower_label), color = TextGray, fontSize = 12.sp)
                }
            }
        }
        
        // 4) Main Action Button
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isOwner) {
                    Button(
                        onClick = onManageClick,
                        modifier = Modifier.fillMaxWidth(0.9f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.manage_store_label), color = DarkBg, fontWeight = FontWeight.ExtraBold)
                    }
                } else {
                    OutlinedButton(
                        onClick = { /* Follow Logic */ },
                        modifier = Modifier.fillMaxWidth(0.9f),
                        border = BorderStroke(1.5.dp, PrimaryGreen),
                        shape = RoundedCornerShape(14.dp)
                    ) {
                        Icon(Icons.Default.Add, null, tint = PrimaryGreen, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(androidx.compose.ui.res.stringResource(R.string.follow_label), color = PrimaryGreen, fontWeight = FontWeight.ExtraBold)
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
                    color = TextWhite
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = store.description.takeIf { it.isNotBlank() } ?: androidx.compose.ui.res.stringResource(R.string.no_description),
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextGray,
                    lineHeight = 22.sp
                )
            }
        }

        // Divider
        item {
            HorizontalDivider(modifier = Modifier.padding(vertical = 16.dp, horizontal = 24.dp), color = BorderColor)
        }

        // 6) Products Section Title
        item {
            Text(
                text = androidx.compose.ui.res.stringResource(R.string.products_title),
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.ExtraBold,
                color = TextWhite,
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
                        color = DarkCard
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = null,
                                tint = PrimaryGreen.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.no_products_yet),
                        color = TextWhite,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.follow_for_updates),
                        color = TextGray,
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProductCard(
    product: Product,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1.1f)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                AsyncImage(
                    model = product.imageUrls.firstOrNull(),
                    contentDescription = product.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Column(
                modifier = Modifier.padding(12.dp)
            ) {
                Text(
                    text = product.title,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        text = "$${product.price}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black,
                        color = PrimaryGreen
                    )
                }
            }
        }
    }
}
