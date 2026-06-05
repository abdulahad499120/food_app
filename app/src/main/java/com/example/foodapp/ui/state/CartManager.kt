package com.example.foodapp.ui.state

import com.example.foodapp.data.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.example.foodapp.data.models.CartItem

data class CartState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 50.0,
    val total: Double = 0.0
)

object CartManager {
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun addItem(product: Product, quantity: Int = 1) {
        _cartState.update { state ->
            val existingItem = state.items.find { it.product.id == product.id }
            val newItems = if (existingItem != null) {
                state.items.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + quantity) else it
                }
            } else {
                state.items + CartItem(product, quantity)
            }
            recalculate(state.copy(items = newItems))
        }
    }

    fun removeItem(productId: Long) {
        _cartState.update { state ->
            val newItems = state.items.filter { it.product.id != productId }
            recalculate(state.copy(items = newItems))
        }
    }

    fun updateQuantity(productId: Long, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(productId)
            return
        }
        _cartState.update { state ->
            val newItems = state.items.map {
                if (it.product.id == productId) it.copy(quantity = newQuantity) else it
            }
            recalculate(state.copy(items = newItems))
        }
    }

    private fun recalculate(state: CartState): CartState {
        val subtotal = state.items.sumOf { it.product.price * it.quantity }
        val total = if (state.items.isEmpty()) 0.0 else subtotal + state.deliveryFee
        return state.copy(
            subtotal = subtotal,
            total = total
        )
    }

    fun clearCart() {
        _cartState.value = CartState()
    }
}
