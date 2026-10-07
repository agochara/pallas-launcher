package com.pallas.launcher.ui.home

import android.content.pm.ShortcutInfo
import android.os.UserHandle
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.theme.AccentGold
import com.pallas.launcher.ui.theme.AccentRed
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
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
    val colors = LocalLauncherColors.current
    val colorScheme = MaterialTheme.colorScheme
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    val runAndDismiss: (() -> Unit) -> Unit = { action ->
        coroutineScope.launch {
            try {
                sheetState.hide()
            } finally {
                action()
                onDismiss()
            }
        }
    }

    val surfaceContainer = colorScheme.surfaceContainer
    val cardTonal = colorScheme.surfaceContainerHigh

    ModalBottomSheet(
        onDismissRequest = { runAndDismiss {} },
        sheetState = sheetState,
        containerColor = surfaceContainer,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        dragHandle = {
            Box(
                modifier = Modifier
                    .padding(vertical = 12.dp)
                    .size(width = 36.dp, height = 4.dp)
                    .clip(CircleShape)
                    .background(colorScheme.onSurfaceVariant.copy(alpha = 0.4f))
            )
        }
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Hero Header: App Icon + Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AppIconImage(
                    app = app,
                    size = 54.dp,
                    shapeRadius = 14.dp
                )

                Spacer(modifier = Modifier.width(16.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = app.displayLabel,
                        style = LauncherTypography.appLabel.copy(
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            // Shortcuts Section (if any)
            if (shortcuts.isNotEmpty()) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "App Shortcuts",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurfaceVariant
                    )

                    shortcuts.take(4).forEach { shortcut ->
                        val label = shortcut.shortLabel?.toString()
                            ?: shortcut.longLabel?.toString()
                            ?: shortcut.id

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardTonal,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    runAndDismiss { onLaunchShortcut(shortcut.id, shortcut.userHandle) }
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = label,
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Actions Group
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(20.dp))
                    .background(cardTonal)
            ) {
                if (!isOnHomeScreen) {
                    AppOptionTile(
                        title = "Add to Screen",
                        onClick = { runAndDismiss(onAddToScreen) }
                    )
                    Divider(surfaceContainer)
                }

                AppOptionTile(
                    title = "Set Search Alias",
                    onClick = { runAndDismiss(onSetAlias) }
                )

                Divider(surfaceContainer)

                AppOptionTile(
                    title = "Rename Display Label",
                    onClick = { runAndDismiss(onRenameApp) }
                )

                Divider(surfaceContainer)

                AppOptionTile(
                    title = "Hide from App List",
                    onClick = { runAndDismiss(onHideFromAppList) }
                )

                Divider(surfaceContainer)

                AppOptionTile(
                    title = "Hide App",
                    onClick = { runAndDismiss(onHideApp) }
                )

                Divider(surfaceContainer)

                AppOptionTile(
                    title = "App Info & Permissions",
                    onClick = { runAndDismiss(onOpenAppInfo) }
                )

                if (isOnHomeScreen) {
                    Divider(surfaceContainer)

                    AppOptionTile(
                        title = "Remove from this Screen",
                        titleColor = colorScheme.error,
                        onClick = { runAndDismiss(onRemoveFromScreen) }
                    )
                }

                Divider(surfaceContainer)

                AppOptionTile(
                    title = "Uninstall",
                    titleColor = colorScheme.error,
                    onClick = { runAndDismiss(onUninstall) }
                )
            }

            // Bottom Settings Shortcut
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = { runAndDismiss(onOpenSettings) }) {
                    Text(
                        text = "Launcher Settings",
                        color = colorScheme.primary,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
private fun Divider(color: Color) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(color.copy(alpha = 0.5f))
    )
}

@Composable
private fun AppOptionTile(
    title: String,
    onClick: () -> Unit,
    titleColor: Color? = null
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = title,
            fontSize = 15.sp,
            fontWeight = FontWeight.SemiBold,
            color = titleColor ?: colorScheme.onSurface,
            modifier = Modifier.weight(1f)
        )
    }
}
