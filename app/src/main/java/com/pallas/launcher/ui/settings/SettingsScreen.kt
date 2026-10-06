package com.pallas.launcher.ui.settings

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.model.ThemeMode
import com.pallas.launcher.platform.ContactsHelper
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.MainViewModel

private enum class SettingsSubpage {
    MAIN,
    APPEARANCE,
    CLOCK_DATE,
    SCREENS_APPS,
    GESTURES,
    SEARCH_CONTACTS,
    ABOUT
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val screens by viewModel.screens.collectAsState()
    val currentScreenIndex by viewModel.currentScreenIndex.collectAsState()

    var currentSubpage by remember { mutableStateOf(SettingsSubpage.MAIN) }

    // Intercept back key: if on subpage, go back to main; else close settings
    BackHandler {
        if (currentSubpage != SettingsSubpage.MAIN) {
            currentSubpage = SettingsSubpage.MAIN
        } else {
            onDismiss()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        AnimatedContent(
            targetState = currentSubpage,
            transitionSpec = {
                if (targetState == SettingsSubpage.MAIN) {
                    (slideInHorizontally(animationSpec = tween(180)) { -it } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { it } + fadeOut(tween(180)))
                } else {
                    (slideInHorizontally(animationSpec = tween(180)) { it } + fadeIn(tween(180)))
                        .togetherWith(slideOutHorizontally(animationSpec = tween(180)) { -it } + fadeOut(tween(180)))
                }
            },
            label = "settings_subpage_transition"
        ) { subpage ->
            when (subpage) {
                SettingsSubpage.MAIN -> {
                    MainSettingsView(
                        viewModel = viewModel,
                        onNavigate = { currentSubpage = it },
                        onDismiss = onDismiss
                    )
                }
                SettingsSubpage.APPEARANCE -> {
                    AppearanceSettingsView(
                        viewModel = viewModel,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.CLOCK_DATE -> {
                    ClockDateSettingsView(
                        viewModel = viewModel,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.SCREENS_APPS -> {
                    ScreensAppsSettingsView(
                        viewModel = viewModel,
                        screens = screens,
                        currentScreenIndex = currentScreenIndex,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.GESTURES -> {
                    GesturesSettingsView(
                        viewModel = viewModel,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.SEARCH_CONTACTS -> {
                    SearchContactsSettingsView(
                        viewModel = viewModel,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
                SettingsSubpage.ABOUT -> {
                    AboutSettingsView(
                        viewModel = viewModel,
                        onBack = { currentSubpage = SettingsSubpage.MAIN }
                    )
                }
            }
        }
    }
}

@Composable
private fun SettingsTopBar(
    title: String,
    onBack: (() -> Unit)? = null,
    onDismiss: (() -> Unit)? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (onBack != null) {
                IconButton(onClick = onBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = colorScheme.onSurface
                    )
                }
            }
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge.copy(
                    fontSize = 22.sp,
                    fontWeight = FontWeight.SemiBold
                ),
                color = colorScheme.onSurface
            )
        }

        if (onDismiss != null) {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Done",
                    color = colorScheme.primary,
                    style = MaterialTheme.typography.labelLarge.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                )
            }
        }
    }
}

@Composable
private fun MainSettingsView(
    viewModel: MainViewModel,
    onNavigate: (SettingsSubpage) -> Unit,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDefault = remember { SystemActionsManager.isDefaultLauncher(context) }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Pallas Settings",
            onDismiss = onDismiss
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Default Launcher status
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(20.dp))
                        .background(colorScheme.surfaceContainer)
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = if (isDefault) "Default Launcher Active" else "Set as Default Launcher",
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                        color = colorScheme.onSurface
                    )
                    Text(
                        text = if (isDefault) "Pallas is your current default home screen."
                        else "Set Pallas as your default launcher to enjoy a fast, distraction-free home.",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (!isDefault) {
                        Button(
                            onClick = { SystemActionsManager.requestSetDefaultLauncher(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(text = "Make Default", color = colorScheme.onPrimary)
                        }
                    }
                }
            }

            // Categories with expressive leading icons
            item {
                SettingsCategoryRow(
                    title = "Appearance & Theme",
                    subtitle = "Dark/Light mode, wallpaper scrim, font scale, status bar",
                    icon = Icons.Default.Palette,
                    onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
                )
            }

            item {
                SettingsCategoryRow(
                    title = "Clock & Date",
                    subtitle = "Position (Top/Bottom, Left/Right), 24h, battery indicator",
                    icon = Icons.Default.Schedule,
                    onClick = { onNavigate(SettingsSubpage.CLOCK_DATE) }
                )
            }

            item {
                SettingsCategoryRow(
                    title = "Home Screens & Apps",
                    subtitle = "Apps alignment & placement, context screens, reordering, hidden apps",
                    icon = Icons.Default.GridView,
                    onClick = { onNavigate(SettingsSubpage.SCREENS_APPS) }
                )
            }

            item {
                SettingsCategoryRow(
                    title = "Gestures",
                    subtitle = "Swipe shortcuts for Camera, Dialer, Search, Notifications",
                    icon = Icons.Default.TouchApp,
                    onClick = { onNavigate(SettingsSubpage.GESTURES) }
                )
            }

            item {
                SettingsCategoryRow(
                    title = "Search & Contacts",
                    subtitle = "Search aliases, searchable contacts, instant dial/SMS",
                    icon = Icons.Default.Search,
                    onClick = { onNavigate(SettingsSubpage.SEARCH_CONTACTS) }
                )
            }

            item {
                SettingsCategoryRow(
                    title = "About Pallas",
                    subtitle = "Design principles, version info, home launcher settings",
                    icon = Icons.Default.Info,
                    onClick = { onNavigate(SettingsSubpage.ABOUT) }
                )
            }

            item {
                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }
}

@Composable
private fun SettingsCategoryRow(
    title: String,
    subtitle: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(colorScheme.surfaceContainer)
            .clickable(onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colorScheme.surfaceContainerHigh),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Medium
                    ),
                    color = colorScheme.onSurface
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant
                )
            }
        }
        Icon(
            imageVector = Icons.Default.ChevronRight,
            contentDescription = null,
            tint = colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
    }
}

