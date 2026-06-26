package dev.domus.android.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.ui.components.StateHistorySection
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.brightnessPercent
import dev.domus.shared.model.colorMode
import dev.domus.shared.model.colorTempKelvin
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.hueColor
import dev.domus.shared.model.maxColorTempKelvin
import dev.domus.shared.model.minColorTempKelvin
import dev.domus.shared.model.supportedColorModes
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlin.math.abs
import dev.domus.android.ui.LocalAnimatedVisibilityScope
import dev.domus.android.ui.LocalSharedTransitionScope

private val COLOR_MODES_WITH_BRIGHTNESS = setOf("brightness", "color_temp", "hs", "rgb", "rgbw", "rgbww", "xy", "white")
private val COLOR_MODES_WITH_TEMP = setOf("color_temp")
private val COLOR_MODES_WITH_COLOR = setOf("hs", "rgb", "rgbw", "rgbww", "xy")

private data class LightColorPreset(val label: String, val color: Color, val hue: Float, val saturation: Float)

private val LIGHT_COLOR_PRESETS = listOf(
    LightColorPreset("White",      Color(0xFFFFFFFF), 0f,   0f),
    LightColorPreset("Warm white", Color(0xFFFFD080), 36f,  50f),
    LightColorPreset("Red",        Color(0xFFFF3333), 0f,   100f),
    LightColorPreset("Orange",     Color(0xFFFF8800), 30f,  100f),
    LightColorPreset("Yellow",     Color(0xFFFFEE00), 58f,  100f),
    LightColorPreset("Green",      Color(0xFF22CC55), 135f, 100f),
    LightColorPreset("Cyan",       Color(0xFF00CCFF), 195f, 100f),
    LightColorPreset("Blue",       Color(0xFF3366FF), 228f, 100f),
    LightColorPreset("Violet",     Color(0xFF9933FF), 270f, 100f),
    LightColorPreset("Pink",       Color(0xFFFF33AA), 320f, 100f),
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LightDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current

    fun callService(call: HaServiceCall) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            try {
                session.repository.callService(call)
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't update: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = entity?.friendlyName ?: entityId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isOn = entity.state.equals("on", ignoreCase = true)
        // Fall back to current color_mode if supported_color_modes isn't populated.
        val colorModes = entity.supportedColorModes.ifEmpty { listOfNotNull(entity.colorMode) }
        val hasBrightness = colorModes.any { it in COLOR_MODES_WITH_BRIGHTNESS }
        val hasColorTemp = colorModes.any { it in COLOR_MODES_WITH_TEMP }
        val hasColor = colorModes.any { it in COLOR_MODES_WITH_COLOR }

        // Hero badge color: actual light hue when on+colored, primary when on, surfaceVariant when off.
        val actualLightColor = if (isOn) {
            entity.hueColor?.let { hs ->
                Color.hsv(hs.first, (hs.second / 100f * 0.85f).coerceIn(0f, 1f), 0.95f)
            }
        } else null
        val heroBgColor = actualLightColor
            ?: if (isOn) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant
        val heroIconTint = if (actualLightColor != null) {
            val lum = 0.299f * actualLightColor.red + 0.587f * actualLightColor.green + 0.114f * actualLightColor.blue
            if (lum > 0.5f) Color.Black.copy(alpha = 0.87f) else Color.White
        } else {
            if (isOn) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant
        }

        val sharedTransitionScope = LocalSharedTransitionScope.current
        val animatedVisibilityScope = LocalAnimatedVisibilityScope.current
        val heroSharedModifier = if (sharedTransitionScope != null && animatedVisibilityScope != null) {
            with(sharedTransitionScope) {
                Modifier.sharedElement(
                    sharedContentState = rememberSharedContentState(key = "hero_$entityId"),
                    animatedVisibilityScope = animatedVisibilityScope,
                )
            }
        } else Modifier

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

            // Tapping the hero badge toggles the light — no separate TopAppBar button needed.
            Surface(
                onClick = {
                    callService(
                        HaServiceCall("light", if (isOn) "turn_off" else "turn_on", entity.entityId),
                    )
                },
                shape = CircleShape,
                color = heroBgColor,
                modifier = heroSharedModifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.Lightbulb,
                        contentDescription = if (isOn) "Turn off" else "Turn on",
                        tint = heroIconTint,
                        modifier = Modifier.size(48.dp),
                    )
                }
            }

            Text(
                text = entity.state.toDisplayLabel(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
            )

            if (isOn) {
                Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

                if (hasBrightness) {
                    HorizontalDivider()
                    Spacer(Modifier.height(DesignTokens.Spacing.md.dp))
                    LightBrightnessSection(entity = entity, onCallService = ::callService)
                }

                if (hasColorTemp) {
                    Spacer(Modifier.height(DesignTokens.Spacing.md.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(DesignTokens.Spacing.md.dp))
                    LightColorTempSection(entity = entity, onCallService = ::callService)
                }

                if (hasColor) {
                    Spacer(Modifier.height(DesignTokens.Spacing.md.dp))
                    HorizontalDivider()
                    Spacer(Modifier.height(DesignTokens.Spacing.md.dp))
                    LightColorSection(entity = entity, onCallService = ::callService)
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.lg.dp))
            StateHistorySection(session = session, entityId = entityId)
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
        }
    }
}

