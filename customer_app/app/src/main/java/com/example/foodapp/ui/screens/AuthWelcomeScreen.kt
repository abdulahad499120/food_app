package com.example.foodapp.ui.screens

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahad.foodapp.R
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.utils.bounceClick
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import com.google.android.libraries.identity.googleid.GetGoogleIdOption
import kotlinx.coroutines.launch

@Composable
fun AuthWelcomeScreen(
    viewModel: AuthViewModel,
    onNavigateToHome: () -> Unit,
    onNavigateToEmailSignIn: () -> Unit
) {
    val authState by viewModel.authState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val credentialManager = remember { CredentialManager.create(context) }

    LaunchedEffect(authState) {
        if (authState is AuthState.Authenticated) {
            onNavigateToHome()
        }
    }

    val signInWithGoogle: (Boolean) -> Unit = { autoSelect ->
        coroutineScope.launch {
            try {
                // Ensure default_web_client_id exists in strings.xml for Firebase
                val clientId = context.getString(R.string.default_web_client_id)
                val googleIdOption = GetGoogleIdOption.Builder()
                    .setFilterByAuthorizedAccounts(false)
                    .setServerClientId(clientId)
                    .setAutoSelectEnabled(autoSelect)
                    .build()

                val request = GetCredentialRequest.Builder()
                    .addCredentialOption(googleIdOption)
                    .build()

                val result = credentialManager.getCredential(
                    request = request,
                    context = context
                )
                viewModel.handleCredentialManagerResult(result.credential)
            } catch (e: GetCredentialException) {
                Log.e("AuthWelcomeScreen", "CredentialManager Error: ${e.message}")
            } catch (e: Exception) {
                Log.e("AuthWelcomeScreen", "Error: ${e.message}")
            }
        }
    }

    // Try 1-Tap (Auto-Select) automatically on load
    LaunchedEffect(Unit) {
        if (authState !is AuthState.Authenticated && authState !is AuthState.Loading) {
            signInWithGoogle(true)
        }
    }

    Box(
        modifier = Modifier.fillMaxSize()
    ) {
        // Aesthetic Background or Branding
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.verticalGradient(
                        colors = listOf(
                            VAL_BRAND_PRIMARY.copy(alpha = 0.8f),
                            VAL_BRAND_PRIMARY
                        )
                    )
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Ice Land",
                style = MaterialTheme.typography.titleLarge.copy(fontSize = 32.sp),
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Your favorite shakes & ice creams, delivered fast.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = SurfaceWhite,
                shadowElevation = 12.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = VAL_BRAND_PRIMARY)
                    } else {
                        // 1-Tap/Explicit Google Sign-In Button
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color(0xFFF3F4F6),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick { signInWithGoogle(false) }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Continue with Google",
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legacy Email Sign-In
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp)
                                .bounceClick { onNavigateToEmailSignIn() }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Text(
                                    "Continue with Email", 
                                    style = MaterialTheme.typography.labelLarge,
                                    color = TextPrimary
                                )
                            }
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Guest mode entry
                        TextButton(
                            onClick = onNavigateToHome,
                            modifier = Modifier.bounceClick { onNavigateToHome() }
                        ) {
                            Text(
                                "Browse as Guest", 
                                style = MaterialTheme.typography.bodyMedium,
                                color = TextSecondary
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}
