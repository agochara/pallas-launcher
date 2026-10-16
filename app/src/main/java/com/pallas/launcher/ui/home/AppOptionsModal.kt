package com.pallas.launcher.ui.home

import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.DeleteOutline
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.RemoveCircleOutline
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.theme.LauncherTypography

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun AppOptionsModal(
    app: ScreenApp,
    shortcuts: List<ShortcutInfo>,
    isOnHomeScreen: Boolean,
    onLaunchShortcut: (String, UserHandle?) -> Unit,
    onOpenAppInfo: () -> Unit,
    onSetAlias: () -> Unit,
    onRenameApp: () -> Unit,
    onRemoveFromScreen: () -> Unit,
    onAddToScreen: () -> Unit,
    onHideFromAppList: () -> Unit,
    onHideApp: () -> Unit,
    onUninstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    val runAndClose: (() -> Unit) -> Unit = { action ->
        action()
        onDismiss()
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth(0.88f)
                .padding(vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = colorScheme.surfaceContainer,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier.padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
                // 1. Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AppIconImage(app = app, size = 48.dp, shapeRadius = 14.dp)
                    Spacer(modifier = Modifier.width(14.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = app.displayLabel,
                            style = LauncherTypography.modalTitle,
                            color = colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = app.packageName,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.75f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    IconButton(
                        onClick = { runAndClose(onOpenSettings) },
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Settings,
                            contentDescription = "Launcher Settings",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // 2. Quick 4-Icon Action Bar (Rename, Alias, Info, Uninstall)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    QuickIconAction(
                        icon = Icons.Default.Edit,
                        label = "Rename",
                        onClick = { runAndClose(onRenameApp) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickIconAction(
                        icon = Icons.Default.Tag,
                        label = "Alias",
                        onClick = { runAndClose(onSetAlias) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickIconAction(
                        icon = Icons.Default.Info,
                        label = "Info",
                        onClick = { runAndClose(onOpenAppInfo) },
                        modifier = Modifier.weight(1f)
                    )
                    QuickIconAction(
                        icon = Icons.Default.DeleteOutline,
                        label = "Uninstall",
                        tint = colorScheme.error,
                        onClick = { runAndClose(onUninstall) },
                        modifier = Modifier.weight(1f)
                    )
                }

                // 3. Compact Inline Shortcut Pills (if any)
                if (shortcuts.isNotEmpty()) {
                    FlowRow(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        shortcuts.take(4).forEach { shortcut ->
                            val label = shortcut.shortLabel?.toString()
                                ?: shortcut.longLabel?.toString()
                                ?: shortcut.id
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.clickable {
                                    runAndClose { onLaunchShortcut(shortcut.id, shortcut.userHandle) }
                                }
                            ) {
                                Text(
                                    text = "↗ $label",
                                    style = LauncherTypography.chip,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                                )
                            }
                        }
                    }
                }

                // 4. Contextual Screen & Visibility Chips (2-Column Grid)
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        if (isOnHomeScreen) {
                            CompactChipTile(
                                icon = Icons.Default.RemoveCircleOutline,
                                label = "Unpin Screen",
                                tint = colorScheme.error,
                                onClick = { runAndClose(onRemoveFromScreen) },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            CompactChipTile(
                                icon = Icons.Default.Add,
                                label = "Pin to Screen",
                                tint = colorScheme.primary,
                                onClick = { runAndClose(onAddToScreen) },
                                modifier = Modifier.weight(1f)
                            )
                        }

                        CompactChipTile(
                            icon = Icons.Default.VisibilityOff,
                            label = "Hide in List",
                            onClick = { runAndClose(onHideFromAppList) },
                            modifier = Modifier.weight(1f)
                        )
                    }

                    CompactChipTile(
                        icon = Icons.Default.VisibilityOff,
                        label = "Hide Completely (Drawer & Search)",
                        onClick = { runAndClose(onHideApp) },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
    }
}

@Composable
private fun QuickIconAction(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurface
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(colorScheme.surfaceContainerHigh)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(imageVector = icon, contentDescription = label, tint = tint, modifier = Modifier.size(20.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = tint,
            maxLines = 1
        )
    }
}

@Composable
private fun CompactChipTile(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color = MaterialTheme.colorScheme.onSurfaceVariant
) {
    val colorScheme = MaterialTheme.colorScheme
    Row(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(colorScheme.surfaceContainerHigh.copy(alpha = 0.7f))
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center
    ) {
        Icon(imageVector = icon, contentDescription = null, tint = tint, modifier = Modifier.size(16.dp))
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = label,
            style = LauncherTypography.chip,
            color = tint,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
