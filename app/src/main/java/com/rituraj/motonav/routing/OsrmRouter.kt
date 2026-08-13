package com.rituraj.motonav.routing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject

class OsrmRouter {

    private val client = OkHttpClient()

    suspend fun getRoute(
        startLatitude: Double,
        startLongitude: Double,
        destinationLatitude: Double,
        destinationLongitude: Double
    ): List<Pair<Double, Double>> = withContext(Dispatchers.IO) {

        val url =
            "https://router.project-osrm.org/route/v1/driving/" +
                    "$startLongitude,$startLatitude;" +
                    "$destinationLongitude,$destinationLatitude" +
                    "?overview=full&geometries=geojson"

        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        client.newCall(request).execute().use { response ->

            if (!response.isSuccessful) {
                throw Exception(
                    "OSRM request failed: ${response.code}"
                )
            }

            val responseBody = response.body?.string()
                ?: throw Exception("Empty OSRM response")

            val json = JSONObject(responseBody)

            val routes = json.getJSONArray("routes")

            if (routes.length() == 0) {
                throw Exception("No route found")
            }

            val geometry =
                routes
                    .getJSONObject(0)
                    .getJSONObject("geometry")

            val coordinates =
                geometry.getJSONArray("coordinates")

            val routePoints = mutableListOf<Pair<Double, Double>>()

            for (i in 0 until coordinates.length()) {

                val coordinate =
                    coordinates.getJSONArray(i)

                val longitude = coordinate.getDouble(0)
                val latitude = coordinate.getDouble(1)

                routePoints.add(
                    Pair(latitude, longitude)
                )
            }

            routePoints
        }
    }
}