package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
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
import com.example.ui.theme.BrandBackground
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSoftGray
import com.example.ui.theme.BrandTextMuted
import com.example.ui.theme.BrandTextPrimary

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
        containerColor = BrandBackground,
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize().padding(innerPadding)) {
            when {
                state.isLoading -> {
                    CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
                }
                state.error != null -> {
                    Text(
                        text = state.error!!,
                        color = BrandTextMuted,
                        modifier = Modifier.align(Alignment.Center)
                    )
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
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        // 1) Banner item
        item {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .background(BrandSoftGray)
            ) {
                AsyncImage(
                    model = store.bannerUrl,
                    contentDescription = "Store Banner",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )

                // Back Button overlaid
                IconButton(
                    onClick = onBack,
                    modifier = Modifier
                        .padding(top = 16.dp, start = 16.dp, end = 16.dp)
                        .align(Alignment.TopStart)
                        .background(Color.Black.copy(alpha = 0.5f), CircleShape)
                ) {
                    Icon(imageVector = Icons.Default.ArrowBack, contentDescription = "رجوع", tint = Color.White)
                }
            }
        }

        // 2) Header Row
        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .clip(CircleShape)
                        .background(BrandSoftGray)
                ) {
                    AsyncImage(
                        model = store.logoUrl,
                        contentDescription = store.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = store.name,
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = store.ownerUsername,
                        style = MaterialTheme.typography.bodyMedium,
                        color = BrandTextMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                if (isOwner) {
                    Button(
                        onClick = onManageClick,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("إدارة المتجر", color = Color.White, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // 3) Follow / Followers
        item {
            Text(
                text = "متابعين: ${store.followersCount}",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextMuted,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4) Description
        item {
            Text(
                text = store.description.takeIf { it.isNotBlank() } ?: "لا يوجد وصف للمتجر",
                style = MaterialTheme.typography.bodyMedium,
                color = BrandTextPrimary,
                maxLines = 4,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        // 5) Products Title
        item {
            Text(
                text = "المنتجات",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp)
            )
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 6) Products Grid (2-column chunked)
        val chunkedProducts = products.chunked(2)
        items(chunkedProducts) { rowProducts ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp),
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
                // Fill if row has only 1 product
                if (rowProducts.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
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
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .background(BrandSoftGray)
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
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$${product.price}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }
        }
    }
}
