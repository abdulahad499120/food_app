package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.remote.SafepayRepository
import com.example.foodapp.data.repository.PaymentRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

sealed class SafepayVaultState {
    object Idle : SafepayVaultState()
    object Loading : SafepayVaultState()
    
    // Triggered when 3DS Device Data Collection is needed
    data class Requires3DS(
        val tracker: String,
        val accessToken: String,
        val ddcUrl: String
    ) : SafepayVaultState()

    object Success : SafepayVaultState()
    data class Error(val message: String) : SafepayVaultState()
}

class SafepayVaultViewModel(
    private val repository: SafepayRepository = SafepayRepository(),
    private val paymentRepository: PaymentRepository = PaymentRepository()
) : ViewModel() {

    private val _uiState = MutableStateFlow<SafepayVaultState>(SafepayVaultState.Idle)
    val uiState: StateFlow<SafepayVaultState> = _uiState.asStateFlow()

    // Temporary storage for flow
    private var currentTracker: String? = null

    fun startVaulting(
        userId: String,
        email: String,
        firstName: String,
        lastName: String,
        phone: String,
        cardNumber: String,
        expMonth: String,
        expYear: String,
        cvv: String
    ) {
        val cleanCardNumber = cardNumber.replace(" ", "").replace("-", "")
        if (cleanCardNumber.length !in 16..19) {
            _uiState.value = SafepayVaultState.Error("Card number must be 16 to 19 digits.")
            return
        }
        val formattedYear = if (expYear.length == 2) "20$expYear" else expYear
        if (formattedYear.length != 4) {
            _uiState.value = SafepayVaultState.Error("Expiration year must be 4 digits (e.g. 2026).")
            return
        }

        viewModelScope.launch {
            _uiState.value = SafepayVaultState.Loading
            
            // 1. Get or Create Customer
            var customerId = paymentRepository.getSafepayCustomerId(userId)
            if (customerId == null) {
                customerId = repository.createCustomer(email, firstName, lastName, phone, "PK")
                if (customerId == null) {
                    _uiState.value = SafepayVaultState.Error("Failed to create customer record")
                    return@launch
                }
                // Save it for next time
                paymentRepository.setSafepayCustomerId(userId, customerId)
            }

            // 2. Initialize Tracker
            val tracker = repository.initializeTracker(customerId)
            if (tracker == null) {
                _uiState.value = SafepayVaultState.Error("Failed to initialize payment tracker")
                return@launch
            }
            currentTracker = tracker

            // 3. Setup Auth (Tokenize Card)
            val authSetup = repository.submitAuthSetup(tracker, cleanCardNumber, expMonth, formattedYear, cvv)
            if (authSetup == null) {
                _uiState.value = SafepayVaultState.Error("Failed to tokenize card details. Please check your card number and expiry.")
                return@launch
            }

            // 4. Trigger 3DS DDC
            _uiState.value = SafepayVaultState.Requires3DS(
                tracker = tracker,
                accessToken = authSetup.accessToken,
                ddcUrl = authSetup.deviceDataCollectionUrl
            )
        }
    }

    // Called when the WebView intercepts the Cardinal Commerce DDC success
    fun onDdcSuccess(sessionId: String) {
        val tracker = currentTracker ?: return
        viewModelScope.launch {
            _uiState.value = SafepayVaultState.Loading
            
            // 5. Submit Enrollment
            val enrollResult = repository.submitEnrollment(
                tracker = tracker,
                sessionId = sessionId,
                street = "Street 1",
                city = "Islamabad",
                postalCode = "44000",
                country = "PK"
            )
            
            when (enrollResult) {
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.RequiresChallenge -> {
                    // For now, fail it because AddEditPaymentScreen doesn't have Challenge WebView UI yet
                    _uiState.value = SafepayVaultState.Error("OTP Challenge is required but not supported on this screen. Please add card during checkout.")
                    return@launch
                }
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.Error -> {
                    _uiState.value = SafepayVaultState.Error(enrollResult.message)
                    return@launch
                }
                is com.example.foodapp.data.remote.SafepayRepository.EnrollmentResult.Success -> {
                    // 6. Submit Final Authorization
                    val authorized = repository.submitAuthorization(tracker)
                    if (authorized) {
                        _uiState.value = SafepayVaultState.Success
                    } else {
                        _uiState.value = SafepayVaultState.Error("Failed final authorization")
                    }
                }
            }
        }
    }

    fun onCancel() {
        _uiState.value = SafepayVaultState.Idle
    }
}
