package wtf_osd

import de.drick.wtf_osd.FlightData
import de.drick.wtf_osd.FontVariant
import de.drick.wtf_osd.GpsPoint
import de.drick.wtf_osd.Height
import de.drick.wtf_osd.HeightUnit
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.Speed
import de.drick.wtf_osd.SpeedUnit
import de.drick.wtf_osd.Symbols
import de.drick.wtf_osd.extractFlightData
import de.drick.wtf_osd.parseOsdFile
import kotlinx.coroutines.test.runTest
import kotlinx.io.Buffer
import wtfflightlog.libs.wtf_osd.generated.resources.Res
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
                speed = Speed(value = 2, unit = SpeedUnit.Kmh),
                height = Height(3f, HeightUnit.Meter),
                amp = 81.22f,
                aircraftIdentifier = null,
                gpsPoint = null
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
        val symbols = Symbols(osdRecord)
        val data = extractFlightData(symbols, aircraftIdentifier)
        val wayPoints = data.mapNotNull { it.gpsPoint }
        assertEquals(700, wayPoints.size)
        assertEquals(
            expected = GpsPoint(38.4997578, -9.1823096, 0.10000000149011612),
            actual = wayPoints.first()
        )
        assertEquals(
            expected = GpsPoint(38.4997303, -9.1823309, altitude=38.29999923706055),
            actual = wayPoints.last()
        )
        assertEquals(
            expected = FlightData(
                millis = 309957,
                speed = Speed(value=61, unit= SpeedUnit.Kmh),
                height = Height(value=15.5f, unit= HeightUnit.Meter),
                amp = 11.12f,
                aircraftIdentifier = "SPC5",
                gpsPoint = GpsPoint(latitude=38.4997991, longitude=-9.1808245, altitude=15.5)
            ),
            actual = data[578]
        )
    }
}

suspend fun parseOsdTestFile(fileName: String): de.drick.wtf_osd.OsdRecord {
    val data = Res.readBytes("files/$fileName")
    assertNotNull(data)
    val buffer = Buffer()
    buffer.write(data)
    val result = parseOsdFile(buffer) as ParseResult.Success
    return result.record
}