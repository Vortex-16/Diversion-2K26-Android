package com.example.chaintorquenative

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.chaintorquenative.mobile.ui.viewmodels.WalletViewModel
import com.example.chaintorquenative.ui.components.BottomNavigationBar
import com.example.chaintorquenative.ui.screens.MarketplaceScreen
import com.example.chaintorquenative.ui.screens.ModelViewerScreen
import com.example.chaintorquenative.ui.screens.ProfileScreen
import com.example.chaintorquenative.ui.screens.WalletScreen
import com.example.chaintorquenative.ui.screens.SettingsScreen
import com.example.chaintorquenative.ui.screens.AnimatedSplashScreen
import com.example.chaintorquenative.ui.theme.ChainTorqueTheme
import com.example.chaintorquenative.wallet.MetaMaskManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "MainActivity"

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var metaMaskManager: MetaMaskManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        // Initialize MetaMask SDK
        metaMaskManager.initialize(this)

        setContent {
            ChainTorqueTheme {
                ChainTorqueAppWithSplash()
            }
        }
    }
}

// Screen enum for navigation
enum class Screen {
    MARKETPLACE, PROFILE, WALLET, SETTINGS
}

@Composable
fun ChainTorqueAppWithSplash() {
    var showSplash by remember { mutableStateOf(true) }

    if (showSplash) {
        AnimatedSplashScreen(
            onSplashComplete = {
                showSplash = false
            }
        )
    } else {
        ChainTorqueApp()
    }
}

@Composable
fun ChainTorqueApp() {
    val navController = rememberNavController()
    var currentScreen by remember { mutableStateOf(Screen.MARKETPLACE) }
    
    // Create a SHARED WalletViewModel at the app level
    // This ensures all screens see the same wallet state!
    val sharedWalletViewModel: WalletViewModel = hiltViewModel()

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            BottomNavigationBar(
                currentScreen = currentScreen,
                onScreenSelected = { screen ->
                    currentScreen = screen
                    val route = when (screen) {
                        Screen.MARKETPLACE -> "marketplace"
                        Screen.PROFILE -> "profile"
                        Screen.WALLET -> "wallet"
                        Screen.SETTINGS -> "settings"
                    }
                    navController.navigate(route) {
                        popUpTo(navController.graph.startDestinationId) {
                            saveState = true
                        }
                        launchSingleTop = true
                        restoreState = true
                    }
                }
            )
        }
    ) { paddingValues ->
        NavHost(
            navController = navController,
            startDestination = "marketplace",
            modifier = Modifier.padding(paddingValues)
        ) {
            composable("marketplace") {
                MarketplaceScreen(
                    onNavigateToWallet = {
                        currentScreen = Screen.WALLET
                        navController.navigate("wallet")
                    },
                    walletViewModel = sharedWalletViewModel,
                    onNavigateToModelViewer = { modelUrl, title ->
                        navController.navigate(
                            "model_viewer/${Uri.encode(modelUrl)}?title=${Uri.encode(title)}"
                        )
                    }
                )
            }
            composable("profile") {
                ProfileScreen(
                    onNavigateToWallet = {
                        currentScreen = Screen.WALLET
                        navController.navigate("wallet")
                    },
                    walletViewModel = sharedWalletViewModel,
                    onNavigateToModelViewer = { modelUrl, title ->
                        navController.navigate(
                            "model_viewer/${Uri.encode(modelUrl)}?title=${Uri.encode(title)}"
                        )
                    }
                )
            }
            composable("wallet") {
                WalletScreen(viewModel = sharedWalletViewModel)
            }
            composable("settings") {
                SettingsScreen()
            }
            composable("model_viewer/{modelUrl}?title={title}") { backStackEntry ->
                val modelUrl = Uri.decode(backStackEntry.arguments?.getString("modelUrl") ?: "")
                val title = Uri.decode(backStackEntry.arguments?.getString("title") ?: "3D Model")
                ModelViewerScreen(
                    modelUrl = modelUrl,
                    title = title,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}