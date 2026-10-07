package com.pallas.launcher.ui.search

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectVerticalDragGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
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
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.ui.theme.NunitoFontFamily
import com.pallas.launcher.ui.theme.launcherBackdropBrush
import kotlinx.coroutines.delay

/**
 * Apps-only search. Results are laid out bottom-up (reverseLayout) so the BEST match sits directly above the
 * search pill, i.e. closest to the thumb and to the keyboard. No shortcuts, no web results, no clutter.
 */
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
    val latestDismiss by rememberUpdatedState(onDismiss)

    LaunchedEffect(Unit) {
        // requestFocus() throws if the BasicTextField isn't attached yet (happens during the fade-in)
        delay(60)
        runCatching { focusRequester.requestFocus() }
        keyboardController?.show()
    }

    LaunchedEffect(listState) {
        snapshotFlow { listState.isScrollInProgress }.collect { scrolling ->
            if (scrolling) keyboardController?.hide()
        }
    }

    val colorScheme = MaterialTheme.colorScheme
    val dismissThresholdPx = with(LocalDensity.current) { 80.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(launcherBackdropBrush())
            .statusBarsPadding()
            .imePadding()
            .navigationBarsPadding()
            // tap on empty space closes search (rows / pill consume their own taps)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = { keyboardController?.hide(); latestDismiss() }
            )
            .pointerInput(Unit) {
                var total = 0f
                detectVerticalDragGestures(
                    onDragStart = { total = 0f },
                    onVerticalDrag = { _, amount ->
                        total += amount
                        if (total > dismissThresholdPx) {
                            total = Float.NEGATIVE_INFINITY // fire once
                            keyboardController?.hide()
                            latestDismiss()
                        }
                    },
                    onDragEnd = { total = 0f },
                    onDragCancel = { total = 0f }
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
            LazyColumn(
                state = listState,
                reverseLayout = true, // index 0 (best match) is drawn at the BOTTOM, next to the pill
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) {
                item { Spacer(modifier = Modifier.height(8.dp)) }

                if (query.isNotEmpty() && results.isEmpty()) {
                    item {
                        Text(
                            text = "No apps match \"$query\"",
                            style = TextStyle(
                                fontFamily = NunitoFontFamily,
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Normal
                            ),
                            color = colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)
                        )
                    }
                } else {
                    itemsIndexed(items = results, key = { _, item -> item.id }) { index, result ->
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
            }

            Surface(
                shape = RoundedCornerShape(28.dp),
                color = colorScheme.surfaceContainerHigh,
                tonalElevation = 6.dp,
                shadowElevation = 4.dp,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp)
                    .height(56.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 18.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = null,
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))

                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        if (query.isEmpty()) {
                            Text(
                                text = "Search apps",
                                style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 17.sp),
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                            )
                        }
                        BasicTextField(
                            value = query,
                            onValueChange = onQueryChange,
                            textStyle = TextStyle(
                                fontFamily = NunitoFontFamily,
                                fontSize = 17.sp,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            ),
                            cursorBrush = SolidColor(colorScheme.primary),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                            keyboardActions = KeyboardActions(
                                onSearch = {
                                    results.firstOrNull()?.let {
                                        keyboardController?.hide()
                                        onResultClick(it)
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
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onQueryChange("") }
                                )
                        )
                    }
                }
            }
        }
    }
}