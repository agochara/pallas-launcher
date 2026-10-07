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
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsTopBar

@Composable
fun GesturesSettingsPage(
    viewModel: MainViewModel,
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
        GestureAction.OpenCamera to "Open Camera",
        GestureAction.OpenDialer to "Open Dialer",
        GestureAction.OpenSearch to "Open Search",
        GestureAction.OpenNotifications to "Open Notifications",
        GestureAction.OpenQuickSettings to "Open Quick Settings",
        GestureAction.OpenAppDrawer to "Open App List",
        GestureAction.None to "None"
    )

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsTopBar(title = "Gestures", onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsSectionHeader("Vertical Swipes")
                SettingsSegmentedRow(
                    title = "Swipe Up",
                    options = listOf(GestureAction.OpenAppDrawer, GestureAction.OpenSearch, GestureAction.None),
                    selected = settings.swipeUpAction,
                    onSelected = { viewModel.updateGestureActions(swipeUp = it) },
                    label = { when (it) {
                        GestureAction.OpenAppDrawer -> "Apps"
                        GestureAction.OpenSearch -> "Search"
                        else -> "None"
                    } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))

                SettingsSegmentedRow(
                    title = "Swipe Down",
                    options = listOf(GestureAction.OpenNotifications, GestureAction.OpenQuickSettings, GestureAction.None),
                    selected = settings.swipeDownAction,
                    onSelected = { viewModel.updateGestureActions(swipeDown = it) },
                    label = {
                        when (it) {
                            GestureAction.OpenNotifications -> "Notifications"
                            GestureAction.OpenQuickSettings -> "Quick Settings"
                            else -> "None"
                        }
                    }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Horizontal Swipes")

                Surface(
                    color = colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pickerGestureTitle = "Short Swipe Left Action"
                            currentSelectedAction = settings.shortSwipeLeftAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateGestureActions(swipeLeft = action)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Short Swipe Left", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = getActionLabel(settings.shortSwipeLeftAction),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))

                Surface(
                    color = colorScheme.surface,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            pickerGestureTitle = "Short Swipe Right Action"
                            currentSelectedAction = settings.shortSwipeRightAction
                            onActionSelectedCallback = { action ->
                                viewModel.updateGestureActions(swipeRight = action)
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Short Swipe Right", style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = getActionLabel(settings.shortSwipeRightAction),
                                style = MaterialTheme.typography.bodyMedium,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            item {
                SettingsSectionHeader("Alphabet Edge Scrub")

                SettingsSegmentedRow(
                    title = "Edge Rail Position",
                    options = EdgeScrubPosition.entries,
                    selected = settings.edgeScrubPosition,
                    onSelected = { viewModel.updateEdgeScrubPosition(it) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))

                SettingsSegmentedRow(
                    title = "Action On Touch Release",
                    options = EdgeScrubReleaseAction.entries,
                    selected = settings.edgeScrubReleaseAction,
                    onSelected = { viewModel.updateEdgeScrubReleaseAction(it) },
                    label = {
                        when (it) {
                            EdgeScrubReleaseAction.STAY_OPEN -> "Stay Open"
                            EdgeScrubReleaseAction.LAUNCH_TOP -> "Launch Top"
                        }
                    }
                )
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Action Picker Dialog
    pickerGestureTitle?.let { title ->
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
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }

                    // App picker option for LaunchApp
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
                            selected = currentSelectedAction is GestureAction.LaunchApp,
                            onClick = {
                                val callback = onActionSelectedCallback
                                pickerGestureTitle = null
                                isAppPickerOpenForGesture = callback
                            }
                        )
                        Column(modifier = Modifier.padding(start = 12.dp)) {
                            Text(
                                text = "Launch Specific App...",
                                style = MaterialTheme.typography.bodyLarge,
                                color = colorScheme.onSurface
                            )
                            if (currentSelectedAction is GestureAction.LaunchApp) {
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
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { pickerGestureTitle = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // App Picker Sheet for selecting an app target
    isAppPickerOpenForGesture?.let { callback ->
        AppPickerSheet(
            installedApps = installedApps,
            onAppSelected = { app ->
                callback(GestureAction.LaunchApp(app.packageName))
                isAppPickerOpenForGesture = null
            },
            onDismiss = {
                isAppPickerOpenForGesture = null
            }
        )
    }
}
