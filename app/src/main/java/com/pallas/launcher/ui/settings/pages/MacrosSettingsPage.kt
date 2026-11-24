package com.pallas.launcher.ui.settings.pages

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.MacroAudioStream
import com.pallas.launcher.domain.model.MacroMediaAction
import com.pallas.launcher.domain.model.MacroRingerMode
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.MacroStep
import com.pallas.launcher.domain.model.MacroSystemSetting
import com.pallas.launcher.domain.model.MacroToggleMode
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow
import java.util.Locale
import java.util.UUID
import kotlin.math.roundToInt

private val WEEK_DAYS = listOf(
    1 to "S",
    2 to "M",
    3 to "T",
    4 to "W",
    5 to "T",
    6 to "F",
    7 to "S"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MacrosSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    onRunMacro: (MacroRoutine) -> Unit = { viewModel.runMacro(it) },
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val screens by viewModel.screens.collectAsState()
    val allApps by viewModel.allInstalledApps.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var editingMacro by remember { mutableStateOf<MacroRoutine?>(null) }
    var isNewMacro by remember { mutableStateOf(false) }

    val appLabelByKey = remember(allApps) { allApps.associate { it.uniqueKey to it.label } }
    val screenNameById = remember(screens) { screens.associate { it.id to it.name } }

    fun stepLabel(step: MacroStep): String = when (step) {
        is MacroStep.SetVolume -> "${step.stream.label} volume → ${step.levelPercent}%"
        is MacroStep.SetRingerMode -> "Ringer → ${step.mode.label}"
        is MacroStep.MediaControl -> "Media: ${step.action.label}"
        is MacroStep.SetDnd -> {
            val lvl = when (step.dndMode) {
                FocusDndMode.PRIORITY -> "Priority"
                FocusDndMode.ALARMS_ONLY -> "Alarms"
                FocusDndMode.TOTAL_SILENCE -> "Silence"
            }
            when (step.mode) {
                MacroToggleMode.ON -> "Do Not Disturb ON ($lvl)"
                MacroToggleMode.OFF -> "Do Not Disturb OFF"
                MacroToggleMode.TOGGLE -> "Toggle Do Not Disturb"
            }
        }
        is MacroStep.SetFlashlight -> "Flashlight: ${step.mode.name.lowercase().replaceFirstChar { it.uppercase() }}"
        is MacroStep.SetBrightness -> "Screen brightness → ${step.levelPercent}%"
        is MacroStep.SetAutoBrightness -> if (step.enabled) "Auto-brightness ON" else "Auto-brightness OFF"
        is MacroStep.SetAutoRotate -> if (step.enabled) "Auto-rotate ON" else "Auto-rotate OFF"
        is MacroStep.SetScreenTimeout -> "Screen timeout → ${step.seconds}s"
        is MacroStep.OpenQuickPanel -> "Quick Panel: ${step.panel.label}"
        is MacroStep.OpenSystemSetting -> "Open ${step.target.label} settings"
        is MacroStep.SetThemeMode -> "Theme → ${step.mode.name.lowercase().replaceFirstChar { it.uppercase() }}"
        is MacroStep.SetHideStatusBar -> if (step.hidden) "Hide status bar" else "Show status bar"
        is MacroStep.SetShowClock -> if (step.show) "Show clock" else "Hide clock"
        is MacroStep.SetShowDate -> if (step.show) "Show date" else "Hide date"
        is MacroStep.SetShowBattery -> if (step.show) "Show battery" else "Hide battery"
        is MacroStep.SetWallpaperScrim -> if (step.enabled) "Enable wallpaper scrim" else "Disable wallpaper scrim"
        is MacroStep.SetEdgeScrubPosition -> "Edge rail → ${step.position.name.lowercase().replaceFirstChar { it.uppercase() }}"
        is MacroStep.SetAutoLaunchSingleMatch -> if (step.enabled) "Auto-launch single match ON" else "Auto-launch single match OFF"
        is MacroStep.SwitchHomeScreen -> "Switch screen → ${screenNameById[step.screenId] ?: "Screen"}"
        is MacroStep.StartTimer -> "Timer: ${step.durationSeconds / 60}m"
        is MacroStep.SetAlarm -> String.format(Locale.US, "Alarm: %02d:%02d", step.hour, step.minute)
        is MacroStep.AppendQuickNote -> "Quick note: ${step.text}"
        is MacroStep.WaitDelay -> "Wait ${step.seconds}s"
        is MacroStep.LaunchApp -> "Open ${appLabelByKey[step.uniqueKey] ?: step.uniqueKey.substringBefore('/')}"
        is MacroStep.OpenNotebook -> "Open Notebook"
        is MacroStep.OpenLatexStudio -> "Open LaTeX Studio"
        is MacroStep.OpenAtlas -> "Open World Atlas"
        is MacroStep.OpenQuickCapture -> "Open Quick Capture"
        is MacroStep.OpenUrl -> "Open URL: ${step.url}"
    }

    editingMacro?.let { initial ->
        BackHandler { editingMacro = null }
        MacroEditorPage(
            initial = initial,
            isNew = isNewMacro,
            existingTriggers = settings.macros.map { it.triggerWord },
            screens = screens,
            allApps = allApps,
            stepLabel = ::stepLabel,
            onBack = { editingMacro = null },
            onSave = { saved ->
                viewModel.saveMacro(saved)
                editingMacro = null
            }
        )
        return
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            SettingsLargeTopBar(title = "Macros", scrollBehavior = scrollBehavior, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Trigger a chain of actions from search, gestures, or a schedule.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    FilledTonalButton(
                        onClick = {
                            isNewMacro = true
                            editingMacro = MacroRoutine(
                                id = UUID.randomUUID().toString(),
                                name = "",
                                triggerWord = "",
                                steps = emptyList()
                            )
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            if (settings.macros.isEmpty()) {
                item {
                    Text(
                        text = "No macros yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 16.dp)
                    )
                }
            } else {
                item {
                    SettingsGroup {
                        settings.macros.forEach { macro ->
                            val schedSuffix = if (macro.scheduleEnabled) {
                                String.format(Locale.US, " • %02d:%02d", macro.scheduleHour, macro.scheduleMinute)
                            } else ""
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isNewMacro = false
                                            editingMacro = macro
                                        }
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Bolt,
                                        contentDescription = null,
                                        tint = colorScheme.primary,
                                        modifier = Modifier.size(22.dp)
                                    )
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 12.dp)) {
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                                        ) {
                                            Text(
                                                text = macro.name,
                                                style = MaterialTheme.typography.bodyLarge,
                                                fontWeight = FontWeight.SemiBold,
                                                color = colorScheme.onSurface
                                            )
                                            Surface(
                                                shape = RoundedCornerShape(6.dp),
                                                color = colorScheme.primaryContainer
                                            ) {
                                                Text(
                                                    text = macro.triggerWord,
                                                    fontFamily = FontFamily.Monospace,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.onPrimaryContainer,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                        Text(
                                            text = "${macro.steps.size} action${if (macro.steps.size == 1) "" else "s"}$schedSuffix",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRunMacro(macro) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.PlayArrow, contentDescription = "Run", tint = colorScheme.primary)
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteMacro(macro.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Delete", tint = colorScheme.error)
                                    }
                                }
                            }
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(32.dp)) }
        }
    }
}

private enum class PendingMacroStepType {
    VOLUME, RINGER, MEDIA, DND, FLASHLIGHT, BRIGHTNESS, AUTO_BRIGHTNESS, AUTO_ROTATE,
    SCREEN_TIMEOUT, QUICK_PANEL, SYSTEM_SETTING,
    THEME, STATUS_BAR, CLOCK, DATE, BATTERY, SCRIM, EDGE_RAIL, AUTO_LAUNCH, SWITCH_SCREEN,
    TIMER, ALARM, QUICK_NOTE, WAIT, URL
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun MacroEditorPage(
    initial: MacroRoutine,
    isNew: Boolean,
    existingTriggers: List<String>,
    screens: List<HomeScreen>,
    allApps: List<AppItem>,
    stepLabel: (MacroStep) -> String,
    onBack: () -> Unit,
    onSave: (MacroRoutine) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var trigger by remember(initial.id) { mutableStateOf(initial.triggerWord) }
    var steps by remember(initial.id) { mutableStateOf(initial.steps) }
    var scheduleEnabled by remember(initial.id) { mutableStateOf(initial.scheduleEnabled) }
    var scheduleHour by remember(initial.id) { mutableIntStateOf(initial.scheduleHour) }
    var scheduleMinute by remember(initial.id) { mutableIntStateOf(initial.scheduleMinute) }
    var scheduleDays by remember(initial.id) { mutableStateOf(initial.scheduleDays) }

    var showScheduleTimePicker by remember { mutableStateOf(false) }
    var showAddStepPicker by remember { mutableStateOf(false) }
    var showAppPicker by remember { mutableStateOf(false) }
    var pendingStepType by remember { mutableStateOf<PendingMacroStepType?>(null) }

    val triggerError = remember(trigger, existingTriggers) {
        MacroRoutine.validateTrigger(
            raw = trigger,
            existingTriggers = existingTriggers,
            ignoreExistingTrigger = if (isNew) null else initial.triggerWord
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            SettingsLargeTopBar(
                title = if (isNew) "New Macro" else initial.name,
                scrollBehavior = scrollBehavior,
                onBack = onBack
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Identity")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = trigger,
                        onValueChange = { trigger = it },
                        label = { Text("Trigger word") },
                        singleLine = true,
                        isError = trigger.isNotEmpty() && triggerError != null,
                        supportingText = triggerError?.takeIf { trigger.isNotEmpty() }?.let { err -> { Text(err) } },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsSectionHeader("Schedule")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Run on schedule",
                        subtitle = "Triggers automatically at the chosen time",
                        checked = scheduleEnabled,
                        onCheckedChange = { scheduleEnabled = it }
                    )
                    if (scheduleEnabled) {
                        SettingsNavRow(
                            title = "Trigger time",
                            subtitle = String.format(Locale.US, "%02d:%02d", scheduleHour, scheduleMinute),
                            onClick = { showScheduleTimePicker = true }
                        )
                    }
                }
                if (scheduleEnabled) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        WEEK_DAYS.forEach { (dayNum, dayLabel) ->
                            val selected = dayNum in scheduleDays
                            FilterChip(
                                selected = selected,
                                onClick = {
                                    val next = if (selected) scheduleDays - dayNum else scheduleDays + dayNum
                                    if (next.isNotEmpty()) scheduleDays = next
                                },
                                label = { Text(dayLabel) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Actions",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(
                        onClick = { showAddStepPicker = true },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("Add Action")
                    }
                }

                if (steps.isEmpty()) {
                    Text(
                        text = "No actions added yet.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                } else {
                    SettingsGroup {
                        steps.forEachIndexed { idx, step ->
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${idx + 1}. ${stepLabel(step)}",
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                    IconButton(
                                        enabled = idx > 0,
                                        onClick = {
                                            val mutable = steps.toMutableList()
                                            val item = mutable.removeAt(idx)
                                            mutable.add(idx - 1, item)
                                            steps = mutable
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowUpward, contentDescription = "Move up", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        enabled = idx < steps.lastIndex,
                                        onClick = {
                                            val mutable = steps.toMutableList()
                                            val item = mutable.removeAt(idx)
                                            mutable.add(idx + 1, item)
                                            steps = mutable
                                        },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.ArrowDownward, contentDescription = "Move down", modifier = Modifier.size(16.dp))
                                    }
                                    IconButton(
                                        onClick = { steps = steps.filterIndexed { i, _ -> i != idx } },
                                        modifier = Modifier.size(30.dp)
                                    ) {
                                        Icon(Icons.Default.Delete, contentDescription = "Remove", tint = colorScheme.error, modifier = Modifier.size(16.dp))
                                    }
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(22.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onBack,
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        enabled = name.isNotBlank() && triggerError == null && steps.isNotEmpty(),
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name.trim(),
                                    triggerWord = trigger.trim(),
                                    steps = steps,
                                    scheduleEnabled = scheduleEnabled,
                                    scheduleHour = scheduleHour,
                                    scheduleMinute = scheduleMinute,
                                    scheduleDays = scheduleDays
                                )
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Macro", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    if (showScheduleTimePicker) {
        TimeStepPickerDialog(
            title = "Macro Schedule Time",
            initialHour = scheduleHour,
            initialMinute = scheduleMinute,
            onDismiss = { showScheduleTimePicker = false },
            onSave = { h, m ->
                scheduleHour = h
                scheduleMinute = m
                showScheduleTimePicker = false
            }
        )
    }

    if (showAddStepPicker) {
        val categories = listOf(
            "Audio & Media" to listOf(
                "Set Volume (Media, Call, Ring, Alarm…)" to { pendingStepType = PendingMacroStepType.VOLUME },
                "Ringer Mode (Normal / Vibrate / Silent)" to { pendingStepType = PendingMacroStepType.RINGER },
                "Media Control (Play/Pause, Next, Prev)" to { pendingStepType = PendingMacroStepType.MEDIA }
            ),
            "System & Hardware" to listOf(
                "Do Not Disturb" to { pendingStepType = PendingMacroStepType.DND },
                "Flashlight" to { pendingStepType = PendingMacroStepType.FLASHLIGHT },
                "Screen Brightness" to { pendingStepType = PendingMacroStepType.BRIGHTNESS },
                "Auto-Brightness" to { pendingStepType = PendingMacroStepType.AUTO_BRIGHTNESS },
                "Auto-Rotate" to { pendingStepType = PendingMacroStepType.AUTO_ROTATE },
                "Screen Timeout" to { pendingStepType = PendingMacroStepType.SCREEN_TIMEOUT },
                "Quick Panel (Internet, Wi-Fi, Bluetooth, Volume)" to { pendingStepType = PendingMacroStepType.QUICK_PANEL },
                "Open System Setting" to { pendingStepType = PendingMacroStepType.SYSTEM_SETTING }
            ),
            "Pallas Settings" to listOf(
                "Theme Mode" to { pendingStepType = PendingMacroStepType.THEME },
                "Status Bar Visibility" to { pendingStepType = PendingMacroStepType.STATUS_BAR },
                "Clock Visibility" to { pendingStepType = PendingMacroStepType.CLOCK },
                "Date Visibility" to { pendingStepType = PendingMacroStepType.DATE },
                "Battery Visibility" to { pendingStepType = PendingMacroStepType.BATTERY },
                "Wallpaper Scrim" to { pendingStepType = PendingMacroStepType.SCRIM },
                "Alphabet Edge Rail" to { pendingStepType = PendingMacroStepType.EDGE_RAIL },
                "Auto-Launch Single Match" to { pendingStepType = PendingMacroStepType.AUTO_LAUNCH },
                "Switch Home Screen" to { pendingStepType = PendingMacroStepType.SWITCH_SCREEN }
            ),
            "Productivity" to listOf(
                "Start Timer" to { pendingStepType = PendingMacroStepType.TIMER },
                "Set Alarm" to { pendingStepType = PendingMacroStepType.ALARM },
                "Append Quick Note" to { pendingStepType = PendingMacroStepType.QUICK_NOTE },
                "Wait Delay" to { pendingStepType = PendingMacroStepType.WAIT }
            ),
            "Apps & Tools" to listOf(
                "Launch App" to { showAppPicker = true },
                "Open Computational Notebook" to { steps = steps + MacroStep.OpenNotebook },
                "Open LaTeX Studio" to { steps = steps + MacroStep.OpenLatexStudio },
                "Open World Atlas" to { steps = steps + MacroStep.OpenAtlas },
                "Open Quick Capture Inbox" to { steps = steps + MacroStep.OpenQuickCapture },
                "Open URL" to { pendingStepType = PendingMacroStepType.URL }
            )
        )
        AlertDialog(
            onDismissRequest = { showAddStepPicker = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Add Action") },
            text = {
                Column(
                    modifier = Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    categories.forEach { (catTitle, items) ->
                        Text(
                            text = catTitle,
                            style = MaterialTheme.typography.labelLarge,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.SemiBold
                        )
                        items.forEach { (label, action) ->
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showAddStepPicker = false
                                        action()
                                    }
                                    .padding(vertical = 8.dp, horizontal = 4.dp)
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showAddStepPicker = false }) { Text("Cancel") }
            }
        )
    }

    if (showAppPicker) {
        AppPickerSheet(
            installedApps = allApps,
            onAppSelected = { app ->
                steps = steps + MacroStep.LaunchApp(app.uniqueKey)
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    pendingStepType?.let { type ->
        MacroStepConfigDialog(
            type = type,
            screens = screens,
            onDismiss = { pendingStepType = null },
            onAdd = { step ->
                steps = steps + step
                pendingStepType = null
            }
        )
    }
}

@Composable
private fun MacroStepConfigDialog(
    type: PendingMacroStepType,
    screens: List<HomeScreen>,
    onDismiss: () -> Unit,
    onAdd: (MacroStep) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    when (type) {
        PendingMacroStepType.VOLUME -> {
            var stream by remember { mutableStateOf(MacroAudioStream.MEDIA) }
            var pct by remember { mutableFloatStateOf(50f) }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Set Volume") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroAudioStream.entries.forEach { s ->
                            OutlinedButton(onClick = { stream = s }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (stream == s) "✓ ${s.label}" else s.label)
                            }
                        }
                        Text("Level: ${pct.roundToInt()}%", style = MaterialTheme.typography.labelLarge)
                        Slider(value = pct, onValueChange = { pct = it }, valueRange = 0f..100f)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onAdd(MacroStep.SetVolume(stream, pct.roundToInt())) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.RINGER -> SimpleOptionDialog(
            title = "Ringer Mode",
            options = MacroRingerMode.entries.map { it.label to MacroStep.SetRingerMode(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.MEDIA -> SimpleOptionDialog(
            title = "Media Control",
            options = MacroMediaAction.entries.map { it.label to MacroStep.MediaControl(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.DND -> {
            var mode by remember { mutableStateOf(MacroToggleMode.ON) }
            var level by remember { mutableStateOf(FocusDndMode.PRIORITY) }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Do Not Disturb") },
                text = {
                    Column(
                        modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        MacroToggleMode.entries.forEach { m ->
                            OutlinedButton(onClick = { mode = m }, modifier = Modifier.fillMaxWidth()) {
                                Text(if (mode == m) "✓ ${m.name}" else m.name)
                            }
                        }
                        if (mode == MacroToggleMode.ON) {
                            FocusDndMode.entries.forEach { d ->
                                OutlinedButton(onClick = { level = d }, modifier = Modifier.fillMaxWidth()) {
                                    Text(if (level == d) "✓ ${d.name}" else d.name)
                                }
                            }
                        }
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onAdd(MacroStep.SetDnd(mode, level)) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.FLASHLIGHT -> SimpleOptionDialog(
            title = "Flashlight",
            options = MacroToggleMode.entries.map { it.name to MacroStep.SetFlashlight(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.BRIGHTNESS -> {
            var pct by remember { mutableFloatStateOf(50f) }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Screen Brightness") },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text("Brightness: ${pct.roundToInt()}%", style = MaterialTheme.typography.titleMedium)
                        Slider(value = pct, onValueChange = { pct = it }, valueRange = 1f..100f)
                    }
                },
                confirmButton = {
                    TextButton(onClick = { onAdd(MacroStep.SetBrightness(pct.roundToInt())) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.AUTO_BRIGHTNESS -> SimpleOptionDialog(
            title = "Auto-Brightness",
            options = listOf(
                "Enable Auto-Brightness" to MacroStep.SetAutoBrightness(true),
                "Disable Auto-Brightness" to MacroStep.SetAutoBrightness(false)
            ),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.AUTO_ROTATE -> SimpleOptionDialog(
            title = "Auto-Rotate",
            options = listOf(
                "Enable Auto-Rotate" to MacroStep.SetAutoRotate(true),
                "Lock Portrait (Disable Auto-Rotate)" to MacroStep.SetAutoRotate(false)
            ),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.SCREEN_TIMEOUT -> SimpleOptionDialog(
            title = "Screen Timeout",
            options = listOf(15, 30, 60, 120, 300, 600).map { secs ->
                val label = if (secs < 60) "${secs} seconds" else "${secs / 60} min"
                label to MacroStep.SetScreenTimeout(secs)
            },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.QUICK_PANEL -> SimpleOptionDialog(
            title = "Quick Panel",
            options = com.pallas.launcher.domain.model.MacroQuickPanel.entries.map {
                it.label to MacroStep.OpenQuickPanel(it)
            },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.SYSTEM_SETTING -> SimpleOptionDialog(
            title = "Open System Setting",
            options = MacroSystemSetting.entries.map { it.label to MacroStep.OpenSystemSetting(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.THEME -> SimpleOptionDialog(
            title = "Theme Mode",
            options = ThemeMode.entries.map { it.name to MacroStep.SetThemeMode(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.STATUS_BAR -> SimpleOptionDialog(
            title = "Status Bar",
            options = listOf("Hide Status Bar" to MacroStep.SetHideStatusBar(true), "Show Status Bar" to MacroStep.SetHideStatusBar(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.CLOCK -> SimpleOptionDialog(
            title = "Clock Visibility",
            options = listOf("Show Clock" to MacroStep.SetShowClock(true), "Hide Clock" to MacroStep.SetShowClock(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.DATE -> SimpleOptionDialog(
            title = "Date Visibility",
            options = listOf("Show Date" to MacroStep.SetShowDate(true), "Hide Date" to MacroStep.SetShowDate(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.BATTERY -> SimpleOptionDialog(
            title = "Battery Visibility",
            options = listOf("Show Battery" to MacroStep.SetShowBattery(true), "Hide Battery" to MacroStep.SetShowBattery(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.SCRIM -> SimpleOptionDialog(
            title = "Wallpaper Scrim",
            options = listOf("Enable Scrim" to MacroStep.SetWallpaperScrim(true), "Disable Scrim" to MacroStep.SetWallpaperScrim(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.EDGE_RAIL -> SimpleOptionDialog(
            title = "Alphabet Edge Rail",
            options = EdgeScrubPosition.entries.map { it.name to MacroStep.SetEdgeScrubPosition(it) },
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.AUTO_LAUNCH -> SimpleOptionDialog(
            title = "Auto-Launch Single Match",
            options = listOf("Enable" to MacroStep.SetAutoLaunchSingleMatch(true), "Disable" to MacroStep.SetAutoLaunchSingleMatch(false)),
            onDismiss = onDismiss,
            onSelect = onAdd
        )
        PendingMacroStepType.SWITCH_SCREEN -> {
            if (screens.isEmpty()) {
                AlertDialog(
                    onDismissRequest = onDismiss,
                    shape = RoundedCornerShape(24.dp),
                    containerColor = colorScheme.surfaceContainerHigh,
                    title = { Text("Switch Home Screen") },
                    text = { Text("No home screens available.") },
                    confirmButton = { TextButton(onClick = onDismiss) { Text("Close") } }
                )
            } else {
                SimpleOptionDialog(
                    title = "Switch Home Screen",
                    options = screens.map { it.name to MacroStep.SwitchHomeScreen(it.id) },
                    onDismiss = onDismiss,
                    onSelect = onAdd
                )
            }
        }
        PendingMacroStepType.TIMER -> {
            var minsText by remember { mutableStateOf("25") }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Start Timer") },
                text = {
                    OutlinedTextField(
                        value = minsText,
                        onValueChange = { minsText = it.filter(Char::isDigit).take(4) },
                        label = { Text("Minutes (1–1440)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    val mins = (minsText.toIntOrNull() ?: 0).coerceIn(0, 1440)
                    TextButton(enabled = mins > 0, onClick = { onAdd(MacroStep.StartTimer(mins * 60)) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.ALARM -> TimeStepPickerDialog(
            title = "Set Alarm",
            initialHour = 7,
            initialMinute = 0,
            onDismiss = onDismiss,
            onSave = { h, m -> onAdd(MacroStep.SetAlarm(h, m)) }
        )
        PendingMacroStepType.QUICK_NOTE -> {
            var noteText by remember { mutableStateOf("") }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Append Quick Note") },
                text = {
                    OutlinedTextField(
                        value = noteText,
                        onValueChange = { noteText = it },
                        label = { Text("Note text") },
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    TextButton(enabled = noteText.isNotBlank(), onClick = { onAdd(MacroStep.AppendQuickNote(noteText.trim())) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.WAIT -> {
            var secsText by remember { mutableStateOf("2") }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Wait Delay") },
                text = {
                    OutlinedTextField(
                        value = secsText,
                        onValueChange = { secsText = it.filter(Char::isDigit) },
                        label = { Text("Seconds (1–60)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    val secs = (secsText.toIntOrNull() ?: 0).coerceIn(1, 60)
                    TextButton(enabled = (secsText.toIntOrNull() ?: 0) > 0, onClick = { onAdd(MacroStep.WaitDelay(secs)) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
        PendingMacroStepType.URL -> {
            var urlText by remember { mutableStateOf("https://") }
            AlertDialog(
                onDismissRequest = onDismiss,
                shape = RoundedCornerShape(24.dp),
                containerColor = colorScheme.surfaceContainerHigh,
                title = { Text("Open URL") },
                text = {
                    OutlinedTextField(
                        value = urlText,
                        onValueChange = { urlText = it },
                        label = { Text("URL") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                },
                confirmButton = {
                    val urlClean = urlText.trim()
                    val urlValid = urlClean.isNotBlank() &&
                        !urlClean.equals("http://", ignoreCase = true) &&
                        !urlClean.equals("https://", ignoreCase = true) &&
                        urlClean.removePrefix("https://").removePrefix("http://").isNotBlank()
                    TextButton(enabled = urlValid, onClick = { onAdd(MacroStep.OpenUrl(urlClean)) }) { Text("Add") }
                },
                dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
            )
        }
    }
}

@Composable
private fun SimpleOptionDialog(
    title: String,
    options: List<Pair<String, MacroStep>>,
    onDismiss: () -> Unit,
    onSelect: (MacroStep) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            Column(
                modifier = Modifier.heightIn(max = 360.dp).verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                options.forEach { (label, step) ->
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodyLarge,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelect(step) }
                            .padding(vertical = 10.dp, horizontal = 4.dp)
                    )
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Cancel") } }
    )
}
