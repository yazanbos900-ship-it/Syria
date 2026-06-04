package com.example.features.marketplace

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.core.di.ServiceLocator
import com.example.domain.model.Order
import com.example.domain.model.OrderItem
import com.example.domain.model.PaymentTransaction
import com.example.domain.usecase.CreatePaymentTransactionUseCase
import com.example.domain.usecase.VerifyPaymentOtpUseCase
import com.example.domain.usecase.OtpVerificationResult
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

data class CheckoutUiState(
    val isLoading: Boolean = false,
    val cartItems: List<CartItem> = emptyList(),
    val customerName: String = "",
    val customerPhone: String = "",
    val shippingAddress: String = "",
    val latitude: Double? = 33.5138, // Default to Damascus
    val longitude: Double? = 36.2947,
    val locationCountry: String = "Syria",
    val locationCity: String = "Damascus",
    val locationDistrict: String = "Bab Touma",
    val locationStreet: String = "",
    val buildingNumber: String = "",
    val apartment: String = "",
    val floor: String = "",
    val landmark: String = "",
    val additionalNotes: String = "",
    val mapVisible: Boolean = false,
    val paymentMethod: String = "Syriatel Cash", // "Syriatel Cash", "MTN Cash", "Cash On Delivery"
    val error: String? = null,
    
    // Delivery area and dynamic shipping fee
    val selectedDeliveryArea: String = "Damascus",
    val shippingFeeSyp: Double = 20000.0,
    val deliveryAreas: List<String> = listOf("Damascus", "Aleppo", "Homs", "Hama", "Latakia", "Tartous"),
    
    // Verification Screen State
    val showVerification: Boolean = false,
    val securityTimerSeconds: Int = 60,
    val verificationOtp: String = "",
    val currentTransaction: PaymentTransaction? = null,
    val simulatedSmsBody: String = "",
    val showSimulatedSms: Boolean = false,
    val verificationError: String? = null,
    
    // Final success state
    val orderPlacedSuccess: Boolean = false,
    val progressMessage: String = ""
) {
    val subtotal: Double
        get() = cartItems.sumOf { it.price * it.quantity }

    val vatAmount: Double
        get() = subtotal * 0.03

    val shippingFee: Double
        get() = if (cartItems.isEmpty()) 0.0 else shippingFeeSyp / 13500.0

    val grandTotal: Double
        get() = if (cartItems.isEmpty()) 0.0 else subtotal + vatAmount + shippingFee
}

