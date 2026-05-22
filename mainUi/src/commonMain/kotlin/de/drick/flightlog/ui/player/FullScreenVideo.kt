package de.drick.flightlog.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.movableContentOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.IntOffset
import de.drick.core.log
import kotlin.math.roundToInt
import de.drick.flightlog.ui.BasePreview
import de.drick.flightlog.ui.LogItemState
import de.drick.flightlog.ui.icons.MaterialIconsOpenWith
import de.drick.flightlog.ui.icons.MaterialIconsZoom_in
import de.drick.flightlog.ui.icons.MaterialIconsZoom_out
import de.drick.flightlog.ui.icons.MaterialIconsInfo
import de.drick.flightlog.ui.icons.MaterialIconsMyLocation
import org.jetbrains.compose.resources.stringResource
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps_follow
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps_info
import de.drick.flightlog.ui.components.GpsView
import de.drick.flightlog.ui.components.OsdCanvasView
import de.drick.flightlog.ui.components.SrtOverlayView
import de.drick.flightlog.ui.components.VideoPlayer
import de.drick.flightlog.ui.mockLogItem
import de.drick.wtf_osd.FontVariant
import io.github.kdroidfilter.composemediaplayer.InitialPlayerState
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import wtfflightlog.mainui.generated.resources.Res
import wtfflightlog.mainui.generated.resources.preview_map
import kotlinx.coroutines.delay
import org.jetbrains.compose.resources.painterResource
import kotlin.math.roundToLong
import kotlin.time.TimeSource

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewFullScreenPlayerPanel() {
    val testState = remember {
        val font = FontVariant.BETAFLIGHT
        val item = mockLogItem("Test entry 2", font)
        LogItemState(item)
    }
    BasePreview {
        FullScreenPlayerPanel(
            modifier = Modifier.fillMaxSize(),
            state = testState,
            onClose = {},
            showOverlayForPreview = true
        )
    }
}

@Composable
fun FullScreenPlayerPanel(
    state: LogItemState,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
    showOverlayForPreview: Boolean = false
) {
    val previewMode = LocalInspectionMode.current
    val osdData = state.osdData
    val gpsData = osdData?.gpsData
    val srtData = state.srtData
    val videoFile = state.videoFile

    val playerState = rememberVideoPlayerState()


    var showControlOverlay by remember { mutableStateOf(showOverlayForPreview) }
    var showOsd by remember { mutableStateOf(true) }
    var followDrone by remember { mutableStateOf(true) }
    var lastTouchTs by remember { mutableStateOf(TimeSource.Monotonic.markNow()) }
    var gpsButtonState by remember(gpsData) {
        mutableStateOf(
            when {
                gpsData != null -> OverlayButtonState.ACTIVE
                previewMode -> OverlayButtonState.ACTIVE
                else -> OverlayButtonState.DISABLED
            }
        )
    }

    LaunchedEffect(state) {
        if (videoFile != null) {
            playerState.openFile(videoFile.file.platformFile(), InitialPlayerState.PLAY)
            delay(100)
            playerState.seekTo(state.currentSliderPosition)
        }
    }
    DisposableEffect(state) {
        onDispose {
            state.currentSliderPosition = playerState.sliderPos
        }
    }

    LaunchedEffect(showControlOverlay, lastTouchTs) {
        if (showControlOverlay) {
            delay(4000)
            showControlOverlay = false
        }
    }
    val controlButtonState by remember {
        derivedStateOf {
            when {
                playerState.isPlaying -> OverlayControlButtonState.PAUSE
                else -> OverlayControlButtonState.PLAY
                //VideoPlayerState.PlayBackState.ENDED -> OverlayControlButtonState.REPLAY
            }
        }
    }
    val osdState by remember(osdData) {
        derivedStateOf {
            when {
                showOsd && osdData != null -> OverlayButtonState.ACTIVE
                showOsd.not() && osdData != null -> OverlayButtonState.INACTIVE
                else -> OverlayButtonState.DISABLED
            }
        }
    }
    fun onTap() {
        log("Tap detected")
        lastTouchTs = TimeSource.Monotonic.markNow()
        showControlOverlay = showControlOverlay.not()
    }

    OsdPlayerScaffold(
        modifier = modifier.pointerInput(Unit) {
            detectTapGestures(
                onTap = {
                    onTap()
                }
            )
        },
        showControlOverlay = showControlOverlay,
        showGps = gpsButtonState == OverlayButtonState.ACTIVE,
        gpsMapSize = state.gpsMapSize,
        gpsMapOffset = state.gpsMapOffset,
        onGpsMapTransform = { offset, sizeFraction ->
            state.gpsMapOffset += offset
            state.gpsMapSize = (state.gpsMapSize + sizeFraction).coerceIn(0.1f, 0.8f)
            lastTouchTs = TimeSource.Monotonic.markNow()
        },
        videoPlayer = {
            VideoPlayer(
                playerState = playerState,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Fit
            ) {
                if (showOsd) {
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
                        Box(Modifier.fillMaxSize()) {
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
            }
        },
        gpsMap = {
            if (gpsData != null) {
                GpsView(
                    modifier = Modifier.fillMaxSize().clipToBounds().alpha(0.8f),
                    gpsData = gpsData,
                    zoomLevel = state.zoomLevel,
                    positionProvider = {
                        (playerState.currentTime * 1000.0).roundToLong() + state.videoTimeOffset
                    },
                    followDrone = followDrone,
                    onFollowDroneChange = { followDrone = it },
                    changeZoomLevel = { state.setZoom(it) }
                )
            }
            if (previewMode) {
                Image(
                    modifier = Modifier.fillMaxSize().clipToBounds().alpha(0.8f),
                    painter = painterResource(Res.drawable.preview_map),
                    contentDescription = "Map preview"
                )
            }
        },
        gpsControls = {
            if (showControlOverlay && (gpsData != null || previewMode)) {
                Row(modifier = Modifier.padding(4.dp)) {
                    OverlayActionButton(
                        icon = MaterialIconsMyLocation,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps_follow),
                        onClick = { followDrone = !followDrone },
                        inverted = followDrone
                    )
                    OverlayActionButton(
                        icon = MaterialIconsInfo,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps_info),
                        onClick = {  }
                    )
                    OverlayActionButton(
                        icon = MaterialIconsZoom_in,
                        contentDescription = "Zoom in",
                        onClick = { state.setZoom(state.zoomLevel + 1) }
                    )
                    OverlayActionButton(
                        icon = MaterialIconsZoom_out,
                        contentDescription = "Zoom out",
                        onClick = { state.setZoom(state.zoomLevel - 1) }
                    )
                }
            }
        },
        controlOverlay = {
            val progress = playerState.sliderPos / 1000f
            OsdPlayerOverlay(
                modifier = Modifier.fillMaxSize(),
                controlButtonState = controlButtonState,
                osdState = osdState,
                gpsState = gpsButtonState,
                progress = progress,
                onAction = { action ->
                    when (action) {
                        OverlayAction.PLAY_PAUSE_TOGGLE -> {
                            if (playerState.isPlaying) {
                                playerState.pause()
                            } else {
                                playerState.play()
                            }
                        }
                        OverlayAction.OSD_TOGGLE -> {
                            showOsd = showOsd.not()
                        }
                        OverlayAction.GPS_TOGGLE -> {
                            gpsButtonState = if (gpsButtonState == OverlayButtonState.ACTIVE)
                                OverlayButtonState.INACTIVE
                            else
                                OverlayButtonState.ACTIVE
                        }
                        OverlayAction.CLOSE -> {
                            onClose()
                        }
                    }
                },
                onSeek = { position ->
                    playerState.seekTo(position * 1000f)
                }
            )
        }
    )
}

