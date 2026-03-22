package de.drick.compose.tilemap

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput


fun LonLat.toGeoPoint() = GeoPoint(latitude = lat, longitude = lon)

data class MapCircle(
    val p: GeoPoint,
    val r: Float, // radius in tile
    val color: Color
)

data class MapPolygon(
    val points: List<TilePos>,
    val color: Color
)

@Composable
fun TestMapUavZonesPortugal(
    modifier: Modifier = Modifier,
    isDarkMode: Boolean = false
) {
    val viewPortState = rememberViewPortState(
        isDarkMode = isDarkMode,
        initialZoom = 2f,
        tileSize = 1024,
        darkTileProvider = tileProviderMapBoxDark,
        lightTileProvider = tileProviderMapBoxLight
    )

    var zones by remember { mutableStateOf<UASZoneData?>(null) }
    var uavZoneCircles by remember { mutableStateOf<List<MapCircle>>(emptyList()) }
    var uavZonePolygons by remember { mutableStateOf<List<MapPolygon>>(emptyList()) }
    LaunchedEffect(Unit) {
        zones = loadUavZones()
    }
    LaunchedEffect(zones, viewPortState.zoom) {
        val zoom = viewPortState.zoom.toInt()
        val color = Color.Red.copy(alpha = 0.3f)
        zones?.let { zoneData ->
            val geometryList = zoneData.features.flatMap { it.geometry }
            uavZoneCircles = extractCircles(geometryList, zoom, viewPortState, color)
            uavZonePolygons = extractPolygons(geometryList, zoom, color)
        }
    }
    val dragState = rememberDraggable2DState { offset ->
        viewPortState.movePx(offset.x, offset.y)
    }
    TileMapView(
        modifier = modifier
            .fillMaxSize()
            .focusable()
            .draggable2D(dragState)
            .pointerInput(Unit) {
                awaitPointerEventScope {
                    while (true) {
                        val event = awaitPointerEvent()
                        if (event.type == PointerEventType.Scroll) {
                            val inputChange = event.changes.first()
                            val scrollDelta = inputChange.scrollDelta.y.coerceIn(-1f, 1f)
                            val zoom = (viewPortState.zoom - scrollDelta).coerceIn(1f, 19f)
                            viewPortState.zoom(
                                newZoom = zoom,
                                x = inputChange.position.x,
                                y =inputChange.position.y
                            )
                        }
                    }
                }
            },
        state = viewPortState,
    ) {
        uavZoneCircles.forEach { zc ->
            drawCircle(
                color = zc.color,
                radius = zc.r,
                center = zc.p.toOffset()
            )
        }
        uavZonePolygons.forEach { poly ->
            val points = poly.points.map { it.toOffset() }
            drawPoints(
                points = points,
                pointMode = PointMode.Polygon,
                color = poly.color
            )
        }
    }
}

private fun extractCircles(
    geometryList: List<ZoneGeometry>,
    zoom: Int,
    viewPortState: ViewPortState,
    color: Color
) = geometryList
    .map { it.horizontalProjection }
    .filterIsInstance<CircleProjection>()
    .map {
        val p = it.center.toGeoPoint()
        val radius = metersToTileLength(p, zoom, it.radius)
        val pxRadius = radius.toFloat() * viewPortState.tileSize.toFloat()
        MapCircle(p, pxRadius.coerceIn(1f, null), color)
    }

private fun extractPolygons(
    geometryList: List<ZoneGeometry>,
    zoom: Int,
    color: Color
) = geometryList
    .map { it.horizontalProjection }
    .filterIsInstance<PolygonProjection>()
    .flatMap {
        it.coordinates
    }.map { coordinates: List<LonLat> ->
        val pointList = coordinates.map { lonLat ->
            lonLat.toGeoPoint().toTilePos(zoom)
        }
        MapPolygon(pointList, color)
    }
