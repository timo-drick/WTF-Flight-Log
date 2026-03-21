package de.drick.compose.tilemap

import kotlin.math.PI
import kotlin.math.asin
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

const val EARTH_RADIUS = 6373000.0 // Used in Mapbox for meter calculation

data class GeoPoint(val latitude: Double, val longitude: Double, val alt: Double? = null)

fun degreesToRadians(degrees: Double): Double = (degrees % 360) * PI / 180
fun radiansToDegree(radians: Double): Double = radians * 180.0 / PI
fun radiansToMeters(radians: Double): Double = radians * EARTH_RADIUS
fun metersToRadians(meters: Double): Double = meters / EARTH_RADIUS

fun List<GeoPoint>.calculateDistance() = zipWithNext { a, b -> a.distanceTo(b) }.sum()
fun GeoPoint.distanceTo(p: GeoPoint) = calculateGeoDistance(this, p)
fun GeoPoint.maxDistanceTo(list: List<GeoPoint>) = list.maxOf { distanceTo(it) }

/**
 * Calculating the Distance Between Two GPS Coordinates with Haversine Formula
 * From: https://nathanrooy.github.io/posts/2016-09-07/haversine-with-python/
 */
fun calculateGeoDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val deltaPhi = degreesToRadians(p2.latitude - p1.latitude)
    val deltaLambda = degreesToRadians(p2.longitude - p1.longitude)
    val phi1 = degreesToRadians(p1.latitude)
    val phi2 = degreesToRadians(p2.latitude)
    val a = (sin(deltaPhi / 2.0).pow(2.0)
            + sin(deltaLambda / 2).pow(2.0) * cos(phi1) * cos(phi2))
    return radiansToMeters(2 * atan2(sqrt(a), sqrt(1 - a)))
}

fun calculatePolylineDistance(geometry: List<GeoPoint>) = geometry
    .windowed(2, 1, false)
    .sumOf { calculateGeoDistance(it[0], it[1]) }

data class ProjectionData(
    val distanceFromLine: Double,
    val nearestPointOnLine: GeoPoint,
    val pointIndex: Int // point in geometry after the nearestPointOnLine is found
)

fun nearestPointOnPolyline(p1: GeoPoint, polyline: List<GeoPoint>): ProjectionData {
    require(polyline.isNotEmpty())
    var minDist = calculateGeoDistance(p1, polyline.first())
    var minPoint = polyline.first()
    var minIndex = 0
    var lastPoint: GeoPoint? = null
    polyline.forEachIndexed { index, p ->
        val lp = lastPoint
        val closestPoint = if (lp == null) p else nearestPointOnLine(p1, lp, p)
        val dist = calculateGeoDistance(p1, closestPoint)
        if (dist < minDist) { // closer point found
            minDist = dist
            minPoint = closestPoint
            minIndex = index - 1
        }
        lastPoint = p
    }
    return ProjectionData(minDist, minPoint, minIndex)
}

// https://github.com/Turfjs/turf/blob/master/packages/turf-nearest-point-on-line/index.ts
fun nearestPointOnLine(pt: GeoPoint, start: GeoPoint, stop: GeoPoint): GeoPoint {
    val startDist = calculateGeoDistance(pt, start)
    val stopDist = calculateGeoDistance(pt, stop)
    val heightDistance = max(startDist, stopDist)
    val direction = bearing(start, stop)
    val perpendicularPt1 = destination(pt, heightDistance, direction + 90.0)
    val perpendicularPt2 = destination(pt, heightDistance, direction - 90.0)
    val intersect = lineIntersects(
        l1a = perpendicularPt1.toVec2(),
        l1b = perpendicularPt2.toVec2(),
        l2a = start.toVec2(),
        l2b = stop.toVec2()
    )
    var closestPtOnLine = if (startDist < stopDist) start else stop
    val closestDist = min(startDist, stopDist)
    val intersectPt = intersect?.toGeoPoint()
    if (intersectPt != null) {
        val intersectDist = calculateGeoDistance(pt, intersectPt)
        if (intersectDist < closestDist) {
            closestPtOnLine = intersectPt
        }
    }
    return closestPtOnLine
}

fun destination(from: GeoPoint, distance: Double, bearing: Double) : GeoPoint {
    val long1 = degreesToRadians(from.longitude)
    val lat1 = degreesToRadians(from.latitude)
    val bearingRad = degreesToRadians(bearing)
    val radians = metersToRadians(distance)
    val lat2 = asin(sin(lat1) * cos(radians)
            + cos(lat1) * sin(radians) * cos(bearingRad))
    val long2 = long1 + atan2(
        y = sin(bearingRad) * sin(radians) * cos(lat1),
        x = cos(radians) - sin(lat1) * sin(lat2)
    )
    return GeoPoint(radiansToDegree(lat2), radiansToDegree(long2))
}

fun bearing(start: GeoPoint, end: GeoPoint, final: Boolean = false): Double {
    if (final) return calculateFinalBearing(start, end)

    val long1 = degreesToRadians(start.longitude)
    val lat1 = degreesToRadians(start.latitude)
    val long2 = degreesToRadians(end.longitude)
    val lat2 = degreesToRadians(end.latitude)

    val a = sin(long2 - long1) * cos(lat2)
    val b = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(long2 - long1)
    return radiansToDegree(atan2(a,b))
}

private fun calculateFinalBearing(start: GeoPoint, end: GeoPoint): Double {
    val bear = bearing(end, start)
    return (bear + 180.0) % 360.0
}


data class Vec2(val x: Double, val y: Double)

operator fun Vec2.minus(v: Vec2) = Vec2(x - v.x, y - v.y)

fun GeoPoint.toVec2() = Vec2(latitude, longitude)
fun Vec2.toGeoPoint() = GeoPoint(x, y)

// https://github.com/Turfjs/turf/blob/master/packages/turf-line-intersect/index.ts
private fun lineIntersects(
    l1a: Vec2, // line 1 start
    l1b: Vec2, // line 1 end
    l2a: Vec2, // line 2 start
    l2b: Vec2  // line 2 end
): Vec2? {
    // denom = (y4-y3) * (x2-x1)  -  (x4-x3) * (y2-y1)
    val denom = (l2b.y - l2a.y) * (l1b.x - l1a.x) - (l2b.x - l2a.x) * (l1b.y - l1a.y)
    // numeA = (x4-x3) * (y1-y3)  -  (y4-y3) * (x1-x3)
    val numeA = (l2b.x - l2a.x) * (l1a.y - l2a.y) - (l2b.y - l2a.y) * (l1a.x - l2a.x)
    // numeB = (x2-x1) * (y1-y3)  -  (y2-y1) * (x1-x3)
    val numeB = (l1b.x - l1a.x) * (l1a.y - l2a.y) - (l1b.y - l1a.y) * (l1a.x - l2a.x)

    if (denom == 0.0) {
        return null
    }

    val uA = numeA / denom
    val uB = numeB / denom

    if (uA in 0.0..1.0 && uB in 0.0..1.0) {
        return Vec2(
            x = l1a.x + uA * (l1b.x - l1a.x),
            y = l1a.y + uA * (l1b.y - l1a.y)
        )
    }
    return null
}