/* =========================================================================================
 * 1. APPEARANCE SETTINGS
 * ========================================================================================= */
@Composable
private fun AppearanceSettingsView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Appearance & Theme",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Theme Mode
            item {
                SectionCard(title = "Theme Mode") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ThemeMode.entries.forEach { mode ->
                            val isSelected = settings.themeMode == mode
                            PillOption(
                                text = mode.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.settingsRepository.updateThemeMode(mode) }
                            )
                        }
                    }
                }
            }

            // Wallpaper Scrim
            item {
                SectionCard(title = "Wallpaper Contrast") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Wallpaper Scrim", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Subtle background veil to improve legibility on bright wallpapers.",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = settings.showWallpaperScrim,
                            onCheckedChange = { viewModel.settingsRepository.updateWallpaperScrim(it) }
                        )
                    }
                }
            }

            // Hide Status Bar
            item {
                SectionCard(title = "Status Bar") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(text = "Hide Status Bar", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Text(
                                text = "Completely hide the Android status bar icons for a pure minimalist home.",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                        }
                        Switch(
                            checked = settings.hideStatusBar,
                            onCheckedChange = { viewModel.settingsRepository.updateHideStatusBar(it) }
                        )
                    }
                }
            }

            // Font Scale with M3 Slider
            item {
                SectionCard(title = "App Text Scale") {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(settings.appFontScale * 100).toInt()}%",
                                style = MaterialTheme.typography.titleMedium,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                            TextButton(
                                onClick = { viewModel.settingsRepository.updateFontScale(1.0f) },
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 2.dp)
                            ) {
                                Text("Reset (100%)", style = MaterialTheme.typography.labelSmall, color = colorScheme.primary)
                            }
                        }
                        Slider(
                            value = settings.appFontScale,
                            onValueChange = { viewModel.settingsRepository.updateFontScale(it) },
                            valueRange = 0.8f..1.3f,
                            steps = 4
                        )
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

/* =========================================================================================
 * 2. CLOCK & DATE SETTINGS
 * ========================================================================================= */
