package com.example.features.marketplace

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.core.di.ServiceLocator
import com.example.core.utils.CurrencyManager
import com.example.core.utils.LanguageManager
import com.example.core.utils.NotificationManager
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.domain.usecase.GetOrdersForStoreUseCase
import com.example.domain.usecase.UpdateOrderStatusUseCase
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import java.util.*

private val DarkBg: Color get() = BrandBackground
private val DarkCard: Color get() = BrandSurface
private val PrimaryGreen: Color get() = BrandPrimary
private val TextWhite: Color get() = BrandTextPrimary
private val TextGray: Color get() = BrandTextMuted
private val BorderColor: Color get() = BrandSoftGray

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrdersSection(
    isArabic: Boolean,
    storeId: String,
    viewModel: SellerOrdersViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return SellerOrdersViewModel(
                authRepo = ServiceLocator.authRepository,
                storeRepo = ServiceLocator.storeRepository,
                getOrdersForStoreUseCase = GetOrdersForStoreUseCase(ServiceLocator.orderRepository),
                updateOrderStatusUseCase = UpdateOrderStatusUseCase(ServiceLocator.orderRepository)
            ) as T
        }
    })
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    val state by viewModel.state.collectAsStateWithLifecycle()

    var activeTabFilter by remember { mutableStateOf("All") }
    var selectedOrderForDetail by remember { mutableStateOf<Order?>(null) }
    
    // Status update transaction trackers
    var orderToUpdateStatus by remember { mutableStateOf<Pair<Order, String>?>(null) } // Order to (nextStatus)
    var showConfirmationDialog by remember { mutableStateOf(false) }

    // Filtered orders list mapping
    val filteredOrders = remember(state.orders, activeTabFilter) {
        if (activeTabFilter == "All") {
            state.orders
        } else {
            state.orders.filter { it.status.equals(activeTabFilter, ignoreCase = true) }
        }
    }

    // Analytics calculations derived purely from live state
    val pendingCount = remember(state.orders) { state.orders.count { it.status.equals("Pending", ignoreCase = true) } }
    val processingCount = remember(state.orders) { state.orders.count { it.status.equals("Processing", ignoreCase = true) } }
    val shippedCount = remember(state.orders) { state.orders.count { it.status.equals("Shipped", ignoreCase = true) } }
    val deliveredCount = remember(state.orders) { state.orders.count { it.status.equals("Delivered", ignoreCase = true) } }
    
    // Total Revenue of Delivered (completed) orders converted based on store rate
    val totalRevenue = remember(state.orders, state.store) {
        val usdTotal = state.orders.filter { it.status.equals("Delivered", ignoreCase = true) }.sumOf { it.totalAmount }
        usdTotal
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(DarkBg)
    ) {
        // 1. STATS METRIC GRID PANEL AT TOP
        SellerAnalyticsSummaryGrid(
            isArabic = isArabic,
            pending = pendingCount,
            processing = processingCount,
            shipped = shippedCount,
            delivered = deliveredCount,
            revenue = totalRevenue,
            storeRate = state.store?.usdExchangeRate ?: 13500.0,
            storeCurrency = state.store?.defaultCurrency ?: "USD"
        )

        Spacer(modifier = Modifier.height(12.dp))

        // 2. HORIZONTAL FILTER CHIPS TAB BAR
        SellerFilterTabsRow(
            isArabic = isArabic,
            selectedTab = activeTabFilter,
            orders = state.orders,
            onTabSelected = { activeTabFilter = it }
        )

        // Loading Indicators
        if (state.isLoading && state.orders.isEmpty()) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = PrimaryGreen)
            }
        } else if (filteredOrders.isEmpty()) {
            // 3. CLEAN HIGH-FI EMPTY STATE
            SellerEmptyOrdersState(isArabic = isArabic, filter = activeTabFilter)
        } else {
            // 4. LIVE FIRESTORE SCROLLABLE ORDERS list
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .testTag("seller_orders_list"),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(filteredOrders, key = { it.orderId }) { order ->
                    SellerOrderCard(
                        order = order,
                        isArabic = isArabic,
                        storeRate = state.store?.usdExchangeRate ?: 13500.0,
                        onUpdateStatus = { nextStatus ->
                            orderToUpdateStatus = Pair(order, nextStatus)
                            showConfirmationDialog = true
                        },
                        onViewDetails = {
                            selectedOrderForDetail = order
                        }
                    )
                }
            }
        }
    }

    // A. EXQUISITE INVOICE DETAIL SHEET / DIALOG MODAL
    if (selectedOrderForDetail != null) {
        SellerOrderDetailDialog(
            order = selectedOrderForDetail!!,
            isArabic = isArabic,
            storeRate = state.store?.usdExchangeRate ?: 13500.0,
            onDismiss = { selectedOrderForDetail = null },
            onUpdateStatus = { nextStatus ->
                orderToUpdateStatus = Pair(selectedOrderForDetail!!, nextStatus)
                showConfirmationDialog = true
            },
            onCopyDetails = { text ->
                clipboardManager.setText(AnnotatedString(text))
                Toast.makeText(
                    context,
                    if (isArabic) "تم نسخ المعلومات إلى الحافظة" else "Copied information to clipboard",
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    // B. TRANSACTION STATE STAGE TRANSITION CONFIRMATION DIALOG MODAL
    if (showConfirmationDialog && orderToUpdateStatus != null) {
        val (order, nextStatus) = orderToUpdateStatus!!
        AlertDialog(
            onDismissRequest = {
                showConfirmationDialog = false
                orderToUpdateStatus = null
            },
            containerColor = DarkCard,
            title = {
                Text(
                    text = if (isArabic) "تأكيد تغيير حالة الطلب" else "Confirm Status Transition",
                    color = TextWhite,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                val statusLabel = getStatusLabel(nextStatus, isArabic)
                Text(
                    text = if (isArabic) {
                        "هل أنت متأكد من تغيير حالة الطلب رقم ${order.orderId} إلى [ $statusLabel ]؟"
                    } else {
                        "Are you sure you want to shift order status of ${order.orderId} to [ $statusLabel ]?"
                    },
                    color = TextGray,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    colors = ButtonDefaults.buttonColors(containerColor = if (nextStatus == "Cancelled") Color.Red else PrimaryGreen),
                    shape = RoundedCornerShape(8.dp),
                    onClick = {
                        viewModel.updateOrderStatus(order.orderId, nextStatus) {
                            // On Success write, send a push notification simulation details
                            val firstItemName = order.items.firstOrNull()?.productName ?: "Product"
                            NotificationManager.dispatchOrderStatusNotification(
                                context = context,
                                orderId = order.orderId,
                                status = nextStatus,
                                storeName = order.storeName,
                                customerName = order.customerName.ifBlank { "WasetPlus Customer" },
                                itemName = firstItemName
                            )
                        }
                        showConfirmationDialog = false
                        orderToUpdateStatus = null
                        // Refresh details view if matching updated document
                        if (selectedOrderForDetail?.orderId == order.orderId) {
                            selectedOrderForDetail = order.copy(status = nextStatus)
                        }
                    }
                ) {
                    Text(
                        text = if (isArabic) "نعم، تأكيد" else "Confirm",
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showConfirmationDialog = false
                        orderToUpdateStatus = null
                    }
                ) {
                    Text(
                        text = if (isArabic) "إلغاء السحب" else "Cancel",
                        color = PrimaryGreen
                    )
                }
            }
        )
    }
}

@Composable
fun SellerAnalyticsSummaryGrid(
    isArabic: Boolean,
    pending: Int,
    processing: Int,
    shipped: Int,
    delivered: Int,
    revenue: Double,
    storeRate: Double,
    storeCurrency: String
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        // Revenue display header (Shopify / Noon style large metrics card)
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) "إجمالي الإيرادات المحققة" else "Accrued Total Revenue",
                        color = TextGray,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = CurrencyManager.formatPrice(revenue, storeRate, isArabic),
                        color = PrimaryGreen,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                    Text(
                        text = if (isArabic) "من المبيعات والطلبات المستلمة والمسلمة بنجاح" else "Based on successfully delivered orders",
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(PrimaryGreen.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.MonetizationOn,
                        contentDescription = null,
                        tint = PrimaryGreen,
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
        }

        // Small 4 metrics widgets grid
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            MetricBadgeMiniCard(
                title = if (isArabic) "معلق" else "Pending",
                count = pending,
                icon = Icons.Default.PendingActions,
                iconColor = Color(0xFFFFB300),
                modifier = Modifier.weight(1f)
            )
            MetricBadgeMiniCard(
                title = if (isArabic) "قيد التجهيز" else "Processing",
                count = processing,
                icon = Icons.Default.Cached,
                iconColor = Color(0xFF29B6F6),
                modifier = Modifier.weight(1f)
            )
            MetricBadgeMiniCard(
                title = if (isArabic) "تم الشحن" else "Shipped",
                count = shipped,
                icon = Icons.Default.LocalShipping,
                iconColor = Color(0xFF26A69A),
                modifier = Modifier.weight(1f)
            )
            MetricBadgeMiniCard(
                title = if (isArabic) "مكتمل" else "Delivered",
                count = delivered,
                icon = Icons.Default.TaskAlt,
                iconColor = PrimaryGreen,
                modifier = Modifier.weight(1f)
            )
        }
    }
}

@Composable
fun MetricBadgeMiniCard(
    title: String,
    count: Int,
    icon: ImageVector,
    iconColor: Color,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconColor,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = count.toString(),
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = TextWhite
            )
            Text(
                text = title,
                fontSize = 10.sp,
                color = TextGray,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun SellerFilterTabsRow(
    isArabic: Boolean,
    selectedTab: String,
    orders: List<Order>,
    onTabSelected: (String) -> Unit
) {
    val filters = remember(orders) {
        listOf(
            "All" to orders.size,
            "Pending" to orders.count { it.status.equals("Pending", ignoreCase = true) },
            "Processing" to orders.count { it.status.equals("Processing", ignoreCase = true) },
            "Shipped" to orders.count { it.status.equals("Shipped", ignoreCase = true) },
            "Delivered" to orders.count { it.status.equals("Delivered", ignoreCase = true) },
            "Cancelled" to orders.count { it.status.equals("Cancelled", ignoreCase = true) }
        )
    }

    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 2.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        contentPadding = PaddingValues(end = 16.dp)
    ) {
        items(filters) { item ->
            val status = item.first
            val count = item.second
            val isSelected = selectedTab == status
            val label = getStatusLabel(status, isArabic)
            
            FilterChip(
                selected = isSelected,
                onClick = { onTabSelected(status) },
                label = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Text(text = label, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (isSelected) Color.White.copy(alpha = 0.25f) else BorderColor)
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = count.toString(),
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) TextWhite else PrimaryGreen
                            )
                        }
                    }
                },
                colors = FilterChipDefaults.filterChipColors(
                    containerColor = DarkCard,
                    labelColor = TextGray,
                    selectedContainerColor = PrimaryGreen,
                    selectedLabelColor = Color.White
                ),
                border = FilterChipDefaults.filterChipBorder(
                    borderColor = BorderColor,
                    selectedBorderColor = PrimaryGreen,
                    enabled = true,
                    selected = isSelected
                ),
                shape = RoundedCornerShape(20.dp)
            )
        }
    }
}

