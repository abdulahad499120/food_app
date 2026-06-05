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

class CheckoutViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    
    private val _uiState = MutableStateFlow(CheckoutUiState())
    val uiState: StateFlow<CheckoutUiState> = _uiState.asStateFlow()

    fun updateAddress(houseNo: String, street: String, area: String) {
        _uiState.update { state ->
            state.copy(
                address = Address(houseNo, street, area),
                errorMessage = null
            )
        }
    }

    fun setPaymentMethod(method: String) {
        _uiState.update { it.copy(paymentMethod = method) }
    }

    fun placeOrder(authState: AuthState) {
        if (!_uiState.value.address.isComplete) {
            _uiState.update { it.copy(errorMessage = "Please complete your delivery address") }
            return
        }
        
        val user = (authState as? AuthState.Authenticated)?.user
        if (user == null) {
            _uiState.update { it.copy(errorMessage = "You must be logged in to place an order") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(status = CheckoutStatus.Loading, errorMessage = null) }
            
            val cartState = CartManager.cartState.value
            val order = Order(
                userId = user.uid,
                customerName = user.name,
                deliveryAddress = _uiState.value.address,
                items = cartState.items,
                subtotal = cartState.subtotal,
                deliveryFee = cartState.deliveryFee,
                totalAmount = cartState.total,
                status = "Pending"
            )
            
            orderRepository.placeOrder(order).fold(
                onSuccess = {
                    CartManager.clearCart()
                    _uiState.update { state -> state.copy(status = CheckoutStatus.Success) }
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
