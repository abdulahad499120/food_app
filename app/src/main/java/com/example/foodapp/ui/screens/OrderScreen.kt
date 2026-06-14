package com.example.foodapp.ui.screens

import androidx.lifecycle.compose.collectAsStateWithLifecycle

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodapp.data.models.Product
import com.example.foodapp.theme.*
import com.example.foodapp.ui.components.EmptyStateView
import com.example.foodapp.ui.components.ErrorStateView
import com.example.foodapp.ui.state.MenuUiState
import com.example.foodapp.ui.state.MenuViewModel
import com.example.foodapp.ui.state.FavoritesViewModel
import com.example.foodapp.ui.state.PreviousOrdersViewModel
import com.example.foodapp.ui.state.AuthState
import com.example.foodapp.ui.components.GuestLockedState
import com.example.foodapp.ui.components.MasterCatalogView
import com.example.foodapp.ui.components.FilteredCategoryGridView
import com.example.foodapp.ui.components.ProductGrid
import com.example.foodapp.ui.components.OrderTopBar
import com.example.foodapp.ui.components.GuestActiveOrderBanner

@Composable
fun OrderScreen(
    viewModel: MenuViewModel,
    favoritesViewModel: FavoritesViewModel,
    previousOrdersViewModel: PreviousOrdersViewModel,
    authState: AuthState,
    cartItemCount: Int = 0,
    onNavigateToCart: () -> Unit = {},
    onNavigateToStoreSelection: () -> Unit = {},
    onNavigateToAddressSelection: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onQuickAddClick: (Product) -> Unit,
    onNavigateToAuth: () -> Unit = {},
    onNavigateToGuestTracking: (String) -> Unit = {},
    guestActiveOrderId: String? = null,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val favoriteProductIds by favoritesViewModel.favoriteProductIds.collectAsStateWithLifecycle()
    val previouslyPurchasedProducts by previousOrdersViewModel.previouslyPurchasedProducts.collectAsStateWithLifecycle()
    
    var selectedSubTab by remember { mutableStateOf("Menu") }
    val subTabs = listOf("Menu", "Featured", "Previous", "Favorites")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        val activeBranch = (uiState as? MenuUiState.Success)?.activeBranch
        val activeDeliveryAddress = (uiState as? MenuUiState.Success)?.activeDeliveryAddress
        val activeBranchName = activeBranch?.name
        
        val orderFlowState by viewModel.orderFlowState.collectAsStateWithLifecycle()
        
        val hasPrerequisite = when(orderFlowState) {
            com.example.foodapp.ui.state.OrderFlowState.PICKUP -> activeBranch != null
            com.example.foodapp.ui.state.OrderFlowState.DELIVERY -> activeDeliveryAddress != null
        }
        val handlePrerequisiteIntercept = {
            if (orderFlowState == com.example.foodapp.ui.state.OrderFlowState.PICKUP) {
                onNavigateToStoreSelection()
            } else {
                onNavigateToAddressSelection()
            }
        }

        OrderTopBar(
            orderFlowState = orderFlowState,
            onStateChange = { viewModel.setOrderFlowState(it) }
        )

        com.example.foodapp.ui.components.StoreSelectionStrip(
            activeBranchName = activeBranchName,
            cartItemCount = cartItemCount,
            onStoreClick = onNavigateToStoreSelection,
            onCartClick = onNavigateToCart
        )

        if (guestActiveOrderId != null) {
            GuestActiveOrderBanner(
                orderId = guestActiveOrderId,
                onClick = onNavigateToGuestTracking
            )
        }

        // Horizontal Sub-Navigation Tabs
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                subTabs.forEach { tab ->
                    val isSelected = selectedSubTab == tab
                    Text(
                        text = tab,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                        color = if (isSelected) TextPrimary else TextSecondary,
                        modifier = Modifier
                            .padding(end = 24.dp)
                            .clickable {
                                if (!hasPrerequisite) {
                                    handlePrerequisiteIntercept()
                                } else {
                                    selectedSubTab = tab
                                }
                            }
                    )
                }
            }
        }
        
        HorizontalDivider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

        when (val state = uiState) {
            is MenuUiState.Loading -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = VAL_BRAND_PRIMARY)
                }
            }
            is MenuUiState.Error -> {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    ErrorStateView(
                        errorMessage = state.message,
                        onRetry = { viewModel.loadData() }
                    )
                }
            }
            is MenuUiState.Success -> {
                when (selectedSubTab) {
                    "Menu" -> {
                        if (state.selectedCategoryId == null) {
                            MasterCatalogView(
                                categories = state.categories,
                                onCategoryClick = { 
                                    if (!hasPrerequisite) handlePrerequisiteIntercept() 
                                    else viewModel.selectCategory(it) 
                                }
                            )
                        } else {
                            FilteredCategoryGridView(
                                categoryName = state.categories.find { it.id == state.selectedCategoryId }?.name ?: "Category",
                                products = state.filteredProducts,
                                favoriteProductIds = favoriteProductIds,
                                onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                                onBackClick = { viewModel.selectCategory(null) },
                                onProductClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onProductClick(it) },
                                onQuickAddClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onQuickAddClick(it) }
                            )
                        }
                    }
                    "Featured" -> {
                        val featured = state.allProducts.filter { it.isFeatured }
                        if (featured.isEmpty()) {
                            TabEmptyState(
                                icon = Icons.Default.Star,
                                title = "Nothing Featured Yet",
                                message = "Check back later for seasonal specials."
                            )
                        } else {
                            ProductGrid(
                                products = featured,
                                favoriteProductIds = favoriteProductIds,
                                onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                                onProductClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onProductClick(it) },
                                onQuickAddClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onQuickAddClick(it) }
                            )
                        }
                    }
                    "Previous" -> {
                        if (authState !is AuthState.Authenticated) {
                            GuestLockedState(
                                illustration = Icons.Default.History,
                                headlineText = "View Past Orders",
                                subtext = "Sign in to easily reorder your past favorites.",
                                onSignInClick = onNavigateToAuth,
                                onSignUpClick = onNavigateToAuth
                            )
                        } else if (previouslyPurchasedProducts.isEmpty()) {
                            TabEmptyState(
                                icon = Icons.Default.History,
                                title = "Nothing here yet",
                                message = "Items you order will appear here so you can easily order them again."
                            )
                        } else {
                            ProductGrid(
                                products = previouslyPurchasedProducts,
                                favoriteProductIds = favoriteProductIds,
                                onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                                onProductClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onProductClick(it) },
                                onQuickAddClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onQuickAddClick(it) }
                            )
                        }
                    }
                    "Favorites" -> {
                        if (authState !is AuthState.Authenticated) {
                            GuestLockedState(
                                illustration = Icons.Default.Favorite,
                                headlineText = "Save Your Favorites",
                                subtext = "Sign in to keep track of the items you love the most.",
                                onSignInClick = onNavigateToAuth,
                                onSignUpClick = onNavigateToAuth
                            )
                        } else {
                            val favoriteProducts = state.allProducts.filter { favoriteProductIds.contains(it.id) }
                            if (favoriteProducts.isEmpty()) {
                                TabEmptyState(
                                    icon = Icons.Default.FavoriteBorder,
                                    title = "No favorites yet",
                                    message = "You haven't saved any favorites yet. Tap the heart on any item to save it here."
                                )
                            } else {
                                ProductGrid(
                                    products = favoriteProducts,
                                    favoriteProductIds = favoriteProductIds,
                                    onToggleFavorite = { favoritesViewModel.toggleFavorite(it) },
                                    onProductClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onProductClick(it) },
                                    onQuickAddClick = { if (!hasPrerequisite) handlePrerequisiteIntercept() else onQuickAddClick(it) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TabEmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(VAL_BACKGROUND) // Warm light mode background
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = TextSecondary.copy(alpha = 0.5f)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.headlineSmall, // Semantic H3
            color = TextPrimary,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = TextSecondary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

