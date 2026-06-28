package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.LocalShipping
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Route
import androidx.compose.material.icons.filled.Payments
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Order
import com.example.foodapp.ui.state.OrderHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    onNavigateBack: (() -> Unit)? = null,
    viewModel: OrderHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        containerColor = Color(0xFFFFF8F7), // Surface
        topBar = {
            TopAppBar(
                title = { Text("Delivery History", color = Color(0xFFB80049), fontWeight = FontWeight.ExtraBold) }, // Primary
                navigationIcon = {
                    if (onNavigateBack != null) {
                        IconButton(onClick = onNavigateBack) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color(0xFFB80049))
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.White)
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // Summary Cards
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Deliveries Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFFFF0F1)), // SurfaceContainerLow
                    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4BDC2).copy(alpha = 0.3f))
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.LocalShipping,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.TopEnd).size(64.dp).alpha(0.1f).offset(x = 16.dp, y = (-16).dp),
                            tint = Color(0xFFB80049) // Primary
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Route, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFF5B3F43)) // OnSurfaceVariant
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "TOTAL DELIVERIES",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFF5B3F43),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "${uiState.pastOrders.size}",
                                style = MaterialTheme.typography.displayMedium,
                                color = Color(0xFF28171A), // OnSurface
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }

                // Earnings Card
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .height(140.dp),
                    shape = RoundedCornerShape(12.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFFB80049)), // Primary
                    elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                ) {
                    Box(modifier = Modifier.fillMaxSize().padding(16.dp)) {
                        Icon(
                            imageVector = Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            modifier = Modifier.align(Alignment.BottomEnd).size(100.dp).alpha(0.2f).offset(x = 16.dp, y = 16.dp),
                            tint = Color.White
                        )
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(Icons.Default.Payments, contentDescription = null, modifier = Modifier.size(16.dp), tint = Color(0xFFFFB2BE)) // PrimaryFixedDim
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    "TOTAL EARNINGS",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = Color(0xFFFFB2BE),
                                    fontWeight = FontWeight.Bold
                                )
                            }
                            Text(
                                text = "Rs. ${uiState.totalEarnings.toInt()}",
                                style = MaterialTheme.typography.headlineMedium, // Closer to display size in available space
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }
                }
            }

            Text(
                text = "Order History",
                style = MaterialTheme.typography.headlineSmall,
                color = Color(0xFF28171A), // OnSurface
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )

            if (uiState.isLoading) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color(0xFFB80049))
                }
            } else if (uiState.pastOrders.isEmpty()) {
                Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No deliveries completed yet.", color = Color(0xFF5B3F43))
                }
            } else {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(uiState.pastOrders, key = { it.orderId }) { order ->
                        HistoryOrderCard(order)
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryOrderCard(order: Order) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0xFFE4BDC2).copy(alpha = 0.2f))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        text = formatTimestamp(order.timestamp?.time),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color(0xFF28171A), // OnSurface
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "ID: ORD-${order.orderId.takeLast(4).uppercase()}",
                        style = MaterialTheme.typography.labelMedium,
                        color = Color(0xFF5B3F43) // OnSurfaceVariant
                    )
                }
                Text(
                    text = "Rs. ${order.deliveryFee?.toInt() ?: 150}", // Fallback fee
                    style = MaterialTheme.typography.titleLarge,
                    color = Color(0xFFE91E63), // BerryPink
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(8.dp))
            HorizontalDivider(color = Color(0xFFE4BDC2).copy(alpha = 0.3f))
            Spacer(modifier = Modifier.height(8.dp))

            Row(verticalAlignment = Alignment.Top) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = null,
                    tint = Color(0xFF006673), // Tertiary
                    modifier = Modifier.size(16.dp).padding(top = 2.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "DROPOFF",
                        style = MaterialTheme.typography.labelSmall,
                        color = Color(0xFF5B3F43),
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = order.deliveryAddress.streetAddress.ifEmpty { order.generalSector ?: "Local Sector" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color(0xFF28171A)
                    )
                }
            }
        }
    }
}

fun formatTimestamp(timestamp: Long?): String {
    if (timestamp == null || timestamp == 0L) return "Unknown Date"
    val sdf = SimpleDateFormat("MMM d, h:mm a", Locale.getDefault())
    return sdf.format(Date(timestamp))
}
