package com.pallas.launcher.ui.reference

import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDownward
import androidx.compose.material.icons.filled.ArrowUpward
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.reference.EngineeringCheatsheetData
import com.pallas.launcher.domain.reference.EngineeringMaterial
import com.pallas.launcher.domain.reference.GeometryFinanceCsBioData
import com.pallas.launcher.domain.reference.PhysicalConstant
import com.pallas.launcher.domain.reference.PhysicsCheatsheetData
import com.pallas.launcher.domain.search.UnitConverterEngine
import com.pallas.launcher.platform.CurrencyRateService
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.components.LatexFormula
import com.pallas.launcher.ui.theme.LauncherFonts
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

// =========================================================================
// Physical Constants Table (Physics Cheatsheet Tab)
// =========================================================================

@Composable
fun PhysicalConstantsTable(
    constants: List<PhysicalConstant> = PhysicsCheatsheetData.PHYSICAL_CONSTANTS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var filter by remember { mutableStateOf("") }
    val filtered = remember(filter, constants) {
        val q = filter.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) constants
        else constants.filter {
            it.name.lowercase(Locale.ROOT).contains(q) ||
                it.symbolLatex.lowercase(Locale.ROOT).contains(q) ||
                it.unit.lowercase(Locale.ROOT).contains(q) ||
                it.note.lowercase(Locale.ROOT).contains(q)
        }
    }

    Column(modifier = modifier.fillMaxSize()) {
        CompactFilterField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = "Filter physical constants…",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "const_hint") {
                Text(
                    text = "${filtered.size} CODATA constants · Tap any row to copy value",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.outline,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
                )
            }
            itemsIndexed(filtered, key = { _, c -> c.name }) { idx, c ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val clip = if (c.unit == "dimensionless") c.value else "${c.value} ${c.unit}"
                            TexExportHelper.copyPlainText(context, c.name, clip, "Copied ${c.name}: $clip")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Box(
                            modifier = Modifier.width(52.dp),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            LatexFormula(
                                latex = c.symbolLatex,
                                color = colorScheme.primary,
                                fontSize = 16.sp
                            )
                        }
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(2.dp)
                        ) {
                            Text(
                                text = c.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            Text(
                                text = "${c.value}  ${if (c.unit == "dimensionless") "" else c.unit}".trim(),
                                fontFamily = FontFamily.Monospace,
                                fontSize = 12.5.sp,
                                color = colorScheme.primary
                            )
                            if (c.note.isNotBlank()) {
                                Text(
                                    text = c.note,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    if (idx < filtered.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.28f),
                            modifier = Modifier.padding(horizontal = 20.dp)
                        )
                    }
                }
            }
        }
    }
}

// =========================================================================
// Engineering Materials Reference Table (Sortable + Scrollable)
// =========================================================================

private enum class MaterialSortColumn(val label: String) {
    NAME("Material"),
    DENSITY("ρ (kg/m³)"),
    MODULUS("E (GPa)"),
    YIELD("Yield (MPa)"),
    ULTIMATE("UTS (MPa)")
}

