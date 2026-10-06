package com.pallas.launcher.ui.apps

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

object AppIconHelper {
    private val iconCache = LruCache<String, ImageBitmap>(300)

    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        iconCache.get(packageName)?.let { return it }

        return try {
            val pm = context.packageManager
            val drawable = pm.getApplicationIcon(packageName)
            val width = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val height = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(packageName, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 42.dp,
    shapeRadius: Dp = 10.dp
) {
    val context = LocalContext.current
    val icon = remember(packageName) {
        AppIconHelper.getAppIcon(context, packageName)
    }

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(RoundedCornerShape(shapeRadius))
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Gray.copy(alpha = 0.2f), RoundedCornerShape(shapeRadius))
        )
    }
}
