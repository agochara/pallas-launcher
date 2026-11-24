package com.pallas.launcher.platform

import android.content.ContentResolver
import android.content.ContentUris
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.provider.Settings
import android.widget.Toast
import com.pallas.launcher.domain.search.LauncherSearchResult
import com.pallas.launcher.domain.search.SmartCommandType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.Locale

object LocalFileSearchService {

    fun hasFileSearchAccess(context: Context): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Environment.isExternalStorageManager()
        } else {
            true
        }
    }

    fun openAllFilesAccessSettings(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val appIntent = Intent(
                Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION,
                Uri.parse("package:${context.packageName}")
            ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                Toast.makeText(context, "Allow file access to search documents & downloads", Toast.LENGTH_LONG).show()
                context.startActivity(appIntent)
                return
            } catch (_: Exception) {
                try {
                    val fallback = Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(fallback)
                    return
                } catch (_: Exception) {}
            }
        }
        Toast.makeText(context, "Unable to open file access settings", Toast.LENGTH_SHORT).show()
    }

    suspend fun searchFiles(
        context: Context,
        query: String,
        limit: Int = 15
    ): List<LauncherSearchResult.Command> = withContext(Dispatchers.IO) {
        val clean = query.trim()
        if (clean.isEmpty()) return@withContext emptyList()

        val uri = MediaStore.Files.getContentUri("external")
        val projection = arrayOf(
            MediaStore.Files.FileColumns._ID,
            MediaStore.Files.FileColumns.DISPLAY_NAME,
            MediaStore.Files.FileColumns.MIME_TYPE,
            MediaStore.Files.FileColumns.SIZE,
            MediaStore.Files.FileColumns.RELATIVE_PATH,
            MediaStore.Files.FileColumns.DATA
        )

        val escaped = clean
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_")
        val selection = "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? ESCAPE '\\' " +
            "AND ${MediaStore.Files.FileColumns.DISPLAY_NAME} NOT LIKE '.%' " +
            "AND (${MediaStore.Files.FileColumns.SIZE} IS NULL OR ${MediaStore.Files.FileColumns.SIZE} > 0) " +
            "AND ${MediaStore.Files.FileColumns.MIME_TYPE} IS NOT NULL " +
            "AND (${MediaStore.Files.FileColumns.RELATIVE_PATH} IS NULL OR " +
            "(${MediaStore.Files.FileColumns.RELATIVE_PATH} NOT LIKE 'Android/data/%' " +
            "AND ${MediaStore.Files.FileColumns.RELATIVE_PATH} NOT LIKE 'Android/obb/%'))"
        val selectionArgs = arrayOf("%$escaped%")
        val queryArgs = Bundle().apply {
            putString(ContentResolver.QUERY_ARG_SQL_SELECTION, selection)
            putStringArray(ContentResolver.QUERY_ARG_SQL_SELECTION_ARGS, selectionArgs)
            putStringArray(ContentResolver.QUERY_ARG_SORT_COLUMNS, arrayOf(MediaStore.Files.FileColumns.DATE_MODIFIED))
            putInt(ContentResolver.QUERY_ARG_SORT_DIRECTION, ContentResolver.QUERY_SORT_DIRECTION_DESCENDING)
            putInt(ContentResolver.QUERY_ARG_LIMIT, limit)
        }

        val results = mutableListOf<LauncherSearchResult.Command>()
        try {
            context.contentResolver.query(uri, projection, queryArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.SIZE)
                val pathCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.RELATIVE_PATH)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext() && results.size < limit) {
                    val id = cursor.getLong(idCol)
                    val displayName = if (nameCol >= 0) cursor.getString(nameCol) else null
                    val dataPath = if (dataCol >= 0) cursor.getString(dataCol) else null
                    val name = displayName?.takeIf { it.isNotBlank() }
                        ?: dataPath?.substringAfterLast('/')?.takeIf { it.isNotBlank() }
                        ?: continue
                    val mime = if (mimeCol >= 0) cursor.getString(mimeCol) else null
                    val sizeBytes = if (sizeCol >= 0) cursor.getLong(sizeCol) else 0L
                    val relPath = if (pathCol >= 0) cursor.getString(pathCol).orEmpty().trimEnd('/') else ""
                    val folder = relPath.ifBlank { "Storage" }
                    val sizeLabel = if (sizeBytes > 0L) " • ${formatFileSize(sizeBytes)}" else ""
                    val subtitle = "$folder$sizeLabel"
                    val contentUri = ContentUris.withAppendedId(uri, id)

                    results += LauncherSearchResult.Command(
                        id = "file_$id",
                        title = name,
                        subtitle = subtitle,
                        type = SmartCommandType.FILE,
                        execute = { ctx -> openFileUri(ctx, contentUri, mime) }
                    )
                }
            }
        } catch (_: SecurityException) {
            // Storage permission not granted
        } catch (_: Exception) {
            // Query failed gracefully
        }
        results
    }

    private fun openFileUri(context: Context, contentUri: Uri, mimeType: String?) {
        val resolvedMime = mimeType?.takeIf { it.isNotBlank() } ?: "*/*"
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(contentUri, resolvedMime)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            context.startActivity(intent)
        } catch (_: Exception) {
            if (resolvedMime != "*/*") {
                try {
                    val fallback = Intent(Intent.ACTION_VIEW).apply {
                        setDataAndType(contentUri, "*/*")
                        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
                    }
                    context.startActivity(fallback)
                    return
                } catch (_: Exception) {}
            }
            Toast.makeText(context, "No app available to open this file", Toast.LENGTH_SHORT).show()
        }
    }

    private fun formatFileSize(bytes: Long): String = when {
        bytes < 1024L -> "$bytes B"
        bytes < 1024L * 1024L -> String.format(Locale.US, "%.1f KB", bytes / 1024.0)
        bytes < 1024L * 1024L * 1024L -> String.format(Locale.US, "%.1f MB", bytes / (1024.0 * 1024.0))
        else -> String.format(Locale.US, "%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
    }
}
