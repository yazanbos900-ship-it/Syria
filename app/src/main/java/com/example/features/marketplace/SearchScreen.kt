package com.example.features.marketplace

import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.SearchOff
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.res.stringResource
import com.example.R
import coil.compose.AsyncImage
import com.example.ui.theme.*

// Catalog Data Definition
data class MarketProduct(
    val id: String,
    val name: String,
    val price: Double,
    val originalPrice: Double?,
    val rating: Double,
    val reviewsCount: Int,
    val category: String,
    val storeName: String,
    val deliveryTime: String, // "Same Day", "Within 3 Days", "Standard (1 Week)"
    val dateAdded: String, // YYYY-MM-DD
    val imageUrl: String
)

enum class SortOption(val label: String) {
    Newest("Newest Arrivals"),
    PriceLowToHigh("Price: Low to High"),
    PriceHighToLow("Price: High to Low"),
    BestRated("Best Rated (★)")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchScreen(
    onNavigateBack: () -> Unit,
    onProductSelected: (String) -> Unit
) {
    val focusManager = LocalFocusManager.current

    // Curated real-time Search & Filter Source Catalog
    val productCatalog = remember {
        listOf(
            MarketProduct(
                id = "apple_watch_ultra_2",
                name = "Precision Active Chrono",
                price = 189.0,
                originalPrice = 378.0,
                rating = 4.9,
                reviewsCount = 124,
                category = "Electronics",
                storeName = "Bespoke Horology Lab",
                deliveryTime = "Same Day",
                dateAdded = "2026-05-20",
                imageUrl = "https://images.unsplash.com/photo-1434494878577-86c23bcb06b9?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "woolen_trench_coat",
                name = "Tailored Overcoat",
                price = 249.0,
                originalPrice = 498.0,
                rating = 4.8,
                reviewsCount = 89,
                category = "Apparel",
                storeName = "Atelier & Co",
                deliveryTime = "Within 3 Days",
                dateAdded = "2026-05-18",
                imageUrl = "https://images.unsplash.com/photo-1591047139829-d91aecb6caea?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "nordic_dining_chair",
                name = "Nordic Oak Dining Chair",
                price = 120.0,
                originalPrice = 180.0,
                rating = 4.6,
                reviewsCount = 42,
                category = "Furniture",
                storeName = "Minimalist Living",
                deliveryTime = "Within 3 Days",
                dateAdded = "2026-05-22",
                imageUrl = "https://images.unsplash.com/photo-1506439773649-6e0eb8cfb237?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "clay_teapot_set",
                name = "Artisanal Clay Teapot Set",
                price = 45.0,
                originalPrice = 60.0,
                rating = 4.7,
                reviewsCount = 65,
                category = "Artisanal",
                storeName = "Kyoto Earthwares",
                deliveryTime = "Same Day",
                dateAdded = "2026-05-15",
                imageUrl = "https://images.unsplash.com/photo-1576092768241-dec231879fc3?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "essential_oils_kit",
                name = "Organic Essential Oils Kit",
                price = 35.0,
                originalPrice = 50.0,
                rating = 4.5,
                reviewsCount = 110,
                category = "Wellness",
                storeName = "Botanica Organics",
                deliveryTime = "Same Day",
                dateAdded = "2026-05-24",
                imageUrl = "https://images.unsplash.com/photo-1608571423902-eed4a5ad8108?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "cashmere_scarf",
                name = "Luxe Cashmere Scarf",
                price = 85.0,
                originalPrice = 120.0,
                rating = 4.9,
                reviewsCount = 37,
                category = "Apparel",
                storeName = "Atelier & Co London",
                deliveryTime = "Same Day",
                dateAdded = "2026-05-25",
                imageUrl = "https://images.unsplash.com/photo-1520635360276-79f3dbd809f6?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "terrazzo_lamp",
                name = "Terrazzo Stone Lamp",
                price = 195.0,
                originalPrice = null,
                rating = 4.4,
                reviewsCount = 18,
                category = "Furniture",
                storeName = "Minimalist Living",
                deliveryTime = "Within 3 Days",
                dateAdded = "2026-05-10",
                imageUrl = "https://images.unsplash.com/photo-1507473885765-e6ed057f782c?auto=format&fit=crop&w=400&q=80"
            ),
            MarketProduct(
                id = "chrono_leather_strap",
                name = "Chrono Leather Loop Strap",
                price = 35.0,
                originalPrice = null,
                rating = 4.7,
                reviewsCount = 29,
                category = "Electronics",
                storeName = "Bespoke Horology Lab",
                deliveryTime = "Same Day",
                dateAdded = "2026-05-21",
                imageUrl = "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=400&q=80"
            )
        )
    }

    // Search & Filter States
    var queryText by remember { mutableStateOf("") }
    var filtersVisible by remember { mutableStateOf(false) }
    
    // Filter Critera States
    var selectedCategoryFilter by SharedFilterState.selectedCategoryFilterState
    var maxPriceRange by SharedFilterState.maxPriceRangeState
    var minRatingFilter by SharedFilterState.minRatingFilterState
    var deliveryFilterSameDayOnly by SharedFilterState.deliveryFilterSameDayOnlyState
    var selectedSortOption by SharedFilterState.selectedSortOptionState

    // Categories defined for multi-vendor search
    val filterCategories = listOf("All", "Apparel", "Artisanal", "Bespoke", "Furniture", "Wellness")
    val ratingOptions = listOf(0.0, 4.0, 4.5, 4.8)

    // Filter computation logic (instant, reactive and high performance updates)
    val filteredProducts = remember(queryText, selectedCategoryFilter, maxPriceRange, minRatingFilter, deliveryFilterSameDayOnly, selectedSortOption) {
        productCatalog.filter { product ->
            val matchesQuery = product.name.contains(queryText, ignoreCase = true) || 
                               product.category.contains(queryText, ignoreCase = true) ||
                               product.storeName.contains(queryText, ignoreCase = true)
            
            val matchesCategory = selectedCategoryFilter == "All" || product.category == selectedCategoryFilter
            val matchesPrice = product.price <= maxPriceRange
            val matchesRating = product.rating >= minRatingFilter
            val matchesDelivery = !deliveryFilterSameDayOnly || product.deliveryTime == "Same Day"

            matchesQuery && matchesCategory && matchesPrice && matchesRating && matchesDelivery
        }.sortedWith { a, b ->
            when (selectedSortOption) {
                SortOption.Newest -> b.dateAdded.compareTo(a.dateAdded)
                SortOption.PriceLowToHigh -> a.price.compareTo(b.price)
                SortOption.PriceHighToLow -> b.price.compareTo(a.price)
                SortOption.BestRated -> b.rating.compareTo(a.rating)
            }
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            // Highly polished and modern sticky Search & Sort Panel
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(BrandSurface)
                    .shadow(1.dp)
            ) {
                // Main sticky Row containing Search Input Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 24.dp, top = 16.dp, bottom = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(CircleShape)
                            .background(BrandBackground)
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Navigate Back",
                            tint = BrandTextPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(12.dp))

                    // Premium sticky input bar
                    OutlinedTextField(
                        value = queryText,
                        onValueChange = { queryText = it },
                        placeholder = { Text(stringResource(id = R.string.search_placeholder), fontSize = 14.sp, color = BrandTextMuted) },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Search,
                                contentDescription = null,
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            if (queryText.isNotEmpty()) {
                                IconButton(
                                    onClick = { queryText = "" },
                                    modifier = Modifier.size(36.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Clear,
                                        contentDescription = "Clear search query",
                                        tint = BrandTextPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = { focusManager.clearFocus() }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandBackground,
                            focusedContainerColor = BrandBackground,
                            unfocusedContainerColor = BrandBackground
                        ),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier
                            .weight(1f)
                            .height(52.dp)
                            .testTag("search_text_input")
                    )
                }

                // Horizontal Filters & Sorting actions Row (keeps filters highly accessible and within thumbs reach)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 24.dp, end = 24.dp, bottom = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Filter Toggle Button with state decoration
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(if (filtersVisible) BrandPrimary.copy(alpha = 0.15f) else BrandBackground)
                            .clickable { filtersVisible = !filtersVisible }
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .testTag("filter_button"),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.List,
                            contentDescription = "Toggle filters panel",
                            tint = if (filtersVisible) BrandPrimary else BrandTextPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.filters_label),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (filtersVisible) BrandPrimary else BrandTextPrimary
                        )
                        // Small indicator badged count when criteria is active
                        val activeCriteriaCount = (if (selectedCategoryFilter != "All") 1 else 0) +
                                (if (maxPriceRange < 300f) 1 else 0) +
                                (if (minRatingFilter > 0.0) 1 else 0) +
                                (if (deliveryFilterSameDayOnly) 1 else 0)

                        if (activeCriteriaCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(CircleShape)
                                    .background(BrandPrimary)
                                    .size(16.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = "$activeCriteriaCount",
                                    fontSize = 9.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    // Sort dropdown layout
                    Row(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandBackground)
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        var sortExpanded by remember { mutableStateOf(false) }

                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .clickable { sortExpanded = true }
                                .testTag("sort_dropdown")
                        ) {
                            Text(
                                text = selectedSortOption.label,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTextPrimary
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand Sort Selection",
                                tint = BrandTextPrimary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        DropdownMenu(
                            expanded = sortExpanded,
                            onDismissRequest = { sortExpanded = false },
                            modifier = Modifier.background(BrandSurface)
                        ) {
                            SortOption.values().forEach { sortOpt ->
                                DropdownMenuItem(
                                    text = { Text(sortOpt.label, fontSize = 13.sp, color = BrandTextPrimary) },
                                    onClick = {
                                        selectedSortOption = sortOpt
                                        sortExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }

                // Expandable Responsive Inline Filters Panel with custom smooth transitions
                AnimatedVisibility(
                    visible = filtersVisible,
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandSurface)
                            .border(BorderStroke(1.dp, BrandBackground))
                            .padding(horizontal = 24.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "REFINE MULTI-VENDOR DISCOVERY",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextMuted,
                            letterSpacing = 1.sp,
                            modifier = Modifier.padding(bottom = 12.dp)
                        )

                        // 1. Horizontal Category Selector row in filter drawer
                        Text(
                            text = stringResource(id = R.string.filter_by_category),
                            fontSize = 13.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = BrandTextPrimary,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .padding(bottom = 16.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            filterCategories.forEach { cat ->
                                val isSelected = selectedCategoryFilter == cat
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(if (isSelected) BrandPrimary else BrandBackground)
                                        .clickable { selectedCategoryFilter = cat }
                                        .padding(horizontal = 14.dp, vertical = 8.dp)
                                ) {
                                    Text(
                                        text = cat,
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (isSelected) Color.White else BrandTextPrimary
                                    )
                                }
                            }
                        }

                        // 2. Continuous Slider for Max Price Range with responsive labels
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 2.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = stringResource(id = R.string.max_price_usd),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = stringResource(id = R.string.up_to_price, maxPriceRange.toInt()),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandPrimary
                            )
                        }
                        Slider(
                            value = maxPriceRange,
                            onValueChange = { maxPriceRange = it },
                            valueRange = 30f..300f,
                            colors = SliderDefaults.colors(
                                thumbColor = BrandPrimary,
                                activeTrackColor = BrandPrimary,
                                inactiveTrackColor = BrandSoftGray
                            ),
                            modifier = Modifier.fillMaxWidth().padding(bottom = 14.dp)
                        )

                        // 3. Row for ratings and same day delivery selectors (Multi-layout compliance)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Minimum Rating selection columns
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.min_rating),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandTextPrimary
                                )
                                Row(
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    ratingOptions.forEach { rating ->
                                        val isSelected = minRatingFilter == rating
                                        val labelText = if (rating == 0.0) stringResource(id = R.string.all_category) else "★ ${rating.toString().substringBefore(".")}"
                                        Box(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clip(RoundedCornerShape(8.dp))
                                                .background(if (isSelected) BrandPrimary else BrandBackground)
                                                .clickable { minRatingFilter = rating }
                                                .padding(vertical = 8.dp),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = labelText,
                                                fontSize = 11.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) Color.White else BrandTextPrimary
                                            )
                                        }
                                    }
                                }
                            }

