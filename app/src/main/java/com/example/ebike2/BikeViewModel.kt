package com.example.ebike2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Filter
import com.juul.kable.Scanner
import com.juul.kable.State
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets

@Serializable
data class EVData(
    val speed: Int = 0,
    val odo: Float = 0f,
    val trip: Float = 0f,
    val throt: Int = 0,
    val bat: Int = 0,
    val volt: Float = 0f,
    val temp: Int = 0,
    val lux: Int = 0,
    val chrg: Boolean = false,
    val t_chrg: Int = 0,
    val flags: Int = 0,
    val lean: Int = 0 // Matching the MPU6050 data from ESP32
)

class BikeViewModel : ViewModel() {
    private val _evData = MutableStateFlow(EVData())
    val evData: StateFlow<EVData> = _evData.asStateFlow()

    private val json = Json { ignoreUnknownKeys = true }

    // Replace these with the UUIDs you used in the ESP32 code
    private val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    private val CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"

    fun startBleProcess() {
        viewModelScope.launch {
            try {
                // 1. Scan for the device
                val scanner = Scanner {
                    filters = listOf(Filter.Service(com.benasher44.uuid.uuidFrom(SERVICE_UUID)))
                }

                // Get the first bike found
                val advertisement = scanner.advertisements.first()
                val peripheral = viewModelScope.peripheral(advertisement)

                // 2. Connect
                peripheral.connect()

                // 3. Observe notifications
                val characteristic = characteristicOf(
                    service = SERVICE_UUID,
                    characteristic = CHARACTERISTIC_UUID
                )

                peripheral.observe(characteristic).collect { data ->
                    val payload = String(data, StandardCharsets.UTF_8)
                    try {
                        _evData.value = json.decodeFromString<EVData>(payload)
                    } catch (e: Exception) {
                        println("BLE JSON Error: ${e.message}")
                    }
                }
            } catch (e: Exception) {
                println("BLE Connection Error: ${e.message}")
            }
        }
    }
}
