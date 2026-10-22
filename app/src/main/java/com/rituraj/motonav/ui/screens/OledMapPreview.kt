package com.rituraj.motonav.ui.screens

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.rituraj.motonav.routing.RoutePoint
import kotlinx.coroutines.delay
import org.maplibre.android.camera.CameraPosition
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.Style
import org.maplibre.android.snapshotter.MapSnapshotter
import org.maplibre.android.style.layers.CircleLayer
import org.maplibre.android.style.layers.LineLayer
import org.maplibre.android.style.layers.PropertyFactory
import org.maplibre.android.style.sources.GeoJsonSource
import org.maplibre.geojson.Feature
import org.maplibre.geojson.LineString
import org.maplibre.geojson.Point
import com.rituraj.motonav.OledFrameSender
import kotlinx.coroutines.launch
import androidx.compose.runtime.rememberCoroutineScope

@Composable
fun OledMapPreview(
    latitude: Double?,
    longitude: Double?,
    routePoints: List<RoutePoint>,
    destinationLatitude: Double?,
    destinationLongitude: Double?,
    modifier: Modifier = Modifier
) {
    val coroutineScope = rememberCoroutineScope()

    val context = LocalContext.current

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

            // ----------------------------------------------------------
            // BUILD MAP STYLE
            // ----------------------------------------------------------

            val styleBuilder =
                Style.Builder()
                    .fromUri(
                        "https://tiles.openfreemap.org/styles/liberty"
                    )

            // ----------------------------------------------------------
            // ROUTE
            // ----------------------------------------------------------

            if (routePoints.size >= 2) {

                val routeCoordinates =
                    routePoints.map {
                        Point.fromLngLat(
                            it.longitude,
                            it.latitude
                        )
                    }

                val routeSource =
                    GeoJsonSource(
                        "oled-route-source",
                        Feature.fromGeometry(
                            LineString.fromLngLats(
                                routeCoordinates
                            )
                        )
                    )

                val routeLayer =
                    LineLayer(
                        "oled-route-layer",
                        "oled-route-source"
                    )

                routeLayer.setProperties(

                    // Dark route for monochrome conversion.
                    PropertyFactory.lineColor(
                        "#000000"
                    ),

                    PropertyFactory.lineWidth(
                        2.5f
                    ),

                    PropertyFactory.lineOpacity(
                        1f
                    )
                )

                styleBuilder
                    .withSource(routeSource)
                    .withLayer(routeLayer)
            }

            // ----------------------------------------------------------
            // CURRENT LOCATION
            // ----------------------------------------------------------

            val locationSource =
                GeoJsonSource(
                    "oled-location-source",
                    Feature.fromGeometry(
                        Point.fromLngLat(
                            longitude,
                            latitude
                        )
                    )
                )

            val locationLayer =
                CircleLayer(
                    "oled-location-layer",
                    "oled-location-source"
                )

            locationLayer.setProperties(

                PropertyFactory.circleRadius(
                    4f
                ),

                PropertyFactory.circleColor(
                    "#000000"
                ),

                PropertyFactory.circleStrokeWidth(
                    2f
                ),

                PropertyFactory.circleStrokeColor(
                    "#FFFFFF"
                )
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

                val destinationSource =
                    GeoJsonSource(
                        "oled-destination-source",
                        Feature.fromGeometry(
                            Point.fromLngLat(
                                destinationLongitude,
                                destinationLatitude
                            )
                        )
                    )

                val destinationLayer =
                    CircleLayer(
                        "oled-destination-layer",
                        "oled-destination-source"
                    )

                destinationLayer.setProperties(

                    PropertyFactory.circleRadius(
                        5f
                    ),

                    PropertyFactory.circleColor(
                        "#FFFFFF"
                    ),

                    PropertyFactory.circleStrokeWidth(
                        2f
                    ),

                    PropertyFactory.circleStrokeColor(
                        "#000000"
                    )
                )

                styleBuilder
                    .withSource(destinationSource)
                    .withLayer(destinationLayer)
            }

            // ----------------------------------------------------------
            // SNAPSHOT CAMERA
            // ----------------------------------------------------------

            val cameraPosition =
                CameraPosition.Builder()
                    .target(
                        LatLng(
                            latitude,
                            longitude
                        )
                    )
                    .zoom(
                        15.0
                    )
                    .build()

            val options =
                MapSnapshotter.Options(
                    128,
                    64
                )
                    .withStyleBuilder(
                        styleBuilder
                    )
                    .withCameraPosition(
                        cameraPosition
                    )

            // ----------------------------------------------------------
            // CREATE SNAPSHOT
            // ----------------------------------------------------------

            val snapshotter =
                MapSnapshotter(
                    context,
                    options
                )

            snapshotter.start(

                object :
                    MapSnapshotter.SnapshotReadyCallback {

                    override fun onSnapshotReady(
                        snapshot:
                        org.maplibre.android.snapshotter.MapSnapshot
                    ) {

//                        oledBitmap =
//                            convertToMonochrome(
//                                snapshot.bitmap
//                            )
                        val monochromeBitmap = convertToMonochrome(snapshot.bitmap)

                        oledBitmap = monochromeBitmap

                        coroutineScope.launch {
                            try {
                                val result = OledFrameSender.sendFrame(
                                    bitmap = monochromeBitmap,
                                    espIpAddress = "10.154.224.214" // Replace with your ESP8266's actual IP
                                )

                                println("OLED SEND SUCCESS: $result")
                            } catch (e: Exception) {
                                println("OLED SEND ERROR: ${e.message}")
                            }
                        }

                        snapshotter.cancel()
                    }
                },

                MapSnapshotter.ErrorHandler {

                    println(
                        "OLED SNAPSHOT ERROR"
                    )

                    snapshotter.cancel()
                }
            )

        } catch (e: Exception) {

            println(
                "OLED SNAPSHOT ERROR: ${e.message}"
            )
        }
    }

    // --------------------------------------------------------------
    // PREVIEW
    // --------------------------------------------------------------

    Box(
        modifier = modifier
            .background(Color.Black),
        contentAlignment = Alignment.Center
    ) {

        oledBitmap?.let { bitmap ->

            Image(
                bitmap = bitmap.asImageBitmap(),

                contentDescription =
                    "128x64 OLED preview",

                modifier = Modifier
                    .fillMaxSize()
                    .padding(20.dp),

                contentScale =
                    ContentScale.FillBounds
            )

        } ?: Text(
            text =
                "OLED PREVIEW\nWaiting for GPS...",

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

    // Local contrast required for a pixel
    // to become black.
    val contrastThreshold = 10.0

    // Radius of local brightness sampling.
    val radius = 2

    fun brightness(
        x: Int,
        y: Int
    ): Double {

        val pixel =
            source.getPixel(
                x,
                y
            )

        val red =
            android.graphics.Color.red(
                pixel
            )

        val green =
            android.graphics.Color.green(
                pixel
            )

        val blue =
            android.graphics.Color.blue(
                pixel
            )

        return 0.299 * red +
                0.587 * green +
                0.114 * blue
    }

    for (y in 0 until height) {

        for (x in 0 until width) {

            val currentBrightness =
                brightness(
                    x,
                    y
                )

            var surroundingBrightness =
                0.0

            var count =
                0

            for (dy in -radius..radius) {

                for (dx in -radius..radius) {

                    if (
                        dx == 0 &&
                        dy == 0
                    ) {
                        continue
                    }

                    val sampleX =
                        (x + dx)
                            .coerceIn(
                                0,
                                width - 1
                            )

                    val sampleY =
                        (y + dy)
                            .coerceIn(
                                0,
                                height - 1
                            )

                    surroundingBrightness +=
                        brightness(
                            sampleX,
                            sampleY
                        )

                    count++
                }
            }

            surroundingBrightness /=
                count

            val isDarkFeature =
                currentBrightness <
                        surroundingBrightness -
                        contrastThreshold

            val outputColor =
                if (isDarkFeature) {
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