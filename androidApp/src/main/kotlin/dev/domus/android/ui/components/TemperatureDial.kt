package dev.domus.android.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import kotlin.math.roundToInt

private const val START_ANGLE_DEG = 135f
private const val SWEEP_ANGLE_DEG = 270f

/**
 * A circular thermostat-style dial: drag anywhere on the ring to choose a target
 * temperature within [minValue, maxValue], snapped to [step]. The 90-degree gap at the
 * bottom is intentional (matches the common Nest/HA thermostat-card layout).
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
    val progressColor = MaterialTheme.colorScheme.primary

    fun angleToValue(angleDeg: Float): Float {
        var relative = (angleDeg - START_ANGLE_DEG) % 360f
        if (relative < 0f) relative += 360f
        val clamped = relative.coerceIn(0f, SWEEP_ANGLE_DEG)
        val fraction = clamped / SWEEP_ANGLE_DEG
        val raw = minValue + fraction * (maxValue - minValue)
        val stepped = (raw / step).roundToInt() * step
        return stepped.toFloat().coerceIn(minValue.toFloat(), maxValue.toFloat())
    }

    Box(
        modifier = modifier
            .size(240.dp)
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
        Canvas(modifier = Modifier.size(240.dp)) {
            val strokeWidth = 18.dp.toPx()
            val radius = (size.minDimension - strokeWidth) / 2f
            val topLeft = Offset((size.width - radius * 2) / 2f, (size.height - radius * 2) / 2f)
            val arcSize = Size(radius * 2, radius * 2)

            drawArc(
                color = trackColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = SWEEP_ANGLE_DEG,
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )

            val valueFraction = ((dragTarget - minValue) / (maxValue - minValue)).coerceIn(0.0, 1.0)
            drawArc(
                color = progressColor,
                startAngle = START_ANGLE_DEG,
                sweepAngle = (SWEEP_ANGLE_DEG * valueFraction).toFloat(),
                useCenter = false,
                topLeft = topLeft,
                size = arcSize,
                style = Stroke(width = strokeWidth, cap = StrokeCap.Round),
            )
        }

        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = "%.1f%s".format(dragTarget, unit), style = MaterialTheme.typography.displaySmall)
            if (current != null) {
                Text(text = "Current %.1f%s".format(current, unit), style = MaterialTheme.typography.bodyMedium)
            }
        }
    }
}
