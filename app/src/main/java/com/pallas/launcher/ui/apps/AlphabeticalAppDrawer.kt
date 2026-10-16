package com.pallas.launcher.ui.apps

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
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
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsIgnoringVisibility
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.systemGestureExclusion
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
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
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalHomeContrastState
import com.pallas.launcher.ui.theme.LocalLauncherColors
import com.pallas.launcher.ui.theme.launcherBackdropBrush
import com.pallas.launcher.ui.theme.scaled
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------------------------------------------
// Niagara-style drawer.
//
//  * The rail lives in the lower ~72% of the screen (thumb zone) and the list starts at ~26% of the screen height,
//    so both line up and everything is reachable one-handed.
//  * While you drag on the rail, the list is REPLACED by a preview of just the active letter (what you see is what
//    you get). The full list only jumps to that letter after you lift your finger.
//  * Rail letters bulge inward in a smooth wave around the finger, and a gold bubble follows it.
//  * Sorting inside a letter uses the usage-score tiers, and the data is frozen while the drawer is open so rows
//    never reshuffle under your thumb.
// ---------------------------------------------------------------------------------------------------------------

private const val RECENT_SLOT = "RECENT"
private val LETTER_ORDER: List<String> = ('A'..'Z').map { it.toString() } + "#"
private const val HYSTERESIS_MARGIN = 0.5

// Layout
private const val RAIL_HEIGHT_FRACTION = 0.72f
private const val LIST_TOP_FRACTION = 0.26f
private val RAIL_MIN_SLOT = 14.dp
private val RAIL_MAX_SLOT = 24.dp
private val RAIL_STRIP_WIDTH = 52.dp      // touch strip, flush with the screen edge (letters are centred in it)
private val RAIL_ZONE_WIDTH = 120.dp      // room for the bubble
private val RAIL_BOTTOM_MARGIN = 12.dp
private val BUBBLE_SIZE = 52.dp
private val BUBBLE_EDGE_PADDING = 24.dp

// Fisheye
private val FISHEYE_RADIUS = 180.dp
private val FISHEYE_SHIFT = 72.dp
private const val FISHEYE_SCALE = 0.45f

private val DrawerHeaderStyle = TextStyle(
    fontFamily = LauncherFonts.serif,
    fontWeight = FontWeight.SemiBold,
    fontSize = 22.sp,
    lineHeight = 28.sp,
    letterSpacing = 0.sp
)

private sealed interface DrawerRow {
    val key: String

