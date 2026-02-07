package de.drick.flightlog

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import de.drick.flightlog.file.LogItem
import kotlinx.collections.immutable.ImmutableList

@Composable
fun LogItemListView(
    logList: ImmutableList<LogItem>,
    onLogItemClick: (LogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyColumn(modifier = modifier) {
        items(logList) { logEntry ->
            LogItemRow(
                logEntry = logEntry,
                onClick = { onLogItemClick(logEntry) }
            )
            HorizontalDivider()
        }
    }
}

@Composable
private fun LogItemRow(
    logEntry: LogItem,
    onClick: () -> Unit
) {
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
        val types = logEntry.files.joinToString { it.extension }
        Text(
            text = "Files: $types",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