@Composable
private fun ClockDateSettingsView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Clock & Date",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Vertical Position
            item {
                SectionCard(title = "Clock Vertical Position") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClockVerticalPosition.entries.forEach { pos ->
                            val isSelected = settings.clockVerticalPosition == pos
                            PillOption(
                                text = pos.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.settingsRepository.updateClockPosition(
                                        pos,
                                        settings.clockHorizontalAlignment
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Horizontal Alignment
            item {
                SectionCard(title = "Clock Horizontal Alignment") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ClockHorizontalAlignment.entries.forEach { align ->
                            val isSelected = settings.clockHorizontalAlignment == align
                            PillOption(
                                text = align.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.settingsRepository.updateClockPosition(
                                        settings.clockVerticalPosition,
                                        align
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Time & Date Formats
            item {
                SectionCard(title = "Formats & Indicators") {
                    Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "24-Hour Clock", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.is24HourClock,
                                onCheckedChange = { viewModel.settingsRepository.update24HourClock(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Show Date", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.showDate,
                                onCheckedChange = { viewModel.settingsRepository.updateShowDate(it) }
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(text = "Show Battery in Date", color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                            Switch(
                                checked = settings.showBattery,
                                onCheckedChange = { viewModel.settingsRepository.updateShowBattery(it) }
                            )
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

/* =========================================================================================
 * 3. SCREENS & APPS SETTINGS
 * ========================================================================================= */
@Composable
private fun ScreensAppsSettingsView(
    viewModel: MainViewModel,
    screens: List<HomeScreen>,
    currentScreenIndex: Int,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()

    var selectedIndex by remember { mutableStateOf(currentScreenIndex.coerceIn(0, (screens.size - 1).coerceAtLeast(0))) }
    val activeScreen = screens.getOrNull(selectedIndex) ?: screens.firstOrNull()

    var showAddScreenDialog by remember { mutableStateOf(false) }
    var newScreenName by remember { mutableStateOf("") }
    var newContextTag by remember { mutableStateOf("") }

    var screenToRename by remember { mutableStateOf<HomeScreen?>(null) }
    var renameTitle by remember { mutableStateOf("") }
    var renameContext by remember { mutableStateOf("") }

    var appToRename by remember { mutableStateOf<ScreenApp?>(null) }
    var appNewLabel by remember { mutableStateOf("") }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Home Screens & Apps",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Apps Placement
            item {
                SectionCard(title = "Apps Vertical Placement") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppsVerticalPosition.entries.forEach { pos ->
                            val isSelected = settings.appsVerticalPosition == pos
                            PillOption(
                                text = pos.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.settingsRepository.updateAppsPosition(
                                        pos,
                                        settings.appsHorizontalAlignment
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Apps Alignment
            item {
                SectionCard(title = "Apps Horizontal Alignment") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        AppsHorizontalAlignment.entries.forEach { align ->
                            val isSelected = settings.appsHorizontalAlignment == align
                            PillOption(
                                text = align.name.lowercase().replaceFirstChar { it.uppercase() },
                                isSelected = isSelected,
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    viewModel.settingsRepository.updateAppsPosition(
                                        settings.appsVerticalPosition,
                                        align
                                    )
                                }
                            )
                        }
                    }
                }
            }

            // Screen Selector & Manager
            item {
                SectionCard(title = "Screens Manager") {
                    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Select screen to configure:",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            TextButton(onClick = { showAddScreenDialog = true }) {
                                Text(text = "+ Add Screen", color = colorScheme.primary)
                            }
                        }

                        // Horizontal Screen Pills
                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            itemsIndexed(screens) { index, s ->
                                val isSelected = index == selectedIndex
                                val label = if (s.contextTag.isNullOrBlank()) s.name else "${s.name} (${s.contextTag})"
                                PillOption(
                                    text = label,
                                    isSelected = isSelected,
                                    onClick = {
                                        selectedIndex = index
                                        viewModel.setScreenIndex(index)
                                    }
                                )
                            }
                        }

                        // Screen operations for active screen
                        if (activeScreen != null) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Move screen left
                                if (selectedIndex > 0) {
                                    TextButton(onClick = {
                                        viewModel.screenRepository.moveScreen(selectedIndex, selectedIndex - 1)
                                        selectedIndex -= 1
                                    }) {
                                        Text(text = "◀ Move Left", color = colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                // Move screen right
                                if (selectedIndex < screens.size - 1) {
                                    TextButton(onClick = {
                                        viewModel.screenRepository.moveScreen(selectedIndex, selectedIndex + 1)
                                        selectedIndex += 1
                                    }) {
                                        Text(text = "Move Right ▶", color = colorScheme.onSurface, style = MaterialTheme.typography.bodySmall)
                                    }
                                }

                                Spacer(modifier = Modifier.weight(1f))

                                TextButton(onClick = {
                                    screenToRename = activeScreen
                                    renameTitle = activeScreen.name
                                    renameContext = activeScreen.contextTag ?: ""
                                }) {
                                    Text(text = "Rename", color = colorScheme.primary, style = MaterialTheme.typography.bodySmall)
                                }

                                if (screens.size > 1) {
                                    TextButton(onClick = {
                                        viewModel.screenRepository.removeScreen(activeScreen.id)
                                        selectedIndex = 0
                                    }) {
                                        Text(text = "Delete", color = colorScheme.error, style = MaterialTheme.typography.bodySmall)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Apps in Active Screen
            item {
                SectionCard(
                    title = "Apps on '${activeScreen?.name ?: "Screen"}'",
                    action = {
                        TextButton(onClick = { viewModel.openAppPicker() }) {
                            Text(text = "+ Add App", color = colorScheme.primary)
                        }
                    }
                ) {
                    val apps = activeScreen?.apps ?: emptyList()
                    if (apps.isEmpty()) {
                        Text(
                            text = "No apps on this screen. Tap '+ Add App' to add your favorites.",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            apps.forEachIndexed { appIdx, app ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 14.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f),
                                        verticalArrangement = Arrangement.spacedBy(1.dp)
                                    ) {
                                        Text(
                                            text = app.displayLabel,
                                            color = colorScheme.onSurface,
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
                                        // Move Up
                                        if (appIdx > 0) {
                                            IconButton(
                                                onClick = {
                                                    activeScreen?.let { s ->
                                                        val list = s.apps.toMutableList()
                                                        val item = list.removeAt(appIdx)
                                                        list.add(appIdx - 1, item)
                                                        viewModel.screenRepository.reorderApps(s.id, list)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowUp,
                                                    contentDescription = "Move Up",
                                                    tint = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Move Down
                                        if (appIdx < apps.size - 1) {
                                            IconButton(
                                                onClick = {
                                                    activeScreen?.let { s ->
                                                        val list = s.apps.toMutableList()
                                                        val item = list.removeAt(appIdx)
                                                        list.add(appIdx + 1, item)
                                                        viewModel.screenRepository.reorderApps(s.id, list)
                                                    }
                                                },
                                                modifier = Modifier.size(32.dp)
                                            ) {
                                                Icon(
                                                    imageVector = Icons.Default.KeyboardArrowDown,
                                                    contentDescription = "Move Down",
                                                    tint = colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(18.dp)
                                                )
                                            }
                                        }

                                        // Rename Label
                                        IconButton(
                                            onClick = {
                                                appToRename = app
                                                appNewLabel = app.displayLabel
                                            },
                                            modifier = Modifier.size(32.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Edit,
                                                contentDescription = "Rename",
                                                tint = colorScheme.primary,
                                                modifier = Modifier.size(16.dp)
                                            )
                                        }

                                        // Delete App
                                        IconButton(
                                            onClick = {
                                                activeScreen?.let { s ->
                                                    viewModel.screenRepository.removeAppFromScreen(s.id, app.uniqueKey)
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
                }
            }

            // Hidden Apps Management Card
            item {
                SectionCard(title = "Hidden Apps (${settings.hiddenApps.size})") {
                    if (settings.hiddenApps.isEmpty()) {
                        Text(
                            text = "No apps are hidden. To hide an app, long press it and choose 'Hide App'.",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text(
                                text = "These apps are hidden from the app drawer and home screens:",
                                color = colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.bodySmall
                            )
                            settings.hiddenApps.forEach { pkg ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(colorScheme.surfaceContainerHigh)
                                        .padding(horizontal = 14.dp, vertical = 8.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = pkg,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurface,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    TextButton(
                                        onClick = { viewModel.settingsRepository.unhideApp(pkg) }
                                    ) {
                                        Text("Unhide", color = colorScheme.primary)
                                    }
                                }
                            }
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    // Add Screen Dialog
    if (showAddScreenDialog) {
        AlertDialog(
            onDismissRequest = { showAddScreenDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainer,
            title = { Text(text = "Add Screen", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newScreenName,
                        onValueChange = { newScreenName = it },
                        label = { Text("Screen Name (e.g. Work, Focus)") },
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
                        viewModel.screenRepository.addScreen(newScreenName.trim(), newContextTag.trim())
                        newScreenName = ""
                        newContextTag = ""
                        showAddScreenDialog = false
                    }
                }) {
                    Text(text = "Add", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddScreenDialog = false }) {
                    Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Rename Screen Dialog
    screenToRename?.let { s ->
        AlertDialog(
            onDismissRequest = { screenToRename = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainer,
            title = { Text(text = "Rename Screen", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = renameTitle,
                        onValueChange = { renameTitle = it },
                        label = { Text("Screen Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = renameContext,
                        onValueChange = { renameContext = it },
                        label = { Text("Context Tag") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (renameTitle.isNotBlank()) {
                        viewModel.screenRepository.renameScreen(s.id, renameTitle.trim(), renameContext.trim())
                        screenToRename = null
                    }
                }) {
                    Text(text = "Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { screenToRename = null }) {
                    Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Rename App Label Dialog
    appToRename?.let { targetApp ->
        AlertDialog(
            onDismissRequest = { appToRename = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainer,
            title = { Text(text = "Rename App Label", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = appNewLabel,
                        onValueChange = { appNewLabel = it },
                        label = { Text("Display Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (appNewLabel.isNotBlank()) {
                        activeScreen?.let { s ->
                            viewModel.screenRepository.updateAppLabel(s.id, targetApp.uniqueKey, appNewLabel.trim())
                        }
                        appToRename = null
                    }
                }) {
                    Text(text = "Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { appToRename = null }) {
                    Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/* =========================================================================================
 * 4. GESTURES SETTINGS
 * ========================================================================================= */
@Composable
private fun GesturesSettingsView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()

    val gestureOptions = listOf(
        GestureAction.OpenCamera to "Camera",
        GestureAction.OpenDialer to "Dialer",
        GestureAction.OpenSearch to "Search",
        GestureAction.OpenNotifications to "Notifications",
        GestureAction.OpenQuickSettings to "Quick Settings",
        GestureAction.None to "None"
    )

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Gestures",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard(title = "Swipe Up") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(GestureAction.OpenSearch to "Search", GestureAction.None to "None").forEach { (action, label) ->
                            PillOption(
                                text = label,
                                isSelected = settings.swipeUpAction == action,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.settingsRepository.updateGestureActions(swipeUp = action) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Swipe Down") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(
                            GestureAction.OpenNotifications to "Notifications",
                            GestureAction.OpenQuickSettings to "Quick Settings",
                            GestureAction.None to "None"
                        ).forEach { (action, label) ->
                            PillOption(
                                text = label,
                                isSelected = settings.swipeDownAction == action,
                                modifier = Modifier.weight(1f),
                                onClick = { viewModel.settingsRepository.updateGestureActions(swipeDown = action) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Small Swipe Left") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gestureOptions) { (action, label) ->
                            PillOption(
                                text = label,
                                isSelected = settings.shortSwipeLeftAction == action,
                                onClick = { viewModel.settingsRepository.updateGestureActions(swipeLeft = action) }
                            )
                        }
                    }
                }
            }

            item {
                SectionCard(title = "Small Swipe Right") {
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        items(gestureOptions) { (action, label) ->
                            PillOption(
                                text = label,
                                isSelected = settings.shortSwipeRightAction == action,
                                onClick = { viewModel.settingsRepository.updateGestureActions(swipeRight = action) }
                            )
                        }
                    }
                }
            }

            // Gesture reference guide
            item {
                SectionCard(title = "Navigation Guide") {
                    Text(
                        text = "• Long swipe left / right: Switch between contextual screens\n" +
                                "• Long press empty wallpaper: Open this Settings screen\n" +
                                "• Long press an app: App options & dynamic shortcuts\n" +
                                "• Tap Clock: Open Clock & Alarms\n" +
                                "• Tap Date: Open Calendar",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        lineHeight = 20.sp
                    )
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

/* =========================================================================================
 * 5. SEARCH & CONTACTS SETTINGS
 * ========================================================================================= */
@Composable
private fun SearchContactsSettingsView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()

    var showAddAliasDialog by remember { mutableStateOf(false) }
    var newAliasKey by remember { mutableStateOf("") }
    var newAliasTarget by remember { mutableStateOf("") }

    var showAddContactDialog by remember { mutableStateOf(false) }
    var newContactName by remember { mutableStateOf("") }
    var newContactPhone by remember { mutableStateOf("") }
    var contactFilterQuery by remember { mutableStateOf("") }
    var deviceContacts by remember { mutableStateOf<List<com.pallas.launcher.domain.model.ContactItem>>(emptyList()) }
    var hasContactsPerm by remember {
        mutableStateOf(ContactsHelper.hasContactsPermission(context))
    }

    val contactPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        hasContactsPerm = isGranted
        if (isGranted) {
            deviceContacts = ContactsHelper.getAllContacts(context)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "Search & Contacts",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Aliases Card
            item {
                SectionCard(
                    title = "Search Aliases",
                    action = {
                        TextButton(onClick = { showAddAliasDialog = true }) {
                            Text(text = "+ Add Alias", color = colorScheme.primary)
                        }
                    }
                ) {
                    Text(
                        text = "Instant acronym abbreviations (e.g. 'wa' -> WhatsApp, 'calc' -> Calculator):",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    settings.customAliases.forEach { (alias, target) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .padding(horizontal = 14.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(text = alias, color = colorScheme.primary, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.bodyLarge)
                                Text(text = "→", color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                                Text(text = target, color = colorScheme.onSurface, style = MaterialTheme.typography.bodyMedium)
                            }
                            IconButton(
                                onClick = { viewModel.settingsRepository.removeAlias(alias) },
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

            // Searchable Contacts Card
            item {
                SectionCard(
                    title = "Searchable Contacts",
                    action = {
                        TextButton(onClick = {
                            if (hasContactsPerm) {
                                deviceContacts = ContactsHelper.getAllContacts(context)
                            }
                            showAddContactDialog = true
                        }) {
                            Text(text = "+ Add Contact", color = colorScheme.primary)
                        }
                    }
                ) {
                    Text(
                        text = "Only selected contacts are searched (e.g. 'call mom', 'sms dad', 'wa mom'):",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )

                    if (settings.pinnedContacts.isEmpty()) {
                        Text(
                            text = "No contacts added yet. Tap '+ Add Contact' to select contacts.",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    } else {
                        settings.pinnedContacts.forEach { (name, phone) ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(colorScheme.surfaceContainerHigh)
                                    .padding(horizontal = 14.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                                    Text(text = name, color = colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
                                    Text(text = phone, color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                                IconButton(
                                    onClick = { viewModel.settingsRepository.removePinnedContact(name) },
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

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }

    // Add Alias Dialog
    if (showAddAliasDialog) {
        AlertDialog(
            onDismissRequest = { showAddAliasDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainer,
            title = { Text(text = "Add Search Alias", color = colorScheme.onSurface) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAliasKey,
                        onValueChange = { newAliasKey = it },
                        label = { Text("Alias shortcut (e.g. 'wa')") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newAliasTarget,
                        onValueChange = { newAliasTarget = it },
                        label = { Text("Target app name or query") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAliasKey.isNotBlank() && newAliasTarget.isNotBlank()) {
                        viewModel.settingsRepository.setAlias(newAliasKey.trim(), newAliasTarget.trim())
                        newAliasKey = ""
                        newAliasTarget = ""
                        showAddAliasDialog = false
                    }
                }) {
                    Text(text = "Add", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAliasDialog = false }) {
                    Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = {
                showAddContactDialog = false
                newContactName = ""
                newContactPhone = ""
                contactFilterQuery = ""
            },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainer,
            title = { Text(text = "Add Searchable Contact", color = colorScheme.onSurface) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (!hasContactsPerm) {
                        Button(
                            onClick = {
                                contactPermissionLauncher.launch(android.Manifest.permission.READ_CONTACTS)
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(12.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(text = "Pick from Device Contacts", color = colorScheme.onPrimary)
                        }
                        Text(
                            text = "Or enter contact manually below:",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall
                        )
                    }

                    OutlinedTextField(
                        value = newContactName,
                        onValueChange = { newContactName = it },
                        label = { Text("Contact Name") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    OutlinedTextField(
                        value = newContactPhone,
                        onValueChange = { newContactPhone = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )

                    if (hasContactsPerm && deviceContacts.isNotEmpty()) {
                        Text(
                            text = "Or tap a contact from phonebook:",
                            color = colorScheme.onSurfaceVariant,
                            style = MaterialTheme.typography.bodySmall,
                            modifier = Modifier.padding(top = 4.dp)
                        )
                        OutlinedTextField(
                            value = contactFilterQuery,
                            onValueChange = { contactFilterQuery = it },
                            label = { Text("Filter phonebook...") },
                            singleLine = true,
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        )

                        val filtered = remember(deviceContacts, contactFilterQuery) {
                            if (contactFilterQuery.isBlank()) {
                                deviceContacts.take(5)
                            } else {
                                deviceContacts.filter {
                                    it.name.contains(contactFilterQuery, ignoreCase = true) ||
                                            it.phoneNumber.contains(contactFilterQuery)
                                }.take(5)
                            }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(colorScheme.surfaceContainerHigh)
                                .padding(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            filtered.forEach { c ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable {
                                            newContactName = c.name
                                            newContactPhone = c.phoneNumber
                                        }
                                        .padding(vertical = 4.dp, horizontal = 6.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(text = c.name, color = colorScheme.primary, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                                    Text(text = c.phoneNumber, color = colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newContactName.isNotBlank() && newContactPhone.isNotBlank()) {
                        viewModel.settingsRepository.addPinnedContact(newContactName.trim(), newContactPhone.trim())
                        newContactName = ""
                        newContactPhone = ""
                        contactFilterQuery = ""
                        showAddContactDialog = false
                    }
                }) {
                    Text(text = "Add", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = {
                    showAddContactDialog = false
                    newContactName = ""
                    newContactPhone = ""
                    contactFilterQuery = ""
                }) {
                    Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}

/* =========================================================================================
 * 6. ABOUT SETTINGS
 * ========================================================================================= */
@Composable
private fun AboutSettingsView(
    viewModel: MainViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxSize()) {
        SettingsTopBar(
            title = "About Pallas",
            onBack = onBack
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                SectionCard(title = "Pallas Launcher") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Minimalist. Elegant. Calming.",
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Built on Information Theory principles: high-frequency tasks require minimal bits and instant interaction. No gamification, no app badges, no visual clutter, typography first.",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            lineHeight = 20.sp
                        )
                    }
                }
            }

            item {
                SectionCard(title = "System Launcher") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Default Launcher Settings",
                            color = colorScheme.onSurface,
                            style = MaterialTheme.typography.bodyLarge
                        )
                        Button(
                            onClick = { SystemActionsManager.requestSetDefaultLauncher(context) },
                            colors = ButtonDefaults.buttonColors(containerColor = colorScheme.primary),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Text(text = "Configure", color = colorScheme.onPrimary)
                        }
                    }
                }
            }

            item { Spacer(modifier = Modifier.height(28.dp)) }
        }
    }
}

/* =========================================================================================
 * REUSABLE COMPONENTS
 * ========================================================================================= */
@Composable
private fun SectionCard(
    title: String,
    action: (@Composable () -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(colorScheme.surfaceContainer)
            .padding(18.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold
                ),
                color = colorScheme.onSurface
            )
            action?.invoke()
        }
        content()
    }
}

@Composable
private fun PillOption(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(
                if (isSelected) colorScheme.primaryContainer
                else colorScheme.surfaceContainerHigh
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.labelMedium.copy(
                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal
            )
        )
    }
}
