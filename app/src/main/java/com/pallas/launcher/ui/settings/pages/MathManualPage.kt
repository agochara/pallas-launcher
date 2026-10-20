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
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.automirrored.filled.ShowChart
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Functions
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.search.ManualEntry
import com.pallas.launcher.domain.search.MathStatsEngine
import com.pallas.launcher.ui.settings.components.SettingsLargeTopBar

data class CategoryMeta(
    val name: String,
    val description: String,
    val icon: ImageVector
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MathManualPage(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    var searchQuery by remember { mutableStateOf("") }
    var activeCategorySubpage by remember { mutableStateOf<String?>(null) }

    // System back gesture navigation: subpage -> manual index -> onBack()
    BackHandler(enabled = activeCategorySubpage != null) {
        activeCategorySubpage = null
    }

    val copyToClipboard: (String) -> Unit = { text ->
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText("Pallas Launcher Command", text))
        Toast.makeText(context, "Copied: $text", Toast.LENGTH_SHORT).show()
    }

    val categories = remember {
        listOf(
            CategoryMeta(
                name = "Arithmetic & Elementary Math",
                description = "Operators, powers, roots, trigonometry, primes, Fibonacci, and combinatorics.",
                icon = Icons.Default.Functions
            ),
            CategoryMeta(
                name = "Lists & Vectors",
                description = "Sequence generators, vector math, sorting, slicing, and prefix sums.",
                icon = Icons.Default.Tag
            ),
            CategoryMeta(
                name = "Statistics",
                description = "Sample mean, median, variance, standard deviation, quantiles, and correlation.",
                icon = Icons.AutoMirrored.Filled.ShowChart
            ),
            CategoryMeta(
                name = "Distributions",
                description = "Normal, Uniform, Exponential, Poisson, Binomial, Student-t, Cauchy, Gamma, Beta.",
                icon = Icons.Default.Functions
            ),
            CategoryMeta(
                name = "Heavy Tails & Power Laws",
                description = "Pareto power-law, Lévy stable, Fréchet, Weibull, and Log-Normal tails.",
                icon = Icons.AutoMirrored.Filled.ShowChart
            ),
            CategoryMeta(
                name = "Probability Ops",
                description = "PDF, CDF, SurvivalFunction, HazardFunction, moments, and inverse CDF quantiles.",
                icon = Icons.Default.TouchApp
            ),
            CategoryMeta(
                name = "Random Generation",
                description = "RandomInteger, RandomReal, RandomChoice, RandomSample, and distribution sampling.",
                icon = Icons.Default.Timer
            ),
            CategoryMeta(
                name = "Launcher Commands",
                description = "Command palette shortcuts: weather, solar times, stock quotes, alarms, timers, calls.",
                icon = Icons.Default.Settings
            )
        )
    }

    val allEntries = MathStatsEngine.MANUAL_ENTRIES

    val searchResults = remember(searchQuery) {
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

    Scaffold(
        modifier = modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            SettingsLargeTopBar(
                title = activeCategorySubpage ?: "Computational Engine",
                scrollBehavior = scrollBehavior,
                onBack = {
                    if (activeCategorySubpage != null) {
                        activeCategorySubpage = null
                    } else {
                        onBack()
                    }
                }
            )
        },
        containerColor = colorScheme.surface
    ) { innerPadding ->
        AnimatedContent(
            targetState = activeCategorySubpage,
            transitionSpec = {
                if (targetState == null) {
                    (slideInHorizontally { -it / 2 } + fadeIn()) togetherWith (slideOutHorizontally { it } + fadeOut())
                } else {
                    (slideInHorizontally { it } + fadeIn()) togetherWith (slideOutHorizontally { -it / 2 } + fadeOut())
                }
            },
            label = "manual_subpage_nav",
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) { subpage ->
            if (subpage != null) {
                // Subpage view for specific category
                val categoryEntries = remember(subpage) {
                    allEntries.filter { it.category.equals(subpage, ignoreCase = true) }
                }
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    item {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colorScheme.surfaceContainerHigh,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(16.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                Text(
                                    text = subpage,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colorScheme.primary
                                )
                                val meta = categories.firstOrNull { it.name.equals(subpage, ignoreCase = true) }
                                Text(
                                    text = meta?.description ?: "Functions and documentation for $subpage.",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant
                                )
                                Text(
                                    text = "${categoryEntries.size} functions available • Tap any example to copy",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colorScheme.outline
                                )
                            }
                        }
                    }

                    items(categoryEntries, key = { it.signature }) { entry ->
                        ManualEntryCard(entry = entry, onCopyExample = { copyToClipboard(entry.example) })
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            } else {
                // Main Index view with Global Search
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Global Search Bar
                    item {
                        OutlinedTextField(
                            value = searchQuery,
                            onValueChange = { searchQuery = it },
                            placeholder = { Text("Search all functions, distributions, or commands…") },
                            leadingIcon = {
                                Icon(
                                    imageVector = Icons.Default.Search,
                                    contentDescription = null,
                                    tint = colorScheme.onSurfaceVariant
                                )
                            },
                            trailingIcon = {
                                if (searchQuery.isNotEmpty()) {
                                    IconButton(onClick = { searchQuery = "" }) {
                                        Icon(
                                            imageVector = Icons.Default.Clear,
                                            contentDescription = "Clear search",
                                            tint = colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            },
                            singleLine = true,
                            shape = RoundedCornerShape(16.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = colorScheme.surfaceContainerLow,
                                unfocusedContainerColor = colorScheme.surfaceContainerLow,
                                focusedBorderColor = colorScheme.primary,
                                unfocusedBorderColor = colorScheme.outlineVariant
                            ),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    if (searchQuery.isNotBlank()) {
                        // Display search results
                        item {
                            Text(
                                text = "Found ${searchResults.size} results for \"${searchQuery.trim()}\"",
                                style = MaterialTheme.typography.labelLarge,
                                color = colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                            )
                        }

                        if (searchResults.isEmpty()) {
                            item {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(vertical = 40.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    Text(
                                        text = "No matching functions found",
                                        style = MaterialTheme.typography.titleMedium,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = "Try searching for a function name like Mean, NormalDistribution, or w.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.outline
                                    )
                                }
                            }
                        } else {
                            items(searchResults, key = { it.signature }) { entry ->
                                ManualEntryCard(entry = entry, onCopyExample = { copyToClipboard(entry.example) })
                            }
                        }
                    } else {
                        // Guide Overview Banner
                        item {
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = colorScheme.surfaceContainerHigh,
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(16.dp),
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Functions,
                                            contentDescription = null,
                                            tint = colorScheme.primary,
                                            modifier = Modifier.size(22.dp)
                                        )
                                        Text(
                                            text = "Wolfram-Inspired Computational Engine",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colorScheme.onSurface
                                        )
                                    }
                                    Text(
                                        text = "Type '=' in search palette followed by any expression. Functions use square brackets Func[…], lists use braces {…}. Expressions compose naturally (e.g. Mean[RandomVariate[NormalDistribution[0, 1], 100]]). Browse categories below or search above.",
                                        style = MaterialTheme.typography.bodyMedium,
                                        color = colorScheme.onSurfaceVariant,
                                        lineHeight = 20.sp
                                    )
                                }
                            }
                        }

                        // Categories Subpage Section Header
                        item {
                            Text(
                                text = "Documentation Categories",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.SemiBold,
                                color = colorScheme.primary,
                                modifier = Modifier.padding(start = 4.dp, top = 6.dp)
                            )
                        }

                        // Category Subpage Navigation Cards
                        items(categories, key = { it.name }) { cat ->
                            val count = remember(cat.name) {
                                allEntries.count { it.category.equals(cat.name, ignoreCase = true) }
                            }
                            CategoryNavCard(
                                category = cat,
                                entryCount = count,
                                onClick = { activeCategorySubpage = cat.name }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }
}

@Composable
private fun CategoryNavCard(
    category: CategoryMeta,
    entryCount: Int,
    onClick: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Surface(
                shape = CircleShape,
                color = colorScheme.primaryContainer,
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    imageVector = category.icon,
                    contentDescription = null,
                    tint = colorScheme.onPrimaryContainer,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            }

            Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(
                    text = category.name,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.onSurface
                )
                Text(
                    text = category.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = colorScheme.onSurfaceVariant,
                    lineHeight = 16.sp
                )
                Text(
                    text = "$entryCount items",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    fontWeight = FontWeight.Medium
                )
            }

            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = "Open ${category.name}",
                tint = colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun ManualEntryCard(
    entry: ManualEntry,
    onCopyExample: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colorScheme.surfaceContainerLow,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // Header: Signature + Category Badge
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

                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colorScheme.surfaceContainerHighest,
                    modifier = Modifier.padding(start = 8.dp)
                ) {
                    Text(
                        text = entry.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Description
            Text(
                text = entry.description,
                style = MaterialTheme.typography.bodyMedium,
                color = colorScheme.onSurfaceVariant,
                lineHeight = 19.sp
            )

            // Example chip/pill that can be clicked to copy
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colorScheme.surfaceContainerHigh,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onCopyExample() }
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 12.dp, vertical = 9.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = entry.example,
                        fontFamily = FontFamily.Monospace,
                        fontSize = 12.sp,
                        color = colorScheme.onSurface,
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.ContentCopy,
                            contentDescription = "Copy Example",
                            tint = colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                        Text(
                            text = "Copy",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = colorScheme.primary
                        )
                    }
                }
            }
        }
    }
}
