package com.pallas.launcher.ui.atlas

import android.content.Context
import android.graphics.Paint
import android.graphics.Typeface
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.WindowInsetsSides
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.only
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsIgnoringVisibility
import androidx.compose.foundation.layout.union
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationCity
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pallas.launcher.domain.atlas.AtlasCity
import com.pallas.launcher.domain.atlas.AtlasCountry
import com.pallas.launcher.domain.atlas.AtlasData
import com.pallas.launcher.domain.atlas.AtlasPolygon
import com.pallas.launcher.domain.atlas.AtlasRepository
import com.pallas.launcher.domain.atlas.AtlasRing
import com.pallas.launcher.domain.atlas.AtlasSearchHit
import com.pallas.launcher.ui.theme.LauncherFonts
import com.pallas.launcher.ui.theme.LocalLauncherColors
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max
import kotlin.math.min

enum class AtlasViewMode(val label: String) {
    PHYSICAL("Physical"),
    POLITICAL("Political")
}

private sealed interface SelectedAtlasFeature {
    data class Country(val country: AtlasCountry) : SelectedAtlasFeature
    data class City(val city: AtlasCity) : SelectedAtlasFeature
}

private const val ATLAS_SHOW_CITIES_KEY = "atlas_show_cities"

private val POLITICAL_PALETTE_DARK = listOf(
    Color(0xFF282E34),
    Color(0xFF332D24),
    Color(0xFF24302C),
    Color(0xFF2E2A36),
    Color(0xFF312A27)
)

