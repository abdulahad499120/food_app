package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.repository.AddressRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

sealed class AddressListUiState {
    object Loading : AddressListUiState()
    data class Success(val addresses: List<Address>) : AddressListUiState()
    data class Error(val message: String) : AddressListUiState()
}

class AddressViewModel(
    private val repository: AddressRepository = AddressRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow<AddressListUiState>(AddressListUiState.Loading)
    val uiState: StateFlow<AddressListUiState> = _uiState.asStateFlow()

    fun loadAddresses(userId: String) {
        viewModelScope.launch {
            _uiState.update { AddressListUiState.Loading }
            try {
                repository.getUserAddresses(userId).collect { addresses ->
                    _uiState.update { AddressListUiState.Success(addresses) }
                }
            } catch (e: Exception) {
                _uiState.update { AddressListUiState.Error(e.message ?: "Failed to load addresses") }
            }
        }
    }

    fun saveAddress(userId: String, address: Address, onSuccess: () -> Unit) {
        viewModelScope.launch {
            repository.saveAddress(userId, address).onSuccess {
                onSuccess()
            }
        }
    }
    
    fun deleteAddress(userId: String, addressId: String) {
        viewModelScope.launch {
            repository.deleteAddress(userId, addressId)
        }
    }

    fun validateAddress(street: String, city: String, postalCode: String, phone: String?): String? {
        if (street.trim().length < 5) {
            return "Street address must be at least 5 characters"
        }
        if (city.trim().isEmpty()) {
            return "City cannot be empty"
        }
        if (!postalCode.trim().matches(Regex("^[0-9]+$"))) {
            return "Postal code must be numeric"
        }
        if (phone != null && phone.isNotBlank()) {
            if (!phone.trim().matches(Regex("^[0-9]{10,11}$"))) {
                return "Phone number must be 10-11 digits"
            }
        }
        return null
    }
}
