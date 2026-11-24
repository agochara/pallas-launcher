package com.pallas.launcher.ui.reference

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.reference.ElementCategory
import com.pallas.launcher.domain.reference.PeriodicElement
import com.pallas.launcher.domain.reference.PeriodicTableData
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LocalLauncherColors
import java.util.Locale

@Composable
fun PeriodicTableScreen(
    modifier: Modifier = Modifier,
    initialSelectedElement: PeriodicElement? = null
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalLauncherColors.current.isDark

    var query by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf<ElementCategory?>(null) }
    var activeElement by remember { mutableStateOf(initialSelectedElement) }

    val elementsByCell = remember {
        PeriodicTableData.ELEMENTS.associateBy { it.gridRow to it.gridCol }
    }

    val normalizedQuery = query.trim().lowercase(Locale.ROOT)
    val matchingElements = remember(normalizedQuery, selectedCategory) {
        PeriodicTableData.ELEMENTS.filter { el ->
            val catOk = selectedCategory == null || el.category == selectedCategory
            val qOk = normalizedQuery.isEmpty() ||
                el.symbol.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                el.name.lowercase(Locale.ROOT).contains(normalizedQuery) ||
                el.atomicNumber.toString() == normalizedQuery ||
                el.category.label.lowercase(Locale.ROOT).contains(normalizedQuery)
            catOk && qOk
        }
    }
    val matchingNumbers = remember(matchingElements) {
        matchingElements.map { it.atomicNumber }.toSet()
    }
    val hasActiveFilter = normalizedQuery.isNotEmpty() || selectedCategory != null

    Column(modifier = modifier.fillMaxSize()) {
        // Search bar
        Surface(
            shape = RoundedCornerShape(10.dp),
            color = colorScheme.surfaceContainerLow,
            border = BorderStroke(
                width = 1.dp,
                color = if (query.isNotEmpty()) colorScheme.primary.copy(alpha = 0.55f)
                else colorScheme.outlineVariant.copy(alpha = 0.45f)
            ),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 14.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Search,
                    contentDescription = null,
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Box(
                    modifier = Modifier.weight(1f),
                    contentAlignment = Alignment.CenterStart
                ) {
                    if (query.isEmpty()) {
                        Text(
                            text = "Search by symbol, name, or atomic number…",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    BasicTextField(
                        value = query,
                        onValueChange = { query = it },
                        singleLine = true,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colorScheme.onSurface,
                            fontSize = 15.sp
                        ),
                        cursorBrush = SolidColor(colorScheme.primary),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = { query = "" },
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Clear,
                            contentDescription = "Clear search",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Category filter chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item(key = "cat_all") {
                CategoryFilterChip(
                    label = "All (118)",
                    selected = selectedCategory == null,
                    accent = colorScheme.primary,
                    onClick = { selectedCategory = null }
                )
            }
            items(ElementCategory.entries, key = { it.name }) { cat ->
                CategoryFilterChip(
                    label = cat.label,
                    selected = selectedCategory == cat,
                    accent = categoryAccent(cat, isDark),
                    onClick = {
                        selectedCategory = if (selectedCategory == cat) null else cat
                    }
                )
            }
        }

        // Quick jump strip when searching or filtering
        if (hasActiveFilter) {
            if (matchingElements.isEmpty()) {
                Text(
                    text = "No elements match your filter",
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            } else {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    items(matchingElements.take(16), key = { "hit_${it.atomicNumber}" }) { el ->
                        val accent = categoryAccent(el.category, isDark)
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = colorScheme.surfaceContainerHigh,
                            border = BorderStroke(1.dp, accent.copy(alpha = 0.55f)),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { activeElement = el }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "${el.atomicNumber}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = accent,
                                    fontFamily = FontFamily.Monospace
                                )
                                Text(
                                    text = el.symbol,
                                    style = MaterialTheme.typography.labelLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = el.name,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }

        val horizScroll = rememberScrollState()
        val vertScroll = rememberScrollState()
        val cellWidth = 54.dp
        val cellHeight = 56.dp
        val cellGap = 3.dp

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .verticalScroll(vertScroll)
                .horizontalScroll(horizScroll)
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(cellGap)) {
                // Column group numbers 1..18
                Row(horizontalArrangement = Arrangement.spacedBy(cellGap)) {
                    Spacer(modifier = Modifier.width(18.dp))
                    for (col in 1..18) {
                        Box(
                            modifier = Modifier.width(cellWidth),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "$col",
                                style = MaterialTheme.typography.labelSmall,
                                fontFamily = FontFamily.Monospace,
                                color = colorScheme.outline,
                                fontSize = 10.sp
                            )
                        }
                    }
                }

                // Periods 1..7
                for (row in 1..7) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(cellGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "$row",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(18.dp)
                        )
                        for (col in 1..18) {
                            val element = elementsByCell[row to col]
                            if (element != null) {
                                val dimmed = hasActiveFilter && element.atomicNumber !in matchingNumbers
                                PeriodicElementTile(
                                    element = element,
                                    accent = categoryAccent(element.category, isDark),
                                    dimmed = dimmed,
                                    width = cellWidth,
                                    height = cellHeight,
                                    onClick = { activeElement = element }
                                )
                            } else if ((row == 6 || row == 7) && col == 3) {
                                // Lanthanide / Actinide placeholder marker
                                Box(
                                    modifier = Modifier
                                        .size(cellWidth, cellHeight)
                                        .clip(RoundedCornerShape(6.dp))
                                        .background(colorScheme.surfaceContainerLowest.copy(alpha = 0.6f))
                                        .border(
                                            1.dp,
                                            colorScheme.outlineVariant.copy(alpha = 0.3f),
                                            RoundedCornerShape(6.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = if (row == 6) "57–71" else "89–103",
                                        style = MaterialTheme.typography.labelSmall,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 9.5.sp,
                                        color = colorScheme.primary.copy(alpha = 0.8f)
                                    )
                                }
                            } else {
                                Spacer(modifier = Modifier.size(cellWidth, cellHeight))
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Lanthanides (row 9) and Actinides (row 10)
                for (row in listOf(9, 10)) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(cellGap),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = if (row == 9) "La" else "Ac",
                            style = MaterialTheme.typography.labelSmall,
                            fontFamily = FontFamily.Monospace,
                            color = colorScheme.outline,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.width(18.dp)
                        )
                        for (col in 1..18) {
                            val element = elementsByCell[row to col]
                            if (element != null) {
                                val dimmed = hasActiveFilter && element.atomicNumber !in matchingNumbers
                                PeriodicElementTile(
                                    element = element,
                                    accent = categoryAccent(element.category, isDark),
                                    dimmed = dimmed,
                                    width = cellWidth,
                                    height = cellHeight,
                                    onClick = { activeElement = element }
                                )
                            } else {
                                Spacer(modifier = Modifier.size(cellWidth, cellHeight))
                            }
                        }
                    }
                }
            }
        }
    }

    activeElement?.let { el ->
        ElementDetailBottomSheet(
            element = el,
            isDark = isDark,
            onDismiss = { activeElement = null }
        )
    }
}

@Composable
private fun CategoryFilterChip(
    label: String,
    selected: Boolean,
    accent: Color,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = if (selected) accent.copy(alpha = 0.2f) else colorScheme.surfaceContainerLow,
        border = BorderStroke(
            1.dp,
            if (selected) accent.copy(alpha = 0.7f) else colorScheme.outlineVariant.copy(alpha = 0.35f)
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            color = if (selected) accent else colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun PeriodicElementTile(
    element: PeriodicElement,
    accent: Color,
    dimmed: Boolean,
    width: androidx.compose.ui.unit.Dp,
    height: androidx.compose.ui.unit.Dp,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(6.dp),
        color = accent.copy(alpha = if (dimmed) 0.04f else 0.13f),
        border = BorderStroke(
            width = 1.dp,
            color = accent.copy(alpha = if (dimmed) 0.14f else 0.45f)
        ),
        modifier = Modifier
            .size(width, height)
            .alpha(if (dimmed) 0.28f else 1f)
            .clip(RoundedCornerShape(6.dp))
            .clickable(onClick = onClick)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            Text(
                text = "${element.atomicNumber}",
                style = TextStyle(
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    lineHeight = 11.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    color = accent
                ),
                maxLines = 1,
                modifier = Modifier.align(Alignment.TopStart)
            )
            Text(
                text = element.symbol,
                style = TextStyle(
                    fontFamily = LauncherFonts.serif,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    lineHeight = 22.sp,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    color = colorScheme.onSurface
                ),
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(top = 6.dp)
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ElementDetailBottomSheet(
    element: PeriodicElement,
    isDark: Boolean,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val accent = categoryAccent(element.category, isDark)
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    fun formatTemp(k: Double?): String {
        if (k == null) return "—"
        val c = k - 273.15
        return String.format(Locale.US, "%.2f K (%.1f °C)", k, c)
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        containerColor = colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Header card
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = accent.copy(alpha = 0.16f),
                    border = BorderStroke(1.5.dp, accent.copy(alpha = 0.65f)),
                    modifier = Modifier.size(68.dp)
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(6.dp),
                        verticalArrangement = Arrangement.SpaceBetween,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "${element.atomicNumber}",
                            fontFamily = FontFamily.Monospace,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = accent
                        )
                        Text(
                            text = element.symbol,
                            fontFamily = LauncherFonts.serif,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = colorScheme.onSurface
                        )
                    }
                }

                Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text(
                        text = element.name,
                        fontFamily = LauncherFonts.serif,
                        fontSize = 26.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colorScheme.onSurface
                    )
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = accent.copy(alpha = 0.14f),
                        border = BorderStroke(1.dp, accent.copy(alpha = 0.35f))
                    ) {
                        Text(
                            text = element.category.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = accent,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
            }

            HorizontalDivider(color = colorScheme.outlineVariant.copy(alpha = 0.35f))

            val rows = listOf(
                "Atomic Number" to "${element.atomicNumber}",
                "Atomic Mass" to "${element.atomicMass} u",
                "Period / Group" to "Period ${element.period} · Group ${element.group}",
                "Phase at STP" to element.phaseAtStp,
                "Electron Configuration" to element.electronConfig,
                "Electronegativity (Pauling)" to (element.electronegativity?.let { String.format(Locale.US, "%.2f", it) } ?: "—"),
                "Oxidation States" to element.oxidationStates,
                "Melting Point" to formatTemp(element.meltingPointK),
                "Boiling Point" to formatTemp(element.boilingPointK),
                "Density" to (element.densityGcm3?.let { "$it g/cm³" } ?: "—")
            )

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    rows.forEachIndexed { idx, (label, value) ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = label,
                                style = MaterialTheme.typography.bodySmall,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(end = 12.dp)
                            )
                            Text(
                                text = value,
                                style = MaterialTheme.typography.bodyMedium,
                                fontFamily = if (label == "Electron Configuration" || label == "Atomic Mass") FontFamily.Monospace else FontFamily.Default,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface,
                                textAlign = TextAlign.End,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                        }
                        if (idx < rows.lastIndex) {
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                                modifier = Modifier.padding(horizontal = 14.dp)
                            )
                        }
                    }
                }
            }

            OutlinedButton(
                onClick = {
                    val summary = "${element.name} (${element.symbol}, Z=${element.atomicNumber}) • Mass: ${element.atomicMass} u • ${element.electronConfig} • ${element.category.label}"
                    TexExportHelper.copyPlainText(context, element.name, summary, "Copied ${element.name} properties")
                },
                shape = RoundedCornerShape(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Copy Element Summary")
            }
        }
    }
}

