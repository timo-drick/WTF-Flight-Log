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
    val positionList = mutableListOf<GpsRecord>()
    osdRecord.frames.forEach { frame ->
        try {
            val latChars = frame.data.detectTrailingChars(137, 10)?.cleanString()
            val lonChars = frame.data.detectTrailingChars(152, 10)?.cleanString()
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

fun ShortArray.detectTrailingChars(code: Int, number: Int): CharArray? {
    val shortCode = code.toShort()
    val index = indexOfFirst { it == shortCode }
    return if (index >= 0) {
        CharArray(number) {
            val short = this[index + it + 1]
            Char(short.toInt())
        }
    } else {
        null
    }
}

fun ShortArray.detectLeadingChars(code: Int, number: Int): CharArray? {
    val shortCode = code.toShort()
    val index = indexOfFirst { it == shortCode }
    return if (index >= 0) {
        val x = index / 22
        val y = index % 22
        CharArray(number) {
            val p = x - number + it
            val short = this[y + p * 22]
            Char(short.toInt())
        }
    } else {
        null
    }
}

fun CharArray.cleanString(): String {
    val endIndex = indexOf('\u0000')
    return if (endIndex >= 0) {
        concatToString(startIndex = 0, endIndex)
    } else {
        concatToString()
    }
}