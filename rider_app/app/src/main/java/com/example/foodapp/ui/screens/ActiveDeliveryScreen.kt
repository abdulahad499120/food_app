package com.example.foodapp.ui.screens

import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.net.Uri
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.services.LocationBroadcastService
import com.example.foodapp.ui.state.ActiveDeliveryViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import kotlin.math.*

fun calculateBearing(start: Point, end: Point): Double {
    val lat1 = Math.toRadians(start.latitude())
    val lon1 = Math.toRadians(start.longitude())
    val lat2 = Math.toRadians(end.latitude())
    val lon2 = Math.toRadians(end.longitude())

    val dLon = lon2 - lon1
    val y = sin(dLon) * cos(lat2)
    val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
    var bearing = Math.toDegrees(atan2(y, x))
    bearing = (bearing + 360) % 360
    return bearing
}



fun calculateDistance(p1: Point, p2: Point): Double {
    val r = 6371000.0
    val lat1 = Math.toRadians(p1.latitude())
    val lon1 = Math.toRadians(p1.longitude())
    val lat2 = Math.toRadians(p2.latitude())
    val lon2 = Math.toRadians(p2.longitude())
    val dLat = lat2 - lat1
    val dLon = lon2 - lon1
    val a = sin(dLat / 2).pow(2) + cos(lat1) * cos(lat2) * sin(dLon / 2).pow(2)
    val c = 2 * atan2(sqrt(a), sqrt(1 - a))
    return r * c
}

