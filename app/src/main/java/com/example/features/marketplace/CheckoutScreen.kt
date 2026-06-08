package com.example.features.marketplace

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.R
import com.example.core.utils.CurrencyManager
import com.example.core.utils.LanguageManager
import com.example.ui.theme.*
import com.example.components.MapLocationPicker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToOrders: () -> Unit,
    viewModel: CheckoutViewModel = viewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val isArabic = LanguageManager.isArabic(context)

    // Handle Toast Errors
    LaunchedEffect(state.error) {
        state.error?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_LONG).show()
            viewModel.resetError()
        }
    }

    LaunchedEffect(state.verificationError) {
        state.verificationError?.let { err ->
            Toast.makeText(context, err, Toast.LENGTH_SHORT).show()
            viewModel.resetVerificationError()
        }
    }

    Scaffold(
        containerColor = BrandBackground,
        contentWindowInsets = WindowInsets.safeDrawing,
        topBar = {
            CenterAlignedTopAppBar(
                title = {
                    Text(
                        text = if (state.showVerification) {
                            if (isArabic) "توثيق عملية الدفع" else "Verify Transaction"
                        } else if (state.orderPlacedSuccess) {
                            if (isArabic) "اكتمل الطلب" else "Order Ready"
                        } else {
                            if (isArabic) "مراجعة الطلب وتأكيده" else "Review & Confirm"
                        },
                        fontWeight = FontWeight.ExtraBold,
                        color = BrandTextPrimary,
                        fontSize = 18.sp
                    )
                },
                navigationIcon = {
                    if (!state.orderPlacedSuccess) {
                        IconButton(
                            onClick = {
                                if (state.showVerification) {
                                    // Go back to form
                                    viewModel.startCountdown() // reset timer on back optional
                                    // but we just reset showVerification
                                    // to allow updating payment methods
                                } else {
                                    onNavigateBack()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Default.ArrowBack,
                                contentDescription = if (isArabic) "رجوع" else "Back",
                                tint = BrandTextPrimary
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
                    containerColor = BrandBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            when {
                state.orderPlacedSuccess -> {
                    CheckoutSuccessView(
                        isArabic = isArabic,
                        state = state,
                        onGoToOrders = onNavigateToOrders,
                        onGoToHome = onNavigateBack
                    )
                }
                else -> {
                    CheckoutDetailsView(
                        isArabic = isArabic,
                        state = state,
                        onNameChange = { viewModel.onNameChange(it) },
                        onPhoneChange = { viewModel.onPhoneChange(it) },
                        onAddressChange = { viewModel.onAddressChange(it) },
                        onDeliveryAreaChange = { viewModel.onDeliveryAreaChange(it) },
                        onPaymentMethodChange = { viewModel.onPaymentMethodChange(it) },
                        onSubmit = { viewModel.processCheckout() },
                        onMapVisibleChange = { viewModel.setMapVisible(it) },
                        onLocationSelect = { lat, lng, country, city, district, street, address -> 
                            viewModel.selectLocationCoordinates(lat, lng, country, city, district, street, address) 
                        },
                        onAddressDetailsChange = { bldg, apt, flr, land, notes ->
                            viewModel.updateDetailsFields(bldg, apt, flr, land, notes)
                        }
                    )
                }
            }

            // (Simulated SMS overlay removed for marketplace flow)

            // Simple loading overlay
            if (state.isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color.Black.copy(alpha = 0.5f))
                        .clickable(enabled = false) {},
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = BrandSurface),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.widthIn(min = 280.dp, max = 340.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            CircularProgressIndicator(color = BrandPrimary)
                            Text(
                                text = state.progressMessage.ifBlank { "Processing..." },
                                color = BrandTextPrimary,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CheckoutDetailsView(
    isArabic: Boolean,
    state: CheckoutUiState,
    onNameChange: (String) -> Unit,
    onPhoneChange: (String) -> Unit,
    onAddressChange: (String) -> Unit,
    onDeliveryAreaChange: (String) -> Unit,
    onPaymentMethodChange: (String) -> Unit,
    onSubmit: () -> Unit,
    onMapVisibleChange: (Boolean) -> Unit,
    onLocationSelect: (Double, Double, String, String, String, String, String) -> Unit,
    onAddressDetailsChange: (building: String, apartment: String, floor: String, landmark: String, notes: String) -> Unit
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp)
    ) {
        // Step header
        Text(
            text = if (isArabic) "معلومات مستلم الشحنة" else "Delivery Coordinates",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = BrandTextPrimary
        )

        // Info input fields
        OutlinedTextField(
            value = state.customerName,
            onValueChange = onNameChange,
            label = { Text(if (isArabic) "الاسم الكامل" else "Full Name") },
            textStyle = LocalTextStyle.current.copy(color = BrandTextPrimary),
            leadingIcon = { Icon(Icons.Default.Person, null, tint = BrandPrimary) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_name_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BrandSoftGray,
                focusedLabelColor = BrandPrimary
            )
        )

        OutlinedTextField(
            value = state.customerPhone,
            onValueChange = onPhoneChange,
            label = { Text(if (isArabic) "رقم الموبايل" else "Mobile Number") },
            textStyle = LocalTextStyle.current.copy(color = BrandTextPrimary),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
            leadingIcon = { Icon(Icons.Default.Phone, null, tint = BrandPrimary) },
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("checkout_phone_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = BrandPrimary,
                unfocusedBorderColor = BrandSoftGray,
                focusedLabelColor = BrandPrimary
            )
        )

        // Delivery Area Selector (Dialog-based, extremely stable, zero sizing jumps)
        var showAreaDialog by remember { mutableStateOf(false) }

        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                text = if (isArabic) "منطقة التوصيل والمدينة الجغرافية" else "Delivery Area & City",
                fontSize = 12.sp,
                color = BrandPrimary,
                fontWeight = FontWeight.SemiBold
            )
            
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(BrandSurface)
                    .border(1.dp, BrandSoftGray, RoundedCornerShape(12.dp))
                    .clickable { showAreaDialog = true }
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = BrandPrimary
                        )
                        Text(
                            text = if (isArabic) {
                                when(state.selectedDeliveryArea) {
                                    "Damascus" -> "دمشق (Damascus)"
                                    "Aleppo" -> "حلب (Aleppo)"
                                    "Homs" -> "حمص (Homs)"
                                    "Hama" -> "حماة (Hama)"
                                    "Latakia" -> "اللاذقية (Latakia)"
                                    "Tartous" -> "طرطوس (Tartous)"
                                    else -> state.selectedDeliveryArea
                                }
                            } else {
                                state.selectedDeliveryArea
                            },
                            color = BrandTextPrimary,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null,
                        tint = BrandTextMuted
                    )
                }
            }
        }

        if (showAreaDialog) {
            AlertDialog(
                onDismissRequest = { showAreaDialog = false },
                containerColor = BrandSurface,
                title = {
                    Text(
                        text = if (isArabic) "اختر منطقة التوصيل" else "Select Delivery Area",
                        color = BrandTextPrimary,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        state.deliveryAreas.forEach { area ->
                            val areaNameAr = when(area) {
                                "Damascus" -> "دمشق"
                                "Aleppo" -> "حلب"
                                "Homs" -> "حمص"
                                "Hama" -> "حماة"
                                "Latakia" -> "اللاذقية"
                                "Tartous" -> "طرطوس"
                                else -> area
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        onDeliveryAreaChange(area)
                                        showAreaDialog = false
                                    }
                                    .padding(vertical = 12.dp, horizontal = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = if (isArabic) "$areaNameAr ($area)" else "$area ($areaNameAr)",
                                    color = BrandTextPrimary,
                                    fontSize = 14.sp,
                                    fontWeight = if (state.selectedDeliveryArea == area) FontWeight.Bold else FontWeight.Normal
                                )
                                if (state.selectedDeliveryArea == area) {
                                    Icon(
                                        imageVector = Icons.Default.Check,
                                        contentDescription = null,
                                        tint = BrandPrimary
                                    )
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { showAreaDialog = false }) {
                        Text(
                            text = if (isArabic) "إلغاء" else "Cancel",
                            color = BrandPrimary
                        )
                    }
                }
            )
        }

        if (state.mapVisible) {
            MapLocationPicker(
                initialLatitude = state.latitude,
                initialLongitude = state.longitude,
                onLocationConfirmed = { lat, lng, country, city, district, street, fullAddress ->
                    onLocationSelect(lat, lng, country, city, district, street, fullAddress)
                    onMapVisibleChange(false)
                },
                onDismissRequest = { onMapVisibleChange(false) },
                isArabic = isArabic
            )
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            border = BorderStroke(1.dp, if (state.latitude == null) BrandPrimary.copy(alpha = 0.5f) else BrandSoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PinDrop,
                            contentDescription = null,
                            tint = BrandPrimary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (isArabic) "موقع التوصيل والشحن" else "Delivery Coordinates",
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = BrandTextPrimary
                        )
                    }
                    Button(
                        onClick = { onMapVisibleChange(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = RoundedCornerShape(8.dp),
                        contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (isArabic) "اختر من الخريطة" else "Map Picker",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                if (state.latitude == null) {
                    Text(
                        text = if (isArabic) "يرجى تحديد موقع التوصيل على الخريطة للمتابعة" else "Please select your delivery point on the map to proceed.",
                        fontSize = 12.sp,
                        color = Color.Red,
                        modifier = Modifier.padding(vertical = 4.dp)
                    )
                } else {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandSoftGray.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                            .padding(10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Map, null, tint = BrandTextPrimary, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "${state.locationDistrict}, ${state.locationCity}, ${state.locationCountry}",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = BrandTextPrimary
                        )
                    }
                    
                    Text(
                        text = "Lat: " + String.format("%.5f", state.latitude) + ", Lng: " + String.format("%.5f", state.longitude),
                        fontSize = 10.sp,
                        color = BrandTextMuted
                    )
                }

                HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)

                Text(
                    text = if (isArabic) "تفاصيل العنوان الإضافية" else "Additional Address Details",
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = BrandTextPrimary
                )

                var building by remember(state.buildingNumber) { mutableStateOf(state.buildingNumber) }
                var apartment by remember(state.apartment) { mutableStateOf(state.apartment) }
                var floor by remember(state.floor) { mutableStateOf(state.floor) }
                var landmark by remember(state.landmark) { mutableStateOf(state.landmark) }
                var additionalNotes by remember(state.additionalNotes) { mutableStateOf(state.additionalNotes) }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = building,
                        onValueChange = {
                            building = it
                            onAddressDetailsChange(building, apartment, floor, landmark, additionalNotes)
                        },
                        label = { Text(if (isArabic) "رقم البناء" else "Bldg No.") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandSoftGray)
                    )
                    OutlinedTextField(
                        value = floor,
                        onValueChange = {
                            floor = it
                            onAddressDetailsChange(building, apartment, floor, landmark, additionalNotes)
                        },
                        label = { Text(if (isArabic) "الطابق" else "Floor") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandSoftGray)
                    )
                    OutlinedTextField(
                        value = apartment,
                        onValueChange = {
                            apartment = it
                            onAddressDetailsChange(building, apartment, floor, landmark, additionalNotes)
                        },
                        label = { Text(if (isArabic) "شقة" else "Apt") },
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandSoftGray)
                    )
                }

                OutlinedTextField(
                    value = landmark,
                    onValueChange = {
                        landmark = it
                        onAddressDetailsChange(building, apartment, floor, landmark, additionalNotes)
                    },
                    label = { Text(if (isArabic) "علامة مميزة (مثال: بجانب مسجد...)" else "Landmark (e.g. Near Mosque)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandSoftGray)
                )

                OutlinedTextField(
                    value = additionalNotes,
                    onValueChange = {
                        additionalNotes = it
                        onAddressDetailsChange(building, apartment, floor, landmark, additionalNotes)
                    },
                    label = { Text(if (isArabic) "ملاحظات إضافية للمندوب..." else "Additional delivery instructions...") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    colors = OutlinedTextFieldDefaults.colors(focusedBorderColor = BrandPrimary, unfocusedBorderColor = BrandSoftGray)
                )

                // Render compiled final shipping address read-only preview
                if (state.shippingAddress.isNotBlank()) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(BrandPrimary.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .padding(12.dp)
                    ) {
                        Text(
                            text = if (isArabic) "العنوان المعتمد للتوصيل:\n${state.shippingAddress}" else "Compiled Shipping Address:\n${state.shippingAddress}",
                            fontSize = 11.sp,
                            color = BrandPrimary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)

        // Intermediary explanation note
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.06f)),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(14.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.Top
            ) {
                Icon(
                    imageVector = Icons.Default.Info,
                    contentDescription = "Platform Intermediary Notice",
                    tint = BrandPrimary,
                    modifier = Modifier.size(20.dp)
                )
                Text(
                    text = if (isArabic) {
                        "تعمل هذه المنصة كـ وسيط سحابي محايد ولا تتم معالجة أو تحصيل أموال حقيقية عبرها. يتم ترتيب وتأكيد تفاصيل الدفع والتسليم الفعلي مباشرة بين البائع والمشتري خارج المنصة."
                    } else {
                        "The platform acts as an intermediary marketplace. Payment and delivery arrangements are coordinated directly between buyer and seller."
                    },
                    fontSize = 12.sp,
                    color = BrandTextPrimary,
                    lineHeight = 18.sp,
                    fontWeight = FontWeight.Medium
                )
            }
        }

        HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)

        // Summary breakdowns (dynamic values from ViewModel)
        val subtotal = state.subtotal
        val vatAmount = state.vatAmount
        val shipping = state.shippingFee
        val total = state.grandTotal

        Card(
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandSoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = if (isArabic) "تفاصيل الفاتورة" else "Summary Invoice",
                    fontWeight = FontWeight.Bold,
                    color = BrandTextPrimary,
                    fontSize = 13.sp
                )

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "المجموع الفرعي لمواد السلة:" else "Products Subtotal:", color = BrandTextMuted, fontSize = 12.sp)
                    Text(text = CurrencyManager.formatPrice(subtotal, 13500.0, isArabic), color = BrandTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "أجور وضريبة القيمة المضافة (3%):" else "VAT (3%):", color = BrandTextMuted, fontSize = 12.sp)
                    Text(text = CurrencyManager.formatPrice(vatAmount, 13500.0, isArabic), color = BrandTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(text = if (isArabic) "أجور الشحن وتوصيل الطرود:" else "Shipping Fee:", color = BrandTextMuted, fontSize = 12.sp)
                    Text(text = CurrencyManager.formatPrice(shipping, 13500.0, isArabic), color = BrandTextPrimary, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }

                HorizontalDivider(color = BrandSoftGray, thickness = 0.5.dp)

                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(text = if (isArabic) "المبلغ الإجمالي المستحق:" else "Grand Total:", color = BrandTextPrimary, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    Text(text = CurrencyManager.formatPrice(total, 13500.0, isArabic), color = BrandPrimary, fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Button(
            onClick = onSubmit,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("checkout_submit_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isArabic) "تأكيد وإرسال الطلب • ${CurrencyManager.formatPrice(total, 13500.0, isArabic)}" else "Confirm Order • ${CurrencyManager.formatPrice(total, 13500.0, isArabic)}",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp
            )
        }
    }
}

@Composable
fun PaymentMethodCard(
    title: String,
    description: String,
    color: Color,
    icon: Any,
    isSelected: Boolean,
    isSelectedYellow: Boolean = false,
    isArabic: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                if (isSelectedYellow) color.copy(alpha = 0.12f) else color.copy(alpha = 0.08f)
            } else BrandSurface
        ),
        shape = RoundedCornerShape(14.dp),
        border = BorderStroke(
            width = if (isSelected) 2.dp else 1.dp,
            color = if (isSelected) color else BrandSoftGray
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(color.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (icon is androidx.compose.ui.graphics.vector.ImageVector) {
                    Icon(imageVector = icon, contentDescription = null, tint = color, modifier = Modifier.size(22.dp))
                }
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (title == "Syriatel Cash") {
                        if (isArabic) "سيرياتيل كاش (محفظة)" else "Syriatel Cash Wallet"
                    } else if (title == "MTN Cash") {
                        if (isArabic) "ام تي ان كاش (محفظة)" else "MTN Cash Wallet"
                    } else {
                        if (isArabic) "الدفع عند الاستلام" else "Cash On Delivery (COD)"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp,
                    color = BrandTextPrimary
                )
                Text(
                    text = description,
                    fontSize = 11.sp,
                    color = BrandTextMuted
                )
            }

            RadioButton(
                selected = isSelected,
                onClick = onClick,
                colors = RadioButtonDefaults.colors(selectedColor = color, unselectedColor = BrandTextMuted)
            )
        }
    }
}

@Composable
fun CheckoutVerificationView(
    isArabic: Boolean,
    state: CheckoutUiState,
    onOtpChange: (String) -> Unit,
    onVerify: () -> Unit,
    onResend: () -> Unit,
    onDismissSms: () -> Unit
) {
    val txn = state.currentTransaction ?: return
    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        val themeColor = if (txn.paymentMethod.contains("Syriatel", ignoreCase = true)) {
            Color(0xFFD32F2F)
        } else {
            Color(0xFFFFB300)
        }

        // Provider Header Card
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandSurface),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, BrandSoftGray),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(54.dp)
                        .clip(CircleShape)
                        .background(themeColor.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Security,
                        contentDescription = null,
                        tint = themeColor,
                        modifier = Modifier.size(28.dp)
                    )
                }

                Text(
                    text = if (txn.paymentMethod.contains("Syriatel", ignoreCase = true)) "Syriatel Cash SecurePay" else "MTN Cash Wallet Gateway",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = BrandTextPrimary
                )

                Text(
                    text = if (isArabic) "رقم الهاتف المحفظ: ${state.customerPhone}" else "Wallet Mobile: ${state.customerPhone}",
                    color = BrandTextMuted,
                    fontSize = 12.sp
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceAround
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isArabic) "الرقم المرجعي المعرف" else "Ref. Ticket ID", color = BrandTextMuted, fontSize = 10.sp)
                        Text(text = txn.transactionId, fontWeight = FontWeight.Bold, color = BrandTextPrimary, fontSize = 13.sp)
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(text = if (isArabic) "المبلغ الإجمالي" else "Total Premium", color = BrandTextMuted, fontSize = 10.sp)
                        Text(text = CurrencyManager.formatPrice(txn.amount, 13500.0, isArabic), fontWeight = FontWeight.Bold, color = BrandPrimary, fontSize = 13.sp)
                    }
                }
            }
        }

        // Verification Sent Toast Simulation
        Card(
            colors = CardDefaults.cardColors(containerColor = BrandPrimary.copy(alpha = 0.08f)),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.CellTower, null, tint = BrandPrimary, modifier = Modifier.size(16.dp))
                Text(
                    text = if (isArabic) "تم إرسال رمز التحقق إلى رقم موبايلك المسجل!" else "Verification code sent to your mobile number",
                    color = BrandSecondary,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }

        // Simulated SMS Preview Card
        AnimatedVisibility(
            visible = state.showSimulatedSms,
            enter = slideInVertically() + fadeIn(),
            exit = slideOutVertically() + fadeOut()
        ) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1E2E)),
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onDismissSms() }
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(20.dp)
                                    .clip(CircleShape)
                                    .background(BrandPrimary),
                                contentAlignment = Alignment.Center
                            ) {
                                Text("W", color = Color.White, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                            }
                            Text(
                                text = "WasetPlus Payment",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp
                            )
                        }

                        IconButton(onClick = onDismissSms, modifier = Modifier.size(24.dp)) {
                            Icon(Icons.Default.Close, null, tint = Color.LightGray, modifier = Modifier.size(14.dp))
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = if (isArabic) {
                            "رمز التحقق الخاص بك لخدمة واصل بلس للدفع هو:\n\n${state.simulatedSmsBody}\n\nلا تشارك هذا الرمز مع أي شخص.\nهذا مجرد رمز محاكاة لأغراض العرض التوضيحي."
                        } else {
                            "Your verification code is:\n\n${state.simulatedSmsBody}\n\nDo not share this code with anyone.\nThis is only a simulation for demo purposes."
                        },
                        color = Color.White.copy(alpha = 0.9f),
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        lineHeight = 16.sp
                    )
                }
            }
        }

        // OTP inputs
        OutlinedTextField(
            value = state.verificationOtp,
            onValueChange = { if (it.length <= 6) onOtpChange(it) },
            label = { Text(if (isArabic) "أدخل الرمز المكون من 6 أرقام" else "Input 6-digit OTP Code") },
            textStyle = LocalTextStyle.current.copy(
                color = BrandTextPrimary,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 18.sp,
                textAlign = TextAlign.Center
            ),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier
                .fillMaxWidth()
                .testTag("verification_otp_input"),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = themeColor,
                unfocusedBorderColor = BrandSoftGray,
                focusedLabelColor = themeColor
            )
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Timer counter
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HourglassBottom,
                    contentDescription = null,
                    tint = if (state.securityTimerSeconds > 0) BrandPrimary else Color.Red,
                    modifier = Modifier.size(14.dp)
                )
                Text(
                    text = if (state.securityTimerSeconds > 0) {
                        "${state.securityTimerSeconds}s"
                    } else {
                        if (isArabic) "منتهي الصلاحية" else "Expired"
                    },
                    color = if (state.securityTimerSeconds > 0) BrandTextPrimary else Color.Red,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.sp
                )
            }

            // Resend Button
            TextButton(
                onClick = onResend,
                enabled = state.securityTimerSeconds == 0
            ) {
                Text(
                    text = if (isArabic) "إعادة إرسال الرمز" else "Resend Code",
                    color = if (state.securityTimerSeconds == 0) BrandPrimary else BrandTextMuted,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Button(
            onClick = onVerify,
            enabled = state.verificationOtp.length == 6,
            modifier = Modifier
                .fillMaxWidth()
                .height(54.dp)
                .testTag("checkout_verify_confirm_button"),
            colors = ButtonDefaults.buttonColors(containerColor = themeColor),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text(
                text = if (isArabic) "تأكيد والتحقق من الدفع" else "Confirm & Authorize Payment",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun CheckoutSuccessView(
    isArabic: Boolean,
    state: CheckoutUiState,
    onGoToOrders: () -> Unit,
    onGoToHome: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(80.dp)
                .clip(CircleShape)
                .background(Color(0xFFE8F5E9)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Default.CheckCircle,
                contentDescription = null,
                tint = Color(0xFF2E7D32),
                modifier = Modifier.size(54.dp)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = if (isArabic) "تم استلام الطلب بنجاح!" else "Order Received Successfully!",
            fontWeight = FontWeight.ExtraBold,
            color = BrandTextPrimary,
            fontSize = 18.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (isArabic) {
                "تم إرسال طلبك إلى البائع بنجاح. يرجى التواصل مع البائع لترتيب وتسليم الطلب."
            } else {
                "Your order request has been sent to the seller. Please coordinate with the seller for arrangement and delivery."
            },
            color = BrandTextMuted,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Button(
            onClick = onGoToOrders,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("checkout_success_orders_button"),
            colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ListAlt,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "الذهاب وقراءة تفاصيل طلباتي" else "View Order History",
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp
            )
        }

        Spacer(modifier = Modifier.height(12.dp))

        OutlinedButton(
            onClick = onGoToHome,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("checkout_success_home_button"),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = BrandPrimary),
            shape = RoundedCornerShape(12.dp),
            border = BorderStroke(1.dp, BrandSoftGray)
        ) {
            Icon(
                imageVector = Icons.Default.Storefront,
                contentDescription = null,
                tint = BrandPrimary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = if (isArabic) "مواصلة التسوق" else "Continue Shopping",
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}
