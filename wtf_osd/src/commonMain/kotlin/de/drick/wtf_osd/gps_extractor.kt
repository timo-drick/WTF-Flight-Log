package de.drick.wtf_osd

import de.drick.core.log

data class GpsData(
    val wayPoints: List<GpsRecord>
)

data class GpsRecord(
    val position: GeoPoint,
    val osdMillis: Long
)

fun extractGps(osdRecord: OsdRecord): GpsData {
    val font = osdRecord.fontVariant
    val positionList = mutableListOf<GpsRecord>()
    val lat = font.LAT
    val lon = font.LON
    if (lat == null || lon == null) return GpsData(positionList)
    osdRecord.frames.forEach { frame ->
        try {
            val latChars = frame.data.detectTrailingString(font, lat, 10)
            val lonChars = frame.data.detectTrailingString(font, lon, 10)
            if (latChars != null && lonChars != null) {
                val lat = latChars.toDouble()
                val lon = lonChars.toDouble()
                positionList.add(GpsRecord(GeoPoint(lat, lon), frame.millis))
            }
        } catch (err: Throwable) {
            log("Error while parsing GPS data: ${err.message}")
        }
    }
    log("Gps data loaded ${positionList.size} points.")
    return GpsData(positionList)
}
