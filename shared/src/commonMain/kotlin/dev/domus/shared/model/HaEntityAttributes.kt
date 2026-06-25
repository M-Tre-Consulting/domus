package dev.domus.shared.model

import kotlinx.serialization.json.JsonArray
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

/** Fan/cover/valve position or speed as a 0-100 percentage attribute. */
val HaEntityState.percentage: Int?
    get() = (attribute("percentage") as? JsonPrimitive)?.intOrNull

val HaEntityState.currentPosition: Int?
    get() = (attribute("current_position") as? JsonPrimitive)?.intOrNull

/** Numeric value for `number`/`input_number` entities; the state itself is the value. */
val HaEntityState.numericValue: Double?
    get() = JsonPrimitive(state).doubleOrNull

val HaEntityState.minValue: Double?
    get() = (attribute("min") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.maxValue: Double?
    get() = (attribute("max") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.step: Double
    get() = (attribute("step") as? JsonPrimitive)?.doubleOrNull ?: 1.0

/** Options list for `select`/`input_select` entities. */
val HaEntityState.options: List<String>
    get() = (attribute("options") as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?: emptyList()

private fun HaEntityState.stringList(attributeName: String): List<String> =
    (attribute(attributeName) as? JsonArray)
        ?.mapNotNull { (it as? JsonPrimitive)?.contentOrNull }
        ?: emptyList()

/** For `climate`/`water_heater`: the entity state itself is the current HVAC/operation mode. */
val HaEntityState.hvacMode: String get() = state

val HaEntityState.hvacModes: List<String> get() = stringList("hvac_modes")

/** Informational only (idle/heating/cooling/drying...); not settable. */
val HaEntityState.hvacAction: String?
    get() = (attribute("hvac_action") as? JsonPrimitive)?.contentOrNull

val HaEntityState.fanMode: String?
    get() = (attribute("fan_mode") as? JsonPrimitive)?.contentOrNull

val HaEntityState.fanModes: List<String> get() = stringList("fan_modes")

val HaEntityState.swingMode: String?
    get() = (attribute("swing_mode") as? JsonPrimitive)?.contentOrNull

val HaEntityState.swingModes: List<String> get() = stringList("swing_modes")

val HaEntityState.currentHumidity: Double?
    get() = (attribute("current_humidity") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.targetHumidity: Double?
    get() = (attribute("humidity") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.minTemp: Double
    get() = (attribute("min_temp") as? JsonPrimitive)?.doubleOrNull ?: 7.0

val HaEntityState.maxTemp: Double
    get() = (attribute("max_temp") as? JsonPrimitive)?.doubleOrNull ?: 35.0

val HaEntityState.targetTempStep: Double
    get() = (attribute("target_temp_step") as? JsonPrimitive)?.doubleOrNull ?: 0.5

// Light color and color temperature
val HaEntityState.colorMode: String?
    get() = (attribute("color_mode") as? JsonPrimitive)?.contentOrNull

val HaEntityState.supportedColorModes: List<String>
    get() = stringList("supported_color_modes")

val HaEntityState.colorTempKelvin: Int?
    get() = (attribute("color_temp_kelvin") as? JsonPrimitive)?.intOrNull

val HaEntityState.minColorTempKelvin: Int?
    get() = (attribute("min_color_temp_kelvin") as? JsonPrimitive)?.intOrNull

val HaEntityState.maxColorTempKelvin: Int?
    get() = (attribute("max_color_temp_kelvin") as? JsonPrimitive)?.intOrNull

/** HS color as (hue 0–360, saturation 0–100); null if light doesn't report hs_color. */
val HaEntityState.hueColor: Pair<Float, Float>?
    get() {
        val arr = attribute("hs_color") as? JsonArray ?: return null
        val h = (arr.getOrNull(0) as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: return null
        val s = (arr.getOrNull(1) as? JsonPrimitive)?.doubleOrNull?.toFloat() ?: return null
        return h to s
    }

// Power monitoring (smart plug / switch entities with energy metering)
val HaEntityState.currentPowerW: Double?
    get() = (attribute("current_power_w") as? JsonPrimitive)?.doubleOrNull
        ?: (attribute("power") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.voltageV: Double?
    get() = (attribute("voltage") as? JsonPrimitive)?.doubleOrNull

/** Current in milli-Amperes (some integrations report Amps directly in "current"). */
val HaEntityState.currentMa: Double?
    get() = (attribute("current_ma") as? JsonPrimitive)?.doubleOrNull
        ?: (attribute("current") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.todayEnergyKwh: Double?
    get() = (attribute("today_energy_kwh") as? JsonPrimitive)?.doubleOrNull
        ?: (attribute("energy") as? JsonPrimitive)?.doubleOrNull

val HaEntityState.deviceClass: String?
    get() = (attribute("device_class") as? JsonPrimitive)?.contentOrNull

val HaEntityState.unitOfMeasurement: String?
    get() = (attribute("unit_of_measurement") as? JsonPrimitive)?.contentOrNull
