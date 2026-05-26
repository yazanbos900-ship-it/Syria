package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Category
import com.example.domain.repository.ProductRepository
import com.example.ui.theme.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

internal val productCatalog = listOf(
    MarketProduct(
        id = "apple_watch_ultra_2",
        name = "Precision Active Chrono",
        price = 189.0,
        originalPrice = 378.0,
        rating = 4.9,
        reviewsCount = 124,
        category = "Bespoke",
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
        category = "Bespoke",
        storeName = "Bespoke Horology Lab",
        deliveryTime = "Same Day",
        dateAdded = "2026-05-21",
        imageUrl = "https://images.unsplash.com/photo-1547996160-81dfa63595aa?auto=format&fit=crop&w=400&q=80"
    )
)

object SharedFilterState {
    val selectedCategoryFilterState = mutableStateOf("All")
    val maxPriceRangeState = mutableStateOf(300f)
    val minRatingFilterState = mutableStateOf(0.0)
    val deliveryFilterSameDayOnlyState = mutableStateOf(false)
    val selectedSortOptionState = mutableStateOf(SortOption.Newest)
    val categoriesListState = mutableStateOf(listOf(Category(id = "All", name = "All")))

    var selectedCategoryFilter by selectedCategoryFilterState
    var maxPriceRange by maxPriceRangeState
    var minRatingFilter by minRatingFilterState
    var deliveryFilterSameDayOnly by deliveryFilterSameDayOnlyState
    var selectedSortOption by selectedSortOptionState
    var categoriesList by categoriesListState

    val isActive: Boolean
        get() = selectedCategoryFilter != "All" ||
                maxPriceRange < 300f ||
                minRatingFilter > 0.0 ||
                deliveryFilterSameDayOnly

    fun init(productRepository: ProductRepository, scope: CoroutineScope) {
        scope.launch {
            productRepository.getCategories().collect { cats ->
                categoriesList = listOf(Category(id = "All", name = "All")) + cats
            }
        }
    }

    fun reset() {
        selectedCategoryFilter = "All"
        maxPriceRange = 300f
        minRatingFilter = 0.0
        deliveryFilterSameDayOnly = false
        selectedSortOption = SortOption.Newest
    }
}

@Composable
fun FilterBottomSheetContent(
    modifier: Modifier = Modifier,
    onApply: () -> Unit = {}
) {
    var selectedCategoryFilter by SharedFilterState.selectedCategoryFilterState
    var maxPriceRange by SharedFilterState.maxPriceRangeState
    var minRatingFilter by SharedFilterState.minRatingFilterState
    var deliveryFilterSameDayOnly by SharedFilterState.deliveryFilterSameDayOnlyState
    val filterCategories = SharedFilterState.categoriesList

    val ratingOptions = listOf(0.0, 4.0, 4.5, 4.8)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(Color.White)
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
            text = "Filter by Category",
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
            filterCategories.forEach { category ->
                val catName = category.name
                val isSelected = selectedCategoryFilter == catName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) BrandPrimary else BrandBackground)
                        .clickable { selectedCategoryFilter = catName }
                        .padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Text(
                        text = catName,
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
                text = "Max Price (USD)",
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = BrandTextPrimary
            )
            Text(
                text = "Up to $${maxPriceRange.toInt()}",
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
                    text = "Minimum Rating",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    ratingOptions.forEach { rating ->
                        val isSelected = minRatingFilter == rating
                        val labelText = if (rating == 0.0) "All" else "★ ${rating.toString().substringBefore(".")}"
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
                    text = "Fulfillment Mode",
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
                        text = "⚡ Same Day Only",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (deliveryFilterSameDayOnly) BrandPrimary else BrandTextPrimary
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // Clear all filters shortcut control rows & Close button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(
                onClick = { SharedFilterState.reset() },
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Reset All Filters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandError
                )
            }

            Button(
                onClick = onApply,
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                shape = RoundedCornerShape(12.dp),
                modifier = Modifier.height(44.dp)
            ) {
                Text(
                    text = "Apply Filters",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }
        }
    }
}
