package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodapp.data.models.CartItem
import com.example.foodapp.data.models.Product
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.PrimaryButton
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@Composable
fun DeepCustomizerSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCartClick: (CartItem) -> Unit,
    isGuestViewing: Boolean = false,
    isFavorite: Boolean = false,
    onToggleFavorite: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    // Customization states
    var selectedSize by remember { mutableStateOf("Regular") }
    
    // Standard Recipe Block States
    var standardMilk by remember { mutableStateOf("Standard Milk") }
    var standardSyrupPumps by remember { mutableStateOf(2) }

    // Add-in Modifiers
    var extraToppings by remember { mutableStateOf(0) }
    var extraScoops by remember { mutableStateOf(0) }

    // Cost calculations
    val sizeBump = when (selectedSize) {
        "Large" -> 100.0
        "Family" -> 150.0
        else -> 0.0
    }
    
    // Calculate final dynamic price
    val toppingsBump = extraToppings * 20.0
    val scoopsBump = extraScoops * 50.0
    val finalPrice = product.price + sizeBump + toppingsBump + scoopsBump

    // Calculate dynamic calories
    val currentCalories = remember(selectedSize, extraToppings, extraScoops, standardSyrupPumps) {
        val base = product.baseCalories
        val largeBonus = if (selectedSize == "Large" || selectedSize == "Family") product.largeCalorieBonus else 0
        val toppingsCals = extraToppings * (product.ingredientCalorieMap["extraToppings"] ?: 0)
        base + largeBonus + toppingsCals
    }

    Box(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 100.dp) // Padding for sticky footer
        ) {
            // Hero Image
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(280.dp)
                    .background(DividerColor)
            ) {
                @OptIn(ExperimentalSharedTransitionApi::class)
                val sharedTransitionScope = com.example.foodapp.ui.navigation.LocalSharedTransitionScope.current
                @OptIn(ExperimentalSharedTransitionApi::class)
                val animatedVisibilityScope = com.example.foodapp.ui.navigation.LocalAnimatedVisibilityScope.current

                @OptIn(ExperimentalSharedTransitionApi::class)
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = product.localImagePath,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize().sharedElement(
                                rememberSharedContentState(key = "image_${product.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    AsyncImage(
                        model = product.localImagePath,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                
                if (onToggleFavorite != null) {
                    val transition = updateTransition(targetState = isFavorite, label = "FavoriteTransitionSheet")
                    
                    val heartScale by transition.animateFloat(
                        transitionSpec = {
                            androidx.compose.animation.core.spring(
                                dampingRatio = androidx.compose.animation.core.Spring.DampingRatioMediumBouncy,
                                stiffness = androidx.compose.animation.core.Spring.StiffnessLow
                            )
                        },
                        label = "HeartScaleSheet"
                    ) { favorite ->
                        if (favorite) 1.2f else 1.0f
                    }

                    val heartColor by transition.animateColor(
                        transitionSpec = { androidx.compose.animation.core.tween(durationMillis = 300) },
                        label = "HeartColorSheet"
                    ) { favorite ->
                        if (favorite) VAL_BRAND_PRIMARY else Color.Gray
                    }

                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(16.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.8f)),
                        contentAlignment = Alignment.Center
                    ) {
                        IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                                contentDescription = "Favorite",
                                tint = heartColor,
                                modifier = Modifier
                                    .size(24.dp)
                                    .scale(heartScale)
                            )
                        }
                    }
                }
            }

            // Product Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineMedium, // Semantic H2
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "$currentCalories calories",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.SemiBold,
                    color = TextSecondary
                )
            }

            HorizontalDivider(color = DividerColor, thickness = 8.dp)

            // Customization: Size Options
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Size options",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(24.dp))

                val sizes = listOf(
                    "Mini" to 24.dp,
                    "Small" to 32.dp,
                    "Regular" to 40.dp,
                    "Large" to 48.dp
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.Bottom
                ) {
                    sizes.forEach { (size, heightDp) ->
                        val isSelected = selectedSize == size
                        val sizeIconColor = if (isSelected) VAL_BRAND_PRIMARY else TextSecondary
                        val sizeTextColor = if (isSelected) TextPrimary else TextSecondary
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier.weight(1f)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(width = 40.dp, height = heightDp)
                                    .clip(RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp))
                                    .background(sizeIconColor.copy(alpha = 0.1f))
                                    .border(
                                        width = if (isSelected) 2.dp else 1.dp,
                                        color = sizeIconColor,
                                        shape = RoundedCornerShape(bottomStart = 8.dp, bottomEnd = 8.dp)
                                    )
                                    .bounceClick { selectedSize = size }
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            TextButton(onClick = { selectedSize = size }) {
                                Text(
                                    text = size,
                                    style = MaterialTheme.typography.bodyMedium,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = sizeTextColor
                                )
                            }
                        }
                    }
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 8.dp)

            // Interactive Standard Recipe Block
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Standard Recipe",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                CustomizationAccordion(title = "Milk", subtitle = standardMilk) {
                    val milkOptions = listOf("Standard Milk", "Almond Milk", "Oat Milk", "Soy Milk")
                    milkOptions.forEach { milk ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .bounceClick { standardMilk = milk }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = standardMilk == milk,
                                onClick = { standardMilk = milk },
                                colors = RadioButtonDefaults.colors(selectedColor = VAL_BRAND_PRIMARY)
                            )
                            Text(text = milk, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                HorizontalDivider(color = DividerColor, thickness = 1.dp)
                
                CustomizationAccordion(title = "Syrup", subtitle = "$standardSyrupPumps pump(s)") {
                    ModifierStepper(
                        label = "Classic Syrup Pumps",
                        value = standardSyrupPumps,
                        onDecrease = { if (standardSyrupPumps > 0) standardSyrupPumps-- },
                        onIncrease = { standardSyrupPumps++ }
                    )
                }
            }

            HorizontalDivider(color = DividerColor, thickness = 8.dp)

            // Add-in Modifiers
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Add-ins",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                ModifierStepper(
                    label = "Extra Toppings (+$20)",
                    value = extraToppings,
                    onDecrease = { if (extraToppings > 0) extraToppings-- },
                    onIncrease = { extraToppings++ }
                )
                HorizontalDivider(color = DividerColor, thickness = 1.dp, modifier = Modifier.padding(vertical = 8.dp))
                ModifierStepper(
                    label = "Extra Dry Fruit Scoops (+$50)",
                    value = extraScoops,
                    onDecrease = { if (extraScoops > 0) extraScoops-- },
                    onIncrease = { extraScoops++ }
                )
            }
        }

        // Persistent Sticky Bottom CTA
        Surface(
            color = SurfaceWhite,
            shadowElevation = 16.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
        ) {
            Box(modifier = Modifier.padding(16.dp)) {
                if (isGuestViewing) {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = DividerColor)
                    ) {
                        Text(text = "Choose a store to add to order", color = TextSecondary)
                    }
                } else if (product.isAvailable) {
                    PrimaryButton(
                        onClick = {
                            val cartItem = CartItem(
                                product = product,
                                quantity = 1,
                                size = selectedSize,
                                sweetness = standardSyrupPumps,
                                extraToppings = extraToppings,
                                nutType = standardMilk,
                                scoops = 2 + extraScoops
                            )
                            onAddToCartClick(cartItem)
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp)
                    ) {
                        Text(text = "Add to Order • Rs. ${finalPrice.toInt()}", style = MaterialTheme.typography.labelLarge)
                    }
                } else {
                    Button(
                        onClick = {},
                        enabled = false,
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(disabledContainerColor = DividerColor)
                    ) {
                        Text(text = "Sold Out at this Store", color = TextSecondary)
                    }
                }
            }
        }
    }
}

