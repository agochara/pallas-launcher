package com.pallas.launcher.ui.home

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

enum class SwipeDir {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

private enum class DragAxis {
    NONE,
    HORIZONTAL,
    VERTICAL
}

@Composable
fun GestureContainer(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onSwipeDownLeft: () -> Unit = onSwipeDown,
    onSwipeDownRight: () -> Unit = onSwipeDown,
    onShortSwipeLeft: () -> Unit,
    onLongSwipeLeft: () -> Unit,
    onShortSwipeRight: () -> Unit,
    onLongSwipeRight: () -> Unit,
    onBackgroundLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canLongSwipeHorizontal: Boolean = true,
    onDoubleTap: (() -> Unit)? = null,
    labelFor: ((SwipeDir, isLong: Boolean) -> String?)? = null,
    onHorizontalDrag: ((dragX: Float) -> Unit)? = null,
    onHorizontalDragEnd: ((isCommitLong: Boolean, isLeft: Boolean) -> Unit)? = null,
    onHorizontalDragCancel: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val haptic = LocalHapticFeedback.current
    val shortSwipeMinPx = with(density) { 40.dp.toPx() }
    val longSwipeMinPx = with(density) { 140.dp.toPx() }
    val verticalThresholdPx = with(density) { 40.dp.toPx() }
    val touchSlopPx = with(density) { 16.dp.toPx() }

    val currentOnSwipeUp by rememberUpdatedState(onSwipeUp)
    val currentOnSwipeDown by rememberUpdatedState(onSwipeDown)
    val currentOnSwipeDownLeft by rememberUpdatedState(onSwipeDownLeft)
    val currentOnSwipeDownRight by rememberUpdatedState(onSwipeDownRight)
    val currentOnShortSwipeLeft by rememberUpdatedState(onShortSwipeLeft)
    val currentOnLongSwipeLeft by rememberUpdatedState(onLongSwipeLeft)
    val currentOnShortSwipeRight by rememberUpdatedState(onShortSwipeRight)
    val currentOnLongSwipeRight by rememberUpdatedState(onLongSwipeRight)
    val currentOnBackgroundLongPress by rememberUpdatedState(onBackgroundLongPress)
    val currentOnDoubleTap by rememberUpdatedState(onDoubleTap)
    val currentOnHorizontalDrag by rememberUpdatedState(onHorizontalDrag)
    val currentOnHorizontalDragEnd by rememberUpdatedState(onHorizontalDragEnd)
    val currentOnHorizontalDragCancel by rememberUpdatedState(onHorizontalDragCancel)

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var lockedAxis by remember { mutableStateOf(DragAxis.NONE) }
    var verticalSwipeStartX by remember { mutableFloatStateOf(0f) }

    val gestureModifier = if (enabled) {
        modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                detectTapGestures(
                    onDoubleTap = currentOnDoubleTap?.let { action ->
                        {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            action()
                        }
                    },
                    onLongPress = {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        currentOnBackgroundLongPress()
                    }
                )
            }
            .pointerInput(enabled, canLongSwipeHorizontal) {
                detectDragGestures(
                    onDragStart = { start ->
                        totalDragX = 0f
                        totalDragY = 0f
                        lockedAxis = DragAxis.NONE
                        verticalSwipeStartX = start.x
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        val absX = abs(totalDragX)
                        val absY = abs(totalDragY)

                        if (lockedAxis == DragAxis.NONE) {
                            if (maxOf(absX, absY) >= touchSlopPx) {
                                lockedAxis = if (absX >= absY) DragAxis.HORIZONTAL else DragAxis.VERTICAL
                            }
                        }

                        if (lockedAxis == DragAxis.HORIZONTAL) {
                            if (canLongSwipeHorizontal) {
                                val excess = (absX - shortSwipeMinPx).coerceAtLeast(0f) * sign(totalDragX)
                                currentOnHorizontalDrag?.invoke(excess)
                            } else {
                                currentOnHorizontalDrag?.invoke(0f)
                            }
                        } else if (lockedAxis == DragAxis.VERTICAL) {
                            currentOnHorizontalDrag?.invoke(0f)
                        }
                    },
                    onDragEnd = {
                        val wasHorizontal = lockedAxis == DragAxis.HORIZONTAL
                        val wasVertical = lockedAxis == DragAxis.VERTICAL
                        val finalAbsX = abs(totalDragX)
                        val finalAbsY = abs(totalDragY)
                        lockedAxis = DragAxis.NONE

                        if (wasVertical && finalAbsY >= verticalThresholdPx) {
                            currentOnHorizontalDragCancel?.invoke()
                            if (totalDragY < 0) {
                                currentOnSwipeUp()
                            } else {
                                if (verticalSwipeStartX < size.width / 2f) currentOnSwipeDownLeft() else currentOnSwipeDownRight()
                            }
                        } else if (wasHorizontal && finalAbsX >= shortSwipeMinPx) {
                            val commitLong = canLongSwipeHorizontal && finalAbsX >= longSwipeMinPx
                            val isLeft = totalDragX < 0
                            val onEnd = currentOnHorizontalDragEnd
                            if (onEnd != null) {
                                onEnd(commitLong, isLeft)
                            } else {
                                if (isLeft) {
                                    if (commitLong) currentOnLongSwipeLeft() else currentOnShortSwipeLeft()
                                } else {
                                    if (commitLong) currentOnLongSwipeRight() else currentOnShortSwipeRight()
                                }
                            }
                        } else {
                            currentOnHorizontalDragCancel?.invoke()
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        lockedAxis = DragAxis.NONE
                        currentOnHorizontalDragCancel?.invoke()
                        totalDragX = 0f
                        totalDragY = 0f
                    }
                )
            }
    } else {
        modifier.fillMaxSize()
    }

    Box(modifier = gestureModifier) {
        content()
    }
}
