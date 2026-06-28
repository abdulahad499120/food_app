package com.example.foodapp.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Order
import com.example.foodapp.ui.state.RiderSessionViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun JobPoolScreen(
    onJobClaimed: (String) -> Unit,
    onNavigateToHistory: () -> Unit,
    viewModel: RiderSessionViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var hasLocationPermission by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val permissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true || 
                                permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (!hasLocationPermission) {
            scope.launch { snackbarHostState.showSnackbar("Location permission is required to accept jobs") }
        }
    }

    LaunchedEffect(Unit) {
        val permissionsToRequest = mutableListOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
        }
        if (!hasLocationPermission) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    Scaffold(
        containerColor = Color(0xFFFFF8F7),
        topBar = {
            TopAppBar(
                title = { Text("Rider Connect", fontWeight = FontWeight.ExtraBold, color = Color(0xFFB80049)) },
                actions = {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 8.dp)) {
                        Text(
                            text = if (uiState.isOnline) "ONLINE" else "OFFLINE",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (uiState.isOnline) Color(0xFF006673) else Color.Gray,
                            modifier = Modifier.padding(end = 8.dp)
                        )
                        Switch(
                            checked = uiState.isOnline,
                            onCheckedChange = { viewModel.toggleOnlineStatus(it) },
                            colors = SwitchDefaults.colors(
                                checkedThumbColor = Color.White,
                                checkedTrackColor = Color(0xFF006673),
                                uncheckedThumbColor = Color.LightGray,
                                uncheckedTrackColor = Color.DarkGray
                            )
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFFFFFFFF),
                )
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) }
    ) { padding ->
        if (!uiState.isOnline) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Default.Person, contentDescription = "Offline", modifier = Modifier.size(64.dp), tint = Color.Gray)
                    Spacer(Modifier.height(16.dp))
                    Text("You are offline", style = MaterialTheme.typography.titleLarge)
                    Text("Go online to start receiving jobs", color = Color.Gray)
                }
            }
        } else if (uiState.isLoading && uiState.availableJobs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else if (uiState.availableJobs.isEmpty()) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                Text("No jobs currently available.", style = MaterialTheme.typography.titleMedium, color = Color(0xFF5B3F43))
            }
        } else {
            Column(modifier = Modifier.padding(padding).fillMaxSize()) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Job Pool",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color(0xFF28171A),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "New requests in your area",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF5B3F43)
                    )
                }
                
                LazyColumn(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    contentPadding = PaddingValues(bottom = 16.dp)
                ) {
                    items(uiState.availableJobs, key = { it.orderId }) { job ->
                        JobCard(
                            order = job,
                            onClaim = {
                                if (!hasLocationPermission) {
                                    scope.launch { snackbarHostState.showSnackbar("Please grant location permission first") }
                                    val permissionsToRequest = mutableListOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                        permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
                                    }
                                    permissionLauncher.launch(permissionsToRequest.toTypedArray())
                                } else {
                                    viewModel.claimJob(
                                        orderId = job.orderId,
                                        onSuccess = { onJobClaimed(job.orderId) },
                                        onError = { msg ->
                                            scope.launch {
                                                snackbarHostState.showSnackbar(msg)
                                            }
                                        }
                                    )
                                }
                            }
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun JobCard(order: Order, onClaim: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFFFFFFF)), // CreamWhite
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            // Top Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                // Status Pill
                Box(
                    modifier = Modifier
                        .background(Color(0xFFFFD9DE), RoundedCornerShape(percent = 50))
                        .padding(horizontal = 12.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = "Available",
                        color = Color(0xFFB80049),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                // Order ID
                Text(
                    text = "#${order.orderId.takeLast(6).uppercase()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color(0xFF28171A),
                    fontWeight = FontWeight.Bold
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Location Section
            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Location",
                    tint = Color(0xFF006673),
                    modifier = Modifier.size(20.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "Drop-off",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5B3F43)
                    )
                    Text(
                        text = order.generalSector ?: "Local Sector",
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF28171A),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider(color = Color(0xFFFADBDE)) // surface-variant
            Spacer(modifier = Modifier.height(16.dp))
            
            // Items Section
            val itemSummary = order.itemSummary ?: "Items Info Unavailable"
            val itemsList = itemSummary.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (itemsList.isEmpty()) {
                    Box(
                        modifier = Modifier
                            .background(Color(0xFFFFE9EA), RoundedCornerShape(percent = 50))
                            .padding(horizontal = 12.dp, vertical = 6.dp)
                    ) {
                        Text(
                            text = itemSummary,
                            style = MaterialTheme.typography.labelMedium,
                            color = Color(0xFF28171A)
                        )
                    }
                } else {
                    itemsList.take(2).forEach { item ->
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFFFE9EA), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = item,
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF28171A)
                            )
                        }
                    }
                    if (itemsList.size > 2) {
                         Box(
                            modifier = Modifier
                                .background(Color(0xFFFFE9EA), RoundedCornerShape(percent = 50))
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "+${itemsList.size - 2}",
                                style = MaterialTheme.typography.labelMedium,
                                color = Color(0xFF28171A)
                            )
                        }
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action Button
            Button(
                onClick = onClaim,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(48.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFE91E63)), // BerryPink
                shape = RoundedCornerShape(50)
            ) {
                Row(
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("ACCEPT JOB", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ArrowForward,
                        contentDescription = "Accept",
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