class CheckoutViewModel(
    private val createPaymentTransactionUseCase: CreatePaymentTransactionUseCase = CreatePaymentTransactionUseCase(ServiceLocator.paymentRepository),
    private val verifyPaymentOtpUseCase: VerifyPaymentOtpUseCase = VerifyPaymentOtpUseCase(ServiceLocator.paymentRepository, ServiceLocator.orderRepository)
) : ViewModel() {

    private val _state = MutableStateFlow(CheckoutUiState())
    val state: StateFlow<CheckoutUiState> = _state.asStateFlow()

    private var countdownJob: Job? = null

    init {
        loadUserAndCart()
    }

    private fun loadUserAndCart() {
        _state.update { it.copy(isLoading = true) }
        viewModelScope.launch {
            try {
                val session = ServiceLocator.authRepository.getCurrentUserSession()
                if (session != null) {
                    val uid = session.id
                    _state.update {
                        it.copy(
                            customerName = session.name,
                            customerPhone = session.phoneNumber ?: ""
                        )
                    }
                    // Fetch cart items
                    ServiceLocator.cartRepository.getCartItems(uid).collect { items ->
                        _state.update { it.copy(cartItems = items, isLoading = false) }
                    }
                } else {
                    _state.update { it.copy(isLoading = false, error = "User not logged in") }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun onNameChange(name: String) = _state.update { it.copy(customerName = name) }
    fun onPhoneChange(phone: String) = _state.update { it.copy(customerPhone = phone) }
    fun onAddressChange(address: String) = _state.update { it.copy(shippingAddress = address) }
    
    fun setMapVisible(visible: Boolean) = _state.update { it.copy(mapVisible = visible) }

    fun selectLocationCoordinates(lat: Double, lng: Double, country: String, city: String, district: String, street: String, fullAddress: String) {
        _state.update {
            val updatedState = it.copy(
                latitude = lat,
                longitude = lng,
                locationCountry = country,
                locationCity = city,
                locationDistrict = district,
                locationStreet = street
            )
            updatedState.copy(
                shippingAddress = compileAddress(updatedState),
                selectedDeliveryArea = city
            )
        }
        
        onDeliveryAreaChange(city)
    }

    fun updateDetailsFields(
        building: String,
        apt: String,
        floor: String,
        landmark: String,
        notes: String
    ) {
        _state.update {
            val updatedState = it.copy(
                buildingNumber = building,
                apartment = apt,
                floor = floor,
                landmark = landmark,
                additionalNotes = notes
            )
            updatedState.copy(
                shippingAddress = compileAddress(updatedState)
            )
        }
    }

    private fun compileAddress(s: CheckoutUiState): String {
        val parts = mutableListOf<String>()
        if (s.buildingNumber.isNotBlank()) parts.add(s.buildingNumber)
        if (s.floor.isNotBlank()) parts.add("Flr ${s.floor}")
        if (s.apartment.isNotBlank()) parts.add("Apt ${s.apartment}")
        if (s.landmark.isNotBlank()) parts.add("Near ${s.landmark}")
        if (s.locationStreet.isNotBlank()) parts.add(s.locationStreet)
        if (s.locationDistrict.isNotBlank()) parts.add(s.locationDistrict)
        if (s.locationCity.isNotBlank()) parts.add(s.locationCity)
        if (s.locationCountry.isNotBlank()) parts.add(s.locationCountry)
        
        var base = parts.joinToString(", ")
        if (s.additionalNotes.isNotBlank()) {
            base += " (Notes: ${s.additionalNotes})"
        }
        return base
    }

    fun reverseGeocodeSimulated(lat: Double, lng: Double): Triple<String, String, String> {
        val cities = listOf(
            Triple(33.5138, 36.2947, "Damascus"),
            Triple(36.2021, 37.1343, "Aleppo"),
            Triple(34.7324, 36.7137, "Homs"),
            Triple(35.1318, 36.7578, "Hama"),
            Triple(35.5312, 35.7921, "Latakia"),
            Triple(34.8890, 35.8864, "Tartous")
        )
        
        var closestCity = "Damascus"
        var minDist = Double.MAX_VALUE
        for (c in cities) {
            val dist = Math.hypot(lat - c.first, lng - c.second)
            if (dist < minDist) {
                minDist = dist
                closestCity = c.third
            }
        }
        
        val district = when (closestCity) {
            "Damascus" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.01) "Bab Touma"
                else if (offset < 0.02) "Mezzeh"
                else if (offset < 0.03) "Malki"
                else if (offset < 0.04) "Shaalan"
                else "Kafar Souseh"
            }
            "Aleppo" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.015) "Al-Jamilia"
                else if (offset < 0.03) "Mogambo"
                else "Shahba"
            }
            "Homs" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.025) "Al-Inshaat"
                else "Ghouta"
            }
            "Hama" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.02) "Al-Sharia"
                else "Al-Hader"
            }
            "Latakia" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.02) "Al-Ziraa"
                else "Sheikh Dher"
            }
            "Tartous" -> {
                val offset = Math.abs(lat % 0.05)
                if (offset < 0.02) "Al-Karameh"
                else "Corniche"
            }
            else -> "Center"
        }
        
        return Triple("Syria", closestCity, district)
    }

    fun onDeliveryAreaChange(area: String) {
        val sypFee = when (area) {
            "Damascus" -> 20000.0
            "Aleppo" -> 20000.0
            "Homs" -> 20000.0
            "Hama" -> 20000.0
            "Latakia" -> 20000.0
            "Tartous" -> 20000.0
            else -> 20000.0
        }
        _state.update {
            it.copy(
                selectedDeliveryArea = area,
                shippingFeeSyp = sypFee
            )
        }
    }
    fun onPaymentMethodChange(method: String) = _state.update { it.copy(paymentMethod = method) }
    fun onOtpChange(otp: String) = _state.update { it.copy(verificationOtp = otp, verificationError = null) }

    fun dismissSms() = _state.update { it.copy(showSimulatedSms = false) }

    fun processCheckout() {
        val currentState = _state.value
        if (currentState.customerName.isBlank()) {
            _state.update { it.copy(error = "Please enter your name") }
            return
        }
        if (currentState.customerPhone.isBlank()) {
            _state.update { it.copy(error = "Please enter your mobile phone number") }
            return
        }
        if (currentState.cartItems.isEmpty()) {
            _state.update { it.copy(error = "Your cart is empty") }
            return
        }

        if (currentState.paymentMethod == "Cash On Delivery") {
            placeCodOrder()
        } else {
            initiateEWalletPayment()
        }
    }

    private fun placeCodOrder() {
        _state.update { it.copy(isLoading = true, progressMessage = "Placing COD order...") }
        viewModelScope.launch {
            try {
                val session = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
                val uid = session.id
                val itemsByStore = _state.value.cartItems.groupBy { it.storeName }
                
                itemsByStore.forEach { (storeName, items) ->
                    val orderId = "WS-" + UUID.randomUUID().toString().take(8).uppercase()
                    var targetStoreId = "sample_store_id"
                    try {
                        val firstProduct = ServiceLocator.productRepository.getProductDetails(items.first().id)
                        if (firstProduct != null) {
                            targetStoreId = firstProduct.storeId
                        }
                    } catch (e: Exception) {}

                    val orderItems = items.map { cartItem ->
                        OrderItem(
                            productId = cartItem.id,
                            productName = cartItem.name,
                            productImage = cartItem.image,
                            quantity = cartItem.quantity,
                            unitPrice = cartItem.price
                        )
                    }
                    val storeSubtotal = items.sumOf { it.price * it.quantity }
                    val storeVatAmount = storeSubtotal * 0.03
                    val storeShippingFee = if (items.isEmpty()) 0.0 else _state.value.shippingFeeSyp / 13500.0
                    val storeGrandTotal = storeSubtotal + storeVatAmount + storeShippingFee

                    val order = Order(
                        orderId = orderId,
                        userId = uid,
                        storeId = targetStoreId,
                        storeName = storeName,
                        status = "Pending",
                        createdAt = System.currentTimeMillis(),
                        totalAmount = storeGrandTotal,
                        currency = "USD",
                        items = orderItems,
                        customerName = _state.value.customerName,
                        customerPhone = _state.value.customerPhone,
                        shippingAddress = _state.value.shippingAddress,
                        paymentMethod = "Cash On Delivery",
                        paymentStatus = "Pending",
                        subtotal = storeSubtotal,
                        vatAmount = storeVatAmount,
                        shippingFee = storeShippingFee,
                        grandTotal = storeGrandTotal,
                        selectedDeliveryArea = _state.value.selectedDeliveryArea
                    )

                    ServiceLocator.orderRepository.createOrder(order)
                    
                    // Clear cart
                    items.forEach { cartItem ->
                        ServiceLocator.cartRepository.removeFromCart(uid, cartItem.id)
                    }
                }

                _state.update { it.copy(isLoading = false, orderPlacedSuccess = true) }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    private fun initiateEWalletPayment() {
        val originalMethod = _state.value.paymentMethod
        val methodLabel = if (originalMethod.contains("Syriatel", ignoreCase = true)) "Syriatel Cash" else "MTN Cash"
        
        _state.update { it.copy(isLoading = true, progressMessage = "Opening secure link with $methodLabel... / جاري فتح الاتصال الآمن مع $methodLabel...") }
        viewModelScope.launch {
            try {
                // Progressive step 1: Port-handshake
                delay(1000)
                val totalAmount = _state.value.grandTotal
                _state.update { it.copy(progressMessage = "Authenticating Wallet Session +${_state.value.customerPhone}... / جاري التحقق من حساب المحفظة +${_state.value.customerPhone}...") }
                
                // Progressive step 2: Generate token
                delay(1200)
                _state.update { it.copy(progressMessage = "Requesting secure 6-digit OTP code... / جاري طلب رمز التحقق OTP الآمن لـ $methodLabel...") }
                
                delay(1000)
                val session = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
                val uid = session.id
                
                val transactionId = "TXN-" + UUID.randomUUID().toString().take(8).uppercase()
                val orderIdSeed = "WS-" + UUID.randomUUID().toString().take(8).uppercase()
                
                val randomOtp = (100000..999999).random().toString()
                val now = System.currentTimeMillis()
                val expires = now + 60000 // expires in 60s

                val txn = PaymentTransaction(
                    transactionId = transactionId,
                    userId = uid,
                    orderId = orderIdSeed,
                    paymentMethod = _state.value.paymentMethod,
                    phoneNumber = _state.value.customerPhone,
                    otpCode = randomOtp,
                    amount = totalAmount,
                    currency = "USD",
                    status = "Pending",
                    createdAt = now,
                    expiresAt = expires
                )

                createPaymentTransactionUseCase(txn).onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentTransaction = txn,
                            showVerification = true,
                            securityTimerSeconds = 60,
                            verificationOtp = "",
                            simulatedSmsBody = randomOtp,
                            showSimulatedSms = true,
                            verificationError = null
                        )
                    }
                    startCountdown()
                }.onFailure { err ->
                    _state.update { it.copy(isLoading = false, error = err.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun startCountdown() {
        countdownJob?.cancel()
        _state.update { it.copy(securityTimerSeconds = 60) }
        countdownJob = viewModelScope.launch {
            while (_state.value.securityTimerSeconds > 0) {
                delay(1000)
                _state.update {
                    it.copy(securityTimerSeconds = it.securityTimerSeconds - 1)
                }
            }
        }
    }

    fun resendOtp() {
        _state.update { it.copy(isLoading = true, progressMessage = "Resending OTP verification code...") }
        viewModelScope.launch {
            try {
                val currentTxn = _state.value.currentTransaction ?: return@launch
                val randomOtp = (100000..999999).random().toString()
                val now = System.currentTimeMillis()
                val expires = now + 60000

                val newTxn = currentTxn.copy(
                    otpCode = randomOtp,
                    createdAt = now,
                    expiresAt = expires,
                    status = "Pending"
                )

                createPaymentTransactionUseCase(newTxn).onSuccess {
                    _state.update {
                        it.copy(
                            isLoading = false,
                            currentTransaction = newTxn,
                            verificationOtp = "",
                            simulatedSmsBody = randomOtp,
                            showSimulatedSms = true,
                            verificationError = null
                        )
                    }
                    startCountdown()
                }.onFailure { err ->
                    _state.update { it.copy(isLoading = false, verificationError = err.message) }
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, verificationError = e.message) }
            }
        }
    }

    fun verifyOtp() {
        val currentState = _state.value
        val currentTxn = currentState.currentTransaction ?: return
        
        if (currentState.verificationOtp.length < 6) {
            _state.update { it.copy(verificationError = "Please enter the 6-digit verification code") }
            return
        }

        _state.update { it.copy(isLoading = true, progressMessage = "Verifying security handshake... / جاري التحقق من الهوية الرقمية والرمز...") }
        viewModelScope.launch {
            // Processing step 1: Handshake validation
            delay(1200)
            _state.update { it.copy(progressMessage = "Connecting secure ledger API... / جاري الاتصال بدفتر الحسابات المؤمن للشبكة...") }
            
            // Processing step 2: Transaction submission
            delay(1200)
            _state.update { it.copy(progressMessage = "Authorizing checkout settlement... / جاري تفويض وتطهير تسوية المعاملة المالية...") }
            
            delay(800)
            val result = verifyPaymentOtpUseCase(currentTxn.transactionId, currentState.verificationOtp)
            
            when (result) {
                is OtpVerificationResult.Success -> {
                    saveVerifiedOrders(currentTxn)
                }
                is OtpVerificationResult.Expired -> {
                    _state.update { it.copy(isLoading = false, verificationError = "Verification code has expired. Please resend code. / انتهت صلاحية كود التحقق.") }
                }
                is OtpVerificationResult.Incorrect -> {
                    _state.update { it.copy(isLoading = false, verificationError = "Incorrect verification code. Please try again. / رمز التحقق غير صحيح، يرجى المحاولة مجدداً.") }
                }
                is OtpVerificationResult.Error -> {
                    _state.update { it.copy(isLoading = false, verificationError = result.message) }
                }
            }
        }
    }

    private fun saveVerifiedOrders(txn: PaymentTransaction) {
        _state.update { it.copy(progressMessage = "Writing order data to local safe storage... / جاري تخزين بيانات الطلبات محلياً ومزامنتها...") }
        viewModelScope.launch {
            try {
                delay(1200)
                _state.update { it.copy(progressMessage = "Dispatching notifications to sellers... / جاري إخطار المتاجر وتجهيز مستندات الشحنة الآمنة...") }
                delay(1000)
                
                val session = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
                val uid = session.id
                val itemsByStore = _state.value.cartItems.groupBy { it.storeName }

                itemsByStore.forEach { (storeName, items) ->
                    val orderId = "WS-" + UUID.randomUUID().toString().take(8).uppercase()
                    var targetStoreId = "sample_store_id"
                    try {
                        val firstProduct = ServiceLocator.productRepository.getProductDetails(items.first().id)
                        if (firstProduct != null) {
                            targetStoreId = firstProduct.storeId
                        }
                    } catch (e: Exception) {}

                    val orderItems = items.map { cartItem ->
                        OrderItem(
                            productId = cartItem.id,
                            productName = cartItem.name,
                            productImage = cartItem.image,
                            quantity = cartItem.quantity,
                            unitPrice = cartItem.price
                        )
                    }
                    val storeSubtotal = items.sumOf { it.price * it.quantity }
                    val storeVatAmount = storeSubtotal * 0.03
                    val storeShippingFee = if (items.isEmpty()) 0.0 else _state.value.shippingFeeSyp / 13500.0
                    val storeGrandTotal = storeSubtotal + storeVatAmount + storeShippingFee

                    val order = Order(
                        orderId = orderId,
                        userId = uid,
                        storeId = targetStoreId,
                        storeName = storeName,
                        status = "Pending",
                        createdAt = System.currentTimeMillis(),
                        totalAmount = storeGrandTotal,
                        currency = "USD",
                        items = orderItems,
                        customerName = _state.value.customerName,
                        customerPhone = _state.value.customerPhone,
                        shippingAddress = _state.value.shippingAddress,
                        paymentMethod = txn.paymentMethod,
                        paymentStatus = "Paid",
                        subtotal = storeSubtotal,
                        vatAmount = storeVatAmount,
                        shippingFee = storeShippingFee,
                        grandTotal = storeGrandTotal,
                        selectedDeliveryArea = _state.value.selectedDeliveryArea,
                        latitude = _state.value.latitude,
                        longitude = _state.value.longitude,
                        city = _state.value.locationCity,
                        district = _state.value.locationDistrict
                    )

                    ServiceLocator.orderRepository.createOrder(order)

                    // Clear cart
                    items.forEach { cartItem ->
                        ServiceLocator.cartRepository.removeFromCart(uid, cartItem.id)
                    }
                }

                _state.update {
                    it.copy(
                        isLoading = false,
                        orderPlacedSuccess = true,
                        showVerification = false
                    )
                }
            } catch (e: Exception) {
                _state.update { it.copy(isLoading = false, error = e.message) }
            }
        }
    }

    fun resetError() = _state.update { it.copy(error = null) }
    fun resetVerificationError() = _state.update { it.copy(verificationError = null) }

    override fun onCleared() {
        countdownJob?.cancel()
        super.onCleared()
    }
}
