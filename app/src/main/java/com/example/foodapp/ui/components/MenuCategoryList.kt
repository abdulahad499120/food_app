package com.example.foodapp.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.animation.core.updateTransition
import androidx.compose.animation.animateColor
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.tween
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.foodapp.data.models.Category
import com.example.foodapp.data.models.Product
import com.example.foodapp.theme.TextPrimary
import com.example.foodapp.theme.VAL_BRAND_PRIMARY
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.AnimatedVisibilityScope

@Composable
fun MasterCatalogView(
    categories: List<Category>,
    onCategoryClick: (String) -> Unit
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp)
    ) {
        item {
            Text(
                text = "Menu",
                style = MaterialTheme.typography.headlineLarge, // Semantic H1
                fontWeight = FontWeight.Bold,
                color = TextPrimary,
                modifier = Modifier.padding(bottom = 24.dp, top = 8.dp)
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
                    style = MaterialTheme.typography.headlineMedium, // Semantic H2
                    fontWeight = FontWeight.SemiBold,
                    color = TextPrimary
                )
            }
            HorizontalDivider(color = Color.LightGray.copy(alpha = 0.3f), thickness = 1.dp)
        }
    }
}

@Composable
fun FilteredCategoryGridView(
    categoryName: String,
    products: List<Product>,
    favoriteProductIds: List<String>,
    onToggleFavorite: (String) -> Unit,
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
        
        ProductGrid(
            products = products,
            favoriteProductIds = favoriteProductIds,
            onToggleFavorite = onToggleFavorite,
            onProductClick = onProductClick,
            onQuickAddClick = onQuickAddClick
        )
    }
}

@Composable
fun ProductGrid(
    products: List<Product>,
    favoriteProductIds: List<String>,
    onToggleFavorite: (String) -> Unit,
    onProductClick: (Product) -> Unit,
    onQuickAddClick: (Product) -> Unit
) {
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
                isFavorite = favoriteProductIds.contains(product.id),
                onToggleFavorite = { onToggleFavorite(product.id) },
                onClick = { onProductClick(product) },
                onQuickAddClick = { onQuickAddClick(product) }
            )
        }
    }
}

@Composable
fun ProductNodeComponent(
    product: Product,
    isFavorite: Boolean,
    onToggleFavorite: () -> Unit,
    onClick: () -> Unit,
    onQuickAddClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(140.dp)
        ) {
            Box(modifier = Modifier.fillMaxSize().clip(CircleShape).background(Color.LightGray.copy(alpha = 0.2f))) {
                @OptIn(ExperimentalSharedTransitionApi::class)
                val sharedTransitionScope = com.example.foodapp.ui.navigation.LocalSharedTransitionScope.current
                @OptIn(ExperimentalSharedTransitionApi::class)
                val animatedVisibilityScope = com.example.foodapp.ui.navigation.LocalAnimatedVisibilityScope.current

                @OptIn(ExperimentalSharedTransitionApi::class)
                if (sharedTransitionScope != null && animatedVisibilityScope != null) {
                    with(sharedTransitionScope) {
                        AsyncImage(
                            model = product.localImagePath,
                            contentDescription = product.name,
                            modifier = Modifier.fillMaxSize().sharedElement(
                                rememberSharedContentState(key = "image_${product.id}"),
                                animatedVisibilityScope = animatedVisibilityScope
                            ),
                            contentScale = ContentScale.Crop
                        )
                    }
                } else {
                    AsyncImage(
                        model = product.localImagePath,
                        contentDescription = product.name,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            
            // Heart toggle in top right
            val transition = updateTransition(targetState = isFavorite, label = "FavoriteTransition")
            
            val heartScale by transition.animateFloat(
                transitionSpec = {
                    spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                },
                label = "HeartScale"
            ) { favorite ->
                if (favorite) 1.2f else 1.0f
            }

            val heartColor by transition.animateColor(
                transitionSpec = { tween(durationMillis = 300) },
                label = "HeartColor"
            ) { favorite ->
                if (favorite) VAL_BRAND_PRIMARY else Color.Gray
            }

            Box(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = (-4).dp, y = 4.dp)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.8f)),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onToggleFavorite, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        tint = heartColor,
                        modifier = Modifier.size(20.dp).scale(heartScale)
                    )
                }
            }
            
            // Frictionless Cart Adder Utility
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .offset(x = 0.dp, y = 0.dp)
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(VAL_BRAND_PRIMARY),
                contentAlignment = Alignment.Center
            ) {
                IconButton(onClick = onQuickAddClick, modifier = Modifier.fillMaxSize()) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Add to Cart",
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
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
