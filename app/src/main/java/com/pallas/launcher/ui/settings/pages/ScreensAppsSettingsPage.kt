package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.DragHandle
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.components.RenameAppDialog
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsTopBar

@Composable
fun ScreensAppsSettingsPage(
    viewModel: MainViewModel,
    screens: List<HomeScreen>,
    currentScreenIndex: Int,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val haptic = LocalHapticFeedback.current

    var selectedIndex by remember { mutableStateOf(currentScreenIndex.coerceIn(0, (screens.size - 1).coerceAtLeast(0))) }
    val activeScreen = screens.getOrNull(selectedIndex) ?: screens.firstOrNull()

    var draggedScreenIndex by remember { mutableStateOf<Int?>(null) }
    var screenDragDeltaX by remember { mutableFloatStateOf(0f) }

    var draggedAppIdx by remember { mutableStateOf<Int?>(null) }
    var appDragDeltaY by remember { mutableFloatStateOf(0f) }

    var showAddScreenDialog by remember { mutableStateOf(false) }
    var newScreenName by remember { mutableStateOf("") }
    var newContextTag by remember { mutableStateOf("") }

    var screenToRename by remember { mutableStateOf<HomeScreen?>(null) }
    var appToRename by remember { mutableStateOf<ScreenApp?>(null) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsTopBar(title = "Home Screens & Apps", onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Placement & Alignment
            item {
                SettingsSectionHeader("Apps Placement")
                SettingsSegmentedRow(
                    title = "Vertical Position",
                    options = AppsVerticalPosition.entries,
                    selected = settings.appsVerticalPosition,
                    onSelected = { viewModel.updateAppsPosition(it, settings.appsHorizontalAlignment) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
                SettingsSegmentedRow(
                    title = "Horizontal Alignment",
                    options = AppsHorizontalAlignment.entries,
                    selected = settings.appsHorizontalAlignment,
                    onSelected = { viewModel.updateAppsPosition(settings.appsVerticalPosition, it) },
                    label = { it.name.lowercase().replaceFirstChar { char -> char.uppercase() } }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            // Screens Manager
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Screens Manager",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { showAddScreenDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Screen",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                // Horizontal screen tabs
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                ) {
                    itemsIndexed(screens, key = { _, s -> s.id }) { index, s ->
                        val isSelected = index == selectedIndex
                        val label = if (s.contextTag.isNullOrBlank()) s.name else "${s.name} (${s.contextTag})"
                        val isBeingDragged = draggedScreenIndex == index

                        Box(
                            modifier = Modifier
                                .zIndex(if (isBeingDragged) 2f else 0f)
                                .graphicsLayer {
                                    translationX = if (isBeingDragged) screenDragDeltaX else 0f
                                    if (isBeingDragged) {
                                        scaleX = 1.05f
                                        scaleY = 1.05f
                                        shadowElevation = 8f
                                    }
                                }
                                .pointerInput(index, screens.size) {
                                    detectDragGestures(
                                        onDragStart = {
                                            draggedScreenIndex = index
                                            screenDragDeltaX = 0f
                                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                        },
                                        onDrag = { change, dragAmount ->
                                            change.consume()
                                            screenDragDeltaX += dragAmount.x
                                            val stepThreshold = 80.dp.toPx()
                                            if (screenDragDeltaX > stepThreshold && index < screens.size - 1) {
                                                viewModel.moveScreen(index, index + 1)
                                                selectedIndex = index + 1
                                                screenDragDeltaX -= stepThreshold
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            } else if (screenDragDeltaX < -stepThreshold && index > 0) {
                                                viewModel.moveScreen(index, index - 1)
                                                selectedIndex = index - 1
                                                screenDragDeltaX += stepThreshold
                                                haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                            }
                                        },
                                        onDragEnd = {
                                            draggedScreenIndex = null
                                            screenDragDeltaX = 0f
                                        },
                                        onDragCancel = {
                                            draggedScreenIndex = null
                                            screenDragDeltaX = 0f
                                        }
                                    )
                                }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(20.dp),
                                color = if (isSelected) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    selectedIndex = index
                                    viewModel.setScreenIndex(index)
                                }
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelLarge,
                                    color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurface,
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
                                )
                            }
                        }
                    }
                }

                // Active screen actions (Rename, Delete)
                if (activeScreen != null) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.End,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TextButton(onClick = { screenToRename = activeScreen }) {
                            Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Rename", style = MaterialTheme.typography.bodySmall)
                        }

                        if (screens.size > 1) {
                            TextButton(onClick = {
                                viewModel.removeScreen(activeScreen.id)
                                selectedIndex = 0
                            }) {
                                Icon(Icons.Default.Close, contentDescription = null, modifier = Modifier.size(16.dp), tint = colorScheme.error)
                                Spacer(modifier = Modifier.width(4.dp))
                                Text("Delete", color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
                            }
                        }
                    }
                }
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            // Apps in Active Screen
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 16.dp, end = 16.dp, top = 20.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Apps on '${activeScreen?.name ?: "Screen"}'",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { viewModel.openAppPicker() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add App",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                val apps = activeScreen?.apps ?: emptyList()
                if (apps.isEmpty()) {
                    Text(
                        text = "No apps on this screen. Tap '+' to add apps.",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        apps.forEachIndexed { appIdx, app ->
                            val isAppBeingDragged = draggedAppIdx == appIdx
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .zIndex(if (isAppBeingDragged) 2f else 0f)
                                    .graphicsLayer {
                                        translationY = if (isAppBeingDragged) appDragDeltaY else 0f
                                        if (isAppBeingDragged) {
                                            shadowElevation = 10f
                                            scaleX = 1.02f
                                            scaleY = 1.02f
                                        }
                                    }
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Drag handle
                                Box(
                                    modifier = Modifier
                                        .padding(end = 10.dp)
                                        .pointerInput(appIdx, apps.size) {
                                            detectDragGestures(
                                                onDragStart = {
                                                    draggedAppIdx = appIdx
                                                    appDragDeltaY = 0f
                                                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                },
                                                onDrag = { change, dragAmount ->
                                                    change.consume()
                                                    appDragDeltaY += dragAmount.y
                                                    val rowHeightThreshold = 52.dp.toPx()
                                                    if (appDragDeltaY > rowHeightThreshold && appIdx < apps.size - 1) {
                                                        activeScreen?.let { s ->
                                                            val list = s.apps.toMutableList()
                                                            val item = list.removeAt(appIdx)
                                                            list.add(appIdx + 1, item)
                                                            viewModel.reorderApps(s.id, list)
                                                        }
                                                        appDragDeltaY -= rowHeightThreshold
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    } else if (appDragDeltaY < -rowHeightThreshold && appIdx > 0) {
                                                        activeScreen?.let { s ->
                                                            val list = s.apps.toMutableList()
                                                            val item = list.removeAt(appIdx)
                                                            list.add(appIdx - 1, item)
                                                            viewModel.reorderApps(s.id, list)
                                                        }
                                                        appDragDeltaY += rowHeightThreshold
                                                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                                                    }
                                                },
                                                onDragEnd = {
                                                    draggedAppIdx = null
                                                    appDragDeltaY = 0f
                                                },
                                                onDragCancel = {
                                                    draggedAppIdx = null
                                                    appDragDeltaY = 0f
                                                }
                                            )
                                        }
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.DragHandle,
                                        contentDescription = "Drag to reorder",
                                        tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                                        modifier = Modifier.size(24.dp)
                                    )
                                }

                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(1.dp)
                                ) {
                                    Text(
                                        text = if (app.isProfileRemoved) "${app.displayLabel} (Removed profile)" else app.displayLabel,
                                        color = if (app.isProfileRemoved) colorScheme.error else colorScheme.onSurface,
                                        style = MaterialTheme.typography.bodyLarge,
                                        fontWeight = FontWeight.Medium
                                    )
                                    if (app.customLabel != null) {
                                        Text(
                                            text = "Original: ${app.defaultLabel}",
                                            color = colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodySmall
                                        )
                                    }
                                }

                                Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
                                    IconButton(
                                        onClick = { appToRename = app },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }

                                    IconButton(
                                        onClick = {
                                            activeScreen?.let { s ->
                                                viewModel.removeAppFromScreen(s.id, app.uniqueKey)
                                            }
                                        },
                                        modifier = Modifier.size(32.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Close,
                                            contentDescription = "Remove",
                                            tint = colorScheme.error,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
                HorizontalDivider(modifier = Modifier.padding(top = 16.dp), color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }

            // Hidden Apps
            item {
                SettingsSectionHeader("Hidden Apps (${settings.hiddenApps.size})")
                if (settings.hiddenApps.isEmpty()) {
                    Text(
                        text = "No apps are hidden. To hide an app, long press it and choose 'Hide App'.",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp)
                    )
                } else {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        settings.hiddenApps.forEach { pkg ->
                            val appItem = installedApps.find { it.packageName == pkg }
                            val appLabel = appItem?.label ?: pkg
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = appLabel,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (appLabel != pkg) {
                                        Text(
                                            text = pkg,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                                TextButton(onClick = { viewModel.unhideApp(pkg) }) {
                                    Text("Unhide", color = colorScheme.primary)
                                }
                            }
                        }
                    }
                }
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Add Screen Dialog
    if (showAddScreenDialog) {
        AlertDialog(
            onDismissRequest = { showAddScreenDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Add Screen", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newScreenName,
                        onValueChange = { newScreenName = it },
                        label = { Text("Screen Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newContextTag,
                        onValueChange = { newContextTag = it },
                        label = { Text("Context Tag (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newScreenName.isNotBlank()) {
                        viewModel.addScreen(newScreenName.trim(), newContextTag.trim())
                        newScreenName = ""
                        newContextTag = ""
                        showAddScreenDialog = false
                    }
                }) {
                    Text("Add", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScreenDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Rename Screen Dialog
    screenToRename?.let { s ->
        var renameName by remember { mutableStateOf(s.name) }
        var renameTag by remember { mutableStateOf(s.contextTag ?: "") }
        AlertDialog(
            onDismissRequest = { screenToRename = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Rename Screen", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = renameName,
                        onValueChange = { renameName = it },
                        label = { Text("Screen Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = renameTag,
                        onValueChange = { renameTag = it },
                        label = { Text("Context Tag (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameName.isNotBlank()) {
                        viewModel.renameScreen(s.id, renameName.trim(), renameTag.trim())
                        screenToRename = null
                    }
                }) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { screenToRename = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Rename App Dialog (Deduplicated)
    appToRename?.let { app ->
        RenameAppDialog(
            initialLabel = app.displayLabel,
            onConfirm = { trimmed ->
                activeScreen?.let { s ->
                    viewModel.updateAppLabel(s.id, app.uniqueKey, trimmed)
                }
            },
            onDismiss = { appToRename = null }
        )
    }
}
