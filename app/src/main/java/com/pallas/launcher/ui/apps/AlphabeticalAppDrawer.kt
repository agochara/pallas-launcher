package com.pallas.launcher.ui.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.EdgeScrubPosition
import com.pallas.launcher.domain.model.EdgeScrubReleaseAction
import com.pallas.launcher.domain.search.SearchRanking
import com.pallas.launcher.ui.theme.NunitoFontFamily
import com.pallas.launcher.ui.theme.launcherBackdropBrush
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

/** Niagara rail: star (recent) on top, A-Z, dot (#) at the bottom. */
private val RAIL_ITEMS: List<String> = listOf("★") + ('A'..'Z').map { it.toString() } + listOf("•")
private val LETTER_ORDER: List<String> = ('A'..'Z').map { it.toString() } + "#"

private const val RECENT_HEADER = "Recent"

private sealed interface DrawerRow {
    val key: String

    data class Header(val label: String) : DrawerRow {
        override val key: String get() = "h_$label"
    }

    data class App(val app: AppItem, val section: String) : DrawerRow {
        override val key: String get() = "a_${section}_${app.uniqueKey}"
    }

    data object Empty : DrawerRow {
        override val key: String get() = "empty"
    }
}

private fun bucketOf(app: AppItem): String {
    val c = SearchRanking.normalize(app.label.trim()).firstOrNull()?.uppercaseChar()
    return if (c != null && c in 'A'..'Z') c.toString() else "#"
}

private fun railToBucket(rail: String): String = if (rail == "•") "#" else rail

