package de.drick.flightlog.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

data class TableColumn<T>(
    val header: String,
    val weight: Float,
    val content: @Composable (T, TextStyle, Modifier) -> Unit
)

@Composable
fun <T> Table(
    items: List<T>,
    columns: List<TableColumn<T>>,
    modifier: Modifier = Modifier,
    headerStyle: TextStyle = MaterialTheme.typography.labelLarge,
    rowStyle: (T) -> TextStyle = { TextStyle.Default },
    divider: @Composable () -> Unit = { HorizontalDivider(thickness = 0.5.dp) },
    footer: @Composable (() -> Unit)? = null
) {
    val defaultRowStyle = MaterialTheme.typography.bodyMedium
    val finalRowStyle: (T) -> TextStyle = {
        val style = rowStyle(it)
        if (style == TextStyle.Default) defaultRowStyle else style
    }
    Column(modifier = modifier) {
        // Header
        Row(
            modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            columns.forEachIndexed { index, column ->
                Text(
                    text = column.header,
                    modifier = Modifier.weight(column.weight),
                    style = headerStyle
                )
                if (index < columns.size - 1) {
                    VerticalDivider(Modifier.height(16.dp))
                }
            }
        }
        HorizontalDivider()

        // Rows
        items.forEach { item ->
            val style = finalRowStyle(item)
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                columns.forEachIndexed { index, column ->
                    column.content(item, style, Modifier.weight(column.weight))
                    if (index < columns.size - 1) {
                        VerticalDivider(Modifier.height(16.dp))
                    }
                }
            }
            divider()
        }

        // Footer
        footer?.invoke()
    }
}