@Composable
fun SellerOrderCard(
    order: Order,
    isArabic: Boolean,
    storeRate: Double,
    onUpdateStatus: (String) -> Unit,
    onViewDetails: () -> Unit
) {
    val context = LocalContext.current
    val formattedDate = remember(order.createdAt) {
        try {
            val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", if (isArabic) Locale("ar") else Locale.US)
            sdf.format(Date(order.createdAt))
        } catch (e: Exception) {
            "Just now"
        }
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onViewDetails)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Header Row: ID + Status label indicator
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Receipt,
                            contentDescription = null,
                            tint = PrimaryGreen,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = order.orderId,
                            color = TextWhite,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp
                        )
                    }
                    Text(
                        text = formattedDate,
                        color = TextGray,
                        fontSize = 11.sp
                    )
                }

                StatusBadgeTag(status = order.status, isArabic = isArabic)
            }

            Spacer(modifier = Modifier.height(10.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Body info: Customer & Product Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Product preview (first item thumbnail)
                val firstItem = order.items.firstOrNull()
                if (firstItem != null && firstItem.productImage.isNotEmpty()) {
                    AsyncImage(
                        model = firstItem.productImage,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .border(1.dp, BorderColor, RoundedCornerShape(8.dp))
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .size(54.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(BorderColor),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Default.ShoppingBag, null, tint = TextGray)
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (isArabic) {
                            "المشتري: ${order.customerName.ifBlank { "مستخدم المنصة" }}"
                        } else {
                            "Customer: ${order.customerName.ifBlank { "WasetPlatform User" }}"
                        },
                        color = TextWhite,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = firstItem?.productName ?: "",
                        color = TextGray,
                        fontSize = 12.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = if (isArabic) {
                            "الكمية: ${firstItem?.quantity} حزمة"
                        } else {
                            "Quantity: ${firstItem?.quantity} units"
                        },
                        fontSize = 11.sp,
                        color = TextGray
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = if (isArabic) "الإجمالي" else "Total",
                        color = TextGray,
                        fontSize = 10.sp
                    )
                    Text(
                        text = CurrencyManager.formatPrice(order.totalAmount, storeRate, isArabic),
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryGreen,
                        fontSize = 15.sp
                    )
                }
            }

            // Expanded count if multiple items
            if (order.items.size > 1) {
                Spacer(modifier = Modifier.height(6.dp))
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(BorderColor.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = if (isArabic) {
                            "+ ${order.items.size - 1} سلع أخرى داخل سلة الطرد"
                        } else {
                            "+ ${order.items.size - 1} other items in parcel pack"
                        },
                        fontSize = 10.sp,
                        color = PrimaryGreen,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(10.dp))

            // Action Workflows (Fulfillment lifecycle manager)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Cancel Action Button
                if (order.status.equals("Pending", ignoreCase = true) || order.status.equals("Processing", ignoreCase = true)) {
                    TextButton(
                        onClick = { onUpdateStatus("Cancelled") },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color.Red.copy(alpha = 0.8f))
                    ) {
                        Icon(Icons.Default.Cancel, null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isArabic) "إلغاء الطلب" else "Cancel Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                // Workflow forward transitioning controls
                when (order.status) {
                    "Pending" -> {
                        Button(
                            shape = RoundedCornerShape(8.dp),
                            onClick = { onUpdateStatus("Processing") },
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen)
                        ) {
                            Icon(Icons.Default.Check, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "قبول وتحضير" else "Accept & Prepare",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    "Processing" -> {
                        Button(
                            shape = RoundedCornerShape(8.dp),
                            onClick = { onUpdateStatus("Shipped") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6))
                        ) {
                            Icon(Icons.Default.LocalShipping, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تأكيد الشحن والارسال" else "Confirm Dispatch",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                    "Shipped" -> {
                        Button(
                            shape = RoundedCornerShape(8.dp),
                            onClick = { onUpdateStatus("Delivered") },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A))
                        ) {
                            Icon(Icons.Default.CheckCircle, null, modifier = Modifier.size(16.dp), tint = Color.White)
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isArabic) "تسليم للمستودع/العميل" else "Mark Delivered",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun StatusBadgeTag(status: String, isArabic: Boolean) {
    val (bgColor, textColor) = remember(status) {
        when (status) {
            "Pending" -> Color(0xFFFFB300).copy(alpha = 0.15f) to Color(0xFFFFB300)
            "Processing" -> Color(0xFF29B6F6).copy(alpha = 0.15f) to Color(0xFF29B6F6)
            "Shipped" -> Color(0xFF26A69A).copy(alpha = 0.15f) to Color(0xFF26A69A)
            "Delivered" -> PrimaryGreen.copy(alpha = 0.15f) to PrimaryGreen
            "Cancelled" -> Color.Red.copy(alpha = 0.1f) to Color.Red
            else -> BorderColor to TextGray
        }
    }
    val label = getStatusLabel(status, isArabic)

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bgColor)
            .padding(horizontal = 8.dp, vertical = 4.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 11.sp
        )
    }
}

@Composable
fun SellerEmptyOrdersState(isArabic: Boolean, filter: String) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(80.dp)
                    .clip(CircleShape)
                    .background(BorderColor.copy(alpha = 0.4f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = TextGray,
                    modifier = Modifier.size(40.dp)
                )
            }

            Text(
                text = if (isArabic) "لا يوجد طلبات حالياً" else "No orders registered",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 18.sp
            )

            Text(
                text = if (isArabic) {
                    "لا يتوفر أي معاملات تحت تصنيف [ ${getStatusLabel(filter, true)} ] متصلة بمتجرك الآن."
                } else {
                    "There are no active transactions under [ ${getStatusLabel(filter, false)} ] status for your store."
                },
                color = TextGray,
                fontSize = 13.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SellerOrderDetailDialog(
    order: Order,
    isArabic: Boolean,
    storeRate: Double,
    onDismiss: () -> Unit,
    onUpdateStatus: (String) -> Unit,
    onCopyDetails: (String) -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = DarkCard),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BorderColor),
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(vertical = 12.dp)
        ) {
            Column(modifier = Modifier.fillMaxSize()) {
                // Header Title
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(BorderColor.copy(alpha = 0.3f))
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Icon(Icons.Default.ReceiptLong, null, tint = PrimaryGreen)
                        Text(
                            text = if (isArabic) "تفاصيل الفاتورة والطلب" else "Invoice & Order Receipt",
                            fontWeight = FontWeight.ExtraBold,
                            color = TextWhite,
                            fontSize = 16.sp
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, null, tint = TextWhite)
                    }
                }

                // Scrollable details content container
                Box(modifier = Modifier.weight(1f)) {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // 1. ORDER SUMMARY CARD
                        item {
                            OrderStatusOverviewCard(order = order, isArabic = isArabic)
                        }

                        // 2. TIMELINE Fulfillment Progress Tracker
                        item {
                            FulfillmentTimelineProgressTracker(order = order, isArabic = isArabic)
                        }

                        // 3. PRODUCTS LIST DETAIL
                        item {
                            Text(
                                text = if (isArabic) "المنتجات المطلوبة" else "Ordered Merchandises",
                                fontWeight = FontWeight.Bold,
                                color = TextWhite,
                                fontSize = 14.sp
                            )
                        }

                        items(order.items) { item ->
                            InvoiceLineItemRow(item = item, isArabic = isArabic, storeRate = storeRate)
                        }

                        // 4. FINANCIAL BREAKDOWN DETAILS
                        item {
                            FinancialpricingSummarySheet(order = order, isArabic = isArabic, storeRate = storeRate)
                        }

                        // 5. CUSTOMER DISPATCH INFORMATION
                        item {
                            CustomerDispatchSheet(order = order, isArabic = isArabic, onCopyClick = onCopyDetails)
                        }
                    }
                }

                // BOTTOM ACTION AREA
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (order.status.equals("Pending", ignoreCase = true) || order.status.equals("Processing", ignoreCase = true)) {
                        OutlinedButton(
                            onClick = { onUpdateStatus("Cancelled") },
                            shape = RoundedCornerShape(8.dp),
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.Red),
                            border = BorderStroke(1.dp, Color.Red),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(if (isArabic) "إلغاء تماماً" else "Cancel Order", fontWeight = FontWeight.Bold)
                        }
                    }

                    when (order.status) {
                        "Pending" -> {
                            Button(
                                onClick = { onUpdateStatus("Processing") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = PrimaryGreen),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "تأكيد وقبول" else "Accept Order", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        "Processing" -> {
                            Button(
                                onClick = { onUpdateStatus("Shipped") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF29B6F6)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "شحن الطرد" else "Dispatch Items", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        "Shipped" -> {
                            Button(
                                onClick = { onUpdateStatus("Delivered") },
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF26A69A)),
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(if (isArabic) "تسليم الطلب" else "Confirm Delivered", fontWeight = FontWeight.Bold, color = Color.White)
                            }
                        }
                        else -> {
                            Button(
                                onClick = onDismiss,
                                shape = RoundedCornerShape(8.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = BorderColor),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(if (isArabic) "إغلاق النافذة" else "Dismiss Receipt", fontWeight = FontWeight.Bold, color = TextWhite)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OrderStatusOverviewCard(order: Order, isArabic: Boolean) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BorderColor.copy(alpha = 0.2f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isArabic) "ملف الفاتورة ID:" else "Invoice ID:",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 12.sp
                )
                Text(
                    text = order.orderId,
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen,
                    fontSize = 12.sp
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                Text(
                    text = if (isArabic) "الحالة الحالية للشحن:" else "Fulfillment Status:",
                    color = TextGray,
                    fontSize = 12.sp
                )
                StatusBadgeTag(status = order.status, isArabic = isArabic)
            }
        }
    }
}

@Composable
fun FulfillmentTimelineProgressTracker(order: Order, isArabic: Boolean) {
    val stages = remember {
        listOf("Pending", "Processing", "Shipped", "Delivered")
    }
    val currentStageIndex = stages.indexOf(order.status)

    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = if (isArabic) "خارطة طريق التوصيل 🧭" else "Fulfillment Pipeline 🧭",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(14.dp))

            // Sequence row pipeline drawing
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                stages.forEachIndexed { index, stage ->
                    val isCompleted = index <= currentStageIndex && order.status != "Cancelled"
                    val isActive = index == currentStageIndex && order.status != "Cancelled"
                    val bubbleColor = if (isCompleted) PrimaryGreen else BorderColor
                    val textColor = if (index <= currentStageIndex) TextWhite else TextGray

                    Column(
                        modifier = Modifier.weight(1f),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(bubbleColor),
                            contentAlignment = Alignment.Center
                        ) {
                            if (isCompleted) {
                                Icon(Icons.Default.Check, null, tint = Color.White, modifier = Modifier.size(14.dp))
                            } else {
                                Text((index + 1).toString(), color = TextGray, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = getStatusLabel(stage, isArabic),
                            fontSize = 9.sp,
                            fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Medium,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (index < stages.size - 1) {
                        val lineColor = if (index < currentStageIndex && order.status != "Cancelled") PrimaryGreen else BorderColor
                        Box(
                            modifier = Modifier
                                .weight(0.5f)
                                .height(2.dp)
                                .background(lineColor)
                                .offset(y = (-10).dp)
                        )
                    }
                }
            }

            if (order.status == "Cancelled") {
                Spacer(modifier = Modifier.height(10.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.Red.copy(alpha = 0.1f))
                        .padding(8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Cancel, null, tint = Color.Red, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isArabic) "تم إلغاء شحن المعاملة تماماً" else "Transaction Shipped Package Cancelled",
                        fontWeight = FontWeight.Bold,
                        color = Color.Red,
                        fontSize = 11.sp
                    )
                }
            }
        }
    }
}

