package com.example.foodapp.ui.state

import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.models.Branch
import com.example.foodapp.data.models.OrderStatus
import com.mapbox.geojson.Point
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.net.URL
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError

data class LiveTrackingState(
    val orderStatus: OrderStatus = OrderStatus.GRACE_PERIOD,
    val branchLocation: Point? = null,
    val userLocation: Point? = null,
    val driverLocation: Point? = null,
    val routePoints: List<Point> = emptyList(),
    val gracePeriodSecondsRemaining: Int = 60,
    val currentOrderId: String? = null,
    val fulfillmentMode: String = "DELIVERY",
    val activeOrder: com.example.foodapp.data.models.Order? = null
)

class LiveTrackingViewModel(application: android.app.Application) : androidx.lifecycle.AndroidViewModel(application) {
    private val _uiState = MutableStateFlow(LiveTrackingState())
    val uiState: StateFlow<LiveTrackingState> = _uiState.asStateFlow()

    private val guestSessionRepo = com.example.foodapp.data.repository.GuestSessionRepository(application)

    private val orderRepository = com.example.foodapp.data.repository.OrderRepository()
    private val rewardsRepository = com.example.foodapp.data.repository.RewardsRepository()
    private var trackingJob: kotlinx.coroutines.Job? = null
    private var locationListener: ValueEventListener? = null
    private var locationRef: com.google.firebase.database.DatabaseReference? = null

    fun initializeTracking(branch: Branch?, address: Address?, orderId: String?) {
        // Hint locations from nav args if available, will be overridden by Firestore snapshot
        val hintBranch = branch?.location?.let { Point.fromLngLat(it.longitude, it.latitude) }
        val hintUser = address?.location?.let { Point.fromLngLat(it.longitude, it.latitude) }

        _uiState.value = LiveTrackingState(
            orderStatus = OrderStatus.GRACE_PERIOD,
            branchLocation = hintBranch,
            userLocation = hintUser,
            gracePeriodSecondsRemaining = 60,
            currentOrderId = orderId
        )

        trackingJob?.cancel()
        locationRef?.let { ref -> locationListener?.let { listener -> ref.removeEventListener(listener) } }

        if (orderId != null) {
            trackingJob = viewModelScope.launch {
                orderRepository.observeOrder(orderId).collect { order ->
                    if (order != null) {
                        // Always read locations from Firestore order — source of truth
                        val branchPt = order.branchLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
                            ?: _uiState.value.branchLocation
                        val userPt = order.deliveryLocation?.let { Point.fromLngLat(it.longitude, it.latitude) }
                            ?: order.deliveryAddress.location?.let { Point.fromLngLat(it.longitude, it.latitude) }
                            ?: _uiState.value.userLocation
                        // driverPt is now continuously updated by the RTDB listener below
                        val driverPt = _uiState.value.driverLocation

                        val prevBranch = _uiState.value.branchLocation
                        val prevUser = _uiState.value.userLocation

                        _uiState.value = _uiState.value.copy(
                            orderStatus = order.orderStatus,
                            branchLocation = branchPt,
                            userLocation = userPt,
                            driverLocation = driverPt,
                            fulfillmentMode = order.fulfillmentMode,
                            activeOrder = order
                        )

                        if (order.orderStatus == OrderStatus.PREPARING) {
                            kotlinx.coroutines.withContext(kotlinx.coroutines.Dispatchers.Main) {
                                CartManager.clearCart()
                            }
                        }

                        // Fetch route if we now have both endpoints and didn't before
                        val routeIsEmpty = _uiState.value.routePoints.isEmpty()
                        val locationsChanged = branchPt != prevBranch || userPt != prevUser
                        if (branchPt != null && userPt != null && (routeIsEmpty || locationsChanged)) {
                            fetchRoute(branchPt, userPt)
                        }
                    }
                }
            }

            val dbRef = FirebaseDatabase.getInstance().getReference("active_deliveries").child(orderId)
            locationRef = dbRef
            val listener = object : ValueEventListener {
                override fun onDataChange(snapshot: DataSnapshot) {
                    val lat = snapshot.child("lat").getValue(Double::class.java)
                    val lng = snapshot.child("lng").getValue(Double::class.java)
                    if (lat != null && lng != null) {
                        _uiState.value = _uiState.value.copy(driverLocation = Point.fromLngLat(lng, lat))
                    }
                }
                override fun onCancelled(error: DatabaseError) {
                    android.util.Log.e("LiveTracking", "RTDB listen failed: ${error.message}")
                }
            }
            dbRef.addValueEventListener(listener)
            locationListener = listener
        }
    }

    private fun fetchRoute(origin: Point, destination: Point) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val ctx = getApplication<android.app.Application>()
                val token = ctx.getString(ctx.resources.getIdentifier("mapbox_access_token", "string", ctx.packageName))
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                    "${origin.longitude()},${origin.latitude()};" +
                    "${destination.longitude()},${destination.latitude()}" +
                    "?geometries=geojson&overview=full&access_token=$token"

                val response = URL(url).readText()
                val json = JSONObject(response)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val coords = routes.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")
                    val points = (0 until coords.length()).map { i ->
                        val pair = coords.getJSONArray(i)
                        Point.fromLngLat(pair.getDouble(0), pair.getDouble(1))
                    }
                    _uiState.value = _uiState.value.copy(routePoints = points)
                }
            } catch (e: Exception) {
                android.util.Log.w("LiveTracking", "Route fetch failed: ${e.message}")
            }
        }
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

    override fun onCleared() {
        super.onCleared()
        trackingJob?.cancel()
        locationRef?.let { ref -> locationListener?.let { listener -> ref.removeEventListener(listener) } }
    }
}
