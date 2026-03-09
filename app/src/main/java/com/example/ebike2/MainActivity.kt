package com.example.ebike2
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.ebike2.ui.theme.EBike2Theme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // This ensures the UI draws behind the system bars for a modern look
        enableEdgeToEdge()

        setContent {
            EBike2Theme {
                // We are passing hardcoded dummy data here just to test the UI layout.
                // Later, this data will come from your ESP32 via a ViewModel.
                BikeApp()
            }
        }
    }
}
