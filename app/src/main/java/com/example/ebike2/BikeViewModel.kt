package com.example.ebike2

import androidx.lifecycle.ViewModel
import com.hivemq.client.mqtt.MqttClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.util.UUID

@Serializable
data class EVData(
    val speed: Int = 10,
    val range: Int = 0,
    val odo: Float = 0f,
    val trip: Float = 0f,
    val bat: Int = 0,
    val volt: Float = 0f,
    val throt: Int = 0,
    val temp: Int = 0,
    val lux: Int = 0,
    val chrg: Boolean = false,
    val t_chrg: Int = 0,
    val flags: Int = 0
)

class BikeViewModel : ViewModel() {
    private val _evData = MutableStateFlow(EVData())
    val evData: StateFlow<EVData> = _evData.asStateFlow()

    private var mqttClient: Mqtt3AsyncClient? = null

    // Lenient JSON parser to avoid crashes if ESP32 sends extra fields
    private val json = Json { ignoreUnknownKeys = true }

    init {
        connectToMqtt()
    }

    private fun connectToMqtt() {
        mqttClient = MqttClient.builder()
            .useMqttVersion3()
            .identifier(UUID.randomUUID().toString())
            .serverHost("broker.emqx.io") // Must match ESP32
            .serverPort(1883)
            .buildAsync()

        mqttClient?.connectWith()?.send()?.whenComplete { _, throwable ->
            if (throwable == null) {
                subscribeToTelemetry()
            } else {
                println("MQTT Connection failed: ${throwable.message}")
            }
        }
    }

    private fun subscribeToTelemetry() {
        mqttClient?.subscribeWith()
            ?.topicFilter("my_ebike_project_arqam/telemetry")
            ?.callback { publish ->
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                try {
                    val parsedData = json.decodeFromString<EVData>(payload)
                    _evData.value = parsedData
                } catch (e: Exception) {
                    println("JSON Parse Error: ${e.message}")
                }
            }
            ?.send()
    }

    override fun onCleared() {
        super.onCleared()
        mqttClient?.disconnect()
    }
}
