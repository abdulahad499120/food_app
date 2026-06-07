package com.example.foodapp.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodapp.data.models.CartItem
import com.example.foodapp.data.models.Product
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.PrimaryButton

@Composable
fun ProductDetailSheet(
    product: Product,
    onDismiss: () -> Unit,
    onAddToCartClick: (CartItem) -> Unit,
    isGuestViewing: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Customization states
    var selectedSize by remember { mutableStateOf("Regular") }
    var sweetness by remember { mutableStateOf(2) }
    var extraToppings by remember { mutableStateOf(0) }
    var nutType by remember { mutableStateOf("Mixed Nuts") }
    var scoops by remember { mutableStateOf(2) }

    // Cost calculations
    val sizeBump = when (selectedSize) {
        "Large", "Family" -> 100.0
        else -> 0.0
    }
    
    // Calculate final dynamic price
    val toppingsBump = extraToppings * 20.0
    val scoopsBump = if (scoops > 2) (scoops - 2) * 50.0 else 0.0
    val finalPrice = product.price + sizeBump + toppingsBump + scoopsBump

    // Calculate dynamic calories
    val currentCalories = remember(selectedSize, extraToppings, scoops) {
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
                AsyncImage(
                    model = product.localImagePath,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }

            // Product Header
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.headlineMedium,
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
                Spacer(modifier = Modifier.height(16.dp))

                val sizes = listOf("Mini", "Small", "Regular", "Large")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    sizes.forEach { size ->
                        val isSelected = selectedSize == size
                        val sizeIconColor = if (isSelected) VAL_BRAND_PRIMARY else TextSecondary
                        val sizeTextColor = if (isSelected) TextPrimary else TextSecondary
                        
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { selectedSize = size }
                                .padding(8.dp)
                        ) {
                            // Mocking silhouette with a circle
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(sizeIconColor.copy(alpha = 0.2f))
                                    .border(2.dp, sizeIconColor, CircleShape)
                            )
                            Spacer(modifier = Modifier.height(8.dp))
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

            HorizontalDivider(color = DividerColor, thickness = 8.dp)

            // Customization: What's Included Accordions
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "What's included",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = TextPrimary
                )
                Spacer(modifier = Modifier.height(8.dp))

                // Nut Options
                CustomizationAccordion(title = "Nuts", subtitle = nutType) {
                    val nuts = listOf("Mixed Nuts", "Almonds", "Cashews", "Pistachios", "Walnuts")
                    nuts.forEach { nut ->
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { nutType = nut }
                                .padding(vertical = 8.dp)
                        ) {
                            RadioButton(
                                selected = nutType == nut,
                                onClick = { nutType = nut },
                                colors = RadioButtonDefaults.colors(selectedColor = VAL_BRAND_PRIMARY)
                            )
                            Text(text = nut, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                }
                HorizontalDivider(color = DividerColor, thickness = 1.dp)

                // Scoops Options
                CustomizationAccordion(title = "Dry Fruit Scoops", subtitle = "$scoops Scoop(s)") {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Extra Premium Scoops", fontWeight = FontWeight.Bold)
                            Text("Premium Mixed Dry Fruits", style = MaterialTheme.typography.bodyMedium)
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (scoops > 1) scoops-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (scoops > 1) VAL_BRAND_PRIMARY else TextSecondary)
                            }
                            Text(text = scoops.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { scoops++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = VAL_BRAND_PRIMARY)
                            }
                        }
                    }
                }
                
                HorizontalDivider(color = DividerColor, thickness = 1.dp)

                // Syrups / Sweetness
                CustomizationAccordion(title = "Flavors & Sweeteners", subtitle = "Sweetness: $sweetness pump(s)") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Classic Syrup", style = MaterialTheme.typography.bodyMedium)
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(onClick = { if (sweetness > 0) sweetness-- }) {
                                Icon(Icons.Default.Remove, contentDescription = "Decrease", tint = if (sweetness > 0) VAL_BRAND_PRIMARY else TextSecondary)
                            }
                            Text(text = sweetness.toString(), modifier = Modifier.padding(horizontal = 8.dp))
                            IconButton(onClick = { sweetness++ }) {
                                Icon(Icons.Default.Add, contentDescription = "Increase", tint = VAL_BRAND_PRIMARY)
                            }
                        }
                    }
                }
            }
        }

        // Sticky Footer
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
                                sweetness = sweetness,
                                extraToppings = extraToppings,
                                nutType = nutType,
                                scoops = scoops
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
                .clickable { expanded = !expanded }
                .padding(vertical = 16.dp),
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
            Column(modifier = Modifier.padding(bottom = 16.dp)) {
                content()
            }
        }
    }
}
