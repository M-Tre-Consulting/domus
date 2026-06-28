package dev.domus.desktop.ui.screens

fun String.toDisplayLabel(): String = replace('_', ' ').replaceFirstChar { it.uppercase() }
