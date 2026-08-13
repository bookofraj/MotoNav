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
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.Point
import org.maplibre.android.style.layers.PropertyFactory

@Composable
fun MotoMap(
    latitude: Double?,
    longitude: Double?,
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

                        // Source containing our current position
                        val locationSource = GeoJsonSource(
                            "current-location-source",
                            Feature.fromGeometry(
                                Point.fromLngLat(0.0, 0.0)
                            )
                        )

                        style.addSource(locationSource)

                        // Small circle representing the rider
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
                    }
                }
            }
        },

        update = {

            val map = mapReference["map"]

            if (map != null && latitude != null && longitude != null) {

                val currentLocation = LatLng(
                    latitude,
                    longitude
                )

                // Move camera to current GPS position
                map.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        currentLocation
                    )
                )

                // Update marker position
                val style = map.style

                val source =
                    style?.getSourceAs<GeoJsonSource>(
                        "current-location-source"
                    )

                source?.setGeoJson(
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            longitude,
                            latitude
                        )
                    )
                )
            }
        }
    )
}