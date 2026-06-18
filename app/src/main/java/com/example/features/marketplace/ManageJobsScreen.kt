package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.People
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.ServiceLocator
import com.example.domain.model.Job
import com.example.domain.model.JobApplication
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ManageJobsScreen(
    onBack: () -> Unit,
    onNavigateToCreateJob: () -> Unit,
    onNavigateToEditJob: (String) -> Unit,
    viewModel: ManageJobsViewModel = viewModel()
) {
    val jobs by viewModel.jobs.collectAsState()
    val applications by viewModel.applications.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val error by viewModel.error.collectAsState()

    val context = androidx.compose.ui.platform.LocalContext.current
    val isArabic = java.util.Locale.getDefault().language == "ar"

    var selectedJobId by remember { mutableStateOf<String?>(null) }
    
    // Auth context to fetch the store id (in a real app, you might pass it or infer from user session context)
    LaunchedEffect(Unit) {
        val userSession = ServiceLocator.authRepository.getCurrentUserSession()
        if (userSession != null) {
            val store = ServiceLocator.storeRepository.getStoreByOwnerId(userSession.id)
            if (store != null) {
                viewModel.loadData(store.id)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "إدارة الوظائف" else "Manage Jobs") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = onNavigateToCreateJob) {
                        Icon(Icons.Default.Add, contentDescription = "Add Job", tint = BrandPrimary)
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
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(padding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (jobs.isEmpty()) {
                    item {
                        Text(
                            text = if (isArabic) "لا توجد وظائف بعد. اضغط على + لإضافة وظيفة." else "No jobs found. Click + to add.",
                            color = BrandTextMuted,
                            modifier = Modifier.fillMaxWidth(),
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                } else {
                    items(jobs) { job ->
                        val jobApps = applications.filter { it.jobId == job.id }
                        ManageJobCard(
                            job = job,
                            applications = jobApps,
                            isArabic = isArabic,
                            onEdit = { onNavigateToEditJob(job.id) },
                            onDelete = { viewModel.deleteJob(job.id) },
                            onToggleStatus = { 
                                val newStatus = if (job.status == "active") "paused" else "active"
                                viewModel.updateJobStatus(job, newStatus)
                            },
                            onViewApplicants = {
                                selectedJobId = if (selectedJobId == job.id) null else job.id
                            }
                        )
                        
                        if (selectedJobId == job.id) {
                            Column(modifier = Modifier.fillMaxWidth().padding(start = 16.dp, end = 16.dp, top = 8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                                Text(if (isArabic) "المتقدمين:" else "Applicants:", fontWeight = FontWeight.Bold, color = BrandTextPrimary)
                                if (jobApps.isEmpty()) {
                                    Text(if (isArabic) "لا يوجد متقدمين" else "No applicants yet", color = BrandTextMuted, fontSize = 14.sp)
                                } else {
                                    jobApps.forEach { app ->
                                        ApplicantCard(
                                            application = app,
                                            isArabic = isArabic,
                                            onUpdateStatus = { status ->
                                                viewModel.updateApplicationStatus(app.id, status)
                                            }
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

@Composable
fun ManageJobCard(
    job: Job,
    applications: List<JobApplication>,
    isArabic: Boolean,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onToggleStatus: () -> Unit,
    onViewApplicants: () -> Unit
) {
    val pendingCount = applications.count { it.status == "pending" }
    val reviewedCount = applications.count { it.status == "reviewed" }
    val acceptedCount = applications.count { it.status == "accepted" }
    val rejectedCount = applications.count { it.status == "rejected" }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(job.title, fontWeight = FontWeight.Bold, fontSize = 18.sp, color = BrandTextPrimary, modifier = Modifier.weight(1f))
                val statusColor = if (job.status == "active") BrandSuccess else BrandTextMuted
                Text(
                    text = job.status.uppercase(),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
            
            Text(job.employmentType, color = BrandTextMuted, fontSize = 14.sp)

            // Status Breakdown
            if (applications.isNotEmpty()) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusPill("Pending: $pendingCount", BrandTextMuted)
                    StatusPill("Reviewed: $reviewedCount", BrandGoldenYellow)
                    StatusPill("Accepted: $acceptedCount", BrandSuccess)
                    StatusPill("Rejected: $rejectedCount", BrandError)
                }
            }
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = onViewApplicants, colors = ButtonDefaults.textButtonColors(contentColor = BrandPrimary)) {
                    Icon(Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "${applications.size} ${if (isArabic) "متقدمين" else "Applications"}")
                }
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    TextButton(onClick = onToggleStatus) {
                        Text(if (job.status == "active") (if (isArabic) "إيقاف" else "Pause") else (if (isArabic) "تفعيل" else "Activate"))
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", tint = BrandPrimary)
                    }
                }
            }
        }
    }
}

@Composable
fun StatusPill(text: String, color: androidx.compose.ui.graphics.Color) {
    Text(
        text = text,
        color = color,
        fontSize = 10.sp,
        fontWeight = FontWeight.Bold,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    )
}

@Composable
fun ApplicantCard(application: JobApplication, isArabic: Boolean, onUpdateStatus: (String) -> Unit) {
    val context = androidx.compose.ui.platform.LocalContext.current
    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandBackground)) {
        Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(application.applicantName, fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Text(application.email, color = BrandTextMuted, fontSize = 12.sp)
            Text(application.phone, color = BrandTextMuted, fontSize = 12.sp)
            if (application.message.isNotBlank()) {
                Text(application.message, color = BrandTextPrimary, fontSize = 14.sp, modifier = Modifier.padding(top = 4.dp))
            }
            if (!application.cvUrl.isNullOrBlank()) {
                TextButton(
                    onClick = {
                        try {
                            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW)
                            intent.data = android.net.Uri.parse(application.cvUrl)
                            context.startActivity(intent)
                        } catch (e: Exception) {
                            // ignore open error
                        }
                    },
                    modifier = Modifier.padding(top = 4.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(androidx.compose.material.icons.Icons.Default.People, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(if (isArabic) "يوجد سيرة ذاتية مرفقة (CV)" else "View CV", fontSize = 14.sp)
                }
            }
            
            val statusColor = when (application.status) {
                "accepted" -> BrandSuccess
                "rejected" -> BrandError
                "reviewed" -> BrandGoldenYellow
                else -> BrandTextMuted
            }
            
            Row(horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = application.status.uppercase(),
                    color = statusColor,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                )
                
                Row {
                    if (application.status == "pending") {
                        TextButton(onClick = { onUpdateStatus("reviewed") }) { Text(if (isArabic) "مراجعة" else "Reviewed", color = BrandGoldenYellow) }
                    }
                    TextButton(onClick = { onUpdateStatus("accepted") }) { Text(if (isArabic) "قبول" else "Accept", color = BrandSuccess) }
                    TextButton(onClick = { onUpdateStatus("rejected") }) { Text(if (isArabic) "رفض" else "Reject", color = BrandError) }
                }
            }
        }
    }
}
