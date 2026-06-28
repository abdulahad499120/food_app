package com.example.foodapp.ui.state

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.data.repository.OrderRepository
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await

class ActiveDeliveryViewModel(
    private val orderRepository: OrderRepository = OrderRepository()
) : ViewModel() {

    private val _order = MutableStateFlow<Order?>(null)
    val order: StateFlow<Order?> = _order.asStateFlow()

    fun loadOrder(orderId: String) {
        viewModelScope.launch {
            orderRepository.observeActiveOrder(orderId)
                .catch { /* handle error silently or update state */ }
                .collect { orderData ->
                _order.update { orderData }
            }
        }
    }

    private val _routePoints = MutableStateFlow<List<com.mapbox.geojson.Point>>(emptyList())
    val routePoints: StateFlow<List<com.mapbox.geojson.Point>> = _routePoints.asStateFlow()

    fun fetchRoute(context: android.content.Context, origin: com.mapbox.geojson.Point, destination: com.mapbox.geojson.Point) {
        viewModelScope.launch(kotlinx.coroutines.Dispatchers.IO) {
            try {
                val token = context.getString(context.resources.getIdentifier("mapbox_access_token", "string", context.packageName))
                val url = "https://api.mapbox.com/directions/v5/mapbox/driving/" +
                    "${origin.longitude()},${origin.latitude()};" +
                    "${destination.longitude()},${destination.latitude()}" +
                    "?geometries=geojson&overview=full&access_token=$token"

                val response = java.net.URL(url).readText()
                val json = org.json.JSONObject(response)
                val routes = json.getJSONArray("routes")
                if (routes.length() > 0) {
                    val coords = routes.getJSONObject(0)
                        .getJSONObject("geometry")
                        .getJSONArray("coordinates")
                    val points = (0 until coords.length()).map { i ->
                        val pair = coords.getJSONArray(i)
                        com.mapbox.geojson.Point.fromLngLat(pair.getDouble(0), pair.getDouble(1))
                    }
                    _routePoints.value = points
                }
            } catch (e: Exception) {
                android.util.Log.w("ActiveDeliveryViewModel", "Route fetch failed: ${e.message}")
            }
        }
    }

    fun updateOrderStatus(orderId: String, status: OrderStatus, onSuccess: () -> Unit) {
        viewModelScope.launch {
            try {
                val db = FirebaseFirestore.getInstance()
                db.collection("orders").document(orderId).update("orderStatus", status.name).await()
                onSuccess()
            } catch (e: Exception) {
                // handle error
            }
        }
    }
}
