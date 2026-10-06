package com.pallas.launcher.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.search.SearchRanking
import com.pallas.launcher.ui.theme.CardBackground
import com.pallas.launcher.ui.theme.CardBackgroundLight
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.TextDimmed
import com.pallas.launcher.ui.theme.TextMuted
import com.pallas.launcher.ui.theme.TextWhite

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppPickerSheet(
    installedApps: List<AppItem>,
    onAppSelected: (AppItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var filterQuery by remember { mutableStateOf("") }

    val filteredApps = remember(installedApps, filterQuery) {
        if (filterQuery.isBlank()) {
            installedApps
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
        containerColor = CardBackground,
        modifier = modifier.imePadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = 24.dp)
        ) {
            Text(
                text = "Select App",
                style = LauncherTypography.searchResultTitle.copy(fontSize = 22.sp),
                color = TextWhite,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Search filter
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(CardBackgroundLight, RoundedCornerShape(8.dp))
                    .padding(horizontal = 16.dp, vertical = 12.dp)
            ) {
                if (filterQuery.isEmpty()) {
                    Text(
                        text = "Filter apps...",
                        style = LauncherTypography.searchResultSubtitle.copy(fontSize = 16.sp),
                        color = TextDimmed
                    )
                }
                BasicTextField(
                    value = filterQuery,
                    onValueChange = { filterQuery = it },
                    textStyle = LauncherTypography.searchResultSubtitle.copy(
                        fontSize = 16.sp,
                        color = TextWhite
                    ),
                    cursorBrush = SolidColor(TextWhite),
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
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onAppSelected(app) }
                            )
                            .padding(vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = app.label,
                                style = LauncherTypography.searchResultTitle,
                                color = TextWhite
                            )
                            Text(
                                text = app.packageName,
                                style = LauncherTypography.searchResultSubtitle,
                                color = TextDimmed
                            )
                        }
                    }
                }
            }
        }
    }
}
