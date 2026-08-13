package com.rituraj.motonav.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point

@Composable
fun MotoMap(
    latitude: Double?,
    longitude: Double?,
    routePoints: List<Pair<Double, Double>>,
    modifier: Modifier = Modifier
) {

    val mapReference = remember {
        mutableMapOf<String, MapLibreMap>()
    }

    AndroidView(
        modifier = modifier,

        factory = { context ->

            MapView(context).apply {

                onCreate(null)

                getMapAsync { map ->

                    mapReference["map"] = map

                    map.setStyle(
                        "https://tiles.openfreemap.org/styles/liberty"
                    ) { style ->

                        // --------------------------------------------------
                        // CURRENT LOCATION
                        // --------------------------------------------------

                        val locationSource = GeoJsonSource(
                            "current-location-source",
                            Feature.fromGeometry(
                                Point.fromLngLat(0.0, 0.0)
                            )
                        )

                        style.addSource(locationSource)

                        val locationLayer = CircleLayer(
                            "current-location-layer",
                            "current-location-source"
                        )

                        locationLayer.setProperties(
                            PropertyFactory.circleRadius(6f),
                            PropertyFactory.circleColor("#000000"),
                            PropertyFactory.circleStrokeWidth(2f),
                            PropertyFactory.circleStrokeColor("#FFFFFF")
                        )

                        style.addLayer(locationLayer)


                        // --------------------------------------------------
                        // ROUTE
                        // --------------------------------------------------

                        val routeSource = GeoJsonSource(
                            "route-source",
                            Feature.fromGeometry(
                                LineString.fromLngLats(
                                    emptyList()
                                )
                            )
                        )

                        style.addSource(routeSource)

                        val routeLayer = LineLayer(
                            "route-layer",
                            "route-source"
                        )

                        routeLayer.setProperties(
                            PropertyFactory.lineColor("#000000"),
                            PropertyFactory.lineWidth(5f),
                            PropertyFactory.lineOpacity(0.8f)
                        )

                        style.addLayer(routeLayer)
                    }
                }
            }
        },

        update = {

            val map = mapReference["map"]

            if (map != null) {

                // ----------------------------------------------------------
                // CURRENT LOCATION
                // ----------------------------------------------------------

                if (latitude != null && longitude != null) {

                    val currentLocation = LatLng(
                        latitude,
                        longitude
                    )

                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            currentLocation
                        )
                    )

                    val style = map.style

                    val locationSource =
                        style?.getSourceAs<GeoJsonSource>(
                            "current-location-source"
                        )

                    locationSource?.setGeoJson(
                        Feature.fromGeometry(
                            Point.fromLngLat(
                                longitude,
                                latitude
                            )
                        )
                    )
                }


                // ----------------------------------------------------------
                // ROUTE
                // ----------------------------------------------------------

                if (routePoints.isNotEmpty()) {

                    val style = map.style

                    val routeSource =
                        style?.getSourceAs<GeoJsonSource>(
                            "route-source"
                        )

                    val coordinates = routePoints.map {
                        Point.fromLngLat(
                            it.second,
                            it.first
                        )
                    }

                    routeSource?.setGeoJson(
                        Feature.fromGeometry(
                            LineString.fromLngLats(
                                coordinates
                            )
                        )
                    )
                }
            }
        }
    )
}