package com.pallas.launcher.platform

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ContentValues
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.net.Uri
import android.os.Environment
import android.provider.MediaStore
import android.widget.Toast
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

object TexExportHelper {
    private const val EXPORT_DIR = "tex_exports"
    private const val MAX_CACHED_EXPORTS = 6

    fun writeTempPng(context: Context, bitmap: Bitmap, prefix: String = "pallas_tex"): Uri {
        val dir = File(context.cacheDir, EXPORT_DIR).apply { mkdirs() }
        val existing = dir.listFiles { f -> f.isFile && f.extension.equals("png", ignoreCase = true) }
            ?.sortedBy { it.lastModified() }
            .orEmpty()
        if (existing.size >= MAX_CACHED_EXPORTS) {
            existing.take(existing.size - MAX_CACHED_EXPORTS + 1).forEach { it.delete() }
        }
        val outFile = File(dir, "${prefix}_${System.currentTimeMillis()}.png")
        FileOutputStream(outFile).use { stream ->
            if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                error("Failed to encode PNG image")
            }
        }
        return FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", outFile)
    }

    suspend fun savePngToGallery(
        context: Context,
        bitmap: Bitmap,
        filePrefix: String = "pallas_tex",
        folderName: String = "Pallas LaTeX"
    ): Result<Uri> = withContext(Dispatchers.IO) {
        runCatching {
            val resolver = context.contentResolver
            val fileName = "${filePrefix}_${System.currentTimeMillis()}.png"
            val values = ContentValues().apply {
                put(MediaStore.Images.Media.DISPLAY_NAME, fileName)
                put(MediaStore.Images.Media.MIME_TYPE, "image/png")
                put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/$folderName")
                put(MediaStore.Images.Media.IS_PENDING, 1)
            }
            val uri = resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
                ?: error("Could not create MediaStore entry")
            try {
                resolver.openOutputStream(uri)?.use { stream ->
                    if (!bitmap.compress(Bitmap.CompressFormat.PNG, 100, stream)) {
                        error("Failed to encode PNG image")
                    }
                } ?: error("Could not open output stream")

                val doneValues = ContentValues().apply {
                    put(MediaStore.Images.Media.IS_PENDING, 0)
                }
                resolver.update(uri, doneValues, null, null)
                uri
            } catch (t: Throwable) {
                runCatching { resolver.delete(uri, null, null) }
                throw t
            }
        }
    }

    suspend fun sharePng(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val uri = withContext(Dispatchers.IO) { writeTempPng(context, bitmap) }
        withContext(Dispatchers.Main) {
            val sendIntent = Intent(Intent.ACTION_SEND).apply {
                type = "image/png"
                putExtra(Intent.EXTRA_STREAM, uri)
                clipData = ClipData.newRawUri("LaTeX Formula", uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            val chooser = Intent.createChooser(sendIntent, "Share LaTeX image").apply {
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                if (context !is Activity) {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            }
            context.startActivity(chooser)
        }
    }

    suspend fun copyPngToClipboard(context: Context, bitmap: Bitmap): Result<Unit> = runCatching {
        val uri = withContext(Dispatchers.IO) { writeTempPng(context, bitmap) }
        withContext(Dispatchers.Main) {
            val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                ?: error("Clipboard unavailable")
            cm.setPrimaryClip(ClipData.newUri(context.contentResolver, "LaTeX Formula", uri))
        }
    }

    fun copyLatexText(context: Context, latex: String) {
        copyPlainText(context, "LaTeX Source", latex, "Copied LaTeX source")
    }

    fun copyPlainText(context: Context, label: String, text: String, toastMessage: String = "Copied") {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
        cm?.setPrimaryClip(ClipData.newPlainText(label, text))
        Toast.makeText(context, toastMessage, Toast.LENGTH_SHORT).show()
    }
}
