package com.pallas.launcher.ui.home

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.unit.dp
import kotlin.math.abs
import androidx.compose.foundation.layout.BoxScope

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
    content: @Composable BoxScope.() -> Unit
) {
    val density = LocalDensity.current
    val shortSwipeMinPx = with(density) { 60.dp.toPx() }
    val longSwipeMinPx = with(density) { 180.dp.toPx() }
    val verticalThresholdPx = with(density) { 55.dp.toPx() }

    val haptic = LocalHapticFeedback.current
    var totalDragX by remember { mutableFloatStateOf(0f) }
    var totalDragY by remember { mutableFloatStateOf(0f) }
    var dragStartTime by remember { mutableLongStateOf(0L) }

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
            .pointerInput(enabled) {
                detectDragGestures(
                    onDragStart = {
                        totalDragX = 0f
                        totalDragY = 0f
                        dragStartTime = System.currentTimeMillis()
                    },
                    onDrag = { change, dragAmount ->
                        change.consume()
                        totalDragX += dragAmount.x
                        totalDragY += dragAmount.y
                    },
                    onDragEnd = {
                        val absX = abs(totalDragX)
                        val absY = abs(totalDragY)

                        if (absY > absX && absY >= verticalThresholdPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            if (totalDragY < 0) {
                                onSwipeUp()
                            } else {
                                onSwipeDown()
                            }
                        } else if (absX > absY && absX >= shortSwipeMinPx) {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            val isLongSwipe = absX >= longSwipeMinPx

                            if (totalDragX < 0) {
                                if (isLongSwipe) {
                                    onLongSwipeLeft()
                                } else {
                                    onShortSwipeLeft()
                                }
                            } else {
                                if (isLongSwipe) {
                                    onLongSwipeRight()
                                } else {
                                    onShortSwipeRight()
                                }
                            }
                        }
                    },
                    onDragCancel = {
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
