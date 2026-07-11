package com.arena0077.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.arena0077.app.ui.navigation.Routes
import com.arena0077.app.ui.screens.chat.ChatScreen
import com.arena0077.app.ui.screens.chat.ChatViewModel
import com.arena0077.app.ui.screens.leaderboard.LeaderboardScreen
import com.arena0077.app.ui.screens.login.LoginScreen
import com.arena0077.app.ui.screens.settings.SettingsScreen
import com.arena0077.app.ui.screens.sidebar.SidebarScreen
import com.arena0077.app.ui.theme.ArenaTheme
import dagger.hilt.android.AndroidEntryPoint

/**
 * MainActivity - the single Activity host for the Arena0077 app.
 *
 * Uses a Scaffold with a drawer for the sidebar (matching arena.ai's layout)
 * and a NavHost for the main content area.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            ArenaTheme {
                AppRoot()
            }
        }
    }
}

@Composable
private fun AppRoot(
    viewModel: ChatViewModel = hiltViewModel()
) {
    val navController = rememberNavController()
    var sidebarOpen by remember { mutableStateOf(false) }

    val isLoggedIn by viewModel.isLoggedIn.collectAsStateWithLifecycle()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Scaffold { padding ->
            Box(modifier = Modifier
                .fillMaxSize()
                .padding(padding)) {

                NavHost(
                    navController = navController,
                    startDestination = if (isLoggedIn) Routes.CHAT else Routes.LOGIN
                ) {
                    composable(Routes.LOGIN) {
                        LoginScreen(
                            onLoginSuccess = {
                                navController.navigate(Routes.CHAT) {
                                    popUpTo(Routes.LOGIN) { inclusive = true }
                                }
                            }
                        )
                    }

                    composable(Routes.CHAT) {
                        ChatScreen()
                    }

                    composable(Routes.LEADERBOARD) {
                        LeaderboardScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }

                    composable(Routes.SETTINGS) {
                        SettingsScreen(
                            onBack = { navController.popBackStack() }
                        )
                    }
                }

                // Sidebar overlay
                if (sidebarOpen) {
                    SidebarOverlay(
                        onClose = { sidebarOpen = false },
                        onLeaderboardClick = {
                            sidebarOpen = false
                            navController.navigate(Routes.LEADERBOARD)
                        },
                        onSettingsClick = {
                            sidebarOpen = false
                            navController.navigate(Routes.SETTINGS)
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun SidebarOverlay(
    onClose: () -> Unit,
    onLeaderboardClick: () -> Unit,
    onSettingsClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable(onClick = onClose)
    ) {
        // Scrim - the outer Box handles clicks to close
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.6f))
        )
        // Sidebar (left side)
        Box(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.surface)
        ) {
            SidebarScreen(
                onClose = onClose,
                onLeaderboardClick = onLeaderboardClick,
                onSettingsClick = onSettingsClick
            )
        }
    }
}
