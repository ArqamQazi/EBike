package com.example.ebike2

import android.Manifest
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import com.example.ebike2.ui.theme.EBike2Theme

class MainActivity : ComponentActivity() {

    private val viewModel: BikeViewModel by viewModels()

    // Permission launcher for BLE
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.entries.all { it.value }
        if (allGranted) {
            // Change this to viewModel.startBleProcess() when you are
            // ready to connect to your real ESP32-S3 hardware!
            viewModel.startSimulation()
        } else {
            println("Permissions not granted by the user.")
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Smart Permission Request based on Android Version
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            // Android 12+ (Only needs Bluetooth, no location required)
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT
            )
        } else {
            // Android 11 and lower (Requires BOTH location types to scan for BLE)
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        }

        // Launch the permission popup
        requestPermissionLauncher.launch(permissions)

        setContent {
            EBike2Theme {
                BikeApp(viewModel = viewModel)
            }
        }
    }
}
