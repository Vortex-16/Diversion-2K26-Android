package com.example.chaintorquenative.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.chaintorquenative.Screen

// Brand colors
private val PrimaryColor = Color(0xFF6366F1)
private val SurfaceColor = Color(0xFF1E293B)

@Composable
fun BottomNavigationBar(
    currentScreen: Screen,
    onScreenSelected: (Screen) -> Unit
) {
    NavigationBar(
        containerColor = SurfaceColor,
        contentColor = Color.White
    ) {
        val items = listOf(
            BottomNavItem(Screen.MARKETPLACE, Icons.Default.Home, "Marketplace"),
            BottomNavItem(Screen.PROFILE, Icons.Default.AccountCircle, "Profile"),
            BottomNavItem(Screen.WALLET, Icons.Filled.AccountBalanceWallet, "Wallet"),
            BottomNavItem(Screen.SETTINGS, Icons.Default.Settings, "Settings")
        )

        items.forEach { item ->
            NavigationBarItem(
                icon = { Icon(item.icon, contentDescription = item.label) },
                label = { Text(item.label) },
                selected = currentScreen == item.screen,
                onClick = { onScreenSelected(item.screen) },
                colors = NavigationBarItemDefaults.colors(
                    selectedIconColor = PrimaryColor,
                    selectedTextColor = PrimaryColor,
                    unselectedIconColor = Color.White.copy(alpha = 0.6f),
                    unselectedTextColor = Color.White.copy(alpha = 0.6f),
                    indicatorColor = PrimaryColor.copy(alpha = 0.1f)
                )
            )
        }
    }
}

private data class BottomNavItem(
    val screen: Screen,
    val icon: ImageVector,
    val label: String
)