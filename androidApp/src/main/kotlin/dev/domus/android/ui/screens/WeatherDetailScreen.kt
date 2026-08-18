package dev.domus.android.ui.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.R
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaForecastEntry
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.weatherAttribution
import dev.domus.shared.model.weatherHumidity
import dev.domus.shared.model.weatherPressure
import dev.domus.shared.model.weatherPressureUnit
import dev.domus.shared.model.weatherTemperature
import dev.domus.shared.model.weatherTemperatureUnit
import dev.domus.shared.model.weatherWindSpeed
import dev.domus.shared.model.weatherWindSpeedUnit
import java.time.LocalDate
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

/** Vertical sky gradient (top, bottom) for an HA weather `condition` - the visual mood of the
 *  hero header instead of a flat card background. */
private fun skyGradientFor(condition: String): Pair<Color, Color> = when (condition.lowercase()) {
    "sunny" -> Color(0xFF2E9BFF) to Color(0xFFA7D9FF)
    "clear-night" -> Color(0xFF0B1440) to Color(0xFF2C2465)
    "partlycloudy" -> Color(0xFF4E86C6) to Color(0xFFB9D3E8)
    "cloudy" -> Color(0xFF6E8CA0) to Color(0xFFB9C7D1)
    "fog" -> Color(0xFF93A2AC) to Color(0xFFD6DEE3)
    "windy", "windy-variant" -> Color(0xFF4E8583) to Color(0xFFAFCFC9)
    "rainy", "pouring" -> Color(0xFF32475C) to Color(0xFF6E8CA3)
    "lightning", "lightning-rainy" -> Color(0xFF1C2333) to Color(0xFF3E4E68)
    "snowy" -> Color(0xFF7FA3C9) to Color(0xFFEAF3FA)
    "snowy-rainy", "hail" -> Color(0xFF6C87A6) to Color(0xFFD6E3EC)
    else -> Color(0xFF4E86C6) to Color(0xFFB9D3E8)
}

private enum class ParticleKind { RAIN, SNOW, STARS, SUN, NONE }

private fun particleKindFor(condition: String): ParticleKind = when (condition.lowercase()) {
    "rainy", "pouring", "lightning", "lightning-rainy" -> ParticleKind.RAIN
    "snowy", "snowy-rainy", "hail" -> ParticleKind.SNOW
    "clear-night" -> ParticleKind.STARS
    "sunny" -> ParticleKind.SUN
    else -> ParticleKind.NONE
}

/** Ambient motion behind the hero - falling rain/snow, twinkling stars or a soft pulsing sun
 *  glow, matching the condition - so the header feels like weather instead of a static card. */
@Composable
private fun WeatherParticles(condition: String, modifier: Modifier = Modifier) {
    val kind = particleKindFor(condition)
    if (kind == ParticleKind.NONE) return

    val durationMillis = when (kind) {
        ParticleKind.RAIN -> 1200
        ParticleKind.SNOW -> 6000
        ParticleKind.STARS -> 4000
        else -> 5000
    }
    val transition = rememberInfiniteTransition(label = "weatherParticles")
    val t by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(durationMillis, easing = LinearEasing)),
        label = "particleTime",
    )

    Canvas(modifier = modifier) {
        when (kind) {
            ParticleKind.RAIN -> drawRain(t, size)
            ParticleKind.SNOW -> drawSnow(t, size)
            ParticleKind.STARS -> drawStars(t, size)
            ParticleKind.SUN -> drawSunGlow(t, size)
            ParticleKind.NONE -> {}
        }
    }
}

private fun DrawScope.drawRain(t: Float, canvasSize: androidx.compose.ui.geometry.Size) {
    val random = Random(42)
    repeat(40) {
        val seedX = random.nextFloat()
        val phase = random.nextFloat()
        val speed = 0.85f + random.nextFloat() * 0.3f
        val progress = (t * speed + phase) % 1f
        val length = 16.dp.toPx()
        val x = seedX * canvasSize.width
        val y = progress * (canvasSize.height + length) - length
        drawLine(
            color = Color.White.copy(alpha = 0.5f),
            start = Offset(x, y),
            end = Offset(x - 4.dp.toPx(), y + length),
            strokeWidth = 1.5.dp.toPx(),
            cap = StrokeCap.Round,
        )
    }
}

