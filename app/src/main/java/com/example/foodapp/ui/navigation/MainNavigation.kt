package com.example.foodapp.ui.navigation

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.IconButton
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.launch
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.dialog
import androidx.navigation.compose.navigation
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.example.foodapp.data.models.Product
import com.example.foodapp.ui.components.BottomNavBar
import com.example.foodapp.ui.components.BottomNavItem
import com.example.foodapp.ui.screens.CartScreen
import com.example.foodapp.ui.screens.HomeScreen
import com.example.foodapp.ui.screens.ProductDetailSheet
import com.example.foodapp.ui.screens.OrderHistoryScreen
import com.example.foodapp.ui.screens.RewardsScreen
import com.example.foodapp.ui.screens.GiftScreen

import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import com.example.foodapp.ui.state.MenuViewModel
import com.example.foodapp.ui.state.MenuUiState
import com.example.foodapp.ui.state.CartManager
import com.example.foodapp.ui.components.LocationPrerequisiteModal
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
    object OrderHistory : Screen("order_history")
    object Gift : Screen("gift")
    object Rewards : Screen("rewards")
    object Profile : Screen("profile")
    object AddressList : Screen("address_list")
    object AddressMapPicker : Screen("address_map_picker")
    object Payments : Screen("payments")
    object AddPayment : Screen("add_payment")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object OrderSuccess : Screen("order_success")
    
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }
    
    // Auth Flow routes
    object AuthFlow : Screen("auth_flow")
    object AuthWelcome : Screen("auth_welcome")
    object AuthSignIn : Screen("auth_sign_in")
    object AuthSignUp : Screen("auth_sign_up")
    object AuthOtp : Screen("auth_otp")
    object AuthForgotPassword : Screen("auth_forgot_password")
    object BranchLocator : Screen("branch_locator")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FoodAppRoot(
    authViewModel: AuthViewModel = viewModel()
) {
    val navController = rememberNavController()
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    // Shared Menu State across the app
    val sharedMenuViewModel: MenuViewModel = viewModel()

    // Auth State
    val authState by authViewModel.authState.collectAsState()

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Failsafe Intercept State
    var showLocationModal by remember { mutableStateOf(false) }
    var showGuestInterceptorModal by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    
    // Snackbar & Cart State
    var allowGuestViewing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cartState by CartManager.cartState.collectAsState()
    val cartItemCount = cartState.items.sumOf { it.quantity }

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home),
        BottomNavItem("Order", Icons.Default.List),
        BottomNavItem("Gift", Icons.Default.CardGiftcard),
        BottomNavItem("Rewards", Icons.Default.Star),
        BottomNavItem("Profile", Icons.Default.Person)
    )

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        // Removed global topBar to eliminate blank space. Each screen will handle its own header and cart icon.
        bottomBar = {
            // Hide bottom bar on auth flow, checkout, and success screens
            if (currentRoute?.startsWith("auth") != true && 
                currentRoute != Screen.Checkout.route && 
                currentRoute != Screen.OrderSuccess.route &&
                currentRoute != Screen.BranchLocator.route) {
                val currentIndex = when (currentRoute) {
                    Screen.Home.route -> 0
                    Screen.OrderHistory.route -> 1 // This will map to Order for now
                    Screen.Gift.route -> 2
                    Screen.Rewards.route -> 3
                    Screen.Profile.route -> 4
                    else -> 0
                }
                Column {
                    BottomNavBar(
                        items = navItems,
                        currentActiveIndex = currentIndex,
                        cartItemCount = cartItemCount,
                        onTabSelected = { index ->
                        val targetRoute = when (index) {
                            0 -> Screen.Home.route
                            1 -> Screen.OrderHistory.route // Order tab
                            2 -> Screen.Gift.route
                            3 -> Screen.Rewards.route
                            4 -> Screen.Profile.route
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
                    val uiState by sharedMenuViewModel.uiState.collectAsState()
                    HomeScreen(
                        authState = authState,
                        cartItemCount = cartItemCount,
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                        onNavigateToLocator = {
                            navController.navigate(Screen.BranchLocator.route)
                        },
                        onNavigateToAuth = {
                            navController.navigate(Screen.AuthWelcome.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onNavigateToOrder = {
                            navController.navigate(Screen.OrderHistory.route) // Order tab
                        }
                    )
                }
                composable(Screen.Cart.route) {
                    CartScreen(
                        onCheckoutRequest = {
                            if (authState is AuthState.Authenticated) {
                                navController.navigate(Screen.Checkout.route)
                            } else {
                                showGuestInterceptorModal = true
                            }
                        },
                        onNavigateToHome = {
                            navController.navigate(Screen.Home.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                            }
                        }
                    )
                }
                composable(Screen.Gift.route) {
                    if (authState is AuthState.Authenticated) {
                        GiftScreen()
                    } else {
                        com.example.foodapp.ui.components.GuestLockedState(
                            illustration = Icons.Default.CardGiftcard,
                            headlineText = "Share the Joy",
                            subtext = "Create an account to access the eGift marketplace. Send digital gift cards to friends and family instantly.",
                            onSignInClick = { navController.navigate(Screen.AuthSignIn.route) },
                            onSignUpClick = { navController.navigate(Screen.AuthSignUp.route) }
                        )
                    }
                }

                composable(Screen.Rewards.route) {
                    if (authState is AuthState.Authenticated) {
                        RewardsScreen()
                    } else {
                        com.example.foodapp.ui.components.GuestLockedState(
                            illustration = Icons.Default.Star,
                            headlineText = "Earn Stars on Every Order",
                            subtext = "Join our loyalty program to start accumulating Stars. Unlock free ice creams, dry fruit boxes, and exclusive tier benefits.",
                            onSignInClick = { navController.navigate(Screen.AuthSignIn.route) },
                            onSignUpClick = { navController.navigate(Screen.AuthSignUp.route) }
                        )
                    }
                }

                composable(Screen.Profile.route) {
                    if (authState is AuthState.Authenticated) {
                        com.example.foodapp.ui.screens.ProfileScreen(
                            user = (authState as AuthState.Authenticated).user,
                            onLogout = {
                                authViewModel.logout()
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onNavigateToAddresses = { navController.navigate(Screen.AddressList.route) },
                            onNavigateToPayments = { navController.navigate(Screen.Payments.route) }
                        )
                    } else {
                        com.example.foodapp.ui.components.GuestLockedState(
                            illustration = Icons.Default.Person,
                            headlineText = "Your Digital Wallet",
                            subtext = "Log in to save your delivery addresses, manage payment methods, and securely update your preferences.",
                            onSignInClick = { navController.navigate(Screen.AuthSignIn.route) },
                            onSignUpClick = { navController.navigate(Screen.AuthSignUp.route) }
                        )
                    }
                }
                
                composable(Screen.Payments.route) {
                    com.example.foodapp.ui.screens.PaymentMethodsScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddPayment = { navController.navigate(Screen.AddPayment.route) }
                    )
                }

                composable(Screen.AddPayment.route) {
                    com.example.foodapp.ui.screens.AddEditPaymentScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() }
                    )
                }

                composable(Screen.OrderHistory.route) {
                    val uiState by sharedMenuViewModel.uiState.collectAsState()
                    com.example.foodapp.ui.screens.OrderScreen(
                        viewModel = sharedMenuViewModel,
                        cartItemCount = cartItemCount,
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                        onNavigateToStoreSelection = { navController.navigate(Screen.BranchLocator.route) },
                        onProductClick = { product ->
                            val successState = uiState as? MenuUiState.Success
                            if (successState != null && successState.activeBranch == null && !allowGuestViewing) {
                                showLocationModal = true
                            } else {
                                selectedProduct = product
                            }
                        },
                        onQuickAddClick = { product ->
                            val successState = uiState as? MenuUiState.Success
                            if (successState != null && successState.activeBranch == null && !allowGuestViewing) {
                                showLocationModal = true
                            } else {
                                if (authState !is AuthState.Authenticated) {
                                    navController.navigate(Screen.AuthWelcome.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else if (product.requiresCustomization) {
                                    selectedProduct = product
                                } else {
                                    CartManager.addItem(product)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Added ${product.name} to your cart.")
                                    }
                                }
                            }
                        }
                    )
                }
                composable(Screen.OrderDetail.route) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    if (orderId != null) {
                        com.example.foodapp.ui.screens.OrderDetailScreen(
                            orderId = orderId,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                
                composable(Screen.OrderTracking.route) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    if (orderId != null) {
                        com.example.foodapp.ui.screens.ActiveOrderTrackingScreen(
                            orderId = orderId,
                            onNavigateBack = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } }
                        )
                    }
                }
                
                // Nested Auth Graph
                navigation(route = Screen.AuthFlow.route, startDestination = Screen.AuthWelcome.route) {
                    dialog(Screen.AuthWelcome.route) {
                        WelcomeScreen(
                            onNavigateToSignIn = { navController.navigate(Screen.AuthSignIn.route) },
                            onNavigateToSignUp = { navController.navigate(Screen.AuthSignUp.route) },
                            onDismiss = { navController.popBackStack() }
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
                            onNavigateToForgotPassword = { navController.navigate(Screen.AuthForgotPassword.route) },
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
                    composable(Screen.AuthForgotPassword.route) {
                        com.example.foodapp.ui.screens.ForgotPasswordScreen(
                            authViewModel = authViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                
                composable(Screen.Checkout.route) {
                    val authState by authViewModel.authState.collectAsState()
                    CheckoutScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddressList = { navController.navigate(Screen.AddressList.route) },
                        onOrderSuccess = { orderId ->
                            // Pop checkout and cart, navigate to tracking
                            navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                                popUpTo(Screen.Cart.route) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable(Screen.AddressList.route) {
                    val authState by authViewModel.authState.collectAsState()
                    com.example.foodapp.ui.screens.AddressListScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMapPicker = { navController.navigate(Screen.AddressMapPicker.route) },
                        onAddressSelected = { navController.popBackStack() } // For now, just pop back
                    )
                }
                
                composable(Screen.AddressMapPicker.route) {
                    val authState by authViewModel.authState.collectAsState()
                    com.example.foodapp.ui.screens.AddressMapPickerScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() }
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
                composable(Screen.BranchLocator.route) {
                    com.example.foodapp.ui.screens.BranchLocatorScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onBranchSelected = { branchId ->
                            sharedMenuViewModel.setActiveBranch(branchId)
                            navController.popBackStack()
                        }
                    )
                }
            }
        }
    }

    if (selectedProduct != null) {
        val uiState by sharedMenuViewModel.uiState.collectAsState()
        val isGuestViewing = (uiState as? MenuUiState.Success)?.activeBranch == null && allowGuestViewing
        ModalBottomSheet(
            onDismissRequest = { selectedProduct = null },
            sheetState = sheetState
        ) {
            ProductDetailSheet(
                product = selectedProduct!!,
                onDismiss = { selectedProduct = null },
                onAddToCartClick = { cartItem ->
                    if (authState !is AuthState.Authenticated) {
                        selectedProduct = null
                        navController.navigate(Screen.AuthWelcome.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    } else {
                        CartManager.addItem(cartItem)
                        selectedProduct = null
                        coroutineScope.launch {
                            snackbarHostState.currentSnackbarData?.dismiss()
                            snackbarHostState.showSnackbar("Added ${cartItem.product.name} to your cart.")
                        }
                    }
                },
                isGuestViewing = isGuestViewing
            )
        }
    }
    
    if (showLocationModal) {
        LocationPrerequisiteModal(
            onDismiss = { showLocationModal = false },
            onChooseStore = {
                showLocationModal = false
                navController.navigate(Screen.BranchLocator.route)
            },
            onProceedAsGuest = {
                showLocationModal = false
                allowGuestViewing = true
            }
        )
    }

    if (showGuestInterceptorModal) {
        AlertDialog(
            onDismissRequest = { showGuestInterceptorModal = false },
            title = { Text("Checkout") },
            text = { Text("Sign in to earn Stars and redeem rewards, or continue as a guest.") },
            confirmButton = {
                Button(
                    onClick = {
                        showGuestInterceptorModal = false
                        navController.navigate(Screen.AuthWelcome.route) {
                            launchSingleTop = true
                            restoreState = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = com.example.foodapp.theme.VAL_BRAND_PRIMARY)
                ) {
                    Text("Sign In")
                }
            },
            dismissButton = {
                OutlinedButton(
                    onClick = {
                        showGuestInterceptorModal = false
                        navController.navigate(Screen.Checkout.route)
                    }
                ) {
                    Text("Checkout as Guest")
                }
            }
        )
    }
}
