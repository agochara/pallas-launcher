package com.pallas.launcher.ui.apps

import android.content.Context
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.Drawable
import android.os.Build
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
import kotlin.math.roundToInt

enum class IconStyle {
    ORIGINAL,
    THEMED
}

enum class AppIconShape {
    CIRCLE,
    SQUIRCLE
}

object AppIconHelper {
    private const val CACHE_CAPACITY = 400
    private val iconCache = LruCache<String, ImageBitmap>(CACHE_CAPACITY)

    fun cacheKey(
        packageName: String,
        activityName: String? = null,
        userSerialNumber: Long = 0L,
        uniqueKey: String? = null,
        iconStyle: IconStyle = IconStyle.ORIGINAL,
        shape: AppIconShape = AppIconShape.CIRCLE
    ): String {
        val base = uniqueKey
            ?: if (activityName != null) "$packageName/$activityName#$userSerialNumber" else packageName
        return "$base:${iconStyle.name}:${shape.name}"
    }

    fun peek(key: String): ImageBitmap? = iconCache.get(key)

    fun invalidate(packageName: String) {
        val snapshot = iconCache.snapshot()
        for (key in snapshot.keys) {
            if (key.startsWith(packageName) || key.contains("/$packageName") || key.contains("$packageName/")) {
                iconCache.remove(key)
            }
        }
    }

    fun clear() = iconCache.evictAll()

    fun getAppIcon(
        context: Context,
        packageName: String,
        activityName: String? = null,
        userHandle: UserHandle? = null,
        userSerialNumber: Long = 0L,
        uniqueKey: String? = null,
        iconStyle: IconStyle = IconStyle.ORIGINAL,
        iconShape: AppIconShape = AppIconShape.CIRCLE
    ): ImageBitmap? {
        val key = cacheKey(packageName, activityName, userSerialNumber, uniqueKey, iconStyle, iconShape)
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

            val metrics = context.resources.displayMetrics
            val rawDrawable: Drawable = if (activityInfo != null) {
                activityInfo.getBadgedIcon(metrics.densityDpi)
            } else {
                context.packageManager.getApplicationIcon(packageName)
            }

            val sizePx = (48f * metrics.density).roundToInt().coerceIn(96, 192)
            val bitmap = Bitmap.createBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)

            val clipPath = Path()
            when (iconShape) {
                AppIconShape.CIRCLE -> {
                    clipPath.addOval(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), Path.Direction.CW)
                }
                AppIconShape.SQUIRCLE -> {
                    val cornerRadius = sizePx * 0.28f
                    clipPath.addRoundRect(
                        0f, 0f, sizePx.toFloat(), sizePx.toFloat(),
                        cornerRadius, cornerRadius,
                        Path.Direction.CW
                    )
                }
            }

            val isAdaptive = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && rawDrawable is AdaptiveIconDrawable

            if (isAdaptive) {
                canvas.clipPath(clipPath)
                val adaptive = rawDrawable as AdaptiveIconDrawable
                val inset = (sizePx * AdaptiveIconDrawable.getExtraInsetFraction()).roundToInt()
                val monoDrawable = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    adaptive.monochrome
                } else null

                if (iconStyle == IconStyle.THEMED && monoDrawable != null) {
                    val bgPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = 0xFF2A2B2F.toInt()
                        style = Paint.Style.FILL
                    }
                    canvas.drawRect(0f, 0f, sizePx.toFloat(), sizePx.toFloat(), bgPaint)

                    val mono = monoDrawable.mutate()
                    mono.colorFilter = PorterDuffColorFilter(0xFFE2C48E.toInt(), PorterDuff.Mode.SRC_IN)
                    mono.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                    mono.draw(canvas)
                } else {
                    adaptive.background?.let { bg ->
                        bg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                        bg.draw(canvas)
                    }
                    adaptive.foreground?.let { fg ->
                        fg.setBounds(-inset, -inset, sizePx + inset, sizePx + inset)
                        fg.draw(canvas)
                    }
                }
            } else if (iconShape == AppIconShape.CIRCLE) {
                val tilePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = 0xFF282924.toInt()
                    style = Paint.Style.FILL
                }
                canvas.drawPath(clipPath, tilePaint)
                val pad = (sizePx * 0.18f).roundToInt()
                rawDrawable.setBounds(pad, pad, sizePx - pad, sizePx - pad)
                rawDrawable.draw(canvas)
            } else {
                canvas.clipPath(clipPath)
                rawDrawable.setBounds(0, 0, sizePx, sizePx)
                rawDrawable.draw(canvas)
            }

            bitmap.asImageBitmap().also { iconCache.put(key, it) }
        } catch (_: Exception) {
            null
        }
    }

    fun getAppIcon(context: Context, packageName: String): ImageBitmap? =
        getAppIcon(context, packageName, null, null, 0L, packageName)
}

