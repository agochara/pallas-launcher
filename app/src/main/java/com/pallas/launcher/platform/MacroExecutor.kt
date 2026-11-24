package com.pallas.launcher.platform

import android.app.NotificationManager
import android.content.Context
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.QuickCaptureRepository
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.FocusDndMode
import com.pallas.launcher.domain.model.MacroRoutine
import com.pallas.launcher.domain.model.MacroStep
import com.pallas.launcher.domain.model.MacroToggleMode
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay

object MacroExecutor {

    sealed interface OverlayCommand {
        data object Notebook : OverlayCommand
        data object LatexStudio : OverlayCommand
        data object Atlas : OverlayCommand
        data object QuickCapture : OverlayCommand
        data class SwitchScreen(val screenId: String) : OverlayCommand
    }

    suspend fun execute(
        context: Context,
        macro: MacroRoutine,
        settingsRepository: SettingsRepository,
        appRepository: AppRepository,
        quickCaptureRepository: QuickCaptureRepository,
        allowedKeys: Set<String>?,
        onOverlay: (OverlayCommand) -> Unit
    ) {
        for (step in macro.steps) {
            val s = settingsRepository.settings.value
            try {
                when (step) {
                    is MacroStep.SetVolume -> SystemActionsManager.setStreamVolumePercent(context, step.stream, step.levelPercent)
                    is MacroStep.SetRingerMode -> SystemActionsManager.setRingerMode(context, step.mode)
                    is MacroStep.MediaControl -> SystemActionsManager.dispatchMediaAction(context, step.action)
                    is MacroStep.SetDnd -> {
                        val filter = when (step.dndMode) {
                            FocusDndMode.PRIORITY -> NotificationManager.INTERRUPTION_FILTER_PRIORITY
                            FocusDndMode.ALARMS_ONLY -> NotificationManager.INTERRUPTION_FILTER_ALARMS
                            FocusDndMode.TOTAL_SILENCE -> NotificationManager.INTERRUPTION_FILTER_NONE
                        }
                        when (step.mode) {
                            MacroToggleMode.ON -> SystemActionsManager.setDnd(context, true, filter, showToast = false)
                            MacroToggleMode.OFF -> SystemActionsManager.setDnd(context, false, filter, showToast = false)
                            MacroToggleMode.TOGGLE -> {
                                val nextEnabled = !SystemActionsManager.isDndEnabled(context)
                                SystemActionsManager.setDnd(context, nextEnabled, filter, showToast = false)
                            }
                        }
                    }
                    is MacroStep.SetFlashlight -> when (step.mode) {
                        MacroToggleMode.ON -> SystemActionsManager.setTorch(context, true, showToast = false)
                        MacroToggleMode.OFF -> SystemActionsManager.setTorch(context, false, showToast = false)
                        MacroToggleMode.TOGGLE -> SystemActionsManager.toggleTorch(context)
                    }
                    is MacroStep.SetBrightness -> SystemActionsManager.setScreenBrightnessPercent(context, step.levelPercent)
                    is MacroStep.SetAutoBrightness -> SystemActionsManager.setAutoBrightness(context, step.enabled)
                    is MacroStep.SetAutoRotate -> SystemActionsManager.setAutoRotate(context, step.enabled)
                    is MacroStep.SetScreenTimeout -> SystemActionsManager.setScreenTimeoutSeconds(context, step.seconds)
                    is MacroStep.OpenQuickPanel -> SystemActionsManager.openQuickPanel(context, step.panel)
                    is MacroStep.OpenSystemSetting -> SystemActionsManager.openSystemSettingTarget(context, step.target)

                    // Pallas Launcher Settings
                    is MacroStep.SetThemeMode -> settingsRepository.update { copy(themeMode = step.mode) }
                    is MacroStep.SetHideStatusBar -> settingsRepository.update { copy(hideStatusBar = step.hidden) }
                    is MacroStep.SetShowClock -> settingsRepository.update { copy(showClock = step.show) }
                    is MacroStep.SetShowDate -> settingsRepository.update { copy(showDate = step.show) }
                    is MacroStep.SetShowBattery -> settingsRepository.update { copy(showBattery = step.show) }
                    is MacroStep.SetWallpaperScrim -> settingsRepository.update { copy(showWallpaperScrim = step.enabled) }
                    is MacroStep.SetEdgeScrubPosition -> settingsRepository.update { copy(edgeScrubPosition = step.position) }
                    is MacroStep.SetAutoLaunchSingleMatch -> settingsRepository.update { copy(autoLaunchSingleMatch = step.enabled) }
                    is MacroStep.SwitchHomeScreen -> onOverlay(OverlayCommand.SwitchScreen(step.screenId))

                    // Productivity & Utilities
                    is MacroStep.StartTimer -> {
                        SystemActionsManager.setTimer(
                            context = context,
                            durationSeconds = step.durationSeconds,
                            message = step.label.ifBlank { macro.name },
                            skipUi = true
                        )
                    }
                    is MacroStep.SetAlarm -> SystemActionsManager.setAlarm(context, step.hour, step.minute)
                    is MacroStep.AppendQuickNote -> quickCaptureRepository.appendNote(step.text)
                    is MacroStep.WaitDelay -> delay(step.seconds.coerceIn(1, 60) * 1000L)
                    is MacroStep.LaunchApp -> {
                        val pkg = step.uniqueKey.substringBefore('/')
                        val allowed = allowedKeys == null ||
                            step.uniqueKey in allowedKeys ||
                            pkg in allowedKeys ||
                            allowedKeys.any { it.substringBefore('/') == pkg }
                        if (allowed) {
                            val apps = appRepository.installedApps.value
                            val app = apps.firstOrNull { it.uniqueKey == step.uniqueKey }
                                ?: apps.firstOrNull { it.packageName == pkg }
                            if (app != null) {
                                appRepository.launchApp(app.packageName, app.activityName, app.userHandle)
                            } else {
                                appRepository.launchApp(pkg, null, null)
                            }
                        }
                    }
                    is MacroStep.OpenNotebook -> onOverlay(OverlayCommand.Notebook)
                    is MacroStep.OpenLatexStudio -> onOverlay(OverlayCommand.LatexStudio)
                    is MacroStep.OpenAtlas -> onOverlay(OverlayCommand.Atlas)
                    is MacroStep.OpenQuickCapture -> onOverlay(OverlayCommand.QuickCapture)
                    is MacroStep.OpenUrl -> SystemActionsManager.openUrl(context, step.url, s.preferredBrowserPackage)
                }
            } catch (ce: CancellationException) {
                throw ce
            } catch (_: Exception) {
                // Continue executing remaining steps if an individual step fails
            }
        }
    }
}
