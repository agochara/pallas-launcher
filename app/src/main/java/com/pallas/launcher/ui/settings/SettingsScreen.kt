package com.pallas.launcher.ui.settings

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.settings.pages.AppearanceSettingsPage
import com.pallas.launcher.ui.settings.pages.ClockDateSettingsPage
import com.pallas.launcher.ui.settings.pages.GesturesSettingsPage
import com.pallas.launcher.ui.settings.pages.MainSettingsPage
import com.pallas.launcher.ui.settings.pages.ScreensAppsSettingsPage
import com.pallas.launcher.ui.settings.pages.SearchSettingsPage
import com.pallas.launcher.ui.theme.MaterialExpressiveSettingsTheme

enum class SettingsSubpage {
    MAIN,
    APPEARANCE,
    CLOCK_DATE,
    SCREENS_APPS,
    GESTURES,
    SEARCH
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val screens by viewModel.screens.collectAsState()
    val currentScreenIndex by viewModel.currentScreenIndex.collectAsState()

    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }

    // Intercept back key: subpage returns to MAIN; MAIN closes Settings
    BackHandler {
        if (currentSubpage != SettingsSubpage.MAIN) {
            currentSubpage = SettingsSubpage.MAIN
        } else {
            onDismiss()
        }
    }

    MaterialExpressiveSettingsTheme(themeMode = settings.themeMode) {
        val colorScheme = MaterialTheme.colorScheme

        Box(
            modifier = modifier
                .fillMaxSize()
                .background(colorScheme.surface)
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            AnimatedContent(
                targetState = currentSubpage,
                transitionSpec = {
                    val slideDuration = 250
                    val fadeDuration = 200
                    if (targetState == SettingsSubpage.MAIN) {
                        (slideInHorizontally(animationSpec = tween(slideDuration, easing = FastOutSlowInEasing)) { -it / 2 } +
                                fadeIn(animationSpec = tween(fadeDuration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(slideDuration, easing = FastOutSlowInEasing)) { it } +
                                        fadeOut(animationSpec = tween(fadeDuration)))
                    } else {
                        (slideInHorizontally(animationSpec = tween(slideDuration, easing = FastOutSlowInEasing)) { it } +
                                fadeIn(animationSpec = tween(fadeDuration))) togetherWith
                                (slideOutHorizontally(animationSpec = tween(slideDuration, easing = FastOutSlowInEasing)) { -it / 2 } +
                                        fadeOut(animationSpec = tween(fadeDuration)))
                    }
                },
                label = "settings_navigation"
            ) { subpage ->
                when (subpage) {
                    SettingsSubpage.MAIN -> {
                        MainSettingsPage(
                            onNavigate = { currentSubpage = it },
                            onDismiss = onDismiss
                        )
                    }
                    SettingsSubpage.APPEARANCE -> {
                        AppearanceSettingsPage(
                            viewModel = viewModel,
                            onBack = { currentSubpage = SettingsSubpage.MAIN }
                        )
                    }
                    SettingsSubpage.CLOCK_DATE -> {
                        ClockDateSettingsPage(
                            viewModel = viewModel,
                            onBack = { currentSubpage = SettingsSubpage.MAIN }
                        )
                    }
                    SettingsSubpage.SCREENS_APPS -> {
                        ScreensAppsSettingsPage(
                            viewModel = viewModel,
                            screens = screens,
                            currentScreenIndex = currentScreenIndex,
                            onBack = { currentSubpage = SettingsSubpage.MAIN }
                        )
                    }
                    SettingsSubpage.GESTURES -> {
                        GesturesSettingsPage(
                            viewModel = viewModel,
                            onBack = { currentSubpage = SettingsSubpage.MAIN }
                        )
                    }
                    SettingsSubpage.SEARCH -> {
                        SearchSettingsPage(
                            viewModel = viewModel,
                            onBack = { currentSubpage = SettingsSubpage.MAIN }
                        )
                    }
                }
            }
        }
    }
}
