package com.example.foodapp.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.foodapp.data.models.Gift
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import kotlinx.coroutines.delay

@Composable
fun GiftRevealOverlay(
    pendingGift: Gift?,
    onClaimGift: (Gift) -> Unit
) {
    var isVisible by remember { mutableStateOf(false) }
    var isOpened by remember { mutableStateOf(false) }

    LaunchedEffect(pendingGift) {
        if (pendingGift != null) {
            isVisible = true
            isOpened = false
            // Auto open the "envelope" after a short delay for dramatic effect
            delay(800)
            isOpened = true
        } else {
            isVisible = false
            isOpened = false
        }
    }

    AnimatedVisibility(
        visible = isVisible && pendingGift != null,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it }),
        exit = fadeOut() + slideOutVertically(targetOffsetY = { it })
    ) {
        pendingGift?.let { gift ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.85f)),
                contentAlignment = Alignment.Center
            ) {
                // Spring animated scale for the card popping out
                val scale by animateFloatAsState(
                    targetValue = if (isOpened) 1f else 0.5f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "CardPopAnimation"
                )

                Column(
                    modifier = Modifier.padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "You've received a gift!",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "From: ${gift.senderName}",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.LightGray
                    )
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    // The Gift Card Image
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .aspectRatio(1.5f)
                            .scale(scale)
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color.DarkGray)
                    ) {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(gift.themeUrl)
                                .crossfade(true)
                                .build(),
                            contentDescription = "Gift Theme",
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                        // Mock background if remote image fails
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color(0xFFE8F5E9).copy(alpha = 0.5f))
                        )
                    }

                    Spacer(modifier = Modifier.height(32.dp))
                    
                    // The Custom Message
                    if (gift.message.isNotBlank() && isOpened) {
                        Surface(
                            color = Color.White.copy(alpha = 0.1f),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = "\"${gift.message}\"",
                                style = MaterialTheme.typography.bodyLarge,
                                color = Color.White,
                                modifier = Modifier.padding(16.dp),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                    
                    Spacer(modifier = Modifier.height(48.dp))
                    
                    if (isOpened) {
                        Button(
                            onClick = { onClaimGift(gift) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                            shape = RoundedCornerShape(50)
                        ) {
                            Text(
                                "Add Rs. ${gift.amount.toInt()} to Wallet",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}
