package com.example.foodapp.ui.navigation

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.Alignment
import androidx.compose.material3.Surface
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.BottomSheetDefaults
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
import com.example.foodapp.ui.screens.DeepCustomizerSheet
import com.example.foodapp.ui.screens.OrderHistoryScreen
import com.example.foodapp.ui.screens.RewardsScreen
import com.example.foodapp.ui.screens.RewardsScreen
import com.example.foodapp.ui.screens.GiftScreen
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.utils.NetworkConnectivityObserver
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.state.AuthViewModel
import com.example.foodapp.ui.state.MenuViewModel
import com.example.foodapp.ui.state.OrderSessionViewModel
import com.example.foodapp.ui.state.FulfillmentMode
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

@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object OrderHistory : Screen("order_history")
    object Gift : Screen("gift")
    object Rewards : Screen("rewards")
    object Profile : Screen("profile")
    object OrderHistoryList : Screen("order_history_list")
    object AddressList : Screen("address_list")
    object AddressMapPicker : Screen("address_map_picker")
    object Payments : Screen("payments")
    object AddPayment : Screen("add_payment")
    object Cart : Screen("cart")
    object Checkout : Screen("checkout")
    object OrderSuccess : Screen("order_success")
    object PickupStatus : Screen("pickup_status/{orderId}") {
        fun createRoute(orderId: String) = "pickup_status/$orderId"
    }
    object RiderChat : Screen("rider_chat/{orderId}") {
        fun createRoute(orderId: String) = "rider_chat/$orderId"
    }
    
    object OrderDetail : Screen("order_detail/{orderId}") {
        fun createRoute(orderId: String) = "order_detail/$orderId"
    }

    object OrderTracking : Screen("order_tracking/{orderId}") {
        fun createRoute(orderId: String) = "order_tracking/$orderId"
    }
    
    object OrderComplete : Screen("order_complete/{orderId}") {
        fun createRoute(orderId: String) = "order_complete/$orderId"
    }

    object SupportChat : Screen("support_chat/{orderId}?prompt={prompt}") {
        fun createRoute(orderId: String, prompt: String? = null) = 
            if (prompt != null) "support_chat/$orderId?prompt=${android.net.Uri.encode(prompt)}"
            else "support_chat/$orderId"
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
    val orderSessionViewModel: OrderSessionViewModel = viewModel()
    val favoritesViewModel: com.example.foodapp.ui.state.FavoritesViewModel = viewModel()
    val previousOrdersViewModel: com.example.foodapp.ui.state.PreviousOrdersViewModel = viewModel()

    // Sync OrderSession -> MenuViewModel
    val sessionFulfillmentMode by orderSessionViewModel.fulfillmentMode.collectAsStateWithLifecycle()
    val sessionActiveBranch by orderSessionViewModel.activeBranch.collectAsStateWithLifecycle()
    
    androidx.compose.runtime.LaunchedEffect(sessionFulfillmentMode) {
        sessionFulfillmentMode?.let { sharedMenuViewModel.setFulfillmentMode(it) }
    }
    androidx.compose.runtime.LaunchedEffect(sessionActiveBranch) {
        sessionActiveBranch?.let { sharedMenuViewModel.setActiveBranch(it.branchId) }
    }

    val authState by authViewModel.authState.collectAsStateWithLifecycle()
    val userId = (authState as? AuthState.Authenticated)?.user?.uid
    
    // Connectivity
    val navContext = LocalContext.current
    val connectivityObserver = remember { NetworkConnectivityObserver(navContext) }
    val isOnline by connectivityObserver.connectivityState.collectAsStateWithLifecycle(initialValue = true)
    
    // User Active Order State
    val orderHistoryViewModel: com.example.foodapp.ui.state.OrderHistoryViewModel = viewModel()
    val orderHistoryState by orderHistoryViewModel.uiState.collectAsStateWithLifecycle()
    val userActiveOrderId = (orderHistoryState as? com.example.foodapp.ui.state.OrderHistoryUiState.Success)?.activeOrders?.firstOrNull()?.orderId

    androidx.compose.runtime.LaunchedEffect(userId) {
        if (userId != null) {
            favoritesViewModel.loadFavoritesForUser(userId)
            previousOrdersViewModel.loadPreviousOrdersForUser(userId)
            orderHistoryViewModel.loadOrders(userId)
        }
    }

    // Bottom Sheet State
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    // Guest Session State
    val context = LocalContext.current
    val guestSessionRepo = remember { com.example.foodapp.data.repository.GuestSessionRepository(context) }
    val guestOrderId by guestSessionRepo.guestActiveOrderIdFlow.collectAsStateWithLifecycle(initialValue = null)

    var hasCheckedGuestSession by remember { mutableStateOf(false) }

    androidx.compose.runtime.LaunchedEffect(guestOrderId, authState) {
        if (!hasCheckedGuestSession && guestOrderId != null && authState is AuthState.Unauthenticated) {
            hasCheckedGuestSession = true
            navController.navigate(Screen.OrderTracking.createRoute(guestOrderId!!)) {
                popUpTo(Screen.Home.route) { inclusive = false }
            }
        } else if (guestOrderId == null || authState is AuthState.Authenticated) {
            hasCheckedGuestSession = true
        }
    }
    
    // Failsafe Intercept State
    var showLocationModal by remember { mutableStateOf(false) }
    var showGuestInterceptorModal by remember { mutableStateOf(false) }
    var showProductDetailSheet by remember { mutableStateOf(false) }
    var selectedProduct by remember { mutableStateOf<Product?>(null) }
    
    // Snackbar & Cart State
    var allowGuestViewing by remember { mutableStateOf(false) }
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    val cartState by CartManager.cartState.collectAsStateWithLifecycle()
    val cartItemCount = cartState.items.sumOf { it.quantity }

    val navItems = listOf(
        BottomNavItem("Home", Icons.Default.Home),
        BottomNavItem("Menu", Icons.Default.RestaurantMenu),
        BottomNavItem("Orders", Icons.Default.List),
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
                    Screen.OrderHistory.route -> 1 // Menu tab
                    Screen.OrderHistoryList.route -> 2 // Orders tab
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
                            1 -> Screen.OrderHistory.route // Menu tab
                            2 -> Screen.OrderHistoryList.route // Orders tab
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
        SharedTransitionLayout {
            CompositionLocalProvider(LocalSharedTransitionScope provides this) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(paddingValues)
                ) {
                    NavHost(
                        navController = navController,
                startDestination = Screen.Home.route,
                enterTransition = { androidx.compose.animation.slideInHorizontally(initialOffsetX = { 300 }, animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
                exitTransition = { androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) },
                popEnterTransition = { androidx.compose.animation.fadeIn(animationSpec = androidx.compose.animation.core.tween(300)) },
                popExitTransition = { androidx.compose.animation.slideOutHorizontally(targetOffsetX = { 300 }, animationSpec = androidx.compose.animation.core.tween(300)) + androidx.compose.animation.fadeOut(animationSpec = androidx.compose.animation.core.tween(300)) }
            ) {
                composable(Screen.Home.route) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        val uiState by sharedMenuViewModel.uiState.collectAsStateWithLifecycle()
                        HomeScreen(
                            orderSessionViewModel = orderSessionViewModel,
                            authState = authState,
                            cartItemCount = cartItemCount,
                            activeOrderId = if (authState is AuthState.Unauthenticated) guestOrderId else userActiveOrderId,
                            onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                            onNavigateToAuth = {
                                navController.navigate(Screen.AuthWelcome.route) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },
                            onNavigateToOrder = {
                                navController.navigate(Screen.OrderHistory.route) // Order tab
                            },
                            onNavigateToTracking = { oid ->
                                navController.navigate(Screen.OrderTracking.createRoute(oid))
                            }
                        )
                    }
                }
                composable(Screen.Cart.route) {
                    val cartMenuState = sharedMenuViewModel.uiState.value as? com.example.foodapp.ui.state.MenuUiState.Success
                    val activeAddr by orderSessionViewModel.activeDeliveryAddress.collectAsStateWithLifecycle()
                    CartScreen(
                        activeBranch = cartMenuState?.activeBranch,
                        authState = authState,
                        hasActiveDeliveryAddress = activeAddr != null,
                        onAddressSaved = { orderSessionViewModel.setDeliveryAddress(it) },
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
                            onNavigateToOrderHistory = { navController.navigate(Screen.OrderHistoryList.route) },
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

                composable(Screen.OrderHistoryList.route) {
                    OrderHistoryScreen(
                        authState = authState,
                        onNavigateHome = { navController.navigate(Screen.Home.route) },
                        onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                        onNavigateToOrderDetail = { orderId -> navController.navigate(Screen.OrderDetail.createRoute(orderId)) }
                    )
                }

                composable(Screen.AddPayment.route) {
                    com.example.foodapp.ui.screens.AddEditPaymentScreen(
                        userId = userId ?: "",
                        onBack = { navController.popBackStack() },
                        onSuccess = { navController.popBackStack() }
                    )
                }

                composable(Screen.OrderHistory.route) {
                    CompositionLocalProvider(LocalAnimatedVisibilityScope provides this) {
                        val uiState by sharedMenuViewModel.uiState.collectAsStateWithLifecycle()
                        com.example.foodapp.ui.screens.OrderScreen(
                            viewModel = sharedMenuViewModel,
                            orderSessionViewModel = orderSessionViewModel,
                            favoritesViewModel = favoritesViewModel,
                            previousOrdersViewModel = previousOrdersViewModel,
                            authState = authState,
                            cartItemCount = cartItemCount,
                            activeOrderId = if (authState is AuthState.Unauthenticated) guestOrderId else userActiveOrderId,
                            onNavigateToCart = { navController.navigate(Screen.Cart.route) },
                            onNavigateToStoreSelection = { navController.navigate(Screen.BranchLocator.route) },
                            onNavigateToAddressSelection = { navController.navigate(Screen.AddressMapPicker.route) },
                            onNavigateToAuth = {
                                navController.navigate(Screen.AuthWelcome.route)
                            },
                            onNavigateToTracking = { oid ->
                                navController.navigate(Screen.OrderTracking.createRoute(oid)) {
                                    launchSingleTop = true
                                    restoreState = true
                                }
                            },

                            onProductClick = { product ->
                                selectedProduct = product
                                showProductDetailSheet = true
                            },
                            onQuickAddClick = { product ->
                                if (authState !is AuthState.Authenticated) {
                                    navController.navigate(Screen.AuthWelcome.route) {
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                } else if (product.requiresCustomization) {
                                    selectedProduct = product
                                    showProductDetailSheet = true
                                } else {
                                    CartManager.addItem(product)
                                    coroutineScope.launch {
                                        snackbarHostState.currentSnackbarData?.dismiss()
                                        snackbarHostState.showSnackbar("Added ${product.name} to your cart.")
                                    }
                                }
                            }
                        )
                    }
                }
                composable(Screen.OrderDetail.route) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    if (orderId != null) {
                        com.example.foodapp.ui.screens.OrderDetailScreen(
                            orderId = orderId,
                            onNavigateBack = { navController.popBackStack() },
                            onNavigateToCart = { navController.navigate(Screen.Cart.route) }
                        )
                    }
                }
                
                composable(Screen.OrderTracking.route) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    if (orderId != null) {
                        val liveTrackingViewModel: com.example.foodapp.ui.state.LiveTrackingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        val successState = sharedMenuViewModel.uiState.value as? com.example.foodapp.ui.state.MenuUiState.Success
                        
                        androidx.compose.runtime.LaunchedEffect(orderId) {
                            liveTrackingViewModel.initializeTracking(
                                branch = successState?.activeBranch,
                                address = orderSessionViewModel.activeDeliveryAddress.value,
                                orderId = orderId
                            )
                        }

                        val trackingUiState by liveTrackingViewModel.uiState.collectAsStateWithLifecycle()
                        
                        if (trackingUiState.fulfillmentMode == "PICKUP") {
                            com.example.foodapp.ui.screens.PickupStatusScreen(
                                viewModel = liveTrackingViewModel,
                                onNavigateBack = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } },
                                onNavigateToOrderComplete = { oid -> 
                                    navController.navigate(Screen.OrderComplete.createRoute(oid)) {
                                        popUpTo(Screen.OrderTracking.route) { inclusive = true }
                                    }
                                }
                            )
                        } else {
                            com.example.foodapp.ui.screens.LiveTrackingScreen(
                                viewModel = liveTrackingViewModel,
                                onNavigateBack = { navController.navigate(Screen.Home.route) { popUpTo(Screen.Home.route) { inclusive = false } } },
                                onNavigateToSupport = { oid, prompt -> navController.navigate(Screen.SupportChat.createRoute(oid, prompt)) },
                                onNavigateToOrderComplete = { oid -> 
                                    navController.navigate(Screen.OrderComplete.createRoute(oid)) {
                                        popUpTo(Screen.OrderTracking.route) { inclusive = true }
                                    }
                                },
                                onNavigateToRiderChat = { oid -> navController.navigate(Screen.RiderChat.createRoute(oid)) }
                            )
                        }
                    }
                }
                
                composable(
                    route = Screen.PickupStatus.route,
                    arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                    val liveTrackingViewModel: com.example.foodapp.ui.state.LiveTrackingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    
                    androidx.compose.runtime.LaunchedEffect(orderId) {
                        liveTrackingViewModel.initializeTracking(
                            branch = null, // Will be fetched from state
                            address = null,
                            orderId = orderId
                        )
                    }

                    com.example.foodapp.ui.screens.PickupStatusScreen(
                        onNavigateBack = { navController.popBackStack() },
                        viewModel = liveTrackingViewModel,
                        onNavigateToOrderComplete = { oid -> 
                            navController.navigate(Screen.OrderComplete.createRoute(oid)) {
                                popUpTo(Screen.PickupStatus.route) { inclusive = true }
                            }
                        }
                    )
                }

                composable(
                    route = Screen.RiderChat.route,
                    arguments = listOf(androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType })
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId") ?: return@composable
                    val chatViewModel: com.example.foodapp.ui.state.ChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    val liveTrackingViewModel: com.example.foodapp.ui.state.LiveTrackingViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                    
                    androidx.compose.runtime.LaunchedEffect(orderId) {
                        liveTrackingViewModel.initializeTracking(
                            branch = null,
                            address = null,
                            orderId = orderId
                        )
                    }

                    com.example.foodapp.ui.screens.RiderChatScreen(
                        orderId = orderId,
                        onNavigateBack = { navController.popBackStack() },
                        chatViewModel = chatViewModel,
                        liveTrackingViewModel = liveTrackingViewModel
                    )
                }
                
                composable(Screen.OrderComplete.route) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    if (orderId != null) {
                        com.example.foodapp.ui.screens.OrderCompleteScreen(
                            orderId = orderId,
                            onNavigateHome = {
                                navController.navigate(Screen.Home.route) {
                                    popUpTo(navController.graph.startDestinationId) { inclusive = true }
                                }
                            },
                            onNavigateToSupport = { oid -> navController.navigate(Screen.SupportChat.createRoute(oid)) }
                        )
                    }
                }

                composable(
                    route = Screen.SupportChat.route,
                    arguments = listOf(
                        androidx.navigation.navArgument("orderId") { type = androidx.navigation.NavType.StringType },
                        androidx.navigation.navArgument("prompt") { 
                            type = androidx.navigation.NavType.StringType
                            nullable = true
                            defaultValue = null
                        }
                    )
                ) { backStackEntry ->
                    val orderId = backStackEntry.arguments?.getString("orderId")
                    val prompt = backStackEntry.arguments?.getString("prompt")
                    if (orderId != null) {
                        val supportChatViewModel: com.example.foodapp.ui.state.SupportChatViewModel = androidx.lifecycle.viewmodel.compose.viewModel()
                        androidx.compose.runtime.LaunchedEffect(orderId) {
                            supportChatViewModel.initialize(orderId, prompt)
                        }
                        com.example.foodapp.ui.screens.SupportChatScreen(
                            viewModel = supportChatViewModel,
                            onNavigateBack = { navController.popBackStack() }
                        )
                    }
                }
                
                // Nested Auth Graph
                navigation(route = Screen.AuthFlow.route, startDestination = Screen.AuthWelcome.route) {
                    composable(Screen.AuthWelcome.route) {
                        com.example.foodapp.ui.screens.AuthWelcomeScreen(
                            viewModel = authViewModel,
                            onNavigateToHome = { navController.popBackStack(Screen.AuthFlow.route, inclusive = true) },
                            onNavigateToEmailSignIn = { navController.navigate(Screen.AuthSignIn.route) }
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
                    val authState by authViewModel.authState.collectAsStateWithLifecycle()
                    val menuState = sharedMenuViewModel.uiState.value as? MenuUiState.Success
                    CheckoutScreen(
                        authState = authState,
                        activeBranch = menuState?.activeBranch,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAddressList = { navController.navigate(Screen.AddressList.route) },
                        onNavigateToPayments = { navController.navigate(Screen.Payments.route) },
                        onOrderSuccess = { orderId ->
                            // Pop checkout and cart, navigate to tracking
                            navController.navigate(Screen.OrderTracking.createRoute(orderId)) {
                                popUpTo(Screen.Cart.route) { inclusive = true }
                            }
                        }
                    )
                }
                
                composable(Screen.AddressList.route) {
                    val authState by authViewModel.authState.collectAsStateWithLifecycle()
                    com.example.foodapp.ui.screens.AddressListScreen(
                        authState = authState,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToMapPicker = { navController.navigate(Screen.AddressMapPicker.route) },
                        onAddressSelected = { navController.popBackStack() } // For now, just pop back
                    )
                }
                
                composable(Screen.AddressMapPicker.route) {
                    val authState by authViewModel.authState.collectAsStateWithLifecycle()
                    val uiState by sharedMenuViewModel.uiState.collectAsStateWithLifecycle()
                    val branchLoc = (uiState as? MenuUiState.Success)?.activeBranch?.location
                    com.example.foodapp.ui.screens.AddressMapPickerScreen(
                        authState = authState,
                        branchLocation = branchLoc,
                        onNavigateBack = { navController.popBackStack() },
                        onNavigateToAuth = {
                            navController.navigate(Screen.AuthWelcome.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        onAddressSaved = { newAddress ->
                            orderSessionViewModel.setDeliveryAddress(newAddress)
                            navController.popBackStack()
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
                composable(Screen.BranchLocator.route) {
                    com.example.foodapp.ui.screens.BranchLocatorScreen(
                        onNavigateBack = { navController.popBackStack() },
                        onBranchSelected = { branch ->
                            orderSessionViewModel.setPickupBranch(branch)
                            navController.popBackStack()
                        },
                        onSkip = {
                            navController.popBackStack()
                        },
                        onSwitchToDelivery = {
                            orderSessionViewModel.setFulfillmentMode(FulfillmentMode.DELIVERY)
                            navController.popBackStack()
                            // If needed, we can trigger address map picker here. But popBackStack is fine for now as it goes back to Home/Menu where they can select Delivery.
                        }
                    )
                } // composable BranchLocator
            } // NavHost
            
            // Offline Banner
            androidx.compose.animation.AnimatedVisibility(
                visible = !isOnline,
                enter = androidx.compose.animation.slideInVertically() + androidx.compose.animation.fadeIn(),
                exit = androidx.compose.animation.slideOutVertically() + androidx.compose.animation.fadeOut(),
                modifier = Modifier.align(Alignment.TopCenter).zIndex(10f)
            ) {
                Surface(
                    color = Color.Black.copy(alpha = 0.8f),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 48.dp, start = 16.dp, end = 16.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(12.dp))
                ) {
                    Text(
                        text = "You are currently offline. Check your connection.",
                        color = Color.White,
                        textAlign = TextAlign.Center,
                        style = androidx.compose.material3.MaterialTheme.typography.labelMedium,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            } // AnimatedVisibility
        } // Box
    } // CompositionLocalProvider
} // SharedTransitionLayout
} // Scaffold

    if (showProductDetailSheet && selectedProduct != null) {
        val uiState by sharedMenuViewModel.uiState.collectAsStateWithLifecycle()
        val isGuestViewing = (uiState as? MenuUiState.Success)?.activeBranch == null && allowGuestViewing
        
        ModalBottomSheet(
            onDismissRequest = { 
                showProductDetailSheet = false 
                selectedProduct = null
            },
            sheetState = sheetState,
            containerColor = Color.White,
            dragHandle = { BottomSheetDefaults.DragHandle() },
            modifier = Modifier.fillMaxHeight(0.9f)
        ) {
            val favoriteProductIds by favoritesViewModel.favoriteProductIds.collectAsStateWithLifecycle()
            DeepCustomizerSheet(
                product = selectedProduct!!,
                onDismiss = {
                    showProductDetailSheet = false
                    selectedProduct = null
                },
                onAddToCartClick = { cartItem ->
                    CartManager.addItem(cartItem)
                    showProductDetailSheet = false
                    selectedProduct = null
                    coroutineScope.launch {
                        snackbarHostState.currentSnackbarData?.dismiss()
                        snackbarHostState.showSnackbar("Added ${cartItem.product.name} to your cart.")
                    }
                },
                isGuestViewing = isGuestViewing,
                isFavorite = favoriteProductIds.contains(selectedProduct!!.id),
                onToggleFavorite = { favoritesViewModel.toggleFavorite(selectedProduct!!.id) }
            )
        }
    }
    
    if (showLocationModal) {
        LocationPrerequisiteModal(
            onDismiss = { showLocationModal = false },
            onChooseStore = {
                showLocationModal = false
                if (navController.currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) {
                    navController.navigate(Screen.BranchLocator.route)
                }
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
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) {
                            navController.navigate(Screen.AuthWelcome.route) {
                                launchSingleTop = true
                                restoreState = true
                            }
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
                        if (navController.currentBackStackEntry?.lifecycle?.currentState == androidx.lifecycle.Lifecycle.State.RESUMED) {
                            navController.navigate(Screen.Checkout.route)
                        }
                    }
                ) {
                    Text("Checkout as Guest")
                }
            }
        )
    }
}
