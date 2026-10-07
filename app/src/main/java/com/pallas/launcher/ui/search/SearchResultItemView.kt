package com.pallas.launcher.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.theme.GaramondFontFamily

@Composable
fun SearchResultItemView(
    result: LauncherSearchResult,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 3.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(if (isFirst) colorScheme.surfaceContainerHigh.copy(alpha = 0.7f) else Color.Transparent)
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick
            )
            .padding(horizontal = 14.dp, vertical = 8.dp),
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
                                withStyle(SpanStyle(color = colorScheme.primary, fontWeight = FontWeight.Bold)) { append(chunk) }
                            } else append(chunk)
                            i = j
                        }
                    }
                }

                AppIconImage(app = result.app, size = 32.dp, shape = CircleShape)
                Spacer(modifier = Modifier.width(16.dp))
                Text(
                    text = annotated,
                    style = TextStyle(
                        fontFamily = GaramondFontFamily,
                        fontSize = 16.sp,
                        fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Medium
                    ),
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            is LauncherSearchResult.Command -> {
                val icon = when (result.type) {
                    SmartCommandType.ALARM -> Icons.Default.Alarm
                    SmartCommandType.TIMER -> Icons.Default.Timer
                    SmartCommandType.CALL -> Icons.Default.Call
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
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = result.title,
                        style = TextStyle(
                            fontFamily = GaramondFontFamily,
                            fontSize = 16.sp,
                            fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = result.subtitle,
                        style = TextStyle(
                            fontFamily = GaramondFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            is LauncherSearchResult.ContactCall -> {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Call,
                        contentDescription = null,
                        tint = colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(modifier = Modifier.width(16.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Call ${result.nickname}",
                        style = TextStyle(
                            fontFamily = GaramondFontFamily,
                            fontSize = 16.sp,
                            fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Medium
                        ),
                        color = colorScheme.onSurface,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    val details = listOf(result.contactName, result.phoneNumber).filter { it.isNotBlank() }.joinToString(" • ")
                    Text(
                        text = details,
                        style = TextStyle(
                            fontFamily = GaramondFontFamily,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Normal
                        ),
                        color = colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}