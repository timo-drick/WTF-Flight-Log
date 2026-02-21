package de.drick.compose.tilemap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.sinh
import kotlin.math.tan

fun GpsPoint.toTilePos(zoom: Int): TilePos {
    val n = 1 shl zoom
    val latRad = latitude.toRadians()
    return TilePos(
        zoom = zoom,
        x = (longitude + 180.0) / 360.0 * n,
        y = (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n
    )
}

fun TilePos.toGeoPoint() = GpsPoint(
    latitude = tileYToLat(y, zoom),
    longitude = tileXToLon(x, zoom)
)

private fun tileXToLon(x: Double, zoom: Int): Double {
    val n = 1 shl zoom
    return x / n * 360.0 - 180.0
}

private fun tileYToLat(y: Double, zoom: Int): Double {
    val n = 1 shl zoom
    val latRad = atan(sinh(PI * (1 - 2.0 * y / n)))
    return latRad.toDegrees()
}

fun Double.toRadians(): Double = this / 180.0 * PI
fun Double.toDegrees(): Double = this * 180.0 / PI

// https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84
val earthRadius = 6378137.0 // in meters WGS84

// EPSG:4326 lon,lat

// EPSG:3857 meters mercator projection

data class MeterPos(val x: Double, val y: Double)

fun GpsPoint.toMeter() = convert4326To3857(this)
fun MeterPos.toGeoPoint() = convert3857To4326(this)

private fun convert4326To3857(gpsPoint: GpsPoint) = MeterPos(
    x = earthRadius * gpsPoint.longitude.toRadians(),
    y = earthRadius * ln(tan(PI / 4 + gpsPoint.latitude.toRadians() / 2))
)

private fun convert3857To4326(meterPos: MeterPos) =  GpsPoint(
    latitude = (2 * atan(exp(meterPos.y / earthRadius)) - PI / 2).toDegrees(),
    longitude = (meterPos.x / earthRadius).toDegrees()
)
