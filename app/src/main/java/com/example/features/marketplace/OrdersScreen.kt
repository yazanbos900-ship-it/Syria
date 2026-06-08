package com.example.features.marketplace

import androidx.compose.animation.*
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.components.BrandCard
import com.example.core.utils.CurrencyManager
import com.example.core.utils.LanguageManager
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.ui.theme.*
import java.text.SimpleDateFormat
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.core.di.ServiceLocator
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersScreen(
    onNavigateBack: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    viewModel: OrdersViewModel = viewModel(factory = object : androidx.lifecycle.ViewModelProvider.Factory {
        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return OrdersViewModel(
                getOrdersForUserUseCase = com.example.domain.usecase.GetOrdersForUserUseCase(ServiceLocator.orderRepository),
                authRepo = ServiceLocator.authRepository,
                cartRepo = ServiceLocator.cartRepository,
                updateOrderStatusUseCase = com.example.domain.usecase.UpdateOrderStatusUseCase(ServiceLocator.orderRepository)
            ) as T
        }
    })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)

    var trackingOrder by remember { mutableStateOf<Order?>(null) }
    var selectedDetailOrder by remember { mutableStateOf<Order?>(null) }

    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(state.reorderSuccess) {
        if (state.reorderSuccess) {
            snackbarHostState.showSnackbar(
                message = if (isArabic) "تمت إعادة الطلب وإضافة العناصر إلى السلة!" else "Reordered successfully! Items added to cart."
            )
            viewModel.dismissReorderSuccess()
        }
    }

    LaunchedEffect(state.error) {
        state.error?.let { err ->
            snackbarHostState.showSnackbar(
                message = err
            )
            viewModel.resetError()
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (isArabic) "طلباتي" else "My Orders",
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.testTag("orders_back_button")
                    ) {
                        Icon(
                            imageVector = if (isArabic) Icons.AutoMirrored.Default.ArrowForward else Icons.AutoMirrored.Default.ArrowBack,
                            contentDescription = if (isArabic) "رجوع" else "Back",
                            tint = BrandTextPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandBackground
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // 1. Order Status Capsule Row
            OrderStatusCapsuleRow(
                selectedTab = state.selectedTab,
                onTabSelected = { viewModel.selectTab(it) },
                isArabic = isArabic
            )

            // 2. Main Content
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
            ) {
                if (state.isLoading) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator(color = BrandPrimary)
                    }
                } else if (state.filteredOrders.isEmpty()) {
                    EmptyOrdersState(
                        isArabic = isArabic,
                        selectedTab = state.selectedTab,
                        onResetFilter = { viewModel.selectTab(OrderStatusTab.ALL) },
                        onNavigateToHome = onNavigateToHome
                    )
                } else {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("orders_list"),
                        contentPadding = PaddingValues(24.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(state.filteredOrders, key = { it.orderId }) { order ->
                            OrderCard(
                                order = order,
                                isArabic = isArabic,
                                onReorder = { viewModel.reorder(order) },
                                onTrack = { trackingOrder = order },
                                onCancel = { viewModel.cancelOrder(order.orderId) },
                                onSimulate = { viewModel.simulateStatusTransition(order.orderId, order.status) },
                                onClick = { selectedDetailOrder = order }
                            )
                        }
                    }
                }
            }
        }
    }

    // 3. Modals & Dialogs
    trackingOrder?.let { order ->
        TrackingTimelineDialog(
            order = order,
            isArabic = isArabic,
            onDismiss = { trackingOrder = null }
        )
    }

    selectedDetailOrder?.let { order ->
        OrderDetailDialog(
            order = order,
            isArabic = isArabic,
            onDismiss = { selectedDetailOrder = null },
            onReorder = { viewModel.reorder(order) }
        )
    }
}

@Composable
fun OrderStatusCapsuleRow(
    selectedTab: OrderStatusTab,
    onTabSelected: (OrderStatusTab) -> Unit,
    isArabic: Boolean
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(BrandBackground)
            .padding(vertical = 12.dp, horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        items(OrderStatusTab.values()) { tab ->
            val isSelected = selectedTab == tab
            val label = if (isArabic) tab.labelAr else tab.labelEn

            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(30.dp))
                    .background(
                        if (isSelected) BrandPrimary else BrandSurface
                    )
                    .clickable { onTabSelected(tab) }
                    .padding(horizontal = 18.dp, vertical = 10.dp)
                    .testTag("status_tab_${tab.key.lowercase()}")
            ) {
                Text(
                    text = label,
                    color = if (isSelected) Color.White else BrandTextPrimary,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    fontSize = 13.sp
                )
            }
        }
    }
}

