package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.TextShadowMode
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSliderRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SettingsLargeTopBar(title = "Appearance", scrollBehavior = scrollBehavior, onBack = onBack) }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Theme")
                SettingsGroup {
                    SettingsSegmentedRow(
                        title = "Color mode",
                        options = ThemeMode.entries,
                        selected = settings.themeMode,
                        onSelected = { viewModel.updateSettings { copy(themeMode = it) } },
                        label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    )
                }

                SettingsSectionHeader("Home")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Wallpaper scrim",
                        subtitle = "Add a soft gradient behind the home screen",
                        checked = settings.showWallpaperScrim,
                        onCheckedChange = { viewModel.updateSettings { copy(showWallpaperScrim = it) } }
                    )
                    SettingsSliderRow(
                        title = "Scrim strength",
                        persistedValue = settings.wallpaperScrimStrength,
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(wallpaperScrimStrength = value) } },
                        valueRange = 0f..1f,
                        steps = 9,
                        enabled = settings.showWallpaperScrim
                    )
                    SettingsSegmentedRow(
                        title = "Text shadow",
                        options = TextShadowMode.entries,
                        selected = settings.textShadowMode,
                        onSelected = { viewModel.updateSettings { copy(textShadowMode = it) } },
                        label = { when (it) { TextShadowMode.OFF -> "Off"; TextShadowMode.AUTO -> "Auto"; TextShadowMode.ON -> "On" } }
                    )
                    SettingsSwitchRow(
                        title = "Hide status bar",
                        subtitle = "Cleaner home screen",
                        checked = settings.hideStatusBar,
                        onCheckedChange = { viewModel.updateSettings { copy(hideStatusBar = it) } }
                    )
                }

                Text(
                    text = "Text shadow follows wallpaper contrast when set to Auto. Scrim strength is available while the wallpaper scrim is on.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                )

                SettingsSectionHeader("Typography")
                SettingsGroup {
                    SettingsSliderRow(
                        title = "Clock & Date size",
                        persistedValue = settings.appFontScale,
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(appFontScale = value) } },
                        valueRange = 0.8f..1.3f,
                        steps = 4
                    )
                    SettingsSliderRow(
                        title = "Labels in Screens",
                        persistedValue = settings.screensFontScale,
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(screensFontScale = value) } },
                        valueRange = 0.7f..1.5f,
                        steps = 7
                    )
                    SettingsSliderRow(
                        title = "Labels in App List",
                        persistedValue = settings.appListFontScale,
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(appListFontScale = value) } },
                        valueRange = 0.8f..1.3f,
                        steps = 4
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
