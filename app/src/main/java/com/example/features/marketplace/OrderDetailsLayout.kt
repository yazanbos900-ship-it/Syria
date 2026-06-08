package com.example.features.marketplace

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.core.utils.CurrencyManager
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.ui.theme.BrandPrimary
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun OrderDetailsLayout(
    order: Order,
    isArabic: Boolean,
    storeRate: Double = 13500.0,
    isSellerView: Boolean = false,
    headerActions: @Composable RowScope.() -> Unit = {},
    bottomActions: @Composable () -> Unit = {}
) {
    val formattedDate = try {
        val sdf = SimpleDateFormat("dd MMM yyyy, hh:mm a", if (isArabic) Locale("ar") else Locale.US)
        sdf.format(Date(order.createdAt))
    } catch (e: Exception) {
        "Just now"
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.surface)
    ) {
        // Sticky Header with proper M3 styling
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.ReceiptLong,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
                Text(
                    text = if (isArabic) "تفاصيل الطلب" else "Order Details",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontSize = 18.sp
                )
            }
            Row {
                headerActions()
            }
        }

        // Scrollable Content
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Order Header
            item {
                SectionCard {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top
                    ) {
                        Column {
                            Text(
                                text = if (isArabic) "رقم الطلب" else "Order ID",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                            Text(
                                text = "#${order.orderId.uppercase()}",
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface,
                                fontSize = 14.sp
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = formattedDate,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }

                        // Status Badge
                        OrderStatusBadge(status = order.status, isArabic = isArabic)
                    }
                }
            }

            // 2. Customer Info
            item {
                SectionCard {
                    SectionTitle(
                        title = if (isArabic) "معلومات الزبون" else "Customer Info",
                        icon = Icons.Default.Person
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = if (isArabic) "الاسم" else "Name",
                        value = order.customerName.ifBlank { if (isArabic) "غير متوفر" else "N/A" }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    InfoRow(
                        label = if (isArabic) "رقم الهاتف" else "Phone",
                        value = order.customerPhone.ifBlank { "N/A" }
                    )
                }
            }

            // 3. Delivery / Shipping Info
            item {
                SectionCard {
                    SectionTitle(
                        title = if (isArabic) "معلومات التوصيل" else "Delivery Info",
                        icon = Icons.Default.LocalShipping
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    InfoRow(
                        label = if (isArabic) "العنوان" else "Address",
                        value = order.shippingAddress.ifBlank { if (isArabic) "غير متوفر" else "N/A" }
                    )
                    if (order.city.isNotBlank() || order.selectedDeliveryArea.isNotBlank()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        InfoRow(
                            label = if (isArabic) "المنطقة" else "Area",
                            value = listOf(order.city, order.selectedDeliveryArea).filter { it.isNotBlank() }.joinToString(" - ")
                        )
                    }
                }
            }

            // 4. Items List
            item {
                SectionTitle(
                    title = if (isArabic) "العناصر المطلوبة" else "Ordered Items",
                    icon = Icons.Default.ShoppingBag,
                    modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
                )
            }

            items(order.items) { item ->
                ItemRow(item = item, isArabic = isArabic, storeRate = storeRate)
            }

            // 5. Pricing Summary
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionCard {
                    SectionTitle(
                        title = if (isArabic) "ملخص الدفع" else "Pricing Summary",
                        icon = Icons.Default.Payments
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    
                    PricingRow(
                        label = if (isArabic) "المجموع الفرعي" else "Subtotal",
                        value = CurrencyManager.formatPrice(order.subtotal.takeIf { it > 0 } ?: order.totalAmount, storeRate, isArabic)
                    )
                    PricingRow(
                        label = if (isArabic) "رسوم التوصيل" else "Delivery Fee",
                        value = if (order.shippingFee > 0) CurrencyManager.formatPrice(order.shippingFee, storeRate, isArabic) else (if (isArabic) "مجاني" else "Free")
                    )
                    if (order.vatAmount > 0) {
                        PricingRow(
                            label = if (isArabic) "الضريبة" else "VAT",
                            value = CurrencyManager.formatPrice(order.vatAmount, storeRate, isArabic)
                        )
                    }
                    
                    DividerRow()
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (isArabic) "الإجمالي" else "Total",
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface,
                            fontSize = 16.sp
                        )
                        Text(
                            text = CurrencyManager.formatPrice(order.grandTotal.takeIf { it > 0 } ?: order.totalAmount, storeRate, isArabic),
                            fontWeight = FontWeight.Black,
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 18.sp
                        )
                    }
                }
            }
        }

        // 6. Actions (Buttons) at bottom
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp)
        ) {
            bottomActions()
        }
    }
}

@Composable
private fun SectionCard(content: @Composable ColumnScope.() -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f)
        ),
        elevation = CardDefaults.cardElevation(0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            content = content
        )
    }
}

@Composable
private fun SectionTitle(title: String, icon: ImageVector, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(
            text = title,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun InfoRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top
    ) {
        Text(
            text = label,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 13.sp,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = 13.sp,
            modifier = Modifier.weight(1.5f),
            textAlign = androidx.compose.ui.text.style.TextAlign.End
        )
    }
}

@Composable
private fun ItemRow(item: OrderItem, isArabic: Boolean, storeRate: Double) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AsyncImage(
            model = item.productImage.ifEmpty { "https://i.imgur.com/g0K5Iu9.jpeg" },
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .size(56.dp)
                .clip(RoundedCornerShape(8.dp))
                .background(MaterialTheme.colorScheme.surfaceVariant)
        )

        Spacer(modifier = Modifier.width(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.productName,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 14.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(modifier = Modifier.height(4.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(
                    text = "${CurrencyManager.formatPrice(item.unitPrice, storeRate, isArabic)} x ${item.quantity}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Text(
            text = CurrencyManager.formatPrice(item.unitPrice * item.quantity, storeRate, isArabic),
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
    }
}

@Composable
private fun PricingRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(text = label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 13.sp)
        Text(text = value, color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DividerRow() {
    HorizontalDivider(
        modifier = Modifier.padding(vertical = 12.dp),
        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.2f),
        thickness = 1.dp
    )
}

@Composable
fun OrderStatusBadge(status: String, isArabic: Boolean) {
    val (bgColor, textColor) = when (status.lowercase()) {
        "pending" -> Color(0xFFFFB300).copy(alpha = 0.15f) to Color(0xFFFFB300)
        "processing" -> Color(0xFF29B6F6).copy(alpha = 0.15f) to Color(0xFF29B6F6)
        "shipped" -> Color(0xFF26A69A).copy(alpha = 0.15f) to Color(0xFF26A69A)
        "delivered" -> BrandPrimary.copy(alpha = 0.15f) to BrandPrimary
        "cancelled" -> Color.Red.copy(alpha = 0.15f) to Color.Red
        else -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }

    val label = when (status.lowercase()) {
        "pending" -> if (isArabic) "قيد الانتظار" else "Pending"
        "processing" -> if (isArabic) "قيد التحضير" else "Processing"
        "shipped" -> if (isArabic) "تم الشحن" else "Shipped"
        "delivered" -> if (isArabic) "تم التوصيل" else "Delivered"
        "cancelled" -> if (isArabic) "ملغي" else "Cancelled"
        else -> status
    }

    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(bgColor)
            .padding(horizontal = 10.dp, vertical = 6.dp)
    ) {
        Text(
            text = label,
            color = textColor,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp
        )
    }
}
