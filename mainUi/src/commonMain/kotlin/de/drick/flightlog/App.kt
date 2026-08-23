package de.drick.flightlog

import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.ui.unit.dp
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfoV2
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.AndroidUiModes
import androidx.compose.ui.tooling.preview.Preview
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.drick.compose.tilemap.BuildConfig
import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.ViewPortState
import de.drick.compose.tilemap.tileProviderMapBoxSat
import de.drick.flightlog.ui.AircraftIdentifierPane
import de.drick.flightlog.ui.FlightLogState
import de.drick.flightlog.ui.FlightLogStateImpl
import de.drick.flightlog.ui.LogItemDetailPane
import de.drick.flightlog.ui.LogItemListOverview
import de.drick.flightlog.ui.LogItemListPane
import de.drick.flightlog.ui.LogItemState
import de.drick.flightlog.ui.mockFlightLogState
import de.drick.flightlog.ui.player.FullScreenPlayerPanel


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
        mockFlightLogState(isWorking = true)
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
    backStack: SnapshotStateList<Any>,
    modifier: Modifier = Modifier
) {
    val scope = rememberCoroutineScope()
    val windowAdaptiveInfo = currentWindowAdaptiveInfoV2()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo).copy(
            horizontalPartitionSpacerSize = MaterialTheme.panePadding()
        )
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>(directive = directive)
    val padding = if (backStack.last() is FullScreenPane) 0.dp else MaterialTheme.panePadding()
    FlightLogTheme {
        Scaffold(
            modifier = modifier,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            SharedTransitionLayout {
                NavDisplay(
                    modifier = Modifier.padding(paddingValues).padding(padding),
                    backStack = backStack,
                    onBack = { backStack.removeLastOrNull() },
                    sceneStrategies = listOf(listDetailStrategy),
                    sharedTransitionScope = this,
                    transitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { it },
                            animationSpec = tween(500)
                        ) + fadeIn(animationSpec = tween(500)) togetherWith slideOutHorizontally(
                            targetOffsetX = { -it },
                            animationSpec = tween(500)
                        ) + fadeOut(animationSpec = tween(500))
                    },
                    popTransitionSpec = {
                        slideInHorizontally(
                            initialOffsetX = { -it },
                            animationSpec = tween(500)
                        ) + fadeIn(animationSpec = tween(500)) togetherWith slideOutHorizontally(
                            targetOffsetX = { it },
                            animationSpec = tween(500)
                        ) + fadeOut(animationSpec = tween(500))
                    },
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
                                    val viewPortState = ViewPortState(
                                        scope = scope,
                                        initialZoom = 17f,
                                        initialPos = GeoPoint(0.0, 0.0),
                                        tileSize = 256,
                                        tileProviderMapBoxSat(BuildConfig.MAPBOX_TOKEN)
                                    )
                                    val state = LogItemState(logItem, viewPortState)
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
                        entry<FullScreenPane>(
                            metadata = androidx.navigation3.runtime.metadata {
                                put(NavDisplay.TransitionKey) {
                                    scaleIn(
                                        initialScale = 0.8f,
                                        animationSpec = tween(500)
                                    ) + fadeIn(
                                        animationSpec = tween(500)
                                    ) togetherWith fadeOut(
                                        animationSpec = tween(500)
                                    )
                                }
                                put(NavDisplay.PopTransitionKey) {
                                    fadeIn(
                                        animationSpec = tween(500)
                                    ) togetherWith scaleOut(
                                        targetScale = 0.8f,
                                        animationSpec = tween(500)
                                    ) + fadeOut(animationSpec = tween(500))
                                }
                            }
                        ) { key ->
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
                                    flightLogState.rescanLogItems(true)
                                    backStack.removeLast()
                                }
                            )
                        }
                    }
                )
            }
        }
    }
}