    /** [slot] is either [RECENT_SLOT] or a bucket letter. */
    data class Header(val slot: String, val label: String) : DrawerRow {
        override val key: String get() = "h_$slot"
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

private fun labelFor(slot: String): String = if (slot == RECENT_SLOT) "Recent" else slot

@Composable
fun AlphabeticalAppDrawer(
    installedApps: List<AppItem>,
    favoriteApps: List<AppItem> = emptyList(),
    usageScores: Map<String, Double> = emptyMap(),
    edgeScrubPosition: EdgeScrubPosition,
    edgeScrubReleaseAction: EdgeScrubReleaseAction,
    isOpen: Boolean,
    onOpenChange: (Boolean) -> Unit,
    onLaunchApp: (AppItem) -> Unit,
    onAppLongPress: (AppItem) -> Unit,
    onDismiss: () -> Unit,
    fontScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val density = LocalDensity.current
    val isLeft = edgeScrubPosition == EdgeScrubPosition.LEFT

    // ------------------------------------------------------------------ data (frozen while open)
    var frozenInstalledApps by remember { mutableStateOf(installedApps) }
    var frozenFavoriteApps by remember { mutableStateOf(favoriteApps) }
    var frozenUsageScores by remember { mutableStateOf(usageScores) }

    LaunchedEffect(isOpen, installedApps) {
        if (isOpen || frozenInstalledApps.isEmpty()) {
            frozenInstalledApps = installedApps
            frozenFavoriteApps = favoriteApps
            frozenUsageScores = usageScores
        }
    }

    val useFrozen = isOpen && frozenInstalledApps.isNotEmpty()
    val activeApps = if (useFrozen) frozenInstalledApps else installedApps
    val activeFavorites = if (useFrozen) frozenFavoriteApps else favoriteApps
    val activeScores = if (useFrozen) frozenUsageScores else usageScores

    val buckets: Map<String, List<AppItem>> = remember(activeApps, activeScores) {
        activeApps
            .sortedWith { a, b ->
                val scoreA = activeScores[a.uniqueKey] ?: activeScores[a.packageName] ?: 0.0
                val scoreB = activeScores[b.uniqueKey] ?: activeScores[b.packageName] ?: 0.0
                val tierA = (scoreA / HYSTERESIS_MARGIN).toInt()
                val tierB = (scoreB / HYSTERESIS_MARGIN).toInt()
                if (tierA != tierB) {
                    tierB.compareTo(tierA)
                } else {
                    val cmp = SearchRanking.normalize(a.label).compareTo(SearchRanking.normalize(b.label))
                    if (cmp != 0) cmp else a.label.compareTo(b.label)
                }
            }
            .groupBy { bucketOf(it) }
    }

    val fullRows: List<DrawerRow> = remember(buckets, activeFavorites) {
        buildList {
            if (activeFavorites.isNotEmpty()) {
                add(DrawerRow.Header(RECENT_SLOT, labelFor(RECENT_SLOT)))
                activeFavorites.forEach { add(DrawerRow.App(it, RECENT_SLOT)) }
            }
            LETTER_ORDER.forEach { letter ->
                val apps = buckets[letter] ?: return@forEach
                add(DrawerRow.Header(letter, letter))
                apps.forEach { add(DrawerRow.App(it, letter)) }
            }
        }
    }

    val headerIndex: Map<String, Int> = remember(fullRows) {
        buildMap {
            fullRows.forEachIndexed { i, row -> if (row is DrawerRow.Header) put(row.slot, i) }
        }
    }

    // Fixed positions: Recent (only if there are favourites) + A-Z + #.
    val railSlots: List<String> = remember(activeFavorites.isNotEmpty()) {
        buildList {
            if (activeFavorites.isNotEmpty()) add(RECENT_SLOT)
            addAll(LETTER_ORDER)
        }
    }

    fun hasAppsInSlot(slot: String): Boolean =
        if (slot == RECENT_SLOT) activeFavorites.isNotEmpty() else !buckets[slot].isNullOrEmpty()

    /**
     * Maps a rail slot to the bucket that is actually shown/scrolled to/launched: the slot itself if it has apps,
     * otherwise the next populated letter, otherwise the previous one. Null only when there are no apps at all.
     */
    fun resolveSlot(slot: String): String? {
        if (slot == RECENT_SLOT) {
            if (activeFavorites.isNotEmpty()) return RECENT_SLOT
            return LETTER_ORDER.firstOrNull { !buckets[it].isNullOrEmpty() }
        }
        val start = LETTER_ORDER.indexOf(slot).coerceAtLeast(0)
        for (i in start until LETTER_ORDER.size) {
            if (!buckets[LETTER_ORDER[i]].isNullOrEmpty()) return LETTER_ORDER[i]
        }
        for (i in (start - 1) downTo 0) {
            if (!buckets[LETTER_ORDER[i]].isNullOrEmpty()) return LETTER_ORDER[i]
        }
        return null
    }

    fun scrollIndexFor(slot: String): Int = resolveSlot(slot)?.let { headerIndex[it] } ?: 0

    fun appsFor(slot: String): List<AppItem> {
        val resolved = resolveSlot(slot) ?: return emptyList()
        return if (resolved == RECENT_SLOT) activeFavorites else buckets[resolved].orEmpty()
    }

    // ------------------------------------------------------------------ state
    var isScrubbing by remember { mutableStateOf(false) }
    var activeSlot by remember { mutableStateOf("A") }
    var touchYPx by remember { mutableFloatStateOf(0f) }
    var pendingScrollTo by remember { mutableStateOf<String?>(null) }

    val fullListState = rememberLazyListState()
    val scrubListState = rememberLazyListState()

    LaunchedEffect(railSlots) {
        if (activeSlot !in railSlots) activeSlot = railSlots.first()
    }

    // The preview list: header + the apps of the letter under the finger.
    val scrubRows: List<DrawerRow> = remember(activeSlot, buckets, activeFavorites) {
        val resolved = resolveSlot(activeSlot)
        buildList {
            if (resolved == null) {
                add(DrawerRow.Empty)
            } else {
                add(DrawerRow.Header(resolved, labelFor(resolved)))
                appsFor(activeSlot).forEach { add(DrawerRow.App(it, "scrub")) }
            }
        }
    }

    LaunchedEffect(activeSlot, isScrubbing) {
        if (isScrubbing) scrubListState.scrollToItem(0)
    }

    // Keep the rail highlight in sync with the full list while the user scrolls it by hand.
    val firstVisibleIndex by remember { derivedStateOf { fullListState.firstVisibleItemIndex } }
    LaunchedEffect(firstVisibleIndex, isScrubbing, isOpen) {
        if (isOpen && !isScrubbing) {
            val slot = when (val row = fullRows.getOrNull(firstVisibleIndex)) {
                is DrawerRow.Header -> row.slot
                is DrawerRow.App -> row.section
                DrawerRow.Empty, null -> null
            }
            if (slot != null && slot in railSlots) activeSlot = slot
        }
    }

    // Re-open at the top every time.
    LaunchedEffect(isOpen) {
        if (!isOpen) {
            fullListState.scrollToItem(0)
            pendingScrollTo = null
            railSlots.firstOrNull()?.let { activeSlot = it }
        }
    }

    // Jump the FULL list to the chosen letter only after the finger is lifted.
    LaunchedEffect(pendingScrollTo, isScrubbing) {
        val target = pendingScrollTo
        if (target != null && !isScrubbing) {
            fullListState.scrollToItem(scrollIndexFor(target))
            pendingScrollTo = null
        }
    }

    // Values read from inside the long-lived pointerInput block must not be captured by value.
    val latestIsOpen by rememberUpdatedState(isOpen)
    val latestOnOpenChange by rememberUpdatedState(onOpenChange)
    val latestOnLaunch by rememberUpdatedState(onLaunchApp)
    val latestOnDismiss by rememberUpdatedState(onDismiss)
    val latestReleaseAction by rememberUpdatedState(edgeScrubReleaseAction)
    val latestAppsFor by rememberUpdatedState<(String) -> List<AppItem>>(::appsFor)
    val latestRailSlots by rememberUpdatedState(railSlots)

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

    val statusBarTop = WindowInsets.statusBarsIgnoringVisibility.asPaddingValues().calculateTopPadding()
    val navBarBottom = WindowInsets.navigationBarsIgnoringVisibility.asPaddingValues().calculateBottomPadding()
    val dismissDragPx = with(density) { 96.dp.toPx() }
    val slideOffsetPx = with(density) { 12.dp.roundToPx() }

    val launcherColors = LocalLauncherColors.current
    val contrast = LocalHomeContrastState.current

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
        val topInset = (maxHeight * LIST_TOP_FRACTION - statusBarTop).coerceAtLeast(72.dp)

        // ------------------------------------------------------------- list overlay
        AnimatedVisibility(
            visible = isOpen,
            enter = fadeIn(spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy)) +
                    slideInVertically(
                        animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioNoBouncy),
                        initialOffsetY = { slideOffsetPx }
                    ),
            exit = fadeOut(tween(120))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(launcherBackdropBrush())
                    .padding(top = statusBarTop, bottom = navBarBottom)
                    // tap on empty space dismisses (rows consume their own taps)
                    .pointerInput(Unit) { detectTapGestures(onTap = { latestOnDismiss() }) }
                    // a long horizontal drag away from the rail dismisses
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
                        fontScale = fontScale,
                        onLaunchApp = onLaunchApp,
                        onAppLongPress = onAppLongPress
                    )
                }
            }
        }

        // ------------------------------------------------------------- rail
        if (edgeScrubPosition != EdgeScrubPosition.OFF && railSlots.isNotEmpty()) {
            val slotCount = railSlots.size
            val maxRailHeight = (maxHeight - navBarBottom - statusBarTop - RAIL_BOTTOM_MARGIN - 16.dp)
                .coerceAtLeast(RAIL_MIN_SLOT)
            val idealSlot = (maxHeight * RAIL_HEIGHT_FRACTION / slotCount).coerceIn(RAIL_MIN_SLOT, RAIL_MAX_SLOT)
            val railHeightDp = (idealSlot * slotCount).coerceAtMost(maxRailHeight)
            val slotDp = railHeightDp / slotCount
            val slotPx = with(density) { slotDp.toPx() }.coerceAtLeast(1f)
            val railHeightPx = slotPx * slotCount
            val latestSlotPx by rememberUpdatedState(slotPx)
            val latestRailHeightPx by rememberUpdatedState(railHeightPx)

            val fisheyeRadiusPx = with(density) { FISHEYE_RADIUS.toPx() }
            val fisheyeShiftPx = with(density) { FISHEYE_SHIFT.toPx() }
            val letterSizeSp = (slotDp.value * 0.55f).coerceIn(10f, 13f)
            val railLetterStyle = LauncherTypography.railLetter.copy(
                fontSize = letterSizeSp.sp,
                lineHeight = (letterSizeSp + 3f).sp
            )

            // On the open drawer the letters sit on the drawer backdrop; on the home screen they sit on the wallpaper,
            // so they use the home contrast-aware ink instead.
            val idleInk = if (isOpen) colorScheme.onSurface else contrast.resolveSecondaryText(launcherColors.textSecondary)

            Box(
                modifier = Modifier
                    .align(if (isLeft) Alignment.BottomStart else Alignment.BottomEnd)
                    .padding(bottom = navBarBottom + RAIL_BOTTOM_MARGIN)
                    .width(RAIL_ZONE_WIDTH)
                    .height(railHeightDp),
                contentAlignment = if (isLeft) Alignment.CenterStart else Alignment.CenterEnd
            ) {
                // Floating bubble
                if (isScrubbing || animatedBulge > 0.02f) {
                    val bubbleRadiusPx = with(density) { (BUBBLE_SIZE / 2).toPx() }
                    Box(
                        modifier = Modifier
                            .align(if (isLeft) Alignment.TopStart else Alignment.TopEnd)
                            .padding(
                                start = if (isLeft) BUBBLE_EDGE_PADDING else 0.dp,
                                end = if (!isLeft) BUBBLE_EDGE_PADDING else 0.dp
                            )
                            .offset {
                                val bubbleY = (animatedTouchY - bubbleRadiusPx)
                                    .coerceIn(0f, (railHeightPx - bubbleRadiusPx * 2f).coerceAtLeast(0f))
                                val bulge = fisheyeShiftPx * animatedBulge
                                IntOffset(
                                    x = if (isLeft) bulge.roundToInt() else -bulge.roundToInt(),
                                    y = bubbleY.roundToInt()
                                )
                            }
                            .graphicsLayer { alpha = animatedBulge.coerceIn(0f, 1f) }
                            .clip(CircleShape)
                            .background(colorScheme.primary)
                            .size(BUBBLE_SIZE),
                        contentAlignment = Alignment.Center
                    ) {
                        if (activeSlot == RECENT_SLOT) {
                            Icon(
                                imageVector = Icons.Default.Schedule,
                                contentDescription = null,
                                tint = colorScheme.onPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        } else {
                            Text(
                                text = activeSlot,
                                style = LauncherTypography.drawerRow.copy(
                                    fontSize = 22.sp,
                                    lineHeight = 26.sp,
                                    fontWeight = FontWeight.Bold
                                ),
                                color = colorScheme.onPrimary
                            )
                        }
                    }
                }

                // Touch strip + letters. Same height as the visible rail, so y / slotPx is exactly the slot index.
                Box(
                    modifier = Modifier
                        .width(RAIL_STRIP_WIDTH)
                        .fillMaxHeight()
                        // Note: Android honours at most ~200dp of exclusion per edge.
                        .systemGestureExclusion()
                        .pointerInput(Unit) {
                            awaitEachGesture {
                                val down = awaitFirstDown()
                                down.consume()

                                fun slotFor(y: Float): String {
                                    val slots = latestRailSlots
                                    val idx = (y / latestSlotPx).toInt().coerceIn(0, slots.lastIndex)
                                    return slots[idx]
                                }

                                fun update(y: Float) {
                                    touchYPx = y.coerceIn(0f, latestRailHeightPx)
                                    val slot = slotFor(y)
                                    if (slot != activeSlot) {
                                        activeSlot = slot
                                        haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                    }
                                }

                                isScrubbing = true
                                if (!latestIsOpen) latestOnOpenChange(true)
                                activeSlot = slotFor(down.position.y)
                                touchYPx = down.position.y.coerceIn(0f, latestRailHeightPx)
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)

                                while (true) {
                                    val event = awaitPointerEvent()
                                    val change = event.changes.firstOrNull() ?: break
                                    change.consume()
                                    if (!change.pressed) break
                                    update(change.position.y)
                                }

                                isScrubbing = false
                                val slot = activeSlot

                                if (latestReleaseAction == EdgeScrubReleaseAction.LAUNCH_TOP) {
                                    val top = latestAppsFor(slot).firstOrNull()
                                    if (top != null) {
                                        latestOnLaunch(top)
                                        latestOnOpenChange(false)
                                    } else {
                                        pendingScrollTo = slot
                                    }
                                } else {
                                    pendingScrollTo = slot
                                }
                            }
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        modifier = Modifier.fillMaxSize(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        railSlots.forEachIndexed { index, slot ->
                            val slotCenterPx = (index + 0.5f) * slotPx
                            val isCurrent = (isOpen || isScrubbing) && slot == activeSlot
                            val populated = hasAppsInSlot(slot)
                            val ink = when {
                                isCurrent -> colorScheme.primary
                                populated -> idleInk.copy(alpha = if (isScrubbing) 0.85f else 0.72f)
                                else -> idleInk.copy(alpha = 0.30f)
                            }

                            Box(
                                modifier = Modifier
                                    .height(slotDp)
                                    .fillMaxWidth()
                                    .graphicsLayer {
                                        val dist = abs(slotCenterPx - animatedTouchY)
                                        val f = if (dist < fisheyeRadiusPx) {
                                            ((cos((dist / fisheyeRadiusPx) * Math.PI.toFloat()) + 1f) / 2f) * animatedBulge
                                        } else 0f
                                        translationX = if (isLeft) f * fisheyeShiftPx else -(f * fisheyeShiftPx)
                                        val s = 1f + f * FISHEYE_SCALE
                                        scaleX = s
                                        scaleY = s
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                if (slot == RECENT_SLOT) {
                                    Icon(
                                        imageVector = Icons.Default.Schedule,
                                        contentDescription = null,
                                        tint = ink,
                                        modifier = Modifier.size((letterSizeSp + 1f).dp)
                                    )
                                } else {
                                    Text(
                                        text = slot,
                                        style = if (isCurrent) {
                                            railLetterStyle.copy(fontWeight = FontWeight.Bold)
                                        } else {
                                            railLetterStyle
                                        },
                                        color = ink
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

// ----------------------------------------------------------------------------------------------------------------

@Composable
private fun DrawerList(
    rows: List<DrawerRow>,
    state: LazyListState,
    topInset: Dp,
    isLeft: Boolean,
    fontScale: Float,
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
            .padding(
                start = if (isLeft) 64.dp else 24.dp,
                end = if (isLeft) 24.dp else 64.dp
            )
    ) {
        items(items = rows, key = { it.key }) { row ->
            when (row) {
                is DrawerRow.Header -> Text(
                    text = row.label,
                    style = DrawerHeaderStyle.scaled(fontScale),
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = 12.dp, top = 18.dp, bottom = 6.dp)
                )

                is DrawerRow.App -> AppRowItem(
                    app = row.app,
                    fontScale = fontScale,
                    onClick = { onLaunchApp(row.app) },
                    onLongClick = { onAppLongPress(row.app) }
                )

                DrawerRow.Empty -> Text(
                    text = "No apps",
                    style = LauncherTypography.searchResultSubtitle,
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
    fontScale: Float,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.97f else 1f, label = "row_press")
    val iconSize = (44f * fontScale).dp

    Row(
        modifier = modifier
            .fillMaxWidth()
            .heightIn(min = (56f * fontScale).dp)
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
            .padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        AppIconImage(app = app, size = iconSize, shape = AppIconShape.CIRCLE)
        Spacer(modifier = Modifier.width(18.dp))
        Text(
            text = app.label,
            style = LauncherTypography.drawerRow.scaled(fontScale),
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}