package com.pallas.launcher.ui.apps

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Shadow
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import androidx.compose.ui.graphics.graphicsLayer
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
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

// Alphabet rail items matching Image 1 and 2
private val RAIL_ITEMS = listOf("★") + ('A'..'Z').map { it.toString() } + listOf("•")

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

    val colors = LocalLauncherColors.current
    val haptic = LocalHapticFeedback.current
    val coroutineScope = rememberCoroutineScope()
    val fullListState = rememberLazyListState()
    val density = LocalDensity.current

    // Strict alphabetical order by normalized label
    val sortedApps = remember(installedApps) {
        installedApps.sortedWith(
            compareBy<AppItem> { SearchRanking.normalize(it.label) }
                .thenBy { it.label }
        )
    }

    // Apps grouped by letter for the full idle list
    val groupedApps = remember(sortedApps) {
        val map = linkedMapOf<String, MutableList<AppItem>>()
        for (app in sortedApps) {
            val norm = SearchRanking.normalize(app.label.trim())
            val first = norm.firstOrNull()?.uppercaseChar() ?: '#'
            val letter = if (first in 'A'..'Z') first.toString() else "#"
            map.getOrPut(letter) { mutableListOf() }.add(app)
        }
        map
    }

    // Map each letter to its index in LazyColumn
    val letterToScrollIndex = remember(groupedApps) {
        val map = mutableMapOf<String, Int>()
        var currentIndex = 0
        groupedApps.forEach { (letter, appsInLetter) ->
            map[letter] = currentIndex
            // 1 item for header + N items for apps
            currentIndex += 1 + appsInLetter.size
        }
        map
    }

    var isScrubbing by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf("A") }
    var touchYPx by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }

    // Smooth wave animated states (no abrupt snapping)
    val animatedBulge by animateFloatAsState(
        targetValue = if (isScrubbing) 1f else 0f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
        label = "rail_bulge"
    )

    val animatedTouchY by animateFloatAsState(
        targetValue = touchYPx,
        animationSpec = spring(stiffness = Spring.StiffnessMedium, dampingRatio = 0.9f),
        label = "rail_touch_y"
    )

    // Apps for currently active scrubbed letter
    val currentLetterApps = remember(activeLetter, sortedApps) {
        if (activeLetter == "★") {
            sortedApps.take(8)
        } else if (activeLetter == "•" || activeLetter == "#") {
            sortedApps.filter { app ->
                val norm = SearchRanking.normalize(app.label.trim())
                val c = norm.firstOrNull()?.uppercaseChar() ?: '#'
                c !in 'A'..'Z'
            }
        } else {
            val targetChar = activeLetter.firstOrNull()?.uppercaseChar()
            if (targetChar != null) {
                sortedApps.filter { app ->
                    val norm = SearchRanking.normalize(app.label.trim())
                    val c = norm.firstOrNull()?.uppercaseChar() ?: '#'
                    c == targetChar
                }
            } else {
                emptyList()
            }
        }
    }

    val isLeft = edgeScrubPosition == EdgeScrubPosition.LEFT
    val stripAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd

    Box(
        modifier = modifier.fillMaxSize()
    ) {
        val scrimColor = if (colors.isDark) Color(0x99000000) else Color(0x55000000)

        // App List Overlay (Faint overlay over wallpaper for high contrast & readability)
        AnimatedVisibility(
            visible = isOpen,
            enter = if (isLeft) slideInHorizontally { -it } + fadeIn() else slideInHorizontally { it } + fadeIn(),
            exit = if (isLeft) slideOutHorizontally { -it } + fadeOut() else slideOutHorizontally { it } + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(scrimColor)
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    .pointerInput(isLeft) {
                        detectHorizontalDragGestures { _, dragAmount ->
                            if (isLeft && dragAmount < -40f) {
                                onDismiss()
                            } else if (!isLeft && dragAmount > 40f) {
                                onDismiss()
                            }
                        }
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                val textShadow = Shadow(
                    color = Color.Black.copy(alpha = 0.65f),
                    offset = Offset(0f, 1.5f),
                    blurRadius = 4f
                )

                if (isScrubbing) {
                    // Actively scrolling through the list -> ONLY apps under activeLetter visible!
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .padding(
                                start = if (isLeft) 44.dp else 24.dp,
                                end = if (isLeft) 24.dp else 56.dp
                            )
                    ) {
                        Spacer(modifier = Modifier.height(16.dp))

                        // Large Section Letter Header (e.g. "I")
                        Text(
                            text = if (activeLetter == "★") "Favorites" else activeLetter,
                            style = MaterialTheme.typography.headlineMedium.copy(
                                fontWeight = FontWeight.Bold,
                                fontSize = 28.sp,
                                shadow = textShadow
                            ),
                            color = colors.textPrimary,
                            modifier = Modifier.padding(start = 12.dp, top = 8.dp, bottom = 12.dp)
                        )

                        // Apps under active letter
                        LazyColumn(
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(
                                items = currentLetterApps,
                                key = { it.uniqueKey }
                            ) { app ->
                                AppRowItem(
                                    app = app,
                                    textShadow = textShadow,
                                    onClick = { onLaunchApp(app) },
                                    onLongClick = { onAppLongPress(app) }
                                )
                            }

                            item {
                                Spacer(modifier = Modifier.height(64.dp))
                            }
                        }
                    }
                } else {
                    // Normal idle state -> full list separated by letter headers
                    Column(
                        modifier = Modifier
                            .fillMaxHeight()
                            .widthIn(max = 520.dp)
                            .fillMaxWidth()
                            .padding(
                                start = if (isLeft) 44.dp else 24.dp,
                                end = if (isLeft) 24.dp else 56.dp
                            )
                    ) {
                        LazyColumn(
                            state = fullListState,
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Spacer(modifier = Modifier.height(16.dp))
                            }

                            groupedApps.forEach { (letter, appsInLetter) ->
                                item(key = "header_$letter") {
                                    Text(
                                        text = letter,
                                        style = MaterialTheme.typography.titleLarge.copy(
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 22.sp,
                                            shadow = textShadow
                                        ),
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp)
                                    )
                                }

                                items(
                                    items = appsInLetter,
                                    key = { it.uniqueKey }
                                ) { app ->
                                    AppRowItem(
                                        app = app,
                                        textShadow = textShadow,
                                        onClick = { onLaunchApp(app) },
                                        onLongClick = { onAppLongPress(app) }
                                    )
                                }
                            }

                            item {
                                Spacer(modifier = Modifier.height(96.dp))
                            }
                        }
                    }
                }
            }
        }

        // Floating Letter Bubble at the peak of the curve (Image 2: rich green circle with white letter)
        if (isScrubbing && animatedBulge > 0.05f) {
            val bubbleRadiusPx = with(density) { 26.dp.toPx() }
            val bubbleY = (animatedTouchY - bubbleRadiusPx).coerceIn(
                with(density) { 32.dp.toPx() },
                (railHeightPx - with(density) { 64.dp.toPx() }).coerceAtLeast(32f)
            )

            val bubbleBulgeOffset = with(density) { (72.dp.toPx() * animatedBulge) }
            val bubbleAlignment = if (isLeft) Alignment.TopStart else Alignment.TopEnd
            val bubblePaddingModifier = if (isLeft) {
                Modifier.padding(start = 28.dp)
            } else {
                Modifier.padding(end = 28.dp)
            }

            Box(
                modifier = Modifier
                    .align(bubbleAlignment)
                    .then(bubblePaddingModifier)
                    .offset {
                        IntOffset(
                            x = if (isLeft) bubbleBulgeOffset.roundToInt() else -bubbleBulgeOffset.roundToInt(),
                            y = bubbleY.roundToInt()
                        )
                    }
                    .shadow(6.dp, CircleShape)
                    .clip(CircleShape)
                    .background(Color(0xFF2E7D32)) // Rich green as shown in Image 2
                    .size(52.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = activeLetter,
                    style = LauncherTypography.clock.copy(
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    ),
                    color = Color.White
                )
            }
        }

        // Edge Alphabet Rail with Smooth Niagara-Style Curve Animation (Image 1 & 2)
        Box(
            modifier = Modifier
                .align(stripAlignment)
                .fillMaxHeight()
                .width(42.dp)
                .systemGestureExclusion()
                .statusBarsPadding()
                .navigationBarsPadding()
                .onGloballyPositioned { coordinates ->
                    railHeightPx = coordinates.size.height.toFloat().coerceAtLeast(1f)
                }
                .pointerInput(Unit) {
                    awaitEachGesture {
                        val down = awaitFirstDown()
                        down.consume()

                        isScrubbing = true
                        if (!isOpen) {
                            onOpenChange(true)
                        }

                        fun updateTouch(y: Float) {
                            touchYPx = y
                            val fraction = (y / size.height).coerceIn(0f, 1f)
                            val slotIdx = (fraction * (RAIL_ITEMS.size - 1)).roundToInt().coerceIn(0, RAIL_ITEMS.size - 1)
                            val targetLetter = RAIL_ITEMS[slotIdx]

                            if (targetLetter != activeLetter) {
                                activeLetter = targetLetter
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                            }
                        }

                        updateTouch(down.position.y)

                        while (true) {
                            val event = awaitPointerEvent()
                            val change = event.changes.firstOrNull() ?: break
                            if (!change.pressed) {
                                change.consume()
                                break
                            }
                            change.consume()
                            updateTouch(change.position.y)
                        }

                        isScrubbing = false
                        val finalLetter = activeLetter

                        // If configured to launch on release:
                        if (edgeScrubReleaseAction == EdgeScrubReleaseAction.LAUNCH_TOP) {
                            val topApp = currentLetterApps.firstOrNull()
                            if (topApp != null) {
                                onLaunchApp(topApp)
                                onOpenChange(false)
                            }
                        } else {
                            // Scroll full list to that letter upon release
                            val targetIndex = letterToScrollIndex[finalLetter]
                            if (targetIndex != null) {
                                coroutineScope.launch {
                                    fullListState.scrollToItem(targetIndex)
                                }
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            // Letters column with smooth wave arc deformation
            Column(
                modifier = Modifier
                    .fillMaxHeight()
                    .padding(vertical = 12.dp),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val radiusPx = with(density) { 130.dp.toPx() }
                val maxBulgePx = with(density) { 48.dp.toPx() }

                RAIL_ITEMS.forEachIndexed { index, letter ->
                    val letterFraction = index.toFloat() / (RAIL_ITEMS.size - 1)
                    val letterY = letterFraction * railHeightPx
                    val dist = abs(letterY - animatedTouchY)

                    val factor = if (dist < radiusPx) {
                        val norm = dist / radiusPx
                        ((cos(norm * Math.PI.toFloat()) + 1f) / 2f) * animatedBulge
                    } else 0f

                    val xOffset = if (isLeft) (factor * maxBulgePx) else -(factor * maxBulgePx)
                    val scale = 1f + (factor * 0.45f)
                    val isCurrent = letter == activeLetter && isScrubbing

                    Text(
                        text = letter,
                        fontSize = 11.sp,
                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.Medium,
                        color = if (isCurrent) Color(0xFF2E7D32) else colors.textPrimary.copy(alpha = 0.85f),
                        modifier = Modifier
                            .graphicsLayer {
                                translationX = xOffset
                                scaleX = scale
                                scaleY = scale
                            }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRowItem(
    app: AppItem,
    textShadow: Shadow = Shadow.None,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLauncherColors.current

    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 7.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconImage(
            app = app,
            size = 42.dp,
            shape = CircleShape
        )
        Spacer(modifier = Modifier.width(16.dp))
        Text(
            text = app.label,
            style = LauncherTypography.appLabel.copy(
                fontSize = 17.sp,
                fontWeight = FontWeight.Normal,
                shadow = textShadow
            ),
            color = colors.textPrimary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
