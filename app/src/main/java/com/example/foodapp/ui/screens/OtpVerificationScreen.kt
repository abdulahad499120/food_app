package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtpVerificationScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    var otpCode by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    
    // Simple countdown timer for UX
    var countdown by remember { mutableStateOf(60) }
    LaunchedEffect(countdown) {
        if (countdown > 0) {
            delay(1000)
            countdown--
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    LaunchedEffect(otpCode) {
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
            text = "Verify Phone",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = VAL_SURFACE_DARK,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter the 6-digit code we just sent you.",
            style = MaterialTheme.typography.bodyLarge,
            color = VAL_SURFACE_DARK.copy(alpha = 0.7f),
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
            value = otpCode,
            onValueChange = { if (it.length <= 6) otpCode = it },
            label = { Text("6-Digit Code") },
            placeholder = { Text("XXXXXX") },
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = VAL_BRAND_PRIMARY,
                focusedLabelColor = VAL_BRAND_PRIMARY
            ),
            singleLine = true,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center, letterSpacing = 8.sp)
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = {
                if (otpCode.length < 6) {
                    validationError = "Please enter all 6 digits."
                    return@Button
                }
                if (authState is AuthState.OTPVerification) {
                    val verificationId = (authState as AuthState.OTPVerification).verificationId
                    authViewModel.verifyOTP(verificationId, otpCode)
                }
            },
            colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
            modifier = Modifier.fillMaxWidth().height(56.dp),
            shape = MaterialTheme.shapes.extraLarge,
            enabled = authState !is AuthState.Loading && otpCode.length == 6
        ) {
            Text("Verify", color = VAL_BRAND_ON_PRIMARY)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(
            onClick = { /* TODO: Resend logic */ },
            enabled = countdown == 0
        ) {
            Text(
                text = if (countdown > 0) "Resend code in $countdown s" else "Resend Code",
                color = if (countdown > 0) VAL_SURFACE_DARK.copy(alpha = 0.5f) else VAL_BRAND_PRIMARY
            )
        }
    }
}
