package com.example.foodapp.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.OrderFlowState

@Composable
fun OrderTopBar(
    orderFlowState: OrderFlowState,
    onStateChange: (OrderFlowState) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f))
                .padding(4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                val isPickup = orderFlowState == OrderFlowState.PICKUP
                val pickupBgColor by animateColorAsState(if (isPickup) VAL_BRAND_PRIMARY else Color.Transparent, label = "pickupBg")
                val pickupTextColor by animateColorAsState(if (isPickup) Color.White else TextPrimary, label = "pickupText")

                val isDelivery = orderFlowState == OrderFlowState.DELIVERY
                val deliveryBgColor by animateColorAsState(if (isDelivery) VAL_BRAND_PRIMARY else Color.Transparent, label = "deliveryBg")
                val deliveryTextColor by animateColorAsState(if (isDelivery) Color.White else TextPrimary, label = "deliveryText")

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(pickupBgColor)
                        .clickable { onStateChange(OrderFlowState.PICKUP) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Pickup",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = pickupTextColor
                    )
                }

                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(deliveryBgColor)
                        .clickable { onStateChange(OrderFlowState.DELIVERY) }
                        .padding(horizontal = 24.dp, vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Delivery",
                        style = MaterialTheme.typography.bodyMedium,
                        fontWeight = FontWeight.Bold,
                        color = deliveryTextColor
                    )
                }
            }
        }
    }
}
