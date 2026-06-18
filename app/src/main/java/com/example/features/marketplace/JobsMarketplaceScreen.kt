package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.domain.model.Job
import com.example.ui.theme.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.filled.Close

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobsMarketplaceScreen(
    onNavigateToJobDetails: (String) -> Unit,
    onBack: () -> Unit,
    viewModel: JobsMarketplaceViewModel = viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val selectedLocation by viewModel.selectedLocation.collectAsState()
    val selectedEmploymentType by viewModel.selectedEmploymentType.collectAsState()
    val selectedCategory by viewModel.selectedCategory.collectAsState()
    val selectedExperienceLevel by viewModel.selectedExperienceLevel.collectAsState()
    val availableLocations = viewModel.availableLocations
    val availableEmploymentTypes = viewModel.availableEmploymentTypes
    val availableCategories = viewModel.availableCategories
    val availableExperienceLevels = viewModel.availableExperienceLevels

    val isArabic = java.util.Locale.getDefault().language == "ar"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الوظائف" else "Jobs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = BrandTextPrimary,
                    navigationIconContentColor = BrandTextPrimary
                )
            )
        },
        containerColor = BrandBackground
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = viewModel::onSearchQueryChanged,
                placeholder = { Text(if (isArabic) "ابحث عن وظائف..." else "Search jobs...") },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 8.dp),
                shape = RoundedCornerShape(12.dp),
                leadingIcon = {
                    Icon(Icons.Default.Search, contentDescription = null, tint = BrandTextMuted)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = BrandPrimary,
                    unfocusedBorderColor = BrandSoftGray,
                    focusedContainerColor = BrandSurface,
                    unfocusedContainerColor = BrandSurface
                )
            )

            // Filters Layer
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                    if (selectedLocation != null || selectedEmploymentType != null || selectedCategory != null || selectedExperienceLevel != null) {
                        Surface(
                            shape = CircleShape,
                            color = BrandSoftGray,
                            modifier = Modifier.clickable { viewModel.clearFilters() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    androidx.compose.material.icons.Icons.Default.Close,
                                    contentDescription = "Clear",
                                    modifier = Modifier.size(16.dp),
                                    tint = BrandTextPrimary
                                )
                                Text(if (isArabic) "مسح" else "Clear", fontSize = 14.sp)
                            }
                        }
                    }

                    FilterDropdown(
                        label = if (isArabic) "الاختصاص" else "Category",
                        options = availableCategories,
                        selectedOption = selectedCategory,
                        onOptionSelected = { viewModel.setCategoryFilter(it) }
                    )

                    FilterDropdown(
                        label = if (isArabic) "الخبرة" else "Experience",
                        options = availableExperienceLevels,
                        selectedOption = selectedExperienceLevel,
                        onOptionSelected = { viewModel.setExperienceLevelFilter(it) }
                    )

                    FilterDropdown(
                        label = if (isArabic) "الموقع" else "Location",
                        options = availableLocations,
                        selectedOption = selectedLocation,
                        onOptionSelected = { viewModel.setLocationFilter(it) }
                    )

                    FilterDropdown(
                        label = if (isArabic) "نوع الوظيفة" else "Employment Type",
                        options = availableEmploymentTypes,
                        selectedOption = selectedEmploymentType,
                        onOptionSelected = { viewModel.setEmploymentTypeFilter(it) }
                    )
                }
            Spacer(modifier = Modifier.height(8.dp))

            if (isLoading) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = BrandPrimary)
                }
            } else if (error != null) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error)
                }
            } else if (jobs.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(text = if (isArabic) "لا توجد وظائف متاحة" else "No jobs available", color = BrandTextMuted)
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(jobs) { job ->
                        JobCard(job = job, isArabic = isArabic, onClick = { onNavigateToJobDetails(job.id) })
                    }
                }
            }
        }
    }
}

@Composable
fun JobCard(job: Job, isArabic: Boolean, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    if (job.storeLogoUrl != null) {
                        AsyncImage(
                            model = job.storeLogoUrl,
                            contentDescription = null,
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandSoftGray),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .size(48.dp)
                                .clip(CircleShape)
                                .background(BrandSoftGray),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = BrandTextMuted)
                        }
                    }
                    
                    Column {
                        Text(
                            text = job.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandTextPrimary,
                            maxLines = 1
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = job.storeName,
                                fontSize = 14.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            if (job.isStoreVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = BrandGoldenYellow,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(16.dp))
                    Text(text = job.location.ifEmpty { "N/A" }, fontSize = 13.sp, color = BrandTextMuted)
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    Icon(Icons.Default.Work, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(16.dp))
                    Text(text = job.employmentType.ifEmpty { "N/A" }, fontSize = 13.sp, color = BrandTextMuted)
                }
            }

            if (job.salary.isNotBlank()) {
                Text(
                    text = job.salary,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = BrandPrimary
                )
            }
            
            val timeString = android.text.format.DateUtils.getRelativeTimeSpanString(job.createdAt)
            Text(
                text = timeString.toString(),
                fontSize = 12.sp,
                color = BrandTextMuted,
                modifier = Modifier.align(Alignment.End)
            )
        }
    }
}

@Composable
fun FilterDropdown(
    label: String,
    options: List<String>,
    selectedOption: String?,
    onOptionSelected: (String?) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = if (selectedOption != null) BrandPrimary.copy(alpha = 0.1f) else BrandSurface,
            border = BorderStroke(1.dp, if (selectedOption != null) BrandPrimary else BrandSoftGray),
            modifier = Modifier.clickable { expanded = true }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = selectedOption ?: label,
                    color = if (selectedOption != null) BrandPrimary else BrandTextPrimary,
                    fontSize = 14.sp
                )
                Icon(
                    Icons.Default.ArrowDropDown,
                    contentDescription = null,
                    tint = if (selectedOption != null) BrandPrimary else BrandTextMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = BrandSurface
        ) {
            DropdownMenuItem(
                text = { Text(if (java.util.Locale.getDefault().language == "ar") "الكل" else "All", color = BrandTextPrimary) },
                onClick = {
                    onOptionSelected(null)
                    expanded = false
                }
            )
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(option, color = BrandTextPrimary) },
                    onClick = {
                        onOptionSelected(option)
                        expanded = false
                    }
                )
            }
        }
    }
}

