package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Business
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.domain.model.JobApplication
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun UserApplicationsScreen(
    onBack: () -> Unit,
    onNavigateToJobDetails: (String) -> Unit,
    viewModel: UserApplicationsViewModel = viewModel()
) {
    val applications by viewModel.applications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val isArabic = java.util.Locale.getDefault().language == "ar"

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "طلباتي" else "My Applications") },
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
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = error ?: "Error", color = MaterialTheme.colorScheme.error)
            }
        } else if (applications.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text(text = if (isArabic) "لم تقدم على أي وظيفة بعد" else "You haven't applied to any jobs yet", color = BrandTextMuted)
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(applications) { app ->
                    ApplicationCard(
                        application = app,
                        isArabic = isArabic,
                        onClick = { onNavigateToJobDetails(app.jobId) }
                    )
                }
            }
        }
    }
}

@Composable
fun ApplicationCard(application: JobApplication, isArabic: Boolean, onClick: () -> Unit) {
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
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Icon(Icons.Default.Business, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(20.dp))
                    Text(
                        text = "Store ID: ${application.storeId.take(5)}...", // Ideally you'd fetch the actual job title/store name if you denormalize it
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = BrandTextPrimary
                    )
                }
                
                val statusColor = when (application.status) {
                    "accepted" -> BrandSuccess
                    "rejected" -> BrandError
                    "reviewed" -> BrandGoldenYellow
                    else -> BrandTextMuted
                }
                
                Text(
                    text = application.status.uppercase(),
                    color = statusColor,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    modifier = Modifier
                        .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }

            val timeString = android.text.format.DateUtils.getRelativeTimeSpanString(application.createdAt)
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Icon(Icons.Default.Info, contentDescription = null, tint = BrandTextMuted, modifier = Modifier.size(14.dp))
                Text(text = "${if (isArabic) "تاريخ التقديم:" else "Applied:"} $timeString", fontSize = 12.sp, color = BrandTextMuted)
            }
        }
    }
}
