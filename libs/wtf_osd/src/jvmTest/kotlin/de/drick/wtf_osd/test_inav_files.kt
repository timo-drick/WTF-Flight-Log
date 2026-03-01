package de.drick.wtf_osd


import kotlinx.coroutines.test.runTest
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

class InavParserTest {

    @Test
    fun mspOsdINavTest() = runTest {
        val osdRecord = parseOsdTestFile("inav/DJIG0113.osd")
        assertEquals(1091, osdRecord.frames.size)
        assertEquals(113967, osdRecord.frames.last().millis)
        val gpsData = extractGps(osdRecord)
        assertEquals(0, gpsData.wayPoints.size)
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols)
        assertEquals(
            expected = FlightData(millis=33, speed=null, height=Height(0f, HeightUnit.Meter), 1.21f),
            actual = data[0]
        )
        assertEquals(
            expected = FlightData(millis=113217, speed=null, height=Height(5f, HeightUnit.Meter), 1.19f),
            actual = data[1079]
        )
        /*data.forEachIndexed { index, data ->
            val string = symbols.toString(osdRecord.frames[index].data).replace("SYM_NONE", " ")
            println("[$index] = $data")
        }*/

    }
    @Test
    fun mspOsdINavGpsTest() = runTest {
        val osdRecord = parseOsdTestFile("inav/DJIG0136.osd")
        assertEquals(13507, osdRecord.frames.size)
        assertEquals(1413167, osdRecord.frames.last().millis)
        val gpsData = extractGps(osdRecord)
        assertEquals(13493, gpsData.wayPoints.size)
        assertEquals(
            expected = GpsRecord(position=GpsPoint(latitude=48.5956185, longitude=9.5113062), osdMillis=17),
            actual = gpsData.wayPoints[0]
        )
        assertEquals(
            expected = GpsRecord(position=GpsPoint(latitude=48.5956272, longitude=9.5113094), osdMillis=1411800),
            actual = gpsData.wayPoints[13492]
        )
        val symbols = Symbols(osdRecord)
        val string = symbols.toString(osdRecord.frames.first().data).replace("SYM_NONE", " ")
        val match = string.contains("DOLPHIN", ignoreCase = true)
        val data = extractFlightData(symbols)
        assertEquals(
            expected = FlightData(millis=526567, speed=Speed(value=59, unit= SpeedUnit.Kmh), height=Height(value=11f, unit= HeightUnit.Meter), amp=6.11f),
            actual = data[5036]
        )
        /*data.forEachIndexed { index, data ->
            println("[$index] = $data")
        }*/
        //gpsData.wayPoints.forEach { println(it) }
    }

    private fun parseOsdTestFile(fileName: String): OsdRecord {
        val inputStream = javaClass.classLoader.getResourceAsStream(fileName)
        assertNotNull(inputStream)
        val result = parseOsdFile(inputStream.asSource().buffered()) as ParseResult.Success
        return result.record
    }
}