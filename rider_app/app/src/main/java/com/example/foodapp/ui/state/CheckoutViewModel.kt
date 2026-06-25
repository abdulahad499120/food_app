package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.repository.OrderRepository
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.repository.AddressRepository

class CheckoutViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    private val addressRepository = AddressRepository()
    private val rewardsRepository = com.example.foodapp.data.repository.RewardsRepository()
    
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

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
    }

    fun updateAddress(address: Address) {
        _uiState.update { state ->
            state.copy(
                address = address,
                errorMessage = null
            )
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun placeOrder(authState: AuthState, context: android.content.Context,
                   fulfillmentMode: com.example.foodapp.ui.state.FulfillmentMode = com.example.foodapp.ui.state.FulfillmentMode.DELIVERY,
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
        // If guest, userId is empty string or "GUEST"
        val userId = user?.uid ?: "GUEST"
        val userName = user?.name ?: "Guest User"

        viewModelScope.launch {
            _uiState.update { it.copy(status = CheckoutStatus.Loading, errorMessage = null) }
            
            val cartState = CartManager.cartState.value
            val order = Order(
                userId = userId,
                customerName = userName,
                deliveryAddress = if (fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY) _uiState.value.address else com.example.foodapp.data.models.Address(),
                items = cartState.items,
                subtotal = cartState.subtotal,
                deliveryFee = cartState.deliveryFee,
                totalAmount = cartState.total,
                fulfillmentMode = fulfillmentMode.name,
                orderStatus = com.example.foodapp.data.models.OrderStatus.PENDING,
                branchLocation = branchGeoPoint,
                deliveryLocation = deliveryGeoPoint ?: _uiState.value.address.location
            )
            
            orderRepository.placeOrder(order).fold(
                onSuccess = { orderId ->
                    if (cartState.payWithStars && user != null) {
                        rewardsRepository.updateUserStars(user.uid, -150)
                    }
                    if (user == null) {
                        // Save guest active order id
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

    fun resetState() {
        _uiState.value = CheckoutUiState()
    }
}
