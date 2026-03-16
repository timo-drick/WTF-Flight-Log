package wtf_osd


import de.drick.wtf_osd.FlightData
import de.drick.wtf_osd.GpsPoint
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.HeightUnit
import de.drick.wtf_osd.Speed
import de.drick.wtf_osd.SpeedUnit
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

private val aircraftIdentifier = setOf("DOLPHIN")

class InavParserTest {

    @Test
    fun mspOsdINavTest() = runTest {
        val osdRecord = parseOsdTestFile("inav/DJIG0113.osd")
        assertEquals(1091, osdRecord.frames.size)
        assertEquals(113967, osdRecord.frames.last().millis)
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols, aircraftIdentifier)
        assertEquals(
            expected = FlightData(
                millis = 33,
                speed = null,
                height = Height(0f, HeightUnit.Meter),
                amp = 1.21f,
                aircraftIdentifier = null,
                gpsPoint = null
            ),
            actual = data[0]
        )
        assertEquals(
            expected = FlightData(
                millis = 113217,
                speed = null,
                height = Height(5f, HeightUnit.Meter),
                amp = 1.19f,
                aircraftIdentifier = null,
                gpsPoint = null
            ),
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
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols, aircraftIdentifier)
        val wayPoints = data.mapNotNull { it.gpsPoint }
        assertEquals(13493, wayPoints.size)
        assertEquals(
            expected = GpsPoint(latitude=48.5956185, longitude=9.5113062, altitude = null),
            actual = wayPoints[0]
        )
        assertEquals(
            expected = GpsPoint(latitude=48.5956272, longitude=9.5113094, altitude = 4.0),
            actual = wayPoints[13492]
        )
        assertEquals(
            expected = FlightData(
                millis = 526567,
                speed = Speed(value = 59, unit = SpeedUnit.Kmh),
                height = Height(value = 11f, unit = HeightUnit.Meter),
                amp = 6.11f,
                aircraftIdentifier = "DOLPHIN",
                gpsPoint = GpsPoint(latitude=48.601706, longitude=9.5071766, altitude=11.0)
            ),
            actual = data[5036]
        )
    }
}