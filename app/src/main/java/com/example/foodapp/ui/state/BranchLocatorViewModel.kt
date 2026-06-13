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
    val branchDistances: Map<String, Float> = emptyMap()
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
        return branches.sortedWith(compareBy<Branch> { 
            distances[it.branchId] ?: Float.MAX_VALUE 
        }.thenBy { it.name })
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
