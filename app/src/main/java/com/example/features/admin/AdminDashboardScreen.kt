package com.example.features.admin

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.core.di.ServiceLocator
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.domain.model.User
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import com.example.ui.theme.*

// Modular dashboard section identifier following Requirement 7
sealed class AdminModule(
    val id: String,
    val titleAr: String,
    val titleEn: String,
    val icon: ImageVector,
    val descriptionAr: String,
    val descriptionEn: String,
    val isAvailable: Boolean = true
) {
    object Stores : AdminModule(
        id = "stores",
        titleAr = "إدارة المتاجر",
        titleEn = "Manage Stores",
        icon = Icons.Default.Storefront,
        descriptionAr = "مراجعة، تعديل، تعطيل، وحذف المتاجر المسجلة",
        descriptionEn = "Review, edit, disable, or delete registered stores"
    )

    object Products : AdminModule(
        id = "products",
        titleAr = "إدارة المنتجات",
        titleEn = "Manage Products",
        icon = Icons.Default.ShoppingBag,
        descriptionAr = "تعديل وحذف كافة المنتجات المدرجة ومتابعة العروض",
        descriptionEn = "Edit or remove listed products globally and track offers"
    )

    object Users : AdminModule(
        id = "users",
        titleAr = "إدارة المستخدمين",
        titleEn = "Manage Users",
        icon = Icons.Default.People,
        descriptionAr = "إضافة مستخدمين، إعادة تعيين كلمة المرور، وحذف الحسابات",
        descriptionEn = "Add manual users, trigger password resets, and delete accounts"
    )

    object Analytics : AdminModule(
        id = "analytics",
        titleAr = "التحليلات والإحصائيات",
        titleEn = "Analytics & Reports",
        icon = Icons.Default.Analytics,
        descriptionAr = "مخططات متقدمة لنمو المبيعات وفئات المنتجات الأكثر طلباً",
        descriptionEn = "Advanced charts tracking store growth and active search categories",
        isAvailable = false
    )

    object Logs : AdminModule(
        id = "logs",
        titleAr = "سجلات الأمان والرقابة",
        titleEn = "Audit Logs",
        icon = Icons.Default.History,
        descriptionAr = "مراقبة نشاط المسؤولين وتغييرات قاعدة البيانات بشكل أمني",
        descriptionEn = "Track admin dashboard actions, database modifications, and login alerts",
        isAvailable = false
    )
}

data class AdminUiState(
    val stores: List<Store> = emptyList(),
    val products: List<Product> = emptyList(),
    val users: List<User> = emptyList(),
    val isLoading: Boolean = false,
    val selectedModule: AdminModule? = null,
    val errorMessage: String? = null
)

class AdminViewModel : ViewModel() {
    private val _state = MutableStateFlow(AdminUiState())
    val state: StateFlow<AdminUiState> = _state.asStateFlow()

    private val storeRepo = ServiceLocator.storeRepository
    private val productRepo = ServiceLocator.productRepository
    private val authRepo = ServiceLocator.authRepository

    private val firestore: FirebaseFirestore? by lazy {
        try { FirebaseFirestore.getInstance() } catch (e: Exception) { null }
    }

    init {
        observeData()
        loadUsersDirectly()
    }

    private fun observeData() {
        // Collect Stores dynamically under VM lifecycles
        viewModelScope.launch {
            _state.update { it.copy(isLoading = true) }
            storeRepo.getAllStores().collect { result ->
                result.fold(
                    onSuccess = { storeList ->
                        _state.update { it.copy(stores = storeList, isLoading = false) }
                    },
                    onFailure = { err ->
                        _state.update { it.copy(errorMessage = err.localizedMessage, isLoading = false) }
                    }
                )
            }
        }

        // Collect Products dynamically in VM
        viewModelScope.launch {
            productRepo.getProducts().collect { prodList ->
                _state.update { it.copy(products = prodList) }
            }
        }
    }

