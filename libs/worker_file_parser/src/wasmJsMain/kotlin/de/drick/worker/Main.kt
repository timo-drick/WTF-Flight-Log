package de.drick.worker

import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.GeoPointMath.maxDistanceTo
import de.drick.compose.tilemap.calculateDistance
import de.drick.concurrency.runBackgroundWorker
import de.drick.wtf_osd.FileParserResult
import de.drick.wtf_osd.GpsPoint
import de.drick.wtf_osd.OSDSummeryData
import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.parseOsdFile
import io.github.vinceglb.filekit.PlatformFile
import io.github.vinceglb.filekit.name
import io.github.vinceglb.filekit.readBytes
import kotlinx.io.Buffer
import kotlin.time.Duration.Companion.milliseconds

fun main() {
    println("Start web worker")
    // This is the entry point for the WebWorker.
    // It will process Int messages and return the square of the input.
    runBackgroundWorker(FileParserResult.serializer()) { input ->
        println("Request received: ${input.name}")
        // Simulate heavy work
        when (val result = parseOsdFile(input.toSource())) {
            is ParseResult.Success -> {
                val summary = analyzeOsdFile(result.record, emptySet())
                FileParserResult.OsdResult(summary)
            }
            is ParseResult.Error<*> -> {
                FileParserResult.ErrorResult(result.toString())
            }
        }
    }
}

private suspend fun analyzeOsdFile(record: OsdRecord, identifier: Set<String>): OSDSummeryData {
    val symbols = Symbols(record)
    val data = extractFlightData(symbols, identifier)
    val gpsPoints = data.mapNotNull { it.gpsPoint }
    val identifier = data.find { it.aircraftIdentifier != null }?.aircraftIdentifier
    val maxSpeed = data.mapNotNull { it.speed }.maxByOrNull { it.value }
    val maxHeight = data.mapNotNull { it.height }.maxByOrNull { it.value }
    val geoPoints = gpsPoints.map { it.toGeoPoint() }
    val distanceTotal = if (gpsPoints.size > 2) geoPoints.calculateDistance() else null
    val maxDistanceHome = if (gpsPoints.size > 2) {
        geoPoints.first().maxDistanceTo(geoPoints)
    } else {
        null
    }
    return OSDSummeryData(
        fontVariant = record.fontVariant,
        duration = record.frames.last().millis.milliseconds,
        hasGpsData = gpsPoints.isNotEmpty(),
        startPosition = gpsPoints.firstOrNull(),
        aircraftIdentifier = identifier,
        maxSpeed = maxSpeed,
        maxHeight = maxHeight,
        distanceTotal = distanceTotal,
        maxDistanceHome = maxDistanceHome
    )
}

suspend fun PlatformFile.toSource() = Buffer().apply {
    write(readBytes())
}

fun GpsPoint.toGeoPoint() = GeoPoint(latitude, longitude, altitude)
fun GeoPoint.toGpsPoint() = GpsPoint(latitude, longitude, altitude)