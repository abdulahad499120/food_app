package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.BorderStroke
import com.example.foodapp.utils.bounceClick
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
import androidx.compose.ui.graphics.Color
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
    paymentViewModel: com.example.foodapp.ui.state.PaymentViewModel = viewModel(),
    authState: AuthState = AuthState.Unauthenticated,
    activeBranch: com.example.foodapp.data.models.Branch? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val cartState by CartManager.cartState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current

    val user = (authState as? AuthState.Authenticated)?.user
    LaunchedEffect(user) {
        viewModel.initialize(user?.uid)
        if (user != null) {
            paymentViewModel.loadPayments(user.uid)
        }
    }

    var showSuccessAnimation by remember { mutableStateOf(false) }
    var showPaymentSheet by remember { mutableStateOf(false) }
    // Safety Net #2: Pickup intercept — prevent accidental pickup orders
    var showPickupConfirmSheet by remember { mutableStateOf(false) }
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

    val safepayCheckoutUrl by viewModel.safepayCheckoutUrl.collectAsStateWithLifecycle()
    LaunchedEffect(safepayCheckoutUrl) {
        safepayCheckoutUrl?.let { url ->
            val intent = android.content.Intent(android.content.Intent.ACTION_VIEW, android.net.Uri.parse(url))
            context.startActivity(intent)
            viewModel.clearCheckoutUrl()
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
                            val isPickup = cartState.fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.PICKUP
                            if (cartState.fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY && !uiState.address.isComplete) {
                                coroutineScope.launch {
                                    snackbarHostState.showSnackbar("Please select a delivery address to continue.")
                                }
                            } else if (isPickup) {
                                // Safety Net #2: Intercept accidental pickup orders
                                showPickupConfirmSheet = true
                            } else {
                                viewModel.placeOrder(
                                    authState = authState,
                                    context = context,
                                    fulfillmentMode = cartState.fulfillmentMode ?: com.example.foodapp.ui.state.FulfillmentMode.DELIVERY,
                                    branchGeoPoint = activeBranch?.location,
                                    deliveryGeoPoint = uiState.address.location
                                )
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
                                val btnText = if (uiState.paymentMethodId == "payfast_webview") "Pay with PayFast • Rs. ${cartState.total.toInt()}" else "Place Order • Rs. ${cartState.total.toInt()}"
                                Text(text = btnText)
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
            if (cartState.fulfillmentMode == com.example.foodapp.ui.state.FulfillmentMode.DELIVERY) {
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
            } else {
                // Pickup Store Section
                Text(
                    text = "Pickup Location",
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
                            contentDescription = "Store",
                            tint = BrandPrimary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = activeBranch?.name ?: "No store selected", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(text = activeBranch?.address ?: "", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Order Summary Section
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .bounceClick { isOrderSummaryExpanded = !isOrderSummaryExpanded }
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
                    Text(text = uiState.paymentMethodName, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = { showPaymentSheet = true }) {
                        Text("Edit", color = BrandPrimary)
                    }
                }
                
                // Native Card Input Form
                AnimatedVisibility(
                    visible = uiState.paymentMethodId == "native_card",
                    enter = expandVertically() + fadeIn(),
                    exit = shrinkVertically() + fadeOut()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp)
                            .padding(bottom = 16.dp)
                    ) {
                        Spacer(modifier = Modifier.height(8.dp))
                        com.example.foodapp.ui.components.TextInput(
                            value = uiState.cardNumber,
                            onValueChange = { viewModel.updateCardNumber(it) },
                            label = "Card Number",
                            keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(modifier = Modifier.fillMaxWidth()) {
                            com.example.foodapp.ui.components.TextInput(
                                value = uiState.cardExpMonth,
                                onValueChange = { viewModel.updateCardExpMonth(it) },
                                label = "MM",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.foodapp.ui.components.TextInput(
                                value = uiState.cardExpYear,
                                onValueChange = { viewModel.updateCardExpYear(it) },
                                label = "YYYY",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            com.example.foodapp.ui.components.TextInput(
                                value = uiState.cardCvv,
                                onValueChange = { viewModel.updateCardCvv(it) },
                                label = "CVV",
                                modifier = Modifier.weight(1f),
                                keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(keyboardType = androidx.compose.ui.text.input.KeyboardType.Number)
                            )
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { viewModel.toggleSaveCardForLater(!uiState.saveCardForLater) }
                        ) {
                            Checkbox(
                                checked = uiState.saveCardForLater,
                                onCheckedChange = { viewModel.toggleSaveCardForLater(it) },
                                colors = CheckboxDefaults.colors(checkedColor = BrandPrimary)
                            )
                            Text(text = "Save this card for future checkouts", style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
            }
        }
        
        if (showPaymentSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPaymentSheet = false },
                containerColor = SurfaceWhite
            ) {
                val paymentsState by paymentViewModel.uiState.collectAsStateWithLifecycle()
                Column(modifier = Modifier.fillMaxWidth().padding(24.dp).padding(bottom = 32.dp)) {
                    Text("Select Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Cash on Delivery Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                viewModel.setPaymentMethod("COD", "Cash on Delivery")
                                showPaymentSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.paymentMethodId == "COD",
                            onClick = {
                                viewModel.setPaymentMethod("COD", "Cash on Delivery")
                                showPaymentSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Cash on Delivery", style = MaterialTheme.typography.bodyLarge)
                    }
                    
                    // Native Card Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                viewModel.setPaymentMethod("native_card", "Credit / Debit Card")
                                showPaymentSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.paymentMethodId == "native_card",
                            onClick = {
                                viewModel.setPaymentMethod("native_card", "Credit / Debit Card")
                                showPaymentSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("Credit / Debit Card", style = MaterialTheme.typography.bodyLarge)
                    }

                    // PayFast Hosted Checkout Option
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .bounceClick {
                                viewModel.setPaymentMethod("payfast_webview", "PayFast Checkout")
                                showPaymentSheet = false
                            }
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = uiState.paymentMethodId == "payfast_webview",
                            onClick = {
                                viewModel.setPaymentMethod("payfast_webview", "PayFast Checkout")
                                showPaymentSheet = false
                            },
                            colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text("PayFast Checkout", style = MaterialTheme.typography.bodyLarge)
                    }

                    if (paymentsState is com.example.foodapp.ui.state.PaymentListUiState.Success) {
                        val payments = (paymentsState as com.example.foodapp.ui.state.PaymentListUiState.Success).payments
                        payments.forEach { method ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .bounceClick {
                                        val name = when (method.category) {
                                            com.example.foodapp.data.models.PaymentMethodCategory.RAAST -> "Raast: ${method.raastId}"
                                            com.example.foodapp.data.models.PaymentMethodCategory.CARD -> "${method.type} ending in ${method.last4}"
                                            else -> "${method.bankName} Account"
                                        }
                                        viewModel.setPaymentMethod(method.id, name)
                                        showPaymentSheet = false
                                    }
                                    .padding(vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                RadioButton(
                                    selected = uiState.paymentMethodId == method.id,
                                    onClick = {
                                        val name = when (method.category) {
                                            com.example.foodapp.data.models.PaymentMethodCategory.RAAST -> "Raast: ${method.raastId}"
                                            com.example.foodapp.data.models.PaymentMethodCategory.CARD -> "${method.type} ending in ${method.last4}"
                                            else -> "${method.bankName} Account"
                                        }
                                        viewModel.setPaymentMethod(method.id, name)
                                        showPaymentSheet = false
                                    },
                                    colors = RadioButtonDefaults.colors(selectedColor = BrandPrimary)
                                )
                                Spacer(modifier = Modifier.width(16.dp))
                                val title = when (method.category) {
                                    com.example.foodapp.data.models.PaymentMethodCategory.RAAST -> "Raast ${method.raastId}"
                                    com.example.foodapp.data.models.PaymentMethodCategory.CARD -> "${method.type} •••• ${method.last4}"
                                    else -> "${method.bankName} ${method.accountNumber}"
                                }
                                Text(title, style = MaterialTheme.typography.bodyLarge)
                            }
                        }
                    }
                }
            }
        }

        // Safety Net #2: Pickup Order Intercept Sheet
        if (showPickupConfirmSheet) {
            ModalBottomSheet(
                onDismissRequest = { showPickupConfirmSheet = false },
                containerColor = SurfaceWhite,
                shape = MaterialTheme.shapes.large
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp)
                        .padding(bottom = 40.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Warning Icon
                    Surface(
                        color = Color(0xFFFFF3E0),
                        shape = androidx.compose.foundation.shape.CircleShape,
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.LocationOn,
                                contentDescription = null,
                                tint = Color(0xFFE65100),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Just double checking!",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (activeBranch != null)
                            "You are placing an order for Pickup at ${activeBranch.name}. No rider will be dispatched."
                        else
                            "You are placing a Pickup order. No rider will be dispatched.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = TextSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    // Confirm Pickup
                    Button(
                        onClick = {
                            showPickupConfirmSheet = false
                            viewModel.placeOrder(
                                authState = authState,
                                context = context,
                                branchGeoPoint = activeBranch?.location,
                                deliveryGeoPoint = null
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = BrandPrimary),
                        shape = MaterialTheme.shapes.medium
                    ) {
                        Text("Confirm Pickup", fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    // Switch to Delivery
                    OutlinedButton(
                        onClick = { showPickupConfirmSheet = false },
                        modifier = Modifier.fillMaxWidth().height(50.dp),
                        shape = MaterialTheme.shapes.medium,
                        border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary)
                    ) {
                        Text("Cancel", color = BrandPrimary, fontWeight = FontWeight.Medium)
                    }
                }
            }
        }

    }


    // Vaulted card DDC overlay — full-screen WebView that runs Cardinal Commerce's
    // device fingerprinting silently, then hands the sessionId back to the ViewModel.
    uiState.pendingVaultedDdc?.let { ddc ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            SafepayWebViewScreen(
                ddcUrl = ddc.ddcUrl,
                accessToken = ddc.accessToken,
                onSuccess = { sessionId ->
                    viewModel.onVaultedCardDdcSuccess(sessionId, context)
                },
                onFailure = { msg ->
                    viewModel.onVaultedCardDdcFailure("3DS verification failed: $msg")
                }
            )
        }
    }
    
    // 3DS Challenge WebView (Bank OTP)
    uiState.pendingChallengeUrl?.let { url ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            SafepayChallengeWebView(
                challengeUrl = url,
                onSuccess = {
                    viewModel.onChallengeSuccess(context)
                },
                onFailure = {
                    viewModel.onChallengeFailure()
                }
            )
        }
    }

    // Payfast Checkout WebView
    uiState.payfastCheckoutUrl?.let { url ->
        androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize()) {
            PayfastCheckoutWebView(
                checkoutUrl = url,
                onSuccess = { txId ->
                    viewModel.onPayfastSuccess(context, txId)
                },
                onCancel = {
                    viewModel.onPayfastCancel()
                },
                onFailure = { errorMsg ->
                    viewModel.onPayfastFailure(errorMsg)
                }
            )
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
