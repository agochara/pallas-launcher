package com.pallas.launcher.ui.apps

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.search.SearchRanking
import com.pallas.launcher.ui.theme.AccentGold
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.roundToInt

private val FULL_ALPHABET = ('A'..'Z').map { it.toString() } + "#"

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AlphabeticalAppDrawer(
    installedApps: List<AppItem>,
    edgeScrubPosition: EdgeScrubPosition,
    edgeScrubReleaseAction: EdgeScrubReleaseAction,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    if (edgeScrubPosition == EdgeScrubPosition.OFF && !isOpen) {
        return
    }

    if (isOpen) {
        BackHandler(onBack = onDismiss)
    }

    val colors = LocalLauncherColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val listState = rememberLazyListState()
    val density = LocalDensity.current

    // Strict alphabetical order by normalized label
    val sortedApps = remember(installedApps) {
        installedApps.sortedWith(
            compareBy<AppItem> { SearchRanking.normalize(it.label) }
                .thenBy { it.label }
        )
    }

    // Letters that actually have apps
    val (availableLetters, letterToIndexMap) = remember(sortedApps) {
        val letterMap = mutableMapOf<String, Int>()
        val letters = mutableListOf<String>()

        sortedApps.forEachIndexed { index, app ->
            val norm = SearchRanking.normalize(app.label.trim())
            val firstChar = norm.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (firstChar in 'A'..'Z') firstChar.toString() else "#"
            if (!letterMap.containsKey(letter)) {
                letterMap[letter] = index
                letters.add(letter)
            }
        }
        Pair(letters, letterMap)
    }

    var isDragging by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf<String?>(null) }
    var touchYPx by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }

    val isLeft = edgeScrubPosition == EdgeScrubPosition.LEFT
    val stripAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
    val surfaceColor = MaterialTheme.colorScheme.surface

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        // App List Overlay (Only visible when drawer is open)
        AnimatedVisibility(
            visible = isOpen,
            enter = if (isLeft) slideInHorizontally { -it } + fadeIn() else slideInHorizontally { it } + fadeIn(),
            exit = if (isLeft) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(surfaceColor)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(isLeft) {
                        // Swipe dismiss gesture
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (isLeft && dragAmount < -40f) {
                                onDismiss()
                            } else if (!isLeft && dragAmount > 40f) {
                                onDismiss()
                            }
                        }
                    }
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(
                            start = if (isLeft) 44.dp else 24.dp,
                            end = if (isLeft) 24.dp else 44.dp
                        )
                ) {
                    item {
                        Spacer(modifier = Modifier.height(16.dp))
                    }

                    items(
                        items = sortedApps,
                        key = { it.uniqueKey }
                    ) { app ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .combinedClickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onAppLongPress(app) }
                                )
                                .padding(vertical = 11.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = app.label,
                                style = LauncherTypography.appLabel.copy(
                                    fontSize = 17.sp,
                                    fontWeight = FontWeight.Normal
                                ),
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(64.dp))
                    }
                }
            }
        }

        // Floating Letter Bubble (Visible while dragging)
        if (isDragging && activeLetter != null) {
            val bubbleY = with(density) {
                (touchYPx - 27.dp.toPx()).coerceIn(24.dp.toPx(), (railHeightPx - 58.dp.toPx()).coerceAtLeast(24.dp.toPx()))
            }

            val bubbleAlignment = if (isLeft) Alignment.TopStart else Alignment.TopEnd
            val bubblePaddingModifier = if (isLeft) {
                Modifier.padding(start = 48.dp)
            } else {
                Modifier.padding(end = 48.dp)
            }

            Box(
                modifier = Modifier
                    .align(bubbleAlignment)
                    .then(bubblePaddingModifier)
                    .offset { IntOffset(0, bubbleY.roundToInt()) }
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(AccentGold)
                    .size(54.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter ?: "",
                    style = LauncherTypography.clock.copy(
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color(0xFF1C1917)
                )
            }
        }

        // Vertical Edge Strip & Alphabet Rail
        Box(
            modifier = Modifier
                .align(stripAlignment)
                .fillMaxHeight()
                .width(28.dp)
                .systemGestureExclusion()
                .onGloballyPositioned { coordinates ->
                    railHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(availableLetters, edgeScrubReleaseAction, isOpen) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()

                        isDragging = true
                        if (!isOpen) {
                            onOpenChange(true)
                        }

                        var currentLetter: String? = null

                        fun updateLetterForY(y: Float) {
                            touchYPx = y
                            val fraction = (y / size.height).coerceIn(0f, 1f)
                            val rawSlot = (fraction * (FULL_ALPHABET.size - 1)).roundToInt().coerceIn(0, FULL_ALPHABET.size - 1)
                            val rawLetter = FULL_ALPHABET[rawSlot]

                            // Snap to the nearest letter that has apps
                            val snapped = if (availableLetters.contains(rawLetter)) {
                                rawLetter
                            } else {
                                availableLetters.minByOrNull { avail ->
                                    val availSlot = FULL_ALPHABET.indexOf(avail)
                                    abs(availSlot - rawSlot)
                                } ?: rawLetter
                            }

                            if (snapped != currentLetter) {
                                currentLetter = snapped
                                activeLetter = snapped
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                letterToIndexMap[snapped]?.let { targetIndex ->
                                    coroutineScope.launch {
                                        listState.scrollToItem(targetIndex)
                                    }
                                }
                            }
                        }

                        updateLetterForY(down.position.y)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            updateLetterForY(change.position.y)
                        }

                        isDragging = false
                        val finalLetter = currentLetter
                        activeLetter = null

                        if (edgeScrubReleaseAction == EdgeScrubReleaseAction.LAUNCH_TOP && finalLetter != null) {
                            val targetIndex = letterToIndexMap[finalLetter]
                            if (targetIndex != null && targetIndex in sortedApps.indices) {
                                onLaunchApp(sortedApps[targetIndex])
                                onOpenChange(false)
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // While finger is down, show faint letters along the strip
            if (isDragging) {
                Column(
                    modifier = Modifier
                        .fillMaxHeight()
                        .padding(vertical = 16.dp),
                    verticalArrangement = Arrangement.SpaceBetween,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    FULL_ALPHABET.forEach { letter ->
                        val hasApps = availableLetters.contains(letter)
                        val isCurrent = letter == activeLetter
                        Text(
                            text = letter,
                            fontSize = 8.sp,
                            fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Normal,
                            color = if (isCurrent) AccentGold else if (hasApps) colors.textDimmed.copy(alpha = 0.5f) else colors.textDimmed.copy(alpha = 0.2f)
                        )
                    }
                }
            }
        }
    }
}
