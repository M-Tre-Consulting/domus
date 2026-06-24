package dev.domus.shared.model

import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull

/**
 * Common HA attribute readers, shared by every UI so Android and Desktop interpret
 * entity attributes (brightness, temperature, media metadata...) the same way.
 */

val HaEntityState.friendlyName: String
    get() = (attribute("friendly_name") as? JsonPrimitive)?.contentOrNull ?: entityId

/** Brightness as a 0-100 percentage; HA reports it as a 0-255 attribute. */
val HaEntityState.brightnessPercent: Int?
    get() = (attribute("brightness") as? JsonPrimitive)?.intOrNull?.let { (it * 100 + 127) / 255 }

val HaEntityState.currentTemperature: Double?
    get() = (attribute("current_temperature") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.targetTemperature: Double?
    get() = (attribute("temperature") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.temperatureUnit: String
    get() = (attribute("unit_of_measurement") as? JsonPrimitive)?.contentOrNull ?: "°"

val HaEntityState.mediaTitle: String?
    get() = (attribute("media_title") as? JsonPrimitive)?.contentOrNull

val HaEntityState.mediaArtist: String?
    get() = (attribute("media_artist") as? JsonPrimitive)?.contentOrNull