@Composable
fun InvoiceLineItemRow(item: OrderItem, isArabic: Boolean, storeRate: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(BorderColor.copy(alpha = 0.15f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (item.productImage.isNotEmpty()) {
            AsyncImage(
                model = item.productImage,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
            )
        } else {
            Box(
                Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(BorderColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.ShoppingBag, null, tint = TextGray)
            }
        }

        Spacer(modifier = Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = if (isArabic) {
                        "السعر: ${CurrencyManager.formatPrice(item.unitPrice, storeRate, true)}"
                    } else {
                        "Price: ${CurrencyManager.formatPrice(item.unitPrice, storeRate, false)}"
                    },
                    fontSize = 11.sp,
                    color = TextGray
                )
                Text(
                    text = "x ${item.quantity}",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryGreen
                )
            }
        }

        Text(
            text = CurrencyManager.formatPrice(item.unitPrice * item.quantity, storeRate, isArabic),
            color = TextWhite,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp
        )
    }
}

@Composable
fun FinancialpricingSummarySheet(order: Order, isArabic: Boolean, storeRate: Double) {
    Card(
        colors = CardDefaults.cardColors(containerColor = DarkCard),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(1.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = if (isArabic) "تفاصيل الدفعات والعمولات" else "Financial Statement",
                fontWeight = FontWeight.Bold,
                color = TextWhite,
                fontSize = 13.sp
            )
            Spacer(modifier = Modifier.height(4.dp))

            InvoiceSummaryLine(
                label = if (isArabic) "القيمة الصافية للمنتجات" else "Subtotal Net Sales",
                value = CurrencyManager.formatPrice(order.totalAmount, storeRate, isArabic)
            )

            InvoiceSummaryLine(
                label = if (isArabic) "ضريبة الخدمة المشتركة (0%)" else "WasetPlus Escrow fee (0%)",
                value = if (isArabic) "0 ل.س" else "FREE"
            )

            InvoiceSummaryLine(
                label = if (isArabic) "عمولة حجز الضمان والوساطة" else "Platform Protection Cover",
                value = if (isArabic) "مجاني" else "Covered"
            )

            Spacer(modifier = Modifier.height(4.dp))
            HorizontalDivider(color = BorderColor, thickness = 0.5.dp)
            Spacer(modifier = Modifier.height(4.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "الصافي المراد تحصيله:" else "Payout Net Balance:",
                    fontWeight = FontWeight.ExtraBold,
                    color = TextWhite,
                    fontSize = 13.sp
                )
                Text(
                    text = CurrencyManager.formatPrice(order.totalAmount, storeRate, isArabic),
                    fontWeight = FontWeight.ExtraBold,
                    color = PrimaryGreen,
                    fontSize = 16.sp
                )
            }
        }
    }
}

