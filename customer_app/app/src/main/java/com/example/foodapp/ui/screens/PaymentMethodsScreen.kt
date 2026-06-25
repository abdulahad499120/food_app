package com.example.foodapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.PaymentMethod
import com.example.foodapp.data.models.PaymentMethodCategory
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.PaymentListUiState
import com.example.foodapp.ui.state.PaymentViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun PaymentMethodsScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    onNavigateToAddPayment: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = (authState as? AuthState.Authenticated)?.user
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()

    val lifecycleOwner = LocalLifecycleOwner.current

    DisposableEffect(lifecycleOwner, user) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                user?.let { viewModel.loadPayments(it.uid) }
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AccountBalanceWallet,
                            contentDescription = null,
                            tint = VAL_BRAND_PRIMARY,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text("My Wallet", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
                    }
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
            Surface(
                color = VAL_BACKGROUND,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                PrimaryButton(
                    onClick = onNavigateToAddPayment,
                    modifier = Modifier.fillMaxWidth().height(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Add", tint = Color.White)
                    Spacer(Modifier.width(8.dp))
                    Text("Add Payment Method", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.fillMaxSize().padding(paddingValues)) {
            if (user == null) {
                Text(
                    "Please log in to manage payments.",
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
                        // Premium Empty State
                        Column(
                            modifier = Modifier.align(Alignment.Center).padding(32.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            val infiniteTransition = rememberInfiniteTransition(label = "empty")
                            val scale by infiniteTransition.animateFloat(
                                initialValue = 0.95f, targetValue = 1.05f,
                                animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
                                label = "pulse"
                            )
                            Box(
                                modifier = Modifier
                                    .size(96.dp)
                                    .graphicsLayer { scaleX = scale; scaleY = scale }
                                    .clip(CircleShape)
                                    .background(VAL_BRAND_PRIMARY.copy(alpha = 0.1f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.CreditCard,
                                    contentDescription = null,
                                    tint = VAL_BRAND_PRIMARY,
                                    modifier = Modifier.size(48.dp)
                                )
                            }
                            Spacer(Modifier.height(24.dp))
                            Text(
                                "No payment methods yet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = TextPrimary
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "Add a card, Raast ID, or bank account\nto pay faster at checkout.",
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(state.payments, key = { it.id }) { payment ->
                                SwipeablePremiumPaymentCard(
                                    payment = payment,
                                    onMakeDefault = {
                                        viewModel.updatePaymentDefaultStatus(user.uid, payment)
                                    },
                                    onDelete = {
                                        viewModel.deletePayment(user.uid, payment.id)
                                        coroutineScope.launch {
                                            val result = snackbarHostState.showSnackbar(
                                                message = "Card removed",
                                                actionLabel = "Undo",
                                                duration = SnackbarDuration.Short
                                            )
                                            // Undo is a future enhancement — for now just inform
                                        }
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

// ─────────────────────────────────────────────
// Swipeable wrapper
// ─────────────────────────────────────────────
@Composable
fun SwipeablePremiumPaymentCard(
    payment: PaymentMethod,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit
) {
    var menuExpanded by remember { mutableStateOf(false) }
    var offsetX by remember { mutableFloatStateOf(0f) }
    val animatedOffset by animateFloatAsState(targetValue = offsetX, label = "swipe")

    Box(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp))) {
        // Delete background revealed on swipe
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(Color(0xFFFF3B30)),
            contentAlignment = Alignment.CenterEnd
        ) {
            Icon(
                Icons.Default.Delete,
                contentDescription = "Delete",
                tint = Color.White,
                modifier = Modifier.padding(end = 24.dp).size(28.dp)
            )
        }

        Box(
            modifier = Modifier
                .offset(x = animatedOffset.dp)
                .pointerInput(Unit) {
                    detectHorizontalDragGestures(
                        onDragEnd = {
                            if (offsetX < -100f) {
                                onDelete()
                                offsetX = 0f
                            } else {
                                offsetX = 0f
                            }
                        }
                    ) { _, dragAmount ->
                        offsetX = (offsetX + dragAmount / 3f).coerceIn(-120f, 0f)
                    }
                }
        ) {
            PremiumPaymentCard(
                payment = payment,
                menuExpanded = menuExpanded,
                onMenuToggle = { menuExpanded = it },
                onMakeDefault = onMakeDefault,
                onDelete = onDelete
            )
        }
    }
}

// ─────────────────────────────────────────────
// Visual Bank Card
// ─────────────────────────────────────────────
@Composable
fun PremiumPaymentCard(
    payment: PaymentMethod,
    menuExpanded: Boolean,
    onMenuToggle: (Boolean) -> Unit,
    onMakeDefault: () -> Unit,
    onDelete: () -> Unit
) {
    val gradient = when {
        payment.category == PaymentMethodCategory.RAAST -> Brush.linearGradient(
            colors = listOf(Color(0xFF00897B), Color(0xFF004D40))
        )
        payment.category == PaymentMethodCategory.BANK_ACCOUNT -> Brush.linearGradient(
            colors = listOf(Color(0xFF1565C0), Color(0xFF0D47A1))
        )
        payment.type == "Mastercard" -> Brush.linearGradient(
            colors = listOf(Color(0xFF37474F), Color(0xFF1C1C2E))
        )
        else -> Brush.linearGradient( // Visa
            colors = listOf(Color(0xFF4A148C), Color(0xFF6A1B9A))
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .shadow(12.dp, RoundedCornerShape(20.dp))
            .clip(RoundedCornerShape(20.dp))
            .background(gradient)
    ) {
        // Decorative circles
        Box(
            modifier = Modifier
                .size(150.dp)
                .offset(x = 200.dp, y = (-40).dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.06f))
        )
        Box(
            modifier = Modifier
                .size(100.dp)
                .offset(x = 240.dp, y = 60.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.04f))
        )

        Column(modifier = Modifier.fillMaxWidth().padding(20.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Category icon + label
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val icon = when (payment.category) {
                        PaymentMethodCategory.RAAST -> Icons.Default.PhoneAndroid
                        PaymentMethodCategory.BANK_ACCOUNT -> Icons.Default.AccountBalance
                        else -> Icons.Default.CreditCard
                    }
                    Icon(icon, contentDescription = null, tint = Color.White.copy(alpha = 0.8f), modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = when (payment.category) {
                            PaymentMethodCategory.RAAST -> "Raast"
                            PaymentMethodCategory.BANK_ACCOUNT -> payment.bankName.ifBlank { "Bank" }
                            else -> payment.type.ifBlank { "Card" }
                        },
                        color = Color.White.copy(alpha = 0.8f),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Default badge
                    AnimatedVisibility(visible = payment.isDefault) {
                        Surface(
                            color = Color(0xFF4CAF50),
                            shape = RoundedCornerShape(20.dp)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(12.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Default", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                    // 3-dot menu
                    Box {
                        IconButton(onClick = { onMenuToggle(true) }, modifier = Modifier.size(32.dp)) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Options", tint = Color.White.copy(alpha = 0.8f))
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { onMenuToggle(false) },
                            modifier = Modifier.background(SurfaceWhite)
                        ) {
                            if (!payment.isDefault) {
                                DropdownMenuItem(
                                    text = { Text("Set as Default") },
                                    leadingIcon = { Icon(Icons.Default.Star, contentDescription = null, tint = Color(0xFFFFC107)) },
                                    onClick = { onMenuToggle(false); onMakeDefault() }
                                )
                            }
                            DropdownMenuItem(
                                text = { Text("Delete", color = ErrorRed) },
                                leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = ErrorRed) },
                                onClick = { onMenuToggle(false); onDelete() }
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            // Masked number / ID
            val maskedNumber = when (payment.category) {
                PaymentMethodCategory.RAAST -> "•••• •••• ${payment.raastId.takeLast(4)}"
                PaymentMethodCategory.BANK_ACCOUNT -> "•••• •••• •••• ${payment.accountNumber.takeLast(4)}"
                else -> "•••• •••• •••• ${payment.last4}"
            }
            Text(
                text = maskedNumber,
                color = Color.White,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                fontFamily = FontFamily.Monospace
            )

            Spacer(Modifier.height(16.dp))

            // Bottom row
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                if (payment.category == PaymentMethodCategory.CARD && payment.expiry.length == 4) {
                    Column {
                        Text("EXPIRES", color = Color.White.copy(alpha = 0.6f), fontSize = 9.sp, letterSpacing = 1.sp)
                        Text(
                            "${payment.expiry.take(2)}/${payment.expiry.drop(2)}",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                    }
                } else {
                    Spacer(Modifier.width(1.dp))
                }
                // Brand logo text
                Text(
                    text = when {
                        payment.category == PaymentMethodCategory.RAAST -> "RAAST"
                        payment.category == PaymentMethodCategory.BANK_ACCOUNT -> "BANK"
                        payment.type == "Visa" -> "VISA"
                        payment.type == "Mastercard" -> "MC"
                        else -> ""
                    },
                    color = Color.White,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 2.sp,
                    fontFamily = FontFamily.Serif
                )
            }
        }
    }
}

