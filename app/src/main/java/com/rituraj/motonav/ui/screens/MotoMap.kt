package com.rituraj.motonav.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
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
import com.rituraj.motonav.routing.RoutePoint

@Composable
fun MotoMap(
    latitude: Double?,
    longitude: Double?,
    routePoints: List<RoutePoint>,
    destinationLatitude: Double?,
    destinationLongitude: Double?,
    recenterRequest: Int,
    onMapTap: (latitude: Double, longitude: Double) -> Unit,
    onUserMovedMap: () -> Unit,
    modifier: Modifier = Modifier
) {

    val mapReference = remember {
        mutableMapOf<String, MapLibreMap>()
    }

    // --------------------------------------------------------------
    // CAMERA STATE
    // --------------------------------------------------------------

    // Makes sure the first GPS fix centers the map only once.
    val hasInitiallyCentered = remember {
        mutableStateOf(false)
    }

    // Used to distinguish our own camera movements from
    // movements caused by the user dragging the map.
    val isProgrammaticCameraMove = remember {
        mutableStateOf(false)
    }

    // Keeps track of the last recenter request we processed.
    val lastHandledRecenterRequest = remember {
        mutableStateOf(0)
    }

    AndroidView(
        modifier = modifier,

        factory = { context ->

            MapView(context).apply {

                onCreate(null)

                getMapAsync { map ->

                    mapReference["map"] = map

                    // --------------------------------------------------
                    // MAP TAP
                    // --------------------------------------------------

                    map.addOnMapClickListener { point ->

                        onMapTap(
                            point.latitude,
                            point.longitude
                        )

                        true
                    }

                    // --------------------------------------------------
                    // CAMERA IDLE
                    // --------------------------------------------------
                    //
                    // We use camera-idle instead of REASON_GESTURE
                    // because that constant is not available in our
                    // MapLibre version.
                    //
                    // If the camera movement was NOT initiated by us,
                    // then it was a user movement.
                    //

                    map.addOnCameraIdleListener {

                        if (isProgrammaticCameraMove.value) {

                            isProgrammaticCameraMove.value = false

                        } else {

                            onUserMovedMap()
                        }
                    }

                    // --------------------------------------------------
                    // MAP STYLE
                    // --------------------------------------------------

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

                        // --------------------------------------------------
                        // DESTINATION
                        // --------------------------------------------------

                        val destinationSource = GeoJsonSource(
                            "destination-source",
                            Feature.fromGeometry(
                                Point.fromLngLat(0.0, 0.0)
                            )
                        )

                        style.addSource(destinationSource)

                        val destinationLayer = CircleLayer(
                            "destination-layer",
                            "destination-source"
                        )

                        destinationLayer.setProperties(
                            PropertyFactory.circleRadius(7f),
                            PropertyFactory.circleColor("#FFFFFF"),
                            PropertyFactory.circleStrokeWidth(2f),
                            PropertyFactory.circleStrokeColor("#000000")
                        )

                        style.addLayer(destinationLayer)
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

                    // ------------------------------------------------------
                    // INITIAL CENTER
                    // ------------------------------------------------------
                    //
                    // Only center the map once, on the first GPS fix.
                    //

                    if (!hasInitiallyCentered.value) {

                        isProgrammaticCameraMove.value = true

                        map.animateCamera(
                            CameraUpdateFactory.newLatLng(
                                currentLocation
                            )
                        )

                        hasInitiallyCentered.value = true
                    }

                    // ------------------------------------------------------
                    // UPDATE LOCATION MARKER
                    // ------------------------------------------------------

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
                            it.longitude,
                            it.latitude
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

                // ----------------------------------------------------------
                // DESTINATION
                // ----------------------------------------------------------

                if (
                    destinationLatitude != null &&
                    destinationLongitude != null
                ) {

                    val style = map.style

                    val destinationSource =
                        style?.getSourceAs<GeoJsonSource>(
                            "destination-source"
                        )

                    destinationSource?.setGeoJson(
                        Feature.fromGeometry(
                            Point.fromLngLat(
                                destinationLongitude,
                                destinationLatitude
                            )
                        )
                    )
                }

                // ----------------------------------------------------------
                // RECENTER
                // ----------------------------------------------------------
                //
                // Process each recenter request exactly ONCE.
                //
                // This is important because update() can run every time
                // GPS changes.
                //

                if (
                    recenterRequest > 0 &&
                    recenterRequest != lastHandledRecenterRequest.value &&
                    latitude != null &&
                    longitude != null
                ) {

                    lastHandledRecenterRequest.value =
                        recenterRequest

                    isProgrammaticCameraMove.value = true

                    map.animateCamera(
                        CameraUpdateFactory.newLatLng(
                            LatLng(
                                latitude,
                                longitude
                            )
                        )
                    )
                }
            }
        }
    )
}