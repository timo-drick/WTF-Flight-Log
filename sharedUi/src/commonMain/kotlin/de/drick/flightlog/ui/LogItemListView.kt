package de.drick.flightlog.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.FontFile
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.VideoFile
import de.drick.flightlog.ui.icons.BootstrapFile
import de.drick.flightlog.ui.icons.BootstrapFileFont
import de.drick.flightlog.ui.icons.MaterialSymbolsVideo_file
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.format
import kotlinx.datetime.format.DateTimeComponents
import kotlinx.datetime.format.char
import kotlinx.datetime.toLocalDateTime


val dateTimeFormat = LocalDateTime.Format {
    date(LocalDate.Formats.ISO)
    char(' ')
    hour()
    char(':')
    minute()
}


val groupFormat = DateTimeComponents.Format {
    date(LocalDate.Formats.ISO)
}

@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemList() {
    val testLogItem = mockLogItem("Test entry 1")
    val testLogItem2 = mockLogItem("Test entry 2")
    val list = persistentListOf(testLogItem, testLogItem2)
    BasePreview {
        LogItemListView(
            modifier = Modifier.fillMaxSize(),
            logList = list,
            onLogItemClick = {}
        )
    }
}

val cornerRadius = 8.dp

@Composable
fun LogItemListView(
    logList: ImmutableList<LogItem>,
    onLogItemClick: (LogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val groups = remember(logList) {
        logList.sortedByDescending { it.lastModified }
            .groupBy { it.files.firstOrNull()?.lastModified?.format(groupFormat) }
    }
    LazyColumn(modifier = modifier) {
        item {
            Text(
                modifier = Modifier.semantics { heading() },
                color = MaterialTheme.colorScheme.onSurface,
                text = "Log entries",
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                style = MaterialTheme.typography.headlineMedium
            )
        }
        groups.forEach { (group, list) ->
            stickyHeader(group, "Header") {
                val cornerShape = RoundedCornerShape(
                    topStart = cornerRadius,
                    topEnd = cornerRadius
                )
                Surface(shape = cornerShape) {
                    Text(
                        modifier = Modifier.padding(8.dp).fillMaxWidth(),
                        text = group ?: "-",
                        //color = MaterialTheme.colors.onBackground
                    )
                }
            }
            itemsIndexed(list) { index, logEntry ->
                if (index > 0) HorizontalDivider()
                LogItemRow(
                    logEntry = logEntry,
                    onClick = { onLogItemClick(logEntry) }
                )
            }
        }
    }
}



@Preview(widthDp = 200, heightDp = 100, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(widthDp = 200, heightDp = 100, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemRow() {
    val testLogItem = mockLogItem("Test entry 2")
    BasePreview {
        LogItemRow(
            logEntry = testLogItem,
            onClick = {}
        )
    }
}

fun FileItem.icon() = when (this) {
    is VideoFile -> MaterialSymbolsVideo_file
    is OSDFile -> BootstrapFileFont
    is BaseFile -> BootstrapFile
    //is ErrorFile -> TODO()
    is FontFile -> BootstrapFileFont
    else -> MaterialSymbolsVideo_file
}

@Composable
private fun LogItemRow(
    logEntry: LogItem,
    onClick: () -> Unit
) {
    val icons = remember(logEntry) {
        logEntry.files.map { it.icon() }
    }
    Row(verticalAlignment = Alignment.CenterVertically) {
        icons.fastForEach {
            Icon(
                modifier = Modifier.size(32.dp),
                imageVector = it,
                contentDescription = null
            )
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(16.dp)
        ) {
            Text(
                text = logEntry.name,
                style = MaterialTheme.typography.titleMedium
            )
            val lastModified = remember(logEntry) {
                val instant = logEntry.files.firstOrNull()?.lastModified?.let {
                    it.toLocalDateTime(TimeZone.currentSystemDefault()).format(dateTimeFormat)
                }
                instant?.toString() ?: "-"
            }
            Text(
                text = "Last modified: $lastModified",
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
