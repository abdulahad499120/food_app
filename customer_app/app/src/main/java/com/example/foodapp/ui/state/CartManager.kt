package com.example.foodapp.ui.state

import com.example.foodapp.data.models.Product
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

import com.example.foodapp.data.models.CartItem
import com.example.foodapp.ui.state.FulfillmentMode

data class CartState(
    val items: List<CartItem> = emptyList(),
    val subtotal: Double = 0.0,
    val deliveryFee: Double = 0.0,
    val serviceFee: Double = 0.0,
    val driverTip: Double = 0.0,
    val payWithStars: Boolean = false,
    val total: Double = 0.0,
    val fulfillmentMode: FulfillmentMode = FulfillmentMode.PICKUP
)

object CartManager {
    private val _cartState = MutableStateFlow(CartState())
    val cartState: StateFlow<CartState> = _cartState.asStateFlow()

    fun setFulfillmentMode(mode: FulfillmentMode) {
        _cartState.update { recalculate(it.copy(fulfillmentMode = mode)) }
    }

    fun setDriverTip(tip: Double) {
        _cartState.update { recalculate(it.copy(driverTip = tip)) }
    }

    fun togglePayWithStars(enabled: Boolean) {
        _cartState.update { recalculate(it.copy(payWithStars = enabled)) }
    }

    fun addItem(item: CartItem) {
        _cartState.update { state ->
            // Match exactly on product ID AND all customizations
            val existingItem = state.items.find { 
                it.product.id == item.product.id &&
                it.size == item.size &&
                it.sweetness == item.sweetness &&
                it.extraToppings == item.extraToppings &&
                it.nutType == item.nutType &&
                it.scoops == item.scoops
            }
            val newItems = if (existingItem != null) {
                state.items.map {
                    if (it === existingItem) it.copy(quantity = it.quantity + item.quantity) else it
                }
            } else {
                state.items + item
            }
            recalculate(state.copy(items = newItems))
        }
    }

    // For backwards compatibility where no customization is specified
    fun addItem(product: Product, quantity: Int = 1) {
        addItem(CartItem(product = product, quantity = quantity))
    }

    // Remove needs to use an exact index or instance now since product ID is no longer unique
    fun removeItem(item: CartItem) {
        _cartState.update { state ->
            val newItems = state.items.filter { it !== item }
            recalculate(state.copy(items = newItems))
        }
    }

    // Temporarily keep productId remove for backward compatibility, removing all instances of that product
    fun removeItem(productId: String) {
        _cartState.update { state ->
            val newItems = state.items.filter { it.product.id != productId }
            recalculate(state.copy(items = newItems))
        }
    }

    fun updateQuantity(item: CartItem, newQuantity: Int) {
        if (newQuantity <= 0) {
            removeItem(item)
            return
        }
        _cartState.update { state ->
            val newItems = state.items.map {
                if (it === item) it.copy(quantity = newQuantity) else it
            }
            recalculate(state.copy(items = newItems))
        }
    }

    // Temporarily keep productId update for backward compatibility
    fun updateQuantity(productId: String, newQuantity: Int) {
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
        val rawSubtotal = state.items.sumOf { item ->
            // Calculate item price including customizations
            val basePrice = item.product.price
            val sizeBump = if (item.size == "Large" || item.size == "Family") 100.0 else 0.0
            val toppingsBump = item.extraToppings * 20.0
            val scoopsBump = if (item.scoops > 2) (item.scoops - 2) * 50.0 else 0.0
            
            val finalItemPrice = basePrice + sizeBump + toppingsBump + scoopsBump
            finalItemPrice * item.quantity
        }
        
        val subtotal = if (state.payWithStars) 0.0 else rawSubtotal
        
        val deliveryFee = if (state.fulfillmentMode == FulfillmentMode.DELIVERY) 50.0 else 0.0
        val serviceFee = if (state.fulfillmentMode == FulfillmentMode.DELIVERY && !state.payWithStars) rawSubtotal * 0.05 else 0.0
        val tip = if (state.fulfillmentMode == FulfillmentMode.DELIVERY) state.driverTip else 0.0
        
        val total = if (state.items.isEmpty()) 0.0 else subtotal + deliveryFee + serviceFee + tip
        
        return state.copy(
            subtotal = subtotal,
            deliveryFee = deliveryFee,
            serviceFee = serviceFee,
            driverTip = tip,
            total = total
        )
    }

    fun clearCart() {
        _cartState.update { CartState(fulfillmentMode = it.fulfillmentMode) }
    }
}
