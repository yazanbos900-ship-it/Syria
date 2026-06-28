package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.domain.model.Store
import com.example.ui.theme.*
import com.example.core.utils.LanguageManager

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AllStoresScreen(
    onNavigateBack: () -> Unit,
    onStoreSelected: (String) -> Unit,
    viewModel: HomeStoresViewModel = viewModel(
        factory = object : androidx.lifecycle.ViewModelProvider.Factory {
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                return HomeStoresViewModel(com.example.core.di.ServiceLocator.storeRepository) as T
            }
        }
    )
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val state by viewModel.state.collectAsState()
    
    var showFilterSheet by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    val stores = state.stores
    val filteredStores = remember(stores, SharedFilterState.selectedCategoryFilter, SharedFilterState.verifiedStoresOnly, SharedFilterState.selectedStoreSubscriptionFilter, SharedFilterState.selectedSortOption, searchQuery) {
        val filtered = stores.filter { store ->
            val matchesCategory = SharedFilterState.selectedCategoryFilter == "All" || 
                                  SharedFilterState.selectedCategoryFilter.isBlank() ||
                                  store.categoryId == SharedFilterState.selectedCategoryFilter
            val matchesSearch = searchQuery.isBlank() || 
                                store.name.contains(searchQuery, ignoreCase = true)
            val matchesVerification = !SharedFilterState.verifiedStoresOnly || store.isVerified || store.verificationStatus == "Verified"
            val matchesSubscription = SharedFilterState.selectedStoreSubscriptionFilter == "All" || store.subscriptionTier == SharedFilterState.selectedStoreSubscriptionFilter
            matchesCategory && matchesSearch && matchesVerification && matchesSubscription
        }
        
        android.util.Log.d("AllStoresScreen", "Total stores in state: ${stores.size}")
        android.util.Log.d("AllStoresScreen", "Total stores after filtering: ${filtered.size}")
        android.util.Log.d("AllStoresScreen", "Active category filter: ${SharedFilterState.selectedCategoryFilter}")
        android.util.Log.d("AllStoresScreen", "Active search query: $searchQuery")
        android.util.Log.d("AllStoresScreen", "Verified only filter: ${SharedFilterState.verifiedStoresOnly}")
        android.util.Log.d("AllStoresScreen", "Subscription filter: ${SharedFilterState.selectedStoreSubscriptionFilter}")
        
        when (SharedFilterState.selectedSortOption) {
            SortOption.Newest -> filtered.sortedByDescending { it.createdAt }
            SortOption.BestRated -> filtered.sortedByDescending { state.reputationScores[it.id] ?: 0.0 }
            else -> filtered.sortedByDescending { state.reputationScores[it.id] ?: 0.0 }
        }
    }

    if (showFilterSheet) {
        ModalBottomSheet(
            onDismissRequest = { showFilterSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = BrandSurface
        ) {
            FilterBottomSheetContent(
                onApply = { showFilterSheet = false }
            )
        }
    }

    Scaffold(
        topBar = {
            Column(modifier = Modifier.background(BrandSurface)) {
                TopAppBar(
                    title = { Text(if (isArabic) "جميع المتاجر" else "All Stores", fontWeight = FontWeight.Bold) },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        IconButton(onClick = { showFilterSheet = true }) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(Icons.Default.List, contentDescription = "Filters", tint = BrandPrimary)
                                if (SharedFilterState.isActive) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.TopEnd)
                                            .offset(x = 2.dp, y = (-2).dp)
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(BrandPrimary)
                                    )
                                }
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = BrandSurface,
                        titleContentColor = BrandTextPrimary,
                        navigationIconContentColor = BrandTextPrimary
                    )
                )
                
                // Search Bar
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    placeholder = { Text(if (isArabic) "ابحث عن متجر..." else "Search stores...", color = BrandSoftGray) },
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null, tint = BrandSoftGray)
                    },
                    trailingIcon = {
                        if (searchQuery.isNotEmpty()) {
                            IconButton(onClick = { searchQuery = "" }) {
                                Icon(Icons.Default.Clear, contentDescription = "Clear", tint = BrandSoftGray)
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = BrandSurface,
                        unfocusedContainerColor = BrandBackground,
                        focusedBorderColor = BrandPrimary,
                        unfocusedBorderColor = Color.LightGray,
                        focusedTextColor = BrandTextPrimary,
                        unfocusedTextColor = BrandTextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
            }
        },
        containerColor = BrandBackground
    ) { padding ->
        if (state.isLoadingTopStores && stores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (filteredStores.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(64.dp), tint = BrandSoftGray)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isArabic) "لا توجد متاجر تطابق بحثك" else "No stores match your criteria",
                        color = BrandTextMuted,
                        fontSize = 16.sp
                    )
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(horizontal = 16.dp),
                contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredStores, key = { it.id }) { store ->
                    val score = state.reputationScores[store.id]
                    StoreCard(store = store, reputationScore = score, onClick = { onStoreSelected(store.id) })
                }
            }
        }
    }
}

@Composable
fun StoreCard(store: Store, reputationScore: Double? = null, onClick: () -> Unit) {
    val context = LocalContext.current
    val category = SharedFilterState.categoriesList.find { it.id == store.categoryId }
    val isArabic = LanguageManager.isArabic(context)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Banner & Logo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(120.dp)
            ) {
                if (store.bannerUrl != null) {
                    AsyncImage(
                        model = store.bannerUrl,
                        contentDescription = "Store Banner",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(100.dp)
                            .background(
                                color = MaterialTheme.colorScheme.primaryContainer
                            )
                    )
                }

                // Logo Overlay
                Surface(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(start = 16.dp)
                        .size(64.dp)
                        .offset(y = 8.dp),
                    shape = CircleShape,
                    color = BrandSurface,
                    border = androidx.compose.foundation.BorderStroke(3.dp, BrandSurface)
                ) {
                    if (store.logoUrl != null) {
                        AsyncImage(
                            model = store.logoUrl,
                            contentDescription = "Store Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .clip(CircleShape)
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(BrandSoftGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = store.name.take(1).uppercase(),
                                color = BrandTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 24.sp
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Details
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Top
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = store.name,
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary,
                                maxLines = 1,
                                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
                            )
                            if (store.isVerified || store.verificationStatus == "Verified") {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    imageVector = Icons.Default.CheckCircle,
                                    contentDescription = "Verified",
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                        
                        if (category != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = category.getName(isArabic),
                                fontSize = 12.sp,
                                color = BrandTextMuted
                            )
                        }
                    }

                    if (store.subscriptionTier != "free" && store.subscriptionTier != "Starter") {
                        com.example.features.marketplace.StoreSubscriptionBadge(tier = store.subscriptionTier)
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = "Rating",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = String.format("%.1f", store.rating),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Person,
                            contentDescription = "Followers",
                            tint = BrandTextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "${store.followersCount}",
                            fontSize = 13.sp,
                            color = BrandTextMuted
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = "Reputation",
                            tint = BrandPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        val displayScore = reputationScore ?: com.example.domain.utils.StoreReputationCalculator.calculateReputationScore(store)
                        Text(
                            text = if (isArabic) "السمعة: ${displayScore.toInt()}" else "Reputation: ${displayScore.toInt()}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }
        }
    }
}
