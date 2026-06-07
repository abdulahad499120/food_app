package com.example.foodapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material.icons.filled.CardGiftcard
import androidx.compose.material3.Text
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.FoodAppTheme
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextSecondary
import androidx.compose.material3.NavigationBarItemColors
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp

data class BottomNavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentActiveIndex: Int,
    cartItemCount: Int = 0,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = SurfaceWhite,
        contentColor = BrandPrimary,
        tonalElevation = 0.dp
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = currentActiveIndex == index,
                onClick = { onTabSelected(index) },
                icon = {
                    val scale by animateFloatAsState(
                        targetValue = if (currentActiveIndex == index) 1.1f else 1.0f,
                        animationSpec = spring(
                            dampingRatio = Spring.DampingRatioMediumBouncy,
                            stiffness = Spring.StiffnessLow
                        ),
                        label = "IconScale"
                    )
                    Icon(
                        imageVector = item.icon, 
                        contentDescription = item.title,
                        modifier = Modifier.scale(scale)
                    )
                },
                label = {
                    Text(
                        text = item.title,
                        style = androidx.compose.material3.MaterialTheme.typography.labelSmall
                    )
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    indicatorColor = Color.Transparent,
                    unselectedIconColor = Color.Gray,
                    unselectedTextColor = Color.Gray
                )
            )
        }
    }
}

@Preview
@Composable
fun BottomNavBarPreview() {
    val items = listOf(
        BottomNavItem("Home", Icons.Default.Home),
        BottomNavItem("Order", Icons.Default.List),
        BottomNavItem("Gift", Icons.Default.CardGiftcard),
        BottomNavItem("Rewards", Icons.Default.Star),
        BottomNavItem("Profile", Icons.Default.Person)
    )
    FoodAppTheme {
        BottomNavBar(
            items = items,
            currentActiveIndex = 0,
            onTabSelected = {}
        )
    }
}
