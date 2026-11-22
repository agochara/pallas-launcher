package com.pallas.launcher.ui.settings.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.math.CheatsheetFormula
import com.pallas.launcher.domain.math.CheatsheetSubsection
import com.pallas.launcher.domain.math.CheatsheetTopic
import com.pallas.launcher.domain.math.MathCheatsheets
import com.pallas.launcher.domain.search.ManualEntry
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.components.LatexFormula
import com.pallas.launcher.ui.components.TexPreviewDialog
import com.pallas.launcher.ui.settings.SettingsViewModel
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar
import com.pallas.launcher.ui.theme.LauncherFonts
import kotlinx.coroutines.launch

private enum class GuideTab(val label: String) {
    ENGINE("Engine"),
    CHEATSHEETS("Cheatsheets"),
    COMMANDS("Commands")
}

private data class EngineCategoryMeta(
    val name: String,
    val description: String,
    val symbol: String
)

private sealed interface ManualDestination {
    data object Index : ManualDestination
    data class Category(val name: String) : ManualDestination
    data class Cheatsheet(val topicId: String) : ManualDestination
}

private data class CheatsheetSearchHit(
    val topic: CheatsheetTopic,
    val subsection: CheatsheetSubsection,
    val formula: CheatsheetFormula
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathManualPage(
    viewModel: SettingsViewModel,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    val settings by viewModel.settings.collectAsState()
    val allEntries = remember(settings.commandConfigs) {
        MathStatsEngine.manualEntriesFor(settings.commandConfigs)
    }
    val mathPrefix = settings.commandConfigs["math"]?.prefix?.ifBlank { "=" } ?: "="
    val texPrefix = settings.commandConfigs["tex"]?.prefix?.ifBlank { "tex" } ?: "tex"

    var selectedTab by remember { mutableStateOf(GuideTab.ENGINE) }
    var searchQuery by remember { mutableStateOf("") }
    var destination by remember { mutableStateOf<ManualDestination>(ManualDestination.Index) }
    var previewLatex by remember { mutableStateOf<String?>(null) }

    // System back gesture navigation: subpage -> manual index -> onBack()
    BackHandler(enabled = destination != ManualDestination.Index) {
        destination = ManualDestination.Index
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Pallas Launcher Command", text))
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    val engineCategories = remember {
        listOf(
            EngineCategoryMeta(
                name = "Arithmetic & Number Theory",
                description = "Arithmetic, powers, roots, trigonometry, special functions, modular math, primes, and combinatorics.",
                symbol = "±"
            ),
            EngineCategoryMeta(
                name = "Calculus & Solvers",
                description = "Numerical integration, differentiation, root finding, optimization, sums, and products.",
                symbol = "∫"
            ),
            EngineCategoryMeta(
                name = "Lists & Linear Algebra",
                description = "Sequences, tables, Span slicing, list transforms, set ops, and vector/matrix algebra.",
                symbol = "{ }"
            ),
            EngineCategoryMeta(
                name = "Statistics",
                description = "Sample moments, quantiles, correlation, standardization, moving averages, and tail risk metrics.",
                symbol = "μ"
            ),
            EngineCategoryMeta(
                name = "Distributions",
                description = "Continuous, discrete, heavy-tailed, and extreme-value probability distributions.",
                symbol = "𝒩"
            ),
            EngineCategoryMeta(
                name = "Probability & Random",
                description = "PDF, CDF, survival/hazard functions, inverse CDF, moments, and pseudo-random sampling.",
                symbol = "ℙ"
            ),
            EngineCategoryMeta(
                name = "Operators & Control Flow",
                description = "Prefix/postfix, Map/Apply, composition, rules, scoping, loops, Print/Echo, and strings.",
                symbol = "/@"
            ),
            EngineCategoryMeta(
                name = "Plotting & Charts",
                description = "2D function, log, parametric, polar, and dataset plots plus histograms, bar, pie & box charts.",
                symbol = "ƒ"
            )
        )
    }

    val commandEntries = remember(allEntries) {
        allEntries.filter { it.category.equals("Launcher Commands", ignoreCase = true) }
    }

    val entrySearchResults = remember(searchQuery, allEntries) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else {
            allEntries.filter { entry ->
                entry.signature.lowercase().contains(q) ||
                    entry.description.lowercase().contains(q) ||
                    entry.example.lowercase().contains(q) ||
                    entry.category.lowercase().contains(q)
            }
        }
    }

    val cheatsheetSearchResults = remember(searchQuery) {
        val q = searchQuery.trim().lowercase()
        if (q.isEmpty()) emptyList()
        else {
            buildList {
                MathCheatsheets.TOPICS.forEach { topic ->
                    topic.subsections.forEach { sub ->
                        sub.formulas.forEach { formula ->
                            val matches = topic.title.lowercase().contains(q) ||
                                topic.shortTitle.lowercase().contains(q) ||
                                sub.title.lowercase().contains(q) ||
                                formula.title.lowercase().contains(q) ||
                                (formula.note?.lowercase()?.contains(q) == true) ||
                                formula.latex.lowercase().contains(q)
                            if (matches) {
                                add(CheatsheetSearchHit(topic, sub, formula))
                            }
                        }
                    }
                }
            }
        }
    }

    val topBarTitle = when (val d = destination) {
        ManualDestination.Index -> "Math & Engine"
        is ManualDestination.Category -> d.name
        is ManualDestination.Cheatsheet -> MathCheatsheets.topicById(d.topicId)?.shortTitle ?: "Cheatsheet"
    }

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsLargeTopBar(
                title = topBarTitle,
                scrollBehavior = scrollBehavior,
                onBack = {
                    if (destination != ManualDestination.Index) {
                        destination = ManualDestination.Index
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        AnimatedContent(
            targetState = destination,
            transitionSpec = {
                if (targetState == ManualDestination.Index) {
                    (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                }
            },
            label = "manual_subpage_nav",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { target ->
            when (target) {
                is ManualDestination.Category -> {
                    val categoryEntries = remember(target.name, allEntries) {
                        allEntries.filter { it.category.equals(target.name, ignoreCase = true) }
                    }
                    val meta = engineCategories.firstOrNull { it.name.equals(target.name, ignoreCase = true) }
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
                    ) {
                        item(key = "cat_lead_${target.name}") {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = meta?.description ?: "Functions and syntax for ${target.name}.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${categoryEntries.size} items · Tap any example to copy",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.outline
                                )
                            }
                        }

                        itemsIndexed(
                            items = categoryEntries,
                            key = { _, entry -> "cat_${entry.category}_${entry.signature}" }
                        ) { index, entry ->
                            ManualEntryRow(
                                entry = entry,
                                showCategory = false,
                                showDivider = index < categoryEntries.lastIndex,
                                onCopyExample = { copyToClipboard(entry.example) }
                            )
                        }
                    }
                }

                is ManualDestination.Cheatsheet -> {
                    val topic = MathCheatsheets.topicById(target.topicId)
                    val listState = rememberLazyListState()
                    val scope = rememberCoroutineScope()
                    val hasJumpStrip = topic != null && topic.subsections.size > 2
                    val subsectionHeaderIndices = remember(topic?.id, hasJumpStrip) {
                        if (topic == null) emptyList()
                        else buildList {
                            var idx = if (hasJumpStrip) 2 else 1
                            topic.subsections.forEach { sub ->
                                add(idx)
                                idx += 1 + sub.formulas.size
                            }
                        }
                    }

                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
                    ) {
                        if (topic != null) {
                            item(key = "cs_lead_${topic.id}") {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 20.dp, vertical = 10.dp),
                                    verticalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Text(
                                        text = topic.description,
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = if (topic.showLatexSource) {
                                            "${topic.formulaCount} snippets · Tap code to copy · Tap render to export PNG"
                                        } else {
                                            "${topic.formulaCount} formulas · Tap any equation to export PNG"
                                        },
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.outline
                                    )
                                }
                            }

                            if (hasJumpStrip) {
                                item(key = "cs_jump_${topic.id}") {
                                    Column(modifier = Modifier.fillMaxWidth()) {
                                        LazyRow(
                                            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 10.dp),
                                            horizontalArrangement = Arrangement.spacedBy(18.dp)
                                        ) {
                                            itemsIndexed(
                                                items = topic.subsections,
                                                key = { _, sub -> "jump_${topic.id}_${sub.title}" }
                                            ) { idx, sub ->
                                                Text(
                                                    text = sub.shortTitle,
                                                    style = MaterialTheme.typography.labelLarge,
                                                    fontWeight = FontWeight.Medium,
                                                    color = colorScheme.primary,
                                                    modifier = Modifier
                                                        .clickable {
                                                            subsectionHeaderIndices.getOrNull(idx)?.let { targetIdx ->
                                                                scope.launch { listState.animateScrollToItem(targetIdx) }
                                                            }
                                                        }
                                                        .padding(vertical = 2.dp)
                                                )
                                            }
                                        }
                                        HorizontalDivider(
                                            thickness = 0.5.dp,
                                            color = colorScheme.outlineVariant.copy(alpha = 0.35f),
                                            modifier = Modifier.padding(horizontal = 20.dp)
                                        )
                                    }
                                }
                            }

                            topic.subsections.forEach { subsection ->
                                item(key = "cs_sub_${topic.id}_${subsection.title}") {
                                    Text(
                                        text = subsection.title,
                                        fontFamily = LauncherFonts.serif,
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.primary,
                                        modifier = Modifier.padding(
                                            start = 20.dp,
                                            end = 20.dp,
                                            top = 22.dp,
                                            bottom = 4.dp
                                        )
                                    )
                                }

                                itemsIndexed(
                                    items = subsection.formulas,
                                    key = { _, formula -> "cs_${topic.id}_${subsection.title}_${formula.title}" }
                                ) { index, formula ->
                                    CheatsheetFormulaRow(
                                        formula = formula,
                                        showDivider = index < subsection.formulas.lastIndex,
                                        showSource = topic.showLatexSource,
                                        onPreview = { previewLatex = formula.latex },
                                        onCopyLatex = { TexExportHelper.copyLatexText(context, formula.latex) }
                                    )
                                }
                            }
                        }
                    }
                }

                ManualDestination.Index -> {
                    Column(modifier = Modifier.fillMaxSize()) {
                        // Compact command-bar style search input
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                width = 1.dp,
                                color = if (searchQuery.isNotEmpty()) colorScheme.primary.copy(alpha = 0.55f)
                                else colorScheme.outlineVariant.copy(alpha = 0.45f)
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp)
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 11.dp),
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
                                    if (searchQuery.isEmpty()) {
                                        Text(
                                            text = "Search formulas, functions, commands…",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = colorScheme.onSurfaceVariant.copy(alpha = 0.65f),
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                    BasicTextField(
                                        value = searchQuery,
                                        onValueChange = { searchQuery = it },
                                        singleLine = true,
                                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                                            color = colorScheme.onSurface,
                                            fontSize = 15.sp
                                        ),
                                        cursorBrush = SolidColor(colorScheme.primary),
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(
                                        onClick = { searchQuery = "" },
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

                        if (searchQuery.isBlank()) {
                            // 3-tab underline switcher
                            GuideTabStrip(
                                selectedTab = selectedTab,
                                onSelectTab = { selectedTab = it }
                            )
                        }

                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f),
                            contentPadding = PaddingValues(top = 4.dp, bottom = 36.dp)
                        ) {
                            if (searchQuery.isNotBlank()) {
                                val totalHits = cheatsheetSearchResults.size + entrySearchResults.size
                                item(key = "search_summary") {
                                    Text(
                                        text = "$totalHits result${if (totalHits == 1) "" else "s"} for \"${searchQuery.trim()}\"",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
                                    )
                                }

                                if (totalHits == 0) {
                                    item(key = "search_empty") {
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 24.dp, vertical = 48.dp),
                                            horizontalAlignment = Alignment.CenterHorizontally,
                                            verticalArrangement = Arrangement.spacedBy(4.dp)
                                        ) {
                                            Text(
                                                text = "No matching formulas or functions",
                                                style = MaterialTheme.typography.titleMedium,
                                                color = colorScheme.onSurfaceVariant
                                            )
                                            Text(
                                                text = "Try Stokes, Leibniz, Mean, Pareto, or $texPrefix.",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.outline
                                            )
                                        }
                                    }
                                } else {
                                    if (cheatsheetSearchResults.isNotEmpty()) {
                                        item(key = "search_cs_header") {
                                            Text(
                                                text = "Cheatsheets (${cheatsheetSearchResults.size})",
                                                fontFamily = LauncherFonts.serif,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 20.dp,
                                                    end = 20.dp,
                                                    top = 14.dp,
                                                    bottom = 4.dp
                                                )
                                            )
                                        }
                                        itemsIndexed(
                                            items = cheatsheetSearchResults,
                                            key = { _, hit -> "search_cs_${hit.topic.id}_${hit.subsection.title}_${hit.formula.title}" }
                                        ) { index, hit ->
                                            CheatsheetFormulaRow(
                                                formula = hit.formula,
                                                contextLabel = hit.topic.shortTitle,
                                                showDivider = index < cheatsheetSearchResults.lastIndex,
                                                showSource = hit.topic.showLatexSource,
                                                onPreview = { previewLatex = hit.formula.latex },
                                                onCopyLatex = { TexExportHelper.copyLatexText(context, hit.formula.latex) }
                                            )
                                        }
                                    }

                                    if (entrySearchResults.isNotEmpty()) {
                                        item(key = "search_entry_header") {
                                            Text(
                                                text = "Engine & Commands (${entrySearchResults.size})",
                                                fontFamily = LauncherFonts.serif,
                                                fontSize = 21.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = colorScheme.primary,
                                                modifier = Modifier.padding(
                                                    start = 20.dp,
                                                    end = 20.dp,
                                                    top = 18.dp,
                                                    bottom = 4.dp
                                                )
                                            )
                                        }
                                        itemsIndexed(
                                            items = entrySearchResults,
                                            key = { _, entry -> "search_entry_${entry.category}_${entry.signature}" }
                                        ) { index, entry ->
                                            ManualEntryRow(
                                                entry = entry,
                                                showCategory = true,
                                                showDivider = index < entrySearchResults.lastIndex,
                                                onCopyExample = { copyToClipboard(entry.example) }
                                            )
                                        }
                                    }
                                }
                            } else {
                                when (selectedTab) {
                                    GuideTab.CHEATSHEETS -> {
                                        item(key = "tab_cs_lead") {
                                            Text(
                                                text = "Math & syntax reference sheets rendered in LaTeX",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                            )
                                        }
                                        itemsIndexed(
                                            items = MathCheatsheets.TOPICS,
                                            key = { _, topic -> "topic_${topic.id}" }
                                        ) { index, topic ->
                                            EditorialNavRow(
                                                symbol = topic.symbol,
                                                title = topic.title,
                                                description = topic.description,
                                                count = topic.formulaCount,
                                                showDivider = index < MathCheatsheets.TOPICS.lastIndex,
                                                onClick = { destination = ManualDestination.Cheatsheet(topic.id) }
                                            )
                                        }
                                    }

                                    GuideTab.ENGINE -> {
                                        item(key = "tab_engine_syntax") {
                                            val syntaxStrip = remember(mathPrefix, texPrefix, colorScheme.primary) {
                                                buildAnnotatedString {
                                                    append("Prefix ")
                                                    withStyle(
                                                        SpanStyle(
                                                            color = colorScheme.primary,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    ) {
                                                        append(mathPrefix)
                                                    }
                                                    append("  ·  Func[…]  ·  {list}  ·  LaTeX: ")
                                                    withStyle(
                                                        SpanStyle(
                                                            color = colorScheme.primary,
                                                            fontFamily = FontFamily.Monospace,
                                                            fontWeight = FontWeight.SemiBold
                                                        )
                                                    ) {
                                                        append(texPrefix)
                                                    }
                                                }
                                            }
                                            Text(
                                                text = syntaxStrip,
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                            )
                                        }
                                        itemsIndexed(
                                            items = engineCategories,
                                            key = { _, cat -> "cat_${cat.name}" }
                                        ) { index, cat ->
                                            val count = remember(cat.name, allEntries) {
                                                allEntries.count { it.category.equals(cat.name, ignoreCase = true) }
                                            }
                                            EditorialNavRow(
                                                symbol = cat.symbol,
                                                title = cat.name,
                                                description = cat.description,
                                                count = count,
                                                showDivider = index < engineCategories.lastIndex,
                                                onClick = { destination = ManualDestination.Category(cat.name) }
                                            )
                                        }
                                    }

                                    GuideTab.COMMANDS -> {
                                        item(key = "tab_cmd_lead") {
                                            Text(
                                                text = "Live prefixes from Search Settings · Tap any example to copy",
                                                style = MaterialTheme.typography.bodySmall,
                                                color = colorScheme.onSurfaceVariant,
                                                modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp)
                                            )
                                        }
                                        itemsIndexed(
                                            items = commandEntries,
                                            key = { _, entry -> "cmd_${entry.signature}" }
                                        ) { index, entry ->
                                            ManualEntryRow(
                                                entry = entry,
                                                showCategory = false,
                                                showDivider = index < commandEntries.lastIndex,
                                                onCopyExample = { copyToClipboard(entry.example) }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    previewLatex?.let { latex ->
        TexPreviewDialog(
            latex = latex,
            onDismiss = { previewLatex = null }
        )
    }
}

@Composable
private fun GuideTabStrip(
    selectedTab: GuideTab,
    onSelectTab: (GuideTab) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
        ) {
            GuideTab.entries.forEach { tab ->
                val selected = tab == selectedTab
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onSelectTab(tab) },
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = tab.label,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
                        color = if (selected) colorScheme.primary else colorScheme.onSurfaceVariant,
                        maxLines = 1,
                        modifier = Modifier.padding(vertical = 11.dp)
                    )
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(if (selected) colorScheme.primary else colorScheme.surface)
                    )
                }
            }
        }
        HorizontalDivider(
            thickness = 0.5.dp,
            color = colorScheme.outlineVariant.copy(alpha = 0.35f)
        )
    }
}

