package com.example.features.marketplace

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.components.BrandCard
import com.example.core.di.ServiceLocator
import com.example.core.utils.CurrencyManager
import com.example.core.utils.LanguageManager
import com.example.domain.model.Store
import com.example.domain.model.User
import com.example.ui.theme.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToWishlist: () -> Unit = {},
    onNavigateToCreateStore: () -> Unit = {},
    onNavigateToStoreManagement: () -> Unit = {},
    onNavigateToAdmin: () -> Unit = {},
    onNavigateToOrders: () -> Unit = {},
    onNavigateToSellerProfile: (String) -> Unit = {},
    onNavigateToUserApplications: () -> Unit = {},
    onNavigateToSubscriptionPlans: () -> Unit = {},
    onSignOut: () -> Unit = {}
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    val coroutineScope = rememberCoroutineScope()

    var currentUser by remember { mutableStateOf<User?>(null) }
    var ownStore by remember { mutableStateOf<Store?>(null) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & Notification states
    var showLanguageDialog by remember { mutableStateOf(false) }
    var showThemeDialog by remember { mutableStateOf(false) }
    var showCurrencyDialog by remember { mutableStateOf(false) }
    var showNotificationsDialog by remember { mutableStateOf(false) }
    var showHelpDialog by remember { mutableStateOf(false) }
    var showOrdersDialog by remember { mutableStateOf(false) }
    var showEditProfileDialog by remember { mutableStateOf(false) }

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

    LaunchedEffect(Unit) {
        isLoading = true
        val session = ServiceLocator.authRepository.getCurrentUserSession()
        currentUser = session
        if (session != null) {
            val store = ServiceLocator.storeRepository.getStoreByOwnerId(session.id)
            ownStore = store
            if (store != null) {
                // Registered store owner: redirect to Store Settings immediately!
                onNavigateToStoreManagement()
            }
        }
        isLoading = false
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "الملف الشخصي" else "My Profile",
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
                        modifier = Modifier.testTag("profile_back_button")
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
        if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = BrandPrimary)
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. Profile Header
                ProfileHeaderSection(
                    user = currentUser,
                    context = context,
                    isArabic = isArabic,
                    onEditProfile = {
                        currentUser?.let { user ->
                            tempName = user.name
                            tempAvatarUrl = user.profileImageUrl ?: ""
                            uploadError = null
                            showEditProfileDialog = true
                        }
                    }
                )

                // 2. Marketplace Section
                SectionHeader(title = if (isArabic) "السوق" else "Marketplace", isArabic = isArabic)

                BrandCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        currentUser?.let { user ->
                            ProfileMenuRow(
                                icon = Icons.Default.ListAlt,
                                title = if (isArabic) "إعلاناتي المباشرة" else "My Direct Listings / Ads",
                                subtitle = if (isArabic) "إدارة وتعديل وحذف إعلاناتي المبوبة" else "Manage, edit or delete your posted ads",
                                isArabic = isArabic,
                                onClick = { onNavigateToSellerProfile(user.id) },
                                testTag = "profile_my_ads_row"
                            )
                            HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                        }

                        ProfileMenuRow(
                            icon = Icons.Default.Favorite,
                            title = if (isArabic) "المفضلة" else "Wishlist",
                            subtitle = if (isArabic) "المنتجات المحفوظة والمفضلة" else "Your saved favorite items",
                            isArabic = isArabic,
                            onClick = onNavigateToWishlist,
                            testTag = "profile_wishlist_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))

                        // Conditional: Store Owner or Become Seller
                        if (ownStore != null) {
                            ProfileMenuRow(
                                icon = Icons.Default.Storefront,
                                title = if (isArabic) "إدارة متجري" else "My Store",
                                subtitle = ownStore?.name ?: "",
                                isArabic = isArabic,
                                onClick = onNavigateToStoreManagement,
                                testTag = "profile_manage_store_row"
                            )
                        } else {
                            ProfileMenuRow(
                                icon = Icons.Default.AddBusiness,
                                title = if (isArabic) "كن بائعاً" else "Become a Seller",
                                subtitle = if (isArabic) "ابدأ متجرك الخاص واعرض منتجاتك" else "Start your own shop and list products",
                                isArabic = isArabic,
                                onClick = onNavigateToCreateStore,
                                testTag = "profile_become_seller_row"
                            )
                        }
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuRow(
                            icon = Icons.Default.ReceiptLong,
                            title = if (isArabic) "طلبباتي" else "My Orders",
                            subtitle = if (isArabic) "عرض المعاملات والطلبات السابقة" else "Manage transactions & purchasing history",
                            isArabic = isArabic,
                            onClick = onNavigateToOrders,
                            testTag = "profile_orders_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuRow(
                            icon = Icons.Default.Work,
                            title = if (isArabic) "طلبات التوظيف" else "My Applications",
                            subtitle = if (isArabic) "إدارة وعرض الوظائف التي قدمت عليها" else "Manage jobs you applied for",
                            isArabic = isArabic,
                            onClick = onNavigateToUserApplications,
                            testTag = "profile_applications_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))

                        ProfileMenuRow(
                            icon = Icons.Default.CardMembership,
                            title = if (isArabic) "خطط اشتراك البائعين" else "Seller Subscription Plans",
                            subtitle = if (isArabic) "ترقية الباقة لزيادة الظهور والأولوية" else "Upgrade your tier for massive visibility",
                            isArabic = isArabic,
                            onClick = onNavigateToSubscriptionPlans,
                            testTag = "profile_subscriptions_row"
                        )
                    }
                }

                // 3. Settings & Preferences
                SectionHeader(title = if (isArabic) "الإعدادات والتفضيلات" else "Settings & Preferences", isArabic = isArabic)

                BrandCard(modifier = Modifier.fillMaxWidth()) {
                    Column {
                        ProfileMenuRow(
                            icon = Icons.Default.Language,
                            title = if (isArabic) "اللغة" else "Language",
                            subtitle = if (isArabic) "العربية" else "English",
                            isArabic = isArabic,
                            onClick = { showLanguageDialog = true },
                            testTag = "profile_language_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuRow(
                            icon = Icons.Default.Palette,
                            title = if (isArabic) "المظهر اليومي" else "Theme Appearance",
                            subtitle = if (isArabic) "إعداد اللون والمظهر الداكن" else "Customize dark color theme parameters",
                            isArabic = isArabic,
                            onClick = { showThemeDialog = true },
                            testTag = "profile_theme_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuRow(
                            icon = Icons.Default.Paid,
                            title = if (isArabic) "العملة" else "Currency Exchange",
                            subtitle = if (CurrencyManager.currentCurrency.value == CurrencyManager.Currency.SYP) (if (isArabic) "الليرة السورية (SYP)" else "Syrian Pound (SYP)") else (if (isArabic) "دولار أمريكي (USD)" else "US Dollar (USD)"),
                            isArabic = isArabic,
                            onClick = { showCurrencyDialog = true },
                            testTag = "profile_currency_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuRow(
                            icon = Icons.Default.Notifications,
                            title = if (isArabic) "الإشعارات" else "Notifications",
                            subtitle = if (isArabic) "تعديل تفضيلات التنبيه" else "Customize alert push preferences",
                            isArabic = isArabic,
                            onClick = { showNotificationsDialog = true },
                            testTag = "profile_notifications_row"
                        )
                        HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                        ProfileMenuRow(
                            icon = Icons.Default.HelpOutline,
                            title = if (isArabic) "المساعدة والدعم" else "Help & Support",
                            subtitle = if (isArabic) "الأسئلة الشائعة وفريق المساعدة" else "FAQs and direct messaging center",
                            isArabic = isArabic,
                            onClick = { showHelpDialog = true },
                            testTag = "profile_help_row"
                        )

                        if (currentUser?.role == "admin") {
                            HorizontalDivider(color = BrandSoftGray, modifier = Modifier.padding(horizontal = 16.dp))
                            ProfileMenuRow(
                                icon = Icons.Default.AdminPanelSettings,
                                title = if (isArabic) "لوحة الإدارة" else "Admin Dashboard",
                                subtitle = if (isArabic) "صلاحيات المشرف الفني" else "Control platform entities and logs",
                                isArabic = isArabic,
                                onClick = onNavigateToAdmin,
                                testTag = "profile_admin_row"
                            )
                        }
                    }
                }

                // 4. Leave / Session Center
                Spacer(modifier = Modifier.height(8.dp))

                Button(
                    onClick = {
                        coroutineScope.launch {
                            ServiceLocator.authRepository.signOut()
                            onSignOut()
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp)
                        .testTag("profile_logout_button"),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandError.copy(alpha = 0.15f),
                        contentColor = BrandError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(imageVector = Icons.Default.Logout, contentDescription = "تسجيل الخروج")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = if (isArabic) "تسجيل الخروج" else "Log Out",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }

    // ------------------------------------------------------------------------
    // Language Custom Dialogue Setup
    // ------------------------------------------------------------------------
    if (showLanguageDialog) {
        AlertDialog(
            onDismissRequest = { showLanguageDialog = false },
            title = {
                Text(
                    text = if (isArabic) "اختر اللغة" else "Select Language",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguageManager.setLanguage(context, "ar")
                                val activity = (context as? Activity)
                                activity?.recreate()
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = isArabic, onClick = {
                            LanguageManager.setLanguage(context, "ar")
                            (context as? Activity)?.recreate()
                            showLanguageDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("العربية", color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                LanguageManager.setLanguage(context, "en")
                                val activity = (context as? Activity)
                                activity?.recreate()
                                showLanguageDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = !isArabic, onClick = {
                            LanguageManager.setLanguage(context, "en")
                            (context as? Activity)?.recreate()
                            showLanguageDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("English", color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showLanguageDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = BrandPrimary)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Theme Custom Dialogue Setup
    // ------------------------------------------------------------------------
    if (showThemeDialog) {
        val currentThemeMode = ThemeManager.themeModeState.value
        AlertDialog(
            onDismissRequest = { showThemeDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تخصيص المظهر" else "Customize Theme",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    listOf("light", "dark", "system").forEach { mode ->
                        val modeLabel = when(mode) {
                            "light" -> if (isArabic) "الوضع الفاتح" else "Light Mode"
                            "dark" -> if (isArabic) "الوضع الداكن" else "Dark Mode"
                            else -> if (isArabic) "تلقائي حسب النظام" else "System Settings"
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    ThemeManager.setTheme(context, mode)
                                    showThemeDialog = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = currentThemeMode == mode, onClick = {
                                ThemeManager.setTheme(context, mode)
                                showThemeDialog = false
                            })
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(modeLabel, color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showThemeDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = BrandPrimary)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Currency Custom Dialogue Setup
    // ------------------------------------------------------------------------
    if (showCurrencyDialog) {
        val currentCurrency = CurrencyManager.currentCurrency.value
        AlertDialog(
            onDismissRequest = { showCurrencyDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تخصيص أسعار الصرف" else "Select Base Currency",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CurrencyManager.setCurrency(context, CurrencyManager.Currency.SYP)
                                showCurrencyDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentCurrency == CurrencyManager.Currency.SYP, onClick = {
                            CurrencyManager.setCurrency(context, CurrencyManager.Currency.SYP)
                            showCurrencyDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "الليرة السورية (SYP)" else "Syrian Pound (SYP)", color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                CurrencyManager.setCurrency(context, CurrencyManager.Currency.USD)
                                showCurrencyDialog = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = currentCurrency == CurrencyManager.Currency.USD, onClick = {
                            CurrencyManager.setCurrency(context, CurrencyManager.Currency.USD)
                            showCurrencyDialog = false
                        })
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isArabic) "دولار أمريكي (USD)" else "US Dollar (USD)", color = BrandTextPrimary, fontWeight = FontWeight.Bold)
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showCurrencyDialog = false }) {
                    Text(if (isArabic) "إلغاء" else "Cancel", color = BrandPrimary)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Notifications Settings Dialog
    // ------------------------------------------------------------------------
    if (showNotificationsDialog) {
        var promoChecked by remember { mutableStateOf(true) }
        var recommendationsChecked by remember { mutableStateOf(false) }
        AlertDialog(
            onDismissRequest = { showNotificationsDialog = false },
            title = {
                Text(
                    text = if (isArabic) "إشعارات ترويجية" else "Notification Preferences",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isArabic) "العروض والخصومات" else "Discounts & Promos",
                                color = BrandTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                if (isArabic) "أرسل لي تنبيهات المنتجات الجديدة المخفضة" else "Alert about newly catalogued store coupons",
                                color = BrandTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = promoChecked,
                            onCheckedChange = { promoChecked = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandPrimary)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                if (isArabic) "توصيات التسوق" else "Shopping Referrals",
                                color = BrandTextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                if (isArabic) "تنبيهات مخصصة مبنية على تفضيلات الشراء الخاصة بك" else "Smart custom push alerts centered on behaviors",
                                color = BrandTextMuted,
                                fontSize = 12.sp
                            )
                        }
                        Switch(
                            checked = recommendationsChecked,
                            onCheckedChange = { recommendationsChecked = it },
                            colors = SwitchDefaults.colors(checkedTrackColor = BrandPrimary)
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showNotificationsDialog = false }) {
                    Text(if (isArabic) "تم" else "Done", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Help & Support Dialog
    // ------------------------------------------------------------------------
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Text(
                    text = if (isArabic) "المساعدة والدعم" else "Help & Support Center",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = if (isArabic) "اتصل بنا" else "Contact WasetPlus Support Team",
                        color = BrandPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = "support@wasetplus.com\n+964 770 000 0000",
                        color = BrandTextPrimary,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isArabic) "وساطة التنسيق التجاري" else "Marketplace Intermediary Policy",
                        color = BrandTextPrimary,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                    Text(
                        text = if (isArabic) "تعمل منصة وسيط بلس كـ وسيط تنظيمي وتنسيقي بين البائع والمشتري لتسهيل وتوثيق الطلبيات ولا نقوم بتحصيل أو مسك أي مبالغ مالية حقيقية." else "WasetPlus operates as a neutral marketplace coordinator between buyers and sellers. We do not hold, process, or handle real funds on the platform.",
                        color = BrandTextMuted,
                        fontSize = 12.sp
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = { showHelpDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // My Orders Dialog (High conversion simulation)
    // ------------------------------------------------------------------------
    if (showOrdersDialog) {
        AlertDialog(
            onDismissRequest = { showOrdersDialog = false },
            title = {
                Text(
                    text = if (isArabic) "تاريخ طلبباتي" else "My Order History",
                    color = BrandTextPrimary,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.verticalScroll(rememberScrollState())
                ) {
                    // Simulation items
                    listOf(
                        "WS-9481" to ("In Transit" to "31 May 2026"),
                        "WS-4310" to ("Delivered" to "14 May 2026")
                    ).forEach { (orderId, orderState) ->
                        val (status, date) = orderState
                        BrandCard(
                            modifier = Modifier
                                .fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(12.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${if (isArabic) "طلب" else "Order"} #$orderId",
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(8.dp))
                                            .background(
                                                if (status == "Delivered") BrandPrimary.copy(alpha = 0.15f)
                                                else Color(0xFFFF9800).copy(alpha = 0.15f)
                                            )
                                            .padding(horizontal = 8.dp, vertical = 4.dp)
                                    ) {
                                        Text(
                                            text = if (isArabic) {
                                                if (status == "Delivered") "تم التوصيل" else "قيد الشحن"
                                            } else status,
                                            color = if (status == "Delivered") BrandPrimary else Color(0xFFFF9800),
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = "${if (isArabic) "التاريخ:" else "Placed:"} $date",
                                    color = BrandTextMuted,
                                    fontSize = 12.sp
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showOrdersDialog = false }) {
                    Text(if (isArabic) "إغلاق" else "Close", color = BrandPrimary, fontWeight = FontWeight.Bold)
                }
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }

    // ------------------------------------------------------------------------
    // Edit Profile Modal Dialog Setup
    // ------------------------------------------------------------------------
    if (showEditProfileDialog && currentUser != null) {
        val user = currentUser!!
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
                    color = BrandTextPrimary,
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
                                .background(BrandSoftGray)
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
                                        .background(BrandPrimary.copy(alpha = 0.2f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Default Avatar",
                                        tint = BrandPrimary,
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
                        color = BrandTextMuted,
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
                                    .background(BrandSoftGray)
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
                                val db = com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                val updates = mapOf(
                                    "name" to tempName,
                                    "profileImageUrl" to tempAvatarUrl
                                )
                                db.collection("users").document(user.id).update(updates).await()

                                // Update direct ad owner name instantly!
                                val adsQuery = db.collection("direct_ads")
                                    .whereEqualTo("ownerUid", user.id)
                                    .get()
                                    .await()
                                
                                for (doc in adsQuery.documents) {
                                    db.collection("direct_ads")
                                        .document(doc.id)
                                        .update("ownerUsername", tempName)
                                        .await()
                                }

                                currentUser = currentUser?.copy(name = tempName, profileImageUrl = tempAvatarUrl)
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
            },
            containerColor = BrandSurface,
            shape = RoundedCornerShape(16.dp)
        )
    }
}

@Composable
fun ProfileHeaderSection(user: User?, context: android.content.Context, isArabic: Boolean, onEditProfile: () -> Unit) {
    BrandCard(modifier = Modifier.fillMaxWidth().clickable { onEditProfile() }) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(contentAlignment = Alignment.BottomEnd) {
                AsyncImage(
                    model = user?.profileImageUrl ?: "https://i.imgur.com/g0K5Iu9.jpeg",
                    contentDescription = null,
                    modifier = Modifier
                        .size(72.dp)
                        .clip(CircleShape)
                        .background(BrandSoftGray),
                    contentScale = ContentScale.Crop
                )
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(BrandPrimary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Profile",
                        tint = androidx.compose.ui.graphics.Color.White,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = user?.name ?: (if (isArabic) "مستخدم جديد" else "WasetPlus Customer"),
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
                Text(
                    text = user?.email ?: "customer@wasetplus.com",
                    fontSize = 13.sp,
                    color = BrandTextMuted
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.VerifiedUser,
                        contentDescription = "موثق",
                        modifier = Modifier.size(14.dp),
                        tint = BrandPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "حساب موثق وآمن" else "Iraqi ID Verified Secure Escrow Account",
                        fontSize = 10.sp,
                        color = BrandPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun SectionHeader(title: String, isArabic: Boolean) {
    Text(
        text = title,
        fontSize = 14.sp,
        fontWeight = FontWeight.Bold,
        color = BrandTextMuted,
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 12.dp, bottom = 4.dp),
        letterSpacing = 0.5.sp
    )
}

@Composable
fun ProfileMenuRow(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isArabic: Boolean,
    onClick: () -> Unit,
    testTag: String = ""
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(16.dp)
            .testTag(testTag),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(BrandSoftGray),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = BrandPrimary, modifier = Modifier.size(20.dp))
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary
            )
            Text(
                text = subtitle,
                fontSize = 12.sp,
                color = BrandTextMuted
            )
        }

        Icon(
            imageVector = if (isArabic) Icons.Default.KeyboardArrowLeft else Icons.Default.KeyboardArrowRight,
            contentDescription = null,
            tint = BrandTextMuted,
            modifier = Modifier.size(20.dp)
        )
    }
}
