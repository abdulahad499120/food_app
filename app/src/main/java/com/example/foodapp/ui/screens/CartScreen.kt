package com.example.foodapp.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
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

@Composable
fun CartScreen(
    modifier: Modifier = Modifier,
    onCheckoutRequest: () -> Unit = {},
    onNavigateToHome: () -> Unit = {}
) {
    val cartState by CartManager.cartState.collectAsState()

    Scaffold(
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
                    icon = Icons.Default.ShoppingCart,
                    title = "Your cart is empty",
                    message = "Looks like you haven't added any delicious food yet.",
                    actionText = "Browse Menu",
                    onActionClick = onNavigateToHome,
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
    }
}
