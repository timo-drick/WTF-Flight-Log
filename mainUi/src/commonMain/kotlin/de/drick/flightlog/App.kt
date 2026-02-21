package de.drick.flightlog

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.lazy.LazyListState
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
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.analyzeFlow
import de.drick.flightlog.ui.player.FullScreenPlayerPanel
import de.drick.flightlog.ui.LogItemDetailPane
import de.drick.flightlog.ui.LogItemListOverview
import de.drick.flightlog.ui.LogItemListPane
import de.drick.flightlog.ui.LogItemState
import de.drick.flightlog.ui.formatLocalized
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.yield
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime


fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)
fun PlatformFile.toFileItem() = BaseFile(this)


class FlightLogState {
    val lazyListState = LazyListState()
    var list: List<LogItem> by mutableStateOf(emptyList())
        private set
    var groups: Map<String?, List<LogItem>> by mutableStateOf(emptyMap())
        private set
    var entryCount by mutableStateOf(0)
        private set

    private val logList = mutableListOf<LogItem>()

    suspend fun import() {
        FileKit.openFilePicker(mode = FileKitMode.Multiple())
            ?.let { selectedFiles ->
                addFiles(selectedFiles)
            }
    }

    private suspend fun addFiles(fileList: List<PlatformFile>) {
        log("Analyzing files: $fileList")
        val fileItemList = fileList
            .map { it.toFileItem() }
            .sortedByDescending { it.lastModified }
        fileItemList.analyzeFlow().collect { item ->
            addItem(item)
            yield()
        }
    }

    fun addItem(item: LogItem) {
        logList.add(item)
        entryCount = logList.size
        updateList()
    }

    private fun updateList() {
        list = logList.toPersistentList()
        groups = list
            .sortedByDescending { it.lastModified }
            .groupBy {
                it.files.firstOrNull()?.lastModified
                    ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
                    ?.formatLocalized()
            }
    }
}

data class ListPaneData(val state: FlightLogState)
data class DetailPaneData(val itemState: LogItemState)
data class FullScreenPane(val itemState: LogItemState)

@OptIn(ExperimentalMaterial3AdaptiveApi::class)
@Composable
fun App() {
    val listPaneData = remember { ListPaneData(FlightLogState()) }
    val backStack = remember { mutableStateListOf<Any>(listPaneData) }
    val windowAdaptiveInfo = currentWindowAdaptiveInfo()
    val directive = remember(windowAdaptiveInfo) {
        calculatePaneScaffoldDirective(windowAdaptiveInfo)
    }
    val listDetailStrategy = rememberListDetailSceneStrategy<Any>(directive = directive)

    FlightLogTheme {
        Scaffold(
            contentWindowInsets = WindowInsets.safeDrawing
        ) { paddingValues ->
            NavDisplay(
                modifier = Modifier.padding(paddingValues),
                backStack = backStack,
                onBack = { backStack.removeLastOrNull() },
                sceneStrategy = listDetailStrategy,
                entryProvider = entryProvider {
                    entry<ListPaneData>(
                        metadata = ListDetailSceneStrategy.listPane(
                            detailPlaceholder = {
                                LogItemListOverview(
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
                            }
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
                }
            )
        }
    }
}
