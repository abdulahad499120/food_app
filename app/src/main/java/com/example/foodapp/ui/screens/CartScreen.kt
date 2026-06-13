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
import com.example.foodapp.ui.state.OrderFlowState
import androidx.compose.foundation.shape.RoundedCornerShape

@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    rewardsViewModel: com.example.foodapp.ui.state.RewardsViewModel = androidx.lifecycle.viewmodel.compose.viewModel(),
    onCheckoutRequest: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val cartState by CartManager.cartState.collectAsStateWithLifecycle()
    val userProfile by rewardsViewModel.userProfile.collectAsStateWithLifecycle()
    var showCustomTipDialog by remember { mutableStateOf(false) }
    var customTipInput by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize().background(com.example.foodapp.theme.VAL_BACKGROUND),
        containerColor = com.example.foodapp.theme.VAL_BACKGROUND,
        topBar = {
            Surface(
                color = SurfaceWhite,
                shadowElevation = 4.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Your Cart",
                    style = MaterialTheme.typography.titleLarge,
                    color = TextPrimary,
                    modifier = Modifier.padding(16.dp)
                )
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
                        if (cartState.orderFlowState == OrderFlowState.DELIVERY) {
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
                                    TextButton(
                                        onClick = { CartManager.setDriverTip(tipAmt) },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                            containerColor = if (isSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else com.example.foodapp.theme.SurfaceWhite,
                                            contentColor = if (isSelected) com.example.foodapp.theme.SurfaceWhite else com.example.foodapp.theme.TextPrimary
                                        ),
                                        modifier = Modifier.background(
                                            color = if (isSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    ) {
                                        Text(text = "Rs. ${tipAmt.toInt()}")
                                    }
                                }
                                item {
                                    val isCustomSelected = cartState.driverTip > 0 && cartState.driverTip !in tipOptions
                                    TextButton(
                                        onClick = { showCustomTipDialog = true },
                                        shape = RoundedCornerShape(16.dp),
                                        colors = androidx.compose.material3.ButtonDefaults.textButtonColors(
                                            containerColor = if (isCustomSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else com.example.foodapp.theme.SurfaceWhite,
                                            contentColor = if (isCustomSelected) com.example.foodapp.theme.SurfaceWhite else com.example.foodapp.theme.TextPrimary
                                        ),
                                        modifier = Modifier.background(
                                            color = if (isCustomSelected) com.example.foodapp.theme.VAL_BRAND_PRIMARY else Color.Transparent,
                                            shape = RoundedCornerShape(16.dp)
                                        )
                                    ) {
                                        Text(text = "Custom")
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))
                        Divider(color = DividerColor)
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
                            onClick = onCheckoutRequest,
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
                    items(cartState.items) { cartItem ->
                        ProductListItem(
                            imageUrl = cartItem.product.localImagePath,
                            title = cartItem.product.name,
                            description = "Quantity: ${cartItem.quantity}",
                            price = cartItem.product.price * cartItem.quantity,
                            onClick = {
                                CartManager.updateQuantity(cartItem.product.id, cartItem.quantity + 1)
                            }
                        )
                        Divider(color = DividerColor, modifier = Modifier.padding(horizontal = 16.dp))
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
    }
}
