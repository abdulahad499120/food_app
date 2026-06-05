package com.example.foodapp.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.ui.components.PrimaryButton

@Composable
fun OrderSuccessScreen(
    onNavigateHome: () -> Unit,
    modifier: Modifier = Modifier
) {
    var isAnimated by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "ScaleAnimation"
    )

    val alpha by animateFloatAsState(
        targetValue = if (isAnimated) 1f else 0f,
        animationSpec = tween(durationMillis = 800),
        label = "AlphaAnimation"
    )

    LaunchedEffect(Unit) {
        isAnimated = true
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .background(BrandPrimary.copy(alpha = 0.1f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .background(BrandPrimary.copy(alpha = 0.2f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(60.dp)
                        .background(BrandPrimary, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Success",
                        tint = SurfaceWhite,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        
        Text(
            text = "Order Placed Successfully!",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = TextPrimary,
            modifier = Modifier.alpha(alpha)
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Your food is being prepared and will be delivered to your address soon.",
            style = MaterialTheme.typography.bodyLarge,
            color = TextSecondary,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(alpha)
        )
        
        Spacer(modifier = Modifier.height(48.dp))
        
        PrimaryButton(
            onClick = onNavigateHome,
            modifier = Modifier
                .fillMaxWidth()
                .alpha(alpha)
        ) {
            Text("Back to Home")
        }
    }
}
