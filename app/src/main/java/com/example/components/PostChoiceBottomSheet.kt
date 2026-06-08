package com.example.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.core.utils.LanguageManager
import com.example.ui.theme.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PostChoiceBottomSheetContent(
    onDismiss: () -> Unit,
    onNavigateToCreateStore: () -> Unit,
    onNavigateToPostDirectAd: () -> Unit
) {
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)
    
    // We can track selection state, defaulting to nothing or the recommended option (1 = Create Store)
    var selectedOption by remember { mutableStateOf<Int?>(1) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color(0xFF0A0B0D)) // Dark theme #0A0B0D background
            .padding(24.dp)
            .navigationBarsPadding(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Grab Handle
        Box(
            modifier = Modifier
                .width(40.dp)
                .height(4.dp)
                .background(Color(0xFF2C2E33), RoundedCornerShape(2.dp))
                .align(Alignment.CenterHorizontally)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Title and Subtitle
        Text(
            text = if (isArabic) "بماذا تريد البدء؟" else "How would you like to start?",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.ExtraBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("post_choice_title")
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = if (isArabic) "اختر الطريقة المناسبة لك" else "Choose the method that works best for you",
            color = BrandTextMuted,
            fontSize = 13.sp,
            fontWeight = FontWeight.Normal,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth().testTag("post_choice_subtitle")
        )

        Spacer(modifier = Modifier.height(28.dp))

        // Option 1 Card: Create Store
        val isFirstSelected = selectedOption == 1
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF17191E)) // Dark card background #17191E
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (isFirstSelected) Color(0xFF1DB954) else Color.Transparent // 1.5dp green border on hover/selection
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    selectedOption = 1
                    onNavigateToCreateStore()
                }
                .padding(16.dp)
                .testTag("post_choice_store_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x1A1DB954), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Storefront,
                            contentDescription = null,
                            tint = Color(0xFF1DB954),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Text(
                                text = if (isArabic) "أنشئ متجرك الخاص" else "Create Your Own Store",
                                color = Color.White,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold
                            )
                            // "Recommended" Badge (green pill)
                            Box(
                                modifier = Modifier
                                    .background(Color(0x1F1DB954), RoundedCornerShape(12.dp))
                                    .border(BorderStroke(1.dp, Color(0xFF1DB954)), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = if (isArabic) "موصى به" else "Recommended",
                                    color = Color(0xFF1DB954),
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) "صورة + غلاف + اسم تجاري + نشر منتجات" else "Logo + cover + brand name + sell products",
                            color = BrandTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isFirstSelected) Color(0xFF1DB954) else BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Option 2 Card: Direct Ad
        val isSecondSelected = selectedOption == 2
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF17191E)) // Dark card background #17191E
                .border(
                    BorderStroke(
                        width = 1.5.dp,
                        color = if (isSecondSelected) Color(0xFF1DB954) else Color.Transparent // 1.5dp green border on hover/selection
                    ),
                    shape = RoundedCornerShape(16.dp)
                )
                .clickable {
                    selectedOption = 2
                    onNavigateToPostDirectAd()
                }
                .padding(16.dp)
                .testTag("post_choice_direct_ad_card")
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    // Icon Box
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .background(Color(0x1A2196F3), RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Campaign,
                            contentDescription = null,
                            tint = Color(0xFF2196F3),
                            modifier = Modifier.size(24.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = if (isArabic) "نشر إعلان مباشر" else "Post Direct Ad",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isArabic) "انشر منتجك أو خدمتك بدون إنشاء متجر" else "Publish your product or service without creating a store",
                            color = BrandTextMuted,
                            fontSize = 11.sp,
                            lineHeight = 15.sp
                        )
                    }
                }

                Icon(
                    imageVector = if (isArabic) Icons.AutoMirrored.Filled.ArrowBack else Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (isSecondSelected) Color(0xFF1DB954) else BrandTextMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
    }
}
