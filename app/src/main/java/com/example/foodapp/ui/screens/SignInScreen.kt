package com.example.foodapp.ui.screens

import android.app.Activity
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodapp.domain.validation.AuthValidator
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToOtp: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    
    var usePhoneAuth by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        } else if (authState is AuthState.OTPVerification) {
            onNavigateToOtp()
        }
    }
    
    LaunchedEffect(usePhoneAuth, email, password, phoneNumber) {
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
            text = "Sign In",
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

        AnimatedContent(
            targetState = usePhoneAuth,
            transitionSpec = { fadeIn(tween(300)) with fadeOut(tween(300)) }
        ) { isPhone ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isPhone) {
                    OutlinedTextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = VAL_BRAND_PRIMARY,
                            focusedLabelColor = VAL_BRAND_PRIMARY
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (!AuthValidator.isValidPhoneNumber(phoneNumber)) {
                                validationError = "Enter a valid phone number with country code (+)."
                                return@Button
                            }
                            activity?.let { authViewModel.sendOTP(phoneNumber, it) }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text("Send Verification Code", color = VAL_BRAND_ON_PRIMARY)
                    }
                } else {
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
                        text = "Forgot Password?",
                        color = VAL_BRAND_PRIMARY,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.fillMaxWidth().clickable { /* TODO */ },
                        textAlign = TextAlign.End
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Button(
                        onClick = {
                            if (!AuthValidator.isValidEmail(email)) {
                                validationError = "Please enter a valid email."
                                return@Button
                            }
                            authViewModel.signInWithEmail(email, password)
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = VAL_BRAND_PRIMARY),
                        modifier = Modifier.fillMaxWidth().height(56.dp),
                        shape = MaterialTheme.shapes.extraLarge,
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text("Sign In", color = VAL_BRAND_ON_PRIMARY)
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { usePhoneAuth = !usePhoneAuth }) {
            Text(if (usePhoneAuth) "Use Email Instead" else "Use Phone Number Instead", color = VAL_SURFACE_DARK)
        }
    }
}
