package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.repository.OrderRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

data class RiderSessionState(
    val currentRiderId: String = "rider_001",
    val riderName: String = "Ali",
    val riderVehicle: String = "Honda CD70",
    val riderPhone: String = "+923001234567",
    val activeBranchId: String = "branch_pia",
    val availableJobs: List<Order> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isOnline: Boolean = false, // Start offline
    val activeOrderId: String? = null // Persisted active order
)

class RiderSessionViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {
    private val _uiState = MutableStateFlow(RiderSessionState())
    val uiState: StateFlow<RiderSessionState> = _uiState.asStateFlow()

    init {
        fetchRiderProfile()
        listenForJobs()
        checkActiveDelivery()
    }

    private fun checkActiveDelivery() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("orders")
                .whereEqualTo("riderId", user.uid)
                .whereIn("orderStatus", listOf(
                    com.example.foodapp.data.models.OrderStatus.RIDER_ASSIGNED.name,
                    com.example.foodapp.data.models.OrderStatus.READY_FOR_RIDER.name,
                    com.example.foodapp.data.models.OrderStatus.OUT_FOR_DELIVERY.name
                ))
                .addSnapshotListener { snapshot, error ->
                    if (error == null && snapshot != null && !snapshot.isEmpty) {
                        // Found an active delivery for this rider
                        val activeDoc = snapshot.documents[0]
                        _uiState.update { it.copy(activeOrderId = activeDoc.id) }
                    } else {
                        _uiState.update { it.copy(activeOrderId = null) }
                    }
                }
        }
    }

    private fun fetchRiderProfile() {
        val user = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
        if (user != null) {
            _uiState.update { it.copy(currentRiderId = user.uid, riderName = user.displayName ?: "Rider", riderPhone = "N/A") }
            // Fetch more details from Firestore 'riders' collection if needed
            com.google.firebase.firestore.FirebaseFirestore.getInstance().collection("riders").document(user.uid).get()
                .addOnSuccessListener { doc ->
                    if (doc.exists()) {
                        _uiState.update { 
                            it.copy(
                                riderName = doc.getString("name") ?: it.riderName,
                                riderPhone = doc.getString("phone") ?: it.riderPhone,
                                riderVehicle = doc.getString("vehicle") ?: it.riderVehicle
                            ) 
                        }
                    }
                }
        }
    }

    private fun listenForJobs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            orderRepository.getAvailableJobs(_uiState.value.activeBranchId)
                .catch { e -> 
                    _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
                }
                .collect { jobs ->
                    _uiState.update { it.copy(availableJobs = jobs, isLoading = false) }
                }
        }
    }

    fun claimJob(orderId: String, onSuccess: () -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val state = _uiState.value
            val result = orderRepository.claimOrderTransaction(
                orderId = orderId,
                riderId = state.currentRiderId,
                riderName = state.riderName,
                riderVehicle = state.riderVehicle,
                riderPhone = state.riderPhone
            )
            
            result.onSuccess {
                onSuccess()
            }.onFailure { e ->
                onError(e.message ?: "Failed to claim order")
            }
        }
    }

    fun toggleOnlineStatus(isOnline: Boolean) {
        _uiState.update { it.copy(isOnline = isOnline) }
    }
}
