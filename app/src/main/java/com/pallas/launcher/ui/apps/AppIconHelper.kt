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
import androidx.compose.runtime.produceState
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.pallas.launcher.domain.model.AppItem
import com.pallas.launcher.domain.model.ScreenApp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object AppIconHelper {
    private val iconCache = LruCache<String, ImageBitmap>(400)

    fun cacheKey(
        packageName: String,
        activityName: String? = null,
        userSerialNumber: Long = 0L,
        uniqueKey: String? = null
    ): String = uniqueKey
        ?: if (activityName != null) "$packageName/$activityName#$userSerialNumber" else packageName

    /** Cheap, main-thread-safe cache lookup. */
    fun peek(key: String): ImageBitmap? = iconCache.get(key)

    /** Call when packages are added/removed/updated so stale icons are not shown. */
    fun clear() = iconCache.evictAll()

    /** Blocking: call from a background dispatcher only. */
    fun getAppIcon(
        context: Context,
        packageName: String,
        activityName: String? = null,
        userHandle: UserHandle? = null,
        userSerialNumber: Long = 0L,
        uniqueKey: String? = null
    ): ImageBitmap? {
        val key = cacheKey(packageName, activityName, userSerialNumber, uniqueKey)
        iconCache.get(key)?.let { return it }

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
                activityInfo.getBadgedIcon(context.resources.displayMetrics.densityDpi)
            } else {
                context.packageManager.getApplicationIcon(packageName)
            }

            val width = (drawable.intrinsicWidth.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val height = (drawable.intrinsicHeight.takeIf { it > 0 } ?: 96).coerceIn(48, 192)
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            drawable.setBounds(0, 0, canvas.width, canvas.height)
            drawable.draw(canvas)
            bitmap.asImageBitmap().also { iconCache.put(key, it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getAppIcon(context: Context, packageName: String): ImageBitmap? =
        getAppIcon(context, packageName, null, null, 0L, packageName)
}

/** Loads the icon on Dispatchers.Default; returns the cached bitmap immediately when available. */
@Composable
private fun rememberAppIcon(
    packageName: String,
    activityName: String?,
    userHandle: UserHandle?,
    userSerialNumber: Long,
    uniqueKey: String?
): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val key = AppIconHelper.cacheKey(packageName, activityName, userSerialNumber, uniqueKey)
    val state = produceState(initialValue = AppIconHelper.peek(key), key1 = key) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                AppIconHelper.getAppIcon(context, packageName, activityName, userHandle, userSerialNumber, uniqueKey)
            }
        }
    }
    return state.value
}

@Composable
private fun IconOrPlaceholder(icon: ImageBitmap?, modifier: Modifier, size: Dp, shape: Shape) {
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier.size(size).clip(shape)
        )
    } else {
        Box(modifier = modifier.size(size).background(Color.Gray.copy(alpha = 0.2f), shape))
    }
}

@Composable
fun AppIconImage(
    app: AppItem,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape
) {
    val icon = rememberAppIcon(app.packageName, app.activityName, app.userHandle, app.userSerialNumber, app.uniqueKey)
    IconOrPlaceholder(icon, modifier, size, shape)
}

@Composable
fun AppIconImage(
    app: ScreenApp,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp
) {
    val icon = rememberAppIcon(app.packageName, app.activityName, app.userHandle, app.userSerialNumber, app.uniqueKey)
    IconOrPlaceholder(icon, modifier, size, RoundedCornerShape(shapeRadius))
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape
) {
    val icon = rememberAppIcon(packageName, null, null, 0L, packageName)
    IconOrPlaceholder(icon, modifier, size, shape)
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp
) {
    AppIconImage(packageName = packageName, modifier = modifier, size = size, shape = RoundedCornerShape(shapeRadius))
}
