package com.pallas.launcher.domain.latex

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class LatexTextAlign {
    LEFT,
    CENTER,
    RIGHT,
    JUSTIFY
}

data class LatexPageConfig(
    val pageWidthPt: Int = 595,
    val pageHeightPt: Int = 842,
    val baseFontSizePt: Float = 11f,
    val marginLeftPt: Float = 90f,
    val marginRightPt: Float = 90f,
    val marginTopPt: Float = 82f,
    val marginBottomPt: Float = 86f,
    val useParskip: Boolean = false,
    val showPageNumbers: Boolean = true
)

sealed interface InlineSegment {
    data class TextRun(
        val text: String,
        val bold: Boolean = false,
        val italic: Boolean = false,
        val mono: Boolean = false,
        val underline: Boolean = false,
        val smallCaps: Boolean = false,
        val sans: Boolean = false,
        val sizeScale: Float = 1f
    ) : InlineSegment

    data class InlineMath(
        val latex: String,
        val sizeScale: Float = 1f
    ) : InlineSegment
}

sealed interface LatexDocBlock {
    data class Section(
        val level: Int,
        val title: String,
        val starred: Boolean = false
    ) : LatexDocBlock

    data class DisplayMath(
        val latex: String,
        val numbered: Boolean = false
    ) : LatexDocBlock

    data class Paragraph(
        val segments: List<InlineSegment>,
        val align: LatexTextAlign = LatexTextAlign.JUSTIFY,
        val noIndent: Boolean = false
    ) : LatexDocBlock

    data class BulletList(
        val items: List<List<InlineSegment>>,
        val ordered: Boolean,
        val customLabels: List<String?> = emptyList()
    ) : LatexDocBlock

    data class Quote(
        val blocks: List<LatexDocBlock>
    ) : LatexDocBlock

    data class Abstract(
        val blocks: List<LatexDocBlock>
    ) : LatexDocBlock

    data class TheoremBox(
        val kind: String,
        val title: String?,
        val blocks: List<LatexDocBlock>,
        val italicBody: Boolean,
        val isProof: Boolean = false
    ) : LatexDocBlock

    data class Tabular(
        val colAligns: List<LatexTextAlign>,
        val rows: List<List<List<InlineSegment>>>,
        val hlinesBeforeRow: Set<Int>
    ) : LatexDocBlock

    data class Verbatim(
        val text: String
    ) : LatexDocBlock

    object HorizontalRule : LatexDocBlock

    data class VerticalSpace(val pt: Float) : LatexDocBlock

    object PageBreak : LatexDocBlock
}

data class ParsedLatexDocument(
    val documentClass: String,
    val packages: List<String>,
    val extraPreamble: List<String>,
    val title: String,
    val author: String,
    val date: String,
    val bodySource: String,
    val blocks: List<LatexDocBlock>,
    val isPureMath: Boolean,
    val pageConfig: LatexPageConfig = LatexPageConfig(),
    val hasExplicitDate: Boolean = false
) {
    val hasHeaderMetadata: Boolean
        get() = title.isNotBlank() || author.isNotBlank() || date.isNotBlank()

    val displayDate: String
        get() {
            if (date.isBlank()) return ""
            val todayStr = SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date())
            return LatexDocumentProcessor.expandDateText(date, todayStr)
        }
}

object LatexDocumentProcessor {

    const val DEFAULT_DOCUMENT_CLASS = "\\documentclass[11pt,a4paper]{article}"
    val DEFAULT_PACKAGES = listOf("\\usepackage{amsmath}", "\\usepackage{amssymb}")

    private val DISPLAY_ENVS = setOf(
        "align", "align*", "aligned", "equation", "equation*", "gather", "gather*",
        "gathered", "multline", "multline*", "flalign", "flalign*", "eqnarray", "eqnarray*",
        "split", "cases", "pmatrix", "bmatrix", "Bmatrix", "vmatrix", "Vmatrix", "matrix"
    )

    private val STRUCTURAL_BODY_COMMANDS = listOf(
        "\\section", "\\subsection", "\\subsubsection", "\\paragraph",
        "\\begin{itemize}", "\\begin{enumerate}", "\\begin{center}",
        "\\begin{flushleft}", "\\begin{flushright}", "\\begin{quote}",
        "\\begin{quotation}", "\\begin{abstract}", "\\begin{proof}",
        "\\begin{theorem}", "\\begin{lemma}", "\\begin{proposition}",
        "\\begin{corollary}", "\\begin{definition}", "\\begin{remark}",
        "\\begin{example}", "\\begin{tabular}", "\\begin{verbatim}",
        "\\begin{lstlisting}", "\\maketitle", "\\textbf", "\\textit",
        "\\emph", "\\texttt", "\\underline", "\\textsc", "\\textsf",
        "\\textrm", "\\Large", "\\LARGE", "\\large", "\\huge", "\\Huge",
        "\\small", "\\footnotesize", "\\scriptsize", "\\tiny", "\\noindent",
        "\\newpage", "\\clearpage", "\\pagebreak", "\\today", "\\footnote"
    )

    private val KNOWN_MATH_WORDS = setOf(
        "sin", "cos", "tan", "cot", "sec", "csc", "arcsin", "arccos", "arctan",
        "sinh", "cosh", "tanh", "log", "ln", "exp", "lim", "sup", "inf",
        "max", "min", "arg", "det", "dim", "ker", "deg", "gcd", "lcm", "mod",
        "dx", "dy", "dz", "dt", "dr", "df", "du", "dv"
    )

    private val MATH_COMMAND_HINT = Regex(
        """\\(frac|dfrac|tfrac|sqrt|sum|prod|int|iint|iiint|oint|lim|partial|nabla|alpha|beta|gamma|delta|theta|lambda|mu|sigma|omega|pi|infty|cdot|times|binom|vec|hat|bar|tilde|mathcal|mathbb|mathrm|mathbf|operatorname|sin|cos|tan|log|ln|exp|left|right|forall|exists|in|notin|subset|subseteq|cup|cap|le|leq|ge|geq|neq|approx|equiv|pm|mp|div|to|rightarrow|leftarrow|Rightarrow|Leftarrow|implies|iff|mapsto|epsilon|varepsilon|phi|varphi|psi|rho|tau|eta|zeta|xi|chi|kappa|nu|Gamma|Delta|Theta|Lambda|Sigma|Omega|Phi|Psi|Pi|over|dots|cdots|ldots|begin\{(align|aligned|equation|gather|gathered|cases|[pbBvV]?matrix))"""
    )

    private data class MacroDef(
        val name: String,
        val argCount: Int,
        val body: String
    )

    private data class StyleState(
        val bold: Boolean = false,
        val italic: Boolean = false,
        val mono: Boolean = false,
        val underline: Boolean = false,
        val smallCaps: Boolean = false,
        val sans: Boolean = false,
        val sizeScale: Float = 1f
    )

    fun parse(rawSource: String): ParsedLatexDocument {
        val trimmed = rawSource.trim()
        if (trimmed.isEmpty()) {
            return ParsedLatexDocument(
                documentClass = DEFAULT_DOCUMENT_CLASS,
                packages = DEFAULT_PACKAGES,
                extraPreamble = emptyList(),
                title = "",
                author = "",
                date = "",
                bodySource = "",
                blocks = emptyList(),
                isPureMath = false
            )
        }

        val beginDoc = "\\begin{document}"
        val endDoc = "\\end{document}"
        val beginIdx = trimmed.indexOf(beginDoc)
        val rawPreamble: String
        val rawBody: String

        if (beginIdx >= 0) {
            rawPreamble = trimmed.substring(0, beginIdx)
            val afterBegin = trimmed.substring(beginIdx + beginDoc.length)
            val endIdx = afterBegin.lastIndexOf(endDoc)
            rawBody = if (endIdx >= 0) afterBegin.substring(0, endIdx).trim() else afterBegin.trim()
        } else {
            val extracted = extractPreambleDirectives(trimmed)
            rawPreamble = extracted.first
            rawBody = extracted.second
        }

        val preambleMeta = parsePreambleMetadata(rawPreamble)
        val bodyDirectives = extractBodyHeaderDirectives(rawBody, preambleMeta)
        val bodyCleaned = bodyDirectives.cleanedBody.trim()
        val macros = parseMacroDefinitions(preambleMeta.extraPreamble)

        val expandedTitle = cleanTitleOrAuthorText(expandMacros(bodyDirectives.title, macros))
        val expandedAuthor = cleanTitleOrAuthorText(expandMacros(bodyDirectives.author, macros))
        val expandedDate = expandMacros(bodyDirectives.date, macros).trim()
        val expandedBody = expandMacros(bodyCleaned, macros)

        val pureMath = beginIdx < 0 && looksLikePureMathBody(expandedBody)
        val blocks = if (pureMath && expandedBody.isNotEmpty()) {
            val stripped = stripOuterMathDelimiters(expandedBody)
            val singleEnv = extractSingleDisplayEnv(stripped)
            if (singleEnv != null) {
                val (rendered, isNum) = prepareEnvForRenderer(singleEnv.first, singleEnv.second)
                listOf(LatexDocBlock.DisplayMath(rendered, numbered = isNum))
            } else {
                listOf(LatexDocBlock.DisplayMath(cleanMathForRenderer(stripped), numbered = false))
            }
        } else {
            parseBodyBlocks(expandedBody, bodyDirectives.pageConfig.baseFontSizePt)
        }

        return ParsedLatexDocument(
            documentClass = preambleMeta.documentClass,
            packages = preambleMeta.packages,
            extraPreamble = preambleMeta.extraPreamble,
            title = expandedTitle,
            author = expandedAuthor,
            date = expandedDate,
            bodySource = bodyCleaned,
            blocks = blocks,
            isPureMath = pureMath,
            pageConfig = bodyDirectives.pageConfig,
            hasExplicitDate = bodyDirectives.hasExplicitDate
        )
    }

