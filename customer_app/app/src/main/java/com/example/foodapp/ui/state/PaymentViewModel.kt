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

sealed class PaymentListUiState {
    object Loading : PaymentListUiState()
    data class Success(val payments: List<PaymentMethod>) : PaymentListUiState()
    data class Error(val message: String) : PaymentListUiState()
}

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

    fun saveRaastPayment(
        userId: String,
        raastId: String,
        isDefault: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (raastId.isBlank() || raastId.length < 11) {
            onError("Please enter a valid Raast ID (Mobile Number).")
            return
        }

        viewModelScope.launch {
            val payment = PaymentMethod(
                userId = userId,
                category = com.example.foodapp.data.models.PaymentMethodCategory.RAAST,
                raastId = raastId,
                isDefault = isDefault
            )

            repository.savePaymentMethod(userId, payment).onSuccess {
                loadPayments(userId)
                onSuccess()
            }.onFailure { e ->
                onError("Failed to save Raast ID: \${e.message}")
            }
        }
    }

    fun saveBankAccount(
        userId: String,
        bankName: String,
        accountNumber: String,
        isDefault: Boolean,
        onSuccess: () -> Unit,
        onError: (String) -> Unit
    ) {
        if (bankName.isBlank() || accountNumber.isBlank()) {
            onError("Bank name and Account Number are required.")
            return
        }

        viewModelScope.launch {
            val payment = PaymentMethod(
                userId = userId,
                category = com.example.foodapp.data.models.PaymentMethodCategory.BANK_ACCOUNT,
                bankName = bankName,
                accountNumber = accountNumber,
                isDefault = isDefault
            )

            repository.savePaymentMethod(userId, payment).onSuccess {
                loadPayments(userId)
                onSuccess()
            }.onFailure { e ->
                onError("Failed to save Bank Account: \${e.message}")
            }
        }
    }

    fun updatePaymentDefaultStatus(userId: String, payment: PaymentMethod) {
        viewModelScope.launch {
            repository.setDefaultPayment(userId, payment.id)
        }
    }

    fun deletePayment(userId: String, paymentId: String) {
        viewModelScope.launch {
            repository.deletePaymentMethod(userId, paymentId)
        }
    }
}