@Suppress("LongMethod")
@Composable
fun OsdPlayerScaffold(
    showControlOverlay: Boolean,
    showGps: Boolean,
    gpsMapSize: Float,
    gpsMapOffset: Offset,
    onGpsMapTransform: (Offset, Float) -> Unit,
    modifier: Modifier = Modifier,
    videoPlayer: @Composable () -> Unit = {},
    gpsMap: @Composable () -> Unit = {},
    gpsControls: @Composable () -> Unit = {},
    controlOverlay: @Composable () -> Unit = {}
) {
    val mapContent = remember(gpsMap as Any) { movableContentOf(gpsMap) }
    val videoContent = remember(videoPlayer as Any) { movableContentOf(videoPlayer) }
    val controlsContent = remember(gpsControls as Any) { movableContentOf(gpsControls) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {
        videoContent()
        // Landscape mode
        if (showGps) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .padding(bottom = 32.dp)
            ) {
                Box(
                    Modifier
                        .align(Alignment.TopStart)
                        .offset {
                            IntOffset(
                                gpsMapOffset.x.roundToInt(),
                                gpsMapOffset.y.roundToInt()
                            )
                        }
                ) {
                    Column(horizontalAlignment = Alignment.End) {
                        if (gpsMapOffset.y > -200f) {
                            controlsContent()
                        }
                        Box(
                            Modifier
                                .fillMaxWidth(gpsMapSize)
                                .aspectRatio(1f)
                                .clip(RoundedCornerShape(16.dp))
                                .then(
                                    if (showControlOverlay) {
                                        Modifier
                                            .border(5.dp, Color.White, RoundedCornerShape(16.dp))
                                            .pointerInput(Unit) {
                                                detectDragGestures { change, dragAmount ->
                                                    change.consume()
                                                    onGpsMapTransform(dragAmount, 0f)
                                                }
                                                detectTapGestures {
                                                    // Consume taps to prevent them from toggling the overlay
                                                    // while interacting with the GPS view
                                                }
                                            }
                                    } else Modifier
                                )
                        ) {
                            Box(Modifier.fillMaxSize()) {
                                mapContent()
                            }

                            if (showControlOverlay) {
                                // Transparent overlay to capture all gestures when in edit mode
                                Box(
                                    Modifier
                                        .fillMaxSize()
                                        .background(Color.Transparent)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                onGpsMapTransform(dragAmount, 0f)
                                            }
                                        }
                                )
                                // Move handle (icon in center)
                                Icon(
                                    imageVector = MaterialIconsOpenWith,
                                    contentDescription = "Move",
                                    modifier = Modifier.align(Alignment.Center).size(32.dp),
                                    tint = Color.White.copy(alpha = 0.5f)
                                )

                                // Resize handle (bottom-left)
                                Box(
                                    Modifier
                                        .align(Alignment.BottomStart)
                                        .size(32.dp)
                                        .background(Color.White.copy(alpha = 0.5f), CircleShape)
                                        .pointerInput(Unit) {
                                            detectDragGestures { change, dragAmount ->
                                                change.consume()
                                                // Dragging top-left towards center decreases size
                                                // dragAmount.x > 0 means moving right (shrinking if at bottom-left)
                                                // We use the change in x to determine size change
                                                val sizeChange = -dragAmount.x / 1000f // heuristic scaling
                                                onGpsMapTransform(dragAmount, sizeChange)
                                            }
                                        }
                                )
                            }
                        }
                        if (gpsMapOffset.y <= -200f) {
                            controlsContent()
                        }
                    }
                }
            }
        }
        AnimatedVisibility(
            visible = showControlOverlay,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            controlOverlay()
        }
    }
}