@Composable
fun OrderCard(
    order: Order,
    isArabic: Boolean,
    onReorder: () -> Unit,
    onTrack: () -> Unit,
    onCancel: () -> Unit,
    onSimulate: () -> Unit,
    onClick: () -> Unit
) {
    val statusColor = getStatusColor(order.status)
    val statusLabel = getLocalizedStatusLabel(order.status, isArabic)
    val formattedDate = formatDate(order.createdAt)

    BrandCard(
        modifier = Modifier
            .fillMaxWidth()
            .testTag("order_card_${order.orderId}"),
        onClick = onClick
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            // Header: ID & Status Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "#${order.orderId.takeLast(7).uppercase()}",
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        fontSize = 15.sp
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = formattedDate,
                        color = BrandTextMuted,
                        fontSize = 11.sp
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(statusColor.copy(alpha = 0.12f))
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = statusLabel,
                        color = statusColor,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Divider
            HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)

            // Shop Name Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Storefront,
                    contentDescription = null,
                    tint = BrandPrimary,
                    modifier = Modifier.size(16.dp)
                )
                Text(
                    text = order.storeName,
                    fontWeight = FontWeight.SemiBold,
                    color = BrandTextPrimary,
                    fontSize = 13.sp
                )
            }

            // Product Items Stack
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                order.items.forEach { item ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = item.productImage.ifEmpty { "https://i.imgur.com/g0K5Iu9.jpeg" },
                            contentDescription = item.productName,
                            modifier = Modifier
                                .size(56.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(BrandSoftGray),
                            contentScale = ContentScale.Crop
                        )

                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = item.productName,
                                fontWeight = FontWeight.SemiBold,
                                color = BrandTextPrimary,
                                fontSize = 13.sp,
                                maxLines = 1
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = if (isArabic) "الكمية: ${item.quantity}" else "Qty: ${item.quantity}",
                                color = BrandTextMuted,
                                fontSize = 11.sp
                            )
                        }

                        // Price and Currency conversion
                        val formattedPrice = CurrencyManager.formatPrice(item.unitPrice, 13500.0, isArabic)

                        Text(
                            text = formattedPrice,
                            fontWeight = FontWeight.Bold,
                            color = BrandPrimary,
                            fontSize = 13.sp
                        )
                    }
                }
            }

            // Order Total row
            HorizontalDivider(color = BrandSoftGray, thickness = 1.dp)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val totalItemsCount = order.items.sumOf { it.quantity }
                Text(
                    text = if (isArabic) "$totalItemsCount عناصر" else "$totalItemsCount items",
                    color = BrandTextMuted,
                    fontSize = 12.sp
                )

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isArabic) "الإجمالي: " else "Total: ",
                        color = BrandTextMuted,
                        fontSize = 12.sp
                    )
                    Text(
                        text = CurrencyManager.formatPrice(order.totalAmount, 13500.0, isArabic),
                        fontWeight = FontWeight.Bold,
                        color = BrandTextPrimary,
                        fontSize = 16.sp
                    )
                }
            }

            // Quick Actions Panel
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Secondary action: Track or detail trigger
                OutlinedButton(
                    onClick = onTrack,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("track_button_${order.orderId}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = BrandPrimary
                    ),
                    border = ButtonDefaults.outlinedButtonBorder.copy(
                        brush = androidx.compose.ui.graphics.SolidColor(BrandPrimary)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.AltRoute,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "تتبع الشحنة" else "Track Shipment",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }

                // Primary Action: Reorder
                Button(
                    onClick = onReorder,
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .testTag("reorder_button_${order.orderId}"),
                    shape = RoundedCornerShape(10.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BrandPrimary,
                        contentColor = Color.White
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Replay,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = if (isArabic) "إعادة الطلب" else "Reorder",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp
                    )
                }
            }

            // Dev simulation actions line + cancel support
            val canCancel = order.status.lowercase() == "pending" || order.status.lowercase() == "processing"
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (canCancel) {
                    TextButton(
                        onClick = onCancel,
                        modifier = Modifier
                            .height(36.dp)
                            .testTag("cancel_button_${order.orderId}"),
                        colors = ButtonDefaults.textButtonColors(contentColor = BrandError)
                    ) {
                        Text(
                            text = if (isArabic) "إلغاء الطلب" else "Cancel Order",
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }

                // Subtle dev transition action
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Gray.copy(alpha = 0.1f))
                        .clickable { onSimulate() }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                        .testTag("simulate_button_${order.orderId}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.DeveloperMode,
                            contentDescription = null,
                            tint = BrandTextMuted,
                            modifier = Modifier.size(10.dp)
                        )
                        Text(
                            text = if (isArabic) "محاكي الحالة" else "Simulate Stage",
                            color = BrandTextMuted,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 9.sp
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersState(
    isArabic: Boolean,
    selectedTab: OrderStatusTab,
    onResetFilter: () -> Unit,
    onNavigateToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .testTag("orders_empty_state"),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .clip(CircleShape)
                .background(BrandSurface)
                .padding(24.dp),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.ReceiptLong,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.fillMaxSize()
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isArabic) "لا توجد طلبات هنا" else "No Orders Found",
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary,
            fontSize = 20.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = if (selectedTab != OrderStatusTab.ALL) {
                if (isArabic) "لا توجد معاملات تصفية تطابق فئة \"${selectedTab.labelAr}\"." else "No past history found with status \"${selectedTab.labelEn}\"."
            } else {
                if (isArabic) "لم تقم بإنشاء أي طلبات حتى الآن في وسيط بلس." else "You haven't initiated any orders or purchasing journeys yet on WasetPlus."
            },
            color = BrandTextMuted,
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.widthIn(max = 280.dp)
        )

        Spacer(modifier = Modifier.height(32.dp))

        if (selectedTab != OrderStatusTab.ALL) {
            OutlinedButton(
                onClick = onResetFilter,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(12.dp),
                border = ButtonDefaults.outlinedButtonBorder.copy(
                    brush = androidx.compose.ui.graphics.SolidColor(BrandPrimary)
                ),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary)
            ) {
                Text(
                    text = if (isArabic) "عرض جميع الطلبات" else "View All Orders",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }

            Spacer(modifier = Modifier.height(12.dp))
        }

        Button(
            onClick = onNavigateToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("start_shopping_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = BrandPrimary,
                contentColor = Color.White
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = if (isArabic) "البدء بالتسوق" else "Start Shopping",
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun TrackingTimelineDialog(
    order: Order,
    isArabic: Boolean,
    onDismiss: () -> Unit
) {
    val states = listOf("Pending", "Processing", "Shipped", "Delivered")
    val currentIndex = states.indexOfFirst { it.equals(order.status, ignoreCase = true) }
    // If status is Cancelled, we will display unique warning.
    val isCancelled = order.status.lowercase() == "cancelled"

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (isArabic) "تتبع شحنتك" else "Live Tracking",
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    fontSize = 18.sp
                )
                Text(
                    text = "#${order.orderId.takeLast(7).uppercase()}",
                    color = BrandPrimary,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (isCancelled) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandError.copy(alpha = 0.08f)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(imageVector = Icons.Default.Cancel, contentDescription = null, tint = BrandError)
                            Text(
                                text = if (isArabic) "تم إلغاء هذا الطلب من قبل البائع أو الزبون." else "This order was cancelled by the store owner or customer.",
                                color = BrandError,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                } else {
                    // Modern Timeline Steps
                    val stepsEn = listOf(
                        Triple("Order Created", "Your transaction is placed successfully", Icons.Default.Receipt),
                        Triple("Seller Accepted", "Store hand-verified items & preparing payload", Icons.Default.CheckCircle),
                        Triple("Dispatched", "Order has left store and registered with logistic partner", Icons.Default.LocalShipping),
                        Triple("Delivered successfully", "Package marked received at delivery coordinates", Icons.Default.TaskAlt)
                    )

                    val stepsAr = listOf(
                        Triple("تم تقديم الطلب", "وُضع طلبك بنجاح في تطبيق وسيط بلس", Icons.Default.Receipt),
                        Triple("تم القبول من المعرض", "قام البائع بتأكيد طلبك وتجهيز الطرد البديل", Icons.Default.CheckCircle),
                        Triple("تم شحن الطرد", "غادر الطرد المعرض وتم تسليمه لشركة التوصيل", Icons.Default.LocalShipping),
                        Triple("توصيل آمن وناجح", "تم استلام الطرد وتوقيع وصوله للموقع المحدد", Icons.Default.TaskAlt)
                    )

                    val steps = if (isArabic) stepsAr else stepsEn

                    Column {
                        steps.forEachIndexed { index, (stepTitle, stepDesc, icon) ->
                            val isCompleted = index <= currentIndex
                            val isActive = index == currentIndex
                            val activeBorderColor = if (isCompleted) BrandPrimary else Color.LightGray

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Left icon + line column
                                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(CircleShape)
                                            .background(
                                                if (isCompleted) BrandPrimary.copy(alpha = 0.15f) else Color.LightGray.copy(
                                                    alpha = 0.15f
                                                )
                                            ),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = icon,
                                            contentDescription = null,
                                            tint = if (isCompleted) BrandPrimary else Color.LightGray,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    if (index < steps.size - 1) {
                                        Box(
                                            modifier = Modifier
                                                .width(2.dp)
                                                .height(40.dp)
                                                .background(activeBorderColor)
                                        )
                                    }
                                }

                                // Right info description
                                Column(modifier = Modifier.padding(top = 2.dp)) {
                                    Text(
                                        text = stepTitle,
                                        fontWeight = if (isActive) FontWeight.ExtraBold else FontWeight.Bold,
                                        color = if (isCompleted) BrandTextPrimary else BrandTextMuted,
                                        fontSize = 14.sp
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = stepDesc,
                                        color = BrandTextMuted,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }
                    }
                }

                // Escrow Notice Panel
                Card(
                    colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.05f)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.VerifiedUser,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(16.dp)
                        )
                        Text(
                            text = if (isArabic) "تعمل المنصة كوسيط تنسيقي بينك وبين البائع. يرجى التنسيق المباشر وترتيب تفاصيل الدفع الفعلي مع البائع." else "The platform acts as a coordination intermediary. Please coordinate directly with the seller to finalize actual payment arrangement details.",
                            color = BrandPrimary,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(text = if (isArabic) "إغلاق" else "Close", color = BrandPrimary, fontWeight = FontWeight.Bold)
            }
        },
        containerColor = BrandSurface,
        shape = RoundedCornerShape(20.dp)
    )
}

@Composable
fun OrderDetailDialog(
    order: Order,
    isArabic: Boolean,
    onDismiss: () -> Unit,
    onReorder: () -> Unit
) {
    androidx.compose.ui.window.Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            elevation = CardDefaults.cardElevation(8.dp)
        ) {
            OrderDetailsLayout(
                order = order,
                isArabic = isArabic,
                isSellerView = false,
                headerActions = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = if (isArabic) "إغلاق" else "Close",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                bottomActions = {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = onDismiss,
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = if (isArabic) "إغلاق" else "Close",
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }

                        Button(
                            onClick = {
                                onDismiss()
                                onReorder()
                            },
                            modifier = Modifier.weight(1.5f),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text(
                                text = if (isArabic) "إعادة طلب العناصر" else "Reorder Items",
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        }
                    }
                }
            )
        }
    }
}

