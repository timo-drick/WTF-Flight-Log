package de.drick.flightlog.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.flightlog.SrtTextStyle
import de.drick.flightlog.ui.BasePreview
import de.drick.flightlog.ui.icons.IconSpeed
import de.drick.flightlog.ui.icons.LucideHourglass
import de.drick.flightlog.ui.icons.LucideRectangleGoggles
import de.drick.wtf_osd.SrtData
import de.drick.wtf_osd.SrtFrame
import kotlinx.coroutines.isActive

@Preview
@Composable
private fun PreviewSrtView() {
    val data = remember {
        SrtData(
            frames = listOf(
                SrtFrame(0, 0, 100, 22, 25f, "10"),
                SrtFrame(1, 101, 200, 23, 20f, "9")
            )
        )
    }
    BasePreview {
        SrtOverlayView(
            srtData = data,
            positionProvider = { 10 }
        )
    }
}

@Composable
fun SrtOverlayView(
    srtData: SrtData,
    positionProvider: () -> Long,
    modifier: Modifier = Modifier
) {
    var frame: SrtFrame? by remember { mutableStateOf(null) }
    LaunchedEffect(srtData) {
        val frameIterator = srtData.frames.listIterator()
        if (!frameIterator.hasNext()) return@LaunchedEffect
        var currentFrame = frameIterator.next()
        while (isActive) {
            withFrameMillis {
                val videoPositionMillis = positionProvider()
                // Seek forward while the current frame already ended
                while (currentFrame.endTimeMs.toLong() <= videoPositionMillis && frameIterator.hasNext()) {
                    currentFrame = frameIterator.next()
                }
                // Seek backward while the current frame starts after position
                while (currentFrame.startTimeMs.toLong() > videoPositionMillis && frameIterator.hasPrevious()) {
                    currentFrame = frameIterator.previous()
                }
                if (currentFrame != frame) {
                    frame = currentFrame
                }
            }
        }
    }

    frame?.let { f ->
        Row(
            modifier = modifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            val bitrateMbps = f.bitrateMbps
            if (bitrateMbps != null) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(
                        imageVector = IconSpeed,
                        contentDescription = "Bitrate",
                        tint = SrtTextStyle.color,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = "${(kotlin.math.round(bitrateMbps * 10f) / 10f)} Mbps",
                        style = SrtTextStyle
                    )
                }
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = LucideHourglass,
                    contentDescription = "Delay",
                    tint = SrtTextStyle.color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = "${f.delayMs} ms",
                    style = SrtTextStyle
                )
            }
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Icon(
                    imageVector = LucideRectangleGoggles,
                    contentDescription = "Google Battery",
                    tint = SrtTextStyle.color,
                    modifier = Modifier.size(18.dp)
                )
                Text(
                    text = f.glsBat ?: "NA",
                    style = SrtTextStyle
                )
            }
        }
    }
}

