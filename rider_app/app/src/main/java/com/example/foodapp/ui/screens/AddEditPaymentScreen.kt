package com.example.foodapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.PaymentViewModel
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.foodapp.ui.components.ExpressiveFullScreenLoader

/**
 * # H1 Add Edit Payment Screen
 * 
 * Allows users to add a new payment method. Card details are tokenized via the ViewModel.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val user = (authState as? AuthState.Authenticated)?.user

    var cardNumber by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    var cvv by remember { mutableStateOf("") }
    var nameOnCard by remember { mutableStateOf("") }
    var isDefault by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    // H4 Card Formatting Transformation
    val cardNumberVisualTransformation = remember {
        object : VisualTransformation {
            override fun filter(text: AnnotatedString): TransformedText {
                val originalText = text.text.replace(" ", "")
                val formatted = buildString {
                    for (i in originalText.indices) {
                        append(originalText[i])
                        if ((i + 1) % 4 == 0 && i != originalText.lastIndex) {
                            append(" ")
                        }
                    }
                }
                val offsetMapping = object : OffsetMapping {
                    override fun originalToTransformed(offset: Int): Int {
                        var spaces = 0
                        for (i in 0 until offset) {
                            if (i > 0 && i % 4 == 0) spaces++
                        }
                        return offset + spaces
                    }
                    override fun transformedToOriginal(offset: Int): Int {
                        var originalOffset = 0
                        for (i in 0 until offset) {
                            if (formatted.getOrNull(i) != ' ') originalOffset++
                        }
                        return originalOffset
                    }
                }
                return TransformedText(AnnotatedString(formatted), offsetMapping)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    // H2 Screen Title
                    Text("Add Payment Method", style = MaterialTheme.typography.titleLarge) 
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back", tint = TextPrimary)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = VAL_BACKGROUND)
            )
        },
        containerColor = VAL_BACKGROUND
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            // H3 Card Inputs
            OutlinedTextField(
                value = cardNumber,
                onValueChange = { if (it.length <= 16) cardNumber = it },
                label = { Text("Card Number") },
                modifier = Modifier.fillMaxWidth(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                visualTransformation = cardNumberVisualTransformation,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VAL_BRAND_PRIMARY,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = expiry,
                    onValueChange = { if (it.length <= 5) expiry = it },
                    label = { Text("MM/YY") },
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VAL_BRAND_PRIMARY,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )

                OutlinedTextField(
                    value = cvv,
                    onValueChange = { if (it.length <= 4) cvv = it },
                    label = { Text("CVV") },
                    modifier = Modifier.weight(1f),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    shape = RoundedCornerShape(12.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = VAL_BRAND_PRIMARY,
                        unfocusedBorderColor = DividerColor,
                        focusedContainerColor = SurfaceWhite,
                        unfocusedContainerColor = SurfaceWhite
                    )
                )
            }

            OutlinedTextField(
                value = nameOnCard,
                onValueChange = { nameOnCard = it },
                label = { Text("Name on Card") },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = VAL_BRAND_PRIMARY,
                    unfocusedBorderColor = DividerColor,
                    focusedContainerColor = SurfaceWhite,
                    unfocusedContainerColor = SurfaceWhite
                )
            )

            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Checkbox(
                    checked = isDefault,
                    onCheckedChange = { isDefault = it },
                    colors = CheckboxDefaults.colors(checkedColor = VAL_BRAND_PRIMARY)
                )
                Text("Set as default payment method", style = MaterialTheme.typography.bodyMedium)
            }

            AnimatedVisibility(
                visible = errorMessage != null,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Text(
                    text = errorMessage ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.weight(1f))

            PrimaryButton(
                onClick = {
                    if (user != null) {
                        val rawCardNumber = cardNumber.replace("\\s".toRegex(), "")
                        val error = viewModel.validatePayment(rawCardNumber, expiry, cvv)
                        if (error != null) {
                            errorMessage = error
                            return@PrimaryButton
                        }
                        errorMessage = null

                        isLoading = true
                        viewModel.simulateGatewayTokenization(
                            userId = user.uid,
                            cardNumber = rawCardNumber,
                            expiry = expiry,
                            cvv = cvv,
                            isDefault = isDefault,
                            onSuccess = {
                                isLoading = false
                                onNavigateBack()
                            },
                            onError = { err ->
                                isLoading = false
                                errorMessage = err
                            }
                        )
                    }
                },
                modifier = Modifier.fillMaxWidth().height(56.dp),
                enabled = !isLoading
            ) {
                Text("Save Card", style = MaterialTheme.typography.labelLarge, color = Color.White)
            }
        }
    }

    if (isLoading) {
        ExpressiveFullScreenLoader(message = "Securing Payment...")
    }
}
