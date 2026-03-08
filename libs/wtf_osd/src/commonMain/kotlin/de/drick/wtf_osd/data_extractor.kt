package de.drick.wtf_osd

import kotlinx.coroutines.yield

enum class SpeedUnit(val short: String, val description: String, val convertToKmh: Float = 1f) {
    Unknown("?", "Unit not detected!"),
    Kmh("Km/h", "Kilometer per hour)"),
    Mph("Mph", "miles per hour", 1.609344f)
}

enum class HeightUnit(val short: String, val description: String, val convertToMeter: Float = 1f) {
    Unknown("?", "Unit not detected!"),
    Meter("m", "meter"),
    KMeter("Km", "Kilometer", 1000f),
    Feet("ft", "feet", 0.3048f),
    KFeet("kft", "? not sure", 3048f)
}

fun Speed.unifiedValue() = value * unit.convertToKmh
fun Height.unifiedValue() = value * unit.convertToMeter

data class Speed(
    val value: Int,
    val unit: SpeedUnit
)

data class Height(
    val value: Float,
    val unit: HeightUnit
)

data class FlightData(
    val millis: Long,
    val aircraftIdentifier: String?,
    val speed: Speed?,
    val height: Height?,
    val amp: Float?
)

suspend fun extractFlightData(symbols: Symbols, aircraftIdentifier: Set<String>) = symbols.osdRecord.frames.map { frame ->
    yield()
    extractDataPoint(symbols, frame, aircraftIdentifier)
}


private val speedRegexBetaflight = Regex("""SYM_SPEED\D*(\d+(?:.\d+)?)(SYM_KPH|SYM_MPH)""")
private val altitudeRegexBetaflight = Regex("""SYM_ALTITUDE\D*(\d+(?:.\d+)?)(SYM_M)""")
/**
 * Sample:   2.33SYM_AMP
 */
private val ampereBetaflight = Regex("""\D(\d+(?:.\d+)?)?SYM_AMP""")
private val ampereINAV = ampereBetaflight

private val speedRegexINAV = Regex("""SYM_AIR(?:SYM_NONE)*(\d+(?:.\d+)?)(SYM_KMH|SYM_MPH)""")
private val altitudeRegexINAV = Regex("""\D(\d+(?:.\d+)?)(SYM_ALT_M|SYM_ALT_KM|SYM_ALT_FT|SYM_ALT_KFT)""")

fun extractDataPoint(symbols: Symbols, frame: MspFrame, aircraftIdentifier: Set<String>): FlightData {
    val string = symbols.toString(frame.data)
    val stringRaw = symbols.toString(frame.data, false)

    val speedRegex = when (symbols.osdRecord.fontVariant) {
        FontVariant.BETAFLIGHT -> speedRegexBetaflight
        FontVariant.INAV -> speedRegexINAV
        else -> null
    }
    val speed = speedRegex?.find(string)?.destructured?.let { (speed, symUnit) ->
        val unit = when (symUnit) {
            "SYM_KPH" -> SpeedUnit.Kmh
            "SYM_KMH" -> SpeedUnit.Kmh
            "SYM_MPH" -> SpeedUnit.Mph
            else -> SpeedUnit.Unknown
        }
        Speed(speed.toInt(), unit)
    }

    val altitudeRegex = when (symbols.osdRecord.fontVariant) {
        FontVariant.BETAFLIGHT -> altitudeRegexBetaflight
        FontVariant.INAV -> altitudeRegexINAV
        else -> null
    }
    val height = altitudeRegex?.find(string)?.destructured?.let { (height, symUnit) ->
        val unit = when (symUnit) {
            "SYM_M" -> HeightUnit.Meter
            "SYM_FT" -> HeightUnit.Feet
            "SYM_ALT_M" -> HeightUnit.Meter
            "SYM_ALT_KM" -> HeightUnit.KMeter
            "SYM_ALT_FT" -> HeightUnit.Feet
            "SYM_ALT_KFT" -> HeightUnit.KFeet
            else -> HeightUnit.Unknown
        }
        Height(height.toFloat(), unit)
    }

    val ampRegex = when (symbols.osdRecord.fontVariant) {
        FontVariant.BETAFLIGHT -> ampereBetaflight
        FontVariant.INAV -> ampereINAV
        else -> null
    }
    val amps = ampRegex?.find(string)?.groupValues?.last()?.toFloat()
    val identifier = aircraftIdentifier.find { string.contains(it) }


    //val lat = latRegex.find(string)?.groupValues?.last()
    //val lon = lonRegex.find(string)?.groupValues?.last()
    val data = FlightData(
        millis = frame.millis,
        aircraftIdentifier = identifier,
        speed = speed,
        height = height,
        amp = amps
    )
    return data
}