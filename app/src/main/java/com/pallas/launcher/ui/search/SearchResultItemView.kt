package com.pallas.launcher.ui.search

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.ShowChart
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.theme.LauncherTypography

@Composable
fun SearchResultItemView(
    result: LauncherSearchResult,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    var showMathManual by remember { mutableStateOf(false) }

    if (showMathManual) {
        MathManualDialog(onDismiss = { showMathManual = false })
    }

    val titleStyle = if (isFirst) {
        LauncherTypography.searchResultTitle.copy(fontWeight = FontWeight.SemiBold)
    } else {
        LauncherTypography.searchResultTitle
    }

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(if (isFirst) colorScheme.surfaceContainerHigh.copy(alpha = 0.78f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        when (result) {
            is LauncherSearchResult.App -> {
                val label = result.app.label
                val annotated = remember(label, result.matchedRanges, colorScheme.primary) {
                    buildAnnotatedString {
                        val marks = BooleanArray(label.length)
                        result.matchedRanges.forEach { r ->
                            for (i in r.first.coerceAtLeast(0)..r.last.coerceAtMost(label.lastIndex)) marks[i] = true
                        }
                        var i = 0
                        while (i < label.length) {
                            var j = i
                            while (j < label.length && marks[j] == marks[i]) j++
                            val chunk = label.substring(i, j)
                            if (marks[i]) {
                                withStyle(SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.SemiBold)) {
                                    append(chunk)
                                }
                            } else {
                                append(chunk)
                            }
                            i = j
                        }
                    }
                }

                AppIconImage(app = result.app, size = 32.dp, shape = CircleShape)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = annotated,
                    style = titleStyle,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is LauncherSearchResult.Command -> {
                CommandLeadingIcon(result.type)
                Spacer(modifier = Modifier.width(14.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = titleStyle,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.subtitle,
                        style = LauncherTypography.searchResultSubtitle,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                if (result.hasInfoManual) {
                    IconButton(onClick = { showMathManual = true }, modifier = Modifier.size(30.dp)) {
                        Icon(
                            imageVector = Icons.Default.Info,
                            contentDescription = "Distribution Manual",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            is LauncherSearchResult.RichCard -> {
                CommandLeadingIcon(result.type)
                Spacer(modifier = Modifier.width(14.dp))

                if (result.isLoading) {
                    val infinite = rememberInfiniteTransition(label = "pulse")
                    val pulseAlpha by infinite.animateFloat(
                        initialValue = 0.35f,
                        targetValue = 0.95f,
                        animationSpec = infiniteRepeatable(tween(750), RepeatMode.Reverse),
                        label = "pulse_alpha"
                    )
                    Column(modifier = Modifier.weight(1f).alpha(pulseAlpha)) {
                        Text(
                            text = result.title,
                            style = LauncherTypography.searchResultTitle.copy(fontWeight = FontWeight.SemiBold),
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = result.subtitle,
                            style = LauncherTypography.searchResultSubtitle,
                            color = colorScheme.primary
                        )
                    }
                } else {
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = result.title,
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            result.badgeText?.let { badge ->
                                val badgeColor = when (result.isBadgePositive) {
                                    true -> colorScheme.primaryContainer
                                    false -> colorScheme.errorContainer
                                    null -> colorScheme.surfaceContainerHighest
                                }
                                val textColor = when (result.isBadgePositive) {
                                    true -> colorScheme.onPrimaryContainer
                                    false -> colorScheme.onErrorContainer
                                    null -> colorScheme.onSurfaceVariant
                                }
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = badgeColor,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = badge,
                                        style = LauncherTypography.badge,
                                        color = textColor,
                                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        if (result.primaryValue.isNotEmpty()) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = result.primaryValue,
                                    style = LauncherTypography.cardValue,
                                    color = colorScheme.onSurface,
                                    modifier = Modifier.weight(1f)
                                )
                                if (result.sparkline.size >= 2) {
                                    SparklineCanvas(
                                        points = result.sparkline,
                                        isPositive = result.isBadgePositive != false,
                                        modifier = Modifier.width(64.dp).height(22.dp).padding(start = 8.dp)
                                    )
                                }
                            }
                        }

                        if (result.subtitle.isNotEmpty()) {
                            Text(
                                text = result.subtitle,
                                style = LauncherTypography.searchResultSubtitle,
                                color = colorScheme.onSurfaceVariant,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (result.hasInfoManual) {
                        IconButton(onClick = { showMathManual = true }, modifier = Modifier.size(30.dp)) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = "Distribution Manual",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }

            is LauncherSearchResult.ContactCall -> {
                CommandLeadingIcon(SmartCommandType.CALL)
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Call ${result.nickname}",
                        style = titleStyle,
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOf(result.contactName, result.phoneNumber).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(
                        text = details,
                        style = LauncherTypography.searchResultSubtitle,
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun CommandLeadingIcon(type: SmartCommandType) {
    val colorScheme = MaterialTheme.colorScheme
    val icon = when (type) {
        SmartCommandType.ALARM -> Icons.Default.Alarm
        SmartCommandType.TIMER -> Icons.Default.Timer
        SmartCommandType.CALL -> Icons.Default.Call
        SmartCommandType.WEB_SEARCH -> Icons.Default.Search
        SmartCommandType.MATH_STATS, SmartCommandType.WOLFRAM -> Icons.Default.Functions
        SmartCommandType.FINANCE -> Icons.Default.ShowChart
        SmartCommandType.WEATHER -> Icons.Default.Cloud
        SmartCommandType.SOLAR -> Icons.Default.WbSunny
        SmartCommandType.SETTINGS -> Icons.Default.Settings
    }
    Box(
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(colorScheme.primaryContainer),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = colorScheme.onPrimaryContainer,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun SparklineCanvas(
    points: List<Float>,
    isPositive: Boolean,
    modifier: Modifier = Modifier
) {
    val strokeColor = if (isPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
    Canvas(modifier = modifier) {
        val min = points.minOrNull() ?: return@Canvas
        val max = points.maxOrNull() ?: return@Canvas
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val path = Path()
        points.forEachIndexed { idx, v ->
            val x = (idx.toFloat() / (points.size - 1)) * size.width
            val y = size.height - ((v - min) / range) * size.height
            if (idx == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path = path, color = strokeColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round))
        val lastY = size.height - ((points.last() - min) / range) * size.height
        drawCircle(color = strokeColor, radius = 2.5.dp.toPx(), center = Offset(size.width, lastY))
    }
}

@Composable
private fun MathManualDialog(onDismiss: () -> Unit) {
    val colorScheme = MaterialTheme.colorScheme
    val signatureStyle = MaterialTheme.typography.bodyMedium.copy(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    )
    val descriptionStyle = LauncherTypography.searchResultSubtitle
    val exampleStyle = MaterialTheme.typography.bodySmall

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(26.dp),
        containerColor = colorScheme.surfaceContainerHigh,
        title = {
            Text(
                text = "Probability, Fat Tails & Math",
                style = LauncherTypography.modalTitle,
                color = colorScheme.onSurface
            )
        },
        text = {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.height(380.dp)) {
                items(MathStatsEngine.MANUAL_ENTRIES) { entry ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colorScheme.surfaceContainer
                    ) {
                        Column(
                            modifier = Modifier.fillMaxWidth().padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Row(
                                horizontalArrangement = Arrangement.SpaceBetween,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(
                                    text = entry.signature,
                                    style = signatureStyle,
                                    color = colorScheme.primary
                                )
                                Text(
                                    text = entry.family,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            Text(
                                text = entry.description,
                                style = descriptionStyle,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "Example: ${entry.example}",
                                style = exampleStyle,
                                color = colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "Got it",
                    style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold)
                )
            }
        }
    )
}
