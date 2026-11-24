package com.pallas.launcher.platform

import android.app.Activity
import android.content.ClipData
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.text.TextPaint
import androidx.core.content.FileProvider
import androidx.core.content.res.ResourcesCompat
import com.pallas.launcher.R
import com.pallas.launcher.domain.latex.InlineSegment
import com.pallas.launcher.domain.latex.LatexDocBlock
import com.pallas.launcher.domain.latex.LatexDocumentProcessor
import com.pallas.launcher.domain.latex.LatexTextAlign
import com.pallas.launcher.domain.latex.ParsedLatexDocument
import com.pallas.launcher.ui.components.TexExportTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale
import kotlin.math.ceil
import kotlin.math.max
import kotlin.math.min

object LatexStudioExporter {

    private const val SHARE_CACHE_DIR = "pallas_exports"
    private const val MAX_CACHE_FILES = 10

    fun sanitizeFileName(title: String, fallback: String = "pallas_document"): String {
        val cleaned = title
            .replace(Regex("""[^A-Za-z0-9._ -]"""), "_")
            .trim()
            .replace(Regex("""\s+"""), "_")
            .take(48)
        return cleaned.ifBlank { fallback }
    }

    private fun prepareCacheFile(context: Context, fileName: String): File {
        val dir = File(context.cacheDir, SHARE_CACHE_DIR).apply { mkdirs() }
        val existing = dir.listFiles { f -> f.isFile }?.sortedBy { it.lastModified() }.orEmpty()
        if (existing.size >= MAX_CACHE_FILES) {
            existing.take(existing.size - MAX_CACHE_FILES + 1).forEach { it.delete() }
        }
        return File(dir, fileName)
    }

