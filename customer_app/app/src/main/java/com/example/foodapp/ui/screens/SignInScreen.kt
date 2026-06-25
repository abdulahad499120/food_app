package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

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
import com.example.foodapp.utils.bounceClick
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.foodapp.domain.validation.AuthValidator
import com.example.foodapp.theme.VAL_BACKGROUND
import com.example.foodapp.theme.VAL_BRAND_ON_PRIMARY
import androidx.compose.ui.graphics.Color
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.theme.VAL_SURFACE_DARK
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.ApiException
import com.google.firebase.auth.GoogleAuthProvider
import com.ahad.foodapp.R

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.foodapp.ui.components.ExpressiveFullScreenLoader

@OptIn(ExperimentalMaterial3Api::class, ExperimentalAnimationApi::class)
@Composable
fun SignInScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateToOtp: () -> Unit,
    onNavigateToForgotPassword: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val activity = context as? Activity
    val webClientId = androidx.compose.ui.res.stringResource(id = R.string.default_web_client_id)
    
    var usePhoneAuth by remember { mutableStateOf(false) }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var phoneNumber by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }

    val googleSignInLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val task = GoogleSignIn.getSignedInAccountFromIntent(result.data)
        try {
            val account = task.getResult(ApiException::class.java)!!
            val credential = GoogleAuthProvider.getCredential(account.idToken, null)
            authViewModel.handleGoogleSignInResult(credential)
        } catch (e: Exception) {
            validationError = "Google Sign-In failed"
        }
    }

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

        AnimatedVisibility(
            visible = validationError != null || authState is AuthState.Error,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            val errorMsg = validationError ?: (authState as? AuthState.Error)?.message ?: ""
            Text(
                text = errorMsg,
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp)
            )
        }

        AnimatedContent(
            targetState = usePhoneAuth,
            transitionSpec = { fadeIn(tween(300)) with fadeOut(tween(300)) }
        ) { isPhone ->
            Column(modifier = Modifier.fillMaxWidth()) {
                if (isPhone) {
                    TextField(
                        value = phoneNumber,
                        onValueChange = { phoneNumber = it },
                        label = { Text("Phone Number") },
                        placeholder = { Text("+1234567890") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = VAL_BRAND_PRIMARY,
                            focusedLabelColor = VAL_BRAND_PRIMARY,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(32.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (authState is AuthState.Loading) Color.Gray else VAL_BRAND_PRIMARY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .bounceClick {
                                if (authState !is AuthState.Loading) {
                                    if (!AuthValidator.isValidPhoneNumber(phoneNumber)) {
                                        validationError = "Enter a valid phone number with country code (+)."
                                        return@bounceClick
                                    }
                                    activity?.let { authViewModel.sendOTP(phoneNumber, it) }
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Send Verification Code", color = VAL_BRAND_ON_PRIMARY, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                } else {
                    TextField(
                        value = email,
                        onValueChange = { email = it },
                        label = { Text("Email Address") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = VAL_BRAND_PRIMARY,
                            focusedLabelColor = VAL_BRAND_PRIMARY,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    TextField(
                        value = password,
                        onValueChange = { password = it },
                        label = { Text("Password") },
                        modifier = Modifier.fillMaxWidth(),
                        colors = TextFieldDefaults.colors(
                            focusedIndicatorColor = VAL_BRAND_PRIMARY,
                            focusedLabelColor = VAL_BRAND_PRIMARY,
                            unfocusedContainerColor = Color.Transparent,
                            focusedContainerColor = Color.Transparent
                        ),
                        singleLine = true
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = onNavigateToForgotPassword) {
                            Text(
                                text = "Forgot Password?",
                                color = VAL_BRAND_PRIMARY,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Surface(
                        shape = MaterialTheme.shapes.extraLarge,
                        color = if (authState is AuthState.Loading) Color.Gray else VAL_BRAND_PRIMARY,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .bounceClick {
                                if (authState !is AuthState.Loading) {
                                    val error = authViewModel.validateSignIn(email, password)
                                    if (error != null) {
                                        validationError = error
                                        return@bounceClick
                                    }
                                    authViewModel.signInWithEmail(email, password)
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text("Sign In", color = VAL_BRAND_ON_PRIMARY, style = MaterialTheme.typography.labelLarge)
                        }
                    }
                }
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        TextButton(onClick = { usePhoneAuth = !usePhoneAuth }) {
            Text(if (usePhoneAuth) "Use Email Instead" else "Use Phone Number Instead", color = VAL_SURFACE_DARK)
        }

        Spacer(modifier = Modifier.height(16.dp))
        Surface(
            shape = MaterialTheme.shapes.extraLarge,
            color = Color.Transparent,
            border = androidx.compose.foundation.BorderStroke(1.dp, VAL_SURFACE_DARK.copy(alpha = 0.2f)),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .bounceClick {
                    val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                        .requestIdToken(webClientId)
                        .requestEmail()
                        .build()
                    val client = GoogleSignIn.getClient(context, gso)
                    googleSignInLauncher.launch(client.signInIntent)
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Continue with Google", color = VAL_SURFACE_DARK, style = MaterialTheme.typography.labelLarge)
            }
        }
    }

    if (authState is AuthState.Loading) {
        ExpressiveFullScreenLoader(message = "Verifying Credentials...")
    }
}