fun categoryAccent(category: ElementCategory, isDark: Boolean): Color = if (isDark) {
    when (category) {
        ElementCategory.ALKALI_METAL -> Color(0xFFE08D79)
        ElementCategory.ALKALINE_EARTH -> Color(0xFFE2C48E)
        ElementCategory.TRANSITION_METAL -> Color(0xFF9BB4D4)
        ElementCategory.POST_TRANSITION -> Color(0xFF8EC5B6)
        ElementCategory.METALLOID -> Color(0xFFB5CEA8)
        ElementCategory.REACTIVE_NONMETAL -> Color(0xFFA7C492)
        ElementCategory.HALOGEN -> Color(0xFFD9A76C)
        ElementCategory.NOBLE_GAS -> Color(0xFFC3A6D8)
        ElementCategory.LANTHANIDE -> Color(0xFFD4A5A5)
        ElementCategory.ACTINIDE -> Color(0xFFC492B1)
    }
} else {
    when (category) {
        ElementCategory.ALKALI_METAL -> Color(0xFFB04632)
        ElementCategory.ALKALINE_EARTH -> Color(0xFF725A2E)
        ElementCategory.TRANSITION_METAL -> Color(0xFF345E8A)
        ElementCategory.POST_TRANSITION -> Color(0xFF287360)
        ElementCategory.METALLOID -> Color(0xFF476938)
        ElementCategory.REACTIVE_NONMETAL -> Color(0xFF3A6B35)
        ElementCategory.HALOGEN -> Color(0xFF9A5B13)
        ElementCategory.NOBLE_GAS -> Color(0xFF6B428C)
        ElementCategory.LANTHANIDE -> Color(0xFF8C4545)
        ElementCategory.ACTINIDE -> Color(0xFF7A3A63)
    }
}
