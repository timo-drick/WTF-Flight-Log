package de.drick.compose.tilemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.sp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import de.drick.core.log
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.painterResource
import wtfflightlog.libs.tile_map.generated.resources.Res
import wtfflightlog.libs.tile_map.generated.resources.preview_map
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.roundToInt


data class TileImage(
    val pos: TilePos,
    var image: ImageBitmap?
)

data class TilePos(
    val zoom: Int,
    val x: Double,
    val y: Double
) {
    val tileX get() = x.toInt()
    val tileY get() = y.toInt()
}

data class VisibleTileRange(
    val startX: Int,
    val stopX: Int,
    val startY: Int,
    val stopY: Int
)

class TileLayerState(
    private val tileProvider: TileProvider,
    private val onInvalidate: () -> Unit
) {
    val tileList = mutableListOf<TileImage>()
    suspend fun update(
        newTileList: List<TilePos>
    ) {
        log("Prepare new tiles")
        val newTiles = newTileList.map { tp ->
            val existingTile = tileList.find { it.pos == tp && it.image != null }
            if (existingTile != null) {
                existingTile
            } else {
                val cachedImage = tileProvider.cachedTile(tp)
                TileImage(tp, cachedImage)
            }
        }
        tileList.clear()
        tileList.addAll(newTiles)
        onInvalidate()
        //Loading tiles
        val tilesToLoad = newTiles.filter { it.image == null }
        log("${tileProvider.name}: load ${tilesToLoad.size}")

        tilesToLoad.forEach { tile ->
            try {
                log("${tileProvider.name}: loading...: ${tile.pos}")
                val image = tileProvider.loadTile(tile.pos)
                tile.image = image
                log("${tileProvider.name}: loaded: ${tile.pos}")
                onInvalidate()
            } catch (err: CancellationException) {
                log("${tileProvider.name}: download interrupted")
                throw err // Propagate cancellation to stop when scope is canceled
            } catch (err: Throwable) {
                log(err)
            }
        }
        log("${tileProvider.name}: Loading finished")
    }
}

