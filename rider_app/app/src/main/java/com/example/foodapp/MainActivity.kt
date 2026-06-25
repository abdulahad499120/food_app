package com.example.foodapp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.foodapp.theme.FoodAppTheme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue

import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.lifecycle.compose.collectAsStateWithLifecycle

class MainActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)

    enableEdgeToEdge()
    setContent {
      FoodAppTheme { 
        Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) { 
            val navController = androidx.navigation.compose.rememberNavController()
            val authViewModel: com.example.foodapp.ui.state.AuthViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
            val authState by authViewModel.authState.collectAsStateWithLifecycle()

            androidx.compose.runtime.LaunchedEffect(authState) {
                if (authState is com.example.foodapp.ui.state.AuthState.Authenticated) {
                    navController.navigate("home") {
                        popUpTo("auth") { inclusive = true }
                    }
                }
            }

            androidx.navigation.compose.NavHost(
                navController = navController, 
                startDestination = if (com.google.firebase.auth.FirebaseAuth.getInstance().currentUser != null) "home" else "auth"
            ) {
                navigation(startDestination = "welcome", route = "auth") {
                    composable("welcome") {
                        com.example.foodapp.ui.screens.WelcomeScreen(
                            onNavigateToSignIn = { navController.navigate("signin") },
                            onNavigateToSignUp = { /* Disabled for riders */ }
                        )
                    }
                    composable("signin") {
                        com.example.foodapp.ui.screens.SignInScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = {
                                navController.navigate("home") {
                                    popUpTo("auth") { inclusive = true }
                                }
                            },
                            onNavigateToOtp = { },
                            onNavigateToForgotPassword = { },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                composable("home") {
                    val sessionViewModel: com.example.foodapp.ui.state.RiderSessionViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val sessionState by sessionViewModel.uiState.collectAsStateWithLifecycle()
                    
                    var manualActiveOrderId by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf<String?>(null) }
                    
                    val activeOrderId = sessionState.activeOrderId ?: manualActiveOrderId
                    
                    if (activeOrderId == null) {
                        com.example.foodapp.ui.screens.JobPoolScreen(
                            onJobClaimed = { claimedId -> manualActiveOrderId = claimedId },
                            onNavigateToHistory = { navController.navigate("order_history") },
                            viewModel = sessionViewModel
                        )
                    } else {
                        com.example.foodapp.ui.screens.ActiveDeliveryScreen(
                            orderId = activeOrderId,
                            onDeliveryComplete = { manualActiveOrderId = null }
                        )
                    }
                }
                composable("order_history") {
                    com.example.foodapp.ui.screens.OrderHistoryScreen(
                        onNavigateBack = { navController.popBackStack() }
                    )
                }
            }
        }  
      }
    }
  }
}
