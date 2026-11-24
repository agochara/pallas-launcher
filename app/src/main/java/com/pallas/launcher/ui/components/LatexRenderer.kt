package com.pallas.launcher.ui.components

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.hrm.latex.renderer.Latex
import com.hrm.latex.renderer.export.ExportConfig
import com.hrm.latex.renderer.export.ImageFormat
import com.hrm.latex.renderer.export.rememberLatexExporter
import com.hrm.latex.renderer.model.LatexConfig
import com.hrm.latex.renderer.model.LatexTheme
import com.pallas.launcher.platform.TexExportHelper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

enum class TexExportTheme(val label: String) {
    LIGHT("Light"),
    DARK("Dark"),
    TRANSPARENT("Clear")
}

private val IMPLIED_BY_REGEX = Regex("""(\\+)impliedby(?![A-Za-z])""")

internal fun preprocessLatexForRenderer(raw: String): String {
    if (!raw.contains("\\impliedby")) return raw
    return IMPLIED_BY_REGEX.replace(raw) { match ->
        val slashes = match.groupValues[1]
        if (slashes.length % 2 == 1) {
            slashes.dropLast(1) + "\\;\\Longleftarrow\\;"
        } else {
            match.value
        }
    }
}

@Composable
fun LatexFormula(
    latex: String,
    color: Color = MaterialTheme.colorScheme.onSurface,
    backgroundColor: Color = Color.Transparent,
    fontSize: TextUnit = 18.sp,
    modifier: Modifier = Modifier
) {
    val renderLatex = remember(latex) { preprocessLatexForRenderer(latex) }
    val config = remember(fontSize, color, backgroundColor) {
        LatexConfig(
            fontSize = fontSize,
            theme = LatexTheme.light(
                color = color,
                backgroundColor = backgroundColor
            )
        )
    }
    Latex(
        latex = renderLatex,
        modifier = modifier,
        config = config
    )
}

private fun buildPaddedExportBitmap(
    raw: Bitmap,
    paddingPx: Int,
    bgArgb: Int?
): Bitmap {
    val outWidth = (raw.width + paddingPx * 2).coerceAtLeast(1)
    val outHeight = (raw.height + paddingPx * 2).coerceAtLeast(1)
    val out = Bitmap.createBitmap(outWidth, outHeight, Bitmap.Config.ARGB_8888)
    val canvas = Canvas(out)
    if (bgArgb != null) {
        canvas.drawColor(bgArgb)
    }
    val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    canvas.drawBitmap(raw, paddingPx.toFloat(), paddingPx.toFloat(), paint)
    return out
}

