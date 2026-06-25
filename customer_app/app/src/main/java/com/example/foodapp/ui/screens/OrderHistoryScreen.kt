package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import android.widget.Toast
import androidx.compose.foundation.background
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.Order
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.components.GhostButton
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.OrderHistoryUiState
import com.example.foodapp.ui.state.OrderHistoryViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.foundation.layout.BoxScope

@OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)
@Composable
fun OrderHistoryScreen(
    authState: AuthState,
    onNavigateHome: () -> Unit,
    onNavigateToCart: () -> Unit,
    onNavigateToOrderDetail: (String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: OrderHistoryViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            viewModel.loadOrders(authState.user.uid)
        }
    }

    var selectedTabIndex by androidx.compose.runtime.remember { androidx.compose.runtime.mutableIntStateOf(0) }
    val tabs = listOf("Active", "Past")
    var orderToCancel by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<Order?>(null) }

    if (orderToCancel != null) {
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { orderToCancel = null },
            title = { Text("Cancel Order") },
            text = { Text("Are you sure you want to cancel this order?") },
            confirmButton = {
                androidx.compose.material3.TextButton(
                    onClick = {
                        viewModel.cancelOrder(orderToCancel!!.orderId)
                        orderToCancel = null
                    }
                ) {
                    Text("Yes", color = com.example.foodapp.theme.ErrorRed)
                }
            },
            dismissButton = {
                androidx.compose.material3.TextButton(onClick = { orderToCancel = null }) {
                    Text("No", color = TextPrimary)
                }
            }
        )
    }

    var isRefreshing by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            if (authState is AuthState.Authenticated) {
                viewModel.loadOrders(authState.user.uid)
            }
            kotlinx.coroutines.delay(800)
            isRefreshing = false
        }
    }

    androidx.compose.material3.Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            Column {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(SurfaceWhite)
                        .padding(vertical = 16.dp, horizontal = 16.dp)
                ) {
                    Text(
                        text = "Order History",
                        style = MaterialTheme.typography.headlineMedium,
                        color = TextPrimary
                    )
                }
                androidx.compose.material3.PrimaryTabRow(
                    selectedTabIndex = selectedTabIndex,
                    containerColor = SurfaceWhite
                ) {
                    tabs.forEachIndexed { index, title ->
                        androidx.compose.material3.Tab(
                            selected = selectedTabIndex == index,
                            onClick = { selectedTabIndex = index },
                            text = { Text(title) },
                            selectedContentColor = VAL_BRAND_PRIMARY,
                            unselectedContentColor = TextSecondary
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        androidx.compose.material3.pulltorefresh.PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = onRefresh,
            modifier = Modifier.fillMaxSize().padding(paddingValues).background(VAL_BACKGROUND)
        ) {
            when (val state = uiState) {
                is OrderHistoryUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = VAL_BRAND_PRIMARY
                    )
                }
                is OrderHistoryUiState.Empty -> {
                    EmptyStateView(onNavigateHome)
                }
                is OrderHistoryUiState.Error -> {
                    ErrorStateView(state.message, authState, viewModel)
                }
                is OrderHistoryUiState.Success -> {
                    val currentList = if (selectedTabIndex == 0) state.activeOrders else state.pastOrders
                    if (currentList.isEmpty()) {
                        EmptyStateView(onNavigateHome)
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                            verticalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            items(currentList, key = { it.orderId }) { order ->
                                if (selectedTabIndex == 0) {
                                    ActiveOrderCard(
                                        order = order,
                                        onClick = { onNavigateToOrderDetail(order.orderId) },
                                        onCancelRequest = { orderToCancel = order }
                                    )
                                } else {
                                    val dismissState = androidx.compose.material3.rememberSwipeToDismissBoxState(
                                        confirmValueChange = { dismissValue ->
                                            if (dismissValue == androidx.compose.material3.SwipeToDismissBoxValue.EndToStart) {
                                                viewModel.hideOrder(order.orderId)
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
                                            Box(
                                                modifier = Modifier
                                                    .fillMaxSize()
                                                    .background(color, androidx.compose.foundation.shape.RoundedCornerShape(8.dp))
                                                    .padding(horizontal = 20.dp),
                                                contentAlignment = Alignment.CenterEnd
                                            ) {
                                                androidx.compose.material3.Icon(
                                                    imageVector = Icons.Default.Delete,
                                                    contentDescription = "Delete",
                                                    tint = androidx.compose.ui.graphics.Color.White
                                                )
                                            }
                                        }
                                    ) {
                                        PastOrderCard(
                                            order = order,
                                            onClick = { onNavigateToOrderDetail(order.orderId) },
                                            onReorder = {
                                                viewModel.reorder(order) {
                                                    Toast.makeText(context, "Order items added to cart", Toast.LENGTH_SHORT).show()
                                                    onNavigateToCart()
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
    }
}

@Composable
private fun BoxScope.EmptyStateView(onNavigateHome: () -> Unit) {
    Column(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        androidx.compose.foundation.Image(
            painter = androidx.compose.ui.res.painterResource(id = com.ahad.foodapp.R.drawable.empty_cart_illustration),
            contentDescription = "Empty State",
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text("No orders found.", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
        Spacer(modifier = Modifier.height(8.dp))
        Text("When you place orders, they will appear here.", style = MaterialTheme.typography.bodyMedium, color = TextSecondary, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        Spacer(modifier = Modifier.height(24.dp))
        PrimaryButton(onClick = onNavigateHome) { Text("Browse Menu") }
    }
}

@Composable
private fun BoxScope.ErrorStateView(message: String, authState: AuthState, viewModel: OrderHistoryViewModel) {
    Column(
        modifier = Modifier.align(Alignment.Center).padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(message, color = com.example.foodapp.theme.ErrorRed, style = MaterialTheme.typography.bodyMedium)
        Spacer(modifier = Modifier.height(16.dp))
        GhostButton(onClick = { if (authState is AuthState.Authenticated) viewModel.loadOrders(authState.user.uid) }) { Text("Retry") }
    }
}

@Composable
fun ActiveOrderCard(order: Order, onClick: () -> Unit, onCancelRequest: () -> Unit) {
    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = formatter.format(order.timestamp ?: Date())
    val summary = order.items.take(3).joinToString(separator = ", ") { "${it.quantity}x ${it.product.name}" } + if (order.items.size > 3) ", and more..." else ""

    Card(
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Order #${order.orderId.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = order.orderStatus.name.replace("_", " "), style = MaterialTheme.typography.titleSmall, color = VAL_BRAND_PRIMARY)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = formattedDate, style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            androidx.compose.material3.OutlinedButton(
                onClick = onCancelRequest,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Request Cancellation", color = com.example.foodapp.theme.ErrorRed)
            }
        }
    }
}

@Composable
fun PastOrderCard(order: Order, onClick: () -> Unit, onReorder: () -> Unit) {
    val formatter = SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault())
    val formattedDate = formatter.format(order.timestamp ?: Date())
    val summary = order.items.take(3).joinToString(separator = ", ") { "${it.quantity}x ${it.product.name}" } + if (order.items.size > 3) ", and more..." else ""

    Card(
        modifier = Modifier.fillMaxWidth().bounceClick { onClick() },
        colors = CardDefaults.cardColors(containerColor = SurfaceWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                Text(text = "Order #${order.orderId.takeLast(6).uppercase()}", style = MaterialTheme.typography.titleMedium, color = TextPrimary)
                Text(text = "Rs. ${order.totalAmount.toInt()}", style = MaterialTheme.typography.titleMedium, color = VAL_BRAND_PRIMARY)
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text(text = "${order.orderStatus.name.replace("_", " ")} • $formattedDate", style = MaterialTheme.typography.labelSmall, color = TextSecondary)
            Spacer(modifier = Modifier.height(12.dp))
            Text(text = summary, style = MaterialTheme.typography.bodyMedium, color = TextPrimary)
            Spacer(modifier = Modifier.height(16.dp))
            GhostButton(onClick = onReorder, modifier = Modifier.fillMaxWidth()) {
                Text("Reorder")
            }
        }
    }
}
