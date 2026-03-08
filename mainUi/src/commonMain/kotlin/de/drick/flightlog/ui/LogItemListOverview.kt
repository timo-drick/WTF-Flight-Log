package de.drick.flightlog.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.graphics.Color.Companion.Green
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.TileMapView
import de.drick.compose.tilemap.rememberViewPortState
import de.drick.compose.tilemap.tileProviderMapBoxDark
import de.drick.compose.tilemap.tileProviderMapBoxLight
import de.drick.flightlog.cornerRadius
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.wtf_osd.FontVariant
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.HeightUnit
import de.drick.wtf_osd.Speed
import de.drick.wtf_osd.SpeedUnit
import de.drick.wtf_osd.unifiedValue
import io.github.kdroidfilter.platformtools.darkmodedetector.isSystemInDarkMode
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemList() {
    val testState = remember {
        mockFlightLogState(
            isWorking = false,
            mockLogItem("Test entry 2", FontVariant.ARDUPILOT),
            mockLogItem("Test entry 1", FontVariant.BETAFLIGHT),
            mockLogItem("Test entry 3", FontVariant.INAV),
            mockLogItem("Test entry 4", FontVariant.GENERIC),
        )
    }
    BasePreview {
        LogItemListOverview(
            modifier = Modifier.fillMaxSize(),
            state = testState
        )
    }
}


fun LogItem.startPosition() =
    files.filterIsInstance<OSDFile>().firstOrNull()?.startPosition

data class FlightDataOverview(
    val global: UiFlightData,
    val aircraftMap: Map<AircraftIdentifier, UiFlightData>
)

data class UiFlightData(
    val count: Int,
    val duration: Duration,
    val maxSpeed: Speed,
    val maxHeight: Height
)

fun calculateOverview(aircraftIdentifierList: List<AircraftIdentifier>, completeList: List<LogItem>) = FlightDataOverview(
    global = completeList.calculateFlightData(),
    aircraftMap = aircraftIdentifierList.associateWith { id ->
        completeList.filter { it.getOSDFile()?.aircraftIdentifier == id.name }
            .calculateFlightData()
    }
)

fun List<LogItem>.calculateFlightData(): UiFlightData {
    var count = 0
    var duration = 0.seconds
    var maxSpeed = Speed(0, SpeedUnit.Unknown)
    var maxHeight = Height(0f, HeightUnit.Unknown)
    forEach { item ->
        count++
        item.duration()?.let { duration += it }
        item.getOSDFile()?.let { osdFile ->
            osdFile.maxSpeed?.let { newSpeed ->
                if (newSpeed.unifiedValue() > maxSpeed.unifiedValue()) {
                    maxSpeed = newSpeed
                }
            }
            osdFile.maxHeight?.let { newHeight ->
                if (newHeight.unifiedValue() > maxHeight.unifiedValue()) {
                    maxHeight = newHeight
                }
            }
        }
    }
    return UiFlightData(
        count = count,
        duration = duration,
        maxSpeed = maxSpeed,
        maxHeight = maxHeight
    )
}

@Composable
fun LogItemListOverview(
    state: FlightLogState,
    modifier: Modifier = Modifier
) {
    val logList = state.list
    val flightData = remember(logList) {
        calculateOverview(state.aircraftIdentifierList, logList)
    }
    val flightTime = remember(flightData) {
        flightData.global.duration.inWholeSeconds.seconds.toString()
    }
    val positions = remember(logList) {
        logList.mapNotNull { it.startPosition() }
    }
    val viewPortState = rememberViewPortState(
        isDarkMode = isSystemInDarkMode(),
        initialZoom = 2f,
        tileSize = 1024,
        darkTileProvider = tileProviderMapBoxDark,
        lightTileProvider = tileProviderMapBoxLight
    )
    val middlePosition = remember(positions) {
        if (positions.isEmpty()) null
        else GeoPoint(
            latitude = positions.sumOf { it.latitude } / positions.size,
            longitude = positions.sumOf { it.longitude } / positions.size
        )
    }
    LaunchedEffect(middlePosition) {
        middlePosition?.let { viewPortState.center(it) }
    }
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.cornerRadius()),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(Modifier.padding(8.dp)) {
            Text("Log entries: ${state.list.size}")
            Text("Flight time: $flightTime")
            Text("Maximum speed: ${flightData.global.maxSpeed.label()}")
            Text("Maximum height: ${flightData.global.maxHeight.label()}")
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text("Detected aircrafts: ${state.aircraftIdentifierList.size}")
            }
            /*positions.forEach { pos ->
                Text("Pos: $pos")
            }*/
            val dragState = rememberDraggable2DState { offset ->
                viewPortState.movePx(offset.x, offset.y)
            }
            TileMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
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
                positions.forEach { pos ->
                    // Current point
                    drawCircle(Green, radius = 10f, center = pos.toOffset())
                }
            }
        }
    }
}