    fun buildCompilableTex(rawSource: String): String {
        val parsed = parse(rawSource)
        return buildString {
            appendLine(parsed.documentClass)
            parsed.packages.forEach { appendLine(it) }
            parsed.extraPreamble.forEach { appendLine(it) }
            appendLine("\\title{${parsed.title}}")
            appendLine("\\author{${parsed.author}}")
            appendLine("\\date{${parsed.date}}")
            appendLine()
            appendLine("\\begin{document}")
            if (parsed.hasHeaderMetadata && !parsed.bodySource.contains("\\maketitle")) {
                appendLine("\\maketitle")
                appendLine()
            }
            if (parsed.bodySource.isNotEmpty()) {
                if (parsed.isPureMath) {
                    val inner = stripOuterMathDelimiters(parsed.bodySource)
                    val alreadyMathEnv = inner.startsWith("\\begin{align") ||
                        inner.startsWith("\\begin{equation") ||
                        inner.startsWith("\\begin{gather")
                    if (alreadyMathEnv) {
                        appendLine(inner)
                    } else {
                        appendLine("\\[")
                        appendLine(inner)
                        appendLine("\\]")
                    }
                } else {
                    appendLine(parsed.bodySource)
                }
            }
            append("\\end{document}")
        }
    }

    fun deriveAutoTitle(rawSource: String, createdAtMs: Long): String {
        val parsed = parse(rawSource)
        if (parsed.title.isNotBlank()) {
            return parsed.title.take(48)
        }
        val firstLine = parsed.bodySource
            .lineSequence()
            .map { it.trim() }
            .firstOrNull { line ->
                line.isNotEmpty() &&
                    !line.startsWith("%") &&
                    line != "\\maketitle" &&
                    line != "\\[" &&
                    line != "\\]" &&
                    line != "$$"
            }
        if (!firstLine.isNullOrBlank()) {
            val cleaned = firstLine
                .removePrefix("\\section*{").removePrefix("\\section{")
                .removePrefix("\\subsection*{").removePrefix("\\subsection{")
                .removePrefix("\\subsubsection*{").removePrefix("\\subsubsection{")
                .removePrefix("\\paragraph{").removePrefix("\\begin{center}")
                .removePrefix("\\[").removeSuffix("\\]")
                .removePrefix("$$").removeSuffix("$$")
                .removePrefix("$").removeSuffix("$")
                .trim()
                .trimEnd('}')
                .trim()
            if (cleaned.isNotEmpty()) {
                return if (cleaned.length > 40) cleaned.take(39) + "…" else cleaned
            }
        }
        val stamp = SimpleDateFormat("MMM d, HH:mm", Locale.getDefault()).format(Date(createdAtMs))
        return "Document • $stamp"
    }

    internal fun expandDateText(rawDate: String, todayStr: String): String {
        val substituted = rawDate.replace(Regex("""\\today(?![A-Za-z])"""), todayStr).trim()
        return cleanTitleOrAuthorText(substituted)
    }

    private fun extractSingleDisplayEnv(stripped: String): Pair<String, String>? {
        if (!stripped.startsWith("\\begin{")) return null
        val closeBrace = stripped.indexOf('}', 7)
        if (closeBrace <= 7) return null
        val envName = stripped.substring(7, closeBrace).trim()
        if (envName !in DISPLAY_ENVS) return null
        val endTag = "\\end{$envName}"
        val endIdx = findMatchingEnvEnd(stripped, envName, closeBrace + 1)
        if (endIdx < 0 || endIdx + endTag.length != stripped.length) return null
        val inner = stripped.substring(closeBrace + 1, endIdx).trim()
        return envName to inner
    }

    private fun cleanTitleOrAuthorText(raw: String): String {
        if (raw.isEmpty()) return ""
        val expanded = raw
            .replace(Regex("""\\and(?![A-Za-z])"""), ", ")
        val segs = parseInlineSegmentsInternal(expanded, StyleState(), depth = 0)
        return segs.joinToString("") { seg ->
            when (seg) {
                is InlineSegment.TextRun -> seg.text
                is InlineSegment.InlineMath -> seg.latex
            }
        }.trim()
    }

    private data class PreambleMetadata(
        val documentClass: String,
        val packages: List<String>,
        val extraPreamble: List<String>,
        val title: String,
        val author: String,
        val date: String,
        val hasExplicitDate: Boolean,
        val pageConfig: LatexPageConfig
    )

    private data class BodyHeaderResult(
        val cleanedBody: String,
        val title: String,
        val author: String,
        val date: String,
        val hasExplicitDate: Boolean,
        val pageConfig: LatexPageConfig
    ) {
        val hasHeaderMetadata: Boolean
            get() = title.isNotBlank() || author.isNotBlank() || date.isNotBlank()
    }

    private fun extractPreambleDirectives(source: String): Pair<String, String> {
        val preamble = StringBuilder()
        val body = StringBuilder()
        val len = source.length
        var pos = 0

        fun isCommandAt(idx: Int, cmd: String): Boolean {
            if (!source.startsWith(cmd, idx)) return false
            val next = source.getOrNull(idx + cmd.length)
            return next == null || !next.isLetter()
        }

        while (pos < len) {
            // Preserve blank-line separation ONLY in body; consume preamble commands cleanly even across lines
            val atLineStart = pos == 0 || source[pos - 1] == '\n'
            if (atLineStart) {
                var ws = pos
                while (ws < len && (source[ws] == ' ' || source[ws] == '\t')) ws++
                when {
                    isCommandAt(ws, "\\documentclass") ||
                        isCommandAt(ws, "\\usepackage") -> {
                        val end = consumeCommandWithOptionalAndBrace(source, ws + if (source.startsWith("\\documentclass", ws)) 14 else 11)
                        preamble.append(source.substring(ws, end)).append('\n')
                        pos = skipHorizontalAndOneNewline(source, end)
                        continue
                    }
                    isCommandAt(ws, "\\geometry") -> {
                        val (content, end) = readBraceArg(source, ws + 9)
                        if (content != null) {
                            preamble.append(source.substring(ws, end)).append('\n')
                            pos = skipHorizontalAndOneNewline(source, end)
                            continue
                        }
                    }
                    isCommandAt(ws, "\\title") -> {
                        val (content, end) = readBraceArg(source, ws + 6)
                        if (content != null) {
                            preamble.append(source.substring(ws, end)).append('\n')
                            pos = skipHorizontalAndOneNewline(source, end)
                            continue
                        }
                    }
                    isCommandAt(ws, "\\author") -> {
                        val (content, end) = readBraceArg(source, ws + 7)
                        if (content != null) {
                            preamble.append(source.substring(ws, end)).append('\n')
                            pos = skipHorizontalAndOneNewline(source, end)
                            continue
                        }
                    }
                    isCommandAt(ws, "\\date") -> {
                        val (content, end) = readBraceArg(source, ws + 5)
                        if (content != null) {
                            preamble.append(source.substring(ws, end)).append('\n')
                            pos = skipHorizontalAndOneNewline(source, end)
                            continue
                        }
                    }
                    isCommandAt(ws, "\\pagestyle") -> {
                        val (content, end) = readBraceArg(source, ws + 10)
                        if (content != null) {
                            preamble.append(source.substring(ws, end)).append('\n')
                            pos = skipHorizontalAndOneNewline(source, end)
                            continue
                        }
                    }
                    isCommandAt(ws, "\\newcommand") || isCommandAt(ws, "\\renewcommand") -> {
                        val cmdLen = if (source.startsWith("\\newcommand", ws)) 11 else 13
                        val end = consumeMacroDefinition(source, ws + cmdLen)
                        preamble.append(source.substring(ws, end)).append('\n')
                        pos = skipHorizontalAndOneNewline(source, end)
                        continue
                    }
                    isCommandAt(ws, "\\DeclareMathOperator") -> {
                        val end = consumeMacroDefinition(source, ws + 20)
                        preamble.append(source.substring(ws, end)).append('\n')
                        pos = skipHorizontalAndOneNewline(source, end)
                        continue
                    }
                }
            }
            val nl = source.indexOf('\n', pos)
            if (nl >= 0) {
                body.append(source, pos, nl + 1)
                pos = nl + 1
            } else {
                body.append(source, pos, len)
                pos = len
            }
        }
        return preamble.toString() to body.toString().trim()
    }

