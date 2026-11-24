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
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.FocusProfile
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsCompactTile
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow
import java.util.Locale
import java.util.UUID

private val FOCUS_DISABLEABLE_COMMAND_IDS = listOf(
    "math", "tex", "app_search", "web", "stock", "weather", "solar", "wiki", "wolfram", "def", "files", "calendar"
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FocusModeSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val focusState by viewModel.focusState.collectAsState()
    val screens by viewModel.screens.collectAsState()
    val allApps by viewModel.allInstalledApps.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var hasDndAccess by remember { mutableStateOf(SystemActionsManager.isDndAccessGranted(context)) }
    LifecycleResumeEffect(Unit) {
        hasDndAccess = SystemActionsManager.isDndAccessGranted(context)
        onPauseOrDispose { }
    }

    var editingProfile by remember { mutableStateOf<FocusProfile?>(null) }
    var isNewProfile by remember { mutableStateOf(false) }
    var showScheduleTimePickerFor by remember { mutableStateOf<String?>(null) }
    var showScheduleProfilePicker by remember { mutableStateOf(false) }

    val activeProfile = remember(focusState, settings.focusProfiles) {
        if (!focusState.isActive) null
        else settings.focusProfiles.firstOrNull { it.id == focusState.activeProfileId }
    }
    val defaultProfile = remember(settings.defaultFocusProfileId, settings.focusProfiles) {
        settings.focusProfiles.firstOrNull { it.id == settings.defaultFocusProfileId }
            ?: settings.focusProfiles.firstOrNull()
    }

    fun formatMinute(minuteOfDay: Int): String {
        val m = minuteOfDay.coerceIn(0, 1439)
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60)
    }

    editingProfile?.let { initial ->
        BackHandler { editingProfile = null }
        FocusProfileEditorPage(
            initial = initial,
            isNew = isNewProfile,
            existingNames = settings.focusProfiles.map { it.name },
            screens = screens,
            allApps = allApps,
            macros = settings.macros,
            onBack = { editingProfile = null },
            onSave = { saved, makeDefault ->
                viewModel.saveFocusProfile(saved, makeDefault)
                editingProfile = null
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
            SettingsLargeTopBar(title = "Focus Mode", scrollBehavior = scrollBehavior, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                val statusSubtitle = if (activeProfile != null) {
                    val remaining = if (focusState.endTimeMillis == -1L) {
                        if (focusState.isScheduled) "Scheduled" else "Until ended"
                    } else {
                        val mins = ((focusState.endTimeMillis - System.currentTimeMillis()) / 60_000L).coerceAtLeast(1L)
                        if (mins >= 60) "${mins / 60}h ${mins % 60}m left" else "${mins}m left"
                    }
                    "${activeProfile.name} • $remaining"
                } else {
                    "Inactive"
                }

                Surface(
                    color = if (activeProfile != null) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(
                            modifier = Modifier.weight(1f).padding(end = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = null,
                                tint = if (activeProfile != null) colorScheme.onPrimaryContainer else colorScheme.primary,
                                modifier = Modifier.size(26.dp)
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = if (activeProfile != null) "Active" else "Off",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (activeProfile != null) colorScheme.onPrimaryContainer else colorScheme.onSurface
                                )
                                Text(
                                    text = statusSubtitle,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (activeProfile != null) {
                                        colorScheme.onPrimaryContainer.copy(alpha = 0.85f)
                                    } else {
                                        colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }

                        if (activeProfile != null) {
                            Button(
                                onClick = { viewModel.deactivateFocus() },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.Stop, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(6.dp))
                                Text("End")
                            }
                        } else if (defaultProfile != null) {
                            FilledTonalButton(
                                onClick = { viewModel.activateFocus(defaultProfile) },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Icon(Icons.Default.PlayArrow, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(Modifier.width(4.dp))
                                Text("Start")
                            }
                        }
                    }
                }
            }

            if (!hasDndAccess && settings.focusProfiles.any { it.syncDnd || it.muteRinger }) {
                item {
                    Surface(
                        color = colorScheme.errorContainer.copy(alpha = 0.7f),
                        shape = RoundedCornerShape(20.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 18.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Do Not Disturb access is needed for DND sync and ringer mute",
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onErrorContainer,
                                modifier = Modifier.weight(1f).padding(end = 8.dp)
                            )
                            TextButton(onClick = { SystemActionsManager.openDndAccessSettings(context) }) {
                                Text("Grant", fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }
            }

            // Profiles Section
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Profiles",
                        style = MaterialTheme.typography.titleSmall,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    FilledTonalButton(
                        onClick = {
                            isNewProfile = true
                            editingProfile = FocusProfile(id = UUID.randomUUID().toString(), name = "")
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(Modifier.width(4.dp))
                        Text("New")
                    }
                }
            }

            if (settings.focusProfiles.isEmpty()) {
                item {
                    Text(
                        text = "No profiles configured yet. Create a profile to select which Home Screens and apps remain visible during Focus Mode.",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                }
            } else {
                item {
                    SettingsGroup {
                        settings.focusProfiles.forEach { profile ->
                            val isDefault = profile.id == defaultProfile?.id
                            val isRunning = activeProfile?.id == profile.id
                            val screenCount = profile.allowedScreenIds.size
                            val appCount = profile.allowedAppKeys.size
                            val summary = buildList {
                                add("$screenCount screen${if (screenCount == 1) "" else "s"}")
                                if (appCount > 0) add("+$appCount app${if (appCount == 1) "" else "s"}")
                                if (profile.syncDnd) add("DND")
                                if (profile.muteRinger) add("Mute")
                            }.joinToString(" • ")

                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            isNewProfile = false
                                            editingProfile = profile
                                        }
                                        .padding(horizontal = 14.dp, vertical = 12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    IconButton(
                                        onClick = { viewModel.setDefaultFocusProfile(profile.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isDefault) Icons.Default.Star else Icons.Default.StarBorder,
                                            contentDescription = "Set default",
                                            tint = if (isDefault) colorScheme.primary else colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Column(modifier = Modifier.weight(1f).padding(horizontal = 8.dp)) {
                                        Text(
                                            text = profile.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        Text(
                                            text = summary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            if (isRunning) viewModel.deactivateFocus()
                                            else viewModel.activateFocus(profile)
                                        },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isRunning) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isRunning) "End" else "Start",
                                            tint = colorScheme.primary
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.deleteFocusProfile(profile.id) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete",
                                            tint = colorScheme.error
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Schedule Section
            item {
                SettingsSectionHeader("Schedule")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Daily schedule",
                        subtitle = "Automatically activate during this window",
                        checked = settings.focusScheduleEnabled,
                        onCheckedChange = { enabled ->
                            viewModel.updateSettings { copy(focusScheduleEnabled = enabled) }
                            viewModel.focusModeManager.syncState(viewModel.settings.value)
                        }
                    )
                    if (settings.focusScheduleEnabled) {
                        val schedProfile = settings.focusProfiles.firstOrNull { it.id == settings.focusScheduleProfileId }
                            ?: defaultProfile
                        SettingsNavRow(
                            title = "Profile",
                            subtitle = schedProfile?.name ?: "None",
                            onClick = { if (settings.focusProfiles.isNotEmpty()) showScheduleProfilePicker = true }
                        )
                    }
                }

                if (settings.focusScheduleEnabled) {
                    Spacer(Modifier.height(10.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsCompactTile(
                            title = formatMinute(settings.focusScheduleStartMinute),
                            subtitle = "Start time",
                            icon = Icons.Default.CenterFocusStrong,
                            containerColor = colorScheme.surfaceContainerHigh,
                            onClick = { showScheduleTimePickerFor = "start" },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsCompactTile(
                            title = formatMinute(settings.focusScheduleEndMinute),
                            subtitle = "End time",
                            icon = Icons.Default.Stop,
                            containerColor = colorScheme.surfaceContainerHigh,
                            onClick = { showScheduleTimePickerFor = "end" },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }

                Text(
                    text = "Search commands: 'focus', 'focus 60m', 'focus <profile> 3h', 'focus <profile> + dnd'. End with 'give up' or 'focus off'.",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    showScheduleTimePickerFor?.let { which ->
        val initialMinute = if (which == "start") settings.focusScheduleStartMinute else settings.focusScheduleEndMinute
        TimeStepPickerDialog(
            title = if (which == "start") "Start Time" else "End Time",
            initialHour = initialMinute / 60,
            initialMinute = initialMinute % 60,
            onDismiss = { showScheduleTimePickerFor = null },
            onSave = { h, m ->
                val total = h * 60 + m
                viewModel.updateSettings {
                    if (which == "start") copy(focusScheduleStartMinute = total)
                    else copy(focusScheduleEndMinute = total)
                }
                viewModel.focusModeManager.syncState(viewModel.settings.value)
                showScheduleTimePickerFor = null
            }
        )
    }

    if (showScheduleProfilePicker) {
        AlertDialog(
            onDismissRequest = { showScheduleProfilePicker = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Scheduled Profile") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    settings.focusProfiles.forEach { p ->
                        val selected = p.id == (settings.focusScheduleProfileId ?: defaultProfile?.id)
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.updateSettings { copy(focusScheduleProfileId = p.id) }
                                    viewModel.focusModeManager.syncState(viewModel.settings.value)
                                    showScheduleProfilePicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = selected, onClick = null)
                            Text(p.name, modifier = Modifier.padding(start = 10.dp))
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showScheduleProfilePicker = false }) { Text("Close") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
private fun FocusProfileEditorPage(
    initial: FocusProfile,
    isNew: Boolean,
    existingNames: List<String>,
    screens: List<HomeScreen>,
    allApps: List<AppItem>,
    macros: List<com.pallas.launcher.domain.model.MacroRoutine>,
    onBack: () -> Unit,
    onSave: (FocusProfile, Boolean) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var name by remember(initial.id) { mutableStateOf(initial.name) }
    var selectedScreens by remember(initial.id) { mutableStateOf(initial.allowedScreenIds) }
    var selectedApps by remember(initial.id) { mutableStateOf(initial.allowedAppKeys) }
    var syncDnd by remember(initial.id) { mutableStateOf(initial.syncDnd) }
    var dndMode by remember(initial.id) { mutableStateOf(initial.dndMode) }
    var muteRinger by remember(initial.id) { mutableStateOf(initial.muteRinger) }
    var disabledCommands by remember(initial.id) { mutableStateOf(initial.disabledCommands) }
    var onEnterMacroId by remember(initial.id) { mutableStateOf(initial.onEnterMacroId) }
    var makeDefault by remember(initial.id) { mutableStateOf(isNew) }
    var showAppPicker by remember { mutableStateOf(false) }
    var showMacroPicker by remember { mutableStateOf(false) }

    val nameError = remember(name, existingNames) {
        FocusProfile.validateName(
            raw = name,
            existingNames = existingNames,
            ignoreExistingName = if (isNew) null else initial.name
        )
    }
    val appLabelByKey = remember(allApps) { allApps.associate { it.uniqueKey to it.label } }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = {
            SettingsLargeTopBar(
                title = if (isNew) "New Profile" else initial.name,
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
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    OutlinedTextField(
                        value = name,
                        onValueChange = { name = it },
                        label = { Text("Profile name") },
                        singleLine = true,
                        isError = name.isNotEmpty() && nameError != null,
                        supportingText = nameError?.takeIf { name.isNotEmpty() }?.let { err -> { Text(err) } },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }

                SettingsSectionHeader("Home Screens")
                SettingsGroup {
                    if (screens.isEmpty()) {
                        Surface(color = colorScheme.surfaceContainerHigh, shape = RoundedCornerShape(6.dp)) {
                            Text(
                                text = "No home screens configured",
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.fillMaxWidth().padding(20.dp)
                            )
                        }
                    } else {
                        screens.forEach { screen ->
                            val checked = screen.id in selectedScreens
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            selectedScreens = if (checked) selectedScreens - screen.id else selectedScreens + screen.id
                                        }
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Checkbox(
                                        checked = checked,
                                        onCheckedChange = { c ->
                                            selectedScreens = if (c) selectedScreens + screen.id else selectedScreens - screen.id
                                        }
                                    )
                                    Column(modifier = Modifier.padding(start = 8.dp)) {
                                        Text(
                                            text = screen.name,
                                            style = MaterialTheme.typography.bodyLarge,
                                            fontWeight = FontWeight.Medium
                                        )
                                        Text(
                                            text = "${screen.apps.size} app${if (screen.apps.size == 1) "" else "s"}",
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                SettingsSectionHeader("Additional Apps")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Keep alongside screens",
                        subtitle = if (selectedApps.isEmpty()) "None selected" else "${selectedApps.size} app${if (selectedApps.size == 1) "" else "s"} selected",
                        onClick = { showAppPicker = true }
                    )
                }
                if (selectedApps.isNotEmpty()) {
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        selectedApps.sorted().forEach { key ->
                            InputChip(
                                selected = false,
                                onClick = { selectedApps = selectedApps - key },
                                label = { Text(appLabelByKey[key] ?: key.substringBefore('/')) },
                                trailingIcon = {
                                    Icon(Icons.Default.Close, contentDescription = "Remove", modifier = Modifier.size(16.dp))
                                }
                            )
                        }
                    }
                }

                SettingsSectionHeader("Interruptions")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Sync Do Not Disturb",
                        subtitle = "Turn on Android Do Not Disturb while active",
                        checked = syncDnd,
                        onCheckedChange = { syncDnd = it }
                    )
                    if (syncDnd) {
                        SettingsSegmentedRow(
                            title = "DND Level",
                            options = FocusDndMode.entries,
                            selected = dndMode,
                            onSelected = { dndMode = it },
                            label = {
                                when (it) {
                                    FocusDndMode.PRIORITY -> "Priority"
                                    FocusDndMode.ALARMS_ONLY -> "Alarms"
                                    FocusDndMode.TOTAL_SILENCE -> "Silence"
                                }
                            }
                        )
                    }
                    SettingsSwitchRow(
                        title = "Mute ringer",
                        subtitle = "Set device ringer mode to silent while active",
                        checked = muteRinger,
                        onCheckedChange = { muteRinger = it }
                    )
                }
                if (syncDnd) {
                    val dndExplanation = when (dndMode) {
                        FocusDndMode.PRIORITY -> "Priority: allows alarms, repeat callers, and starred/priority notifications."
                        FocusDndMode.ALARMS_ONLY -> "Alarms: blocks all calls and notifications; only alarms can sound."
                        FocusDndMode.TOTAL_SILENCE -> "Silence: blocks all sounds and vibrations, including alarms and media."
                    }
                    Text(
                        text = dndExplanation,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                    )
                }

                SettingsSectionHeader("Disable Built-in Commands")
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier.padding(horizontal = 16.dp)
                ) {
                    FOCUS_DISABLEABLE_COMMAND_IDS.forEach { cmdId ->
                        val cfg = CommandConfig.DEFAULTS[cmdId] ?: return@forEach
                        val isDisabled = cmdId in disabledCommands
                        FilterChip(
                            selected = isDisabled,
                            onClick = {
                                disabledCommands = if (isDisabled) disabledCommands - cmdId else disabledCommands + cmdId
                            },
                            label = { Text(cfg.label) }
                        )
                    }
                }

                SettingsSectionHeader("Run Macro on Enter")
                val chosenMacro = macros.firstOrNull { it.id == onEnterMacroId }
                SettingsGroup {
                    SettingsNavRow(
                        title = chosenMacro?.name ?: "None",
                        subtitle = chosenMacro?.let { "Trigger '${it.triggerWord}' • ${it.steps.size} steps" }
                            ?: "Optionally run a macro when this profile activates",
                        onClick = { showMacroPicker = true }
                    )
                }

                if (isNew) {
                    Spacer(Modifier.height(10.dp))
                    SettingsGroup {
                        SettingsSwitchRow(
                            title = "Set as default profile",
                            checked = makeDefault,
                            onCheckedChange = { makeDefault = it }
                        )
                    }
                }

                Spacer(Modifier.height(20.dp))
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
                        enabled = nameError == null,
                        onClick = {
                            onSave(
                                initial.copy(
                                    name = name.trim(),
                                    allowedScreenIds = selectedScreens,
                                    allowedAppKeys = selectedApps,
                                    syncDnd = syncDnd,
                                    dndMode = dndMode,
                                    muteRinger = muteRinger,
                                    disabledCommands = disabledCommands,
                                    onEnterMacroId = onEnterMacroId
                                ),
                                makeDefault
                            )
                        },
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.weight(1.5f)
                    ) {
                        Text("Save Profile", fontWeight = FontWeight.SemiBold)
                    }
                }
                Spacer(Modifier.height(36.dp))
            }
        }
    }

    if (showAppPicker) {
        AppPickerSheet(
            installedApps = allApps,
            alreadyAddedKeys = selectedApps,
            title = "Keep Apps",
            onAppsConfirmed = { picked ->
                selectedApps = selectedApps + picked.map { it.uniqueKey }
                showAppPicker = false
            },
            onDismiss = { showAppPicker = false }
        )
    }

    if (showMacroPicker) {
        AlertDialog(
            onDismissRequest = { showMacroPicker = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Run Macro on Enter") },
            text = {
                Column(
                    modifier = Modifier
                        .heightIn(max = 360.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                onEnterMacroId = null
                                showMacroPicker = false
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(selected = onEnterMacroId == null, onClick = null)
                        Text("None", modifier = Modifier.padding(start = 10.dp))
                    }
                    macros.forEach { macro ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEnterMacroId = macro.id
                                    showMacroPicker = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(selected = onEnterMacroId == macro.id, onClick = null)
                            Column(modifier = Modifier.padding(start = 10.dp)) {
                                Text(macro.name, style = MaterialTheme.typography.bodyLarge)
                                Text(
                                    "'${macro.triggerWord}' • ${macro.steps.size} steps",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showMacroPicker = false }) { Text("Close") }
            }
        )
    }
}

@Composable
internal fun TimeStepPickerDialog(
    title: String,
    initialHour: Int,
    initialMinute: Int,
    onDismiss: () -> Unit,
    onSave: (Int, Int) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    var hour by remember(initialHour) { mutableIntStateOf(initialHour.coerceIn(0, 23)) }
    var minute by remember(initialMinute) { mutableIntStateOf(initialMinute.coerceIn(0, 59)) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = { Text(title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = String.format(Locale.US, "%02d:%02d", hour, minute),
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = colorScheme.primary
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Hour", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { hour = (hour + 23) % 24 }) { Text("-") }
                            OutlinedButton(onClick = { hour = (hour + 1) % 24 }) { Text("+") }
                        }
                    }
                    Column(modifier = Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("Minute", style = MaterialTheme.typography.labelMedium)
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            OutlinedButton(onClick = { minute = (minute + 55) % 60 }) { Text("-5") }
                            OutlinedButton(onClick = { minute = (minute + 5) % 60 }) { Text("+5") }
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(hour, minute) }) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}
