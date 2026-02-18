package de.drick.flightlog

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.adaptive.ExperimentalMaterial3AdaptiveApi
import androidx.compose.material3.adaptive.currentWindowAdaptiveInfo
import androidx.compose.material3.adaptive.layout.calculatePaneScaffoldDirective
import androidx.compose.material3.adaptive.navigation3.ListDetailSceneStrategy
import androidx.compose.material3.adaptive.navigation3.rememberListDetailSceneStrategy
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.ui.NavDisplay
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.analyzeFlow
import de.drick.flightlog.ui.player.FullScreenPlayer
import de.drick.flightlog.ui.LogItemDetailView
import de.drick.flightlog.ui.LogItemListOverview
import de.drick.flightlog.ui.LogItemListView
import de.drick.flightlog.ui.LogItemState
import de.drick.flightlog.ui.formatLocalized
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.coroutines.launch
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)
fun PlatformFile.toFileItem() = BaseFile(this)


class FlightLogState {
    val listState = LazyListState()
    var groups: Map<String?, List<LogItem>> by mutableStateOf(emptyMap())
        private set
    var entryCount by mutableStateOf(0)
        private set

    private val logList = mutableStateListOf<LogItem>()

    fun addItem(item: LogItem) {
        logList.add(item)
        entryCount = logList.size
        updateList()
    }

    private fun updateList() {
        groups = logList.sortedByDescending { it.lastModified }
            .groupBy {
                it.files.firstOrNull()?.lastModified
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    ?.formatLocalized()
            }
    }
}

data class ListPane(val state: FlightLogState)
data class DetailPane(val itemState: LogItemState)
data class FullScreenPane(val itemState: LogItemState)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    val scope = rememberCoroutineScope()

    val listPane = remember { ListPane(FlightLogState()) }
    val backStack = remember { mutableStateListOf<Any>(listPane) }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>(directive = directive)

    FlightLogTheme {
        Scaffold { paddingValues ->
            NavDisplay(
                modifier = Modifier.padding(paddingValues),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<ListPane>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                LogItemListOverview(
                                    state = listPane.state
                                )
                            }
                        )
                    ) { key ->
                        Column(
                            modifier = Modifier.fillMaxSize(),
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            Button(onClick = {
                                scope.launch {
                                    FileKit.openFilePicker(mode = FileKitMode.Multiple())
                                        ?.let { selectedFiles ->
                                            val fileItemList = selectedFiles
                                                .map { it.toFileItem() }
                                            log("Analyzing files: $fileItemList")
                                            fileItemList.analyzeFlow().collect { item ->
                                                key.state.addItem(item)
                                            }
                                        }
                                }
                            }) {
                                Text("Import files")
                            }
                            LogItemListView(
                                state = key.state,
                                onLogItemClick = { logItem ->
                                    val state = LogItemState(logItem)
                                    if (backStack.last() is DetailPane) backStack.removeLast()
                                    backStack.add(DetailPane(state))
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    entry<DetailPane>(
                        metadata = ListDetailSceneStrategy.detailPane()
                    ) { key ->
                        LogItemDetailView(
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
                        FullScreenPlayer(key.itemState)
                    }
                }
            )
        }
    }
}
