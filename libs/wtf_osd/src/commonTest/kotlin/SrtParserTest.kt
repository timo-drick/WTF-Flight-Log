import de.drick.wtf_osd.SrtFrame
import de.drick.wtf_osd.parseSrtData
import kotlin.test.Test
import kotlin.test.assertEquals

class SrtParserTest {

    @Test
    fun testParseSrtFile() {
        val srtContent = """
1
00:00:01,000 --> 00:00:02,000
signal:4 glsBat:7.7V delay:100ms bitrate:50.5Mbps rcSignal:0

2
00:00:03,500 --> 00:00:04,600
some other data glsBat:80% delay:200ms bitrate:40.1Mbps more data

260
00:00:38,866 --> 00:00:39,016
signal:4 ch:65535 flightTime:40 uavBat:24.7V glsBat:95% uavBatCells:6 glsBatCells:0 delay:33ms bitrate:25.4Mbps rcSignal:0

372
00:00:33,318 --> 00:00:33,408
Signal:4 CH:3 Hz:5805000 FlightTime:33 Sp=16 Gp=27 SBat:4.9V GBat:16.3V Delay:35ms Bitrate:25.0Mbps Distance:10m
"""

        val frames = parseSrtData(srtContent).toList()
        assertEquals(4, frames.size)

        val expectedFrame1 = SrtFrame(
            index = 1,
            startTimeMs = 1000,
            endTimeMs = 2000,
            delayMs = 100,
            bitrateMbps = 50.5f,
            glsBat = "7.7V"
        )
        assertEquals(expectedFrame1, frames[0])

        val expectedFrame2 = SrtFrame(
            index = 2,
            startTimeMs = 3500,
            endTimeMs = 4600,
            delayMs = 200,
            bitrateMbps = 40.1f,
            glsBat = "80%"
        )
        assertEquals(expectedFrame2, frames[1])

        val expectedFrame3 = SrtFrame(
            index = 260,
            startTimeMs = 38866,
            endTimeMs = 39016,
            delayMs = 33,
            bitrateMbps = 25.4f,
            glsBat = "95%"
        )
        assertEquals(expectedFrame3, frames[2])

        val expectedFrame4 = SrtFrame(
            index = 372,
            startTimeMs = 33318,
            endTimeMs = 33408,
            delayMs = 35,
            bitrateMbps = 25.0f,
            glsBat = "16.3V"
        )
        assertEquals(expectedFrame4, frames[3])
    }
}