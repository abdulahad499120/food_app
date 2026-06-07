package com.example.foodapp.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.BrandSecondary
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.AuthState

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    authState: AuthState = AuthState.Unauthenticated,
    cartItemCount: Int = 0,
    onNavigateToLocator: () -> Unit = {},
    onNavigateToAuth: () -> Unit = {},
    onNavigateToOrder: () -> Unit = {},
    onNavigateToCart: () -> Unit = {}
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .verticalScroll(scrollState)
    ) {
        // Top App Bar Elements (Rendered inline for Home Hub)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Left: Sign In or Greeting
            if (authState is AuthState.Unauthenticated) {
                Text(
                    text = "Sign in",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = VAL_BRAND_PRIMARY,
                    modifier = Modifier.clickable { onNavigateToAuth() }
                )
            } else {
                Text(
                    text = "Welcome, ${(authState as? AuthState.Authenticated)?.user?.name ?: "Member"}",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Center: Stores Pin
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.clickable { onNavigateToLocator() }
            ) {
                Icon(
                    imageVector = Icons.Default.LocationOn,
                    contentDescription = "Stores",
                    tint = VAL_BRAND_PRIMARY,
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Stores",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
            }

            // Right: Profile Avatar
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                .background(Color.LightGray.copy(alpha = 0.3f))
                .clickable { onNavigateToCart() },
            contentAlignment = Alignment.Center
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

        Spacer(modifier = Modifier.height(16.dp))

        // Hero Unit
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = CardDefaults.cardColors(containerColor = VAL_BRAND_PRIMARY.copy(alpha = 0.1f)),
            elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                // In a real app this would be an Image. Using Box placeholder for now to match 1:1 structure.
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.5f)
                        .background(VAL_BRAND_PRIMARY.copy(alpha = 0.3f))
                )
                
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "Summer's here. Dive right in.",
                        style = MaterialTheme.typography.headlineMedium, // H2 semantic equivalent
                        fontWeight = FontWeight.ExtraBold,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Cool off with our vibrant, refreshing new summer lineup.",
                        style = MaterialTheme.typography.bodyLarge,
                        color = TextPrimary
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = { onNavigateToOrder() },
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Explore the summer menu", modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp))
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Loyalty Conversion Module
        if (authState is AuthState.Unauthenticated) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(containerColor = VAL_SURFACE_DARK),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text(
                        text = "ICE LAND REWARDS",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = Color.White.copy(alpha = 0.7f),
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Let us treat you",
                        style = MaterialTheme.typography.headlineSmall, // H3 semantic
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Earn Stars, get free ice creams, dry fruits, and enjoy exclusive perks. Join today!",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.9f)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Row {
                        Button(
                            onClick = { onNavigateToAuth() },
                            colors = ButtonDefaults.buttonColors(containerColor = Color.White),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Join now", color = VAL_SURFACE_DARK, fontWeight = FontWeight.Bold)
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        OutlinedButton(
                            onClick = { onNavigateToAuth() },
                            colors = ButtonDefaults.outlinedButtonColors(contentColor = Color.White),
                            border = androidx.compose.foundation.BorderStroke(1.dp, Color.White),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text("Sign in", fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(32.dp))
    }
}