@Composable
private fun LightBrightnessSection(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val remotePct = entity.brightnessPercent?.toFloat() ?: 100f
    var sliderPct by remember(entity.entityId, entity.brightnessPercent) { mutableFloatStateOf(remotePct) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Brightness", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${sliderPct.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Slider(
            value = sliderPct,
            onValueChange = { sliderPct = it },
            onValueChangeFinished = {
                onCallService(
                    HaServiceCall(
                        domain = "light",
                        service = "turn_on",
                        entityId = entity.entityId,
                        data = mapOf("brightness_pct" to JsonPrimitive(sliderPct.toInt())),
                    ),
                )
            },
            valueRange = 1f..100f,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Composable
private fun LightColorTempSection(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val minCt = entity.minColorTempKelvin ?: 2000
    val maxCt = entity.maxColorTempKelvin ?: 6500
    val remoteCt = entity.colorTempKelvin?.coerceIn(minCt, maxCt) ?: ((minCt + maxCt) / 2)
    var sliderCt by remember(entity.entityId, entity.colorTempKelvin) { mutableFloatStateOf(remoteCt.toFloat()) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Color temperature", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${sliderCt.toInt()} K",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color(0xFFFF8C00), Color(0xFFFFD070), Color(0xFFFFFFFF), Color(0xFFCCE8FF)),
                    ),
                ),
        )
        Slider(
            value = sliderCt,
            onValueChange = { sliderCt = it },
            onValueChangeFinished = {
                onCallService(
                    HaServiceCall(
                        domain = "light",
                        service = "turn_on",
                        entityId = entity.entityId,
                        data = mapOf("color_temp_kelvin" to JsonPrimitive(sliderCt.toInt())),
                    ),
                )
            },
            valueRange = minCt.toFloat()..maxCt.toFloat(),
            modifier = Modifier.fillMaxWidth(),
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text("Warm  ${minCt}K", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text("${maxCt}K  Cool", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun LightColorSection(entity: HaEntityState, onCallService: (HaServiceCall) -> Unit) {
    val currentHs = entity.hueColor
    var hueSlider by remember(entity.entityId, currentHs?.first) {
        mutableFloatStateOf(currentHs?.first ?: 0f)
    }
    var satSlider by remember(entity.entityId, currentHs?.second) {
        mutableFloatStateOf(currentHs?.second ?: 100f)
    }
    val hueGradientColors = remember { (0..12).map { i -> Color.hsv(i * 30f, 1f, 1f) } }

    fun sendHsColor() {
        onCallService(
            HaServiceCall(
                domain = "light",
                service = "turn_on",
                entityId = entity.entityId,
                data = mapOf(
                    "hs_color" to JsonArray(listOf(JsonPrimitive(hueSlider), JsonPrimitive(satSlider))),
                ),
            ),
        )
    }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Color", style = MaterialTheme.typography.labelLarge)
            Box(
                modifier = Modifier
                    .size(20.dp)
                    .clip(CircleShape)
                    .background(Color.hsv(hueSlider, (satSlider / 100f).coerceIn(0f, 1f), 1f)),
            )
        }

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(Brush.horizontalGradient(hueGradientColors)),
        )
        Slider(
            value = hueSlider,
            onValueChange = { hueSlider = it },
            onValueChangeFinished = { sendHsColor() },
            valueRange = 0f..360f,
            modifier = Modifier.fillMaxWidth(),
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("Saturation", style = MaterialTheme.typography.labelLarge)
            Text(
                text = "${satSlider.toInt()}%",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(RoundedCornerShape(3.dp))
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.White, Color.hsv(hueSlider, 1f, 1f)),
                    ),
                ),
        )
        Slider(
            value = satSlider,
            onValueChange = { satSlider = it },
            onValueChangeFinished = { sendHsColor() },
            valueRange = 0f..100f,
            modifier = Modifier.fillMaxWidth(),
        )

        Text(
            text = "Presets",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = DesignTokens.Spacing.xs.dp),
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp)) {
            items(LIGHT_COLOR_PRESETS) { preset ->
                val isSelected = currentHs != null && (
                    (preset.saturation < 5f && currentHs.second < 5f) ||
                    (abs(currentHs.first - preset.hue) < 15f && abs(currentHs.second - preset.saturation) < 20f)
                )
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(preset.color)
                        .then(
                            if (isSelected) {
                                Modifier.border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                            } else {
                                Modifier.border(1.dp, MaterialTheme.colorScheme.outlineVariant, CircleShape)
                            },
                        )
                        .clickable {
                            hueSlider = preset.hue
                            satSlider = preset.saturation
                            onCallService(
                                HaServiceCall(
                                    domain = "light",
                                    service = "turn_on",
                                    entityId = entity.entityId,
                                    data = mapOf(
                                        "hs_color" to JsonArray(
                                            listOf(JsonPrimitive(preset.hue), JsonPrimitive(preset.saturation)),
                                        ),
                                    ),
                                ),
                            )
                        },
                )
            }
        }
    }
}
