package com.example.foodapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodapp.data.models.Product
import com.example.foodapp.ui.components.BottomNavBar
import com.example.foodapp.ui.components.BottomNavItem
import com.example.foodapp.ui.screens.CartScreen
import com.example.foodapp.ui.screens.HomeScreen
import com.example.foodapp.ui.screens.ProductDetailSheet

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import com.example.foodapp.ui.screens.WelcomeScreen
import com.example.foodapp.ui.screens.SignInScreen
import com.example.foodapp.ui.screens.SignUpScreen
import com.example.foodapp.ui.screens.OtpVerificationScreen
import com.example.foodapp.ui.screens.CheckoutScreen
import com.example.foodapp.ui.screens.OrderSuccessScreen
import com.example.foodapp.ui.state.CheckoutViewModel
import androidx.compose.runtime.collectAsState

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Cart : Screen("cart")
    object Profile : Screen("profile")
    object Checkout : Screen("checkout")
    object OrderSuccess : Screen("order_success")
    
    // Auth Flow routes
    object AuthFlow : Screen("auth_flow")
    object AuthWelcome : Screen("auth_welcome")
    object AuthSignIn : Screen("auth_sign_in")
    object AuthSignUp : Screen("auth_sign_up")
    object AuthOtp : Screen("auth_otp")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodAppRoot(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Auth State
    val authState by authViewModel.authState.collectAsState()

    // Bottom Sheet State
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home),
        BottomNavItem("Cart", Icons.Default.ShoppingCart),
        BottomNavItem("Profile", Icons.Default.Person)
    )

    Scaffold(
        bottomBar = {
            // Hide bottom bar on auth flow, checkout, and success screens
            if (currentRoute?.startsWith("auth") != true && currentRoute != Screen.Checkout.route && currentRoute != Screen.OrderSuccess.route) {
                val currentIndex = when (currentRoute) {
                    Screen.Home.route -> 0
                    Screen.Cart.route -> 1
                    Screen.Profile.route -> 2
                    else -> 0
                }
                BottomNavBar(
                    items = navItems,
                    currentActiveIndex = currentIndex,
                    onTabSelected = { index ->
                        val targetRoute = when (index) {
                            0 -> Screen.Home.route
                            1 -> Screen.Cart.route
                            2 -> if (authState is AuthState.Authenticated) Screen.Profile.route else Screen.AuthFlow.route
                            else -> Screen.Home.route
                        }
                        navController.navigate(targetRoute) {
                            popUpTo(navController.graph.startDestinationId) {
                                saveState = true
                            }
                            launchSingleTop = true
                            restoreState = true
                        }
                    }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            NavHost(
                navController = navController,
                startDestination = Screen.Home.route
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        authState = authState,
                        onProductClick = { product ->
                            selectedProduct = product
                        }
                    )
                }
                composable(Screen.Cart.route) {
                    CartScreen(
                        onCheckoutRequest = {
                            if (authState is AuthState.Authenticated) {
                                navController.navigate(Screen.Checkout.route)
                            } else {
                                navController.navigate(Screen.AuthFlow.route)
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    )
                }
                composable(Screen.Profile.route) {
                    // ProfileScreen() -> placeholder
                    Box(modifier = Modifier.fillMaxSize())
                }
                
                // Nested Auth Graph
                navigation(route = Screen.AuthFlow.route, startDestination = Screen.AuthWelcome.route) {
                    composable(Screen.AuthWelcome.route) {
                        WelcomeScreen(
                            onNavigateToSignIn = { navController.navigate(Screen.AuthSignIn.route) },
                            onNavigateToSignUp = { navController.navigate(Screen.AuthSignUp.route) }
                        )
                    }
                    composable(Screen.AuthSignIn.route) {
                        SignInScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { 
                                // Pop the entire auth flow from the stack
                                navController.popBackStack(Screen.AuthFlow.route, inclusive = true) 
                            },
                            onNavigateToOtp = { navController.navigate(Screen.AuthOtp.route) },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.AuthSignUp.route) {
                        SignUpScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { 
                                navController.popBackStack(Screen.AuthFlow.route, inclusive = true) 
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                    composable(Screen.AuthOtp.route) {
                        OtpVerificationScreen(
                            authViewModel = authViewModel,
                            onAuthSuccess = { 
                                navController.popBackStack(Screen.AuthFlow.route, inclusive = true) 
                            },
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                
                composable(Screen.Checkout.route) {
                    val checkoutViewModel: CheckoutViewModel = viewModel()
                    CheckoutScreen(
                        authState = authState,
                        onNavigateBack = {
                            navController.popBackStack()
                        },
                        onOrderSuccess = {
                            navController.navigate(Screen.OrderSuccess.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    )
                }
                composable(Screen.OrderSuccess.route) {
                    OrderSuccessScreen(
                        onNavigateHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(navController.graph.startDestinationId) { inclusive = true }
                            }
                        }
                    )
                }
            }
        }
    }

    if (selectedProduct != null) {
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState
        ) {
            ProductDetailSheet(
                product = selectedProduct!!,
                onDismiss = { selectedProduct = null }
            )
        }
    }
}
