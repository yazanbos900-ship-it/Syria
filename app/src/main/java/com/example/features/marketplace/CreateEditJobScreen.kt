package com.example.features.marketplace

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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
    var employmentType by remember { mutableStateOf("") }
    var category by remember { mutableStateOf("") }
    var experienceLevel by remember { mutableStateOf("") }
    var salary by remember { mutableStateOf("") }
    var contactWhatsApp by remember { mutableStateOf("") }
    var whatsappError by remember { mutableStateOf(false) }

    val locations = listOf("دمشق", "ريف دمشق", "حلب", "حمص", "حماة", "اللاذقية", "طرطوس", "السويداء", "درعا", "دير الزور", "الحسكة", "الرقة", "إدلب", "القنيطرة")
    val locationMap = mapOf(
        "دمشق" to "Damascus (دمشق)",
        "ريف دمشق" to "Rif Dimashq (ريف دمشق)",
        "حلب" to "Aleppo (حلب)",
        "حمص" to "Homs (حمص)",
        "حماة" to "Hama (حماة)",
        "اللاذقية" to "Latakia (اللاذقية)",
        "طرطوس" to "Tartus (طرطوس)",
        "السويداء" to "As-Suwayda (السويداء)",
        "درعا" to "Daraa (درعا)",
        "دير الزور" to "Deir ez-Zor (دير الزور)",
        "الحسكة" to "Al-Hasakah (الحسكة)",
        "الرقة" to "Raqqa (الرقة)",
        "إدلب" to "Idlib (إدلب)",
        "القنيطرة" to "Quneitra (القنيطرة)"
    )

    var specifySalary by remember { mutableStateOf(false) }
    var selectedCurrency by remember { mutableStateOf("SYP") } // "SYP" or "USD"
    var salaryAmount by remember { mutableStateOf("") }

    val employmentTypes = listOf("دوام كامل", "دوام جزئي", "عن بعد", "عقد", "تطوع", "تدريب", "مستقل")
    val categories = listOf("إدارة أعمال", "تقنية المعلومات", "إدخال بيانات", "لوجستيات", "مبيعات", "تسويق", "هندسة", "طب وصيدلة", "موارد بشرية", "محاسبة", "خدمة عملاء", "تصميم", "أخرى")
    val experienceLevels = listOf("حديث التخرج", "مبتدئ (جونيور)", "متوسط", "خبير (سينيور)", "مدير")
    var expandedLoc by remember { mutableStateOf(false) }
    var expandedEmp by remember { mutableStateOf(false) }
    var expandedCat by remember { mutableStateOf(false) }
    var expandedExp by remember { mutableStateOf(false) }

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
            category = job!!.category
            experienceLevel = job!!.experienceLevel
            
            val rawSalary = job!!.salary
            if (rawSalary.isBlank() || rawSalary.equals("Negotiable", ignoreCase = true) || rawSalary.contains("تفاوض")) {
                specifySalary = false
                salaryAmount = ""
            } else {
                specifySalary = true
                if (rawSalary.startsWith("$")) {
                    selectedCurrency = "USD"
                    salaryAmount = rawSalary.replace("$", "").trim()
                } else {
                    selectedCurrency = "SYP"
                    // Extract digits only for the amount
                    salaryAmount = rawSalary.replace(Regex("[^0-9]"), "")
                }
            }
            
            val contact = job!!.contactWhatsApp
            contactWhatsApp = if (contact.startsWith("+963")) contact.removePrefix("+963") else contact
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

            ExposedDropdownMenuBox(
                expanded = expandedLoc,
                onExpandedChange = { expandedLoc = !expandedLoc }
            ) {
                OutlinedTextField(
                    value = if (isArabic) location else (locationMap[location] ?: location),
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (isArabic) "المحافظة" else "Governorate / Location") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedLoc) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedLoc,
                    onDismissRequest = { expandedLoc = false }
                ) {
                    locations.forEach { loc ->
                        DropdownMenuItem(
                            text = { Text(if (isArabic) loc else (locationMap[loc] ?: loc)) },
                            onClick = {
                                location = loc
                                expandedLoc = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedEmp,
                onExpandedChange = { expandedEmp = !expandedEmp }
            ) {
                OutlinedTextField(
                    value = employmentType,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (isArabic) "نوع التوظيف" else "Employment Type") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedEmp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedEmp,
                    onDismissRequest = { expandedEmp = false }
                ) {
                    employmentTypes.forEach { type ->
                        DropdownMenuItem(
                            text = { Text(type) },
                            onClick = {
                                employmentType = type
                                expandedEmp = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedCat,
                onExpandedChange = { expandedCat = !expandedCat }
            ) {
                OutlinedTextField(
                    value = category,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (isArabic) "الاختصاص" else "Category") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedCat) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedCat,
                    onDismissRequest = { expandedCat = false }
                ) {
                    categories.forEach { cat ->
                        DropdownMenuItem(
                            text = { Text(cat) },
                            onClick = {
                                category = cat
                                expandedCat = false
                            }
                        )
                    }
                }
            }

            ExposedDropdownMenuBox(
                expanded = expandedExp,
                onExpandedChange = { expandedExp = !expandedExp }
            ) {
                OutlinedTextField(
                    value = experienceLevel,
                    onValueChange = { },
                    readOnly = true,
                    label = { Text(if (isArabic) "الخبرة" else "Experience Level") },
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expandedExp) },
                    modifier = Modifier.fillMaxWidth().menuAnchor()
                )
                ExposedDropdownMenu(
                    expanded = expandedExp,
                    onDismissRequest = { expandedExp = false }
                ) {
                    experienceLevels.forEach { exp ->
                        DropdownMenuItem(
                            text = { Text(exp) },
                            onClick = {
                                experienceLevel = exp
                                expandedExp = false
                            }
                        )
                    }
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isArabic) "تحديد الراتب" else "Specify Salary",
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary
                )
                Switch(
                    checked = specifySalary,
                    onCheckedChange = { specifySalary = it },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor = BrandPrimary,
                        checkedTrackColor = BrandPrimary.copy(alpha = 0.5f)
                    )
                )
            }

            if (specifySalary) {
                Text(
                    text = if (isArabic) "اختر العملة" else "Select Currency",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextMuted
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    FilterChip(
                        selected = selectedCurrency == "SYP",
                        onClick = { selectedCurrency = "SYP" },
                        label = { Text(if (isArabic) "🇸🇾 ليرة سورية (SYP)" else "🇸🇾 Syrian Pound (SYP)") }
                    )
                    FilterChip(
                        selected = selectedCurrency == "USD",
                        onClick = { selectedCurrency = "USD" },
                        label = { Text(if (isArabic) "🇺🇸 دولار أمريكي (USD)" else "🇺🇸 US Dollar (USD)") }
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                if (selectedCurrency == "USD") {
                    OutlinedTextField(
                        value = salaryAmount,
                        onValueChange = { input -> salaryAmount = input.filter { it.isDigit() } },
                        label = { Text(if (isArabic) "الراتب بالدولار" else "Salary in USD") },
                        prefix = { Text("$", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("600") },
                        modifier = Modifier.fillMaxWidth()
                    )
                } else {
                    OutlinedTextField(
                        value = salaryAmount,
                        onValueChange = { input -> salaryAmount = input.filter { it.isDigit() } },
                        label = { Text(if (isArabic) "الراتب بالليرة السورية" else "Salary in SYP") },
                        suffix = { Text(if (isArabic) "ل.س" else "SYP", fontWeight = FontWeight.Bold) },
                        placeholder = { Text("3,500,000") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }

            OutlinedTextField(
                value = contactWhatsApp,
                onValueChange = { input -> 
                    contactWhatsApp = input.filter { it.isDigit() }
                    whatsappError = contactWhatsApp.startsWith("0") || contactWhatsApp.startsWith("963") || contactWhatsApp.isEmpty()
                },
                label = { Text(if (isArabic) "رقم واتساب الوظيفة" else "WhatsApp Number") },
                prefix = { Text("+963", fontWeight = FontWeight.Bold, color = BrandTextPrimary, modifier = Modifier.padding(end = 4.dp)) },
                placeholder = { Text("930111157") },
                isError = whatsappError,
                supportingText = {
                    if (whatsappError) {
                        Text(if (isArabic) "يرجى إدخال الرقم بدون 0 وبدون +963 (مثال: 930111157)" else "Please enter without 0 and without +963 (e.g. 930111157)")
                    }
                },
                modifier = Modifier.fillMaxWidth()
            )

            val coroutineScope = rememberCoroutineScope()
            Button(
                onClick = {
                    val isContactValid = contactWhatsApp.isNotEmpty() && !contactWhatsApp.startsWith("0") && !contactWhatsApp.startsWith("963")
                    if (!isContactValid) {
                        whatsappError = true
                        return@Button
                    }
                    val fullWhatsApp = "+963$contactWhatsApp"

                    coroutineScope.launch {
                        val session = ServiceLocator.authRepository.getCurrentUserSession()
                        if (session != null) {
                            // get store ID
                            val store = ServiceLocator.storeRepository.getStoreByOwnerId(session.id)
                            if (store != null) {
                                val finalSalary = if (!specifySalary) {
                                    ""
                                } else {
                                    if (selectedCurrency == "USD") {
                                        "$${salaryAmount.trim()}"
                                    } else {
                                        val parsedAmount = salaryAmount.replace(Regex("[^0-9]"), "").toLongOrNull()
                                        val formattedSyp = if (parsedAmount != null) {
                                            java.text.NumberFormat.getNumberInstance(java.util.Locale.US).format(parsedAmount)
                                        } else {
                                            salaryAmount
                                        }
                                        if (formattedSyp.isBlank()) "" else "${formattedSyp} ل.س"
                                    }
                                }
                                viewModel.saveJob(
                                    storeId = store.id,
                                    jobId = jobId,
                                    title = title,
                                    description = description,
                                    requirements = requirements,
                                    responsibilities = responsibilities,
                                    location = location,
                                    employmentType = employmentType,
                                    category = category,
                                    experienceLevel = experienceLevel,
                                    salary = finalSalary,
                                    contactWhatsApp = fullWhatsApp
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
