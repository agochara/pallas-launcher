package com.pallas.launcher.ui.home

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import kotlin.math.sign

enum class SwipeDir {
    UP,
    DOWN,
    LEFT,
    RIGHT
}

@Composable
fun GestureContainer(
    onSwipeUp: () -> Unit,
    onSwipeDown: () -> Unit,
    onShortSwipeLeft: () -> Unit,
    onLongSwipeLeft: () -> Unit,
    onShortSwipeRight: () -> Unit,
    onLongSwipeRight: () -> Unit,
    onBackgroundLongPress: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    canLongSwipeHorizontal: Boolean = true,
    labelFor: (SwipeDir, isLong: Boolean) -> String? = { _, _ -> null },
    onHorizontalDrag: ((dragX: Float) -> Unit)? = null,
    onHorizontalDragEnd: ((isCommitLong: Boolean, isLeft: Boolean) -> Unit)? = null,
    onHorizontalDragCancel: (() -> Unit)? = null,
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val shortSwipeMinPx = with(density) { 40.dp.toPx() }
    val longSwipeMinPx = with(density) { 140.dp.toPx() }
    val verticalThresholdPx = with(density) { 40.dp.toPx() }

    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }

    val gestureModifier = if (enabled) {
        modifier
            .fillMaxSize()
            .pointerInput(enabled) {
                detectTapGestures(
                    onLongPress = {
                        onBackgroundLongPress()
                    }
                )
            }
            .pointerInput(enabled, canLongSwipeHorizontal) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                        if (canLongSwipeHorizontal && abs(totalDragX) >= abs(totalDragY)) {
                            val excess = (abs(totalDragX) - shortSwipeMinPx).coerceAtLeast(0f) * sign(totalDragX)
                            onHorizontalDrag?.invoke(excess)
                        } else {
                            onHorizontalDrag?.invoke(0f)
                        }
                    },
                    onDragEnd = {
                        val finalAbsX = abs(totalDragX)
                        val finalAbsY = abs(totalDragY)
                        val finalIsHorizontal = finalAbsX >= finalAbsY

                        if (!finalIsHorizontal && finalAbsY >= verticalThresholdPx) {
                            onHorizontalDragCancel?.invoke()
                            if (totalDragY < 0) {
                                onSwipeUp()
                            } else {
                                onSwipeDown()
                            }
                        } else if (finalIsHorizontal && finalAbsX >= shortSwipeMinPx) {
                            val commitLong = canLongSwipeHorizontal && finalAbsX >= longSwipeMinPx
                            val isLeft = totalDragX < 0
                            if (onHorizontalDragEnd != null) {
                                onHorizontalDragEnd(commitLong, isLeft)
                            } else {
                                if (isLeft) {
                                    if (commitLong) onLongSwipeLeft() else onShortSwipeLeft()
                                } else {
                                    if (commitLong) onLongSwipeRight() else onShortSwipeRight()
                                }
                            }
                        } else {
                            onHorizontalDragCancel?.invoke()
                        }
                        totalDragX = 0f
                        totalDragY = 0f
                    },
                    onDragCancel = {
                        onHorizontalDragCancel?.invoke()
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