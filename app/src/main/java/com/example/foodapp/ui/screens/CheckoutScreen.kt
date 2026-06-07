package com.example.foodapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.DividerColor
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.ui.components.ErrorStateView
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.components.TextInput
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.CartManager
import com.example.foodapp.ui.state.CheckoutStatus
import com.example.foodapp.ui.state.CheckoutViewModel
import androidx.lifecycle.viewmodel.compose.viewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    viewModel: CheckoutViewModel = viewModel(),
    authState: AuthState = AuthState.Unauthenticated,
    onNavigateBack: () -> Unit,
    onNavigateToAddressList: () -> Unit,
    onOrderSuccess: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val cartState by CartManager.cartState.collectAsState()

    val user = (authState as? AuthState.Authenticated)?.user
    LaunchedEffect(user) {
        viewModel.initialize(user?.uid)
    }

    LaunchedEffect(uiState.status) {
        if (uiState.status == CheckoutStatus.Success) {
            uiState.placedOrderId?.let { orderId ->
                onOrderSuccess(orderId)
                viewModel.resetState()
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Checkout", style = MaterialTheme.typography.titleLarge) },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = SurfaceWhite
                )
            )
        },
        bottomBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    if (uiState.status == CheckoutStatus.Error && uiState.errorMessage != null) {
                        ErrorStateView(
                            errorMessage = uiState.errorMessage ?: "Unknown error",
                            onRetry = { viewModel.placeOrder(authState) },
                            modifier = Modifier.fillMaxWidth().padding(vertical = 16.dp)
                        )
                    } else {
                        PrimaryButton(
                            onClick = { viewModel.placeOrder(authState) },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = uiState.status != CheckoutStatus.Loading
                        ) {
                            if (uiState.status == CheckoutStatus.Loading) {
                                CircularProgressIndicator(
                                    modifier = Modifier.size(24.dp),
                                    color = SurfaceWhite,
                                    strokeWidth = 2.dp
                                )
                            } else {
                                Text(text = "Place Order • Rs. ${cartState.total.toInt()}")
                            }
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp)
        ) {
            // Delivery Address Section
            Text(
                text = "Delivery Address",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.LocationOn,
                        contentDescription = "Location",
                        tint = BrandPrimary
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        if (uiState.address.isComplete) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(text = uiState.address.label, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                                if (uiState.address.isDefault) {
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Surface(color = BrandPrimary.copy(alpha = 0.1f), shape = MaterialTheme.shapes.small) {
                                        Text("Default", style = MaterialTheme.typography.labelSmall, color = BrandPrimary, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = uiState.address.streetAddress, style = MaterialTheme.typography.bodyMedium)
                            if (uiState.address.deliveryInstructions.isNotBlank()) {
                                Text(text = "Note: ${uiState.address.deliveryInstructions}", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                            }
                        } else {
                            Text(text = "No address selected", style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                        }
                    }
                    TextButton(onClick = onNavigateToAddressList) {
                        Text("Edit", color = BrandPrimary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Order Summary Section
            Text(
                text = "Order Summary",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    cartState.items.forEach { cartItem ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "${cartItem.quantity}x ${cartItem.product.name}", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Rs. ${(cartItem.product.price * cartItem.quantity).toInt()}", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    HorizontalDivider(color = DividerColor)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rs. ${cartState.subtotal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(text = "Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                        Text(text = "Rs. ${cartState.deliveryFee.toInt()}", style = MaterialTheme.typography.bodyMedium)
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Payment Method Section
            Text(
                text = "Payment Method",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(
                        selected = true,
                        onClick = null,
                        colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                    )
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(text = uiState.paymentMethod, style = MaterialTheme.typography.bodyMedium)
                }
            }
        }

    }
}
