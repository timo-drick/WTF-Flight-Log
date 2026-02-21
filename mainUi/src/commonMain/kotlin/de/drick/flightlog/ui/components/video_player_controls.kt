package de.drick.flightlog.ui.components

import de.drick.flightlog.ui.icons.IconVolumeOff
import de.drick.flightlog.ui.icons.IconVolumeUp
import de.drick.flightlog.ui.icons.MaterialIconsPause
import de.drick.flightlog.ui.icons.MaterialIconsPlay_arrow
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.flightlog.ui.icons.IconSpeed
import de.drick.flightlog.ui.icons.MaterialIconsFullscreen
import io.github.kdroidfilter.composemediaplayer.PreviewableVideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun VideoPlayerControlsPreview() {
    val state = remember {
        PreviewableVideoPlayerState(
            hasMedia = true,
            durationText = "1:00"
        )
    }
    Column {
        VideoPlayer(state)
        VideoPlayerControls(state, {})
    }
}

@Composable
fun VideoPlayerControls(
    playerState: VideoPlayerState,
    onFullScreen: () -> Unit,
    modifier: Modifier = Modifier.fillMaxWidth()
) {
    Row(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilledIconButton(
            onClick = {
                if (playerState.isPlaying) playerState.pause() else playerState.play()
            },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.primaryContainer
            )
        ) {
            Icon(
                imageVector = if (playerState.isPlaying)
                    MaterialIconsPause else MaterialIconsPlay_arrow,
                contentDescription = if (playerState.isPlaying) "Pause" else "Play"
            )
        }
        SeekingControls(
            modifier = Modifier.weight(1f),
            playerState = playerState
        )
        FilledIconButton(
            onClick = { onFullScreen() },
            colors = IconButtonDefaults.filledIconButtonColors(
                containerColor = MaterialTheme.colorScheme.secondaryContainer
            )
        ) {
            Icon(MaterialIconsFullscreen, contentDescription = "Fullscreen")
        }
    }
}

@Composable
fun VolumeAndPlaybackControls(
    playerState: VideoPlayerState
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Volume control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.width(200.dp)
            ) {
                IconButton(
                    onClick = {
                        if (playerState.volume > 0f) {
                            playerState.volume = 0f
                        } else {
                            playerState.volume = 1f
                        }
                    }
                ) {
                    Icon(
                        imageVector = if (playerState.volume > 0f)
                            IconVolumeUp
                        else
                            IconVolumeOff,
                        contentDescription = "Volume",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Slider(
                    value = playerState.volume,
                    onValueChange = { playerState.volume = it },
                    valueRange = 0f..1f,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = "${(playerState.volume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }

            // Playback speed control
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            ) {
                Icon(
                    imageVector = IconSpeed,
                    contentDescription = "Playback Speed",
                    modifier = Modifier.width(50.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Slider(
                    value = playerState.playbackSpeed,
                    onValueChange = { playerState.playbackSpeed = it },
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.width(100.dp)
                )
                Text(
                    text = "${(playerState.playbackSpeed * 10).toInt() / 10.0}x",
                    style = MaterialTheme.typography.bodySmall,
                    modifier = Modifier.width(40.dp)
                )
            }
        }
    }
}

@Composable
fun SeekingControls(
    playerState: VideoPlayerState,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier) {
        Slider(
            value = playerState.sliderPos,
            onValueChange = {
                playerState.sliderPos = it
                playerState.userDragging = true
            },
            onValueChangeFinished = {
                playerState.userDragging = false
                playerState.seekTo(playerState.sliderPos)
            },
            valueRange = 0f..1000f,
            colors = SliderDefaults.colors(
                thumbColor = MaterialTheme.colorScheme.primary,
                activeTrackColor = MaterialTheme.colorScheme.primary,
                inactiveTrackColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.24f)
            )
        )
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = playerState.positionText,
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = "Current: ${playerState.currentTime.toInt()}s",
                style = MaterialTheme.typography.bodySmall
            )
            Text(
                text = playerState.durationText,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}