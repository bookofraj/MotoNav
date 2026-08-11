package com.rituraj.motonav.ui.screens

import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.rituraj.motonav.location.MotoLocationManager

@Composable
fun NavigationScreen() {

    val context = LocalContext.current

    var latitude by remember {
        mutableStateOf<Double?>(null)
    }

    var longitude by remember {
        mutableStateOf<Double?>(null)
    }

    val locationManager = remember {
        MotoLocationManager(context)
    }

    DisposableEffect(Unit) {
        onDispose {
            locationManager.stopLocationUpdates()
        }
    }

    val hasLocationPermission =
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED

    LaunchedEffect(hasLocationPermission) {

        if (hasLocationPermission) {

            locationManager.startLocationUpdates { lat, lon ->

                latitude = lat
                longitude = lon
            }
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {

        Text(
            text = "MotoNav",
            color = Color.White,
            fontSize = 20.sp,
            modifier = Modifier.padding(16.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
        ) {

            MotoMap(
                latitude = latitude,
                longitude = longitude,
                modifier = Modifier.fillMaxSize()
            )
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(70.dp)
                .background(Color.Black)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

            if (latitude != null && longitude != null) {

                Column {

                    Text(
                        text = "LAT: %.6f".format(latitude),
                        color = Color.White,
                        fontSize = 13.sp
                    )

                    Text(
                        text = "LON: %.6f".format(longitude),
                        color = Color.White,
                        fontSize = 13.sp
                    )
                }

            } else {

                Text(
                    text = "GPS: Waiting...",
                    color = Color.White,
                    fontSize = 14.sp
                )
            }

            Text(
                text = "BLE: Disconnected",
                color = Color.White,
                fontSize = 14.sp
            )
        }
    }
}