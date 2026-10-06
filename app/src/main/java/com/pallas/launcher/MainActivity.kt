package com.pallas.launcher

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.addCallback
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

        onBackPressedDispatcher.addCallback(this) {
            if (viewModel.isAppDrawerOpen.value) {
                viewModel.setAppDrawerOpen(false)
            } else if (viewModel.isSearchActive.value) {
                viewModel.setSearchActive(false)
            } else if (viewModel.isSettingsOpen.value) {
                viewModel.closeSettings()
            } else if (viewModel.isScreenEditorOpen.value) {
                viewModel.closeScreenEditor()
            } else if (viewModel.isAppPickerOpen.value) {
                viewModel.closeAppPicker()
            } else if (viewModel.appToCustomize.value != null) {
                viewModel.setAppToCustomize(null)
            } else {
                // If on secondary screen, go to home screen (screen 0)
                if (viewModel.currentScreenIndex.value != 0) {
                    viewModel.setScreenIndex(0)
                }
            }
        }

        setContent {
            val settings by viewModel.settings.collectAsState()
            
            val isDark = when (settings.themeMode) {
                com.pallas.launcher.domain.model.ThemeMode.DARK -> true
                com.pallas.launcher.domain.model.ThemeMode.LIGHT -> false
                com.pallas.launcher.domain.model.ThemeMode.SYSTEM -> androidx.compose.foundation.isSystemInDarkTheme()
            }
            
            androidx.compose.runtime.LaunchedEffect(isDark) {
                val style = if (isDark) androidx.activity.SystemBarStyle.dark(android.graphics.Color.TRANSPARENT) else androidx.activity.SystemBarStyle.light(android.graphics.Color.TRANSPARENT, android.graphics.Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }

            androidx.compose.runtime.LaunchedEffect(settings.hideStatusBar) {
                val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
                if (settings.hideStatusBar) {
                    insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                } else {
                    insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
                }
            }

            PallasLauncherTheme(themeMode = settings.themeMode) {
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
        // Home button pressed
        viewModel.resetToHomeScreen()
    }

    override fun onResume() {
        super.onResume()
        // No need to reloadApps, the callback covers this
        
        val settings = viewModel.settings.value
        val insetsController = androidx.core.view.WindowCompat.getInsetsController(window, window.decorView)
        insetsController.systemBarsBehavior = androidx.core.view.WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        if (settings.hideStatusBar) {
            insetsController.hide(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        } else {
            insetsController.show(androidx.core.view.WindowInsetsCompat.Type.statusBars())
        }
    }
}