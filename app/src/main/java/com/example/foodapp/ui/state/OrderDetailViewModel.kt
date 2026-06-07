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

sealed class OrderDetailUiState {
    object Loading : OrderDetailUiState()
    data class Success(val order: Order) : OrderDetailUiState()
    data class Error(val message: String) : OrderDetailUiState()
}

class OrderDetailViewModel(
    private val repository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<OrderDetailUiState>(OrderDetailUiState.Loading)
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val _submitState = MutableStateFlow<ReviewSubmitState>(ReviewSubmitState.Idle)
    val submitState: StateFlow<ReviewSubmitState> = _submitState.asStateFlow()

    sealed class ReviewSubmitState {
        object Idle : ReviewSubmitState()
        object Submitting : ReviewSubmitState()
        object Success : ReviewSubmitState()
        data class Error(val message: String) : ReviewSubmitState()
    }

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            _uiState.update { OrderDetailUiState.Loading }
            repository.getOrder(orderId).fold(
                onSuccess = { order ->
                    if (order != null) {
                        _uiState.update { OrderDetailUiState.Success(order) }
                    } else {
                        _uiState.update { OrderDetailUiState.Error("Order not found") }
                    }
                },
                onFailure = { e ->
                    _uiState.update { OrderDetailUiState.Error(e.message ?: "Failed to load order") }
                }
            )
        }
    }

    fun submitReview(orderId: String, rating: Int, reviewText: String) {
        if (rating < 1 || rating > 5) return
        viewModelScope.launch {
            _submitState.value = ReviewSubmitState.Submitting
            repository.submitReview(orderId, rating, reviewText).fold(
                onSuccess = {
                    // Update local state to reflect the new rating
                    val currentState = _uiState.value
                    if (currentState is OrderDetailUiState.Success) {
                        val updatedOrder = currentState.order.copy(
                            rating = rating,
                            reviewText = reviewText
                        )
                        _uiState.update { OrderDetailUiState.Success(updatedOrder) }
                    }
                    _submitState.value = ReviewSubmitState.Success
                },
                onFailure = { e ->
                    _submitState.value = ReviewSubmitState.Error(e.message ?: "Failed to submit review")
                }
            )
        }
    }
    
    fun resetSubmitState() {
        _submitState.value = ReviewSubmitState.Idle
    }
}