@Composable
private fun rememberAppIcon(
    packageName: String,
    activityName: String?,
    userHandle: UserHandle?,
    userSerialNumber: Long,
    uniqueKey: String?,
    iconStyle: IconStyle = IconStyle.ORIGINAL,
    shape: AppIconShape = AppIconShape.CIRCLE
): ImageBitmap? {
    val context = LocalContext.current.applicationContext
    val key = AppIconHelper.cacheKey(packageName, activityName, userSerialNumber, uniqueKey, iconStyle, shape)
    val state = produceState(initialValue = AppIconHelper.peek(key), key1 = key) {
        if (value == null) {
            value = withContext(Dispatchers.Default) {
                AppIconHelper.getAppIcon(
                    context, packageName, activityName, userHandle, userSerialNumber, uniqueKey,
                    iconStyle, shape
                )
            }
        }
    }
    return state.value
}

@Composable
private fun IconOrPlaceholder(
    icon: ImageBitmap?,
    modifier: Modifier,
    size: Dp,
    shape: Shape
) {
    if (icon != null) {
        Image(
            bitmap = icon,
            contentDescription = null,
            modifier = modifier.size(size)
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
    shape: AppIconShape = AppIconShape.CIRCLE,
    iconStyle: IconStyle = IconStyle.ORIGINAL
) {
    val icon = rememberAppIcon(
        packageName = app.packageName,
        activityName = app.activityName,
        userHandle = app.userHandle,
        userSerialNumber = app.userSerialNumber,
        uniqueKey = app.uniqueKey,
        iconStyle = iconStyle,
        shape = shape
    )
    val composeShape: Shape = if (shape == AppIconShape.CIRCLE) CircleShape else RoundedCornerShape(12.dp)
    IconOrPlaceholder(icon, modifier, size, composeShape)
}

@Composable
fun AppIconImage(
    app: AppItem,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape
) {
    val iconShape = if (shape == CircleShape) AppIconShape.CIRCLE else AppIconShape.SQUIRCLE
    val icon = rememberAppIcon(
        packageName = app.packageName,
        activityName = app.activityName,
        userHandle = app.userHandle,
        userSerialNumber = app.userSerialNumber,
        uniqueKey = app.uniqueKey,
        shape = iconShape
    )
    IconOrPlaceholder(icon, modifier, size, shape)
}

@Composable
fun AppIconImage(
    app: ScreenApp,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shapeRadius: Dp = 10.dp
) {
    val icon = rememberAppIcon(
        packageName = app.packageName,
        activityName = app.activityName,
        userHandle = app.userHandle,
        userSerialNumber = app.userSerialNumber,
        uniqueKey = app.uniqueKey,
        shape = AppIconShape.SQUIRCLE
    )
    IconOrPlaceholder(icon, modifier, size, RoundedCornerShape(shapeRadius))
}

@Composable
fun AppIconImage(
    packageName: String,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    shape: Shape = CircleShape
) {
    val iconShape = if (shape == CircleShape) AppIconShape.CIRCLE else AppIconShape.SQUIRCLE
    val icon = rememberAppIcon(
        packageName = packageName,
        activityName = null,
        userHandle = null,
        userSerialNumber = 0L,
        uniqueKey = packageName,
        shape = iconShape
    )
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