@Composable
private fun EditorialNavRow(
    symbol: String,
    title: String,
    description: String,
    count: Int,
    showDivider: Boolean,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = symbol,
                fontFamily = LauncherFonts.serif,
                fontSize = when {
                    symbol.length >= 4 -> 13.5.sp
                    symbol.length > 1 -> 20.sp
                    else -> 28.sp
                },
                maxLines = 1,
                fontWeight = FontWeight.Medium,
                color = colorScheme.primary,
                textAlign = TextAlign.Center,
                modifier = Modifier.width(42.dp)
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 16.sp
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = "$count",
                    style = MaterialTheme.typography.labelMedium,
                    color = colorScheme.outline
                )
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = "Open $title",
                    tint = colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.size(18.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.3f),
                modifier = Modifier.padding(start = 74.dp, end = 20.dp)
            )
        }
    }
}

@Composable
private fun CheatsheetFormulaRow(
    formula: CheatsheetFormula,
    showDivider: Boolean,
    onPreview: () -> Unit,
    onCopyLatex: () -> Unit,
    contextLabel: String? = null,
    showSource: Boolean = false
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp, bottom = 14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = formula.title,
                        style = MaterialTheme.typography.labelLarge,
                        fontWeight = FontWeight.Medium,
                        color = colorScheme.onSurfaceVariant
                    )
                    if (!contextLabel.isNullOrBlank()) {
                        Text(
                            text = contextLabel,
                            style = MaterialTheme.typography.labelSmall,
                            color = colorScheme.primary
                        )
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (!showSource) {
                        IconButton(
                            onClick = onCopyLatex,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy LaTeX",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                    IconButton(
                        onClick = onPreview,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Image,
                            contentDescription = "Preview & export PNG",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onPreview)
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                LatexFormula(
                    latex = formula.latex,
                    color = colorScheme.onSurface,
                    fontSize = 19.sp
                )
            }

            if (showSource) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorScheme.surfaceContainerLow,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .clickable(onClick = onCopyLatex)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = formula.latex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.5.sp,
                            lineHeight = 18.sp,
                            color = colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy LaTeX",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }

            formula.note?.takeIf { it.isNotBlank() }?.let { note ->
                Text(
                    text = note,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.85f),
                    lineHeight = 17.sp,
                    modifier = Modifier.padding(horizontal = 20.dp)
                )
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.28f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}

@Composable
private fun ManualEntryRow(
    entry: ManualEntry,
    showCategory: Boolean,
    showDivider: Boolean,
    onCopyExample: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Column(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 13.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = entry.signature,
                    fontFamily = FontFamily.Monospace,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f, fill = false)
                )

                if (!entry.isEnabled || showCategory) {
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(start = 8.dp)
                    ) {
                        if (!entry.isEnabled) {
                            Text(
                                text = "Disabled",
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Medium,
                                color = colorScheme.error
                            )
                        }
                        if (showCategory) {
                            Text(
                                text = entry.category,
                                style = MaterialTheme.typography.labelSmall,
                                color = colorScheme.outline
                            )
                        }
                    }
                }
            }

            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            Surface(
                shape = RoundedCornerShape(6.dp),
                color = colorScheme.surfaceContainerLow,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onCopyExample)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.example,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.5.sp,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy example",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }
        }
        if (showDivider) {
            HorizontalDivider(
                thickness = 0.5.dp,
                color = colorScheme.outlineVariant.copy(alpha = 0.28f),
                modifier = Modifier.padding(horizontal = 20.dp)
            )
        }
    }
}
