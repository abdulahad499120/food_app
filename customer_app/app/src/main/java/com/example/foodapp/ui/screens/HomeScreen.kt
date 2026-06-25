package com.example.foodapp.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import com.example.foodapp.utils.bounceClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DeliveryDining
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Storefront
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Address
import com.example.foodapp.data.models.Branch
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.ActiveOrderBanner
import com.example.foodapp.ui.components.GiftRevealOverlay
import com.example.foodapp.ui.state.*
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    orderSessionViewModel: OrderSessionViewModel,
    authState: AuthState = AuthState.Unauthenticated,
    cartItemCount: Int = 0,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToOrder: () -> Unit = {},
    onNavigateToCart: () -> Unit = {},
    onNavigateToTracking: (String) -> Unit = {},
    activeOrderId: String? = null,
    giftViewModel: GiftViewModel = viewModel()
) {
    val scrollState = rememberScrollState()
    val pendingGifts by giftViewModel.pendingGifts.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    val cartScale = remember { Animatable(1f) }
    LaunchedEffect(cartItemCount) {
        if (cartItemCount > 0) {
            cartScale.snapTo(1.2f)
            cartScale.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    // Modal Bottom Sheet States
    var showAddressPickerBottomSheet by remember { mutableStateOf(false) }
    var showBranchLocatorBottomSheet by remember { mutableStateOf(false) }
    var showSavedAddressesBottomSheet by remember { mutableStateOf(false) }
    
    val userAddresses by orderSessionViewModel.userAddresses.collectAsStateWithLifecycle()
    val activeDeliveryAddress by orderSessionViewModel.activeDeliveryAddress.collectAsStateWithLifecycle()
    val fulfillmentMode by orderSessionViewModel.fulfillmentMode.collectAsStateWithLifecycle()

    LaunchedEffect(authState) {
        val userId = (authState as? AuthState.Authenticated)?.user?.uid
        orderSessionViewModel.initializeUser(userId)
    }

    // Cart Persistence Dialog State
    var pendingModeSwitch by remember { mutableStateOf<FulfillmentMode?>(null) }
    
    // Routing Failure Dialog State
    var routingErrorReason by remember { mutableStateOf<String?>(null) }

    val handleModeSelection = { mode: FulfillmentMode ->
        val proceedWithDelivery = {
            if (userAddresses.isEmpty() || authState !is AuthState.Authenticated) {
                showAddressPickerBottomSheet = true
            } else if (activeDeliveryAddress == null) {
                showSavedAddressesBottomSheet = true
            } else {
                onNavigateToOrder()
            }
        }
        
        if (cartItemCount > 0) {
            val currentMode = orderSessionViewModel.fulfillmentMode.value
            if (currentMode != null && currentMode != mode) {
                pendingModeSwitch = mode
            } else {
                // Same mode, proceed
                if (mode == FulfillmentMode.DELIVERY) proceedWithDelivery()
                else showBranchLocatorBottomSheet = true
            }
        } else {
            // Cart is empty, proceed
            if (mode == FulfillmentMode.DELIVERY) proceedWithDelivery()
            else showBranchLocatorBottomSheet = true
        }
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            kotlinx.coroutines.delay(1000)
            orderSessionViewModel.activeBranch.value?.let { 
                // Just trigger re-emission or delay to simulate visual refresh
            }
            isRefreshing = false
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = SurfaceWhite
    ) { innerPadding ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = modifier.fillMaxSize().padding(innerPadding)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
            ) {
                // Custom Top Bar Content
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (fulfillmentMode != null) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (fulfillmentMode == FulfillmentMode.DELIVERY) "Delivering to:" else "Pickup from:",
                                style = MaterialTheme.typography.labelSmall,
                                color = Color.Gray
                            )
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (fulfillmentMode == FulfillmentMode.DELIVERY) 
                                               activeDeliveryAddress?.streetAddress?.takeIf { it.isNotBlank() } ?: "Select Address"
                                           else 
                                               orderSessionViewModel.activeBranch.value?.name ?: "Select Branch",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = VAL_BRAND_PRIMARY,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "Change",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary,
                                    modifier = Modifier.bounceClick {
                                        if (fulfillmentMode == FulfillmentMode.DELIVERY) {
                                            if (userAddresses.isEmpty() || authState !is AuthState.Authenticated) {
                                                showAddressPickerBottomSheet = true
                                            } else {
                                                showSavedAddressesBottomSheet = true
                                            }
                                        } else {
                                            showBranchLocatorBottomSheet = true
                                        }
                                    }.padding(4.dp)
                                )
                            }
                        }
                    } else if (authState is AuthState.Unauthenticated) {
                        TextButton(onClick = onNavigateToAuth) {
                            Text(
                                text = "Sign in",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = VAL_BRAND_PRIMARY
                            )
                        }
                    } else {
                        val userName = (authState as? AuthState.Authenticated)?.user?.name ?: "Member"
                        Text(
                            text = "Welcome, $userName",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }

                    // Right: Profile Avatar / Cart
                    IconButton(
                        onClick = onNavigateToCart,
                        modifier = Modifier
                            .scale(cartScale.value)
                            .bounceClick { onNavigateToCart() }
                    ) {
                        BadgedBox(
                            badge = { 
                                if (cartItemCount > 0) {
                                    Badge { Text(cartItemCount.toString()) }
                                } 
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.ShoppingCart,
                                contentDescription = "Cart",
                                tint = TextPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }

                if (activeOrderId != null) {
                    ActiveOrderBanner(
                        orderId = activeOrderId,
                        onClick = onNavigateToTracking
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Premium Hero Illustration
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .height(250.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(VAL_BRAND_PRIMARY.copy(alpha = 0.1f))
                ) {
                    Image(
                        painter = painterResource(id = com.ahad.foodapp.R.drawable.home_hero_dessert),
                        contentDescription = "Hero Image",
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f))
                    )
                    Column(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(24.dp)
                    ) {
                        Text(
                            text = "Craving something sweet?",
                            style = MaterialTheme.typography.headlineMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Select how you'd like your order.",
                            style = MaterialTheme.typography.bodyLarge,
                            color = Color.White.copy(alpha = 0.9f)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Mode Selectors
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // Delivery Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .bounceClick { handleModeSelection(FulfillmentMode.DELIVERY) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = VAL_SURFACE_DARK),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.DeliveryDining,
                                contentDescription = "Delivery",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Order Delivery",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }

                    // Pickup Card
                    Card(
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .bounceClick { handleModeSelection(FulfillmentMode.PICKUP) },
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(containerColor = VAL_BRAND_PRIMARY),
                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(16.dp),
                            verticalArrangement = Arrangement.Center,
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Storefront,
                                contentDescription = "Pickup",
                                tint = Color.White,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "Store Pickup",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
                
                Spacer(modifier = Modifier.height(32.dp))
            }
        }

        // Cart Persistence Warning Dialog
        if (pendingModeSwitch != null) {
            AlertDialog(
                onDismissRequest = { pendingModeSwitch = null },
                title = { Text("Change Fulfillment Method?") },
                text = { Text("Changing your fulfillment method will clear your current cart. Do you want to proceed?") },
                confirmButton = {
                    TextButton(
                        onClick = {
                            CartManager.clearCart()
                            val mode = pendingModeSwitch
                            pendingModeSwitch = null
                            if (mode == FulfillmentMode.DELIVERY) showAddressPickerBottomSheet = true
                            else showBranchLocatorBottomSheet = true
                        }
                    ) {
                        Text("Proceed")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { pendingModeSwitch = null }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Auto-Routing Failure Dialog
        if (routingErrorReason != null) {
            AlertDialog(
                onDismissRequest = { routingErrorReason = null },
                title = { Text("Delivery Unavailable") },
                text = { Text(routingErrorReason!!) },
                confirmButton = {
                    TextButton(onClick = { routingErrorReason = null }) {
                        Text("OK")
                    }
                }
            )
        }

        // Modal Bottom Sheets
        if (showAddressPickerBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showAddressPickerBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SurfaceWhite
            ) {
                // Wrap in Box with imePadding for keyboard support
                Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    AddressMapPickerScreen(
                        onAddressSaved = { address ->
                            showAddressPickerBottomSheet = false
                            val result = orderSessionViewModel.setDeliveryAddress(address)
                            if (result is com.example.foodapp.ui.state.AutoRoutingResult.Success) {
                                onNavigateToOrder()
                            } else if (result is com.example.foodapp.ui.state.AutoRoutingResult.Failure) {
                                routingErrorReason = result.reason
                            }
                        },
                        onNavigateBack = { showAddressPickerBottomSheet = false },
                        authState = authState
                    )
                }
            }
        }

        if (showBranchLocatorBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showBranchLocatorBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SurfaceWhite
            ) {
                Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    com.example.foodapp.ui.screens.BranchLocatorScreen(
                        onNavigateBack = { showBranchLocatorBottomSheet = false },
                        onBranchSelected = { branchId ->
                            orderSessionViewModel.setPickupBranch(branchId)
                            showBranchLocatorBottomSheet = false
                            onNavigateToOrder()
                        },
                        onSkip = {
                            showBranchLocatorBottomSheet = false
                            onNavigateToOrder()
                        },
                        onSwitchToDelivery = {
                            showBranchLocatorBottomSheet = false
                            orderSessionViewModel.setFulfillmentMode(FulfillmentMode.DELIVERY)
                            showAddressPickerBottomSheet = true
                        }
                    )
                }
            }
        }

        if (showSavedAddressesBottomSheet) {
            ModalBottomSheet(
                onDismissRequest = { showSavedAddressesBottomSheet = false },
                sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SurfaceWhite
            ) {
                SavedAddressesSheet(
                    addresses = userAddresses,
                    onAddressSelected = { address ->
                        showSavedAddressesBottomSheet = false
                        val result = orderSessionViewModel.setDeliveryAddress(address)
                        if (result is AutoRoutingResult.Success) {
                            orderSessionViewModel.setFulfillmentMode(FulfillmentMode.DELIVERY)
                            onNavigateToOrder()
                        } else if (result is AutoRoutingResult.Failure) {
                            routingErrorReason = result.reason
                        }
                    },
                    onAddNewClick = {
                        showSavedAddressesBottomSheet = false
                        showAddressPickerBottomSheet = true
                    }
                )
            }
        }

        // Gift Reveal Overlay
        GiftRevealOverlay(
            pendingGift = pendingGifts.firstOrNull(),
            onClaimGift = { gift ->
                giftViewModel.claimGift(
                    gift = gift,
                    onSuccess = {
                        scope.launch { snackbarHostState.showSnackbar("Added Rs. ${gift.amount.toInt()} to Wallet!") }
                    },
                    onError = {
                        scope.launch { snackbarHostState.showSnackbar("Error claiming gift: ${it.message}") }
                    }
                )
            }
        )
    }
}