private fun DrawScope.drawSnow(t: Float, canvasSize: androidx.compose.ui.geometry.Size) {
    val random = Random(7)
    repeat(30) {
        val seedX = random.nextFloat()
        val phase = random.nextFloat()
        val speed = 0.5f + random.nextFloat() * 0.4f
        val swayFreq = 1f + random.nextFloat() * 2f
        val swayAmp = 6.dp.toPx() + random.nextFloat() * 10.dp.toPx()
        val radius = 1.5.dp.toPx() + random.nextFloat() * 2.dp.toPx()
        val progress = (t * speed + phase) % 1f
        val baseX = seedX * canvasSize.width
        val x = baseX + sin((progress * 2 * PI * swayFreq).toFloat()) * swayAmp
        val y = progress * (canvasSize.height + radius * 2) - radius
        drawCircle(color = Color.White.copy(alpha = 0.85f), radius = radius, center = Offset(x, y))
    }
}

private fun DrawScope.drawStars(t: Float, canvasSize: androidx.compose.ui.geometry.Size) {
    val random = Random(99)
    repeat(25) {
        val x = random.nextFloat() * canvasSize.width
        val y = random.nextFloat() * canvasSize.height * 0.8f
        val phase = random.nextFloat()
        val freq = 0.7f + random.nextFloat() * 1.3f
        val alpha = (sin((t * 2 * PI * freq + phase * 2 * PI).toFloat()) * 0.35f + 0.65f).coerceIn(0.3f, 1f)
        val radius = 1.dp.toPx() + random.nextFloat() * 1.2.dp.toPx()
        drawCircle(color = Color.White.copy(alpha = alpha), radius = radius, center = Offset(x, y))
    }
}

private fun DrawScope.drawSunGlow(t: Float, canvasSize: androidx.compose.ui.geometry.Size) {
    val pulse = sin((t * 2 * PI).toFloat()) * 0.06f + 1f
    val center = Offset(canvasSize.width / 2f, canvasSize.height * 0.34f)
    val baseRadius = canvasSize.width * 0.32f
    val radius = baseRadius * pulse
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(Color.White.copy(alpha = 0.35f), Color.White.copy(alpha = 0f)),
            center = center,
            radius = radius,
        ),
        radius = radius,
        center = center,
    )
}

private fun parseForecastTime(datetime: String): OffsetDateTime? =
    runCatching { OffsetDateTime.parse(datetime) }.getOrNull()

private fun formatHour(datetime: String): String {
    val local = parseForecastTime(datetime)?.atZoneSameInstant(ZoneId.systemDefault()) ?: return datetime.take(5)
    return DateTimeFormatter.ofPattern("HH:mm").format(local)
}

