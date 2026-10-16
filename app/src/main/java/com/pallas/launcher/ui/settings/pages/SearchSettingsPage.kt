package com.pallas.launcher.ui.settings.pages

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.ContactsContract
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContactPhone
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.CommandConfig
import com.pallas.launcher.domain.model.WebSearchEngine
import com.pallas.launcher.platform.BrowserHelper
import com.pallas.launcher.platform.BrowserInfo
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader
import com.pallas.launcher.ui.settings.components.SettingsSegmentedRow
import com.pallas.launcher.ui.settings.components.SettingsSwitchRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchSettingsPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var showBrowserDialog by remember { mutableStateOf(false) }
    var browsers by remember { mutableStateOf(emptyList<BrowserInfo>()) }
    val browserLabel = remember(settings.preferredBrowserPackage) {
        BrowserHelper.labelFor(context, settings.preferredBrowserPackage) ?: "System default"
    }

    if (showBrowserDialog) {
        AlertDialog(
            onDismissRequest = { showBrowserDialog = false },
            title = { Text("Open searches in") },
            text = {
                Column {
                    TextButton(onClick = { viewModel.updateSettings { copy(preferredBrowserPackage = null) }; showBrowserDialog = false }) { Text("System default") }
                    browsers.forEach { b ->
                        TextButton(onClick = { viewModel.updateSettings { copy(preferredBrowserPackage = b.packageName) }; showBrowserDialog = false }) { Text(b.label) }
                    }
                }
            },
            confirmButton = { TextButton(onClick = { showBrowserDialog = false }) { Text("Close") } }
        )
    }

    var showAddAliasDialog by remember { mutableStateOf(false) }
    var newAliasKey by remember { mutableStateOf("") }
    var newAliasTarget by remember { mutableStateOf("") }

    // Smart Commands state
    var showWeatherCityDialog by remember { mutableStateOf(false) }
    var weatherCityInput by remember { mutableStateOf("") }
    var showWolframDialog by remember { mutableStateOf(false) }
    var wolframInput by remember { mutableStateOf("") }
    var editingConfig by remember { mutableStateOf<CommandConfig?>(null) }
    var editPrefixInput by remember { mutableStateOf("") }

    // Contacts state
    var showAddContactDialog by remember { mutableStateOf(false) }
    var contactNickname by remember { mutableStateOf("") }
    var contactName by remember { mutableStateOf("") }
    var contactNumber by remember { mutableStateOf("") }

    val pickContactLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val contactUri: Uri? = result.data?.data
            if (contactUri != null) {
                try {
                    val projection = arrayOf(
                        ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                        ContactsContract.CommonDataKinds.Phone.NUMBER
                    )
                    context.contentResolver.query(contactUri, projection, null, null, null)?.use { cursor ->
                        if (cursor.moveToFirst()) {
                            val nameIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                            val numIdx = cursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                            val name = if (nameIdx != -1) cursor.getString(nameIdx) ?: "" else ""
                            val number = if (numIdx != -1) cursor.getString(numIdx) ?: "" else ""
                            contactName = name
                            contactNumber = number
                            // Suggest first word or name as initial nickname
                            contactNickname = name.trim().split("\\s+".toRegex()).firstOrNull() ?: name
                            showAddContactDialog = true
                        }
                    }
                } catch (_: Exception) {
                    showAddContactDialog = true
                }
            }
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            SettingsLargeTopBar(title = "Search", scrollBehavior = scrollBehavior, onBack = onBack)
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // ------------------------------------------------------------- Section: Search Behavior
            item {
                SettingsSectionHeader("Search Behavior")
                SettingsGroup {
                    SettingsSwitchRow(
                        title = "Open single match automatically",
                        subtitle = "Launch the app as soon as only one confident match is left",
                        checked = settings.autoLaunchSingleMatch,
                        onCheckedChange = { viewModel.updateSettings { copy(autoLaunchSingleMatch = it) } }
                    )
                }
            }

            // ------------------------------------------------------------- Section: Web search
            item {
                SettingsSectionHeader("Web Search")
                SettingsGroup {
                    SettingsSegmentedRow(
                        title = "Search engine",
                        options = WebSearchEngine.entries,
                        selected = settings.webSearchEngine,
                        onSelected = { viewModel.updateSettings { copy(webSearchEngine = it) } },
                        label = { it.label }
                    )
                    SettingsNavRow(
                        title = "Browser",
                        subtitle = browserLabel,
                        onClick = {
                            browsers = BrowserHelper.listBrowsers(context)
                            showBrowserDialog = true
                        }
                    )
                }
                Text(
                    text = "Type 'q <words>' in search to look something up. It never clutters normal results.",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 6.dp)
                )
            }

            // ------------------------------------------------------------- Section: Smart Commands
            item {
                SettingsSectionHeader("Smart Commands")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Default Weather & Solar City",
                        subtitle = settings.defaultWeatherCity.ifBlank { "Not set (pass city inline)" },
                        onClick = {
                            weatherCityInput = settings.defaultWeatherCity
                            showWeatherCityDialog = true
                        }
                    )
                    SettingsNavRow(
                        title = "Wolfram Alpha AppID",
                        subtitle = if (settings.wolframAppId.isBlank()) "Not configured" else "Configured",
                        onClick = {
                            wolframInput = settings.wolframAppId
                            showWolframDialog = true
                        }
                    )

                    settings.commandConfigs.values.forEach { config ->
                        Surface(
                            color = colorScheme.surfaceContainerHigh,
                            shape = RoundedCornerShape(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 12.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = config.label,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(end = 12.dp)
                                )
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                                ) {
                                    Surface(
                                        shape = RoundedCornerShape(8.dp),
                                        color = colorScheme.surfaceContainerHighest,
                                        modifier = Modifier.clickable {
                                            editingConfig = config
                                            editPrefixInput = config.prefix
                                        }
                                    ) {
                                        Text(
                                            text = config.prefix,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                    Switch(
                                        checked = config.enabled,
                                        onCheckedChange = { isChecked ->
                                            viewModel.updateCommandConfig(config.id, isChecked, config.prefix)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // ------------------------------------------------------------- Section: Contact Nicknames
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Contact Nicknames",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = {
                            contactNickname = ""
                            contactName = ""
                            contactNumber = ""
                            showAddContactDialog = true
                        },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Contact",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "Assign nicknames to call contacts directly from search using 'call <nickname>' (e.g. 'call Mom'):",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = {
                            try {
                                val pickIntent = Intent(Intent.ACTION_PICK, ContactsContract.CommonDataKinds.Phone.CONTENT_URI)
                                pickContactLauncher.launch(pickIntent)
                            } catch (_: Exception) {
                                contactNickname = ""
                                contactName = ""
                                contactNumber = ""
                                showAddContactDialog = true
                            }
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primaryContainer,
                            contentColor = colorScheme.onPrimaryContainer
                        ),
                        shape = RoundedCornerShape(12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContactPhone, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.size(8.dp))
                        Text("Pick from Contacts", style = MaterialTheme.typography.labelLarge)
                    }

                    OutlinedButton(
                        onClick = {
                            contactNickname = ""
                            contactName = ""
                            contactNumber = ""
                            showAddContactDialog = true
                        },
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("Manual Entry")
                    }
                }

                if (settings.searchContacts.isEmpty()) {
                    Text(
                        text = "No contact nicknames added yet. Only contacts configured here can be called via search.",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                } else {
                    SettingsGroup {
                        settings.searchContacts.forEach { contact ->
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Phone,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(20.dp)
                                        )
                                        Column {
                                            Row(
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Text(
                                                    text = contact.nickname,
                                                    fontWeight = FontWeight.Bold,
                                                    color = colorScheme.primary,
                                                    style = MaterialTheme.typography.titleMedium
                                                )
                                                Text(
                                                    text = "('call ${contact.nickname}')",
                                                    color = colorScheme.onSurfaceVariant,
                                                    style = MaterialTheme.typography.bodySmall
                                                )
                                            }
                                            val details = listOf(contact.name, contact.phoneNumber).filter { it.isNotBlank() }.joinToString(" • ")
                                            Text(
                                                text = details,
                                                color = colorScheme.onSurface,
                                                style = MaterialTheme.typography.bodySmall
                                            )
                                        }
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeSearchContact(contact.id) },
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

            // ------------------------------------------------------------- Section: Search Aliases
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 28.dp, end = 20.dp, top = 24.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "App Search Aliases",
                        style = MaterialTheme.typography.labelLarge,
                        color = colorScheme.primary,
                        fontWeight = FontWeight.SemiBold
                    )
                    IconButton(
                        onClick = { showAddAliasDialog = true },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = "Add Alias",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }

                Text(
                    text = "Instant keyword shortcuts (e.g. 'wa' → WhatsApp, 'calc' → Calculator):",
                    color = colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.padding(horizontal = 28.dp, vertical = 4.dp)
                )

                if (settings.customAliases.isEmpty()) {
                    Text(
                        text = "No custom aliases added.",
                        color = colorScheme.onSurfaceVariant,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(horizontal = 28.dp, vertical = 8.dp)
                    )
                } else {
                    SettingsGroup {
                        settings.customAliases.forEach { (alias, target) ->
                            Surface(
                                color = colorScheme.surfaceContainerHigh,
                                shape = RoundedCornerShape(6.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 10.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                                    ) {
                                        Text(
                                            text = alias,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.primary,
                                            style = MaterialTheme.typography.titleMedium
                                        )
                                        Text(
                                            text = "→",
                                            color = colorScheme.onSurfaceVariant,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                        Text(
                                            text = target,
                                            color = colorScheme.onSurface,
                                            style = MaterialTheme.typography.bodyMedium
                                        )
                                    }
                                    IconButton(
                                        onClick = { viewModel.removeAlias(alias) },
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

                Spacer(modifier = Modifier.height(36.dp))
            }
        }
    }

    // Add Contact Dialog
    if (showAddContactDialog) {
        AlertDialog(
            onDismissRequest = { showAddContactDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Add Contact Shortcut", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = contactNickname,
                        onValueChange = { contactNickname = it },
                        label = { Text("Nickname (e.g. 'Mom')") },
                        supportingText = { Text("Used for 'call <nickname>' command in search") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactName,
                        onValueChange = { contactName = it },
                        label = { Text("Contact Name (optional)") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = contactNumber,
                        onValueChange = { contactNumber = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (contactNickname.isNotBlank() && contactNumber.isNotBlank()) {
                        viewModel.addSearchContact(
                            nickname = contactNickname.trim(),
                            name = contactName.trim(),
                            phoneNumber = contactNumber.trim()
                        )
                        contactNickname = ""
                        contactName = ""
                        contactNumber = ""
                        showAddContactDialog = false
                    }
                }) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddContactDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Add Alias Dialog
    if (showAddAliasDialog) {
        AlertDialog(
            onDismissRequest = { showAddAliasDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Add Search Alias", style = MaterialTheme.typography.titleLarge) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newAliasKey,
                        onValueChange = { newAliasKey = it },
                        label = { Text("Alias Shortcut (e.g. 'wa')") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newAliasTarget,
                        onValueChange = { newAliasTarget = it },
                        label = { Text("Target App Name (e.g. 'WhatsApp')") },
                        singleLine = true,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (newAliasKey.isNotBlank() && newAliasTarget.isNotBlank()) {
                        viewModel.setAlias(newAliasKey.trim(), newAliasTarget.trim())
                        newAliasKey = ""
                        newAliasTarget = ""
                        showAddAliasDialog = false
                    }
                }) {
                    Text("Add", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddAliasDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Default Weather & Solar City Dialog
    if (showWeatherCityDialog) {
        AlertDialog(
            onDismissRequest = { showWeatherCityDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Default Weather & Solar City", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = weatherCityInput,
                    onValueChange = { weatherCityInput = it },
                    label = { Text("City name (e.g. London)") },
                    supportingText = { Text("Used when typing weather or solar command without a city") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSettings { copy(defaultWeatherCity = weatherCityInput.trim()) }
                    showWeatherCityDialog = false
                }) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWeatherCityDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Wolfram Alpha AppID Dialog
    if (showWolframDialog) {
        AlertDialog(
            onDismissRequest = { showWolframDialog = false },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Wolfram Alpha AppID", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = wolframInput,
                    onValueChange = { wolframInput = it },
                    label = { Text("App ID") },
                    supportingText = { Text("From developer.wolframalpha.com") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    viewModel.updateSettings { copy(wolframAppId = wolframInput.trim()) }
                    showWolframDialog = false
                }) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWolframDialog = false }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    // Edit Command Prefix Dialog
    editingConfig?.let { targetCfg ->
        AlertDialog(
            onDismissRequest = { editingConfig = null },
            shape = RoundedCornerShape(24.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text(text = "Edit Prefix: ${targetCfg.label}", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = editPrefixInput,
                    onValueChange = { editPrefixInput = it },
                    label = { Text("Command prefix") },
                    supportingText = { Text("Default was '${CommandConfig.DEFAULTS[targetCfg.id]?.prefix ?: targetCfg.prefix}'") },
                    singleLine = true,
                    shape = RoundedCornerShape(14.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    val newPrefix = editPrefixInput.trim()
                    if (newPrefix.isNotEmpty()) {
                        viewModel.updateCommandConfig(targetCfg.id, targetCfg.enabled, newPrefix)
                        editingConfig = null
                    }
                }) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { editingConfig = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }
}
