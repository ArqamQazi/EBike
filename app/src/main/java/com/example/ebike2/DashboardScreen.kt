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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.ExperimentalTextApi
import android.content.res.Configuration
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.TextStyle
import kotlin.math.cos
import kotlin.math.sin
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.getValue

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
        val isLowBat = (evData.flags and 1) != 0
        val isLightOn = (evData.flags and 2) != 0
        val isLeftInd = (evData.flags and 4) != 0
        val isRightInd = (evData.flags and 8) != 0

        // 3. Process dynamic values
        val formattedTime = "${evData.t_chrg / 60}h ${evData.t_chrg % 60}m"
        val activeWarnings = mutableListOf<String>()
        if (isLowBat) activeWarnings.add("Low Power Mode Active")
        if (evData.temp > 45) activeWarnings.add("Battery Temp High!")

        // 🟢 FIX: Calculate the estimated range on the Android side
        // Assuming a max range of 60km. Formula: (Battery% / 100) * MaxRange
        val estimatedRange = ((evData.bat / 100f) * 60f).toInt()

        // 4. Determine Headlight Mode based on Light flag + Lux (auto-highbeam logic)
        val currentHeadlightMode = when {
            !isLightOn -> HeadlightMode.OFF
            evData.lux < 30 -> HeadlightMode.HIGH_BEAM
            else -> HeadlightMode.LOW_BEAM
        }

        // 5. Pass data to Dashboard
        DashboardScreen(
            speed = evData.speed,
            batteryPercent = evData.bat,
            throttleLevel = evData.throt / 100f,
            tripMeter = evData.trip,
            odoMeter = evData.odo,
            rangeRemaining = estimatedRange,
            isAutoBrightness = true,
            isCharging = evData.chrg,
            timeToCharge = formattedTime,
            warnings = activeWarnings,
            isAntiTheftActive = true,
            headlightMode = currentHeadlightMode,
            isLeftIndicatorOn = isLeftInd,
            isRightIndicatorOn = isRightInd,
            temp = evData.temp
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
    temp: Int
) {
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .systemBarsPadding()
            .padding(24.dp)
    ) {
        if (isLandscape) {
            // --- LANDSCAPE LAYOUT ---
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Left Side: Gauge
                Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.Center) {
                    CenterGauge(speed, throttleLevel, batteryPercent, isCharging, timeToCharge)
                }

                Spacer(modifier = Modifier.width(32.dp))

                // Right Side: Info & Warnings
                Column(
                    modifier = Modifier.weight(1f).fillMaxHeight(),
                    verticalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        TopStatusBar(isAutoBrightness, headlightMode, isLeftIndicatorOn, isRightIndicatorOn, temp)
                        if (!isCharging && batteryPercent < 20) WarningBanner(warnings)
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        SecurityAndGpsRow(isAntiTheftActive)
                        BottomInfoRow(tripMeter, odoMeter, rangeRemaining)
                    }
                }
            }
        } else {
            // --- PORTRAIT LAYOUT (Original) ---
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.fillMaxWidth()) {
                    TopStatusBar(isAutoBrightness, headlightMode, isLeftIndicatorOn, isRightIndicatorOn, temp)
                    if (!isCharging && batteryPercent < 20) WarningBanner(warnings)
                }

                CenterGauge(speed, throttleLevel, batteryPercent, isCharging, timeToCharge)

                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SecurityAndGpsRow(isAntiTheftActive)
                    BottomInfoRow(tripMeter, odoMeter, rangeRemaining)
                }
            }
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
@OptIn(ExperimentalTextApi::class)
@Composable
fun CenterGauge(speed: Int, throttleLevel: Float, battery: Int, isCharging: Boolean, timeToCharge: String) {
    val onBackgroundColor = MaterialTheme.colorScheme.onBackground
    val primaryColor = MaterialTheme.colorScheme.primary
    val secondaryColor = MaterialTheme.colorScheme.secondary
    val neonCyan = Color(0xFF00E5FF)
    val darkGray = Color.DarkGray

    val textMeasurer = rememberTextMeasurer()

    // --- ANIMATIONS ---
    // This makes the needle sweep smoothly over 300 milliseconds
    val animatedSpeed by animateFloatAsState(
        targetValue = speed.toFloat(),
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "speed_anim"
    )

    // This makes the throttle ring fill up smoothly
    val animatedThrottle by animateFloatAsState(
        targetValue = throttleLevel,
        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "throttle_anim"
    )

    // Gauge Configuration
    val maxSpeed = 160f
    val startAngle = 140f
    val sweepAngle = 260f

    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.aspectRatio(1f).fillMaxHeight(0.9f).padding(16.dp)
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val center = Offset(size.width / 2, size.height / 2)
            val radius = size.minDimension / 2

            // 1. Draw Outer Track (Background arc)
            drawArc(
                color = darkGray.copy(alpha = 0.5f),
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 2. Draw Animated Throttle Ring
            drawArc(
                color = secondaryColor,
                startAngle = startAngle,
                sweepAngle = sweepAngle * animatedThrottle.coerceIn(0f, 1f),
                useCenter = false,
                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
            )

            // 3. Draw Ticks and Numbers
            for (i in 0..160 step 10) {
                val isMajor = i % 20 == 0
                val angleDeg = startAngle + (i / maxSpeed) * sweepAngle
                val angleRad = Math.toRadians(angleDeg.toDouble())

                val outerRad = radius - 10.dp.toPx()
                val innerRad = if (isMajor) outerRad - 15.dp.toPx() else outerRad - 8.dp.toPx()

                val startLine = Offset(
                    x = center.x + outerRad * cos(angleRad).toFloat(),
                    y = center.y + outerRad * sin(angleRad).toFloat()
                )
                val endLine = Offset(
                    x = center.x + innerRad * cos(angleRad).toFloat(),
                    y = center.y + innerRad * sin(angleRad).toFloat()
                )

                drawLine(
                    color = if (isMajor) Color.White else Color.Gray,
                    start = startLine,
                    end = endLine,
                    strokeWidth = if (isMajor) 3.dp.toPx() else 1.5.dp.toPx()
                )

                if (isMajor) {
                    val text = i.toString()
                    val textLayoutResult = textMeasurer.measure(
                        text = text,
                        style = TextStyle(color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                    )

                    val textRad = innerRad - 20.dp.toPx()
                    val textCenter = Offset(
                        x = center.x + textRad * cos(angleRad).toFloat() - (textLayoutResult.size.width / 2),
                        y = center.y + textRad * sin(angleRad).toFloat() - (textLayoutResult.size.height / 2)
                    )

                    drawText(
                        textLayoutResult = textLayoutResult,
                        topLeft = textCenter
                    )
                }
            }

            // 4. Draw Animated Needle
            // Use animatedSpeed here instead of the raw speed
            val clampedSpeed = animatedSpeed.coerceIn(0f, maxSpeed)
            val needleAngleDeg = startAngle + (clampedSpeed / maxSpeed) * sweepAngle
            val needleAngleRad = Math.toRadians(needleAngleDeg.toDouble())

            val needleLength = radius - 35.dp.toPx()
            val needleEnd = Offset(
                x = center.x + needleLength * cos(needleAngleRad).toFloat(),
                y = center.y + needleLength * sin(needleAngleRad).toFloat()
            )

            // Needle Line
            drawLine(
                color = neonCyan,
                start = center,
                end = needleEnd,
                strokeWidth = 6.dp.toPx(),
                cap = StrokeCap.Round
            )

            // Center Dot
            drawCircle(
                color = neonCyan,
                radius = 12.dp.toPx(),
                center = center
            )
            drawCircle(
                color = Color.Black,
                radius = 6.dp.toPx(),
                center = center
            )
        }

        // Inner Digital Text (Bottom Center)
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 24.dp)
        ) {
            Text(text = speed.toString(), fontSize = 48.sp, fontWeight = FontWeight.Bold, color = onBackgroundColor)
            Text(text = "km/h", fontSize = 14.sp, color = Color.Gray)
            Spacer(modifier = Modifier.height(8.dp))

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
        // Use .toInt() to remove all decimal points!
        MetricItem(label = "TRIP", value = "${trip.toInt()}km")
        MetricItem(label = "RANGE", value = "${range}km", valueColor = MaterialTheme.colorScheme.primary)
        MetricItem(label = "ODO", value = "${odo.toInt()}km")
    }
}

@Composable
fun MetricItem(label: String, value: String, valueColor: Color = MaterialTheme.colorScheme.onBackground) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(text = label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f))
        Text(text = value, fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = valueColor)
    }
}
