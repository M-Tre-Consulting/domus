package dev.domus.desktop.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.rememberTransformableState
import androidx.compose.foundation.gestures.transformable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.AssistChip
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.Dp
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

private val TIME_RANGES = listOf(24 to "24 h", 48 to "48 h", 168 to "7 days")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StateHistorySection(
    session: HaSession,
    entityId: String,
    entityName: String = entityId,
    modifier: Modifier = Modifier,
) {
    var points by remember(entityId) { mutableStateOf<List<HaHistoryPoint>?>(null) }
    LaunchedEffect(entityId) { points = session.restApi.getHistory(entityId) }

    var showSheet by remember { mutableStateOf(false) }
    var sheetHours by remember { mutableIntStateOf(24) }
    var sheetPoints by remember { mutableStateOf<List<HaHistoryPoint>?>(null) }

    LaunchedEffect(showSheet, sheetHours, entityId) {
        if (!showSheet) { sheetPoints = null; return@LaunchedEffect }
        sheetPoints = null
        sheetPoints = session.restApi.getHistory(entityId, sheetHours)
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
                Box(
                    modifier = Modifier
                        .clip(MaterialTheme.shapes.small)
                        .clickable { showSheet = true },
                ) {
                    StateHistoryChart(
                        points = points!!,
                        windowStartMs = windowStartMs,
                        windowEndMs = windowEndMs,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                Text(
                    text = "Click to expand",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    modifier = Modifier.align(Alignment.End).padding(top = 2.dp),
                )
            }
        }
    }

    if (showSheet) {
        ModalBottomSheet(
            onDismissRequest = { showSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = DesignTokens.Spacing.lg.dp)
                    .padding(bottom = DesignTokens.Spacing.xl.dp),
            ) {
                Text(
                    text = entityName,
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = DesignTokens.Spacing.md.dp),
                )
                Row(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp)) {
                    TIME_RANGES.forEach { (h, label) ->
                        FilterChip(
                            selected = sheetHours == h,
                            onClick = { sheetHours = h },
                            label = { Text(label) },
                        )
                    }
                }
                Spacer(Modifier.height(DesignTokens.Spacing.md.dp))

                when {
                    sheetPoints == null -> Box(
                        Modifier.fillMaxWidth().height(200.dp),
                        contentAlignment = Alignment.Center,
                    ) { CircularProgressIndicator() }

                    sheetPoints!!.isEmpty() -> Text(
                        text = "No history available for this period",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )

                    else -> {
                        val sheetEndMs = remember(sheetHours) {
                            kotlin.time.Clock.System.now().toEpochMilliseconds()
                        }
                        val sheetStartMs = sheetEndMs - sheetHours * 3_600_000L
                        StateHistoryChart(
                            points = sheetPoints!!,
                            windowStartMs = sheetStartMs,
                            windowEndMs = sheetEndMs,
                            largeMode = true,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun StateHistoryChart(
    points: List<HaHistoryPoint>,
    windowStartMs: Long,
    windowEndMs: Long,
    largeMode: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val chartPoints = remember(points) {
        points.mapNotNull { p ->
            val ms = try {
                Instant.parse(p.lastChanged).toEpochMilliseconds()
            } catch (_: Exception) {
                return@mapNotNull null
            }
            ChartPoint(ms, p.state.toDoubleOrNull(), p.state)
        }.sortedBy { it.timeMs }
    }
    val isNumeric = chartPoints.isNotEmpty() && chartPoints.all { it.value != null }

    var scale by remember { mutableFloatStateOf(1f) }
    var panOffsetFraction by remember { mutableFloatStateOf(0f) }
    var canvasWidthPx by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(windowStartMs, windowEndMs) {
        scale = 1f
        panOffsetFraction = 0f
    }

    val transformableState = rememberTransformableState { zoomChange, panChange, _ ->
        val newScale = (scale * zoomChange).coerceIn(1f, 8f)
        val maxOffset = (1f - 1f / newScale).coerceAtLeast(0f)
        val panDelta = if (canvasWidthPx > 0f) (-panChange.x / canvasWidthPx) * (1f / newScale) else 0f
        panOffsetFraction = (panOffsetFraction + panDelta).coerceIn(0f, maxOffset)
        scale = newScale
    }

    val totalMs = (windowEndMs - windowStartMs).toFloat()
    val effectiveStart = if (largeMode) windowStartMs + (totalMs * panOffsetFraction).toLong() else windowStartMs
    val effectiveEnd = if (largeMode) (effectiveStart + totalMs / scale).toLong().coerceAtMost(windowEndMs) else windowEndMs
    val drawDurationMs = (effectiveEnd - effectiveStart).toFloat().coerceAtLeast(1f)

    val primaryColor = MaterialTheme.colorScheme.primary
    val surfaceVariantColor = MaterialTheme.colorScheme.surfaceVariant
    val primaryContainerColor = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariantColor = MaterialTheme.colorScheme.onSurfaceVariant
    val shape = MaterialTheme.shapes.small

    val timelineHeight: Dp = if (largeMode) 140.dp else 56.dp
    val lineChartHeight: Dp = if (largeMode) 240.dp else 100.dp

    if (largeMode) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(bottom = 4.dp),
            horizontalArrangement = Arrangement.End,
        ) {
            if (scale > 1.05f) {
                AssistChip(
                    onClick = { scale = 1f; panOffsetFraction = 0f },
                    label = { Text("Reset zoom  ×${"%.1f".format(scale)}") },
                )
            } else {
                Text(
                    text = "Scroll to zoom · drag to pan",
                    style = MaterialTheme.typography.labelSmall,
                    color = onSurfaceVariantColor.copy(alpha = 0.6f),
                )
            }
        }
    }

    if (isNumeric) {
        val numericPoints = chartPoints.filter { it.value != null }
        val minVal = numericPoints.minOf { it.value!! }
        val maxVal = numericPoints.maxOf { it.value!! }
        val valRange = (maxVal - minVal).coerceAtLeast(0.001)

        Canvas(
            modifier = modifier
                .height(lineChartHeight)
                .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                .run { if (largeMode) transformable(transformableState) else this }
                .clip(shape),
        ) {
            drawRect(surfaceVariantColor.copy(alpha = 0.3f))

            val linePath = Path()
            val fillPath = Path()
            var prevY = size.height / 2f

            numericPoints.forEachIndexed { i, pt ->
                val x = ((pt.timeMs - effectiveStart) / drawDurationMs * size.width).coerceIn(0f, size.width)
                val y = ((1.0 - (pt.value!! - minVal) / valRange) * size.height).toFloat().coerceIn(0f, size.height)

                if (i == 0) {
                    linePath.moveTo(0f, y); linePath.lineTo(x, y)
                    fillPath.moveTo(0f, size.height); fillPath.lineTo(0f, y); fillPath.lineTo(x, y)
                } else {
                    linePath.lineTo(x, prevY); linePath.lineTo(x, y)
                    fillPath.lineTo(x, prevY); fillPath.lineTo(x, y)
                }
                prevY = y
            }
            linePath.lineTo(size.width, prevY)
            fillPath.lineTo(size.width, prevY)
            fillPath.lineTo(size.width, size.height)
            fillPath.close()

            drawPath(fillPath, primaryContainerColor.copy(alpha = 0.4f))
            drawPath(linePath, primaryColor, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text("%.1f".format(minVal), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
            Text("%.1f".format(maxVal), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
        }
    } else {
        Canvas(
            modifier = modifier
                .height(timelineHeight)
                .onSizeChanged { canvasWidthPx = it.width.toFloat() }
                .run { if (largeMode) transformable(transformableState) else this }
                .clip(shape),
        ) {
            drawRect(surfaceVariantColor.copy(alpha = 0.5f))

            chartPoints.forEachIndexed { i, pt ->
                val nextPt = chartPoints.getOrNull(i + 1)
                val x1 = ((pt.timeMs - effectiveStart) / drawDurationMs * size.width).coerceIn(0f, size.width)
                val x2 = if (nextPt != null) {
                    ((nextPt.timeMs - effectiveStart) / drawDurationMs * size.width).coerceIn(0f, size.width)
                } else {
                    size.width
                }
                if (pt.state in ACTIVE_STATES) {
                    drawRect(
                        color = primaryColor,
                        topLeft = Offset(x1, 0f),
                        size = Size((x2 - x1).coerceAtLeast(0f), size.height),
                    )
                }
            }

            listOf(0.25f, 0.5f, 0.75f).forEach { frac ->
                drawLine(
                    color = surfaceVariantColor.copy(alpha = 0.6f),
                    start = Offset(frac * size.width, 0f),
                    end = Offset(frac * size.width, size.height),
                    strokeWidth = 1.dp.toPx(),
                )
            }
        }
    }

    val formatLabel = { ms: Long ->
        val str = Instant.fromEpochMilliseconds(ms).toString()
        if (largeMode) str.substring(5, 16).replace('T', ' ') else str.substring(11, 16)
    }
    Row(
        modifier = Modifier.fillMaxWidth().padding(top = 2.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(formatLabel(effectiveStart), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
        Text(formatLabel(effectiveEnd), style = MaterialTheme.typography.labelSmall, color = onSurfaceVariantColor)
    }
}
