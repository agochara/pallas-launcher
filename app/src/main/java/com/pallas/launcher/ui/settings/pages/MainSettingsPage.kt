package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pallas.launcher.ui.settings.SettingsSubpage
import com.pallas.launcher.ui.settings.components.SettingsNavRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsPage(
    onNavigate: (SettingsSubpage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = MaterialTheme.colorScheme.surface,
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Settings",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                },
                actions = {
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            item {
                SettingsNavRow(
                    title = "Appearance",
                    subtitle = "Wallpaper scrim, status bar, font scale, theme",
                    icon = Icons.Default.Palette,
                    onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }
            item {
                SettingsNavRow(
                    title = "Clock & Date",
                    subtitle = "Position, alignment, 24-hour time, date, battery",
                    icon = Icons.Default.Schedule,
                    onClick = { onNavigate(SettingsSubpage.CLOCK_DATE) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }
            item {
                SettingsNavRow(
                    title = "Home Screens & Apps",
                    subtitle = "Manage screens, reorder apps, hidden apps",
                    icon = Icons.Default.GridView,
                    onClick = { onNavigate(SettingsSubpage.SCREENS_APPS) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }
            item {
                SettingsNavRow(
                    title = "Gestures",
                    subtitle = "Swipe actions, edge scrub launcher",
                    icon = Icons.Default.TouchApp,
                    onClick = { onNavigate(SettingsSubpage.GESTURES) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }
            item {
                SettingsNavRow(
                    title = "Search",
                    subtitle = "Custom search aliases, index options",
                    icon = Icons.Default.Search,
                    onClick = { onNavigate(SettingsSubpage.SEARCH) }
                )
                HorizontalDivider(color = MaterialTheme.colorScheme.surfaceContainerHighest.copy(alpha = 0.5f))
            }
        }
    }
}
