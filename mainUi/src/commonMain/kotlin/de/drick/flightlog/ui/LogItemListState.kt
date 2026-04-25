package de.drick.flightlog.ui

import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.drick.flightlog.file.BaseFile
import de.drick.flightlog.file.FileItem
import de.drick.flightlog.file.LogItem
import de.drick.flightlog.file.OSDFile
import de.drick.flightlog.file.mergeItems
import de.drick.flightlog.file.toTypedItem
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.flightlog.localStorage.AircraftIdentifierDB
import de.drick.flightlog.localStorage.OsdSummeryDataCache
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.lastModified
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.size
import kotlinx.collections.immutable.toImmutableList
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
fun PlatformFile.toBaseFile() = BaseFile(this)


interface FlightLogState {
    val isWorking: Boolean
    val lazyListState: LazyListState
    val list: List<LogItem>
    val groups: Map<String?, List<LogItem>>
    val aircraftIdentifierList: List<AircraftIdentifier>
    fun importFiles(files: List<PlatformFile>)
    fun rescanLogItems(force: Boolean = false)
    fun addAircraft(aircraftIdentifier: AircraftIdentifier)
    fun removeAircraft(aircraftIdentifier: AircraftIdentifier)
}

fun PlatformFile.id(): String {
    val size = size()
    val lastModified = lastModified().toEpochMilliseconds()
    return "$name:${(lastModified + size).hashCode().toHexString()}"
}

inline fun <K, V> MutableMap<K, V>.getOrPutIfNotNull(key: K, defaultValue: () -> V?): V? {
    val value = get(key)
    return if (value == null) {
        val answer = defaultValue()
        if (answer != null) {
            put(key, answer)
        }
        answer
    } else {
        value
    }
}

class FlightLogStateImpl(
    private val scope: CoroutineScope
): FlightLogState {
    private val aircraftDB = AircraftIdentifierDB()
    private val baseFileMap = mutableMapOf<String, BaseFile>()
    private val fileItemMap = mutableMapOf<String, FileItem>()
    private val fileScanningLock = Mutex()

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
        scope.launch {
            aircraftIdentifierList = aircraftDB.loadAll()
        }
    }

    private var runningScanJob: Job? = null
    override fun rescanLogItems(
        force: Boolean
    ) {
        runningScanJob?.cancel()
        runningScanJob = scope.launch(Dispatchers.Default) {
            fileScanningLock.withLock {
                isWorking = true
                val fileItemList = baseFileMap.values.sortedByDescending { it.lastModified }
                val identifier = aircraftIdentifierList.map { it.name }.toSet()
                logList.clear()
                if (force) {
                    val osdCacheKeyToDeleteList = fileItemMap.values.filterIsInstance<OSDFile>()
                        .filter { it.aircraftIdentifier == null || it.aircraftIdentifier !in identifier }
                        .map { it.platformFile().id() }
                    osdCacheKeyToDeleteList.forEach {
                        OsdSummeryDataCache.remove(it)
                        fileItemMap.remove(it)
                    }
                }
                fileItemList.groupBy { it.path + it.name }.forEach { (_, fileList) ->
                    val items = fileList.mapNotNull { fileItem ->
                        fileItemMap.getOrPutIfNotNull(fileItem.file.id()) {
                            fileItem.toTypedItem(identifier)
                        }
                    }
                    if (items.isNotEmpty()) {
                        val name = fileList.first().name
                        val logItem = LogItem(name, items.distinct().toImmutableList())
                        logList.add(logItem)
                        list = logList.toPersistentList()
                        groups = list.group()
                        yield()
                    }
                }
                val mergedList = list.mergeItems()
                    .sortedByDescending { it.lastModified }
                    .toPersistentList()
                list = mergedList
                groups = list.group()
                isWorking = false
            }
        }
    }

    override fun addAircraft(aircraftIdentifier: AircraftIdentifier) {
        scope.launch {
            aircraftDB.addAircraft(aircraftIdentifier)
            updateAircraftList()
        }
    }
    override fun removeAircraft(aircraftIdentifier: AircraftIdentifier) {
        scope.launch {
            aircraftDB.removeAircraft(aircraftIdentifier)
            updateAircraftList()
        }
    }

    override fun importFiles(files: List<PlatformFile>) {
        scope.launch {
            baseFileMap.putAll(files.map { it.toBaseFile() }.associateBy { it.platformFile().id() })
            rescanLogItems()
        }
    }
}

fun List<LogItem>.group() = sortedByDescending { it.lastModified }.groupBy {
    it.files.firstOrNull()?.lastModified
        ?.toLocalDateTime(TimeZone.currentSystemDefault())?.date
        ?.formatLocalized()
}
