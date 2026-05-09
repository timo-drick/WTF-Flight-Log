package de.drick.flightlog.ui.components

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.ViewPortState
import de.drick.compose.tilemap.tileProviderDipulZones
import de.drick.compose.tilemap.tileProviderMapBoxSat
import de.drick.core.log
import de.drick.compose.tilemap.GeoPoint
import de.drick.wtf_osd.GpsData
import de.drick.wtf_osd.GpsPoint
import de.drick.wtf_osd.GpsRecord
import kotlinx.coroutines.isActive

val Ble2 = Color(0xff90caf9)
val Ble7 = Color(0xff1976d2)

fun GpsPoint.toGeoPoint() = GeoPoint(latitude, longitude, altitude)

private fun GpsRecord.interpolatePosition(nextFrame: GpsRecord?, videoPositionMillis: Long): GeoPoint {
    if (nextFrame == null || nextFrame.osdMillis == osdMillis) return position.toGeoPoint()

    val progress = ((videoPositionMillis - osdMillis).toDouble() / (nextFrame.osdMillis - osdMillis).toDouble())
        .coerceIn(0.0, 1.0)
    return GeoPoint(
        latitude = position.latitude.interpolateTo(nextFrame.position.latitude, progress),
        longitude = position.longitude.interpolateTo(nextFrame.position.longitude, progress),
        alt = position.altitude.interpolateTo(nextFrame.position.altitude, progress)
    )
}

private fun Double.interpolateTo(end: Double, progress: Double): Double = this + (end - this) * progress

private fun Double?.interpolateTo(end: Double?, progress: Double): Double? = when {
    this != null && end != null -> interpolateTo(end, progress)
    else -> this ?: end
}

@Composable
fun GpsView(
    gpsData: GpsData,
    zoomLevel: Double,
    positionProvider: () -> Long,
    changeZoomLevel: (Double) -> Unit,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var frame: GpsRecord by remember(gpsData) { mutableStateOf(gpsData.wayPoints.first()) }
    val viewPortState = remember {
        ViewPortState(
            scope = scope,
            initialZoom = zoomLevel.toFloat(),
            initialPos = gpsData.wayPoints.first().position.toGeoPoint(),
            tileSize = 256,
            tileProviderMapBoxSat,
            //tileProviderDipulZones
        )
    }
    val overviewPoints = remember(gpsData) {
        gpsData.wayPoints.map { it.position.toGeoPoint() }
    }
    val startPoint = remember(gpsData) {
        gpsData.wayPoints.first().position.toGeoPoint()
    }
    val endPoint = remember(gpsData) {
        gpsData.wayPoints.last().position.toGeoPoint()
    }
    var currentPoint by remember { mutableStateOf(startPoint) }

    LaunchedEffect(zoomLevel) {
        viewPortState.zoom(zoomLevel.toFloat())
    }

    LaunchedEffect(gpsData) {
        log("launched effect start")
        var currentIndex = 0
        while (isActive) {
            withFrameMillis {
                val videoPositionMillis = positionProvider()
                while (currentIndex < gpsData.wayPoints.lastIndex &&
                    gpsData.wayPoints[currentIndex + 1].osdMillis <= videoPositionMillis
                ) {
                    currentIndex++
                }
                while (currentIndex > 0 && gpsData.wayPoints[currentIndex].osdMillis > videoPositionMillis) {
                    currentIndex--
                }

                val currentFrame = gpsData.wayPoints[currentIndex]
                val nextFrame = gpsData.wayPoints.getOrNull(currentIndex + 1)
                currentPoint = currentFrame.interpolatePosition(nextFrame, videoPositionMillis)

                if (currentFrame != frame) {
                    frame = currentFrame
                    viewPortState.smoothCenter(currentPoint)
                }
            }
        }
        log("launched effect end")
    }

    TileMapView(
        modifier = modifier
            .focusable()
            .draggable2D(
                state = rememberDraggable2DState {
                        offset ->
                    // Log.d("Draggable2D", "Dragged to $offset")
                    viewPortState.movePx(offset.x, offset.y)
                }
            )
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val inputChange = event.changes.first()
                            val scrollDelta = inputChange.scrollDelta.y.coerceIn(-1f, 1f).toDouble()
                            changeZoomLevel(viewPortState.zoom - scrollDelta)
                        }
                    }
                }
            },
        state = viewPortState,
    ) {
        val start = startPoint.toOffset()
        val path = Path().apply {
            moveTo(start.x, start.y)
            overviewPoints.forEach {
                it.toOffset().let { p ->
                    lineTo(p.x, p.y)
                }
            }
        }
        drawPath(path, Ble2, style = Stroke(width = 4.0f))
        // Start
        drawCircle(Black, radius = 10f, center = start)
        // End
        drawCircle(White, radius = 10f, center = endPoint.toOffset())
        // Current point
        drawCircle(Green, radius = 10f, center = currentPoint.toOffset())
    }
}
