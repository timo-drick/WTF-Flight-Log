package de.drick.flightlog.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.drick.flightlog.FlightLogState
import de.drick.flightlog.cornerRadius
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.panePadding
import de.drick.flightlog.ui.components.ScrollBar
import de.drick.flightlog.ui.components.SuspendButton
import de.drick.wtf_osd.FontVariant

@Preview(heightDp = 600, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(heightDp = 600, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemList() {
    val state = remember {
        FlightLogState().apply {
            addItem(mockLogItem("Test entry 1", FontVariant.BETAFLIGHT))
            addItem(mockLogItem("Test entry 2", FontVariant.ARDUPILOT))
            addItem(mockLogItem("Test entry 3", FontVariant.INAV))
            addItem(mockLogItem("Test entry 4", FontVariant.GENERIC))
        }
    }
    BasePreview {
        LogItemListPane(
            modifier = Modifier.fillMaxSize(),
            state = state,
            onLogItemClick = {}
        )
    }
}

@Composable
fun LogItemListPane(
    state: FlightLogState,
    onLogItemClick: (LogItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val cornerRadius = MaterialTheme.cornerRadius()
    Box(modifier) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = state.lazyListState,
            contentPadding = PaddingValues(MaterialTheme.panePadding())
        ) {
            item {
                SuspendButton(onClick = {
                    state.import()
                }) {
                    Text("Import files")
                }
            }
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
            state.groups.forEach { (group, list) ->
                stickyHeader(group, "Header") {
                    val cornerShape = RoundedCornerShape(
                        topStart = cornerRadius,
                        topEnd = cornerRadius
                    )
                    Surface(
                        modifier = Modifier.background(MaterialTheme.colorScheme.background),
                        shape = cornerShape,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        Box {
                            Text(
                                modifier = Modifier.padding(8.dp).fillMaxWidth(),
                                text = group ?: "-",
                                //color = MaterialTheme.colors.onBackground
                            )
                            HorizontalDivider(Modifier.align(Alignment.BottomStart))
                        }
                    }
                }
                itemsIndexed(list) { index, logEntry ->
                    val cornerShape = when (index) {
                        list.size - 1 -> RoundedCornerShape(
                            bottomStart = cornerRadius,
                            bottomEnd = cornerRadius
                        )

                        else -> RectangleShape
                    }
                    Surface(
                        shape = cornerShape,
                        color = MaterialTheme.colorScheme.surfaceContainer
                    ) {
                        if (index > 0)
                            HorizontalDivider(Modifier.padding(start = 42.dp))

                        LogItemView(
                            modifier = Modifier.padding(horizontal = 8.dp),
                            logEntry = logEntry,
                            onClick = { onLogItemClick(logEntry) }
                        )
                    }
                }
                item {
                    Spacer(Modifier.height(8.dp))
                }
            }
        }
        ScrollBar(
            lazyListState = state.lazyListState,
        )
    }
}
