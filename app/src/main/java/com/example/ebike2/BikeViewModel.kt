package com.example.ebike2

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.juul.kable.Filter
import com.juul.kable.Scanner
import com.juul.kable.characteristicOf
import com.juul.kable.peripheral
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import kotlin.math.sin

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

    // BLE UUIDs
    private val SERVICE_UUID = "4fafc201-1fb5-459e-8fcc-c5c9c331914b"
    private val CHARACTERISTIC_UUID = "beb5483e-36e1-4688-b7f5-ea07361b26a8"

    // MOCK SIMULATION MODE
    fun startSimulation() {
        viewModelScope.launch {
            var simSpeed = 0
            var simBat = 100
            var simOdo = 1450.0f
            var simTrip = 12.4f
            var isAccelerating = true
            var tick = 0

            while (true) {
                // 1. Simulate Speed & Throttle (Accelerate to 140, then brake)
                if (isAccelerating) {
                    simSpeed += 2
                    if (simSpeed >= 140) isAccelerating = false
                } else {
                    simSpeed -= 3
                    if (simSpeed <= 0) {
                        simSpeed = 0
                        isAccelerating = true
                        simBat -= 5 // Drain battery a bit every time we stop
                        if (simBat < 0) simBat = 100 // Reset battery
                    }
                }
                val simThrot = (simSpeed / 140f) * 100f

                // 2. Simulate Distance (km traveled per 100ms)
                val distanceStep = (simSpeed / 3600f) * 0.1f
                simOdo += distanceStep
                simTrip += distanceStep

                // 3. Simulate Bitwise Flags (Blinking indicators, toggling lights)
                var simFlags = 0
                if (simBat < 20) simFlags = simFlags or 1 // Turn on Low Bat flag
                if (tick % 40 < 20) simFlags = simFlags or 2 // Toggle Headlight every few seconds
                if (tick % 10 < 5) simFlags = simFlags or 4 // Blink Left Indicator fast

                // 4. Simulate Environment (Lean angle swaying, Lux changing)
                val simLean = (sin(tick / 10.0) * 30).toInt() // Sway between -30 and +30 degrees
                val simLux = 50 + (sin(tick / 20.0) * 40).toInt() // Fade light dark to bright
                val simTemp = 24 + (simSpeed / 20) // Temp goes up slightly as speed increases

                // 5. Push data to the UI
                _evData.value = EVData(
                    speed = simSpeed,
                    odo = simOdo,
                    trip = simTrip,
                    throt = simThrot.toInt(),
                    bat = simBat,
                    volt = 41f + (simBat / 100f) * 13f, // Calculate fake voltage (41v - 54v)
                    temp = simTemp,
                    lux = simLux,
                    chrg = false,
                    t_chrg = 0,
                    flags = simFlags,
                    lean = simLean
                )

                tick++
                delay(100) // Update UI 10 times per second
            }
        }
    }

    /**
     * REAL HARDWARE MODE
     * Call this when you are ready to connect to the physical ESP32-S3.
     */
    fun startBleProcess() {
        viewModelScope.launch {
            try {
                val scanner = Scanner {
                    filters = listOf(Filter.Service(com.benasher44.uuid.uuidFrom(SERVICE_UUID)))
                }
                val advertisement = scanner.advertisements.first()
                val peripheral = viewModelScope.peripheral(advertisement)

                peripheral.connect()

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
