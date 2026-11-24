package com.pallas.launcher.data.repository

import android.content.Context
import android.content.Intent
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

data class QuickNote(
    val id: String,
    val timestamp: String,
    val text: String,
    val rawLine: String
)

/**
 * Append-only offline Markdown inbox for Quick Capture (`n <text>`).
 * Always writes to an internal `pallas_inbox.md` file and optionally mirrors each
 * appended line to a user-selected SAF `.md` file via DocumentProvider.
 */
class QuickCaptureRepository(
    private val inboxFile: File,
    private val appContext: Context? = null
) {
    private val lock = Any()
    private val prefs = appContext?.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    private val _notes = MutableStateFlow(loadAllFromDisk())
    val notes: StateFlow<List<QuickNote>> = _notes.asStateFlow()

    private val _linkedFileUri = MutableStateFlow(loadLinkedUri())
    val linkedFileUri: StateFlow<String?> = _linkedFileUri.asStateFlow()

    private val _linkedFileLabel = MutableStateFlow(loadLinkedLabel())
    val linkedFileLabel: StateFlow<String?> = _linkedFileLabel.asStateFlow()

    private fun loadLinkedUri(): String? = prefs?.getString(KEY_LINKED_URI, null)?.takeIf { it.isNotBlank() }
    private fun loadLinkedLabel(): String? = prefs?.getString(KEY_LINKED_LABEL, null)?.takeIf { it.isNotBlank() }

    private fun loadAllFromDisk(): List<QuickNote> {
        if (!inboxFile.isFile) return emptyList()
        val lines = runCatching { inboxFile.readLines(Charsets.UTF_8) }.getOrNull().orEmpty()
        // File stores chronological oldest -> newest; surface newest first in inbox UI.
        return lines.mapNotNull(::parseLine).asReversed()
    }

    private fun parseLine(rawLine: String): QuickNote? {
        val t = rawLine.trim()
        if (t.isEmpty()) return null
        val match = NOTE_LINE_REGEX.matchEntire(t)
        return if (match != null) {
            QuickNote(
                id = UUID.randomUUID().toString(),
                timestamp = match.groupValues[1],
                text = match.groupValues[2].trim(),
                rawLine = t
            )
        } else {
            val cleaned = t.removePrefix("- ").trim()
            if (cleaned.isEmpty()) return null
            QuickNote(
                id = UUID.randomUUID().toString(),
                timestamp = "",
                text = cleaned,
                rawLine = t
            )
        }
    }

    fun appendNote(text: String, nowMs: Long = System.currentTimeMillis()): QuickNote? {
        val clean = text.trim().replace(Regex("""[\r\n]+"""), " ")
        if (clean.isEmpty()) return null
        val stamp = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.US).format(Date(nowMs))
        val rawLine = "- [$stamp] $clean"
        val note = QuickNote(
            id = UUID.randomUUID().toString(),
            timestamp = stamp,
            text = clean,
            rawLine = rawLine
        )

        synchronized(lock) {
            inboxFile.parentFile?.mkdirs()
            inboxFile.appendText("$rawLine\n", Charsets.UTF_8)
            _notes.value = listOf(note) + _notes.value
        }

        mirrorToLinkedUri("$rawLine\n")
        return note
    }

    private fun mirrorToLinkedUri(lineWithNewline: String) {
        val ctx = appContext ?: return
        val uriStr = _linkedFileUri.value ?: return
        runCatching {
            val uri = Uri.parse(uriStr)
            try {
                ctx.contentResolver.openOutputStream(uri, "wa")?.use { out ->
                    out.write(lineWithNewline.toByteArray(Charsets.UTF_8))
                    out.flush()
                    return
                }
            } catch (_: Throwable) {
                // Fallback for document providers that disallow "wa" mode
            }
            val existing = ctx.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                it.readText()
            }.orEmpty()
            val sep = if (existing.isEmpty() || existing.endsWith('\n')) "" else "\n"
            ctx.contentResolver.openOutputStream(uri, "wt")?.use { out ->
                out.write((existing + sep + lineWithNewline).toByteArray(Charsets.UTF_8))
                out.flush()
            }
        }
    }

    fun deleteNote(note: QuickNote) {
        synchronized(lock) {
            val remaining = _notes.value.filterNot { it.id == note.id }
            rewriteDisk(remaining)
            _notes.value = remaining
        }
    }

    fun restoreNote(note: QuickNote, indexHint: Int = 0) {
        synchronized(lock) {
            val current = _notes.value.toMutableList()
            val idx = indexHint.coerceIn(0, current.size)
            current.add(idx, note)
            rewriteDisk(current)
            _notes.value = current
        }
    }

    fun clearAll() {
        synchronized(lock) {
            rewriteDisk(emptyList())
            _notes.value = emptyList()
        }
    }

    fun exportMarkdown(): String {
        val chronological = _notes.value.asReversed()
        return if (chronological.isEmpty()) "" else chronological.joinToString("\n", postfix = "\n") { it.rawLine }
    }

    fun setLinkedFile(uri: Uri?, displayName: String?) {
        val ctx = appContext
        if (uri != null && ctx != null) {
            runCatching {
                val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                ctx.contentResolver.takePersistableUriPermission(uri, flags)
            }
        }
        val uriStr = uri?.toString()
        val label = displayName?.takeIf { it.isNotBlank() }
            ?: uri?.lastPathSegment?.substringAfterLast('/')
            ?: "Linked .md file"
        prefs?.edit()?.apply {
            if (uriStr == null) {
                remove(KEY_LINKED_URI)
                remove(KEY_LINKED_LABEL)
            } else {
                putString(KEY_LINKED_URI, uriStr)
                putString(KEY_LINKED_LABEL, label)
            }
            apply()
        }
        _linkedFileUri.value = uriStr
        _linkedFileLabel.value = if (uriStr == null) null else label
    }

    private fun rewriteDisk(newestFirst: List<QuickNote>) {
        inboxFile.parentFile?.mkdirs()
        val content = newestFirst.asReversed().joinToString("\n") { it.rawLine }
            .let { if (it.isEmpty()) "" else "$it\n" }
        val tmp = File(inboxFile.parentFile, "${inboxFile.name}.tmp")
        tmp.writeText(content, Charsets.UTF_8)
        if (!tmp.renameTo(inboxFile)) {
            inboxFile.writeText(content, Charsets.UTF_8)
            tmp.delete()
        }
    }

    companion object {
        private const val PREFS_NAME = "pallas_quick_capture"
        private const val KEY_LINKED_URI = "linked_md_uri"
        private const val KEY_LINKED_LABEL = "linked_md_label"
        private val NOTE_LINE_REGEX = Regex("""^-\s+\[([^\]]+)\]\s*(.+)$""")

        @Volatile
        private var instance: QuickCaptureRepository? = null

        fun getInstance(context: Context): QuickCaptureRepository =
            instance ?: synchronized(this) {
                val app = context.applicationContext
                instance ?: QuickCaptureRepository(
                    inboxFile = File(app.filesDir, "pallas_inbox.md"),
                    appContext = app
                ).also { instance = it }
            }
    }
}
