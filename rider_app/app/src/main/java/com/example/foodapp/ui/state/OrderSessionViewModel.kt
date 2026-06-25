package com.example.foodapp.ui.state

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.models.Branch
import com.example.foodapp.data.repository.RestaurantRepository
import com.example.foodapp.utils.BranchHoursParser
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

enum class FulfillmentMode {
    PICKUP,
    DELIVERY
}

sealed class AutoRoutingResult {
    object Success : AutoRoutingResult()
    data class Failure(val reason: String) : AutoRoutingResult()
}

class OrderSessionViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = RestaurantRepository()

    private val _fulfillmentMode = MutableStateFlow<FulfillmentMode?>(null)
    val fulfillmentMode: StateFlow<FulfillmentMode?> = _fulfillmentMode.asStateFlow()

    private val _activeDeliveryAddress = MutableStateFlow<Address?>(null)
    val activeDeliveryAddress: StateFlow<Address?> = _activeDeliveryAddress.asStateFlow()

    private val _activeBranch = MutableStateFlow<Branch?>(null)
    val activeBranch: StateFlow<Branch?> = _activeBranch.asStateFlow()

    private val _branches = MutableStateFlow<List<Branch>>(emptyList())
    val branches: StateFlow<List<Branch>> = _branches.asStateFlow()

    init {
        viewModelScope.launch {
            repository.getBranches().collect { fetchedBranches ->
                _branches.value = fetchedBranches
            }
        }
    }

    fun setFulfillmentMode(mode: FulfillmentMode?) {
        _fulfillmentMode.value = mode
    }

    fun setPickupBranch(branch: Branch) {
        _fulfillmentMode.value = FulfillmentMode.PICKUP
        _activeBranch.value = branch
        _activeDeliveryAddress.value = null
    }

    fun setPickupBranch(branchId: String) {
        val branch = _branches.value.find { it.branchId == branchId } ?: return
        setPickupBranch(branch)
    }

    /**
     * Called when the user confirms their delivery address.
     * Calculates distance to the nearest branch, validates if open & < 25km.
     */
    fun setDeliveryAddress(address: Address): AutoRoutingResult {
        if (_branches.value.isEmpty()) {
            return AutoRoutingResult.Failure("System initializing branches. Try again in a moment.")
        }

        val userLoc = android.location.Location("user").apply {
            latitude = address.location?.latitude ?: 0.0
            longitude = address.location?.longitude ?: 0.0
        }

        var minDistance = Float.MAX_VALUE
        var nearestBranch: Branch? = null

        for (branch in _branches.value) {
            branch.location?.let { geoPoint ->
                val branchLoc = android.location.Location("branch").apply {
                    latitude = geoPoint.latitude
                    longitude = geoPoint.longitude
                }
                val distance = userLoc.distanceTo(branchLoc)
                if (distance < minDistance) {
                    minDistance = distance
                    nearestBranch = branch
                }
            }
        }

        if (nearestBranch == null) {
            return AutoRoutingResult.Failure("Could not determine the nearest branch to your location.")
        }

        // Distance Check (25km)
        if (minDistance > 25_000f) {
            return AutoRoutingResult.Failure("Sorry, the nearest branch (${nearestBranch.name}) is too far away for delivery.")
        }

        // Hours Check
        val hoursStr = nearestBranch.operatingHours
        val isOpenRightNow = if (hoursStr.isNotBlank()) {
            BranchHoursParser.isOpenNow(hoursStr) ?: true
        } else {
            nearestBranch.isOpen
        }

        if (!isOpenRightNow) {
            return AutoRoutingResult.Failure("Sorry, the nearest branch (${nearestBranch.name}) is currently closed for delivery.")
        }

        // Auto-route success
        _fulfillmentMode.value = FulfillmentMode.DELIVERY
        _activeDeliveryAddress.value = address
        _activeBranch.value = nearestBranch

        return AutoRoutingResult.Success
    }

    fun clearSession() {
        _fulfillmentMode.value = null
        _activeDeliveryAddress.value = null
        _activeBranch.value = null
        CartManager.clearCart() // Assumes we add this to CartManager
    }
}