@Composable
fun AlphabeticalAppDrawer(
    installedApps: List<AppItem>,
    favoriteApps: List<AppItem> = emptyList(),
    edgeScrubPosition: EdgeScrubPosition,
    edgeScrubReleaseAction: EdgeScrubReleaseAction,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val configuration = LocalConfiguration.current
    val topInset = (configuration.screenHeightDp.dp * 0.26f).coerceAtLeast(120.dp)
    val isLeft = edgeScrubPosition == EdgeScrubPosition.LEFT

    // ---------------------------------------------------------------- data
    val buckets: Map<String, List<AppItem>> = remember(installedApps) {
        installedApps
            .sortedWith(compareBy<AppItem>({ SearchRanking.normalize(it.label) }, { it.label }))
            .groupBy { bucketOf(it) }
    }

    val fullRows: List<DrawerRow> = remember(buckets, favoriteApps) {
        buildList {
            if (favoriteApps.isNotEmpty()) {
                add(DrawerRow.Header(RECENT_HEADER))
                favoriteApps.forEach { add(DrawerRow.App(it, "recent")) }
            }
            LETTER_ORDER.forEach { letter ->
                val apps = buckets[letter] ?: return@forEach
                add(DrawerRow.Header(letter))
                apps.forEach { add(DrawerRow.App(it, letter)) }
            }
        }
    }

    val headerIndex: Map<String, Int> = remember(fullRows) {
        buildMap {
            fullRows.forEachIndexed { i, row -> if (row is DrawerRow.Header) put(row.label, i) }
        }
    }

    /** Index to scroll to for a rail item. Letters with no apps jump to the next letter that has some. */
    fun scrollIndexFor(rail: String): Int {
        if (rail == "★") return headerIndex[RECENT_HEADER] ?: 0
        val bucket = railToBucket(rail)
        val start = LETTER_ORDER.indexOf(bucket).coerceAtLeast(0)
        for (i in start until LETTER_ORDER.size) {
            headerIndex[LETTER_ORDER[i]]?.let { return it }
        }
        return fullRows.lastIndex.coerceAtLeast(0)
    }

    fun appsFor(rail: String): List<AppItem> =
        if (rail == "★") favoriteApps else buckets[railToBucket(rail)].orEmpty()

    // ------------------------------------------------------------- state
    var isScrubbing by remember { mutableStateOf(false) }
    var activeLetter by remember { mutableStateOf("A") }
    var touchYPx by remember { mutableFloatStateOf(0f) }
    var railHeightPx by remember { mutableFloatStateOf(1f) }
    var pendingScrollTo by remember { mutableStateOf<String?>(null) }

    val fullListState = rememberLazyListState()
    val scrubListState = rememberLazyListState()

    val scrubRows: List<DrawerRow> = remember(activeLetter, buckets, favoriteApps) {
        val apps = appsFor(activeLetter)
        buildList {
            add(DrawerRow.Header(if (activeLetter == "★") RECENT_HEADER else railToBucket(activeLetter)))
            if (apps.isEmpty()) add(DrawerRow.Empty) else apps.forEach { add(DrawerRow.App(it, "scrub")) }
        }
    }

    // Reset to top whenever the drawer closes so it always re-opens at "Recent / A".
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            fullListState.scrollToItem(0)
            pendingScrollTo = null
        }
    }

    // Jump to the chosen letter only AFTER the full list is back on screen.
    LaunchedEffect(pendingScrollTo, isScrubbing) {
        val target = pendingScrollTo
        if (target != null && !isScrubbing) {
            fullListState.scrollToItem(scrollIndexFor(target))
            pendingScrollTo = null
        }
    }

    // ---- values read from inside the long-lived pointerInput block must NOT be captured by value.
    val latestIsOpen by rememberUpdatedState(isOpen)
    val latestOnOpenChange by rememberUpdatedState(onOpenChange)
    val latestOnLaunch by rememberUpdatedState(onLaunchApp)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestReleaseAction by rememberUpdatedState(edgeScrubReleaseAction)
    val latestAppsFor by rememberUpdatedState<(String) -> List<AppItem>>(::appsFor)

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

    val stripAlignment = if (isLeft) Alignment.BottomStart else Alignment.BottomEnd
    val dismissDragPx = with(density) { 96.dp.toPx() }

    Box(modifier = modifier.fillMaxSize()) {

        // ------------------------------------------------------- list overlay
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(tween(200)) + slideInVertically(tween(260, easing = FastOutSlowInEasing)) { it / 12 },
            exit = fadeOut(tween(160)) + slideOutVertically(tween(200, easing = FastOutSlowInEasing)) { it / 12 }
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(launcherBackdropBrush())
                    .statusBarsPadding()
                    .navigationBarsPadding()
                    // tap on the empty wallpaper area dismisses
                    .pointerInput(Unit) { detectTapGestures(onTap = { latestOnDismiss() }) }
                    // cumulative horizontal drag towards the rail's opposite side dismisses
                    .pointerInput(isLeft) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                if ((isLeft && total < -dismissDragPx) || (!isLeft && total > dismissDragPx)) {
                                    latestOnDismiss()
                                }
                                total = 0f
                            },
                            onDragCancel = { total = 0f },
                            onHorizontalDrag = { _, amount -> total += amount }
                        )
                    },
                contentAlignment = Alignment.TopCenter
            ) {
                Crossfade(
                    targetState = isScrubbing,
                    animationSpec = tween(120),
                    label = "scrub_crossfade"
                ) { scrubbing ->
                    DrawerList(
                        rows = if (scrubbing) scrubRows else fullRows,
                        state = if (scrubbing) scrubListState else fullListState,
                        topInset = topInset,
                        isLeft = isLeft,
                        onLaunchApp = onLaunchApp,
                        onAppLongPress = onAppLongPress
                    )
                }
            }
        }

        // ----------------------------------------------------------- rail
        if (edgeScrubPosition != EdgeScrubPosition.OFF) {
            Box(
                modifier = Modifier
                    .align(stripAlignment)
                    .fillMaxHeight(0.72f)
                    .navigationBarsPadding()
                    .width(110.dp),
                contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                // Floating letter bubble
                if (isScrubbing && animatedBulge > 0.05f) {
                    val bubbleRadiusPx = with(density) { 26.dp.toPx() }
                    val bubbleY = (animatedTouchY - bubbleRadiusPx)
                        .coerceIn(0f, (railHeightPx - bubbleRadiusPx * 2).coerceAtLeast(0f))
                    val bulgePx = with(density) { 72.dp.toPx() * animatedBulge }

                    Box(
                        modifier = Modifier
                            .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                            .padding(
                                start = if (isLeft) 24.dp else 0.dp,
                                end = if (!isLeft) 24.dp else 0.dp
                            )
                            .offset {
                                IntOffset(
                                    x = if (isLeft) bulgePx.roundToInt() else -bulgePx.roundToInt(),
                                    y = bubbleY.roundToInt()
                                )
                            }
                            .shadow(8.dp, CircleShape)
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .size(52.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = activeLetter,
                            style = TextStyle(
                                fontFamily = NunitoFontFamily,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            ),
                            color = colorScheme.onPrimary
                        )
                    }
                }

                // Touch strip + letters
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(42.dp)
                        .systemGestureExclusion()
                        .onGloballyPositioned { railHeightPx = it.size.height.toFloat().coerceAtLeast(1f) }
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()

                                fun slotFor(y: Float): String {
                                    val h = size.height.toFloat().coerceAtLeast(1f)
                                    val idx = ((y / h) * RAIL_ITEMS.size).toInt().coerceIn(0, RAIL_ITEMS.lastIndex)
                                    return RAIL_ITEMS[idx]
                                }

                                fun update(y: Float) {
                                    touchYPx = y
                                    val letter = slotFor(y)
                                    if (letter != activeLetter) {
                                        activeLetter = letter
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                isScrubbing = true
                                if (!latestIsOpen) latestOnOpenChange(true)
                                update(down.position.y)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    change.consume()
                                    if (!change.pressed) break
                                    update(change.position.y)
                                }

                                isScrubbing = false
                                val letter = activeLetter

                                if (latestReleaseAction == EdgeScrubReleaseAction.LAUNCH_TOP) {
                                    val top = latestAppsFor(letter).firstOrNull()
                                    if (top != null) {
                                        latestOnLaunch(top)
                                        latestOnOpenChange(false)
                                    } else {
                                        pendingScrollTo = letter
                                    }
                                } else {
                                    pendingScrollTo = letter
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    val slotPx = railHeightPx / RAIL_ITEMS.size
                    val slotDp = slotPx / density.density
                    val letterSize = (slotDp * 0.62f).coerceIn(8f, 13f).sp
                    val radiusPx = with(density) { 180.dp.toPx() }
                    val maxBulgePx = with(density) { 72.dp.toPx() }

                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        RAIL_ITEMS.forEachIndexed { index, letter ->
                            // Every slot has identical height, so this matches the touch -> slot mapping exactly.
                            val letterY = (index + 0.5f) * slotPx
                            val dist = abs(letterY - animatedTouchY)
                            val factor = if (dist < radiusPx) {
                                ((cos((dist / radiusPx) * Math.PI.toFloat()) + 1f) / 2f) * animatedBulge
                            } else 0f
                            val isCurrent = isScrubbing && letter == activeLetter

                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .fillMaxWidth(),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = letter,
                                    style = TextStyle(
                                        fontFamily = NunitoFontFamily,
                                        fontSize = letterSize,
                                        fontWeight = if (isCurrent) FontWeight.Bold else FontWeight.SemiBold
                                    ),
                                    color = if (isCurrent) colorScheme.primary
                                    else colorScheme.onSurface.copy(alpha = if (isScrubbing) 0.85f else 0.70f),
                                    modifier = Modifier.graphicsLayer {
                                        translationX = if (isLeft) factor * maxBulgePx else -(factor * maxBulgePx)
                                        val s = 1f + factor * 0.45f
                                        scaleX = s
                                        scaleY = s
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ------------------------------------------------------------------------------------------------

@Composable
private fun DrawerList(
    rows: List<DrawerRow>,
    state: LazyListState,
    topInset: Dp,
    isLeft: Boolean,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    LazyColumn(
        state = state,
        contentPadding = PaddingValues(top = topInset, bottom = 110.dp),
        modifier = Modifier
            .fillMaxHeight()
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            // keep rows clear of the rail
            .padding(start = if (isLeft) 64.dp else 24.dp, end = if (isLeft) 24.dp else 64.dp)
    ) {
        items(items = rows, key = { it.key }) { row ->
            when (row) {
                is DrawerRow.Header -> Text(
                    text = row.label,
                    style = TextStyle(
                        fontFamily = NunitoFontFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp
                    ),
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 20.dp, bottom = 8.dp)
                )

                is DrawerRow.App -> AppRowItem(
                    app = row.app,
                    onClick = { onLaunchApp(row.app) },
                    onLongClick = { onAppLongPress(row.app) }
                )

                DrawerRow.Empty -> Text(
                    text = "No apps",
                    style = TextStyle(fontFamily = NunitoFontFamily, fontSize = 15.sp),
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(start = 12.dp, top = 8.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun AppRowItem(
    app: AppItem,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "row_press")

    Row(
        modifier = modifier
            .fillMaxWidth()
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
                alpha = if (pressed) 0.75f else 1f
            }
            .combinedClickable(
                interactionSource = interaction,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconImage(app = app, size = 48.dp, shape = CircleShape)
        Spacer(modifier = Modifier.width(22.dp))
        Text(
            text = app.label,
            style = TextStyle(
                fontFamily = NunitoFontFamily,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium
            ),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
