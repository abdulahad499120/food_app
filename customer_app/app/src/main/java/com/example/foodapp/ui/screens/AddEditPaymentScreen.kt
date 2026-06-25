package com.example.foodapp.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.ui.state.SafepayVaultState
import com.example.foodapp.ui.state.SafepayVaultViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddEditPaymentScreen(
    userId: String,
    onBack: () -> Unit,
    onSuccess: () -> Unit,
    viewModel: SafepayVaultViewModel = viewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    var cardNumber by remember { mutableStateOf("4111 1111 1111 1111") }
    var expMonth by remember { mutableStateOf("02") }
    var expYear by remember { mutableStateOf("2028") }
    var cvv by remember { mutableStateOf("123") }
    var firstName by remember { mutableStateOf("Test") }
    var lastName by remember { mutableStateOf("User") }
    var email by remember { mutableStateOf("test@example.com") }

    when (val state = uiState) {
        is SafepayVaultState.Requires3DS -> {
            SafepayWebViewScreen(
                ddcUrl = state.ddcUrl,
                accessToken = state.accessToken,
                onSuccess = { sessionId ->
                    viewModel.onDdcSuccess(sessionId)
                },
                onFailure = {
                    viewModel.onCancel()
                }
            )
        }
        is SafepayVaultState.Success -> {
            LaunchedEffect(Unit) {
                onSuccess()
            }
        }
        else -> {
            Scaffold(
                topBar = {
                    TopAppBar(title = { Text("Add New Card") })
                }
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .padding(16.dp)
                        .fillMaxSize()
                ) {
                    if (state is SafepayVaultState.Error) {
                        Text(text = state.message, color = MaterialTheme.colorScheme.error)
                        Spacer(modifier = Modifier.height(8.dp))
                    }

                    OutlinedTextField(
                        value = cardNumber,
                        onValueChange = { cardNumber = it },
                        label = { Text("Card Number") },
                        modifier = Modifier.fillMaxWidth(),
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    Row(modifier = Modifier.fillMaxWidth()) {
                        OutlinedTextField(
                            value = expMonth,
                            onValueChange = { expMonth = it },
                            label = { Text("MM") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = expYear,
                            onValueChange = { expYear = it },
                            label = { Text("YYYY") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        OutlinedTextField(
                            value = cvv,
                            onValueChange = { cvv = it },
                            label = { Text("CVV") },
                            modifier = Modifier.weight(1f),
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                        )
                    }

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = {
                            viewModel.startVaulting(
                                userId = userId,
                                email = email,
                                firstName = firstName,
                                lastName = lastName,
                                phone = "+923000000000",
                                cardNumber = cardNumber,
                                expMonth = expMonth,
                                expYear = expYear,
                                cvv = cvv
                            )
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = state !is SafepayVaultState.Loading
                    ) {
                        if (state is SafepayVaultState.Loading) {
                            CircularProgressIndicator(modifier = Modifier.size(24.dp))
                        } else {
                            Text("Save Card Securely")
                        }
                    }
                }
            }
        }
    }
}
