package com.pallas.launcher.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.HideSource
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.RemoveCircleOutline
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.ui.apps.AppIconImage

private val GroupShape = RoundedCornerShape(24.dp)
private val RowShape = RoundedCornerShape(4.dp)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppOptionsModal(
    app: ScreenApp,
    isOnHomeScreen: Boolean,
    isHiddenFromAppList: Boolean,
    currentAlias: String?,
    onOpenAppInfo: () -> Unit,
    onSetAlias: () -> Unit,
    onRenameApp: () -> Unit,
    onRemoveFromScreen: () -> Unit,
    onAddToScreen: () -> Unit,
    onToggleHideFromAppList: () -> Unit,
    onHideApp: () -> Unit,
    onUninstall: () -> Unit,
    onOpenSettings: () -> Unit,
    onDismiss: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val runAction: (() -> Unit) -> Unit = { action ->
        action()
        onDismiss()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerLow,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(top = 12.dp, bottom = 8.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colorScheme.outlineVariant)
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .verticalScroll(rememberScrollState())
                .padding(bottom = 20.dp)
        ) {
            // Editorial header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp, top = 4.dp, bottom = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(app = app, size = 48.dp, shapeRadius = 14.dp)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayLabel,
                        style = MaterialTheme.typography.headlineSmall,
                        color = colorScheme.onSurface,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!app.customLabel.isNullOrBlank() &&
                        app.defaultLabel.isNotBlank() &&
                        app.customLabel != app.defaultLabel
                    ) {
                        Text(
                            text = app.defaultLabel,
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                IconButton(onClick = { runAction(onOpenSettings) }) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = "Launcher Settings",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Group 1: Screen & Customization
            OptionGroup {
                if (!isOnHomeScreen) {
                    OptionRow(
                        icon = Icons.Outlined.AddCircleOutline,
                        title = "Add to home screen",
                        iconTint = colorScheme.primary,
                        onClick = { runAction(onAddToScreen) }
                    )
                }
                OptionRow(
                    icon = Icons.Outlined.Edit,
                    title = "Rename",
                    onClick = { runAction(onRenameApp) }
                )
                OptionRow(
                    icon = Icons.Outlined.Tag,
                    title = "Search alias",
                    trailingBadge = currentAlias,
                    onClick = { runAction(onSetAlias) }
                )
                OptionRow(
                    icon = Icons.Outlined.Info,
                    title = "App info",
                    onClick = { runAction(onOpenAppInfo) }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Group 2: Visibility & Removal
            OptionGroup {
                if (isOnHomeScreen) {
                    OptionRow(
                        icon = Icons.Outlined.RemoveCircleOutline,
                        title = "Remove from screen",
                        onClick = { runAction(onRemoveFromScreen) }
                    )
                }
                OptionRow(
                    icon = if (isHiddenFromAppList) Icons.Outlined.Visibility else Icons.Outlined.VisibilityOff,
                    title = if (isHiddenFromAppList) "Show in app list" else "Hide from app list",
                    subtitle = if (isHiddenFromAppList) {
                        "Show in alphabetical drawer again"
                    } else {
                        "Keep on home screen and search"
                    },
                    onClick = { runAction(onToggleHideFromAppList) }
                )
                OptionRow(
                    icon = Icons.Outlined.HideSource,
                    title = "Hide completely",
                    subtitle = "Hide from screens, app list, and search",
                    onClick = { runAction(onHideApp) }
                )
                OptionRow(
                    icon = Icons.Outlined.DeleteOutline,
                    title = "Uninstall",
                    iconTint = colorScheme.error,
                    titleColor = colorScheme.error,
                    onClick = { runAction(onUninstall) }
                )
            }
        }
    }
}

@Composable
private fun OptionGroup(
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp)
            .clip(GroupShape),
        verticalArrangement = Arrangement.spacedBy(2.dp),
        content = content
    )
}

@Composable
private fun OptionRow(
    icon: ImageVector,
    title: String,
    subtitle: String? = null,
    trailingBadge: String? = null,
    iconTint: Color = MaterialTheme.colorScheme.onSurfaceVariant,
    titleColor: Color = MaterialTheme.colorScheme.onSurface,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        color = colorScheme.surfaceContainerHigh,
        shape = RowShape,
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 52.dp)
                .clickable(role = Role.Button, onClick = onClick)
                .padding(horizontal = 18.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = iconTint,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.Medium),
                    color = titleColor
                )
                if (!subtitle.isNullOrBlank()) {
                    Text(
                        text = subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            if (!trailingBadge.isNullOrBlank()) {
                Spacer(modifier = Modifier.width(12.dp))
                Text(
                    text = trailingBadge,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.primary,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(colorScheme.primaryContainer.copy(alpha = 0.45f))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                )
            }
        }
    }
}
