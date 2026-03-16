package de.drick.wtf_osd

import de.drick.core.log

data class GpsPoint(val latitude: Double, val longitude: Double, val altitude: Double? = null)

data class GpsData(
    val wayPoints: List<GpsRecord>
)

data class GpsRecord(
    val position: GpsPoint,
    val osdMillis: Long
)

private val latRegex = Regex("""SYM_LAT(?:SYM_NONE|\s)*([+-]?\d+.\d+)\D""")
private val lonRegex = Regex("""SYM_LON(?:SYM_NONE|\s)*([+-]?\d+.\d+)\D""")

fun extractGps(frame: MspFrame, symbols: Symbols, height: Double? = null): GpsPoint? {
    try {
        val string = symbols.toString(frame.data)
        val lat = latRegex.find(string)?.groupValues?.last()?.toDouble()
        val lon = lonRegex.find(string)?.groupValues?.last()?.toDouble()
        if (lat != null && lon != null && (lat != 0.0 || lon != 0.0)) {
            return GpsPoint(lat, lon, height)
        }
    } catch (err: Throwable) {
        log("Error while parsing GPS data: ${err.message}")
    }
    return null
}
