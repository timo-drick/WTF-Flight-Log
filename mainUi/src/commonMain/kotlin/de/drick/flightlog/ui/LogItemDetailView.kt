package de.drick.flightlog.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import de.drick.wtf_osd.GpsData
import de.drick.wtf_osd.OsdFont
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.SrtData
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.loadOsdFont
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import io.github.kdroidfilter.composemediaplayer.createVideoPlayerState
import kotlin.math.roundToLong

data class OsdData(
    val font: OsdFont,
    val record: OsdRecord,
    val gpsData: GpsData?
)

@Preview(
    widthDp = 1200, heightDp = 700,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewLogItemDetail() {
    val testState = remember {
        val item = mockLogItem("Test entry 2")
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
    val playerState = createVideoPlayerState()

    val videoFile = logItem.files.filterIsInstance<VideoFile>().firstOrNull()

    var osdData : OsdData? by mutableStateOf(null)
    var srtData : SrtData? by mutableStateOf(null)

    private var initialized = false

    suspend fun init() {
        if (initialized.not()) {
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
                            OsdData(font, result.record, gps)
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
            if (videoFile != null) {
                playerState.openFile(videoFile.file.platformFile())
            }
            initialized = true
        }
    }

    //TODO call dispose
    fun dispose() {
        playerState.dispose()
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
    val playerState = state.playerState

    LaunchedEffect(state) {
        state.init()
    }

    Surface(
        shape = RoundedCornerShape(MaterialTheme.cornerRadius()),
        color = MaterialTheme.colorScheme.surfaceContainer
    ) {

        Column(modifier = modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text("x")
                }
                Text(
                    text = state.logItem.name,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }

            HorizontalDivider()

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                if (videoFile != null) {
                    Column(
                        modifier = Modifier.weight(0.6667f)
                    ) {
                        Box(
                            modifier = modifier
                                .aspectRatio(playerState.aspectRatio),
                            contentAlignment = Alignment.BottomCenter
                        ) {
                            VideoPlayer(
                                modifier = Modifier.fillMaxSize(),
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


