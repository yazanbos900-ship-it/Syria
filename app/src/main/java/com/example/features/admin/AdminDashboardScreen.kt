package com.example.features.admin

import android.util.Log
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState
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
import androidx.compose.ui.platform.testTag
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.example.core.di.ServiceLocator
import com.example.core.utils.CurrencyManager
import com.example.domain.model.Product
import com.example.domain.model.Store
import com.example.domain.model.User
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.domain.model.SubscriptionRequest
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
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

    object Jobs : AdminModule(
        id = "jobs",
        titleAr = "إدارة الوظائف",
        titleEn = "Manage Jobs",
        icon = Icons.Default.Work,
        descriptionAr = "مراجعة وحذف وإدارة إعلانات الوظائف والمتقدمين",
        descriptionEn = "Review, delete, and manage job postings and applications"
    )

    object Products : AdminModule(
        id = "products",
        titleAr = "إدارة المنتجات",
        titleEn = "Manage Products",
        icon = Icons.Default.ShoppingBag,
        descriptionAr = "تعديل وحذف المنتجات وتفتيش السلع المشكوك بأمرها",
        descriptionEn = "Edit or remove listed products globally and track flagged items"
    )

    object Users : AdminModule(
        id = "users",
        titleAr = "إدارة المستخدمين",
        titleEn = "Manage Users",
        icon = Icons.Default.People,
        descriptionAr = "إضافة مستخدمين، إعادة تعيين كلمة المرور، وحذف الحسابات",
        descriptionEn = "Add manual users, trigger password resets, and delete accounts"
    )

    object Orders : AdminModule(
        id = "orders",
        titleAr = "إدارة الطلبات",
        titleEn = "Manage Orders",
        icon = Icons.Default.ReceiptLong,
        descriptionAr = "متابعة وتحديث حالات كافة الطلبات والعمليات في السوق الكلي",
        descriptionEn = "Track and update statuses of all marketplace transactions"
    )

    object Subscriptions : AdminModule(
        id = "subscriptions",
        titleAr = "اشتراكات البائعين",
        titleEn = "Subscriptions Center",
        icon = Icons.Default.CardMembership,
        descriptionAr = "مراجعة باقات وترقيات المتاجر (Starter, Growth, Pro)",
        descriptionEn = "Approve, reject, upgrade, and downgrade seller subscription tiers"
    )

    object Settings : AdminModule(
        id = "settings",
        titleAr = "إعدادات السوق الكلية",
        titleEn = "Marketplace Settings",
        icon = Icons.Default.Settings,
        descriptionAr = "الرسوم، ضريبة VAT، أجور الشحن الافتراضية، والمدن المدعومة",
        descriptionEn = "Configure dynamic platform fees, VAT, default shipping, and cities"
    )

    object Analytics : AdminModule(
        id = "analytics",
        titleAr = "التحليلات والإحصائيات",
        titleEn = "Analytics & Reports",
        icon = Icons.Default.Analytics,
        descriptionAr = "مخططات والمنتجات الأكثر تفاعلاً وزيارات المتاجر والإيرادات",
        descriptionEn = "Interactive products views, favorites, visits, and revenue analysis"
    )

    object ExchangeRate : AdminModule(
        id = "exchange_rate",
        titleAr = "أسعار الصرف والعملات",
        titleEn = "Exchange Rate Manager",
        icon = Icons.Default.SwapHoriz,
        descriptionAr = "تعديل وإدارة سعر الصرف الموحد وتتبع فروقات الأسعار للمتاجر",
        descriptionEn = "Manage standard exchange rates and track store-level differences"
    )
}

