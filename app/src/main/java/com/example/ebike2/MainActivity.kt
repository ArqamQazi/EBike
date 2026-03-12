package com.example.ebike2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ebike2.ui.theme.EBike2Theme

import androidx.activity.viewModels

class MainActivity : ComponentActivity() {

    // Instantiate ViewModel tied to Activity lifecycle
    private val viewModel: BikeViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        enableEdgeToEdge();
        setContent {
            EBike2Theme { // Assuming your theme is here
                BikeApp(viewModel = viewModel)
            }
        }
    }
}
