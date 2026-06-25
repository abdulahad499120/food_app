package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Product
import com.example.foodapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class PreviousOrdersViewModel : ViewModel() {
    private val orderRepository = OrderRepository()

    private val _previouslyPurchasedProducts = MutableStateFlow<List<Product>>(emptyList())
    val previouslyPurchasedProducts: StateFlow<List<Product>> = _previouslyPurchasedProducts.asStateFlow()

    private var currentUserId: String? = null

    fun loadPreviousOrdersForUser(userId: String?) {
        if (userId == currentUserId) return
        currentUserId = userId
        
        if (userId == null) {
            _previouslyPurchasedProducts.value = emptyList()
            return
        }

        viewModelScope.launch {
            try {
                orderRepository.getUserOrders(userId).collectLatest { orders ->
                    // Extract unique products from all past orders
                    val allProducts = orders.flatMap { order -> 
                        order.items.map { it.product } 
                    }
                    // Distinct by product ID
                    val uniqueProducts = allProducts.distinctBy { it.id }
                    _previouslyPurchasedProducts.value = uniqueProducts
                }
            } catch (e: Exception) {
                _previouslyPurchasedProducts.value = emptyList()
            }
        }
    }
}
