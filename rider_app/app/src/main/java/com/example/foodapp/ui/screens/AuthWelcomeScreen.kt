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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.exceptions.GetCredentialException
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ahad.riderapp.R
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
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
        modifier = Modifier
            .fillMaxSize()
            .background(VAL_BRAND_PRIMARY)
    ) {
        // Aesthetic Background or Branding
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            verticalArrangement = Arrangement.Bottom,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Welcome to Ice Land",
                style = MaterialTheme.typography.headlineLarge,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Your favorite shakes & ice creams, delivered fast.",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.9f),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            
            Spacer(modifier = Modifier.height(48.dp))

            Surface(
                shape = RoundedCornerShape(24.dp),
                color = SurfaceWhite,
                shadowElevation = 8.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    if (authState is AuthState.Loading) {
                        CircularProgressIndicator(color = VAL_BRAND_PRIMARY)
                    } else {
                        // 1-Tap/Explicit Google Sign-In Button
                        Button(
                            onClick = { signInWithGoogle(false) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(56.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF1F3F5),
                                contentColor = TextPrimary
                            ),
                            shape = RoundedCornerShape(16.dp)
                        ) {
                            Text("Continue with Google", fontWeight = FontWeight.SemiBold)
                        }

                        Spacer(modifier = Modifier.height(16.dp))

                        // Legacy Email Sign-In
                        TextButton(
                            onClick = onNavigateToEmailSignIn,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text("Continue with Email", color = TextPrimary)
                        }
                        
                        Spacer(modifier = Modifier.height(16.dp))
                        
                        // Guest mode entry (if needed here, though Map Picker is usually the entry)
                        TextButton(onClick = onNavigateToHome) {
                            Text("Browse as Guest", color = TextSecondary)
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}
