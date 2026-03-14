package de.drick.flightlog.file

import de.drick.compose.tilemap.calculateGeoDistance
import de.drick.compose.tilemap.maxDistanceTo
import de.drick.flightlog.localStorage.AircraftIdentifier
import de.drick.flightlog.ui.components.toGeoPoint
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.parseOsdFile
import de.drick.wtf_osd.parseSrtFile
import kotlinx.collections.immutable.toImmutableSet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.withContext
import kotlin.time.Duration.Companion.milliseconds

fun List<FileItem>.analyzeFlow(aircraftIdentifier: List<AircraftIdentifier>) = flow {
    val identifier = aircraftIdentifier.map { it.name }.toSet()
    groupBy { it.name }.forEach { (name, fileList) ->
        val items = fileList.mapNotNull { fileItem ->
            when (fileItem.extension.lowercase()) {
                "osd" -> analyzeOsdFile(fileItem, identifier)
                "srt" -> {
                    val srt = withContext(Dispatchers.Default) {
                        parseSrtFile(fileItem.source())
                    }
                    when(srt) {
                        is ParseResult.Success -> {
                            val duration = srt.record.frames.last().endTimeMs.milliseconds
                            SRTFile(
                                file = fileItem,
                                duration = duration
                            )
                        }
                        is ParseResult.Error -> ErrorFile(fileItem, srt.type.name)
                    }
                }
                "mov", "mp4" -> {
                    VideoFile(fileItem)
                }
                else -> null
            }
        }
        if (items.isNotEmpty()) {
            emit(LogItem(name, items.toImmutableSet()))
        }
    }
}.flowOn(Dispatchers.Default)

private suspend fun analyzeOsdFile(fileItem: FileItem, identifier: Set<String>) = when(val osd = parseOsdFile(fileItem.source())) {
    is ParseResult.Success -> {
        val symbols = Symbols(osd.record)
        val duration = osd.record.frames.last().millis.milliseconds
        val gpsPoints = extractGps(osd.record).wayPoints.map { it.position.toGeoPoint() }
        val distanceTotal = if (gpsPoints.size > 2) gpsPoints.calculateGeoDistance() else null
        val maxDistanceHome = if (gpsPoints.size > 2) gpsPoints.first().maxDistanceTo(gpsPoints) else null
        val data = extractFlightData(symbols, identifier)
        val identifier = data.find { it.aircraftIdentifier != null }?.aircraftIdentifier
        val maxSpeed = data.mapNotNull { it.speed }.maxByOrNull { it.value }
        val maxHeight = data.mapNotNull { it.height }.maxByOrNull { it.value }
        OSDFile(
            file = fileItem,
            fontVariant = osd.record.fontVariant,
            duration = duration,
            hasGpsData = gpsPoints.isNotEmpty(),
            startPosition = gpsPoints.firstOrNull(),
            aircraftIdentifier = identifier,
            maxSpeed = maxSpeed,
            maxHeight = maxHeight,
            distanceTotal = distanceTotal,
            maxDistanceHome = maxDistanceHome
        )
    }
    is ParseResult.Error -> ErrorFile(fileItem, osd.type.name)
}