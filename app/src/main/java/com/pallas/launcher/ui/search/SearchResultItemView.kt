package com.pallas.launcher.ui.search

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.EditNote
import androidx.compose.material.icons.filled.Event
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.QrCode2
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.WbSunny
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
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.compose.ui.platform.LocalContext
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.domain.search.SmartCommandType
import com.pallas.launcher.ui.apps.AppIconImage
import com.pallas.launcher.ui.components.LatexFormula
import com.pallas.launcher.ui.components.MathChartCanvas
import com.pallas.launcher.ui.components.QrCodeCanvas
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LauncherTypography
import com.pallas.launcher.ui.theme.LocalLauncherColors

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SearchResultItemView(
    result: LauncherSearchResult,
    isFirst: Boolean,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    onExpandToLatexStudio: ((String) -> Unit)? = null,
    onExpandToNotebook: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val haptic = LocalHapticFeedback.current
    val context = LocalContext.current

    val titleStyle = if (isFirst) {
        LauncherTypography.searchResultTitle.copy(fontWeight = FontWeight.SemiBold)
    } else {
        LauncherTypography.searchResultTitle
    }

    val rowShape = RoundedCornerShape(8.dp)
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = if (result is LauncherSearchResult.RichCard) 4.dp else 2.dp)
            .clip(rowShape)
            .background(
                when {
                    result is LauncherSearchResult.RichCard -> if (isFirst) colorScheme.surfaceContainerHigh else colorScheme.surfaceContainer
                    isFirst -> colorScheme.surfaceContainerHigh.copy(alpha = 0.72f)
                    else -> Color.Transparent
                }
            )
            .then(
                if (result is LauncherSearchResult.RichCard || isFirst) {
                    Modifier.border(
                        width = 1.dp,
                        color = if (isFirst) colorScheme.outlineVariant.copy(alpha = 0.5f)
                        else colorScheme.outlineVariant.copy(alpha = 0.3f),
                        shape = rowShape
                    )
                } else {
                    Modifier
                }
            )
            .combinedClickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick?.let {
                    {
                        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                        it()
                    }
                }
            )
            .padding(horizontal = 12.dp, vertical = if (result is LauncherSearchResult.RichCard) 11.dp else 9.dp),
        verticalAlignment = if (result is LauncherSearchResult.RichCard) Alignment.Top else Alignment.CenterVertically
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

                AppIconImage(app = result.app, size = 32.dp, shape = RoundedCornerShape(8.dp))
                Spacer(modifier = Modifier.width(14.dp))
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
                    if (result.subtitle.isNotEmpty()) {
                        Text(
                            text = result.subtitle,
                            style = LauncherTypography.searchResultSubtitle,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            is LauncherSearchResult.RichCard -> {
                if (result.colorSwatchArgb != null) {
                    val swatchColor = Color(result.colorSwatchArgb)
                    Box(
                        modifier = Modifier
                            .size(30.dp)
                            .clip(RoundedCornerShape(7.dp))
                            .background(swatchColor)
                            .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f), RoundedCornerShape(7.dp))
                    )
                } else {
                    CommandLeadingIcon(result.type)
                }
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
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            val isMathTitle = result.type == SmartCommandType.MATH_STATS
                            val isEditorialTitle = result.type == SmartCommandType.WIKI || result.type == SmartCommandType.DEFINE
                            Text(
                                text = result.title,
                                style = when {
                                    isMathTitle -> MaterialTheme.typography.labelLarge.copy(
                                        fontFamily = FontFamily.Monospace,
                                        fontWeight = FontWeight.Medium
                                    )
                                    isEditorialTitle -> MaterialTheme.typography.titleMedium.copy(
                                        fontFamily = LauncherFonts.serif,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                    else -> MaterialTheme.typography.labelLarge
                                },
                                color = if (isEditorialTitle) colorScheme.onSurface else colorScheme.onSurfaceVariant,
                                softWrap = true,
                                modifier = Modifier.weight(1f, fill = true)
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                result.badgeText?.let { badge ->
                                    val badgeColor = when (result.isBadgePositive) {
                                        true -> colorScheme.primary.copy(alpha = 0.12f)
                                        false -> colorScheme.errorContainer.copy(alpha = 0.65f)
                                        null -> colorScheme.surfaceContainerHighest
                                    }
                                    val borderColor = when (result.isBadgePositive) {
                                        true -> colorScheme.primary.copy(alpha = 0.28f)
                                        false -> colorScheme.error.copy(alpha = 0.28f)
                                        null -> colorScheme.outlineVariant.copy(alpha = 0.4f)
                                    }
                                    val textColor = when (result.isBadgePositive) {
                                        true -> colorScheme.primary
                                        false -> colorScheme.onErrorContainer
                                        null -> colorScheme.onSurfaceVariant
                                    }
                                    Surface(
                                        shape = RoundedCornerShape(5.dp),
                                        color = badgeColor,
                                        border = BorderStroke(1.dp, borderColor),
                                        modifier = Modifier.padding(start = 6.dp)
                                    ) {
                                        Text(
                                            text = badge,
                                            style = LauncherTypography.badge.copy(
                                                fontSize = 10.sp,
                                                letterSpacing = 0.3.sp
                                            ),
                                            color = textColor,
                                            maxLines = 1,
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }

                                if (!result.copyText.isNullOrBlank()) {
                                    IconButton(
                                        onClick = {
                                            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                            cm?.setPrimaryClip(ClipData.newPlainText(result.title, result.copyText))
                                            Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show()
                                        },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.ContentCopy,
                                            contentDescription = "Copy",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                if (result.type == SmartCommandType.LATEX && !result.latexSource.isNullOrBlank() && onExpandToLatexStudio != null) {
                                    IconButton(
                                        onClick = { onExpandToLatexStudio(result.rawInput ?: result.latexSource) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = "Expand in LaTeX Studio",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }

                                if (result.type == SmartCommandType.MATH_STATS && onExpandToNotebook != null) {
                                    IconButton(
                                        onClick = { onExpandToNotebook(result.rawInput ?: result.title) },
                                        modifier = Modifier.size(28.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.OpenInFull,
                                            contentDescription = "Expand in Computational Notebook",
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(15.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if ((result.type == SmartCommandType.WIKI || result.type == SmartCommandType.DEFINE) && result.subtitle.isNotEmpty()) {
                            Text(
                                text = result.subtitle,
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.primary.copy(alpha = 0.85f),
                                softWrap = true
                            )
                        }

                        if (!result.bodyText.isNullOrBlank()) {
                            Text(
                                text = result.bodyText,
                                style = if (result.type == SmartCommandType.EMI || result.type == SmartCommandType.CALENDAR) {
                                    MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    )
                                } else {
                                    MaterialTheme.typography.bodyMedium.copy(
                                        fontSize = 13.5.sp,
                                        lineHeight = 19.sp
                                    )
                                },
                                color = colorScheme.onSurface.copy(alpha = 0.92f),
                                maxLines = if (result.type == SmartCommandType.CALENDAR) 16 else 6,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }

                        if (result.primaryValue.isNotEmpty()) {
                            val isMathOrWolfram = result.type == SmartCommandType.MATH_STATS || result.type == SmartCommandType.WOLFRAM
                            val isMultilineConvert = result.type == SmartCommandType.CONVERT && result.primaryValue.contains('\n')
                            val isLongOrMultiline = (isMathOrWolfram &&
                                (result.primaryValue.length > 36 || result.primaryValue.contains('\n'))) || isMultilineConvert
                            val isLongConvert = result.type == SmartCommandType.CONVERT && result.primaryValue.length > 28
                            val formattedPrimary = remember(result.primaryValue, colorScheme.primary) {
                                if (result.primaryValue.startsWith("= ")) {
                                    buildAnnotatedString {
                                        withStyle(
                                            SpanStyle(
                                                color = colorScheme.primary,
                                                fontFamily = FontFamily.Monospace,
                                                fontWeight = FontWeight.Bold
                                            )
                                        ) {
                                            append("= ")
                                        }
                                        append(result.primaryValue.removePrefix("= "))
                                    }
                                } else {
                                    buildAnnotatedString { append(result.primaryValue) }
                                }
                            }
                            if (isLongOrMultiline) {
                                Surface(
                                    shape = RoundedCornerShape(7.dp),
                                    color = colorScheme.surfaceContainerLowest,
                                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 2.dp)
                                ) {
                                    val textModifier = if (isMultilineConvert) {
                                        Modifier
                                            .fillMaxWidth()
                                            .horizontalScroll(rememberScrollState())
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    } else {
                                        Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 8.dp)
                                    }
                                    Text(
                                        text = formattedPrimary,
                                        style = MaterialTheme.typography.bodyMedium.copy(
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = if (isMultilineConvert) 12.5.sp else 14.sp,
                                            lineHeight = 20.sp
                                        ),
                                        color = colorScheme.onSurface,
                                        softWrap = !isMultilineConvert,
                                        modifier = textModifier
                                    )
                                }
                            } else {
                                val valueStyle = when {
                                    isMathOrWolfram -> MaterialTheme.typography.titleLarge.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 28.sp
                                    )
                                    isLongConvert -> MaterialTheme.typography.titleMedium.copy(
                                        fontWeight = FontWeight.SemiBold,
                                        lineHeight = 24.sp
                                    )
                                    else -> LauncherTypography.cardValue.copy(lineHeight = 28.sp)
                                }
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    modifier = Modifier.fillMaxWidth().padding(vertical = 1.dp)
                                ) {
                                    Text(
                                        text = formattedPrimary,
                                        style = valueStyle,
                                        color = colorScheme.onSurface,
                                        softWrap = true,
                                        modifier = Modifier.weight(1f)
                                    )
                                    if (result.sparkline.size >= 2) {
                                        SparklineCanvas(
                                            points = result.sparkline,
                                            isPositive = result.isBadgePositive != false,
                                            modifier = Modifier.width(64.dp).height(24.dp).padding(start = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        if (!result.consoleOutput.isNullOrEmpty()) {
                            val barColor = colorScheme.primary.copy(alpha = 0.55f)
                            Surface(
                                shape = RoundedCornerShape(7.dp),
                                color = colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(top = 3.dp, bottom = 2.dp)
                            ) {
                                Text(
                                    text = result.consoleOutput,
                                    style = MaterialTheme.typography.bodySmall.copy(
                                        fontFamily = FontFamily.Monospace,
                                        lineHeight = 18.sp
                                    ),
                                    color = colorScheme.onSurface.copy(alpha = 0.9f),
                                    softWrap = true,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .drawBehind {
                                            drawRect(
                                                color = barColor,
                                                topLeft = Offset.Zero,
                                                size = Size(3.dp.toPx(), size.height)
                                            )
                                        }
                                        .padding(start = 11.dp, end = 10.dp, top = 7.dp, bottom = 7.dp)
                                )
                            }
                        }

                        if (!result.latexSource.isNullOrBlank()) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .horizontalScroll(rememberScrollState())
                                        .padding(horizontal = 12.dp, vertical = 10.dp),
                                    contentAlignment = Alignment.CenterStart
                                ) {
                                    LatexFormula(
                                        latex = result.latexSource,
                                        color = colorScheme.onSurface,
                                        fontSize = 20.sp
                                    )
                                }
                            }
                        }

                        if (result.colorSwatchArgb != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(26.dp)
                                    .padding(vertical = 3.dp)
                                    .clip(RoundedCornerShape(6.dp))
                                    .background(Color(result.colorSwatchArgb))
                                    .border(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f), RoundedCornerShape(6.dp))
                            )
                        }

                        if (result.qrMatrix != null) {
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.CenterStart
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(164.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(Color.White)
                                        .padding(6.dp)
                                ) {
                                    QrCodeCanvas(
                                        matrix = result.qrMatrix,
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                            }
                        }

                        if (result.chart != null) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colorScheme.surfaceContainerLowest,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp)
                            ) {
                                MathChartCanvas(
                                    chart = result.chart,
                                    isDark = LocalLauncherColors.current.isDark,
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .height(186.dp)
                                        .padding(horizontal = 8.dp, vertical = 8.dp)
                                )
                            }
                        }

                        if (result.subtitle.isNotEmpty() && result.type != SmartCommandType.WIKI && result.type != SmartCommandType.DEFINE) {
                            Text(
                                text = result.subtitle,
                                style = LauncherTypography.searchResultSubtitle.copy(lineHeight = 18.sp),
                                color = colorScheme.onSurfaceVariant,
                                softWrap = true
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
                    val details = listOfNotNull(
                        result.contactName.takeIf { it.isNotBlank() && !it.equals(result.nickname, ignoreCase = true) },
                        result.phoneNumber.takeIf { it.isNotBlank() }
                    ).joinToString(" • ")
                    if (details.isNotEmpty()) {
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
}

@Composable
private fun CommandLeadingIcon(type: SmartCommandType) {
    val colorScheme = MaterialTheme.colorScheme
    val tileShape = RoundedCornerShape(7.dp)
    Box(
        modifier = Modifier
            .size(30.dp)
            .clip(tileShape)
            .background(colorScheme.primary.copy(alpha = 0.12f))
            .border(1.dp, colorScheme.primary.copy(alpha = 0.22f), tileShape),
        contentAlignment = Alignment.Center
    ) {
        if (type == SmartCommandType.LATEX) {
            Text(
                text = "LaTeX",
                fontFamily = FontFamily.Serif,
                fontWeight = FontWeight.Bold,
                fontSize = 8.sp,
                letterSpacing = (-0.3).sp,
                maxLines = 1,
                color = colorScheme.primary
            )
        } else if (type == SmartCommandType.WIKI) {
            Text(
                text = "W",
                fontFamily = LauncherFonts.serif,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                maxLines = 1,
                color = colorScheme.primary
            )
        } else if (type == SmartCommandType.DEFINE) {
            Text(
                text = "Aa",
                fontFamily = LauncherFonts.serif,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                maxLines = 1,
                color = colorScheme.primary
            )
        } else {
            val icon = when (type) {
                SmartCommandType.ALARM -> Icons.Default.Alarm
                SmartCommandType.TIMER -> Icons.Default.Timer
                SmartCommandType.CALL -> Icons.Default.Call
                SmartCommandType.WEB_SEARCH -> Icons.Default.Search
                SmartCommandType.MATH_STATS, SmartCommandType.WOLFRAM -> Icons.Default.Functions
                SmartCommandType.CONVERT -> Icons.Default.SwapHoriz
                SmartCommandType.FINANCE -> Icons.AutoMirrored.Filled.ShowChart
                SmartCommandType.WEATHER -> Icons.Default.Cloud
                SmartCommandType.SOLAR -> Icons.Default.WbSunny
                SmartCommandType.WORLD_TIME -> Icons.Default.Public
                SmartCommandType.DATE_ARITHMETIC -> Icons.Default.DateRange
                SmartCommandType.NOTE -> Icons.Default.EditNote
                SmartCommandType.QR -> Icons.Default.QrCode2
                SmartCommandType.COLOR_BASE -> Icons.Default.Palette
                SmartCommandType.EMI -> Icons.Default.AccountBalance
                SmartCommandType.FOCUS -> Icons.Default.CenterFocusStrong
                SmartCommandType.FILE -> Icons.Default.Description
                SmartCommandType.CALENDAR -> Icons.Default.Event
                SmartCommandType.MACRO -> Icons.Default.Bolt
                SmartCommandType.HARDWARE -> Icons.Default.FlashlightOn
                SmartCommandType.APP_SEARCH -> Icons.Default.Search
                SmartCommandType.SETTINGS -> Icons.Default.Settings
                SmartCommandType.LATEX, SmartCommandType.WIKI, SmartCommandType.DEFINE -> Icons.Default.Functions
            }
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colorScheme.primary,
                modifier = Modifier.size(16.dp)
            )
        }
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


