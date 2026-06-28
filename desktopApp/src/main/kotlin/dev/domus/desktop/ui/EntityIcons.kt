package dev.domus.desktop.ui

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
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector

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
    else -> Icons.Filled.DeviceUnknown
}

private val DOMAIN_LABELS = mapOf(
    "light" to "Lights",
    "switch" to "Switches",
    "input_boolean" to "Toggles",
    "fan" to "Fans",
    "automation" to "Automations",
    "binary_sensor" to "Sensors",
    "sensor" to "Sensors",
    "climate" to "Climate",
    "water_heater" to "Water Heaters",
    "media_player" to "Media Players",
    "button" to "Buttons",
    "lock" to "Locks",
    "cover" to "Covers",
    "garage_door" to "Garage Doors",
    "valve" to "Valves",
    "camera" to "Cameras",
    "speaker" to "Speakers",
    "vacuum" to "Vacuums",
    "lawn_mower" to "Lawn Mowers",
    "scene" to "Scenes",
    "script" to "Scripts",
    "number" to "Numbers",
    "input_number" to "Numbers",
    "select" to "Selectors",
    "input_select" to "Selectors",
)

fun domainLabel(domain: String): String =
    DOMAIN_LABELS[domain] ?: domain.replace('_', ' ').replaceFirstChar { it.uppercase() }

fun iconForHvacMode(mode: String): ImageVector = when (mode.lowercase()) {
    "cool" -> Icons.Filled.AcUnit
    "heat" -> Icons.Filled.Whatshot
    "dry" -> Icons.Filled.WaterDrop
    "fan_only" -> Icons.Filled.Air
    "auto", "heat_cool" -> Icons.Filled.Autorenew
    "off" -> Icons.Filled.PowerSettingsNew
    else -> Icons.Filled.Thermostat
}
