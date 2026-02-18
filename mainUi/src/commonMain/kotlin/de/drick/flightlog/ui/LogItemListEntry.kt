package de.drick.flightlog.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.FontFile
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.ui.icons.BootstrapFile
import de.drick.flightlog.ui.icons.BootstrapFileFont
import de.drick.flightlog.ui.icons.MaterialIconsMovie
import de.drick.wtf_osd.FontVariant
import wtfflightlog.mainui.generated.resources.Res
import wtfflightlog.mainui.generated.resources.ardupilot_icon
import wtfflightlog.mainui.generated.resources.betaflight_icon
import wtfflightlog.mainui.generated.resources.inav_icon
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime
import org.jetbrains.compose.resources.painterResource

@Preview(widthDp = 200, heightDp = 100, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(widthDp = 200, heightDp = 100, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemView() {
    val testLogItem = mockLogItem("Test entry 2", FontVariant.KISS_ULTRA)
    BasePreview {
        LogItemView(
            logEntry = testLogItem,
            onClick = {}
        )
    }
}


private val dateTimeFormat = LocalDateTime.Format {
    hour()
    char(':')
    minute()
    char(':')
    second()
}

fun FileItem.icon() = when (this) {
    is VideoFile -> MaterialIconsMovie
    is OSDFile -> BootstrapFileFont
    is BaseFile -> BootstrapFile
    //is ErrorFile -> TODO()
    is FontFile -> BootstrapFileFont
    else -> BootstrapFile
}

@Composable
fun LogItemView(
    modifier: Modifier = Modifier,
    logEntry: LogItem,
    onClick: () -> Unit
) {
    val iconSize = 48.dp
    val fcIcon = remember(logEntry) {
        val osdFile = logEntry.files.filterIsInstance<OSDFile>().firstOrNull()
        when (osdFile?.fontVariant) {
            FontVariant.BETAFLIGHT -> Res.drawable.betaflight_icon
            FontVariant.INAV -> Res.drawable.inav_icon
            FontVariant.ARDUPILOT -> Res.drawable.ardupilot_icon
            else -> null
        }
    }
    val fallbackItem = remember(logEntry) {
        val videoFile = logEntry.files.filterIsInstance<VideoFile>().firstOrNull()
        videoFile?.icon() ?: logEntry.files.firstOrNull()?.icon() ?: BootstrapFile
    }

    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (fcIcon != null) {
            Image(
                modifier = Modifier.size(iconSize),
                painter = painterResource(fcIcon),
                contentDescription = "Flight controller logo"
            )
        } else {
            Icon(
                modifier = Modifier.size(iconSize),
                imageVector = fallbackItem,
                contentDescription = "Flight controller logo"
            )
        }
        Column(
            modifier = Modifier
                .weight(1f)
                .clickable(onClick = onClick)
        ) {
            Text(
                text = logEntry.name,
                style = MaterialTheme.typography.titleMedium
            )
            val lastModified: String = remember(logEntry) {
                logEntry.lastModified
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())
                    ?.format(dateTimeFormat)
                    ?: "NA"
            }
            Text(
                text = lastModified,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            val types = logEntry.files.joinToString { it.extension }
            Text(
                text = "Files: $types",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
