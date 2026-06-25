package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Address
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.components.TextInput
import com.example.foodapp.ui.state.AddressViewModel
import com.example.foodapp.ui.state.AuthState
import com.google.firebase.firestore.GeoPoint
import com.mapbox.geojson.Point
import com.mapbox.maps.extension.compose.MapboxMap
import com.mapbox.maps.extension.compose.animation.viewport.rememberMapViewportState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressMapPickerScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    branchLocation: com.google.firebase.firestore.GeoPoint? = null,
    onNavigateToAuth: () -> Unit = {},
    onAddressSaved: (com.example.foodapp.data.models.Address) -> Unit = {},
    viewModel: AddressViewModel = viewModel()
) {
    val user = (authState as? AuthState.Authenticated)?.user

    // Default to Lahore, Pakistan
    val mapViewportState = rememberMapViewportState {
        setCameraOptions {
            center(Point.fromLngLat(74.3587, 31.5204))
            zoom(14.0)
        }
    }

    var showDetailsSheet by remember { mutableStateOf(false) }
    var showDistanceErrorSheet by remember { mutableStateOf(false) }
    var selectedGeoPoint by remember { mutableStateOf<GeoPoint?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Choose Location", color = TextPrimary) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceWhite)
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Full Screen Map
            MapboxMap(
                modifier = Modifier.fillMaxSize(),
                mapViewportState = mapViewportState
            )

            // Fixed Center Pin
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Pin",
                    tint = VAL_BRAND_PRIMARY,
                    modifier = Modifier.size(48.dp).offset(y = (-24).dp)
                )
            }

            // Bottom Action Button
            if (!showDetailsSheet) {
                Box(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .padding(16.dp)
                ) {
                    PrimaryButton(
                        onClick = {
                            val center = mapViewportState.cameraState?.center
                            if (center != null) {
                                var dist = 0f
                                if (branchLocation != null) {
                                    val results = FloatArray(1)
                                    android.location.Location.distanceBetween(
                                        center.latitude(), center.longitude(),
                                        branchLocation.latitude, branchLocation.longitude,
                                        results
                                    )
                                    dist = results[0]
                                }
                                if (dist > 25000f) {
                                    showDistanceErrorSheet = true
                                } else {
                                    selectedGeoPoint = GeoPoint(center.latitude(), center.longitude())
                                    showDetailsSheet = true
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text("Confirm Location")
                    }
                }
            }
        }

        // Address Details Bottom Sheet
        if (showDetailsSheet) {
            selectedGeoPoint?.let { geoPoint ->
                ModalBottomSheet(
                    onDismissRequest = { showDetailsSheet = false },
                    containerColor = SurfaceWhite,
                    shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
                ) {
                    AddressDetailsSheetContent(
                        location = geoPoint,
                        viewModel = viewModel,
                        isGuest = user == null,
                        onNavigateToAuth = {
                            showDetailsSheet = false
                            onNavigateToAuth()
                        },
                        onSave = { newAddress ->
                            if (user != null) {
                                viewModel.saveAddress(user.uid, newAddress) {
                                    showDetailsSheet = false
                                    onAddressSaved(newAddress)
                                    onNavigateBack() // Pop back to Address List
                                }
                            } else {
                                // Guest RAM save
                                showDetailsSheet = false
                                onAddressSaved(newAddress)
                                onNavigateBack()
                            }
                        },
                        onCancel = { showDetailsSheet = false }
                    )
                }
            }
        }

        if (showDistanceErrorSheet) {
            ModalBottomSheet(
                onDismissRequest = { showDistanceErrorSheet = false },
                containerColor = SurfaceWhite,
                shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Text("Out of Delivery Zone", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.error)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text("We aren't delivering to this area just yet! Choose another location or select Pick-up instead.", style = MaterialTheme.typography.bodyLarge, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                    Spacer(modifier = Modifier.height(32.dp))
                    PrimaryButton(onClick = { showDistanceErrorSheet = false }, modifier = Modifier.fillMaxWidth()) {
                        Text("Change Location")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddressDetailsSheetContent(
    location: GeoPoint,
    viewModel: AddressViewModel,
    isGuest: Boolean,
    onNavigateToAuth: () -> Unit,
    onSave: (Address) -> Unit,
    onCancel: () -> Unit
) {
    var label by remember { mutableStateOf("Home") }
    var streetAddress by remember { mutableStateOf("") }
    var city by remember { mutableStateOf("") }
    var postalCode by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var instructions by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var validationError by remember { mutableStateOf<String?>(null) }

    val labels = listOf("Home", "Work", "Other")

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp)
            .padding(bottom = 32.dp)
    ) {
        Text(
            text = "Address Details",
            style = MaterialTheme.typography.titleLarge,
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Segmented Labels
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            labels.forEach { l ->
                FilterChip(
                    selected = label == l,
                    onClick = { label = l },
                    label = { Text(l) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = VAL_BRAND_PRIMARY.copy(alpha = 0.1f),
                        selectedLabelColor = VAL_BRAND_PRIMARY
                    )
                )
            }
        }
        Spacer(modifier = Modifier.height(16.dp))

        AnimatedVisibility(
            visible = validationError != null,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Text(
                text = validationError ?: "",
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        if (isGuest) {
            Surface(
                color = VAL_BRAND_PRIMARY.copy(alpha = 0.1f),
                shape = RoundedCornerShape(12.dp),
                onClick = onNavigateToAuth,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            ) {
                Row(modifier = Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.LocationOn, contentDescription = null, tint = VAL_BRAND_PRIMARY)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Tired of re-typing?", fontWeight = FontWeight.Bold, color = VAL_BRAND_PRIMARY)
                        Text("Secure your account in 2 taps to save this location forever.", style = MaterialTheme.typography.bodySmall, color = TextPrimary)
                    }
                }
            }
        }

        TextInput(
            label = "Complete Street Address",
            value = streetAddress,
            onValueChange = { streetAddress = it; validationError = null },
            placeholder = "e.g. 12B, Main Boulevard, Phase 6"
        )
        Spacer(modifier = Modifier.height(16.dp))

        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            TextInput(
                label = "City",
                value = city,
                onValueChange = { city = it; validationError = null },
                placeholder = "e.g. Lahore",
                modifier = Modifier.weight(1f)
            )
            TextInput(
                label = "Postal Code",
                value = postalCode,
                onValueChange = { postalCode = it; validationError = null },
                placeholder = "e.g. 54000",
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(modifier = Modifier.height(16.dp))

        TextInput(
            label = "Phone Number",
            value = phoneNumber,
            onValueChange = { phoneNumber = it; validationError = null },
            placeholder = "e.g. 03001234567"
        )
        Spacer(modifier = Modifier.height(16.dp))

        TextInput(
            label = "Delivery Instructions (Optional)",
            value = instructions,
            onValueChange = { instructions = it },
            placeholder = "e.g. Leave at the front gate"
        )
        Spacer(modifier = Modifier.height(16.dp))

        // Default Toggle
        if (!isGuest) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column {
                    Text("Save as Default", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                    Text("Use this address automatically for checkout", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                }
                Switch(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = SwitchDefaults.colors(checkedTrackColor = VAL_BRAND_PRIMARY)
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        } else {
            Spacer(modifier = Modifier.height(16.dp))
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(onClick = onCancel) {
                Text("Cancel", color = TextSecondary)
            }
            Spacer(modifier = Modifier.width(16.dp))
            PrimaryButton(
                onClick = {
                    val error = viewModel.validateAddress(streetAddress, city, postalCode, phoneNumber)
                    if (error != null) {
                        validationError = error
                        return@PrimaryButton
                    }
                    onSave(
                        Address(
                            label = label,
                            streetAddress = streetAddress,
                            city = city,
                            postalCode = postalCode,
                            phoneNumber = phoneNumber,
                            deliveryInstructions = instructions,
                            location = location,
                            isDefault = isDefault
                        )
                    )
                },
                enabled = streetAddress.isNotBlank()
            ) {
                Text("Save Address")
            }
        }
    }
}
