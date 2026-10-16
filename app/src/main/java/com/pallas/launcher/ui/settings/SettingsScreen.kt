package com.pallas.launcher.ui.settings

import androidx.activity.BackEventCompat
import androidx.activity.compose.PredictiveBackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pallas.launcher.ui.settings.pages.AppearanceSettingsPage
import com.pallas.launcher.ui.settings.pages.ClockDateSettingsPage
import com.pallas.launcher.ui.settings.pages.GesturesSettingsPage
import com.pallas.launcher.ui.settings.pages.MainSettingsPage
import com.pallas.launcher.ui.settings.pages.ScreensAppsSettingsPage
import com.pallas.launcher.ui.settings.pages.SearchSettingsPage
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

enum class SettingsSubpage {
    MAIN,
    APPEARANCE,
    CLOCK_DATE,
    SCREENS_APPS,
    GESTURES,
    SEARCH,
    BACKUP
}

@Composable
fun SettingsScreen(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = viewModel()
) {
    val screens by viewModel.screens.collectAsState()
    val currentScreenIndex by viewModel.currentScreenIndex.collectAsState()

    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }

    val animProgress = remember { Animatable(0f) }
    var backSwipeEdge by remember { mutableIntStateOf(BackEventCompat.EDGE_LEFT) }

    // Intercept back gesture: subpage returns to MAIN; MAIN closes Settings
    PredictiveBackHandler { progress: Flow<BackEventCompat> ->
        try {
            progress.collect { backEvent ->
                backSwipeEdge = backEvent.swipeEdge
                animProgress.snapTo(backEvent.progress)
            }
            if (currentSubpage != SettingsSubpage.MAIN) {
                currentSubpage = SettingsSubpage.MAIN
            } else {
                onDismiss()
            }
            animProgress.snapTo(0f)
        } catch (e: CancellationException) {
            withContext(NonCancellable) {
                animProgress.animateTo(0f)
            }
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val spatialSpec = MaterialTheme.motionScheme.defaultSpatialSpec<IntOffset>()
    val effectsSpec = MaterialTheme.motionScheme.defaultEffectsSpec<Float>()

    val p = animProgress.value
    val scale = 1f - (p * 0.1f)
    val maxTranslationPx = with(LocalDensity.current) { 56.dp.toPx() }
    val translationX = if (backSwipeEdge == BackEventCompat.EDGE_RIGHT) -p * maxTranslationPx else p * maxTranslationPx

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .pointerInput(Unit) { detectTapGestures { } }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                this.translationX = translationX
            }
    ) {
        AnimatedContent(
            targetState = currentSubpage,
            transitionSpec = {
                if (targetState == SettingsSubpage.MAIN) {
                    (slideInHorizontally(animationSpec = spatialSpec) { -it / 2 } +
                            fadeIn(animationSpec = effectsSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = spatialSpec) { it } +
                                    fadeOut(animationSpec = effectsSpec))
                } else {
                    (slideInHorizontally(animationSpec = spatialSpec) { it } +
                            fadeIn(animationSpec = effectsSpec)) togetherWith
                            (slideOutHorizontally(animationSpec = spatialSpec) { -it / 2 } +
                                    fadeOut(animationSpec = effectsSpec))
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
                SettingsSubpage.BACKUP -> {
                    com.pallas.launcher.ui.settings.pages.BackupSettingsPage(
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
