package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.data.models.RedemptionItem
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.ui.state.RewardsViewModel

// Soft, high-end palette
private val CreamBackground = Color(0xFFFFFDF5)
private val GoldAccent = Color(0xFFD4AF37)

/**
 * ### Rewards Dashboard
 * Primary screen for the Gamified Rewards Ledger.
 * Provides a highly premium aesthetic with `Cream`, `Gold`, and `Emerald` tokens.
 * Data is driven instantaneously via `StateFlow` from `RewardsViewModel`.
 */
@Composable
fun RewardsScreen(
    viewModel: RewardsViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val userProfile by viewModel.userProfile.collectAsState()
    val redemptionItems by viewModel.redemptionItems.collectAsState()

    val starsBalance = userProfile.starsBalance
    val activeTier = userProfile.loyaltyTier

    var redemptionSuccess by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = VAL_BACKGROUND,
        modifier = modifier.fillMaxSize()
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(24.dp))
            
            // Progression Ring
            ProgressionRing(starsBalance = starsBalance)

            Spacer(modifier = Modifier.height(32.dp))

            // Tier Horizontal Row
            TierRow(activeTier = activeTier)

            Spacer(modifier = Modifier.height(32.dp))

            // Redemption Gallery
            Text(
                text = "Redeem Your Stars",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold,
                    color = com.example.foodapp.theme.TextPrimary
                ),
                modifier = Modifier.fillMaxWidth()
            )
            
            Spacer(modifier = Modifier.height(16.dp))

            LazyColumn(
                contentPadding = PaddingValues(bottom = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(redemptionItems, key = { it.id }) { item ->
                    RedemptionCard(
                        item = item, 
                        currentStars = starsBalance,
                        onRedeem = { redemptionSuccess = true }
                    )
                }
            }
        }
        
        if (redemptionSuccess) {
            RedemptionSuccessOverlay(onDismiss = { redemptionSuccess = false })
        }
    }
}

/**
 * #### Progression Ring
 * Circular indicator displaying the user's current star balance.
 */
@Composable
fun ProgressionRing(starsBalance: Int) {
    Box(contentAlignment = Alignment.Center, modifier = Modifier.size(160.dp)) {
        CircularProgressIndicator(
            progress = { 1f },
            modifier = Modifier.fillMaxSize(),
            color = Color.LightGray.copy(alpha = 0.3f),
            strokeWidth = 8.dp,
        )
        // Dummy progress calculation (e.g. out of 400 stars max)
        val progress = (starsBalance / 400f).coerceIn(0f, 1f)
        CircularProgressIndicator(
            progress = { progress },
            modifier = Modifier.fillMaxSize(),
            color = GoldAccent,
            strokeWidth = 8.dp,
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = starsBalance.toString(),
                style = MaterialTheme.typography.displayMedium.copy(
                    fontWeight = FontWeight.ExtraBold,
                    color = VAL_BRAND_PRIMARY
                )
            )
            Text(
                text = "Stars",
                style = MaterialTheme.typography.titleMedium.copy(color = com.example.foodapp.theme.TextSecondary)
            )
        }
    }
}

/**
 * #### Tier Row
 * Horizontal indicator displaying available loyalty tiers.
 * The active tier uses `AnimatedContent` to seamlessly transition its border and typography.
 */
@Composable
fun TierRow(activeTier: String) {
    val tiers = listOf("Green", "Gold", "Reserve")

    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(16.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        items(tiers) { tier ->
            val isActive = tier == activeTier
            AnimatedContent(
                targetState = isActive,
                transitionSpec = {
                    (fadeIn(animationSpec = tween(300)) + scaleIn(initialScale = 0.95f)) togetherWith fadeOut(animationSpec = tween(300))
                },
                label = "TierTransition"
            ) { stateIsActive ->
                val borderModifier = if (stateIsActive) {
                    Modifier.border(2.dp, VAL_BRAND_PRIMARY, RoundedCornerShape(24.dp))
                } else {
                    Modifier.border(1.dp, Color.LightGray, RoundedCornerShape(24.dp))
                }

                Box(
                    modifier = Modifier
                        .then(borderModifier)
                        .background(
                            if (stateIsActive) VAL_BRAND_PRIMARY.copy(alpha = 0.1f) else Color.Transparent,
                            RoundedCornerShape(24.dp)
                        )
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tier,
                        style = MaterialTheme.typography.labelLarge.copy(
                            fontWeight = if (stateIsActive) FontWeight.Bold else FontWeight.Medium,
                            color = if (stateIsActive) VAL_BRAND_PRIMARY else Color.Gray
                        )
                    )
                }
            }
        }
    }
}

/**
 * #### Redemption Card
 * Displays an individual reward item. Automatically dims out unaffordable items.
 */
@Composable
fun RedemptionCard(item: RedemptionItem, currentStars: Int, onRedeem: () -> Unit) {
    val canRedeem = currentStars >= item.costInStars

    Card(
        colors = CardDefaults.cardColors(containerColor = Color.White),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .alpha(if (canRedeem) 1f else 0.6f)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = item.itemName,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.SemiBold,
                        color = com.example.foodapp.theme.TextPrimary
                    )
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${item.costInStars} Stars",
                    style = MaterialTheme.typography.labelMedium.copy(color = GoldAccent)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            if (canRedeem) {
                Button(
                    onClick = onRedeem,
                    colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY)
                ) {
                    Text(text = "Redeem")
                }
            } else {
                Column(horizontalAlignment = Alignment.End) {
                    Text(
                        text = "$currentStars / ${item.costInStars} stars",
                        style = MaterialTheme.typography.labelSmall.copy(color = Color.Gray)
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    val progress = (currentStars.toFloat() / item.costInStars.toFloat()).coerceIn(0f, 1f)
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier
                            .width(80.dp)
                            .height(6.dp),
                        color = GoldAccent,
                        trackColor = Color.LightGray.copy(alpha = 0.5f)
                    )
                }
            }
        }
    }
}

/**
 * #### Redemption Success Overlay
 * Displays a full-screen or prominent animated success state upon redeeming an item.
 */
@Composable
fun RedemptionSuccessOverlay(onDismiss: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.6f))
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        AnimatedContent(
            targetState = true,
            transitionSpec = {
                (fadeIn(animationSpec = tween(400)) + scaleIn(initialScale = 0.8f)) togetherWith fadeOut()
            },
            label = "RedemptionSuccess"
        ) { _ ->
            Card(
                colors = CardDefaults.cardColors(containerColor = CreamBackground),
                shape = RoundedCornerShape(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Redemption Successful!",
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold, color = com.example.foodapp.theme.TextPrimary)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Your reward has been applied to your account.",
                        style = MaterialTheme.typography.bodyMedium.copy(color = Color.DarkGray)
                    )
                    Spacer(modifier = Modifier.height(24.dp))
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("Awesome")
                    }
                }
            }
        }
    }
}