private fun formatDayLabel(datetime: String, todayLabel: String): String {
    val local = parseForecastTime(datetime)?.atZoneSameInstant(ZoneId.systemDefault()) ?: return datetime.take(10)
    return if (local.toLocalDate() == LocalDate.now()) todayLabel
    else DateTimeFormatter.ofPattern("EEE", Locale.getDefault()).format(local).replaceFirstChar { it.uppercase() }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]

    var hourly by remember(entityId) { mutableStateOf<List<HaForecastEntry>>(emptyList()) }
    var daily by remember(entityId) { mutableStateOf<List<HaForecastEntry>>(emptyList()) }

    LaunchedEffect(entityId) {
        hourly = runCatching { session.repository.getForecast(entityId, "hourly") }.getOrDefault(emptyList())
        daily = runCatching { session.repository.getForecast(entityId, "daily") }.getOrDefault(emptyList())
    }

    val (skyTop, skyBottom) = skyGradientFor(entity?.state.orEmpty())
    val heroBrush = Brush.verticalGradient(listOf(skyTop, skyBottom))

    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
        Box(modifier = Modifier.fillMaxWidth().height(320.dp).background(heroBrush))
        WeatherParticles(condition = entity?.state.orEmpty(), modifier = Modifier.fillMaxWidth().height(320.dp))
        Scaffold(
            containerColor = Color.Transparent,
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = entity?.friendlyName ?: entityId,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            color = Color.White,
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onBack) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back), tint = Color.White)
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent),
                )
            },
        ) { padding ->
            if (entity == null) {
                Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator(color = Color.White)
                }
                return@Scaffold
            }

            val humidityLabel = stringResource(R.string.climate_humidity)
            val windLabel = stringResource(R.string.weather_wind)
            val pressureLabel = stringResource(R.string.weather_pressure)
            val rows = buildList {
                entity.weatherHumidity?.let { add(humidityLabel to "${it.toInt()}%") }
                entity.weatherWindSpeed?.let { add(windLabel to "%.1f %s".format(it, entity.weatherWindSpeedUnit.orEmpty())) }
                entity.weatherPressure?.let { add(pressureLabel to "%.0f %s".format(it, entity.weatherPressureUnit.orEmpty())) }
            }
            val unit = entity.weatherTemperatureUnit ?: "°"
            val todayLabel = stringResource(R.string.weather_today)

            Column(
                modifier = Modifier.fillMaxSize().padding(padding).verticalScroll(rememberScrollState()),
            ) {
                // --- Hero: floating condition icon, big current temp, today's hi/lo ---
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.lg.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    FloatingWeatherIcon(condition = entity.state)
                    Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
                    Text(
                        text = entity.weatherTemperature?.let { "%.0f%s".format(it, unit) } ?: "—",
                        style = MaterialTheme.typography.displayLarge.copy(fontWeight = FontWeight.Bold),
                        color = Color.White,
                    )
                    Text(
                        text = entity.state.toDisplayLabel(),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Color.White.copy(alpha = 0.9f),
                    )
                    daily.firstOrNull()?.let { today ->
                        if (today.temperature != null && today.templow != null) {
                            Text(
                                text = "H:%.0f%s L:%.0f%s".format(today.temperature, unit, today.templow, unit),
                                style = MaterialTheme.typography.bodyMedium,
                                color = Color.White.copy(alpha = 0.85f),
                                modifier = Modifier.padding(top = DesignTokens.Spacing.xs.dp),
                            )
                        }
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))
                }

                // --- Below the fold: hourly strip, daily list, stats, attribution ---
                Column(modifier = Modifier.fillMaxWidth().padding(horizontal = DesignTokens.Spacing.lg.dp)) {
                    if (hourly.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.weather_hourly_forecast),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignTokens.Spacing.lg.dp, bottom = DesignTokens.Spacing.sm.dp),
                        )
                        HourlyForecastRow(entries = hourly.take(24), unit = unit)
                    }

                    if (daily.isNotEmpty()) {
                        Text(
                            text = stringResource(R.string.weather_daily_forecast),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(top = DesignTokens.Spacing.lg.dp, bottom = DesignTokens.Spacing.sm.dp),
                        )
                        DailyForecastCard(entries = daily.take(7), unit = unit, todayLabel = todayLabel)
                    }

                    InfoCard(rows = rows)

                    entity.weatherAttribution?.let { attribution ->
                        Text(
                            text = attribution,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxWidth().padding(top = DesignTokens.Spacing.md.dp),
                        )
                    }
                    Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
                }
            }
        }
    }
}

/** The hero condition icon, gently bobbing up and down - a small sign of life instead of a
 *  static glyph sitting in a circle. */
@Composable
private fun FloatingWeatherIcon(condition: String) {
    val transition = rememberInfiniteTransition(label = "weatherFloat")
    val offset by transition.animateFloat(
        initialValue = -6f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "floatOffset",
    )
    Icon(
        imageVector = iconForWeatherCondition(condition),
        contentDescription = null,
        tint = Color.White,
        modifier = Modifier
            .size(72.dp)
            .graphicsLayer { translationY = offset },
    )
}

