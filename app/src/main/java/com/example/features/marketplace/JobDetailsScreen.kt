package com.example.features.marketplace

import androidx.compose.foundation.background
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
    viewModel: JobDetailsViewModel = viewModel()
) {
    val job by viewModel.job.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()
    val applyState by viewModel.applyState.collectAsState()
    
    var showApplyDialog by remember { mutableStateOf(false) }

    val isArabic = java.util.Locale.getDefault().language == "ar"

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
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = BrandSurface,
                    titleContentColor = BrandTextPrimary,
                    navigationIconContentColor = BrandTextPrimary
                )
            )
        },
        bottomBar = {
            if (job != null && job?.status == "active") {
                BottomAppBar(
                    containerColor = BrandSurface,
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Button(
                        onClick = { showApplyDialog = true },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                    ) {
                        Text(if (isArabic) "تقديم طلب" else "Apply Now", fontWeight = FontWeight.Bold, fontSize = 16.sp)
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
                    
                    Column {
                        Text(
                            text = j.title,
                            fontWeight = FontWeight.Bold,
                            fontSize = 24.sp,
                            color = BrandTextPrimary
                        )
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(top = 4.dp)) {
                            Text(
                                text = j.storeName,
                                fontSize = 16.sp,
                                color = BrandTextMuted
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

                if (j.salary.isNotBlank()) {
                    Text(
                        text = "${if (isArabic) "الراتب:" else "Salary:"} ${j.salary}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = BrandPrimary
                    )
                }

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

    if (showApplyDialog && job != null) {
        ApplyForJobDialog(
            job = job!!,
            viewModel = viewModel,
            isArabic = isArabic,
            onDismiss = { showApplyDialog = false }
        )
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ApplyForJobDialog(
    job: Job,
    viewModel: JobDetailsViewModel,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val applyState by viewModel.applyState.collectAsState()
    val currentUser by viewModel.currentUser.collectAsState()

    var name by remember { mutableStateOf(currentUser?.name ?: "") }
    var phone by remember { mutableStateOf("") }
    var email by remember { mutableStateOf(currentUser?.email ?: "") }
    var message by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = {
            if (applyState !is JobDetailsViewModel.UiState.Loading) {
                viewModel.resetApplyState()
                onDismiss()
            }
        },
        containerColor = BrandSurface,
        title = { Text(if (isArabic) "تقديم طلب للوظيفة" else "Apply for Job") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                if (applyState is JobDetailsViewModel.UiState.Error) {
                    Text((applyState as JobDetailsViewModel.UiState.Error).message, color = MaterialTheme.colorScheme.error)
                } else if (applyState is JobDetailsViewModel.UiState.Success) {
                    Text((applyState as JobDetailsViewModel.UiState.Success).message, color = BrandPrimary)
                } else {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = phone,
                        onValueChange = { phone = it },
                        label = { Text(if (isArabic) "رقم الهاتف" else "Phone Number") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = message,
                        onValueChange = { message = it },
                        label = { Text(if (isArabic) "رسالة التقديم (اختياري)" else "Cover Letter/Message") },
                        modifier = Modifier.fillMaxWidth(),
                        minLines = 3
                    )
                    // CV URL could be added here if we had an upload flow in this dialog.
                }
            }
        },
        confirmButton = {
            if (applyState !is JobDetailsViewModel.UiState.Success) {
                Button(
                    onClick = { viewModel.applyForJob(job, name, phone, email, message, null) },
                    enabled = applyState !is JobDetailsViewModel.UiState.Loading && name.isNotBlank() && phone.isNotBlank() && email.isNotBlank(),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    if (applyState is JobDetailsViewModel.UiState.Loading) {
                        CircularProgressIndicator(color = androidx.compose.ui.graphics.Color.White, modifier = Modifier.size(24.dp))
                    } else {
                        Text(if (isArabic) "إرسال" else "Submit")
                    }
                }
            } else {
                Button(onClick = onDismiss, colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)) {
                    Text(if (isArabic) "إغلاق" else "Close")
                }
            }
        },
        dismissButton = {
            if (applyState !is JobDetailsViewModel.UiState.Loading && applyState !is JobDetailsViewModel.UiState.Success) {
                TextButton(onClick = onDismiss) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        }
    )
}
