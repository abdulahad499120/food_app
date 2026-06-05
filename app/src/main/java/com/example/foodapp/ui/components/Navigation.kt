package com.example.foodapp.ui.components

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import com.example.foodapp.theme.BrandPrimary
import com.example.foodapp.theme.FoodAppTheme
import com.example.foodapp.theme.SurfaceWhite
import com.example.foodapp.theme.TextSecondary

data class BottomNavItem(
    val title: String,
    val icon: ImageVector
)

@Composable
fun BottomNavBar(
    items: List<BottomNavItem>,
    currentActiveIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier
) {
    NavigationBar(
        modifier = modifier,
        containerColor = SurfaceWhite,
        contentColor = BrandPrimary
    ) {
        items.forEachIndexed { index, item ->
            NavigationBarItem(
                selected = currentActiveIndex == index,
                onClick = { onTabSelected(index) },
                icon = {
                    Icon(imageVector = item.icon, contentDescription = item.title)
                },
                label = {
                    Text(text = item.title)
                },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = BrandPrimary,
                    selectedTextColor = BrandPrimary,
                    unselectedIconColor = TextSecondary,
                    unselectedTextColor = TextSecondary,
                    indicatorColor = SurfaceWhite
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
        BottomNavItem("Cart", Icons.Default.ShoppingCart),
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
