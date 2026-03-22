package de.drick.compose.tilemap

import androidx.compose.ui.graphics.Color
import kotlin.math.pow
import kotlin.math.roundToInt

fun List<GeoPoint>.removeConsecutiveDuplicates(): List<GeoPoint> =
    if (isEmpty()) emptyList()
    else zipWithNext { a, b -> a.takeIf { a.latitude != b.latitude || a.longitude != b.longitude  } }
        .filterNotNull()
        .plus(last())

/**
 * Specification: https://developers.google.com/kml/documentation/kmlreference#linestring
 */
suspend fun exportKmlTrack(
    points: List<GeoPoint>,
    name: String = "Drone Flight Path",
    description: String = "Exported GPS track",
    pathColor: Color = Color.Green,
    tessellate: Boolean = true, // Follow the curvature of the earth
    extrude: Boolean = true,   // Connect linestring to earth
): String {
    require(points.isNotEmpty()) { "Points list cannot be empty" }
    val interpolatedPoints = points.interpolateAltitudes().removeConsecutiveDuplicates()
    val elevationFirstPoint = getElevation(points.first()) ?: 0.0
    val coordinates = interpolatedPoints.joinToString(" ") { it.kmlCoordinate(elevationFirstPoint) }
    val colorString = pathColor.toAabbggrr()
    val first = points.first().kmlCoordinate()
    val last = points.last().kmlCoordinate()
    return generateKml(
        name = name,
        description = description,
        colorString = colorString,
        firstCoordinate = first,
        lastCoordinate = last,
        tessellate = tessellate,
        extrude = extrude,
        isAbsoluteAltitude = points.any { it.alt != null },
        coordinates = coordinates
    )
}

private fun generateKml(
    name: String,
    description: String,
    colorString: String,
    firstCoordinate: String,
    lastCoordinate: String,
    tessellate: Boolean,
    extrude: Boolean,
    isAbsoluteAltitude: Boolean,
    coordinates: String
): String = """
<?xml version="1.0" encoding="UTF-8"?>
<kml xmlns="http://www.opengis.net/kml/2.2">
  <Document>
    <name>$name</name>
    <description>$description</description>
    <Style id="trackStyle">
      <LineStyle>
        <color>$colorString</color>  <!-- Green line -->
        <width>4</width>
      </LineStyle>
      <IconStyle>
        <Icon><href>https://maps.google.com/mapfiles/kml/pushpin/ylw-pushpin.png</href></Icon>
      </IconStyle>
    </Style>
    <!-- Start Marker Style (Green) -->
    <Style id="startStyle">
      <IconStyle>
        <color>ff00ff00</color>  <!-- Green -->
        <scale>1.2</scale>
      </IconStyle>
      <LabelStyle>
        <color>ff00ff00</color>
      </LabelStyle>
    </Style>
    
    <!-- End Marker Style (Red) -->
    <Style id="endStyle">
      <IconStyle>
        <color>ffff0000</color>  <!-- Red -->
        <scale>1.2</scale>
      </IconStyle>
      <LabelStyle>
        <color>ffff0000</color>
      </LabelStyle>
    </Style>
    
    <!-- START MARKER -->
    <Placemark>
      <name>START</name>
      <description>Start</description>
      <styleUrl>#startStyle</styleUrl>
      <Point>
        <altitudeMode>clampToGround</altitudeMode>
        <coordinates>$firstCoordinate</coordinates>
      </Point>
    </Placemark>
    
    <Placemark>
      <name>Flight Path</name>
      <styleUrl>#trackStyle</styleUrl>
      <LineString>
        <tessellate>${tessellate.kmlBoolean()}</tessellate>
        <extrude>${extrude.kmlBoolean()}</extrude>
        <altitudeMode>${if (isAbsoluteAltitude) "absolute" else "clampToGround"}</altitudeMode>
        <coordinates>
            $coordinates
        </coordinates>
      </LineString>
    </Placemark>
    
    <!-- END MARKER -->
    <Placemark>
      <name>END</name>
      <description>End</description>
      <styleUrl>#endStyle</styleUrl>
      <Point>
        <altitudeMode>clampToGround</altitudeMode>
        <coordinates>$lastCoordinate</coordinates>
      </Point>
    </Placemark>
  </Document>
</kml>
""".trimIndent()

private fun Boolean.kmlBoolean() = if (this) "1" else "0"
private fun GeoPoint.kmlCoordinate(elevationOffset: Double = 0.0): String {
    val absAlt = alt?.let { it + elevationOffset }
    return "${longitude.formatDecimals(8)},${latitude.formatDecimals(8)},${absAlt?.roundToInt() ?: 0}"
}
fun Double.formatDecimals(decimals: Int): String {
    val multiplier = 10.0.pow(decimals.toDouble())
    val intPart = this.toInt()
    val decimalPart = this-intPart
    val roundedDecimalPart = (decimalPart * multiplier).roundToInt()
    val str = roundedDecimalPart.toString().padStart(decimals, '0')
    return "${intPart}.$str"
}

private fun Color.toAabbggrr(): String {
    val a = (alpha * 255).toInt().toString(16)
    val r = (red * 255).toInt().toString(16)
    val g = (green * 255).toInt().toString(16)
    val b = (blue * 255).toInt().toString(16)

    val aa = a.padStart(2, '0')
    val bb = b.padStart(2, '0')
    val gg = g.padStart(2, '0')
    val rr = r.padStart(2, '0')

    return "#$aa$bb$gg$rr".uppercase()
}

fun <T>List<T>.interpolateBy(value: (T) -> Double?, replace: (T, Double) -> T): List<T> {
    if (size < 2) return this

    val result = toMutableList()

    // Process forward to find interpolatable gaps
    var i = 0
    while (i < size - 1) {
        val startAlt = value(result[i])
        if (startAlt != null) {
            var j = i + 1
            // Skip nulls
            while (j < size && value(result[j]) == null) j++
            if (j < size && value(result[j]) != null) {
                val endAlt = requireNotNull(value(result[j]))
                // Interpolate from i to j (inclusive)
                val steps = j - i - 1  // Number of gaps between known points

                repeat(steps) { step ->
                    val t = (step + 1).toDouble() / (steps + 1.0)
                    val interpolatedAlt = startAlt + t * (endAlt - startAlt)
                    val k = i + step + 1
                    result[k] = replace(result[k], interpolatedAlt)
                }
                i = j  // Jump to end
            } else {
                i++
            }
        } else {
            i++
        }
    }

    return result
}

fun List<GeoPoint>.interpolateAltitudes(): List<GeoPoint> =
    interpolateBy(
        value = { it.alt },
        replace = { i, v -> i.copy(alt = v)}
    )
