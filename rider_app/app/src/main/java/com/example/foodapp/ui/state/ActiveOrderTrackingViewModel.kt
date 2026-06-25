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

sealed class TrackingUiState {
    object Loading : TrackingUiState()
    data class Active(val order: Order) : TrackingUiState()
    data class Error(val message: String) : TrackingUiState()
}

class ActiveOrderTrackingViewModel(
    private val repository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<TrackingUiState>(TrackingUiState.Loading)
    val uiState: StateFlow<TrackingUiState> = _uiState.asStateFlow()

    fun observeOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { TrackingUiState.Loading }
            try {
                repository.observeActiveOrder(orderId).collect { order ->
                    if (order != null) {
                        _uiState.update { TrackingUiState.Active(order) }
                    } else {
                        _uiState.update { TrackingUiState.Error("Order not found or access denied.") }
                    }
                }
            } catch (e: Exception) {
                _uiState.update { TrackingUiState.Error(e.message ?: "An error occurred while tracking the order.") }
            }
        }
    }
}
