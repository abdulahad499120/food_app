package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.foodapp.data.models.OrderStatus
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.LiveTrackingViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PickupStatusScreen(
    viewModel: LiveTrackingViewModel,
    onNavigateBack: () -> Unit,
    onNavigateToOrderComplete: (String) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    LaunchedEffect(uiState.orderStatus) {
        if (uiState.orderStatus == OrderStatus.DELIVERED) {
            uiState.currentOrderId?.let { orderId ->
                onNavigateToOrderComplete(orderId)
            }
        }
    }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = VAL_BACKGROUND
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header with Back Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onNavigateBack,
                    modifier = Modifier
                        .size(48.dp)
                        .shadow(4.dp, CircleShape)
                        .background(SurfaceWhite, CircleShape)
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                }
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = "Pickup Status",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            Spacer(modifier = Modifier.height(48.dp))

            // Main Status Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        imageVector = Icons.Default.Storefront,
                        contentDescription = "Pickup Store",
                        tint = VAL_BRAND_PRIMARY,
                        modifier = Modifier.size(64.dp)
                    )
                    
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    Text(
                        text = "Order #${uiState.currentOrderId?.takeLast(6)?.uppercase() ?: ""}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextSecondary
                    )
                    
                    Spacer(modifier = Modifier.height(8.dp))

                    val statusText = when (uiState.orderStatus) {
                        OrderStatus.GRACE_PERIOD -> "Order Placed"
                        OrderStatus.PENDING -> "Waiting for Confirmation"
                        OrderStatus.PREPARING -> "Preparing your order"
                        OrderStatus.READY_FOR_RIDER -> "Ready"
                        OrderStatus.RIDER_ASSIGNED -> "Ready"
                        OrderStatus.OUT_FOR_DELIVERY -> "Ready for Pickup"
                        OrderStatus.DELIVERED -> "Picked Up"
                        OrderStatus.CANCELLED -> "Order Cancelled"
                    }

                    Text(
                        text = statusText,
                        style = MaterialTheme.typography.headlineSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    if (uiState.orderStatus == OrderStatus.OUT_FOR_DELIVERY || uiState.orderStatus == OrderStatus.READY_FOR_RIDER || uiState.orderStatus == OrderStatus.RIDER_ASSIGNED) {
                        Surface(
                            color = Color(0xFF4CAF50).copy(alpha = 0.1f),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text(
                                text = "Please show your order number at the counter.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color(0xFF2E7D32),
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
                            )
                        }
                    } else if (uiState.orderStatus != OrderStatus.CANCELLED && uiState.orderStatus != OrderStatus.DELIVERED) {
                        CircularProgressIndicator(
                            color = VAL_BRAND_PRIMARY,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }
        }
    }
}
