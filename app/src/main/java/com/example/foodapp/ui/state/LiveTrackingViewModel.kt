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
    val driverLocation: Point? = null,
    val progress: Float = 0f,
    val gracePeriodSecondsRemaining: Int = 60,
    val currentOrderId: String? = null
)

class LiveTrackingViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(LiveTrackingState())
    val uiState: StateFlow<LiveTrackingState> = _uiState.asStateFlow()

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
            driverLocation = bLoc,
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
        CartManager.clearCart() // Lock in the order, clear the cart
        
        delay(3000)
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.PREPARING)
        
        delay(3000)
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.OUT_FOR_DELIVERY)
        
        // Simulate driving along a straight line (mocking polyline)
        val steps = 100
        for (i in 0..steps) {
            val t = i / steps.toFloat()
            val lng = start.longitude() + t * (end.longitude() - start.longitude())
            val lat = start.latitude() + t * (end.latitude() - start.latitude())
            
            _uiState.value = _uiState.value.copy(
                driverLocation = Point.fromLngLat(lng, lat),
                progress = t
            )
            delay(100) // 10 seconds total drive time
        }
        
        _uiState.value = _uiState.value.copy(orderStatus = OrderStatus.DELIVERED)
    }

    fun cancelOrder(userId: String?, onSuccess: () -> Unit) {
        trackingJob?.cancel()
        viewModelScope.launch {
            uiState.value.currentOrderId?.let { orderId ->
                orderRepository.deleteOrder(orderId)
            }
            if (CartManager.cartState.value.payWithStars && userId != null) {
                rewardsRepository.updateUserStars(userId, 150) // Refund stars
            }
            // We DO NOT clear the cart, so it restores for the user.
            onSuccess()
        }
    }
}
