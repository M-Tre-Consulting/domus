package dev.domus.android.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.ArrowDropDownCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Autorenew
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.CleaningServices
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.HotTub
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.Numbers
import androidx.compose.material.icons.filled.Plumbing
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material.icons.filled.Cloud
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonPin
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import dev.domus.android.R

/** Best-effort icon per HA entity domain; falls back to a generic device icon. */
fun iconForDomain(domain: String): ImageVector = when (domain) {
    "light" -> Icons.Filled.Lightbulb
    "switch", "input_boolean" -> Icons.Filled.ToggleOn
    "fan" -> Icons.Filled.Air
    "automation" -> Icons.Filled.Bolt
    "binary_sensor", "sensor" -> Icons.Filled.Sensors
    "climate" -> Icons.Filled.Thermostat
    "water_heater" -> Icons.Filled.HotTub
    "media_player" -> Icons.Filled.PlayCircle
    "button" -> Icons.Filled.SmartButton
    "lock" -> Icons.Filled.Lock
    "cover", "garage_door" -> Icons.Filled.MeetingRoom
    "valve" -> Icons.Filled.Plumbing
    "camera" -> Icons.Filled.Camera
    "speaker" -> Icons.Filled.Speaker
    "vacuum", "lawn_mower" -> Icons.Filled.CleaningServices
    "scene" -> Icons.Filled.AutoAwesome
    "script" -> Icons.Filled.Code
    "number", "input_number" -> Icons.Filled.Numbers
    "select", "input_select" -> Icons.Filled.ArrowDropDownCircle
    "alarm_control_panel" -> Icons.Filled.Security
    "weather" -> Icons.Filled.Cloud
    "person" -> Icons.Filled.PersonPin
    "device_tracker" -> Icons.Filled.Person
    else -> Icons.Filled.DeviceUnknown
}

private val DOMAIN_LABEL_RES = mapOf(
    "light" to R.string.domain_light,
    "switch" to R.string.domain_switch,
    "input_boolean" to R.string.domain_input_boolean,
    "fan" to R.string.domain_fan,
    "automation" to R.string.domain_automation,
    "binary_sensor" to R.string.domain_sensor,
    "sensor" to R.string.domain_sensor,
    "climate" to R.string.domain_climate,
    "water_heater" to R.string.domain_water_heater,
    "media_player" to R.string.domain_media_player,
    "button" to R.string.domain_button,
    "lock" to R.string.domain_lock,
    "cover" to R.string.domain_cover,
    "garage_door" to R.string.domain_garage_door,
    "valve" to R.string.domain_valve,
    "camera" to R.string.domain_camera,
    "speaker" to R.string.domain_speaker,
    "vacuum" to R.string.domain_vacuum,
    "lawn_mower" to R.string.domain_lawn_mower,
    "scene" to R.string.domain_scene,
    "script" to R.string.domain_script,
    "number" to R.string.domain_number,
    "input_number" to R.string.domain_number,
    "select" to R.string.domain_select,
    "input_select" to R.string.domain_select,
    "alarm_control_panel" to R.string.domain_alarm_control_panel,
    "weather" to R.string.domain_weather,
    "person" to R.string.domain_person,
    "device_tracker" to R.string.domain_device_tracker,
)

/** String resource id for a domain's section title, or null for an unmapped/exotic domain. */
fun domainLabelRes(domain: String): Int? = DOMAIN_LABEL_RES[domain]

/** Human-readable section title for a domain, e.g. "binary_sensor" -> "Sensors". Must be called
 *  directly from composable code (not from inside a plain lambda like a sort comparator). */
@Composable
fun domainLabel(domain: String): String =
    domainLabelRes(domain)?.let { stringResource(it) }
        ?: domain.replace('_', ' ').replaceFirstChar { it.uppercase() }

/** Icon per HVAC mode, matching the convention most HA thermostat cards use. */
fun iconForHvacMode(mode: String): ImageVector = when (mode.lowercase()) {
    "cool" -> Icons.Filled.AcUnit
    "heat" -> Icons.Filled.Whatshot
    "dry" -> Icons.Filled.WaterDrop
    "fan_only" -> Icons.Filled.Air
    "auto", "heat_cool" -> Icons.Filled.Autorenew
    "off" -> Icons.Filled.PowerSettingsNew
    else -> Icons.Filled.Thermostat
}
