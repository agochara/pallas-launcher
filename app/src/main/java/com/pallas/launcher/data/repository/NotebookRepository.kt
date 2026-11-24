package com.pallas.launcher.data.repository

import android.content.Context
import com.pallas.launcher.domain.notebook.ComputationalNotebook
import com.pallas.launcher.domain.notebook.NotebookCell
import com.pallas.launcher.domain.notebook.NotebookJsonCodec
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONObject
import java.io.File
import java.util.UUID

class NotebookRepository(private val storageDir: File) {

    private val lock = Any()
    private val _notebooks = MutableStateFlow(loadAllFromDisk())
    val notebooks: StateFlow<List<ComputationalNotebook>> = _notebooks.asStateFlow()

    private fun loadAllFromDisk(): List<ComputationalNotebook> {
        if (!storageDir.exists()) return emptyList()
        val files = storageDir.listFiles { f -> f.isFile && f.extension.equals("json", ignoreCase = true) }
            ?: return emptyList()
        return files.mapNotNull { file ->
            runCatching {
                val obj = JSONObject(file.readText(Charsets.UTF_8))
                NotebookJsonCodec.fromJson(obj)
            }.getOrNull()
        }.sortedByDescending { it.updatedAtMs }
    }

    private fun writeToDisk(nb: ComputationalNotebook) {
        if (!storageDir.exists()) storageDir.mkdirs()
        val json = NotebookJsonCodec.toJson(nb).toString()
        val target = File(storageDir, "${nb.id}.json")
        val tmp = File(storageDir, "${nb.id}.json.tmp")
        tmp.writeText(json, Charsets.UTF_8)
        if (!tmp.renameTo(target)) {
            target.writeText(json, Charsets.UTF_8)
            tmp.delete()
        }
    }

    fun createNotebook(
        initialCells: List<NotebookCell> = listOf(NotebookCell()),
        customTitle: String? = null
    ): ComputationalNotebook {
        val now = System.currentTimeMillis()
        val safeCells = initialCells.ifEmpty { listOf(NotebookCell()) }
        val nb = ComputationalNotebook(
            id = UUID.randomUUID().toString(),
            title = NotebookJsonCodec.deriveAutoTitle(safeCells, now),
            customTitle = customTitle?.trim()?.takeIf { it.isNotEmpty() },
            cells = safeCells,
            createdAtMs = now,
            updatedAtMs = now
        )
        synchronized(lock) {
            writeToDisk(nb)
            _notebooks.value = (listOf(nb) + _notebooks.value).sortedByDescending { it.updatedAtMs }
        }
        return nb
    }

    fun saveNotebook(notebook: ComputationalNotebook): ComputationalNotebook {
        val safeCells = notebook.cells.ifEmpty { listOf(NotebookCell()) }
        val now = System.currentTimeMillis()
        val updated = notebook.copy(
            title = NotebookJsonCodec.deriveAutoTitle(safeCells, notebook.createdAtMs),
            cells = safeCells,
            updatedAtMs = now
        )
        synchronized(lock) {
            writeToDisk(updated)
            val exists = _notebooks.value.any { it.id == updated.id }
            _notebooks.value = if (exists) {
                _notebooks.value.map { if (it.id == updated.id) updated else it }
                    .sortedByDescending { it.updatedAtMs }
            } else {
                (listOf(updated) + _notebooks.value).sortedByDescending { it.updatedAtMs }
            }
        }
        return updated
    }

    fun renameNotebook(id: String, newTitle: String): ComputationalNotebook? {
        val clean = newTitle.trim()
        synchronized(lock) {
            val current = _notebooks.value.firstOrNull { it.id == id } ?: return null
            val updated = current.copy(
                customTitle = clean.takeIf { it.isNotEmpty() },
                updatedAtMs = System.currentTimeMillis()
            )
            writeToDisk(updated)
            _notebooks.value = _notebooks.value
                .map { if (it.id == id) updated else it }
                .sortedByDescending { it.updatedAtMs }
            return updated
        }
    }

    fun deleteNotebook(id: String) {
        synchronized(lock) {
            File(storageDir, "$id.json").delete()
            _notebooks.value = _notebooks.value.filterNot { it.id == id }
        }
    }

    fun restoreNotebook(notebook: ComputationalNotebook) {
        synchronized(lock) {
            writeToDisk(notebook)
            _notebooks.value = (_notebooks.value.filterNot { it.id == notebook.id } + notebook)
                .sortedByDescending { it.updatedAtMs }
        }
    }

    companion object {
        @Volatile
        private var instance: NotebookRepository? = null

        fun getInstance(context: Context): NotebookRepository =
            instance ?: synchronized(this) {
                instance ?: NotebookRepository(
                    File(context.applicationContext.filesDir, "pallas_notebooks")
                ).also { instance = it }
            }
    }
}
