package tilemap

import de.drick.compose.tilemap.GeoPoint
import de.drick.compose.tilemap.interpolateAltitudes
import kotlin.test.Test
import kotlin.test.assertEquals

class TestAltitudeInterpolation {

    @Test
    fun testInterpolation() {
        val gpsPoints = listOf(
            GeoPoint(52.5200, 13.4050, 100.0),     // Known
            GeoPoint(52.5201, 13.4060, null),      // Missing
            GeoPoint(52.5202, 13.4070, null),      // Missing
            GeoPoint(52.5203, 13.4080, 150.0),     // Known
            GeoPoint(52.5204, 13.4090, null),      // Missing
            GeoPoint(52.5205, 13.4100, 160.0),      // Known
            GeoPoint(52.5204, 13.4090, null),      // Missing (no end, stays null)
        )

        val gpsPointsExpected = listOf(
            GeoPoint(52.5200, 13.4050, 100.0),
            GeoPoint(52.5201, 13.4060, 116.66666666666666),
            GeoPoint(52.5202, 13.4070, 133.33333333333331),
            GeoPoint(52.5203, 13.4080, 150.0),
            GeoPoint(52.5204, 13.4090, 155.0),
            GeoPoint(52.5205, 13.4100, 160.0),
            GeoPoint(52.5204, 13.4090, null),
        )

        assertEquals(
            expected = gpsPointsExpected,
            actual = gpsPoints.interpolateAltitudes()
        )
    }
}
