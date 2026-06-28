package com.example.foodapp.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Work
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector

enum class RiderTab(val title: String, val icon: ImageVector) {
    JOBS("Jobs", Icons.Default.Work),
    HISTORY("History", Icons.Default.History),
    PROFILE("Profile", Icons.Default.Person)
}

@Composable
fun MainDashboardScreen(
    onLogout: () -> Unit,
    jobsContent: @Composable () -> Unit,
    historyContent: @Composable () -> Unit,
    profileContent: @Composable () -> Unit
) {
    var selectedTab by remember { mutableStateOf(RiderTab.JOBS) }

    Scaffold(
        bottomBar = {
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                RiderTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(tab.icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                            selectedTextColor = MaterialTheme.colorScheme.onSurface,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    )
                }
            }
        }
    ) { paddingValues ->
        Box(modifier = Modifier.padding(paddingValues)) {
            when (selectedTab) {
                RiderTab.JOBS -> jobsContent()
                RiderTab.HISTORY -> historyContent()
                RiderTab.PROFILE -> profileContent()
            }
        }
    }
}
