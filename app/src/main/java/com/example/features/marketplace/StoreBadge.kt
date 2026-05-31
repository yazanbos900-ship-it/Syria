package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.Store
import com.example.ui.theme.*

// Styling colors
val GoldPremium = Color(0xFFFFB300)
val ProPurple = Color(0xFF8A2BE2)
val GrowthTeal = Color(0xFF008080)
val VerifiedGreen = Color(0xFF1DB954)

@Composable
fun StoreSellerBadge(
    sellerBadge: String,
    modifier: Modifier = Modifier
) {
    if (sellerBadge == "None" || sellerBadge.isBlank()) return

    val (bgColor, textColor, icon, label) = when (sellerBadge) {
        "Pro Seller" -> Quadruple(
            Color(0xFFFFB300).copy(alpha = 0.15f),
            Color(0xFFFFB300),
            Icons.Default.Star,
            "Pro Seller"
        )
        "Verified Seller" -> Quadruple(
            Color(0xFF1DB954).copy(alpha = 0.15f),
            Color(0xFF1DB954),
            Icons.Default.CheckCircle,
            "Verified Seller"
        )
        else -> Quadruple(
            BrandSoftGray,
            BrandTextMuted,
            Icons.Default.CheckCircle,
            sellerBadge
        )
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = textColor,
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun StoreSubscriptionBadge(
    tier: String,
    modifier: Modifier = Modifier
) {
    val (bgColor, textColor, icon, label) = when (tier) {
        "Pro" -> Quadruple(
            Color(0xFF8A2BE2).copy(alpha = 0.15f),
            Color(0xFFD8BFD8),
            Icons.Default.Star,
            "Pro Tier"
        )
        "Growth" -> Quadruple(
            Color(0xFF008080).copy(alpha = 0.15f),
            Color(0xFF20B2AA),
            Icons.Default.Bolt,
            "Growth"
        )
        "Starter" -> Quadruple(
            Color(0xFF333333).copy(alpha = 0.2f),
            BrandTextMuted,
            null,
            "Starter"
        )
        else -> return
    }

    Row(
        modifier = modifier
            .background(bgColor, RoundedCornerShape(4.dp))
            .border(1.5.dp, textColor.copy(alpha = 0.3f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = textColor,
                modifier = Modifier.size(10.dp)
            )
        }
        Text(
            text = label,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = textColor
        )
    }
}

@Composable
fun StoreVerificationBadge(
    status: String,
    isVerifiedLegacy: Boolean,
    modifier: Modifier = Modifier
) {
    val isVerified = status == "Verified" || isVerifiedLegacy
    if (!isVerified) return

    Row(
        modifier = modifier
            .background(Color(0xFF1DB954).copy(alpha = 0.15f), RoundedCornerShape(4.dp))
            .border(1.dp, Color(0xFF1DB954).copy(alpha = 0.4f), RoundedCornerShape(4.dp))
            .padding(horizontal = 6.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Default.CheckCircle,
            contentDescription = "Verified Store",
            tint = Color(0xFF1DB954),
            modifier = Modifier.size(10.dp)
        )
        Text(
            text = "VERIFIED",
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            color = Color(0xFF1DB954)
        )
    }
}

@Composable
fun StoreBadgesContainer(
    store: Store,
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.spacedBy(6.dp)
) {
    Row(
        modifier = modifier,
        horizontalArrangement = horizontalArrangement,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // 1. Seller Badge (Verified Seller, Pro Seller)
        if (store.sellerBadge != "None" && store.sellerBadge.isNotBlank()) {
            StoreSellerBadge(sellerBadge = store.sellerBadge)
        }
        // 2. Subscription Tier (Starter, Growth, Pro)
        StoreSubscriptionBadge(tier = store.subscriptionTier)
        // 3. Verification Status / Legacy Verified
        if (store.verificationStatus == "Verified" || store.isVerified) {
            StoreVerificationBadge(status = store.verificationStatus, isVerifiedLegacy = store.isVerified)
        }
    }
}

// Simple Quadruple helper data package
private data class Quadruple<out A, out B, out C, out D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
