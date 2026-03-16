package de.drick.flightlog

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.drick.flightlog.ui.*
import de.drick.flightlog.ui.player.FullScreenPlayerPanel
import de.drick.wtf_osd.FontVariant


data class ListPaneData(val state: FlightLogState)
data class OverviewPaneData(val state: FlightLogState)
data class DetailPaneData(val itemState: LogItemState)
data class FullScreenPane(val itemState: LogItemState)
data class AircraftIdentifierPaneData(val state: FlightLogState)

@Preview(name = "Web", widthDp = 1280, heightDp = 700, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Preview(name = "Web light", widthDp = 1280, heightDp = 700, uiMode = AndroidUiModes.UI_MODE_NIGHT_NO)
@Preview(name = "Phone", widthDp = 411, heightDp = 914, uiMode = AndroidUiModes.UI_MODE_NIGHT_YES)
@Composable
private fun PreviewApp() {
    val state = remember {
        mockFlightLogState(
            isWorking = true,
            mockLogItem("Test entry 1", FontVariant.BETAFLIGHT),
            mockLogItem("Test entry 2", FontVariant.ARDUPILOT),
            mockLogItem("Test entry 3", FontVariant.INAV),
            mockLogItem("Test entry 4", FontVariant.GENERIC)
        )
    }
    val backStack = remember {
        mutableStateListOf<Any>(
            ListPaneData(state)
        )
    }
    MainScreen(state, backStack)
}

@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val flightLogState = remember {
        FlightLogStateImpl(scope)
    }
    val listPaneData = remember { ListPaneData(flightLogState) }
    val backStack = remember { mutableStateListOf<Any>(listPaneData) }

    MainScreen(listPaneData.state, backStack)
}

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun MainScreen(
    flightLogState: FlightLogState,
    backStack: MutableList<Any>,
) {
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(
            horizontalPartitionSpacerSize = MaterialTheme.panePadding()
        )
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>(directive = directive)
    val padding = if (backStack.last() is FullScreenPane) 0.dp else MaterialTheme.panePadding()
    FlightLogTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            NavDisplay(
                backStack = backStack,
                modifier = Modifier.padding(paddingValues).padding(padding),
                onBack = { backStack.removeLastOrNull() },
                sceneStrategies = listOf(listDetailStrategy),
                entryProvider = entryProvider {
                    entry<ListPaneData>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                LogItemListOverview(
                                    modifier = Modifier.padding(0.dp),
                                    state = flightLogState
                                )
                            }
                        )
                    ) { key ->
                        LogItemListPane(
                            state = key.state,
                            onLogItemClick = { logItem ->
                                val state = LogItemState(logItem)
                                if (backStack.last() is DetailPaneData) backStack.removeLast()
                                backStack.add(DetailPaneData(state))
                            },
                            onEditAircraftListClick = {
                                backStack.add(AircraftIdentifierPaneData(key.state))
                            }
                        )
                    }
                    entry<OverviewPaneData>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { key ->
                        LogItemListOverview(
                            modifier = Modifier.padding(0.dp),
                            state = key.state
                        )
                    }
                    entry<DetailPaneData>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { key ->
                        LogItemDetailPane(
                            state = key.itemState,
                            onBackClick = {
                                backStack.removeLastOrNull()
                            },
                            onFullScreenClick = {
                                backStack.add(FullScreenPane(key.itemState))
                            }
                        )
                    }
                    entry<FullScreenPane> { key ->
                        FullScreenPlayerPanel(
                            state = key.itemState,
                            onClose = {
                                backStack.removeLast()
                            }
                        )
                    }
                    entry<AircraftIdentifierPaneData>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { key ->
                        AircraftIdentifierPane(
                            state = key.state,
                            onBack = {
                                flightLogState.rescanLogItems()
                                backStack.removeLast()
                            }
                        )
                    }
                }
            )
        }
    }
}
