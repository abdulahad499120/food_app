package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.PaymentMethod
import com.example.foodapp.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * # H1 Payment List UI State
 */
sealed class PaymentListUiState {
    object Loading : PaymentListUiState()
    data class Success(val payments: List<PaymentMethod>) : PaymentListUiState()
    data class Error(val message: String) : PaymentListUiState()
}

/**
 * # H1 Payment ViewModel
 * 
 * Manages the UI state for the payment list and contains the simulated 
 * tokenization logic to prevent storing raw PANs or CVVs.
 */
class PaymentViewModel(
    private val repository: PaymentRepository = PaymentRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<PaymentListUiState>(PaymentListUiState.Loading)
    val uiState: StateFlow<PaymentListUiState> = _uiState.asStateFlow()

    fun loadPayments(userId: String) {
        viewModelScope.launch {
            _uiState.update { PaymentListUiState.Loading }
            try {
                repository.getUserPayments(userId).collect { payments ->
                    _uiState.update { PaymentListUiState.Success(payments) }
                }
            } catch (e: Exception) {
                _uiState.update { PaymentListUiState.Error(e.message ?: "Failed to load payments") }
            }
        }
    }

    /**
     * ## H3 Simulate Gateway Tokenization
     * 
     * Simulates a secure payment gateway tokenization process.
     * Enforces a 1.5-second delay to mimic network latency.
     * Extracts safe data and discards raw inputs from memory.
     */
    fun simulateGatewayTokenization(
        userId: String,
        cardNumber: String,
        expiry: String,
        cvv: String,
        isDefault: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        val cleanCard = cardNumber.replace("\\s".toRegex(), "")
        
        // H4 Failsafe Validation
        if (cleanCard.length < 16) {
            onError("Card number must be 16 digits.")
            return
        }
        if (cvv.length < 3) {
            onError("CVV must be at least 3 digits.")
            return
        }

        viewModelScope.launch {
            // Simulate Network Latency
            kotlinx.coroutines.delay(1500)

            val last4 = cleanCard.takeLast(4)
            
            // Simple mock card type detection
            val type = when {
                cleanCard.startsWith("4") -> "Visa"
                cleanCard.startsWith("5") -> "Mastercard"
                cleanCard.startsWith("3") -> "Amex"
                else -> "Card"
            }

            // H4 Token ID Generation
            val mockTokenId = "mock_tok_${(1000..9999).random()}"

            // H4 Build Tokenized Model
            val tokenizedPayment = PaymentMethod(
                id = mockTokenId,
                userId = userId,
                type = type,
                last4 = last4,
                expiry = expiry,
                isDefault = isDefault
            )

            repository.savePaymentMethod(userId, tokenizedPayment).onSuccess {
                onSuccess()
            }.onFailure {
                onError(it.message ?: "Failed to save payment method.")
            }
        }
    }

    fun updatePaymentDefaultStatus(userId: String, payment: PaymentMethod) {
        viewModelScope.launch {
            repository.savePaymentMethod(userId, payment.copy(isDefault = true))
        }
    }

    fun deletePayment(userId: String, paymentId: String) {
        viewModelScope.launch {
            repository.deletePaymentMethod(userId, paymentId)
        }
    }

    fun validatePayment(cardNumber: String, expiry: String, cvv: String): String? {
        val cleanCard = cardNumber.replace("\\s".toRegex(), "")
        if (cleanCard.length != 16 || !cleanCard.all { it.isDigit() }) {
            return "Card number must be 16 digits"
        }
        
        if (!expiry.matches(Regex("^(0[1-9]|1[0-2])/?([0-9]{2})$"))) {
            return "Expiry must be in MM/YY format"
        }
        
        val parts = expiry.split("/")
        if (parts.size == 2) {
            val month = parts[0].toIntOrNull() ?: 0
            val year = parts[1].toIntOrNull() ?: 0
            
            // Simple future validation: year > 24 (assuming 2024 is current year), or year == 24 and month >= current
            val currentYear = java.util.Calendar.getInstance().get(java.util.Calendar.YEAR) % 100
            val currentMonth = java.util.Calendar.getInstance().get(java.util.Calendar.MONTH) + 1
            
            if (year < currentYear || (year == currentYear && month < currentMonth)) {
                return "Expiry date must be in the future"
            }
        }
        
        if (cvv.length !in 3..4 || !cvv.all { it.isDigit() }) {
            return "CVV must be 3 or 4 digits"
        }
        
        return null
    }
}
