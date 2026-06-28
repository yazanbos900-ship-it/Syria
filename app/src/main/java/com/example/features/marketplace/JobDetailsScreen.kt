package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BusinessCenter
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Work
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobDetailsScreen(
    jobId: String,
    onBack: () -> Unit,
    onNavigateToStoreDetail: (String) -> Unit,
    viewModel: JobDetailsViewModel = viewModel()
) {
    val job by viewModel.job.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    
    val isArabic = java.util.Locale.getDefault().language == "ar"
    val uriHandler = androidx.compose.ui.platform.LocalUriHandler.current

    LaunchedEffect(jobId) {
        viewModel.loadJob(jobId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "تفاصيل الوظيفة" else "Job Details") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    val isSaved by viewModel.isSaved.collectAsState()
                    val currentUser by viewModel.currentUser.collectAsState()
                    if (currentUser != null) {
                        IconButton(onClick = { viewModel.toggleSaveJob() }) {
                            Icon(
                                imageVector = if (isSaved) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = if (isSaved) "Unsave Job" else "Save Job",
                                tint = if (isSaved) BrandPrimary else BrandTextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = BrandTextPrimary,
                    navigationIconContentColor = BrandTextPrimary
                )
            )
        },
        bottomBar = {
            if (job != null && job?.status == "active") {
                val j = job!!
                BottomAppBar(
                    containerColor = BrandSurface,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val hasWhatsApp = j.contactWhatsApp.isNotBlank()
                    Button(
                        onClick = { 
                            if (hasWhatsApp) {
                                val message = if (isArabic) {
                                    "مرحباً، أرغب بالتقدم على وظيفة:\n${j.title}\nمن خلال تطبيق WasetPlus."
                                } else {
                                    "Hello,\nI would like to apply for:\n${j.title}\nthrough WasetPlus."
                                }
                                val encodedMessage = java.net.URLEncoder.encode(message, "UTF-8")
                                // If stored as +963930111157, remove the + for the wa.me link
                                val pureNumber = j.contactWhatsApp.removePrefix("+")
                                uriHandler.openUri("https://wa.me/$pureNumber?text=$encodedMessage")
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = hasWhatsApp,
                        colors = ButtonDefaults.buttonColors(containerColor = if (hasWhatsApp) BrandPrimary else BrandSoftGray)
                    ) {
                        Text(
                            if (!hasWhatsApp) {
                                if (isArabic) "رقم التواصل غير متوفر" else "Contact number unavailable"
                            } else {
                                if (isArabic) "تقديم طلب" else "Apply Now"
                            }, 
                            fontWeight = FontWeight.Bold, 
                            fontSize = 16.sp
                        )
                    }
                }
            }
        },
        containerColor = BrandBackground
    ) { padding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (error != null || job == null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Job not found", color = MaterialTheme.colorScheme.error)
            }
        } else {
            val j = job!!
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Header
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier.clickable {
                            if (j.storeId.isNotBlank()) {
                                onNavigateToStoreDetail(j.storeId)
                            }
                        }
                    ) {
                        if (j.storeLogoUrl != null) {
                            AsyncImage(
                                model = j.storeLogoUrl,
                                contentDescription = null,
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BrandSoftGray),
                                contentScale = ContentScale.Crop
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .size(72.dp)
                                    .clip(CircleShape)
                                    .background(BrandSoftGray),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.BusinessCenter, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(36.dp))
                            }
                        }
                    }
                    
                    Column {
                        Text(
                            text = j.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = BrandTextPrimary
                        )
                        Row(
                            verticalAlignment = Alignment.CenterVertically, 
                            modifier = Modifier
                                .padding(top = 4.dp)
                                .clickable {
                                    if (j.storeId.isNotBlank()) {
                                        onNavigateToStoreDetail(j.storeId)
                                    }
                                }
                        ) {
                            Text(
                                text = j.storeName,
                                fontSize = 16.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold,
                                style = androidx.compose.ui.text.TextStyle(
                                    textDecoration = androidx.compose.ui.text.style.TextDecoration.Underline
                                )
                            )
                            if (j.isStoreVerified) {
                                Spacer(modifier = Modifier.width(4.dp))
                                Icon(
                                    Icons.Default.Verified,
                                    contentDescription = "Verified",
                                    tint = BrandGoldenYellow,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }

                // Quick Info Badges
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    BadgeInfo(icon = Icons.Default.LocationOn, text = j.location)
                    BadgeInfo(icon = Icons.Default.Work, text = j.employmentType)
                }

                val salaryText = if (j.salary.isNotBlank()) {
                    "${if (isArabic) "الراتب:" else "Salary:"} ${j.salary}"
                } else {
                    if (isArabic) "الراتب: قابل للتفاوض" else "Salary: Negotiable"
                }
                Text(
                    text = salaryText,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = BrandPrimary
                )

                HorizontalDivider(color = BrandSoftGray)

                // Description section
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "وصف الوظيفة" else "Job Description",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandTextPrimary
                    )
                    Text(
                        text = j.description,
                        fontSize = 15.sp,
                        color = BrandTextMuted,
                        lineHeight = 22.sp
                    )
                }
                
                // Requirements section
                if (j.requirements.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "المتطلبات" else "Requirements",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandTextPrimary
                        )
                        Text(
                            text = j.requirements,
                            fontSize = 15.sp,
                            color = BrandTextMuted,
                            lineHeight = 22.sp
                        )
                    }
                }

                // Responsibilities section
                if (j.responsibilities.isNotBlank()) {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = if (isArabic) "المسؤوليات" else "Responsibilities",
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp,
                            color = BrandTextPrimary
                        )
                        Text(
                            text = j.responsibilities,
                            fontSize = 15.sp,
                            color = BrandTextMuted,
                            lineHeight = 22.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BadgeInfo(icon: androidx.compose.ui.graphics.vector.ImageVector, text: String) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(8.dp))
            .padding(horizontal = 8.dp, vertical = 6.dp)
    ) {
        Icon(icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
        Text(text = text, fontSize = 14.sp, color = BrandPrimary, fontWeight = FontWeight.Medium)
    }
}

