package dev.domus.android.ui

import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.runtime.compositionLocalOf

/**
 * Process-scoped shared transition scope, provided by the single [SharedTransitionLayout] that
 * wraps the NavHost in [MainActivity]. Null outside that layout (e.g. previews).
 */
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope?> { null }

/**
 * Per-destination animated visibility scope; updated by CompositionLocalProvider inside each
 * composable {} block that participates in shared element transitions.
 */
val LocalAnimatedVisibilityScope = compositionLocalOf<AnimatedVisibilityScope?> { null }

/** How often detail screens poll for fresh entity state, in seconds. */
val LocalRefreshIntervalSeconds = compositionLocalOf { 10 }
