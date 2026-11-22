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
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
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
import androidx.lifecycle.compose.LifecycleResumeEffect
import com.pallas.launcher.platform.SystemActionsManager
import com.pallas.launcher.ui.settings.SettingsSubpage
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow
import com.pallas.launcher.ui.settings.components.SettingsSectionHeader

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsPage(
    onNavigate: (SettingsSubpage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()
    val colorScheme = MaterialTheme.colorScheme

    val versionName = remember(context) {
        try {
            val pi = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                context.packageManager.getPackageInfo(context.packageName, android.content.pm.PackageManager.PackageInfoFlags.of(0))
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getPackageInfo(context.packageName, 0)
            }
            pi.versionName ?: "1.0.0"
        } catch (e: Exception) {
            "1.0.0"
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
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = { SettingsLargeTopBar(title = "Settings", scrollBehavior = scrollBehavior, onClose = onDismiss) }
    ) { innerPadding ->
        LazyColumn(
            state = rememberLazyListState(),
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // "Make Default" Action Banner
            item {
                Surface(
                    color = if (isDefault) colorScheme.surfaceContainerHigh else colorScheme.primaryContainer,
                    shape = RoundedCornerShape(28.dp),
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
                                imageVector = if (isDefault) Icons.Default.CheckCircle else Icons.Default.Home,
                                contentDescription = null,
                                tint = if (isDefault) colorScheme.primary else colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = "Default Launcher",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isDefault) colorScheme.onSurface else colorScheme.onPrimaryContainer
                                )
                                Text(
                                    text = if (isDefault) "Pallas is your default launcher" else "Set Pallas as your default home app",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (isDefault) colorScheme.onSurfaceVariant else colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                                )
                            }
                        }

                        if (!isDefault) {
                            Button(
                                onClick = {
                                    SystemActionsManager.requestSetDefaultLauncher(context)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = colorScheme.primary,
                                    contentColor = colorScheme.onPrimary
                                ),
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Make Default", fontWeight = FontWeight.Bold)
                            }
                        } else {
                            OutlinedButton(
                                onClick = {
                                    SystemActionsManager.requestSetDefaultLauncher(context)
                                },
                                shape = RoundedCornerShape(14.dp)
                            ) {
                                Text("Change", style = MaterialTheme.typography.labelLarge)
                            }
                        }
                    }
                }
            }

            item {
                SettingsGroup(modifier = Modifier.padding(top = 8.dp)) {
                    SettingsNavRow(
                        title = "Appearance",
                        subtitle = "Theme, wallpaper scrim, status bar, text size",
                        icon = Icons.Default.Palette,
                        containerColor = colorScheme.primaryContainer,
                        onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
                    )
                    SettingsNavRow(
                        title = "Clock & Date",
                        subtitle = "Position, alignment, 24-hour time, date, battery",
                        icon = Icons.Default.Schedule,
                        containerColor = colorScheme.secondaryContainer,
                        onClick = { onNavigate(SettingsSubpage.CLOCK_DATE) }
                    )
                    SettingsNavRow(
                        title = "Screens & Apps",
                        subtitle = "Manage screens, reorder apps, hidden apps",
                        icon = Icons.Default.GridView,
                        containerColor = colorScheme.tertiaryContainer,
                        onClick = { onNavigate(SettingsSubpage.SCREENS_APPS) }
                    )
                    SettingsNavRow(
                        title = "Gestures",
                        subtitle = "Swipe actions, app list rail",
                        icon = Icons.Default.TouchApp,
                        containerColor = colorScheme.secondaryContainer,
                        onClick = { onNavigate(SettingsSubpage.GESTURES) }
                    )
                    SettingsNavRow(
                        title = "Search",
                        subtitle = "Web engine, smart commands, weather, aliases & contacts",
                        icon = Icons.Default.Search,
                        containerColor = colorScheme.primaryContainer,
                        onClick = { onNavigate(SettingsSubpage.SEARCH) }
                    )
                    SettingsNavRow(
                        title = "Math & Computational Engine Guide",
                        subtitle = "Functions, live commands, math & LaTeX cheatsheets",
                        icon = Icons.Default.Functions,
                        containerColor = colorScheme.secondaryContainer,
                        onClick = { onNavigate(SettingsSubpage.MATH_MANUAL) }
                    )
                    SettingsNavRow(
                        title = "Backup & Restore",
                        subtitle = "Export or import your launcher setup as a JSON file",
                        icon = Icons.Default.Save,
                        containerColor = colorScheme.tertiaryContainer,
                        onClick = { onNavigate(SettingsSubpage.BACKUP) }
                    )
                }

                SettingsSectionHeader("About")
                SettingsGroup {
                    Surface(
                        color = colorScheme.surfaceContainerHigh,
                        shape = RoundedCornerShape(6.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 20.dp, vertical = 14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f).padding(end = 12.dp)) {
                                Text(
                                    text = "Pallas Launcher",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "A minimalist, fast Android launcher",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = "v$versionName",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}
