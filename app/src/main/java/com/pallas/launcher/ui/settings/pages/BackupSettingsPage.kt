package com.pallas.launcher.ui.settings.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pallas.launcher.data.repository.SettingsBackup
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val MAX_BACKUP_BYTES = 2_000_000

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var pendingText by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingSummary by rememberSaveable { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = try {
                val json = viewModel.buildBackupJson()
                withContext(Dispatchers.IO) {
                    val stream = try {
                        context.contentResolver.openOutputStream(uri, "wt")
                    } catch (_: IllegalArgumentException) {
                        null
                    } catch (_: UnsupportedOperationException) {
                        null
                    } ?: context.contentResolver.openOutputStream(uri)
                    stream?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
                }
            } catch (_: Exception) {
                false
            }
            snackbar.showSnackbar(if (ok) "Backup saved" else "Could not write the file")
        }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            var tooLarge = false
            val text = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        val out = ByteArrayOutputStream()
                        val chunk = ByteArray(8192)
                        var total = 0
                        while (true) {
                            val r = input.read(chunk)
                            if (r < 0) break
                            total += r
                            if (total > MAX_BACKUP_BYTES) {
                                tooLarge = true
                                return@use null
                            }
                            out.write(chunk, 0, r)
                        }
                        out.toString(Charsets.UTF_8.name())
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (tooLarge) {
                snackbar.showSnackbar("Backup file is too large (max 2 MB)")
                return@launch
            }
            if (text == null) {
                snackbar.showSnackbar("Could not read that file")
                return@launch
            }
            when (val r = viewModel.previewBackup(text)) {
                is SettingsBackup.ParseResult.Error -> snackbar.showSnackbar(r.message)
                is SettingsBackup.ParseResult.Ok -> {
                    val p = r.parsed
                    pendingSummary = if (p.screenCount > 0) {
                        "This backup contains ${p.screenCount} home screen(s) with ${p.appCount} app(s), plus all settings. Your current settings and home screens will be overwritten."
                    } else {
                        "This backup contains all settings and no home screens. Your current settings will be overwritten, while your existing home screens will remain unchanged."
                    }
                    pendingText = text
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbar) },
        topBar = { SettingsLargeTopBar(title = "Backup & Restore", scrollBehavior = scrollBehavior, onBack = onBack) }
    ) { inner ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(inner)) {
            item {
                SettingsSectionHeader("Backup")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Export to JSON",
                        subtitle = "Settings, home screens, aliases and call nicknames",
                        icon = Icons.Default.Share,
                        onClick = {
                            val stamp = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
                            exportLauncher.launch("pallas-launcher-$stamp.json")
                        }
                    )
                    SettingsNavRow(
                        title = "Import from JSON",
                        subtitle = "Replaces your current setup",
                        icon = Icons.Default.Save,
                        onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }
                    )
                }
                Text(
                    text = "App usage history stays on this device and is not included. Apps that are not installed on the new device simply stay hidden until they are.",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    pendingText?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingText = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Replace current setup?", style = MaterialTheme.typography.titleLarge) },
            text = { Text(pendingSummary, color = colorScheme.onSurfaceVariant) },
            confirmButton = {
                TextButton(onClick = {
                    pendingText = null
                    scope.launch {
                        when (val o = viewModel.applyBackup(text)) {
                            is SettingsViewModel.ImportOutcome.Success -> snackbar.showSnackbar("Backup restored")
                            is SettingsViewModel.ImportOutcome.Failure -> snackbar.showSnackbar(o.message)
                        }
                    }
                }) { Text("Replace", color = colorScheme.primary, fontWeight = FontWeight.Bold) }
            },
            dismissButton = {
                TextButton(onClick = { pendingText = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
