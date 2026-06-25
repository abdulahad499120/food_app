package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.LiveTrackingViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.Style
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.CircleAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.plugin.animation.MapAnimationOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    viewModel: com.example.foodapp.ui.state.LiveTrackingViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    chatViewModel: com.example.foodapp.ui.state.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onNavigateBack: () -> Unit,
    onNavigateToSupport: (String, String?) -> Unit,
    onNavigateToOrderComplete: (String) -> Unit,
    onNavigateToRiderChat: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val chatMessages by chatViewModel.messages.collectAsStateWithLifecycle()
    
    val unreadCount = chatMessages.count { it.senderType == "RIDER" && !it.isRead }
    
    LaunchedEffect(uiState.currentOrderId) {
        uiState.currentOrderId?.let { chatViewModel.initializeChat(it) }
    }
    val context = LocalContext.current

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            zoom(14.0)
            pitch(45.0)
        }
    }

    // When real branch/user locations first arrive from Firestore, re-center the camera.
    // This fixes the "ocean" bug where no coordinates were available at composable init time.
    LaunchedEffect(uiState.branchLocation, uiState.userLocation) {
        val bLoc = uiState.branchLocation
        val uLoc = uiState.userLocation
        if (bLoc != null && uLoc != null && uiState.orderStatus != OrderStatus.OUT_FOR_DELIVERY) {
            val centerLng = (bLoc.longitude() + uLoc.longitude()) / 2.0
            val centerLat = (bLoc.latitude() + uLoc.latitude()) / 2.0
            mapViewportState.easeTo(
                CameraOptions.Builder()
                    .center(Point.fromLngLat(centerLng, centerLat))
                    .zoom(13.0)
                    .build(),
                MapAnimationOptions.Builder().duration(1000).build()
            )
        } else if (uLoc != null) {
            mapViewportState.easeTo(
                CameraOptions.Builder().center(uLoc).zoom(14.0).build(),
                MapAnimationOptions.Builder().duration(800).build()
            )
        }
    }

    var previousDriverLocation by remember { androidx.compose.runtime.mutableStateOf<Point?>(null) }
    var targetDriverLocation by remember { androidx.compose.runtime.mutableStateOf<Point?>(null) }
    val animatedDriverFraction = remember { Animatable(1f) }

    LaunchedEffect(uiState.driverLocation) {
        if (uiState.driverLocation != null) {
            if (targetDriverLocation == null) {
                previousDriverLocation = uiState.driverLocation
                targetDriverLocation = uiState.driverLocation
                animatedDriverFraction.snapTo(1f)
            } else if (targetDriverLocation != uiState.driverLocation) {
                val startP = if (previousDriverLocation != null && targetDriverLocation != null) {
                    viewModel.calculateRiderLocation(previousDriverLocation!!, targetDriverLocation!!, animatedDriverFraction.value)
                } else uiState.driverLocation
                
                previousDriverLocation = startP
                targetDriverLocation = uiState.driverLocation
                animatedDriverFraction.snapTo(0f)
                animatedDriverFraction.animateTo(1f, tween(2000, easing = LinearEasing))
            }
        }
    }

    val currentRiderLocation = remember(animatedDriverFraction.value, previousDriverLocation, targetDriverLocation, uiState.branchLocation, uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
            if (previousDriverLocation != null && targetDriverLocation != null) {
                viewModel.calculateRiderLocation(previousDriverLocation!!, targetDriverLocation!!, animatedDriverFraction.value)
            } else {
                uiState.branchLocation
            }
        } else null
    }

    val riderBearing = remember(previousDriverLocation, targetDriverLocation, uiState.branchLocation, uiState.userLocation) {
        if (previousDriverLocation != null && targetDriverLocation != null) {
            viewModel.calculateBearing(previousDriverLocation!!, targetDriverLocation!!)
        } else {
            val start = uiState.branchLocation
            val end = uiState.userLocation
            if (start != null && end != null) {
                viewModel.calculateBearing(start, end)
            } else 0.0
        }
    }

    // Auto-navigate when Delivered
    LaunchedEffect(uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.DELIVERED) {
            uiState.currentOrderId?.let { orderId ->
                onNavigateToOrderComplete(orderId)
            }
        }
    }

    // Camera easing logic: Bound between user and driver when out for delivery
    LaunchedEffect(targetDriverLocation, uiState.userLocation) {
        val dLoc = targetDriverLocation ?: uiState.branchLocation
        val uLoc = uiState.userLocation
        if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY && dLoc != null && uLoc != null) {
            val centerLng = (dLoc.longitude() + uLoc.longitude()) / 2.0
            val centerLat = (dLoc.latitude() + uLoc.latitude()) / 2.0
            val center = Point.fromLngLat(centerLng, centerLat)
            
            // Heuristic for zoom based on distance
            val dLngDiff = Math.abs(dLoc.longitude() - uLoc.longitude())
            val dLatDiff = Math.abs(dLoc.latitude() - uLoc.latitude())
            val maxDiff = maxOf(dLngDiff, dLatDiff)
            val calculatedZoom = if (maxDiff > 0) 14.0 - kotlin.math.log2(maxDiff * 100) else 16.0
            val zoom = calculatedZoom.coerceIn(10.0, 18.0)

            mapViewportState.easeTo(
                CameraOptions.Builder()
                    .center(center)
                    .zoom(zoom)
                    .build(),
                MapAnimationOptions.Builder().duration(2000).build()
            )
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Marker Bitmaps
            val context = LocalContext.current
            val branchMarker = remember { createMarkerBitmap(context, android.graphics.Color.parseColor("#E91E63"), "", com.ahad.riderapp.R.drawable.ic_store) }
            val riderMarker = remember { createMarkerBitmap(context, android.graphics.Color.parseColor("#FFC107"), "", com.ahad.riderapp.R.drawable.ic_moped) }
            val userMarkerBitmap = remember { createMarkerBitmap(context, android.graphics.Color.parseColor("#4285F4"), "", com.ahad.riderapp.R.drawable.ic_person) }

            // Map Layer Full Screen
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                // Route Polyline between branch and delivery location
                if (uiState.routePoints.size > 1) {
                    PolylineAnnotation(points = uiState.routePoints) {
                        lineColor = Color(0xFFE91E63)
                        lineWidth = 4.0
                        lineOpacity = 0.75
                    }
                }

                // Branch Marker
                uiState.branchLocation?.let { loc ->
                    com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {
                        iconImage = com.mapbox.maps.extension.compose.annotation.IconImage(branchMarker)
                    }
                }

                // User Location Marker
                uiState.userLocation?.let { loc ->
                    com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {
                        iconImage = com.mapbox.maps.extension.compose.annotation.IconImage(userMarkerBitmap)
                    }
                }

                // Dynamic Rider Marker
                currentRiderLocation?.let { loc ->
                    if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                        com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {
                            iconImage = com.mapbox.maps.extension.compose.annotation.IconImage(riderMarker)
                            iconRotate = riderBearing
                        }
                    }
                }
            }

            // Top Overlays
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
                    .statusBarsPadding(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Circular Back Button
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                        .background(SurfaceWhite, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }

                // Pill Shaped Help Button (Edge Case 5)
                androidx.compose.animation.AnimatedVisibility(
                    visible = uiState.orderStatus != OrderStatus.GRACE_PERIOD,
                    enter = androidx.compose.animation.fadeIn() + androidx.compose.animation.scaleIn(),
                    exit = androidx.compose.animation.fadeOut() + androidx.compose.animation.scaleOut()
                ) {
                    val glowInfiniteTransition = androidx.compose.animation.core.rememberInfiniteTransition(label = "glow")
                    val glowAlpha by glowInfiniteTransition.animateFloat(
                        initialValue = 0.3f,
                        targetValue = 0.8f,
                        animationSpec = androidx.compose.animation.core.infiniteRepeatable(
                            animation = tween(1000, easing = LinearEasing),
                            repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
                        ),
                        label = "glowAlpha"
                    )
                    
                    Surface(
                        onClick = {
                            uiState.currentOrderId?.let { orderId ->
                                val prompt = "I need to change my delivery details for Order #$orderId."
                                onNavigateToSupport(orderId, prompt)
                            }
                        },
                        shape = RoundedCornerShape(24.dp),
                        color = SurfaceWhite,
                        shadowElevation = 8.dp,
                        border = androidx.compose.foundation.BorderStroke(2.dp, VAL_BRAND_PRIMARY.copy(alpha = glowAlpha)),
                        modifier = Modifier.height(48.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(Icons.Default.HelpOutline, contentDescription = "Help", tint = VAL_BRAND_PRIMARY, modifier = Modifier.size(20.dp))
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Help", color = VAL_BRAND_PRIMARY, fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            }

            // Bottom Status Card
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.BottomCenter)
                    .padding(16.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = SurfaceWhite,
                    shadowElevation = 16.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize()
                            .padding(24.dp)
                    ) {
                        if (uiState.orderStatus == OrderStatus.GRACE_PERIOD) {
                            GracePeriodView(
                                secondsRemaining = uiState.gracePeriodSecondsRemaining,
                                onCancelOrder = {
                                    val userId = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser?.uid
                                    viewModel.cancelOrder(userId) {
                                        onNavigateBack()
                                    }
                                }
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Doughnut Progress Chart
                                Box(
                                    modifier = Modifier.size(64.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    val targetProgress = if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                                        0.8f
                                    } else {
                                        when (uiState.orderStatus) {
                                            OrderStatus.PENDING -> 0.05f
                                            OrderStatus.PREPARING -> 0.15f
                                            OrderStatus.DELIVERED -> 1.0f
                                            else -> 0f
                                        }
                                    }
                                    val animatedProgress by animateFloatAsState(
                                        targetValue = targetProgress,
                                        animationSpec = tween(800),
                                        label = "ProgressAnimation"
                                    )
                                    
                                    Canvas(modifier = Modifier.fillMaxSize()) {
                                        drawArc(
                                            color = Color(0xFFF1F3F5),
                                            startAngle = 0f,
                                            sweepAngle = 360f,
                                            useCenter = false,
                                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                        drawArc(
                                            color = VAL_BRAND_PRIMARY,
                                            startAngle = -90f,
                                            sweepAngle = 360f * animatedProgress,
                                            useCenter = false,
                                            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                                        )
                                    }
                                    
                                    Text(
                                        text = "${(animatedProgress * 100).toInt()}%",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = VAL_BRAND_PRIMARY
                                    )
                                }
                                
                                Spacer(modifier = Modifier.width(16.dp))
                                
                                // Text Information
                                Column {
                                    AnimatedContent(
                                        targetState = uiState.orderStatus,
                                        transitionSpec = {
                                            fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                                        },
                                        label = "OrderStatusHeader"
                                    ) { status ->
                                        when (status) {
                                            OrderStatus.PENDING -> {
                                                Column {
                                                    Text("Order Placed", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Awaiting restaurant confirmation", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                                }
                                            }
                                            OrderStatus.PREPARING -> {
                                                Column {
                                                    Text("Preparing", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Your food is being prepared", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                                }
                                            }
                                            OrderStatus.OUT_FOR_DELIVERY -> {
                                                Column {
                                                    Text("Rider is on the way.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Arriving soon", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                                }
                                            }
                                            OrderStatus.DELIVERED -> {
                                                Column {
                                                    Text("Delivered!", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = VAL_BRAND_PRIMARY)
                                                    Text("Enjoy your meal", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                                }
                                            }
                                            else -> {}
                                        }
                                    }
                                }
                            }
                            
                            androidx.compose.animation.AnimatedVisibility(
                                visible = uiState.orderStatus == OrderStatus.READY_FOR_RIDER || 
                                          uiState.orderStatus == OrderStatus.RIDER_ASSIGNED || 
                                          uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY
                            ) {
                                Column(modifier = Modifier.fillMaxWidth().padding(top = 16.dp)) {
                                    HorizontalDivider(modifier = Modifier.padding(bottom = 16.dp), color = Color(0xFFEEEEEE))
                                    
                                    val order = uiState.activeOrder
                                    if (order?.riderId != null) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            verticalAlignment = Alignment.CenterVertically
                                        ) {
                                            // Rider Avatar
                                            Box(
                                                modifier = Modifier
                                                    .size(48.dp)
                                                    .clip(CircleShape)
                                                    .background(Color(0xFFF5F5F5)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(Icons.Default.Person, contentDescription = "Rider", tint = Color.Gray)
                                            }
                                            Spacer(modifier = Modifier.width(12.dp))
                                            Column(modifier = Modifier.weight(1f)) {
                                                Text(order.riderName ?: "Your Rider", fontWeight = FontWeight.Bold, color = TextPrimary)
                                                Text(order.riderVehicle ?: "On a vehicle", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                                            }
                                            
                                            // Call Button
                                            val context = androidx.compose.ui.platform.LocalContext.current
                                            IconButton(
                                                onClick = {
                                                    try {
                                                        val intent = android.content.Intent(android.content.Intent.ACTION_DIAL)
                                                        intent.data = android.net.Uri.parse("tel:${order.riderPhone}")
                                                        context.startActivity(intent)
                                                    } catch (e: android.content.ActivityNotFoundException) {
                                                        android.widget.Toast.makeText(context, "Dialer not available.", android.widget.Toast.LENGTH_SHORT).show()
                                                    }
                                                },
                                                modifier = Modifier
                                                    .size(40.dp)
                                                    .background(Color(0xFFE8F5E9), CircleShape)
                                            ) {
                                                Icon(Icons.Default.Phone, contentDescription = "Call Rider", tint = Color(0xFF4CAF50), modifier = Modifier.size(20.dp))
                                            }
                                            Spacer(modifier = Modifier.width(8.dp))
                                            
                                            // Chat Button with Badge
                                            Box(modifier = Modifier.size(40.dp)) {
                                                IconButton(
                                                    onClick = { uiState.currentOrderId?.let { onNavigateToRiderChat(it) } },
                                                    modifier = Modifier
                                                        .size(40.dp)
                                                        .background(VAL_BRAND_PRIMARY.copy(alpha = 0.1f), CircleShape)
                                                ) {
                                                    Icon(Icons.Default.Email, contentDescription = "Chat with Rider", tint = VAL_BRAND_PRIMARY, modifier = Modifier.size(20.dp))
                                                }
                                                if (unreadCount > 0) {
                                                    Badge(
                                                        modifier = Modifier.align(Alignment.TopEnd).offset(x = 4.dp, y = (-4).dp),
                                                        containerColor = Color.Red
                                                    ) {
                                                        Text("$unreadCount", color = Color.White)
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        Text("Looking for a rider...", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                                        // Simulate Rider button for testing
                                        Button(
                                            onClick = {
                                                uiState.currentOrderId?.let { oid ->
                                                    com.google.firebase.firestore.FirebaseFirestore.getInstance()
                                                        .collection("orders").document(oid)
                                                        .update(
                                                            "riderId", "rider_sim_001",
                                                            "riderName", "Alex Smith",
                                                            "riderVehicle", "Honda Beat (Black)",
                                                            "riderPhone", "+1234567890",
                                                            "orderStatus", OrderStatus.RIDER_ASSIGNED
                                                        )
                                                }
                                            },
                                            modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                                            colors = ButtonDefaults.buttonColors(containerColor = Color.Gray)
                                        ) {
                                            Text("Simulate Rider Assignment")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun GracePeriodView(secondsRemaining: Int, onCancelOrder: () -> Unit) {
    val progress by animateFloatAsState(
        targetValue = secondsRemaining / 60f,
        animationSpec = tween(1000, easing = androidx.compose.animation.core.LinearEasing),
        label = "GracePeriodProgress"
    )

    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Preparing to send order...", style = MaterialTheme.typography.titleMedium, color = Color.Gray)
        Spacer(modifier = Modifier.height(24.dp))
        
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(120.dp)) {
            val ringColor by animateColorAsState(
                targetValue = when {
                    secondsRemaining > 30 -> Color(0xFF4CAF50) // Green
                    secondsRemaining > 10 -> Color(0xFFFFC107) // Yellow
                    else -> Color(0xFFF44336) // Red
                },
                animationSpec = tween(500),
                label = "RingColor"
            )

            Canvas(modifier = Modifier.fillMaxSize()) {
                val strokeWidth = 6.dp.toPx()
                // Background Track
                drawArc(
                    color = Color.LightGray.copy(alpha = 0.3f),
                    startAngle = 0f,
                    sweepAngle = 360f,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                // Foreground Progress Arc
                drawArc(
                    color = ringColor,
                    startAngle = -90f,
                    sweepAngle = 360f * progress,
                    useCenter = false,
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
            TextButton(onClick = onCancelOrder) {
                Text("Cancel\nOrder", color = ringColor, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}
