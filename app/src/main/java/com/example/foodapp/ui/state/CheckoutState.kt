package com.example.foodapp.ui.state

import com.example.foodapp.data.models.Address

enum class CheckoutStatus {
    Idle, Loading, Success, Error
}

data class CheckoutUiState(
    val address: Address = Address(),
    val paymentMethod: String = "Cash on Delivery",
    val status: CheckoutStatus = CheckoutStatus.Idle,
    val errorMessage: String? = null,
    val placedOrderId: String? = null
)
