package com.pallas.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.home.PallasHomeScreen
import com.pallas.launcher.ui.theme.PallasLauncherTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE

        setContent {
            val settings by viewModel.settings.collectAsState()
            
            val isDark = when (settings.themeMode) {
                com.pallas.launcher.domain.model.ThemeMode.DARK -> true
                com.pallas.launcher.domain.model.ThemeMode.LIGHT -> false
                com.pallas.launcher.domain.model.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            androidx.compose.runtime.LaunchedEffect(isDark) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                controller.isAppearanceLightStatusBars = !isDark
                controller.isAppearanceLightNavigationBars = !isDark
            }

            androidx.compose.runtime.LaunchedEffect(settings.hideStatusBar) {
                val controller = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (settings.hideStatusBar) {
                    controller.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    controller.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

            val onWindowBlurChange = androidx.compose.runtime.remember {
                { radiusPx: Int ->
                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                        try {
                            window.setBackgroundBlurRadius(radiusPx.coerceAtLeast(0))
                        } catch (_: Throwable) {
                            // Silently fall back to scrim if cross-window blur is unsupported
                        }
                    }
                }
            }

            PallasLauncherTheme(
                themeMode = settings.themeMode,
                onWindowBlurChange = onWindowBlurChange
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Transparent
                ) {
                    PallasHomeScreen(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        // The system delivers HOME intents for many reasons. Only act on a genuine "go home" request, and only reset
        // what makes sense: see MainViewModel.onHomeIntent. Never wipe the screen the user was on.
        val isHome = intent.action == Intent.ACTION_MAIN && intent.hasCategory(Intent.CATEGORY_HOME)
        if (isHome) {
            // onNewIntent runs BEFORE onResume when we come back from another app, so "RESUMED" here means the launcher
            // was already on screen (user pressed Home while looking at it).
            val wasVisible = lifecycle.currentState.isAtLeast(androidx.lifecycle.Lifecycle.State.RESUMED)
            viewModel.onHomeIntent(wasVisible)
        }
    }
}
