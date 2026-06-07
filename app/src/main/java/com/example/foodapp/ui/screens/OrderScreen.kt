package com.example.foodapp.ui.screens

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
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodapp.data.models.Product
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.TextSecondary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import com.example.foodapp.ui.components.EmptyStateView
import com.example.foodapp.ui.components.ErrorStateView
import com.example.foodapp.ui.state.MenuUiState
import com.example.foodapp.ui.state.MenuViewModel

@Composable
fun OrderScreen(
    viewModel: MenuViewModel,
    cartItemCount: Int = 0,
    onNavigateToCart: () -> Unit = {},
    onNavigateToStoreSelection: () -> Unit = {},
    onProductClick: (Product) -> Unit,
    onQuickAddClick: (Product) -> Unit,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    var selectedSubTab by remember { mutableStateOf("Menu") }
    val subTabs = listOf("Menu", "Featured", "Previous", "Favorites")

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(SurfaceWhite)
    ) {
        // Top Store Selection Strip
        val activeBranch = (uiState as? MenuUiState.Success)?.activeBranch
        val activeBranchName = activeBranch?.name
        com.example.foodapp.ui.components.StoreSelectionStrip(
            activeBranchName = activeBranchName,
            cartItemCount = cartItemCount,
            onStoreClick = onNavigateToStoreSelection,
            onCartClick = onNavigateToCart
        )

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
                            .clickable { selectedSubTab = tab }
                    )
                }
            }
        }
        
        Divider(color = Color.LightGray.copy(alpha = 0.5f), thickness = 1.dp)

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
                if (state.selectedCategoryId == null) {
                    // Master Catalog View
                    MasterCatalogView(
                        categories = state.categories,
                        onCategoryClick = { viewModel.selectCategory(it) }
                    )
                } else {
                    // Filtered Grid View
                    FilteredCategoryGridView(
                        categoryName = state.categories.find { it.id == state.selectedCategoryId }?.name ?: "Category",
                        products = state.filteredProducts,
                        onBackClick = { viewModel.selectCategory(null) },
                        onProductClick = onProductClick,
                        onQuickAddClick = onQuickAddClick
                    )
                }
            }
        }
    }
}

/**
 * H2 Semantic Header implementation for Category tree
 */
@Composable
fun MasterCatalogView(
    categories: List<com.example.foodapp.data.models.Category>,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Premium Dry Fruits",
                style = MaterialTheme.typography.headlineMedium, // Semantic H2
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 16.dp, top = 8.dp)
            )
        }
        items(categories) { category ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCategoryClick(category.id) }
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = TextPrimary
                )
            }
            Divider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

/**
 * Screen 04: Filtered Category Grid List View
 */
@Composable
fun FilteredCategoryGridView(
    categoryName: String,
    products: List<Product>,
    onBackClick: () -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddClick: (Product) -> Unit
) {
    Column(modifier = Modifier.fillMaxSize()) {
        // Navigation Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBackClick,
                modifier = Modifier.padding(end = 8.dp)
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = TextPrimary
                )
            }
            Text(
                text = categoryName,
                style = MaterialTheme.typography.headlineMedium, // Semantic H2
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
        }
        
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp)
        ) {
            items(products, key = { it.id }) { product ->
                ProductNodeComponent(
                    product = product,
                    onClick = { onProductClick(product) },
                    onQuickAddClick = { onQuickAddClick(product) }
                )
            }
        }
    }
}

@Composable
fun ProductNodeComponent(
    product: Product,
    onClick: () -> Unit,
    onQuickAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Fix clipping: Box does NOT have clip(CircleShape), only the AsyncImage does
        Box(
            modifier = Modifier.size(140.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f))) {
                AsyncImage(
                    model = product.localImagePath,
                    contentDescription = product.name,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            }
            
            // Frictionless Cart Adder Utility
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 0.dp, y = 0.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(VAL_BRAND_PRIMARY)
                    .clickable { onQuickAddClick() },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Add,
                    contentDescription = "Add to Cart",
                    tint = Color.White,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = product.name,
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = TextPrimary,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            maxLines = 2
        )
    }
}
