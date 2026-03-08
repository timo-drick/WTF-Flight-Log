package de.drick.wtf_osd

import kotlinx.coroutines.test.runTest
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.jvm.javaClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

private val aircraftIdentifier = setOf("SPC5", "LR4")

class BetaflightParserTest {

    @Test
    fun oldOsdV1WithoutGPS() = runTest {
        val osdRecord = parseOsdTestFile("btfl/CEBU003.osd")
        assertEquals(2721, osdRecord.frames.size)
        assertEquals(FontVariant.BETAFLIGHT, osdRecord.fontVariant)
        assertEquals(1, osdRecord.version)
        assertEquals(31, osdRecord.charWidth)
        assertEquals(15, osdRecord.charHeight)
        assertEquals(36, osdRecord.fontWidth)
        assertEquals(54, osdRecord.fontHeight)
        assertEquals(180, osdRecord.xOffset)
        assertEquals(0, osdRecord.yOffset)
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols, aircraftIdentifier)
        assertEquals(
            expected = FlightData(
                millis = 202200,
                speed = Speed(value=2, unit= SpeedUnit.Kmh),
                height = Height(3f, HeightUnit.Meter),
                amp = 81.22f,
                aircraftIdentifier = null
            ),
            actual = data[2434]
        )
    }

    @Test
    fun mspOsdV2Test() = runTest {
        val osdRecord = parseOsdTestFile("btfl/DJIG0161.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
        val duration = osdRecord.frames.last().millis.milliseconds
        println("Duration: $duration")
        val gpsData = extractGps(osdRecord)
        assertEquals(700, gpsData.wayPoints.size)
        assertEquals(
            expected = GpsRecord(GpsPoint(38.4997578, -9.1823096), 6),
            actual = gpsData.wayPoints.first()
        )
        assertEquals(
            expected = GpsRecord(GpsPoint(38.4997303, -9.1823309), 384014),
            actual = gpsData.wayPoints.last()
        )
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols, aircraftIdentifier)
        assertEquals(
            expected = FlightData(
                millis = 309957,
                speed = Speed(value=61, unit= SpeedUnit.Kmh),
                height = Height(value=15.5f, unit= HeightUnit.Meter),
                amp = 11.12f,
                aircraftIdentifier = "SPC5"
            ),
            actual = data[578]
        )
    }

    private suspend fun parseOsdTestFile(fileName: String): OsdRecord {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
        assertNotNull(inputStream)
        val result = parseOsdFile(inputStream.asSource().buffered()) as ParseResult.Success
        return result.record
    }
}