@Composable
private fun HourlyForecastRow(entries: List<HaForecastEntry>, unit: String) {
    val nowLabel = stringResource(R.string.weather_now)
    LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp)) {
        items(entries.size) { index ->
            val entry = entries[index]
            Card(
                shape = RoundedCornerShape(DesignTokens.Shape.cornerLarge.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainerHigh),
                modifier = Modifier.width(64.dp),
            ) {
                Column(
                    modifier = Modifier.padding(vertical = DesignTokens.Spacing.sm.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = if (index == 0) nowLabel else formatHour(entry.datetime),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = 1,
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.xs.dp))
                    Icon(
                        imageVector = iconForWeatherCondition(entry.condition.orEmpty()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(24.dp),
                    )
                    Spacer(Modifier.height(DesignTokens.Spacing.xs.dp))
                    Text(
                        text = entry.temperature?.let { "%.0f%s".format(it, unit) } ?: "—",
                        style = MaterialTheme.typography.labelLarge,
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyForecastCard(entries: List<HaForecastEntry>, unit: String, todayLabel: String) {
    val weekMin = entries.mapNotNull { it.templow ?: it.temperature }.minOrNull() ?: 0.0
    val weekMax = entries.mapNotNull { it.temperature }.maxOrNull() ?: 1.0

    Card(modifier = Modifier.fillMaxWidth()) {
        Column {
            entries.forEachIndexed { index, entry ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(
                        text = formatDayLabel(entry.datetime, todayLabel),
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.width(56.dp),
                    )
                    Icon(
                        imageVector = iconForWeatherCondition(entry.condition.orEmpty()),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(22.dp),
                    )
                    val low = entry.templow
                    val high = entry.temperature
                    if (low != null && high != null) {
                        Text(
                            text = "%.0f%s".format(low, unit),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(start = DesignTokens.Spacing.md.dp).width(36.dp),
                        )
                        DailyTempRangeBar(
                            low = low,
                            high = high,
                            weekMin = weekMin,
                            weekMax = weekMax,
                            modifier = Modifier.weight(1f).padding(horizontal = DesignTokens.Spacing.sm.dp),
                        )
                        Text(
                            text = "%.0f%s".format(high, unit),
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.width(36.dp),
                            textAlign = TextAlign.End,
                        )
                    } else {
                        Spacer(Modifier.weight(1f))
                        Text(
                            text = entry.temperature?.let { "%.0f%s".format(it, unit) } ?: "—",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Bold,
                        )
                    }
                }
                if (index != entries.lastIndex) HorizontalDivider()
            }
        }
    }
}

/** A pill-shaped track showing where today's low/high sits within the week's overall range -
 *  the same idea as the range bar in Apple/Google's weather apps, cool-to-warm gradient fill. */
@Composable
private fun DailyTempRangeBar(low: Double, high: Double, weekMin: Double, weekMax: Double, modifier: Modifier = Modifier) {
    val span = (weekMax - weekMin).takeIf { it > 0.01 } ?: 1.0
    val startFrac = ((low - weekMin) / span).toFloat().coerceIn(0f, 1f)
    val endFrac = ((high - weekMin) / span).toFloat().coerceIn(0f, 1f)
    val trackColor = MaterialTheme.colorScheme.surfaceVariant

    Canvas(modifier = modifier.height(6.dp)) {
        val radius = CornerRadius(size.height / 2, size.height / 2)
        drawRoundRect(color = trackColor, cornerRadius = radius)
        val left = size.width * startFrac
        val right = (size.width * endFrac).coerceAtLeast(left + size.height)
        drawRoundRect(
            brush = Brush.horizontalGradient(listOf(Color(0xFF5B9BD5), Color(0xFFFF8A50))),
            topLeft = Offset(left, 0f),
            size = Size(right - left, size.height),
            cornerRadius = radius,
        )
    }
}
