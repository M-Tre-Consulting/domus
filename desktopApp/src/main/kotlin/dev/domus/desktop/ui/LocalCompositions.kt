package dev.domus.desktop.ui

import androidx.compose.runtime.compositionLocalOf

/** How often detail screens poll for fresh entity state, in seconds. */
val LocalRefreshIntervalSeconds = compositionLocalOf { 10 }