    fun loadUsersDirectly() {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                val userSnap = db.collection("users").get().await()
                val fetchedUsers = userSnap.documents.mapNotNull { doc ->
                    User(
                        id = doc.id,
                        email = doc.getString("email") ?: "",
                        name = doc.getString("name") ?: "",
                        profileImageUrl = doc.getString("profileImageUrl"),
                        phoneNumber = doc.getString("phoneNumber"),
                        isStoreOwner = doc.getBoolean("isStoreOwner") ?: false,
                        role = doc.getString("role") ?: "client",
                        joinedAt = doc.getLong("joinedAt") ?: System.currentTimeMillis()
                    )
                }
                _state.update { it.copy(users = fetchedUsers) }
            } catch (e: Exception) {
                _state.update { it.copy(errorMessage = e.localizedMessage) }
            }
        }
    }

    fun selectModule(module: AdminModule?) {
        _state.update { it.copy(selectedModule = module) }
    }

    // --- STORE REPOSITORY TRIGGER ACTIONS ---
    fun updateStore(store: Store, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = storeRepo.updateStore(store)
            onComplete(res.isSuccess)
        }
    }

    fun disableStore(storeId: String, currentStatus: String, store: Store, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val nextStatus = if (currentStatus == "active") "disabled" else "active"
            val updated = store.copy(status = nextStatus)
            val res = storeRepo.updateStore(updated)
            onComplete(res.isSuccess)
        }
    }

    fun deleteStore(storeId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = storeRepo.deleteStore(storeId)
            onComplete(res.isSuccess)
        }
    }

    // --- PRODUCT REPOSITORY TRIGGER ACTIONS ---
    fun updateProduct(product: Product, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = productRepo.updateProduct(product)
            onComplete(res.isSuccess)
        }
    }

    fun deleteProduct(productId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = productRepo.deleteProduct(productId)
            onComplete(res.isSuccess)
        }
    }

    // --- USER CONTROL PANEL METRICS ---
    fun createUserManually(name: String, email: String, role: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                val generatedId = db.collection("users").document().id
                val userMap = hashMapOf(
                    "id" to generatedId,
                    "email" to email,
                    "name" to name,
                    "role" to role,
                    "isStoreOwner" to false,
                    "joinedAt" to System.currentTimeMillis()
                )
                db.collection("users").document(generatedId).set(userMap).await()
                loadUsersDirectly()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun deleteUser(userId: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                db.collection("users").document(userId).delete().await()
                loadUsersDirectly()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun triggerPasswordReset(email: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = authRepo.sendPasswordReset(email)
            onComplete(result.isSuccess)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(
    onNavigateBack: () -> Unit
) {
    val viewModel = androidx.lifecycle.viewmodel.compose.viewModel<AdminViewModel>()
    val state by viewModel.state.collectAsState()
    val context = LocalContext.current
    val isArabic = com.example.core.utils.LanguageManager.isArabic(context)

    Scaffold(
        containerColor = BrandBackground,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = if (isArabic) "لوحة إدارة واصل بلس" else "WasetPlus Admin Panel",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                        state.selectedModule?.let { mod ->
                            Text(
                                text = if (isArabic) mod.titleAr else mod.titleEn,
                                fontSize = 12.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold
                            )
                        } ?: Text(
                            text = if (isArabic) "تحكم كامل بالنظام" else "Full ecosystem control center",
                            fontSize = 12.sp,
                            color = BrandTextMuted
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = {
                        if (state.selectedModule != null) {
                            viewModel.selectModule(null)
                        } else {
                            onNavigateBack()
                        }
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = BrandTextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = {
                        viewModel.loadUsersDirectly()
                    }) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            tint = BrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = BrandSurface)
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            if (state.isLoading) {
                CircularProgressIndicator(
                    color = BrandPrimary,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                AnimatedVisibility(
                    visible = state.selectedModule == null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    AdminDashboardModulesListing(
                        modules = listOf(
                            AdminModule.Stores,
                            AdminModule.Products,
                            AdminModule.Users,
                            AdminModule.Analytics,
                            AdminModule.Logs
                        ),
                        isArabic = isArabic,
                        onModuleSelect = { mod ->
                            if (mod.isAvailable) {
                                viewModel.selectModule(mod)
                            } else {
                                Toast.makeText(
                                    context,
                                    if (isArabic) "هذه الميزة ستتوفر قريباً في التحديث القادم!" else "This module is coming soon under active architecture!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }
                    )
                }

                AnimatedVisibility(
                    visible = state.selectedModule != null,
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    when (state.selectedModule) {
                        is AdminModule.Stores -> AdminStoresManager(
                            stores = state.stores,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Products -> AdminProductsManager(
                            products = state.products,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Users -> AdminUsersManager(
                            users = state.users,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        else -> Unit
                    }
                }
            }
        }
    }
}

@Composable
fun AdminDashboardModulesListing(
    modules: List<AdminModule>,
    isArabic: Boolean,
    onModuleSelect: (AdminModule) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        items(modules) { module ->
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onModuleSelect(module) },
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(1.dp, BrandSoftGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(52.dp)
                            .clip(CircleShape)
                            .background(if (module.isAvailable) BrandPrimary.copy(alpha = 0.1f) else BrandSoftGray)
                            .border(1.dp, if (module.isAvailable) BrandPrimary.copy(alpha = 0.3f) else Color.Transparent, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = module.icon,
                            contentDescription = module.titleEn,
                            tint = if (module.isAvailable) BrandPrimary else BrandTextMuted,
                            modifier = Modifier.size(26.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) module.titleAr else module.titleEn,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary
                            )
                            if (!module.isAvailable) {
                                Box(
                                    modifier = Modifier
                                        .background(BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "قريباً" else "Soon",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = BrandPrimary
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) module.descriptionAr else module.descriptionEn,
                            fontSize = 12.sp,
                            color = BrandTextMuted
                        )
                    }

                    if (module.isAvailable) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Open",
                            tint = BrandTextMuted,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }
    }
}


// --- INTERNAL MODULE PANELS ---

@Composable
fun AdminStoresManager(
    stores: List<Store>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var editingStore by remember { mutableStateOf<Store?>(null) }
    var storeToDelete by remember { mutableStateOf<Store?>(null) }

    if (stores.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isArabic) "لا توجد متاجر مسجلة حالياً" else "No stores currently registered",
                color = BrandTextMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(stores) { store ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val logoPath = store.logoUrl ?: ""
                            AsyncImage(
                                model = logoPath.ifEmpty { "https://images.unsplash.com/photo-1472851294608-062f824d29cc?w=400" },
                                contentDescription = store.name,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(50.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BrandSoftGray, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = store.name,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextPrimary
                                )
                                Text(
                                    text = "${if (isArabic) "المالك:" else "Owner:"} ${store.ownerUsername.ifEmpty { "Waset Owner" }}",
                                    fontSize = 12.sp,
                                    color = BrandTextMuted
                                )
                                Text(
                                    text = "${if (isArabic) "التصنيف:" else "Category:"} ${store.categoryId.ifEmpty { "Uncategorized" }}",
                                    fontSize = 11.sp,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Active / Disabled Badge
                            val isActive = store.status == "active"
                            Box(
                                modifier = Modifier
                                    .background(
                                        color = if (isActive) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = if (isActive) {
                                        if (isArabic) "نشط" else "Active"
                                    } else {
                                        if (isArabic) "معطل" else "Disabled"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isActive) Color(0xFF2E7D32) else Color(0xFFC62828)
                                )
                            }
                        }

                        if (store.description.isNotEmpty()) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = store.description,
                                fontSize = 12.sp,
                                color = BrandTextMuted,
                                maxLines = 2
                            )
                        }

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { editingStore = store },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "تعديل" else "Edit", fontSize = 13.sp)
                            }

                            Button(
                                onClick = {
                                    viewModel.disableStore(store.id, store.status, store) { success ->
                                        if (success) {
                                            Toast.makeText(context, "Status updated!", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (store.status == "active") Color(0xFFF57C00) else Color(0xFF2E7D32)
                                ),
                                shape = RoundedCornerShape(8.dp),
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Text(
                                    text = if (store.status == "active") {
                                        if (isArabic) "تعطيل" else "Disable"
                                    } else {
                                        if (isArabic) "تفعيل" else "Enable"
                                    },
                                    fontSize = 13.sp
                                )
                            }

                            IconButton(onClick = { storeToDelete = store }) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Store Dialog
    editingStore?.let { store ->
        var editedName by remember { mutableStateOf(store.name) }
        var editedDesc by remember { mutableStateOf(store.description) }
        var editedCategory by remember { mutableStateOf(store.categoryId) }
        var logoUrl by remember { mutableStateOf(store.logoUrl ?: "") }
        var bannerUrl by remember { mutableStateOf(store.bannerUrl ?: "") }

        AlertDialog(
            onDismissRequest = { editingStore = null },
            title = { Text(if (isArabic) "تعديل المتجر" else "Edit Store Details") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = editedName,
                            onValueChange = { editedName = it },
                            label = { Text(if (isArabic) "اسم المتجر" else "Store Name") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedCategory,
                            onValueChange = { editedCategory = it },
                            label = { Text(if (isArabic) "معرف تصنيف المتجر" else "Store Category ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedDesc,
                            onValueChange = { editedDesc = it },
                            label = { Text(if (isArabic) "الوصف المحتوى" else "Store Description") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 4
                        )
                        OutlinedTextField(
                            value = logoUrl,
                            onValueChange = { logoUrl = it },
                            label = { Text(if (isArabic) "رابط الصورة الرمزية للمتجر" else "Logo URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = bannerUrl,
                            onValueChange = { bannerUrl = it },
                            label = { Text(if (isArabic) "رابط غلاف المتجر" else "Banner URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val upd = store.copy(
                            name = editedName,
                            description = editedDesc,
                            categoryId = editedCategory,
                            logoUrl = logoUrl.ifEmpty { null },
                            bannerUrl = bannerUrl.ifEmpty { null }
                        )
                        viewModel.updateStore(upd) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Saved successfully!", Toast.LENGTH_SHORT).show()
                                editingStore = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (isArabic) "حفظ التغييرات" else "Save Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingStore = null }) {
                    Text(if (isArabic) "إلغاء الأمر" else "Cancel")
                }
            }
        )
    }

    // Delete Store Confirmation
    storeToDelete?.let { store ->
        AlertDialog(
            onDismissRequest = { storeToDelete = null },
            title = { Text(if (isArabic) "حذف المتجر نهائياً؟" else "Delete Store Permanently?") },
            text = { Text(if (isArabic) "هل أنت متأكد من حذف ${store.name}؟ هذا الإجراء سيقوم بإزالة المتجر وجميع المنتجات السحابية التابعة له!" else "Are you sure? This deletes ${store.name} and terminates all secondary product feeds.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteStore(store.id) { success ->
                            if (success) {
                                Toast.makeText(context, "Terminated!", Toast.LENGTH_SHORT).show()
                                storeToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isArabic) "حذف للمتجر" else "Confirm Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToDelete = null }) {
                    Text(if (isArabic) "تراجع" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminProductsManager(
    products: List<Product>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var editingProduct by remember { mutableStateOf<Product?>(null) }
    var productToDelete by remember { mutableStateOf<Product?>(null) }

    if (products.isEmpty()) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(
                text = if (isArabic) "لا توجد منتجات مسجلة حالياً" else "No products globally found",
                color = BrandTextMuted
            )
        }
    } else {
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(products) { product ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            val imgPath = product.imageUrls.firstOrNull() ?: ""
                            AsyncImage(
                                model = imgPath.ifEmpty { "https://images.unsplash.com/photo-1523275335684-37898b6baf30?w=400" },
                                contentDescription = product.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(60.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .border(1.dp, BrandSoftGray, RoundedCornerShape(8.dp))
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = product.title,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextPrimary
                                )
                                Text(
                                    text = "${if (isArabic) "معرف المتجر:" else "Store ID:"} ${product.storeId}",
                                    fontSize = 12.sp,
                                    color = BrandTextMuted
                                )
                                Text(
                                    text = "${if (isArabic) "معرف الفئة:" else "Category ID:"} ${product.categoryId}",
                                    fontSize = 11.sp,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }

                            // Prices Block
                            Column(horizontalAlignment = Alignment.End) {
                                Text(
                                    text = "${product.price} ${if (isArabic) "ريال" else "SAR"}",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = product.description,
                            fontSize = 12.sp,
                            color = BrandTextMuted,
                            maxLines = 2
                        )

                        Spacer(modifier = Modifier.height(12.dp))
                        HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.End,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TextButton(
                                onClick = { editingProduct = product },
                                modifier = Modifier.padding(end = 8.dp)
                            ) {
                                Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "تعديل" else "Edit", fontSize = 13.sp)
                            }

                            Button(
                                onClick = { productToDelete = product },
                                colors = ButtonDefaults.buttonColors(containerColor = Color.Red),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(if (isArabic) "حذف" else "Delete", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    }

    // Edit Product Dialog Detail
    editingProduct?.let { prod ->
        var editedTitle by remember { mutableStateOf(prod.title) }
        var editedDesc by remember { mutableStateOf(prod.description) }
        var editedPrice by remember { mutableStateOf(prod.price.toString()) }
        var editedCategoryId by remember { mutableStateOf(prod.categoryId) }
        var editedPrimaryImageUrl by remember { mutableStateOf(prod.imageUrls.firstOrNull() ?: "") }

        AlertDialog(
            onDismissRequest = { editingProduct = null },
            title = { Text(if (isArabic) "تعديل بيانات المنتج" else "Modify Product Details") },
            text = {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    item {
                        OutlinedTextField(
                            value = editedTitle,
                            onValueChange = { editedTitle = it },
                            label = { Text(if (isArabic) "عنوان المنتج" else "Product Title") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedDesc,
                            onValueChange = { editedDesc = it },
                            label = { Text(if (isArabic) "الوصف المحتوى" else "Description") },
                            modifier = Modifier.fillMaxWidth(),
                            maxLines = 3
                        )
                        OutlinedTextField(
                            value = editedPrice,
                            onValueChange = { editedPrice = it },
                            label = { Text(if (isArabic) "السعر" else "Price") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedCategoryId,
                            onValueChange = { editedCategoryId = it },
                            label = { Text(if (isArabic) "معرف الفئة" else "Category ID") },
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = editedPrimaryImageUrl,
                            onValueChange = { editedPrimaryImageUrl = it },
                            label = { Text(if (isArabic) "رابط الصورة" else "Image URL") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val priceVal = editedPrice.toDoubleOrNull() ?: prod.price
                        val updatedProd = prod.copy(
                            title = editedTitle,
                            description = editedDesc,
                            price = priceVal,
                            categoryId = editedCategoryId,
                            imageUrls = if (editedPrimaryImageUrl.isNotEmpty()) listOf(editedPrimaryImageUrl) else prod.imageUrls
                        )
                        viewModel.updateProduct(updatedProd) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Product Sync Complete!", Toast.LENGTH_SHORT).show()
                                editingProduct = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (isArabic) "حفظ التحديثات" else "Apply Changes")
                }
            },
            dismissButton = {
                TextButton(onClick = { editingProduct = null }) {
                    Text(if (isArabic) "إلغاء الأمر" else "Cancel")
                }
            }
        )
    }

    // Delete Product Confirmation
    productToDelete?.let { prod ->
        AlertDialog(
            onDismissRequest = { productToDelete = null },
            title = { Text(if (isArabic) "حذف المنتج نهائياً؟" else "Delete Product Confirm") },
            text = { Text(if (isArabic) "هل تريد حذف منتج ${prod.title} بشكل كامل من المتجر وقاعدة البيانات؟" else "Are you sure you want to completely erase ${prod.title}?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct(prod.id) { success ->
                            if (success) {
                                Toast.makeText(context, "Erased successfully!", Toast.LENGTH_SHORT).show()
                                productToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isArabic) "تأكيد الحذف" else "Delete Product")
                }
            },
            dismissButton = {
                TextButton(onClick = { productToDelete = null }) {
                    Text(if (isArabic) "تراجع" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminUsersManager(
    users: List<User>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var isAddingUser by remember { mutableStateOf(false) }
    var userToDelete by remember { mutableStateOf<User?>(null) }
    var userToResetPassword by remember { mutableStateOf<User?>(null) }

    Box(modifier = Modifier.fillMaxSize()) {
        if (users.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isArabic) "لا يوجد مستخدمون مسجلون" else "No users currently listed",
                    color = BrandTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(users) { usr ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(16.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape)
                                        .background(BrandPrimary.copy(alpha = 0.15f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(Icons.Default.Person, contentDescription = "User", tint = BrandPrimary)
                                }
                                Spacer(modifier = Modifier.width(12.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                        Text(
                                            text = usr.name,
                                            fontSize = 15.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = BrandTextPrimary
                                        )
                                        // Badge
                                        val isAdmin = usr.role == "admin"
                                        Box(
                                            modifier = Modifier
                                                .background(
                                                    color = if (isAdmin) Color(0xFFFFECE0) else Color(0xFFF0F4F8),
                                                    shape = RoundedCornerShape(4.dp)
                                                )
                                                .padding(horizontal = 4.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = if (isAdmin) {
                                                    if (isArabic) "مشرف" else "Admin"
                                                } else {
                                                    if (isArabic) "عميل" else "Client"
                                                },
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isAdmin) Color(0xFFE65100) else Color(0xFF455A64)
                                            )
                                        }
                                    }
                                    Text(
                                        text = usr.email,
                                        fontSize = 12.sp,
                                        color = BrandTextMuted
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TextButton(
                                    onClick = { userToResetPassword = usr },
                                    modifier = Modifier.padding(end = 8.dp)
                                ) {
                                    Icon(Icons.Default.LockReset, contentDescription = "LockReset", modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(if (isArabic) "كلمة المرور" else "Reset Pwd", fontSize = 13.sp)
                                }

                                IconButton(onClick = { userToDelete = usr }) {
                                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = Color.Red)
                                }
                            }
                        }
                    }
                }

                // Add empty padding for FAB
                item {
                    Spacer(modifier = Modifier.height(80.dp))
                }
            }
        }

        // Manual User Creation FAB following Requirement 5
        FloatingActionButton(
            onClick = { isAddingUser = true },
            containerColor = BrandPrimary,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add User")
        }
    }

    // Manual Creation Dialog
    if (isAddingUser) {
        var uName by remember { mutableStateOf("") }
        var uEmail by remember { mutableStateOf("") }
        var uRole by remember { mutableStateOf("client") }

        AlertDialog(
            onDismissRequest = { isAddingUser = false },
            title = { Text(if (isArabic) "إضافة مستخدم جديد يدوياً" else "Create User Manually") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = uName,
                        onValueChange = { uName = it },
                        label = { Text(if (isArabic) "اسم المستخدم" else "Full Name") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = uEmail,
                        onValueChange = { uEmail = it },
                        label = { Text(if (isArabic) "البريد الإلكتروني" else "Email Address") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Text(
                            text = if (isArabic) "تعيين الدور المخصص:" else "Select User Role:",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.weight(1f)
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            RadioButton(
                                selected = uRole == "client",
                                onClick = { uRole = "client" }
                            )
                            Text(if (isArabic) "عميل" else "Client", fontSize = 13.sp)

                            Spacer(modifier = Modifier.width(8.dp))

                            RadioButton(
                                selected = uRole == "admin",
                                onClick = { uRole = "admin" }
                            )
                            Text(if (isArabic) "مشرف" else "Admin", fontSize = 13.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (uName.isNotEmpty() && uEmail.isNotEmpty()) {
                            viewModel.createUserManually(uName, uEmail, uRole) { ok ->
                                if (ok) {
                                    Toast.makeText(context, "User profile registered inside Firestore!", Toast.LENGTH_SHORT).show()
                                    isAddingUser = false
                                }
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (isArabic) "إضافة مستخدم" else "Add User")
                }
            },
            dismissButton = {
                TextButton(onClick = { isAddingUser = false }) {
                    Text(if (isArabic) "تراجع" else "Cancel")
                }
            }
        )
    }

    // Password Reset Email Trigger
    userToResetPassword?.let { usr ->
        AlertDialog(
            onDismissRequest = { userToResetPassword = null },
            title = { Text(if (isArabic) "إرسال رابط استعادة المرور؟" else "Send Password Reset Email?") },
            text = { Text(if (isArabic) "سيقوم النظام بإرسال رابط تأميني استعادة كلمة المرور مباشرة إلى بريده الإلكتروني: ${usr.email}" else "This will fire a secure Firebase password recovery email explicitly to: ${usr.email}") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.triggerPasswordReset(usr.email) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Recovery Link Dispatched!", Toast.LENGTH_SHORT).show()
                            } else {
                                Toast.makeText(context, "Failed to email recovery link", Toast.LENGTH_SHORT).show()
                            }
                            userToResetPassword = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (isArabic) "إرسال الآن" else "Trigger Reset flow")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToResetPassword = null }) {
                    Text(if (isArabic) "إلغاء الأمر" else "Cancel")
                }
            }
        )
    }

    // User Deletion Profile
    userToDelete?.let { usr ->
        AlertDialog(
            onDismissRequest = { userToDelete = null },
            title = { Text(if (isArabic) "حذف المستخدم المختار؟" else "Erase User Profile?") },
            text = { Text(if (isArabic) "هل تريد حذف حساب ${usr.name} بالكامل؟ هذا الإجراء سيمسح وثيقته ولن يستطيع الدخول كصاحب خدمات ممتدة." else "Are you sure you want to permanently erase ${usr.name}'s profile document?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteUser(usr.id) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Profile terminated", Toast.LENGTH_SHORT).show()
                                userToDelete = null
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Red)
                ) {
                    Text(if (isArabic) "تأكيد حذف المستخدم" else "Erase User")
                }
            },
            dismissButton = {
                TextButton(onClick = { userToDelete = null }) {
                    Text(if (isArabic) "تراجع" else "Cancel")
                }
            }
        )
    }
}
