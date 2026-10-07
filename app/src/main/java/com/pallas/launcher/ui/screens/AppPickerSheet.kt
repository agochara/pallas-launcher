package com.pallas.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.search.SearchRanking
import com.pallas.launcher.ui.apps.AppIconImage

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    installedApps: List<AppItem>,
    alreadyAddedKeys: Set<String> = emptySet(),
    onAppsConfirmed: (List<AppItem>) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by remember { mutableStateOf("") }
    var selectedKeys by remember { mutableStateOf(emptySet<String>()) }
    val colorScheme = MaterialTheme.colorScheme

    val filteredApps = remember(installedApps, filterQuery) {
        if (filterQuery.isBlank()) {
            installedApps.sortedBy { it.label.lowercase() }
        } else {
            installedApps
                .mapNotNull { app ->
                    val score = SearchRanking.calculateMatchScore(filterQuery, app.label)
                    if (score > 0.0) Pair(app, score) else null
                }
                .sortedByDescending { it.second }
                .map { it.first }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainer,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.88f)
                .padding(horizontal = 24.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Add Apps to Screen",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    if (selectedKeys.isNotEmpty()) {
                        Text(
                            text = "${selectedKeys.size} selected",
                            style = MaterialTheme.typography.bodySmall,
                            color = colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }

                if (selectedKeys.isNotEmpty()) {
                    TextButton(onClick = { selectedKeys = emptySet() }) {
                        Text("Clear", color = colorScheme.error)
                    }
                }
            }

            // Search filter box
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(colorScheme.surfaceContainerHigh)
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (filterQuery.isEmpty()) {
                    Text(
                        text = "Filter apps...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurfaceVariant
                    )
                }
                BasicTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    textStyle = MaterialTheme.typography.bodyLarge.copy(color = colorScheme.onSurface),
                    cursorBrush = SolidColor(colorScheme.primary),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                items(
                    items = filteredApps,
                    key = { it.uniqueKey }
                ) { app ->
                    val isAlreadyAdded = alreadyAddedKeys.contains(app.uniqueKey)
                    val isSelected = selectedKeys.contains(app.uniqueKey)

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .then(
                                if (isAlreadyAdded) {
                                    Modifier.background(colorScheme.surfaceContainerHigh.copy(alpha = 0.4f))
                                } else if (isSelected) {
                                    Modifier.background(colorScheme.primaryContainer.copy(alpha = 0.5f))
                                } else {
                                    Modifier.clickable {
                                        selectedKeys = if (isSelected) {
                                            selectedKeys - app.uniqueKey
                                        } else {
                                            selectedKeys + app.uniqueKey
                                        }
                                    }
                                }
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AppIconImage(
                            app = app,
                            size = 38.dp,
                            shape = CircleShape
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = app.label,
                                style = MaterialTheme.typography.titleMedium,
                                color = if (isAlreadyAdded) colorScheme.onSurface.copy(alpha = 0.5f) else colorScheme.onSurface
                            )
                            Text(
                                text = if (isAlreadyAdded) "Already on screen" else app.packageName,
                                style = MaterialTheme.typography.bodySmall,
                                color = if (isAlreadyAdded) colorScheme.primary.copy(alpha = 0.8f) else colorScheme.onSurfaceVariant
                            )
                        }

                        if (!isAlreadyAdded) {
                            Checkbox(
                                checked = isSelected,
                                onCheckedChange = { checked ->
                                    selectedKeys = if (checked) {
                                        selectedKeys + app.uniqueKey
                                    } else {
                                        selectedKeys - app.uniqueKey
                                    }
                                }
                            )
                        }
                    }
                }
            }

            // Bottom action confirmation bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text("Cancel")
                }

                Button(
                    onClick = {
                        val chosen = installedApps.filter { selectedKeys.contains(it.uniqueKey) }
                        onAppsConfirmed(chosen)
                    },
                    enabled = selectedKeys.isNotEmpty(),
                    modifier = Modifier.weight(1.5f),
                    shape = RoundedCornerShape(14.dp)
                ) {
                    Text(
                        if (selectedKeys.isEmpty()) "Add Apps"
                        else "Add (${selectedKeys.size})"
                    )
                }
            }
        }
    }
}

@Composable
fun AppPickerSheet(
    installedApps: List<AppItem>,
    onAppSelected: (AppItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    AppPickerSheet(
        installedApps = installedApps,
        alreadyAddedKeys = emptySet(),
        onAppsConfirmed = { list -> list.firstOrNull()?.let(onAppSelected) },
        onDismiss = onDismiss,
        modifier = modifier
    )
}
