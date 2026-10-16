package com.pallas.launcher.ui.home

import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalHomeContrastState
import com.pallas.launcher.ui.theme.LocalLauncherColors
import com.pallas.launcher.ui.theme.homeLocalContrastScrim
import com.pallas.launcher.ui.theme.scaled

@Composable
fun ClockHeader(
    timeString: String,
    dateString: String,
    alignment: ClockHorizontalAlignment,
    showDate: Boolean,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    fontScale: Float = 1.0f,
    paddingLeftDp: Int = 24,
    paddingRightDp: Int = 24,
    paddingTopDp: Int = 24,
    paddingBottomDp: Int = 12,
    shadow: Shadow? = null,
    modifier: Modifier = Modifier
) {
    val colors = LocalLauncherColors.current
    val contrastState = LocalHomeContrastState.current

    val horizontalAlignment = when (alignment) {
        ClockHorizontalAlignment.LEFT -> Alignment.Start
        ClockHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        ClockHorizontalAlignment.RIGHT -> Alignment.End
    }

    val textAlign = when (alignment) {
        ClockHorizontalAlignment.LEFT -> TextAlign.Start
        ClockHorizontalAlignment.CENTER -> TextAlign.Center
        ClockHorizontalAlignment.RIGHT -> TextAlign.End
    }

    val scrimAlignment = when (alignment) {
        ClockHorizontalAlignment.LEFT -> AppsHorizontalAlignment.LEFT
        ClockHorizontalAlignment.CENTER -> AppsHorizontalAlignment.CENTER
        ClockHorizontalAlignment.RIGHT -> AppsHorizontalAlignment.RIGHT
    }

    val effectiveTextPrimary = contrastState.resolvePrimaryText(colors.textPrimary)
    val effectiveTextSecondary = contrastState.resolveSecondaryText(colors.textSecondary)
    val effectiveShadow = if (contrastState.localScrimAlpha > 0f) null else shadow

    Column(
        modifier = modifier
            .fillMaxWidth()
            .homeLocalContrastScrim(
                alignment = scrimAlignment,
                scrimColor = contrastState.scrimColor,
                scrimAlpha = contrastState.localScrimAlpha
            )
            .padding(
                start = paddingLeftDp.dp,
                end = paddingRightDp.dp,
                top = paddingTopDp.dp,
                bottom = paddingBottomDp.dp
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = timeString.ifBlank { "12:00" },
            style = LauncherTypography.clock.scaled(fontScale).copy(
                shadow = effectiveShadow
            ),
            color = effectiveTextPrimary,
            textAlign = textAlign,
            modifier = Modifier
                .clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                    onClick = onClockClick
                )
        )

        if (showDate && dateString.isNotBlank()) {
            Text(
                text = dateString,
                style = LauncherTypography.dateAndBattery.scaled(fontScale).copy(
                    shadow = effectiveShadow
                ),
                color = effectiveTextSecondary,
                textAlign = textAlign,
                modifier = Modifier
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onDateClick
                    )
            )
        }
    }
}