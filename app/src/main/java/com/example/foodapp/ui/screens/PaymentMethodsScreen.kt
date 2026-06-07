package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CreditCard
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.PaymentMethod
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.PaymentListUiState
import com.example.foodapp.ui.state.PaymentViewModel

/**
 * # H1 Payment Methods Screen
 * 
 * Displays the list of tokenized payment methods in a warm, minimalist layout.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    val user = (authState as? AuthState.Authenticated)?.user

    LaunchedEffect(user) {
        if (user != null) {
            viewModel.loadPayments(user.uid)
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // H2 Screen Title
                    Text("Payment Methods", style = MaterialTheme.typography.titleLarge) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VAL_BACKGROUND)
            )
        },
        containerColor = VAL_BACKGROUND,
        bottomBar = {
            Surface(color = VAL_BACKGROUND, modifier = Modifier.fillMaxWidth().padding(24.dp)) {
                PrimaryButton(
                    onClick = onNavigateToAddPayment,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Add Payment Method", style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (user == null) {
                Text(
                    text = "Please log in to manage payments.",
                    modifier = Modifier.align(Alignment.Center),
                    color = TextSecondary
                )
                return@Box
            }

            when (val state = uiState) {
                is PaymentListUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VAL_BRAND_PRIMARY
                    )
                }
                is PaymentListUiState.Error -> {
                    Text(
                        text = state.message,
                        color = ErrorRed,
                        modifier = Modifier.align(Alignment.Center).padding(16.dp)
                    )
                }
                is PaymentListUiState.Success -> {
                    if (state.payments.isEmpty()) {
                        // H3 Empty State
                        Column(
                            modifier = Modifier.align(Alignment.Center),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.CreditCard, 
                                contentDescription = null, 
                                tint = Color.LightGray, 
                                modifier = Modifier.size(64.dp)
                            )
                            Spacer(modifier = Modifier.height(24.dp))
                            Text(
                                "No payment methods", 
                                style = MaterialTheme.typography.titleMedium, 
                                color = TextSecondary
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 24.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.payments) { payment ->
                                PaymentMethodCard(
                                    payment = payment,
                                    onMakeDefault = { 
                                        viewModel.updatePaymentDefaultStatus(user.uid, payment) 
                                    },
                                    onDelete = { 
                                        viewModel.deletePayment(user.uid, payment.id) 
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * # H2 Payment Method Card
 * 
 * Minimalist card component for displaying a tokenized payment method.
 */
@Composable
fun PaymentMethodCard(
    payment: PaymentMethod,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(24.dp), 
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(VAL_BACKGROUND, shape = RoundedCornerShape(12.dp)),
                contentAlignment = Alignment.Center
            ) {
                Icon(Icons.Default.CreditCard, contentDescription = "Card", tint = VAL_BRAND_PRIMARY)
            }
            
            Spacer(modifier = Modifier.width(20.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // H3 Payment Title
                    Text(
                        text = "${payment.type} •••• ${payment.last4}",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        fontWeight = FontWeight.SemiBold
                    )
                    if (payment.isDefault) {
                        Spacer(modifier = Modifier.width(12.dp))
                        Surface(
                            color = VAL_BRAND_PRIMARY.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(6.dp)
                        ) {
                            Text(
                                text = "Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = VAL_BRAND_PRIMARY,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(6.dp))
                // H4 Expiry Text
                Text(
                    text = "Expires ${payment.expiry}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = TextSecondary
                )
            }
            
            Box {
                IconButton(onClick = { menuExpanded = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "More Options", tint = TextSecondary)
                }
                DropdownMenu(
                    expanded = menuExpanded,
                    onDismissRequest = { menuExpanded = false },
                    modifier = Modifier.background(SurfaceWhite)
                ) {
                    if (!payment.isDefault) {
                        DropdownMenuItem(
                            text = { Text("Make Default", style = MaterialTheme.typography.bodyMedium) },
                            onClick = { 
                                menuExpanded = false
                                onMakeDefault()
                            }
                        )
                    }
                    DropdownMenuItem(
                        text = { Text("Delete", color = ErrorRed, style = MaterialTheme.typography.bodyMedium) },
                        onClick = { 
                            menuExpanded = false
                            onDelete()
                        }
                    )
                }
            }
        }
    }
}