    private fun skipHorizontalAndOneNewline(text: String, pos: Int): Int {
        var i = pos
        while (i < text.length && (text[i] == ' ' || text[i] == '\t' || text[i] == '\r')) i++
        if (i < text.length && text[i] == '\n') i++
        return i
    }

    private fun consumeMacroDefinition(text: String, afterCmd: Int): Int {
        var i = skipWhitespace(text, afterCmd)
        if (i < text.length && text[i] == '*') i = skipWhitespace(text, i + 1)
        if (i < text.length && text[i] == '{') {
            val close = findMatchingBrace(text, i)
            i = if (close >= 0) skipWhitespace(text, close + 1) else i + 1
        } else if (i < text.length && text[i] == '\\') {
            i++
            while (i < text.length && text[i].isLetter()) i++
            i = skipWhitespace(text, i)
        }
        if (i < text.length && text[i] == '[') {
            val close = text.indexOf(']', i + 1)
            i = if (close >= 0) skipWhitespace(text, close + 1) else i
        }
        if (i < text.length && text[i] == '[') {
            val close = text.indexOf(']', i + 1)
            i = if (close >= 0) skipWhitespace(text, close + 1) else i
        }
        if (i < text.length && text[i] == '{') {
            val close = findMatchingBrace(text, i)
            if (close >= 0) return close + 1
        }
        val nl = text.indexOf('\n', i)
        return if (nl >= 0) nl else text.length
    }

    private fun parsePreambleMetadata(preamble: String): PreambleMetadata {
        var docClass = DEFAULT_DOCUMENT_CLASS
        val packages = DEFAULT_PACKAGES.toMutableList()
        val extras = mutableListOf<String>()
        var title = ""
        var author = ""
        var date = ""
        var hasExplicitDate = false
        var pageConfig = LatexPageConfig()

        var pos = 0
        while (pos < preamble.length) {
            if (preamble[pos] == '%') {
                val nl = preamble.indexOf('\n', pos)
                pos = if (nl >= 0) nl + 1 else preamble.length
                continue
            }
            if (preamble[pos] != '\\') {
                pos++
                continue
            }
            when {
                isCmdAt(preamble, pos, "\\documentclass") -> {
                    val end = consumeCommandWithOptionalAndBrace(preamble, pos + "\\documentclass".length)
                    docClass = preamble.substring(pos, end).trim()
                    val opts = extractOptionalBracketContent(preamble, pos + "\\documentclass".length)
                    if (opts != null) {
                        pageConfig = applyDocClassOptions(pageConfig, opts)
                    }
                    pos = end
                }
                isCmdAt(preamble, pos, "\\usepackage") -> {
                    val afterCmd = pos + "\\usepackage".length
                    val optAndArgEnd = consumeCommandWithOptionalAndBrace(preamble, afterCmd)
                    val fullPkg = preamble.substring(pos, optAndArgEnd).trim()
                    val opts = extractOptionalBracketContent(preamble, afterCmd)
                    val pkgArg = extractFirstBraceContent(preamble, afterCmd)?.trim().orEmpty()
                    val pkgList = pkgArg.split(",").map { it.trim() }.filter { it.isNotEmpty() }

                    if (pkgList.any { it.equals("geometry", ignoreCase = true) }) {
                        pageConfig = applyGeometryOptions(pageConfig, opts, applyDefaultOneInch = true)
                    }
                    if (pkgList.any { it.equals("parskip", ignoreCase = true) }) {
                        pageConfig = pageConfig.copy(useParskip = true)
                    }

                    if (pkgList.none { it.equals("tikz", ignoreCase = true) }) {
                        if (pkgList.all { it == "amsmath" || it == "amssymb" } && !fullPkg.contains("[")) {
                            // already in default
                        } else if (fullPkg !in packages) {
                            if ("amsmath" in pkgList) packages.removeAll { it == "\\usepackage{amsmath}" }
                            if ("amssymb" in pkgList) packages.removeAll { it == "\\usepackage{amssymb}" }
                            packages.add(fullPkg)
                        }
                    }
                    pos = optAndArgEnd
                }
                isCmdAt(preamble, pos, "\\geometry") -> {
                    val (content, nextPos) = readBraceArg(preamble, pos + "\\geometry".length)
                    if (content != null) {
                        pageConfig = applyGeometryOptions(pageConfig, content, applyDefaultOneInch = false)
                        extras.add("\\geometry{${content.trim()}}")
                    }
                    pos = nextPos
                }
                isCmdAt(preamble, pos, "\\pagestyle") -> {
                    val (content, nextPos) = readBraceArg(preamble, pos + "\\pagestyle".length)
                    if (content != null) {
                        if (content.trim().equals("empty", ignoreCase = true)) {
                            pageConfig = pageConfig.copy(showPageNumbers = false)
                        }
                        extras.add("\\pagestyle{${content.trim()}}")
                    }
                    pos = nextPos
                }
                isCmdAt(preamble, pos, "\\title") -> {
                    val (content, nextPos) = readBraceArg(preamble, pos + "\\title".length)
                    if (content != null) title = content.trim()
                    pos = nextPos
                }
                isCmdAt(preamble, pos, "\\author") -> {
                    val (content, nextPos) = readBraceArg(preamble, pos + "\\author".length)
                    if (content != null) author = content.trim()
                    pos = nextPos
                }
                isCmdAt(preamble, pos, "\\date") -> {
                    val (content, nextPos) = readBraceArg(preamble, pos + "\\date".length)
                    if (content != null) {
                        date = content.trim()
                        hasExplicitDate = true
                    }
                    pos = nextPos
                }
                isCmdAt(preamble, pos, "\\newcommand") ||
                    isCmdAt(preamble, pos, "\\renewcommand") ||
                    isCmdAt(preamble, pos, "\\DeclareMathOperator") -> {
                    val cmdLen = when {
                        preamble.startsWith("\\newcommand", pos) -> 11
                        preamble.startsWith("\\renewcommand", pos) -> 13
                        else -> 20
                    }
                    val end = consumeMacroDefinition(preamble, pos + cmdLen)
                    val line = preamble.substring(pos, end).trim()
                    if (line.isNotEmpty()) extras.add(line)
                    pos = end
                }
                else -> pos++
            }
        }

        return PreambleMetadata(
            documentClass = docClass,
            packages = packages,
            extraPreamble = extras,
            title = title,
            author = author,
            date = date,
            hasExplicitDate = hasExplicitDate,
            pageConfig = pageConfig
        )
    }

    private fun extractBodyHeaderDirectives(body: String, preamble: PreambleMetadata): BodyHeaderResult {
        var title = preamble.title
        var author = preamble.author
        var date = preamble.date
        var hasExplicitDate = preamble.hasExplicitDate
        var pageConfig = preamble.pageConfig

        val out = StringBuilder(body.length)
        var i = 0
        val len = body.length
        while (i < len) {
            // Skip inside verbatim/lstlisting untouched
            if (body.startsWith("\\begin{verbatim}", i) || body.startsWith("\\begin{lstlisting}", i)) {
                val env = if (body.startsWith("\\begin{verbatim}", i)) "verbatim" else "lstlisting"
                val endTag = "\\end{$env}"
                val endIdx = body.indexOf(endTag, i)
                val stop = if (endIdx >= 0) endIdx + endTag.length else len
                out.append(body, i, stop)
                i = stop
                continue
            }
            if (body[i] == '%' && (i == 0 || body[i - 1] != '\\')) {
                val nl = body.indexOf('\n', i)
                val stop = if (nl >= 0) nl + 1 else len
                out.append(body, i, stop)
                i = stop
                continue
            }
            when {
                isCmdAt(body, i, "\\title") -> {
                    val (content, nextPos) = readBraceArg(body, i + 6)
                    if (content != null) {
                        title = content.trim()
                        i = nextPos
                        continue
                    }
                }
                isCmdAt(body, i, "\\author") -> {
                    val (content, nextPos) = readBraceArg(body, i + 7)
                    if (content != null) {
                        author = content.trim()
                        i = nextPos
                        continue
                    }
                }
                isCmdAt(body, i, "\\date") -> {
                    val (content, nextPos) = readBraceArg(body, i + 5)
                    if (content != null) {
                        date = content.trim()
                        hasExplicitDate = true
                        i = nextPos
                        continue
                    }
                }
                isCmdAt(body, i, "\\geometry") -> {
                    val (content, nextPos) = readBraceArg(body, i + 9)
                    if (content != null) {
                        pageConfig = applyGeometryOptions(pageConfig, content, applyDefaultOneInch = false)
                        i = nextPos
                        continue
                    }
                }
                isCmdAt(body, i, "\\pagestyle") || isCmdAt(body, i, "\\thispagestyle") -> {
                    val cmdLen = if (body.startsWith("\\thispagestyle", i)) 14 else 10
                    val (content, nextPos) = readBraceArg(body, i + cmdLen)
                    if (content != null) {
                        if (content.trim().equals("empty", ignoreCase = true)) {
                            pageConfig = pageConfig.copy(showPageNumbers = false)
                        }
                        i = nextPos
                        continue
                    }
                }
            }
            out.append(body[i])
            i++
        }
        return BodyHeaderResult(out.toString(), title, author, date, hasExplicitDate, pageConfig)
    }

