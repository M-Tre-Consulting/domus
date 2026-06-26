package dev.domus.android.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaHistoryPoint
import kotlin.time.Instant

private val ACTIVE_STATES = setOf(
    "on", "playing", "heat", "cool", "fan_only", "dry", "auto", "heat_cool",
    "home", "open", "unlocked", "triggered", "mowing", "cleaning", "armed_home",
    "armed_away", "armed_night", "detected",
)

private data class ChartPoint(val timeMs: Long, val value: Double?, val state: String)

@Composable
fun StateHistorySection(
    session: HaSession,
    entityId: String,
    modifier: Modifier = Modifier,
) {
    var points by remember(entityId) { mutableStateOf<List<HaHistoryPoint>?>(null) }
    LaunchedEffect(entityId) {
        points = session.restApi.getHistory(entityId)
    }

    Column(modifier = modifier.fillMaxWidth()) {
        Text(
            text = "Last 24 hours",
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.primary,
        )
        Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))

        when {
            points == null -> Box(
                Modifier.fillMaxWidth().height(64.dp),
                contentAlignment = Alignment.Center,
            ) {
                CircularProgressIndicator(Modifier.size(24.dp), strokeWidth = 2.dp)
            }

            points!!.isEmpty() -> Text(
                text = "No history available",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )

            else -> {
                val windowEndMs = remember { kotlin.time.Clock.System.now().toEpochMilliseconds() }
                val windowStartMs = windowEndMs - 24 * 3_600_000L
                StateHistoryChart(
                    points = points!!,
                    windowStartMs = windowStartMs,
                    windowEndMs = windowEndMs,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun StateHistoryChart(
    points: List<HaHistoryPoint>,
    windowStartMs: Long,
    windowEndMs: Long,
    modifier: Modifier = Modifier,
) {
    val chartPoints = remember(points) {
        points.mapNotNull { p ->
            val ms = try { Instant.parse(p.lastChanged).toEpochMilliseconds() } catch (_: Exception) { return@mapNotNull null }
            ChartPoint(ms, p.state.toDoubleOrNull(), p.state)
        }.sortedBy { it.timeMs }
    }

    val isNumeric = remember(chartPoints) { chartPoints.all { it.value != null } && chartPoints.any { it.value != null } }

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = MaterialTheme.shapes.small

    val windowDurationMs = (windowEndMs - windowStartMs).toFloat()

    if (isNumeric) {
        val numericPoints = chartPoints.filter { it.value != null }
        val minVal = numericPoints.minOf { it.value!! }
        val maxVal = numericPoints.maxOf { it.value!! }
        val valRange = (maxVal - minVal).coerceAtLeast(0.001)

        Canvas(
            modifier = modifier
                .height(100.dp)
                .clip(shape),
        ) {
            drawRect(surfaceVariantColor.copy(alpha = 0.3f))

            val linePath = Path()
            val fillPath = Path()
            var prevX = 0f
            var prevY = size.height / 2f

            numericPoints.forEachIndexed { i, pt ->
                val x = ((pt.timeMs - windowStartMs) / windowDurationMs * size.width).coerceIn(0f, size.width)
                val y = ((1.0 - (pt.value!! - minVal) / valRange) * size.height).toFloat().coerceIn(0f, size.height)

                if (i == 0) {
                    linePath.moveTo(0f, y)
                    linePath.lineTo(x, y)
                    fillPath.moveTo(0f, size.height)
                    fillPath.lineTo(0f, y)
                    fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, prevY)
                    linePath.lineTo(x, y)
                    fillPath.lineTo(x, prevY)
                    fillPath.lineTo(x, y)
                }
                prevX = x
                prevY = y
            }
            linePath.lineTo(size.width, prevY)
            fillPath.lineTo(size.width, prevY)
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(fillPath, primaryContainerColor.copy(alpha = 0.4f))
            drawPath(
                linePath,
                primaryColor,
                style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round),
            )
        }

        // Y-axis range label
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("%.1f".format(minVal), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
            Text("%.1f".format(maxVal), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
        }
    } else {
        // State timeline
        Canvas(
            modifier = modifier
                .height(56.dp)
                .clip(shape),
        ) {
            drawRect(surfaceVariantColor.copy(alpha = 0.5f))

            chartPoints.forEachIndexed { i, pt ->
                val nextPt = chartPoints.getOrNull(i + 1)
                val x1 = ((pt.timeMs - windowStartMs) / windowDurationMs * size.width).coerceIn(0f, size.width)
                val x2 = if (nextPt != null)
                    ((nextPt.timeMs - windowStartMs) / windowDurationMs * size.width).coerceIn(0f, size.width)
                else
                    size.width

                if (pt.state in ACTIVE_STATES) {
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(x1, 0f),
                        size = Size((x2 - x1).coerceAtLeast(0f), size.height),
                    )
                }
            }

            // Tick marks at 25%, 50%, 75%
            val tickColor = surfaceVariantColor.copy(alpha = 0.6f)
            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color = tickColor,
                    start = Offset(frac * size.width, 0f),
                    end = Offset(frac * size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }

    // Time axis labels
    val labelMs = windowEndMs - windowStartMs
    val labelStartHour = Instant.fromEpochMilliseconds(windowStartMs).toString().substring(11, 16)
    val labelEndHour = Instant.fromEpochMilliseconds(windowEndMs).toString().substring(11, 16)
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(labelStartHour, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
        Text(labelEndHour, style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
    }
}