@Composable
fun InvoiceSummaryLine(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = TextGray, fontSize = 11.sp)
        Text(text = value, color = TextWhite, fontSize = 11.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
fun CustomerDispatchSheet(order: Order, isArabic: Boolean, onCopyClick: (String) -> Unit) {
    Card(
        colors = CardDefaults.cardColors(containerColor = BorderColor.copy(alpha = 0.1f)),
        shape = RoundedCornerShape(10.dp),
        border = BorderStroke(0.5.dp, BorderColor),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = if (isArabic) "معلومات مستلم الطرد 👤" else "Consignee Profile 👤",
                    fontWeight = FontWeight.Bold,
                    color = TextWhite,
                    fontSize = 13.sp
                )

                TextButton(
                    contentPadding = PaddingValues(0.dp),
                    modifier = Modifier.height(24.dp),
                    onClick = {
                        val dispatchNote = """
                            Order ID: ${order.orderId}
                            Customer: ${order.customerName}
                            Phone: ${order.customerPhone}
                            Shipping Address: ${order.shippingAddress}
                        """.trimIndent()
                        onCopyClick(dispatchNote)
                    }
                ) {
                    Icon(Icons.Default.ContentCopy, null, modifier = Modifier.size(12.dp), tint = PrimaryGreen)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "نسخ للترحيل" else "Copy Dispatch",
                        color = PrimaryGreen,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            CustomerFieldDetail(
                icon = Icons.Default.Person,
                label = if (isArabic) "اسم المستلم" else "Full Name",
                value = order.customerName.ifBlank { if (isArabic) "زبون المنصة" else "Guest Buyer" }
            )

            CustomerFieldDetail(
                icon = Icons.Default.Phone,
                label = if (isArabic) "رقم الاتصال المسجل" else "Contact Phone",
                value = order.customerPhone.ifBlank { "N/A" }
            )

            CustomerFieldDetail(
                icon = Icons.Default.Map,
                label = if (isArabic) "عنوان الشحن المعتمد" else "Authorized Address",
                value = order.shippingAddress.ifBlank { if (isArabic) "دمشق، سوريا" else "Damascus, Syria" }
            )
        }
    }
}

@Composable
fun CustomerFieldDetail(icon: ImageVector, label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = TextGray, modifier = Modifier.size(14.dp))
        Column {
            Text(text = label, color = TextGray, fontSize = 10.sp)
            Text(text = value, color = TextWhite, fontWeight = FontWeight.Bold, fontSize = 12.sp)
        }
    }
}

private fun getStatusLabel(status: String, isArabic: Boolean): String {
    return when (status) {
        "All" -> if (isArabic) "الكل" else "All"
        "Pending" -> if (isArabic) "معلق" else "Pending"
        "Processing" -> if (isArabic) "قيد التحضير" else "Processing"
        "Shipped" -> if (isArabic) "تم الشحن" else "Shipped"
        "Delivered" -> if (isArabic) "تم التوصيل" else "Delivered"
        "Cancelled" -> if (isArabic) "ملغي" else "Cancelled"
        else -> status
    }
}
