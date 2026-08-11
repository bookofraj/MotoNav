package com.rituraj.motonav

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.rituraj.motonav.ui.screens.NavigationScreen
import com.rituraj.motonav.ui.theme.MotoNavTheme
import org.maplibre.android.MapLibre

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Initialize MapLibre before any MapView is created.
        MapLibre.getInstance(this)

        enableEdgeToEdge()

        setContent {
            MotoNavTheme {
                NavigationScreen()
            }
        }
    }
}