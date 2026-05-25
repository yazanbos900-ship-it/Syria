package com.example.features.marketplace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.domain.model.Category
import com.example.domain.model.User
import kotlinx.coroutines.delay

// Premium Dark Theme Palette Specific to WasetPlus Store Setup
private val DarkBg = Color(0xFF0A0B0D)
private val DarkCard = Color(0xFF13151A)
private val PrimaryGreen = Color(0xFF1DB954)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF9E9E9E)
private val BorderColor = Color(0xFF22262F)

@OptIn(ExperimentalAnimationApi::class, ExperimentalMaterial3Api::class)
@Composable
fun CreateStoreScreen(
    onNavigateHome: () -> Unit,
    viewModel: CreateStoreViewModel = viewModel()
) {
    val state by viewModel.state.collectAsState()
    val androidContext = LocalContext.current

    // Force RTL direction for fluent Arabic onboarding
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        
        if (state.isSuccess) {
            SuccessScreen(onNavigateHome = {
                viewModel.resetState()
                onNavigateHome()
            })
        } else {
            Scaffold(
                containerColor = DarkBg,
                contentWindowInsets = WindowInsets.safeDrawing,
                topBar = {
                    CenterAlignedTopAppBar(
                        title = {
                            Text(
                                text = stringResource(id = R.string.create_store_title),
                                color = TextWhite,
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold
                            )
                        },
                        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                            containerColor = DarkBg
                        ),
                        navigationIcon = {
                            IconButton(onClick = {
                                if (state.currentStep > 1) {
                                    viewModel.prevStep()
                                } else {
                                    onNavigateHome()
                                }
                            }) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                    contentDescription = "السابق",
                                    tint = TextWhite
                                )
                            }
                        }
                    )
                }
            ) { innerPadding ->
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(DarkBg)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // 1. STEP PROGRESS HEADER INDICATORS
                        StepIndicatorsHeader(currentStep = state.currentStep)

                        Spacer(modifier = Modifier.height(24.dp))

                        // 2. ERROR SNACKBAR SIMULATOR / DYNAMIC NOTIFICATION CARD
                        if (state.error != null) {
                            Card(
                                colors = CardDefaults.cardColors(containerColor = Color(0xFF3B1519)),
                                border = BorderStroke(1.dp, Color(0xFF7A1F26)),
                                shape = RoundedCornerShape(12.dp),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(bottom = 16.dp)
                                    .clickable { viewModel.clearError() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Info,
                                        contentDescription = null,
                                        tint = Color(0xFFFF5252),
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(12.dp))
                                    Text(
                                        text = state.error ?: "",
                                        color = Color(0xFFFFCDD2),
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Medium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    Icon(
                                        imageVector = Icons.Default.Close,
                                        contentDescription = "إغلاق",
                                        tint = Color(0xFFFFCDD2),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // 3. MAIN FORM STEP ANIMATOR
                        AnimatedContent(
                            targetState = state.currentStep,
                            transitionSpec = {
                                if (targetState > initialState) {
                                    slideInHorizontally(animationSpec = tween(300)) { -it } + fadeIn() with
                                            slideOutHorizontally(animationSpec = tween(300)) { it } + fadeOut()
                                } else {
                                    slideInHorizontally(animationSpec = tween(300)) { it } + fadeIn() with
                                            slideOutHorizontally(animationSpec = tween(300)) { -it } + fadeOut()
                                }
                            },
                            modifier = Modifier.weight(1f, fill = false)
                        ) { step ->
                            when (step) {
                                1 -> Step1StoreInfo(viewModel = viewModel, state = state)
                                2 -> Step2StoreIdentity(viewModel = viewModel, state = state)
                                3 -> Step3FirstProduct(viewModel = viewModel, state = state)
                            }
                        }

                        Spacer(modifier = Modifier.height(30.dp))

                        // 4. ACTION BUTTONS FOOTER
                        StepActionFooter(
                            currentStep = state.currentStep,
                            isLoading = state.isLoading,
                            isStep1Valid = state.isStep1Valid,
                            isStep2Valid = state.isStep2Valid,
                            isStep3Valid = state.isStep3Valid,
                            onNext = { viewModel.nextStep() },
                            onBack = { viewModel.prevStep() },
                            onSubmit = { viewModel.submitStore() }
                        )
                    }

                    // 5. GLOBAL FULL SCREEN SUBMISSION LOCK LOADER
                    if (state.isLoading) {
                        Surface(
                            color = Color.Black.copy(alpha = 0.8f),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            Column(
                                modifier = Modifier.fillMaxSize(),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                CircularProgressIndicator(
                                    color = PrimaryGreen,
                                    strokeWidth = 4.dp,
                                    modifier = Modifier.size(54.dp)
                                )
                                Spacer(modifier = Modifier.height(20.dp))
                                Text(
                                    text = stringResource(id = R.string.submitting_loading),
                                    color = TextWhite,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    textAlign = TextAlign.Center,
                                    modifier = Modifier.padding(horizontal = 32.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StepIndicatorsHeader(currentStep: Int) {
    val animatedProgress by animateFloatAsState(
        targetValue = when (currentStep) {
            1 -> 0.33f
            2 -> 0.66f
            else -> 1f
        },
        animationSpec = tween(400),
        label = "Progress"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(DarkCard)
            .border(1.dp, BorderColor, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            StepHeaderItem(
                stepNum = 1,
                label = stringResource(id = R.string.store_info_step),
                isActive = currentStep >= 1,
                isCompleted = currentStep > 1
            )
            Text(
                text = "←",
                color = if (currentStep > 1) PrimaryGreen else BorderColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            StepHeaderItem(
                stepNum = 2,
                label = stringResource(id = R.string.store_identity_step),
                isActive = currentStep >= 2,
                isCompleted = currentStep > 2
            )
            Text(
                text = "←",
                color = if (currentStep > 2) PrimaryGreen else BorderColor,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
            StepHeaderItem(
                stepNum = 3,
                label = stringResource(id = R.string.first_product_step),
                isActive = currentStep >= 3,
                isCompleted = false
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Linear Progress Bar matching % milestones
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(4.dp)
                .clip(CircleShape)
                .background(BorderColor)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth(animatedProgress)
                    .clip(CircleShape)
                    .background(PrimaryGreen)
            )
        }
    }
}

@Composable
fun StepHeaderItem(
    stepNum: Int,
    label: String,
    isActive: Boolean,
    isCompleted: Boolean
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(24.dp)
                .clip(CircleShape)
                .background(
                    if (isCompleted) PrimaryGreen 
                    else if (isActive) PrimaryGreen.copy(alpha = 0.2f) 
                    else Color.Transparent
                )
                .border(
                    width = 1.5.dp,
                    color = if (isActive || isCompleted) PrimaryGreen else BorderColor,
                    shape = CircleShape
                ),
            contentAlignment = Alignment.Center
        ) {
            if (isCompleted) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = DarkBg,
                    modifier = Modifier.size(14.dp)
                )
            } else {
                Text(
                    text = "$stepNum",
                    color = if (isActive) PrimaryGreen else TextGray,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        Text(
            text = label,
            color = if (isActive) TextWhite else TextGray,
            fontSize = 12.sp,
            fontWeight = if (isActive) FontWeight.Bold else FontWeight.Medium
        )
    }
}

@Composable
fun Step1StoreInfo(
    viewModel: CreateStoreViewModel,
    state: StoreUiState
) {
    var showCategoryDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Field 1: Store Name
        StoreInputField(
            label = stringResource(id = R.string.store_name_label),
            value = state.storeName,
            onValueChange = { viewModel.onStoreNameChange(it) },
            placeholder = stringResource(id = R.string.store_name_placeholder),
            testTag = "store_name_input",
            helperText = "${state.storeName.length}/50",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        // Field 2: Read-Only Authenticated Username prefix
        val rawName = state.currentUser?.name ?: "user"
        val displayUsername = "@${rawName.replace("\\s+".toRegex(), "").lowercase()}"
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.username_label),
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .border(1.dp, BorderColor, RoundedCornerShape(12.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.AccountCircle,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = displayUsername,
                        color = TextWhite.copy(alpha = 0.8f),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "تعبئة تلقائية مقفلة",
                        tint = TextGray,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }

        // Field 3: Category Selection from Firestore kolekation
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.category_label),
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(DarkCard)
                    .border(1.dp, if (state.categoryId.isNotBlank()) PrimaryGreen else BorderColor, RoundedCornerShape(12.dp))
                    .clickable { showCategoryDialog = true }
                    .padding(horizontal = 16.dp, vertical = 14.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Menu,
                        contentDescription = null,
                        tint = if (state.categoryId.isNotBlank()) PrimaryGreen else TextGray,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = if (state.categoryId.isNotBlank()) state.categoryName else stringResource(id = R.string.category_placeholder),
                        color = if (state.categoryId.isNotBlank()) TextWhite else TextGray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.weight(1f))
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = "خيارات الفئات",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Field 4: Store Description with LIVE character counter
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Text(
                    text = stringResource(id = R.string.store_description_label),
                    color = TextWhite,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(bottom = 6.dp)
                )
                Text(
                    text = "${state.storeDescription.length} / 500",
                    color = if (state.storeDescription.length >= 50) PrimaryGreen else Color.Red,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
            OutlinedTextField(
                value = state.storeDescription,
                onValueChange = { if (it.length <= 500) viewModel.onStoreDescriptionChange(it) },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.store_description_placeholder),
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 4,
                maxLines = 6,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("store_description_input")
            )
        }
    }

    // Dynamic Category Selection Modal Dialog
    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    Text(
                        text = stringResource(id = R.string.category_label),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )

                    if (state.isCategoriesLoading) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(150.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = PrimaryGreen)
                        }
                    } else if (state.categories.isEmpty()) {
                        // Resilient Fallback categories list matching search/home collection metrics
                        val fallbackCategories = listOf<Category>(
                            Category("bespoke", "شخصي ومميز"),
                            Category("apparel", "الملابس والأناقة"),
                            Category("furniture", "أثاث منزلي"),
                            Category("artisanal", "منتجات يدوية"),
                            Category("wellness", "صحة وعناية")
                        )
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 250.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            fallbackCategories.forEach { category ->
                                CategorySelectionRow(
                                    name = category.name,
                                    isSelected = state.categoryId == category.id,
                                    onClick = {
                                        viewModel.onCategorySelected(category.id, category.name)
                                        showCategoryDialog = false
                                    }
                                )
                            }
                        }
                    } else {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 300.dp)
                                .verticalScroll(rememberScrollState()),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            state.categories.forEach { category ->
                                CategorySelectionRow(
                                    name = category.name,
                                    isSelected = state.categoryId == category.id,
                                    onClick = {
                                        viewModel.onCategorySelected(category.id, category.name)
                                        showCategoryDialog = false
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

@Composable
fun CategorySelectionRow(
    name: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) PrimaryGreen.copy(alpha = 0.15f) else Color.Transparent)
            .border(1.dp, if (isSelected) PrimaryGreen else BorderColor, RoundedCornerShape(10.dp))
            .clickable { onClick() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = name,
                color = if (isSelected) PrimaryGreen else TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.weight(1f))
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun Step2StoreIdentity(
    viewModel: CreateStoreViewModel,
    state: StoreUiState
) {
    val androidContext = LocalContext.current
    val logoPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onLogoSelected(uri.toString())
    }

    val bannerPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onBannerSelected(uri.toString())
    }

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // --- 1. Circular Store Logo Upload ---
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.store_logo_label),
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            Text(
                text = stringResource(id = R.string.store_logo_helper),
                color = TextGray,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 2.dp, bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(DarkCard)
                    .border(2.dp, if (state.logoUriString != null) PrimaryGreen else BorderColor, CircleShape)
                    .clickable { logoPicker.launch("image/*") }
                    .testTag("logo_upload_trigger"),
                contentAlignment = Alignment.Center
            ) {
                if (state.logoUriString != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = state.logoUriString,
                            contentDescription = "شعار المتجر",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Floating Clear button inside
                        Box(
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                .fillMaxWidth()
                                .background(Color.Black.copy(alpha = 0.6f))
                                .clickable { viewModel.onLogoSelected(null) }
                                .padding(vertical = 4.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                stringResource(id = R.string.delete_image),
                                color = Color.Red,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                } else {
                    Icon(
                        imageVector = Icons.Default.AddCircle,
                        contentDescription = "رفع شعار المتجر",
                        tint = PrimaryGreen,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }

        Divider(color = BorderColor, modifier = Modifier.padding(vertical = 8.dp))

        // --- 2. Store Banner 16:9 Upload ---
        Column(
            horizontalAlignment = Alignment.Start,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = stringResource(id = R.string.store_banner_label),
                color = TextWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 4.dp)
            )
            Text(
                text = stringResource(id = R.string.store_banner_helper),
                color = TextGray,
                fontSize = 11.sp,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(16f / 9f)
                    .clip(RoundedCornerShape(14.dp))
                    .background(DarkCard)
                    .border(1.dp, if (state.bannerUriString != null) PrimaryGreen else BorderColor, RoundedCornerShape(14.dp))
                    .clickable { bannerPicker.launch("image/*") }
                    .testTag("banner_upload_trigger"),
                contentAlignment = Alignment.Center
            ) {
                if (state.bannerUriString != null) {
                    Box(modifier = Modifier.fillMaxSize()) {
                        AsyncImage(
                            model = state.bannerUriString,
                            contentDescription = "غلاف المتجر",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        IconButton(
                            onClick = { viewModel.onBannerSelected(null) },
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(8.dp)
                                .clip(CircleShape)
                                .background(Color.Black.copy(alpha = 0.7f))
                                .size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "حذف الغلاف",
                                tint = Color.Red,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(28.dp)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "رفع غلاف المتجر الرئيسي",
                            color = TextGray,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun Step3FirstProduct(
    viewModel: CreateStoreViewModel,
    state: StoreUiState
) {
    val photoGridPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) viewModel.onAddProductImage(uri.toString())
    }

    val androidContext = LocalContext.current

    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Product Line 1: Title
        StoreInputField(
            label = stringResource(id = R.string.product_name_label),
            value = state.productName,
            onValueChange = { viewModel.onProductNameChange(it) },
            placeholder = stringResource(id = R.string.product_name_placeholder),
            testTag = "first_product_name",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text)
        )

        // Product Line 2: Price
        StoreInputField(
            label = stringResource(id = R.string.product_price_label),
            value = state.productPrice,
            onValueChange = { viewModel.onProductPriceChange(it) },
            placeholder = stringResource(id = R.string.product_price_placeholder),
            testTag = "first_product_price",
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
        )

        // Product Line 3: Description
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.product_description_label),
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            OutlinedTextField(
                value = state.productDescription,
                onValueChange = { viewModel.onProductDescriptionChange(it) },
                placeholder = {
                    Text(
                        text = stringResource(id = R.string.product_description_placeholder),
                        color = TextGray,
                        fontSize = 13.sp
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = TextWhite,
                    unfocusedTextColor = TextWhite,
                    focusedBorderColor = PrimaryGreen,
                    unfocusedBorderColor = BorderColor,
                    focusedContainerColor = DarkCard,
                    unfocusedContainerColor = DarkCard
                ),
                shape = RoundedCornerShape(12.dp),
                minLines = 3,
                maxLines = 5,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("first_product_description")
            )
        }

        // Product Line 4: Images grid (1 to 10 images)
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = stringResource(id = R.string.product_images_label),
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 8.dp)
            )

            // 3-Column Product images preview grid
            val selectBtnCount = if (state.productImageUris.size < 10) 1 else 0
            val totalItems = state.productImageUris.size + selectBtnCount
            val gridSpacing = 8.dp

            Column(verticalArrangement = Arrangement.spacedBy(gridSpacing)) {
                // Chunk into rows of 3
                val chunks = (0 until totalItems).chunked(3)
                chunks.forEach { rowIndices ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(gridSpacing)
                    ) {
                        for (i in 0 until 3) {
                            val itemIndex = rowIndices.getOrNull(i)
                            if (itemIndex != null) {
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                ) {
                                    if (itemIndex < state.productImageUris.size) {
                                        // Uploaded thumbnail selection
                                        val imgUri = state.productImageUris[itemIndex]
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .border(2.dp, if (itemIndex == 0) PrimaryGreen else BorderColor, RoundedCornerShape(10.dp))
                                        ) {
                                            AsyncImage(
                                                model = imgUri,
                                                contentDescription = "Thumbnail $itemIndex",
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )

                                            // Main Image cover indicator
                                            if (itemIndex == 0) {
                                                Box(
                                                    modifier = Modifier
                                                        .align(Alignment.BottomStart)
                                                        .fillMaxWidth()
                                                        .background(PrimaryGreen.copy(alpha = 0.9f))
                                                        .padding(vertical = 2.dp)
                                                ) {
                                                    Text(
                                                        text = stringResource(id = R.string.cover_image_badge),
                                                        color = DarkBg,
                                                        fontSize = 8.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        textAlign = TextAlign.Center,
                                                        modifier = Modifier.fillMaxWidth()
                                                    )
                                                }
                                            }

                                            // Delete icon
                                            IconButton(
                                                onClick = { viewModel.onRemoveProductImage(imgUri) },
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(2.dp)
                                                    .size(20.dp)
                                                    .clip(CircleShape)
                                                    .background(Color.Black.copy(alpha = 0.6f))
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.Close,
                                                    contentDescription = "حذف الصورة",
                                                    tint = Color.Red,
                                                    modifier = Modifier.size(12.dp)
                                                )
                                            }
                                        }
                                    } else {
                                        // "+ إضافة صورة" (Add Image) selection button
                                        Box(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(DarkCard)
                                                .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                                .clickable { photoGridPicker.launch("image/*") }
                                                .testTag("add_product_image_btn"),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                Icon(
                                                    imageVector = Icons.Default.Add,
                                                    contentDescription = null,
                                                    tint = PrimaryGreen,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = stringResource(id = R.string.add_image_button),
                                                    color = TextGray,
                                                    fontSize = 11.sp,
                                                    fontWeight = FontWeight.Bold
                                                )
                                            }
                                        }
                                    }
                                }
                            } else {
                                Spacer(
                                    modifier = Modifier
                                        .weight(1f)
                                        .aspectRatio(1f)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StoreInputField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    testTag: String,
    helperText: String? = null,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                color = TextWhite,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 6.dp)
            )
            if (helperText != null) {
                Text(
                    text = helperText,
                    color = TextGray,
                    fontSize = 11.sp
                )
            }
        }
        OutlinedTextField(
            value = value,
            onValueChange = onValueChange,
            placeholder = { Text(text = placeholder, color = TextGray, fontSize = 13.sp) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedTextColor = TextWhite,
                unfocusedTextColor = TextWhite,
                focusedBorderColor = PrimaryGreen,
                unfocusedBorderColor = BorderColor,
                focusedContainerColor = DarkCard,
                unfocusedContainerColor = DarkCard
            ),
            shape = RoundedCornerShape(12.dp),
            singleLine = true,
            keyboardOptions = keyboardOptions,
            modifier = Modifier
                .fillMaxWidth()
                .testTag(testTag)
        )
    }
}

@Composable
fun StepActionFooter(
    currentStep: Int,
    isLoading: Boolean,
    isStep1Valid: Boolean,
    isStep2Valid: Boolean,
    isStep3Valid: Boolean,
    onNext: () -> Unit,
    onBack: () -> Unit,
    onSubmit: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back buttons conditional rendering
        if (currentStep > 1) {
            Button(
                onClick = onBack,
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color.Transparent,
                    contentColor = TextWhite
                ),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier
                    .weight(1f)
                    .height(46.dp)
            ) {
                Text(
                    text = stringResource(id = R.string.back_button),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Logic check to trigger Step transitions vs Final Firestore commits
        val isCurrentStepValid = when (currentStep) {
            1 -> isStep1Valid
            2 -> isStep2Valid
            else -> isStep3Valid
        }

        Button(
            onClick = {
                if (currentStep < 3) {
                    onNext()
                } else {
                    onSubmit()
                }
            },
            enabled = isCurrentStepValid && !isLoading,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isCurrentStepValid) PrimaryGreen else Color(0xFF163820),
                contentColor = if (isCurrentStepValid) DarkBg else TextGray
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .weight(2f)
                .height(46.dp)
                .testTag("step_action_main_btn")
        ) {
            Text(
                text = if (currentStep < 3) stringResource(id = R.string.next_button) else stringResource(id = R.string.submit_button),
                fontSize = 14.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

// Success onboarding Screen Composable
@Composable
fun SuccessScreen(
    onNavigateHome: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Visual green circular checkmark
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(PrimaryGreen.copy(alpha = 0.15f))
                    .border(2.dp, PrimaryGreen, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = PrimaryGreen,
                    modifier = Modifier.size(48.dp)
                )
            }

            Spacer(modifier = Modifier.height(28.dp))

            Text(
                text = "تم إنشاء متجرك بنجاح!",
                color = TextWhite,
                fontSize = 20.sp,
                fontWeight = FontWeight.ExtraBold,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "يمكنك البدء بالبيع الآن",
                color = PrimaryGreen,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 16.dp)
            )

            Spacer(modifier = Modifier.height(36.dp))

            Button(
                onClick = onNavigateHome,
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryGreen,
                    contentColor = DarkBg
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth(0.85f)
                    .height(50.dp)
                    .testTag("success_confirm_home")
            ) {
                Text(
                    text = stringResource(id = R.string.go_to_home_button),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
    }
}
