package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActiveDeliveryViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.observeActiveOrder(orderId)
                .catch { /* handle error silently or update state */ }
                .collect { orderData ->
                _order.update { orderData }
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("orders").document(orderId).update("orderStatus", status.name).await()
                onSuccess()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