class ViewPortState(
    val scope: CoroutineScope,
    initialZoom: Float = 10f,
    initialPos: GeoPoint = GeoPoint(0.0, 0.0),
    val tileSize: Int = 512,
    vararg tileProviderList: TileProvider
) {
    init {
        log("Create instance: $this")
    }
    var zoom by mutableStateOf(initialZoom)
        private set
    val tileZoom get() = zoom.toInt()
    var centerPos by mutableStateOf(initialPos.toTilePos(tileZoom))
    var tileStateList = tileProviderList.toList().toStateList()
    val size = IntSize(tileSize, tileSize)

    private var sizePx = Size(0f, 0f)
    var invalidateCounter by mutableIntStateOf(0)

    fun List<TileProvider>.toStateList() = map { provider ->
        TileLayerState(provider) {
            invalidateCounter++
            log("Invalid counter: $invalidateCounter")
        }
    }

    fun updateTileProvider(vararg providerList: TileProvider){
        val newStateList = providerList.toList().toStateList()
        tileStateList = newStateList
        visibleRange = VisibleTileRange(0, 0, 0, 0)
        update()
        log("Update tile provider")
    }

    fun updateSize(size: Size) {
        if (size != sizePx) {
            log("Update size: $size old($sizePx)")
            sizePx = size
            update()
        }
    }

    fun center(point: GeoPoint) {
        centerPos = point.toTilePos(tileZoom)
        update()
    }
    fun zoom(newZoom: Float, x: Float? = null, y: Float? = null) {
        val xMove = x?.let { (it - (sizePx.width / 2f)) * 0.5f } ?: 0f
        val yMove = y?.let { (it - (sizePx.height / 2f)) * 0.5f } ?: 0f
        movePx(-xMove, -yMove)

        val pos = centerPos.toGeoPoint() // After zoom level changed we need to recalculate the center position
        zoom = newZoom
        centerPos = pos.toTilePos(tileZoom)
        update()
    }
    fun movePx(x: Float, y: Float) {
        val newX = centerPos.x - x / tileSize
        val newY = centerPos.y - y / tileSize
        //log("Old pos: $centerPos -> mx: $x my: $y")
        centerPos = centerPos.copy(
            x = newX,
            y = newY
        ).wrap() // make sure we do stay in the positive valid numbers
        update()
        invalidateCounter++
    }

    private var visibleRange = VisibleTileRange(0, 0, 0, 0)

    fun calculateOffset(pos: TilePos) = IntOffset(
        ((pos.x - centerPos.x) * tileSize).roundToInt(),
        ((pos.y - centerPos.y) * tileSize).roundToInt()
    )
    var updateJob: Job? = null

    private fun update() {
        if (sizePx != Size.Zero) {
            val minX = (sizePx.width / 2f / tileSize).roundToInt()
            val minY = (sizePx.height / 2f / tileSize).roundToInt()
            val range = VisibleTileRange(
                startX = centerPos.tileX - minX - 1,
                stopX = centerPos.tileX + minX + 1,
                startY = centerPos.tileY - minY - 1,
                stopY = centerPos.tileY + minY + 1
            )
            if (visibleRange != range) {
                updateJob?.cancel()
                updateJob = scope.launch(Dispatchers.Main.immediate) {
                    log("Update tile list center: $centerPos")
                    log("Range: $range - $visibleRange")
                    val newTileList = mutableListOf<TilePos>()
                    for (x in range.startX..range.stopX) {
                        for (y in range.startY..range.stopY) {
                            newTileList.add(TilePos(tileZoom, x.toDouble(), y.toDouble()))
                        }
                    }
                    for (tileState in tileStateList) {
                        launch {
                            tileState.update(newTileList)
                        }
                    }
                    visibleRange = range
                }
            }
        }
    }

    /**
     * Returns the number of meters represented by one pixel at the current zoom level and center latitude.
     */
    fun metersPerPixel(): Double {
        val latRad = centerPos.toGeoPoint().latitude.toRadians()
        val n = 1 shl tileZoom
        return 2.0 * PI * earthRadius * cos(latRad) / (n * tileSize)
    }

    fun geoPointToOffset(p: GeoPoint): Offset {
        val tilePos = p.toTilePos(tileZoom)
        return tilePosToOffset(tilePos)
    }
    fun tilePosToOffset(p: TilePos): Offset {
        return Offset(
            x = ((p.x - centerPos.x) * tileSize).toFloat(),
            y = ((p.y - centerPos.y) * tileSize).toFloat()
        )
    }
}

interface MapDrawScope : DrawScope {
    fun GeoPoint.toOffset(): Offset
    fun TilePos.toOffset(): Offset
}

private class MapDrawScopeImpl(
    private val delegate: DrawScope,
    private val viewPortState: ViewPortState,
) : MapDrawScope, DrawScope by delegate {
    override fun GeoPoint.toOffset() = viewPortState.geoPointToOffset(this)
    override fun TilePos.toOffset() = viewPortState.tilePosToOffset(this)
}

@Composable
fun rememberViewPortState(
    initialZoom: Float = 10f,
    initPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileSize: Int = 512,
    vararg tileProvider: TileProvider = arrayOf(tileProviderOsm)
): ViewPortState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        ViewPortState(scope, initialZoom, initPos, tileSize, *tileProvider)
    }
}

@Composable
fun rememberViewPortState(
    isDarkMode: Boolean,
    initialZoom: Float = 10f,
    initPos: GeoPoint = GeoPoint(0.0, 0.0),
    tileSize: Int = 512,
    darkTileProvider: TileProvider,
    lightTileProvider: TileProvider
): ViewPortState {
    val provider = if (isDarkMode) darkTileProvider else lightTileProvider
    val scope = rememberCoroutineScope()
    val state = remember(scope) {
        ViewPortState(scope, initialZoom, initPos, tileSize, provider)
    }
    LaunchedEffect(isDarkMode) {
        state.updateTileProvider(provider)
    }
    return state
}


