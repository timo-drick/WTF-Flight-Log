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
import androidx.compose.material3.Surface
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
import de.drick.flightlog.cornerRadius
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.SRTFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.ui.components.GpsView
import de.drick.flightlog.ui.components.OsdCanvasView
import de.drick.flightlog.ui.components.SrtOverlayView
import de.drick.flightlog.ui.components.VideoPlayer
import de.drick.flightlog.ui.components.VideoPlayerControls
import de.drick.wtf_osd.FontVariant
import de.drick.wtf_osd.GpsData
import de.drick.wtf_osd.OsdFont
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Speed
import de.drick.wtf_osd.SpeedUnit
import de.drick.wtf_osd.SrtData
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.loadOsdFont
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
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
            osdFile = mockOsdFile(
                font = FontVariant.BETAFLIGHT,
                hasGpsData = true,
                maxSpeed = Speed(89, SpeedUnit.Kmh),
                aircraftIdentifier = "DOLPHIN"
            ),
            srtFile = mockSrtFile(duration = 345100.milliseconds)
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
    val videoFile = logItem.files.filterIsInstance<VideoFile>().firstOrNull()

    var osdData : OsdData? by mutableStateOf(null)
    var srtData : SrtData? by mutableStateOf(null)

    private var initialized = false

    var currentSliderPosition: Float by mutableStateOf(0f)

    suspend fun init() {
        if (initialized.not()) {
            withContext(Dispatchers.Main) {
                osdData = logItem.files
                    .filterIsInstance<OSDFile>()
                    .firstOrNull()
                    ?.let { osdFile ->
                        when (val result = parseOsdFile(osdFile.source())) {
                            is ParseResult.Error -> TODO()
                            is ParseResult.Success -> {
                                val font = loadOsdFont(osdFile.fontVariant)
                                val gps = extractGps(result.record).let {
                                    if (it.wayPoints.isEmpty()) null else it
                                }
                                OsdData(osdFile, font, result.record, gps)
                            }
                        }
                    }
                srtData = logItem.files
                    .filterIsInstance<SRTFile>()
                    .firstOrNull()
                    ?.let { srtFile ->
                        when (val result = parseSrtFile(srtFile.source())) {
                            is ParseResult.Error -> TODO()
                            is ParseResult.Success -> result.record
                        }
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

    LaunchedEffect(state) {
        state.init()
        if (videoFile != null) {
            playerState.openFile(videoFile.file.platformFile())
            delay(100)
            playerState.seekTo(state.currentSliderPosition)
        }
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
            }
            osdData?.let { osdData ->
                val aircraftName = remember(osdData) {
                    osdData.file.aircraftIdentifier ?: "Unknown"
                }
                Text("Aircraft: $aircraftName")
                Text("Maximum speed: ${osdData.file.maxSpeed.label()}")
                Text("Maximum height: ${osdData.file.maxHeight.label()}")
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
                        Box(
                            modifier = modifier
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
                                            (playerState.currentTime * 1000.0).roundToLong()
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
                        zoomLevel = 17.0,
                        positionProvider = { (playerState.currentTime * 1000.0).roundToLong() }
                    )
                }
            }
        }
    }
}


