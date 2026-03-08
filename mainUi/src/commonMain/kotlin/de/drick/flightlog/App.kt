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
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.drick.flightlog.ui.*
import de.drick.flightlog.ui.player.FullScreenPlayerPanel


data class ListPaneData(val state: FlightLogState)
data class OverviewPaneData(val state: FlightLogState)
data class DetailPaneData(val itemState: LogItemState)
data class FullScreenPane(val itemState: LogItemState)
data class AircraftIdentifierPaneData(val state: FlightLogState)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()
    val flightLogState = remember {
        FlightLogStateImpl(scope)
    }
    val listPaneData = remember { ListPaneData(flightLogState) }
    val backStack = remember { mutableStateListOf<Any>(listPaneData) }
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
                modifier = Modifier.padding(paddingValues).padding(padding),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<ListPaneData>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                LogItemListOverview(
                                    modifier = Modifier.padding(0.dp),
                                    state = listPaneData.state
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
