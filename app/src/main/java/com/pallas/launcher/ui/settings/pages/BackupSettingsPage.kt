package com.pallas.launcher.ui.settings.pages

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
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
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BackupSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbar = remember { SnackbarHostState() }
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var pendingText by remember { mutableStateOf<String?>(null) }
    var pendingSummary by remember { mutableStateOf("") }

    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri: Uri? ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            val ok = try {
                val json = viewModel.buildBackupJson()
                withContext(Dispatchers.IO) {
                    context.contentResolver.openOutputStream(uri, "wt")?.use { it.write(json.toByteArray(Charsets.UTF_8)) } != null
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
            val text = try {
                withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)?.use { input ->
                        // A backup is tiny; refuse anything absurd instead of reading it into memory.
                        val buf = ByteArray(2_000_001)
                        var n = 0
                        while (n < buf.size) {
                            val r = input.read(buf, n, buf.size - n)
                            if (r < 0) break
                            n += r
                        }
                        if (n > 2_000_000) null else String(buf, 0, n, Charsets.UTF_8)
                    }
                }
            } catch (_: Exception) {
                null
            }
            if (text == null) {
                snackbar.showSnackbar("Could not read that file")
                return@launch
            }
            when (val r = viewModel.previewBackup(text)) {
                is SettingsBackup.ParseResult.Error -> snackbar.showSnackbar(r.message)
                is SettingsBackup.ParseResult.Ok -> {
                    pendingSummary = "${r.parsed.screenCount} home screen(s) with ${r.parsed.appCount} app(s), plus all settings."
                    pendingText = text
                }
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize().nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
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
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 12.dp)
                )
                Spacer(Modifier.height(32.dp))
            }
        }
    }

    pendingText?.let { text ->
        AlertDialog(
            onDismissRequest = { pendingText = null },
            title = { Text("Replace current setup?") },
            text = { Text("This backup contains $pendingSummary Your current settings and home screens will be overwritten.") },
            confirmButton = {
                TextButton(onClick = {
                    pendingText = null
                    scope.launch {
                        when (val o = viewModel.applyBackup(text)) {
                            is SettingsViewModel.ImportOutcome.Success -> snackbar.showSnackbar("Backup restored")
                            is SettingsViewModel.ImportOutcome.Failure -> snackbar.showSnackbar(o.message)
                        }
                    }
                }) { Text("Replace") }
            },
            dismissButton = { TextButton(onClick = { pendingText = null }) { Text("Cancel") } }
        )
    }
}
