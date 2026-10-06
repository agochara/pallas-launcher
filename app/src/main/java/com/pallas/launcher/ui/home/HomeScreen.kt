package com.pallas.launcher.ui.home

import android.content.Context
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.ui.unit.IntOffset
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.platform.SystemActionsManager
import androidx.activity.compose.BackHandler
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.LauncherOverlay
import com.pallas.launcher.ui.apps.AlphabeticalAppDrawer
import com.pallas.launcher.ui.components.RenameAppDialog
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.search.LauncherSearchOverlay
import com.pallas.launcher.ui.settings.SettingsScreen
import com.pallas.launcher.ui.theme.AccentGold
import com.pallas.launcher.ui.theme.AccentRed
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@Composable
fun PallasHomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val screens by viewModel.screens.collectAsState()
    val currentScreenIndex by viewModel.currentScreenIndex.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val isAppsLoaded by viewModel.isAppsLoaded.collectAsState()

    val overlay by viewModel.overlay.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    var aliasToSet by remember { mutableStateOf<ScreenApp?>(null) }
    var appToRename by remember { mutableStateOf<ScreenApp?>(null) }

    BackHandler(enabled = overlay !is LauncherOverlay.None || currentScreenIndex != 0) {
        if (overlay !is LauncherOverlay.None) {
            viewModel.dismissOverlay()
        } else if (currentScreenIndex != 0) {
            viewModel.setScreenIndex(0)
        }
    }

    var showScreenNameIndicator by remember { mutableStateOf(false) }
    LaunchedEffect(currentScreenIndex) {
        if (screens.size > 1) {
            showScreenNameIndicator = true
            kotlinx.coroutines.delay(1200)
            showScreenNameIndicator = false
        }
    }

    val currentScreen = screens.getOrNull(currentScreenIndex) ?: screens.firstOrNull()
    val colors = LocalLauncherColors.current
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    Box(modifier = modifier.fillMaxSize()) {
        GestureContainer(
            onSwipeUp = { viewModel.executeGesture(settings.swipeUpAction) },
            onSwipeDown = { viewModel.executeGesture(settings.swipeDownAction) },
            onShortSwipeLeft = { viewModel.executeGesture(settings.shortSwipeLeftAction) },
            onShortSwipeRight = { viewModel.executeGesture(settings.shortSwipeRightAction) },
            onLongSwipeLeft = { viewModel.onScreenSwiped(1) },
            onLongSwipeRight = { viewModel.onScreenSwiped(-1) },
            onBackgroundLongPress = { viewModel.openSettings() },
            modifier = Modifier.fillMaxSize(),
            enabled = overlay is LauncherOverlay.None
        ) {
        // Wallpaper contrast gradient behind content
        if (settings.showWallpaperScrim) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.15f),
                                Color.Black.copy(alpha = 0.45f)
                            )
                        )
                    )
            )
        }

        // Screen identity indicator pill on swipe
        AnimatedVisibility(
            visible = showScreenNameIndicator && (screens.size > 1),
            enter = fadeIn(tween(150)),
            exit = fadeOut(tween(300)),
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
                .padding(top = 8.dp)
        ) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colorScheme.surfaceContainerHigh.copy(alpha = 0.9f)
            ) {
                val tagStr = currentScreen?.contextTag?.takeIf { it.isNotBlank() }?.let { " • $it" } ?: ""
                Text(
                    text = "${currentScreen?.name ?: "Screen"}$tagStr",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                )
            }
        }

        val isClockTop = settings.clockVerticalPosition == ClockVerticalPosition.TOP
        val appsPos = settings.appsVerticalPosition

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            val clockComposable: @Composable () -> Unit = {
                ClockHeader(
                    timeString = currentTime,
                    dateString = currentDate,
                    alignment = settings.clockHorizontalAlignment,
                    showDate = settings.showDate,
                    onClockClick = { viewModel.openClock() },
                    onDateClick = { viewModel.openCalendar() },
                    fontScale = settings.appFontScale
                )
            }

            val appsComposable: @Composable () -> Unit = {
                AnimatedContent(
                    targetState = currentScreenIndex,
                    transitionSpec = {
                        val spec = tween<Float>(220, easing = FastOutSlowInEasing)
                        val slideSpec = tween<IntOffset>(220, easing = FastOutSlowInEasing)
                        if (targetState > initialState) {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> width / 2 } + fadeIn(animationSpec = spec)) togetherWith
                                    (slideOutHorizontally(animationSpec = slideSpec) { width -> -width / 2 } + fadeOut(animationSpec = spec))
                        } else {
                            (slideInHorizontally(animationSpec = slideSpec) { width -> -width / 2 } + fadeIn(animationSpec = spec)) togetherWith
                                    (slideOutHorizontally(animationSpec = slideSpec) { width -> width / 2 } + fadeOut(animationSpec = spec))
                        }
                    },
                    label = "screen_pager"
                ) { targetIndex ->
                    val screen = screens.getOrNull(targetIndex) ?: currentScreen
                    val allApps = screen?.apps ?: emptyList()
                    val visibleApps = remember(allApps, installedApps, isAppsLoaded) {
                        if (!isAppsLoaded) allApps.filterNot { it.isProfileRemoved }
                        else allApps.filter { app ->
                            !app.isProfileRemoved && installedApps.any { it.uniqueKey == app.uniqueKey }
                        }
                    }

                    AppListView(
                        apps = visibleApps,
                        alignment = settings.appsHorizontalAlignment,
                        fontScale = settings.appFontScale,
                        isLoaded = isAppsLoaded,
                        onAppClick = { app ->
                            if (!app.isProfileRemoved) {
                                viewModel.appUsageTracker.recordAppLaunch(app.packageName)
                                viewModel.appRepository.launchApp(app.packageName, app.activityName, app.userHandle)
                            }
                        },
                        onAppLongClick = { app ->
                            viewModel.setAppToCustomize(app)
                        },
                        onEmptyScreenClick = {
                            viewModel.openAppPicker()
                        }
                    )
                }
            }

            if (isClockTop) {
                clockComposable()
                when (appsPos) {
                    AppsVerticalPosition.TOP -> {
                        Spacer(modifier = Modifier.height(16.dp))
                        appsComposable()
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    AppsVerticalPosition.CENTER -> {
                        Spacer(modifier = Modifier.weight(1f))
                        appsComposable()
                        Spacer(modifier = Modifier.weight(1f))
                    }
                    AppsVerticalPosition.BOTTOM -> {
                        Spacer(modifier = Modifier.weight(1f))
                        appsComposable()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            } else { // Clock at Bottom
                when (appsPos) {
                    AppsVerticalPosition.TOP -> {
                        appsComposable()
                        Spacer(modifier = Modifier.weight(1f))
                        clockComposable()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    AppsVerticalPosition.CENTER -> {
                        Spacer(modifier = Modifier.weight(1f))
                        appsComposable()
                        Spacer(modifier = Modifier.weight(1f))
                        clockComposable()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                    AppsVerticalPosition.BOTTOM -> {
                        Spacer(modifier = Modifier.weight(1f))
                        appsComposable()
                        Spacer(modifier = Modifier.height(16.dp))
                        clockComposable()
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                }
            }
        }
    }

    // Edge Scrub App Drawer
        AlphabeticalAppDrawer(
            installedApps = installedApps,
            edgeScrubPosition = settings.edgeScrubPosition,
            edgeScrubReleaseAction = settings.edgeScrubReleaseAction,
            isOpen = overlay is LauncherOverlay.Drawer,
            onOpenChange = { viewModel.setAppDrawerOpen(it) },
            onLaunchApp = { app ->
                viewModel.closeDrawer()
                viewModel.appUsageTracker.recordAppLaunch(app.packageName)
                viewModel.launchApp(app.packageName, app.activityName, app.userHandle)
            },
            onAppLongPress = { app ->
                val screenApp = ScreenApp(
                    uniqueKey = app.uniqueKey,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    customLabel = null,
                    defaultLabel = app.label,
                    userSerialNumber = app.userSerialNumber,
                    userHandle = app.userHandle
                )
                viewModel.showAppOptions(screenApp)
            },
            onDismiss = { viewModel.closeDrawer() }
        )

        // Global Search Overlay
        AnimatedVisibility(
            visible = overlay is LauncherOverlay.Search,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(180))
        ) {
            LauncherSearchOverlay(
                query = searchQuery,
                results = searchResults,
                onQueryChange = { viewModel.onSearchQueryChanged(it) },
                onResultClick = { viewModel.launchSearchResult(it) },
                onDismiss = { viewModel.closeSearch() }
            )
        }

        // App Picker Bottom Sheet
        if (overlay is LauncherOverlay.AppPicker) {
            AppPickerSheet(
                installedApps = installedApps,
                onAppSelected = { app ->
                    currentScreen?.let {
                        viewModel.addAppToScreen(it.id, app)
                    }
                    viewModel.closeAppPicker()
                },
                onDismiss = { viewModel.closeAppPicker() }
            )
        }

        // Settings Modal
        AnimatedVisibility(
            visible = overlay is LauncherOverlay.Settings,
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180))
        ) {
            SettingsScreen(
                viewModel = viewModel,
                onDismiss = { viewModel.closeSettings() }
            )
        }

        // Material 3 Expressive App Long-Press Action Modal
        (overlay as? LauncherOverlay.AppOptions)?.app?.let { app ->
            val shortcuts = remember(app.packageName) {
                viewModel.getShortcutsForApp(app.packageName)
            }

            AppOptionsModal(
                app = app,
                shortcuts = shortcuts,
                isOnHomeScreen = currentScreen?.apps?.any { it.uniqueKey == app.uniqueKey } == true,
                onLaunchShortcut = { shortcutId, userHandle ->
                    viewModel.launchShortcut(app.packageName, shortcutId, userHandle)
                    viewModel.dismissAppOptions()
                },
                onOpenAppInfo = {
                    viewModel.openAppInfo(app.packageName, app.activityName, app.userHandle)
                    viewModel.dismissAppOptions()
                },
                onSetAlias = {
                    aliasToSet = app
                    viewModel.dismissAppOptions()
                },
                onRenameApp = {
                    appToRename = app
                    viewModel.dismissAppOptions()
                },
                onRemoveFromScreen = {
                    currentScreen?.let {
                        viewModel.removeAppFromScreen(it.id, app.uniqueKey)
                    }
                    viewModel.dismissAppOptions()
                },
                onAddToScreen = {
                    currentScreen?.let { screen ->
                        val item = installedApps.find { it.uniqueKey == app.uniqueKey }
                            ?: com.pallas.launcher.domain.model.AppItem(
                                packageName = app.packageName,
                                activityName = app.activityName,
                                label = app.defaultLabel,
                                userHandle = app.userHandle ?: android.os.Process.myUserHandle(),
                                userSerialNumber = app.userSerialNumber
                            )
                        viewModel.addAppToScreen(screen.id, item)
                    }
                    viewModel.dismissAppOptions()
                },
                onHideApp = {
                    viewModel.hideApp(app.packageName)
                    currentScreen?.let {
                        viewModel.removeAppFromScreen(it.id, app.uniqueKey)
                    }
                    viewModel.dismissAppOptions()
                },
                onUninstall = {
                    SystemActionsManager.uninstallApp(context, app.packageName, app.userHandle)
                    viewModel.dismissAppOptions()
                },
                onOpenSettings = {
                    viewModel.dismissAppOptions()
                    viewModel.openSettings()
                },
                onDismiss = {
                    viewModel.dismissAppOptions()
                }
            )
        }

        // Material 3 Expressive Alias Creation Dialog
        aliasToSet?.let { app ->
            var aliasText by remember { mutableStateOf("") }
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            val existingTarget = remember(aliasText) {
                val clean = aliasText.lowercase().trim()
                if (clean.isNotEmpty()) settings.customAliases[clean] else null
            }

            AlertDialog(
                onDismissRequest = { aliasToSet = null },
                shape = RoundedCornerShape(24.dp),
                title = { Text(text = "Set Alias for '${app.displayLabel}'", color = colorScheme.onSurface) },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(
                            text = "Type an abbreviation (e.g. 'wa', 'calc', 'm'):",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant
                        )
                        OutlinedTextField(
                            value = aliasText,
                            onValueChange = { aliasText = it },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                            keyboardActions = KeyboardActions(
                                onDone = {
                                    if (aliasText.isNotBlank()) {
                                        viewModel.setAlias(aliasText.trim(), app.packageName)
                                        aliasToSet = null
                                    }
                                }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(14.dp)
                        )
                        if (existingTarget != null && existingTarget != app.packageName) {
                            Text(
                                text = "⚠️ Overwrites existing alias pointing to '$existingTarget'",
                                fontSize = 12.sp,
                                color = colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = {
                            if (aliasText.isNotBlank()) {
                                viewModel.setAlias(aliasText.trim(), app.packageName)
                                aliasToSet = null
                            }
                        }
                    ) {
                        Text(text = "Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { aliasToSet = null }) {
                        Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = colorScheme.surfaceContainerHigh
            )
        }

        // Rename App Dialog
        appToRename?.let { app ->
            RenameAppDialog(
                initialLabel = app.displayLabel,
                onConfirm = { trimmed ->
                    val currentScreen = screens.getOrNull(currentScreenIndex) ?: screens.firstOrNull()
                    val isOnAnyScreen = screens.any { s -> s.apps.any { it.uniqueKey == app.uniqueKey } }
                    if (!isOnAnyScreen && currentScreen != null) {
                        viewModel.addAppToScreen(
                            currentScreen.id,
                            com.pallas.launcher.domain.model.AppItem(
                                packageName = app.packageName,
                                activityName = app.activityName,
                                label = app.defaultLabel,
                                userHandle = app.userHandle ?: android.os.Process.myUserHandle(),
                                userSerialNumber = app.userSerialNumber
                            ),
                            customLabel = trimmed
                        )
                    } else {
                        viewModel.updateAppLabelAcrossScreens(app.uniqueKey, trimmed)
                    }
                },
                onDismiss = { appToRename = null }
            )
        }
    }
}

