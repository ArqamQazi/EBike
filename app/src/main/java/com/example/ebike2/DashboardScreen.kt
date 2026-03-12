package com.example.ebike2


import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.runtime.*

enum class HeadlightMode {
    OFF, LOW_BEAM, HIGH_BEAM
}

@Composable
fun BikeApp(viewModel: BikeViewModel) {
    var isUnlocked by remember { mutableStateOf(false) }

    // 1. Observe the live data stream
    val evData by viewModel.evData.collectAsState()

    if (!isUnlocked) {
        PasskeyScreen(onUnlock = { isUnlocked = true })
    } else {
        // 2. Decode the Bitwise Flags
        // 1:LowBat, 2:Light, 4:L_Ind, 8:R_Ind
        val isLowBat = (evData.flags and 1) != 0
        val isLightOn = (evData.flags and 2) != 0
        val isLeftInd = (evData.flags and 4) != 0
        val isRightInd = (evData.flags and 8) != 0

        // 3. Process dynamic values
        val formattedTime = "${evData.t_chrg / 60}h ${evData.t_chrg % 60}m"
        val activeWarnings = mutableListOf<String>()
        if (isLowBat) activeWarnings.add("Low Power Mode Active")
        if (evData.temp > 45) activeWarnings.add("Battery Temp High!")

        // 4. Determine Headlight Mode based on Light flag + Lux (auto-highbeam logic)
        val currentHeadlightMode = when {
            !isLightOn -> HeadlightMode.OFF
            evData.lux < 30 -> HeadlightMode.HIGH_BEAM // Dark = High Beam
            else -> HeadlightMode.LOW_BEAM
        }

        // 5. Pass data to Dashboard
        DashboardScreen(
            speed = evData.speed,
            batteryPercent = evData.bat,
            throttleLevel = evData.throt / 100f, // Convert 0-100 to 0.0-1.0
            tripMeter = evData.trip,
            odoMeter = evData.odo,
            rangeRemaining = evData.range,
            isAutoBrightness = true, // You could drive this from settings later
            isCharging = evData.chrg,
            timeToCharge = formattedTime,
            warnings = activeWarnings,
            isAntiTheftActive = true, // Static for now
            headlightMode = currentHeadlightMode,
            isLeftIndicatorOn = isLeftInd,
            isRightIndicatorOn = isRightInd,
            temp = evData.temp // Pass temperature if you want to update your TopStatusBar
        )
    }
}

// --- 2. Passkey Screen ---
@Composable
fun PasskeyScreen(onUnlock: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(Icons.Default.Lock, contentDescription = "Locked", tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(64.dp))
        Spacer(modifier = Modifier.height(16.dp))
        Text(text = "E-BIKE SECURED", color = MaterialTheme.colorScheme.onBackground, fontSize = 24.sp, fontWeight = FontWeight.Bold)
        Spacer(modifier = Modifier.height(32.dp))
        Button(
            onClick = onUnlock,
            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
        ) {
            Text("ENTER PASSKEY", color = MaterialTheme.colorScheme.background)
        }
    }
}

// --- 3. Main Dashboard ---
@Composable
fun DashboardScreen(
    speed: Int,
    batteryPercent: Int,
    throttleLevel: Float,
    tripMeter: Float,
    odoMeter: Float,
    rangeRemaining: Int,
    isAutoBrightness: Boolean,
    isCharging: Boolean,
    timeToCharge: String,
    warnings: List<String>,
    isAntiTheftActive: Boolean,
    headlightMode: HeadlightMode,
    isLeftIndicatorOn: Boolean,
    isRightIndicatorOn: Boolean,
    temp: Int // Added temperature parameter
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            // Passed temp down to the TopStatusBar
            TopStatusBar(isAutoBrightness, headlightMode, isLeftIndicatorOn, isRightIndicatorOn, temp)

            if (!isCharging && batteryPercent < 20) {
                WarningBanner(warnings)
            }
        }

        CenterGauge(speed, throttleLevel, batteryPercent, isCharging, timeToCharge)

        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
            SecurityAndGpsRow(isAntiTheftActive)
            BottomInfoRow(tripMeter, odoMeter, rangeRemaining)
        }
    }
}
// --- 4. Top Status & Warnings ---
@Composable
fun TopStatusBar(
    isAutoBrightness: Boolean,
    headlightMode: HeadlightMode,
    isLeftIndicatorOn: Boolean,
    isRightIndicatorOn: Boolean,
    temp: Int // Added temperature parameter
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Left Indicator: Bright primary color if ON, dimmed surface color if OFF
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Left Indicator",
            tint = if (isLeftIndicatorOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {

            // Headlight Mode Logic
            val headlightColor = when (headlightMode) {
                HeadlightMode.OFF -> MaterialTheme.colorScheme.surface // Dimmed out
                HeadlightMode.LOW_BEAM -> MaterialTheme.colorScheme.primary // Green/Standard
                HeadlightMode.HIGH_BEAM -> MaterialTheme.colorScheme.tertiary // Bright Yellow
            }

            Icon(
                imageVector = Icons.Default.Lightbulb,
                contentDescription = "Headlight",
                tint = headlightColor
            )

            if (isAutoBrightness) {
                Icon(
                    imageVector = Icons.Default.BrightnessAuto,
                    contentDescription = "Auto Brightness",
                    tint = MaterialTheme.colorScheme.onBackground,
                    modifier = Modifier.size(16.dp)
                )
            }

            // Replaced hardcoded string with dynamic temp variable
            Text(text = "${temp}°C", color = MaterialTheme.colorScheme.onBackground)
        }

        // Right Indicator: Bright primary color if ON, dimmed surface color if OFF
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
            contentDescription = "Right Indicator",
            tint = if (isRightIndicatorOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface
        )
    }
}

