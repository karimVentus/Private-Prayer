package com.prayertime.ui.screens

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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
    val qiblaBearing =
        remember(latitude, longitude) {
            QiblaCalculator.bearing(latitude, longitude)
        }
    val cardinalDir =
        remember(qiblaBearing) {
            QiblaCalculator.cardinalDirection(qiblaBearing)
        }

    val context = LocalContext.current
    val compassSensor =
        remember(context) {
            val entryPoint =
                EntryPointAccessors.fromApplication(
                    context.applicationContext,
                    CompassEntryPoint::class.java,
                )
            entryPoint.compassSensor()
        }
    val azimuth by compassSensor.azimuth.collectAsState(initial = null)

    val smoothAzimuth by animateFloatAsState(
        targetValue = azimuth ?: 0f,
        animationSpec = tween(durationMillis = 600, easing = LinearEasing),
        label = "compassAzimuth",
    )

    val compassAvailable =
        remember(compassSensor) { compassSensor.isAvailable }
    val palette = calendarPalette()
    val resources = context.resources

    Column(
        modifier = modifier.fillMaxSize().background(palette.background),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        // --- Title row ---
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

        Spacer(modifier = Modifier.height(AppSpacing.screenVertical))

        if (!compassAvailable) {
            // --- Sensor-unavailable message ---
            Text(
                text = resources.getString(R.string.compass_not_available),
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal),
            )
        } else if (azimuth == null) {
            // --- Waiting for first sensor reading ---
            Text(
                text = resources.getString(R.string.compass_calibrating),
                color = palette.textSecondary,
                fontSize = 14.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = AppSpacing.screenHorizontal),
            )
        }

        // --- Compass drawing ---
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenHorizontal)
                .weight(1f),
            contentAlignment = Alignment.Center,
        ) {
            val compassSize = minOf(maxWidth, maxHeight) * 0.85f
            val qiblaAngle = qiblaBearing - smoothAzimuth

            Canvas(
                modifier = Modifier.size(compassSize).aspectRatio(1f),
            ) {
                val cx = size.width / 2f
                val cy = size.height / 2f
                val radius = minOf(cx, cy) * 0.90f
                val outerRingWidth = 2.dp.toPx()
                val innerRingWidth = 1.dp.toPx()

                val ringColor = palette.textPrimary.copy(alpha = 0.35f)
                val markColor = palette.textPrimary.copy(alpha = 0.55f)
                val minorMarkColor = palette.textPrimary.copy(alpha = 0.20f)
                val textColorArgb = palette.textPrimary.hashCode()
                val labelPaint =
                    android.graphics.Paint().apply {
                        color = textColorArgb
                        textAlign = android.graphics.Paint.Align.CENTER
                        textSize = 14.sp.toPx()
                        isAntiAlias = true
                        isFakeBoldText = true
                    }
                // --- 1. Outer ring (fixed) ---
                drawCircle(
                    color = ringColor,
                    radius = radius,
                    center = Offset(cx, cy),
                    style = Stroke(width = outerRingWidth),
                )
                drawCircle(
                    color = ringColor.copy(alpha = 0.15f),
                    radius = radius * 0.97f,
                    center = Offset(cx, cy),
                    style = Stroke(width = innerRingWidth),
                )

                // --- 2. Rotating dial: N/S/E/W marks + minor ticks ---
                rotate(degrees = -smoothAzimuth, pivot = Offset(cx, cy)) {
                    // Cardinal marks (0, 90, 180, 270)
                    val cardinals = listOf(0f to "N", 90f to "E", 180f to "S", 270f to "W")
                    for ((angle, label) in cardinals) {
                        val rad = Math.toRadians(angle.toDouble())
                        val sinR = sin(rad).toFloat()
                        val cosR = cos(rad).toFloat()

                        // Tick mark
                        val tickInner = radius * 0.82f
                        val tickOuter = radius * 0.92f
                        drawLine(
                            color = markColor,
                            start = Offset(cx + tickInner * sinR, cy - tickInner * cosR),
                            end = Offset(cx + tickOuter * sinR, cy - tickOuter * cosR),
                            strokeWidth = 2.5.dp.toPx(),
                        )

                        // Label
                        val labelRadius = radius * 0.72f
                        val lx = cx + labelRadius * sinR
                        val ly = cy - labelRadius * cosR
                        drawContext.canvas.nativeCanvas.drawText(label, lx, ly + 5.sp.toPx() / 3f, labelPaint)
                    }

                    // Minor ticks every 30 degrees
                    for (angle in 0 until 360 step 30) {
                        if (angle % 90 == 0) continue
                        val rad = Math.toRadians(angle.toDouble())
                        val sinR = sin(rad).toFloat()
                        val cosR = cos(rad).toFloat()
                        val tickInner = radius * 0.86f
                        val tickOuter = radius * 0.90f
                        drawLine(
                            color = minorMarkColor,
                            start = Offset(cx + tickInner * sinR, cy - tickInner * cosR),
                            end = Offset(cx + tickOuter * sinR, cy - tickOuter * cosR),
                            strokeWidth = 1.dp.toPx(),
                        )
                    }
                }

                // --- 3. Qibla arrow (rotated by bearing - azimuth) ---
                // This arrow stays fixed on screen but its rotation adjusts so it
                // always points toward Qibla relative to the user's facing direction.
                val arrowAngleRad = Math.toRadians(qiblaAngle.toDouble())
                val aSin = sin(arrowAngleRad).toFloat()
                val aCos = cos(arrowAngleRad).toFloat()

                // Arrow tip at the edge of the compass ring
                val arrowLength = radius * 0.72f
                val arrowTipX = cx + arrowLength * aSin
                val arrowTipY = cy - arrowLength * aCos

                // Arrow base (wider) near center
                val baseWidth = 8.dp.toPx()
                val baseLength = radius * 0.08f
                val baseCx = cx + baseLength * aSin
                val baseCy = cy - baseLength * aCos

                // Perpendicular direction for base width
                val perpSin = cos(arrowAngleRad).toFloat()
                val perpCos = (-sin(arrowAngleRad)).toFloat()

                val arrowPath =
                    Path().apply {
                        moveTo(arrowTipX, arrowTipY)
                        lineTo(baseCx + baseWidth * perpSin, baseCy + baseWidth * perpCos)
                        lineTo(baseCx - baseWidth * perpSin, baseCy - baseWidth * perpCos)
                        close()
                    }
                drawPath(
                    path = arrowPath,
                    color = Color(0xFFD32F2F),
                )

                // Small accent circle at the arrow's base
                drawCircle(
                    color = Color(0xFFFFCDD2),
                    radius = baseWidth * 0.4f,
                    center = Offset(baseCx, baseCy),
                )

                // --- 4. Compass center dot ---
                drawCircle(
                    color = palette.textPrimary.copy(alpha = 0.5f),
                    radius = 2.5.dp.toPx(),
                    center = Offset(cx, cy),
                )
            }
        }

        // --- Info labels ---
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = AppSpacing.screenHorizontal),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Bearing text
            Text(
                text = resources.getString(
                    R.string.qibla_bearing_format,
                    qiblaBearing.toInt(),
                    cardinalDir,
                ),
                color = palette.textPrimary,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                textAlign = TextAlign.Center,
            )

            Spacer(modifier = Modifier.height(4.dp))

            // City name
            Text(
                text = cityLabel,
                color = palette.textSecondary,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
            )
        }

        // --- Close button ---
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .navigationBarsPadding(),
            horizontalArrangement = Arrangement.End,
        ) {
            Text(
                text = resources.getString(R.string.back_to_prayer_times),
                color = palette.textSecondary,
                fontSize = 11.sp,
                modifier = Modifier
                    .defaultMinSize(minHeight = AppSpacing.touchTargetMin)
                    .clickable(onClick = onClose)
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }
    }
}
