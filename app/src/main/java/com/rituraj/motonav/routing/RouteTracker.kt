package com.rituraj.motonav.routing

import kotlin.math.*

data class RouteProgress(
    val nearestPoint: RoutePoint,
    val distanceFromRouteMeters: Double,
    val distanceTravelledMeters: Double,
    val distanceRemainingMeters: Double,
    val progressPercent: Double
)

object RouteTracker {

    private const val EARTH_RADIUS_METERS = 6_371_000.0

    fun calculateProgress(
        currentLocation: RoutePoint,
        route: List<RoutePoint>
    ): RouteProgress? {

        if (route.size < 2) {
            return null
        }

        var nearestPoint = route.first()
        var nearestDistance = Double.MAX_VALUE
        var nearestSegmentIndex = 0

        // Find the closest point on the route.
        for (i in 0 until route.size - 1) {

            val projectedPoint = projectPointOntoSegment(
                currentLocation,
                route[i],
                route[i + 1]
            )

            val distance = distanceMeters(
                currentLocation,
                projectedPoint
            )

            if (distance < nearestDistance) {

                nearestDistance = distance
                nearestPoint = projectedPoint
                nearestSegmentIndex = i
            }
        }

        // Calculate distance from route start to nearest point.
        var distanceTravelled = 0.0

        for (i in 0 until nearestSegmentIndex) {

            distanceTravelled += distanceMeters(
                route[i],
                route[i + 1]
            )
        }

        distanceTravelled += distanceMeters(
            route[nearestSegmentIndex],
            nearestPoint
        )

        // Calculate total route length.
        var totalDistance = 0.0

        for (i in 0 until route.size - 1) {

            totalDistance += distanceMeters(
                route[i],
                route[i + 1]
            )
        }

        val distanceRemaining =
            (totalDistance - distanceTravelled)
                .coerceAtLeast(0.0)

        val progressPercent =
            if (totalDistance > 0.0) {
                (distanceTravelled / totalDistance) * 100.0
            } else {
                0.0
            }

        return RouteProgress(
            nearestPoint = nearestPoint,
            distanceFromRouteMeters = nearestDistance,
            distanceTravelledMeters = distanceTravelled,
            distanceRemainingMeters = distanceRemaining,
            progressPercent = progressPercent
        )
    }

    private fun distanceMeters(
        first: RoutePoint,
        second: RoutePoint
    ): Double {

        val lat1 = Math.toRadians(first.latitude)
        val lat2 = Math.toRadians(second.latitude)

        val deltaLat =
            Math.toRadians(second.latitude - first.latitude)

        val deltaLon =
            Math.toRadians(second.longitude - first.longitude)

        val a =
            sin(deltaLat / 2).pow(2) +
                    cos(lat1) *
                    cos(lat2) *
                    sin(deltaLon / 2).pow(2)

        val c =
            2 * atan2(
                sqrt(a),
                sqrt(1 - a)
            )

        return EARTH_RADIUS_METERS * c
    }

    private fun projectPointOntoSegment(
        point: RoutePoint,
        segmentStart: RoutePoint,
        segmentEnd: RoutePoint
    ): RoutePoint {

        val referenceLatitude =
            Math.toRadians(point.latitude)

        val metersPerDegreeLatitude =
            111_320.0

        val metersPerDegreeLongitude =
            111_320.0 * cos(referenceLatitude)

        fun x(routePoint: RoutePoint): Double {
            return routePoint.longitude *
                    metersPerDegreeLongitude
        }

        fun y(routePoint: RoutePoint): Double {
            return routePoint.latitude *
                    metersPerDegreeLatitude
        }

        val px = x(point)
        val py = y(point)

        val ax = x(segmentStart)
        val ay = y(segmentStart)

        val bx = x(segmentEnd)
        val by = y(segmentEnd)

        val dx = bx - ax
        val dy = by - ay

        val lengthSquared =
            dx * dx + dy * dy

        if (lengthSquared == 0.0) {
            return segmentStart
        }

        var t =
            ((px - ax) * dx +
                    (py - ay) * dy) /
                    lengthSquared

        t = t.coerceIn(0.0, 1.0)

        return RoutePoint(
            latitude =
                segmentStart.latitude +
                        t * (
                        segmentEnd.latitude -
                                segmentStart.latitude
                        ),

            longitude =
                segmentStart.longitude +
                        t * (
                        segmentEnd.longitude -
                                segmentStart.longitude
                        )
        )
    }
}