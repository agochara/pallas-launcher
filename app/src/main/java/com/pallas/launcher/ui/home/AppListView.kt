package com.pallas.launcher.ui.home

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.model.AppsHorizontalAlignment
import com.pallas.launcher.domain.model.ScreenApp
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppListView(
    apps: List<ScreenApp>,
    alignment: AppsHorizontalAlignment,
    fontScale: Float,
    paddingLeftDp: Int = 32,
    paddingRightDp: Int = 32,
    paddingTopDp: Int = 12,
    paddingBottomDp: Int = 24,
    isLoaded: Boolean = true,
    onAppClick: (ScreenApp) -> Unit,
    onAppLongClick: (ScreenApp) -> Unit,
    onEmptyScreenClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalLauncherColors.current

    val horizontalAlignment = when (alignment) {
        AppsHorizontalAlignment.LEFT -> Alignment.Start
        AppsHorizontalAlignment.CENTER -> Alignment.CenterHorizontally
        AppsHorizontalAlignment.RIGHT -> Alignment.End
    }

    val textAlign = when (alignment) {
        AppsHorizontalAlignment.LEFT -> TextAlign.Start
        AppsHorizontalAlignment.CENTER -> TextAlign.Center
        AppsHorizontalAlignment.RIGHT -> TextAlign.End
    }

    if (apps.isEmpty()) {
        if (!isLoaded) return // Prevent cold-start flash while apps are loading from disk/PackageManager
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(
                    start = paddingLeftDp.dp,
                    end = paddingRightDp.dp,
                    top = paddingTopDp.dp,
                    bottom = paddingBottomDp.dp
                ),
            horizontalAlignment = horizontalAlignment
        ) {
            Text(
                text = "+ Add apps to this screen",
                style = LauncherTypography.dateAndBattery.copy(shadow = colors.shadow),
                color = colors.textDimmed,
                textAlign = textAlign,
                modifier = Modifier
                    .combinedClickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onEmptyScreenClick
                    )
                    .padding(vertical = 12.dp)
            )
        }
        return
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(
                start = paddingLeftDp.dp,
                end = paddingRightDp.dp,
                top = paddingTopDp.dp,
                bottom = paddingBottomDp.dp
            ),
        horizontalAlignment = horizontalAlignment,
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        apps.forEach { app ->
            val interactionSource = remember { MutableInteractionSource() }
            val isPressed by interactionSource.collectIsPressedAsState()
            val scale by animateFloatAsState(targetValue = if (isPressed) 0.96f else 1.0f, label = "app_scale")
            val textAlpha by animateFloatAsState(targetValue = if (isPressed) 0.7f else 1.0f, label = "app_press")

            Text(
                text = app.displayLabel,
                style = LauncherTypography.appLabel.copy(
                    fontSize = (25 * fontScale).sp,
                    lineHeight = (34 * fontScale).sp,
                    shadow = colors.shadow
                ),
                color = colors.textPrimary,
                textAlign = textAlign,
                modifier = Modifier
                    .graphicsLayer {
                        scaleX = scale
                        scaleY = scale
                        alpha = textAlpha
                    }
                    .combinedClickable(
                        interactionSource = interactionSource,
                        indication = null,
                        onClick = { onAppClick(app) },
                        onLongClick = { onAppLongClick(app) }
                    )
                    .padding(vertical = 4.dp)
            )
        }
    }
}