    private fun applyDocClassOptions(cfg: LatexPageConfig, optionsStr: String): LatexPageConfig {
        var width = cfg.pageWidthPt
        var height = cfg.pageHeightPt
        var baseSize = cfg.baseFontSizePt
        var landscape = false

        optionsStr.split(",").map { it.trim().lowercase(Locale.US) }.forEach { opt ->
            when (opt) {
                "10pt" -> baseSize = 10f
                "11pt" -> baseSize = 11f
                "12pt" -> baseSize = 12f
                "a4paper" -> { width = 595; height = 842 }
                "letterpaper" -> { width = 612; height = 792 }
                "a5paper" -> { width = 420; height = 595 }
                "b5paper" -> { width = 499; height = 709 }
                "legalpaper" -> { width = 612; height = 1008 }
                "landscape" -> landscape = true
            }
        }
        if (landscape && height > width) {
            val tmp = width
            width = height
            height = tmp
        }
        return cfg.copy(
            pageWidthPt = width,
            pageHeightPt = height,
            baseFontSizePt = baseSize
        )
    }

    private fun applyGeometryOptions(
        cfg: LatexPageConfig,
        optionsStr: String?,
        applyDefaultOneInch: Boolean
    ): LatexPageConfig {
        var res = if (applyDefaultOneInch) {
            cfg.copy(marginLeftPt = 72f, marginRightPt = 72f, marginTopPt = 72f, marginBottomPt = 72f)
        } else {
            cfg
        }
        if (optionsStr.isNullOrBlank()) return res

        var landscape = false
        optionsStr.split(",").map { it.trim() }.filter { it.isNotEmpty() }.forEach { token ->
            val eqIdx = token.indexOf('=')
            if (eqIdx < 0) {
                when (token.lowercase(Locale.US)) {
                    "a4paper" -> res = res.copy(pageWidthPt = 595, pageHeightPt = 842)
                    "letterpaper" -> res = res.copy(pageWidthPt = 612, pageHeightPt = 792)
                    "a5paper" -> res = res.copy(pageWidthPt = 420, pageHeightPt = 595)
                    "b5paper" -> res = res.copy(pageWidthPt = 499, pageHeightPt = 709)
                    "legalpaper" -> res = res.copy(pageWidthPt = 612, pageHeightPt = 1008)
                    "landscape" -> landscape = true
                }
            } else {
                val key = token.substring(0, eqIdx).trim().lowercase(Locale.US)
                val valStr = token.substring(eqIdx + 1).trim()
                val pt = parseLengthPt(valStr, res.baseFontSizePt)?.coerceIn(18f, 240f)
                if (pt != null) {
                    res = when (key) {
                        "margin" -> res.copy(marginLeftPt = pt, marginRightPt = pt, marginTopPt = pt, marginBottomPt = pt)
                        "hmargin" -> res.copy(marginLeftPt = pt, marginRightPt = pt)
                        "vmargin" -> res.copy(marginTopPt = pt, marginBottomPt = pt)
                        "left", "lmargin" -> res.copy(marginLeftPt = pt)
                        "right", "rmargin" -> res.copy(marginRightPt = pt)
                        "top", "tmargin" -> res.copy(marginTopPt = pt)
                        "bottom", "bmargin" -> res.copy(marginBottomPt = pt)
                        else -> res
                    }
                }
            }
        }
        if (landscape && res.pageHeightPt > res.pageWidthPt) {
            res = res.copy(pageWidthPt = res.pageHeightPt, pageHeightPt = res.pageWidthPt)
        }
        return res
    }

    internal fun parseLengthPt(raw: String, baseEmPt: Float = 11f): Float? {
        val m = Regex("""^\s*([+-]?(?:\d+(?:\.\d+)?|\.\d+))\s*(pt|bp|in|cm|mm|em)\s*$""", RegexOption.IGNORE_CASE)
            .matchEntire(raw) ?: return null
        val value = m.groupValues[1].toFloatOrNull() ?: return null
        return when (m.groupValues[2].lowercase(Locale.US)) {
            "pt", "bp" -> value
            "in" -> value * 72f
            "cm" -> value * 28.346457f
            "mm" -> value * 2.8346457f
            "em" -> value * baseEmPt
            else -> null
        }
    }

    private fun parseMacroDefinitions(preambleExtras: List<String>): List<MacroDef> {
        val out = mutableListOf<MacroDef>()
        for (line in preambleExtras) {
            when {
                line.startsWith("\\newcommand") || line.startsWith("\\renewcommand") -> {
                    val afterCmd = skipWhitespace(line, if (line.startsWith("\\newcommand")) 11 else 13)
                    var i = if (afterCmd < line.length && line[afterCmd] == '*') skipWhitespace(line, afterCmd + 1) else afterCmd
                    val cmdName = if (i < line.length && line[i] == '{') {
                        val close = findMatchingBrace(line, i)
                        if (close > i) {
                            val name = line.substring(i + 1, close).trim()
                            i = skipWhitespace(line, close + 1)
                            name
                        } else null
                    } else if (i < line.length && line[i] == '\\') {
                        val start = i
                        i++
                        while (i < line.length && line[i].isLetter()) i++
                        val name = line.substring(start, i)
                        i = skipWhitespace(line, i)
                        name
                    } else null

                    if (cmdName.isNullOrBlank() || !cmdName.startsWith("\\")) continue

                    var argCount = 0
                    if (i < line.length && line[i] == '[') {
                        val close = line.indexOf(']', i + 1)
                        if (close > i) {
                            argCount = line.substring(i + 1, close).trim().toIntOrNull() ?: 0
                            i = skipWhitespace(line, close + 1)
                        }
                    }
                    // Skip optional default-arg bracket if any
                    if (i < line.length && line[i] == '[') {
                        val close = line.indexOf(']', i + 1)
                        if (close >= 0) i = skipWhitespace(line, close + 1)
                    }
                    if (i < line.length && line[i] == '{') {
                        val close = findMatchingBrace(line, i)
                        if (close > i && argCount <= 1) {
                            out.add(MacroDef(cmdName, argCount, line.substring(i + 1, close)))
                        }
                    }
                }
                line.startsWith("\\DeclareMathOperator") -> {
                    var i = skipWhitespace(line, 20)
                    val starred = i < line.length && line[i] == '*'
                    if (starred) i = skipWhitespace(line, i + 1)
                    val (cmdRaw, nextPos) = readBraceArg(line, i)
                    val (bodyRaw, _) = readBraceArg(line, nextPos)
                    val cmdName = cmdRaw?.trim().orEmpty()
                    val opText = bodyRaw?.trim().orEmpty()
                    if (cmdName.startsWith("\\") && opText.isNotEmpty()) {
                        val prefix = if (starred) "\\operatorname*" else "\\operatorname"
                        out.add(MacroDef(cmdName, 0, "$prefix{$opText}"))
                    }
                }
            }
        }
        return out
    }

    private fun expandMacros(text: String, macros: List<MacroDef>): String {
        if (macros.isEmpty() || text.isEmpty()) return text
        var current = text
        for (pass in 0 until 8) {
            var changed = false
            val sb = StringBuilder(current.length)
            var i = 0
            val len = current.length
            while (i < len) {
                if (current[i] == '\\') {
                    val matched = macros.firstOrNull { isCmdAt(current, i, it.name) }
                    if (matched != null) {
                        val after = i + matched.name.length
                        if (matched.argCount == 0) {
                            sb.append(matched.body)
                            // Consume optional space immediately following 0-arg macro ONLY if body ends with letter & not in text? Actually append single space if body doesn't break next letter
                            if (after < len && current[after] == ' ' && matched.body.lastOrNull()?.isLetter() == false) {
                                // leave space intact
                            }
                            i = after
                            changed = true
                            continue
                        } else if (matched.argCount == 1) {
                            val (arg, nextPos) = readBraceArg(current, after)
                            if (arg != null) {
                                sb.append(matched.body.replace("#1", arg))
                                i = nextPos
                                changed = true
                                continue
                            }
                        }
                    }
                }
                sb.append(current[i])
                i++
            }
            current = sb.toString()
            if (!changed) break
        }
        return current
    }

