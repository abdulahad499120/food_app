package com.example.foodapp.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.ShoppingBag
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary

@Composable
fun StoreSelectionStrip(
    activeBranchName: String?,
    cartItemCount: Int,
    onStoreClick: () -> Unit,
    onCartClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .shadow(elevation = 8.dp, spotColor = Color.Black.copy(alpha = 0.05f)),
        color = SurfaceWhite
    ) {
        Column(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            // Pickup / Delivery Toggle
            var isPickup by remember { mutableStateOf(true) }
            Row(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                Surface(
                    shape = RoundedCornerShape(24.dp),
                    color = Color.LightGray.copy(alpha = 0.3f)
                ) {
                    Row(modifier = Modifier.padding(4.dp)) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (isPickup) SurfaceWhite else Color.Transparent)
                                .clickable { isPickup = true }
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            Text("Pickup", fontWeight = FontWeight.Bold, color = if (isPickup) TextPrimary else Color.Gray)
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(20.dp))
                                .background(if (!isPickup) SurfaceWhite else Color.Transparent)
                                .clickable { isPickup = false }
                                .padding(horizontal = 32.dp, vertical = 8.dp)
                        ) {
                            Text("Delivery", fontWeight = FontWeight.Bold, color = if (!isPickup) TextPrimary else Color.Gray)
                        }
                    }
                }
            }

            // Store Info & Cart
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onStoreClick() },
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                // Left Side: Store Selection
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = activeBranchName ?: "Choose a store",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowRight,
                        contentDescription = "Select Store",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }

                // Right Side: Floating Cart Bubble
                Box(
                    modifier = Modifier
                        .clip(CircleShape)
                        .background(BrandPrimary)
                        .clickable { onCartClick() }
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.ShoppingBag,
                            contentDescription = "Cart",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        if (cartItemCount > 0) {
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = cartItemCount.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }
    }
}
