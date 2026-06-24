package dev.domus.android.ui.screens

/** Turns a raw HA state/attribute value like "heat_cool" into "Heat cool" for display. */
fun String.toDisplayLabel(): String = replace('_', ' ').replaceFirstChar { it.uppercase() }
