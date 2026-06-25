package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.foodapp.utils.bounceClick
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Cancel
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
import androidx.compose.ui.platform.LocalContext
import com.ahad.foodapp.R
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import com.example.foodapp.ui.components.ExpressiveFullScreenLoader

@Composable
fun SignUpScreen(
    authViewModel: AuthViewModel,
    onAuthSuccess: () -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var validationError by remember { mutableStateOf<String?>(null) }
    val context = LocalContext.current
    val webClientId = androidx.compose.ui.res.stringResource(id = R.string.default_web_client_id)

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

        TextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Full Name") },
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
        Spacer(modifier = Modifier.height(16.dp))
        
        // Live Password Integrity Validator
        Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
            val hasMinLength = password.length >= 8
            val hasUpperLower = password.any { it.isUpperCase() } && password.any { it.isLowerCase() }
            val hasNumber = password.any { it.isDigit() }
            
            @Composable
            fun RequirementRow(met: Boolean, text: String) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(vertical = 4.dp)) {
                    Icon(
                        imageVector = if (met) Icons.Default.CheckCircle else Icons.Default.Cancel,
                        contentDescription = null,
                        tint = if (met) VAL_BRAND_PRIMARY else Color.Red,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(text, style = MaterialTheme.typography.bodySmall, color = VAL_SURFACE_DARK.copy(alpha = 0.8f))
                }
            }
            
            RequirementRow(hasMinLength, "At least 8 characters")
            RequirementRow(hasUpperLower, "Uppercase & lowercase letter")
            RequirementRow(hasNumber, "At least one number")
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
                        val error = authViewModel.validateSignUp(name, email, password)
                        if (error != null) {
                            validationError = error
                            return@bounceClick
                        }
                        authViewModel.signUpWithEmail(email, password, name)
                    }
                }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("Join Now", color = VAL_BRAND_ON_PRIMARY, style = MaterialTheme.typography.labelLarge)
            }
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
        ExpressiveFullScreenLoader(message = "Creating Account...")
    }
}
