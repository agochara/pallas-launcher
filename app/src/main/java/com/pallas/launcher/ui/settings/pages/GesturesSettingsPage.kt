package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GesturesSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()

    var pickerGestureTitle by remember { mutableStateOf<String?>(null) }
    var currentSelectedAction by remember { mutableStateOf<GestureAction>(GestureAction.None) }
    var onActionSelectedCallback by remember { mutableStateOf<(GestureAction) -> Unit>({}) }
    var isAppPickerOpenForGesture by remember { mutableStateOf<((GestureAction) -> Unit)?>(null) }

    fun getActionLabel(action: GestureAction): String {
        return when (action) {
            is GestureAction.OpenCamera -> "Open Camera"
            is GestureAction.OpenDialer -> "Open Dialer"
            is GestureAction.OpenSearch -> "Open Search"
            is GestureAction.OpenNotifications -> "Open Notifications"
            is GestureAction.OpenQuickSettings -> "Open Quick Settings"
            is GestureAction.OpenAppDrawer -> "Open App List"
            is GestureAction.LaunchApp -> {
                val app = installedApps.find { it.packageName == action.packageName }
                if (app != null) "Launch ${app.label}" else "Launch App (${action.packageName})"
            }
            is GestureAction.None -> "None"
        }
    }

    val standardGestureOptions = listOf(
        GestureAction.OpenAppDrawer to "Open App List",
        GestureAction.OpenSearch to "Open Search",
        GestureAction.OpenNotifications to "Open Notifications",
        GestureAction.OpenQuickSettings to "Open Quick Settings",
        GestureAction.OpenCamera to "Open Camera",
        GestureAction.OpenDialer to "Open Dialer",
        GestureAction.None to "None"
    )

    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsLargeTopBar(title = "Gestures", scrollBehavior = scrollBehavior, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Vertical Swipes")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Swipe Up",
                        subtitle = getActionLabel(settings.swipeUpAction),
                        onClick = {
                            pickerGestureTitle = "Swipe Up Action"
                            currentSelectedAction = settings.swipeUpAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateSettings { copy(swipeUpAction = action) }
                            }
                        }
                    )

                    SettingsNavRow(
                        title = "Swipe Down",
                        subtitle = getActionLabel(settings.swipeDownAction),
                        onClick = {
                            pickerGestureTitle = "Swipe Down Action"
                            currentSelectedAction = settings.swipeDownAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateSettings { copy(swipeDownAction = action) }
                            }
                        }
                    )
                }

                SettingsSectionHeader("Horizontal Swipes")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Short Swipe Left",
                        subtitle = getActionLabel(settings.shortSwipeLeftAction),
                        onClick = {
                            pickerGestureTitle = "Short Swipe Left Action"
                            currentSelectedAction = settings.shortSwipeLeftAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateSettings { copy(shortSwipeLeftAction = action) }
                            }
                        }
                    )

                    SettingsNavRow(
                        title = "Short Swipe Right",
                        subtitle = getActionLabel(settings.shortSwipeRightAction),
                        onClick = {
                            pickerGestureTitle = "Short Swipe Right Action"
                            currentSelectedAction = settings.shortSwipeRightAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateSettings { copy(shortSwipeRightAction = action) }
                            }
                        }
                    )
                }

                SettingsSectionHeader("Alphabet Edge Scrub")
                SettingsGroup {
                    SettingsSegmentedRow(
                        title = "Edge Rail Position",
                        options = EdgeScrubPosition.entries,
                        selected = settings.edgeScrubPosition,
                        onSelected = { viewModel.updateSettings { copy(edgeScrubPosition = it) } },
                        label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                    )

                    SettingsSegmentedRow(
                        title = "Action On Touch Release",
                        options = EdgeScrubReleaseAction.entries,
                        selected = settings.edgeScrubReleaseAction,
                        onSelected = { viewModel.updateSettings { copy(edgeScrubReleaseAction = it) } },
                        label = {
                            when (it) {
                                EdgeScrubReleaseAction.STAY_OPEN -> "Stay Open"
                                EdgeScrubReleaseAction.LAUNCH_TOP -> "Launch Top"
                            }
                        }
                    )
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    pickerGestureTitle?.let { title ->
        val isCustomAppSelected = currentSelectedAction is GestureAction.LaunchApp
        AlertDialog(
            onDismissRequest = { pickerGestureTitle = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = title, style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    standardGestureOptions.forEach { (action, label) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onActionSelectedCallback(action)
                                    pickerGestureTitle = null
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            RadioButton(
                                selected = currentSelectedAction == action,
                                onClick = {
                                    onActionSelectedCallback(action)
                                    pickerGestureTitle = null
                                }
                            )
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                val callback = onActionSelectedCallback
                                pickerGestureTitle = null
                                isAppPickerOpenForGesture = callback
                            }
                            .padding(vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        RadioButton(
                            selected = isCustomAppSelected,
                            onClick = {
                                val callback = onActionSelectedCallback
                                pickerGestureTitle = null
                                isAppPickerOpenForGesture = callback
                            }
                        )
                        Column(modifier = Modifier.padding(start = 8.dp)) {
                            Text(
                                text = "Launch Custom App…",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface
                            )
                            if (isCustomAppSelected) {
                                Text(
                                    text = getActionLabel(currentSelectedAction),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.primary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { pickerGestureTitle = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    isAppPickerOpenForGesture?.let { callback ->
        AppPickerSheet(
            installedApps = installedApps,
            onAppSelected = { app ->
                callback(GestureAction.LaunchApp(app.packageName))
                isAppPickerOpenForGesture = null
            },
            onDismiss = { isAppPickerOpenForGesture = null }
        )
    }
}