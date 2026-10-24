package com.pallas.launcher.ui.home

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.AppsVerticalPosition
import com.pallas.launcher.domain.model.ClockVerticalPosition
import com.pallas.launcher.domain.model.GestureAction
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.domain.model.TextShadowMode
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.LauncherOverlay
import com.pallas.launcher.ui.MainViewModel
import com.pallas.launcher.ui.apps.AlphabeticalAppDrawer
import com.pallas.launcher.ui.components.RenameAppDialog
import com.pallas.launcher.ui.screens.AppPickerSheet
import com.pallas.launcher.ui.search.LauncherSearchOverlay
import com.pallas.launcher.ui.settings.SettingsScreen
import com.pallas.launcher.ui.theme.LocalWindowBlurController
import kotlinx.coroutines.launch
import kotlin.math.roundToInt


@Composable
fun PallasHomeScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val configuration = LocalConfiguration.current
    val density = LocalDensity.current
    val screenWidthPx = with(density) { configuration.screenWidthDp.dp.toPx() }

    val screens by viewModel.displayScreens.collectAsState()
    val currentScreenIndex by viewModel.currentScreenIndex.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val currentTime by viewModel.currentTime.collectAsState()
    val currentDate by viewModel.currentDate.collectAsState()
    val installedApps by viewModel.installedApps.collectAsState()
    val favoriteApps by viewModel.favoriteApps.collectAsState()
    val usageScores by viewModel.appUsageScores.collectAsState()
    val isAppsLoaded by viewModel.isAppsLoaded.collectAsState()

    val overlay by viewModel.overlay.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val searchResults by viewModel.searchResults.collectAsState()

    val coroutineScope = rememberCoroutineScope()
    val pagerOffsetX = remember { Animatable(0f) }

    BackHandler(enabled = overlay !is LauncherOverlay.None || currentScreenIndex != 0) {
        if (overlay !is LauncherOverlay.None) {
            viewModel.dismissOverlay()
        } else if (currentScreenIndex != 0) {
            viewModel.setScreenIndex(0)
        }
    }

    val currentScreen = screens.getOrNull(currentScreenIndex) ?: screens.firstOrNull()
    val colorScheme = MaterialTheme.colorScheme
    val context = LocalContext.current

    val homeTextShadow: Shadow? = remember(settings.textShadowMode, settings.showWallpaperScrim) {
        when (settings.textShadowMode) {
            TextShadowMode.OFF -> null
            TextShadowMode.AUTO -> if (settings.showWallpaperScrim) {
                null
            } else {
                Shadow(
                    color = Color.Black.copy(alpha = 0.55f),
                    offset = Offset(0f, 2f),
                    blurRadius = 6f
                )
            }
            TextShadowMode.ON -> Shadow(
                color = Color.Black.copy(alpha = 0.72f),
                offset = Offset(0f, 2f),
                blurRadius = 8f
            )
        }
    }

    val isDrawerVisible = when (val currentOverlay = overlay) {
        is LauncherOverlay.Drawer -> true
        is LauncherOverlay.AppOptions -> currentOverlay.returnToDrawer
        is LauncherOverlay.SetAlias -> currentOverlay.returnToDrawer
        is LauncherOverlay.RenameApp -> currentOverlay.returnToDrawer
        else -> false
    }

    val drawerApps = remember(installedApps, settings.hiddenFromAppList) {
        installedApps.filterNot {
            settings.hiddenFromAppList.contains(it.uniqueKey) || settings.hiddenFromAppList.contains(it.packageName)
        }
    }

    Box(modifier = modifier.fillMaxSize()) {
        AlphabeticalAppDrawer(
            installedApps = drawerApps,
            favoriteApps = favoriteApps,
            usageScores = usageScores,
            edgeScrubPosition = settings.edgeScrubPosition,
            edgeScrubReleaseAction = settings.edgeScrubReleaseAction,
            isOpen = isDrawerVisible,
            fontScale = settings.appListFontScale,
            scrubEnabled = overlay is LauncherOverlay.None || overlay is LauncherOverlay.Drawer,
            onOpenChange = { viewModel.setAppDrawerOpen(it) },
            onLaunchApp = { app ->
                viewModel.closeDrawer()
                viewModel.launchApp(app.packageName, app.activityName, app.userHandle)
            },
            onAppLongPress = { app ->
                val existingOnScreen = currentScreen?.apps?.firstOrNull { it.uniqueKey == app.uniqueKey }
                val screenApp = existingOnScreen ?: ScreenApp(
                    uniqueKey = app.uniqueKey,
                    packageName = app.packageName,
                    activityName = app.activityName,
                    customLabel = null,
                    defaultLabel = app.label,
                    userSerialNumber = app.userSerialNumber,
                    userHandle = app.userHandle
                )
                viewModel.showAppOptions(screenApp, returnToDrawer = true)
            },
            onDismiss = { viewModel.closeDrawer() }
        ) {
            GestureContainer(
                onSwipeUp = { viewModel.executeGesture(settings.swipeUpAction) },
                onSwipeDown = { viewModel.executeGesture(settings.swipeDownAction) },
                onShortSwipeLeft = { viewModel.executeGesture(settings.shortSwipeLeftAction) },
                onShortSwipeRight = { viewModel.executeGesture(settings.shortSwipeRightAction) },
                onLongSwipeLeft = { viewModel.onScreenSwiped(1) },
                onLongSwipeRight = { viewModel.onScreenSwiped(-1) },
                onBackgroundLongPress = { viewModel.openSettings() },
                onDoubleTap = { viewModel.executeGesture(settings.doubleTapAction) },
                canLongSwipeHorizontal = screens.size > 1,
                onHorizontalDrag = { dragX ->
                    coroutineScope.launch {
                        pagerOffsetX.snapTo(dragX)
                    }
                },
                onHorizontalDragEnd = { isCommitLong, isLeft ->
                    coroutineScope.launch {
                        if (isCommitLong && screens.size > 1) {
                            val targetOffset = if (isLeft) -screenWidthPx else screenWidthPx
                            pagerOffsetX.animateTo(
                                targetValue = targetOffset,
                                animationSpec = tween(180, easing = FastOutSlowInEasing)
                            )
                            val newIndex = if (isLeft) {
                                (currentScreenIndex + 1) % screens.size
                            } else {
                                (currentScreenIndex - 1 + screens.size) % screens.size
                            }
                            viewModel.setScreenIndex(newIndex)
                            pagerOffsetX.snapTo(0f)
                        } else {
                            pagerOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                            if (isLeft) {
                                viewModel.executeGesture(settings.shortSwipeLeftAction)
                            } else {
                                viewModel.executeGesture(settings.shortSwipeRightAction)
                            }
                        }
                    }
                },
                onHorizontalDragCancel = {
                    coroutineScope.launch {
                        pagerOffsetX.animateTo(0f, spring(stiffness = Spring.StiffnessMediumLow))
                    }
                },
                modifier = Modifier.fillMaxSize(),
                enabled = overlay is LauncherOverlay.None
            ) {
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

                val statusBarTopPadding = if (settings.hideStatusBar) 0.dp else WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
                val navBarBottomPadding = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()

                val isDrawerOrSearchActive = isDrawerVisible || overlay is LauncherOverlay.Search
                val homeAlpha by animateFloatAsState(
                    targetValue = if (isDrawerOrSearchActive) 0f else 1f,
                    animationSpec = if (isDrawerOrSearchActive) {
                        tween(120)
                    } else {
                        spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                    },
                    label = "home_content_alpha"
                )
                val blurFraction by animateFloatAsState(
                    targetValue = if (isDrawerOrSearchActive) 1f else 0f,
                    animationSpec = if (isDrawerOrSearchActive) {
                        spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)
                    } else {
                        tween(120)
                    },
                    label = "window_blur_radius"
                )
                val windowBlurController = LocalWindowBlurController.current
                LaunchedEffect(blurFraction) {
                    windowBlurController((blurFraction * 40f).roundToInt())
                }

                val isClockTop = settings.clockVerticalPosition == ClockVerticalPosition.TOP
                val appsPos = settings.appsVerticalPosition

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            alpha = homeAlpha
                        }
                        .padding(top = statusBarTopPadding, bottom = navBarBottomPadding)
                ) {
                    val clockComposable: @Composable () -> Unit = {
                        if (settings.showClock) {
                            ClockHeader(
                                timeString = currentTime,
                                dateString = currentDate,
                                alignment = settings.clockHorizontalAlignment,
                                showDate = settings.showDate,
                                onClockClick = { viewModel.openClock() },
                                onDateClick = { viewModel.openCalendar() },
                                fontScale = settings.appFontScale,
                                paddingLeftDp = settings.clockPaddingLeftDp,
                                paddingRightDp = settings.clockPaddingRightDp,
                                paddingTopDp = settings.clockPaddingTopDp,
                                paddingBottomDp = settings.clockPaddingBottomDp,
                                shadow = homeTextShadow
                            )
                        }
                    }

                    val appsAlignment = when (appsPos) {
                        AppsVerticalPosition.TOP -> Alignment.TopCenter
                        AppsVerticalPosition.CENTER -> Alignment.Center
                        AppsVerticalPosition.BOTTOM -> Alignment.BottomCenter
                    }

                    val appsComposable: @Composable () -> Unit = {
                        BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                            val screenWidthPx = constraints.maxWidth.toFloat().coerceAtLeast(1f)
                            val currentOffset = pagerOffsetX.value

                            if (screens.isEmpty()) return@BoxWithConstraints

                            val currScreen = screens.getOrNull(currentScreenIndex) ?: screens.first()
                            val currAllApps = currScreen.apps
                            val currVisibleApps = remember(currAllApps, installedApps, isAppsLoaded) {
                                if (!isAppsLoaded) currAllApps.filterNot { it.isProfileRemoved }
                                else {
                                    val installedByKey = installedApps.associateBy { it.uniqueKey }
                                    currAllApps.mapNotNull { app ->
                                        if (app.isProfileRemoved) null
                                        else {
                                            val installed = installedByKey[app.uniqueKey] ?: return@mapNotNull null
                                            if (app.customLabel.isNullOrBlank() && installed.label != app.defaultLabel) {
                                                app.copy(defaultLabel = installed.label)
                                            } else app
                                        }
                                    }
                                }
                            }

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .offset { IntOffset(currentOffset.roundToInt(), 0) },
                                contentAlignment = appsAlignment
                            ) {
                                AppListView(
                                    apps = currVisibleApps,
                                    alignment = settings.appsHorizontalAlignment,
                                    fontScale = settings.screensFontScale,
                                    paddingLeftDp = settings.screensPaddingLeftDp,
                                    paddingRightDp = settings.screensPaddingRightDp,
                                    paddingTopDp = settings.screensPaddingTopDp,
                                    paddingBottomDp = settings.screensPaddingBottomDp,
                                    appSpacingDp = settings.screensAppSpacingDp,
                                    isLoaded = isAppsLoaded,
                                    shadow = homeTextShadow,
                                    onAppClick = { app ->
                                        if (!app.isProfileRemoved) viewModel.launchApp(app.packageName, app.activityName, app.userHandle)
                                    },
                                    onAppLongClick = { app ->
                                        viewModel.showAppOptions(app, returnToDrawer = false)
                                    },
                                    onEmptyScreenClick = {
                                        viewModel.openAppPicker()
                                    }
                                )
                            }

                            if (screens.size > 1 && currentOffset != 0f) {
                                val adjacentIndex = if (currentOffset < 0) {
                                    (currentScreenIndex + 1) % screens.size
                                } else {
                                    (currentScreenIndex - 1 + screens.size) % screens.size
                                }
                                val adjScreen = screens[adjacentIndex]
                                val adjAllApps = adjScreen.apps
                                val adjVisibleApps = remember(adjAllApps, installedApps, isAppsLoaded) {
                                    if (!isAppsLoaded) adjAllApps.filterNot { it.isProfileRemoved }
                                    else {
                                        val installedByKey = installedApps.associateBy { it.uniqueKey }
                                        adjAllApps.mapNotNull { app ->
                                            if (app.isProfileRemoved) null
                                            else {
                                                val installed = installedByKey[app.uniqueKey] ?: return@mapNotNull null
                                                if (app.customLabel.isNullOrBlank() && installed.label != app.defaultLabel) {
                                                    app.copy(defaultLabel = installed.label)
                                                } else app
                                            }
                                        }
                                    }
                                }
                                val adjacentBaseX = if (currentOffset < 0) screenWidthPx else -screenWidthPx
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .offset { IntOffset((adjacentBaseX + currentOffset).roundToInt(), 0) },
                                    contentAlignment = appsAlignment
                                ) {
                                    AppListView(
                                        apps = adjVisibleApps,
                                        alignment = settings.appsHorizontalAlignment,
                                        fontScale = settings.screensFontScale,
                                        paddingLeftDp = settings.screensPaddingLeftDp,
                                        paddingRightDp = settings.screensPaddingRightDp,
                                        paddingTopDp = settings.screensPaddingTopDp,
                                        paddingBottomDp = settings.screensPaddingBottomDp,
                                        appSpacingDp = settings.screensAppSpacingDp,
                                        isLoaded = isAppsLoaded,
                                        shadow = homeTextShadow,
                                        onAppClick = { app ->
                                            if (!app.isProfileRemoved) viewModel.launchApp(app.packageName, app.activityName, app.userHandle)
                                        },
                                        onAppLongClick = { app ->
                                            viewModel.showAppOptions(app, returnToDrawer = false)
                                        },
                                        onEmptyScreenClick = {
                                            viewModel.openAppPicker()
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (isClockTop) clockComposable()
                    Box(
                        modifier = Modifier.weight(1f).fillMaxWidth()
                    ) { appsComposable() }
                    if (!isClockTop) {
                        clockComposable()
                        Spacer(Modifier.height(24.dp))
                    }
                }
            }
        }

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
                onAppLongPress = { app ->
                    val existingOnScreen = currentScreen?.apps?.firstOrNull { it.uniqueKey == app.uniqueKey }
                    val screenApp = existingOnScreen ?: ScreenApp(
                        uniqueKey = app.uniqueKey,
                        packageName = app.packageName,
                        activityName = app.activityName,
                        customLabel = null,
                        defaultLabel = app.label,
                        userSerialNumber = app.userSerialNumber,
                        userHandle = app.userHandle
                    )
                    viewModel.closeSearch()
                    viewModel.showAppOptions(screenApp, returnToDrawer = false)
                },
                onDismiss = { viewModel.closeSearch() }
            )
        }

        if (overlay is LauncherOverlay.AppPicker) {
            val alreadyAddedKeys = remember(currentScreen?.apps) {
                currentScreen?.apps?.map { it.uniqueKey }?.toSet() ?: emptySet()
            }
            AppPickerSheet(
                installedApps = installedApps,
                alreadyAddedKeys = alreadyAddedKeys,
                onAppsConfirmed = { apps ->
                    currentScreen?.let {
                        viewModel.addAppsToScreen(it.id, apps)
                    }
                    viewModel.closeAppPicker()
                },
                onDismiss = { viewModel.closeAppPicker() }
            )
        }

        AnimatedVisibility(
            visible = overlay is LauncherOverlay.Settings,
            enter = slideInHorizontally(tween(220)) { it } + fadeIn(tween(180)),
            exit = slideOutHorizontally(tween(220)) { it } + fadeOut(tween(180))
        ) {
            SettingsScreen(
                onDismiss = { viewModel.closeSettings() }
            )
        }

        (overlay as? LauncherOverlay.AppOptions)?.app?.let { app ->
            val shortcuts = remember(app.packageName, app.userHandle) {
                viewModel.getShortcutsForApp(app.packageName, app.userHandle)
            }

            AppOptionsModal(
                app = app,
                shortcuts = shortcuts,
                isOnHomeScreen = currentScreen?.apps?.any { it.uniqueKey == app.uniqueKey } == true,
                onLaunchShortcut = { shortcutId, userHandle ->
                    viewModel.launchShortcut(app.packageName, shortcutId, userHandle)
                    viewModel.dismissAppOptions(forceCloseAll = true)
                },
                onOpenAppInfo = {
                    viewModel.openAppInfo(app.packageName, app.activityName, app.userHandle)
                    viewModel.dismissAppOptions(forceCloseAll = true)
                },
                onSetAlias = {
                    viewModel.openSetAlias(app)
                },
                onRenameApp = {
                    viewModel.openRenameApp(app)
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
                            ?: AppItem(
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
                onHideFromAppList = {
                    viewModel.hideFromAppList(app.uniqueKey)
                    viewModel.dismissAppOptions()
                },
                onHideApp = {
                    viewModel.hideApp(app.uniqueKey)
                    viewModel.removeAppFromAllScreens(app.uniqueKey)
                    viewModel.dismissAppOptions()
                },
                onUninstall = {
                    SystemActionsManager.uninstallApp(context, app.packageName, app.userHandle)
                    viewModel.dismissAppOptions(forceCloseAll = true)
                },
                onOpenSettings = {
                    viewModel.openSettings()
                },
                onDismiss = {
                    viewModel.dismissAppOptions()
                }
            )
        }

        (overlay as? LauncherOverlay.SetAlias)?.app?.let { app ->
            val currentAliasForApp = remember(app.uniqueKey, app.packageName, settings.customAliases) {
                settings.customAliases.entries.firstOrNull { (_, target) ->
                    target == app.uniqueKey || target == app.packageName
                }?.key.orEmpty()
            }
            var aliasText by remember(app.uniqueKey) { mutableStateOf(currentAliasForApp) }
            val focusRequester = remember { FocusRequester() }
            val keyboardController = LocalSoftwareKeyboardController.current

            LaunchedEffect(Unit) {
                kotlinx.coroutines.delay(100)
                focusRequester.requestFocus()
                keyboardController?.show()
            }

            val existingTarget = remember(aliasText, settings.customAliases) {
                val clean = aliasText.lowercase().trim()
                if (clean.isNotEmpty()) settings.customAliases[clean] else null
            }
            val existingTargetLabel = remember(existingTarget, installedApps) {
                if (existingTarget == null) null
                else installedApps.firstOrNull {
                    it.uniqueKey == existingTarget || it.packageName == existingTarget
                }?.label ?: existingTarget.substringBefore('/').substringAfterLast('.')
            }

            val saveAlias = {
                val clean = aliasText.trim()
                if (clean.isNotBlank()) {
                    if (currentAliasForApp.isNotEmpty() && !currentAliasForApp.equals(clean, ignoreCase = true)) {
                        viewModel.removeAlias(currentAliasForApp)
                    }
                    viewModel.setAlias(clean, app.uniqueKey)
                } else if (currentAliasForApp.isNotEmpty()) {
                    viewModel.removeAlias(currentAliasForApp)
                }
                viewModel.dismissOverlay()
            }

            AlertDialog(
                onDismissRequest = { viewModel.dismissOverlay() },
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
                                onDone = { saveAlias() }
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .focusRequester(focusRequester),
                            shape = RoundedCornerShape(14.dp)
                        )
                        if (existingTarget != null && existingTarget != app.uniqueKey && existingTarget != app.packageName) {
                            Text(
                                text = "⚠️ Overwrites existing alias pointing to '${existingTargetLabel ?: existingTarget}'",
                                fontSize = 12.sp,
                                color = colorScheme.error
                            )
                        }
                    }
                },
                confirmButton = {
                    TextButton(
                        onClick = { saveAlias() }
                    ) {
                        Text(text = "Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                    }
                },
                dismissButton = {
                    TextButton(onClick = { viewModel.dismissOverlay() }) {
                        Text(text = "Cancel", color = colorScheme.onSurfaceVariant)
                    }
                },
                containerColor = colorScheme.surfaceContainerHigh
            )
        }

        (overlay as? LauncherOverlay.RenameApp)?.app?.let { app ->
            RenameAppDialog(
                initialLabel = app.displayLabel,
                onConfirm = { trimmed ->
                    viewModel.updateAppLabelAcrossScreens(app.uniqueKey, trimmed)
                    viewModel.dismissOverlay()
                },
                onDismiss = { viewModel.dismissOverlay() }
            )
        }
    }
}