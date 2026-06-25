package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import com.example.foodapp.data.models.GiftTemplate
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.state.GiftViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GiftScreen(
    viewModel: GiftViewModel = viewModel(),
    modifier: Modifier = Modifier
) {
    val templates by viewModel.templates.collectAsStateWithLifecycle()
    
    var selectedTemplate by remember { mutableStateOf<GiftTemplate?>(null) }
    var selectedAmount by remember { mutableStateOf<Int>(1000) }
    var customAmountText by remember { mutableStateOf("") }
    var isCustomAmount by remember { mutableStateOf(false) }
    
    var recipientEmail by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = androidx.compose.ui.platform.LocalContext.current
    var errorBannerText by remember { mutableStateOf<String?>(null) }
    
    // Auto-select first template when loaded
    LaunchedEffect(templates) {
        if (selectedTemplate == null && templates.isNotEmpty()) {
            selectedTemplate = templates.first()
        }
    }

    val finalAmount = if (isCustomAmount) customAmountText.toIntOrNull() ?: 0 else selectedAmount

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = VAL_BACKGROUND,
        modifier = modifier.fillMaxSize(),
        bottomBar = {
            Surface(
                color = Color.White,
                shadowElevation = 16.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(16.dp)) {
                    Button(
                        onClick = {
                            if (selectedTemplate == null || recipientEmail.isBlank() || finalAmount <= 0) {
                                scope.launch { snackbarHostState.showSnackbar("Please fill all fields properly.") }
                                return@Button
                            }
                            viewModel.submitGift(
                                recipientName = "Friend", // Fallback, we only ask for email now
                                recipientEmail = recipientEmail,
                                amount = finalAmount,
                                message = message,
                                templateId = selectedTemplate!!.templateId,
                                context = context,
                                onSuccess = {
                                    errorBannerText = null
                                    scope.launch { snackbarHostState.showSnackbar("Gift sent successfully!") }
                                    recipientEmail = ""
                                    message = ""
                                },
                                onError = {
                                    errorBannerText = it.message ?: "Failed to send gift"
                                }
                            )
                        },
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                        shape = RoundedCornerShape(50)
                    ) {
                        Text("Send Gift - Rs. $finalAmount", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
        ) {
            com.example.foodapp.ui.components.PremiumErrorBanner(
                errorMessage = errorBannerText ?: "",
                isVisible = errorBannerText != null,
                modifier = Modifier.padding(16.dp)
            )
            Text(
                text = "Digital E-Gifting",
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.Bold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(top = 16.dp)
            )
            Text(
                text = "Send a delightful digital gift card instantly.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.Gray,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 24.dp)
            )

            // Carousel
            Text(
                text = "Select Design",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 16.dp)
            )
            
            LazyRow(
                contentPadding = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                items(templates, key = { it.templateId }) { template ->
                    GiftDesignCard(
                        template = template,
                        isSelected = template.templateId == selectedTemplate?.templateId,
                        onClick = { selectedTemplate = template }
                    )
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Amount Selectors
            Text(
                text = "Select Amount",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                val amounts = listOf(500, 1000, 2000)
                amounts.forEach { amt ->
                    AmountPill(
                        text = "Rs. $amt",
                        isSelected = !isCustomAmount && selectedAmount == amt,
                        onClick = {
                            isCustomAmount = false
                            selectedAmount = amt
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
                AmountPill(
                    text = "Custom",
                    isSelected = isCustomAmount,
                    onClick = {
                        isCustomAmount = true
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            if (isCustomAmount) {
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = customAmountText,
                    onValueChange = { customAmountText = it },
                    label = { Text("Custom Amount (Rs.)") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VAL_BRAND_PRIMARY,
                        focusedLabelColor = VAL_BRAND_PRIMARY
                    )
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Form
            Text(
                text = "Recipient Details",
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.SemiBold),
                color = TextPrimary,
                modifier = Modifier.padding(horizontal = 16.dp).padding(bottom = 12.dp)
            )
            
            OutlinedTextField(
                value = recipientEmail,
                onValueChange = { recipientEmail = it },
                label = { Text("Recipient Email") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VAL_BRAND_PRIMARY,
                    focusedLabelColor = VAL_BRAND_PRIMARY
                )
            )
            
            Spacer(modifier = Modifier.height(16.dp))
            
            OutlinedTextField(
                value = message,
                onValueChange = { message = it },
                label = { Text("Add a personal message") },
                modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(120.dp),
                maxLines = 4,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VAL_BRAND_PRIMARY,
                    focusedLabelColor = VAL_BRAND_PRIMARY
                )
            )

            Spacer(modifier = Modifier.height(80.dp)) // Padding for bottom bar
        }
    }
}

@Composable
fun GiftDesignCard(
    template: GiftTemplate,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .width(260.dp)
            .aspectRatio(1.5f)
            .clip(RoundedCornerShape(12.dp))
            .bounceClick { onClick() }
            .border(
                width = if (isSelected) 3.dp else 0.dp,
                color = if (isSelected) VAL_BRAND_PRIMARY else Color.Transparent,
                shape = RoundedCornerShape(12.dp)
            )
    ) {
        AsyncImage(
            model = coil.request.ImageRequest.Builder(androidx.compose.ui.platform.LocalContext.current)
                .data(template.imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = template.category,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Mock vibrant background if image fails
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color(0xFFE8F5E9).copy(alpha = 0.5f))
        )
        
        Box(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .background(Color.Black.copy(alpha = 0.4f))
                .padding(12.dp)
        ) {
            Text(
                text = template.category,
                color = Color.White,
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
            )
        }
    }
}

@Composable
fun AmountPill(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier.bounceClick { onClick() },
        color = if (isSelected) VAL_BRAND_PRIMARY.copy(alpha = 0.1f) else Color.White,
        shape = RoundedCornerShape(50),
        border = androidx.compose.foundation.BorderStroke(
            width = 1.dp,
            color = if (isSelected) VAL_BRAND_PRIMARY else Color.LightGray
        )
    ) {
        Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(vertical = 12.dp)) {
            Text(
                text = text,
                color = if (isSelected) VAL_BRAND_PRIMARY else TextPrimary,
                style = MaterialTheme.typography.labelLarge.copy(fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium)
            )
        }
    }
}
