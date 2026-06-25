package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.repository.OrderRepository
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class OrderHistoryState(
    val pastOrders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val errorMessage: String? = null
) {
    val totalEarnings: Double
        get() = pastOrders.sumOf { it.deliveryFee ?: 0.0 }
}

class OrderHistoryViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryState())
    val uiState: StateFlow<OrderHistoryState> = _uiState.asStateFlow()

    init {
        loadHistory()
    }

    fun loadHistory() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user == null) {
            _uiState.update { it.copy(isLoading = false, errorMessage = "Not authenticated") }
            return
        }

        viewModelScope.launch {
            try {
                orderRepository.observeRiderOrderHistory(user.uid).collect { orders ->
                    _uiState.update {
                        it.copy(
                            pastOrders = orders,
                            isLoading = false,
                            errorMessage = null
                        )
                    }
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = "Failed to load history: ${e.message}"
                    )
                }
            }
        }
    }
}
