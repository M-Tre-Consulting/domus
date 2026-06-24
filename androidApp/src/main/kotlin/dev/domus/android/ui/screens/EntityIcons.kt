package dev.domus.android.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Air
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.DeviceUnknown
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.MeetingRoom
import androidx.compose.material.icons.filled.PlayCircle
import androidx.compose.material.icons.filled.Sensors
import androidx.compose.material.icons.filled.SmartButton
import androidx.compose.material.icons.filled.Speaker
import androidx.compose.material.icons.filled.Thermostat
import androidx.compose.material.icons.filled.ToggleOn
import androidx.compose.ui.graphics.vector.ImageVector

/** Best-effort icon per HA entity domain; falls back to a generic device icon. */
fun iconForDomain(domain: String): ImageVector = when (domain) {
    "light" -> Icons.Filled.Lightbulb
    "switch", "input_boolean" -> Icons.Filled.ToggleOn
    "fan" -> Icons.Filled.Air
    "automation" -> Icons.Filled.Bolt
    "binary_sensor", "sensor" -> Icons.Filled.Sensors
    "climate" -> Icons.Filled.Thermostat
    "media_player" -> Icons.Filled.PlayCircle
    "button" -> Icons.Filled.SmartButton
    "lock" -> Icons.Filled.Lock
    "cover", "garage_door" -> Icons.Filled.MeetingRoom
    "camera" -> Icons.Filled.Camera
    "speaker" -> Icons.Filled.Speaker
    else -> Icons.Filled.DeviceUnknown
}
