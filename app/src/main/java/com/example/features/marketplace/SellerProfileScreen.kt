package com.example.features.marketplace

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import coil.compose.AsyncImage
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.domain.model.Product
import com.example.ui.theme.BrandPrimary
import com.example.ui.theme.BrandSurface
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerProfileScreen(
    sellerId: String,
    onBack: () -> Unit,
    onProductClick: (String) -> Unit
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val coroutineScope = rememberCoroutineScope()

    var isLoading by remember { mutableStateOf(true) }
    var sellerName by remember { mutableStateOf("") }
    var sellerProfileImg by remember { mutableStateOf("") }
    var sellerProducts by remember { mutableStateOf<List<Product>>(emptyList()) }
    var error by remember { mutableStateOf<String?>(null) }

    val currentUserId = remember { FirebaseAuth.getInstance().currentUser?.uid ?: "" }
    val isOwner = currentUserId == sellerId

    // Edit states
    var showEditProfileDialog by remember { mutableStateOf(false) }
    var showEditAdDialog by remember { mutableStateOf<Product?>(null) }
    var idToDelete by remember { mutableStateOf<String?>(null) }

    // Core image upload states
    var tempName by remember { mutableStateOf("") }
    var tempAvatarUrl by remember { mutableStateOf("") }
    var isUploadingAvatar by remember { mutableStateOf(false) }
    var uploadError by remember { mutableStateOf<String?>(null) }

    val avatarPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let {
            coroutineScope.launch {
                isUploadingAvatar = true
                uploadError = null
                val uploader = CloudinaryUploader()
                uploader.uploadFile(it.toString())
                    .onSuccess { url ->
                        tempAvatarUrl = url
                    }
                    .onFailure { err ->
                        uploadError = err.message ?: (if (isArabic) "فشل التحميل" else "Upload failed")
                    }
                isUploadingAvatar = false
            }
        }
    }

    fun loadSellerData() {
        coroutineScope.launch {
            isLoading = true
            error = null
            try {
                val db = FirebaseFirestore.getInstance()
                
                // 1. Fetch user public profileinfo
                val userDoc = db.collection("users").document(sellerId).get().await()
                if (userDoc.exists()) {
                    sellerName = userDoc.getString("name") ?: (if (isArabic) "بائع مباشر" else "Direct Seller")
                    sellerProfileImg = userDoc.getString("profileImageUrl") ?: ""
                } else {
                    sellerName = if (isArabic) "بائع مباشر" else "Direct Seller"
                }

                // 2. Fetch direct ads
                val adsQuery = db.collection("direct_ads").whereEqualTo("ownerUid", sellerId).get().await()
                val products = mutableListOf<Product>()
                for (doc in adsQuery.documents) {
                    val id = doc.id
                    val title = doc.getString("title") ?: doc.getString("name") ?: ""
                    val description = doc.getString("description") ?: ""
                    val price = (doc.get("price") as? Number)?.toDouble() ?: 0.0
                    val imageUrls = (doc.get("images") as? List<String>)
                        ?: (doc.get("imageUrls") as? List<String>)
                        ?: (doc.get("coverImage") as? String)?.let { listOf(it) }
                        ?: emptyList()
                    val categoryId = doc.getString("categoryId") ?: ""
                    
                    products.add(
                        Product(
                            id = id,
                            title = title,
                            description = description,
                            price = price,
                            imageUrls = imageUrls,
                            categoryId = categoryId,
                            storeId = "direct_ad",
                            sellerType = "DIRECT_SELLER",
                            sellerId = sellerId,
                            rating = 5.0f,
                            reviewCount = 0,
                            isAvailable = true,
                            stockCount = 1,
                            currency = "USD",
                            isApproved = true,
                            isFlagged = false,
                            createdAt = System.currentTimeMillis()
                        )
                    )
                }
                sellerProducts = products
            } catch (e: Exception) {
                error = e.message ?: (if (isArabic) "فشل في تحميل كاتالوج البائع" else "Failed to load seller catalog")
            } finally {
                isLoading = false
            }
        }
    }

    LaunchedEffect(sellerId) {
        loadSellerData()
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                title = { Text(if (isArabic) "الملف الشخصي للبائع" else "Seller Profile", fontWeight = FontWeight.SemiBold) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        if (isLoading) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else if (error != null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(text = error!!, color = MaterialTheme.colorScheme.error)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                // Header Panel
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier.size(96.dp),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        if (sellerProfileImg.isNotEmpty()) {
                            AsyncImage(
                                model = sellerProfileImg,
                                contentDescription = "Avatar",
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                            )
                        } else {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primaryContainer),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Person,
                                    contentDescription = "Seller",
                                    tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                        }

                        if (isOwner) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary)
                                    .clickable {
                                        tempName = sellerName
                                        tempAvatarUrl = sellerProfileImg
                                        uploadError = null
                                        showEditProfileDialog = true
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit Profile",
                                    tint = Color.White,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.clickable(enabled = isOwner) {
                            if (isOwner) {
                                tempName = sellerName
                                tempAvatarUrl = sellerProfileImg
                                uploadError = null
                                showEditProfileDialog = true
                            }
                        }
                    ) {
                        Text(
                            text = sellerName,
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        if (isOwner) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Icon(
                                imageVector = Icons.Default.Edit,
                                contentDescription = "Edit name",
                                tint = BrandPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isArabic) "بائع مستقل • ${sellerProducts.size} إعلانات" else "Independent Seller • ${sellerProducts.size} Ads",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceVariant)

                // Catalog Title Row
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isArabic) "إعلانات البائع" else "Seller's Catalog",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    
                    if (isOwner) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandPrimary.copy(alpha = 0.15f))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = if (isArabic) "قابل للتعديل (المالك)" else "Editable (Owner)",
                                color = BrandPrimary,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Grid of products
                if (sellerProducts.isEmpty()) {
                    Box(modifier = Modifier.fillMaxSize().weight(1f), contentAlignment = Alignment.Center) {
                        Text(if (isArabic) "لا توجد عناصر معروضة حالياً لهذا البائع." else "This seller currently has no items.")
                    }
                } else {
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(sellerProducts) { product ->
                            Card(
                                shape = RoundedCornerShape(12.dp),
                                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
                                border = BorderStroke(1.dp, MaterialTheme.colorScheme.surfaceVariant),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier.clickable { onProductClick(product.id) }
                                ) {
                                    val imageUrl = product.imageUrls.firstOrNull()
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(1f)
                                    ) {
                                        if (imageUrl != null) {
                                            AsyncImage(
                                                model = imageUrl,
                                                contentDescription = product.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        } else {
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                            )
                                        }

                                        // Owner controls overlay
                                        if (isOwner) {
                                            Row(
                                                modifier = Modifier
                                                    .align(Alignment.TopEnd)
                                                    .padding(8.dp),
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                IconButton(
                                                    onClick = { showEditAdDialog = product },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                                    ),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Edit,
                                                        contentDescription = "Edit Ad",
                                                        tint = BrandPrimary,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                                IconButton(
                                                    onClick = { idToDelete = product.id },
                                                    colors = IconButtonDefaults.iconButtonColors(
                                                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
                                                    ),
                                                    modifier = Modifier.size(36.dp)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Delete Ad",
                                                        tint = MaterialTheme.colorScheme.error,
                                                        modifier = Modifier.size(18.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    Column(modifier = Modifier.padding(12.dp)) {
                                        Text(
                                            text = product.title,
                                            style = MaterialTheme.typography.titleSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = "$${product.price}",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = BrandPrimary,
                                            fontWeight = FontWeight.Bold
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

    // ------------------------------------------------------------------------
    // Edit Profile Modal Dialog
    // ------------------------------------------------------------------------
    if (showEditProfileDialog) {
        var isSaving by remember { mutableStateOf(false) }

        val presetAvatars = listOf(
            "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150",
            "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=150",
            "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=150",
            "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=150"
        )

        AlertDialog(
            onDismissRequest = { if (!isSaving && !isUploadingAvatar) showEditProfileDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تعديل الملف الشخصي" else "Edit Core Settings",
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    // Modern Avatar picker area
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Box(
                            modifier = Modifier
                                .size(96.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.surfaceVariant)
                                .clickable(enabled = !isUploadingAvatar && !isSaving) {
                                    avatarPicker.launch("image/*")
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            if (tempAvatarUrl.isNotEmpty()) {
                                AsyncImage(
                                    model = tempAvatarUrl,
                                    contentDescription = "Avatar Preview",
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(MaterialTheme.colorScheme.primaryContainer),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Avatar",
                                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(48.dp)
                                    )
                                }
                            }

                            if (isUploadingAvatar) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.5f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    CircularProgressIndicator(
                                        color = BrandPrimary,
                                        modifier = Modifier.size(36.dp)
                                    )
                                }
                            } else {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(Color.Black.copy(alpha = 0.35f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.CameraAlt,
                                        contentDescription = "Pick Image",
                                        tint = Color.White,
                                        modifier = Modifier.size(24.dp)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        TextButton(
                            onClick = { avatarPicker.launch("image/*") },
                            enabled = !isUploadingAvatar && !isSaving,
                            colors = ButtonDefaults.textButtonColors(contentColor = BrandPrimary)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.PhotoLibrary,
                                    contentDescription = null,
                                    modifier = Modifier.size(16.dp)
                                )
                                Text(
                                    text = if (isArabic) "اختر صورة من الاستوديو" else "Pick from gallery",
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.sp
                                )
                            }
                        }

                        if (uploadError != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = uploadError!!,
                                color = MaterialTheme.colorScheme.error,
                                style = MaterialTheme.typography.bodySmall,
                                textAlign = TextAlign.Center
                            )
                        }
                    }

                    OutlinedTextField(
                        value = tempName,
                        onValueChange = { tempName = it },
                        label = { Text(if (isArabic) "الاسم العريض" else "Display Name") },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = !isSaving
                    )

                    Text(
                        text = if (isArabic) "أو اختر صورة جاهزة:" else "Or pick a beautiful preset avatar:",
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold
                    )

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterHorizontally),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        presetAvatars.forEach { url ->
                            Box(
                                modifier = Modifier
                                    .size(45.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.surfaceVariant)
                                    .clickable(enabled = !isUploadingAvatar && !isSaving) { tempAvatarUrl = url }
                                    .let {
                                        if (tempAvatarUrl == url) {
                                            it.background(BrandPrimary, CircleShape)
                                                .padding(3.dp)
                                                .clip(CircleShape)
                                        } else {
                                            it
                                        }
                                    }
                            ) {
                                AsyncImage(
                                    model = url,
                                    contentDescription = null,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val userUpdateMap = mapOf(
                                    "name" to tempName,
                                    "profileImageUrl" to tempAvatarUrl
                                )
                                db.collection("users").document(sellerId).update(userUpdateMap).await()

                                // Update direct ad owner name instantly!
                                val adOwnerUpdateQuery = db.collection("direct_ads")
                                    .whereEqualTo("ownerUid", sellerId)
                                    .get()
                                    .await()
                                
                                for (doc in adOwnerUpdateQuery.documents) {
                                    db.collection("direct_ads")
                                        .document(doc.id)
                                        .update("ownerUsername", tempName)
                                        .await()
                                }

                                sellerName = tempName
                                sellerProfileImg = tempAvatarUrl
                                loadSellerData() // reload catalog
                                showEditProfileDialog = false
                            } catch (e: Exception) {
                                // gracefully handled
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    enabled = tempName.isNotEmpty() && !isSaving && !isUploadingAvatar
                ) {
                    if (isSaving) {
                        CircularProgressIndicator(color = Color.White, modifier = Modifier.size(18.dp))
                    } else {
                        Text(if (isArabic) "حفظ" else "Save Changes")
                    }
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { showEditProfileDialog = false },
                    enabled = !isSaving
                ) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // ------------------------------------------------------------------------
    // Edit Direct Ad Dialog
    // ------------------------------------------------------------------------
    if (showEditAdDialog != null) {
        val product = showEditAdDialog!!
        var title by remember { mutableStateOf(product.title) }
        var price by remember { mutableStateOf(product.price.toString()) }
        var description by remember { mutableStateOf(product.description) }
        var isSaving by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { showEditAdDialog = null },
            title = { Text(if (isArabic) "تعديل الإعلان" else "Edit Direct Ad") },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    OutlinedTextField(
                        value = title,
                        onValueChange = { title = it },
                        label = { Text(if (isArabic) "عنوان الإعلان" else "Ad Title") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = price,
                        onValueChange = { price = it },
                        label = { Text(if (isArabic) "السعر (USD)" else "Price (USD)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(if (isArabic) "الوصف" else "Description") },
                        modifier = Modifier.fillMaxWidth(),
                        maxLines = 4
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isSaving = true
                        val parsedPrice = price.toDoubleOrNull() ?: 0.0
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                val adMap = mapOf(
                                    "title" to title.trim(),
                                    "price" to parsedPrice,
                                    "description" to description.trim()
                                )
                                db.collection("direct_ads").document(product.id).update(adMap).await()
                                loadSellerData() // reload catalog
                                showEditAdDialog = null
                            } catch (e: Exception) {
                                // handled
                            } finally {
                                isSaving = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    enabled = title.isNotEmpty() && price.isNotEmpty() && !isSaving
                ) {
                    Text(if (isArabic) "حفظ التغييرات" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditAdDialog = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }

    // ------------------------------------------------------------------------
    // Delete Direct Ad Dialog
    // ------------------------------------------------------------------------
    if (idToDelete != null) {
        val targetId = idToDelete!!
        var isDeleting by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { idToDelete = null },
            title = { Text(if (isArabic) "حذف الإعلان" else "Remove Listing") },
            text = { Text(if (isArabic) "هل أنت متأكد من رغبتك في حذف هذا الإعلان نهائياً؟" else "Are you sure you want to permanently delete this listing?") },
            confirmButton = {
                Button(
                    onClick = {
                        isDeleting = true
                        coroutineScope.launch {
                            try {
                                val db = FirebaseFirestore.getInstance()
                                db.collection("direct_ads").document(targetId).delete().await()
                                loadSellerData() // reload catalog
                                idToDelete = null
                            } catch (e: Exception) {
                                // handled
                            } finally {
                                isDeleting = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    enabled = !isDeleting
                ) {
                    Text(if (isArabic) "حذف" else "Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { idToDelete = null }) {
                    Text(if (isArabic) "إلغاء" else "Cancel")
                }
            }
        )
    }
}
