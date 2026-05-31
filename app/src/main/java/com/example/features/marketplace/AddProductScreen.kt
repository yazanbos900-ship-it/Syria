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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
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
import com.example.components.BrandCard
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.domain.model.Category
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.ui.theme.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateStore: () -> Unit = {}
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val scope = rememberCoroutineScope()

    var store by remember { mutableStateOf<Store?>(null) }
    var isLoadingStore by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // Form inputs
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploadingImage by remember { mutableStateOf(false) }
    var selectedCategory by remember { mutableStateOf<Category?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var isSuccess by remember { mutableStateOf(false) }

    var showCategoryDialog by remember { mutableStateOf(false) }
    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }

    LaunchedEffect(Unit) {
        isLoadingStore = true
        try {
            val user = ServiceLocator.authRepository.getCurrentUserSession()
            if (user == null) {
                errorMessage = if (isArabic) "الرجاء تسجيل الدخول أولاً" else "Please sign in first"
            } else {
                val fetchedStore = ServiceLocator.storeRepository.getStoreByOwnerId(user.id)
                if (fetchedStore == null) {
                    errorMessage = if (isArabic) "لا تملك متجراً بعد! يجب إنشاء متجر أولاً." else "You do not own a store yet. Please create a store first."
                } else {
                    store = fetchedStore
                }
            }
        } catch (e: Exception) {
            errorMessage = e.message ?: "Failed to fetch store details"
        } finally {
            isLoadingStore = false
        }
    }

    val photoLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && imageUris.size < 10) {
            scope.launch {
                isUploadingImage = true
                val uploader = CloudinaryUploader()
                val result = uploader.uploadFile(uri.toString())
                result.onSuccess { url ->
                    imageUris = imageUris + url
                }
                isUploadingImage = false
            }
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "إضافة منتج جديد" else "Add New Product",
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        fontSize = 18.sp
                    )
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandBackground
                ),
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("add_product_back_button")
                    ) {
                        Icon(
                            imageVector = if (isArabic) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                            contentDescription = "السابق",
                            tint = BrandTextPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        if (isLoadingStore) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (errorMessage != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = null,
                        modifier = Modifier.size(64.dp),
                        tint = BrandTextMuted
                    )
                    Text(
                        text = errorMessage ?: "",
                        color = BrandTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    )
                    if (store == null && errorMessage?.contains("متجراً") == true || errorMessage?.contains("store") == true) {
                        Button(
                            onClick = onNavigateToCreateStore,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.height(48.dp)
                        ) {
                            Text(
                                text = if (isArabic) "إنشاء متجر الآن" else "Create Store Now",
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = onNavigateBack,
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = if (isArabic) "العودة" else "Go Back", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        } else if (isSuccess) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(BrandPrimary.copy(alpha = 0.15f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Text(
                        text = if (isArabic) "تمت إضافة المنتج بنجاح!" else "Product Added Successfully!",
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                    Text(
                        text = if (isArabic) "تم إدراج منتجك الجديد في معرض متجرك وهو متاح الآن للمشترين." else "Your new product has been successfully registered and published onto the marketplace.",
                        fontSize = 14.sp,
                        color = BrandTextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                    ) {
                        Text(
                            text = if (isArabic) "الذهاب لمتجري" else "Manage Store Products",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = if (isArabic) "أدخل تفاصيل ومواصفات المنتج لعرضه في WasetPlus" else "Enter item details to represent it within WasetPlus",
                    fontSize = 14.sp,
                    color = BrandTextMuted
                )

                // 1. Product Name Input
                StoreInputField(
                    label = if (isArabic) "اسم المنتج" else "Product Name",
                    value = title,
                    onValueChange = { title = it },
                    placeholder = if (isArabic) "مثال: بساط صوف يدوي" else "e.g., Handcrafted Wool Rug",
                    testTag = "add_product_title_input"
                )

                // 2. Product Price Input
                StoreInputField(
                    label = if (isArabic) "السعر (بالدولار الأمريكي USD)" else "Price (in USD)",
                    value = price,
                    onValueChange = { price = it },
                    placeholder = "0.00",
                    testTag = "add_product_price_input",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )

                // 3. Category Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "الفئة (التصنيف)" else "Category",
                        color = BrandTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandSurface)
                            .border(
                                1.dp,
                                if (selectedCategory != null) BrandPrimary else BrandSoftGray,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showCategoryDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                            .testTag("add_product_category_trigger")
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = if (selectedCategory != null) {
                                    if (isArabic) selectedCategory!!.nameAr else selectedCategory!!.nameEn
                                } else {
                                    if (isArabic) "اختر فئة المنتج" else "Select category"
                                },
                                color = if (selectedCategory != null) BrandTextPrimary else BrandTextMuted,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = null,
                                tint = BrandTextMuted
                            )
                        }
                    }
                }

                // 4. Description Input
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "وصف المنتج" else "Product Description",
                        color = BrandTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = {
                            Text(
                                text = if (isArabic) "اكتب وصفاً تفصيلياً للمنتج..." else "Write a comprehensive detail description...",
                                color = BrandTextMuted,
                                fontSize = 13.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = BrandTextPrimary,
                            unfocusedTextColor = BrandTextPrimary,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = BrandSurface,
                            unfocusedContainerColor = BrandSurface
                        ),
                        shape = RoundedCornerShape(12.dp),
                        minLines = 3,
                        maxLines = 6,
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("add_product_desc_input")
                    )
                }

                // 5. Product Images Upload Grid
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = if (isArabic) "صور المنتج المعروضة" else "Product Gallery Images",
                        color = BrandTextPrimary,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(90.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(BrandSurface)
                                .border(1.dp, BrandSoftGray, RoundedCornerShape(12.dp))
                                .clickable(enabled = !isUploadingImage) {
                                    photoLauncher.launch("image/*")
                                }
                                .testTag("add_product_upload_image_button"),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isUploadingImage) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = BrandPrimary,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Icon(
                                        imageVector = Icons.Default.AddPhotoAlternate,
                                        contentDescription = null,
                                        tint = BrandPrimary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = "${imageUris.size}/10",
                                        fontSize = 11.sp,
                                        color = BrandTextMuted,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                        }

                        if (imageUris.isNotEmpty()) {
                            LazyRow(
                                modifier = Modifier.weight(1f),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(imageUris) { url ->
                                    Box(
                                        modifier = Modifier
                                            .size(90.dp)
                                            .clip(RoundedCornerShape(12.dp))
                                    ) {
                                        AsyncImage(
                                            model = url,
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Crop
                                        )
                                        IconButton(
                                            onClick = { imageUris = imageUris.filter { it != url } },
                                            modifier = Modifier
                                                .align(Alignment.TopEnd)
                                                .padding(4.dp)
                                                .size(24.dp)
                                                .background(Color.Black.copy(alpha = 0.6f), CircleShape)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Close,
                                                contentDescription = "حذف",
                                                tint = Color.White,
                                                modifier = Modifier.size(14.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Submit Button
                Button(
                    onClick = {
                        val parsedPrice = price.toDoubleOrNull()
                        if (title.isBlank()) {
                            errorMessage = if (isArabic) "الرجاء كتاية اسم المنتج" else "Please enter product name"
                            return@Button
                        }
                        if (parsedPrice == null || parsedPrice <= 0.0) {
                            errorMessage = if (isArabic) "الرجاء كتاية سعر صحيح" else "Please enter a valid price"
                            return@Button
                        }
                        if (selectedCategory == null) {
                            errorMessage = if (isArabic) "الرجاء تحديد تصنيف المنتج" else "Please select a category"
                            return@Button
                        }
                        if (desc.isBlank()) {
                            errorMessage = if (isArabic) "الرجاء وصف المنتج" else "Please describe the product"
                            return@Button
                        }
                        if (imageUris.isEmpty()) {
                            errorMessage = if (isArabic) "الرجاء إرفاق صورة واحدة على الأقل" else "Please upload at least one image"
                            return@Button
                        }

                        isSubmitting = true
                        val currentStoreId = store?.id ?: ""
                        
                        scope.launch {
                            val newProduct = Product(
                                id = "",
                                title = title,
                                description = desc,
                                price = parsedPrice,
                                imageUrls = imageUris,
                                categoryId = selectedCategory!!.id,
                                storeId = currentStoreId,
                                rating = 0f,
                                reviewCount = 0,
                                isAvailable = true,
                                stockCount = 100,
                                createdAt = System.currentTimeMillis()
                            )
                            val res = ServiceLocator.productRepository.addProduct(newProduct)
                            isSubmitting = false
                            if (res.isSuccess) {
                                isSuccess = true
                                errorMessage = null
                            } else {
                                errorMessage = res.exceptionOrNull()?.message ?: "Failed downstream"
                            }
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                        .testTag("add_product_submit_button"),
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(12.dp),
                    enabled = !isSubmitting
                ) {
                    if (isSubmitting) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text(
                            text = if (isArabic) "عرض وإدراج المنتج" else "Publish Item Now",
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
                        text = if (isArabic) "اختر فئة المنتج" else "Select Product Category",
                        color = BrandTextPrimary,
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
