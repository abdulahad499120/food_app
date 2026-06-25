package com.example.foodapp.data.models

/**
 * Category constants for payment methods.
 * Using String instead of Enum to ensure Firestore toObject() deserialization works correctly.
 */
object PaymentMethodCategory {
    const val CARD = "CARD"
    const val RAAST = "RAAST"
    const val BANK_ACCOUNT = "BANK_ACCOUNT"
}

/**
 * # H1 Payment Method Data Model
 *
 * Represents a tokenized payment method (Card, Raast, or Bank Account).
 * All fields have default values to support Firestore's no-arg constructor requirement.
 * The category is stored as a String to avoid Firestore enum deserialization issues.
 */
data class PaymentMethod(
    val id: String = "",
    val userId: String = "",
    // Store category as String for Firestore compatibility (enum can silently fail on toObject())
    val category: String = PaymentMethodCategory.CARD,
    // The real Safepay Vault instrument token (e.g. "pay_xxx") used for charging
    val vaultToken: String = "",
    // Card specific
    val type: String = "",       // e.g., "Visa", "Mastercard"
    val last4: String = "",
    val expiry: String = "",     // stored as "MMYY" raw digits
    // Raast specific
    val raastId: String = "",    // usually a mobile number
    // Bank Account specific
    val bankName: String = "",
    val accountNumber: String = "",
    val isDefault: Boolean = false
)