@Composable
fun EngineeringMaterialsTable(
    materials: List<EngineeringMaterial> = EngineeringCheatsheetData.ENGINEERING_MATERIALS,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    var sortCol by remember { mutableStateOf(MaterialSortColumn.NAME) }
    var ascending by remember { mutableStateOf(true) }
    var filter by remember { mutableStateOf("") }

    val sorted = remember(materials, sortCol, ascending, filter) {
        val q = filter.trim().lowercase(Locale.ROOT)
        val filtered = if (q.isEmpty()) materials else materials.filter {
            it.name.lowercase(Locale.ROOT).contains(q) || it.note.lowercase(Locale.ROOT).contains(q)
        }
        val cmp = Comparator<EngineeringMaterial> { a, b ->
            when (sortCol) {
                MaterialSortColumn.NAME -> a.name.compareTo(b.name, ignoreCase = true)
                MaterialSortColumn.DENSITY -> a.densityKgM3.compareTo(b.densityKgM3)
                MaterialSortColumn.MODULUS -> a.youngsModulusGPa.compareTo(b.youngsModulusGPa)
                MaterialSortColumn.YIELD -> (a.yieldStrengthMPa ?: -1.0).compareTo(b.yieldStrengthMPa ?: -1.0)
                MaterialSortColumn.ULTIMATE -> a.ultimateStrengthMPa.compareTo(b.ultimateStrengthMPa)
            }
        }
        if (ascending) filtered.sortedWith(cmp) else filtered.sortedWith(cmp.reversed())
    }

    val horizScroll = rememberScrollState()

    Column(modifier = modifier.fillMaxSize()) {
        CompactFilterField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = "Filter materials (Steel, Titanium, Carbon…)",
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
        )
        Text(
            text = "Tap any column header to sort · Scroll horizontally for all properties",
            style = MaterialTheme.typography.labelMedium,
            color = colorScheme.outline,
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            item(key = "mat_header") {
                Row(
                    modifier = Modifier
                        .horizontalScroll(horizScroll)
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val colWidths = listOf(168.dp, 96.dp, 92.dp, 100.dp, 110.dp)
                    MaterialSortColumn.entries.forEachIndexed { idx, col ->
                        val active = sortCol == col
                        Row(
                            modifier = Modifier
                                .width(colWidths[idx])
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    if (sortCol == col) ascending = !ascending
                                    else {
                                        sortCol = col
                                        ascending = col == MaterialSortColumn.NAME
                                    }
                                }
                                .padding(horizontal = 6.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = if (idx == 0) Arrangement.Start else Arrangement.End
                        ) {
                            Text(
                                text = col.label,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) colorScheme.primary else colorScheme.onSurfaceVariant
                            )
                            if (active) {
                                Spacer(modifier = Modifier.width(3.dp))
                                Icon(
                                    imageVector = if (ascending) Icons.Default.ArrowUpward else Icons.Default.ArrowDownward,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(13.dp)
                                )
                            }
                        }
                    }
                }
                HorizontalDivider(
                    thickness = 1.dp,
                    color = colorScheme.primary.copy(alpha = 0.35f),
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
            }

            itemsIndexed(sorted, key = { _, m -> m.name }) { idx, m ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            val summary = "${m.name}: ρ=${m.densityLabel} kg/m³, E=${m.modulusLabel} GPa, Yield=${m.yieldLabel} MPa, UTS=${m.ultimateLabel} MPa"
                            TexExportHelper.copyPlainText(context, m.name, summary, "Copied ${m.name}")
                        }
                ) {
                    Row(
                        modifier = Modifier
                            .horizontalScroll(horizScroll)
                            .padding(horizontal = 16.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.width(168.dp).padding(horizontal = 6.dp)) {
                            Text(
                                text = m.name,
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.onSurface
                            )
                            if (m.note.isNotBlank()) {
                                Text(
                                    text = m.note,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        MaterialCellText(m.densityLabel, 96.dp, colorScheme.onSurface)
                        MaterialCellText(m.modulusLabel, 92.dp, colorScheme.primary)
                        MaterialCellText(m.yieldLabel, 100.dp, colorScheme.onSurface)
                        MaterialCellText(m.ultimateLabel, 110.dp, colorScheme.onSurface)
                    }
                    if (idx < sorted.lastIndex) {
                        HorizontalDivider(
                            thickness = 0.5.dp,
                            color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MaterialCellText(
    text: String,
    width: androidx.compose.ui.unit.Dp,
    color: androidx.compose.ui.graphics.Color
) {
    Text(
        text = text,
        fontFamily = FontFamily.Monospace,
        fontSize = 12.5.sp,
        color = color,
        textAlign = TextAlign.End,
        modifier = Modifier
            .width(width)
            .padding(horizontal = 6.dp)
    )
}

// =========================================================================
// Biology: Human Physiological Baselines
// =========================================================================

@Composable
fun HumanPhysiologyTables(modifier: Modifier = Modifier) {
    val colorScheme = MaterialTheme.colorScheme
    val grouped = remember { GeometryFinanceCsBioData.HUMAN_PHYSIOLOGY_BASELINES.groupBy { it.category } }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp)
    ) {
        item(key = "phys_note") {
            Text(
                text = "Adult resting reference ranges for quick educational reference (values vary slightly by lab & assay).",
                style = MaterialTheme.typography.bodySmall,
                color = colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            )
        }

        grouped.forEach { (category, items) ->
            item(key = "phys_cat_$category") {
                Text(
                    text = category,
                    fontFamily = LauncherFonts.serif,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
                )
            }
            itemsIndexed(items, key = { _, b -> "${category}_${b.metric}" }) { idx, b ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = b.metric,
                        style = MaterialTheme.typography.bodyMedium,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f).padding(end = 12.dp)
                    )
                    Text(
                        text = if (b.unit.isBlank()) b.normalRange else "${b.normalRange} ${b.unit}",
                        fontFamily = FontFamily.Monospace,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.primary,
                        textAlign = TextAlign.End
                    )
                }
                if (idx < items.lastIndex) {
                    HorizontalDivider(
                        thickness = 0.5.dp,
                        color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                        modifier = Modifier.padding(horizontal = 20.dp)
                    )
                }
            }
        }
    }
}

// =========================================================================
// Supported Currencies Table (Guide -> Commands Tab)
// =========================================================================

@Composable
fun SupportedCurrenciesSection(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val ratesVersion by CurrencyRateService.ratesVersion.collectAsState()

    var baseCurrency by remember { mutableStateOf("USD") }
    var filter by remember { mutableStateOf("") }

    val currencies = remember(ratesVersion) { UnitConverterEngine.allSupportedCurrencies() }
    val updatedMs = remember(ratesVersion) { CurrencyRateService.lastUpdatedMs }

    val filtered = remember(currencies, filter) {
        val q = filter.trim().lowercase(Locale.ROOT)
        if (q.isEmpty()) currencies
        else currencies.filter {
            it.code.lowercase(Locale.ROOT).contains(q) || it.name.lowercase(Locale.ROOT).contains(q)
        }
    }

    val updatedText = remember(updatedMs, ratesVersion) {
        if (updatedMs <= 0L) {
            "Offline baseline rates (connect to refresh)"
        } else {
            val stamp = SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()).format(Date(updatedMs))
            "Updated $stamp · ${UnitConverterEngine.formatRateFreshness(updatedMs)}"
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 12.dp)
    ) {
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.4f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(modifier = Modifier.height(14.dp))

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Supported Currencies (${currencies.size})",
                    fontFamily = LauncherFonts.serif,
                    fontSize = 22.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.primary
                )
                Text(
                    text = updatedText,
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.onSurfaceVariant
                )
            }
            IconButton(
                onClick = { CurrencyRateService.refreshInBackground() },
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Refresh,
                    contentDescription = "Refresh FX rates",
                    tint = colorScheme.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Base currency selector chips (most used currencies + INR)
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            items(listOf("USD", "EUR", "GBP", "JPY", "INR"), key = { it }) { code ->
                val selected = baseCurrency == code
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
                    border = BorderStroke(
                        1.dp,
                        if (selected) colorScheme.primary.copy(alpha = 0.65f) else colorScheme.outlineVariant.copy(alpha = 0.4f)
                    ),
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { baseCurrency = code }
                ) {
                    Text(
                        text = "Base: $code",
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                    )
                }
            }
        }

        CompactFilterField(
            value = filter,
            onValueChange = { filter = it },
            placeholder = "Filter code or currency name…",
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
        )

        // Table Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Code",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary,
                modifier = Modifier.width(56.dp)
            )
            Text(
                text = "Currency",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary,
                modifier = Modifier.weight(1f)
            )
            Text(
                text = "1 $baseCurrency =",
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.primary,
                textAlign = TextAlign.End
            )
        }
        HorizontalDivider(
            thickness = 1.dp,
            color = colorScheme.primary.copy(alpha = 0.35f),
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        filtered.forEachIndexed { idx, meta ->
            val rate = remember(meta.code, baseCurrency, ratesVersion) {
                CurrencyRateService.convert(1.0, baseCurrency, meta.code)
            }
            val rateStr = rate?.let { UnitConverterEngine.formatRate(it) } ?: "—"
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        val txt = "1 $baseCurrency = $rateStr ${meta.code}"
                        TexExportHelper.copyPlainText(context, meta.code, txt, "Copied $txt")
                    }
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = meta.code,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    color = colorScheme.primary,
                    modifier = Modifier.width(56.dp)
                )
                Text(
                    text = meta.name,
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f).padding(end = 8.dp)
                )
                Text(
                    text = rateStr,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 13.sp,
                    color = colorScheme.onSurface,
                    textAlign = TextAlign.End
                )
            }
            if (idx < filtered.lastIndex) {
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
    }
}

@Composable
private fun CompactFilterField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(
            width = 1.dp,
            color = if (value.isNotEmpty()) colorScheme.primary.copy(alpha = 0.55f)
            else colorScheme.outlineVariant.copy(alpha = 0.4f)
        ),
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                tint = colorScheme.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                if (value.isEmpty()) {
                    Text(
                        text = placeholder,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                BasicTextField(
                    value = value,
                    onValueChange = onValueChange,
                    singleLine = true,
                    textStyle = MaterialTheme.typography.bodyMedium.copy(
                        color = colorScheme.onSurface,
                        fontSize = 14.sp
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth()
                )
            }
            if (value.isNotEmpty()) {
                IconButton(
                    onClick = { onValueChange("") },
                    modifier = Modifier.size(22.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Clear,
                        contentDescription = "Clear",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
    }
}