@Composable
fun TexPreviewDialog(
    latex: String,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val exporter = rememberLatexExporter()
    val scope = rememberCoroutineScope()

    var exportTheme by remember { mutableStateOf(TexExportTheme.LIGHT) }
    var isExporting by remember { mutableStateOf(false) }

    val fgColor = when (exportTheme) {
        TexExportTheme.LIGHT -> Color(0xFF111318)
        TexExportTheme.DARK -> Color(0xFFF3F4F8)
        TexExportTheme.TRANSPARENT -> colorScheme.onSurface
    }
    val previewBgColor = when (exportTheme) {
        TexExportTheme.LIGHT -> Color.White
        TexExportTheme.DARK -> Color(0xFF181A1F)
        TexExportTheme.TRANSPARENT -> Color.Transparent
    }
    val exportBgArgb: Int? = when (exportTheme) {
        TexExportTheme.LIGHT -> android.graphics.Color.WHITE
        TexExportTheme.DARK -> 0xFF181A1F.toInt()
        TexExportTheme.TRANSPARENT -> null
    }

    suspend fun renderExportBitmap(): Bitmap? = withContext(Dispatchers.Default) {
        val renderLatex = preprocessLatexForRenderer(latex)
        val result = exporter.export(
            latex = renderLatex,
            config = LatexConfig(
                fontSize = 24.sp,
                theme = LatexTheme.light(
                    color = fgColor,
                    backgroundColor = Color.Transparent
                )
            ),
            exportConfig = ExportConfig(
                scale = 3f,
                format = ImageFormat.PNG,
                transparentBackground = true
            )
        ) ?: return@withContext null
        val raw = result.imageBitmap.asAndroidBitmap()
        return@withContext try {
            buildPaddedExportBitmap(
                raw = raw,
                paddingPx = 48,
                bgArgb = exportBgArgb
            )
        } finally {
            if (!raw.isRecycled) raw.recycle()
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colorScheme.surfaceContainerHigh,
            border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.6f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .widthIn(max = 420.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "LaTeX Render",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colorScheme.onSurface
                    )
                    IconButton(
                        onClick = onDismiss,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Background theme selector
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    TexExportTheme.entries.forEach { option ->
                        val selected = exportTheme == option
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (selected) colorScheme.primaryContainer else colorScheme.surfaceContainerLow,
                            border = BorderStroke(
                                1.dp,
                                if (selected) colorScheme.primary.copy(alpha = 0.6f)
                                else colorScheme.outlineVariant.copy(alpha = 0.5f)
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(8.dp))
                                .clickable { exportTheme = option }
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 8.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = option.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (selected) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }

                // Preview stage (horizontally scrollable for wide equations)
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(min = 96.dp, max = 260.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (exportTheme == TexExportTheme.TRANSPARENT) colorScheme.surfaceContainerLowest else previewBgColor)
                ) {
                    if (exportTheme == TexExportTheme.TRANSPARENT) {
                        val checkA = colorScheme.surfaceContainerLow
                        val checkB = colorScheme.surfaceContainerHigh
                        Canvas(modifier = Modifier.matchParentSize()) {
                            val cell = 12.dp.toPx()
                            val cols = (size.width / cell).toInt() + 1
                            val rows = (size.height / cell).toInt() + 1
                            for (r in 0..rows) {
                                for (c in 0..cols) {
                                    drawRect(
                                        color = if ((r + c) % 2 == 0) checkA else checkB,
                                        topLeft = Offset(c * cell, r * cell),
                                        size = Size(cell, cell)
                                    )
                                }
                            }
                        }
                    }

                    BoxWithConstraints(modifier = Modifier.fillMaxWidth()) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .horizontalScroll(rememberScrollState())
                                .widthIn(min = maxWidth)
                                .padding(horizontal = 20.dp, vertical = 20.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            LatexFormula(
                                latex = latex,
                                color = fgColor,
                                backgroundColor = Color.Transparent,
                                fontSize = 24.sp
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = Color.Transparent,
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
                        modifier = Modifier.matchParentSize()
                    ) {}
                }

                // Raw LaTeX source row
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colorScheme.surfaceContainerLowest,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = latex,
                            fontFamily = FontFamily.Monospace,
                            fontSize = 12.sp,
                            color = colorScheme.onSurfaceVariant,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(
                            onClick = { TexExportHelper.copyLatexText(context, latex) },
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = "Copy LaTeX source",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Action buttons: Share / Save PNG / Copy PNG
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                var bmp: Bitmap? = null
                                try {
                                    bmp = renderExportBitmap()
                                    if (bmp == null) {
                                        Toast.makeText(context, "Could not render LaTeX image", Toast.LENGTH_SHORT).show()
                                    } else {
                                        TexExportHelper.sharePng(context, bmp).onFailure {
                                            Toast.makeText(context, "Could not share image", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                } finally {
                                    if (bmp != null && !bmp.isRecycled) bmp.recycle()
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colorScheme.primary,
                            contentColor = colorScheme.onPrimary
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Share, contentDescription = "Share", modifier = Modifier.size(20.dp))
                    }

                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                var bmp: Bitmap? = null
                                try {
                                    bmp = renderExportBitmap()
                                    if (bmp == null) {
                                        Toast.makeText(context, "Could not render LaTeX image", Toast.LENGTH_SHORT).show()
                                    } else {
                                        TexExportHelper.savePngToGallery(context, bmp)
                                            .onSuccess {
                                                Toast.makeText(context, "Saved to Pictures/Pallas LaTeX", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure {
                                                Toast.makeText(context, "Could not save PNG", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                } finally {
                                    if (bmp != null && !bmp.isRecycled) bmp.recycle()
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Download, contentDescription = "Save PNG", modifier = Modifier.size(20.dp))
                    }

                    OutlinedButton(
                        enabled = !isExporting,
                        onClick = {
                            scope.launch {
                                isExporting = true
                                var bmp: Bitmap? = null
                                try {
                                    bmp = renderExportBitmap()
                                    if (bmp == null) {
                                        Toast.makeText(context, "Could not render LaTeX image", Toast.LENGTH_SHORT).show()
                                    } else {
                                        TexExportHelper.copyPngToClipboard(context, bmp)
                                            .onSuccess {
                                                Toast.makeText(context, "Image copied to clipboard", Toast.LENGTH_SHORT).show()
                                            }
                                            .onFailure {
                                                Toast.makeText(context, "Could not copy image", Toast.LENGTH_SHORT).show()
                                            }
                                    }
                                } finally {
                                    if (bmp != null && !bmp.isRecycled) bmp.recycle()
                                    isExporting = false
                                }
                            }
                        },
                        shape = RoundedCornerShape(10.dp),
                        contentPadding = PaddingValues(vertical = 12.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Copy PNG", modifier = Modifier.size(20.dp))
                    }
                }
            }
        }
    }
}