@Composable
fun WarningBanner(warnings: List<String>) {
    if (warnings.isNotEmpty()) {
        Spacer(modifier = Modifier.height(16.dp))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(MaterialTheme.colorScheme.error.copy(alpha = 0.2f), shape = RoundedCornerShape(8.dp))
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Warning, contentDescription = "Warning", tint = MaterialTheme.colorScheme.error)
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = warnings.first(), color = MaterialTheme.colorScheme.error, fontWeight = FontWeight.Bold)
        }
    }
}

// --- 5. Center Gauge (Now supports Charging state) ---
@Composable
fun CenterGauge(speed: Int, throttleLevel: Float, battery: Int, isCharging: Boolean, timeToCharge: String) {
    val surfaceColor = MaterialTheme.colorScheme.surface
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val primaryColor = MaterialTheme.colorScheme.primary
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(300.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val strokeWidthPx = 16.dp.toPx()
            val arcSize = Size(size.width - strokeWidthPx, size.height - strokeWidthPx)

            drawArc(
                color = surfaceColor,
                startAngle = 140f,
                sweepAngle = 260f,
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )

            drawArc(
                color = secondaryColor,
                startAngle = 140f,
                sweepAngle = 260f * throttleLevel.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
                size = arcSize,
                topLeft = Offset(strokeWidthPx / 2, strokeWidthPx / 2)
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = speed.toString(), fontSize = 84.sp, fontWeight = FontWeight.Bold, color = onBackgroundColor)
            Text(text = "km/h", fontSize = 18.sp, color = secondaryColor)
            Spacer(modifier = Modifier.height(16.dp))

            // Show Battery % or Time to Charge based on state
            if (isCharging) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryChargingFull, contentDescription = "Charging", tint = MaterialTheme.colorScheme.tertiary)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = timeToCharge, color = MaterialTheme.colorScheme.tertiary, fontWeight = FontWeight.Bold)
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.BatteryFull, contentDescription = "Battery", tint = if (battery < 20) MaterialTheme.colorScheme.error else primaryColor)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(text = "$battery%", color = onBackgroundColor)
                }
            }
        }
    }
}

// --- 6. GPS & Anti-Theft Row ---
@Composable
fun SecurityAndGpsRow(isAntiTheftActive: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Anti-Theft Toggle Button
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isAntiTheftActive) Icons.Default.GpsFixed else Icons.Default.GpsNotFixed,
                contentDescription = "Anti-Theft",
                tint = if (isAntiTheftActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.5f)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(text = "ANTI-THEFT", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
        }

        // GPS Navigation Button
        Row(
            modifier = Modifier
                .background(MaterialTheme.colorScheme.surface, RoundedCornerShape(12.dp))
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "MAP", color = MaterialTheme.colorScheme.onBackground, fontSize = 12.sp, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(Icons.Default.Map, contentDescription = "GPS Map", tint = MaterialTheme.colorScheme.secondary)
        }
    }
}

// --- 7. Bottom Trip Info ---
@Composable
fun BottomInfoRow(trip: Float, odo: Float, range: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface, shape = RoundedCornerShape(16.dp))
            .padding(20.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        MetricItem(label = "TRIP", value = "${trip}km")
        MetricItem(label = "RANGE", value = "${range}km", valueColor = MaterialTheme.colorScheme.primary)
        MetricItem(label = "ODO", value = "${odo}km")
    }
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onBackground) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