fun closestPointOnSegment(p: Point, a: Point, b: Point): Pair<Point, Double> {
    val latScale = cos(Math.toRadians(p.latitude()))
    val px = p.longitude() * latScale
    val py = p.latitude()
    val ax = a.longitude() * latScale
    val ay = a.latitude()
    val bx = b.longitude() * latScale
    val by = b.latitude()
    
    val dx = bx - ax
    val dy = by - ay
    val lengthSq = dx * dx + dy * dy
    
    val t = if (lengthSq == 0.0) 0.0 else {
        val dot = ((px - ax) * dx + (py - ay) * dy) / lengthSq
        max(0.0, min(1.0, dot))
    }
    
    val projLon = (ax + t * dx) / latScale
    val projLat = ay + t * dy
    val projPoint = Point.fromLngLat(projLon, projLat)
    return Pair(projPoint, calculateDistance(p, projPoint))
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveDeliveryScreen(
    orderId: String,
    onDeliveryComplete: () -> Unit,
    viewModel: ActiveDeliveryViewModel = viewModel()
) {
    val order by viewModel.order.collectAsState()
    val context = LocalContext.current

    LaunchedEffect(orderId) {
        viewModel.loadOrder(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Active Delivery", fontWeight = FontWeight.Bold) },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        }
    ) { padding ->
        if (order == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            val isHeadingToCustomer = order!!.orderStatus == OrderStatus.OUT_FOR_DELIVERY
            val destLat = if (isHeadingToCustomer) order!!.deliveryAddress.location?.latitude ?: 0.0 else order!!.branchLocation?.latitude ?: 24.8607
            val destLng = if (isHeadingToCustomer) order!!.deliveryAddress.location?.longitude ?: 0.0 else order!!.branchLocation?.longitude ?: 67.0011
            val destPoint = Point.fromLngLat(destLng, destLat)

            val mapViewportState = rememberMapViewportState {
                setCameraOptions {
                    center(destPoint)
                    zoom(12.0)
                }
            }

            val destText = if (isHeadingToCustomer) "C" else "B"
            val destMarkerBitmap = remember(isHeadingToCustomer) { createMarkerBitmap(context, android.graphics.Color.parseColor(if (isHeadingToCustomer) "#E91E63" else "#2196F3"), "", if (isHeadingToCustomer) com.ahad.riderapp.R.drawable.ic_person else com.ahad.riderapp.R.drawable.ic_store) }
            val riderMarkerBitmap = remember { createMarkerBitmap(context, android.graphics.Color.parseColor("#FFC107"), "", com.ahad.riderapp.R.drawable.ic_moped) }

            var currentRiderLocation by remember { mutableStateOf<Point?>(null) }
            val routePoints by viewModel.routePoints.collectAsState()
            var initialCameraSet by remember { mutableStateOf(false) }
            val scope = rememberCoroutineScope()

            val trimmedRoutePoints = remember(routePoints, currentRiderLocation) {
                if (currentRiderLocation == null || routePoints.size < 2) return@remember routePoints
                
                var minDistance = Double.MAX_VALUE
                var bestIndex = 0
                
                for (i in 0 until routePoints.size - 1) {
                    val dist = closestPointOnSegment(currentRiderLocation!!, routePoints[i], routePoints[i+1]).second
                    if (dist < minDistance) {
                        minDistance = dist
                        bestIndex = i
                    }
                }
                
                val newRoute = mutableListOf<Point>()
                newRoute.add(currentRiderLocation!!)
                for (i in bestIndex + 1 until routePoints.size) {
                    newRoute.add(routePoints[i])
                }
                newRoute
            }

            LaunchedEffect(Unit) {
                val client = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(context)
                val callback = object : com.google.android.gms.location.LocationCallback() {
                    override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                        result.lastLocation?.let { loc ->
                            val pt = Point.fromLngLat(loc.longitude, loc.latitude)
                            
                            if (routePoints.isEmpty()) {
                                viewModel.fetchRoute(context, pt, destPoint)
                            } else {
                                var minDistance = Double.MAX_VALUE
                                for (i in 0 until routePoints.size - 1) {
                                    val dist = closestPointOnSegment(pt, routePoints[i], routePoints[i+1]).second
                                    if (dist < minDistance) minDistance = dist
                                }
                                if (minDistance > 50.0) {
                                    viewModel.fetchRoute(context, pt, destPoint)
                                }
                            }
                            
                            currentRiderLocation = pt
                            
                            if (!initialCameraSet) {
                                mapViewportState.setCameraOptions {
                                    center(pt)
                                    zoom(15.0)
                                    pitch(60.0)
                                    bearing(calculateBearing(pt, destPoint))
                                    padding(com.mapbox.maps.EdgeInsets(800.0, 0.0, 100.0, 0.0))
                                }
                                initialCameraSet = true
                            }
                        }
                    }
                }
                try {
                    val request = com.google.android.gms.location.LocationRequest.Builder(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, 3000L).build()
                    client.requestLocationUpdates(request, callback, android.os.Looper.getMainLooper())
                    kotlinx.coroutines.awaitCancellation()
                } catch (e: SecurityException) {
                    e.printStackTrace()
                } finally {
                    client.removeLocationUpdates(callback)
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
            ) {
                // Top Half: Mapbox Map
                Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                    MapboxMap(
                        modifier = Modifier.fillMaxSize(),
                        mapViewportState = mapViewportState
                    ) {
                        com.mapbox.maps.extension.compose.MapEffect(Unit) { mapView ->
                            mapView.getPlugin<com.mapbox.maps.plugin.locationcomponent.LocationComponentPlugin>(
                                com.mapbox.maps.plugin.Plugin.MAPBOX_LOCATION_COMPONENT_PLUGIN_ID
                            )?.enabled = false
                        }

                        if (trimmedRoutePoints.size > 1) {
                            com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation(
                                points = trimmedRoutePoints
                            ) {
                                lineColor = Color(0xFFE91E63)
                                lineWidth = 4.0
                            }
                        }

                        PointAnnotation(point = destPoint) {
                            iconImage = IconImage(destMarkerBitmap)
                        }
                        currentRiderLocation?.let { loc ->
                            PointAnnotation(point = loc) {
                                iconImage = IconImage(riderMarkerBitmap)
                            }
                        }
                    }

                    // Recenter Button
                    SmallFloatingActionButton(
                        onClick = {
                            currentRiderLocation?.let { loc ->
                                scope.launch {
                                    mapViewportState.flyTo(
                                        com.mapbox.maps.CameraOptions.Builder()
                                            .center(loc)
                                            .zoom(15.0)
                                            .pitch(60.0)
                                            .bearing(calculateBearing(loc, destPoint))
                                            .padding(com.mapbox.maps.EdgeInsets(800.0, 0.0, 100.0, 0.0))
                                            .build()
                                    )
                                }
                            }
                        },
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .padding(16.dp),
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = Color.White
                    ) {
                        Icon(
                            painter = androidx.compose.ui.res.painterResource(id = com.ahad.riderapp.R.drawable.ic_my_location),
                            contentDescription = "Recenter on Rider",
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                // Bottom Half: Details
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = if (isHeadingToCustomer) "Customer Details" else "Branch Details",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Black,
                                color = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (isHeadingToCustomer) {
                                Text(
                                    text = "Name: ${order!!.customerName}",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Address: ${order!!.deliveryAddress.streetAddress}, ${order!!.deliveryAddress.city}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                                Text(
                                    text = "Phone: ${order!!.deliveryAddress.phoneNumber}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            } else {
                                Text(
                                    text = "Head to Branch",
                                    style = MaterialTheme.typography.bodyLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Order ID: #${order!!.orderId.takeLast(6).uppercase()}",
                                    style = MaterialTheme.typography.bodyLarge
                                )
                            }
                        }
                    }

                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            onClick = {
                                val uri = Uri.parse("geo:0,0?q=${destLat},${destLng}")
                                val intent = Intent(Intent.ACTION_VIEW, uri)
                                try {
                                    context.startActivity(intent)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            modifier = Modifier.weight(1f).height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.surfaceVariant, contentColor = MaterialTheme.colorScheme.onSurfaceVariant)
                        ) {
                            Icon(Icons.Default.Directions, contentDescription = "Navigate")
                            Spacer(Modifier.width(8.dp))
                            Text("NAVIGATE", fontWeight = FontWeight.Bold)
                        }

                        if (isHeadingToCustomer) {
                            Button(
                                onClick = {
                                    val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order!!.deliveryAddress.phoneNumber}"))
                                    context.startActivity(intent)
                                },
                                modifier = Modifier.weight(1f).height(56.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondary)
                            ) {
                                Icon(Icons.Default.Call, contentDescription = "Call")
                                Spacer(Modifier.width(8.dp))
                                Text("CALL", fontWeight = FontWeight.Bold)
                            }
                        }
                    }

                    if (isHeadingToCustomer) {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED) {
                                    val intent = Intent(context, LocationBroadcastService::class.java)
                                    context.stopService(intent)
                                    onDeliveryComplete()
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981))
                        ) {
                            Text("MARK AS DELIVERED", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    } else {
                        Button(
                            onClick = {
                                viewModel.updateOrderStatus(orderId, OrderStatus.OUT_FOR_DELIVERY) {
                                    val intent = Intent(context, LocationBroadcastService::class.java).apply {
                                        putExtra(LocationBroadcastService.EXTRA_ORDER_ID, orderId)
                                    }
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                                        context.startForegroundService(intent)
                                    } else {
                                        context.startService(intent)
                                    }
                                }
                            },
                            modifier = Modifier.fillMaxWidth().height(64.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                        ) {
                            Text("CONFIRM PICKUP", fontSize = 20.sp, fontWeight = FontWeight.Black, color = Color.White)
                        }
                    }
                }
            }
        }
    }
}
