package com.rituraj.motonav.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.camera.CameraUpdateFactory
import org.maplibre.android.geometry.LatLng
import org.maplibre.android.maps.MapLibreMap
import org.maplibre.android.maps.MapView

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
                        "https://demotiles.maplibre.org/style.json"
                    )
                }
            }
        },

        update = {

            val map = mapReference["map"]

            if (map != null && latitude != null && longitude != null) {

                map.animateCamera(
                    CameraUpdateFactory.newLatLng(
                        LatLng(latitude, longitude)
                    )
                )
            }
        }
    )
}