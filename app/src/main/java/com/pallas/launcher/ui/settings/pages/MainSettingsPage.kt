package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.settings.SettingsSubpage
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsCompactTile
import com.pallas.launcher.ui.settings.components.SettingsExpressiveTile
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsPage(
    viewModel: SettingsViewModel,
    onNavigate: (SettingsSubpage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme
    val settings by viewModel.settings.collectAsState()
    val focusState by viewModel.focusState.collectAsState()

    val versionName = remember(context) {
        try {
            val pi = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pi.versionName ?: "3.0.0"
        } catch (e: Exception) {
            "3.0.0"
        }
    }

    var isDefault by remember { mutableStateOf(SystemActionsManager.isDefaultLauncher(context)) }
    LifecycleResumeEffect(Unit) {
        isDefault = SystemActionsManager.isDefaultLauncher(context)
        onPauseOrDispose { }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .nestedScroll(scrollBehavior.nestedScrollConnection),
        containerColor = colorScheme.surface,
        topBar = { SettingsLargeTopBar(title = "Settings", scrollBehavior = scrollBehavior, onClose = onDismiss) }
    ) { innerPadding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (!isDefault) {
                item {
                    Surface(
                        color = colorScheme.primaryContainer,
                        shape = RoundedCornerShape(26.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 16.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                modifier = Modifier.weight(1f).padding(end = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Home,
                                    contentDescription = null,
                                    tint = colorScheme.onPrimaryContainer,
                                    modifier = Modifier.size(26.dp)
                                )
                                Spacer(modifier = Modifier.width(14.dp))
                                Column {
                                    Text(
                                        text = "Set Default Home",
                                        style = MaterialTheme.typography.titleMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                    Text(
                                        text = "Use Pallas as your launcher",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                    )
                                }
                            }
                            Button(
                                onClick = { SystemActionsManager.requestSetDefaultLauncher(context) },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Enable", fontWeight = FontWeight.SemiBold)
                            }
                        }
                    }
                }
            }

            // Pallas Workspace — visually distinct 2x2 Expressive Square/Tile Grid
            item {
                SettingsSectionHeader("Workspace")
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsExpressiveTile(
                            title = "Guide",
                            subtitle = "Reference & syntax",
                            symbol = "ƒ",
                            containerColor = colorScheme.primaryContainer,
                            onClick = { onNavigate(SettingsSubpage.MATH_MANUAL) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsExpressiveTile(
                            title = "World Atlas",
                            subtitle = "Offline map",
                            icon = Icons.Default.Public,
                            containerColor = colorScheme.secondaryContainer,
                            onClick = { onNavigate(SettingsSubpage.WORLD_ATLAS) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        SettingsExpressiveTile(
                            title = "LaTeX Studio",
                            subtitle = "Typeset & export",
                            icon = Icons.Default.Description,
                            containerColor = colorScheme.tertiaryContainer,
                            onClick = { onNavigate(SettingsSubpage.LATEX_STUDIO) },
                            modifier = Modifier.weight(1f)
                        )
                        SettingsExpressiveTile(
                            title = "Notebook",
                            subtitle = "Math cells & plots",
                            icon = Icons.Default.Terminal,
                            containerColor = colorScheme.surfaceContainerHighest,
                            onClick = { onNavigate(SettingsSubpage.NOTEBOOK) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            // Modes & Automation — compact side-by-side M3E tiles
            item {
                SettingsSectionHeader("Modes & Automation")
                val activeFocus = if (focusState.isActive) {
                    settings.focusProfiles.firstOrNull { it.id == focusState.activeProfileId }
                } else null
                val focusSub = if (activeFocus != null) {
                    "Active: ${activeFocus.name}"
                } else {
                    val count = settings.focusProfiles.size
                    "$count profile${if (count == 1) "" else "s"}"
                }
                val scheduledCount = settings.macros.count { it.scheduleEnabled }
                val macroSub = buildString {
                    append("${settings.macros.size} routine${if (settings.macros.size == 1) "" else "s"}")
                    if (scheduledCount > 0) append(" • $scheduledCount sched.")
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    SettingsCompactTile(
                        title = "Focus Mode",
                        subtitle = focusSub,
                        icon = Icons.Default.CenterFocusStrong,
                        containerColor = if (activeFocus != null) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                        onClick = { onNavigate(SettingsSubpage.FOCUS_MODE) },
                        modifier = Modifier.weight(1f)
                    )
                    SettingsCompactTile(
                        title = "Macros",
                        subtitle = macroSub,
                        icon = Icons.Default.Bolt,
                        containerColor = colorScheme.surfaceContainerHigh,
                        onClick = { onNavigate(SettingsSubpage.MACROS) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            // Clean Categorized Settings Groups
            item {
                SettingsSectionHeader("Home & Look")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Appearance",
                        subtitle = "Theme, wallpaper scrim, status bar, type scale",
                        icon = Icons.Default.Palette,
                        containerColor = colorScheme.primaryContainer,
                        onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
                    )
                    SettingsNavRow(
                        title = "Clock & Date",
                        subtitle = "Placement, alignment, format, battery",
                        icon = Icons.Default.Schedule,
                        containerColor = colorScheme.secondaryContainer,
                        onClick = { onNavigate(SettingsSubpage.CLOCK_DATE) }
                    )
                    SettingsNavRow(
                        title = "Screens & Apps",
                        subtitle = "Home screens, order, hidden apps",
                        icon = Icons.Default.GridView,
                        containerColor = colorScheme.tertiaryContainer,
                        onClick = { onNavigate(SettingsSubpage.SCREENS_APPS) }
                    )
                }

                SettingsSectionHeader("Interaction")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Gestures",
                        subtitle = "Swipes, double tap, alphabet edge rail",
                        icon = Icons.Default.TouchApp,
                        containerColor = colorScheme.secondaryContainer,
                        onClick = { onNavigate(SettingsSubpage.GESTURES) }
                    )
                    SettingsNavRow(
                        title = "Search",
                        subtitle = "Engine, commands, triggers, aliases, contacts",
                        icon = Icons.Default.Search,
                        containerColor = colorScheme.primaryContainer,
                        onClick = { onNavigate(SettingsSubpage.SEARCH) }
                    )
                }

                SettingsSectionHeader("System")
                SettingsGroup {
                    SettingsNavRow(
                        title = "Backup & Restore",
                        subtitle = "Export or import setup JSON",
                        icon = Icons.Default.Save,
                        containerColor = colorScheme.tertiaryContainer,
                        onClick = { onNavigate(SettingsSubpage.BACKUP) }
                    )
                    if (isDefault) {
                        SettingsNavRow(
                            title = "Default Launcher",
                            subtitle = "Pallas",
                            icon = Icons.Default.Home,
                            containerColor = colorScheme.surfaceContainerHighest,
                            onClick = { SystemActionsManager.requestSetDefaultLauncher(context) }
                        )
                    }
                }

                Text(
                    text = "Pallas Launcher · v$versionName",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 24.dp, bottom = 36.dp)
                )
            }
        }
    }
}
