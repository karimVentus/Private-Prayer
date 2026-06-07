package com.prayertime.ui.screens

import android.hardware.SensorManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.prayertime.R
import com.prayertime.di.CompassEntryPoint
import com.prayertime.domain.calculator.QiblaCalculator
import com.prayertime.ui.theme.AppSpacing
import dagger.hilt.android.EntryPointAccessors
import kotlin.math.cos
import kotlin.math.sin

/**
 * Full-screen Qibla compass that reads the device heading and shows the
 * Qibla direction via a rotating red arrow and cardinal compass dial.
 *
 * @param latitude  City latitude in degrees (-90..90)
 * @param longitude City longitude in degrees (-180..180)
 * @param cityLabel Display name of the current city
 * @param onClose   Called when the user taps the close/back button
 */
@Composable
fun QiblaScreen(
    latitude: Double,
    longitude: Double,
    cityLabel: String,
    onClose: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val qiblaBearing = remember(latitude, longitude) { QiblaCalculator.bearing(latitude, longitude) }
    val cardinalDir = remember(qiblaBearing) { QiblaCalculator.cardinalDirection(qiblaBearing) }

    val context = LocalContext.current
    val compassSensor =
        remember(context) {
            runCatching {
                EntryPointAccessors
                    .fromApplication(context.applicationContext, CompassEntryPoint::class.java)
                    .compassSensor()
            }.getOrNull()
        }
    val compassAvailable = compassSensor?.isAvailable ?: false
    val palette = calendarPalette()

    var azimuth by remember { mutableStateOf<Float?>(null) }
    var accuracy by remember { mutableStateOf(SensorManager.SENSOR_STATUS_ACCURACY_HIGH) }
    var isCalibrating by remember { mutableStateOf(false) }
    var prevAccuracy by remember { mutableStateOf(-1) }
    LaunchedEffect(compassSensor) {
        compassSensor?.azimuth?.collect { azimuth = it }
    }
    LaunchedEffect(compassSensor) {
        compassSensor?.accuracy?.collect { accuracy = it }
    }
    // When calibrating and accuracy RISES to HIGH → vibrate + auto-close
    LaunchedEffect(isCalibrating, accuracy, prevAccuracy) {
        if (isCalibrating && accuracy >= SensorManager.SENSOR_STATUS_ACCURACY_HIGH && prevAccuracy < SensorManager.SENSOR_STATUS_ACCURACY_HIGH) {
            (context.getSystemService(android.content.Context.VIBRATOR_SERVICE) as? android.os.Vibrator)?.vibrate(
                android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE)
            )
            isCalibrating = false
        }
        if (isCalibrating) prevAccuracy = accuracy
    }
    val smoothAzimuth by animateFloatAsState(
        targetValue = azimuth ?: 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "compassAzimuth",
    )

    Column(
        modifier = modifier.fillMaxSize().background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        QiblaTitleRow(palette)
        Spacer(modifier = Modifier.height(AppSpacing.screenVertical))
        QiblaSensorStatus(compassAvailable, azimuth, palette)
        QiblaCompassBox(
            smoothAzimuth = smoothAzimuth,
            qiblaBearing = qiblaBearing,
            palette = palette,
            modifier = Modifier.weight(1f),
        )
        QiblaInfoLabels(qiblaBearing, cardinalDir, cityLabel, palette, context.resources)
        if (isCalibrating) {
            QiblaCalibrationActive(palette, context.resources)
        } else {
            QiblaCalibrationStatus(
                accuracy = accuracy,
                compassAvailable = compassAvailable,
                palette = palette,
                resources = context.resources,
                onCalibrate = { isCalibrating = true },
            )
        }
        QiblaCloseButton(onClose, palette, context.resources)
    }
}

// ── Sub-composables ───────────────────────────────────────────────────────────

@Composable
private fun QiblaTitleRow(palette: com.prayertime.ui.theme.CalendarPalette) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenHorizontal),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Spacer(modifier = Modifier.weight(1f))
        Text(
            text = stringResource(R.string.qibla_compass_title),
            color = palette.textPrimary,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
private fun QiblaSensorStatus(
    compassAvailable: Boolean,
    azimuth: Float?,
    palette: com.prayertime.ui.theme.CalendarPalette,
) {
    val context = LocalContext.current
    when {
        !compassAvailable ->
            Text(
                text = context.getString(R.string.compass_not_available),
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal),
            )
        azimuth == null ->
            Text(
                text = context.getString(R.string.compass_calibrating),
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal),
            )
        else -> Unit
    }
}

