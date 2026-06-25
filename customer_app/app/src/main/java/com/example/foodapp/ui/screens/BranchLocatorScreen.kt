package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import com.example.foodapp.utils.bounceClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Search
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
import android.Manifest
import android.annotation.SuppressLint
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.ui.platform.LocalContext
import com.example.foodapp.utils.UniversalLocationEngine
import kotlinx.coroutines.launch
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Store
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

// GMS removed

enum class LocationPermissionStatus {
    PENDING_CHECK, GRANTED, DENIED, UNAVAILABLE
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BranchLocatorScreen(
    onNavigateBack: () -> Unit,
    onBranchSelected: (String) -> Unit, // triggers global setActiveBranch
    onSkip: () -> Unit = {},
    onSwitchToDelivery: () -> Unit = {},
    viewModel: BranchLocatorViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    
    // Initial camera position centered over Lahore, Pakistan
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(74.3587, 31.5204))
            zoom(11.0)
        }
    }
    
    val listState = rememberLazyListState()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    var permissionStatus by remember { mutableStateOf(LocationPermissionStatus.PENDING_CHECK) }
    var isLocatingUser by remember { mutableStateOf(false) }
    var showSkipDialog by remember { mutableStateOf(false) }
    var showGpsPrompt by remember { mutableStateOf(false) }

    fun isGpsEnabled(context: android.content.Context): Boolean {
        val locationManager = context.getSystemService(android.content.Context.LOCATION_SERVICE) as android.location.LocationManager
        return locationManager.isProviderEnabled(android.location.LocationManager.GPS_PROVIDER)
    }

    val locationPermissionRequest = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.getOrDefault(Manifest.permission.ACCESS_FINE_LOCATION, false) ||
                      permissions.getOrDefault(Manifest.permission.ACCESS_COARSE_LOCATION, false)
        if (granted) {
            permissionStatus = LocationPermissionStatus.GRANTED
            isLocatingUser = true
            scope.launch {
                val location = UniversalLocationEngine.getCurrentLocation(context)
                isLocatingUser = false
                if (location != null) {
                    viewModel.updateUserLocation(location.latitude, location.longitude)
                    android.widget.Toast.makeText(context, "Location found!", android.widget.Toast.LENGTH_SHORT).show()
                } else {
                    android.widget.Toast.makeText(context, "Location Error: Unable to determine location", android.widget.Toast.LENGTH_LONG).show()
                }
            }
        } else {
            permissionStatus = LocationPermissionStatus.DENIED
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
    
    if (showSkipDialog) {
        com.example.foodapp.ui.components.LocationPrerequisiteModal(
            onDismiss = { showSkipDialog = false },
            onChooseStore = { showSkipDialog = false },
            onProceedAsGuest = { 
                showSkipDialog = false
                onSkip()
            }
        )
    }

    if (showGpsPrompt) {
        AlertDialog(
            onDismissRequest = { showGpsPrompt = false },
            title = { Text("Location Services Disabled", fontWeight = FontWeight.Bold) },
            text = { Text("Please turn on your device's GPS/Location Services to use your current location.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGpsPrompt = false
                        val intent = android.content.Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS)
                        context.startActivity(intent)
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
                ) {
                    Text("Open Settings", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showGpsPrompt = false }) {
                    Text("Cancel", color = Color.Gray)
                }
            }
        )
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
    val activeMarker = remember { createMarkerBitmap(context, VAL_BRAND_PRIMARY.toArgb(), "", com.ahad.foodapp.R.drawable.ic_store) }
    val inactiveMarker = remember { createMarkerBitmap(context, VAL_SURFACE_DARK.toArgb(), "", com.ahad.foodapp.R.drawable.ic_store) }
    val userMarker = remember { createUserMarkerBitmap() }

    Column(modifier = modifier.fillMaxSize().background(VAL_BACKGROUND)) {
        // Mode Selector Toggle (Pickup vs Delivery)
        Box(modifier = Modifier.fillMaxWidth().background(Color.White).padding(top = 8.dp, bottom = 4.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .align(Alignment.Center)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color(0xFFE0E0E0)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .bounceClick { onSwitchToDelivery() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Delivery", color = Color.Gray, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(20.dp))
                        .background(VAL_BRAND_PRIMARY)
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("Pickup", color = Color.White, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                }
            }
        }
        
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
                TextButton(onClick = { showSkipDialog = true }) {
                    Text("Skip", color = VAL_BRAND_PRIMARY, fontWeight = FontWeight.Bold)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White),
            windowInsets = WindowInsets(0.dp)
        )

        // Snackbar host (25km warnings float here, below the TopAppBar)
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier.align(Alignment.CenterHorizontally)
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

            // Locate Me FAB
            FloatingActionButton(
                onClick = {
                    if (!isGpsEnabled(context)) {
                        showGpsPrompt = true
                    } else {
                        locationPermissionRequest.launch(
                            arrayOf(
                                android.Manifest.permission.ACCESS_FINE_LOCATION,
                                android.Manifest.permission.ACCESS_COARSE_LOCATION
                            )
                        )
                    }
                },
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(16.dp),
                containerColor = Color.White,
                contentColor = VAL_BRAND_PRIMARY
            ) {
                Icon(Icons.Default.MyLocation, contentDescription = "Locate Me")
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
                Column(
                    modifier = Modifier.fillMaxSize().padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "No stores found",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = Color.Black,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Try searching another area or zooming in or out to get a better look.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.DarkGray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "You can change location permissions in the Settings app to search the area around you more easily.",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                }
            } else {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    if (permissionStatus == LocationPermissionStatus.DENIED) {
                        item { PermissionRecoveryState() }
                    }

                    items(uiState.branches) { branch ->
                        // Safety Net #1: Only show distance if GPS distances are available.
                        // When branchDistances is empty, no GPS fix yet — show nothing.
                        val distance = if (uiState.branchDistances.isNotEmpty()) {
                            uiState.branchDistances[branch.branchId]
                        } else null
                        val isRecommended = branch.branchId == uiState.recommendedBranchId
                        BranchCard(
                            branch = branch,
                            isSelected = branch.branchId == uiState.selectedBranchId,
                            distance = distance,
                            isRecommended = isRecommended,
                            onClick = { viewModel.selectBranch(branch.branchId) },
                            onChoose = {
                                // Safety Net #3: 25km sanity check — delegate to ViewModel.
                                // If branch is >25km, checkAndSelectBranch sets
                                // farBranchDialogBranchId and returns false; the AlertDialog
                                // below then picks it up and prompts the user.
                                val shouldProceed = viewModel.checkAndSelectBranch(branch.branchId)
                                if (shouldProceed) onBranchSelected(branch.branchId)
                            }
                        )
                    }
                }
            }
        }

        // Safety Net #3: AlertDialog shown when user picks a branch >25km away
        uiState.farBranchDialogBranchId?.let { farId ->
            val farBranch = uiState.branches.find { it.branchId == farId }
            val distKm = uiState.branchDistances[farId]?.let { it / 1000f }
            AlertDialog(
                onDismissRequest = { viewModel.dismissFarBranchDialog() },
                icon = {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = null,
                        tint = Color(0xFFFFA000),
                        modifier = Modifier.size(32.dp)
                    )
                },
                title = {
                    Text(
                        text = "Branch is far away",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                },
                text = {
                    val distText = if (distKm != null) String.format("%.1f km", distKm) else "far"
                    Text(
                        text = "${farBranch?.name ?: "This branch"} is $distText from your current location. " +
                               "Delivery or pickup will take significantly longer. Do you want to continue?",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    Button(
                        onClick = {
                            viewModel.confirmFarBranchSelection()
                            onBranchSelected(farId)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
                    ) {
                        Text("Yes, continue", color = Color.White)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissFarBranchDialog() }) {
                        Text("Cancel", color = VAL_BRAND_PRIMARY)
                    }
                }
            )
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
    // Safety Net #5: Gray out closed branches
    val isClosed = !branch.isOpen
    val cardAlpha = if (isClosed) 0.55f else 1f

    Card(
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isClosed) Color(0xFFF5F5F5) else Color.White
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .then(if (!isClosed) Modifier.bounceClick { onClick() } else Modifier)
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
                    color = if (isClosed) Color.Gray else VAL_SURFACE_DARK
                )
                // Status badge: red "Closed" or green "Open"
                Surface(
                    color = if (branch.isOpen) Color(0xFFE8F5E9) else Color(0xFFFFEBEE),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = if (branch.isOpen) "Open" else "Closed",
                        style = MaterialTheme.typography.labelMedium,
                        color = if (branch.isOpen) Color(0xFF2E7D32) else Color(0xFFD60000),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
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
                        color = if (isClosed) Color.LightGray else Color.Gray
                    )
                } else {
                    Spacer(modifier = Modifier.width(1.dp))
                }
                if (isRecommended && !isClosed) {
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

            if (isSelected && !isClosed) {
                Spacer(modifier = Modifier.height(16.dp))
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = VAL_BRAND_PRIMARY,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .bounceClick { onChoose() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "Choose this store",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            } else if (isSelected && isClosed) {
                // Show a grayed-out disabled button for closed branches
                Spacer(modifier = Modifier.height(16.dp))
                Button(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().height(48.dp),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Text("Currently Closed", fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// Utility to generate a map pin bitmap
fun createMarkerBitmap(context: android.content.Context, color: Int, text: String = "", iconResId: Int? = null): Bitmap {
    if (iconResId != null) {
        val drawable = androidx.core.content.ContextCompat.getDrawable(context, iconResId)
        if (drawable != null) {
            val bitmap = Bitmap.createBitmap(96, 96, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            
            // Draw circle background
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
            canvas.drawCircle(48f, 48f, 48f, paint)
            
            // Draw vector icon in center
            drawable.setBounds(20, 20, 76, 76)
            drawable.setTint(android.graphics.Color.WHITE)
            drawable.draw(canvas)
            return bitmap
        }
    }

    val sizePx = 48
    val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply { this.color = color }
    canvas.drawCircle(sizePx / 2f, sizePx / 2f, sizePx / 2f, paint)
    if (text.isNotEmpty()) {
        val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.color = android.graphics.Color.WHITE
            textSize = sizePx * 0.5f
            textAlign = Paint.Align.CENTER
            typeface = android.graphics.Typeface.DEFAULT_BOLD
        }
        val yPos = (sizePx / 2f) - ((textPaint.descent() + textPaint.ascent()) / 2f)
        canvas.drawText(text, sizePx / 2f, yPos, textPaint)
    }
    
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
// End of BranchLocatorScreen.kt