private val scaleSteps = listOf(1, 2, 5, 10, 20, 50, 100, 200, 500, 1000, 2000, 5000, 10000, 20000, 50000, 100000, 200000, 500000, 1000000, 2000000)

private fun DrawScope.drawScaleBar(metersPerPixel: Double, canvasSize: Size, textMeasurer: TextMeasurer) {
    val maxBarWidthPx = canvasSize.width / 2f
    val maxBarMeters = maxBarWidthPx * metersPerPixel
    val scaleMeters = scaleSteps.lastOrNull { it <= maxBarMeters } ?: scaleSteps.first()
    val barWidthPx = (scaleMeters / metersPerPixel).toFloat()

    val label = if (scaleMeters >= 1000) "${scaleMeters / 1000} km" else "$scaleMeters m"

    val margin = 16f
    val tickHeight = 8f
    val barY = canvasSize.height - margin - tickHeight
    val barStartX = margin
    val barEndX = margin + barWidthPx

    // White outline for contrast
    val outlineColor = Color.White
    val barColor = Color.Black
    val strokeOutline = 4f
    val strokeBar = 2f

    // Draw outline
    drawLine(outlineColor, Offset(barStartX, barY), Offset(barEndX, barY), strokeWidth = strokeOutline + strokeBar)
    drawLine(outlineColor, Offset(barStartX, barY - tickHeight / 2), Offset(barStartX, barY + tickHeight / 2), strokeWidth = strokeOutline + strokeBar)
    drawLine(outlineColor, Offset(barEndX, barY - tickHeight / 2), Offset(barEndX, barY + tickHeight / 2), strokeWidth = strokeOutline + strokeBar)

    // Draw bar
    drawLine(barColor, Offset(barStartX, barY), Offset(barEndX, barY), strokeWidth = strokeBar)
    drawLine(barColor, Offset(barStartX, barY - tickHeight / 2), Offset(barStartX, barY + tickHeight / 2), strokeWidth = strokeBar)
    drawLine(barColor, Offset(barEndX, barY - tickHeight / 2), Offset(barEndX, barY + tickHeight / 2), strokeWidth = strokeBar)

    // Draw label
    val textLayoutResult = textMeasurer.measure(label, TextStyle(color = barColor, fontSize = 12.sp))
    val textX = barStartX + (barWidthPx - textLayoutResult.size.width) / 2
    val textY = barY - tickHeight / 2 - textLayoutResult.size.height - 2f
    // White background for text
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(color = outlineColor, fontSize = 12.sp, shadow = androidx.compose.ui.graphics.Shadow(color = outlineColor, blurRadius = 4f))
    )
    drawText(
        textMeasurer = textMeasurer,
        text = label,
        topLeft = Offset(textX, textY),
        style = TextStyle(color = barColor, fontSize = 12.sp)
    )
}

@Composable
fun TileMapView(
    state: ViewPortState,
    modifier: Modifier = Modifier,
    onDraw: MapDrawScope.() -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = modifier,
            painter = painterResource(Res.drawable.preview_map),
            contentDescription = "Map preview",
            contentScale = ContentScale.Crop
        )
    } else {
        val textMeasurer = rememberTextMeasurer()
        Canvas(modifier.clipToBounds()) {
            state.updateSize(size)
            val mapDrawScope = MapDrawScopeImpl(this, state)
            val frame = state.invalidateCounter
            //log("Frame: $frame")
            translate(size.width / 2, size.height / 2) {
                for (tileState in state.tileStateList) {
                    for (tile in tileState.tileList) {
                        tile.image?.let { image ->
                            drawImage(
                                image = image,
                                dstOffset = state.calculateOffset(tile.pos),
                                dstSize = state.size
                            )
                        }
                    }
                }
                onDraw(mapDrawScope)
            }
            drawScaleBar(state.metersPerPixel(), size, textMeasurer)
        }
    }
}