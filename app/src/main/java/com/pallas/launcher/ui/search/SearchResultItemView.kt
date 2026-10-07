package com.pallas.launcher.ui.search

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.pallas.launcher.domain.search.AppSearchResult
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.theme.NunitoFontFamily

@Composable
fun SearchResultItemView(
    result: AppSearchResult,
    isFirst: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val label = result.app.label

    // Highlight the matched characters (ranges come from the index; clamp defensively)
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
        AppIconImage(app = result.app, size = 48.dp, shape = CircleShape)
        Spacer(modifier = Modifier.width(20.dp))
        Text(
            text = annotated,
            style = TextStyle(
                fontFamily = NunitoFontFamily,
                fontSize = 16.sp,
                fontWeight = if (isFirst) FontWeight.SemiBold else FontWeight.Medium
            ),
            color = colorScheme.onSurface,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}