package de.drick.compose.tilemap

import kotlin.math.PI
import kotlin.math.atan
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.ln
import kotlin.math.tan

fun GeoPoint.toTilePos(zoom: Int): TilePos {
    val n = 1 shl zoom
    val latRad = latitude.toRadians()
    return TilePos(
        zoom = zoom,
        x = (longitude + 180.0) / 360.0 * n,
        y = (1.0 - ln(tan(latRad) + 1 / cos(latRad)) / PI) / 2.0 * n
    )
}

fun metersToTileLength(geoPoint: GeoPoint, zoom: Int, meters: Double): Double {
    val n = 1 shl zoom
    val latRad = geoPoint.latitude.toRadians()
    return meters * n / (2.0 * PI * EARTH_RADIUS_WGS84 * cos(latRad))
}

// https://en.wikipedia.org/wiki/World_Geodetic_System#WGS84
const val EARTH_RADIUS_WGS84 = 6378137.0 // in meters WGS84

// EPSG:4326 lon,lat

// EPSG:3857 meters mercator projection

data class MeterPos(val x: Double, val y: Double)

fun GeoPoint.toMeter() = convert4326To3857(this)
fun MeterPos.toGeoPoint() = convert3857To4326(this)

private fun convert4326To3857(geoPoint: GeoPoint) = MeterPos(
    x = EARTH_RADIUS_WGS84 * geoPoint.longitude.toRadians(),
    y = EARTH_RADIUS_WGS84 * ln(tan(PI / 4 + geoPoint.latitude.toRadians() / 2))
)

private fun convert3857To4326(meterPos: MeterPos) =  GeoPoint(
    latitude = (2 * atan(exp(meterPos.y / EARTH_RADIUS_WGS84)) - PI / 2).toDegrees(),
    longitude = (meterPos.x / EARTH_RADIUS_WGS84).toDegrees()
)
