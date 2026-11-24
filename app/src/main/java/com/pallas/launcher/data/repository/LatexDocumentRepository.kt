package com.pallas.launcher.data.repository

import android.content.Context
import com.pallas.launcher.domain.latex.LatexDocumentProcessor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

data class LatexDocument(
    val id: String,
    val title: String,
    val customTitle: String? = null,
    val content: String = "",
    val createdAtMs: Long = System.currentTimeMillis(),
    val updatedAtMs: Long = System.currentTimeMillis()
) {
    val displayTitle: String
        get() = customTitle?.takeIf { it.isNotBlank() } ?: title
}

class LatexDocumentRepository(private val storageDir: File) {

    private val lock = Any()
    private val _documents = MutableStateFlow(loadAllFromDisk())
    val documents: StateFlow<List<LatexDocument>> = _documents.asStateFlow()

    private fun loadAllFromDisk(): List<LatexDocument> {
        if (!storageDir.exists()) return emptyList()
        val files = storageDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                val obj = JSONObject(file.readText(Charsets.UTF_8))
                val id = obj.optString("id").ifBlank { file.nameWithoutExtension }
                val created = obj.optLong("createdAtMs", file.lastModified())
                val updated = obj.optLong("updatedAtMs", file.lastModified())
                val content = obj.optString("content", "")
                val customTitle = if (obj.has("customTitle") && !obj.isNull("customTitle")) {
                    obj.optString("customTitle").takeIf { it.isNotBlank() }
                } else null
                val title = obj.optString("title").ifBlank {
                    LatexDocumentProcessor.deriveAutoTitle(content, created)
                }
                LatexDocument(
                    id = id,
                    title = title,
                    customTitle = customTitle,
                    content = content,
                    createdAtMs = created,
                    updatedAtMs = updated
                )
            }.getOrNull()
        }.sortedByDescending { it.updatedAtMs }
    }

    private fun writeToDisk(doc: LatexDocument) {
        if (!storageDir.exists()) storageDir.mkdirs()
        val json = JSONObject().apply {
            put("id", doc.id)
            put("title", doc.title)
            if (doc.customTitle != null) put("customTitle", doc.customTitle) else put("customTitle", JSONObject.NULL)
            put("content", doc.content)
            put("createdAtMs", doc.createdAtMs)
            put("updatedAtMs", doc.updatedAtMs)
        }.toString()
        val target = File(storageDir, "${doc.id}.json")
        val tmp = File(storageDir, "${doc.id}.json.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(json, Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun createDocument(initialContent: String = "", customTitle: String? = null): LatexDocument {
        val now = System.currentTimeMillis()
        val autoTitle = LatexDocumentProcessor.deriveAutoTitle(initialContent, now)
        val doc = LatexDocument(
            id = UUID.randomUUID().toString(),
            title = autoTitle,
            customTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() },
            content = initialContent,
            createdAtMs = now,
            updatedAtMs = now
        )
        synchronized(lock) {
            writeToDisk(doc)
            _documents.value = (listOf(doc) + _documents.value).sortedByDescending { it.updatedAtMs }
        }
        return doc
    }

    fun updateContent(id: String, newContent: String): LatexDocument? {
        synchronized(lock) {
            val current = _documents.value.firstOrNull { it.id == id } ?: return null
            if (current.content == newContent) return current
            val now = System.currentTimeMillis()
            val updated = current.copy(
                title = LatexDocumentProcessor.deriveAutoTitle(newContent, current.createdAtMs),
                content = newContent,
                updatedAtMs = now
            )
            writeToDisk(updated)
            _documents.value = _documents.value
                .map { if (it.id == id) updated else it }
                .sortedByDescending { it.updatedAtMs }
            return updated
        }
    }

    fun renameDocument(id: String, newTitle: String): LatexDocument? {
        val clean = newTitle.trim()
        synchronized(lock) {
            val current = _documents.value.firstOrNull { it.id == id } ?: return null
            val updated = current.copy(
                customTitle = clean.takeIf { it.isNotEmpty() },
                updatedAtMs = System.currentTimeMillis()
            )
            writeToDisk(updated)
            _documents.value = _documents.value
                .map { if (it.id == id) updated else it }
                .sortedByDescending { it.updatedAtMs }
            return updated
        }
    }

    fun deleteDocument(id: String) {
        synchronized(lock) {
            File(storageDir, "$id.json").delete()
            _documents.value = _documents.value.filterNot { it.id == id }
        }
    }

    fun restoreDocument(doc: LatexDocument) {
        synchronized(lock) {
            writeToDisk(doc)
            _documents.value = (_documents.value.filterNot { it.id == doc.id } + doc)
                .sortedByDescending { it.updatedAtMs }
        }
    }

    companion object {
        @Volatile
        private var instance: LatexDocumentRepository? = null

        fun getInstance(context: Context): LatexDocumentRepository =
            instance ?: synchronized(this) {
                instance ?: LatexDocumentRepository(
                    File(context.applicationContext.filesDir, "pallas_latex")
                ).also { instance = it }
            }
    }
}
