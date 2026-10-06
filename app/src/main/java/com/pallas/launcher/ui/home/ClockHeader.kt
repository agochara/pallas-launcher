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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.ClockHorizontalAlignment
import com.pallas.launcher.domain.model.TextAlignmentOption
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@Composable
fun ClockHeader(
    timeString: String,
    dateString: String,
    alignment: ClockHorizontalAlignment,
    showDate: Boolean,
    onClockClick: () -> Unit,
    onDateClick: () -> Unit,
    fontScale: Float = 1.0f,
    modifier: Modifier = Modifier
) {
    val colors = LocalLauncherColors.current

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

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(start = 28.dp, end = 28.dp, top = 26.dp, bottom = 12.dp),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text(
            text = timeString.ifBlank { "12:00" },
            style = LauncherTypography.clock.copy(
                fontSize = (78 * fontScale).sp,
                lineHeight = (82 * fontScale).sp,
                shadow = colors.shadow
            ),
            color = colors.textPrimary,
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
                style = LauncherTypography.dateAndBattery.copy(
                    fontSize = (16 * fontScale).sp,
                    lineHeight = (22 * fontScale).sp,
                    shadow = colors.shadow
                ),
                color = colors.textSecondary,
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
