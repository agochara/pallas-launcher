package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSliderRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow
import com.pallas.launcher.ui.settings.components.SettingsTopBar

@Composable
fun AppearanceSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsTopBar(title = "Appearance", onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Theme")
                SettingsSegmentedRow(
                    title = "Color Mode",
                    options = ThemeMode.entries,
                    selected = settings.themeMode,
                    onSelected = { viewModel.updateThemeMode(it) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Home Display")
                SettingsSwitchRow(
                    title = "Wallpaper Scrim",
                    subtitle = "Adds a subtle contrast gradient to ensure icons and text stay readable over bright wallpapers.",
                    checked = settings.showWallpaperScrim,
                    onCheckedChange = { viewModel.updateWallpaperScrim(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSwitchRow(
                    title = "Hide Status Bar",
                    subtitle = "Hides Android battery, clock, and notification icons for an ultra-clean home screen.",
                    checked = settings.hideStatusBar,
                    onCheckedChange = { viewModel.updateHideStatusBar(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Typography")
                SettingsSliderRow(
                    title = "App Label Text Scale",
                    persistedValue = settings.appFontScale,
                    onValueChangeFinished = { viewModel.updateFontScale(it) },
                    valueRange = 0.8f..1.3f,
                    steps = 4,
                    valueFormat = { "${(it * 100).toInt()}%" }
                )
            }
        }
    }
}