                            // Same Day Express delivery switch selection columns
                            Column(
                                modifier = Modifier.weight(1f),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.fulfillment_mode),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandTextPrimary
                                )
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(if (deliveryFilterSameDayOnly) BrandPrimary.copy(alpha = 0.12f) else BrandBackground)
                                        .border(
                                            1.dp,
                                            if (deliveryFilterSameDayOnly) BrandPrimary else Color.Transparent,
                                            RoundedCornerShape(8.dp)
                                        )
                                        .clickable { deliveryFilterSameDayOnly = !deliveryFilterSameDayOnly }
                                        .padding(vertical = 8.dp, horizontal = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    Text(
                                        text = stringResource(id = R.string.same_day_only),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (deliveryFilterSameDayOnly) BrandPrimary else BrandTextPrimary
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Clear all filters shortcut control rows
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End
                        ) {
                            TextButton(
                                onClick = {
                                    selectedCategoryFilter = "All"
                                    maxPriceRange = 300f
                                    minRatingFilter = 0.0
                                    deliveryFilterSameDayOnly = false
                                    selectedSortOption = SortOption.Newest
                                },
                                modifier = Modifier.height(44.dp)
                            ) {
                                Text(
                                    text = stringResource(id = R.string.reset_filters),
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandError
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { padding ->

        // Dynamic State UI Switch (Displays counts, results grid, or elegantly stylized empty state triggers)
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Count feedback banner
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(id = R.string.search_results),
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextMuted,
                    letterSpacing = 1.sp
                )

                Text(
                    text = stringResource(id = R.string.items_found, filteredProducts.size),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandPrimary
                )
            }

            if (filteredProducts.isEmpty()) {
                // Highly visual Empty state with smart troubleshooting prompts to maximize conversion redirection
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.SearchOff,
                            contentDescription = null,
                            tint = BrandTextMuted.copy(alpha = 0.5f),
                            modifier = Modifier.size(64.dp).padding(bottom = 16.dp)
                        )
                        Text(
                            text = stringResource(id = R.string.no_matching_found),
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = stringResource(id = R.string.try_reducing_filters),
                            fontSize = 13.sp,
                            color = BrandTextMuted,
                            textAlign = TextAlign.Center,
                            lineHeight = 18.sp
                        )
                        Spacer(modifier = Modifier.height(24.dp))
                        Button(
                            onClick = {
                                queryText = ""
                                selectedCategoryFilter = "All"
                                maxPriceRange = 300f
                                minRatingFilter = 0.0
                                deliveryFilterSameDayOnly = false
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(
                                text = stringResource(id = R.string.reset_filters).uppercase(),
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            } else {
                // Premium 2-column Product Results Grid
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("product_grid")
                        .padding(horizontal = 16.dp),
                    contentPadding = PaddingValues(top = 4.dp, bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    items(filteredProducts, key = { it.id }) { product ->
                        Card(
                            colors = CardDefaults.cardColors(containerColor = BrandSurface),
                            shape = RoundedCornerShape(16.dp),
                            border = BorderStroke(1.dp, BrandSoftGray),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onProductSelected(product.id) }
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(130.dp)
                            ) {
                                // Dynamic product image loader
                                AsyncImage(
                                    model = product.imageUrl,
                                    contentDescription = product.name,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                // Delivery indicator tag (Same day or fast Shipping highlights conversion)
                                Box(
                                    modifier = Modifier
                                        .padding(8.dp)
                                        .align(Alignment.TopStart)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(
                                            if (product.deliveryTime == "Same Day") Color(0xFFE8F5E9) else Color(0xFFFFF3E0)
                                        )
                                        .padding(horizontal = 6.dp, vertical = 3.dp)
                                ) {
                                    Text(
                                        text = if (product.deliveryTime == "Same Day") stringResource(id = R.string.same_day_delivery_badge) else stringResource(id = R.string.fast_delivery),
                                        color = if (product.deliveryTime == "Same Day") BrandPrimary else Color(0xFFE65100),
                                        fontSize = 8.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }

                                // Savings discount tag
                                if (product.originalPrice != null) {
                                    val savings = ((product.originalPrice - product.price) / product.originalPrice * 100).toInt()
                                    Box(
                                        modifier = Modifier
                                            .padding(8.dp)
                                            .align(Alignment.BottomEnd)
                                            .clip(RoundedCornerShape(6.dp))
                                            .background(BrandError)
                                            .padding(horizontal = 6.dp, vertical = 2.dp)
                                    ) {
                                        Text(
                                            text = stringResource(id = R.string.off_badge, savings),
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold
                                        )
                                    }
                                }
                            }

                            // Card attributes context & description details
                            Column(
                                modifier = Modifier.padding(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                // Multi-Vendor badge info
                                Text(
                                    text = product.storeName,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Text(
                                    text = product.name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandTextPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(2.dp))

                                // Rating and Stars feedback
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
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
                                        fontSize = 11.sp,
                                        color = BrandTextMuted
                                    )
                                }

                                Spacer(modifier = Modifier.height(2.dp))

                                // Price comparisons
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = "$${String.format("%.2f", product.price)}",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandTextPrimary
                                    )

                                    if (product.originalPrice != null) {
                                        Text(
                                            text = "$${String.format("%.2f", product.originalPrice)}",
                                            fontSize = 11.sp,
                                            color = BrandTextMuted,
                                            textDecoration = TextDecoration.LineThrough
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
