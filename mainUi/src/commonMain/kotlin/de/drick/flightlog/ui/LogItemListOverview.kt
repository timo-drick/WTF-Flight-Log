package de.drick.flightlog.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import de.drick.flightlog.FlightLogState
import de.drick.wtf_osd.FontVariant

@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(heightDp = 300, widthDp = 400, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Composable
private fun PreviewLogItemList() {
    val testState = remember {
        FlightLogState().apply {
            addItem(mockLogItem("Test entry 2", FontVariant.ARDUPILOT))
            addItem(mockLogItem("Test entry 1", FontVariant.BETAFLIGHT))
            addItem(mockLogItem("Test entry 3", FontVariant.INAV))
            addItem(mockLogItem("Test entry 4", FontVariant.GENERIC))

        }
    }
    BasePreview {
        LogItemListOverview(
            modifier = Modifier.fillMaxSize(),
            state = testState
        )
    }
}

@Composable
fun LogItemListOverview(
    state: FlightLogState,
    modifier: Modifier = Modifier
) {
    Box(modifier) {
        Text("Log entries: ${state.entryCount}")
    }
}