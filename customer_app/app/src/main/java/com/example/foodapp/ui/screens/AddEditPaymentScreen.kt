package com.example.foodapp.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import com.example.foodapp.utils.bounceClick
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.ExpressiveFullScreenLoader
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.PaymentViewModel

enum class PaymentTab { RAAST, BANK_ACCOUNT }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentScreen(
    authState: AuthState,
    onNavigateBack: () -> Unit,
    viewModel: PaymentViewModel = viewModel()
) {
    val user = (authState as? AuthState.Authenticated)?.user

    var selectedTab by remember { mutableStateOf(PaymentTab.RAAST) }
    
    // Raast State
    var raastId by remember { mutableStateOf("") }
    
    // Bank Account State
    var bankName by remember { mutableStateOf("") }
    var accountNumber by remember { mutableStateOf("") }

    // Shared State
    var isDefault by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Add Payment Method", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold) },
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
                .padding(paddingValues),
            verticalArrangement = Arrangement.Top
        ) {
            // ── Premium Segmented Pill Tab Selector ──────────────────────
            val tabs = listOf("📱  Raast", "🏦  Bank")
            val selectedIdx = selectedTab.ordinal
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
                    .clip(RoundedCornerShape(50))
                    .background(Color(0xFFEEEEEE))
                    .height(44.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(modifier = Modifier.fillMaxSize()) {
                    tabs.forEachIndexed { idx, label ->
                        val isSelected = idx == selectedIdx
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .padding(4.dp)
                                .clip(RoundedCornerShape(50))
                                .background(
                                    if (isSelected) VAL_BRAND_PRIMARY else Color.Transparent,
                                    shape = RoundedCornerShape(50)
                                )
                                .bounceClick {
                                    selectedTab = PaymentTab.entries[idx]
                                    errorMessage = null
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) Color.White else TextSecondary
                            )
                        }
                    }
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    color = BrandPrimary.copy(alpha = 0.05f),
                    shape = RoundedCornerShape(12.dp),
                    border = androidx.compose.foundation.BorderStroke(1.dp, BrandPrimary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Card Payments", style = MaterialTheme.typography.labelLarge, color = BrandPrimary)
                            Text("Credit & Debit cards are added securely during checkout.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                        }
                    }
                }

                when (selectedTab) {
                    PaymentTab.RAAST -> {
                        OutlinedTextField(
                            value = raastId,
                            onValueChange = { raastId = it },
                            label = { Text("Raast ID (Mobile Number)") },
                            modifier = Modifier.fillMaxWidth(),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VAL_BRAND_PRIMARY, unfocusedBorderColor = DividerColor,
                                focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite
                            )
                        )
                        Text("Your Raast ID is usually your registered mobile number.", style = MaterialTheme.typography.bodySmall, color = TextSecondary)
                    }
                    PaymentTab.BANK_ACCOUNT -> {
                        OutlinedTextField(
                            value = bankName,
                            onValueChange = { bankName = it },
                            label = { Text("Bank Name") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VAL_BRAND_PRIMARY, unfocusedBorderColor = DividerColor,
                                focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite
                            )
                        )
                        OutlinedTextField(
                            value = accountNumber,
                            onValueChange = { accountNumber = it },
                            label = { Text("IBAN / Account Number") },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedBorderColor = VAL_BRAND_PRIMARY, unfocusedBorderColor = DividerColor,
                                focusedContainerColor = SurfaceWhite, unfocusedContainerColor = SurfaceWhite
                            )
                        )
                    }
                }

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

                val isSaveEnabled = when (selectedTab) {
                    PaymentTab.RAAST -> raastId.isNotBlank() && !isLoading
                    PaymentTab.BANK_ACCOUNT -> bankName.isNotBlank() && accountNumber.isNotBlank() && !isLoading
                }

                PrimaryButton(
                    onClick = {
                        if (user != null) {
                            errorMessage = null
                            isLoading = true
                            
                            val successCallback = {
                                isLoading = false
                                onNavigateBack()
                            }
                            val errorCallback = { err: String ->
                                isLoading = false
                                errorMessage = err
                            }

                            when (selectedTab) {
                                PaymentTab.RAAST -> {
                                    viewModel.saveRaastPayment(user.uid, raastId, isDefault, successCallback, errorCallback)
                                }
                                PaymentTab.BANK_ACCOUNT -> {
                                    viewModel.saveBankAccount(user.uid, bankName, accountNumber, isDefault, successCallback, errorCallback)
                                }
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth().height(56.dp),
                    enabled = isSaveEnabled
                ) {
                    Text("Save", style = MaterialTheme.typography.labelLarge, color = Color.White)
                }
            }
        }
    }

    if (isLoading) {
        ExpressiveFullScreenLoader(message = "Saving Payment...")
    }
}
