package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CardMembership
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.*

data class SubscriptionPlan(
    val id: String,
    val nameEn: String,
    val nameAr: String,
    val descEn: String,
    val descAr: String,
    val featuresEn: List<String>,
    val featuresAr: List<String>,
    val primaryColor: Color,
    val bgSoftColor: Color
)

@Composable
fun SubscriptionPlansSection(
    state: MarketplaceUiState,
    onRequestPlan: (String) -> Unit,
    isAr: Boolean,
    modifier: Modifier = Modifier
) {
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
            descEn = "Higher priority search visibility & verifications",
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
            descEn = "Premium placement opportunities and full seller rewards",
            descAr = "مساحات ترويجية فائقة ومكافآت بائع كاملة",
            featuresEn = listOf("Premium homepage placement opportunities", "Elite Pro Badge identifier", "Top ranking weight in discovery algorithm"),
            featuresAr = listOf("فرص الصدارة على الصفحة الرئيسية", "شارة بائع محترف النخبة", "الوزن الأقصى للظهور وخوارزميات الاكتشاف"),
            primaryColor = Color(0xFF8A2BE2),
            bgSoftColor = Color(0xFF8A2BE2).copy(alpha = 0.05f)
        )
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 24.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = if (isAr) "خطط اشتراك البائعين" else "Seller Subscription Plans",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
                Text(
                    text = if (isAr) "اختر الباقة المناسبة لتنمية أعمالك متى شئت" else "Choose the plan that powers your local store",
                    fontSize = 12.sp,
                    color = BrandTextMuted
                )
            }
            Icon(
                imageVector = Icons.Default.CardMembership,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        Spacer(modifier = Modifier.height(14.dp))

        LazyRow(
            contentPadding = PaddingValues(horizontal = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            items(plans) { plan ->
                PlanCard(
                    plan = plan,
                    state = state,
                    onRequestPlan = onRequestPlan,
                    isAr = isAr
                )
            }
        }
    }
}

@Composable
fun PlanCard(
    plan: SubscriptionPlan,
    state: MarketplaceUiState,
    onRequestPlan: (String) -> Unit,
    isAr: Boolean
) {
    val store = state.ownerStore
    val pendingRequests = state.pendingRequests

    val hasStore = store != null
    val currentPlan = store?.subscriptionTier ?: "Starter" // newly created store fallback

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

    Box(
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BrandSurface)
            .border(
                1.5.dp, 
                if (isCurrent) plan.primaryColor else plan.primaryColor.copy(alpha = 0.15f), 
                RoundedCornerShape(16.dp)
            )
            .padding(18.dp)
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            // Plan Header
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isAr) plan.nameAr else plan.nameEn,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = plan.primaryColor
                    )
                    if (isCurrent) {
                        Surface(
                            color = plan.primaryColor.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isAr) "نشط" else "ACTIVE",
                                color = plan.primaryColor,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    } else if (isPending) {
                        Surface(
                            color = Color(0xFFFFA500).copy(alpha = 0.15f),
                            shape = RoundedCornerShape(8.dp)
                        ) {
                            Text(
                                text = if (isAr) "قيد المراجعة" else "PENDING",
                                color = Color(0xFFFF8C00),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(6.dp))
                
                Text(
                    text = if (isAr) plan.descAr else plan.descEn,
                    fontSize = 12.sp,
                    color = BrandTextMuted,
                    lineHeight = 16.sp,
                    minLines = 2
                )
            }

            Divider(
                color = plan.primaryColor.copy(alpha = 0.12f),
                thickness = 1.dp,
                modifier = Modifier.padding(vertical = 12.dp)
            )

            // Features list
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.weight(1f, fill = false)
            ) {
                val features = if (isAr) plan.featuresAr else plan.featuresEn
                features.forEach { feature ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.Top,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = plan.primaryColor,
                            modifier = Modifier.size(15.dp).padding(top = 2.dp)
                        )
                        Text(
                            text = feature,
                            fontSize = 11.sp,
                            color = BrandTextPrimary,
                            lineHeight = 14.sp
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(18.dp))

            // CTA Button
            Button(
                onClick = { onRequestPlan(plan.id) },
                enabled = isButtonEnabled,
                colors = ButtonDefaults.buttonColors(
                    containerColor = plan.primaryColor,
                    contentColor = Color.White,
                    disabledContainerColor = if (isCurrent) plan.primaryColor.copy(alpha = 0.15f) else BrandSoftGray,
                    disabledContentColor = if (isCurrent) plan.primaryColor else BrandTextMuted
                ),
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp)
            ) {
                Text(
                    text = buttonText,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
