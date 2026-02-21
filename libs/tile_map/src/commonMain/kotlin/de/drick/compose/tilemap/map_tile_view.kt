package de.drick.compose.tilemap

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.translate
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
import kotlin.math.roundToInt

class GpsPoint(val latitude: Double, val longitude: Double) {
    override fun toString() = "GeoPoint($latitude, $longitude)"
}

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
    initialPos: GpsPoint = GpsPoint(0.0, 0.0),
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
    val tileStateList = tileProviderList.map { provider ->
        TileLayerState(provider) {
            invalidateCounter++
            log("Invalid counter: $invalidateCounter")
        }
    }
    val size = IntSize(tileSize, tileSize)

    private var sizePx = Size(0f, 0f)
    var invalidateCounter by mutableIntStateOf(0)

    fun updateSize(size: Size) {
        if (size != sizePx) {
            log("Update size: $size old($sizePx)")
            sizePx = size
            update()
        }
    }

    fun center(point: GpsPoint) {
        centerPos = point.toTilePos(tileZoom)
        update()
    }
    fun zoom(newZoom: Float) {
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
        )
        log("Move to: ${centerPos.toGeoPoint()}")
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

    fun geoPointToOffset(p: GpsPoint): Offset {
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
    fun GpsPoint.toOffset(): Offset
    fun TilePos.toOffset(): Offset
}

private class MapDrawScopeImpl(
    private val delegate: DrawScope,
    private val viewPortState: ViewPortState,
) : MapDrawScope, DrawScope by delegate {
    override fun GpsPoint.toOffset() = viewPortState.geoPointToOffset(this)
    override fun TilePos.toOffset() = viewPortState.tilePosToOffset(this)
}

@Composable
fun rememberViewPortState(
    initialZoom: Float = 10f,
    initPos: GpsPoint = GpsPoint(0.0, 0.0),
    tileSize: Int = 512,
    vararg tileProvider: TileProvider = arrayOf(tileProviderOsm)
): ViewPortState {
    val scope = rememberCoroutineScope()
    return remember(scope) {
        ViewPortState(scope, initialZoom, initPos, tileSize, *tileProvider)
    }
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
            contentDescription = "Map preview"
        )
    } else {
        Canvas(modifier) {
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
        }
    }
}