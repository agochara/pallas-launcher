package com.pallas.launcher.ui.latex

import android.text.format.DateUtils
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.ImageFormat
import com.hrm.latex.renderer.export.rememberLatexExporter
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.pallas.launcher.data.repository.LatexDocument
import com.pallas.launcher.data.repository.LatexDocumentRepository
import com.pallas.launcher.domain.latex.InlineSegment
import com.pallas.launcher.domain.latex.LatexDocBlock
import com.pallas.launcher.domain.latex.LatexDocumentProcessor
import com.pallas.launcher.domain.latex.LatexTextAlign
import com.pallas.launcher.platform.LatexStudioExporter
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.components.LatexFormula
import com.pallas.launcher.ui.components.TexExportTheme
import com.pallas.launcher.ui.components.preprocessLatexForRenderer
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private enum class StudioMode(val label: String) {
    EDIT("Edit"),
    PREVIEW("Preview")
}

private data class LatexSnippet(
    val label: String,
    val insert: String,
    val cursorBack: Int = 0
)

private val QUICK_SNIPPETS = listOf(
    LatexSnippet("\\frac", "\\frac{}{}", 3),
    LatexSnippet("\\sqrt", "\\sqrt{}", 1),
    LatexSnippet("x^{}", "^{}", 1),
    LatexSnippet("x_{}", "_{}", 1),
    LatexSnippet("\\sum", "\\sum_{i=1}^{n} ", 0),
    LatexSnippet("\\int", "\\int_{a}^{b} \\,dx", 4),
    LatexSnippet("\\lim", "\\lim_{x \\to \\infty} ", 0),
    LatexSnippet("$$", "\$\$\n\n\$\$", 3),
    LatexSnippet("align", "\\begin{align}\n  &= \n\\end{align}", 13),
    LatexSnippet("\\section", "\\section{}\n", 2),
    LatexSnippet("\\title", "\\title{}\n", 2),
    LatexSnippet("\\textbf", "\\textbf{}", 1)
)

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun LatexStudioScreen(
    onClose: () -> Unit,
    initialContent: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val repo = remember(context) { LatexDocumentRepository.getInstance(context) }
    val documents by repo.documents.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeDocId by remember { mutableStateOf<String?>(null) }
    var renamingDoc by remember { mutableStateOf<LatexDocument?>(null) }
    var confirmingDeleteDoc by remember { mutableStateOf<LatexDocument?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var showExportSheet by remember { mutableStateOf(false) }
    var studioMode by remember { mutableStateOf(StudioMode.EDIT) }

    // Pre-load when launched from search `tex` Expand button
    var handledInitialContent by remember(initialContent) { mutableStateOf(false) }
    LaunchedEffect(initialContent) {
        if (initialContent != null && !handledInitialContent) {
            handledInitialContent = true
            val created = repo.createDocument(initialContent = initialContent)
            activeDocId = created.id
            studioMode = StudioMode.EDIT
        }
    }

    val activeDoc = remember(documents, activeDocId) {
        documents.firstOrNull { it.id == activeDocId }
    }

    var textFieldValue by remember(activeDoc?.id) {
        mutableStateOf(
            TextFieldValue(
                text = activeDoc?.content.orEmpty(),
                selection = TextRange(activeDoc?.content?.length ?: 0)
            )
        )
    }

    // Debounced continuous auto-save
    LaunchedEffect(activeDocId, textFieldValue.text) {
        val docId = activeDocId ?: return@LaunchedEffect
        val currentStored = documents.firstOrNull { it.id == docId }?.content
        if (currentStored == textFieldValue.text) return@LaunchedEffect
        delay(350L)
        repo.updateContent(docId, textFieldValue.text)
    }

    fun flushCurrentEdit() {
        val docId = activeDocId ?: return
        repo.updateContent(docId, textFieldValue.text)
    }

    DisposableEffect(Unit) {
        onDispose { flushCurrentEdit() }
    }

    BackHandler {
        if (activeDocId != null) {
            flushCurrentEdit()
            keyboardController?.hide()
            activeDocId = null
        } else {
            onClose()
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colorScheme.surface)
            .windowInsetsPadding(
                WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
                    .union(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
            )
            .imePadding()
            .navigationBarsPadding(),
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        containerColor = colorScheme.surface,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Surface(color = colorScheme.surface) {
                Column {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(
                            onClick = {
                                if (activeDocId != null) {
                                    flushCurrentEdit()
                                    keyboardController?.hide()
                                    activeDocId = null
                                } else {
                                    onClose()
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colorScheme.onSurface
                            )
                        }

                        if (activeDoc == null) {
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(
                                    text = "LaTeX Studio",
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 24.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface
                                )
                                Text(
                                    text = "${documents.size} document${if (documents.size == 1) "" else "s"} · Auto-saved offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val created = repo.createDocument("")
                                    activeDocId = created.id
                                    studioMode = StudioMode.EDIT
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New document",
                                    tint = colorScheme.primary
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        renamingDoc = activeDoc
                                        renameInput = activeDoc.displayTitle
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeDoc.displayTitle,
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "Tap title to rename",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            // Edit / Preview toggle pill
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surfaceContainerLow,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                                modifier = Modifier.padding(end = 4.dp)
                            ) {
                                Row(modifier = Modifier.padding(2.dp)) {
                                    StudioMode.entries.forEach { mode ->
                                        val selected = studioMode == mode
                                        Surface(
                                            shape = RoundedCornerShape(8.dp),
                                            color = if (selected) colorScheme.primary else Color.Transparent,
                                            modifier = Modifier
                                                .clip(RoundedCornerShape(8.dp))
                                                .clickable {
                                                    if (mode == StudioMode.PREVIEW) {
                                                        flushCurrentEdit()
                                                        keyboardController?.hide()
                                                    }
                                                    studioMode = mode
                                                }
                                        ) {
                                            Row(
                                                modifier = Modifier.padding(horizontal = 9.dp, vertical = 6.dp),
                                                verticalAlignment = Alignment.CenterVertically,
                                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                                            ) {
                                                Icon(
                                                    imageVector = if (mode == StudioMode.EDIT) Icons.Default.Edit else Icons.Default.Visibility,
                                                    contentDescription = mode.label,
                                                    tint = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                                    modifier = Modifier.size(14.dp)
                                                )
                                                Text(
                                                    text = mode.label,
                                                    style = MaterialTheme.typography.labelSmall,
                                                    fontWeight = FontWeight.SemiBold,
                                                    color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant
                                                )
                                            }
                                        }
                                    }
                                }
                            }

                            IconButton(
                                onClick = {
                                    keyboardController?.hide()
                                    confirmingDeleteDoc = activeDoc
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Delete,
                                    contentDescription = "Delete document",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }

                            IconButton(
                                onClick = {
                                    flushCurrentEdit()
                                    keyboardController?.hide()
                                    showExportSheet = true
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.FileDownload,
                                    contentDescription = "Export document",
                                    tint = colorScheme.primary
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            }
        }
    ) { innerPadding ->
        if (activeDoc == null) {
            // Flat chronological list of saved documents
            if (documents.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(horizontal = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "LaTeX Document Studio",
                            fontFamily = LauncherFonts.serif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Type raw math or structured LaTeX right away — no \\documentclass boilerplate needed. Export to full A4 PDF, cropped PNG, or .tex.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val created = repo.createDocument("")
                                activeDocId = created.id
                                studioMode = StudioMode.EDIT
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Document", fontWeight = FontWeight.SemiBold)
                        }
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(top = 8.dp, bottom = 36.dp)
                ) {
                    items(documents, key = { it.id }) { doc ->
                        val dismissState = rememberSwipeToDismissBoxState()

                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            onDismiss = { direction ->
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    val snapshot = doc
                                    repo.deleteDocument(doc.id)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Deleted \"${snapshot.displayTitle}\"",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            repo.restoreDocument(snapshot)
                                        }
                                    }
                                }
                            },
                            backgroundContent = {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp, vertical = 4.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colorScheme.errorContainer)
                                        .padding(horizontal = 20.dp),
                                    contentAlignment = Alignment.CenterEnd
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Delete,
                                        contentDescription = "Delete",
                                        tint = colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) {
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 16.dp, vertical = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable {
                                        activeDocId = doc.id
                                        studioMode = StudioMode.EDIT
                                    }
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp, vertical = 14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Column(
                                        modifier = Modifier.weight(1f).padding(end = 8.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = doc.displayTitle,
                                            fontFamily = LauncherFonts.serif,
                                            fontSize = 19.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.onSurface,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        val snippet = doc.content.trim().replace('\n', ' ')
                                        if (snippet.isNotEmpty()) {
                                            Text(
                                                text = snippet,
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 12.sp,
                                                color = colorScheme.onSurfaceVariant,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                        }
                                        val timeAgo = DateUtils.getRelativeTimeSpanString(
                                            doc.updatedAtMs,
                                            System.currentTimeMillis(),
                                            DateUtils.MINUTE_IN_MILLIS
                                        )
                                        Text(
                                            text = "Edited $timeAgo",
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colorScheme.outline
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            renamingDoc = doc
                                            renameInput = doc.displayTitle
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Edit,
                                            contentDescription = "Rename",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                    IconButton(
                                        onClick = {
                                            val snapshot = doc
                                            repo.deleteDocument(doc.id)
                                            scope.launch {
                                                val res = snackbarHostState.showSnackbar(
                                                    message = "Deleted \"${snapshot.displayTitle}\"",
                                                    actionLabel = "Undo",
                                                    duration = SnackbarDuration.Short
                                                )
                                                if (res == SnackbarResult.ActionPerformed) {
                                                    repo.restoreDocument(snapshot)
                                                }
                                            }
                                        },
                                        modifier = Modifier.size(34.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Delete,
                                            contentDescription = "Delete document",
                                            tint = colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(17.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        } else {
            // Editor / Preview Body
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                when (studioMode) {
                    StudioMode.EDIT -> {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                                .verticalScroll(rememberScrollState())
                                .padding(horizontal = 18.dp, vertical = 14.dp)
                        ) {
                            if (textFieldValue.text.isEmpty()) {
                                Text(
                                    text = "Start typing LaTeX math or prose directly…\n\nExamples:\n\\int_0^\\infty e^{-x^2}\\,dx = \\frac{\\sqrt{\\pi}}{2}\n\nOr write structured docs:\n\\title{Notes}\n\\section{Gaussian Integral}\nConsider \$I = \\int_{-\\infty}^\\infty e^{-x^2}\\,dx\$.",
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 13.5.sp,
                                    lineHeight = 20.sp,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                                )
                            }
                            BasicTextField(
                                value = textFieldValue,
                                onValueChange = { textFieldValue = it },
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    fontFamily = FontFamily.Monospace,
                                    fontSize = 14.5.sp,
                                    lineHeight = 22.sp,
                                    color = colorScheme.onSurface
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        // Mobile-ergonomic LaTeX quick-insert bar
                        Surface(
                            color = colorScheme.surfaceContainerLow,
                            border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            LazyRow(
                                contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                items(QUICK_SNIPPETS, key = { it.label }) { snippet ->
                                    Surface(
                                        shape = RoundedCornerShape(7.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(7.dp))
                                            .clickable {
                                                val sel = textFieldValue.selection
                                                val cur = textFieldValue.text
                                                val start = minOf(sel.start, sel.end).coerceIn(0, cur.length)
                                                val end = maxOf(sel.start, sel.end).coerceIn(0, cur.length)
                                                val nextText = cur.substring(0, start) + snippet.insert + cur.substring(end)
                                                val newCursor = (start + snippet.insert.length - snippet.cursorBack)
                                                    .coerceIn(0, nextText.length)
                                                textFieldValue = TextFieldValue(
                                                    text = nextText,
                                                    selection = TextRange(newCursor)
                                                )
                                            }
                                    ) {
                                        Text(
                                            text = snippet.label,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    StudioMode.PREVIEW -> {
                        LatexDocumentPreview(
                            rawSource = textFieldValue.text,
                            modifier = Modifier
                                .fillMaxWidth()
                                .weight(1f)
                        )
                    }
                }
            }
        }
    }

    renamingDoc?.let { target ->
        AlertDialog(
            onDismissRequest = { renamingDoc = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Rename Document", style = MaterialTheme.typography.titleLarge) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("Title (leave blank for auto-title)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.renameDocument(target.id, renameInput)
                        renamingDoc = null
                    }
                ) {
                    Text("Save", color = colorScheme.primary, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingDoc = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    confirmingDeleteDoc?.let { target ->
        AlertDialog(
            onDismissRequest = { confirmingDeleteDoc = null },
            shape = RoundedCornerShape(22.dp),
            containerColor = colorScheme.surfaceContainerHigh,
            title = { Text("Delete Document?", style = MaterialTheme.typography.titleLarge) },
            text = { Text("Delete \"${target.displayTitle}\" from LaTeX Studio?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        val snapshot = target
                        confirmingDeleteDoc = null
                        if (activeDocId == snapshot.id) {
                            activeDocId = null
                        }
                        repo.deleteDocument(snapshot.id)
                        scope.launch {
                            val res = snackbarHostState.showSnackbar(
                                message = "Deleted \"${snapshot.displayTitle}\"",
                                actionLabel = "Undo",
                                duration = SnackbarDuration.Short
                            )
                            if (res == SnackbarResult.ActionPerformed) {
                                repo.restoreDocument(snapshot)
                            }
                        }
                    }
                ) {
                    Text("Delete", color = colorScheme.error, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmingDeleteDoc = null }) {
                    Text("Cancel", color = colorScheme.onSurfaceVariant)
                }
            }
        )
    }

    if (showExportSheet && activeDoc != null) {
        LatexStudioExportBottomSheet(
            title = activeDoc.displayTitle,
            rawSource = textFieldValue.text,
            onDismiss = { showExportSheet = false }
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LatexDocumentPreview(
    rawSource: String,
    modifier: Modifier = Modifier
) {
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalLauncherColors.current.isDark
    var paperLight by remember(isDark) { mutableStateOf(!isDark) }
    val parsed = remember(rawSource) { LatexDocumentProcessor.parse(rawSource) }

    val pageBg = if (paperLight) Color(0xFFFAF8F4) else Color(0xFF16181D)
    val pageFg = if (paperLight) Color(0xFF18191D) else Color(0xFFECE9E2)
    val mutedFg = if (paperLight) Color(0xFF5A5D66) else Color(0xFFA09C94)

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = if (parsed.isPureMath) "Math Expression" else "Document Layout",
                style = MaterialTheme.typography.labelMedium,
                color = colorScheme.onSurfaceVariant
            )
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .clickable { paperLight = !paperLight }
            ) {
                Text(
                    text = if (paperLight) "Paper: Light" else "Paper: Dark",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.primary,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                )
            }
        }

        Surface(
            shape = RoundedCornerShape(12.dp),
            color = pageBg,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.45f)),
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 14.dp, vertical = 4.dp)
                .padding(bottom = 12.dp)
        ) {
            val dispDate = parsed.displayDate
            if (parsed.blocks.isEmpty() && !parsed.hasHeaderMetadata && dispDate.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text(
                        text = "Nothing to preview yet",
                        style = MaterialTheme.typography.bodyMedium,
                        color = mutedFg
                    )
                }
            } else {
                val previewState = PreviewCounters()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp, vertical = 22.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    if (parsed.hasHeaderMetadata || dispDate.isNotBlank()) {
                        Column(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            if (parsed.title.isNotBlank()) {
                                Text(
                                    text = parsed.title,
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = pageFg,
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (parsed.author.isNotBlank()) {
                                Text(
                                    text = parsed.author,
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 15.sp,
                                    color = pageFg.copy(alpha = 0.9f),
                                    textAlign = TextAlign.Center
                                )
                            }
                            if (dispDate.isNotBlank()) {
                                Text(
                                    text = dispDate,
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 13.5.sp,
                                    color = mutedFg,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                    }

                    PreviewBlocksColumn(
                        blocks = parsed.blocks,
                        pageFg = pageFg,
                        mutedFg = mutedFg,
                        counters = previewState
                    )
                }
            }
        }
    }
}

private class PreviewCounters {
    var s1 = 0
    var s2 = 0
    var s3 = 0
    var eqNum = 0
    val theoremCounters = mutableMapOf<String, Int>()
    fun reset() {
        s1 = 0; s2 = 0; s3 = 0; eqNum = 0
        theoremCounters.clear()
    }
}

@Composable
private fun PreviewBlocksColumn(
    blocks: List<LatexDocBlock>,
    pageFg: Color,
    mutedFg: Color,
    counters: PreviewCounters,
    sizeMultiplier: Float = 1f
) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        blocks.forEach { block ->
            when (block) {
                is LatexDocBlock.Section -> {
                    val num = if (block.starred) "" else when (block.level) {
                        1 -> { counters.s1++; counters.s2 = 0; counters.s3 = 0; "${counters.s1}  " }
                        2 -> { counters.s2++; counters.s3 = 0; "${counters.s1}.${counters.s2}  " }
                        else -> { counters.s3++; "${counters.s1}.${counters.s2}.${counters.s3}  " }
                    }
                    Text(
                        text = num + block.title,
                        fontFamily = LauncherFonts.serif,
                        fontSize = when (block.level) { 1 -> 20.sp; 2 -> 17.sp; else -> 15.5.sp },
                        fontWeight = FontWeight.Bold,
                        color = pageFg,
                        modifier = Modifier.padding(top = 6.dp)
                    )
                }

                is LatexDocBlock.DisplayMath -> {
                    val eqLbl = if (block.numbered) { counters.eqNum++; "(${counters.eqNum})" } else null
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState()),
                            contentAlignment = Alignment.Center
                        ) {
                            LatexFormula(
                                latex = block.latex,
                                color = pageFg,
                                fontSize = (19 * sizeMultiplier).sp
                            )
                        }
                        if (eqLbl != null) {
                            Text(
                                text = eqLbl,
                                fontFamily = LauncherFonts.serif,
                                fontSize = 14.sp,
                                color = mutedFg,
                                modifier = Modifier.padding(start = 8.dp)
                            )
                        }
                    }
                }

                is LatexDocBlock.Paragraph -> {
                    InlineSegmentsRow(
                        segments = block.segments,
                        fgColor = pageFg,
                        align = block.align,
                        sizeMultiplier = sizeMultiplier
                    )
                }

                is LatexDocBlock.BulletList -> {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        block.items.forEachIndexed { idx, segs ->
                            val marker = block.customLabels.getOrNull(idx)?.takeIf { it.isNotBlank() }
                                ?: if (block.ordered) "${idx + 1}." else "•"
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = marker,
                                    fontFamily = LauncherFonts.serif,
                                    fontWeight = if (block.customLabels.getOrNull(idx) != null) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = (15.5 * sizeMultiplier).sp,
                                    color = pageFg,
                                    modifier = Modifier.padding(top = 1.dp)
                                )
                                Box(modifier = Modifier.weight(1f)) {
                                    InlineSegmentsRow(segments = segs, fgColor = pageFg, sizeMultiplier = sizeMultiplier)
                                }
                            }
                        }
                    }
                }

                is LatexDocBlock.Quote -> {
                    Box(modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 2.dp)) {
                        PreviewBlocksColumn(block.blocks, pageFg, mutedFg, counters, sizeMultiplier)
                    }
                }

                is LatexDocBlock.Abstract -> {
                    Column(
                        modifier = Modifier.fillMaxWidth().padding(horizontal = 14.dp, vertical = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = "Abstract",
                            fontFamily = LauncherFonts.serif,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp,
                            color = pageFg,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth()
                        )
                        PreviewBlocksColumn(block.blocks, pageFg, mutedFg, counters, sizeMultiplier * 0.91f)
                    }
                }

                is LatexDocBlock.TheoremBox -> {
                    val leadText = if (block.isProof) {
                        buildString {
                            append("Proof")
                            if (!block.title.isNullOrBlank()) append(" (${block.title})")
                            append(". ")
                        }
                    } else {
                        val n = (counters.theoremCounters[block.kind] ?: 0) + 1
                        counters.theoremCounters[block.kind] = n
                        buildString {
                            append("${block.kind} $n")
                            if (!block.title.isNullOrBlank()) append(" (${block.title})")
                            append(". ")
                        }
                    }
                    val leadRun = InlineSegment.TextRun(leadText, bold = !block.isProof, italic = block.isProof)
                    val merged = if (block.blocks.firstOrNull() is LatexDocBlock.Paragraph) {
                        val p0 = block.blocks.first() as LatexDocBlock.Paragraph
                        listOf(p0.copy(segments = listOf(leadRun) + p0.segments)) + block.blocks.drop(1)
                    } else {
                        listOf(LatexDocBlock.Paragraph(listOf(leadRun), align = LatexTextAlign.LEFT, noIndent = true)) + block.blocks
                    }
                    Column(modifier = Modifier.fillMaxWidth()) {
                        PreviewBlocksColumn(merged, pageFg, mutedFg, counters, sizeMultiplier)
                        if (block.isProof) {
                            Text(
                                text = "□",
                                fontFamily = LauncherFonts.serif,
                                fontSize = 14.sp,
                                color = mutedFg,
                                textAlign = TextAlign.End,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                }

                is LatexDocBlock.Tabular -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = Color.Transparent,
                        border = BorderStroke(0.8.dp, mutedFg.copy(alpha = 0.25f)),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        Column(modifier = Modifier.padding(8.dp)) {
                            val numCols = maxOf(block.colAligns.size, block.rows.maxOfOrNull { it.size } ?: 0)
                            block.rows.forEachIndexed { rIdx, row ->
                                if (rIdx in block.hlinesBeforeRow) {
                                    HorizontalDivider(color = mutedFg.copy(alpha = 0.45f), modifier = Modifier.padding(vertical = 3.dp))
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                                    for (c in 0 until numCols) {
                                        val segs = row.getOrElse(c) { emptyList() }
                                        Box(modifier = Modifier.padding(vertical = 2.dp)) {
                                            InlineSegmentsRow(segments = segs, fgColor = pageFg, sizeMultiplier = sizeMultiplier)
                                        }
                                    }
                                }
                            }
                            if (block.rows.size in block.hlinesBeforeRow) {
                                HorizontalDivider(color = mutedFg.copy(alpha = 0.45f), modifier = Modifier.padding(top = 3.dp))
                            }
                        }
                    }
                }

                is LatexDocBlock.Verbatim -> {
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = mutedFg.copy(alpha = 0.1f),
                        modifier = Modifier.fillMaxWidth().horizontalScroll(rememberScrollState())
                    ) {
                        Text(
                            text = block.text,
                            fontFamily = FontFamily.Monospace,
                            fontSize = (13 * sizeMultiplier).sp,
                            color = pageFg,
                            modifier = Modifier.padding(10.dp)
                        )
                    }
                }

                LatexDocBlock.HorizontalRule -> {
                    HorizontalDivider(color = mutedFg.copy(alpha = 0.5f), modifier = Modifier.padding(vertical = 4.dp))
                }

                is LatexDocBlock.VerticalSpace -> {
                    Spacer(modifier = Modifier.height((block.pt * 0.7f).dp))
                }

                LatexDocBlock.PageBreak -> {
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        HorizontalDivider(modifier = Modifier.weight(1f), color = mutedFg.copy(alpha = 0.3f))
                        Text("Page Break", fontSize = 10.sp, color = mutedFg)
                        HorizontalDivider(modifier = Modifier.weight(1f), color = mutedFg.copy(alpha = 0.3f))
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun InlineSegmentsRow(
    segments: List<InlineSegment>,
    fgColor: Color,
    align: LatexTextAlign = LatexTextAlign.JUSTIFY,
    sizeMultiplier: Float = 1f
) {
    val hasInlineMath = segments.any { it is InlineSegment.InlineMath }
    val composeAlign = when (align) {
        LatexTextAlign.CENTER -> TextAlign.Center
        LatexTextAlign.RIGHT -> TextAlign.End
        LatexTextAlign.LEFT -> TextAlign.Start
        LatexTextAlign.JUSTIFY -> TextAlign.Justify
    }
    if (!hasInlineMath) {
        val annotated = remember(segments, fgColor, sizeMultiplier) {
            buildAnnotatedString {
                segments.filterIsInstance<InlineSegment.TextRun>().forEach { run ->
                    val basePt = 15.5f * sizeMultiplier * run.sizeScale
                    withStyle(
                        SpanStyle(
                            fontWeight = if (run.bold) FontWeight.Bold else FontWeight.Normal,
                            fontStyle = if (run.italic) FontStyle.Italic else FontStyle.Normal,
                            fontFamily = when {
                                run.mono -> FontFamily.Monospace
                                run.sans -> FontFamily.SansSerif
                                else -> LauncherFonts.serif
                            },
                            fontSize = basePt.sp,
                            textDecoration = if (run.underline) TextDecoration.Underline else TextDecoration.None,
                            color = fgColor
                        )
                    ) {
                        append(if (run.smallCaps) run.text.uppercase() else run.text)
                    }
                }
            }
        }
        Text(
            text = annotated,
            fontSize = (15.5f * sizeMultiplier).sp,
            lineHeight = (23f * sizeMultiplier).sp,
            textAlign = composeAlign,
            color = fgColor,
            modifier = Modifier.fillMaxWidth()
        )
    } else {
        FlowRow(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = when (align) {
                LatexTextAlign.CENTER -> Arrangement.spacedBy(2.dp, Alignment.CenterHorizontally)
                LatexTextAlign.RIGHT -> Arrangement.spacedBy(2.dp, Alignment.End)
                else -> Arrangement.spacedBy(2.dp)
            },
            verticalArrangement = Arrangement.Center
        ) {
            segments.forEach { seg ->
                when (seg) {
                    is InlineSegment.TextRun -> {
                        val displayText = if (seg.smallCaps) seg.text.uppercase() else seg.text
                        val sz = (15.5f * sizeMultiplier * seg.sizeScale).sp
                        displayText.split(Regex("""(?<=\s)|(?=\s)""")).filter { it.isNotEmpty() }.forEach { word ->
                            Text(
                                text = word,
                                fontFamily = when {
                                    seg.mono -> FontFamily.Monospace
                                    seg.sans -> FontFamily.SansSerif
                                    else -> LauncherFonts.serif
                                },
                                fontWeight = if (seg.bold) FontWeight.Bold else FontWeight.Normal,
                                fontStyle = if (seg.italic) FontStyle.Italic else FontStyle.Normal,
                                textDecoration = if (seg.underline) TextDecoration.Underline else TextDecoration.None,
                                fontSize = sz,
                                lineHeight = (23f * sizeMultiplier * seg.sizeScale).sp,
                                color = fgColor
                            )
                        }
                    }
                    is InlineSegment.InlineMath -> {
                        Box(
                            modifier = Modifier
                                .padding(horizontal = 2.dp)
                                .align(Alignment.CenterVertically)
                        ) {
                            LatexFormula(
                                latex = seg.latex,
                                color = fgColor,
                                fontSize = (15f * sizeMultiplier * seg.sizeScale).sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LatexStudioExportBottomSheet(
    title: String,
    rawSource: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()

    var pngTheme by remember { mutableStateOf(TexExportTheme.LIGHT) }
    var isBusy by remember { mutableStateOf(false) }

    val renderMathBitmap: suspend (String, Float, Int) -> android.graphics.Bitmap? = { latex, fontSp, fgArgb ->
        runCatching {
            val clean = preprocessLatexForRenderer(latex)
            exporter.export(
                latex = clean,
                config = LatexConfig(
                    fontSize = fontSp.sp,
                    theme = LatexTheme.light(
                        color = Color(fgArgb),
                        backgroundColor = Color.Transparent
                    )
                ),
                exportConfig = ExportConfig(
                    scale = 3f,
                    format = ImageFormat.PNG,
                    transparentBackground = true
                )
            )?.imageBitmap?.asAndroidBitmap()
        }.getOrNull()
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colorScheme.surfaceContainerHigh
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp)
                .padding(bottom = 24.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(
                text = "Export Document",
                fontFamily = LauncherFonts.serif,
                fontSize = 24.sp,
                fontWeight = FontWeight.SemiBold,
                color = colorScheme.onSurface
            )

            // 1. Full LaTeX PDF
            ExportFormatCard(
                icon = Icons.Default.PictureAsPdf,
                title = "PDF Document (.pdf)",
                subtitle = "LaTeX article layout with preamble sizing, geometry & rendered math",
                enabled = !isBusy,
                onSave = {
                    scope.launch {
                        isBusy = true
                        try {
                            LatexStudioExporter.savePdfToDocuments(context, title, rawSource, renderMathBitmap)
                                .onSuccess {
                                    Toast.makeText(context, "Saved PDF to Documents/Pallas", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .onFailure {
                                    Toast.makeText(context, "Could not export PDF", Toast.LENGTH_SHORT).show()
                                }
                        } finally {
                            isBusy = false
                        }
                    }
                },
                onShare = {
                    scope.launch {
                        isBusy = true
                        try {
                            LatexStudioExporter.sharePdf(context, title, rawSource, renderMathBitmap)
                                .onFailure {
                                    Toast.makeText(context, "Could not share PDF", Toast.LENGTH_SHORT).show()
                                }
                        } finally {
                            isBusy = false
                        }
                    }
                }
            )

            // 2. Cropped PNG Image
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Icon(Icons.Default.Image, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text("Cropped Image (.png)", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                            Text("Tightly cropped to content (no full page)", style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        TexExportTheme.entries.forEach { opt ->
                            val sel = pngTheme == opt
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (sel) colorScheme.primaryContainer else colorScheme.surfaceContainerHigh,
                                border = BorderStroke(1.dp, if (sel) colorScheme.primary.copy(alpha = 0.6f) else colorScheme.outlineVariant.copy(alpha = 0.4f)),
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable { pngTheme = opt }
                            ) {
                                Box(modifier = Modifier.padding(vertical = 7.dp), contentAlignment = Alignment.Center) {
                                    Text(
                                        text = opt.label,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = if (sel) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    try {
                                        val bmp = LatexStudioExporter.renderCroppedPngBitmap(context, rawSource, pngTheme, renderMathBitmap)
                                        if (bmp == null) {
                                            Toast.makeText(context, "Nothing to render", Toast.LENGTH_SHORT).show()
                                        } else {
                                            TexExportHelper.copyPngToClipboard(context, bmp)
                                                .onSuccess {
                                                    Toast.makeText(context, "Copied PNG to clipboard", Toast.LENGTH_SHORT).show()
                                                    onDismiss()
                                                }
                                                .onFailure {
                                                    Toast.makeText(context, "Could not copy PNG", Toast.LENGTH_SHORT).show()
                                                }
                                        }
                                    } finally {
                                        isBusy = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy", maxLines = 1)
                        }
                        OutlinedButton(
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    try {
                                        val bmp = LatexStudioExporter.renderCroppedPngBitmap(context, rawSource, pngTheme, renderMathBitmap)
                                        if (bmp == null) {
                                            Toast.makeText(context, "Nothing to render", Toast.LENGTH_SHORT).show()
                                        } else {
                                            TexExportHelper.savePngToGallery(
                                                context,
                                                bmp,
                                                filePrefix = LatexStudioExporter.sanitizeFileName(title),
                                                folderName = "Pallas"
                                            ).onSuccess {
                                                Toast.makeText(context, "Saved PNG to Pictures/Pallas", Toast.LENGTH_SHORT).show()
                                                onDismiss()
                                            }.onFailure {
                                                Toast.makeText(context, "Could not save PNG", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } finally {
                                        isBusy = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Save", maxLines = 1)
                        }
                        Button(
                            enabled = !isBusy,
                            contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp),
                            onClick = {
                                scope.launch {
                                    isBusy = true
                                    try {
                                        val bmp = LatexStudioExporter.renderCroppedPngBitmap(context, rawSource, pngTheme, renderMathBitmap)
                                        if (bmp == null) {
                                            Toast.makeText(context, "Nothing to render", Toast.LENGTH_SHORT).show()
                                        } else {
                                            TexExportHelper.sharePng(context, bmp).onFailure {
                                                Toast.makeText(context, "Could not share PNG", Toast.LENGTH_SHORT).show()
                                            }
                                        }
                                    } finally {
                                        isBusy = false
                                    }
                                }
                            },
                            shape = RoundedCornerShape(10.dp),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Share", maxLines = 1)
                        }
                    }
                }
            }

            // 3. Compilable .tex Source
            ExportFormatCard(
                icon = Icons.Default.Code,
                title = "LaTeX Source (.tex)",
                subtitle = "Full preamble (amsmath, amssymb) ready for external compilation",
                enabled = !isBusy,
                onSave = {
                    scope.launch {
                        isBusy = true
                        try {
                            LatexStudioExporter.saveTexToDocuments(context, title, rawSource)
                                .onSuccess {
                                    Toast.makeText(context, "Saved .tex to Documents/Pallas", Toast.LENGTH_SHORT).show()
                                    onDismiss()
                                }
                                .onFailure {
                                    Toast.makeText(context, "Could not save .tex", Toast.LENGTH_SHORT).show()
                                }
                        } finally {
                            isBusy = false
                        }
                    }
                },
                onShare = {
                    scope.launch {
                        isBusy = true
                        try {
                            LatexStudioExporter.shareTex(context, title, rawSource)
                                .onFailure {
                                    Toast.makeText(context, "Could not share .tex", Toast.LENGTH_SHORT).show()
                                }
                        } finally {
                            isBusy = false
                        }
                    }
                }
            )
        }
    }
}

@Composable
private fun ExportFormatCard(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    enabled: Boolean,
    onSave: () -> Unit,
    onShare: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceContainerLow,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(icon, contentDescription = null, tint = colorScheme.primary, modifier = Modifier.size(22.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                    Text(subtitle, style = MaterialTheme.typography.bodySmall, color = colorScheme.onSurfaceVariant)
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    enabled = enabled,
                    onClick = onSave,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Save")
                }
                Button(
                    enabled = enabled,
                    onClick = onShare,
                    shape = RoundedCornerShape(10.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Share")
                }
            }
        }
    }
}
