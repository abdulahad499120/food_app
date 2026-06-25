package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodapp.data.models.UserProfile
import com.example.foodapp.ui.components.PrimaryButton

@Composable
fun ProfileScreen(
    user: UserProfile,
    onLogout: () -> Unit,
    onNavigateToOrderHistory: () -> Unit = {},
    onNavigateToAddresses: () -> Unit = {},
    onNavigateToPayments: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    LazyColumn(
        modifier = modifier
            .fillMaxSize()
            .background(com.example.foodapp.theme.VAL_BACKGROUND)
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            // Profile Header
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primaryContainer),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = user.name.take(1).uppercase(),
                    fontSize = 40.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onPrimaryContainer
                )
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = user.name,
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = Color.Black
            )
            if (!user.email.isNullOrEmpty()) {
                Text(
                    text = user.email,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
            if (!user.phoneNumber.isNullOrEmpty()) {
                Text(
                    text = user.phoneNumber,
                    style = MaterialTheme.typography.bodyLarge,
                    color = Color.Gray
                )
            }
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Profile Menu Items
        item {
            ProfileMenuItem(icon = Icons.Default.ShoppingBag, title = "Order History", onClick = onNavigateToOrderHistory)
            ProfileMenuItem(icon = Icons.Default.LocationOn, title = "Saved Addresses", onClick = onNavigateToAddresses)
            ProfileMenuItem(icon = Icons.Default.Payment, title = "Payment Methods", onClick = onNavigateToPayments)
            ProfileMenuItem(icon = Icons.Default.Settings, title = "Settings")
            ProfileMenuItem(icon = Icons.Default.Help, title = "Help & Support")
            
            Spacer(modifier = Modifier.height(32.dp))
        }

        // Logout Button
        item {
            PrimaryButton(
                onClick = onLogout,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Logout")
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
fun ProfileMenuItem(
    icon: ImageVector,
    title: String,
    onClick: () -> Unit = {}
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .bounceClick { onClick() },
        colors = CardDefaults.cardColors(containerColor = Color.White),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
