package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.domain.validation.AuthValidator
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }
    
    LaunchedEffect(name, email, password) {
        validationError = null
        authViewModel.resetError()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(VAL_BACKGROUND)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Start) {
            TextButton(onClick = onNavigateBack) {
                Text("Back", color = VAL_SURFACE_DARK)
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))
        Text(
            text = "Create an account",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = VAL_SURFACE_DARK,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(32.dp))

        if (authState is AuthState.Loading) {
            CircularProgressIndicator(color = VAL_BRAND_PRIMARY, modifier = Modifier.padding(16.dp))
        } else {
            if (validationError != null || authState is AuthState.Error) {
                val errorMsg = validationError ?: (authState as AuthState.Error).message
                Text(
                    text = errorMsg,
                    color = MaterialTheme.colorScheme.error,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
                )
            }
        }

        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VAL_BRAND_PRIMARY,
                focusedLabelColor = VAL_BRAND_PRIMARY
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email Address") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VAL_BRAND_PRIMARY,
                focusedLabelColor = VAL_BRAND_PRIMARY
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VAL_BRAND_PRIMARY,
                focusedLabelColor = VAL_BRAND_PRIMARY
            ),
            singleLine = true
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Must be at least 8 characters with uppercase, lowercase, and a number.",
            style = MaterialTheme.typography.bodySmall,
            color = VAL_SURFACE_DARK.copy(alpha = 0.6f),
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (name.isBlank()) {
                    validationError = "Name cannot be empty."
                    return@Button
                }
                if (!AuthValidator.isValidEmail(email)) {
                    validationError = "Please enter a valid email."
                    return@Button
                }
                if (!AuthValidator.isStrongPassword(password)) {
                    validationError = "Password does not meet strength requirements."
                    return@Button
                }
                authViewModel.signUpWithEmail(email, password, name)
            },
            colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = authState !is AuthState.Loading
        ) {
            Text("Join Now", color = VAL_BRAND_ON_PRIMARY)
        }
    }
}
