package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.clickable
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
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CheckoutScreen(
    onNavigateBack: () -> Unit,
    onNavigateToAddressList: () -> Unit,
    onNavigateToPayments: () -> Unit,
    onOrderSuccess: (String) -> Unit,
    viewModel: CheckoutViewModel = viewModel(),
    authState: AuthState = AuthState.Unauthenticated,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by CartManager.cartState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val user = (authState as? AuthState.Authenticated)?.user
    LaunchedEffect(user) {
        viewModel.initialize(user?.uid)
    }

    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    var completedOrderId by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(uiState.status) {
        if (uiState.status == CheckoutStatus.Success) {
            uiState.placedOrderId?.let { orderId ->
                completedOrderId = orderId
                showSuccessAnimation = true
                viewModel.resetState()
            }
        }
    }

    var isOrderSummaryExpanded by remember { mutableStateOf(true) }

    Box(modifier = modifier.fillMaxSize()) {
    Scaffold(
        modifier = Modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    PrimaryButton(
                        onClick = {
                            if (cartState.orderFlowState == com.example.foodapp.ui.state.OrderFlowState.DELIVERY && !uiState.address.isComplete) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please select a delivery address to continue.")
                                }
                            } else {
                                viewModel.placeOrder(authState, context)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .animateContentSize(),
                        enabled = uiState.status != CheckoutStatus.Loading
                    ) {
                        AnimatedContent(
                            targetState = uiState.status == CheckoutStatus.Loading,
                            transitionSpec = { fadeIn() togetherWith fadeOut() },
                            label = "CheckoutButtonMorph"
                        ) { isLoading ->
                            if (isLoading) {
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
            com.example.foodapp.ui.components.PremiumErrorBanner(
                errorMessage = uiState.errorMessage ?: "",
                isVisible = uiState.status == CheckoutStatus.Error && uiState.errorMessage != null,
                modifier = Modifier.padding(bottom = 16.dp)
            )
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
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { isOrderSummaryExpanded = !isOrderSummaryExpanded }
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Order Summary",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Text(
                    text = if (isOrderSummaryExpanded) "Hide" else "Show",
                    color = BrandPrimary,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Surface(
                color = SurfaceWhite,
                shadowElevation = 2.dp,
                shape = MaterialTheme.shapes.medium,
                modifier = Modifier
                    .fillMaxWidth()
                    .animateContentSize(animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    AnimatedVisibility(
                        visible = isOrderSummaryExpanded,
                        enter = expandVertically() + fadeIn(),
                        exit = shrinkVertically() + fadeOut()
                    ) {
                        Column {
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
                        }
                    }
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
                    Text(text = uiState.paymentMethod, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showPaymentSheet = true }) {
                        Text("Edit", color = BrandPrimary)
                    }
                }
            }
        }
        
        if (showPaymentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaymentSheet = false },
                containerColor = SurfaceWhite
            ) {
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
                    Text("Select Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cash on Delivery Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                viewModel.setPaymentMethod("Cash on Delivery")
                                showPaymentSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.paymentMethod == "Cash on Delivery",
                            onClick = {
                                viewModel.setPaymentMethod("Cash on Delivery")
                                showPaymentSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Cash on Delivery", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    // Credit Card Option (If Authenticated)
                    if (user != null) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setPaymentMethod("Credit Card")
                                    showPaymentSheet = false
                                }
                                .padding(vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = uiState.paymentMethod == "Credit Card",
                                onClick = {
                                    viewModel.setPaymentMethod("Credit Card")
                                    showPaymentSheet = false
                                },
                                colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                            )
                            Spacer(modifier = Modifier.width(16.dp))
                            Text("Credit Card", style = MaterialTheme.typography.bodyLarge)
                        }
                    }
                }
            }
        }

    }
    
    if (showSuccessAnimation) {
        com.example.foodapp.ui.components.CheckoutSuccessEffect(
            onAnimationFinished = {
                showSuccessAnimation = false
                completedOrderId?.let { onOrderSuccess(it) }
            }
        )
    }
    }
}
