package com.example.foodapp.data.models

/**
 * # H1 Payment Method Data Model
 * 
 * Represents a tokenized payment method. It securely stores only the required 
 * display information and excludes sensitive data such as full PAN or CVV.
 */
data class PaymentMethod(
    val id: String = "",
    val userId: String = "",
    val type: String = "", // e.g., "Visa", "Mastercard", "Amex"
    val last4: String = "",
    val expiry: String = "", // e.g., "12/25"
    val isDefault: Boolean = false
)
