package dev.domus.desktop

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import dev.domus.shared.DesignTokens

private val SeedColor = Color(DesignTokens.SEED_COLOR_ARGB)

/**
 * Desktop has no wallpaper-derived dynamic color, so the scheme is seeded directly from
 * [DesignTokens.SEED_COLOR_ARGB] — the same seed the Android build falls back to — to
 * keep the two UIs visually consistent.
 */
@Composable
fun DomusDesktopTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colorScheme = if (darkTheme) {
        darkColorScheme(primary = SeedColor)
    } else {
        lightColorScheme(primary = SeedColor)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
