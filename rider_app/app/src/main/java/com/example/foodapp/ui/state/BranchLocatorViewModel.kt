package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Branch
import com.example.foodapp.data.repository.RestaurantRepository
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class BranchLocatorState(
    val branches: List<Branch> = emptyList(),
    val selectedBranchId: String? = null,
    val isLoading: Boolean = true,
    val userLocation: android.location.Location? = null,
    val recommendedBranchId: String? = null,
    val branchDistances: Map<String, Float> = emptyMap(),
    // Non-null when the user taps "Choose this store" on a branch >25km away.
    // The UI renders an AlertDialog asking them to confirm.
    val farBranchDialogBranchId: String? = null
)

class BranchLocatorViewModel : ViewModel() {
    private val repository = RestaurantRepository()

    private val _uiState = MutableStateFlow(BranchLocatorState())
    val uiState: StateFlow<BranchLocatorState> = _uiState.asStateFlow()

    // SharedFlow to trigger map camera fly-to events from the ViewModel
    private val _mapCameraEvents = MutableSharedFlow<Point>()
    val mapCameraEvents: SharedFlow<Point> = _mapCameraEvents.asSharedFlow()

    init {
        fetchBranches()
    }

    private fun fetchBranches() {
        viewModelScope.launch {
            try {
                repository.getBranches().collect { fetchedBranches ->
                    _uiState.update { state ->
                        val sortedBranches = sortBranchesByDistance(fetchedBranches, state.branchDistances)
                        state.copy(
                            branches = sortedBranches, 
                            isLoading = false
                        ) 
                    }
                }
            } catch (e: Exception) {
                // Ignore or handle silently to prevent app crash if read permission is denied
                _uiState.update { state -> state.copy(isLoading = false) }
            }
        }
    }

    fun updateUserLocation(lat: Double, lng: Double) {
        val location = android.location.Location("user").apply {
            latitude = lat
            longitude = lng
        }
        _uiState.update { state ->
            val distances = mutableMapOf<String, Float>()
            var minDistance = Float.MAX_VALUE
            var recommendedId: String? = null

            for (branch in state.branches) {
                branch.location?.let { geoPoint ->
                    val branchLoc = android.location.Location("branch").apply {
                        latitude = geoPoint.latitude
                        longitude = geoPoint.longitude
                    }
                    val distance = location.distanceTo(branchLoc)
                    distances[branch.branchId] = distance
                    if (distance < minDistance) {
                        minDistance = distance
                        recommendedId = branch.branchId
                    }
                }
            }
            
            val sortedBranches = sortBranchesByDistance(state.branches, distances)
            
            state.copy(
                userLocation = location,
                branchDistances = distances,
                recommendedBranchId = recommendedId,
                branches = sortedBranches
            )
        }
    }

    private fun sortBranchesByDistance(branches: List<Branch>, distances: Map<String, Float>): List<Branch> {
        // Only sort by distance if we actually have GPS-derived distances.
        // If the map is empty (no GPS fix yet), sort alphabetically to avoid
        // showing phantom 12,000km distances from Null Island (0,0).
        return if (distances.isEmpty()) {
            branches.sortedBy { it.name }
        } else {
            branches.sortedWith(compareBy<Branch> {
                distances[it.branchId] ?: Float.MAX_VALUE
            }.thenBy { it.name })
        }
    }

    /**
     * Called when the user taps "Choose this store".
     *
     * Returns true  → proceed immediately (branch is close enough, or GPS unavailable).
     * Returns false → branch is >25km away; the ViewModel has set [farBranchDialogBranchId]
     *                 so the UI should show an AlertDialog asking the user to confirm.
     */
    fun checkAndSelectBranch(branchId: String): Boolean {
        val distanceM = _uiState.value.branchDistances[branchId]
        val hasGps = _uiState.value.userLocation != null
        val isFar = hasGps && distanceM != null && distanceM > 25_000f

        return if (isFar) {
            _uiState.update { it.copy(farBranchDialogBranchId = branchId) }
            false
        } else {
            true
        }
    }

    /** Called when the user confirms the AlertDialog for a far branch. */
    fun confirmFarBranchSelection() {
        _uiState.update { it.copy(farBranchDialogBranchId = null) }
    }

    /** Called when the user dismisses the AlertDialog. */
    fun dismissFarBranchDialog() {
        _uiState.update { it.copy(farBranchDialogBranchId = null) }
    }

    /**
     * Called when a user taps a branch card or a map pin.
     * Updates the selected ID and emits a flyTo event for the map camera.
     */
    fun selectBranch(branchId: String) {
        val branch = _uiState.value.branches.find { it.branchId == branchId }
        _uiState.update { it.copy(selectedBranchId = branchId) }
        
        branch?.location?.let { geoPoint ->
            viewModelScope.launch {
                // Note: GeoPoint is (lat, lng), but Mapbox Point is (lng, lat)
                _mapCameraEvents.emit(Point.fromLngLat(geoPoint.longitude, geoPoint.latitude))
            }
        }
    }
}