    private fun isCmdAt(text: String, pos: Int, cmd: String): Boolean {
        if (!text.startsWith(cmd, pos)) return false
        val next = text.getOrNull(pos + cmd.length) ?: return true
        return !next.isLetter()
    }

    private fun consumeCommandWithOptionalAndBrace(text: String, startPos: Int): Int {
        var i = skipWhitespace(text, startPos)
        if (i < text.length && text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            i = if (closeBracket >= 0) skipWhitespace(text, closeBracket + 1) else i
        }
        if (i < text.length && text[i] == '{') {
            val closeBrace = findMatchingBrace(text, i)
            if (closeBrace >= 0) return closeBrace + 1
        }
        val nl = text.indexOf('\n', i)
        return if (nl >= 0) nl else text.length
    }

    private fun extractOptionalBracketContent(text: String, startPos: Int): String? {
        val i = skipWhitespace(text, startPos)
        if (i < text.length && text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket > i) return text.substring(i + 1, closeBracket)
        }
        return null
    }

    private fun extractFirstBraceContent(text: String, startPos: Int): String? {
        var i = skipWhitespace(text, startPos)
        if (i < text.length && text[i] == '[') {
            val closeBracket = text.indexOf(']', i + 1)
            if (closeBracket >= 0) i = skipWhitespace(text, closeBracket + 1)
        }
        if (i < text.length && text[i] == '{') {
            val close = findMatchingBrace(text, i)
            if (close > i) return text.substring(i + 1, close)
        }
        return null
    }

    private fun readBraceArg(text: String, startPos: Int): Pair<String?, Int> {
        val i = skipWhitespace(text, startPos)
        if (i < text.length && text[i] == '{') {
            val close = findMatchingBrace(text, i)
            if (close >= i) {
                return text.substring(i + 1, close) to (close + 1)
            }
        }
        return null to (startPos + 1).coerceAtMost(text.length)
    }

    private fun readOptionalBracketArg(text: String, startPos: Int): Pair<String?, Int> {
        val i = skipWhitespace(text, startPos)
        if (i < text.length && text[i] == '[') {
            var depth = 1
            var j = i + 1
            while (j < text.length && depth > 0) {
                if (text[j] == '\\' && j + 1 < text.length) {
                    j += 2
                    continue
                }
                if (text[j] == '[') depth++
                else if (text[j] == ']') {
                    depth--
                    if (depth == 0) return text.substring(i + 1, j) to (j + 1)
                }
                j++
            }
        }
        return null to startPos
    }

    private fun skipWhitespace(text: String, start: Int): Int {
        var i = start
        while (i < text.length && text[i].isWhitespace()) i++
        return i
    }

    private fun findMatchingBrace(text: String, openIdx: Int): Int {
        var depth = 0
        var i = openIdx
        while (i < text.length) {
            val ch = text[i]
            if (ch == '\\' && i + 1 < text.length) {
                i += 2
                continue
            }
            if (ch == '{') depth++
            else if (ch == '}') {
                depth--
                if (depth == 0) return i
            }
            i++
        }
        return -1
    }

    private fun looksLikePureMathBody(body: String): Boolean {
        if (body.isEmpty()) return false
        if (STRUCTURAL_BODY_COMMANDS.any { cmd ->
                val idx = body.indexOf(cmd)
                idx >= 0 && (body.getOrNull(idx + cmd.length)?.isLetter() != true)
            }
        ) {
            return false
        }
        val stripped = stripOuterMathDelimiters(body)
        if (stripped != body) {
            return !stripped.contains("$$") && !stripped.contains("\\[")
        }
        if (body.contains("$") || body.contains("\\(")) {
            return false
        }
        if (MATH_COMMAND_HINT.containsMatchIn(body)) {
            return !body.contains("\n\n") || body.lines().all {
                it.isBlank() || MATH_COMMAND_HINT.containsMatchIn(it) || it.contains("=")
            }
        }
        if (body.lines().size <= 2 && (body.contains("^") || body.contains("_") || body.contains("="))) {
            // Reject ordinary English sentences that happen to contain '=' (e.g. "Here x = 5 is solved.")
            val words = Regex("""[A-Za-z]+""").findAll(body).map { it.value.lowercase(Locale.US) }.toList()
            val nonMathProseWords = words.count { it.length >= 3 && it !in KNOWN_MATH_WORDS }
            return nonMathProseWords <= 1 && body.count { it == ' ' } <= 12
        }
        return false
    }

    private fun stripOuterMathDelimiters(raw: String): String {
        var s = raw.trim()
        while (true) {
            when {
                s.startsWith("$$") && s.endsWith("$$") && s.length >= 4 && !s.substring(2, s.length - 2).contains("$$") ->
                    s = s.substring(2, s.length - 2).trim()
                s.startsWith("\\[") && s.endsWith("\\]") && s.length >= 4 && !s.substring(2, s.length - 2).contains("\\]") ->
                    s = s.substring(2, s.length - 2).trim()
                s.startsWith("\\(") && s.endsWith("\\)") && s.length >= 4 && !s.substring(2, s.length - 2).contains("\\)") ->
                    s = s.substring(2, s.length - 2).trim()
                s.startsWith("$") && !s.startsWith("$$") && s.endsWith("$") && !s.endsWith("$$") && s.length >= 2 &&
                    !s.substring(1, s.length - 1).contains("$") ->
                    s = s.substring(1, s.length - 1).trim()
                else -> return s
            }
        }
    }

    internal fun cleanMathForRenderer(latex: String): String {
        return latex
            .replace(Regex("""\\label\s*\{[^}]*\}"""), "")
            .replace(Regex("""\\tag\s*\*?\s*\{[^}]*\}"""), "")
            .replace(Regex("""\\(nonumber|notag)(?![A-Za-z])"""), "")
            .replace(Regex("""\\displaystyle(?![A-Za-z])"""), "")
            .trim()
    }

    private fun prepareEnvForRenderer(envName: String, rawInner: String): Pair<String, Boolean> {
        val starred = envName.endsWith("*")
        val base = envName.removeSuffix("*")
        val hasSuppression = rawInner.contains("\\nonumber") || rawInner.contains("\\notag")
        val numbered = !starred && !hasSuppression && base in setOf("equation", "align", "gather", "multline", "flalign", "eqnarray")
        val cleanInner = cleanMathForRenderer(rawInner)
        val rendered = when (base) {
            "equation" -> cleanInner
            "align", "flalign" -> "\\begin{aligned}\n$cleanInner\n\\end{aligned}"
            "gather", "multline" -> "\\begin{gathered}\n$cleanInner\n\\end{gathered}"
            "eqnarray" -> {
                val fixed = cleanInner.replace(Regex("""\s*&\s*=\s*&\s*"""), " &= ")
                "\\begin{aligned}\n$fixed\n\\end{aligned}"
            }
            else -> "\\begin{$envName}\n$cleanInner\n\\end{$envName}"
        }
        return rendered to numbered
    }

    fun parseBodyBlocks(
        body: String,
        baseEmPt: Float = 11f
    ): List<LatexDocBlock> =
        parseBodyBlocksInternal(body, baseEmPt, LatexTextAlign.JUSTIFY, StyleState())

    private fun parseBodyBlocksInternal(
        body: String,
        baseEmPt: Float = 11f,
        defaultAlign: LatexTextAlign = LatexTextAlign.JUSTIFY,
        initialState: StyleState? = null
    ): List<LatexDocBlock> {
        if (body.isBlank()) return emptyList()
        val blocks = mutableListOf<LatexDocBlock>()
        val paraBuf = StringBuilder()
        var pendingNoIndent = false
        val inlineBaseState = initialState ?: StyleState()

        fun flushParagraph() {
            val raw = paraBuf.toString().trim()
            paraBuf.clear()
            if (raw.isEmpty()) return
            raw.split(Regex("""\n\s*\n+""")).forEachIndexed { chunkIdx, chunk ->
                val collapsed = chunk.replace(Regex("""\s*\n\s*"""), " ").trim()
                if (collapsed.isNotEmpty() && collapsed != "\\maketitle") {
                    if (looksLikePureMathBody(collapsed)) {
                        val stripped = stripOuterMathDelimiters(collapsed)
                        val singleEnv = extractSingleDisplayEnv(stripped)
                        if (singleEnv != null) {
                            val (rendered, isNum) = prepareEnvForRenderer(singleEnv.first, singleEnv.second)
                            blocks.add(LatexDocBlock.DisplayMath(rendered, numbered = isNum))
                        } else {
                            val math = cleanMathForRenderer(stripped)
                            if (math.isNotEmpty()) {
                                blocks.add(LatexDocBlock.DisplayMath(math, numbered = false))
                            }
                        }
                    } else {
                        val segs = parseInlineSegmentsInternal(collapsed, inlineBaseState, depth = 0)
                        if (segs.isNotEmpty()) {
                            blocks.add(
                                LatexDocBlock.Paragraph(
                                    segments = segs,
                                    align = defaultAlign,
                                    noIndent = if (chunkIdx == 0) pendingNoIndent else false
                                )
                            )
                        }
                    }
                }
            }
            pendingNoIndent = false
        }

        var i = 0
        val len = body.length
        while (i < len) {
            // Line comments %
            if (body[i] == '%' && (i == 0 || body[i - 1] != '\\')) {
                val nl = body.indexOf('\n', i)
                i = if (nl >= 0) nl + 1 else len
                continue
            }

            // Skip \maketitle
            if (isCmdAt(body, i, "\\maketitle")) {
                i += "\\maketitle".length
                continue
            }

            // \noindent / \indent
            if (isCmdAt(body, i, "\\noindent")) {
                flushParagraph()
                pendingNoIndent = true
                i += "\\noindent".length
                while (i < len && body[i] == ' ') i++
                continue
            }
            if (isCmdAt(body, i, "\\par")) {
                flushParagraph()
                i += "\\par".length
                while (i < len && body[i] == ' ') i++
                continue
            }

            // Page breaks
            if (isCmdAt(body, i, "\\newpage") || isCmdAt(body, i, "\\clearpage") || isCmdAt(body, i, "\\pagebreak")) {
                flushParagraph()
                blocks.add(LatexDocBlock.PageBreak)
                val cmdLen = when {
                    body.startsWith("\\newpage", i) -> 8
                    body.startsWith("\\clearpage", i) -> 10
                    else -> 10
                }
                i += cmdLen
                continue
            }

            // Horizontal rules
            if (isCmdAt(body, i, "\\hrule") || isCmdAt(body, i, "\\hrulefill")) {
                flushParagraph()
                blocks.add(LatexDocBlock.HorizontalRule)
                i += if (body.startsWith("\\hrulefill", i)) 10 else 6
                continue
            }
            if (isCmdAt(body, i, "\\rule")) {
                var p = i + 5
                val (_, p1) = readOptionalBracketArg(body, p)
                val (_, p2) = readBraceArg(body, p1)
                val (_, p3) = readBraceArg(body, p2)
                if (p3 > p2) {
                    flushParagraph()
                    blocks.add(LatexDocBlock.HorizontalRule)
                    i = p3
                    continue
                }
            }

            // Vertical spaces
            when {
                isCmdAt(body, i, "\\smallskip") -> {
                    flushParagraph(); blocks.add(LatexDocBlock.VerticalSpace(4f)); i += 10; continue
                }
                isCmdAt(body, i, "\\medskip") -> {
                    flushParagraph(); blocks.add(LatexDocBlock.VerticalSpace(8f)); i += 8; continue
                }
                isCmdAt(body, i, "\\bigskip") -> {
                    flushParagraph(); blocks.add(LatexDocBlock.VerticalSpace(14f)); i += 8; continue
                }
                isCmdAt(body, i, "\\vspace") -> {
                    var p = i + 7
                    if (p < len && body[p] == '*') p++
                    val (arg, nextPos) = readBraceArg(body, p)
                    if (arg != null) {
                        val pt = (parseLengthPt(arg, baseEmPt) ?: 10f).coerceIn(0f, 180f)
                        flushParagraph()
                        if (pt > 0f) blocks.add(LatexDocBlock.VerticalSpace(pt))
                        i = nextPos
                        continue
                    }
                }
            }

            // Display math: $$...$$
            if (body.startsWith("$$", i) && (i == 0 || body[i - 1] != '\\')) {
                val close = body.indexOf("$$", i + 2)
                if (close > i + 2) {
                    flushParagraph()
                    val math = cleanMathForRenderer(body.substring(i + 2, close))
                    if (math.isNotEmpty()) blocks.add(LatexDocBlock.DisplayMath(math, numbered = false))
                    i = close + 2
                    continue
                }
            }

            // Display math: \[...\]
            if (body.startsWith("\\[", i)) {
                val close = body.indexOf("\\]", i + 2)
                if (close > i + 2) {
                    flushParagraph()
                    val math = cleanMathForRenderer(body.substring(i + 2, close))
                    if (math.isNotEmpty()) blocks.add(LatexDocBlock.DisplayMath(math, numbered = false))
                    i = close + 2
                    continue
                }
            }

            // Environments: \begin{...}
            if (body.startsWith("\\begin{", i)) {
                val closeBrace = body.indexOf('}', i + "\\begin{".length)
                if (closeBrace > i) {
                    val envName = body.substring(i + "\\begin{".length, closeBrace).trim()
                    val endTag = "\\end{$envName}"
                    val endIdx = findMatchingEnvEnd(body, envName, closeBrace + 1)
                    if (endIdx > closeBrace) {
                        val inner = body.substring(closeBrace + 1, endIdx).trim()
                        val nextAfterEnv = endIdx + endTag.length

                        when {
                            envName in DISPLAY_ENVS -> {
                                flushParagraph()
                                val (mathLatex, isNum) = prepareEnvForRenderer(envName, inner)
                                if (mathLatex.isNotBlank()) {
                                    blocks.add(LatexDocBlock.DisplayMath(mathLatex, numbered = isNum))
                                }
                                i = nextAfterEnv
                                continue
                            }
                            envName == "itemize" || envName == "enumerate" -> {
                                flushParagraph()
                                val listBlock = parseListEnvironment(inner, ordered = envName == "enumerate", state = inlineBaseState)
                                if (listBlock != null) blocks.add(listBlock)
                                i = nextAfterEnv
                                continue
                            }
                            envName == "center" -> {
                                flushParagraph()
                                blocks.addAll(parseBodyBlocksInternal(inner, baseEmPt, LatexTextAlign.CENTER, inlineBaseState))
                                i = nextAfterEnv
                                continue
                            }
                            envName == "flushleft" -> {
                                flushParagraph()
                                blocks.addAll(parseBodyBlocksInternal(inner, baseEmPt, LatexTextAlign.LEFT, inlineBaseState))
                                i = nextAfterEnv
                                continue
                            }
                            envName == "flushright" -> {
                                flushParagraph()
                                blocks.addAll(parseBodyBlocksInternal(inner, baseEmPt, LatexTextAlign.RIGHT, inlineBaseState))
                                i = nextAfterEnv
                                continue
                            }
                            envName == "quote" || envName == "quotation" -> {
                                flushParagraph()
                                val innerBlocks = parseBodyBlocksInternal(inner, baseEmPt, defaultAlign, inlineBaseState)
                                if (innerBlocks.isNotEmpty()) blocks.add(LatexDocBlock.Quote(innerBlocks))
                                i = nextAfterEnv
                                continue
                            }
                            envName == "abstract" -> {
                                flushParagraph()
                                val innerBlocks = parseBodyBlocksInternal(inner, baseEmPt, defaultAlign, inlineBaseState)
                                if (innerBlocks.isNotEmpty()) blocks.add(LatexDocBlock.Abstract(innerBlocks))
                                i = nextAfterEnv
                                continue
                            }
                            envName == "proof" -> {
                                flushParagraph()
                                val (optTitle, contentStart) = readOptionalBracketArg(inner, 0)
                                val actualInner = inner.substring(contentStart).trim()
                                val innerBlocks = parseBodyBlocksInternal(actualInner, baseEmPt, defaultAlign, inlineBaseState)
                                blocks.add(
                                    LatexDocBlock.TheoremBox(
                                        kind = "Proof",
                                        title = optTitle?.trim()?.takeIf { it.isNotEmpty() },
                                        blocks = innerBlocks,
                                        italicBody = false,
                                        isProof = true
                                    )
                                )
                                i = nextAfterEnv
                                continue
                            }
                            envName in setOf("theorem", "lemma", "proposition", "corollary", "definition", "remark", "example") -> {
                                flushParagraph()
                                val (optTitle, contentStart) = readOptionalBracketArg(inner, 0)
                                val actualInner = inner.substring(contentStart).trim()
                                val italicBody = envName in setOf("theorem", "lemma", "proposition", "corollary")
                                val thmState = if (italicBody) inlineBaseState.copy(italic = true) else inlineBaseState
                                val innerBlocks = parseBodyBlocksInternal(actualInner, baseEmPt, defaultAlign, thmState)
                                val kindTitle = envName.replaceFirstChar { it.uppercase(Locale.US) }
                                blocks.add(
                                    LatexDocBlock.TheoremBox(
                                        kind = kindTitle,
                                        title = optTitle?.trim()?.takeIf { it.isNotEmpty() },
                                        blocks = innerBlocks,
                                        italicBody = italicBody,
                                        isProof = false
                                    )
                                )
                                i = nextAfterEnv
                                continue
                            }
                            envName == "tabular" -> {
                                flushParagraph()
                                val (colSpec, afterSpec) = readBraceArg(body, closeBrace + 1)
                                if (colSpec != null) {
                                    val tabInner = body.substring(afterSpec, endIdx)
                                    parseTabular(colSpec, tabInner, inlineBaseState)?.let { blocks.add(it) }
                                    i = nextAfterEnv
                                    continue
                                }
                            }
                            envName == "verbatim" || envName == "lstlisting" -> {
                                flushParagraph()
                                val rawVerb = body.substring(closeBrace + 1, endIdx).trim('\n', '\r')
                                if (rawVerb.isNotEmpty()) blocks.add(LatexDocBlock.Verbatim(rawVerb))
                                i = nextAfterEnv
                                continue
                            }
                        }
                    }
                }
            }

            // Section headings + \paragraph
            val secMatch = when {
                body.startsWith("\\subsubsection*", i) -> Triple(3, "\\subsubsection*", true)
                body.startsWith("\\subsubsection", i) -> Triple(3, "\\subsubsection", false)
                body.startsWith("\\subsection*", i) -> Triple(2, "\\subsection*", true)
                body.startsWith("\\subsection", i) -> Triple(2, "\\subsection", false)
                body.startsWith("\\section*", i) -> Triple(1, "\\section*", true)
                body.startsWith("\\section", i) -> Triple(1, "\\section", false)
                else -> null
            }
            if (secMatch != null) {
                val (level, cmd, starred) = secMatch
                val afterCmd = i + cmd.length
                val nextChar = body.getOrNull(afterCmd)
                if (nextChar == null || nextChar == '{' || nextChar == '[' || nextChar.isWhitespace()) {
                    val (_, afterOpt) = readOptionalBracketArg(body, afterCmd)
                    val (secTitle, nextPos) = readBraceArg(body, afterOpt)
                    if (secTitle != null) {
                        flushParagraph()
                        blocks.add(LatexDocBlock.Section(level, cleanInlineFormatting(secTitle), starred = starred))
                        i = nextPos
                        continue
                    }
                }
            }
            if (isCmdAt(body, i, "\\paragraph")) {
                val (paraTitle, nextPos) = readBraceArg(body, i + 10)
                if (paraTitle != null) {
                    flushParagraph()
                    paraBuf.append("\\textbf{").append(paraTitle).append("} ")
                    pendingNoIndent = true
                    i = nextPos
                    continue
                }
            }

            paraBuf.append(body[i])
            i++
        }

        flushParagraph()
        return blocks
    }

    private fun findMatchingEnvEnd(body: String, envName: String, startFrom: Int): Int {
        val openTag = "\\begin{$envName}"
        val closeTag = "\\end{$envName}"
        var depth = 1
        var i = startFrom
        while (i < body.length) {
            when {
                body.startsWith(openTag, i) -> {
                    depth++
                    i += openTag.length
                }
                body.startsWith(closeTag, i) -> {
                    depth--
                    if (depth == 0) return i
                    i += closeTag.length
                }
                else -> i++
            }
        }
        return -1
    }

    private fun parseListEnvironment(inner: String, ordered: Boolean, state: StyleState): LatexDocBlock.BulletList? {
        val itemMatches = Regex("""\\item(?![A-Za-z])""").findAll(inner).toList()
        if (itemMatches.isEmpty()) return null
        val items = mutableListOf<List<InlineSegment>>()
        val labels = mutableListOf<String?>()
        for (idx in itemMatches.indices) {
            val startAfterCmd = itemMatches[idx].range.last + 1
            val endPos = if (idx + 1 < itemMatches.size) itemMatches[idx + 1].range.first else inner.length
            val (customLabel, afterOpt) = readOptionalBracketArg(inner.substring(0, endPos), startAfterCmd)
            val content = inner.substring(afterOpt.coerceAtMost(endPos), endPos)
                .replace(Regex("""\s*\n\s*"""), " ")
                .trim()
            if (content.isNotEmpty() || customLabel != null) {
                items.add(parseInlineSegmentsInternal(content, state, 0))
                labels.add(customLabel?.trim()?.takeIf { it.isNotEmpty() }?.let(::cleanInlineFormatting))
            }
        }
        if (items.isEmpty()) return null
        return LatexDocBlock.BulletList(items, ordered = ordered, customLabels = labels)
    }

    private fun parseTabular(colSpec: String, rawInner: String, state: StyleState): LatexDocBlock.Tabular? {
        val aligns = mutableListOf<LatexTextAlign>()
        for (ch in colSpec) {
            when (ch) {
                'l' -> aligns.add(LatexTextAlign.LEFT)
                'c' -> aligns.add(LatexTextAlign.CENTER)
                'r' -> aligns.add(LatexTextAlign.RIGHT)
                'p' -> aligns.add(LatexTextAlign.LEFT)
            }
        }
        if (aligns.isEmpty()) return null

        val cleanedInner = rawInner.trim()
        val rawParts = splitTabularRows(cleanedInner)
        val rows = mutableListOf<List<List<InlineSegment>>>()
        val hlinesBeforeRow = mutableSetOf<Int>()

        for (rawPart in rawParts) {
            var part = rawPart.trim()
            while (true) {
                val trimmed = part.trimStart()
                val ruleMatch = Regex("""^\\(hline|toprule|midrule|bottomrule|cline\s*\{[^}]*\})(?![A-Za-z])\s*""").find(trimmed)
                if (ruleMatch != null) {
                    hlinesBeforeRow.add(rows.size)
                    part = trimmed.substring(ruleMatch.value.length).trimStart()
                } else {
                    break
                }
            }
            if (part.isEmpty()) continue
            val cellsRaw = splitUnescapedAmpersands(part)
            val cellsParsed = cellsRaw.map { cell ->
                parseInlineSegmentsInternal(cell.replace(Regex("""\s*\n\s*"""), " ").trim(), state, 0)
            }
            rows.add(cellsParsed)
        }
        if (rows.isEmpty()) return null
        return LatexDocBlock.Tabular(aligns, rows, hlinesBeforeRow)
    }

    private fun splitTabularRows(text: String): List<String> {
        val parts = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var braceDepth = 0
        var inMath = false
        while (i < text.length) {
            val ch = text[i]
            if (ch == '$' && (i == 0 || text[i - 1] != '\\')) {
                inMath = !inMath
                sb.append(ch)
                i++
                continue
            }
            if (!inMath) {
                if (ch == '{') braceDepth++
                else if (ch == '}' && braceDepth > 0) braceDepth--
                if (braceDepth == 0 && text.startsWith("\\\\", i)) {
                    parts.add(sb.toString())
                    sb.clear()
                    i += 2
                    val (_, nextPos) = readOptionalBracketArg(text, i)
                    i = nextPos
                    continue
                }
            }
            sb.append(ch)
            i++
        }
        if (sb.isNotBlank()) parts.add(sb.toString())
        return parts
    }

    private fun splitUnescapedAmpersands(row: String): List<String> {
        val cells = mutableListOf<String>()
        val sb = StringBuilder()
        var i = 0
        var braceDepth = 0
        while (i < row.length) {
            val ch = row[i]
            if (ch == '\\' && i + 1 < row.length) {
                sb.append(ch).append(row[i + 1])
                i += 2
                continue
            }
            if (ch == '{') braceDepth++
            else if (ch == '}' && braceDepth > 0) braceDepth--
            if (ch == '&' && braceDepth == 0) {
                cells.add(sb.toString())
                sb.clear()
            } else {
                sb.append(ch)
            }
            i++
        }
        cells.add(sb.toString())
        return cells
    }

    fun parseInlineSegments(text: String): List<InlineSegment> =
        parseInlineSegmentsInternal(text, StyleState(), depth = 0)

    private fun parseInlineSegmentsInternal(
        text: String,
        initialState: StyleState,
        depth: Int
    ): List<InlineSegment> {
        if (depth > 16) return listOf(InlineSegment.TextRun(cleanEscapedChars(text)))
        val out = mutableListOf<InlineSegment>()
        val textBuf = StringBuilder()
        var state = initialState

        fun flushText() {
            if (textBuf.isNotEmpty()) {
                val cleaned = cleanEscapedChars(textBuf.toString())
                textBuf.clear()
                if (cleaned.isNotEmpty()) {
                    val prev = out.lastOrNull()
                    if (prev is InlineSegment.TextRun &&
                        prev.bold == state.bold &&
                        prev.italic == state.italic &&
                        prev.mono == state.mono &&
                        prev.underline == state.underline &&
                        prev.smallCaps == state.smallCaps &&
                        prev.sans == state.sans &&
                        prev.sizeScale == state.sizeScale
                    ) {
                        out[out.lastIndex] = prev.copy(text = prev.text + cleaned)
                    } else {
                        out.add(
                            InlineSegment.TextRun(
                                text = cleaned,
                                bold = state.bold,
                                italic = state.italic,
                                mono = state.mono,
                                underline = state.underline,
                                smallCaps = state.smallCaps,
                                sans = state.sans,
                                sizeScale = state.sizeScale
                            )
                        )
                    }
                }
            }
        }

        val todayStr = lazy(LazyThreadSafetyMode.NONE) { SimpleDateFormat("MMMM d, yyyy", Locale.US).format(Date()) }
        var i = 0
        val len = text.length
        while (i < len) {
            // Inline math $...$
            if (text[i] == '$' && (i == 0 || text[i - 1] != '\\') && !text.startsWith("$$", i)) {
                var close = i + 1
                while (close < len) {
                    if (text[close] == '$' && text[close - 1] != '\\') break
                    close++
                }
                if (close < len && close > i + 1) {
                    flushText()
                    val math = cleanMathForRenderer(text.substring(i + 1, close))
                    if (math.isNotEmpty()) {
                        out.add(InlineSegment.InlineMath(math, sizeScale = state.sizeScale))
                    }
                    i = close + 1
                    continue
                }
            }

            // Inline math \(...\)
            if (text.startsWith("\\(", i)) {
                val close = text.indexOf("\\)", i + 2)
                if (close > i + 2) {
                    flushText()
                    val math = cleanMathForRenderer(text.substring(i + 2, close))
                    if (math.isNotEmpty()) {
                        out.add(InlineSegment.InlineMath(math, sizeScale = state.sizeScale))
                    }
                    i = close + 2
                    continue
                }
            }

            // Scoped brace group { ... }
            if (text[i] == '{') {
                val close = findMatchingBrace(text, i)
                if (close > i) {
                    flushText()
                    val inner = text.substring(i + 1, close)
                    out.addAll(parseInlineSegmentsInternal(inner, state, depth + 1))
                    i = close + 1
                    continue
                }
            }

            // Backslash commands
            if (text[i] == '\\') {
                // Escaped special chars & LaTeX spacing macros
                when {
                    text.startsWith("\\\\", i) -> {
                        textBuf.append('\n')
                        i += 2
                        val (_, nextPos) = readOptionalBracketArg(text, i)
                        i = nextPos
                        continue
                    }
                    isCmdAt(text, i, "\\newline") -> {
                        textBuf.append('\n'); i += 8; continue
                    }
                    isCmdAt(text, i, "\\today") -> {
                        textBuf.append(todayStr.value); i += 6; continue
                    }
                    isCmdAt(text, i, "\\LaTeX") -> {
                        textBuf.append("LaTeX"); i += 6; continue
                    }
                    isCmdAt(text, i, "\\TeX") -> {
                        textBuf.append("TeX"); i += 4; continue
                    }
                    isCmdAt(text, i, "\\textbackslash") -> {
                        textBuf.append("\\"); i += 14; continue
                    }
                    text.startsWith("\\,", i) -> {
                        textBuf.append("\u2009"); i += 2; continue
                    }
                    text.startsWith("\\;", i) || text.startsWith("\\:", i) || text.startsWith("\\ ", i) -> {
                        textBuf.append(' '); i += 2; continue
                    }
                    text.startsWith("\\!", i) -> {
                        i += 2; continue
                    }
                    isCmdAt(text, i, "\\qquad") -> {
                        textBuf.append("    "); i += 6; continue
                    }
                    isCmdAt(text, i, "\\quad") -> {
                        textBuf.append("  "); i += 5; continue
                    }
                }

                // Inline size switches
                val sizeSwitch: Pair<Int, Float>? = when {
                    isCmdAt(text, i, "\\tiny") -> 5 to 0.55f
                    isCmdAt(text, i, "\\scriptsize") -> 11 to 0.70f
                    isCmdAt(text, i, "\\footnotesize") -> 13 to 0.82f
                    isCmdAt(text, i, "\\small") -> 6 to 0.91f
                    isCmdAt(text, i, "\\normalsize") -> 11 to 1.0f
                    isCmdAt(text, i, "\\large") -> 6 to 1.18f
                    isCmdAt(text, i, "\\Large") -> 6 to 1.44f
                    isCmdAt(text, i, "\\LARGE") -> 6 to 1.73f
                    isCmdAt(text, i, "\\huge") -> 5 to 2.07f
                    isCmdAt(text, i, "\\Huge") -> 5 to 2.49f
                    else -> null
                }
                if (sizeSwitch != null) {
                    flushText()
                    state = state.copy(sizeScale = sizeSwitch.second)
                    i += sizeSwitch.first
                    if (i < len && text[i] == ' ') i++
                    continue
                }

                // Inline declaration style switches
                when {
                    isCmdAt(text, i, "\\bfseries") -> {
                        flushText(); state = state.copy(bold = true); i += 9; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\itshape") -> {
                        flushText(); state = state.copy(italic = true); i += 8; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\ttfamily") -> {
                        flushText(); state = state.copy(mono = true, sans = false); i += 9; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\sffamily") -> {
                        flushText(); state = state.copy(sans = true, mono = false); i += 9; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\rmfamily") -> {
                        flushText(); state = state.copy(sans = false, mono = false, bold = false, italic = false); i += 9; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\scshape") -> {
                        flushText(); state = state.copy(smallCaps = true); i += 8; if (i < len && text[i] == ' ') i++; continue
                    }
                    isCmdAt(text, i, "\\em") -> {
                        flushText(); state = state.copy(italic = !state.italic); i += 3; if (i < len && text[i] == ' ') i++; continue
                    }
                }

                // Brace-argument style commands (recursive!)
                val childTransform: Pair<Int, (StyleState) -> StyleState>? = when {
                    isCmdAt(text, i, "\\textbf") -> 7 to { s -> s.copy(bold = true) }
                    isCmdAt(text, i, "\\textit") -> 7 to { s -> s.copy(italic = true) }
                    isCmdAt(text, i, "\\emph") -> 5 to { s -> s.copy(italic = !s.italic) }
                    isCmdAt(text, i, "\\texttt") -> 7 to { s -> s.copy(mono = true, sans = false) }
                    isCmdAt(text, i, "\\textsf") -> 7 to { s -> s.copy(sans = true, mono = false) }
                    isCmdAt(text, i, "\\textrm") -> 7 to { s -> s.copy(sans = false, mono = false) }
                    isCmdAt(text, i, "\\textsc") -> 7 to { s -> s.copy(smallCaps = true) }
                    isCmdAt(text, i, "\\underline") -> 10 to { s -> s.copy(underline = true) }
                    isCmdAt(text, i, "\\text") -> 5 to { s -> s }
                    isCmdAt(text, i, "\\mbox") -> 5 to { s -> s }
                    else -> null
                }
                if (childTransform != null) {
                    val (content, nextPos) = readBraceArg(text, i + childTransform.first)
                    if (content != null) {
                        flushText()
                        out.addAll(parseInlineSegmentsInternal(content, childTransform.second(state), depth + 1))
                        i = nextPos
                        continue
                    }
                }

                // \href{url}{label} and \url{url} and \footnote{note}
                if (isCmdAt(text, i, "\\href")) {
                    val (_, afterUrl) = readBraceArg(text, i + 5)
                    val (label, nextPos) = readBraceArg(text, afterUrl)
                    if (label != null) {
                        flushText()
                        out.addAll(parseInlineSegmentsInternal(label, state.copy(underline = true), depth + 1))
                        i = nextPos
                        continue
                    }
                }
                if (isCmdAt(text, i, "\\url")) {
                    val (urlText, nextPos) = readBraceArg(text, i + 4)
                    if (urlText != null) {
                        flushText()
                        out.addAll(parseInlineSegmentsInternal(urlText, state.copy(mono = true, sans = false), depth + 1))
                        i = nextPos
                        continue
                    }
                }
                if (isCmdAt(text, i, "\\footnote")) {
                    val (noteText, nextPos) = readBraceArg(text, i + 9)
                    if (noteText != null) {
                        flushText()
                        val fnState = state.copy(sizeScale = state.sizeScale * 0.85f)
                        out.add(InlineSegment.TextRun(" (", sizeScale = fnState.sizeScale))
                        out.addAll(parseInlineSegmentsInternal(noteText, fnState, depth + 1))
                        out.add(InlineSegment.TextRun(")", sizeScale = fnState.sizeScale))
                        i = nextPos
                        continue
                    }
                }
            }

            textBuf.append(text[i])
            i++
        }
        flushText()
        return out
    }

    private fun cleanInlineFormatting(raw: String): String =
        cleanEscapedChars(raw.trim())

    private fun cleanEscapedChars(s: String): String = s
        .replace("\\&", "&")
        .replace("\\%", "%")
        .replace("\\$", "$")
        .replace("\\#", "#")
        .replace("\\_", "_")
        .replace("\\{", "{")
        .replace("\\}", "}")
        .replace("``", "“")
        .replace("''", "”")
        .replace("---", "—")
        .replace("--", "–")
        .replace("~", "\u00A0")
        .replace("\\par", "\n")
}