data class AdminUiState(
    val stores: List<Store> = emptyList(),
    val products: List<Product> = emptyList(),
    val users: List<User> = emptyList(),
    val orders: List<Order> = emptyList(),
    val jobs: List<com.example.domain.model.Job> = emptyList(),
    val applications: List<com.example.domain.model.JobApplication> = emptyList(),
    val subscriptionRequests: List<SubscriptionRequest> = emptyList(),
    val interactions: List<Map<String, Any>> = emptyList(),
    val settings: com.example.core.utils.MarketplaceSettingsManager.MarketplaceSettings = com.example.core.utils.MarketplaceSettingsManager.settings.value,
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

        // Collect Orders dynamically in VM
        viewModelScope.launch {
            ServiceLocator.orderRepository.getAllOrders().collect { result ->
                result.fold(
                    onSuccess = { orderList ->
                        _state.update { it.copy(orders = orderList) }
                    },
                    onFailure = { err ->
                        Log.e("AdminViewModel", "Error fetching orders", err)
                    }
                )
            }
        }

        // Collect Subscription Requests dynamically
        viewModelScope.launch {
            val db = firestore ?: return@launch
            db.collection("subscription_requests")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val reqs = snapshot.documents.mapNotNull { doc ->
                            try {
                                SubscriptionRequest(
                                    requestId = doc.getString("requestId") ?: doc.id,
                                    userId = doc.getString("userId") ?: "",
                                    storeId = doc.getString("storeId") ?: "",
                                    requestedTier = doc.getString("requestedTier") ?: "Starter",
                                    requestDate = doc.getLong("requestDate") ?: System.currentTimeMillis(),
                                    status = doc.getString("status") ?: "pending"
                                )
                            } catch (e: Exception) {
                                null
                            }
                        }
                        _state.update { it.copy(subscriptionRequests = reqs) }
                    }
                }
        }

        // Collect Jobs dynamically
        viewModelScope.launch {
            try {
                ServiceLocator.jobRepository.getAllJobs()
                    .catch { e -> Log.e("AdminVM", "Error collecting jobs", e) }
                    .collect { jobList ->
                        _state.update { it.copy(jobs = jobList) }
                    }
            } catch (e: Exception) {
                Log.e("AdminVM", "Crash avoided on jobs", e)
            }
        }

        // Collect Applications dynamically
        viewModelScope.launch {
            try {
                ServiceLocator.jobRepository.getAllApplications()
                    .catch { e -> Log.e("AdminVM", "Error collecting applications", e) }
                    .collect { appList ->
                        _state.update { it.copy(applications = appList) }
                    }
            } catch (e: Exception) {
                Log.e("AdminVM", "Crash avoided on applications", e)
            }
        }

        // Load Interactions dynamically
        viewModelScope.launch {
            val db = firestore ?: return@launch
            db.collection("interactions")
                .addSnapshotListener { snapshot, error ->
                    if (error != null) return@addSnapshotListener
                    if (snapshot != null) {
                        val actions = snapshot.documents.map { doc ->
                            doc.data ?: emptyMap()
                        }
                        _state.update { it.copy(interactions = actions) }
                    }
                }
        }

        // Load Settings flow
        viewModelScope.launch {
            com.example.core.utils.MarketplaceSettingsManager.settings.collect { currentSettings ->
                _state.update { it.copy(settings = currentSettings) }
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

    // --- SUBSCRIPTION MANAGEMENT ---
    fun handleSubscriptionRequest(requestId: String, storeId: String, tier: String, approve: Boolean, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                val nextStatus = if (approve) "approved" else "rejected"
                db.collection("subscription_requests").document(requestId).update("status", nextStatus).await()
                if (approve) {
                    db.collection("stores").document(storeId).update("subscriptionTier", tier).await()
                }
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun setStoreTier(storeId: String, tier: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                db.collection("stores").document(storeId).update("subscriptionTier", tier).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- SELLER VERIFICATION MANAGEMENT ---
    fun handleVerificationRequest(storeId: String, approve: Boolean, badge: String = "None", onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                val status = if (approve) "Verified" else "Rejected"
                val updates = mapOf(
                    "verificationStatus" to status,
                    "isVerified" to approve,
                    "sellerBadge" to if (approve) badge else "None"
                )
                db.collection("stores").document(storeId).update(updates).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    // --- MARKETPLACE SETTINGS MANAGEMENT ---
    fun saveMarketplaceSettings(newSettings: com.example.core.utils.MarketplaceSettingsManager.MarketplaceSettings, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val result = com.example.core.utils.MarketplaceSettingsManager.saveSettings(newSettings)
            onComplete(result.isSuccess)
        }
    }

    // --- ORDERS MANAGEMENT ---
    fun updateOrderStatusAdmin(orderId: String, status: String, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val res = ServiceLocator.orderRepository.updateOrderStatus(orderId, status)
            onComplete(res.isSuccess)
        }
    }

    fun logAdminAction(action: String) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            val adminId = authRepo.getCurrentUserSession()?.id ?: "unknown"
            val map = mapOf(
                "adminId" to adminId,
                "action" to action,
                "timestamp" to System.currentTimeMillis()
            )
            db.collection("audit_logs").add(map)
        }
    }

    fun updateJobStatusAdmin(jobId: String, newStatus: String, onResult: (Boolean) -> Unit) {
        val db = firestore
        if (db == null) {
            onResult(false)
            return
        }
        db.collection("jobs").document(jobId).update("status", newStatus)
            .addOnSuccessListener {
                logAdminAction("Updated job $jobId status to $newStatus")
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    fun deleteJobAdmin(jobId: String, onResult: (Boolean) -> Unit) {
        val db = firestore
        if (db == null) {
            onResult(false)
            return
        }
        db.collection("jobs").document(jobId).delete()
            .addOnSuccessListener {
                logAdminAction("Deleted job $jobId")
                onResult(true)
            }
            .addOnFailureListener {
                onResult(false)
            }
    }

    // --- GLOBAL EXCHANGE RATE ACTIONS ---
    fun updateGlobalExchangeRate(rate: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                db.collection("admin_settings").document("exchange_rate").set(mapOf("rate" to rate)).await()
                val current = com.example.core.utils.MarketplaceSettingsManager.settings.value
                com.example.core.utils.MarketplaceSettingsManager.saveSettings(current.copy(defaultExchangeRate = rate))
                val storesSnapshot = db.collection("stores").get().await()
                val batch = db.batch()
                var hasUpdates = false
                for (doc in storesSnapshot.documents) {
                    val usingGlobal = doc.getBoolean("usingGlobalRate") ?: true
                    if (usingGlobal) {
                        batch.update(doc.reference, mapOf(
                            "usdExchangeRate" to rate,
                            "exchangeRate" to rate,
                            "exchangeRateUpdatedAt" to com.google.firebase.Timestamp.now(),
                            "usingGlobalRate" to true
                        ))
                        hasUpdates = true
                    }
                }
                if (hasUpdates) {
                    batch.commit().await()
                }
                onComplete(true)
            } catch (e: Exception) {
                Log.e("AdminViewModel", "updateGlobalExchangeRate failed", e)
                onComplete(false)
            }
        }
    }

    fun applyGlobalRateStore(storeId: String, globalRate: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                db.collection("stores").document(storeId).update(mapOf(
                    "usingGlobalRate" to true,
                    "usdExchangeRate" to globalRate,
                    "exchangeRate" to globalRate,
                    "exchangeRateUpdatedAt" to com.google.firebase.Timestamp.now()
                )).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
        }
    }

    fun applyCustomRateStore(storeId: String, customRate: Double, onComplete: (Boolean) -> Unit) {
        viewModelScope.launch {
            val db = firestore ?: return@launch
            try {
                db.collection("stores").document(storeId).update(mapOf(
                    "usingGlobalRate" to false,
                    "usdExchangeRate" to customRate,
                    "exchangeRate" to customRate,
                    "exchangeRateUpdatedAt" to com.google.firebase.Timestamp.now()
                )).await()
                onComplete(true)
            } catch (e: Exception) {
                onComplete(false)
            }
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
                            AdminModule.Jobs,
                            AdminModule.Products,
                            AdminModule.Users,
                            AdminModule.Orders,
                            AdminModule.Subscriptions,
                            AdminModule.Settings,
                            AdminModule.Analytics,
                            AdminModule.ExchangeRate
                        ),
                        state = state,
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
                        is AdminModule.Jobs -> AdminJobsManager(
                            jobs = state.jobs,
                            applications = state.applications,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Products -> AdminProductsManager(
                            products = state.products,
                            stores = state.stores,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Users -> AdminUsersManager(
                            users = state.users,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Orders -> AdminOrdersManager(
                            orders = state.orders,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Subscriptions -> AdminSubscriptionsManager(
                            requests = state.subscriptionRequests,
                            stores = state.stores,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Settings -> AdminSettingsManager(
                            settings = state.settings,
                            isArabic = isArabic,
                            viewModel = viewModel
                        )
                        is AdminModule.Analytics -> AdminAnalyticsManager(
                            orders = state.orders,
                            products = state.products,
                            stores = state.stores,
                            interactions = state.interactions,
                            isArabic = isArabic
                        )
                        is AdminModule.ExchangeRate -> AdminExchangeRateManager(
                            stores = state.stores,
                            products = state.products,
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
    state: AdminUiState = AdminUiState(),
    isArabic: Boolean,
    onModuleSelect: (AdminModule) -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Card(
                modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(1.dp, BrandSoftGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                    Text(
                        text = if (isArabic) "نظرة عامة على السوق الكلي" else "Marketplace Live Overview",
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        fontSize = 16.sp,
                        modifier = Modifier.padding(bottom = 12.dp)
                    )
                    
                    // Grid of metrics
                    val stats = listOf(
                        Triple(if (isArabic) "المستخدمين" else "Total Users", state.users.size.toString(), Icons.Default.People),
                        Triple(if (isArabic) "المتاجر" else "Total Stores", state.stores.size.toString(), Icons.Default.Storefront),
                        Triple(if (isArabic) "المنتجات" else "Total Products", state.products.size.toString(), Icons.Default.ShoppingBag),
                        Triple(if (isArabic) "الطلبات" else "Total Orders", state.orders.size.toString(), Icons.Default.ReceiptLong),
                        Triple(if (isArabic) "المبيعات (دولار)" else "Sales (USD)", "$" + state.orders.filter { it.currency == "USD" && it.status != "cancelled" }.sumOf { it.grandTotal }.toInt().toString(), Icons.Default.MonetizationOn),
                        Triple(if (isArabic) "المبيعات (ل.س)" else "Sales (SYP)", state.orders.filter { it.currency == "SYP" && it.status != "cancelled" }.sumOf { it.grandTotal }.toInt().toString() + " ل.س", Icons.Default.Payments),
                        Triple(if (isArabic) "بانتظار ترقية باقة" else "Pending Plans", state.subscriptionRequests.filter { it.status == "pending" }.size.toString(), Icons.Default.CardMembership),
                        Triple(if (isArabic) "بانتظار التحقق" else "Pending Verify", state.stores.filter { it.verificationStatus == "Pending" || it.verificationStatus == "Submitted" }.size.toString(), Icons.Default.Verified)
                    )
                    
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (i in stats.indices step 2) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                for (j in i..minOf(i + 1, stats.lastIndex)) {
                                    val stat = stats[j]
                                    Card(
                                        modifier = Modifier.weight(1f),
                                        colors = CardDefaults.cardColors(containerColor = BrandBackground.copy(alpha = 0.5f)),
                                        border = BorderStroke(1.dp, BrandSoftGray)
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth().padding(10.dp),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            Icon(stat.third, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Column {
                                                Text(stat.first, fontSize = 10.sp, color = BrandTextMuted)
                                                Text(stat.second, fontSize = 13.sp, fontWeight = FontWeight.Bold, color = BrandTextPrimary)
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

                        // --- Store Job Inspection ---
                        val storeJobs = viewModel.state.collectAsState().value.jobs.filter { it.storeId == store.id }
                        if (storeJobs.isNotEmpty()) {
                            val storeApps = viewModel.state.value.applications.filter { app -> storeJobs.any { it.id == app.jobId } }
                            val sActiveJobsCount = storeJobs.count { it.status == "active" }
                            val sClosedJobsCount = storeJobs.count { it.status == "closed" }
                            Text(
                                text = "Jobs: ${storeJobs.size} | Active: $sActiveJobsCount | Closed: $sClosedJobsCount | Applicants: ${storeApps.size}",
                                fontSize = 11.sp,
                                color = BrandPrimary,
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(bottom = 8.dp).background(BrandPrimary.copy(alpha = 0.1f), RoundedCornerShape(4.dp)).padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

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
    stores: List<Store>,
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
                                val adminProductStore = stores.find { it.id == product.storeId }
                                val adminRate = adminProductStore?.usdExchangeRate ?: 13500.0
                                Text(
                                    text = CurrencyManager.formatPrice(product.price, adminRate, isArabic),
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

fun formatAdminPrice(amount: Double, currency: String, isArabic: Boolean): String {
    return if (currency == "USD") {
        val symbol = if (isArabic) "$" else "USD"
        String.format("%.2f %s", amount, symbol)
    } else {
        val symbol = if (isArabic) "ل.س" else "SYP"
        String.format("%,d %s", amount.toLong(), symbol)
    }
}

@Composable
fun AdminOrdersManager(
    orders: List<Order>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedStatusTab by remember { mutableStateOf("All") }
    
    val statusFilters = listOf("All", "pending", "processing", "shipped", "delivered", "cancelled")
    
    val filteredOrders = orders.filter { order ->
        val matchesSearch = order.orderId.contains(searchQuery, ignoreCase = true) || 
                order.customerName.contains(searchQuery, ignoreCase = true)
        val matchesTab = selectedStatusTab == "All" || order.status.lowercase() == selectedStatusTab.lowercase()
        matchesSearch && matchesTab
    }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            label = { Text(if (isArabic) "البحث برقم الطلب أو اسم المشتري" else "Search by Order ID or Buyer Name") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        )
        
        ScrollableTabRow(
            selectedTabIndex = statusFilters.indexOf(selectedStatusTab).coerceAtLeast(0),
            edgePadding = 0.dp,
            containerColor = Color.Transparent,
            divider = {},
            modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
        ) {
            statusFilters.forEach { status ->
                val isSelected = selectedStatusTab == status
                Tab(
                    selected = isSelected,
                    onClick = { selectedStatusTab = status },
                    text = {
                        Text(
                            text = when(status) {
                                "All" -> if (isArabic) "الكل" else "All"
                                "pending" -> if (isArabic) "معلق" else "Pending"
                                "processing" -> if (isArabic) "قيد المعالجة" else "Processing"
                                "shipped" -> if (isArabic) "تم الشحن" else "Shipped"
                                "delivered" -> if (isArabic) "تم التسليم" else "Delivered"
                                "cancelled" -> if (isArabic) "ملغي" else "Cancelled"
                                else -> status
                            },
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                )
            }
        }
        
        if (filteredOrders.isEmpty()) {
            Box(modifier = Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                Text(
                    text = if (isArabic) "لا توجد طلبات تطابق الفلتر الحالي" else "No orders matching current filter",
                    color = BrandTextMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(filteredOrders) { order ->
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "طلب #${order.orderId.take(8)}" else "Order #${order.orderId.take(8)}",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = BrandTextPrimary
                                )
                                val statusColor = when (order.status.lowercase()) {
                                    "pending" -> Color(0xFFE65100)
                                    "processing" -> Color(0xFF0288D1)
                                    "shipped" -> Color(0xFF512DA8)
                                    "delivered" -> Color(0xFF2E7D32)
                                    else -> Color.Red
                                }
                                Box(
                                    modifier = Modifier
                                        .background(statusColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                                        .padding(horizontal = 8.dp, vertical = 4.dp)
                                ) {
                                    Text(
                                        text = order.status.uppercase(),
                                        color = statusColor,
                                        fontSize = 10.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = if (isArabic) "المشتري: ${order.customerName.ifEmpty { "غير معروف" }}" else "Buyer: ${order.customerName.ifEmpty { "Unknown" }}",
                                fontSize = 13.sp,
                                color = BrandTextPrimary
                            )
                            Text(
                                text = if (isArabic) "الهاتف: ${order.customerPhone.ifEmpty { "غير متوفر" }}" else "Phone: ${order.customerPhone.ifEmpty { "N/A" }}",
                                fontSize = 12.sp,
                                color = BrandTextMuted
                            )
                            Text(
                                text = if (isArabic) "العنوان: ${order.shippingAddress.ifEmpty { "غير متوفر" }}" else "Address: ${order.shippingAddress.ifEmpty { "N/A" }}",
                                fontSize = 12.sp,
                                color = BrandTextMuted
                            )
                            
                            Spacer(modifier = Modifier.height(8.dp))
                            HorizontalDivider(color = BrandSoftGray)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            order.items.forEach { item ->
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(text = "${item.productName} x${item.quantity}", fontSize = 12.sp, color = BrandTextPrimary)
                                    Text(text = formatAdminPrice(item.unitPrice * item.quantity, order.currency, isArabic), fontSize = 12.sp, color = BrandTextPrimary)
                                }
                            }
                            
                            Spacer(modifier = Modifier.height(6.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = if (isArabic) "المجموع الفرعي:" else "Subtotal:", fontSize = 11.sp, color = BrandTextMuted)
                                Text(text = formatAdminPrice(order.subtotal, order.currency, isArabic), fontSize = 11.sp, color = BrandTextMuted)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = if (isArabic) "ضريبة VAT (3%):" else "VAT (3%):", fontSize = 11.sp, color = BrandTextMuted)
                                Text(text = formatAdminPrice(order.vatAmount, order.currency, isArabic), fontSize = 11.sp, color = BrandTextMuted)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = if (isArabic) "أجور التوصيل:" else "Shipping Cost:", fontSize = 11.sp, color = BrandTextMuted)
                                Text(text = formatAdminPrice(order.shippingFee, order.currency, isArabic), fontSize = 11.sp, color = BrandTextMuted)
                            }
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = if (isArabic) "المجموع الكلي:" else "Grand Total:", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandPrimary)
                                Text(text = formatAdminPrice(order.grandTotal, order.currency, isArabic), fontWeight = FontWeight.Bold, fontSize = 14.sp, color = BrandPrimary)
                            }
                            
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                if (order.status.lowercase() == "pending") {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatusAdmin(order.orderId, "processing") { ok ->
                                                if (ok) Toast.makeText(context, "Moved to Processing", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "تأكيد واستلام" else "Accept", fontSize = 11.sp)
                                    }
                                }
                                if (order.status.lowercase() == "processing") {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatusAdmin(order.orderId, "shipped") { ok ->
                                                if (ok) Toast.makeText(context, "Moved to Shipped", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF512DA8)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "شحن الطلب" else "Ship", fontSize = 11.sp)
                                    }
                                }
                                if (order.status.lowercase() == "shipped") {
                                    Button(
                                        onClick = {
                                            viewModel.updateOrderStatusAdmin(order.orderId, "delivered") { ok ->
                                                if (ok) Toast.makeText(context, "Order Delivered", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "تسليم الطلب" else "Deliver", fontSize = 11.sp)
                                    }
                                }
                                if (order.status.lowercase() != "delivered" && order.status.lowercase() != "cancelled") {
                                    OutlinedButton(
                                        onClick = {
                                            viewModel.updateOrderStatusAdmin(order.orderId, "cancelled") { ok ->
                                                if (ok) Toast.makeText(context, "Order Cancelled", Toast.LENGTH_SHORT).show()
                                            }
                                        },
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Text(if (isArabic) "إلغاء الطلب" else "Cancel", fontSize = 11.sp, color = Color.Red)
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
fun AdminSubscriptionsManager(
    requests: List<SubscriptionRequest>,
    stores: List<Store>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var selectedStoreForManualUpgrade by remember { mutableStateOf<Store?>(null) }
    var showUpgradeDialog by remember { mutableStateOf(false) }
    var selectedTierOverride by remember { mutableStateOf("Starter") }

    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "طلبات ترقية الباقات المعلقة" else "Pending Subscription Requests",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandPrimary
            )
        }
        
        val pendingRequests = requests.filter { it.status == "pending" }
        if (pendingRequests.isEmpty()) {
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray)
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(24.dp), contentAlignment = Alignment.Center) {
                        Text(text = if (isArabic) "لا توجد طلبات معلقة حالياً" else "No pending requests currently", color = BrandTextMuted)
                    }
                }
            }
        } else {
            items(pendingRequests) { req ->
                val associatedStore = stores.firstOrNull { it.id == req.storeId }
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = associatedStore?.name ?: (if (isArabic) "متجر مجهول" else "Unknown Store"),
                                fontWeight = FontWeight.Bold,
                                color = BrandTextPrimary,
                                fontSize = 15.sp
                            )
                            Box(
                                modifier = Modifier
                                    .background(BrandPrimary.copy(alpha = 0.15f), RoundedCornerShape(6.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = req.requestedTier.uppercase(),
                                    color = BrandPrimary,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(text = "Requested: ${req.requestedTier} Tier", fontSize = 12.sp, color = BrandTextMuted)
                        Text(
                            text = "Date: " + java.text.DateFormat.getDateTimeInstance().format(java.util.Date(req.requestDate)),
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.handleSubscriptionRequest(req.requestId, req.storeId, req.requestedTier, true) { ok ->
                                        if (ok) Toast.makeText(context, "Subscription request Approved!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "قبول الترقية" else "Approve")
                            }
                            OutlinedButton(
                                onClick = {
                                    viewModel.handleSubscriptionRequest(req.requestId, req.storeId, req.requestedTier, false) { ok ->
                                        if (ok) Toast.makeText(context, "Subscription request Declined!", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "رفض" else "Decline", color = Color.Red)
                            }
                        }
                    }
                }
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = if (isArabic) "تخصيص الباقات والترقية اليدوية" else "Manual Plan Upgrades & Overrides",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = BrandPrimary
            )
        }
        
        if (stores.isEmpty()) {
            item {
                Text(if (isArabic) "لا توجد متاجر للتعديل يدوياً" else "No stores listed yet", color = BrandTextMuted)
            }
        } else {
            items(stores) { store ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = store.name, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 15.sp)
                            Text(
                                text = (if (isArabic) "الباقة الحالية: " else "Current Plan: ") + store.subscriptionTier,
                                color = BrandTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Button(
                            onClick = {
                                selectedStoreForManualUpgrade = store
                                selectedTierOverride = store.subscriptionTier
                                showUpgradeDialog = true
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                        ) {
                            Text(if (isArabic) "تغيير" else "Change")
                        }
                    }
                }
            }
        }
    }
    
    if (showUpgradeDialog && selectedStoreForManualUpgrade != null) {
        AlertDialog(
            onDismissRequest = { showUpgradeDialog = false },
            title = { Text(if (isArabic) "تغيير باقة المتجر يدوياً" else "Manual Plan Override") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(text = "Store: ${selectedStoreForManualUpgrade?.name}")
                    Text(text = if (isArabic) "اختر الباقة المناسبة:" else "Select Tier Plan:")
                    
                    val plans = listOf("Starter", "Growth", "Pro")
                    plans.forEach { plan ->
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.clickable { selectedTierOverride = plan }) {
                            RadioButton(
                                selected = selectedTierOverride == plan,
                                onClick = { selectedTierOverride = plan }
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(plan, fontSize = 14.sp)
                        }
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val storeId = selectedStoreForManualUpgrade?.id ?: return@Button
                        viewModel.setStoreTier(storeId, selectedTierOverride) { ok ->
                            if (ok) {
                                Toast.makeText(context, "Store tier override successful!", Toast.LENGTH_SHORT).show()
                                showUpgradeDialog = false
                            }
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text(if (isArabic) "تأكيد" else "Apply")
                }
            },
            dismissButton = {
                TextButton(onClick = { showUpgradeDialog = false }) {
                    Text(if (isArabic) "تراجع" else "Cancel")
                }
            }
        )
    }
}

@Composable
fun AdminSettingsManager(
    settings: com.example.core.utils.MarketplaceSettingsManager.MarketplaceSettings,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    var platformFeeInput by remember { mutableStateOf(settings.platformFeePercent.toString()) }
    var vatInput by remember { mutableStateOf(settings.vatPercent.toString()) }
    var shippingInput by remember { mutableStateOf(settings.defaultShippingFeeSyp.toString()) }
    var exchangeRateInput by remember { mutableStateOf(settings.defaultExchangeRate.toString()) }
    
    var newCityInput by remember { mutableStateOf("") }
    var citiesList by remember { mutableStateOf(settings.supportedCities) }
    
    var selectedMethods by remember { mutableStateOf(settings.supportedPaymentMethods) }
    
    LazyColumn(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = if (isArabic) "إعدادات الرسوم والضرائب الكلية" else "Global Fees & Tax Rates Configuration",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandPrimary
            )
        }
        
        item {
            OutlinedTextField(
                value = platformFeeInput,
                onValueChange = { platformFeeInput = it },
                label = { Text(if (isArabic) "عمولة المنصة الكلية (%)" else "Platform Service Fee (%)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            OutlinedTextField(
                value = vatInput,
                onValueChange = { vatInput = it },
                label = { Text(if (isArabic) "ضريبة القيمة المضافة لواصل بلس VAT (%)" else "VAT / Service Tax (%)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            OutlinedTextField(
                value = shippingInput,
                onValueChange = { shippingInput = it },
                label = { Text(if (isArabic) "أجور التوصيل الأساسية (ل.س)" else "Default Base Shipping Fee (SYP)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            OutlinedTextField(
                value = exchangeRateInput,
                onValueChange = { exchangeRateInput = it },
                label = { Text(if (isArabic) "سعر الصرف الافتراضي (دولار مقابل ليرة)" else "Base Conversion Exchange Rate (1 USD to SYP)") },
                modifier = Modifier.fillMaxWidth()
            )
        }
        
        item {
            Text(
                text = if (isArabic) "المناطق والمدن المدعومة للشحن" else "Supported Cities & Shipping Areas",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = newCityInput,
                    onValueChange = { newCityInput = it },
                    label = { Text(if (isArabic) "إضافة مدينة جديدة" else "Add New Delivery City") },
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = {
                        if (newCityInput.isNotBlank() && !citiesList.contains(newCityInput.trim())) {
                            citiesList = citiesList + newCityInput.trim()
                            newCityInput = ""
                        }
                    },
                    modifier = Modifier.height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add")
                }
            }
        }
        
        item {
            Row(
                modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                citiesList.forEach { city ->
                    AssistChip(
                        onClick = { /* No-op */ },
                        label = { Text(city) },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    citiesList = citiesList - city
                                },
                                modifier = Modifier.size(16.dp)
                            ) {
                                Icon(Icons.Default.Cancel, contentDescription = "Remove", tint = Color.Red, modifier = Modifier.size(12.dp))
                            }
                        }
                    )
                }
            }
        }
        
        item {
            Text(
                text = if (isArabic) "خيارات التنسيق المالي المعتمدة (تنسيق بائع ومشتري فقط)" else "Allowed Payment Channels (Marketplace Coordination Only)",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = BrandPrimary
            )
        }
        
        val paymentChannels = listOf("Cash On Delivery", "Syriatel Cash", "MTN Cash", "Bank Transfer")
        items(paymentChannels) { method ->
            val isChecked = selectedMethods.contains(method)
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        selectedMethods = if (isChecked) selectedMethods - method else selectedMethods + method
                    }
                    .padding(vertical = 4.dp)
            ) {
                Checkbox(
                    checked = isChecked,
                    onCheckedChange = {
                        selectedMethods = if (isChecked) selectedMethods - method else selectedMethods + method
                    }
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = method)
            }
        }
        
        item {
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = {
                    val finalFee = platformFeeInput.toDoubleOrNull() ?: settings.platformFeePercent
                    val finalVat = vatInput.toDoubleOrNull() ?: settings.vatPercent
                    val finalShipping = shippingInput.toDoubleOrNull() ?: settings.defaultShippingFeeSyp
                    val finalRate = exchangeRateInput.toDoubleOrNull() ?: settings.defaultExchangeRate
                    
                    val updated = settings.copy(
                        platformFeePercent = finalFee,
                        vatPercent = finalVat,
                        defaultShippingFeeSyp = finalShipping,
                        supportedCities = citiesList,
                        supportedPaymentMethods = selectedMethods,
                        defaultExchangeRate = finalRate
                    )
                    
                    viewModel.saveMarketplaceSettings(updated) { success ->
                        if (success) {
                            Toast.makeText(context, "Marketplace settings updated dynamically on Firestore!", Toast.LENGTH_SHORT).show()
                        } else {
                            Toast.makeText(context, "Failed to persist marketplace settings", Toast.LENGTH_SHORT).show()
                        }
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                modifier = Modifier.fillMaxWidth().height(48.dp)
            ) {
                Text(if (isArabic) "حفظ وحفظ الإعدادات" else "Publish Changes Live")
            }
        }
    }
}

@Composable
fun AdminAnalyticsManager(
    orders: List<Order>,
    products: List<Product>,
    stores: List<Store>,
    interactions: List<Map<String, Any>>,
    isArabic: Boolean
) {
    var selectedTab by remember { mutableStateOf("interact") }
    
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        TabRow(
            selectedTabIndex = if (selectedTab == "interact") 0 else if (selectedTab == "stores") 1 else 2,
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
        ) {
            Tab(
                selected = selectedTab == "interact",
                onClick = { selectedTab = "interact" },
                text = { Text(if (isArabic) "تفاعلات السلع" else "Item Views") }
            )
            Tab(
                selected = selectedTab == "stores",
                onClick = { selectedTab = "stores" },
                text = { Text(if (isArabic) "ترتيب المتاجر" else "Top Stores") }
            )
            Tab(
                selected = selectedTab == "finance",
                onClick = { selectedTab = "finance" },
                text = { Text(if (isArabic) "الأداء المالي" else "Finance") }
            )
        }
        
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxSize()) {
            if (selectedTab == "interact") {
                val viewCounts = interactions.filter { it["interactionType"] == "view" }
                    .groupBy { it["productId"] as? String ?: "" }
                    .mapValues { it.value.size }
                
                val favoriteCounts = interactions.filter { it["interactionType"] == "favorite" }
                    .groupBy { it["productId"] as? String ?: "" }
                    .mapValues { it.value.size }
                
                val trendingMap = products.map { prod ->
                    val views = viewCounts[prod.id] ?: 0
                    val favs = favoriteCounts[prod.id] ?: 0
                    prod to (views + favs * 3)
                }.sortedByDescending { it.second }.take(5)
                
                item {
                    Text(if (isArabic) "المنتجات الأكثر مشاهدة" else "Most Viewed Products", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                }
                
                val topViewed = products.map { it to (viewCounts[it.id] ?: 0) }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .take(3)
                
                if (topViewed.isEmpty()) {
                    item { Text(if (isArabic) "لا توجد تفاعلات مسجلة بعد" else "No view interactions recorded yet", color = BrandTextMuted, fontSize = 13.sp) }
                } else {
                    items(topViewed) { (prod, count) ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandSurface), border = BorderStroke(1.dp, BrandSoftGray)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$count " + (if (isArabic) "مشاهدة" else "Views"), color = BrandPrimary, fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isArabic) "السلع الأكثر تمييزاً بالمفضلة" else "Most Favorited Products", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                }
                
                val topFavs = products.map { it to (favoriteCounts[it.id] ?: 0) }
                    .filter { it.second > 0 }
                    .sortedByDescending { it.second }
                    .take(3)
                    
                if (topFavs.isEmpty()) {
                    item { Text(if (isArabic) "لا توجد تفضيلات مضافة بعد" else "No favorite interactions recorded yet", color = BrandTextMuted, fontSize = 13.sp) }
                } else {
                    items(topFavs) { (prod, count) ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandSurface), border = BorderStroke(1.dp, BrandSoftGray)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text("$count " + (if (isArabic) "إضافة مفضلة" else "Favs"), color = Color(0xFFD81B60), fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }
                
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(if (isArabic) "السلع الأكثر رواجاً (تريند)" else "Trending Products (Weighted Score)", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                }
                
                val activeTrends = trendingMap.filter { it.second > 0 }
                if (activeTrends.isEmpty()) {
                    item { Text(if (isArabic) "لا توجد نقاط كافية لتصنيف الرواج" else "Not enough interaction weight for trending", color = BrandTextMuted, fontSize = 13.sp) }
                } else {
                    items(activeTrends) { (prod, score) ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandSurface), border = BorderStroke(1.dp, BrandSoftGray)) {
                            Row(modifier = Modifier.fillMaxWidth().padding(12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(prod.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                                Text(if (isArabic) "نقاط الرواج: $score" else "Trend Weight: $score", color = Color(0xFF00ACC1), fontWeight = FontWeight.Bold, fontSize = 12.sp)
                            }
                        }
                    }
                }
            } else if (selectedTab == "stores") {
                val completedOrders = orders.filter { it.status != "cancelled" }
                val storeEarnings = completedOrders
                    .groupBy { it.storeId }
                    .mapValues { entry -> 
                        val name = entry.value.firstOrNull()?.storeName ?: "Store"
                        val count = entry.value.size
                        val total = entry.value.sumOf { it.grandTotal }
                        Triple(name, count, total)
                    }.values.sortedByDescending { it.third }.take(5)
                
                item {
                    Text(if (isArabic) "ترتيب أفضل المتاجر حسب المبيعات" else "Top Performing Stores by Revenue", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                }
                
                if (storeEarnings.isEmpty()) {
                    item { Text(if (isArabic) "لا توجد مبيعات كافية في المتاجر حالياً" else "No store sales recorded yet", color = BrandTextMuted, fontSize = 13.sp) }
                } else {
                    items(storeEarnings.toList()) { (name, count, total) ->
                        Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandSurface), border = BorderStroke(1.dp, BrandSoftGray)) {
                            Column(modifier = Modifier.fillMaxWidth().padding(12.dp)) {
                                Text(name, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                    Text(if (isArabic) "${count} طلبات منفذة" else "${count} orders completed", fontSize = 12.sp, color = BrandTextMuted)
                                    Text(formatAdminPrice(total, "SYP", isArabic) + " / " + formatAdminPrice(total / 14000.0, "USD", isArabic), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 13.sp)
                                }
                            }
                        }
                    }
                }
            } else if (selectedTab == "finance") {
                val activeOrders = orders.filter { it.status != "cancelled" }
                val usdTotal = activeOrders.filter { it.currency == "USD" }.sumOf { it.grandTotal }
                val sypTotal = activeOrders.filter { it.currency == "SYP" }.sumOf { it.grandTotal }
                
                val vatUsd = activeOrders.filter { it.currency == "USD" }.sumOf { it.vatAmount }
                val vatSyp = activeOrders.filter { it.currency == "SYP" }.sumOf { it.vatAmount }
                
                item {
                    Text(if (isArabic) "مؤشرات التنسيق المالي المتبادل" else "Finance, Revenue & System Commissions", fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 15.sp)
                }
                
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(8.dp),
                        border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier.padding(10.dp),
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.Info, null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                            Text(
                                text = if (isArabic) {
                                    "تنسيق مبيعات خارجي فقط - Marketplace Coordination Only"
                                } else {
                                    "Marketplace Coordination Only - Not Real Financial Processing"
                                },
                                fontWeight = FontWeight.Bold,
                                color = BrandPrimary,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
                
                item {
                    Card(modifier = Modifier.fillMaxWidth(), colors = CardDefaults.cardColors(containerColor = BrandSurface), border = BorderStroke(1.dp, BrandSoftGray)) {
                        Column(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "إجمالي المبيعات بالدولار:" else "Total USD Volume:", fontSize = 13.sp)
                                Text(formatAdminPrice(usdTotal, "USD", isArabic), fontWeight = FontWeight.Bold, color = BrandPrimary)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "إجمالي المبيعات بالليرة:" else "Total SYP Volume:", fontSize = 13.sp)
                                Text(formatAdminPrice(sypTotal, "SYP", isArabic), fontWeight = FontWeight.Bold, color = BrandPrimary)
                            }
                            HorizontalDivider(color = BrandSoftGray)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "إجمالي ضريبة VAT المستقطعة (USD):" else "System Collected VAT (USD):", fontSize = 12.sp, color = BrandTextMuted)
                                Text(formatAdminPrice(vatUsd, "USD", isArabic), fontWeight = FontWeight.Bold)
                            }
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (isArabic) "إجمالي ضريبة VAT المستقطعة (SYP):" else "System Collected VAT (SYP):", fontSize = 12.sp, color = BrandTextMuted)
                                Text(formatAdminPrice(vatSyp, "SYP", isArabic), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminExchangeRateManager(
    stores: List<Store>,
    products: List<Product>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val context = LocalContext.current
    val state by viewModel.state.collectAsState()
    val globalRate = state.settings.defaultExchangeRate
    
    var globalRateInput by remember(globalRate) { mutableStateOf(globalRate.toInt().toString()) }
    var isSavingGlobal by remember { mutableStateOf(false) }
    
    // Dialog states for custom store rate edit
    var storeToEditRate by remember { mutableStateOf<Store?>(null) }
    var customStoreRateInput by remember { mutableStateOf("") }
    var isSavingStoreRate by remember { mutableStateOf(false) }

    // Count statistics
    val storesUsingGlobal = stores.filter { it.usingGlobalRate }.size
    val storesUsingCustom = stores.filter { !it.usingGlobalRate }.size
    
    val customStores = stores.filter { !it.usingGlobalRate }
    val averageCustomRate = if (customStores.isNotEmpty()) {
        customStores.map { it.exchangeRate }.average()
    } else {
        0.0
    }
    
    val averageAllRate = if (stores.isNotEmpty()) {
        stores.map { it.exchangeRate }.average()
    } else {
        0.0
    }
    
    val minRate = if (stores.isNotEmpty()) {
        stores.minOfOrNull { it.exchangeRate } ?: 0.0
    } else {
        0.0
    }
    
    val maxRate = if (stores.isNotEmpty()) {
        stores.maxOfOrNull { it.exchangeRate } ?: 0.0
    } else {
        0.0
    }

    val maxCustomRateDistance = if (customStores.isNotEmpty()) {
        customStores.maxOfOrNull { kotlin.math.abs(it.exchangeRate - globalRate) } ?: 0.0
    } else {
        0.0
    }

    androidx.compose.runtime.LaunchedEffect(stores) {
        if (stores.isEmpty()) {
            Log.e("ExchangeRateAnalytics", "Aggregation empty: Layer 'Firestore write' or 'Repository mapping' returned 0 stores")
        } else {
            Log.d("ExchangeRateAnalytics", "Loaded ${stores.size} stores successfully in UI State collection.")
            val zeroRates = stores.filter { it.exchangeRate <= 0.0 }
            if (zeroRates.isNotEmpty()) {
                Log.e("ExchangeRateAnalytics", "Aggregation failure: ${zeroRates.size} stores have 0.0 exchange rate. Source: Firestore/Repository mapping layer error.")
            } else {
                Log.d("ExchangeRateAnalytics", "Analytics recomputation triggered. Averages and range are success: AvgAll = $averageAllRate, AvgCustom = $averageCustomRate, Min = $minRate, Max = $maxRate")
            }
        }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Module Headline Title
        item {
            Text(
                text = if (isArabic) "إدارة أسعار الصرف والعملات" else "Exchange Rate Manager",
                fontWeight = FontWeight.Bold,
                fontSize = 17.sp,
                color = BrandPrimary
            )
        }

        // 1. GLOBAL RATE CARD
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                border = BorderStroke(1.dp, BrandSoftGray),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = if (isArabic) "سعر الصرف العالمي الموحد" else "Unified Global Exchange Rate",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = Color.White
                    )
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "السعر العالمي الحالي:" else "Current Global Rate:",
                            fontSize = 13.sp,
                            color = BrandTextMuted
                        )
                        Text(
                            text = "1 USD = ${globalRate.toInt()} SYP",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = BrandPrimary
                        )
                    }

                    OutlinedTextField(
                        value = globalRateInput,
                        onValueChange = { globalRateInput = it },
                        label = { Text(if (isArabic) "سعر الصرف الجديد (ل.س)" else "New Exchange Rate (SYP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedContainerColor = BrandBackground,
                            unfocusedContainerColor = BrandBackground,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandSoftGray
                        ),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().testTag("admin_global_exchange_rate_input")
                    )

                    Button(
                        onClick = {
                            val rateVal = globalRateInput.toDoubleOrNull()
                            if (rateVal == null || rateVal <= 0) {
                                Toast.makeText(context, if (isArabic) "الرجاء إدخال سعر صرف صحيح" else "Please enter a valid exchange rate", Toast.LENGTH_SHORT).show()
                                return@Button
                            }
                            isSavingGlobal = true
                            viewModel.updateGlobalExchangeRate(rateVal) { success ->
                                isSavingGlobal = false
                                if (success) {
                                    Toast.makeText(context, if (isArabic) "تم تحديث سعر الصرف العالمي وتطبيقه بالكامل!" else "Global exchange rate updated & applied!", Toast.LENGTH_SHORT).show()
                                } else {
                                    Toast.makeText(context, if (isArabic) "حدث خطأ أثناء حفظ الإعدادات" else "Error updating settings", Toast.LENGTH_SHORT).show()
                                }
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.fillMaxWidth().height(44.dp).testTag("save_global_rate_btn")
                    ) {
                        if (isSavingGlobal) {
                            CircularProgressIndicator(color = Color.Black, modifier = Modifier.size(18.dp))
                        } else {
                            Text(
                                text = if (isArabic) "تطبيق على كافة المتاجر" else "Apply on All Stores",
                                fontWeight = FontWeight.Bold,
                                color = Color.Black
                            )
                        }
                    }
                }
            }
        }

        // 2. ANALYTICS CARDS (Grid layout: 2 cards in each row, 3 rows total)
        item {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isArabic) "إحصائيات وتحليلات أسعار الصرف" else "Exchange Rates Analytics",
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = BrandTextMuted
                )
                
                // Row 1: Global vs Custom counts
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 1: Stores Using Global Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "متاجر السعر العالمي" else "Using Global Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = storesUsingGlobal.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = BrandPrimary
                            )
                        }
                    }
                    // Card 2: Stores Using Custom Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "متاجر السعر المخصص" else "Using Custom Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = storesUsingCustom.toString(),
                                fontSize = 20.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFFFB74D)
                            )
                        }
                    }
                }

                // Row 2: Average rates (all vs custom)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 3: Average Exchange Rate (All)
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "متوسط أسعار الصرف" else "Avg Exchange Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%,d ل.س", averageAllRate.toInt()),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color.White
                            )
                        }
                    }
                    // Card 4: Average Custom Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "متوسط الأسعار المخصصة" else "Avg Custom Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%,d ل.س", averageCustomRate.toInt()),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF81C784)
                            )
                        }
                    }
                }

                // Row 3: Min vs Max rates across all stores
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Card 5: Minimum Exchange Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "أدنى سعر صرف مسجل" else "Min Exchange Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%,d ل.س", minRate.toInt()),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFF64B5F6)
                            )
                        }
                    }
                    // Card 6: Maximum Exchange Rate
                    Card(
                        modifier = Modifier.weight(1f),
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        border = BorderStroke(1.dp, BrandSoftGray)
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Text(
                                text = if (isArabic) "أعلى سعر صرف مسجل" else "Max Exchange Rate",
                                fontSize = 11.sp,
                                color = BrandTextMuted,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = String.format("%,d ل.س", maxRate.toInt()),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = Color(0xFFE57373)
                            )
                        }
                    }
                }
            }
        }

        // 3. STORES EXCHANGE RATE TABLE
        item {
            Text(
                text = if (isArabic) "سجل المتاجر ومعدلات الصرف" else "Sellers Currency Directory",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = BrandTextMuted
            )
        }

        if (stores.isEmpty()) {
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (isArabic) "لا توجد متاجر مسجلة حالياً" else "No registered stores in system registry",
                        color = BrandTextMuted,
                        fontSize = 13.sp
                    )
                }
            }
        } else {
            items(stores) { store ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
                    colors = CardDefaults.cardColors(containerColor = BrandSurface),
                    border = BorderStroke(1.dp, BrandSoftGray),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = store.name,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White,
                                    fontSize = 14.sp
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "1 USD = ${store.exchangeRate.toInt()} SYP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = BrandPrimary
                                )
                            }

                            // Rate status badge
                            val usingGlobal = store.usingGlobalRate
                            val badgeText = if (usingGlobal) {
                                if (isArabic) "سعر عالمي" else "Global Rate"
                            } else {
                                if (isArabic) "سعر مخصص" else "Custom Rate"
                            }
                            val badgeColor = if (usingGlobal) BrandPrimary.copy(alpha = 0.15f) else Color(0xFFFFB74D).copy(alpha = 0.15f)
                            val badgeBorder = if (usingGlobal) BrandPrimary else Color(0xFFFFB74D)

                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(badgeColor)
                                    .border(1.dp, badgeBorder, RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = badgeText,
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (usingGlobal) BrandPrimary else Color(0xFFFFB74D)
                                )
                            }
                        }

                        // Actions buttons
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            if (!store.usingGlobalRate) {
                                Button(
                                    onClick = {
                                        viewModel.applyGlobalRateStore(store.id, globalRate) { success ->
                                            if (success) {
                                                Toast.makeText(context, if (isArabic) "تطبيق السعر العالمي بنجاح" else "Applied global rate successfully", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    },
                                    modifier = Modifier.weight(1f).height(36.dp),
                                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary.copy(alpha = 0.2f)),
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Text(
                                        text = if (isArabic) "تطبيق السعر العالمي" else "Apply Global Rate",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandPrimary
                                    )
                                }
                            }
                            
                            Button(
                                onClick = {
                                    storeToEditRate = store
                                    customStoreRateInput = store.exchangeRate.toInt().toString()
                                },
                                modifier = Modifier.weight(1f).height(36.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BrandSoftGray),
                                shape = RoundedCornerShape(8.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "تعديل السعر" else "Edit Rate",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Dialog for setting exact custom store exchange rate
    if (storeToEditRate != null) {
        AlertDialog(
            onDismissRequest = { storeToEditRate = null },
            title = {
                Text(
                    text = if (isArabic) "تعديل سعر الصرف لـ ${storeToEditRate!!.name}" else "Set Rate for ${storeToEditRate!!.name}",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = if (isArabic) "قم بتعيين سعر الصرف مقابل 1 دولار أمريكي:" else "Set the conversion rate per 1 USD:",
                        fontSize = 12.sp,
                        color = BrandTextMuted
                    )
                    OutlinedTextField(
                        value = customStoreRateInput,
                        onValueChange = { customStoreRateInput = it },
                        label = { Text(if (isArabic) "سعر الصرف (ل.س)" else "Exchange Rate (SYP)") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedTextColor = Color.White,
                            unfocusedTextColor = Color.White,
                            focusedBorderColor = BrandPrimary,
                            unfocusedBorderColor = BrandSoftGray,
                            focusedContainerColor = BrandBackground,
                            unfocusedContainerColor = BrandBackground
                        ),
                        modifier = Modifier.fillMaxWidth().testTag("custom_store_rate_dialog_input")
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val storeVal = customStoreRateInput.toDoubleOrNull()
                        if (storeVal == null || storeVal <= 0.0) {
                            Toast.makeText(context, if (isArabic) "أدخل سعر صرف صحيح" else "Enter a correct rate", Toast.LENGTH_SHORT).show()
                            return@TextButton
                        }
                        isSavingStoreRate = true
                        viewModel.applyCustomRateStore(storeToEditRate!!.id, storeVal) { success ->
                            isSavingStoreRate = false
                            if (success) {
                                storeToEditRate = null
                                Toast.makeText(context, if (isArabic) "تم تعيين سعر الصرف بنجاح" else "Store rate configured successfully", Toast.LENGTH_SHORT).show()
                            }
                        }
                    },
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandPrimary)
                ) {
                    if (isSavingStoreRate) {
                        CircularProgressIndicator(color = BrandPrimary, modifier = Modifier.size(16.dp))
                    } else {
                        Text(if (isArabic) "تفعيل السعر المخصص" else "Verify custom rate")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { storeToEditRate = null }, colors = ButtonDefaults.textButtonColors(contentColor = Color.White)) {
                    Text(if (isArabic) "تراجع" else "Back")
                }
            },
            containerColor = BrandSurface
        )
    }
}

@Composable
fun AdminStatCard(title: String, value: String, icon: ImageVector) {
    Card(
        modifier = Modifier.width(140.dp),
        colors = CardDefaults.cardColors(containerColor = BrandSurface),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(imageVector = icon, contentDescription = title, tint = BrandPrimary, modifier = Modifier.size(24.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BrandTextPrimary)
            Text(text = title, fontSize = 12.sp, color = BrandTextMuted)
        }
    }
}

@Composable
fun AdminJobsManager(
    jobs: List<com.example.domain.model.Job>,
    applications: List<com.example.domain.model.JobApplication>,
    isArabic: Boolean,
    viewModel: AdminViewModel
) {
    val activeJobsCount = jobs.count { it.status == "active" }
    val pausedJobsCount = jobs.count { it.status == "paused" }
    val closedJobsCount = jobs.count { it.status == "closed" }

    val todayMillis = System.currentTimeMillis() - 24 * 60 * 60 * 1000
    val monthMillis = System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
    val jobsToday = jobs.count { it.createdAt >= todayMillis }
    val jobsMonth = jobs.count { it.createdAt >= monthMillis }
    val avgApps = if (jobs.isNotEmpty()) applications.size / jobs.size.toFloat() else 0f
    
    val storeJobsMap = jobs.groupBy { it.storeId }
    val mostActiveStoreId = storeJobsMap.maxByOrNull { it.value.size }?.key ?: ""
    val mostActiveStoreName = jobs.find { it.storeId == mostActiveStoreId }?.storeName ?: "N/A"

    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        // Stats Row
        Row(
            modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AdminStatCard(title = if (isArabic) "إجمالي الوظائف" else "Total Jobs", value = jobs.size.toString(), icon = Icons.Default.Work)
            AdminStatCard(title = if (isArabic) "نشط" else "Active", value = activeJobsCount.toString(), icon = Icons.Default.CheckCircle)
            AdminStatCard(title = if (isArabic) "مغلق" else "Closed", value = closedJobsCount.toString(), icon = Icons.Default.Close)
            AdminStatCard(title = if (isArabic) "اليوم" else "Today", value = jobsToday.toString(), icon = Icons.Default.Today)
            AdminStatCard(title = if (isArabic) "هذا الشهر" else "This Month", value = jobsMonth.toString(), icon = Icons.Default.DateRange)
            AdminStatCard(title = if (isArabic) "إجمالي الطلبات" else "Total Apps", value = applications.size.toString(), icon = Icons.Default.People)
            AdminStatCard(title = if (isArabic) "متوسط الطلب/العرض" else "Avg Apps/Job", value = String.format("%.1f", avgApps), icon = Icons.Default.Equalizer)
            AdminStatCard(title = if (isArabic) "المتجر الأنشط" else "Top Store", value = mostActiveStoreName.take(10), icon = Icons.Default.Storefront)
        }

        val context = LocalContext.current
        Text(
            text = if (isArabic) "إعلانات الوظائف" else "Job Postings",
            color = BrandTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        jobs.forEach { job ->
            val jobApps = applications.filter { it.jobId == job.id }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
                        Text(job.title, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 16.sp)
                        val statusColor = when (job.status) {
                            "active" -> BrandSuccess
                            "paused" -> BrandGoldenYellow
                            else -> BrandError
                        }
                        Text(
                            text = job.status.uppercase(),
                            color = statusColor,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text("${if (isArabic) "المتجر:" else "Store:"} ${job.storeName}", color = BrandTextMuted, fontSize = 14.sp)
                    Text("${if (isArabic) "الطلبات:" else "Applications:"} ${jobApps.size}", color = BrandPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                        if (job.status == "active") {
                            Button(
                                onClick = {
                                    viewModel.updateJobStatusAdmin(job.id, "paused") { success ->
                                        if (success) Toast.makeText(context, if (isArabic) "تم الإيقاف" else "Paused", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandGoldenYellow),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "إيقاف مؤقت" else "Pause")
                            }
                        } else {
                            Button(
                                onClick = {
                                    viewModel.updateJobStatusAdmin(job.id, "active") { success ->
                                        if (success) Toast.makeText(context, if (isArabic) "تم التفعيل" else "Activated", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandSuccess),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "تنشيط" else "Activate")
                            }
                        }

                        if (job.status != "closed") {
                            Button(
                                onClick = {
                                    viewModel.updateJobStatusAdmin(job.id, "closed") { success ->
                                        if (success) Toast.makeText(context, if (isArabic) "تم الإغلاق" else "Closed", Toast.LENGTH_SHORT).show()
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(containerColor = BrandError.copy(alpha = 0.8f)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "إغلاق النهائى" else "Close")
                            }
                        }

                        IconButton(onClick = {
                            viewModel.deleteJobAdmin(job.id) { success ->
                                if (success) Toast.makeText(context, if (isArabic) "تم الحذف" else "Deleted", Toast.LENGTH_SHORT).show()
                            }
                        }) {
                            Icon(Icons.Default.Delete, contentDescription = "Delete", tint = BrandError)
                        }
                    }
                }
            }
        }

        if (jobs.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(if (isArabic) "لا يوجد وظائف" else "No jobs found", color = BrandTextMuted)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)
        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = if (isArabic) "سجل الطلبات" else "Applications Ledger",
            color = BrandTextPrimary,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp
        )

        applications.forEach { app ->
            val jobTitle = jobs.find { it.id == app.jobId }?.title ?: "Unknown Job"
            val storeName = jobs.find { it.id == app.jobId }?.storeName ?: "Unknown Store"

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                shape = RoundedCornerShape(8.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = app.applicantName,
                        fontWeight = FontWeight.Bold,
                        color = BrandPrimary,
                        fontSize = 15.sp
                    )
                    Text(
                        text = "${if (isArabic) "الوظيفة:" else "Job:"} $jobTitle | ${if (isArabic) "المتجر:" else "Store:"} $storeName",
                        color = BrandTextMuted,
                        fontSize = 13.sp
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = java.text.DateFormat.getDateTimeInstance().format(java.util.Date(app.createdAt)),
                            fontSize = 11.sp,
                            color = BrandTextMuted
                        )
                        
                        val statusColor = when (app.status) {
                            "accepted" -> BrandSuccess
                            "rejected" -> BrandError
                            "reviewed" -> BrandGoldenYellow
                            else -> BrandTextMuted
                        }
                        Text(
                            text = app.status.uppercase(),
                            color = statusColor,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier
                                .background(statusColor.copy(alpha = 0.1f), RoundedCornerShape(4.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }

        if (applications.isEmpty()) {
            Box(modifier = Modifier.fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(if (isArabic) "لا يوجد طلبات" else "No applications found", color = BrandTextMuted)
            }
        }
    }
}
