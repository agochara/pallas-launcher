package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSliderRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow

/**
 * Reference migration for the other pages (Clock, Screens & Apps, Gestures, Search):
 *  1. Scaffold + nestedScroll(scrollBehavior) + SettingsLargeTopBar instead of SettingsTopBar
 *  2. each section = SettingsSectionHeader + SettingsGroup { rows }, no HorizontalDivider
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppearanceSettingsPage(
    viewModel: MainViewModel,
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
                        onSelected = { viewModel.updateThemeMode(it) },
                        label = { it.name.lowercase().replaceFirstChar { c -> c.uppercase() } }
                    )
                }

                SettingsSectionHeader("Home")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Wallpaper scrim",
                        subtitle = "Soft gradient so text stays readable on bright wallpapers",
                        checked = settings.showWallpaperScrim,
                        onCheckedChange = { viewModel.updateWallpaperScrim(it) }
                    )
                    SettingsSwitchRow(
                        title = "Hide status bar",
                        subtitle = "Cleaner home screen",
                        checked = settings.hideStatusBar,
                        onCheckedChange = { viewModel.updateHideStatusBar(it) }
                    )
                }

                SettingsSectionHeader("Typography")
                SettingsGroup {
                    SettingsSliderRow(
                        title = "Labels in Screens",
                        persistedValue = settings.screensFontScale,
                        onValueChangeFinished = { viewModel.updateScreensFontScale(it) },
                        valueRange = 0.8f..1.3f,
                        steps = 4
                    )
                    SettingsSliderRow(
                        title = "Labels in App List",
                        persistedValue = settings.appListFontScale,
                        onValueChangeFinished = { viewModel.updateAppListFontScale(it) },
                        valueRange = 0.8f..1.3f,
                        steps = 4
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}