@Composable
private fun QiblaCompassBox(
    smoothAzimuth: Float,
    qiblaBearing: Float,
    palette: com.prayertime.ui.theme.CalendarPalette,
    modifier: Modifier = Modifier,
) {
    BoxWithConstraints(
        modifier =
            modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenHorizontal),
        contentAlignment = Alignment.Center,
    ) {
        val compassSize = minOf(maxWidth, maxHeight) * 0.85f
        val qiblaAngle = qiblaBearing - smoothAzimuth
        Canvas(modifier = Modifier.size(compassSize).aspectRatio(1f)) {
            drawCompassContent(smoothAzimuth, qiblaAngle, palette)
        }
    }
}

/** All canvas drawing for the compass dial + arrow. */
private fun androidx.compose.ui.graphics.drawscope.DrawScope.drawCompassContent(
    smoothAzimuth: Float,
    qiblaAngle: Float,
    palette: com.prayertime.ui.theme.CalendarPalette,
) {
    val cx = size.width / 2f
    val cy = size.height / 2f
    val radius = minOf(cx, cy) * 0.90f

    val ringColor = palette.textPrimary.copy(alpha = 0.35f)
    val markColor = palette.textPrimary.copy(alpha = 0.55f)
    val minorMarkColor = palette.textPrimary.copy(alpha = 0.20f)
    val labelPaint =
        android.graphics.Paint().apply {
            color = palette.textPrimary.hashCode()
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = 14.sp.toPx()
            isAntiAlias = true
            isFakeBoldText = true
        }

    // 1. Outer ring
    drawCircle(color = ringColor, radius = radius, center = Offset(cx, cy), style = Stroke(width = 2.dp.toPx()))
    drawCircle(color = ringColor.copy(alpha = 0.15f), radius = radius * 0.97f, center = Offset(cx, cy), style = Stroke(width = 1.dp.toPx()))

    // 2. Rotating dial
    rotate(degrees = -smoothAzimuth, pivot = Offset(cx, cy)) {
        val cardinals = listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
        for ((angle, label) in cardinals) {
            val rad = Math.toRadians(angle.toDouble())
            val sinR = sin(rad).toFloat()
            val cosR = cos(rad).toFloat()
            drawLine(
                markColor,
                Offset(cx + radius * 0.82f * sinR, cy - radius * 0.82f * cosR),
                Offset(cx + radius * 0.92f * sinR, cy - radius * 0.92f * cosR),
                2.5.dp.toPx(),
            )
            val lr = radius * 0.72f
            drawContext.canvas.nativeCanvas.drawText(label, cx + lr * sinR, cy - lr * cosR + 5.sp.toPx() / 3f, labelPaint)
        }
        for (angle in 0 until 360 step 30) {
            if (angle % 90 == 0) continue
            val rad = Math.toRadians(angle.toDouble())
            val sinR = sin(rad).toFloat()
            val cosR = cos(rad).toFloat()
            drawLine(
                minorMarkColor,
                Offset(cx + radius * 0.86f * sinR, cy - radius * 0.86f * cosR),
                Offset(cx + radius * 0.90f * sinR, cy - radius * 0.90f * cosR),
                1.dp.toPx(),
            )
        }
    }

    // 3. Qibla arrow
    val rad = Math.toRadians(qiblaAngle.toDouble())
    val aSin = sin(rad).toFloat()
    val aCos = cos(rad).toFloat()
    val arrowLength = radius * 0.72f
    val baseLength = radius * 0.08f
    val baseWidth = 8.dp.toPx()
    val baseCx = cx + baseLength * aSin
    val baseCy = cy - baseLength * aCos
    val perpSin = cos(rad).toFloat()
    val perpCos = (-sin(rad)).toFloat()
    drawPath(
        Path().apply {
            moveTo(cx + arrowLength * aSin, cy - arrowLength * aCos)
            lineTo(baseCx + baseWidth * perpSin, baseCy + baseWidth * perpCos)
            lineTo(baseCx - baseWidth * perpSin, baseCy - baseWidth * perpCos)
            close()
        },
        color = Color(0xFFD32F2F),
    )
    drawCircle(Color(0xFFFFCDD2), radius = baseWidth * 0.4f, center = Offset(baseCx, baseCy))

    // Kaaba icon at arrow tip
    val kaabaSize = 10.dp.toPx()
    val kx = cx + arrowLength * aSin
    val ky = cy - arrowLength * aCos
    // Kaaba body
    drawRoundRect(
        color = Color(0xFF2C2C2C),
        topLeft = Offset(kx - kaabaSize / 2f, ky - kaabaSize * 1.3f),
        size = androidx.compose.ui.geometry.Size(kaabaSize, kaabaSize * 1.3f),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.dp.toPx(), 2.dp.toPx()),
    )
    // Gold door line
    drawLine(
        color = Color(0xFFD4A847),
        start = Offset(kx - kaabaSize * 0.2f, ky - kaabaSize * 0.6f),
        end = Offset(kx + kaabaSize * 0.2f, ky - kaabaSize * 0.6f),
        strokeWidth = 2.dp.toPx(),
    )
    // Roof line
    drawRect(
        color = Color(0xFF1A1A1A),
        topLeft = Offset(kx - kaabaSize * 0.6f, ky - kaabaSize * 1.45f),
        size = androidx.compose.ui.geometry.Size(kaabaSize * 1.2f, kaabaSize * 0.15f),
    )

    // 4. Center dot
    drawCircle(palette.textPrimary.copy(alpha = 0.5f), radius = 2.5.dp.toPx(), center = Offset(cx, cy))
}

