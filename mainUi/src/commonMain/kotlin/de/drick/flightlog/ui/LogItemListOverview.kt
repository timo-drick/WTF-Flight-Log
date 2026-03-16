package de.drick.flightlog.ui

import androidx.compose.foundation.focusable
import androidx.compose.foundation.gestures.draggable2D
import androidx.compose.foundation.gestures.rememberDraggable2DState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
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
import de.drick.flightlog.ui.components.Table
import de.drick.flightlog.ui.components.TableColumn
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
import kotlin.math.max
import kotlin.math.roundToInt
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
    val aircraftMap: Map<AircraftIdentifier, UiFlightData>
)

data class UiFlightData(
    val count: Int,
    val duration: Duration,
    val maxDuration: Duration,
    val maxSpeed: Speed,
    val maxHeight: Height,
    val maxDistanceHome: Int,
    val maxDistanceTotal: Int
)

fun calculateOverview(aircraftIdentifierList: List<AircraftIdentifier>, completeList: List<LogItem>) = FlightDataOverview(
    aircraftMap = aircraftIdentifierList.associateWith { id ->
        completeList.filter { it.getOSDFile()?.aircraftIdentifier == id.name }
            .calculateFlightData()
    } + Pair(
        first = AircraftIdentifier("Unknown"),
        second = completeList.filter { it.getOSDFile()?.aircraftIdentifier == null }
            .calculateFlightData()
    ) + Pair(
        first = AircraftIdentifier("All"),
        second = completeList.calculateFlightData()
    )
)

fun List<LogItem>.calculateFlightData(): UiFlightData {
    var count = 0
    var duration = 0.seconds
    var maxDuration = 0.seconds
    var maxSpeed = Speed(0, SpeedUnit.Unknown)
    var maxHeight = Height(0f, HeightUnit.Unknown)
    var maxDistanceHome = 0
    var maxDistanceTotal = 0
    forEach { item ->
        count++
        item.duration()?.let {
            duration += it
            maxDuration = if (maxDuration < it) it else maxDuration
        }
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
            osdFile.maxDistanceHome?.let { newDistance ->
                maxDistanceHome = max(maxDistanceHome, newDistance.roundToInt())
            }
            osdFile.distanceTotal?.let { newDistance ->
                maxDistanceTotal = max(maxDistanceTotal, newDistance.roundToInt())
            }
        }
    }
    return UiFlightData(
        count = count,
        duration = duration.inWholeSeconds.seconds,
        maxDuration = maxDuration.inWholeSeconds.seconds,
        maxSpeed = maxSpeed,
        maxHeight = maxHeight,
        maxDistanceHome = maxDistanceHome,
        maxDistanceTotal = maxDistanceTotal
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
        Column(Modifier.padding(8.dp).verticalScroll(rememberScrollState())) {
            Text(
                text = "Flight Log Overview",
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(bottom = 8.dp)
            )
            FlightDataSummaryTable(flightData)

            Spacer(Modifier.height(16.dp))

            val dragState = rememberDraggable2DState { offset ->
                viewPortState.movePx(offset.x, offset.y)
            }
            TileMapView(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(400.dp)
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

@Composable
fun FlightDataSummaryTable(overview: FlightDataOverview) {
    val columns = remember {
        listOf<TableColumn<Pair<String, UiFlightData>>>(
            TableColumn(
                header = "Aircraft",
                weight = 0.3f,
                content = { item, style, modifier -> Text(item.first, modifier = modifier, style = style) }
            ),
            TableColumn(
                header = "Count",
                weight = 0.15f,
                content = { item, style, modifier ->
                    Text(
                        text = item.second.count.toString(),
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Time",
                weight = 0.2f,
                content = { item, style, modifier ->
                    Text(
                        text = item.second.duration.toString(),
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Max Time",
                weight = 0.2f,
                content = { item, style, modifier ->
                    Text(
                        text = item.second.maxDuration.toString(),
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Max Speed",
                weight = 0.175f,
                content = { item, style, modifier ->
                    Text(
                        text = item.second.maxSpeed.label(),
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Max Height",
                weight = 0.175f,
                content = { item, style, modifier ->
                    Text(
                        text = item.second.maxHeight.label() ?: "-",
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Max Dist Travel",
                weight = 0.175f,
                content = { item, style, modifier ->
                    Text(
                        text = "${item.second.maxDistanceTotal} m",
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            ),
            TableColumn(
                header = "Max Dist Home",
                weight = 0.175f,
                content = { item, style, modifier ->
                    Text(
                        text = "${item.second.maxDistanceHome} m",
                        modifier = modifier.padding(horizontal = 4.dp),
                        style = style
                    )
                }
            )
        )
    }

    val aircraftItems = remember(overview) {
        overview.aircraftMap.filter { it.value.count > 0 }.map { it.key.name to it.value }
    }

    Table(
        modifier = Modifier.fillMaxWidth().width(IntrinsicSize.Max),
        items = aircraftItems,
        columns = columns
    )
}