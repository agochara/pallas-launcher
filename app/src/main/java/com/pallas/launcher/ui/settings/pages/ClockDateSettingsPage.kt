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
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSliderRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ClockDateSettingsPage(
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
        topBar = {
            SettingsLargeTopBar(title = "Clock & Date", scrollBehavior = scrollBehavior, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Clock Placement")
                SettingsGroup {
                    SettingsSegmentedRow(
                        title = "Vertical Position",
                        options = ClockVerticalPosition.entries,
                        selected = settings.clockVerticalPosition,
                        onSelected = { viewModel.updateSettings { copy(clockVerticalPosition = it) } },
                        label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )
                    SettingsSegmentedRow(
                        title = "Horizontal Alignment",
                        options = ClockHorizontalAlignment.entries,
                        selected = settings.clockHorizontalAlignment,
                        onSelected = { viewModel.updateSettings { copy(clockHorizontalAlignment = it) } },
                        label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )
                }

                SettingsSectionHeader("Clock Spacing")
                SettingsGroup {
                    SettingsSliderRow(
                        title = "Left Padding",
                        persistedValue = settings.clockPaddingLeftDp.toFloat(),
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(clockPaddingLeftDp = value.roundToInt()) } },
                        valueRange = 0f..160f,
                        steps = 0,
                        valueFormat = { "${it.roundToInt()} dp" },
                        stepIncrement = 1f
                    )
                    SettingsSliderRow(
                        title = "Right Padding",
                        persistedValue = settings.clockPaddingRightDp.toFloat(),
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(clockPaddingRightDp = value.roundToInt()) } },
                        valueRange = 0f..160f,
                        steps = 0,
                        valueFormat = { "${it.roundToInt()} dp" },
                        stepIncrement = 1f
                    )
                    SettingsSliderRow(
                        title = "Top Padding",
                        persistedValue = settings.clockPaddingTopDp.toFloat(),
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(clockPaddingTopDp = value.roundToInt()) } },
                        valueRange = 0f..160f,
                        steps = 0,
                        valueFormat = { "${it.roundToInt()} dp" },
                        stepIncrement = 1f
                    )
                    SettingsSliderRow(
                        title = "Bottom Padding",
                        persistedValue = settings.clockPaddingBottomDp.toFloat(),
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(clockPaddingBottomDp = value.roundToInt()) } },
                        valueRange = 0f..160f,
                        steps = 0,
                        valueFormat = { "${it.roundToInt()} dp" },
                        stepIncrement = 1f
                    )
                    SettingsSliderRow(
                        title = "Clock–Date Spacing",
                        persistedValue = settings.clockDateSpacingDp.toFloat(),
                        onValueChangeFinished = { value -> viewModel.updateSettings { copy(clockDateSpacingDp = value.roundToInt()) } },
                        valueRange = 0f..48f,
                        steps = 0,
                        valueFormat = { "${it.roundToInt()} dp" },
                        stepIncrement = 1f
                    )
                }

                SettingsSectionHeader("Format & Metadata")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Show Clock",
                        subtitle = "Display the digital clock on the home screen",
                        checked = settings.showClock,
                        onCheckedChange = { viewModel.updateSettings { copy(showClock = it) } }
                    )
                    SettingsSwitchRow(
                        title = "24-Hour Time",
                        subtitle = "Use 24-hour clock format (e.g. 14:30)",
                        checked = settings.is24HourClock,
                        onCheckedChange = { viewModel.updateSettings { copy(is24HourClock = it) } }
                    )
                    SettingsSwitchRow(
                        title = "Show Date",
                        subtitle = "Show current date below the clock time",
                        checked = settings.showDate,
                        onCheckedChange = { viewModel.updateSettings { copy(showDate = it) } }
                    )
                    SettingsSwitchRow(
                        title = "Show Battery Level",
                        subtitle = "Show battery percentage beside the date",
                        checked = settings.showBattery,
                        onCheckedChange = { viewModel.updateSettings { copy(showBattery = it) } }
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
