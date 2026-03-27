package de.drick.flightlog.file

import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.GeoPointMath.maxDistanceTo
import de.drick.compose.tilemap.calculateDistance
import de.drick.concurrency.createBackgroundWorkerInstance
import de.drick.core.log
import de.drick.flightlog.localStorage.OsdSummeryDataCache
import de.drick.flightlog.localStorage.SrtSummeryDataCache
import de.drick.flightlog.ui.components.toGeoPoint
import de.drick.flightlog.ui.id
import de.drick.wtf_osd.FileParserResult
import de.drick.wtf_osd.GpsPoint
import de.drick.wtf_osd.OSDSummeryData
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import kotlinx.collections.immutable.toImmutableList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.collections.filter
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

suspend fun FileItem.toTypedItem(identifier: Set<String>): FileItem? = when (extension.lowercase()) {
    "osd" -> getCachedOsdFile(this, identifier)
    "srt" -> getCachedSrtFile(this)
    "mov", "mp4" -> VideoFile(this)
    else -> null
}


private val logItemNameRegex = Regex("""(\D*)(\d+)""")

fun List<LogItem>.mergeItems(): List<LogItem> {
    val originalList = this
    val itemsWithOsdFile = originalList.filter { it.files.filterIsInstance<OSDFile>().isNotEmpty() }
    val logItemsToRemove = mutableListOf<LogItem>()
    val mergedLogItems = mutableMapOf<String, LogItem>()
    itemsWithOsdFile.forEach { logItem ->
        //Check if there are more files belonging to this log:
        val osdFileDuration = logItem.files.filterIsInstance<OSDFile>().firstOrNull()?.duration
        val srtFileDuration = logItem.files.filterIsInstance<SRTFile>().firstOrNull()?.duration
        if (osdFileDuration != null && srtFileDuration != null) {
            var srtDuration: Duration = srtFileDuration
            var previousLogItem = logItem
            val collectedFiles = logItem.files.toMutableList()
            while (osdFileDuration - srtDuration > 2.seconds) {
                val nextItem = getNextName(previousLogItem.name)?.let { nextName ->
                    log("Next: $nextName")
                    originalList.find { it.name == nextName }
                }
                if (nextItem != null && nextItem.files.none { it is OSDFile }) {
                    logItemsToRemove.add(nextItem)
                    collectedFiles.addAll(nextItem.files)
                    srtDuration = collectedFiles.filterIsInstance<SRTFile>()
                        .fold(Duration.ZERO) { acc, item -> acc + item.duration }
                    previousLogItem = nextItem
                } else {
                    break
                }
            }
            if (collectedFiles.size != logItem.files.size) {
                mergedLogItems[logItem.name] = logItem.copy(
                    files = collectedFiles.toImmutableList()
                )
            }
        }
    }
    return originalList.mapNotNull { originalItem ->
        val mergedItem = mergedLogItems[originalItem.name]
        when {
            mergedItem != null -> mergedItem
            logItemsToRemove.contains(originalItem) -> null
            else -> originalItem
        }
    }
}

fun getNextName(name: String): String? {
    logItemNameRegex.find(name)?.destructured?.let { (namePart, index) ->
        val numbers = index.length
        val nextIndex = (index.toInt() + 1).toString().padStart(numbers, '0')
        return "${namePart}${nextIndex}"
    }
    return null
}

val srtCache = SrtSummeryDataCache

private suspend fun getCachedSrtFile(fileItem: FileItem): FileItem {
    val id = fileItem.platformFile().id()
    val summeryData = srtCache.load(id)
    return if (summeryData != null) {
        SRTFile(
            file = fileItem,
            duration = summeryData.duration
        )
    } else {
        val srt = withContext(Dispatchers.Default) {
            parseSrtFile(fileItem.source())
        }
        when (srt) {
            is ParseResult.Success -> {
                val duration = srt.record.frames.last().endTimeMs.milliseconds
                srtCache.save(id, SrtSummeryData(duration))
                SRTFile(
                    file = fileItem,
                    duration = duration
                )
            }
            is ParseResult.Error -> ErrorFile(fileItem, srt.type.name)
        }
    }
}

private val osdSummeryDataCache = OsdSummeryDataCache

private suspend fun getCachedOsdFile(fileItem: FileItem, identifier: Set<String>): FileItem {
    val id = fileItem.platformFile().id()
    /*val cachedData = osdSummeryDataCache.load(id)
    return if (cachedData != null) {
        OSDFile(
            file = fileItem,
            fontVariant = cachedData.fontVariant,
            duration = cachedData.duration,
            hasGpsData = cachedData.hasGpsData,
            startPosition = cachedData.startPosition,
            aircraftIdentifier = cachedData.aircraftIdentifier,
            maxSpeed = cachedData.maxSpeed,
            maxHeight = cachedData.maxHeight,
            distanceTotal = cachedData.distanceTotal,
            maxDistanceHome = cachedData.maxDistanceHome
        )
    } else {*/
    val worker = createBackgroundWorkerInstance(
        workerScriptUrl = "worker_file_parser.js",
        nonWebImplementation = { TODO("Not implemented yet!") },
        outputSerializer = FileParserResult.serializer(),
    )
    return when (val result = worker.execute(fileItem.platformFile())) {
        is FileParserResult.ErrorResult -> ErrorFile(fileItem, result.message)
        is FileParserResult.OsdResult -> {
            val summery = result.data
            osdSummeryDataCache.save(id, summery)
            OSDFile(
                file = fileItem,
                fontVariant = summery.fontVariant,
                duration = summery.duration,
                hasGpsData = summery.hasGpsData,
                startPosition = summery.startPosition?.toGeoPoint(),
                aircraftIdentifier = summery.aircraftIdentifier,
                maxSpeed = summery.maxSpeed,
                maxHeight = summery.maxHeight,
                distanceTotal = summery.distanceTotal,
                maxDistanceHome = summery.maxDistanceHome
            )
        }
    }
}

fun GpsPoint.toGeoPoint() = GeoPoint(latitude, longitude, altitude)
fun GeoPoint.toGpsPoint() = GpsPoint(latitude, longitude, altitude)
