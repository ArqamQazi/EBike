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
            viewModel.startBleProcess()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Check/Request permissions based on Android Version
        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }

        requestPermissionLauncher.launch(permissions)

        setContent {
            EBike2Theme {
                BikeApp(viewModel = viewModel)
            }
        }
    }
}
