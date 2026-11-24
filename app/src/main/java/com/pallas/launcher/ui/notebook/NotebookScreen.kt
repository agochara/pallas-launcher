package com.pallas.launcher.ui.notebook

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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.IosShare
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.data.repository.NotebookRepository
import com.pallas.launcher.domain.notebook.ComputationalNotebook
import com.pallas.launcher.domain.notebook.NotebookCell
import com.pallas.launcher.domain.notebook.NotebookCellOutput
import com.pallas.launcher.domain.notebook.NotebookKernel
import com.pallas.launcher.domain.search.MathChartData
import com.pallas.launcher.platform.TexExportHelper
import com.pallas.launcher.ui.components.ChartPreviewDialog
import com.pallas.launcher.ui.components.LatexFormula
import com.pallas.launcher.ui.components.MathChartCanvas
import com.pallas.launcher.ui.components.TexPreviewDialog
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun NotebookScreen(
    onClose: () -> Unit,
    initialExpression: String? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalLauncherColors.current.isDark
    val scope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val repo = remember(context) { NotebookRepository.getInstance(context) }
    val notebooks by repo.notebooks.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    var activeNotebookId by remember { mutableStateOf<String?>(null) }
    var editorCells by remember { mutableStateOf<List<NotebookCell>>(emptyList()) }
    var definedSymbols by remember { mutableStateOf<List<String>>(emptyList()) }
    var isRunning by remember { mutableStateOf(false) }

    var renamingNotebook by remember { mutableStateOf<ComputationalNotebook?>(null) }
    var renameInput by remember { mutableStateOf("") }
    var previewChart by remember { mutableStateOf<MathChartData?>(null) }
    var previewLatex by remember { mutableStateOf<String?>(null) }
    var evalJob by remember { mutableStateOf<kotlinx.coroutines.Job?>(null) }

    val kernel = remember { NotebookKernel() }
    val activeNotebookMeta = remember(notebooks, activeNotebookId) {
        notebooks.firstOrNull { it.id == activeNotebookId }
    }

    val latestActiveMeta by rememberUpdatedState(activeNotebookMeta)
    val latestEditorCells by rememberUpdatedState(editorCells)

    fun openNotebookInEditor(nb: ComputationalNotebook) {
        evalJob?.cancel()
        evalJob = null
        isRunning = false
        kernel.reset()
        activeNotebookId = nb.id
        editorCells = nb.cells.ifEmpty { listOf(NotebookCell()) }
        definedSymbols = emptyList()
    }

    fun flushActiveNotebook() {
        val meta = latestActiveMeta ?: return
        val cells = latestEditorCells
        if (cells.isNotEmpty() && meta.cells != cells) {
            repo.saveNotebook(meta.copy(cells = cells))
        }
    }

    // Auto-create & evaluate Cell 1 when launched from search Expand button
    var handledInitialExpression by remember(initialExpression) { mutableStateOf(false) }
    LaunchedEffect(initialExpression) {
        val initial = initialExpression?.trim()
        if (!initial.isNullOrEmpty() && !handledInitialExpression) {
            handledInitialExpression = true
            kernel.reset()
            val firstCell = NotebookCell(source = initial)
            val created = repo.createNotebook(initialCells = listOf(firstCell))
            activeNotebookId = created.id
            editorCells = created.cells
            definedSymbols = emptyList()

            isRunning = true
            try {
                val (execNum, out, syms) = withContext(Dispatchers.Default) {
                    val n = kernel.nextExecutionNumber()
                    val res = kernel.evaluateCell(initial)
                    Triple(n, res, kernel.definedSymbols())
                }
                val updatedCells = listOf(firstCell.copy(executionOrder = execNum, output = out))
                editorCells = updatedCells
                definedSymbols = syms
                repo.saveNotebook(created.copy(cells = updatedCells))
            } finally {
                isRunning = false
            }
        }
    }

    // Debounced auto-save when cell sources change
    LaunchedEffect(activeNotebookId, editorCells) {
        val meta = activeNotebookMeta ?: return@LaunchedEffect
        if (meta.cells == editorCells || editorCells.isEmpty()) return@LaunchedEffect
        delay(350L)
        repo.saveNotebook(meta.copy(cells = editorCells))
    }

    DisposableEffect(Unit) {
        onDispose {
            evalJob?.cancel()
            evalJob = null
            flushActiveNotebook()
        }
    }

    fun runSingleCell(cellId: String) {
        if (isRunning) return
        val targetNotebookId = activeNotebookId ?: return
        val currentCells = editorCells
        val target = currentCells.firstOrNull { it.id == cellId } ?: return
        val snapshotSource = target.source
        keyboardController?.hide()
        evalJob?.cancel()
        evalJob = scope.launch {
            isRunning = true
            try {
                if (snapshotSource.isBlank()) {
                    if (activeNotebookId != targetNotebookId) return@launch
                    val updated = editorCells.map {
                        if (it.id == cellId) it.copy(executionOrder = null, output = null) else it
                    }
                    editorCells = updated
                    latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = updated)) }
                } else {
                    val (execNum, out, syms) = withContext(Dispatchers.Default) {
                        val n = kernel.nextExecutionNumber()
                        val res = kernel.evaluateCell(snapshotSource)
                        Triple(n, res, kernel.definedSymbols())
                    }
                    if (activeNotebookId != targetNotebookId) return@launch
                    val updated = editorCells.map {
                        if (it.id == cellId && it.source == snapshotSource) {
                            it.copy(executionOrder = execNum, output = out)
                        } else {
                            it
                        }
                    }
                    editorCells = updated
                    definedSymbols = syms
                    latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = updated)) }
                }
            } finally {
                isRunning = false
            }
        }
    }

    fun runAllCells() {
        if (isRunning) return
        val targetNotebookId = activeNotebookId ?: return
        val snapshot = editorCells
        val snapshotSourceById = snapshot.associate { it.id to it.source }
        keyboardController?.hide()
        evalJob?.cancel()
        evalJob = scope.launch {
            isRunning = true
            try {
                val (resultById, syms) = withContext(Dispatchers.Default) {
                    kernel.reset()
                    val resultMap = snapshot.associate { cell ->
                        val evaluated = if (cell.source.isBlank()) {
                            cell.copy(executionOrder = null, output = null)
                        } else {
                            val n = kernel.nextExecutionNumber()
                            val res = kernel.evaluateCell(cell.source)
                            cell.copy(executionOrder = n, output = res)
                        }
                        cell.id to evaluated
                    }
                    resultMap to kernel.definedSymbols()
                }
                if (activeNotebookId != targetNotebookId) return@launch
                val updated = editorCells.map { cell ->
                    val res = resultById[cell.id]
                    if (res != null && cell.source == snapshotSourceById[cell.id]) {
                        cell.copy(executionOrder = res.executionOrder, output = res.output)
                    } else {
                        cell
                    }
                }
                editorCells = updated
                definedSymbols = syms
                latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = updated)) }
            } finally {
                isRunning = false
            }
        }
    }

    fun resetKernel() {
        if (isRunning) return
        kernel.reset()
        definedSymbols = emptyList()
        val cleared = editorCells.map { it.copy(executionOrder = null) }
        editorCells = cleared
        latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = cleared)) }
        Toast.makeText(context, "Kernel reset", Toast.LENGTH_SHORT).show()
    }

    fun addCell() {
        val updated = editorCells + NotebookCell()
        editorCells = updated
        latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = updated)) }
    }

    fun moveCell(index: Int, delta: Int) {
        val target = index + delta
        if (index !in editorCells.indices || target !in editorCells.indices) return
        val mutable = editorCells.toMutableList()
        val item = mutable.removeAt(index)
        mutable.add(target, item)
        editorCells = mutable
        latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = mutable)) }
    }

    fun deleteCell(index: Int) {
        if (index !in editorCells.indices) return
        val updated = if (editorCells.size <= 1) {
            listOf(NotebookCell(id = editorCells[0].id))
        } else {
            editorCells.toMutableList().also { it.removeAt(index) }
        }
        editorCells = updated
        latestActiveMeta?.let { repo.saveNotebook(it.copy(cells = updated)) }
    }

    BackHandler {
        if (activeNotebookId != null) {
            evalJob?.cancel()
            evalJob = null
            isRunning = false
            flushActiveNotebook()
            keyboardController?.hide()
            activeNotebookId = null
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
                                if (activeNotebookId != null) {
                                    flushActiveNotebook()
                                    keyboardController?.hide()
                                    activeNotebookId = null
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

                        if (activeNotebookMeta == null) {
                            Column(modifier = Modifier.weight(1f).padding(start = 4.dp)) {
                                Text(
                                    text = "Computational Notebook",
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 23.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${notebooks.size} notebook${if (notebooks.size == 1) "" else "s"} · Auto-saved offline",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }
                            IconButton(
                                onClick = {
                                    val created = repo.createNotebook()
                                    openNotebookInEditor(created)
                                }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Add,
                                    contentDescription = "New notebook",
                                    tint = colorScheme.primary
                                )
                            }
                        } else {
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .clickable {
                                        renamingNotebook = activeNotebookMeta
                                        renameInput = activeNotebookMeta.displayTitle
                                    }
                                    .padding(horizontal = 6.dp, vertical = 2.dp)
                            ) {
                                Text(
                                    text = activeNotebookMeta.displayTitle,
                                    fontFamily = LauncherFonts.serif,
                                    fontSize = 19.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colorScheme.onSurface,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = "${editorCells.size} cell${if (editorCells.size == 1) "" else "s"} · Tap title to rename",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant
                                )
                            }

                            // Run All button
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = colorScheme.primaryContainer,
                                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
                                modifier = Modifier
                                    .padding(end = 4.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable(enabled = !isRunning) { runAllCells() }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.PlayArrow,
                                        contentDescription = "Run all cells",
                                        tint = colorScheme.onPrimaryContainer,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        text = "Run All",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onPrimaryContainer
                                    )
                                }
                            }

                            IconButton(
                                enabled = !isRunning,
                                onClick = { resetKernel() }
                            ) {
                                Icon(
                                    imageVector = Icons.Default.RestartAlt,
                                    contentDescription = "Reset Kernel",
                                    tint = colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                    HorizontalDivider(thickness = 0.5.dp, color = colorScheme.outlineVariant.copy(alpha = 0.35f))
                }
            }
        }
    ) { innerPadding ->
        if (activeNotebookMeta == null) {
            if (notebooks.isEmpty()) {
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
                            text = "Computational Notebook",
                            fontFamily = LauncherFonts.serif,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Medium,
                            color = colorScheme.onSurface
                        )
                        Text(
                            text = "Evaluate multi-cell math, define variables & functions shared across cells, plot charts, and convert expressions with TeXForm.",
                            style = MaterialTheme.typography.bodyMedium,
                            color = colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center
                        )
                        Button(
                            onClick = {
                                val created = repo.createNotebook()
                                openNotebookInEditor(created)
                            },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colorScheme.primary,
                                contentColor = colorScheme.onPrimary
                            ),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("New Notebook", fontWeight = FontWeight.SemiBold)
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
                    items(notebooks, key = { it.id }) { nb ->
                        val dismissState = rememberSwipeToDismissBoxState()
                        SwipeToDismissBox(
                            state = dismissState,
                            enableDismissFromStartToEnd = false,
                            onDismiss = { direction ->
                                if (direction == SwipeToDismissBoxValue.EndToStart) {
                                    val snapshot = nb
                                    repo.deleteNotebook(nb.id)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Deleted \"${snapshot.displayTitle}\"",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            repo.restoreNotebook(snapshot)
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
                                        contentDescription = "Delete notebook",
                                        tint = colorScheme.onErrorContainer
                                    )
                                }
                            }
                        ) {
                            NotebookListRow(
                                notebook = nb,
                                onOpen = { openNotebookInEditor(nb) },
                                onRename = {
                                    renamingNotebook = nb
                                    renameInput = nb.displayTitle
                                },
                                onDelete = {
                                    val snapshot = nb
                                    repo.deleteNotebook(nb.id)
                                    scope.launch {
                                        val res = snackbarHostState.showSnackbar(
                                            message = "Deleted \"${snapshot.displayTitle}\"",
                                            actionLabel = "Undo",
                                            duration = SnackbarDuration.Short
                                        )
                                        if (res == SnackbarResult.ActionPerformed) {
                                            repo.restoreNotebook(snapshot)
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
            ) {
                if (definedSymbols.isNotEmpty()) {
                    Surface(
                        color = colorScheme.surfaceContainerLow,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            FlowRow(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 7.dp),
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "Kernel:",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.align(Alignment.CenterVertically)
                                )
                                definedSymbols.forEach { sym ->
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = colorScheme.surfaceContainerHigh,
                                        border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
                                    ) {
                                        Text(
                                            text = sym,
                                            fontFamily = FontFamily.Monospace,
                                            fontSize = 11.sp,
                                            color = colorScheme.primary,
                                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                            }
                            HorizontalDivider(
                                thickness = 0.5.dp,
                                color = colorScheme.outlineVariant.copy(alpha = 0.3f)
                            )
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(horizontal = 14.dp, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    itemsIndexed(editorCells, key = { _, cell -> cell.id }) { index, cell ->
                        NotebookCellCard(
                            cell = cell,
                            index = index,
                            totalCount = editorCells.size,
                            isDark = isDark,
                            isRunning = isRunning,
                            onSourceChange = { updatedText ->
                                editorCells = editorCells.map {
                                    if (it.id == cell.id) it.copy(source = updatedText) else it
                                }
                            },
                            onRun = { runSingleCell(cell.id) },
                            onMoveUp = { moveCell(index, -1) },
                            onMoveDown = { moveCell(index, 1) },
                            onDelete = { deleteCell(index) },
                            onCopyOutput = { text ->
                                TexExportHelper.copyPlainText(context, "Notebook Output", text, "Copied output")
                            },
                            onPreviewChart = { chart -> previewChart = chart },
                            onPreviewLatex = { latex -> previewLatex = latex }
                        )
                    }

                    item(key = "add_cell_footer") {
                        OutlinedButton(
                            enabled = !isRunning,
                            onClick = { addCell() },
                            shape = RoundedCornerShape(10.dp),
                            border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.45f)),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 2.dp, bottom = 28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Add,
                                contentDescription = null,
                                tint = colorScheme.primary,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Add Cell",
                                color = colorScheme.primary,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }

    renamingNotebook?.let { nb ->
        AlertDialog(
            onDismissRequest = { renamingNotebook = null },
            containerColor = colorScheme.surfaceContainerHigh,
            title = {
                Text(
                    text = "Rename Notebook",
                    fontFamily = LauncherFonts.serif,
                    fontSize = 20.sp
                )
            },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    placeholder = { Text(nb.title) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        repo.renameNotebook(nb.id, renameInput)
                        renamingNotebook = null
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { renamingNotebook = null }) {
                    Text("Cancel")
                }
            }
        )
    }

    previewChart?.let { chart ->
        ChartPreviewDialog(
            chart = chart,
            onDismiss = { previewChart = null }
        )
    }

    previewLatex?.let { latex ->
        TexPreviewDialog(
            latex = latex,
            onDismiss = { previewLatex = null }
        )
    }
}

@Composable
private fun NotebookListRow(
    notebook: ComputationalNotebook,
    onOpen: () -> Unit,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onOpen)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 16.dp, end = 6.dp, top = 12.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                modifier = Modifier.weight(1f).padding(end = 6.dp),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                Text(
                    text = notebook.displayTitle,
                    fontFamily = LauncherFonts.serif,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val snippet = notebook.cells.firstOrNull { it.source.isNotBlank() }?.source
                    ?.trim()?.replace('\n', ' ').orEmpty()
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
                    notebook.updatedAtMs,
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS
                )
                Text(
                    text = "${notebook.cells.size} cell${if (notebook.cells.size == 1) "" else "s"} · Edited $timeAgo",
                    style = MaterialTheme.typography.labelSmall,
                    color = colorScheme.outline
                )
            }
            IconButton(onClick = onRename, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Edit,
                    contentDescription = "Rename notebook",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
            IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete notebook",
                    tint = colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

@Composable
private fun NotebookCellCard(
    cell: NotebookCell,
    index: Int,
    totalCount: Int,
    isDark: Boolean,
    isRunning: Boolean,
    onSourceChange: (String) -> Unit,
    onRun: () -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onDelete: () -> Unit,
    onCopyOutput: (String) -> Unit,
    onPreviewChart: (MathChartData) -> Unit,
    onPreviewLatex: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val inLabel = "In [${cell.executionOrder?.toString() ?: " "}]:"

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colorScheme.surfaceContainerHigh,
        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.4f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Cell header bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(colorScheme.surfaceContainerLow.copy(alpha = 0.65f))
                    .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = inLabel,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colorScheme.primary,
                    modifier = Modifier.weight(1f)
                )

                IconButton(
                    enabled = index > 0 && !isRunning,
                    onClick = onMoveUp,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowUp,
                        contentDescription = "Move cell up",
                        tint = if (index > 0) colorScheme.onSurfaceVariant else colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    enabled = index < totalCount - 1 && !isRunning,
                    onClick = onMoveDown,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.KeyboardArrowDown,
                        contentDescription = "Move cell down",
                        tint = if (index < totalCount - 1) colorScheme.onSurfaceVariant else colorScheme.outline.copy(alpha = 0.4f),
                        modifier = Modifier.size(18.dp)
                    )
                }

                IconButton(
                    enabled = !isRunning,
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = if (totalCount > 1) "Delete cell" else "Clear cell",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    enabled = !isRunning,
                    onClick = onRun,
                    modifier = Modifier.size(34.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run cell",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Cell input editor
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = cell.source,
                    onValueChange = onSourceChange,
                    textStyle = TextStyle(
                        fontFamily = FontFamily.Monospace,
                        fontSize = 14.sp,
                        lineHeight = 20.sp,
                        color = colorScheme.onSurface
                    ),
                    cursorBrush = SolidColor(colorScheme.primary),
                    modifier = Modifier.fillMaxWidth(),
                    decorationBox = { innerTextField ->
                        if (cell.source.isEmpty()) {
                            Text(
                                text = "e.g. x = Range[10]; ListLinePlot[x^2] · TeXForm[x] · tex \\frac{1}{2}",
                                fontFamily = FontFamily.Monospace,
                                fontSize = 13.sp,
                                color = colorScheme.onSurfaceVariant.copy(alpha = 0.55f)
                            )
                        }
                        innerTextField()
                    }
                )
            }

            // Cell output panel
            cell.output?.let { out ->
                HorizontalDivider(
                    thickness = 0.5.dp,
                    color = colorScheme.outlineVariant.copy(alpha = 0.35f)
                )
                NotebookCellOutputPanel(
                    output = out,
                    executionOrder = cell.executionOrder,
                    isDark = isDark,
                    onCopyOutput = onCopyOutput,
                    onPreviewChart = onPreviewChart,
                    onPreviewLatex = onPreviewLatex
                )
            }
        }
    }
}

@Composable
private fun NotebookCellOutputPanel(
    output: NotebookCellOutput,
    executionOrder: Int?,
    isDark: Boolean,
    onCopyOutput: (String) -> Unit,
    onPreviewChart: (MathChartData) -> Unit,
    onPreviewLatex: (String) -> Unit
) {
    val colorScheme = MaterialTheme.colorScheme
    val outLabel = "Out [${executionOrder?.toString() ?: " "}]:"
    val containerColor = if (output.isError) {
        colorScheme.errorContainer.copy(alpha = 0.35f)
    } else {
        colorScheme.surfaceContainerLowest.copy(alpha = 0.7f)
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(containerColor)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // Out [n]: label + badge + Copy/Export actions
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = outLabel,
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (output.isError) colorScheme.error else colorScheme.onSurfaceVariant
            )
            output.badge?.takeIf { it.isNotBlank() }?.let { badge ->
                Spacer(modifier = Modifier.width(8.dp))
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (output.isError) {
                        colorScheme.error.copy(alpha = 0.16f)
                    } else {
                        colorScheme.primaryContainer.copy(alpha = 0.65f)
                    }
                ) {
                    Text(
                        text = badge,
                        style = MaterialTheme.typography.labelSmall,
                        color = if (output.isError) colorScheme.error else colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.weight(1f))
            if (output.copyText.isNotEmpty()) {
                IconButton(
                    onClick = { onCopyOutput(output.copyText) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Copy output",
                        tint = colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
            if (output.chart != null) {
                IconButton(
                    onClick = { onPreviewChart(output.chart) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Expand & export chart",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            } else if (!output.latexSource.isNullOrBlank()) {
                IconButton(
                    onClick = { onPreviewLatex(output.latexSource) },
                    modifier = Modifier.size(28.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.IosShare,
                        contentDescription = "Expand & export LaTeX",
                        tint = colorScheme.primary,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }

        // Console Print[...] output
        if (!output.consoleOutput.isNullOrBlank()) {
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colorScheme.surfaceContainerLow,
                border = BorderStroke(0.5.dp, colorScheme.outlineVariant.copy(alpha = 0.35f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = output.consoleOutput,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 12.sp,
                    color = colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp)
                )
            }
        }

        // Visual / Formula / Text output
        when {
            output.chart != null -> {
                Text(
                    text = output.chart.title,
                    fontFamily = LauncherFonts.serif,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = colorScheme.onSurface
                )
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colorScheme.surfaceContainer)
                        .clickable { onPreviewChart(output.chart) }
                        .padding(6.dp)
                ) {
                    MathChartCanvas(
                        chart = output.chart,
                        isDark = isDark,
                        modifier = Modifier.fillMaxSize()
                    )
                }
                if (output.subtitle.isNotBlank()) {
                    Text(
                        text = output.subtitle,
                        style = MaterialTheme.typography.labelSmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
            !output.latexSource.isNullOrBlank() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .clickable { onPreviewLatex(output.latexSource) }
                        .horizontalScroll(rememberScrollState())
                        .padding(vertical = 6.dp)
                ) {
                    LatexFormula(
                        latex = output.latexSource,
                        color = colorScheme.onSurface,
                        fontSize = 18.sp
                    )
                }
            }
            output.primaryValue.isNotBlank() -> {
                Text(
                    text = output.primaryValue,
                    fontFamily = if (output.isError) LauncherFonts.sans else FontFamily.Monospace,
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Medium,
                    color = if (output.isError) colorScheme.error else colorScheme.onSurface
                )
                if (output.subtitle.isNotBlank()) {
                    Text(
                        text = output.subtitle,
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}
