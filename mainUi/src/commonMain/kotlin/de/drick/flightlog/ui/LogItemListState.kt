package de.drick.flightlog.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.analyzeFlow
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.flightlog.localStorage.AircraftIdentifierDB
import io.github.vinceglb.filekit.PlatformFile
import kotlinx.collections.immutable.toPersistentList
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.yield
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime

fun FileItem.fromPlatformFile(file: PlatformFile) = BaseFile(file)
fun PlatformFile.toFileItem() = BaseFile(this)


interface FlightLogState {
    val isWorking: Boolean
    val lazyListState: LazyListState
    val list: List<LogItem>
    val groups: Map<String?, List<LogItem>>
    val aircraftIdentifierList: List<AircraftIdentifier>
    fun importFiles(files: List<PlatformFile>)
    fun rescanLogItems()
    fun addAircraft(aircraftIdentifier: AircraftIdentifier)
    fun removeAircraft(aircraftIdentifier: AircraftIdentifier)
}

class FlightLogStateImpl(
    private val scope: CoroutineScope
): FlightLogState {
    private val aircraftDB = AircraftIdentifierDB()
    private val platformFileList = mutableListOf<PlatformFile>()
    private val workingLock = Mutex()

    override val lazyListState = LazyListState()
    override var isWorking by mutableStateOf(false)
        private set


    override var list: List<LogItem> by mutableStateOf(emptyList())
        private set
    override var groups: Map<String?, List<LogItem>> by mutableStateOf(emptyMap())
        private set

    override var aircraftIdentifierList by mutableStateOf(emptyList<AircraftIdentifier>())
        private set

    private val logList = mutableListOf<LogItem>()

    init {
        updateAircraftList()
    }

    private fun updateAircraftList() {
        aircraftIdentifierList = aircraftDB.loadAll()
    }

    private var runningScanJob: Job? = null
    override fun rescanLogItems() {
        runningScanJob?.cancel()
        runningScanJob = scope.launch(Dispatchers.Default) {
            workingLock.withLock {
                isWorking = true
                logList.clear()
                val fileItemList = platformFileList
                    .map { it.toFileItem() }
                    .sortedByDescending { it.lastModified }
                fileItemList.analyzeFlow(aircraftIdentifierList).collect { item ->
                    logList.add(item)
                    updateList()
                    yield()
                }
                isWorking = false
            }
        }
    }

    override fun addAircraft(aircraftIdentifier: AircraftIdentifier) {
        aircraftDB.addAircraft(aircraftIdentifier)
        updateAircraftList()
    }
    override fun removeAircraft(aircraftIdentifier: AircraftIdentifier) {
        aircraftDB.removeAircraft(aircraftIdentifier)
        updateAircraftList()
    }

    override fun importFiles(files: List<PlatformFile>) {
        scope.launch {
            val newFiles = files.filterNot { platformFileList.contains(it) }
            platformFileList.addAll(newFiles)
            rescanLogItems()
        }
    }

    private fun updateList() {
        list = logList.toPersistentList()
        groups = list.group()
    }
}

fun List<LogItem>.group() = sortedByDescending { it.lastModified }.groupBy {
    it.files.firstOrNull()?.lastModified
        ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
        ?.formatLocalized()
}
