package dev.domus.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.size
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.roundToInt
import kotlin.math.sin

private const val START_ANGLE_DEG = 135f
private const val SWEEP_ANGLE_DEG = 270f
private val DIAL_SIZE = 260.dp
private val STROKE_WIDTH = 20.dp

/**
 * A circular thermostat-style dial in the Google Home style: a single-color track with a
 * draggable handle at the target temperature, and a small static marker + floating label
 * showing the current ambient temperature. Drag anywhere on the ring to retarget; the
 * 90-degree gap at the bottom is intentional.
 */
@Composable
fun TemperatureDial(
    target: Double,
    current: Double?,
    minValue: Double,
    maxValue: Double,
    step: Double,
    unit: String,
    enabled: Boolean = true,
    onTargetChange: (Double) -> Unit,
    modifier: Modifier = Modifier,
) {
    var dragTarget by remember(target) { mutableFloatStateOf(target.toFloat()) }
    val trackColor = MaterialTheme.colorScheme.surfaceVariant
    val handleColor = MaterialTheme.colorScheme.primary
    val currentMarkerColor = MaterialTheme.colorScheme.onSurfaceVariant

    fun fractionFor(value: Double) = ((value - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0)
    fun angleForFraction(fraction: Double) = START_ANGLE_DEG + SWEEP_ANGLE_DEG * fraction

    fun angleToValue(angleDeg: Float): Float {
        var relative = (angleDeg - START_ANGLE_DEG) % 360f
        if (relative < 0f) relative += 360f
        val clamped = relative.coerceIn(0f, SWEEP_ANGLE_DEG)
        val fraction = clamped / SWEEP_ANGLE_DEG
        val raw = minValue + fraction * (maxValue - minValue)
        val stepped = (raw / step).roundToInt() * step
        return stepped.toFloat().coerceIn(minValue.toFloat(), maxValue.toFloat())
    }

    val ringRadiusDp = (DIAL_SIZE - STROKE_WIDTH) / 2
    val labelRadiusDp = ringRadiusDp + 28.dp
    val centerDp = DIAL_SIZE / 2

    Box(
        modifier = modifier
            .size(DIAL_SIZE)
            .pointerInput(enabled, minValue, maxValue, step) {
                if (!enabled) return@pointerInput
                detectDragGestures(
                    onDrag = { change, _ ->
                        change.consume()
                        val center = Offset(size.width / 2f, size.height / 2f)
                        val pos = change.position
                        val angleDeg = Math.toDegrees(
                            atan2((pos.y - center.y).toDouble(), (pos.x - center.x).toDouble()),
                        ).toFloat().let { if (it < 0f) it + 360f else it }
                        dragTarget = angleToValue(angleDeg)
                    },
                    onDragEnd = { onTargetChange(dragTarget.toDouble()) },
                )
            },
        contentAlignment = Alignment.Center,
    ) {
        Canvas(modifier = Modifier.size(DIAL_SIZE)) {
            val strokeWidthPx = STROKE_WIDTH.toPx()
            val radius = (size.minDimension - strokeWidthPx) / 2f
            val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
            val arcSize = Size(radius * 2, radius * 2)
            val center = Offset(size.width / 2f, size.height / 2f)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = SWEEP_ANGLE_DEG,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidthPx, cap = StrokeCap.Round),
            )

            if (current != null) {
                val angleRad = Math.toRadians(angleForFraction(fractionFor(current)).toDouble())
                val markerPos = Offset(
                    center.x + radius * cos(angleRad).toFloat(),
                    center.y + radius * sin(angleRad).toFloat(),
                )
                drawCircle(color = currentMarkerColor, radius = 6.dp.toPx(), center = markerPos)
            }

            val handleAngleRad = Math.toRadians(angleForFraction(fractionFor(dragTarget.toDouble())).toDouble())
            val handlePos = Offset(
                center.x + radius * cos(handleAngleRad).toFloat(),
                center.y + radius * sin(handleAngleRad).toFloat(),
            )
            drawCircle(color = handleColor, radius = strokeWidthPx / 2f + 4.dp.toPx(), center = handlePos)
        }

        if (current != null) {
            val angleRad = Math.toRadians(angleForFraction(fractionFor(current)).toDouble())
            val labelX = centerDp + labelRadiusDp * cos(angleRad).toFloat()
            val labelY = centerDp + labelRadiusDp * sin(angleRad).toFloat()
            Text(
                text = formatTemp(current, step) + unit,
                style = MaterialTheme.typography.labelLarge,
                color = currentMarkerColor,
                modifier = Modifier
                    .align(Alignment.TopStart)
                    .offset(x = labelX - 16.dp, y = labelY - 10.dp),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = formatTemp(dragTarget.toDouble(), step) + unit, style = MaterialTheme.typography.displayMedium)
        }
    }
}

private fun formatTemp(value: Double, step: Double): String =
    if (step >= 1.0) value.roundToInt().toString() else "%.1f".format(value)
