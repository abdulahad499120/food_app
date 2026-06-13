package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Person
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Order
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.ActiveOrderTrackingViewModel
import com.example.foodapp.ui.state.TrackingUiState
import com.mapbox.geojson.LineString
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.EdgeInsets
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PolylineAnnotation
import com.mapbox.maps.extension.compose.annotation.IconImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ActiveOrderTrackingScreen(
    orderId: String,
    onNavigateBack: () -> Unit,
    viewModel: ActiveOrderTrackingViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(orderId) {
        viewModel.observeOrder(orderId)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Track Order", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        },
        containerColor = VAL_BACKGROUND
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = uiState) {
                is TrackingUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VAL_BRAND_PRIMARY
                    )
                }
                is TrackingUiState.Error -> {
                    Text(
                        text = state.message,
                        color = com.example.foodapp.theme.ErrorRed,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is TrackingUiState.Active -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Top Half: Mapbox Map
                        Box(modifier = Modifier.weight(1.2f)) {
                            TrackingMap(order = state.order)
                        }
                        
                        // Bottom Half: Status Stepper Bottom Sheet Area
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxWidth()
                                .background(Color.White, shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
                                .padding(top = 24.dp, start = 24.dp, end = 24.dp)
                        ) {
                            TrackingStatusPanel(order = state.order)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TrackingMap(order: Order) {
    // If locations are missing, mock them near Lahore for demonstration purposes
    val branchPoint = if (order.branchLocation != null) {
        Point.fromLngLat(order.branchLocation.longitude, order.branchLocation.latitude)
    } else {
        Point.fromLngLat(74.3587, 31.5204)
    }
    
    val deliveryPoint = if (order.deliveryLocation != null) {
        Point.fromLngLat(order.deliveryLocation.longitude, order.deliveryLocation.latitude)
    } else {
        Point.fromLngLat(74.3650, 31.5250) // Mock destination slightly northeast
    }

    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(branchPoint)
            zoom(13.0)
        }
    }

    // Adjust camera to fit both points
    LaunchedEffect(branchPoint, deliveryPoint) {
        // A simple approach is to center between the two points or just focus on branch
        val centerLng = (branchPoint.longitude() + deliveryPoint.longitude()) / 2
        val centerLat = (branchPoint.latitude() + deliveryPoint.latitude()) / 2
        mapViewportState.flyTo(
            CameraOptions.Builder()
                .center(Point.fromLngLat(centerLng, centerLat))
                .zoom(14.0)
                .build()
        )
    }

    val branchMarker = remember { createMapPinBitmap(VAL_SURFACE_DARK.toArgb()) }
    val deliveryMarker = remember { createMapPinBitmap(VAL_BRAND_PRIMARY.toArgb()) }

    MapboxMap(
        modifier = Modifier.fillMaxSize(),
        mapViewportState = mapViewportState
    ) {
        // Generate Curved Route Line
        val routePoints = remember(branchPoint, deliveryPoint) {
            generateCurvedPath(branchPoint, deliveryPoint)
        }
        
        PolylineAnnotation(
            points = routePoints
        ) {
            lineColor = VAL_BRAND_PRIMARY
            lineWidth = 4.0
        }

        // Draw Branch Marker
        PointAnnotation(
            point = branchPoint
        ) {
            iconImage = IconImage(branchMarker)
            iconSize = 1.0
        }

        // Draw Delivery Marker
        PointAnnotation(
            point = deliveryPoint
        ) {
            iconImage = IconImage(deliveryMarker)
            iconSize = 1.0
        }
    }
}

@Composable
fun TrackingStatusPanel(order: Order) {
    val etaText = when (order.orderStatus) {
        OrderStatus.GRACE_PERIOD -> "Preparing to confirm..."
        OrderStatus.PENDING -> "Confirming Order..."
        OrderStatus.PREPARING -> "Arriving in 25-30 min"
        OrderStatus.OUT_FOR_DELIVERY -> "Arriving in 10-15 min"
        OrderStatus.DELIVERED -> "Delivered"
    }

    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = etaText,
            style = MaterialTheme.typography.headlineSmall,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Order #${order.orderId.takeLast(6).uppercase()}",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Status Stepper
        StatusStepper(currentStatus = order.orderStatus)
        
        Spacer(modifier = Modifier.height(24.dp))
        
        // Conditional Driver Details
        if (order.orderStatus == OrderStatus.OUT_FOR_DELIVERY) {
            DriverDetailsCard()
        }
    }
}

@Composable
fun StatusStepper(currentStatus: OrderStatus) {
    val steps = listOf(
        Pair(OrderStatus.PENDING, "Order Placed"),
        Pair(OrderStatus.PREPARING, "Preparing"),
        Pair(OrderStatus.OUT_FOR_DELIVERY, "Out for Delivery"),
        Pair(OrderStatus.DELIVERED, "Delivered")
    )
    
    val currentIndex = steps.indexOfFirst { it.first == currentStatus }
    
    Column(modifier = Modifier.fillMaxWidth()) {
        steps.forEachIndexed { index, step ->
            val isActive = index == currentIndex
            val isCompleted = index <= currentIndex
            val color = if (isCompleted) VAL_BRAND_PRIMARY else Color(0xFFE0E0E0)
            val textColor = if (isActive) TextPrimary else TextSecondary
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Node
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .background(if (isActive) VAL_BRAND_PRIMARY.copy(alpha = 0.2f) else Color.Transparent, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(12.dp)
                            .background(color, CircleShape)
                    )
                }
                
                Spacer(modifier = Modifier.width(16.dp))
                
                // Label
                Text(
                    text = step.second,
                    style = MaterialTheme.typography.bodyLarge,
                    color = textColor,
                    fontWeight = if (isActive) FontWeight.Bold else FontWeight.Normal
                )
            }
            
            // Connecting Line (except for last item)
            if (index < steps.size - 1) {
                Box(
                    modifier = Modifier
                        .padding(start = 11.dp)
                        .width(2.dp)
                        .height(24.dp)
                        .background(if (index < currentIndex) VAL_BRAND_PRIMARY else Color(0xFFE0E0E0))
                )
            }
        }
    }
}

@Composable
fun DriverDetailsCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = VAL_BACKGROUND),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFEEEEEE)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.Person, contentDescription = "Driver", tint = Color.Gray)
            }
            Spacer(modifier = Modifier.width(16.dp))
            Column {
                Text(
                    text = "Driver: Ali",
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Yamaha YBR 125 • ABC-123",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
        }
    }
}

// Utility to generate a map pin bitmap
private fun createMapPinBitmap(color: Int): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 3f, paint)
    return bitmap
}

// Helper to generate a curved path between two points
fun generateCurvedPath(start: Point, end: Point, numPoints: Int = 50): List<Point> {
    val points = mutableListOf<Point>()
    val startLat = start.latitude()
    val startLng = start.longitude()
    val endLat = end.latitude()
    val endLng = end.longitude()
    
    val midLat = (startLat + endLat) / 2.0
    val midLng = (startLng + endLng) / 2.0
    
    val dx = endLng - startLng
    val dy = endLat - startLat
    
    val offsetFactor = 0.2 // Curve magnitude
    val ctrlLat = midLat - dx * offsetFactor
    val ctrlLng = midLng + dy * offsetFactor
    
    for (i in 0..numPoints) {
        val t = i / numPoints.toDouble()
        val u = 1 - t
        
        val lat = u * u * startLat + 2 * u * t * ctrlLat + t * t * endLat
        val lng = u * u * startLng + 2 * u * t * ctrlLng + t * t * endLng
        
        points.add(Point.fromLngLat(lng, lat))
    }
    return points
}
