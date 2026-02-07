package de.drick.wtf_osd

import kotlin.math.*

const val EARTH_RADIUS = 6373000.0 // Used in Mapbox for meter calculation

data class GeoPoint(val latitude: Double, val longitude: Double)

fun degreesToRadians(degrees: Double): Double = (degrees % 360) * PI / 180
fun radiansToDegree(radians: Double): Double = radians * 180.0 / PI
fun radiansToMeters(radians: Double): Double = radians * EARTH_RADIUS
fun metersToRadians(meters: Double): Double = meters / EARTH_RADIUS

/**
 * Calculating the Distance Between Two GPS Coordinates with Haversine Formula
 * From: https://nathanrooy.github.io/posts/2016-09-07/haversine-with-python/
 */
fun calculateGeoDistance(p1: GeoPoint, p2: GeoPoint): Double {
    val deltaPhi = degreesToRadians(p2.latitude - p1.latitude)
    val deltaLambda = degreesToRadians(p2.longitude - p1.longitude)
    val phi1 = degreesToRadians(p1.latitude)
    val phi2 = degreesToRadians(p2.latitude)
    val a = (sin(deltaPhi / 2.0).pow(2.0) + sin(deltaLambda / 2).pow(2.0) * cos(phi1) * cos(phi2))
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
        line1Start = perpendicularPt1.toVec2(),
        line1Stop = perpendicularPt2.toVec2(),
        line2Start = start.toVec2(),
        line2Stop = stop.toVec2()
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
    val lat2 = asin(sin(lat1) * cos(radians) + cos(lat1) * sin(radians) * cos(bearingRad))
    val long2 = long1 + atan2(sin(bearingRad) * sin(radians) * cos(lat1), cos(radians) - sin(lat1) * sin(lat2))
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
private fun lineIntersects(line1Start: Vec2, line1Stop: Vec2, line2Start: Vec2, line2Stop: Vec2): Vec2? {
    // 1 -> line1Start
    // 2 -> line1Stop
    // 3 -> line2Start
    // 4 -> line2Stop
    // denom = (y4-y3) * (x2-x1)  -  (x4-x3) * (y2-y1)
    val denom = (line2Stop.y - line2Start.y) * (line1Stop.x - line1Start.x) - (line2Stop.x - line2Start.x) * (line1Stop.y - line1Start.y)
    // numeA = (x4-x3) * (y1-y3)  -  (y4-y3) * (x1-x3)
    val numeA = (line2Stop.x - line2Start.x) * (line1Start.y - line2Start.y) - (line2Stop.y - line2Start.y) * (line1Start.x - line2Start.x)
    // numeB = (x2-x1) * (y1-y3)  -  (y2-y1) * (x1-x3)
    val numeB = (line1Stop.x - line1Start.x) * (line1Start.y - line2Start.y) - (line1Stop.y - line1Start.y) * (line1Start.x - line2Start.x)

    if (denom == 0.0) {
        return null
    }

    val uA = numeA / denom
    val uB = numeB / denom

    if (uA in 0.0..1.0 && uB in 0.0..1.0) {
        return Vec2(
            x = line1Start.x + uA * (line1Stop.x - line1Start.x),
            y = line1Start.y + uA * (line1Stop.y - line1Start.y)
        )
    }
    return null
}