package de.drick.flightlog.ui.player

import de.drick.flightlog.ui.icons.MaterialIconsInfo
import de.drick.flightlog.ui.icons.MaterialIconsMap
import de.drick.flightlog.ui.icons.MaterialIconsPause
import de.drick.flightlog.ui.icons.MaterialIconsPlay_arrow
import de.drick.flightlog.ui.icons.MaterialIconsReplay
import de.drick.flightlog.ui.icons.MaterialIconsZoom_in
import de.drick.flightlog.ui.icons.MaterialIconsZoom_out
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drick.flightlog.ui.BasePreview
import de.drick.flightlog.ui.icons.MaterialIconsClose
import wtfflightlog.mainui.generated.resources.Res
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps_info
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps_zoom_in
import wtfflightlog.mainui.generated.resources.screen_osd_player_gps_zoom_out
import wtfflightlog.mainui.generated.resources.screen_osd_player_osd
import wtfflightlog.mainui.generated.resources.screen_osd_player_play_pause
import org.jetbrains.compose.resources.stringResource
import wtfflightlog.mainui.generated.resources.screen_osd_player_close

@Preview(widthDp = 1280, heightDp = 720)
@Composable
private fun PreviewOverlay16_9GPS() {
    BasePreview {
        Scaffold {
            OsdPlayerOverlay(
                modifier = Modifier.fillMaxSize(),
                controlButtonState = OverlayControlButtonState.PLAY,
                osdState = OverlayButtonState.ACTIVE,
                gpsState = OverlayButtonState.ACTIVE,
                progress = 0.5f,
                onAction = {},
                onSeek = {}
            )
        }
    }
}

private val gray900 = Color(0xFF333333)

private val textColor = Color.White
private val shadowColor = gray900
private val backgroundColor = gray900.copy(alpha = 0.5f)

enum class OverlayControlButtonState(val icon: ImageVector) {
    PLAY(MaterialIconsPlay_arrow),
    PAUSE(MaterialIconsPause),
    REPLAY(MaterialIconsReplay)
}

enum class OverlayButtonState {
    ACTIVE, INACTIVE, DISABLED
}

enum class OverlayAction {
    PLAY_PAUSE_TOGGLE,
    OSD_TOGGLE,
    GPS_TOGGLE,
    GPS_INFO,
    GPS_ZOOM_IN,
    GPS_ZOOM_OUT,
    CLOSE
}

@Composable
fun OsdPlayerOverlay(
    controlButtonState: OverlayControlButtonState,
    osdState: OverlayButtonState,
    gpsState: OverlayButtonState,
    progress: Float,
    onAction: (OverlayAction) -> Unit,
    onSeek: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        OverlayActionButton(
            modifier = Modifier.align(Alignment.Center),
            icon = controlButtonState.icon,
            contentDescription = stringResource(Res.string.screen_osd_player_play_pause),
            onClick = { onAction(OverlayAction.PLAY_PAUSE_TOGGLE) }
        )
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(horizontal = 36.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(24.dp),
        ) {
            OverlayActionButton(
                icon = MaterialIconsClose,
                contentDescription = stringResource(Res.string.screen_osd_player_close),
                onClick = {
                    onAction(OverlayAction.CLOSE)
                }
            )
            OverlayStateButton(
                modifier = Modifier
                    .clickable(onClick = { onAction(OverlayAction.OSD_TOGGLE) })
                    .size(64.dp),
                state = osdState
            ) {
                Text(
                    modifier = Modifier.padding(2.dp),
                    text = stringResource(Res.string.screen_osd_player_osd),
                    fontSize = 22.sp
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OverlayStateButton(
                    modifier = Modifier
                        .clickable(onClick = { onAction(OverlayAction.GPS_TOGGLE) })
                        .size(64.dp)
                    ,
                    state = gpsState,
                ) {
                    Icon(
                        imageVector = MaterialIconsMap,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps)
                    )
                }
                if (gpsState == OverlayButtonState.ACTIVE) {
                    OverlayActionButton(
                        icon = MaterialIconsInfo,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps_info),
                        onClick = { onAction(OverlayAction.GPS_INFO) }
                    )
                    OverlayActionButton(
                        icon = MaterialIconsZoom_out,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps_zoom_out),
                        onClick = { onAction(OverlayAction.GPS_ZOOM_OUT) }
                    )
                    OverlayActionButton(
                        icon = MaterialIconsZoom_in,
                        contentDescription = stringResource(Res.string.screen_osd_player_gps_zoom_in),
                        onClick = { onAction(OverlayAction.GPS_ZOOM_IN) }
                    )
                }
            }
            Slider(
                value = progress,
                onValueChange = {
                    onSeek(it)
                    //playerState.sliderPos = it
                    //playerState.userDragging = true
                },
                /*onValueChangeFinished = {
                    playerState.userDragging = false
                    playerState.seekTo(playerState.sliderPos)
                },*/
                colors = SliderDefaults.colors(
                    thumbColor = textColor,
                    activeTrackColor = textColor
                )
            )
        }
    }
}

@Composable
fun OverlayActionButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    inverted: Boolean = false
) {
    val foregroundColor = if (inverted.not()) textColor else backgroundColor
    val backgroundColor = if (inverted.not()) backgroundColor else textColor
    IconButton(
        modifier = modifier
            .size(64.dp)
            .background(
                color = backgroundColor,
                shape = CircleShape
            ),
        onClick = onClick,
    ) {
        Icon(
            modifier = Modifier.size(48.dp),
            imageVector = icon,
            contentDescription = contentDescription,
            tint = foregroundColor
        )
    }
}

@Composable
fun OverlayStateButton(
    state: OverlayButtonState,
    modifier: Modifier = Modifier,
    icon: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(4.dp)
    val active = state == OverlayButtonState.ACTIVE
    val bgColor = if (active) textColor else backgroundColor
    val contentColor = if (active) shadowColor else textColor
    if (state != OverlayButtonState.DISABLED) {
        Box(
            modifier = modifier
                .border(width = 1.dp, color = textColor, shape = shape)
                .background(color = bgColor, shape = shape)
                .padding(4.dp),
            contentAlignment = Alignment.Center
        ) {
            CompositionLocalProvider(LocalContentColor provides contentColor) {
                icon()
            }
        }
    }
}