// ------------------------------------------------------------------------
// Internal Formatting Utilities
// ------------------------------------------------------------------------

fun getStatusColor(status: String): Color {
    return when (status.lowercase()) {
        "pending" -> Color(0xFFFF9800) // Orange
        "processing" -> Color(0xFF2196F3) // Blue
        "shipped" -> Color(0xFF9C27B0) // Indigo
        "delivered" -> BrandPrimary // Brand green
        "cancelled" -> BrandError // Red / Dark Grey
        else -> Color.Gray
    }
}

fun getLocalizedStatusLabel(status: String, isArabic: Boolean): String {
    return when (status.lowercase()) {
        "pending" -> if (isArabic) "قيد الانتظار" else "Pending"
        "processing" -> if (isArabic) "قيد التحضير" else "Processing"
        "shipped" -> if (isArabic) "تم الشحن" else "Shipped"
        "delivered" -> if (isArabic) "تم التوصيل" else "Delivered"
        "cancelled" -> if (isArabic) "تم الإلغاء" else "Cancelled"
        else -> status
    }
}

fun formatDate(timestamp: Long): String {
    if (timestamp <= 0) return "--"
    return try {
        val date = Date(timestamp)
        val format = SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
        format.format(date)
    } catch (e: Exception) {
        "--"
    }
}

fun formatCurrencyString(amount: Double, isArabic: Boolean): String {
    return CurrencyManager.formatPrice(amount, 13500.0, isArabic)
}
