package com.pallas.launcher.ui.settings

import android.app.Application
import android.content.Context
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.pallas.launcher.data.repository.AppRepository
import com.pallas.launcher.data.repository.ScreenRepository
import com.pallas.launcher.data.repository.SettingsBackup
import com.pallas.launcher.data.repository.SettingsRepository
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.HomeScreen
import com.pallas.launcher.domain.model.LauncherSettings
import com.pallas.launcher.domain.model.ScreenApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.withContext

/**
 * Dedicated ViewModel for launcher settings, screens management, backups, and secondary configuration.
 * Decouples settings management from [com.pallas.launcher.ui.MainViewModel].
 */
class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val context = application.applicationContext
    val settingsRepository = SettingsRepository.getInstance(context)
    val screenRepository = ScreenRepository.getInstance(context)
    val appRepository = AppRepository(context, viewModelScope, settingsRepository)
    private val uiPrefs = context.getSharedPreferences("ui_state", Context.MODE_PRIVATE)

    val settings: StateFlow<LauncherSettings> = settingsRepository.settings
    val screens: StateFlow<List<HomeScreen>> = screenRepository.screens
    val installedApps: StateFlow<List<AppItem>> = appRepository.installedApps

    val currentScreenIndex: StateFlow<Int> = combine(
        screenRepository.screens,
        settingsRepository.settings
    ) { screenList, _ ->
        val id = uiPrefs.getString("current_screen_id", null)
        val idx = if (id != null) screenList.indexOfFirst { it.id == id } else 0
        if (screenList.isEmpty()) 0 else idx.coerceIn(0, screenList.size - 1)
    }.stateIn(viewModelScope, SharingStarted.Eagerly, 0)

    fun setScreenIndex(index: Int) {
        val id = screens.value.getOrNull(index)?.id ?: return
        uiPrefs.edit().putString("current_screen_id", id).apply()
    }

    fun updateSettings(transform: LauncherSettings.() -> LauncherSettings) =
        settingsRepository.update(transform)

    fun addScreen(name: String, contextTag: String?) = screenRepository.addScreen(name, contextTag)
    fun renameScreen(id: String, name: String, contextTag: String?) = screenRepository.renameScreen(id, name, contextTag)
    fun removeScreen(id: String) = screenRepository.removeScreen(id)
    fun moveScreen(fromIndex: Int, toIndex: Int) = screenRepository.moveScreen(fromIndex, toIndex)
    fun reorderApps(screenId: String, apps: List<ScreenApp>) = screenRepository.reorderApps(screenId, apps)
    fun removeAppFromScreen(screenId: String, uniqueKey: String) = screenRepository.removeAppFromScreen(screenId, uniqueKey)
    fun updateAppLabel(screenId: String, uniqueKey: String, newLabel: String) =
        screenRepository.updateAppLabel(screenId, uniqueKey, newLabel, settings.value.nameLengthSortOrder)
    fun addAppsToScreen(screenId: String, apps: List<AppItem>) =
        screenRepository.addAppsToScreen(screenId, apps, settings.value.nameLengthSortOrder)
    fun sortedForDisplay(apps: List<ScreenApp>): List<ScreenApp> =
        screenRepository.sortAppsByNameLength(apps, settings.value.nameLengthSortOrder)

    fun unhideApp(uniqueKey: String) = settingsRepository.unhideApp(uniqueKey)
    fun unhideFromAppList(uniqueKey: String) = settingsRepository.unhideFromAppList(uniqueKey)

    fun setAlias(alias: String, target: String) = settingsRepository.setAlias(alias, target)
    fun removeAlias(alias: String) = settingsRepository.removeAlias(alias)
    fun addSearchContact(nickname: String, name: String, phoneNumber: String) =
        settingsRepository.addSearchContact(nickname, name, phoneNumber)
    fun removeSearchContact(id: String) = settingsRepository.removeSearchContact(id)

    fun updateCommandConfig(id: String, enabled: Boolean, prefix: String) {
        updateSettings {
            val existing = commandConfigs[id] ?: CommandConfig.DEFAULTS[id]
                ?: CommandConfig(id, id, enabled, prefix)
            copy(commandConfigs = commandConfigs + (id to existing.copy(enabled = enabled, prefix = prefix.trim())))
        }
    }

    sealed interface ImportOutcome {
        data class Success(val screens: Int, val apps: Int) : ImportOutcome
        data class Failure(val message: String) : ImportOutcome
    }

    suspend fun buildBackupJson(): String = withContext(Dispatchers.Default) {
        SettingsBackup.export(settings.value, screens.value, screenRepository)
    }

    suspend fun previewBackup(text: String): SettingsBackup.ParseResult = withContext(Dispatchers.Default) {
        SettingsBackup.parse(text)
    }

    suspend fun applyBackup(text: String): ImportOutcome = withContext(Dispatchers.Default) {
        when (val result = SettingsBackup.parse(text)) {
            is SettingsBackup.ParseResult.Error -> ImportOutcome.Failure(result.message)
            is SettingsBackup.ParseResult.Ok -> {
                val parsed = result.parsed
                settingsRepository.replaceAll(parsed.settings)
                parsed.screensJson?.let { screenRepository.replaceAll(screenRepository.parseScreens(it)) }
                ImportOutcome.Success(parsed.screenCount, parsed.appCount)
            }
        }
    }

    override fun onCleared() {
        appRepository.unregister()
        super.onCleared()
    }
}