    private fun shareFile(context: Context, file: File, mimeType: String, chooserTitle: String) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val intent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri(file.name, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        val chooser = Intent.createChooser(intent, chooserTitle).apply {
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            if (context !is Activity) addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(chooser)
    }

    private fun saveBytesToDocuments(
        context: Context,
        displayName: String,
        mimeType: String,
        writeContent: (java.io.OutputStream) -> Unit
    ): Uri {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, mimeType)
            put(MediaStore.MediaColumns.RELATIVE_PATH, "${Environment.DIRECTORY_DOCUMENTS}/Pallas")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY)
        val uri = resolver.insert(collection, values)
            ?: error("Could not create document entry")
        try {
            resolver.openOutputStream(uri)?.use { stream ->
                writeContent(stream)
            } ?: error("Could not open document stream")
            val done = ContentValues().apply { put(MediaStore.MediaColumns.IS_PENDING, 0) }
            resolver.update(uri, done, null, null)
            return uri
        } catch (t: Throwable) {
            runCatching { resolver.delete(uri, null, null) }
            throw t
        }
    }

    // ------------------------------------------------------------------------
    // .tex Export
    // ------------------------------------------------------------------------

    suspend fun saveTexToDocuments(context: Context, title: String, rawSource: String): Result<Uri> =
        withContext(Dispatchers.IO) {
            runCatching {
                val tex = LatexDocumentProcessor.buildCompilableTex(rawSource)
                val base = sanitizeFileName(title)
                val fileName = "${base}_${System.currentTimeMillis() % 10000}.tex"
                saveBytesToDocuments(context, fileName, "application/x-tex") { out ->
                    out.write(tex.toByteArray(Charsets.UTF_8))
                }
            }
        }

    suspend fun shareTex(context: Context, title: String, rawSource: String): Result<Unit> =
        runCatching {
            val file = withContext(Dispatchers.IO) {
                val tex = LatexDocumentProcessor.buildCompilableTex(rawSource)
                val base = sanitizeFileName(title)
                prepareCacheFile(context, "${base}.tex").apply {
                    writeText(tex, Charsets.UTF_8)
                }
            }
            withContext(Dispatchers.Main) {
                shareFile(context, file, "text/plain", "Share LaTeX source (.tex)")
            }
        }

    // ------------------------------------------------------------------------
    // Fonts & DPI-Independent Math Calibration
    // ------------------------------------------------------------------------

    private data class FontSet(
        val regular: Typeface,
        val bold: Typeface,
        val italic: Typeface,
        val boldItalic: Typeface,
        val sans: Typeface,
        val sansBold: Typeface,
        val mono: Typeface
    )

    private fun loadFontSet(context: Context): FontSet {
        val reg = runCatching { ResourcesCompat.getFont(context, R.font.garamond_regular) }.getOrNull()
            ?: Typeface.SERIF
        val bold = runCatching { ResourcesCompat.getFont(context, R.font.garamond_bold) }.getOrNull()
            ?: Typeface.create(reg, Typeface.BOLD)
        val italic = Typeface.create(reg, Typeface.ITALIC)
        val boldItalic = Typeface.create(bold, Typeface.ITALIC)
        val sans = runCatching { ResourcesCompat.getFont(context, R.font.inter_variable) }.getOrNull()
            ?: Typeface.SANS_SERIF
        val sansBold = Typeface.create(sans, Typeface.BOLD)
        val mono = Typeface.MONOSPACE
        return FontSet(reg, bold, italic, boldItalic, sans, sansBold, mono)
    }

    private fun paintForRun(
        fonts: FontSet,
        fgArgb: Int,
        baseSizePt: Float,
        run: InlineSegment.TextRun,
        scaleMultiplier: Float = 1f
    ): TextPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = fgArgb
        textSize = baseSizePt * run.sizeScale * scaleMultiplier
        typeface = when {
            run.mono -> fonts.mono
            run.sans && run.bold -> fonts.sansBold
            run.sans && run.italic -> Typeface.create(fonts.sans, Typeface.ITALIC)
            run.sans -> fonts.sans
            run.bold && run.italic -> fonts.boldItalic
            run.bold -> fonts.bold
            run.italic -> fonts.italic
            else -> fonts.regular
        }
        isUnderlineText = run.underline
    }

    private fun measureCapHeight(paint: TextPaint): Float {
        val bounds = Rect()
        paint.getTextBounds("H", 0, 1, bounds)
        val h = bounds.height().toFloat()
        return if (h > 1f) h else paint.textSize * 0.68f
    }

    private data class MathCalibration(
        val capHeightPx: Float,
        val baselinePx: Float,
        val probeHeightPx: Float
    )

    private fun ensureReadableBitmap(bmp: Bitmap): Bitmap =
        if (bmp.config == Bitmap.Config.HARDWARE) {
            bmp.copy(Bitmap.Config.ARGB_8888, false) ?: bmp
        } else {
            bmp
        }

    private suspend fun calibrateMathMetrics(
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?,
        renderSp: Float,
        fgArgb: Int,
        fallbackDensity: Float
    ): MathCalibration {
        val rawProbe = runCatching { renderMathBitmap("\\mathrm{H}", renderSp, fgArgb) }.getOrNull()
        val probe = rawProbe?.let(::ensureReadableBitmap)
        if (probe != null && probe.width > 0 && probe.height > 0 && probe.config != Bitmap.Config.HARDWARE) {
            val w = probe.width
            val h = probe.height
            val pixels = IntArray(w * h)
            probe.getPixels(pixels, 0, w, 0, 0, w, h)
            var inkTop = h
            var inkBottom = -1
            for (y in 0 until h) {
                val rowOff = y * w
                for (x in 0 until w) {
                    if ((pixels[rowOff + x] ushr 24) > 20) {
                        if (y < inkTop) inkTop = y
                        if (y > inkBottom) inkBottom = y
                    }
                }
            }
            if (inkBottom >= inkTop) {
                val cap = (inkBottom - inkTop + 1).toFloat().coerceAtLeast(1f)
                return MathCalibration(
                    capHeightPx = cap,
                    baselinePx = (inkBottom + 1).toFloat(),
                    probeHeightPx = h.toFloat()
                )
            }
        }
        val fallbackCap = (renderSp * 0.7f * fallbackDensity * 3f).coerceAtLeast(8f)
        return MathCalibration(
            capHeightPx = fallbackCap,
            baselinePx = fallbackCap * 1.05f,
            probeHeightPx = fallbackCap * 1.4f
        )
    }

    private fun trimHorizontalTransparent(rawBmp: Bitmap): Bitmap {
        val bmp = ensureReadableBitmap(rawBmp)
        if (bmp.config == Bitmap.Config.HARDWARE) return bmp
        val w = bmp.width
        val h = bmp.height
        if (w <= 2 || h <= 0) return bmp
        val pixels = IntArray(w * h)
        bmp.getPixels(pixels, 0, w, 0, 0, w, h)
        var minX = w
        var maxX = -1
        for (y in 0 until h) {
            val rowOff = y * w
            for (x in 0 until w) {
                if ((pixels[rowOff + x] ushr 24) > 12) {
                    if (x < minX) minX = x
                    if (x > maxX) maxX = x
                }
            }
        }
        if (maxX < minX) return bmp
        val left = (minX - 1).coerceAtLeast(0)
        val right = (maxX + 1).coerceAtMost(w - 1)
        val cropW = right - left + 1
        return if (cropW >= w - 1) bmp else Bitmap.createBitmap(bmp, left, 0, cropW, h)
    }

    private suspend fun collectMathBitmaps(
        blocks: List<LatexDocBlock>,
        displayRenderSp: Float,
        inlineRenderSp: Float,
        fgArgb: Int,
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?,
        displayCache: MutableMap<String, Bitmap>,
        inlineCache: MutableMap<String, Bitmap>
    ) {
        suspend fun collectFromSegments(segs: List<InlineSegment>) {
            for (seg in segs) {
                if (seg is InlineSegment.InlineMath && !inlineCache.containsKey(seg.latex)) {
                    runCatching { renderMathBitmap(seg.latex, inlineRenderSp, fgArgb) }.getOrNull()?.let {
                        inlineCache[seg.latex] = trimHorizontalTransparent(it)
                    }
                }
            }
        }

        for (block in blocks) {
            when (block) {
                is LatexDocBlock.DisplayMath -> {
                    if (!displayCache.containsKey(block.latex)) {
                        runCatching { renderMathBitmap(block.latex, displayRenderSp, fgArgb) }.getOrNull()?.let {
                            displayCache[block.latex] = trimHorizontalTransparent(it)
                        }
                    }
                }
                is LatexDocBlock.Paragraph -> collectFromSegments(block.segments)
                is LatexDocBlock.BulletList -> block.items.forEach { collectFromSegments(it) }
                is LatexDocBlock.Tabular -> block.rows.forEach { row -> row.forEach { collectFromSegments(it) } }
                is LatexDocBlock.Quote -> collectMathBitmaps(block.blocks, displayRenderSp, inlineRenderSp, fgArgb, renderMathBitmap, displayCache, inlineCache)
                is LatexDocBlock.Abstract -> collectMathBitmaps(block.blocks, displayRenderSp, inlineRenderSp, fgArgb, renderMathBitmap, displayCache, inlineCache)
                is LatexDocBlock.TheoremBox -> collectMathBitmaps(block.blocks, displayRenderSp, inlineRenderSp, fgArgb, renderMathBitmap, displayCache, inlineCache)
                is LatexDocBlock.Section,
                is LatexDocBlock.Verbatim,
                LatexDocBlock.HorizontalRule,
                is LatexDocBlock.VerticalSpace,
                LatexDocBlock.PageBreak -> {}
            }
        }
    }

    // ------------------------------------------------------------------------
    // Inline Layout Tokens & Word-Clustered Wrapping + Justification
    // ------------------------------------------------------------------------

    private sealed interface InlineToken {
        data class Word(
            val text: String,
            val paint: TextPaint
        ) : InlineToken

        data class MathBmp(
            val bitmap: Bitmap,
            val drawWidth: Float,
            val drawHeight: Float,
            val baselineOffset: Float
        ) : InlineToken

        object LineBreak : InlineToken
    }

    private data class LayoutLine(
        val tokens: List<InlineToken>,
        val height: Float,
        val baselineOffset: Float,
        val endsWithHardBreak: Boolean
    )

    private fun tokenizeInlineSegments(
        segments: List<InlineSegment>,
        fonts: FontSet,
        fgArgb: Int,
        baseSizePt: Float,
        inlineScaleForSize: (Float) -> Float,
        baselineRatio: Float,
        maxWidth: Float,
        mathLookup: (String) -> Bitmap?
    ): List<InlineToken> {
        val tokens = mutableListOf<InlineToken>()
        val monoFallbackPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgArgb
            typeface = fonts.mono
            textSize = baseSizePt * 0.88f
        }

        fun appendStyledString(text: String, paint: TextPaint) {
            val rawLines = text.split('\n')
            rawLines.forEachIndexed { lineIdx, part ->
                if (lineIdx > 0) tokens.add(InlineToken.LineBreak)
                var idx = 0
                while (idx < part.length) {
                    if (part[idx] == ' ' || part[idx] == '\t') {
                        while (idx < part.length && (part[idx] == ' ' || part[idx] == '\t')) idx++
                        tokens.add(InlineToken.Word(" ", paint))
                    } else {
                        val start = idx
                        while (idx < part.length && part[idx] != ' ' && part[idx] != '\t') idx++
                        tokens.add(InlineToken.Word(part.substring(start, idx), paint))
                    }
                }
            }
        }

        for (seg in segments) {
            when (seg) {
                is InlineSegment.TextRun -> {
                    if (!seg.smallCaps) {
                        val p = paintForRun(fonts, fgArgb, baseSizePt, seg)
                        appendStyledString(seg.text, p)
                    } else {
                        val fullPaint = paintForRun(fonts, fgArgb, baseSizePt, seg, scaleMultiplier = 1f)
                        val smallPaint = paintForRun(fonts, fgArgb, baseSizePt, seg, scaleMultiplier = 0.78f)
                        val buf = StringBuilder()
                        var isLower = false
                        var hasBuf = false
                        for (ch in seg.text) {
                            val chLower = ch.isLowerCase()
                            val outChar = if (chLower) ch.uppercaseChar() else ch
                            if (!hasBuf) {
                                isLower = chLower
                                buf.append(outChar)
                                hasBuf = true
                            } else if (chLower == isLower) {
                                buf.append(outChar)
                            } else {
                                appendStyledString(buf.toString(), if (isLower) smallPaint else fullPaint)
                                buf.clear()
                                isLower = chLower
                                buf.append(outChar)
                            }
                        }
                        if (hasBuf) {
                            appendStyledString(buf.toString(), if (isLower) smallPaint else fullPaint)
                        }
                    }
                }
                is InlineSegment.InlineMath -> {
                    val bmp = mathLookup(seg.latex)
                    if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                        val rawScale = inlineScaleForSize(seg.sizeScale)
                        val maxAllowedScale = maxWidth / bmp.width.toFloat()
                        val s = min(rawScale, maxAllowedScale)
                        val dw = bmp.width * s
                        val dh = bmp.height * s
                        val bOff = dh * baselineRatio.coerceIn(0.60f, 0.90f)
                        tokens.add(InlineToken.MathBmp(bmp, dw, dh, bOff))
                    } else {
                        tokens.add(InlineToken.Word(seg.latex, monoFallbackPaint))
                    }
                }
            }
        }
        return tokens
    }

    private fun layoutInlineSegments(
        segments: List<InlineSegment>,
        fonts: FontSet,
        fgArgb: Int,
        baseSizePt: Float,
        inlineScaleForSize: (Float) -> Float,
        baselineRatio: Float,
        maxWidth: Float,
        firstLineIndent: Float = 0f,
        mathLookup: (String) -> Bitmap?
    ): List<LayoutLine> {
        val tokens = tokenizeInlineSegments(
            segments = segments,
            fonts = fonts,
            fgArgb = fgArgb,
            baseSizePt = baseSizePt,
            inlineScaleForSize = inlineScaleForSize,
            baselineRatio = baselineRatio,
            maxWidth = maxWidth,
            mathLookup = mathLookup
        )
        val bodyPaint = paintForRun(fonts, fgArgb, baseSizePt, InlineSegment.TextRun(""))
        val baseLineH = baseSizePt * 1.42f
        val baseBaseline = baseSizePt * 1.04f

        val lines = mutableListOf<LayoutLine>()
        var curTokens = mutableListOf<InlineToken>()
        var curW = 0f

        fun lineBudget(): Float =
            if (lines.isEmpty()) (maxWidth - firstLineIndent).coerceAtLeast(24f) else maxWidth

        fun isSpaceToken(tok: InlineToken): Boolean = tok is InlineToken.Word && tok.text == " "

        fun flushLine(hardBreak: Boolean) {
            while (curTokens.isNotEmpty() && isSpaceToken(curTokens.last())) {
                curTokens.removeAt(curTokens.lastIndex)
            }
            if (curTokens.isEmpty()) {
                lines.add(LayoutLine(emptyList(), baseLineH, baseBaseline, hardBreak))
                curW = 0f
                return
            }
            var maxAbove = baseBaseline
            var maxBelow = baseLineH - baseBaseline
            for (tok in curTokens) {
                when (tok) {
                    is InlineToken.MathBmp -> {
                        maxAbove = max(maxAbove, tok.baselineOffset + 1f)
                        maxBelow = max(maxBelow, (tok.drawHeight - tok.baselineOffset) + 2f)
                    }
                    is InlineToken.Word -> {
                        val ts = tok.paint.textSize
                        maxAbove = max(maxAbove, ts * 1.04f)
                        maxBelow = max(maxBelow, ts * 0.38f)
                    }
                    is InlineToken.LineBreak -> {}
                }
            }
            lines.add(LayoutLine(curTokens, maxAbove + maxBelow, maxAbove, hardBreak))
            curTokens = mutableListOf()
            curW = 0f
        }

        fun tokenWidth(tok: InlineToken): Float = when (tok) {
            is InlineToken.Word -> tok.paint.measureText(tok.text)
            is InlineToken.MathBmp -> tok.drawWidth
            is InlineToken.LineBreak -> 0f
        }

        // Group adjacent non-space/non-break Word tokens into atomic word clusters so styled sub-words never split across lines
        var i = 0
        while (i < tokens.size) {
            val tok = tokens[i]
            if (tok is InlineToken.LineBreak) {
                flushLine(hardBreak = true)
                i++
                continue
            }
            if (isSpaceToken(tok)) {
                if (curTokens.isNotEmpty()) {
                    val w = tokenWidth(tok)
                    curTokens.add(tok)
                    curW += w
                }
                i++
                continue
            }
            if (tok is InlineToken.MathBmp) {
                if (curW + tok.drawWidth > lineBudget() && curTokens.isNotEmpty()) {
                    flushLine(hardBreak = false)
                }
                curTokens.add(tok)
                curW += tok.drawWidth
                i++
                continue
            }
            // Cluster consecutive non-space Word tokens
            var clusterEnd = i + 1
            var clusterW = tokenWidth(tok)
            while (clusterEnd < tokens.size &&
                tokens[clusterEnd] is InlineToken.Word &&
                !isSpaceToken(tokens[clusterEnd])
            ) {
                clusterW += tokenWidth(tokens[clusterEnd])
                clusterEnd++
            }
            if (curW + clusterW > lineBudget() && curTokens.isNotEmpty()) {
                flushLine(hardBreak = false)
            }
            for (k in i until clusterEnd) {
                curTokens.add(tokens[k])
            }
            curW += clusterW
            i = clusterEnd
        }
        if (curTokens.isNotEmpty()) {
            flushLine(hardBreak = true)
        }
        // Check unused bodyPaint suppression clean
        bodyPaint.alpha = 255
        return lines
    }

    private fun measureLineWidth(line: LayoutLine): Float {
        var x = 0f
        for (tok in line.tokens) {
            x += when (tok) {
                is InlineToken.Word -> tok.paint.measureText(tok.text)
                is InlineToken.MathBmp -> tok.drawWidth
                is InlineToken.LineBreak -> 0f
            }
        }
        return x
    }

    private fun drawAlignedLayoutLine(
        canvas: Canvas,
        line: LayoutLine,
        boxLeft: Float,
        boxWidth: Float,
        topY: Float,
        bmpPaint: Paint,
        align: LatexTextAlign,
        firstLineIndent: Float,
        isFirstLine: Boolean,
        isLastLine: Boolean,
        normalSpaceWidth: Float
    ) {
        val indent = if (isFirstLine) firstLineIndent else 0f
        val lineW = measureLineWidth(line)
        val availW = (boxWidth - indent).coerceAtLeast(1f)

        val startX = when (align) {
            LatexTextAlign.CENTER -> boxLeft + (boxWidth - lineW) * 0.5f
            LatexTextAlign.RIGHT -> boxLeft + (boxWidth - lineW)
            LatexTextAlign.LEFT, LatexTextAlign.JUSTIFY -> boxLeft + indent
        }

        var extraPerSpace = 0f
        if (align == LatexTextAlign.JUSTIFY && !isLastLine && !line.endsWithHardBreak) {
            val spaceCount = line.tokens.count { it is InlineToken.Word && it.text == " " }
            val extra = availW - lineW
            if (spaceCount > 0 && extra > 0.5f && extra <= spaceCount * normalSpaceWidth * 2.2f) {
                extraPerSpace = extra / spaceCount.toFloat()
            }
        }

        var x = startX
        val baseY = topY + line.baselineOffset
        for (tok in line.tokens) {
            when (tok) {
                is InlineToken.Word -> {
                    if (tok.text == " ") {
                        x += tok.paint.measureText(" ") + extraPerSpace
                    } else {
                        canvas.drawText(tok.text, x, baseY, tok.paint)
                        x += tok.paint.measureText(tok.text)
                    }
                }
                is InlineToken.MathBmp -> {
                    val imgTop = baseY - tok.baselineOffset
                    canvas.drawBitmap(
                        tok.bitmap,
                        null,
                        RectF(x, imgTop, x + tok.drawWidth, imgTop + tok.drawHeight),
                        bmpPaint
                    )
                    x += tok.drawWidth
                }
                is InlineToken.LineBreak -> {}
            }
        }
    }

    private fun wrapTextToLines(text: String, paint: TextPaint, maxW: Float): List<String> {
        val lines = mutableListOf<String>()
        for (paragraphLine in text.split('\n')) {
            val words = paragraphLine.split(Regex("""\s+""")).filter { it.isNotEmpty() }
            if (words.isEmpty()) continue
            var current = StringBuilder()
            for (w in words) {
                val candidate = if (current.isEmpty()) w else "$current $w"
                if (paint.measureText(candidate) <= maxW || current.isEmpty()) {
                    current = StringBuilder(candidate)
                } else {
                    lines.add(current.toString())
                    current = StringBuilder(w)
                }
            }
            if (current.isNotEmpty()) lines.add(current.toString())
        }
        return lines
    }

    // ------------------------------------------------------------------------
    // Shared Prepend Helpers for Theorem / Proof Boxes
    // ------------------------------------------------------------------------

    private fun prependLeadInToBlocks(
        blocks: List<LatexDocBlock>,
        leadInSegments: List<InlineSegment>
    ): List<LatexDocBlock> {
        if (blocks.isEmpty()) {
            return listOf(
                LatexDocBlock.Paragraph(
                    segments = leadInSegments,
                    align = LatexTextAlign.JUSTIFY,
                    noIndent = true
                )
            )
        }
        val first = blocks.first()
        return if (first is LatexDocBlock.Paragraph) {
            listOf(first.copy(segments = leadInSegments + first.segments, noIndent = true)) + blocks.drop(1)
        } else {
            listOf(
                LatexDocBlock.Paragraph(
                    segments = leadInSegments,
                    align = LatexTextAlign.LEFT,
                    noIndent = true
                )
            ) + blocks
        }
    }

    // ------------------------------------------------------------------------
    // .pdf Export (Standard LaTeX Article Layout + Preamble Overrides)
    // ------------------------------------------------------------------------

    suspend fun buildPdfBytes(
        context: Context,
        rawSource: String,
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?
    ): ByteArray = withContext(Dispatchers.Default) {
        val parsed = LatexDocumentProcessor.parse(rawSource)
        val fonts = loadFontSet(context)
        val cfg = parsed.pageConfig
        val fallbackDensity = context.resources.displayMetrics.density.coerceAtLeast(1f)

        val pageW = cfg.pageWidthPt
        val pageH = cfg.pageHeightPt
        val marginLeft = cfg.marginLeftPt
        val marginRight = cfg.marginRightPt
        val marginTop = cfg.marginTopPt
        val marginBottom = cfg.marginBottomPt
        val contentW = (pageW - marginLeft - marginRight).coerceAtLeast(120f)
        val maxContentY = pageH - marginBottom
        val baseSize = cfg.baseFontSizePt

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = fonts.regular
            textSize = baseSize
        }
        val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = Color.BLACK
            typeface = fonts.mono
            textSize = baseSize * 0.86f
        }
        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rulePaint = Paint().apply {
            color = Color.BLACK
            strokeWidth = 0.55f
        }
        val verbatimBgPaint = Paint().apply {
            color = 0xFFF6F6F6.toInt()
            style = Paint.Style.FILL
        }

        val inlineRenderSp = 18f
        val displayRenderSp = 24f
        val inlineCal = calibrateMathMetrics(renderMathBitmap, inlineRenderSp, Color.BLACK, fallbackDensity)
        val displayCal = calibrateMathMetrics(renderMathBitmap, displayRenderSp, Color.BLACK, fallbackDensity)
        val bodyCapPt = measureCapHeight(bodyPaint)
        val inlineBaselineRatio = (inlineCal.baselinePx / inlineCal.probeHeightPx).coerceIn(0.60f, 0.90f)

        val displayMathCache = HashMap<String, Bitmap>()
        val inlineMathCache = HashMap<String, Bitmap>()
        collectMathBitmaps(
            blocks = parsed.blocks,
            displayRenderSp = displayRenderSp,
            inlineRenderSp = inlineRenderSp,
            fgArgb = Color.BLACK,
            renderMathBitmap = renderMathBitmap,
            displayCache = displayMathCache,
            inlineCache = inlineMathCache
        )

        val doc = PdfDocument()
        var pageNumber = 1
        var currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
        var canvas = currentPage.canvas
        canvas.drawColor(Color.WHITE)
        var curY = marginTop

        val footerPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = 0xFF333333.toInt()
            typeface = fonts.regular
            textSize = baseSize * 0.91f
            textAlign = Paint.Align.CENTER
        }

        fun finishPage() {
            if (cfg.showPageNumbers) {
                canvas.drawText("$pageNumber", pageW * 0.5f, pageH - marginBottom * 0.45f, footerPaint)
            }
            doc.finishPage(currentPage)
        }

        fun startNewPage() {
            finishPage()
            pageNumber++
            currentPage = doc.startPage(PdfDocument.PageInfo.Builder(pageW, pageH, pageNumber).create())
            canvas = currentPage.canvas
            canvas.drawColor(Color.WHITE)
            curY = marginTop
        }

        fun ensureSpace(neededHeight: Float) {
            if (curY + neededHeight > maxContentY && curY > marginTop + 4f) {
                startNewPage()
            }
        }

        // 1. Standard LaTeX \maketitle Header Block
        val dispDate = parsed.displayDate
        if (parsed.hasHeaderMetadata || dispDate.isNotBlank()) {
            curY += 6f
            if (parsed.title.isNotBlank()) {
                val titleSize = baseSize * 1.65f
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.BLACK
                    typeface = fonts.regular
                    textSize = titleSize
                }
                val titleLines = wrapTextToLines(parsed.title, titlePaint, contentW)
                val lineH = titleSize * 1.25f
                ensureSpace(titleLines.size * lineH + 16f)
                for (line in titleLines) {
                    val tw = titlePaint.measureText(line)
                    canvas.drawText(line, marginLeft + (contentW - tw) * 0.5f, curY + titleSize, titlePaint)
                    curY += lineH
                }
                curY += 10f
            }
            if (parsed.author.isNotBlank()) {
                val authorSize = baseSize * 1.15f
                val authorPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF181818.toInt()
                    typeface = fonts.regular
                    textSize = authorSize
                }
                val authorLines = wrapTextToLines(parsed.author, authorPaint, contentW)
                val lineH = authorSize * 1.32f
                for (line in authorLines) {
                    ensureSpace(lineH)
                    val tw = authorPaint.measureText(line)
                    canvas.drawText(line, marginLeft + (contentW - tw) * 0.5f, curY + authorSize, authorPaint)
                    curY += lineH
                }
                curY += 5f
            }
            if (dispDate.isNotBlank()) {
                val dateSize = baseSize * 1.05f
                val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF282828.toInt()
                    typeface = fonts.regular
                    textSize = dateSize
                }
                val tw = datePaint.measureText(dispDate)
                ensureSpace(dateSize * 1.4f)
                canvas.drawText(dispDate, marginLeft + (contentW - tw) * 0.5f, curY + dateSize, datePaint)
                curY += dateSize * 1.4f
            }
            curY += 18f
        }

        var sec1 = 0
        var sec2 = 0
        var sec3 = 0
        var eqNumber = 0
        val theoremCounters = mutableMapOf<String, Int>()
        val normalSpaceW = bodyPaint.measureText(" ")
        var suppressNextIndent = true

        fun renderBlocks(
            blocks: List<LatexDocBlock>,
            leftX: Float,
            availW: Float,
            blockSizeScale: Float = 1f,
            drawQedAtEnd: Boolean = false
        ) {
            val effBase = baseSize * blockSizeScale
            val effCap = bodyCapPt * blockSizeScale
            val inlineScaleFn: (Float) -> Float = { segScale ->
                (effCap * segScale) / inlineCal.capHeightPx
            }
            val displayTargetScale = (effCap * if (parsed.isPureMath) 1.22f else 1.12f) / displayCal.capHeightPx

            blocks.forEachIndexed { blockIdx, block ->
                val isVeryLastBlock = blockIdx == blocks.lastIndex
                when (block) {
                    is LatexDocBlock.Section -> {
                        val prefix = if (block.starred) "" else when (block.level) {
                            1 -> { sec1++; sec2 = 0; sec3 = 0; "$sec1  " }
                            2 -> { sec2++; sec3 = 0; "$sec1.$sec2  " }
                            else -> { sec3++; "$sec1.$sec2.$sec3  " }
                        }
                        val size = when (block.level) {
                            1 -> baseSize * 1.44f
                            2 -> baseSize * 1.20f
                            else -> baseSize * 1.05f
                        }
                        val secPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            typeface = fonts.bold
                            textSize = size
                        }
                        val lines = wrapTextToLines(prefix + block.title, secPaint, availW)
                        val lineH = size * 1.32f
                        if (curY > marginTop + 2f) {
                            curY += when (block.level) { 1 -> baseSize * 1.15f; 2 -> baseSize * 0.9f; else -> baseSize * 0.7f }
                        }
                        ensureSpace(lines.size * lineH + baseSize * 2.8f)
                        for (line in lines) {
                            canvas.drawText(line, leftX, curY + size, secPaint)
                            curY += lineH
                        }
                        curY += baseSize * 0.45f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.DisplayMath -> {
                        val eqLabel = if (block.numbered) {
                            eqNumber++
                            "($eqNumber)"
                        } else null
                        val labelW = if (eqLabel != null) bodyPaint.measureText(eqLabel) + 12f else 0f
                        val mathMaxW = (availW - labelW).coerceAtLeast(60f)

                        val bmp = displayMathCache[block.latex]
                        if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                            val maxScale = mathMaxW / bmp.width.toFloat()
                            val scale = min(displayTargetScale, maxScale)
                            val drawW = bmp.width * scale
                            val drawH = bmp.height * scale
                            curY += baseSize * 0.5f
                            ensureSpace(drawH + baseSize * 0.9f)
                            val left = leftX + (availW - drawW) * 0.5f
                            val clampedLeft = min(left, leftX + mathMaxW - drawW).coerceAtLeast(leftX)
                            canvas.drawBitmap(bmp, null, RectF(clampedLeft, curY, clampedLeft + drawW, curY + drawH), bmpPaint)
                            if (eqLabel != null) {
                                val lblW = bodyPaint.measureText(eqLabel)
                                val lblY = curY + drawH * 0.5f + bodyPaint.textSize * 0.35f
                                canvas.drawText(eqLabel, leftX + availW - lblW, lblY, bodyPaint)
                            }
                            curY += drawH + baseSize * 0.75f
                        } else {
                            val lines = wrapTextToLines(block.latex, monoPaint, mathMaxW)
                            curY += baseSize * 0.4f
                            for ((lIdx, line) in lines.withIndex()) {
                                ensureSpace(baseSize * 1.4f)
                                canvas.drawText(line, leftX, curY + baseSize, monoPaint)
                                if (eqLabel != null && lIdx == lines.lastIndex) {
                                    val lblW = bodyPaint.measureText(eqLabel)
                                    canvas.drawText(eqLabel, leftX + availW - lblW, curY + baseSize, bodyPaint)
                                }
                                curY += baseSize * 1.4f
                            }
                            curY += baseSize * 0.6f
                        }
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.Paragraph -> {
                        val indent = if (!cfg.useParskip &&
                            !block.noIndent &&
                            !suppressNextIndent &&
                            block.align == LatexTextAlign.JUSTIFY
                        ) effBase * 1.5f else 0f

                        val lines = layoutInlineSegments(
                            segments = block.segments,
                            fonts = fonts,
                            fgArgb = Color.BLACK,
                            baseSizePt = effBase,
                            inlineScaleForSize = inlineScaleFn,
                            baselineRatio = inlineBaselineRatio,
                            maxWidth = availW,
                            firstLineIndent = indent,
                            mathLookup = { inlineMathCache[it] }
                        )
                        var lastLineBaselineY = curY
                        var lastLineEndW = 0f
                        lines.forEachIndexed { lineIdx, line ->
                            ensureSpace(line.height + 2f)
                            drawAlignedLayoutLine(
                                canvas = canvas,
                                line = line,
                                boxLeft = leftX,
                                boxWidth = availW,
                                topY = curY,
                                bmpPaint = bmpPaint,
                                align = block.align,
                                firstLineIndent = indent,
                                isFirstLine = lineIdx == 0,
                                isLastLine = lineIdx == lines.lastIndex,
                                normalSpaceWidth = normalSpaceW
                            )
                            lastLineBaselineY = curY + line.baselineOffset
                            lastLineEndW = measureLineWidth(line) + if (lineIdx == 0) indent else 0f
                            curY += line.height
                        }
                        if (drawQedAtEnd && isVeryLastBlock && lines.isNotEmpty()) {
                            val qedW = bodyPaint.measureText("□")
                            if (lastLineEndW + qedW + 10f <= availW) {
                                canvas.drawText("□", leftX + availW - qedW, lastLineBaselineY, bodyPaint)
                            } else {
                                ensureSpace(baseSize * 1.4f)
                                canvas.drawText("□", leftX + availW - qedW, curY + baseSize, bodyPaint)
                                curY += baseSize * 1.4f
                            }
                        }
                        curY += if (cfg.useParskip) effBase * 0.72f else effBase * 0.25f
                        suppressNextIndent = false
                    }

                    is LatexDocBlock.BulletList -> {
                        val markers = block.items.indices.map { itemIdx ->
                            block.customLabels.getOrNull(itemIdx)?.takeIf { it.isNotBlank() }
                                ?: if (block.ordered) "${itemIdx + 1}." else "•"
                        }
                        val markerPaint = paintForRun(fonts, Color.BLACK, effBase, InlineSegment.TextRun("", bold = false))
                        val customBoldPaint = paintForRun(fonts, Color.BLACK, effBase, InlineSegment.TextRun("", bold = true))
                        val maxMarkerW = markers.indices.maxOfOrNull { idx ->
                            val hasCustom = block.customLabels.getOrNull(idx)?.isNotBlank() == true
                            (if (hasCustom) customBoldPaint else markerPaint).measureText(markers[idx])
                        } ?: 0f
                        val indent = max(effBase * 1.8f, maxMarkerW + effBase * 0.65f)

                        curY += effBase * 0.2f
                        block.items.forEachIndexed { itemIdx, segs ->
                            val marker = markers[itemIdx]
                            val isCustom = block.customLabels.getOrNull(itemIdx)?.isNotBlank() == true
                            val mPaint = if (isCustom) customBoldPaint else markerPaint
                            val lines = layoutInlineSegments(
                                segments = segs,
                                fonts = fonts,
                                fgArgb = Color.BLACK,
                                baseSizePt = effBase,
                                inlineScaleForSize = inlineScaleFn,
                                baselineRatio = inlineBaselineRatio,
                                maxWidth = (availW - indent).coerceAtLeast(36f),
                                firstLineIndent = 0f,
                                mathLookup = { inlineMathCache[it] }
                            )
                            lines.forEachIndexed { lineIdx, line ->
                                ensureSpace(line.height + 2f)
                                if (lineIdx == 0) {
                                    val mw = mPaint.measureText(marker)
                                    val markerX = if (isCustom) leftX else leftX + (indent - mw - effBase * 0.5f).coerceAtLeast(2f)
                                    canvas.drawText(marker, markerX, curY + line.baselineOffset, mPaint)
                                }
                                drawAlignedLayoutLine(
                                    canvas = canvas,
                                    line = line,
                                    boxLeft = leftX + indent,
                                    boxWidth = (availW - indent).coerceAtLeast(36f),
                                    topY = curY,
                                    bmpPaint = bmpPaint,
                                    align = LatexTextAlign.JUSTIFY,
                                    firstLineIndent = 0f,
                                    isFirstLine = lineIdx == 0,
                                    isLastLine = lineIdx == lines.lastIndex,
                                    normalSpaceWidth = normalSpaceW
                                )
                                curY += line.height
                            }
                            curY += effBase * 0.25f
                        }
                        curY += effBase * 0.35f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.Quote -> {
                        val inset = effBase * 2.2f
                        curY += effBase * 0.45f
                        suppressNextIndent = true
                        renderBlocks(
                            blocks = block.blocks,
                            leftX = leftX + inset,
                            availW = (availW - inset * 2f).coerceAtLeast(80f),
                            blockSizeScale = blockSizeScale,
                            drawQedAtEnd = false
                        )
                        curY += effBase * 0.45f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.Abstract -> {
                        val absTitlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            typeface = fonts.bold
                            textSize = effBase * 0.95f
                        }
                        curY += effBase * 0.4f
                        ensureSpace(effBase * 4f)
                        val tw = absTitlePaint.measureText("Abstract")
                        canvas.drawText("Abstract", leftX + (availW - tw) * 0.5f, curY + effBase, absTitlePaint)
                        curY += effBase * 1.55f
                        val inset = effBase * 2.0f
                        suppressNextIndent = true
                        renderBlocks(
                            blocks = block.blocks,
                            leftX = leftX + inset,
                            availW = (availW - inset * 2f).coerceAtLeast(80f),
                            blockSizeScale = blockSizeScale * 0.91f,
                            drawQedAtEnd = false
                        )
                        curY += effBase * 0.8f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.TheoremBox -> {
                        val leadText = if (block.isProof) {
                            buildString {
                                append("Proof")
                                if (!block.title.isNullOrBlank()) append(" (${block.title})")
                                append(". ")
                            }
                        } else {
                            val num = (theoremCounters[block.kind] ?: 0) + 1
                            theoremCounters[block.kind] = num
                            buildString {
                                append("${block.kind} $num")
                                if (!block.title.isNullOrBlank()) append(" (${block.title})")
                                append(". ")
                            }
                        }
                        val leadSeg = InlineSegment.TextRun(
                            text = leadText,
                            bold = !block.isProof,
                            italic = block.isProof,
                            sizeScale = 1f
                        )
                        val mergedBlocks = prependLeadInToBlocks(block.blocks, listOf(leadSeg))
                        curY += effBase * 0.45f
                        suppressNextIndent = true
                        renderBlocks(
                            blocks = mergedBlocks,
                            leftX = leftX,
                            availW = availW,
                            blockSizeScale = blockSizeScale,
                            drawQedAtEnd = block.isProof
                        )
                        curY += effBase * 0.45f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.Tabular -> {
                        val numCols = max(block.colAligns.size, block.rows.maxOfOrNull { it.size } ?: 0)
                        if (numCols > 0) {
                            val cellPadX = effBase * 0.65f
                            val cellPadY = effBase * 0.35f
                            val naturalWidths = FloatArray(numCols)
                            for (row in block.rows) {
                                for (c in 0 until min(numCols, row.size)) {
                                    val cellLines = layoutInlineSegments(
                                        segments = row[c],
                                        fonts = fonts,
                                        fgArgb = Color.BLACK,
                                        baseSizePt = effBase,
                                        inlineScaleForSize = inlineScaleFn,
                                        baselineRatio = inlineBaselineRatio,
                                        maxWidth = availW,
                                        firstLineIndent = 0f,
                                        mathLookup = { inlineMathCache[it] }
                                    )
                                    val maxCellLineW = cellLines.maxOfOrNull { measureLineWidth(it) } ?: 0f
                                    naturalWidths[c] = max(naturalWidths[c], maxCellLineW + cellPadX * 2f)
                                }
                            }
                            val totalNatural = naturalWidths.sum().coerceAtLeast(1f)
                            val colScale = if (totalNatural > availW) availW / totalNatural else 1f
                            val colWidths = FloatArray(numCols) { c -> (naturalWidths[c] * colScale).coerceAtLeast(20f) }
                            val tableW = colWidths.sum()
                            val tableLeft = leftX + ((availW - tableW) * 0.5f).coerceAtLeast(0f)

                            curY += effBase * 0.55f
                            block.rows.forEachIndexed { rIdx, row ->
                                val laidCells = (0 until numCols).map { c ->
                                    val segs = row.getOrElse(c) { emptyList() }
                                    layoutInlineSegments(
                                        segments = segs,
                                        fonts = fonts,
                                        fgArgb = Color.BLACK,
                                        baseSizePt = effBase,
                                        inlineScaleForSize = inlineScaleFn,
                                        baselineRatio = inlineBaselineRatio,
                                        maxWidth = (colWidths[c] - cellPadX * 2f).coerceAtLeast(12f),
                                        firstLineIndent = 0f,
                                        mathLookup = { inlineMathCache[it] }
                                    )
                                }
                                val rowContentH = laidCells.maxOfOrNull { lines -> lines.sumOf { it.height.toDouble() }.toFloat() }
                                    ?.coerceAtLeast(effBase * 1.35f) ?: (effBase * 1.35f)
                                val rowH = rowContentH + cellPadY * 2f
                                ensureSpace(rowH + 4f)
                                if (rIdx in block.hlinesBeforeRow) {
                                    canvas.drawLine(tableLeft, curY, tableLeft + tableW, curY, rulePaint)
                                    curY += 1f
                                }

                                var colX = tableLeft
                                for (c in 0 until numCols) {
                                    val cw = colWidths[c]
                                    val align = block.colAligns.getOrElse(c) { LatexTextAlign.LEFT }
                                    var cellY = curY + cellPadY
                                    val lines = laidCells[c]
                                    lines.forEachIndexed { lIdx, line ->
                                        drawAlignedLayoutLine(
                                            canvas = canvas,
                                            line = line,
                                            boxLeft = colX + cellPadX,
                                            boxWidth = (cw - cellPadX * 2f).coerceAtLeast(8f),
                                            topY = cellY,
                                            bmpPaint = bmpPaint,
                                            align = align,
                                            firstLineIndent = 0f,
                                            isFirstLine = lIdx == 0,
                                            isLastLine = true,
                                            normalSpaceWidth = normalSpaceW
                                        )
                                        cellY += line.height
                                    }
                                    colX += cw
                                }
                                curY += rowH
                            }
                            if (block.rows.size in block.hlinesBeforeRow) {
                                canvas.drawLine(tableLeft, curY, tableLeft + tableW, curY, rulePaint)
                                curY += 1f
                            }
                            curY += effBase * 0.7f
                        }
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.Verbatim -> {
                        val vPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = Color.BLACK
                            typeface = fonts.mono
                            textSize = effBase * 0.85f
                        }
                        val vLineH = effBase * 1.25f
                        val pad = effBase * 0.55f
                        val rawLines = block.text.split('\n')
                        curY += effBase * 0.35f
                        val wrappedLines = rawLines.flatMap { line ->
                            if (line.isEmpty()) listOf("")
                            else {
                                val res = mutableListOf<String>()
                                var rem = line
                                while (rem.isNotEmpty()) {
                                    val fit = vPaint.breakText(rem, true, (availW - pad * 2f).coerceAtLeast(40f), null).coerceAtLeast(1)
                                    res.add(rem.substring(0, fit))
                                    rem = rem.substring(fit)
                                }
                                res
                            }
                        }
                        // Draw contiguous background chunks cleanly even across page breaks
                        var idx = 0
                        while (idx < wrappedLines.size) {
                            val remainingSpace = maxContentY - curY
                            if (remainingSpace < vLineH + pad * 2f && curY > marginTop + 4f) {
                                startNewPage()
                            }
                            val maxLinesOnPage = (((maxContentY - curY) - pad * 2f) / vLineH).toInt().coerceAtLeast(1)
                            val chunkEnd = min(wrappedLines.size, idx + maxLinesOnPage)
                            val chunkCount = chunkEnd - idx
                            val boxH = chunkCount * vLineH + pad * 2f
                            canvas.drawRect(leftX, curY, leftX + availW, curY + boxH, verbatimBgPaint)
                            var ly = curY + pad
                            for (k in idx until chunkEnd) {
                                canvas.drawText(wrappedLines[k], leftX + pad, ly + vPaint.textSize, vPaint)
                                ly += vLineH
                            }
                            curY += boxH
                            idx = chunkEnd
                            if (idx < wrappedLines.size) {
                                startNewPage()
                            }
                        }
                        curY += effBase * 0.55f
                        suppressNextIndent = true
                    }

                    LatexDocBlock.HorizontalRule -> {
                        ensureSpace(12f)
                        curY += 5f
                        canvas.drawLine(leftX, curY, leftX + availW, curY, rulePaint)
                        curY += 7f
                        suppressNextIndent = true
                    }

                    is LatexDocBlock.VerticalSpace -> {
                        curY += block.pt
                        suppressNextIndent = true
                    }

                    LatexDocBlock.PageBreak -> {
                        if (curY > marginTop + 4f) {
                            startNewPage()
                        }
                        suppressNextIndent = true
                    }
                }
            }
        }

        renderBlocks(parsed.blocks, marginLeft, contentW, blockSizeScale = 1f, drawQedAtEnd = false)
        finishPage()

        val baos = java.io.ByteArrayOutputStream()
        doc.writeTo(baos)
        doc.close()
        baos.toByteArray()
    }

    suspend fun savePdfToDocuments(
        context: Context,
        title: String,
        rawSource: String,
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?
    ): Result<Uri> = runCatching {
        val bytes = buildPdfBytes(context, rawSource, renderMathBitmap)
        withContext(Dispatchers.IO) {
            val base = sanitizeFileName(title)
            val fileName = "${base}_${System.currentTimeMillis() % 10000}.pdf"
            saveBytesToDocuments(context, fileName, "application/pdf") { out ->
                out.write(bytes)
            }
        }
    }

    suspend fun sharePdf(
        context: Context,
        title: String,
        rawSource: String,
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?
    ): Result<Unit> = runCatching {
        val bytes = buildPdfBytes(context, rawSource, renderMathBitmap)
        val file = withContext(Dispatchers.IO) {
            val base = sanitizeFileName(title)
            prepareCacheFile(context, "${base}.pdf").apply {
                FileOutputStream(this).use { it.write(bytes) }
            }
        }
        withContext(Dispatchers.Main) {
            shareFile(context, file, "application/pdf", "Share LaTeX PDF")
        }
    }

    // ------------------------------------------------------------------------
    // .png Export (Tightly Cropped — No Full Page)
    // ------------------------------------------------------------------------

    suspend fun renderCroppedPngBitmap(
        context: Context,
        rawSource: String,
        theme: TexExportTheme,
        renderMathBitmap: suspend (latex: String, fontSizeSp: Float, fgArgb: Int) -> Bitmap?
    ): Bitmap? = withContext(Dispatchers.Default) {
        val parsed = LatexDocumentProcessor.parse(rawSource)
        val fgColor = when (theme) {
            TexExportTheme.LIGHT -> 0xFF111318.toInt()
            TexExportTheme.DARK -> 0xFFF3F4F8.toInt()
            TexExportTheme.TRANSPARENT -> 0xFFF3F4F8.toInt()
        }
        val bgColor: Int? = when (theme) {
            TexExportTheme.LIGHT -> Color.WHITE
            TexExportTheme.DARK -> 0xFF181A1F.toInt()
            TexExportTheme.TRANSPARENT -> null
        }

        // Fast path: pure math without title/author/date -> exact tight math crop
        if (!parsed.hasHeaderMetadata && parsed.blocks.size == 1 && parsed.blocks[0] is LatexDocBlock.DisplayMath) {
            val dm = parsed.blocks[0] as LatexDocBlock.DisplayMath
            val bmp = runCatching { renderMathBitmap(dm.latex, 24f, fgColor) }.getOrNull() ?: return@withContext null
            val pad = 48
            val out = Bitmap.createBitmap((bmp.width + pad * 2).coerceAtLeast(1), (bmp.height + pad * 2).coerceAtLeast(1), Bitmap.Config.ARGB_8888)
            val canvas = Canvas(out)
            if (bgColor != null) canvas.drawColor(bgColor)
            canvas.drawBitmap(bmp, pad.toFloat(), pad.toFloat(), Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG))
            return@withContext out
        }

        if (parsed.blocks.isEmpty() && !parsed.hasHeaderMetadata) return@withContext null

        val fonts = loadFontSet(context)
        val fallbackDensity = context.resources.displayMetrics.density.coerceAtLeast(1f)
        val baseSize = 36f
        val ptToPxScale = baseSize / 11f
        val maxContentW = 1040f
        val pad = 48f

        val bodyPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            typeface = fonts.regular
            textSize = baseSize
        }
        val monoPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
            color = fgColor
            typeface = fonts.mono
            textSize = baseSize * 0.86f
        }
        val bmpPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        val rulePaint = Paint().apply {
            color = fgColor
            strokeWidth = 1.6f
        }
        val verbatimBgPaint = Paint().apply {
            color = when (theme) {
                TexExportTheme.LIGHT -> 0xFFF2F3F6.toInt()
                TexExportTheme.DARK -> 0xFF23262E.toInt()
                TexExportTheme.TRANSPARENT -> 0x33FFFFFF
            }
            style = Paint.Style.FILL
        }

        val inlineRenderSp = 18f
        val displayRenderSp = 24f
        val inlineCal = calibrateMathMetrics(renderMathBitmap, inlineRenderSp, fgColor, fallbackDensity)
        val displayCal = calibrateMathMetrics(renderMathBitmap, displayRenderSp, fgColor, fallbackDensity)
        val bodyCapPx = measureCapHeight(bodyPaint)
        val inlineBaselineRatio = (inlineCal.baselinePx / inlineCal.probeHeightPx).coerceIn(0.60f, 0.90f)

        val displayMathCache = HashMap<String, Bitmap>()
        val inlineMathCache = HashMap<String, Bitmap>()
        collectMathBitmaps(
            blocks = parsed.blocks,
            displayRenderSp = displayRenderSp,
            inlineRenderSp = inlineRenderSp,
            fgArgb = fgColor,
            renderMathBitmap = renderMathBitmap,
            displayCache = displayMathCache,
            inlineCache = inlineMathCache
        )

        var maxUsedW = 0f
        var curY = 0f
        val ops = mutableListOf<(Canvas) -> Unit>()

        val dispDate = parsed.displayDate
        if (parsed.hasHeaderMetadata || dispDate.isNotBlank()) {
            if (parsed.title.isNotBlank()) {
                val titlePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.regular; textSize = baseSize * 1.6f }
                val lines = wrapTextToLines(parsed.title, titlePaint, maxContentW)
                for (l in lines) {
                    maxUsedW = max(maxUsedW, titlePaint.measureText(l))
                    val drawY = curY + titlePaint.textSize
                    ops.add { c -> c.drawText(l, pad, pad + drawY, titlePaint) }
                    curY += titlePaint.textSize * 1.25f
                }
                curY += 10f
            }
            if (parsed.author.isNotBlank()) {
                val authPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.regular; textSize = baseSize * 1.05f }
                val lines = wrapTextToLines(parsed.author, authPaint, maxContentW)
                for (l in lines) {
                    maxUsedW = max(maxUsedW, authPaint.measureText(l))
                    val drawY = curY + authPaint.textSize
                    ops.add { c -> c.drawText(l, pad, pad + drawY, authPaint) }
                    curY += authPaint.textSize * 1.3f
                }
            }
            if (dispDate.isNotBlank()) {
                val datePaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.regular; textSize = baseSize * 0.95f }
                maxUsedW = max(maxUsedW, datePaint.measureText(dispDate))
                val drawY = curY + datePaint.textSize
                ops.add { c -> c.drawText(dispDate, pad, pad + drawY, datePaint) }
                curY += datePaint.textSize * 1.35f
            }
            curY += 22f
        }

        var sec1 = 0
        var sec2 = 0
        var sec3 = 0
        var eqNumber = 0
        val theoremCounters = mutableMapOf<String, Int>()
        val normalSpaceW = bodyPaint.measureText(" ")

        fun recordBlocks(
            blocks: List<LatexDocBlock>,
            leftOffset: Float,
            availW: Float,
            blockSizeScale: Float = 1f,
            drawQedAtEnd: Boolean = false
        ) {
            val effBase = baseSize * blockSizeScale
            val effCap = bodyCapPx * blockSizeScale
            val inlineScaleFn: (Float) -> Float = { segScale -> (effCap * segScale) / inlineCal.capHeightPx }
            val displayTargetScale = (effCap * 1.15f) / displayCal.capHeightPx

            blocks.forEachIndexed { bIdx, block ->
                val isLastBlock = bIdx == blocks.lastIndex
                when (block) {
                    is LatexDocBlock.Section -> {
                        val prefix = if (block.starred) "" else when (block.level) {
                            1 -> { sec1++; sec2 = 0; sec3 = 0; "$sec1  " }
                            2 -> { sec2++; sec3 = 0; "$sec1.$sec2  " }
                            else -> { sec3++; "$sec1.$sec2.$sec3  " }
                        }
                        val sz = when (block.level) { 1 -> baseSize * 1.44f; 2 -> baseSize * 1.20f; else -> baseSize * 1.05f }
                        val secPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.bold; textSize = sz }
                        val lines = wrapTextToLines(prefix + block.title, secPaint, availW)
                        if (curY > 0f) curY += 16f
                        for (l in lines) {
                            maxUsedW = max(maxUsedW, leftOffset + secPaint.measureText(l))
                            val drawY = curY + sz
                            ops.add { c -> c.drawText(l, pad + leftOffset, pad + drawY, secPaint) }
                            curY += sz * 1.32f
                        }
                        curY += 10f
                    }
                    is LatexDocBlock.DisplayMath -> {
                        val eqLabel = if (block.numbered) { eqNumber++; "($eqNumber)" } else null
                        val labelW = if (eqLabel != null) bodyPaint.measureText(eqLabel) + 28f else 0f
                        val mathMaxW = (availW - labelW).coerceAtLeast(120f)
                        val bmp = displayMathCache[block.latex]
                        if (bmp != null && bmp.width > 0 && bmp.height > 0) {
                            val scale = min(displayTargetScale, mathMaxW / bmp.width.toFloat())
                            val dw = bmp.width * scale
                            val dh = bmp.height * scale
                            maxUsedW = max(maxUsedW, leftOffset + dw + labelW)
                            val topY = curY
                            ops.add { c ->
                                c.drawBitmap(bmp, null, RectF(pad + leftOffset, pad + topY, pad + leftOffset + dw, pad + topY + dh), bmpPaint)
                                if (eqLabel != null) {
                                    c.drawText(eqLabel, pad + leftOffset + dw + 24f, pad + topY + dh * 0.5f + bodyPaint.textSize * 0.35f, bodyPaint)
                                }
                            }
                            curY += dh + 20f
                        } else {
                            val lines = wrapTextToLines(block.latex, monoPaint, mathMaxW)
                            for ((lIdx, l) in lines.withIndex()) {
                                val lw = monoPaint.measureText(l)
                                maxUsedW = max(maxUsedW, leftOffset + lw + labelW)
                                val dy = curY + monoPaint.textSize
                                ops.add { c ->
                                    c.drawText(l, pad + leftOffset, pad + dy, monoPaint)
                                    if (eqLabel != null && lIdx == lines.lastIndex) {
                                        c.drawText(eqLabel, pad + leftOffset + lw + 24f, pad + dy, bodyPaint)
                                    }
                                }
                                curY += monoPaint.textSize * 1.35f
                            }
                            curY += 12f
                        }
                    }
                    is LatexDocBlock.Paragraph -> {
                        val lines = layoutInlineSegments(
                            segments = block.segments,
                            fonts = fonts,
                            fgArgb = fgColor,
                            baseSizePt = effBase,
                            inlineScaleForSize = inlineScaleFn,
                            baselineRatio = inlineBaselineRatio,
                            maxWidth = availW,
                            firstLineIndent = 0f,
                            mathLookup = { inlineMathCache[it] }
                        )
                        var lastBaseline = curY
                        var lastLineW = 0f
                        lines.forEachIndexed { lineIdx, line ->
                            val lw = measureLineWidth(line)
                            maxUsedW = max(maxUsedW, leftOffset + lw)
                            val ySnap = curY
                            ops.add { c ->
                                drawAlignedLayoutLine(
                                    canvas = c,
                                    line = line,
                                    boxLeft = pad + leftOffset,
                                    boxWidth = if (block.align == LatexTextAlign.CENTER) maxContentW - leftOffset else availW,
                                    topY = pad + ySnap,
                                    bmpPaint = bmpPaint,
                                    align = if (block.align == LatexTextAlign.JUSTIFY) LatexTextAlign.LEFT else block.align,
                                    firstLineIndent = 0f,
                                    isFirstLine = lineIdx == 0,
                                    isLastLine = lineIdx == lines.lastIndex,
                                    normalSpaceWidth = normalSpaceW
                                )
                            }
                            lastBaseline = ySnap + line.baselineOffset
                            lastLineW = lw
                            curY += line.height
                        }
                        if (drawQedAtEnd && isLastBlock && lines.isNotEmpty()) {
                            val qedW = bodyPaint.measureText("□")
                            val qx = leftOffset + lastLineW + 18f
                            maxUsedW = max(maxUsedW, qx + qedW)
                            val bSnap = lastBaseline
                            ops.add { c -> c.drawText("□", pad + qx, pad + bSnap, bodyPaint) }
                        }
                        curY += 16f
                    }
                    is LatexDocBlock.BulletList -> {
                        val markers = block.items.indices.map { itemIdx ->
                            block.customLabels.getOrNull(itemIdx)?.takeIf { it.isNotBlank() }
                                ?: if (block.ordered) "${itemIdx + 1}." else "•"
                        }
                        val markerPaint = paintForRun(fonts, fgColor, effBase, InlineSegment.TextRun(""))
                        val customPaint = paintForRun(fonts, fgColor, effBase, InlineSegment.TextRun("", bold = true))
                        val maxMW = markers.indices.maxOfOrNull { idx ->
                            val custom = block.customLabels.getOrNull(idx)?.isNotBlank() == true
                            (if (custom) customPaint else markerPaint).measureText(markers[idx])
                        } ?: 0f
                        val indent = max(effBase * 1.4f, maxMW + effBase * 0.6f)
                        block.items.forEachIndexed { itemIdx, segs ->
                            val marker = markers[itemIdx]
                            val isCustom = block.customLabels.getOrNull(itemIdx)?.isNotBlank() == true
                            val mPaint = if (isCustom) customPaint else markerPaint
                            val lines = layoutInlineSegments(
                                segments = segs,
                                fonts = fonts,
                                fgArgb = fgColor,
                                baseSizePt = effBase,
                                inlineScaleForSize = inlineScaleFn,
                                baselineRatio = inlineBaselineRatio,
                                maxWidth = (availW - indent).coerceAtLeast(80f),
                                firstLineIndent = 0f,
                                mathLookup = { inlineMathCache[it] }
                            )
                            lines.forEachIndexed { lineIdx, line ->
                                maxUsedW = max(maxUsedW, leftOffset + indent + measureLineWidth(line))
                                val ySnap = curY
                                ops.add { c ->
                                    if (lineIdx == 0) {
                                        c.drawText(marker, pad + leftOffset + 4f, pad + ySnap + line.baselineOffset, mPaint)
                                    }
                                    drawAlignedLayoutLine(
                                        canvas = c,
                                        line = line,
                                        boxLeft = pad + leftOffset + indent,
                                        boxWidth = availW - indent,
                                        topY = pad + ySnap,
                                        bmpPaint = bmpPaint,
                                        align = LatexTextAlign.LEFT,
                                        firstLineIndent = 0f,
                                        isFirstLine = lineIdx == 0,
                                        isLastLine = true,
                                        normalSpaceWidth = normalSpaceW
                                    )
                                }
                                curY += line.height
                            }
                            curY += 10f
                        }
                        curY += 10f
                    }
                    is LatexDocBlock.Quote -> {
                        val inset = effBase * 1.4f
                        curY += 8f
                        recordBlocks(block.blocks, leftOffset + inset, (availW - inset * 2f).coerceAtLeast(160f), blockSizeScale, false)
                        curY += 8f
                    }
                    is LatexDocBlock.Abstract -> {
                        val absPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.bold; textSize = effBase * 0.95f }
                        val tw = absPaint.measureText("Abstract")
                        maxUsedW = max(maxUsedW, leftOffset + tw)
                        val dy = curY + absPaint.textSize
                        ops.add { c -> c.drawText("Abstract", pad + leftOffset, pad + dy, absPaint) }
                        curY += absPaint.textSize * 1.4f
                        val inset = effBase * 1.2f
                        recordBlocks(block.blocks, leftOffset + inset, (availW - inset * 2f).coerceAtLeast(160f), blockSizeScale * 0.91f, false)
                        curY += 12f
                    }
                    is LatexDocBlock.TheoremBox -> {
                        val leadText = if (block.isProof) {
                            buildString {
                                append("Proof")
                                if (!block.title.isNullOrBlank()) append(" (${block.title})")
                                append(". ")
                            }
                        } else {
                            val num = (theoremCounters[block.kind] ?: 0) + 1
                            theoremCounters[block.kind] = num
                            buildString {
                                append("${block.kind} $num")
                                if (!block.title.isNullOrBlank()) append(" (${block.title})")
                                append(". ")
                            }
                        }
                        val leadSeg = InlineSegment.TextRun(leadText, bold = !block.isProof, italic = block.isProof)
                        curY += 8f
                        recordBlocks(prependLeadInToBlocks(block.blocks, listOf(leadSeg)), leftOffset, availW, blockSizeScale, block.isProof)
                        curY += 8f
                    }
                    is LatexDocBlock.Tabular -> {
                        val numCols = max(block.colAligns.size, block.rows.maxOfOrNull { it.size } ?: 0)
                        if (numCols > 0) {
                            val cellPadX = effBase * 0.65f
                            val cellPadY = effBase * 0.35f
                            val natW = FloatArray(numCols)
                            for (row in block.rows) {
                                for (c in 0 until min(numCols, row.size)) {
                                    val ls = layoutInlineSegments(row[c], fonts, fgColor, effBase, inlineScaleFn, inlineBaselineRatio, availW, 0f) { inlineMathCache[it] }
                                    natW[c] = max(natW[c], (ls.maxOfOrNull { measureLineWidth(it) } ?: 0f) + cellPadX * 2f)
                                }
                            }
                            val tot = natW.sum().coerceAtLeast(1f)
                            val colScale = if (tot > availW) availW / tot else 1f
                            val colW = FloatArray(numCols) { c -> (natW[c] * colScale).coerceAtLeast(48f) }
                            val tableW = colW.sum()
                            maxUsedW = max(maxUsedW, leftOffset + tableW)

                            if (0 in block.hlinesBeforeRow) {
                                val ry = curY
                                ops.add { c -> c.drawLine(pad + leftOffset, pad + ry, pad + leftOffset + tableW, pad + ry, rulePaint) }
                                curY += 3f
                            }
                            block.rows.forEachIndexed { rIdx, row ->
                                if (rIdx > 0 && rIdx in block.hlinesBeforeRow) {
                                    val ry = curY
                                    ops.add { c -> c.drawLine(pad + leftOffset, pad + ry, pad + leftOffset + tableW, pad + ry, rulePaint) }
                                    curY += 3f
                                }
                                val laid = (0 until numCols).map { c ->
                                    layoutInlineSegments(row.getOrElse(c) { emptyList() }, fonts, fgColor, effBase, inlineScaleFn, inlineBaselineRatio, (colW[c] - cellPadX * 2f).coerceAtLeast(24f), 0f) { inlineMathCache[it] }
                                }
                                val rowH = (laid.maxOfOrNull { ls -> ls.sumOf { it.height.toDouble() }.toFloat() } ?: effBase * 1.35f) + cellPadY * 2f
                                val rowTopSnap = curY
                                ops.add { canvas ->
                                    var colX = pad + leftOffset
                                    for (c in 0 until numCols) {
                                        val cw = colW[c]
                                        val align = block.colAligns.getOrElse(c) { LatexTextAlign.LEFT }
                                        var cellY = pad + rowTopSnap + cellPadY
                                        laid[c].forEachIndexed { lIdx, line ->
                                            drawAlignedLayoutLine(canvas, line, colX + cellPadX, (cw - cellPadX * 2f).coerceAtLeast(16f), cellY, bmpPaint, align, 0f, lIdx == 0, true, normalSpaceW)
                                            cellY += line.height
                                        }
                                        colX += cw
                                    }
                                }
                                curY += rowH
                            }
                            if (block.rows.size in block.hlinesBeforeRow) {
                                val ry = curY
                                ops.add { c -> c.drawLine(pad + leftOffset, pad + ry, pad + leftOffset + tableW, pad + ry, rulePaint) }
                                curY += 3f
                            }
                            curY += 16f
                        }
                    }
                    is LatexDocBlock.Verbatim -> {
                        val vPaint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply { color = fgColor; typeface = fonts.mono; textSize = effBase * 0.85f }
                        val vLineH = effBase * 1.25f
                        val vPad = 14f
                        val vLines = block.text.split('\n')
                        val boxW = max(120f, (vLines.maxOfOrNull { vPaint.measureText(it) } ?: 0f) + vPad * 2f).coerceAtMost(availW)
                        maxUsedW = max(maxUsedW, leftOffset + boxW)
                        val boxH = vLines.size * vLineH + vPad * 2f
                        val topSnap = curY
                        ops.add { canvas ->
                            canvas.drawRect(pad + leftOffset, pad + topSnap, pad + leftOffset + boxW, pad + topSnap + boxH, verbatimBgPaint)
                            var ly = pad + topSnap + vPad
                            for (vl in vLines) {
                                canvas.drawText(vl, pad + leftOffset + vPad, ly + vPaint.textSize, vPaint)
                                ly += vLineH
                            }
                        }
                        curY += boxH + 16f
                    }
                    LatexDocBlock.HorizontalRule -> {
                        val w = max(maxUsedW - leftOffset, 320f).coerceAtMost(availW)
                        maxUsedW = max(maxUsedW, leftOffset + w)
                        val ry = curY + 8f
                        ops.add { c -> c.drawLine(pad + leftOffset, pad + ry, pad + leftOffset + w, pad + ry, rulePaint) }
                        curY += 18f
                    }
                    is LatexDocBlock.VerticalSpace -> {
                        curY += block.pt * ptToPxScale
                    }
                    LatexDocBlock.PageBreak -> {
                        curY += 18f
                    }
                }
            }
        }

        recordBlocks(parsed.blocks, leftOffset = 0f, availW = maxContentW, blockSizeScale = 1f, drawQedAtEnd = false)

        val finalW = ceil(maxUsedW + pad * 2f).toInt().coerceIn(64, 1600)
        val finalH = ceil(curY + pad * 2f).toInt().coerceIn(64, 6000)
        val out = Bitmap.createBitmap(finalW, finalH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(out)
        if (bgColor != null) canvas.drawColor(bgColor)
        ops.forEach { it(canvas) }
        out
    }
}
