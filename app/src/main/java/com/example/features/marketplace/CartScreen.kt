package com.example.features.marketplace

import androidx.compose.animation.*
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
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.ui.theme.*

@Stable
data class CartItem(
    val id: String,
    val name: String,
    val price: Double,
    val originalPrice: Double?,
    val image: String,
    val size: String?,
    val storeName: String,
    val keyStockLimit: Int = 10
)

sealed class PromoState {
    object None : PromoState()
    data class Applied(val code: String, val discountPercent: Double, val discountAmount: Double) : PromoState()
    data class Invalid(val message: String) : PromoState()
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    onNavigateBack: () -> Unit,
    onCheckoutSuccess: () -> Unit
) {
    // Dynamic global cart items state synchronized across the marketplace application
    val initialCartItems = SharedCartState.cartItems
    val itemQuantities = SharedCartState.itemQuantities

    var promoCodeInput by remember { mutableStateOf("") }
    var promoState by remember { mutableStateOf<PromoState>(PromoState.None) }
    var isCheckoutDialogOpen by remember { mutableStateOf(false) }

    // Computations
    val subtotal = initialCartItems.sumOf { item ->
        val qty = itemQuantities[item.id] ?: 1
        item.price * qty
    }

    // Dynamic shipping rules: grouped by seller counts. Let's make it $5 per unique store, showing real multi-vendor breakdown
    val uniqueStoresCount = initialCartItems.distinctBy { it.storeName }.size
    val rawDeliveryFee = if (initialCartItems.isEmpty()) 0.0 else uniqueStoresCount * 4.99
    
    // Scarcity or discount free shipping logic: Free delivery on order > $350
    val isDeliveryFree = subtotal > 350.0
    val deliveryFee = if (isDeliveryFree) 0.0 else rawDeliveryFee

    val promoDiscountAmount = when (val state = promoState) {
        is PromoState.Applied -> subtotal * (state.discountPercent / 100.0)
        else -> 0.0
    }

    val finalTotal = (subtotal + deliveryFee - promoDiscountAmount).coerceAtLeast(0.0)

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(Color.White)
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandBackground)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Navigate Back",
                        tint = BrandTextPrimary
                    )
                }

                Text(
                    text = "SHOPPING CART",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    letterSpacing = 1.5.sp
                )

                // High visual indicator count
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(BrandPrimary.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    val itemsQuantityTotal = initialCartItems.sumOf { itemQuantities[it.id] ?: 1 }
                    Text(
                        text = "$itemsQuantityTotal ITEMS",
                        color = BrandPrimary,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        },
        bottomBar = {
            // Highly optimized sticky checkout action bar with checkout summary
            if (initialCartItems.isNotEmpty()) {
                Surface(
                    color = Color.White,
                    tonalElevation = 8.dp,
                    shadowElevation = 16.dp,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .navigationBarsPadding()
                            .padding(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Quick price calculation
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "TOTAL PAYABLE (VAT INCL.)",
                                    fontSize = 10.sp,
                                    color = BrandTextMuted,
                                    fontWeight = FontWeight.ExtraBold,
                                    letterSpacing = 0.5.sp
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Text(
                                        text = "$${String.format("%.2f", finalTotal)}",
                                        fontSize = 24.sp,
                                        color = BrandTextPrimary,
                                        fontWeight = FontWeight.ExtraBold
                                    )
                                    if (promoState is PromoState.Applied) {
                                        Box(
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(4.dp))
                                                .background(Color(0xFFE8F5E9))
                                                .padding(horizontal = 6.dp, vertical = 2.dp)
                                        ) {
                                            Text(
                                                text = "SAVING EXTRA",
                                                color = BrandPrimary,
                                                fontSize = 9.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                }
                            }

                            // Minimal trust lock element
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Lock,
                                    contentDescription = null,
                                    tint = BrandPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                                Text(
                                    text = "SECURE ESCROW",
                                    fontSize = 10.sp,
                                    color = BrandPrimary,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }

                        // CTA Button
                        Button(
                            onClick = { isCheckoutDialogOpen = true },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = BrandPrimary,
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Icon(
                                    imageVector = Icons.Default.ShoppingCart,
                                    contentDescription = null,
                                    modifier = Modifier.size(20.dp)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "PROCEED TO CHECKOUT",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    letterSpacing = 0.5.sp
                                )
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->

        if (initialCartItems.isEmpty()) {
            // Elegant Empty state UX to reduce client bounced screens
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "🛒",
                        fontSize = 62.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your cart is feeling light!",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Explore the feed to find tailored items and exclusive discounts.",
                        fontSize = 13.sp,
                        color = BrandTextMuted,
                        textAlign = TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onNavigateBack,
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "START SHOPPING",
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = Color.White
                        )
                    }
                }
            }
        } else {
            // Group lists vertically
            val itemsByStore = initialCartItems.groupBy { it.storeName }

            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues),
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(24.dp)
            ) {
                // Multi-Vendor Group items
                itemsByStore.forEach { (storeName, storeItems) ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(containerColor = Color.White),
                            shape = RoundedCornerShape(20.dp),
                            border = BorderStroke(1.dp, BrandSoftGray),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp)
                            ) {
                                // Group Store Header
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 12.dp)
                                ) {
                                    Text(
                                        text = storeName,
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = BrandTextPrimary
                                    )
                                    Box(
                                        modifier = Modifier
                                            .clip(CircleShape)
                                            .background(BrandPrimary)
                                            .size(5.dp)
                                    )
                                    Text(
                                        text = "Verified Vendor",
                                        fontSize = 11.sp,
                                        color = BrandPrimary,
                                        fontWeight = FontWeight.Medium
                                    )
                                }

                                HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)

                                // Store specific items listing
                                storeItems.forEach { item ->
                                    val currentQty = itemQuantities[item.id] ?: 1

                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(vertical = 12.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        // Product display image
                                        AsyncImage(
                                            model = item.image,
                                            contentDescription = item.name,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier
                                                .size(80.dp)
                                                .clip(RoundedCornerShape(12.dp))
                                                .border(1.dp, BrandSoftGray, RoundedCornerShape(12.dp))
                                        )

                                        Spacer(modifier = Modifier.width(16.dp))

                                        // Vertical product attributes
                                        Column(
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Text(
                                                text = item.name,
                                                fontSize = 13.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = BrandTextPrimary,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis
                                            )

                                            if (item.size != null) {
                                                Spacer(modifier = Modifier.height(4.dp))
                                                Text(
                                                    text = item.size,
                                                    fontSize = 11.sp,
                                                    color = BrandTextMuted
                                                )
                                            }

                                            Spacer(modifier = Modifier.height(8.dp))

                                            // Sub-price row
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                                            ) {
                                                Text(
                                                    text = "$${String.format("%.2f", item.price)}",
                                                    fontSize = 14.sp,
                                                    fontWeight = FontWeight.ExtraBold,
                                                    color = BrandTextPrimary
                                                )

                                                if (item.originalPrice != null) {
                                                    Text(
                                                        text = "$${String.format("%.2f", item.originalPrice)}",
                                                        fontSize = 11.sp,
                                                        color = BrandTextMuted,
                                                        textDecoration = TextDecoration.LineThrough
                                                    )
                                                }
                                            }

                                            Spacer(modifier = Modifier.height(10.dp))

                                            // Interactive quantity controllers + deletion button combined into single accessible element
                                            Row(
                                                modifier = Modifier.fillMaxWidth(),
                                                horizontalArrangement = Arrangement.SpaceBetween,
                                                verticalAlignment = Alignment.CenterVertically
                                            ) {
                                                Row(
                                                    modifier = Modifier
                                                        .background(BrandBackground, RoundedCornerShape(10.dp))
                                                        .border(1.dp, BrandSoftGray, RoundedCornerShape(10.dp))
                                                        .padding(2.dp),
                                                    verticalAlignment = Alignment.CenterVertically,
                                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.White)
                                                            .clickable {
                                                                if (currentQty > 1) {
                                                                    itemQuantities[item.id] = currentQty - 1
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Text(
                                                            text = "—",
                                                            fontSize = 11.sp,
                                                            fontWeight = FontWeight.Bold,
                                                            color = BrandTextPrimary
                                                        )
                                                    }

                                                    Text(
                                                        text = "$currentQty",
                                                        fontSize = 13.sp,
                                                        fontWeight = FontWeight.Bold,
                                                        color = BrandTextPrimary
                                                    )

                                                    Box(
                                                        modifier = Modifier
                                                            .size(28.dp)
                                                            .clip(RoundedCornerShape(6.dp))
                                                            .background(Color.White)
                                                            .clickable {
                                                                if (currentQty < item.keyStockLimit) {
                                                                    itemQuantities[item.id] = currentQty + 1
                                                                }
                                                            },
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            imageVector = Icons.Default.Add,
                                                            contentDescription = "Increase",
                                                            tint = BrandTextPrimary,
                                                            modifier = Modifier.size(14.dp)
                                                        )
                                                    }
                                                }

                                                // Clean Remove from Cart action button
                                                IconButton(
                                                    onClick = {
                                                        initialCartItems.remove(item)
                                                        itemQuantities.remove(item.id)
                                                    },
                                                    modifier = Modifier
                                                        .size(36.dp)
                                                        .clip(CircleShape)
                                                        .background(BrandBackground)
                                                ) {
                                                    Icon(
                                                        imageVector = Icons.Default.Delete,
                                                        contentDescription = "Remove Item",
                                                        tint = BrandError,
                                                        modifier = Modifier.size(16.dp)
                                                    )
                                                }
                                            }
                                        }
                                    }

                                    if (item != storeItems.last()) {
                                        HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)
                                    }
                                }
                            }
                        }
                    }
                }

                // Interactive Promo / Discount input section
                item {
                    val isCouponActive = promoState is PromoState.Applied

                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BrandSoftGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "PROMOTION CODE",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextMuted,
                                letterSpacing = 1.sp
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                OutlinedTextField(
                                    value = promoCodeInput,
                                    onValueChange = {
                                        promoCodeInput = it
                                        if (promoState is PromoState.Invalid) {
                                            promoState = PromoState.None
                                        }
                                    },
                                    placeholder = { Text("e.g. SAVE20", fontSize = 13.sp) },
                                    singleLine = true,
                                    colors = OutlinedTextFieldDefaults.colors(
                                        focusedBorderColor = BrandPrimary,
                                        unfocusedBorderColor = BrandSoftGray
                                    ),
                                    modifier = Modifier.weight(1f),
                                    shape = RoundedCornerShape(12.dp)
                                )

                                Button(
                                    onClick = {
                                        val cleanCode = promoCodeInput.trim().uppercase()
                                        if (cleanCode == "SAVE20") {
                                            promoState = PromoState.Applied("SAVE20", 20.0, subtotal * 0.2)
                                        } else if (cleanCode == "WELCOME") {
                                            promoState = PromoState.Applied("WELCOME", 10.0, subtotal * 0.1)
                                        } else if (cleanCode.isEmpty()) {
                                            promoState = PromoState.None
                                        } else {
                                            promoState = PromoState.Invalid("Invalid code. Try 'SAVE20' for 20% off!")
                                        }
                                    },
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = if (isCouponActive) BrandSoftGray else BrandPrimary,
                                        contentColor = if (isCouponActive) BrandTextPrimary else Color.White
                                    ),
                                    shape = RoundedCornerShape(12.dp),
                                    modifier = Modifier.height(50.dp)
                                ) {
                                    Text(
                                        text = if (isCouponActive) "APPLIED" else "APPLY",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }

                            // Dynamic state prompts
                            AnimatedVisibility(
                                visible = promoState != PromoState.None,
                                enter = fadeIn() + expandVertically(),
                                exit = fadeOut() + shrinkVertically()
                            ) {
                                when (val state = promoState) {
                                    is PromoState.Applied -> {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .background(BrandPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                                .padding(10.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.CheckCircle,
                                                contentDescription = null,
                                                tint = BrandPrimary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                            Text(
                                                text = "Promo '${state.code}' applied! Saved ${state.discountPercent.toInt()}% (-$${String.format("%.2f", state.discountAmount)})",
                                                color = BrandSecondary,
                                                fontSize = 12.sp,
                                                fontWeight = FontWeight.Bold
                                            )
                                        }
                                    }
                                    is PromoState.Invalid -> {
                                        Text(
                                            text = state.message,
                                            color = BrandError,
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.Bold,
                                            modifier = Modifier.padding(start = 4.dp)
                                        )
                                    }
                                    else -> {}
                                }
                            }
                        }
                    }
                }

                // Summary calculations card
                item {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color.White),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, BrandSoftGray),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Text(
                                text = "CHECKOUT BREAKDOWN",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = BrandTextMuted,
                                letterSpacing = 1.sp
                            )

                            // Item Subtotal
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Store Items Subtotal",
                                    fontSize = 13.sp,
                                    color = BrandTextMuted
                                )
                                Text(
                                    text = "$${String.format("%.2f", subtotal)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextPrimary
                                )
                            }

                            // Multi-Vendor Delivery Fee
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Multi-Vendor Shipping",
                                        fontSize = 13.sp,
                                        color = BrandTextMuted
                                    )
                                    Text(
                                        text = "Grouped from $uniqueStoresCount vendors",
                                        fontSize = 10.sp,
                                        color = BrandTextMuted
                                    )
                                }
                                Text(
                                    text = if (isDeliveryFree) "FREE" else "$${String.format("%.2f", deliveryFee)}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDeliveryFree) BrandPrimary else BrandTextPrimary
                                )
                            }

                            // Bonus promo code row
                            if (promoState is PromoState.Applied) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Discount Applied",
                                        fontSize = 13.sp,
                                        color = BrandSecondary,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Text(
                                        text = "-$${String.format("%.2f", promoDiscountAmount)}",
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = BrandSecondary
                                    )
                                }
                            }

                            HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)

                            // Total calculation
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "Estimated Order Total",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = BrandTextPrimary
                                )
                                Text(
                                    text = "$${String.format("%.2f", finalTotal)}",
                                    fontSize = 18.sp,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = BrandPrimary
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // High Conversion Simulated Secure Checkout Modal Dialog
    if (isCheckoutDialogOpen) {
        AlertDialog(
            onDismissRequest = { isCheckoutDialogOpen = false },
            title = {
                Text(
                    text = "🔒 Secure Escrow Checkout",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary
                )
            },
            text = {
                Column(
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Text(
                        text = "You are checking out via WasetPlus Secure Escrow protection holding fee until delivery confirmation.",
                        fontSize = 13.sp,
                        color = BrandTextPrimary,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(4.dp))

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = "Escrow protection activated",
                            color = BrandSecondary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        isCheckoutDialogOpen = false
                        onCheckoutSuccess()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary)
                ) {
                    Text("CONFIRM DEPOSIT", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(
                    onClick = { isCheckoutDialogOpen = false },
                    colors = ButtonDefaults.textButtonColors(contentColor = BrandTextMuted)
                ) {
                    Text("CANCEL", fontWeight = FontWeight.Bold)
                }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }
}
