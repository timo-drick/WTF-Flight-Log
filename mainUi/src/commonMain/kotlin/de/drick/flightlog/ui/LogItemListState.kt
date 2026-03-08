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
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.flightlog.localStorage.AircraftIdentifierDB
import io.github.vinceglb.filekit.FileKit
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.dialogs.FileKitMode
import io.github.vinceglb.filekit.dialogs.openFilePicker
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)
fun PlatformFile.toFileItem() = BaseFile(this)


class FlightLogState(
    private val scope: CoroutineScope
) {
    private val aircraftDB = AircraftIdentifierDB()
    val lazyListState = LazyListState()

    private val platformFileList = mutableListOf<PlatformFile>()

    var list: List<LogItem> by mutableStateOf(emptyList())
        private set
    var groups: Map<String?, List<LogItem>> by mutableStateOf(emptyMap())
        private set
    var entryCount by mutableStateOf(0)
        private set

    var aircraftIdentifierList by mutableStateOf(emptyList<AircraftIdentifier>())
        private set

    private val logList = mutableListOf<LogItem>()

    init {
        updateAircraftList()
    }

    private fun updateAircraftList() {
        aircraftIdentifierList = aircraftDB.loadAll()
    }

    fun rescanLogItems() {
        scope.launch {
            logList.clear()
            val fileItemList = platformFileList
                .map { it.toFileItem() }
                .sortedByDescending { it.lastModified }
            fileItemList.analyzeFlow(aircraftIdentifierList).collect { item ->
                addItem(item)
                yield()
            }
        }
    }

    fun addAircraft(aircraftIdentifier: AircraftIdentifier) {
        aircraftDB.addAircraft(aircraftIdentifier)
        updateAircraftList()
    }
    fun removeAircraft(aircraftIdentifier: AircraftIdentifier) {
        aircraftDB.removeAircraft(aircraftIdentifier)
        updateAircraftList()
    }

    suspend fun import() {
        FileKit.openFilePicker(mode = FileKitMode.Multiple())
            ?.let { selectedFiles ->
                addFiles(selectedFiles)
            }
    }

    private fun addFiles(fileList: List<PlatformFile>) {
        log("Analyzing files: $fileList")
        val newFiles = fileList
            .filterNot { platformFileList.contains(it) }
        platformFileList.addAll(newFiles)
        rescanLogItems()
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
