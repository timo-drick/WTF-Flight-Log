package de.drick.flightlog.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import de.drick.compose.tilemap.GpsPoint
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.ViewPortState
import de.drick.compose.tilemap.tileProviderDipulZones
import de.drick.compose.tilemap.tileProviderMapBox
import de.drick.core.log
import de.drick.wtf_osd.GeoPoint
import de.drick.wtf_osd.GpsData
import de.drick.wtf_osd.GpsRecord
import kotlinx.coroutines.isActive

val Ble2 = Color(0xff90caf9)
val Ble7 = Color(0xff1976d2)

fun GeoPoint.toGpsPoint() = GpsPoint(latitude, longitude)

@Composable
fun GpsView(
    gpsData: GpsData,
    zoomLevel: Double,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    var frame: GpsRecord by remember(gpsData) { mutableStateOf(gpsData.wayPoints.first()) }
    val viewPortState = remember {
        ViewPortState(
            scope = scope,
            initialZoom = zoomLevel.toFloat(),
            initialPos = gpsData.wayPoints.first().position.toGpsPoint(),
            tileSize = 256,
            tileProviderMapBox,
            tileProviderDipulZones
        )
    }
    val overviewPoints = remember(gpsData) {
        gpsData.wayPoints.map { it.position.toGpsPoint() }
    }
    val startPoint = remember(gpsData) {
        gpsData.wayPoints.first().position.toGpsPoint()
    }
    val endPoint = remember(gpsData) {
        gpsData.wayPoints.last().position.toGpsPoint()
    }
    var currentPoint by remember { mutableStateOf(startPoint) }

    LaunchedEffect(zoomLevel) {
        viewPortState.zoom(zoomLevel.toFloat())
    }

    LaunchedEffect(gpsData) {
        log("launched effect start")
        val frameIterator = gpsData.wayPoints.listIterator()
        var currentFrame = frameIterator.next()
        while (isActive) {
            withFrameMillis {
                val videoPositionMillis = positionProvider()
                val currentOsdMillis = currentFrame.osdMillis
                val deltaMillis = currentOsdMillis - videoPositionMillis
                when {
                    deltaMillis < 0 -> {
                        //Seek forward in osd frames
                        while (currentFrame.osdMillis < videoPositionMillis && frameIterator.hasNext()) {
                            val newFrame = frameIterator.next()
                            if (newFrame.osdMillis > videoPositionMillis) {
                                frameIterator.previous()
                                break
                            }
                            currentFrame = newFrame
                        }
                    }

                    deltaMillis > 100 -> {
                        //Seek backward in osd frames
                        while (currentFrame.osdMillis > videoPositionMillis && frameIterator.hasPrevious()) {
                            val newFrame = frameIterator.previous()
                            if (newFrame.osdMillis < videoPositionMillis) {
                                frameIterator.next()
                                break
                            }
                            currentFrame = newFrame
                        }
                    }
                }
                if (currentFrame != frame) {
                    frame = currentFrame
                    currentPoint = currentFrame.position.toGpsPoint()
                    /*viewPortState.easeTo(
                    cameraOptions { center(currentPoint) }
                )*/
                    viewPortState.center(currentPoint) //TODO smooth transition
                }
            }
        }
        log("launched effect end")
    }

    TileMapView(
        modifier = modifier,
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
        // Current point
        drawCircle(Green, radius = 10f, center = Offset.Zero)
        // Start
        drawCircle(Black, radius = 10f, center = start)
        // End
        drawCircle(White, radius = 10f, center = endPoint.toOffset())
    }
}
