package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class OrderHistoryUiState {
    object Loading : OrderHistoryUiState()
    object Empty : OrderHistoryUiState()
    data class Success(val orders: List<Order>) : OrderHistoryUiState()
    data class Error(val message: String) : OrderHistoryUiState()
}

class OrderHistoryViewModel(
    private val repository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderHistoryUiState>(OrderHistoryUiState.Loading)
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    fun loadOrders(userId: String) {
        viewModelScope.launch {
            _uiState.update { OrderHistoryUiState.Loading }
            try {
                repository.getUserOrders(userId).collect { orders ->
                    if (orders.isEmpty()) {
                        _uiState.update { OrderHistoryUiState.Empty }
                    } else {
                        _uiState.update { OrderHistoryUiState.Success(orders) }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { OrderHistoryUiState.Error(e.message ?: "Failed to load orders") }
            }
        }
    }

    fun reorder(order: Order, onComplete: () -> Unit) {
        // Append items to CartManager
        order.items.forEach { cartItem ->
            com.example.foodapp.ui.state.CartManager.addItem(cartItem.product, cartItem.quantity)
        }
        onComplete()
    }
}
