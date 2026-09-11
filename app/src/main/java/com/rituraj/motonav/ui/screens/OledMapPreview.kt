package com.rituraj.motonav.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.layout.ContentScale
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import com.rituraj.motonav.routing.RoutePoint

@Composable
fun OledMapPreview(
    latitude: Double?,
    longitude: Double?,
    routePoints: List<RoutePoint>,
    destinationLatitude: Double?,
    destinationLongitude: Double?,
    modifier: Modifier = Modifier
) {

    val context = androidx.compose.ui.platform.LocalContext.current

    var oledBitmap by remember {
        mutableStateOf<Bitmap?>(null)
    }

    LaunchedEffect(
        latitude,
        longitude,
        routePoints,
        destinationLatitude,
        destinationLongitude
    ) {

        if (latitude == null || longitude == null) {
            return@LaunchedEffect
        }

        try {

            val routeCoordinates = routePoints.map {
                Point.fromLngLat(
                    it.longitude,
                    it.latitude
                )
            }

            val styleBuilder =
                Style.Builder()
                    .fromUri(
                        "https://tiles.openfreemap.org/styles/liberty"
                    )

            // ----------------------------------------------------------
            // ROUTE
            // ----------------------------------------------------------

            if (routeCoordinates.size >= 2) {

                val routeSource = GeoJsonSource(
                    "oled-route-source",
                    Feature.fromGeometry(
                        LineString.fromLngLats(
                            routeCoordinates
                        )
                    )
                )

                val routeLayer = LineLayer(
                    "oled-route-layer",
                    "oled-route-source"
                )

                routeLayer.setProperties(
                    PropertyFactory.lineColor("#000000"),
                    PropertyFactory.lineWidth(5f),
                    PropertyFactory.lineOpacity(1f)
                )

                styleBuilder
                    .withSource(routeSource)
                    .withLayer(routeLayer)
            }

            // ----------------------------------------------------------
            // CURRENT LOCATION
            // ----------------------------------------------------------

            val locationSource = GeoJsonSource(
                "oled-location-source",
                Feature.fromGeometry(
                    Point.fromLngLat(
                        longitude,
                        latitude
                    )
                )
            )

            val locationLayer = CircleLayer(
                "oled-location-layer",
                "oled-location-source"
            )

            locationLayer.setProperties(
                PropertyFactory.circleRadius(5f),
                PropertyFactory.circleColor("#000000"),
                PropertyFactory.circleStrokeWidth(2f),
                PropertyFactory.circleStrokeColor("#FFFFFF")
            )

            styleBuilder
                .withSource(locationSource)
                .withLayer(locationLayer)

            // ----------------------------------------------------------
            // DESTINATION
            // ----------------------------------------------------------

            if (
                destinationLatitude != null &&
                destinationLongitude != null
            ) {

                val destinationSource = GeoJsonSource(
                    "oled-destination-source",
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            destinationLongitude,
                            destinationLatitude
                        )
                    )
                )

                val destinationLayer = CircleLayer(
                    "oled-destination-layer",
                    "oled-destination-source"
                )

                destinationLayer.setProperties(
                    PropertyFactory.circleRadius(6f),
                    PropertyFactory.circleColor("#FFFFFF"),
                    PropertyFactory.circleStrokeWidth(2f),
                    PropertyFactory.circleStrokeColor("#000000")
                )

                styleBuilder
                    .withSource(destinationSource)
                    .withLayer(destinationLayer)
            }

            // ----------------------------------------------------------
            // SNAPSHOT
            // ----------------------------------------------------------

            val options =
                MapSnapshotter.Options(
                    128,
                    64
                )
                    .withStyleBuilder(
                        styleBuilder
                    )
                    .withCameraPosition(
                        CameraPosition.Builder()
                            .target(
                                LatLng(
                                    latitude,
                                    longitude
                                )
                            )
                            .zoom(15.0)
                            .build()
                    )

            val snapshotter =
                MapSnapshotter(
                    context,
                    options
                )

            snapshotter.start(
                object : MapSnapshotter.SnapshotReadyCallback {

                    override fun onSnapshotReady(
                        snapshot: org.maplibre.android.snapshotter.MapSnapshot
                    ) {

                        oledBitmap =
                            convertToMonochrome(
                                snapshot.bitmap
                            )

                        snapshotter.cancel()
                    }
                },

                MapSnapshotter.ErrorHandler {

                    snapshotter.cancel()
                }
            )

        } catch (e: Exception) {

            println(
                "OLED SNAPSHOT ERROR: ${e.message}"
            )
        }
    }

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        oledBitmap?.let { bitmap ->

            Image(
                bitmap = bitmap.asImageBitmap(),
                contentDescription = "128x64 OLED preview",
                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),
                contentScale = ContentScale.FillBounds
            )

        } ?: Text(
            text = "OLED PREVIEW\nWaiting for GPS...",
            color = Color.White
        )
    }
}


// ----------------------------------------------------------------------
// Convert bitmap to 1-bit black/white
// ----------------------------------------------------------------------

private fun convertToMonochrome(
    source: Bitmap
): Bitmap {

    val width = source.width
    val height = source.height

    val output =
        Bitmap.createBitmap(
            width,
            height,
            Bitmap.Config.ARGB_8888
        )

    for (y in 0 until height) {

        for (x in 0 until width) {

            val pixel =
                source.getPixel(
                    x,
                    y
                )

            val red =
                android.graphics.Color.red(pixel)

            val green =
                android.graphics.Color.green(pixel)

            val blue =
                android.graphics.Color.blue(pixel)

            // Standard luminance calculation.
            val brightness =
                (
                        0.299 * red +
                                0.587 * green +
                                0.114 * blue
                        )

            val outputColor =
                if (brightness < 128) {
                    android.graphics.Color.BLACK
                } else {
                    android.graphics.Color.WHITE
                }

            output.setPixel(
                x,
                y,
                outputColor
            )
        }
    }

    return output
}