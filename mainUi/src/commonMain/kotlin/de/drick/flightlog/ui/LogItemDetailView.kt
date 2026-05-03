package de.drick.flightlog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.compose.tilemap.exportKmlTrack
import de.drick.filehandling.rememberFileSaver
import de.drick.flightlog.cornerRadius
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.SRTFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.ui.components.GpsView
import de.drick.flightlog.ui.components.OsdCanvasView
import de.drick.flightlog.ui.components.SrtOverlayView
import de.drick.flightlog.ui.components.SuspendButton
import de.drick.flightlog.ui.components.VideoPlayer
import de.drick.flightlog.ui.components.VideoPlayerControls
import de.drick.flightlog.ui.components.toGeoPoint
import de.drick.wtf_osd.FontVariant
import de.drick.wtf_osd.GpsData
import de.drick.wtf_osd.GpsRecord
import de.drick.wtf_osd.OsdFont
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Speed
import de.drick.wtf_osd.SpeedUnit
import de.drick.wtf_osd.SrtData
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.loadOsdFont
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.roundToInt
import kotlin.math.roundToLong
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class OsdData(
    val file: OSDFile,
    val font: OsdFont,
    val record: OsdRecord,
    val gpsData: GpsData?
)

@Preview(
    widthDp = 1200, heightDp = 800,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Preview(
    widthDp = 800, heightDp = 800,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewLogItemDetail() {
    val testState = remember {
        val item = mockLogItem(
            name = "Test entry 2",
            mockOsdFile(
                font = FontVariant.BETAFLIGHT,
                hasGpsData = true,
                maxSpeed = Speed(89, SpeedUnit.Kmh),
                aircraftIdentifier = "DOLPHIN"
            ),
            mockSrtFile(duration = 345100.milliseconds),
            mockVideoFile("VF1"),
            mockVideoFile("VF2"),
            )
        LogItemState(item)
    }
    BasePreview {
        LogItemDetailPane(
            state = testState,
            onBackClick = {},
            onFullScreenClick = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

class LogItemState(
    val logItem: LogItem
) {
    val videoFileList = logItem.files.filterIsInstance<VideoFile>()
    private val srtFileList = logItem.files.filterIsInstance<SRTFile>()

    var videoFile by mutableStateOf(videoFileList.firstOrNull())
        private set

    val videoFileCount = videoFileList.size

    var selectedVideoIndex by mutableStateOf(0)

    var videoTimeOffset: Long by mutableStateOf(0)
        private set

    var osdData : OsdData? by mutableStateOf(null)
    var srtData : SrtData? by mutableStateOf(null)


    var currentSliderPosition: Float by mutableStateOf(0f)

    var zoomLevel : Double by mutableStateOf(17.0)
        private set

    private var initialized = false
    private val srtDataList = mutableListOf<SrtData>()

    fun setZoom(level: Double) {
        zoomLevel = level.coerceIn(1.0, 20.0)
    }
    
    fun selectVideo(index: Int) {
        selectedVideoIndex = index
        videoTimeOffset = srtFileList
            .take(index)
            .sumOf { it.duration.inWholeMilliseconds }
        videoFile = videoFileList[index]
        currentSliderPosition = 0f
        if (index < srtDataList.size) {
            srtData = srtDataList[index]
        }
    }

    suspend fun init() {
        if (initialized.not()) {
            withContext(Dispatchers.Default) {
                osdData = logItem.files
                    .filterIsInstance<OSDFile>()
                    .firstOrNull()
                    ?.let { osdFile ->
                        when (val result = parseOsdFile(osdFile.source())) {
                            is ParseResult.Error -> TODO()
                            is ParseResult.Success -> {
                                val font = loadOsdFont(osdFile.fontVariant)
                                val symbols = Symbols(result.record)
                                val flightData = extractFlightData(symbols, emptySet())
                                val gpsRecords = flightData.mapNotNull { data ->
                                    data.gpsPoint?.let { GpsRecord(it, data.millis) }
                                }
                                val gpsData = if (gpsRecords.size > 1) GpsData(gpsRecords) else null
                                OsdData(osdFile, font, result.record, gpsData)
                            }
                        }
                    }
                val list: List<SrtData> = logItem.files
                    .filterIsInstance<SRTFile>()
                    .mapNotNull { srtFile ->
                        when (val result = parseSrtFile(srtFile.source())) {
                            is ParseResult.Error -> null
                            is ParseResult.Success -> result.record
                        }
                    }
                srtDataList.clear()
                srtDataList.addAll(list)
                if (selectedVideoIndex < list.size) {
                    srtData = list[selectedVideoIndex]
                }
                initialized = true
            }
        }
    }
}

@Composable
fun LogItemDetailPane(
    state: LogItemState,
    onBackClick: () -> Unit,
    onFullScreenClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val videoFile = state.videoFile
    val osdData = state.osdData
    val srtData = state.srtData
    val playerState = rememberVideoPlayerState()
    val logItem = state.logItem

    val fileSaver = rememberFileSaver()

    LaunchedEffect(videoFile) {
        if (videoFile != null) {
            playerState.openFile(videoFile.file.platformFile())
            delay(100)
            playerState.seekTo(state.currentSliderPosition)
        }
        state.init()
    }

    DisposableEffect(state) {
        onDispose {
            state.currentSliderPosition = playerState.sliderPos
        }
    }

    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(MaterialTheme.cornerRadius()),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {
        Column(modifier = Modifier.fillMaxSize().padding(8.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                TextButton(onClick = onBackClick) {
                    Text("x")
                }
                Text(
                    text = logItem.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier
                )
                Spacer(Modifier.weight(1f))
                osdData?.gpsData?.let { gpsData ->
                    SuspendButton(onClick = {
                        val points = gpsData.wayPoints.map { it.position.toGeoPoint() }
                        val kml = exportKmlTrack(points).encodeToByteArray()
                        fileSaver.saveToFile(kml, "${logItem.name}.kml")
                    }) {
                        Text("Export KML")
                    }
                }
            }
            Text("Folder: ${logItem.files.first().path}")
            osdData?.let { osdData ->
                val aircraftName = remember(osdData) {
                    osdData.file.aircraftIdentifier ?: "Unknown"
                }
                Text("Aircraft: $aircraftName")
                Text("Maximum speed: ${osdData.file.maxSpeed.label()}")
                Text("Maximum height: ${osdData.file.maxHeight.label()}")
                osdData.file.maxDistanceHome?.let {
                    Text("Maximum distance to home: ${it.roundToInt()} m")
                }
                osdData.file.distanceTotal?.let {
                    Text("Travel distance: ${it.roundToInt()} m")
                }
            }
            Text("Flight time: ${logItem.duration()?.inWholeSeconds?.seconds?.toString()}")
            Spacer(Modifier.height(8.dp))
            Row(
                modifier = Modifier.weight(1f, fill = false),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (videoFile != null) {
                    Column(
                        modifier = Modifier.weight(0.6667f)
                    ) {
                        if (state.videoFileCount > 1) {
                            PrimaryTabRow(
                                modifier = Modifier,
                                selectedTabIndex = state.selectedVideoIndex
                            ) {
                                repeat(state.videoFileCount) { index ->
                                    val videoFile = state.videoFileList[index]
                                    Tab(
                                        selected = state.selectedVideoIndex == index,
                                        onClick = {
                                            playerState.pause()
                                            state.selectVideo(index)
                                        },
                                        text = {
                                            Text(videoFile.name)
                                        }
                                    )
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .aspectRatio(playerState.aspectRatio)
                                .weight(1f, fill = false)
                            ,
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            VideoPlayer(
                                modifier = Modifier.aspectRatio(playerState.aspectRatio),
                                playerState = playerState,
                                contentScale = ContentScale.Fit
                            ) {
                                osdData?.let { data ->
                                    OsdCanvasView(
                                        modifier = Modifier.fillMaxSize(),
                                        osdRecord = data.record,
                                        osdFont = data.font,
                                        positionProvider = {
                                            (playerState.currentTime * 1000.0).roundToLong() + state.videoTimeOffset
                                        }
                                    )
                                }
                                srtData?.let { data ->
                                    SrtOverlayView(
                                        modifier = Modifier.align(Alignment.BottomEnd).padding(16.dp),
                                        srtData = data,
                                        positionProvider = {
                                            (playerState.currentTime * 1000.0).roundToLong()
                                        }
                                    )
                                }
                            }
                        }
                        VideoPlayerControls(playerState, onFullScreenClick)
                    }
                }
                val gps = osdData?.gpsData
                if (gps != null) {
                    GpsView(
                        modifier = Modifier
                            .clipToBounds()
                            .weight(.3333f)
                            .aspectRatio(1f),
                        gpsData = gps,
                        zoomLevel = state.zoomLevel,
                        changeZoomLevel = { state.setZoom(it) },
                        positionProvider = {
                            (playerState.currentTime * 1000.0).roundToLong() + state.videoTimeOffset
                        }
                    )
                }
            }
        }
    }
}


