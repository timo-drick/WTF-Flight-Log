package de.drick.flightlog.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.drick.core.log
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.analyzeFlow
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
