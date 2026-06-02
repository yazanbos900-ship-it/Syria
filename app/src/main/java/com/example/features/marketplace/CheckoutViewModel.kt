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
    val shippingAddress: String = "Bab Touma, Block 4, Damascus, Syria",
    val paymentMethod: String = "Syriatel Cash", // "Syriatel Cash", "MTN Cash", "Cash On Delivery"
    val error: String? = null,
    
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
)

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
                    val totalAmount = items.sumOf { it.price * it.quantity }

                    val order = Order(
                        orderId = orderId,
                        userId = uid,
                        storeId = targetStoreId,
                        storeName = storeName,
                        status = "Pending",
                        createdAt = System.currentTimeMillis(),
                        totalAmount = totalAmount,
                        currency = "USD",
                        items = orderItems,
                        customerName = _state.value.customerName,
                        customerPhone = _state.value.customerPhone,
                        shippingAddress = _state.value.shippingAddress,
                        paymentMethod = "Cash On Delivery",
                        paymentStatus = "Pending"
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
        _state.update { it.copy(isLoading = true, progressMessage = "Generating payment request...") }
        viewModelScope.launch {
            try {
                val session = ServiceLocator.authRepository.getCurrentUserSession() ?: return@launch
                val uid = session.id
                
                val totalAmount = _state.value.cartItems.sumOf { it.price * it.quantity }
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

        _state.update { it.copy(isLoading = true, progressMessage = "Verifying transaction...") }
        viewModelScope.launch {
            val result = verifyPaymentOtpUseCase(currentTxn.transactionId, currentState.verificationOtp)
            
            _state.update { it.copy(isLoading = false) }
            
            when (result) {
                is OtpVerificationResult.Success -> {
                    saveVerifiedOrders(currentTxn)
                }
                is OtpVerificationResult.Expired -> {
                    _state.update { it.copy(verificationError = "Verification code has expired. Please resend code.") }
                }
                is OtpVerificationResult.Incorrect -> {
                    _state.update { it.copy(verificationError = "Incorrect verification code. Please try again.") }
                }
                is OtpVerificationResult.Error -> {
                    _state.update { it.copy(verificationError = result.message) }
                }
            }
        }
    }

    private fun saveVerifiedOrders(txn: PaymentTransaction) {
        _state.update { it.copy(isLoading = true, progressMessage = "Processing secure checkout payment and placing orders...") }
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
                    val totalAmount = items.sumOf { it.price * it.quantity }

                    val order = Order(
                        orderId = orderId,
                        userId = uid,
                        storeId = targetStoreId,
                        storeName = storeName,
                        status = "Pending",
                        createdAt = System.currentTimeMillis(),
                        totalAmount = totalAmount,
                        currency = "USD",
                        items = orderItems,
                        customerName = _state.value.customerName,
                        customerPhone = _state.value.customerPhone,
                        shippingAddress = _state.value.shippingAddress,
                        paymentMethod = txn.paymentMethod,
                        paymentStatus = "Paid"
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
