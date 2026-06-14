package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.models.Branch
import com.example.foodapp.data.models.OrderStatus
import com.mapbox.geojson.Point
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin

data class LiveTrackingState(
    val orderStatus: OrderStatus = OrderStatus.GRACE_PERIOD,
    val branchLocation: Point? = null,
    val userLocation: Point? = null,
    val gracePeriodSecondsRemaining: Int = 60,
    val currentOrderId: String? = null
)

class LiveTrackingViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LiveTrackingState())
    val uiState: StateFlow<LiveTrackingState> = _uiState.asStateFlow()

    private val guestSessionRepo = com.example.foodapp.data.repository.GuestSessionRepository(application)

    private val orderRepository = com.example.foodapp.data.repository.OrderRepository()
    private val rewardsRepository = com.example.foodapp.data.repository.RewardsRepository()
    private var trackingJob: kotlinx.coroutines.Job? = null

    fun initializeTracking(branch: Branch?, address: Address?, orderId: String?) {
        val bLoc = branch?.location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(74.3587, 31.5204)
        val uLoc = address?.location?.let { Point.fromLngLat(it.longitude, it.latitude) } ?: Point.fromLngLat(74.34, 31.51)
        
        _uiState.value = LiveTrackingState(
            orderStatus = OrderStatus.GRACE_PERIOD,
            branchLocation = bLoc,
            userLocation = uLoc,
            gracePeriodSecondsRemaining = 60,
            currentOrderId = orderId
        )
        
        trackingJob?.cancel()
        trackingJob = startMockLifecycle(bLoc, uLoc)
    }
    
    private fun startMockLifecycle(start: Point, end: Point) = viewModelScope.launch {
        // Grace Period
        for (i in 60 downTo 1) {
            _uiState.value = _uiState.value.copy(gracePeriodSecondsRemaining = i)
            delay(1000)
        }
        
        // Grace period over, commit order
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.PENDING, gracePeriodSecondsRemaining = 0)
        
        delay(3000)
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.PREPARING)
        kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
            CartManager.clearCart() // Lock in the order, clear the cart
        }
        
        delay(3000)
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.OUT_FOR_DELIVERY)
        // The UI will now take over and animate the rider's journey for 5 minutes.
    }
    
    fun markOrderComplete() {
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.DELIVERED)
        viewModelScope.launch {
            guestSessionRepo.clearGuestActiveOrderId()
        }
    }

    fun calculateRiderLocation(start: Point, end: Point, fraction: Float): Point {
        val lng = start.longitude() + fraction * (end.longitude() - start.longitude())
        val lat = start.latitude() + fraction * (end.latitude() - start.latitude())
        return Point.fromLngLat(lng, lat)
    }

    fun calculateBearing(start: Point, end: Point): Double {
        val lat1 = Math.toRadians(start.latitude())
        val lat2 = Math.toRadians(end.latitude())
        val dLng = Math.toRadians(end.longitude() - start.longitude())
        
        val y = kotlin.math.sin(dLng) * kotlin.math.cos(lat2)
        val x = kotlin.math.cos(lat1) * kotlin.math.sin(lat2) - kotlin.math.sin(lat1) * kotlin.math.cos(lat2) * kotlin.math.cos(dLng)
        var brng = Math.toDegrees(kotlin.math.atan2(y, x))
        brng = (brng + 360) % 360
        return brng
    }

    fun cancelOrder(userId: String?, onSuccess: () -> Unit) {
        trackingJob?.cancel()
        viewModelScope.launch {
            uiState.value.currentOrderId?.let { orderId ->
                orderRepository.deleteOrder(orderId)
                guestSessionRepo.clearGuestActiveOrderId()
            }
            if (CartManager.cartState.value.payWithStars && userId != null) {
                rewardsRepository.updateUserStars(userId, 150) // Refund stars
            }
            // We DO NOT clear the cart, so it restores for the user.
            onSuccess()
        }
    }
}
