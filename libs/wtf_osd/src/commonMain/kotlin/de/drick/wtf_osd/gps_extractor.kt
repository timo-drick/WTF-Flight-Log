package de.drick.wtf_osd

import de.drick.core.log



data class GpsPoint(val latitude: Double, val longitude: Double)

data class GpsData(
    val wayPoints: List<GpsRecord>
)

data class GpsRecord(
    val position: GpsPoint,
    val osdMillis: Long
)

val latRegex = Regex("""SYM_LAT(?:SYM_NONE|\s)*([+-]?\d+.\d+)\D""")
val lonRegex = Regex("""SYM_LON(?:SYM_NONE|\s)*([+-]?\d+.\d+)\D""")

fun extractGps(osdRecord: OsdRecord): GpsData {
    val positionList = mutableListOf<GpsRecord>()
    val symbols = Symbols(osdRecord)
    osdRecord.frames.forEach { frame ->
        try {
            val string = symbols.toString(frame.data)
            val lat = latRegex.find(string)?.groupValues?.last()?.toDouble()
            val lon = lonRegex.find(string)?.groupValues?.last()?.toDouble()
            if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
                positionList.add(GpsRecord(GpsPoint(lat, lon), frame.millis))
            }
        } catch (err: Throwable) {
            log("Error while parsing GPS data: ${err.message}")
        }
    }
    log("Gps data loaded ${positionList.size} points.")
    return GpsData(positionList)
}
