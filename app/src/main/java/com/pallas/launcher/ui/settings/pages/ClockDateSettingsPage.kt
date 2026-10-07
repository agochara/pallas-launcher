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
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSliderRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow
import com.pallas.launcher.ui.settings.components.SettingsTopBar

@Composable
fun ClockDateSettingsPage(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val settings by viewModel.settings.collectAsState()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsTopBar(title = "Clock & Date", onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Clock Placement")
                SettingsSegmentedRow(
                    title = "Vertical Position",
                    options = ClockVerticalPosition.entries,
                    selected = settings.clockVerticalPosition,
                    onSelected = { viewModel.updateClockPosition(it, settings.clockHorizontalAlignment) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSegmentedRow(
                    title = "Horizontal Alignment",
                    options = ClockHorizontalAlignment.entries,
                    selected = settings.clockHorizontalAlignment,
                    onSelected = { viewModel.updateClockPosition(settings.clockVerticalPosition, it) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Clock Spacing")
                SettingsSliderRow(
                    title = "Left Padding",
                    persistedValue = settings.clockPaddingLeftDp.toFloat(),
                    onValueChangeFinished = { viewModel.updateClockPaddingLeft(it.toInt()) },
                    valueRange = 0f..64f,
                    steps = 15,
                    valueFormat = { "${it.toInt()} dp" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSliderRow(
                    title = "Right Padding",
                    persistedValue = settings.clockPaddingRightDp.toFloat(),
                    onValueChangeFinished = { viewModel.updateClockPaddingRight(it.toInt()) },
                    valueRange = 0f..64f,
                    steps = 15,
                    valueFormat = { "${it.toInt()} dp" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSliderRow(
                    title = "Top Padding",
                    persistedValue = settings.clockPaddingTopDp.toFloat(),
                    onValueChangeFinished = { viewModel.updateClockPaddingTop(it.toInt()) },
                    valueRange = 0f..64f,
                    steps = 15,
                    valueFormat = { "${it.toInt()} dp" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSliderRow(
                    title = "Bottom Padding",
                    persistedValue = settings.clockPaddingBottomDp.toFloat(),
                    onValueChangeFinished = { viewModel.updateClockPaddingBottom(it.toInt()) },
                    valueRange = 0f..64f,
                    steps = 15,
                    valueFormat = { "${it.toInt()} dp" }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Format & Metadata")
                SettingsSwitchRow(
                    title = "24-Hour Time",
                    subtitle = "Display time in 24-hour military format instead of AM/PM.",
                    checked = settings.is24HourClock,
                    onCheckedChange = { viewModel.update24HourClock(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSwitchRow(
                    title = "Show Date",
                    subtitle = "Show current date below the clock time.",
                    checked = settings.showDate,
                    onCheckedChange = { viewModel.updateShowDate(it) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSwitchRow(
                    title = "Show Battery Level",
                    subtitle = "Show current device battery percentage beside date.",
                    checked = settings.showBattery,
                    onCheckedChange = { viewModel.updateShowBattery(it) }
                )
            }
        }
    }
}
