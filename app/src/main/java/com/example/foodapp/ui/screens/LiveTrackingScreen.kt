package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
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
import androidx.compose.material.icons.filled.HelpOutline
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
import com.mapbox.maps.plugin.animation.MapAnimationOptions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LiveTrackingScreen(
    viewModel: LiveTrackingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToSupport: (String, String?) -> Unit = { _, _ -> },
    onNavigateToOrderComplete: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            uiState.userLocation?.let { center(it) }
            zoom(14.0)
            pitch(45.0)
        }
    }

    val trackingProgress = remember { Animatable(0f) }

    LaunchedEffect(uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
            trackingProgress.animateTo(1f, tween(300000, easing = LinearEasing))
            viewModel.markOrderComplete()
        }
    }

    val currentRiderLocation = remember(trackingProgress.value, uiState.branchLocation, uiState.userLocation) {
        val start = uiState.branchLocation
        val end = uiState.userLocation
        if (start != null && end != null) {
            viewModel.calculateRiderLocation(start, end, trackingProgress.value)
        } else null
    }

    val riderBearing = remember(uiState.branchLocation, uiState.userLocation) {
        val start = uiState.branchLocation
        val end = uiState.userLocation
        if (start != null && end != null) {
            viewModel.calculateBearing(start, end)
        } else 0.0
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
    LaunchedEffect(currentRiderLocation, uiState.userLocation) {
        val dLoc = currentRiderLocation
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
                MapAnimationOptions.Builder().duration(1000).build()
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
            // Map Layer Full Screen
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState,
                style = { com.mapbox.maps.extension.compose.style.MapStyle(style = com.mapbox.maps.Style.STANDARD) }
            ) {
                // Branch Marker
                uiState.branchLocation?.let { loc ->
                    com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {}
                }

                // User Location Marker
                uiState.userLocation?.let { loc ->
                    com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {}
                }

                // Dynamic Rider Marker
                currentRiderLocation?.let { loc ->
                    if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                        com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation(point = loc) {
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
                                        0.2f + (trackingProgress.value * 0.8f)
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
                                                    val fraction = trackingProgress.value
                                                    val minutesLeft = (5 - (fraction * 5)).toInt().coerceAtLeast(1)
                                                    val headline = if (fraction < 0.2f) "Rider is picking up your order." 
                                                                 else if (fraction < 0.9f) "Rider is on the way."
                                                                 else "Rider is arriving now!"
                                                    
                                                    Text(headline, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = TextPrimary)
                                                    Text("Arriving in ~${minutesLeft} min${if(minutesLeft > 1) "s" else ""}", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
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
