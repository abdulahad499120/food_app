package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.DividerColor
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.components.ProductListItem
import com.example.foodapp.ui.components.EmptyStateView
import com.example.foodapp.ui.state.CartManager
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material3.TextButton
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.OutlinedTextField
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.style.TextAlign
import com.example.foodapp.ui.state.FulfillmentMode
import androidx.compose.foundation.shape.RoundedCornerShape
import com.example.foodapp.data.models.Branch
import com.example.foodapp.utils.BranchHoursParser
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.Box
import androidx.compose.ui.Alignment

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    activeBranch: Branch? = null,
    rewardsViewModel: com.example.foodapp.ui.state.RewardsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    hasActiveDeliveryAddress: Boolean = false,
    onAddressSaved: (com.example.foodapp.data.models.Address) -> Unit = {},
    onCheckoutRequest: () -> Unit = {},
    onNavigateToHome: () -> Unit = {},
    authState: com.example.foodapp.ui.state.AuthState = com.example.foodapp.ui.state.AuthState.Unauthenticated
) {
    val cartState by CartManager.cartState.collectAsStateWithLifecycle()
    val userProfile by rewardsViewModel.userProfile.collectAsStateWithLifecycle()
    var showCustomTipDialog by remember { mutableStateOf(false) }
    var customTipInput by remember { mutableStateOf("") }
    // Safety Net #5: Show an AlertDialog if the branch is dynamically detected as closed
    // at the exact moment the user taps "Proceed to Checkout".
    var showBranchClosedDialog by remember { mutableStateOf(false) }
    var showAddressPickerBottomSheet by remember { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize().background(com.example.foodapp.theme.VAL_BACKGROUND),
        containerColor = com.example.foodapp.theme.VAL_BACKGROUND,
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column {
                    Text(
                        text = "Your Cart",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(16.dp)
                    )
                    androidx.compose.material3.PrimaryTabRow(
                        selectedTabIndex = if (cartState.fulfillmentMode == FulfillmentMode.DELIVERY) 0 else 1,
                        modifier = Modifier.fillMaxWidth(),
                        containerColor = SurfaceWhite
                    ) {
                        androidx.compose.material3.Tab(
                            selected = cartState.fulfillmentMode == FulfillmentMode.DELIVERY,
                            onClick = { CartManager.setFulfillmentMode(FulfillmentMode.DELIVERY) },
                            text = { Text("Delivery") }
                        )
                        androidx.compose.material3.Tab(
                            selected = cartState.fulfillmentMode == FulfillmentMode.PICKUP,
                            onClick = { CartManager.setFulfillmentMode(FulfillmentMode.PICKUP) },
                            text = { Text("Pickup") }
                        )
                    }
                }
            }
        },
        bottomBar = {
            if (cartState.items.isNotEmpty()) {
                Surface(
                    color = SurfaceWhite,
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp),
                            verticalAlignment = androidx.compose.ui.Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text("⭐ Redeem 150 Stars for this order", style = MaterialTheme.typography.titleMedium, color = com.example.foodapp.theme.VAL_BRAND_PRIMARY)
                            androidx.compose.material3.Switch(
                                checked = cartState.payWithStars,
                                onCheckedChange = { CartManager.togglePayWithStars(it) },
                                enabled = userProfile.starsBalance >= 150,
                                colors = androidx.compose.material3.SwitchDefaults.colors(
                                    checkedThumbColor = com.example.foodapp.theme.VAL_BRAND_PRIMARY, 
                                    checkedTrackColor = com.example.foodapp.theme.VAL_BRAND_PRIMARY.copy(alpha = 0.5f)
                                )
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(modifier = Modifier.padding(bottom = 8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Subtotal", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Rs. ${cartState.subtotal.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        }
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Delivery Fee", style = MaterialTheme.typography.bodyMedium)
                            Text(text = "Rs. ${cartState.deliveryFee.toInt()}", style = MaterialTheme.typography.bodyMedium)
                        }
                        if (cartState.fulfillmentMode == FulfillmentMode.DELIVERY) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Service Fee", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Rs. ${cartState.serviceFee.toInt()}", style = MaterialTheme.typography.bodyMedium)
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(text = "Driver Tip", style = MaterialTheme.typography.bodyMedium)
                                Text(text = "Rs. ${cartState.driverTip.toInt()}", style = MaterialTheme.typography.bodyMedium)
                            }
                            
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(text = "Add a tip for the driver", style = MaterialTheme.typography.titleMedium)
                            Spacer(modifier = Modifier.height(8.dp))
                            
                            val tipOptions = listOf(20.0, 50.0, 100.0)
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(tipOptions) { tipAmt ->
                                    val isSelected = cartState.driverTip == tipAmt
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else com.example.foodapp.theme.SurfaceWhite,
                                        modifier = Modifier
                                            .height(40.dp)
                                            .bounceClick { CartManager.setDriverTip(tipAmt) }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "Rs. ${tipAmt.toInt()}",
                                                color = if (isSelected) com.example.foodapp.theme.SurfaceWhite else com.example.foodapp.theme.TextPrimary,
                                                fontWeight = if (isSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                                item {
                                    val isCustomSelected = cartState.driverTip > 0 && cartState.driverTip !in tipOptions
                                    Surface(
                                        shape = RoundedCornerShape(16.dp),
                                        color = if (isCustomSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else com.example.foodapp.theme.SurfaceWhite,
                                        modifier = Modifier
                                            .height(40.dp)
                                            .bounceClick { showCustomTipDialog = true }
                                    ) {
                                        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                                            Text(
                                                text = "Custom",
                                                color = if (isCustomSelected) com.example.foodapp.theme.SurfaceWhite else com.example.foodapp.theme.TextPrimary,
                                                fontWeight = if (isCustomSelected) androidx.compose.ui.text.font.FontWeight.Bold else androidx.compose.ui.text.font.FontWeight.Normal
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        androidx.compose.material3.HorizontalDivider(color = DividerColor)
                        Spacer(modifier = Modifier.height(16.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(text = "Total", style = MaterialTheme.typography.titleMedium)
                            Text(text = "Rs. ${cartState.total.toInt()}", style = MaterialTheme.typography.titleMedium)
                        }
                        Spacer(modifier = Modifier.height(24.dp))
                        PrimaryButton(
                            onClick = {
                                // Safety Net #5: Real-time operating hours check.
                                // Parse the branch's hours string against the device's
                                // current local time at the exact moment of this tap.
                                // This catches backgrounded-app scenarios where branch.isOpen
                                // may be a stale cached value.
                                val branch = activeBranch
                                val hoursStr = branch?.operatingHours
                                val isOpenRightNow = if (!hoursStr.isNullOrBlank()) {
                                    BranchHoursParser.isOpenNow(hoursStr) ?: true // null = unparseable, treat as open
                                } else {
                                    branch?.isOpen ?: true // no hours string, fall back to cached boolean
                                }

                                if (!isOpenRightNow) {
                                    showBranchClosedDialog = true
                                } else if (cartState.fulfillmentMode == FulfillmentMode.DELIVERY && !hasActiveDeliveryAddress) {
                                    showAddressPickerBottomSheet = true
                                } else {
                                    onCheckoutRequest()
                                }
                            },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Proceed to Checkout")
                        }
                    }
                }
            }
        }
    ) { paddingValues ->
        AnimatedContent(
            targetState = cartState.items.isEmpty(),
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "CartStateAnimation"
        ) { isEmpty ->
            if (isEmpty) {
                EmptyStateView(
                    iconId = com.ahad.foodapp.R.drawable.empty_cart_illustration,
                    title = "Your cart is empty",
                    message = "Looks like you haven't added any items yet. Start exploring our menu!",
                    actionText = "Browse Menu",
                    onActionClick = { onNavigateToHome() },
                    modifier = modifier.padding(paddingValues)
                )
            } else {
                LazyColumn(
                    modifier = modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    items(cartState.items, key = { it.hashCode() }) { cartItem ->
                        val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                            confirmValueChange = { dismissValue ->
                                if (dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                    CartManager.removeItem(cartItem)
                                    true
                                } else {
                                    false
                                }
                            }
                        )
                        
                        androidx.compose.material3.SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            backgroundContent = {
                                val color by androidx.compose.animation.animateColorAsState(
                                    if (dismissState.targetValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                        androidx.compose.material3.MaterialTheme.colorScheme.error
                                    } else {
                                        androidx.compose.ui.graphics.Color.Transparent
                                    }
                                )
                                androidx.compose.foundation.layout.Box(
                                    modifier = androidx.compose.ui.Modifier
                                        .fillMaxSize()
                                        .background(color)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = androidx.compose.ui.Alignment.CenterEnd
                                ) {
                                    androidx.compose.material3.Icon(
                                        imageVector = androidx.compose.material.icons.Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = androidx.compose.ui.graphics.Color.White
                                    )
                                }
                            }
                        ) {
                            ProductListItem(
                                imageUrl = cartItem.product.localImagePath,
                                title = cartItem.product.name,
                                description = "Quantity: ${cartItem.quantity}",
                                price = cartItem.product.price * cartItem.quantity,
                                onClick = {
                                    CartManager.updateQuantity(cartItem.product.id, cartItem.quantity + 1)
                                }
                            )
                        }
                        androidx.compose.material3.HorizontalDivider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
                    }
                }
            }
        }
        
        if (showCustomTipDialog) {
            AlertDialog(
                onDismissRequest = { showCustomTipDialog = false },
                title = {
                    Text(text = "Custom Tip", style = MaterialTheme.typography.headlineSmall)
                },
                text = {
                    OutlinedTextField(
                        value = customTipInput,
                        onValueChange = { customTipInput = it },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Amount (Rs.)") },
                        singleLine = true
                    )
                },
                confirmButton = {
                    TextButton(onClick = {
                        val amt = customTipInput.toDoubleOrNull() ?: 0.0
                        CartManager.setDriverTip(amt)
                        showCustomTipDialog = false
                    }) {
                        Text("Apply")
                    }
                },
                dismissButton = {
                    TextButton(onClick = { showCustomTipDialog = false }) {
                        Text("Cancel")
                    }
                }
            )
        }

        // Safety Net #5: AlertDialog shown when BranchHoursParser detects the branch
        // is closed right now, at the exact moment the user tapped Proceed to Checkout.
        if (showBranchClosedDialog) {
            AlertDialog(
                onDismissRequest = { showBranchClosedDialog = false },
                icon = {
                    androidx.compose.material3.Icon(
                        imageVector = androidx.compose.material.icons.Icons.Default.ShoppingCart,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.error
                    )
                },
                title = {
                    Text(
                        text = "Branch is currently closed",
                        style = MaterialTheme.typography.titleLarge,
                        color = MaterialTheme.colorScheme.error
                    )
                },
                text = {
                    val branchName = activeBranch?.name ?: "This branch"
                    val hours = activeBranch?.operatingHours
                    val hoursLine = if (!hours.isNullOrBlank()) "\nHours: $hours" else ""
                    Text(
                        text = "$branchName is not accepting orders right now.$hoursLine\n\nPlease try again during operating hours or switch to a different branch.",
                        style = MaterialTheme.typography.bodyMedium
                    )
                },
                confirmButton = {
                    TextButton(onClick = { showBranchClosedDialog = false }) {
                        Text("OK", color = com.example.foodapp.theme.VAL_BRAND_PRIMARY)
                    }
                }
            )
        }
        
        // Modal Bottom Sheet for Address Selection
        if (showAddressPickerBottomSheet) {
            androidx.compose.material3.ModalBottomSheet(
                onDismissRequest = { showAddressPickerBottomSheet = false },
                sheetState = androidx.compose.material3.rememberModalBottomSheetState(skipPartiallyExpanded = true),
                containerColor = SurfaceWhite
            ) {
                // Wrap in Box with imePadding for keyboard support
                androidx.compose.foundation.layout.Box(modifier = Modifier.fillMaxSize().imePadding()) {
                    com.example.foodapp.ui.screens.AddressMapPickerScreen(
                        onAddressSaved = { address ->
                            showAddressPickerBottomSheet = false
                            onAddressSaved(address)
                            onCheckoutRequest()
                        },
                        onNavigateBack = { showAddressPickerBottomSheet = false },
                        authState = authState
                    )
                }
            }
        }
    }
}
