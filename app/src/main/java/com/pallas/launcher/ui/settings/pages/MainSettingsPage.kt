package com.pallas.launcher.ui.settings.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.unit.dp
import com.pallas.launcher.ui.settings.SettingsSubpage
import com.pallas.launcher.ui.settings.components.SettingsGroup
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.settings.components.SettingsNavRow

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainSettingsPage(
    onNavigate: (SettingsSubpage) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

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
            item {
                SettingsGroup(modifier = Modifier.padding(top = 8.dp)) {
                    SettingsNavRow(
                        title = "Appearance",
                        subtitle = "Theme, wallpaper scrim, status bar, text size",
                        icon = Icons.Default.Palette,
                        onClick = { onNavigate(SettingsSubpage.APPEARANCE) }
                    )
                    SettingsNavRow(
                        title = "Clock & Date",
                        subtitle = "Position, alignment, 24-hour time, date, battery",
                        icon = Icons.Default.Schedule,
                        onClick = { onNavigate(SettingsSubpage.CLOCK_DATE) }
                    )
                    SettingsNavRow(
                        title = "Home Screens & Apps",
                        subtitle = "Manage screens, reorder apps, hidden apps",
                        icon = Icons.Default.GridView,
                        onClick = { onNavigate(SettingsSubpage.SCREENS_APPS) }
                    )
                    SettingsNavRow(
                        title = "Gestures",
                        subtitle = "Swipe actions, app list rail",
                        icon = Icons.Default.TouchApp,
                        onClick = { onNavigate(SettingsSubpage.GESTURES) }
                    )
                    SettingsNavRow(
                        title = "Search",
                        subtitle = "Search aliases",
                        icon = Icons.Default.Search,
                        onClick = { onNavigate(SettingsSubpage.SEARCH) }
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}