package com.pallas.launcher.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@Composable
fun LauncherSearchOverlay(
    query: String,
    results: List<AppSearchResult>,
    onQueryChange: (String) -> Unit,
    onResultClick: (AppSearchResult) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    BackHandler(onBack = onDismiss)

    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    // Immediate keyboard focus without arbitrary delay hacks
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    // Dismiss keyboard when list is scrolled
    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    keyboardController?.hide()
                }
            }
    }

    val colors = LocalLauncherColors.current
    val surfaceColor = MaterialTheme.colorScheme.surface

    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 80.dp.toPx() }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(surfaceColor)
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = {
                        totalDragY = 0f
                    },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                        if (totalDragY > dismissThresholdPx) {
                            keyboardController?.hide()
                            onDismiss()
                        }
                    },
                    onDragEnd = {
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        totalDragY = 0f
                    }
                )
            }
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Clean Search Input Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search apps...",
                            style = LauncherTypography.searchInput,
                            color = colors.textDimmed
                        )
                    }

                    BasicTextField(
                        value = query,
                        onValueChange = onQueryChange,
                        textStyle = LauncherTypography.searchInput.copy(color = colors.textPrimary),
                        cursorBrush = SolidColor(colors.textPrimary),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            imeAction = ImeAction.Search
                        ),
                        keyboardActions = KeyboardActions(
                            onSearch = {
                                val top = results.firstOrNull()
                                if (top != null) {
                                    keyboardController?.hide()
                                    onResultClick(top)
                                }
                            }
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .focusRequester(focusRequester)
                    )
                }

                if (query.isNotEmpty()) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Clear search",
                        tint = colors.textDimmed,
                        modifier = Modifier
                            .size(20.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = { onQueryChange("") }
                            )
                    )
                }
            }

            // Subtle divider line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .height(1.dp)
                    .background(colors.cardBackgroundLight.copy(alpha = 0.5f))
            )

            // Results List (Plain text rows only)
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                if (query.isEmpty()) {
                    item {
                        Spacer(modifier = Modifier.height(24.dp))
                    }
                } else if (results.isEmpty()) {
                    item {
                        Text(
                            text = "No results found for \"$query\"",
                            style = LauncherTypography.searchResultSubtitle,
                            color = colors.textDimmed,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                        )
                    }
                } else {
                    item {
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    itemsIndexed(
                        items = results,
                        key = { _, item -> item.id }
                    ) { index, result ->
                        SearchResultItemView(
                            result = result,
                            isFirst = index == 0,
                            onClick = {
                                keyboardController?.hide()
                                onResultClick(result)
                            }
                        )
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(32.dp))
                }
            }
        }
    }
}