@Composable
private fun QiblaInfoLabels(
    qiblaBearing: Float,
    cardinalDir: String,
    cityLabel: String,
    palette: com.prayertime.ui.theme.CalendarPalette,
    resources: android.content.res.Resources,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Text(
            text = resources.getString(R.string.qibla_bearing_format, qiblaBearing.toInt(), cardinalDir),
            color = palette.textPrimary,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(text = cityLabel, color = palette.textSecondary, fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun QiblaCloseButton(
    onClose: () -> Unit,
    palette: com.prayertime.ui.theme.CalendarPalette,
    resources: android.content.res.Resources,
) {
    Row(
        modifier = Modifier.fillMaxWidth().navigationBarsPadding(),
        horizontalArrangement = Arrangement.End,
    ) {
        Text(
            text = resources.getString(R.string.back_to_prayer_times),
            color = palette.textSecondary,
            fontSize = 11.sp,
            modifier =
                Modifier
                    .defaultMinSize(minHeight = AppSpacing.touchTargetMin)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
        )
    }
}

@Composable
private fun QiblaCalibrationStatus(
    accuracy: Int,
    compassAvailable: Boolean,
    palette: com.prayertime.ui.theme.CalendarPalette,
    resources: android.content.res.Resources,
    onCalibrate: () -> Unit,
) {
    if (!compassAvailable) return
    val (accuracyLabel, isLowAccuracy) = when (accuracy) {
        SensorManager.SENSOR_STATUS_UNRELIABLE,
        SensorManager.SENSOR_STATUS_ACCURACY_LOW ->
            resources.getString(R.string.compass_low_accuracy) to true
        SensorManager.SENSOR_STATUS_ACCURACY_MEDIUM ->
            resources.getString(R.string.compass_accuracy_medium) to false
        else -> resources.getString(R.string.compass_accuracy_high) to false
    }
    val buttonLabel = if (isLowAccuracy) {
        resources.getString(R.string.compass_calibrate_action)
    } else {
        resources.getString(R.string.compass_recalibrate_action)
    }
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        if (isLowAccuracy) {
            Text(
                text = accuracyLabel,
                color = Color(0xFFE65100),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(4.dp))
        }
        Text(
            text = buttonLabel,
            color = Color(0xFF1565C0),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
            modifier = Modifier.clickable(onClick = onCalibrate).padding(8.dp),
        )
    }
}

@Composable
private fun QiblaCalibrationActive(
    palette: com.prayertime.ui.theme.CalendarPalette,
    resources: android.content.res.Resources,
) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(horizontal = AppSpacing.screenHorizontal),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = resources.getString(R.string.compass_calibrating),
            color = Color(0xFFE65100),
            fontSize = 13.sp,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = resources.getString(R.string.compass_calibrate_watch),
            color = palette.textSecondary,
            fontSize = 11.sp,
            textAlign = TextAlign.Center,
        )
    }
}