@Composable
fun ModifierStepper(
    label: String,
    value: Int,
    onDecrease: () -> Unit,
    onIncrease: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = label, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(modifier = Modifier.clip(CircleShape).background(if (value > 0) Color.LightGray.copy(alpha = 0.3f) else Color.Transparent)) {
                IconButton(onClick = onDecrease) {
                    Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (value > 0) VAL_BRAND_PRIMARY else TextSecondary)
                }
            }
            Text(
                text = value.toString(), 
                modifier = Modifier.padding(horizontal = 16.dp),
                style = MaterialTheme.typography.bodyLarge,
                fontWeight = FontWeight.Bold
            )
            Box(modifier = Modifier.clip(CircleShape).background(Color.LightGray.copy(alpha = 0.3f))) {
                IconButton(onClick = onIncrease) {
                    Icon(Icons.Default.Add, contentDescription = "Increase", tint = VAL_BRAND_PRIMARY)
                }
            }
        }
    }
}

@Composable
fun CustomizationAccordion(
    title: String,
    subtitle: String,
    content: @Composable () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val rotationState by animateFloatAsState(targetValue = if (expanded) 180f else 0f, label = "accordion_rotation")

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .bounceClick { expanded = !expanded }
                .padding(vertical = 16.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(text = title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = TextPrimary)
                if (!expanded) {
                    Text(text = subtitle, style = MaterialTheme.typography.bodyMedium, color = TextSecondary)
                }
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowDown,
                contentDescription = "Expand",
                tint = VAL_BRAND_PRIMARY,
                modifier = Modifier.rotate(rotationState)
            )
        }
        AnimatedVisibility(visible = expanded) {
            Column(modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 16.dp)) {
                content()
            }
        }
    }
}
