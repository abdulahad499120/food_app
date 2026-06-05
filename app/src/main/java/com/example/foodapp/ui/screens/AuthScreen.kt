package com.example.foodapp.ui.screens

import android.app.Activity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.with
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.ahad.foodapp.R
import com.example.foodapp.domain.validation.AuthValidator
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.ui.components.GhostButton
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.components.TextInput
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider

// Premium Accent Colors
val BrandTeal = Color(0xFF00B4D8)
val BrandPurple = Color(0xFF7209B7)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsState()
    val context = LocalContext.current
    val activity = context as? Activity
    
    // Tab State: 0 = Email, 1 = Phone
    var selectedTab by remember { mutableStateOf(0) }
    
    // Email State
    var isSignUp by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    
    // Phone State
    var phoneNumber by remember { mutableStateOf("") }
    var otpCode by remember { mutableStateOf("") }
    
    // Validation Errors
    var validationError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
            try {
                val account = task.getResult(ApiException::class.java)
                account?.idToken?.let { idToken ->
                    val credential = GoogleAuthProvider.getCredential(idToken, null)
                    authViewModel.handleGoogleSignInResult(credential)
                }
            } catch (e: ApiException) {
                // handle error
            }
        }
    }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onAuthSuccess()
        }
    }

    // Reset validation error when switching tabs or typing
    LaunchedEffect(selectedTab, email, password, phoneNumber, isSignUp) {
        validationError = null
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
            .padding(horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(64.dp))
        
        Text(
            text = "Welcome",
            style = MaterialTheme.typography.headlineLarge,
            fontWeight = FontWeight.Bold,
            color = TextPrimary
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Sign in or create an account to continue",
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = TextAlign.Center
        )
        
        Spacer(modifier = Modifier.height(32.dp))
        
        // Custom Dual Tab Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .clip(RoundedCornerShape(24.dp))
                .background(Color(0xFFF3F4F6)), // Light gray background
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 0) SurfaceWhite else Color.Transparent)
                    .clickable { selectedTab = 0 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Email",
                    color = if (selectedTab == 0) BrandPurple else TextSecondary,
                    fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Normal
                )
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight()
                    .padding(4.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(if (selectedTab == 1) SurfaceWhite else Color.Transparent)
                    .clickable { selectedTab = 1 },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Phone",
                    color = if (selectedTab == 1) BrandPurple else TextSecondary,
                    fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Normal
                )
            }
        }
        
        Spacer(modifier = Modifier.height(32.dp))

        // State & Error Handling
        if (authState is AuthState.Loading) {
            CircularProgressIndicator(color = BrandPurple, modifier = Modifier.padding(16.dp))
        } else if (authState is AuthState.Error) {
            Text(
                text = (authState as AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }
        
        if (validationError != null) {
            Text(
                text = validationError!!,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        // Tab Content
        AnimatedContent(
            targetState = selectedTab,
            transitionSpec = { fadeIn(tween(300)) with fadeOut(tween(300)) }
        ) { tab ->
            if (tab == 0) {
                // Email Flow
                Column {
                    if (isSignUp) {
                        TextInput(
                            label = "Full Name",
                            value = name,
                            onValueChange = { name = it },
                            placeholder = "John Doe"
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                    }
                    TextInput(
                        label = "Email Address",
                        value = email,
                        onValueChange = { email = it },
                        placeholder = "john@example.com"
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextInput(
                        label = "Password",
                        value = password,
                        onValueChange = { password = it },
                        placeholder = "••••••••"
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    
                    PrimaryButton(
                        onClick = {
                            if (!AuthValidator.isValidEmail(email)) {
                                validationError = "Please enter a valid email address."
                                return@PrimaryButton
                            }
                            if (isSignUp && !AuthValidator.isStrongPassword(password)) {
                                validationError = "Password must be at least 8 characters and contain uppercase, lowercase, and a number."
                                return@PrimaryButton
                            }
                            if (isSignUp && name.isBlank()) {
                                validationError = "Please enter your name."
                                return@PrimaryButton
                            }
                            
                            if (isSignUp) {
                                authViewModel.signUpWithEmail(email, password, name)
                            } else {
                                authViewModel.signInWithEmail(email, password)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = authState !is AuthState.Loading
                    ) {
                        Text(if (isSignUp) "Create Account" else "Sign In")
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    TextButton(
                        onClick = { isSignUp = !isSignUp },
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    ) {
                        Text(
                            text = if (isSignUp) "Already have an account? Sign In" else "Don't have an account? Sign Up",
                            color = BrandTeal
                        )
                    }
                }
            } else {
                // Phone Flow
                Column {
                    if (authState is AuthState.OTPVerification) {
                        TextInput(
                            label = "Enter 6-digit OTP",
                            value = otpCode,
                            onValueChange = { otpCode = it },
                            placeholder = "123456"
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        PrimaryButton(
                            onClick = {
                                if (otpCode.length < 6) {
                                    validationError = "Please enter a valid 6-digit OTP."
                                    return@PrimaryButton
                                }
                                val verificationId = (authState as AuthState.OTPVerification).verificationId
                                authViewModel.verifyOTP(verificationId, otpCode)
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = authState !is AuthState.Loading
                        ) {
                            Text("Verify & Sign In")
                        }
                    } else {
                        TextInput(
                            label = "Phone Number",
                            value = phoneNumber,
                            onValueChange = { phoneNumber = it },
                            placeholder = "+1234567890"
                        )
                        Spacer(modifier = Modifier.height(32.dp))
                        PrimaryButton(
                            onClick = {
                                if (!AuthValidator.isValidPhoneNumber(phoneNumber)) {
                                    validationError = "Please enter a valid phone number starting with '+'."
                                    return@PrimaryButton
                                }
                                activity?.let {
                                    authViewModel.sendOTP(phoneNumber, it)
                                }
                            },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = authState !is AuthState.Loading
                        ) {
                            Text("Send OTP Code")
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.weight(1f))
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
            Text(" OR ", color = TextSecondary, style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(horizontal = 8.dp))
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color(0xFFE5E7EB))
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        GhostButton(
            onClick = {
                val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                    .requestIdToken(context.getString(R.string.default_web_client_id))
                    .requestEmail()
                    .build()
                val googleSignInClient = GoogleSignIn.getClient(context, gso)
                googleSignInLauncher.launch(googleSignInClient.signInIntent)
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Sign in with Google", color = TextPrimary)
        }
        
        Spacer(modifier = Modifier.height(32.dp))
    }
}
