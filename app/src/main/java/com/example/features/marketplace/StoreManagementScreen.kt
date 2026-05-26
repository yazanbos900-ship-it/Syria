package com.example.features.marketplace

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.res.stringResource
import com.example.R
import com.example.core.utils.LanguageManager
import com.example.domain.model.Category
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.border
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.components.*
import com.example.core.di.ServiceLocator
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.ui.theme.*
import kotlinx.coroutines.launch

// Premium Dark Theme Palette (Matching CreateStoreScreen)
private val DarkBg = Color(0xFF0A0B0D)
private val DarkCard = Color(0xFF13151A)
private val PrimaryGreen = Color(0xFF1DB954)
private val TextWhite = Color(0xFFFFFFFF)
private val TextGray = Color(0xFF9E9E9E)
private val BorderColor = Color(0xFF22262F)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StoreManagementScreen(
    onBack: () -> Unit,
    onEditProduct: (Product) -> Unit
) {
    val viewModel: StoreManagementViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return StoreManagementViewModel(
                ServiceLocator.storeRepository,
                ServiceLocator.productRepository,
                ServiceLocator.authRepository
            ) as T
        }
    })

    val state by viewModel.state.collectAsStateWithLifecycle()
    var showAddProductDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        viewModel.loadStoreAndProducts()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(androidx.compose.ui.res.stringResource(R.string.store_management_title), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, androidx.compose.ui.res.stringResource(R.string.back_button))
                    }
                },
                actions = {
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, androidx.compose.ui.res.stringResource(R.string.store_settings_title))
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddProductDialog = true },
                containerColor = BrandPrimary,
                contentColor = Color.White
            ) {
                Icon(Icons.Default.Add, "Add Product")
            }
        },
        containerColor = BrandBackground
    ) { innerPadding ->
        if (state.isLoading && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (state.error != null && state.store == null) {
            Box(Modifier.fillMaxSize().padding(innerPadding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(state.error!!, color = Color.Red)
                    Spacer(Modifier.height(16.dp))
                    Button(onClick = { viewModel.loadStoreAndProducts() }) { Text("Retry") }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(innerPadding),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Store Summary Header
                item {
                    state.store?.let { store ->
                        StoreHeaderCard(store)
                    }
                }

                item {
                    Text(
                        androidx.compose.ui.res.stringResource(R.string.manage_products_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                if (state.products.isEmpty()) {
                    item {
                        Box(
                            Modifier.fillMaxWidth().height(200.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(androidx.compose.ui.res.stringResource(R.string.no_products_yet), color = BrandTextMuted)
                        }
                    }
                } else {
                    items(state.products) { product ->
                        ProductManagementItem(
                            product = product,
                            onEdit = { onEditProduct(product) },
                            onDelete = { viewModel.deleteProduct(product.id) }
                        )
                    }
                }
            }
        }
    }

    if (showAddProductDialog) {
        AddProductDialog(
            onDismiss = { showAddProductDialog = false },
            onAdd = { title, price, desc, categoryId, imageUris ->
                viewModel.addProduct(title, price, desc, imageUris, categoryId)
                showAddProductDialog = false
            }
        )
    }

    if (showSettingsDialog && state.store != null) {
        StoreSettingsDialog(
            store = state.store!!,
            onDismiss = { showSettingsDialog = false },
            onSave = { name, desc, categoryId, logoUrl, bannerUrl ->
                viewModel.updateStore(name, desc, categoryId, logoUrl, bannerUrl)
                showSettingsDialog = false
            }
        )
    }
}

@Composable
fun StoreHeaderCard(store: Store) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = store.logoUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp).clip(CircleShape).background(BrandSoftGray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column {
                Text(store.name, fontWeight = FontWeight.Bold, fontSize = 20.sp)
                Text(store.description, maxLines = 2, overflow = TextOverflow.Ellipsis, color = BrandTextMuted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Person, null, modifier = Modifier.size(16.dp), tint = BrandPrimary)
                    Spacer(Modifier.width(4.dp))
                    Text(androidx.compose.ui.res.stringResource(R.string.followers_count, store.followersCount), fontSize = 12.sp, color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
fun ProductManagementItem(
    product: Product,
    onEdit: () -> Unit,
    onDelete: () -> Unit
) {
    BrandCard(modifier = Modifier.fillMaxWidth()) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            AsyncImage(
                model = product.imageUrls.firstOrNull(),
                contentDescription = null,
                modifier = Modifier.size(80.dp).clip(RoundedCornerShape(8.dp)).background(BrandSoftGray),
                contentScale = ContentScale.Crop
            )
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(product.title, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis)
                Text("$${product.price}", color = BrandPrimary, fontWeight = FontWeight.Bold)
                Text(androidx.compose.ui.res.stringResource(R.string.stock_count, product.stockCount), fontSize = 12.sp, color = BrandTextMuted)
            }
            Row {
                IconButton(onClick = onEdit) {
                    Icon(Icons.Default.Edit, "Edit", tint = Color.Gray)
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, "Delete", tint = Color.Red.copy(alpha = 0.7f))
                }
            }
        }
    }
}

@Composable
fun AddProductDialog(
    onDismiss: () -> Unit,
    onAdd: (String, Double, String, String, List<String>) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var price by remember { mutableStateOf("") }
    var desc by remember { mutableStateOf("") }
    var imageUris by remember { mutableStateOf<List<String>>(emptyList()) }
    var isUploading by remember { mutableStateOf(false) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf<com.example.domain.model.Category?>(null) }

    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val photoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null && imageUris.size < 10) {
            scope.launch {
                isUploading = true
                val uploader = com.example.features.marketplace.CloudinaryUploader()
                val result = uploader.uploadFile(uri.toString())
                result.onSuccess { url ->
                    imageUris = imageUris + url
                }
                isUploading = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Title
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.add_product_dialog_title),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Product Name
                StoreInputField(
                    label = androidx.compose.ui.res.stringResource(R.string.product_name_label),
                    value = title,
                    onValueChange = { title = it },
                    placeholder = androidx.compose.ui.res.stringResource(R.string.product_name_placeholder),
                    testTag = "add_product_name",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    helperText = null
                )

                // Product Price
                StoreInputField(
                    label = androidx.compose.ui.res.stringResource(R.string.product_price_label),
                    value = price,
                    onValueChange = { price = it },
                    placeholder = "0.00",
                    testTag = "add_product_price",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    helperText = null
                )

                // Product Description
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.product_description_label),
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(R.string.product_description_placeholder), color = TextGray, fontSize = 13.sp) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.category_label),
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
                            .border(
                                1.dp,
                                if (selectedCategory != null) PrimaryGreen else BorderColor,
                                RoundedCornerShape(12.dp)
                            )
                            .clickable { showCategoryDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isArabic = LanguageManager.isArabic(context)
                            Text(
                                text = selectedCategory?.getName(isArabic) ?: androidx.compose.ui.res.stringResource(R.string.select_category),
                                color = if (selectedCategory != null) TextWhite else TextGray,
                                fontSize = 14.sp
                            )
                            Icon(
                                imageVector = Icons.Default.ArrowDropDown,
                                contentDescription = null,
                                tint = if (selectedCategory != null) PrimaryGreen else TextGray
                            )
                        }
                    }
                }

                // Images Grid
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.product_images_count, imageUris.size),
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    val showAddBtn = imageUris.size < 10
                    val totalItems = imageUris.size + (if (showAddBtn) 1 else 0)
                    val chunks = (0 until totalItems).chunked(3)

                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        chunks.forEach { rowIndices ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                for (i in 0 until 3) {
                                    val itemIndex = rowIndices.getOrNull(i)
                                    Box(modifier = Modifier.weight(1f).aspectRatio(1f)) {
                                        if (itemIndex != null) {
                                            if (itemIndex < imageUris.size) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .border(
                                                            2.dp,
                                                            if (itemIndex == 0) PrimaryGreen else BorderColor,
                                                            RoundedCornerShape(10.dp)
                                                        )
                                                ) {
                                                    AsyncImage(
                                                        model = imageUris[itemIndex],
                                                        contentDescription = null,
                                                        contentScale = ContentScale.Crop,
                                                        modifier = Modifier.fillMaxSize()
                                                    )
                                                    if (itemIndex == 0) {
                                                        Box(
                                                            modifier = Modifier
                                                                .align(Alignment.BottomStart)
                                                                .fillMaxWidth()
                                                                .background(PrimaryGreen.copy(alpha = 0.9f))
                                                                .padding(vertical = 2.dp)
                                                        ) {
                                                            Text(
                                                                text = androidx.compose.ui.res.stringResource(R.string.main_image_label),
                                                                color = DarkBg,
                                                                fontSize = 8.sp,
                                                                fontWeight = FontWeight.Bold,
                                                                textAlign = TextAlign.Center,
                                                                modifier = Modifier.fillMaxWidth()
                                                            )
                                                        }
                                                    }
                                                    IconButton(
                                                        onClick = { 
                                                            imageUris = imageUris.toMutableList()
                                                                .also { it.removeAt(itemIndex) }
                                                        },
                                                        modifier = Modifier
                                                            .align(Alignment.TopEnd)
                                                            .padding(2.dp)
                                                            .size(20.dp)
                                                            .clip(CircleShape)
                                                            .background(Color.Black.copy(alpha = 0.6f))
                                                    ) {
                                                        Icon(
                                                            Icons.Default.Close,
                                                            contentDescription = null,
                                                            tint = Color.Red,
                                                            modifier = Modifier.size(12.dp)
                                                        )
                                                    }
                                                }
                                            } else {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .clip(RoundedCornerShape(10.dp))
                                                        .background(DarkCard)
                                                        .border(1.dp, BorderColor, RoundedCornerShape(10.dp))
                                                        .clickable { 
                                                            if (!isUploading) photoPickerLauncher.launch("image/*") 
                                                        },
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    if (isUploading) {
                                                        CircularProgressIndicator(
                                                            color = PrimaryGreen,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    } else {
                                                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                                            Icon(
                                                                Icons.Default.Add,
                                                                contentDescription = null,
                                                                tint = PrimaryGreen,
                                                                modifier = Modifier.size(20.dp)
                                                            )
                                                            Spacer(Modifier.height(2.dp))
                                                            Text(
                                                                text = androidx.compose.ui.res.stringResource(R.string.add_label),
                                                                color = TextGray,
                                                                fontSize = 11.sp,
                                                                fontWeight = FontWeight.Bold
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        } else {
                                            Spacer(Modifier.fillMaxSize())
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Buttons Row
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text("إلغاء", color = TextGray)
                    }
                    Button(
                        onClick = {
                            val priceVal = price.toDoubleOrNull() ?: 0.0
                            if (title.isNotBlank() && priceVal > 0 && 
                                selectedCategory != null && imageUris.isNotEmpty()) {
                                onAdd(title, priceVal, desc, selectedCategory!!.id, imageUris)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = title.isNotBlank() && 
                                  (price.toDoubleOrNull() ?: 0.0) > 0 && 
                                  selectedCategory != null && 
                                  imageUris.isNotEmpty() &&
                                  !isUploading
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.add_product_button), color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    // Category Dialog for AddProductDialog
    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val isArabic = LanguageManager.isArabic(context)
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.select_category),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            CategorySelectionRow(
                                name = category.getName(isArabic),
                                isSelected = selectedCategory?.id == category.id,
                                onClick = {
                                    selectedCategory = category
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

@Composable
fun StoreSettingsDialog(
    store: Store,
    onDismiss: () -> Unit,
    onSave: (String, String, String, String?, String?) -> Unit
) {
    var name by remember { mutableStateOf(store.name) }
    var desc by remember { mutableStateOf(store.description) }
    var logoUrl by remember { mutableStateOf(store.logoUrl) }
    var bannerUrl by remember { mutableStateOf(store.bannerUrl) }
    
    var isUploadingLogo by remember { mutableStateOf(false) }
    var isUploadingBanner by remember { mutableStateOf(false) }

    val categories = SharedFilterState.categoriesList.filter { it.id != "All" }
    var selectedCategory by remember { mutableStateOf(categories.find { it.id == store.categoryId } ?: categories.firstOrNull()) }
    var showCategoryDialog by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    val logoPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploadingLogo = true
                val uploader = CloudinaryUploader()
                uploader.uploadFile(it.toString()).onSuccess { url ->
                    logoUrl = url
                }
                isUploadingLogo = false
            }
        }
    }

    val bannerPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            scope.launch {
                isUploadingBanner = true
                val uploader = CloudinaryUploader()
                uploader.uploadFile(it.toString()).onSuccess { url ->
                    bannerUrl = url
                }
                isUploadingBanner = false
            }
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Surface(
            color = DarkCard,
            shape = RoundedCornerShape(20.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(20.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = androidx.compose.ui.res.stringResource(R.string.store_settings_title),
                    color = TextWhite,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                // Media Section: Banner & Logo
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                ) {
                    // Banner
                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp)
                            .clickable { if (!isUploadingBanner) bannerPickerLauncher.launch("image/*") },
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            AsyncImage(
                                model = bannerUrl ?: store.bannerUrl,
                                contentDescription = "Store Banner",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            if (isUploadingBanner) {
                                CircularProgressIndicator(color = PrimaryGreen)
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.3f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Edit, null, tint = Color.White)
                                }
                            }
                        }
                    }

                    // Logo Overlay
                    Box(
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(DarkCard)
                            .border(2.dp, BorderColor, CircleShape)
                            .clickable { if (!isUploadingLogo) logoPickerLauncher.launch("image/*") },
                        contentAlignment = Alignment.Center
                    ) {
                        AsyncImage(
                            model = logoUrl ?: store.logoUrl,
                            contentDescription = "Store Logo",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        if (isUploadingLogo) {
                            CircularProgressIndicator(color = PrimaryGreen, modifier = Modifier.size(24.dp))
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(Color.Black.copy(alpha = 0.3f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(Icons.Default.Edit, null, tint = Color.White, modifier = Modifier.size(20.dp))
                            }
                        }
                    }
                }

                // Text Fields
                StoreInputField(
                    label = androidx.compose.ui.res.stringResource(R.string.store_name_label),
                    value = name,
                    onValueChange = { name = it },
                    placeholder = androidx.compose.ui.res.stringResource(R.string.store_name_placeholder),
                    testTag = "edit_store_name",
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                    helperText = null
                )

                // Store Description
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.about_store_label),
                        color = TextWhite,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                    OutlinedTextField(
                        value = desc,
                        onValueChange = { desc = it },
                        placeholder = { Text(androidx.compose.ui.res.stringResource(R.string.store_description_placeholder), color = TextGray, fontSize = 13.sp) },
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
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                // Category Selector
                Column(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.category_label),
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
                            .clickable { showCategoryDialog = true }
                            .padding(horizontal = 16.dp, vertical = 14.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            val isArabic = LanguageManager.isArabic(LocalContext.current)
                            Text(
                                text = selectedCategory?.getName(isArabic) ?: androidx.compose.ui.res.stringResource(R.string.select_category),
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                            Icon(Icons.Default.ArrowDropDown, null, tint = PrimaryGreen)
                        }
                    }
                }

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        border = BorderStroke(1.dp, BorderColor)
                    ) {
                        Text(androidx.compose.ui.res.stringResource(R.string.cancel), color = TextGray)
                    }
                    Button(
                        onClick = {
                            if (name.isNotBlank() && desc.isNotBlank() && selectedCategory != null) {
                                onSave(name, desc, selectedCategory!!.id, logoUrl, bannerUrl)
                            }
                        },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                        enabled = name.isNotBlank() && desc.isNotBlank() && !isUploadingLogo && !isUploadingBanner
                    ) {
                        val isSaving = isUploadingLogo || isUploadingBanner
                        Text(if (isSaving) androidx.compose.ui.res.stringResource(R.string.saving_label) else androidx.compose.ui.res.stringResource(R.string.save_changes_button), color = DarkBg, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    if (showCategoryDialog) {
        Dialog(onDismissRequest = { showCategoryDialog = false }) {
            Surface(
                color = DarkCard,
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, BorderColor),
                modifier = Modifier.fillMaxWidth().padding(vertical = 24.dp)
            ) {
                Column(modifier = Modifier.padding(20.dp)) {
                    val isArabic = LanguageManager.isArabic(LocalContext.current)
                    Text(
                        text = androidx.compose.ui.res.stringResource(R.string.select_category),
                        color = TextWhite,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.ExtraBold,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 300.dp)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        categories.forEach { category ->
                            CategorySelectionRow(
                                name = category.getName(isArabic),
                                isSelected = selectedCategory?.id == category.id,
                                onClick = {
                                    selectedCategory = category
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

@Composable
private fun CategorySelectionRowInManagement(
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
