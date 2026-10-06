package com.pallas.launcher.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
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
    val focusRequester = remember { FocusRequester() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
        keyboardController?.show()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }
            .collect { isScrolling ->
                if (isScrolling) {
                    keyboardController?.hide()
                }
            }
    }

    val colors = LocalLauncherColors.current
    val density = LocalDensity.current
    val dismissThresholdPx = with(density) { 80.dp.toPx() }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    // Floating pill background color (translucent matching wallpaper tone)
    val pillBgColor = if (colors.isDark) {
        Color(0xDD242921)
    } else {
        Color(0xDDEDF2E8)
    }
    val scrimColor = if (colors.isDark) Color(0x99000000) else Color(0x55000000)

    // Faint overlay over wallpaper for high contrast & readability
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(scrimColor)
            .statusBarsPadding()
            .imePadding()
            .pointerInput(Unit) {
                detectVerticalDragGestures(
                    onDragStart = { totalDragY = 0f },
                    onVerticalDrag = { _, dragAmount ->
                        totalDragY += dragAmount
                        if (totalDragY > dismissThresholdPx) {
                            keyboardController?.hide()
                            onDismiss()
                        }
                    },
                    onDragEnd = { totalDragY = 0f },
                    onDragCancel = { totalDragY = 0f }
                )
            },
        contentAlignment = Alignment.TopCenter
    ) {
        Column(
            modifier = Modifier
                .fillMaxHeight()
                .widthIn(max = 520.dp)
                .fillMaxWidth()
        ) {
            // Floating pill search bar (Image 3)
            Surface(
                shape = RoundedCornerShape(28.dp),
                color = pillBgColor,
                border = BorderStroke(1.dp, if (colors.isDark) Color.White.copy(alpha = 0.15f) else Color.Black.copy(alpha = 0.08f)),
                shadowElevation = 0.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
                    .height(54.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier.weight(1f),
                        contentAlignment = Alignment.CenterStart
                    ) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps...",
                                style = LauncherTypography.searchInput.copy(fontSize = 18.sp),
                                color = colors.textDimmed
                            )
                        }

                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            textStyle = LauncherTypography.searchInput.copy(
                                fontSize = 18.sp,
                                color = colors.textPrimary
                            ),
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
                                .size(22.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onQueryChange("") }
                                )
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Options",
                            tint = colors.textDimmed,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // Results List directly floating over wallpaper
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
                            style = LauncherTypography.searchResultSubtitle.copy(
                                shadow = Shadow(
                                    color = Color.Black.copy(alpha = 0.5f),
                                    offset = Offset(0f, 1.5f),
                                    blurRadius = 4f
                                )
                            ),
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
