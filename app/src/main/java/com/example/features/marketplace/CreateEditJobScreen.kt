package com.example.features.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.ServiceLocator
import com.example.ui.theme.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.Icons
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CreateEditJobScreen(
    jobId: String?,
    onBack: () -> Unit,
    viewModel: CreateEditJobViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val job by viewModel.job.collectAsState()
    val isArabic = java.util.Locale.getDefault().language == "ar"

    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var requirements by remember { mutableStateOf("") }
    var responsibilities by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var employmentType by remember { mutableStateOf("Full Time") }
    var salary by remember { mutableStateOf("") }

    val employmentTypes = listOf("Full Time", "Part Time", "Contract", "Freelance", "Internship")
    var expanded by remember { mutableStateOf(false) }

    LaunchedEffect(jobId) {
        if (jobId != null) {
            viewModel.loadJob(jobId)
        }
    }

    LaunchedEffect(job) {
        if (jobId != null && job != null) {
            title = job!!.title
            description = job!!.description
            requirements = job!!.requirements
            responsibilities = job!!.responsibilities
            location = job!!.location
            employmentType = job!!.employmentType
            salary = job!!.salary
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(if (jobId == null) (if (isArabic) "إضافة وظيفة" else "Add Job") else (if (isArabic) "تعديل وظيفة" else "Edit Job")) },
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
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (uiState is CreateEditJobViewModel.UiState.Loading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp), color = BrandPrimary)
            }

            if (uiState is CreateEditJobViewModel.UiState.Error) {
                Text((uiState as CreateEditJobViewModel.UiState.Error).message, color = MaterialTheme.colorScheme.error)
            }

            if (uiState is CreateEditJobViewModel.UiState.Success) {
                Text((uiState as CreateEditJobViewModel.UiState.Success).message, color = BrandSuccess, fontWeight = FontWeight.Bold)
                LaunchedEffect(Unit) {
                    kotlinx.coroutines.delay(1000)
                    onBack()
                }
            }

            OutlinedTextField(
                value = title,
                onValueChange = { title = it },
                label = { Text(if (isArabic) "المسمى الوظيفي" else "Job Title") },
                modifier = Modifier.fillMaxWidth()
            )

            OutlinedTextField(
                value = description,
                onValueChange = { description = it },
                label = { Text(if (isArabic) "وصف الوظيفة" else "Description") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = requirements,
                onValueChange = { requirements = it },
                label = { Text(if (isArabic) "المتطلبات" else "Requirements") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = responsibilities,
                onValueChange = { responsibilities = it },
                label = { Text(if (isArabic) "المسؤوليات" else "Responsibilities") },
                modifier = Modifier.fillMaxWidth(),
                minLines = 3
            )

            OutlinedTextField(
                value = location,
                onValueChange = { location = it },
                label = { Text(if (isArabic) "الموقع" else "Location") },
                modifier = Modifier.fillMaxWidth()
            )

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = employmentType,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (isArabic) "نوع التوظيف" else "Employment Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    employmentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                employmentType = type
                                expanded = false
                            }
                        )
                    }
                }
            }

            OutlinedTextField(
                value = salary,
                onValueChange = { salary = it },
                label = { Text(if (isArabic) "الراتب (اختياري)" else "Salary (Optional)") },
                modifier = Modifier.fillMaxWidth()
            )

            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    coroutineScope.launch {
                        val session = ServiceLocator.authRepository.getCurrentUserSession()
                        if (session != null) {
                            // get store ID
                            val store = ServiceLocator.storeRepository.getStoreByOwnerId(session.id)
                            if (store != null) {
                                viewModel.saveJob(
                                    storeId = store.id,
                                    jobId = jobId,
                                    title = title,
                                    description = description,
                                    requirements = requirements,
                                    responsibilities = responsibilities,
                                    location = location,
                                    employmentType = employmentType,
                                    salary = salary
                                )
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
            ) {
                Text(if (isArabic) "حفظ الوظيفة" else "Save Job", fontWeight = FontWeight.Bold, fontSize = 16.sp)
            }
        }
    }
}
