package com.pallas.launcher.ui.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.os.Process
import android.os.UserHandle
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.ScreenApp

object AppIconHelper {
    private val iconCache = LruCache<String, ImageBitmap>(300)

    fun getAppIcon(
        context: Context,
        packageName: String,
        activityName: String? = null,
        userHandle: UserHandle? = null,
        userSerialNumber: Long = 0L,
        uniqueKey: String? = null
    ): ImageBitmap? {
        val cacheKey = uniqueKey ?: if (activityName != null) "$packageName/$activityName#$userSerialNumber" else packageName
        iconCache.get(cacheKey)?.let { return it }

        return try {
            val launcherApps = context.getSystemService(Context.LAUNCHER_APPS_SERVICE) as? LauncherApps
            val targetUser = userHandle ?: Process.myUserHandle()
            val activityList = try {
                launcherApps?.getActivityList(packageName, targetUser)
            } catch (_: Exception) {
                null
            }

            val activityInfo = if (!activityName.isNullOrBlank()) {
                activityList?.firstOrNull { it.componentName.className == activityName }
                    ?: activityList?.firstOrNull()
            } else {
                activityList?.firstOrNull()
            }

            val drawable = if (activityInfo != null) {
                val densityDpi = context.resources.displayMetrics.densityDpi
                activityInfo.getBadgedIcon(densityDpi)
            } else {
                context.packageManager.getApplicationIcon(packageName)
            }

            val width = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val height = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            val imageBitmap = bitmap.asImageBitmap()
            iconCache.put(cacheKey, imageBitmap)
            imageBitmap
        } catch (_: Exception) {
            null
        }
    }

    fun getAppIcon(context: Context, packageName: String): ImageBitmap? {
        return getAppIcon(
            context = context,
            packageName = packageName,
            activityName = null,
            userHandle = null,
            userSerialNumber = 0L,
            uniqueKey = packageName
        )
    }
}

@Composable
fun AppIconImage(
    app: AppItem,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
) {
    val context = LocalContext.current
    val icon = remember(app.uniqueKey) {
        AppIconHelper.getAppIcon(
            context = context,
            packageName = app.packageName,
            activityName = app.activityName,
            userHandle = app.userHandle,
            userSerialNumber = app.userSerialNumber,
            uniqueKey = app.uniqueKey
        )
    }

    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Gray.copy(alpha = 0.2f), shape)
        )
    }
}

@Composable
fun AppIconImage(
    app: ScreenApp,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp
) {
    val context = LocalContext.current
    val icon = remember(app.uniqueKey) {
        AppIconHelper.getAppIcon(
            context = context,
            packageName = app.packageName,
            activityName = app.activityName,
            userHandle = app.userHandle,
            userSerialNumber = app.userSerialNumber,
            uniqueKey = app.uniqueKey
        )
    }

    val shape = RoundedCornerShape(shapeRadius)
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier
                .size(size)
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Gray.copy(alpha = 0.2f), shape)
        )
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: androidx.compose.ui.graphics.Shape = CircleShape
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
                .clip(shape)
        )
    } else {
        Box(
            modifier = modifier
                .size(size)
                .background(Color.Gray.copy(alpha = 0.2f), shape)
        )
    }
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp
) {
    AppIconImage(
        packageName = packageName,
        modifier = modifier,
        size = size,
        shape = RoundedCornerShape(shapeRadius)
    )
}
