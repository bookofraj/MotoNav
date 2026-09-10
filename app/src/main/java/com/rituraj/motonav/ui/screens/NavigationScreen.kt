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
import androidx.compose.runtime.rememberCoroutineScope
import com.rituraj.motonav.routing.OsrmRouter
import kotlinx.coroutines.launch
import com.rituraj.motonav.routing.RoutePoint
import com.rituraj.motonav.routing.RouteProgress
import com.rituraj.motonav.routing.RouteTracker
import androidx.compose.material3.Button

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

    var routePoints by remember {
        mutableStateOf<List<RoutePoint>>(emptyList())
    }

    var destinationLatitude by remember {
        mutableStateOf<Double?>(null)
    }

    var destinationLongitude by remember {
        mutableStateOf<Double?>(null)
    }

    val osrmRouter = remember {
        OsrmRouter()
    }

    val coroutineScope = rememberCoroutineScope()

    var routeProgress by remember {
        mutableStateOf<RouteProgress?>(null)
    }

    var isFollowingLocation by remember {
        mutableStateOf(true)
    }

    var recenterRequest by remember {
        mutableStateOf(0)
    }

//    var routeRequested by remember {
//        mutableStateOf(false)
//    }
//
//    LaunchedEffect(latitude, longitude) {
//
//        if (
//            latitude != null &&
//            longitude != null &&
//            !routeRequested
//        ) {
//
//            routeRequested = true
//
//            coroutineScope.launch {
//
//                try {
//
//                    val route = osrmRouter.getRoute(
//                        startLatitude = latitude!!,
//                        startLongitude = longitude!!,
//                        destinationLatitude = latitude!! + 0.01,
//                        destinationLongitude = longitude!! + 0.01
//                    )
//
//                    routePoints = route
//
//                    println(
//                        "OSRM ROUTE POINTS: ${route.size}"
//                    )
//
//                } catch (e: Exception) {
//
//                    println(
//                        "OSRM ERROR: ${e.message}"
//                    )
//                }
//            }
//        }
//    }

    LaunchedEffect(
        destinationLatitude,
        destinationLongitude,
        latitude,
        longitude
    ) {

        if (
            latitude != null &&
            longitude != null &&
            destinationLatitude != null &&
            destinationLongitude != null
        ) {

            try {

                val route = osrmRouter.getRoute(
                    startLatitude = latitude!!,
                    startLongitude = longitude!!,
                    destinationLatitude = destinationLatitude!!,
                    destinationLongitude = destinationLongitude!!
                )

                routePoints = route

                println(
                    "OSRM ROUTE POINTS: ${route.size}"
                )

            } catch (e: Exception) {

                println(
                    "OSRM ERROR: ${e.message}"
                )
            }
        }
    }

    //Calculate progress whenever GPS moves
    LaunchedEffect(
        latitude,
        longitude,
        routePoints
    ) {

        if (
            latitude != null &&
            longitude != null &&
            routePoints.size >= 2
        ) {

            val currentLocation = RoutePoint(
                latitude = latitude!!,
                longitude = longitude!!
            )

            routeProgress =
                RouteTracker.calculateProgress(
                    currentLocation = currentLocation,
                    route = routePoints
                )
        }
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
                routePoints = routePoints,
                destinationLatitude = destinationLatitude,
                destinationLongitude = destinationLongitude,

                recenterRequest = recenterRequest,

                onUserMovedMap = {
                    isFollowingLocation = false
                },

                onMapTap = { tappedLatitude, tappedLongitude ->

                    destinationLatitude = tappedLatitude
                    destinationLongitude = tappedLongitude

                    println(
                        "DESTINATION: $tappedLatitude, $tappedLongitude"
                    )
                },

                modifier = Modifier.fillMaxSize()
            )

            // ----------------------------------------------------------
            // RE-CENTER BUTTON
            // ----------------------------------------------------------

            if (!isFollowingLocation) {

                Button(
                    onClick = {

                        recenterRequest++

                        isFollowingLocation = true
                    },

                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(16.dp)
                ) {

                    Text(
                        text = "RE-CENTER"
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)
                .background(Color.Black)
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {

//            if (latitude != null && longitude != null) {
//
//                Column {
//
//                    Text(
//                        text = "LAT: %.6f".format(latitude),
//                        color = Color.White,
//                        fontSize = 13.sp
//                    )
//
//                    Text(
//                        text = "LON: %.6f".format(longitude),
//                        color = Color.White,
//                        fontSize = 13.sp
//                    )
//                }
//
//            } else {
//
//                Text(
//                    text = "GPS: Waiting...",
//                    color = Color.White,
//                    fontSize = 14.sp
//                )
//            }

            //Temporarily display the progress
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

                    routeProgress?.let { progress ->

                        Text(
                            text = "ROUTE: %.1f%%".format(
                                progress.progressPercent
                            ),
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "REMAINING: %.0f m".format(
                                progress.distanceRemainingMeters
                            ),
                            color = Color.White,
                            fontSize = 13.sp
                        )

                        Text(
                            text = "OFF ROUTE: %.0f m".format(
                                progress.distanceFromRouteMeters
                            ),
                            color = Color.White,
                            fontSize = 13.sp
                        )
                    }
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