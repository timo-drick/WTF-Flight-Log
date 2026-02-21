package de.drick.wtf_osd

import kotlinx.coroutines.test.runTest
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.jvm.javaClass
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

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
    }

    @Test
    fun mspOsdV2Test() = runTest {
        val osdRecord = parseOsdTestFile("btfl/DJIG0161.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
        val duration = osdRecord.frames.last().millis.milliseconds
        println("Duration: $duration")
        val gpsData = extractGps(osdRecord)
        println("Gps points: ${gpsData.wayPoints.size}")
    }

    private fun parseOsdTestFile(fileName: String): OsdRecord {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
        assertNotNull(inputStream)
        val result = parseOsdFile(inputStream.asSource().buffered()) as ParseResult.Success
        return result.record
    }
}