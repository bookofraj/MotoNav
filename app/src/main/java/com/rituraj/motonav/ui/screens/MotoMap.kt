package com.rituraj.motonav.ui.screens

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.maplibre.android.maps.MapView

@Composable
fun MotoMap(
    modifier: Modifier = Modifier
) {

    AndroidView(
        modifier = modifier,

        factory = { context ->

            MapView(context).apply {

                onCreate(null)

                getMapAsync { map ->

                    map.setStyle(
                        "https://demotiles.maplibre.org/style.json"
                    )
                }
            }
        },

        update = {
            // Nothing to update yet.
        }
    )
}