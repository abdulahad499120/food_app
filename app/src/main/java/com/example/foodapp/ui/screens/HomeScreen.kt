package com.example.foodapp.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import com.example.foodapp.data.models.Product
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.ui.components.BottomNavBar
import com.example.foodapp.ui.components.BottomNavItem
import com.example.foodapp.ui.components.GhostButton
import com.example.foodapp.ui.components.PrimaryButton
import com.example.foodapp.ui.components.ProductGridItem
import com.example.foodapp.ui.components.EmptyStateView
import com.example.foodapp.ui.components.ErrorStateView
import com.example.foodapp.ui.state.MenuUiState
import com.example.foodapp.ui.state.MenuViewModel
import com.example.foodapp.ui.state.AuthState
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search

@Composable
fun HomeScreen(
    modifier: Modifier = Modifier,
    viewModel: MenuViewModel = viewModel(),
    authState: AuthState = AuthState.Unauthenticated,
    onProductClick: (Product) -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsState()

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        AnimatedContent(
            targetState = uiState,
            transitionSpec = {
                fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
            },
            label = "HomeScreenStateAnimation"
        ) { state ->
            when (state) {
                is MenuUiState.Loading -> {
                    CircularProgressIndicator(
                        modifier = Modifier.align(Alignment.Center),
                        color = BrandPrimary
                    )
                }
                is MenuUiState.Error -> {
                    ErrorStateView(
                        errorMessage = state.message,
                        onRetry = { viewModel.loadData() }, // Assuming loadData is accessible or we just let it retry somehow
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
                is MenuUiState.Success -> {
                Column(modifier = Modifier.fillMaxSize()) {
                    Spacer(modifier = Modifier.height(24.dp))
                    
                    val greetingName = if (authState is AuthState.Authenticated) {
                        authState.user.name
                    } else {
                        "Guest"
                    }
                    
                    Text(
                        text = "Good Afternoon, $greetingName!",
                        style = MaterialTheme.typography.titleLarge,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Menu Categories",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    
                    LazyRow(
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        item { Spacer(modifier = Modifier.padding(8.dp)) }
                        
                        // "All" chip
                        item {
                            if (state.selectedCategoryId == null) {
                                PrimaryButton(onClick = { viewModel.selectCategory(null) }) {
                                    Text(text = "All")
                                }
                            } else {
                                GhostButton(onClick = { viewModel.selectCategory(null) }) {
                                    Text(text = "All")
                                }
                            }
                            Spacer(modifier = Modifier.padding(4.dp))
                        }

                        items(state.categories) { category ->
                            if (state.selectedCategoryId == category.id) {
                                PrimaryButton(onClick = { viewModel.selectCategory(category.id) }) {
                                    Text(text = category.name)
                                }
                            } else {
                                GhostButton(onClick = { viewModel.selectCategory(category.id) }) {
                                    Text(text = category.name)
                                }
                            }
                            Spacer(modifier = Modifier.padding(4.dp))
                        }
                        item { Spacer(modifier = Modifier.padding(8.dp)) }
                    }

                    Spacer(modifier = Modifier.height(24.dp))
                    Text(
                        text = "Popular Near You",
                        style = MaterialTheme.typography.titleMedium,
                        color = TextPrimary,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    if (state.filteredProducts.isEmpty()) {
                        EmptyStateView(
                            icon = Icons.Default.Search,
                            title = "No items found",
                            message = "Try selecting a different category.",
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    } else {
                        LazyVerticalGrid(
                            columns = GridCells.Fixed(2),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 8.dp)
                                .weight(1f) // Ensure it takes remaining space properly
                        ) {
                            items(state.filteredProducts) { product ->
                                ProductGridItem(
                                    imageUrl = product.localImagePath,
                                    title = product.name,
                                    price = product.price,
                                    onClick = { onProductClick(product) }
                                )
                            }
                        }
                    }
                }
            }
        }
        }
    }
}
