package com.example.features.marketplace

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.WorkspacePremium
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.core.di.ServiceLocator
import com.example.core.utils.LanguageManager
import com.example.domain.usecase.GetSubscriptionRequestsByStoreUseCase
import com.example.domain.usecase.SubmitSubscriptionRequestUseCase
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SubscriptionPlansScreen(
    onNavigateBack: () -> Unit,
    onNavigateToCreateStore: () -> Unit
) {
    val context = LocalContext.current
    val isAr = LanguageManager.isArabic(context)

    val viewModel: MarketplaceViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            return MarketplaceViewModel(
                ServiceLocator.authRepository,
                ServiceLocator.storeRepository,
                SubmitSubscriptionRequestUseCase(ServiceLocator.subscriptionRepository),
                GetSubscriptionRequestsByStoreUseCase(ServiceLocator.subscriptionRepository)
            ) as T
        }
    })

    val state by viewModel.state.collectAsStateWithLifecycle()

    val plans = listOf(
        SubscriptionPlan(
            id = "Starter",
            nameEn = "Starter",
            nameAr = "الباقة المبتدئة",
            descEn = "Essential local retail presence setup",
            descAr = "إثبات وجود محلي أساسي لمتجرك",
            featuresEn = listOf("Basic store listing", "Iraqi Dinar & USD listings", "Standard search visibility"),
            featuresAr = listOf("إدراج المتجر الأساسي", "عرض دينار / دولار / ليرة", "البحث والظهور القياسي"),
            primaryColor = BrandPrimary,
            bgSoftColor = BrandPrimary.copy(alpha = 0.05f)
        ),
        SubscriptionPlan(
            id = "Growth",
            nameEn = "Growth",
            nameAr = "باقة النمو",
            descEn = "Higher priority search visibility",
            descAr = "أولوية أعلى في الظهور وعمليات التحقق",
            featuresEn = listOf("Increased search visibility", "Growth Badge display on cards", "Standard seller verification eligibility"),
            featuresAr = listOf("زيادة أولوية الظهور بالبحث", "شارة متجر متنامي على البطاقات", "أهلية التقديم للتحقق القياسي"),
            primaryColor = Color(0xFF008080),
            bgSoftColor = Color(0xFF008080).copy(alpha = 0.05f)
        ),
        SubscriptionPlan(
            id = "Pro",
            nameEn = "Pro",
            nameAr = "الباقة الاحترافية",
            descEn = "Premium placement and full seller rewards",
            descAr = "مساحات ترويجية فائقة ومكافآت بائع كاملة",
            featuresEn = listOf("Premium homepage placement opportunities", "Elite Pro Badge identifier", "Top ranking weight in discovery algorithm"),
            featuresAr = listOf("فرص الصدارة على الصفحة الرئيسية", "شارة بائع محترف النخبة", "الوزن الأقصى للظهور وخوارزميات الاكتشاف"),
            primaryColor = Color(0xFF8A2BE2),
            bgSoftColor = Color(0xFF8A2BE2).copy(alpha = 0.05f)
        )
    )

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isAr) "خطط الاشتراك المميزة" else "Premium Subscriptions",
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
                        modifier = Modifier.testTag("subscription_plans_back_button")
                    ) {
                        Icon(
                            imageVector = if (isAr) Icons.AutoMirrored.Filled.ArrowForward else Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "السابق",
                            tint = BrandTextPrimary
                        )
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Header Hero Banner with premium UX design
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(24.dp))
                    .background(
                        Brush.linearGradient(
                            colors = listOf(
                                BrandPrimary,
                                BrandPrimary.copy(alpha = 0.85f),
                                Color(0xFF8A2BE2)
                            )
                        )
                    )
                    .padding(24.dp)
            ) {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.WorkspacePremium,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = if (isAr) "ضاعف مبيعاتك وحضورك في السوق" else "Boost Your Sales & Presence",
                        color = Color.White,
                        fontSize = 20.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (isAr) 
                            "اختر إحدى باقاتنا المميزة للحصول على أولوية ظهور لطلبك ومنتجاتك ورفع هوامش المبيعات" 
                        else 
                            "Choose a tier to gain massive indexing boosts, professional premium store badges & top ranking placement",
                        color = Color.White.copy(alpha = 0.85f),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 18.sp
                    )
                }
            }

            // Current state checker card
            val store = state.ownerStore
            val pendingRequests = state.pendingRequests
            val hasStore = store != null
            val currentPlan = store?.subscriptionTier ?: "Starter"

            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = BrandSurface),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isAr) "حالة متجرك الحالي" else "Your Store Status",
                            style = MaterialTheme.typography.bodySmall,
                            color = BrandTextMuted,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (hasStore) store!!.name else (if (isAr) "لا يوجد متجر مسجل" else "No store launched yet"),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = BrandTextPrimary
                        )
                    }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(BrandPrimary.copy(alpha = 0.1f))
                            .padding(horizontal = 14.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (hasStore) currentPlan.uppercase() else (if (isAr) "غير مشترك" else "INACTIVE"),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary
                        )
                    }
                }
            }

            // Direct actions for user without a store
            if (!hasStore) {
                Button(
                    onClick = onNavigateToCreateStore,
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Text(
                        text = if (isAr) "إطلاق متجر جديد الآن" else "Launch New Store Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp
                    )
                }
            }

            Text(
                text = if (isAr) "الباقات المتاحة" else "Available Subscription Plans",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = BrandTextPrimary,
                modifier = Modifier.padding(top = 8.dp)
            )

            // Subscription lists with updated layout
            plans.forEach { plan ->
                val isCurrent = hasStore && currentPlan == plan.id
                val isPending = hasStore && pendingRequests.any { it.requestedTier == plan.id }
                val isSubscribing = state.subscribingTier == plan.id

                val buttonText = when {
                    !hasStore -> if (isAr) "يرجى إنشاء متجر أولاً" else "Launch Store First"
                    isCurrent -> if (isAr) "الباقة الحالية" else "Current Plan"
                    isPending -> if (isAr) "الطلب معلق" else "Request Pending"
                    isSubscribing -> if (isAr) "جاري الإرسال..." else "Submitting..."
                    else -> if (isAr) "طلب الباقة" else "Request Plan"
                }

                val isButtonEnabled = hasStore && !isCurrent && !isPending && !isSubscribing

                ElevatedCard(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(
                            width = if (isCurrent) 2.dp else 0.dp,
                            color = if (isCurrent) plan.primaryColor else Color.Transparent,
                            shape = RoundedCornerShape(18.dp)
                        ),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.elevatedCardColors(containerColor = BrandSurface)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text(
                                    text = if (isAr) plan.nameAr else plan.nameEn,
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = plan.primaryColor
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    text = if (isAr) plan.descAr else plan.descEn,
                                    fontSize = 12.sp,
                                    color = BrandTextMuted
                                )
                            }

                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(plan.primaryColor.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = if (plan.id == "Pro") Icons.Default.Star else Icons.Default.WorkspacePremium,
                                    contentDescription = null,
                                    tint = plan.primaryColor,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }

                        // Feature checkmarks
                        val features = if (isAr) plan.featuresAr else plan.featuresEn
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            features.forEach { feature ->
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = "Eligible",
                                        tint = plan.primaryColor,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Spacer(modifier = Modifier.width(10.dp))
                                    Text(
                                        text = feature,
                                        fontSize = 13.sp,
                                        color = BrandTextPrimary
                                    )
                                }
                            }
                        }

                        Divider(color = BrandSoftGray.copy(alpha = 0.5f))

                        Button(
                            onClick = {
                                viewModel.requestSubscription(plan.id)
                                Toast.makeText(
                                    context,
                                    if (isAr) "تم تقديم طلب الترقية بنجاح بنجاح" else "Upgrade Request Submitted Successfully!",
                                    Toast.LENGTH_LONG
                                ).show()
                            },
                            enabled = isButtonEnabled,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isCurrent) plan.primaryColor.copy(alpha = 0.15f) else plan.primaryColor,
                                contentColor = if (isCurrent) plan.primaryColor else Color.White
                            ),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                        ) {
                            Text(
                                text = buttonText,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
