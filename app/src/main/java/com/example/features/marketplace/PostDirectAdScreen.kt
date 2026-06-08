package com.example.features.marketplace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import coil.compose.AsyncImage
import com.example.R
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.domain.model.Category
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostDirectAdScreen(
    onNavigateBack: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // User authentication guard
    var currentUser by remember { mutableStateOf<com.example.domain.model.User?>(null) }
    var isLoadingUser by remember { mutableStateOf(true) }
    var authError by remember { mutableStateOf<String?>(null) }

    // Form states
    var adTitle by remember { mutableStateOf("") }
    var adPrice by remember { mutableStateOf("") }
    var adDesc by remember { mutableStateOf("") }
    var adLocation by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var selectedCondition by remember { mutableStateOf("new") } // "new" or "used"
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }

    // UI Feedback State
    var isUploadingImage by remember { mutableStateOf(false) }
    var isSubmitting by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }

    LaunchedEffect(Unit) {
        isLoadingUser = true
        try {
            val user = ServiceLocator.authRepository.getCurrentUserSession()
            if (user == null) {
                authError = if (isArabic) "الرجاء تسجيل الدخول أولاً للإعلان المباشر" else "Please log in first before posting a direct ad."
            } else {
                currentUser = user
            }
        } catch (e: Exception) {
            authError = e.message ?: "Failed to inspect authentication"
        } finally {
            isLoadingUser = false
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && imageUris.size < 5) {
            scope.launch {
                isUploadingImage = true
                val uploader = CloudinaryUploader()
                val result = uploader.uploadFile(uri.toString())
                result.onSuccess { url ->
                    imageUris = imageUris + url
                }
                result.onFailure {
                    scope.launch {
                        snackbarHostState.showSnackbar(
                            if (isArabic) "فشل رفع الصورة، حاول مجدداً" else "Upload failed, try again"
                        )
                    }
                }
                isUploadingImage = false
            }
        }
    }

    Scaffold(
        containerColor = Color(0xFF0A0B0D), // Dark theme #0A0B0D background
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "نشر إعلان مباشر جديد" else "Post New Direct Ad",
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = Color(0xFF0A0B0D)
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("post_ad_back_button")
                    ) {
                        Icon(
                            imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                            contentDescription = "السابق",
                            tint = Color.White
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoadingUser) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = Color(0xFF1DB954))
            }
        } else if (authError != null) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                verticalArrangement = Arrangement.Center,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = BrandError,
                    modifier = Modifier.size(64.dp)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = authError ?: "",
                    color = Color.White,
                    fontSize = 15.sp,
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Info Ribbon
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF17191E)),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                        Column {
                            Text(
                                text = if (isArabic) "إعلان مباشر للاستجابة السريعة" else "Direct Ad for Fast Response",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isArabic) "سيظهر الإعلان للجميع على الصفحة الرئيسية فوراً." else "Your ad will appear on the first home shelf immediately.",
                                color = BrandTextMuted,
                                fontSize = 11.sp
                            )
                        }
                    }
                }

                // 1. Title Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "عنوان الإعلان" else "Ad Title",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = adTitle,
                        onValueChange = { adTitle = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "مثال: هاتف آيفون 15 برو ماكس للبيع..." else "e.g. iPhone 15 Pro Max for sale...",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = Color(0xFF17191E),
                            unfocusedContainerColor = Color(0xFF17191E)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_ad_title_input")
                    )
                }

                // 2. Price Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "السعر (USD)" else "Price (USD)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = adPrice,
                        onValueChange = { adPrice = it },
                        placeholder = {
                            Text(
                                text = "e.g. 899.99",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = Color(0xFF17191E),
                            unfocusedContainerColor = Color(0xFF17191E)
                        ),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_ad_price_input")
                    )
                }

                // 3. Category Selection Trigger
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "الفئة" else "Category",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF17191E))
                            .border(
                                1.dp,
                                if (selectedCategory != null) Color(0xFF1DB954) else BrandSoftGray,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showCategoryDialog = true }
                            .padding(horizontal = 16.dp)
                            .testTag("post_ad_category_trigger")
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth().align(Alignment.CenterStart),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = if (selectedCategory != null) {
                                    if (isArabic) selectedCategory!!.nameAr else selectedCategory!!.nameEn
                                } else {
                                    if (isArabic) "اختر فئة الإعلان" else "Select category"
                                },
                                color = if (selectedCategory != null) Color.White else BrandTextMuted,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = Color.White
                            )
                        }
                    }
                }

                // 4. Condition Choice Chips
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "حالة السلعة" else "Item Condition",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .horizontalScroll(rememberScrollState()),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val conditions = listOf(
                            "new" to R.string.condition_new,
                            "open_box" to R.string.condition_open_box,
                            "like_new" to R.string.condition_like_new,
                            "excellent" to R.string.condition_excellent,
                            "good" to R.string.condition_good,
                            "fair" to R.string.condition_fair,
                            "used" to R.string.condition_used,
                            "for_parts" to R.string.condition_for_parts
                        )
                        conditions.forEach { (key, strId) ->
                            val isSelected = selectedCondition == key
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(20.dp))
                                    .background(if (isSelected) Color(0xFF1DB954) else Color(0xFF17191E))
                                    .border(
                                        1.dp,
                                        if (isSelected) Color(0xFF1DB954) else BrandSoftGray,
                                        RoundedCornerShape(20.dp)
                                    )
                                    .clickable { selectedCondition = key }
                                    .padding(horizontal = 16.dp, vertical = 8.dp)
                                    .testTag("post_ad_condition_$key")
                            ) {
                                Text(
                                    text = stringResource(id = strId),
                                    color = if (isSelected) Color.White else BrandTextMuted,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                    }
                }

                // 5. Description
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "تفاصيل الإعلان" else "Ad Details / Description",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = adDesc,
                        onValueChange = { adDesc = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "اكتب مواصفات المنتج والعيوب وتفاصيل الاتصال..." else "Write technical specs, flaws, and contact information...",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = Color(0xFF17191E),
                            unfocusedContainerColor = Color(0xFF17191E)
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_ad_desc_input")
                    )
                }

                // 6. Location Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "الموقع / المدينة" else "Location (City)",
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = adLocation,
                        onValueChange = { adLocation = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "مثال: دمشق، الشعلان" else "e.g. Damascus, Shaalan",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = Color(0xFF1DB954),
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = Color(0xFF17191E),
                            unfocusedContainerColor = Color(0xFF17191E)
                        ),
                        singleLine = true,
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("post_ad_location_input")
                    )
                }

                // 7. Images List Grid (min 1, max 5)
                Column(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "صور الإعلان (1 - 5)" else "Ad Images (1 - 5)",
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "${imageUris.size}/5",
                            color = BrandTextMuted,
                            fontSize = 12.sp
                        )
                    }

                    // 3-Column Image Grid
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        imageUris.forEachIndexed { index, url ->
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF17191E))
                                    .border(
                                        1.dp,
                                        if (index == 0) Color(0xFF1DB954) else BrandSoftGray,
                                        RoundedCornerShape(12.dp)
                                    )
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = "Ad image",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )

                                if (index == 0) {
                                    Box(
                                        modifier = Modifier
                                            .align(Alignment.BottomCenter)
                                            .fillMaxWidth()
                                            .background(Color(0xFF1DB954))
                                            .padding(vertical = 2.dp),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Text(
                                            text = if (isArabic) "الغلاف" else "Cover",
                                            color = Color.White,
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }
                                }

                                // Delete Badge
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopEnd)
                                        .padding(4.dp)
                                        .size(20.dp)
                                        .clip(CircleShape)
                                        .background(Color.Red)
                                        .clickable {
                                            imageUris = imageUris.filterIndexed { idx, _ -> idx != index }
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "Delete",
                                        tint = Color.White,
                                        modifier = Modifier.size(10.dp)
                                    )
                                }
                            }
                        }

                        if (imageUris.size < 5) {
                            Box(
                                modifier = Modifier
                                    .size(80.dp)
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(Color(0xFF17191E))
                                    .border(BorderStroke(1.dp, BrandSoftGray), RoundedCornerShape(12.dp))
                                    .clickable { photoLauncher.launch("image/*") },
                                contentAlignment = Alignment.Center
                            ) {
                                if (isUploadingImage) {
                                    CircularProgressIndicator(
                                        color = Color(0xFF1DB954),
                                        modifier = Modifier.size(24.dp)
                                    )
                                } else {
                                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                        Icon(
                                            imageVector = Icons.Default.AddPhotoAlternate,
                                            contentDescription = "Pick Photo",
                                            tint = Color.White
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = if (isArabic) "إضافة" else "Add",
                                            color = BrandTextMuted,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // Validation Message
                if (validationError != null) {
                    Text(
                        text = validationError!!,
                        color = BrandError,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Submit Button
                Button(
                    onClick = {
                        validationError = null
                        val parsedPrice = adPrice.toDoubleOrNull()
                        
                        // Validation checks
                        if (adTitle.isBlank()) {
                            validationError = if (isArabic) "الرجاء كتابة عنوان الإعلان" else "Please write ad title"
                        } else if (parsedPrice == null || parsedPrice <= 0.0) {
                            validationError = if (isArabic) "الرجاء تحديد سعر صحيح بالدولار" else "Please enter valid numeric price in USD"
                        } else if (selectedCategory == null) {
                            validationError = if (isArabic) "الرجاء اختيار الفئة" else "Please choose a category"
                        } else if (adDesc.isBlank() || adDesc.length < 10) {
                            validationError = if (isArabic) "الرجاء كتابة وصف أطول وتفصيلي (مطلوب 10 خانات على الأقل)" else "Write a longer description (at least 10 chars)"
                        } else if (adLocation.isBlank()) {
                            validationError = if (isArabic) "الرجاء كتابة الموقع أو المدينة" else "Please mention city/location"
                        } else if (imageUris.isEmpty()) {
                            validationError = if (isArabic) "يجب إرفاق صورة واحدة على الأقل للإعلان المباشر" else "At least 1 image is required for a direct ad"
                        } else {
                            // Save to Firestore
                            isSubmitting = true
                            val userObj = currentUser
                            if (userObj != null) {
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val docRef = db.collection("direct_ads").document()
                                val adData = hashMapOf(
                                    "adId" to docRef.id,
                                    "ownerUid" to userObj.id,
                                    "ownerUsername" to userObj.name,
                                    "title" to adTitle.trim(),
                                    "price" to parsedPrice,
                                    "categoryId" to selectedCategory!!.id,
                                    "condition" to selectedCondition,
                                    "description" to adDesc.trim(),
                                    "location" to adLocation.trim(),
                                    "images" to imageUris,
                                    "coverImage" to imageUris.first(),
                                    "status" to "active",
                                    "createdAt" to com.google.firebase.firestore.FieldValue.serverTimestamp()
                                )

                                docRef.set(adData)
                                    .addOnSuccessListener {
                                        isSubmitting = false
                                        scope.launch {
                                            snackbarHostState.showSnackbar(
                                                if (isArabic) "تم نشر إعلانك المباشر بنجاح!" else "Direct Ad published successfully!"
                                            )
                                            // Delay and Navigate Back
                                            kotlinx.coroutines.delay(1000)
                                            onNavigateBack()
                                        }
                                    }
                                    .addOnFailureListener { e ->
                                        isSubmitting = false
                                        validationError = e.localizedMessage ?: "Failed to save ad"
                                    }
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .testTag("submit_direct_ad_button"),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF1DB954), // green
                        contentColor = Color.White
                    ),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(
                            color = Color.White,
                            modifier = Modifier.size(24.dp)
                        )
                    } else {
                        Text(
                            text = if (isArabic) "نشر الإعلان الآن" else "Publish Ad Now",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                color = BrandSurface,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BrandSoftGray),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp)
            ) {
                Column(
                    modifier = Modifier
                        .padding(20.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isArabic) "اختر فئة الإعلان المباشر" else "Select Ad Category",
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    categories.forEach { category ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(8.dp))
                                .clickable {
                                    selectedCategory = category
                                    showCategoryDialog = false
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isArabic) category.nameAr else category.nameEn,
                                color = BrandTextPrimary,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        HorizontalDivider(color = BrandSoftGray)
                    }
                }
            }
        }
    }
}
