package com.example.foodapp.ui.screens

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Directions
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.services.LocationBroadcastService
import com.example.foodapp.ui.state.ActiveDeliveryViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import kotlin.math.*
import kotlinx.coroutines.CancellationException

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

    if (order == null) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator(color = Color(0xFFB80049))
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
            } catch (e: CancellationException) {
                // Ignore cancellation exception
            } finally {
                client.removeLocationUpdates(callback)
            }
        }

        Box(modifier = Modifier.fillMaxSize()) {
            // Map Background
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
                    .padding(end = 16.dp, bottom = 450.dp), // Lifted above bottom sheet
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = Color.White
            ) {
                Icon(
                    painter = androidx.compose.ui.res.painterResource(id = com.ahad.riderapp.R.drawable.ic_my_location),
                    contentDescription = "Recenter on Rider",
                    modifier = Modifier.size(24.dp)
                )
            }

            // Map Gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        brush = androidx.compose.ui.graphics.Brush.verticalGradient(
                            colors = listOf(Color.Transparent, Color.Black.copy(alpha = 0.1f))
                        )
                    )
            )

            // Bottom Sheet Content
            Column(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .shadow(elevation = 16.dp, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                    .background(Color(0xFFFFFFFF)) // CreamWhite
                    .padding(horizontal = 16.dp, vertical = 24.dp)
            ) {
                // Drag Handle
                Box(
                    modifier = Modifier
                        .width(48.dp)
                        .height(6.dp)
                        .background(Color(0xFFFADBDE), RoundedCornerShape(50)) // SurfaceVariant
                        .align(Alignment.CenterHorizontally)
                )
                
                Spacer(modifier = Modifier.height(24.dp))

                // Customer Info Header
                Column {
                    Text(
                        text = if (isHeadingToCustomer) "DELIVERING TO" else "HEADING TO BRANCH",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5B3F43), // OnSurfaceVariant
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = if (isHeadingToCustomer) order!!.customerName else "Branch #${order!!.orderId.takeLast(4).uppercase()}",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF28171A), // OnSurface
                        fontWeight = FontWeight.ExtraBold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.LocationOn,
                            contentDescription = null,
                            tint = Color(0xFF5B3F43),
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isHeadingToCustomer) "${order!!.deliveryAddress.streetAddress}, ${order!!.deliveryAddress.city}" else "Pickup Location",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color(0xFF5B3F43) // OnSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
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
                        modifier = Modifier
                            .weight(1f)
                            .height(56.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFE8F5E9), // CoolMint
                            contentColor = Color(0xFF008091) // TertiaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Directions, contentDescription = "Navigate")
                        Spacer(Modifier.width(8.dp))
                        Text("Navigate", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    }

                    if (isHeadingToCustomer) {
                        Button(
                            onClick = {
                                val intent = Intent(Intent.ACTION_DIAL, Uri.parse("tel:${order!!.deliveryAddress.phoneNumber}"))
                                context.startActivity(intent)
                            },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFFFE1E4), // SurfaceContainerHigh
                                contentColor = Color(0xFFB80049) // Primary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Call, contentDescription = "Call")
                            Spacer(Modifier.width(8.dp))
                            Text("Call", fontSize = 16.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                // Slide to Confirm
                SwipeToConfirm(
                    text = if (isHeadingToCustomer) "Slide to Deliver" else "Slide to Confirm Pickup",
                    onConfirm = {
                        if (isHeadingToCustomer) {
                            viewModel.updateOrderStatus(orderId, OrderStatus.DELIVERED) {
                                val intent = Intent(context, LocationBroadcastService::class.java)
                                context.stopService(intent)
                                onDeliveryComplete()
                            }
                        } else {
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
                        }
                    }
                )
                
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
