package de.drick.flightlog.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Column
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.tooling.preview.Preview
import flightlog.mainui.generated.resources.Res
import flightlog.mainui.generated.resources.video_thumb1
import io.github.kdroidfilter.composemediaplayer.PreviewableVideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerState
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import org.jetbrains.compose.resources.painterResource

@Preview(widthDp = 400, heightDp = 400)
@Composable
private fun VideoPlayerPreview() {
    val state = remember {
        PreviewableVideoPlayerState(
            hasMedia = true,
            durationText = "1:00"
        )
    }
    Column {
        VideoPlayer(state)
        VolumeAndPlaybackControls(state)
    }
}

@Composable
fun VideoPlayer(
    playerState: VideoPlayerState,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Fit,
    overlay: @Composable () -> Unit = {}
) {
    if (LocalInspectionMode.current) {
        Image(
            modifier = modifier,
            painter = painterResource(Res.drawable.video_thumb1),
            contentDescription = null
        )
    } else {
        VideoPlayerSurface(playerState, modifier, contentScale, overlay)
    }
}