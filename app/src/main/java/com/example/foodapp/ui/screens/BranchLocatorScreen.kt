package com.example.foodapp.ui.screens

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.google.android.gms.location.LocationServices
import com.google.android.gms.location.Priority
import com.google.android.gms.tasks.CancellationTokenSource
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

import com.example.foodapp.data.models.Branch
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.BranchLocatorViewModel
import com.mapbox.geojson.Point
import com.mapbox.maps.CameraOptions
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotation
import com.mapbox.maps.extension.compose.annotation.generated.PointAnnotationGroup
import com.mapbox.maps.extension.compose.annotation.IconImage
import com.example.foodapp.ui.components.ExpressiveFullScreenLoader

enum class LocationPermissionStatus {
    PENDING_CHECK, GRANTED, DENIED
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchLocatorScreen(
    onNavigateBack: () -> Unit,
    onBranchSelected: (String) -> Unit, // triggers global setActiveBranch
    viewModel: BranchLocatorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    
    // Initial camera position centered over Lahore, Pakistan
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(74.3587, 31.5204))
            zoom(11.0)
        }
    }
    
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val fusedLocationClient = remember { LocationServices.getFusedLocationProviderClient(context) }
    
    var permissionStatus by remember { mutableStateOf(LocationPermissionStatus.PENDING_CHECK) }
    var isLocatingUser by remember { mutableStateOf(false) }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            permissionStatus = LocationPermissionStatus.GRANTED
            isLocatingUser = true
            fetchLocationWithFallback(
                context = context,
                fusedLocationClient = fusedLocationClient,
                onSuccess = { location ->
                    isLocatingUser = false
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                    android.widget.Toast.makeText(context, "Location found!", android.widget.Toast.LENGTH_SHORT).show()
                },
                onFailure = { e ->
                    isLocatingUser = false
                    android.widget.Toast.makeText(context, "Location Error: ${e.message}", android.widget.Toast.LENGTH_LONG).show()
                }
            )
        } else {
            permissionStatus = LocationPermissionStatus.DENIED
        }
    }

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                val hasFine = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                val hasCoarse = androidx.core.content.ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED
                if (hasFine || hasCoarse) {
                    permissionStatus = LocationPermissionStatus.GRANTED
                    isLocatingUser = true
                    fetchLocationWithFallback(
                        context = context,
                        fusedLocationClient = fusedLocationClient,
                        onSuccess = { location ->
                            isLocatingUser = false
                            viewModel.updateUserLocation(location.latitude, location.longitude)
                        },
                        onFailure = {
                            isLocatingUser = false
                        }
                    )
                } else if (permissionStatus == LocationPermissionStatus.GRANTED) {
                    permissionStatus = LocationPermissionStatus.DENIED
                }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // Sync Map Camera to Selection
    LaunchedEffect(Unit) {
        viewModel.mapCameraEvents.collect { point ->
            mapViewportState.flyTo(
                CameraOptions.Builder()
                    .center(point)
                    .zoom(14.0)
                    .build()
            )
        }
    }

    // Sync Map Camera to User Location & Nearest Branch
    LaunchedEffect(uiState.userLocation) {
        val userLoc = uiState.userLocation
        if (userLoc != null && uiState.branches.isNotEmpty()) {
            val nearest = uiState.branches.find { it.branchId == uiState.recommendedBranchId } ?: uiState.branches.first()
            val branchLoc = nearest.location
            if (branchLoc != null) {
                val midLat = (userLoc.latitude + branchLoc.latitude) / 2.0
                val midLng = (userLoc.longitude + branchLoc.longitude) / 2.0
                val midPoint = Point.fromLngLat(midLng, midLat)
                
                val distanceKm = (uiState.branchDistances[nearest.branchId] ?: 5000f) / 1000f
                val zoomLevel = when {
                    distanceKm < 2f -> 13.0
                    distanceKm < 5f -> 12.0
                    distanceKm < 10f -> 11.0
                    distanceKm < 20f -> 10.0
                    else -> 9.0
                }
                
                mapViewportState.flyTo(
                    CameraOptions.Builder()
                        .center(midPoint)
                        .zoom(zoomLevel)
                        .build()
                )
            }
        }
    }

    // Sync List Scroll to Selection
    LaunchedEffect(uiState.selectedBranchId) {
        val selectedId = uiState.selectedBranchId
        if (selectedId != null && uiState.branches.isNotEmpty()) {
            val index = uiState.branches.indexOfFirst { it.branchId == selectedId }
            if (index >= 0) {
                listState.animateScrollToItem(index)
            }
        }
    }

    // Marker Bitmaps
    val activeMarker = remember { createMarkerBitmap(VAL_BRAND_PRIMARY.toArgb()) }
    val inactiveMarker = remember { createMarkerBitmap(VAL_SURFACE_DARK.toArgb()) }
    val userMarker = remember { createUserMarkerBitmap() }

    Column(modifier = modifier.fillMaxSize().background(VAL_BACKGROUND)) {
        // Top App Bar (Mock Search)
        TopAppBar(
            title = {
                OutlinedTextField(
                    value = "",
                    onValueChange = {},
                    placeholder = { Text("Search area...", color = Color.Gray) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = Color.Gray) },
                    modifier = Modifier.fillMaxWidth().height(50.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = VAL_BACKGROUND,
                        unfocusedContainerColor = VAL_BACKGROUND,
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent
                    ),
                    shape = RoundedCornerShape(25.dp)
                )
            },
            navigationIcon = {
                IconButton(onClick = onNavigateBack) {
                    Icon(Icons.Default.Close, contentDescription = "Close", tint = VAL_SURFACE_DARK)
                }
            },
            actions = {
                if (permissionStatus != LocationPermissionStatus.DENIED) {
                    TextButton(onClick = {
                        locationPermissionRequest.launch(
                            arrayOf(
                                Manifest.permission.ACCESS_FINE_LOCATION,
                                Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }) {
                        Text("Locate Me", color = VAL_BRAND_PRIMARY)
                    }
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.White
            ),
            windowInsets = WindowInsets(0.dp)
        )

        // Split Layout
        // Top Half: Mapbox
        Box(modifier = Modifier.fillMaxWidth().weight(1f)) {
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            ) {
                uiState.branches.forEach { branch ->
                    branch.location?.let { geoPoint ->
                        val isSelected = uiState.selectedBranchId == branch.branchId
                        PointAnnotation(
                            point = Point.fromLngLat(geoPoint.longitude, geoPoint.latitude),
                            onClick = {
                                viewModel.selectBranch(branch.branchId)
                                true
                            }
                        ) {
                            iconImage = IconImage(if (isSelected) activeMarker else inactiveMarker)
                            iconSize = if (isSelected) 1.3 else 0.9
                        }
                    }
                }
                
                // User Location Marker
                uiState.userLocation?.let { loc ->
                    PointAnnotation(
                        point = Point.fromLngLat(loc.longitude, loc.latitude)
                    ) {
                        iconImage = IconImage(userMarker)
                        iconSize = 1.0
                    }
                }
            }
        }

        // Bottom Half: Branch List
        Box(modifier = Modifier.fillMaxWidth().weight(1f).background(VAL_BACKGROUND)) {
            if (uiState.isLoading) {
                CircularProgressIndicator(
                    color = VAL_BRAND_PRIMARY,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else if (uiState.branches.isEmpty()) {
                Text(
                    text = "No stores found",
                    style = MaterialTheme.typography.titleLarge,
                    color = VAL_SURFACE_DARK,
                    modifier = Modifier.align(Alignment.Center)
                )
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (permissionStatus == LocationPermissionStatus.DENIED) {
                        item {
                            PermissionRecoveryState()
                        }
                    }
                    
                    items(uiState.branches) { branch ->
                        val distance = uiState.branchDistances[branch.branchId]
                        val isRecommended = branch.branchId == uiState.recommendedBranchId
                        BranchCard(
                            branch = branch,
                            isSelected = branch.branchId == uiState.selectedBranchId,
                            distance = distance,
                            isRecommended = isRecommended,
                            onClick = { viewModel.selectBranch(branch.branchId) },
                            onChoose = { onBranchSelected(branch.branchId) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun BranchCard(
    branch: Branch,
    isSelected: Boolean,
    distance: Float?,
    isRecommended: Boolean,
    onClick: () -> Unit,
    onChoose: () -> Unit
) {
    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = branch.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VAL_SURFACE_DARK
                )
                Text(
                    text = if (branch.isOpen) "Open" else "Closed",
                    style = MaterialTheme.typography.labelMedium,
                    color = if (branch.isOpen) VAL_BRAND_PRIMARY else Color(0xFFD60000),
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(4.dp))
            if (branch.address.isNotEmpty()) {
                Text(
                    text = branch.address,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
            }
            if (branch.operatingHours.isNotEmpty()) {
                Text(
                    text = branch.operatingHours,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (distance != null) {
                    Text(
                        text = String.format("%.1f km", distance / 1000),
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (isRecommended) {
                    Surface(
                        color = Color(0xFFE8F5E9),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = "Recommended",
                            color = Color(0xFF00704A),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            if (isSelected) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = onChoose,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Choose this store", color = Color.White, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Utility to generate a map pin bitmap
fun createMarkerBitmap(color: Int): Bitmap {
    val size = 64
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        this.color = color
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    // Add a crisp white border for contrast against the map
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 6f
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 3f, paint)
    return bitmap
}

fun createUserMarkerBitmap(): Bitmap {
    val size = 48
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint().apply {
        color = android.graphics.Color.parseColor("#4285F4") // Google Blue
        isAntiAlias = true
    }
    canvas.drawCircle(size / 2f, size / 2f, size / 2f, paint)
    paint.color = android.graphics.Color.WHITE
    paint.style = Paint.Style.STROKE
    paint.strokeWidth = 8f
    canvas.drawCircle(size / 2f, size / 2f, (size / 2f) - 4f, paint)
    return bitmap
}

@Composable
fun PermissionRecoveryState(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF8E1)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Warning, contentDescription = null, tint = VAL_BRAND_PRIMARY)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Location Access Disabled",
                    style = MaterialTheme.typography.headlineSmall, // Semantic H4 styling
                    color = VAL_SURFACE_DARK,
                    fontWeight = FontWeight.Bold
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Please enable location permission in your device settings to find branches near you.",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.DarkGray
            )
            Spacer(modifier = Modifier.height(12.dp))
            OutlinedButton(
                onClick = {
                    val intent = android.content.Intent(
                        android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                        android.net.Uri.fromParts("package", context.packageName, null)
                    )
                    context.startActivity(intent)
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Open Settings", color = VAL_BRAND_PRIMARY, fontWeight = FontWeight.Bold)
            }
        }
    }
}
@SuppressLint("MissingPermission")
private fun fetchLocationWithFallback(
    context: android.content.Context,
    fusedLocationClient: com.google.android.gms.location.FusedLocationProviderClient,
    onSuccess: (android.location.Location) -> Unit,
    onFailure: (Exception) -> Unit
) {
    try {
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, CancellationTokenSource().token)
            .addOnSuccessListener { location ->
                if (location != null) {
                    onSuccess(location)
                } else {
                    tryNativeLocationManager(context, onSuccess, onFailure)
                }
            }
            .addOnFailureListener {
                tryNativeLocationManager(context, onSuccess, onFailure)
            }
    } catch (e: Exception) {
        tryNativeLocationManager(context, onSuccess, onFailure)
    }
}

@SuppressLint("MissingPermission")
private fun tryNativeLocationManager(
    context: android.content.Context,
    onSuccess: (android.location.Location) -> Unit,
    onFailure: (Exception) -> Unit
) {
    try {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        val isGpsEnabled = locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
        val isNetworkEnabled = locationManager.isProviderEnabled(android.location.LocationManager.NETWORK_PROVIDER)

        if (!isGpsEnabled && !isNetworkEnabled) {
            onFailure(Exception("Please turn on GPS in your Android Quick Settings"))
            return
        }

        val provider = if (isGpsEnabled) android.location.LocationManager.GPS_PROVIDER else android.location.LocationManager.NETWORK_PROVIDER

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
            locationManager.getCurrentLocation(
                provider,
                null,
                androidx.core.content.ContextCompat.getMainExecutor(context)
            ) { location ->
                if (location != null) onSuccess(location) else onFailure(Exception("Location manager returned null"))
            }
        } else {
            val lastKnown = locationManager.getLastKnownLocation(provider)
            if (lastKnown != null) onSuccess(lastKnown) else onFailure(Exception("Unable to get last known location"))
        }
    } catch (e: Exception) {
        onFailure(e)
    }
}