private val POLITICAL_PALETTE_LIGHT = listOf(
    Color(0xFFE5DFD3),
    Color(0xFFDAD5C7),
    Color(0xFFDCE3D9),
    Color(0xFFDDD8E2),
    Color(0xFFE7D9CF)
)

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun WorldAtlasScreen(
    modifier: Modifier = Modifier,
    onBack: (() -> Unit)? = null
) {
    val context = LocalContext.current
    val colorScheme = MaterialTheme.colorScheme
    val isDark = LocalLauncherColors.current.isDark
    val keyboardController = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val uiPrefs = remember(context) { context.getSharedPreferences("pallas_ui_state", Context.MODE_PRIVATE) }

    var atlasData by remember { mutableStateOf<AtlasData?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var viewMode by remember { mutableStateOf(AtlasViewMode.PHYSICAL) }
    var showCities by remember { mutableStateOf(uiPrefs.getBoolean(ATLAS_SHOW_CITIES_KEY, true)) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearchFocused by remember { mutableStateOf(false) }
    var selectedFeature by remember { mutableStateOf<SelectedAtlasFeature?>(null) }

    var canvasSize by remember { mutableStateOf(IntSize.Zero) }
    var zoom by remember { mutableFloatStateOf(1.15f) }
    var panX by remember { mutableFloatStateOf(0f) }
    var panY by remember { mutableFloatStateOf(0f) }
    var hasInitializedView by remember { mutableStateOf(false) }
    var cameraAnimJob by remember { mutableStateOf<Job?>(null) }

    BackHandler(enabled = isSearchFocused || selectedFeature != null) {
        if (isSearchFocused) {
            isSearchFocused = false
            keyboardController?.hide()
        } else {
            selectedFeature = null
        }
    }

    val latestViewMode by rememberUpdatedState(viewMode)
    val latestShowCities by rememberUpdatedState(showCities)

    LaunchedEffect(Unit) {
        runCatching { AtlasRepository.load(context) }
            .onSuccess { atlasData = it }
            .onFailure { loadError = it.message ?: "Failed to load offline atlas data" }
    }

    // Center the world neatly once canvas dimensions are known
    LaunchedEffect(canvasSize) {
        if (!hasInitializedView && canvasSize.width > 0 && canvasSize.height > 0) {
            val baseSize = canvasSize.width.toFloat()
            zoom = 1.0f
            panX = 0f
            panY = (canvasSize.height - baseSize) * 0.5f
            hasInitializedView = true
        }
    }

    fun clampPan(nx: Float, ny: Float, targetZoom: Float, size: IntSize): Pair<Float, Float> {
        if (size.width <= 0 || size.height <= 0) return nx to ny
        val worldSide = size.width.toFloat() * targetZoom
        val minX = size.width - worldSide - size.width * 0.15f
        val maxX = size.width * 0.15f
        val minY = min(0f, size.height - worldSide) - size.height * 0.25f
        val maxY = max(0f, size.height - worldSide) + size.height * 0.25f
        val loX = min(minX, maxX)
        val hiX = max(minX, maxX)
        val loY = min(minY, maxY)
        val hiY = max(minY, maxY)
        return nx.coerceIn(loX, hiX) to ny.coerceIn(loY, hiY)
    }

    fun animateToMercatorBox(
        minX: Float,
        minY: Float,
        maxX: Float,
        maxY: Float,
        targetCenterX: Float,
        targetCenterY: Float,
        minTargetZoom: Float = 2.5f
    ) {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val baseSide = canvasSize.width.toFloat()
        val spanX = (maxX - minX).coerceAtLeast(0.0004f)
        val spanY = (maxY - minY).coerceAtLeast(0.0004f)
        val fitZoomX = (canvasSize.width * 0.65f) / (spanX * baseSide)
        val fitZoomY = (canvasSize.height * 0.55f) / (spanY * baseSide)
        val targetZoom = min(fitZoomX, fitZoomY).coerceIn(minTargetZoom, 100f)
        val targetPanX = canvasSize.width * 0.5f - targetCenterX * baseSide * targetZoom
        val targetPanY = canvasSize.height * 0.48f - targetCenterY * baseSide * targetZoom
        val (clampedX, clampedY) = clampPan(targetPanX, targetPanY, targetZoom, canvasSize)

        cameraAnimJob?.cancel()
        cameraAnimJob = scope.launch {
            val startZ = zoom
            val startX = panX
            val startY = panY
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(450, easing = FastOutSlowInEasing)) {
                val t = value
                zoom = startZ + (targetZoom - startZ) * t
                panX = startX + (clampedX - startX) * t
                panY = startY + (clampedY - startY) * t
            }
        }
    }

    fun resetView() {
        if (canvasSize.width <= 0 || canvasSize.height <= 0) return
        val targetZoom = 1.0f
        val targetPanX = 0f
        val targetPanY = (canvasSize.height - canvasSize.width.toFloat()) * 0.5f
        cameraAnimJob?.cancel()
        cameraAnimJob = scope.launch {
            val startZ = zoom
            val startX = panX
            val startY = panY
            val anim = Animatable(0f)
            anim.animateTo(1f, tween(380, easing = FastOutSlowInEasing)) {
                val t = value
                zoom = startZ + (targetZoom - startZ) * t
                panX = startX + (targetPanX - startX) * t
                panY = startY + (targetPanY - startY) * t
            }
        }
    }

    val searchHits = remember(atlasData, searchQuery) {
        val data = atlasData ?: return@remember emptyList()
        AtlasRepository.searchAtlas(data, searchQuery)
    }

    val oceanBg = if (isDark) Color(0xFF0E1217) else Color(0xFFD6E2EC)
    val graticuleColor = if (isDark) Color(0xFFFFFFFF).copy(alpha = 0.055f) else Color(0xFF1A2530).copy(alpha = 0.07f)
    val physicalLandFill = if (isDark) Color(0xFF23262A) else Color(0xFFE7E2D6)
    val physicalCoastStroke = if (isDark) Color(0xFFE2C48E).copy(alpha = 0.32f) else Color(0xFF725A2E).copy(alpha = 0.42f)
    val politicalBorderColor = if (isDark) Color(0xFFE2C48E).copy(alpha = 0.36f) else Color(0xFF5A4929).copy(alpha = 0.48f)
    val selectedBorderColor = colorScheme.primary
    val politicalPalette = if (isDark) POLITICAL_PALETTE_DARK else POLITICAL_PALETTE_LIGHT

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(oceanBg)
    ) {
        val data = atlasData
        if (loadError != null) {
            Box(modifier = Modifier.fillMaxSize().padding(28.dp), contentAlignment = Alignment.Center) {
                Text(
                    text = loadError ?: "Unable to load atlas data",
                    style = MaterialTheme.typography.bodyMedium,
                    color = colorScheme.error
                )
            }
        } else if (data == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    CircularProgressIndicator(color = colorScheme.primary, modifier = Modifier.size(28.dp))
                    Text(
                        text = "Loading offline atlas…",
                        style = MaterialTheme.typography.bodySmall,
                        color = colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            val reusablePath = remember { Path() }

            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .onSizeChanged { canvasSize = it }
                    .pointerInput(data) {
                        detectTransformGestures { centroid, pan, gestureZoom, _ ->
                            cameraAnimJob?.cancel()
                            cameraAnimJob = null
                            isSearchFocused = false
                            keyboardController?.hide()
                            val oldZoom = zoom
                            val newZoom = (oldZoom * gestureZoom).coerceIn(0.85f, 140f)
                            val factor = newZoom / oldZoom
                            val rawX = centroid.x - (centroid.x - panX) * factor + pan.x
                            val rawY = centroid.y - (centroid.y - panY) * factor + pan.y
                            val (cx, cy) = clampPan(rawX, rawY, newZoom, canvasSize)
                            zoom = newZoom
                            panX = cx
                            panY = cy
                        }
                    }
                    .pointerInput(data) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                val targetZoom = (zoom * 2.4f).coerceIn(1f, 140f)
                                val factor = targetZoom / zoom
                                val rawX = tapOffset.x - (tapOffset.x - panX) * factor
                                val rawY = tapOffset.y - (tapOffset.y - panY) * factor
                                val (cx, cy) = clampPan(rawX, rawY, targetZoom, canvasSize)
                                cameraAnimJob?.cancel()
                                cameraAnimJob = scope.launch {
                                    val sz = zoom
                                    val sx = panX
                                    val sy = panY
                                    val anim = Animatable(0f)
                                    anim.animateTo(1f, tween(280, easing = FastOutSlowInEasing)) {
                                        val t = value
                                        zoom = sz + (targetZoom - sz) * t
                                        panX = sx + (cx - sx) * t
                                        panY = sy + (cy - sy) * t
                                    }
                                }
                            },
                            onTap = { tapOffset ->
                                isSearchFocused = false
                                keyboardController?.hide()
                                val worldScale = canvasSize.width.toFloat().coerceAtLeast(1f) * zoom
                                val mx = (tapOffset.x - panX) / worldScale
                                val my = (tapOffset.y - panY) / worldScale

                                // 1. Check visible cities only when Cities are enabled
                                if (latestShowCities) {
                                    val hitRadiusPx = 22.dp.toPx()
                                    var bestCity: AtlasCity? = null
                                    var bestDist = hitRadiusPx
                                    for (city in data.cities) {
                                        val visible = (city.isCapital && zoom >= 2.1f) ||
                                            (!city.isCapital && zoom >= 4.2f) ||
                                            (selectedFeature as? SelectedAtlasFeature.City)?.city == city
                                        if (!visible) continue
                                        val sx = panX + city.mercX * worldScale
                                        val sy = panY + city.mercY * worldScale
                                        val d = hypot(sx - tapOffset.x, sy - tapOffset.y)
                                        if (d < bestDist) {
                                            bestDist = d
                                            bestCity = city
                                        }
                                    }
                                    if (bestCity != null) {
                                        selectedFeature = SelectedAtlasFeature.City(bestCity)
                                        return@detectTapGestures
                                    }
                                }

                                // 2. In Political mode, hit-test country polygon (and interior enclave holes)
                                if (latestViewMode == AtlasViewMode.POLITICAL) {
                                    val country = AtlasRepository.hitTestCountry(data, mx, my)
                                    selectedFeature = country?.let { SelectedAtlasFeature.Country(it) }
                                } else {
                                    selectedFeature = null
                                }
                            }
                        )
                    }
            ) {
                val worldScale = size.width * zoom

                // Subtle graticule lines (every 30° lon, 30° lat + equator)
                for (lonDeg in -150..150 step 30) {
                    val sx = panX + AtlasRepository.lonToMercX(lonDeg.toDouble()) * worldScale
                    if (sx in -2f..(size.width + 2f)) {
                        drawLine(
                            color = graticuleColor,
                            start = Offset(sx, 0f),
                            end = Offset(sx, size.height),
                            strokeWidth = 1f
                        )
                    }
                }
                for (latDeg in listOf(-60, -30, 0, 30, 60)) {
                    val sy = panY + AtlasRepository.latToMercY(latDeg.toDouble()) * worldScale
                    if (sy in -2f..(size.height + 2f)) {
                        drawLine(
                            color = if (latDeg == 0) graticuleColor.copy(alpha = graticuleColor.alpha * 1.8f) else graticuleColor,
                            start = Offset(0f, sy),
                            end = Offset(size.width, sy),
                            strokeWidth = if (latDeg == 0) 1.3f else 1f,
                            pathEffect = if (latDeg == 0) PathEffect.dashPathEffect(floatArrayOf(10f, 8f)) else null
                        )
                    }
                }

                when (viewMode) {
                    AtlasViewMode.PHYSICAL -> {
                        // Physical Earth only: coastlines, landmasses & inland lakes — ZERO country borders
                        val coastStroke = Stroke(width = 0.9.dp.toPx())
                        for (ring in data.landRings) {
                            if (buildRingPathIfVisible(reusablePath, ring, panX, panY, worldScale, size.width, size.height)) {
                                drawPath(reusablePath, color = physicalLandFill)
                                drawPath(reusablePath, color = physicalCoastStroke, style = coastStroke)
                            }
                        }
                        for (ring in data.lakeRings) {
                            if (buildRingPathIfVisible(reusablePath, ring, panX, panY, worldScale, size.width, size.height)) {
                                drawPath(reusablePath, color = oceanBg)
                                drawPath(reusablePath, color = physicalCoastStroke.copy(alpha = 0.35f), style = coastStroke)
                            }
                        }
                    }

                    AtlasViewMode.POLITICAL -> {
                        // Political borders layer (10m topology + interior holes + Survey of India + Palestine)
                        val borderStroke = Stroke(width = 0.9.dp.toPx())
                        val lakeStroke = Stroke(width = 0.75.dp.toPx())
                        val selectedCountry = (selectedFeature as? SelectedAtlasFeature.Country)?.country

                        for (country in data.countries) {
                            val fill = politicalPalette[country.colorIndex.coerceIn(0, politicalPalette.lastIndex)]
                            val isSelected = selectedCountry?.id == country.id
                            for (poly in country.polygons) {
                                if (buildPolygonPathIfVisible(reusablePath, poly, panX, panY, worldScale, size.width, size.height)) {
                                    drawPath(
                                        reusablePath,
                                        color = if (isSelected) selectedBorderColor.copy(alpha = 0.24f) else fill
                                    )
                                    if (!isSelected) {
                                        drawPath(reusablePath, color = politicalBorderColor, style = borderStroke)
                                    }
                                }
                            }
                        }

                        // Inland lakes for geographic clarity
                        for (ring in data.lakeRings) {
                            if (buildRingPathIfVisible(reusablePath, ring, panX, panY, worldScale, size.width, size.height)) {
                                drawPath(reusablePath, color = oceanBg.copy(alpha = 0.92f))
                                drawPath(reusablePath, color = politicalBorderColor.copy(alpha = 0.45f), style = lakeStroke)
                            }
                        }

                        // Highlight selected country border clearly on top
                        if (selectedCountry != null) {
                            val hiStroke = Stroke(width = 2.dp.toPx())
                            for (poly in selectedCountry.polygons) {
                                if (buildPolygonPathIfVisible(reusablePath, poly, panX, panY, worldScale, size.width, size.height)) {
                                    drawPath(reusablePath, color = selectedBorderColor, style = hiStroke)
                                }
                            }
                        }

                        // Progressive non-overlapping country labels
                        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                            color = if (isDark) 0xFFE0D7C5.toInt() else 0xFF342E24.toInt()
                            textSize = 11.sp.toPx()
                            typeface = Typeface.create(Typeface.SERIF, Typeface.NORMAL)
                            textAlign = Paint.Align.CENTER
                        }
                        val placedBounds = ArrayList<FloatArray>()
                        for (country in data.countries) {
                            val screenSpanX = (country.mercMaxX - country.mercMinX) * worldScale
                            val screenSpanY = (country.mercMaxY - country.mercMinY) * worldScale
                            val isSelected = selectedCountry?.id == country.id
                            if (!isSelected && max(screenSpanX, screenSpanY) < 42.dp.toPx()) continue

                            val cx = panX + country.mercCenterX * worldScale
                            val cy = panY + country.mercCenterY * worldScale
                            if (cx !in -40f..(size.width + 40f) || cy !in -20f..(size.height + 20f)) continue

                            val tw = labelPaint.measureText(country.name)
                            val th = 14.dp.toPx()
                            val l = cx - tw * 0.5f - 4f
                            val r = cx + tw * 0.5f + 4f
                            val t = cy - th * 0.7f
                            val b = cy + th * 0.3f

                            val overlaps = !isSelected && placedBounds.any { box ->
                                !(r < box[0] || l > box[2] || b < box[1] || t > box[3])
                            }
                            if (!overlaps) {
                                placedBounds.add(floatArrayOf(l, t, r, b))
                                labelPaint.alpha = if (isSelected) 255 else 205
                                drawContext.canvas.nativeCanvas.drawText(country.name, cx, cy, labelPaint)
                            }
                        }
                    }
                }

                // Cities layer (disabled completely when showCities == false, except an actively searched single city)
                val selectedCity = (selectedFeature as? SelectedAtlasFeature.City)?.city
                if (showCities || selectedCity != null) {
                    val cityPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                        color = if (isDark) 0xFFF2ECE0.toInt() else 0xFF1F2229.toInt()
                        textSize = 10.sp.toPx()
                        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                        textAlign = Paint.Align.LEFT
                    }
                    val placedCityBoxes = ArrayList<FloatArray>()
                    val dotColor = colorScheme.primary
                    val secondaryDotColor = if (isDark) Color(0xFFD0D4DC).copy(alpha = 0.75f) else Color(0xFF3E4452).copy(alpha = 0.75f)

                    val citiesToRender = if (showCities) data.cities else listOfNotNull(selectedCity)
                    for (city in citiesToRender) {
                        val isSelected = selectedCity == city
                        val showDot = isSelected ||
                            (showCities && ((city.isCapital && zoom >= 2.1f) || (!city.isCapital && zoom >= 4.2f)))
                        if (!showDot) continue

                        val sx = panX + city.mercX * worldScale
                        val sy = panY + city.mercY * worldScale
                        if (sx !in -10f..(size.width + 10f) || sy !in -10f..(size.height + 10f)) continue

                        if (isSelected) {
                            drawCircle(
                                color = dotColor.copy(alpha = 0.25f),
                                radius = 11.dp.toPx(),
                                center = Offset(sx, sy)
                            )
                        }
                        drawCircle(
                            color = if (city.isCapital || isSelected) dotColor else secondaryDotColor,
                            radius = if (isSelected) 4.5.dp.toPx() else if (city.isCapital) 3.dp.toPx() else 2.2.dp.toPx(),
                            center = Offset(sx, sy)
                        )

                        val showLabel = isSelected ||
                            (showCities && ((city.isCapital && zoom >= 2.8f) || (!city.isCapital && zoom >= 5.5f)))
                        if (showLabel) {
                            val tw = cityPaint.measureText(city.name)
                            val lx = sx + 6.dp.toPx()
                            val ly = sy + 3.dp.toPx()
                            val box = floatArrayOf(lx - 2f, ly - 12.dp.toPx(), lx + tw + 2f, ly + 3f)
                            val overlaps = !isSelected && placedCityBoxes.any { b ->
                                !(box[2] < b[0] || box[0] > b[2] || box[3] < b[1] || box[1] > b[3])
                            }
                            if (!overlaps) {
                                placedCityBoxes.add(box)
                                cityPaint.alpha = if (isSelected) 255 else 220
                                drawContext.canvas.nativeCanvas.drawText(city.name, lx, ly, cityPaint)
                            }
                        }
                    }
                }
            }
        }

        // Top Search Bar + Back + Controls Row (cleared from camera cutout / status bar)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.TopCenter)
                .windowInsetsPadding(
                    WindowInsets.statusBarsIgnoringVisibility.only(WindowInsetsSides.Top)
                        .union(WindowInsets.displayCutout.only(WindowInsetsSides.Top))
                )
                .padding(horizontal = 14.dp, vertical = 10.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (onBack != null) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 4.dp,
                        modifier = Modifier
                            .size(44.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable(onClick = onBack)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = colorScheme.onSurface,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.94f),
                    border = BorderStroke(
                        width = 1.dp,
                        color = if (searchQuery.isNotEmpty()) colorScheme.primary.copy(alpha = 0.6f)
                        else colorScheme.outlineVariant.copy(alpha = 0.5f)
                    ),
                    shadowElevation = 4.dp,
                    modifier = Modifier.weight(1f)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = null,
                            tint = colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search countries or cities…",
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            BasicTextField(
                                value = searchQuery,
                                onValueChange = {
                                    searchQuery = it
                                    isSearchFocused = true
                                },
                                singleLine = true,
                                textStyle = MaterialTheme.typography.bodyMedium.copy(
                                    color = colorScheme.onSurface,
                                    fontSize = 14.5.sp
                                ),
                                cursorBrush = SolidColor(colorScheme.primary),
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                        if (searchQuery.isNotEmpty()) {
                            IconButton(
                                onClick = {
                                    searchQuery = ""
                                    isSearchFocused = false
                                },
                                modifier = Modifier.size(24.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Clear,
                                    contentDescription = "Clear search",
                                    tint = colorScheme.onSurfaceVariant,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Autocomplete dropdown
            if (isSearchFocused && searchQuery.isNotBlank() && searchHits.isNotEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.97f),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.55f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .heightIn(max = 260.dp)
                ) {
                    LazyColumn {
                        itemsIndexed(searchHits) { idx, hit ->
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        isSearchFocused = false
                                        searchQuery = hit.label
                                        keyboardController?.hide()
                                        when (hit) {
                                            is AtlasSearchHit.Country -> {
                                                val c = hit.country
                                                selectedFeature = SelectedAtlasFeature.Country(c)
                                                viewMode = AtlasViewMode.POLITICAL
                                                animateToMercatorBox(
                                                    minX = c.mercMinX,
                                                    minY = c.mercMinY,
                                                    maxX = c.mercMaxX,
                                                    maxY = c.mercMaxY,
                                                    targetCenterX = c.mercCenterX,
                                                    targetCenterY = c.mercCenterY,
                                                    minTargetZoom = 2.2f
                                                )
                                            }
                                            is AtlasSearchHit.City -> {
                                                val city = hit.city
                                                selectedFeature = SelectedAtlasFeature.City(city)
                                                animateToMercatorBox(
                                                    minX = city.mercX - 0.015f,
                                                    minY = city.mercY - 0.015f,
                                                    maxX = city.mercX + 0.015f,
                                                    maxY = city.mercY + 0.015f,
                                                    targetCenterX = city.mercX,
                                                    targetCenterY = city.mercY,
                                                    minTargetZoom = 7.5f
                                                )
                                            }
                                        }
                                    }
                                    .padding(horizontal = 14.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Icon(
                                    imageVector = if (hit is AtlasSearchHit.Country) Icons.Default.Public else Icons.Default.LocationCity,
                                    contentDescription = null,
                                    tint = colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = hit.label,
                                        style = MaterialTheme.typography.bodyMedium,
                                        fontWeight = FontWeight.Medium,
                                        color = colorScheme.onSurface
                                    )
                                    if (hit.secondary.isNotBlank()) {
                                        Text(
                                            text = hit.secondary,
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                            if (idx < searchHits.lastIndex) {
                                HorizontalDivider(
                                    thickness = 0.5.dp,
                                    color = colorScheme.outlineVariant.copy(alpha = 0.25f),
                                    modifier = Modifier.padding(horizontal = 14.dp)
                                )
                            }
                        }
                    }
                }
            }

            // Floating mode pill switcher + Cities toggle + Reset View button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                    border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                    shadowElevation = 3.dp
                ) {
                    Row(modifier = Modifier.padding(3.dp), horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                        AtlasViewMode.entries.forEach { mode ->
                            val selected = viewMode == mode
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                color = if (selected) colorScheme.primary else Color.Transparent,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable { viewMode = mode }
                            ) {
                                Text(
                                    text = mode.label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                    color = if (selected) colorScheme.onPrimary else colorScheme.onSurfaceVariant,
                                    maxLines = 1,
                                    modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }

                Row(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (showCities) {
                            colorScheme.primaryContainer.copy(alpha = 0.95f)
                        } else {
                            colorScheme.surfaceContainerHigh.copy(alpha = 0.92f)
                        },
                        border = BorderStroke(
                            1.dp,
                            if (showCities) colorScheme.primary.copy(alpha = 0.6f)
                            else colorScheme.outlineVariant.copy(alpha = 0.5f)
                        ),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable {
                                showCities = !showCities
                                uiPrefs.edit().putBoolean(ATLAS_SHOW_CITIES_KEY, showCities).apply()
                                if (!showCities && selectedFeature is SelectedAtlasFeature.City) {
                                    selectedFeature = null
                                }
                            }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.LocationCity,
                                contentDescription = if (showCities) "Hide cities" else "Show cities",
                                tint = if (showCities) colorScheme.primary else colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Cities",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = if (showCities) FontWeight.SemiBold else FontWeight.Medium,
                                color = if (showCities) colorScheme.onPrimaryContainer else colorScheme.onSurfaceVariant,
                                maxLines = 1
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colorScheme.surfaceContainerHigh.copy(alpha = 0.92f),
                        border = BorderStroke(1.dp, colorScheme.outlineVariant.copy(alpha = 0.5f)),
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { resetView() }
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.CenterFocusStrong,
                                contentDescription = "Reset view",
                                tint = colorScheme.primary,
                                modifier = Modifier.size(15.dp)
                            )
                            Text(
                                text = "Reset",
                                style = MaterialTheme.typography.labelMedium,
                                color = colorScheme.onSurface,
                                maxLines = 1
                            )
                        }
                    }
                }
            }
        }

        // Minimal Bottom Detail Card
        AnimatedVisibility(
            visible = selectedFeature != null,
            enter = fadeIn(tween(180)),
            exit = fadeOut(tween(150)),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .navigationBarsPadding()
                .padding(14.dp)
        ) {
            selectedFeature?.let { feat ->
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colorScheme.surfaceContainerHigh.copy(alpha = 0.96f),
                    border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.4f)),
                    shadowElevation = 6.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(
                            modifier = Modifier.weight(1f).padding(end = 10.dp),
                            verticalArrangement = Arrangement.spacedBy(3.dp)
                        ) {
                            when (feat) {
                                is SelectedAtlasFeature.Country -> {
                                    val c = feat.country
                                    Text(
                                        text = c.name,
                                        fontFamily = LauncherFonts.serif,
                                        fontSize = 23.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = colorScheme.onSurface
                                    )
                                    val metaParts = buildList {
                                        if (c.region.isNotBlank()) add(c.region)
                                        if (c.capital.isNotBlank()) add("Capital: ${c.capital}")
                                    }
                                    if (metaParts.isNotEmpty()) {
                                        Text(
                                            text = metaParts.joinToString(" · "),
                                            style = MaterialTheme.typography.bodySmall,
                                            color = colorScheme.onSurfaceVariant
                                        )
                                    }
                                    Text(
                                        text = String.format(Locale.US, "%.2f°, %.2f°", c.centerLat, c.centerLon),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        color = colorScheme.primary
                                    )
                                }
                                is SelectedAtlasFeature.City -> {
                                    val city = feat.city
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = city.name,
                                            fontFamily = LauncherFonts.serif,
                                            fontSize = 23.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colorScheme.onSurface
                                        )
                                        if (city.isCapital) {
                                            Surface(
                                                shape = RoundedCornerShape(5.dp),
                                                color = colorScheme.primary.copy(alpha = 0.14f),
                                                border = BorderStroke(1.dp, colorScheme.primary.copy(alpha = 0.35f))
                                            ) {
                                                Text(
                                                    text = "Capital",
                                                    style = MaterialTheme.typography.labelSmall,
                                                    color = colorScheme.primary,
                                                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                                )
                                            }
                                        }
                                    }
                                    Text(
                                        text = city.country,
                                        style = MaterialTheme.typography.bodySmall,
                                        color = colorScheme.onSurfaceVariant
                                    )
                                    Text(
                                        text = String.format(Locale.US, "%.2f°, %.2f°", city.lat, city.lon),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.5.sp,
                                        color = colorScheme.primary
                                    )
                                }
                            }
                        }
                        IconButton(
                            onClick = { selectedFeature = null },
                            modifier = Modifier.size(30.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss selection",
                                tint = colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

private fun isRingVisibleOnScreen(
    ring: AtlasRing,
    panX: Float,
    panY: Float,
    worldScale: Float,
    viewW: Float,
    viewH: Float
): Boolean {
    val minSx = panX + ring.minX * worldScale
    val maxSx = panX + ring.maxX * worldScale
    val minSy = panY + ring.minY * worldScale
    val maxSy = panY + ring.maxY * worldScale
    if (maxSx < -4f || minSx > viewW + 4f || maxSy < -4f || minSy > viewH + 4f) {
        return false
    }
    return (maxSx - minSx >= 0.65f) || (maxSy - minSy >= 0.65f)
}

private fun appendRingDecimated(
    path: Path,
    ring: AtlasRing,
    panX: Float,
    panY: Float,
    worldScale: Float
) {
    val coords = ring.coords
    val count = coords.size / 2
    if (count < 3) return
    val sx0 = panX + coords[0] * worldScale
    val sy0 = panY + coords[1] * worldScale
    path.moveTo(sx0, sy0)
    if (count <= 16) {
        for (i in 1 until count) {
            path.lineTo(panX + coords[i * 2] * worldScale, panY + coords[i * 2 + 1] * worldScale)
        }
    } else {
        var lastX = sx0
        var lastY = sy0
        for (i in 1 until count) {
            val sx = panX + coords[i * 2] * worldScale
            val sy = panY + coords[i * 2 + 1] * worldScale
            if (abs(sx - lastX) + abs(sy - lastY) >= 1.05f) {
                path.lineTo(sx, sy)
                lastX = sx
                lastY = sy
            }
        }
    }
    path.close()
}

private fun DrawScope.buildRingPathIfVisible(
    path: Path,
    ring: AtlasRing,
    panX: Float,
    panY: Float,
    worldScale: Float,
    viewW: Float,
    viewH: Float
): Boolean {
    if (!isRingVisibleOnScreen(ring, panX, panY, worldScale, viewW, viewH)) return false
    path.reset()
    path.fillType = PathFillType.NonZero
    appendRingDecimated(path, ring, panX, panY, worldScale)
    return true
}

private fun DrawScope.buildPolygonPathIfVisible(
    path: Path,
    poly: AtlasPolygon,
    panX: Float,
    panY: Float,
    worldScale: Float,
    viewW: Float,
    viewH: Float
): Boolean {
    if (!isRingVisibleOnScreen(poly.outer, panX, panY, worldScale, viewW, viewH)) return false
    path.reset()
    path.fillType = if (poly.holes.isEmpty()) PathFillType.NonZero else PathFillType.EvenOdd
    appendRingDecimated(path, poly.outer, panX, panY, worldScale)
    for (hole in poly.holes) {
        if (isRingVisibleOnScreen(hole, panX, panY, worldScale, viewW, viewH)) {
            appendRingDecimated(path, hole, panX, panY, worldScale)
        }
    }
    return true
}
