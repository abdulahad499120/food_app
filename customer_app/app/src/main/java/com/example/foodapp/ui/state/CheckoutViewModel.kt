package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.repository.OrderRepository
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.repository.AddressRepository
import com.example.foodapp.data.network.RetrofitClient
import com.example.foodapp.data.network.TrackerRequest
import com.example.foodapp.utils.SafepayCallbackManager
import com.example.foodapp.utils.SafepayResult

class CheckoutViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    private val addressRepository = AddressRepository()
    private val rewardsRepository = com.example.foodapp.data.repository.RewardsRepository()
    private val paymentRepository = com.example.foodapp.data.repository.PaymentRepository()
    private val safepayRepository = com.example.foodapp.data.remote.SafepayRepository()

    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    // Used to signal the UI to launch the Chrome Custom Tab
    private val _safepayCheckoutUrl = MutableStateFlow<String?>(null)
    val safepayCheckoutUrl: StateFlow<String?> = _safepayCheckoutUrl.asStateFlow()

    // Held across the DDC WebView step for vaulted card checkout
    private var _pendingTracker: String? = null
    private var _pendingOrder: com.example.foodapp.data.models.Order? = null
    private var _pendingPayWithStars: Boolean = false
    private var _pendingIsGuest: Boolean = false
    private var _pendingUserId: String? = null

    fun initialize(userId: String?) {
        if (userId == null) return
        viewModelScope.launch {
            try {
                addressRepository.getUserAddresses(userId).collect { addresses ->
                    val defaultAddress = addresses.find { it.isDefault } ?: addresses.firstOrNull()
                    if (defaultAddress != null) {
                        _uiState.update { it.copy(address = defaultAddress, errorMessage = null) }
                    }
                }
            } catch (e: Exception) {
                // Ignore to avoid crash
            }
        }
        
        viewModelScope.launch {
            try {
                val customerId = paymentRepository.getSafepayCustomerId(userId)
                if (customerId != null) {
                    val vault = safepayRepository.getCustomerVault(customerId)
                    if (vault != null) {
                        _uiState.update { it.copy(savedCards = vault.wallet) }
                    }
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateAddress(address: Address) {
        _uiState.update { state ->
            state.copy(
                address = address,
                errorMessage = null
            )
        }
    }

    fun setPaymentMethod(id: String, name: String) {
        _uiState.update { it.copy(paymentMethodId = id, paymentMethodName = name) }
    }

    fun updateCardNumber(number: String) {
        _uiState.update { it.copy(cardNumber = number) }
    }

    fun updateCardExpMonth(month: String) {
        if (month.length <= 2) _uiState.update { it.copy(cardExpMonth = month) }
    }

    fun updateCardExpYear(year: String) {
        if (year.length <= 4) _uiState.update { it.copy(cardExpYear = year) }
    }

    fun updateCardCvv(cvv: String) {
        if (cvv.length <= 4) _uiState.update { it.copy(cardCvv = cvv) }
    }

    fun toggleSaveCardForLater(save: Boolean) {
        _uiState.update { it.copy(saveCardForLater = save) }
    }

    fun clearCheckoutUrl() {
        _safepayCheckoutUrl.value = null
        _uiState.update { it.copy(safepayHostedCheckoutUrl = null) }
    }

    fun placeOrder(authState: AuthState, context: android.content.Context,
                   fulfillmentMode: com.example.foodapp.ui.state.FulfillmentMode = com.example.foodapp.ui.state.FulfillmentMode.DELIVERY,
                   branchId: String? = null,
                   branchGeoPoint: com.google.firebase.firestore.GeoPoint? = null,
                   deliveryGeoPoint: com.google.firebase.firestore.GeoPoint? = null) {
        if (!com.example.foodapp.utils.NetworkUtils.isNetworkAvailable(context)) {
            _uiState.update { it.copy(errorMessage = "No internet connection. Please check your network and try again.") }
            return
        }

        if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY && !_uiState.value.address.isComplete) {
            _uiState.update { it.copy(errorMessage = "Please complete your delivery address") }
            return
        }
        
        val user = (authState as? AuthState.Authenticated)?.user
        val userId = user?.uid ?: "GUEST"
        val userName = user?.name ?: "Guest User"

        viewModelScope.launch {
            _uiState.update { it.copy(status = CheckoutStatus.Loading, errorMessage = null) }
            val cartState = CartManager.cartState.value

            // 1. Check if Safepay Card Payment is selected
            val paymentId = _uiState.value.paymentMethodId
            val isVaultedCard = _uiState.value.savedCards.any { it.token == paymentId }

            if (paymentId == "safepay_card") {
                try {
                    // Initialize Safepay Tracker
                    val trackerReq = TrackerRequest(
                        client = "sec_97d30ea0-2233-4c05-b344-f21c0c5cc5c3", // WARNING: Usually this should be public key but user provided sec_ key in prompt previously.
                        amount = cartState.total
                    )
                    val response = RetrofitClient.safepayApi.initTracker(trackerReq)
                    val trackerId = response.data.token

                    val fakeOrderId = java.util.UUID.randomUUID().toString().replace("-", "").take(10)

                    // Construct Hosted Checkout URL
                    val url = "https://sandbox.api.getsafepay.com/checkout/pay?environment=sandbox&tracker=\$trackerId&client=\${trackerReq.client}&source=custom&redirect_url=icelandapp://safepay/success&cancel_url=icelandapp://safepay/cancel&reference=\$fakeOrderId"
                    
                    _safepayCheckoutUrl.value = url

                    // Wait for Deep Link Callback
                    val result = SafepayCallbackManager.safepayResult.first()
                    
                    if (result is SafepayResult.Cancelled) {
                        _uiState.update { it.copy(status = CheckoutStatus.Idle, errorMessage = "Payment was cancelled.") }
                        _safepayCheckoutUrl.value = null
                        return@launch
                    }

                    // Result is Success, proceed with Order Creation
                } catch (e: Exception) {
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Failed to initialize Safepay session: ${e.message}") }
                    return@launch
                }
            } else if (paymentId == "safepay_checkout") {
                try {
                    val fakeOrderId = java.util.UUID.randomUUID().toString().replace("-", "").take(10)
                    
                    // We save the pending state so we can place the order when WebView returns success
                    _pendingOrder = Order(
                        userId = userId,
                        customerName = userName,
                        deliveryAddress = if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY) _uiState.value.address else com.example.foodapp.data.models.Address(),
                        items = cartState.items,
                        subtotal = cartState.subtotal,
                        deliveryFee = cartState.deliveryFee,
                        totalAmount = cartState.total,
                        orderStatus = com.example.foodapp.data.models.OrderStatus.PENDING,
                        branchId = branchId ?: "branch_pia",
                        branchLocation = branchGeoPoint,
                        deliveryLocation = deliveryGeoPoint ?: _uiState.value.address.location,
                        paymentMethodId = _uiState.value.paymentMethodId,
                        itemSummary = "${cartState.items.sumOf { it.quantity }} Items",
                        generalSector = if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY && _uiState.value.address.city.isNotBlank()) _uiState.value.address.city else "Local Sector",
                        fulfillmentMode = fulfillmentMode.name
                    )
                    _pendingPayWithStars = cartState.payWithStars
                    _pendingIsGuest = user == null
                    _pendingUserId = user?.uid

                    // Get or create Safepay Customer
                    var safepayCustomerId = paymentRepository.getSafepayCustomerId(userId)
                    if (safepayCustomerId == null) {
                        val email = user?.email ?: "guest@example.com"
                        val phone = user?.phoneNumber ?: "00000000000"
                        val (firstName, lastName) = userName.split(" ", limit = 2).let { 
                            Pair(it[0], if (it.size > 1) it[1] else it[0]) 
                        }
                        safepayCustomerId = safepayRepository.createCustomer(email, firstName, lastName, phone, "PK")
                        if (safepayCustomerId == null) {
                            _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Failed to create Safepay Customer") }
                            return@launch
                        }
                        if (user != null) {
                            paymentRepository.setSafepayCustomerId(userId, safepayCustomerId)
                        }
                    }

                    // Initialize Safepay Tracker for Vaulting AND Payment
                    val trackerId = safepayRepository.initializeTracker(
                        customerId = safepayCustomerId, 
                        mode = "instrument",
                        amount = cartState.total
                    )
                    
                    if (trackerId == null) {
                        _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Failed to initialize Safepay Tracker") }
                        return@launch
                    }
                    
                    // Construct Hosted Checkout URL with HTTPS redirect scheme (Safepay sometimes blocks custom schemes)
                    val url = "https://sandbox.api.getsafepay.com/checkout/pay?env=sandbox&beacon=$trackerId&source=mobile&order_id=$fakeOrderId&redirect_url=https://www.getsafepay.com/success&cancel_url=https://www.getsafepay.com/cancel"
                    
                    _uiState.update { it.copy(status = CheckoutStatus.Idle, safepayHostedCheckoutUrl = url) }
                    return@launch
                } catch (e: Exception) {
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Safepay Error: ${e.message}") }
                    return@launch
                }
            } else if (isVaultedCard) {
                val safepayCustomerId = paymentRepository.getSafepayCustomerId(userId)

                if (safepayCustomerId == null) {
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Customer ID not found for saved card") }
                    return@launch
                }

                val result = safepayRepository.startVaultedCardPayment(
                    customerId = safepayCustomerId,
                    instrumentToken = paymentId,
                    amount = cartState.total
                )

                if (result.isFailure) {
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Failed to charge vaulted card: ${result.exceptionOrNull()?.message}") }
                    return@launch
                }

                // If success, just fall through to place the order in Firestore!
            }

            // COD and hosted Safepay checkout fall through here
            val totalQty = cartState.items.sumOf { it.quantity }
            val computedSector = if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY && _uiState.value.address.city.isNotBlank()) {
                _uiState.value.address.city
            } else {
                "Local Sector"
            }

            val order = Order(
                userId = userId,
                customerName = userName,
                deliveryAddress = if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY) _uiState.value.address else com.example.foodapp.data.models.Address(),
                items = cartState.items,
                subtotal = cartState.subtotal,
                deliveryFee = cartState.deliveryFee,
                totalAmount = cartState.total,
                orderStatus = com.example.foodapp.data.models.OrderStatus.PENDING,
                branchId = branchId ?: "branch_pia",
                branchLocation = branchGeoPoint,
                deliveryLocation = deliveryGeoPoint ?: _uiState.value.address.location,
                paymentMethodId = _uiState.value.paymentMethodId,
                itemSummary = "$totalQty Items",
                generalSector = computedSector,
                fulfillmentMode = fulfillmentMode.name
            )

            orderRepository.placeOrder(order).fold(
                onSuccess = { orderId ->
                    if (cartState.payWithStars && user != null) {
                        rewardsRepository.updateUserStars(user.uid, -150)
                    }
                    if (user == null) {
                        val guestSessionRepo = com.example.foodapp.data.repository.GuestSessionRepository(context)
                        guestSessionRepo.setGuestActiveOrderId(orderId)
                    }
                    _uiState.update { state -> state.copy(status = CheckoutStatus.Success, placedOrderId = orderId) }
                },
                onFailure = { exception ->
                    _uiState.update { state -> state.copy(status = CheckoutStatus.Error, errorMessage = exception.message ?: "Failed to place order") }
                }
            )
        }
    }

    fun onVaultedCardDdcSuccess(sessionId: String, context: android.content.Context) {
        val tracker = _pendingTracker ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(pendingVaultedDdc = null, status = CheckoutStatus.Loading) }

            val safepayRepo = com.example.foodapp.data.remote.SafepayRepository()
            val result = safepayRepo.completeVaultedCardPayment(tracker, sessionId)

            when (result) {
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.RequiresChallenge -> {
                    _uiState.update { 
                        it.copy(
                            status = CheckoutStatus.Idle, 
                            pendingChallengeUrl = result.url
                        ) 
                    }
                    // We don't clear pending state here because we need it after challenge
                    return@launch
                }
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.Error -> {
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Payment failed: ${result.message}") }
                    clearPendingVaultedState()
                    return@launch
                }
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.Success -> {
                    placePendingOrder(context)
                }
            }
        }
    }

    fun onChallengeSuccess(context: android.content.Context) {
        val tracker = _pendingTracker ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(pendingChallengeUrl = null, status = CheckoutStatus.Loading) }
            val safepayRepo = com.example.foodapp.data.remote.SafepayRepository()
            
            // After challenge success, we submit final authorization
            val authorized = safepayRepo.submitAuthorization(tracker)
            if (authorized) {
                placePendingOrder(context)
            } else {
                _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = "Authorization failed.") }
                clearPendingVaultedState()
            }
        }
    }

    fun onChallengeFailure() {
        _uiState.update { it.copy(pendingChallengeUrl = null, status = CheckoutStatus.Error, errorMessage = "3DS Challenge failed or cancelled.") }
        clearPendingVaultedState()
    }

    private fun placePendingOrder(context: android.content.Context) {
        val order = _pendingOrder ?: return
        viewModelScope.launch {
            orderRepository.placeOrder(order).fold(
                onSuccess = { orderId ->
                    if (_pendingPayWithStars) {
                        _pendingUserId?.let { rewardsRepository.updateUserStars(it, -150) }
                    }
                    if (_pendingIsGuest) {
                        val guestSessionRepo = com.example.foodapp.data.repository.GuestSessionRepository(context)
                        guestSessionRepo.setGuestActiveOrderId(orderId)
                    }
                    clearPendingVaultedState()
                    _uiState.update { it.copy(status = CheckoutStatus.Success, placedOrderId = orderId) }
                },
                onFailure = { exception ->
                    clearPendingVaultedState()
                    _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = exception.message ?: "Failed to place order") }
                }
            )
        }
    }

    fun onVaultedCardDdcFailure(message: String) {
        clearPendingVaultedState()
        _uiState.update { it.copy(status = CheckoutStatus.Error, errorMessage = message, pendingVaultedDdc = null) }
    }

    private fun clearPendingVaultedState() {
        _pendingTracker = null
        _pendingOrder = null
        _pendingPayWithStars = false
        _pendingIsGuest = false
        _pendingUserId = null
    }

    fun onSafepayHostedSuccess(context: android.content.Context, tracker: String?, reference: String?) {
        _uiState.update { it.copy(safepayHostedCheckoutUrl = null, status = CheckoutStatus.Loading) }
        placePendingOrder(context)
    }
    
    fun onSafepayHostedCancel() {
        clearPendingVaultedState()
        _uiState.update { it.copy(safepayHostedCheckoutUrl = null, status = CheckoutStatus.Idle, errorMessage = "Payment was cancelled.") }
    }
    
    fun onSafepayHostedFailure(error: String) {
        clearPendingVaultedState()
        _uiState.update { it.copy(safepayHostedCheckoutUrl = null, status = CheckoutStatus.Error, errorMessage = error) }
    }

    fun resetState() {
        _uiState.value = CheckoutUiState()
        _safepayCheckoutUrl.value = null
        clearPendingVaultedState()
    }
}
