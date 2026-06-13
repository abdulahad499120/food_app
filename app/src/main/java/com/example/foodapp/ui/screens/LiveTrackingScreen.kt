package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.LiveTrackingViewModel
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.Style

@Composable
fun LiveTrackingScreen(
    viewModel: LiveTrackingViewModel,
    onNavigateBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            uiState.userLocation?.let { center(it) }
            zoom(14.0)
            pitch(45.0)
        }
    }

    // Follow the driver when out for delivery
    LaunchedEffect(uiState.driverLocation) {
        uiState.driverLocation?.let {
            if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                mapViewportState.easeTo(
                    com.mapbox.maps.CameraOptions.Builder()
                        .center(it)
                        .build(),
                    com.mapbox.maps.plugin.animation.MapAnimationOptions.Builder().duration(100).build()
                )
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Mapbox Engine (Upper Portion)
            Box(modifier = Modifier.weight(1f).fillMaxWidth()) {
                MapboxMap(
                    modifier = Modifier.fillMaxSize(),
                    mapViewportState = mapViewportState,
                    style = {
                        // Using Style.LIGHT as requested
                        com.mapbox.maps.extension.compose.style.MapStyle(style = Style.LIGHT)
                    }
                ) {
                    // Branch location marker
                    uiState.branchLocation?.let {
                        // Simplified placeholder for standard marker
                    }

                    // User location marker
                    uiState.userLocation?.let {
                        
                    }

                    // Driver location marker
                    uiState.driverLocation?.let { driverPt ->
                        if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
                            // Rendering point annotation requires an icon image usually, but we will use a raw point annotation
                            // Mapbox Compose handles annotations natively. 
                            // Note: Without a bitmap, PointAnnotation won't show visually. For production, we load a bitmap.
                            // PointAnnotation(point = driverPt, iconImageBitmap = ...)
                        }
                    }
                }
            }

            // Status Stepper (Bottom Portion)
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shadowElevation = 16.dp,
                color = SurfaceWhite
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
                        AnimatedContent(
                            targetState = uiState.orderStatus,
                            transitionSpec = {
                                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                            },
                            label = "OrderStatusHeader"
                        ) { status ->
                            when (status) {
                                OrderStatus.PENDING -> Text("Order Placed", style = MaterialTheme.typography.headlineMedium)
                                OrderStatus.PREPARING -> Text("Preparing your order...", style = MaterialTheme.typography.headlineMedium)
                                OrderStatus.OUT_FOR_DELIVERY -> Text("Out for Delivery", style = MaterialTheme.typography.headlineMedium)
                                OrderStatus.DELIVERED -> Text("Arrived!", style = MaterialTheme.typography.headlineMedium, color = VAL_BRAND_PRIMARY)
                                else -> {}
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        LiveTrackingStatusStepper(currentStatus = uiState.orderStatus)
                        
                        Spacer(modifier = Modifier.height(24.dp))
                        
                        if (uiState.orderStatus == OrderStatus.DELIVERED) {
                            Button(
                                onClick = onNavigateBack,
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
                            ) {
                                Text("Back to Menu")
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
    val progress by androidx.compose.animation.core.animateFloatAsState(
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
            CircularProgressIndicator(
                progress = { 1f },
                modifier = Modifier.fillMaxSize(),
                color = Color.LightGray.copy(alpha = 0.3f),
                strokeWidth = 6.dp
            )
            CircularProgressIndicator(
                progress = { progress },
                modifier = Modifier.fillMaxSize(),
                color = Color.Red.copy(alpha = 0.8f),
                strokeWidth = 6.dp
            )
            TextButton(onClick = onCancelOrder) {
                Text("Cancel\nOrder", color = Color.Red, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
            }
        }
    }
}

@Composable
private fun LiveTrackingStatusStepper(currentStatus: OrderStatus) {
    val statuses = listOf(
        OrderStatus.PENDING to "Placed",
        OrderStatus.PREPARING to "Preparing",
        OrderStatus.OUT_FOR_DELIVERY to "Driving",
        OrderStatus.DELIVERED to "Arrived"
    )
    
    val currentIndex = statuses.indexOfFirst { it.first == currentStatus }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        statuses.forEachIndexed { index, pair ->
            val isCompleted = index <= currentIndex
            val isCurrent = index == currentIndex

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .background(
                            color = if (isCompleted) VAL_BRAND_PRIMARY else Color.LightGray,
                            shape = CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    if (isCompleted) {
                        Icon(
                            imageVector = if (pair.first == OrderStatus.OUT_FOR_DELIVERY) Icons.Default.LocalShipping else Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = pair.second,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (isCurrent) VAL_BRAND_PRIMARY else Color.Gray
                )
            }

            if (index < statuses.size - 1) {
                Divider(
                    modifier = Modifier
                        .weight(1f)
                        .padding(horizontal = 8.dp),
                    color = if (index < currentIndex) VAL_BRAND_PRIMARY else Color.LightGray,
                    thickness = 2.dp
                )
            }
        }
    }
}
