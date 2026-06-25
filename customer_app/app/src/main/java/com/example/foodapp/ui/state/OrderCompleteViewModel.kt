package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class OrderCompleteViewModel : ViewModel() {
    private val orderRepository = OrderRepository()
    
    private val _submitState = MutableStateFlow<Boolean?>(null)
    val submitState: StateFlow<Boolean?> = _submitState.asStateFlow()
    
    fun submitReview(orderId: String, rating: Int, reviewText: String) {
        viewModelScope.launch {
            val result = orderRepository.submitReview(orderId, rating, reviewText)
            _submitState.value = result.isSuccess
        }
    }
}
