import de.drick.wtf_osd.OsdRecord
import de.drick.wtf_osd.ParseResult
import de.drick.wtf_osd.extractGps
import de.drick.wtf_osd.parseOsdFile
import kotlinx.coroutines.test.runTest
import kotlinx.io.asSource
import kotlinx.io.buffered
import kotlin.test.Test
import kotlin.test.assertNotNull
import kotlin.time.Duration.Companion.milliseconds

class ParserTest {

    @Test
    fun fileWithoutGPS() = runTest {
        //val osdRecord = parseOsdTestFile("DJIG0068.osd")
        val osdRecord = parseOsdTestFile("btfl/CEBU003.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
    }

    @Test
    fun mspOsdV2Test() = runTest {
        val osdRecord = parseOsdTestFile("btfl/DJIG0001.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
        val duration = osdRecord.frames.last().millis.milliseconds
        println("Duration: $duration")
        val gpsData = extractGps(osdRecord)
        println("Gps points: ${gpsData.wayPoints.size}")
    }

    @Test
    fun mspOsdINavTest() = runTest {
        val osdRecord = parseOsdTestFile("inav/DJIG0113.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
        val duration = osdRecord.frames.last().millis.milliseconds
        println("Duration: $duration")
        val gpsData = extractGps(osdRecord)
        println("Gps points: ${gpsData.wayPoints.size}")
    }
    @Test
    fun mspOsdINavGpsTest() = runTest {
        val osdRecord = parseOsdTestFile("inav/DJIG0136.osd")
        println("Parsed frames: ${osdRecord.frames.size}")
        val duration = osdRecord.frames.last().millis.milliseconds
        println("Duration: $duration")
        val gpsData = extractGps(osdRecord)
        println("Gps points: ${gpsData.wayPoints.size}")
        gpsData.wayPoints.forEach { println(it) }
    }


    private fun parseOsdTestFile(fileName: String): OsdRecord {
        val inputStream = ParserTest::class.java.getResourceAsStream(fileName)
        assertNotNull(inputStream)
        val result = parseOsdFile(inputStream.asSource().buffered()) as ParseResult.Success
        return result.record
    }
}