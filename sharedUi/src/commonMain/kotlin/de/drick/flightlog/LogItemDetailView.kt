package de.drick.flightlog

import OsdCanvasView
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.ui.BasePreview
import de.drick.flightlog.ui.mockLogItem
import de.drick.wtf_osd.OsdFont
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.loadOsdFont
import de.drick.wtf_osd.parseOsdFile
import io.github.kdroidfilter.composemediaplayer.VideoPlayerSurface
import io.github.kdroidfilter.composemediaplayer.rememberVideoPlayerState
import kotlin.math.roundToLong

data class OsdData(
    val font: OsdFont,
    val record: OsdRecord,
)

@Preview(
    widthDp = 1200, heightDp = 600,
    uiMode = AndroidUiModes.UI_MODE_NIGHT_YES
)
@Composable
private fun PreviewLogItemDetail() {
    val testLogItem = mockLogItem("Test entry 2")
    BasePreview {
        LogItemDetailView(
            logItem = testLogItem,
            onBackClick = {},
            modifier = Modifier.fillMaxSize()
        )
    }
}

@Composable
fun LogItemDetailView(
    logItem: LogItem,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val playerState = rememberVideoPlayerState()

    val videoFile = logItem.files.filterIsInstance<VideoFile>().firstOrNull()
    var osdData : OsdData? by remember(logItem) {
        mutableStateOf(null)
    }

    LaunchedEffect(logItem) {
        osdData = logItem.files
            .filterIsInstance<OSDFile>()
            .firstOrNull()
            ?.let { osdFile ->
                when (val result = parseOsdFile(osdFile.source())) {
                    is ParseResult.Error -> TODO()
                    is ParseResult.Success -> {
                        val font = loadOsdFont(osdFile.fontVariant)
                        OsdData(font, result.record)
                    }
                }
            }

    }

    Column(modifier = modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            TextButton(onClick = onBackClick) {
                Text("< Back")
            }
            Text(
                text = logItem.name,
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier.padding(start = 8.dp)
            )
        }

        HorizontalDivider()

        if (videoFile != null) {
            val baseFile = videoFile.file as? BaseFile
            if (baseFile != null) {
                LaunchedEffect(baseFile.file) {
                    playerState.openFile(baseFile.file)
                }
                VideoPlayerSurface(
                    playerState = playerState,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(16f / 9f)
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
                }
            }
        }

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {
            item {
                Text(
                    text = "Associated Files",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(16.dp)
                )
            }
            items(logItem.files.toList()) { fileItem ->
                FileItemRow(fileItem)
                HorizontalDivider(modifier = Modifier.padding(horizontal = 16.dp))
            }
        }
    }
}

@Composable
private fun FileItemRow(fileItem: FileItem) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Text(
            text = "${fileItem.name}.${fileItem.extension}",
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Size: ${fileItem.size}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

