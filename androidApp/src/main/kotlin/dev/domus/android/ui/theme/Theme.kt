package dev.domus.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import dev.domus.shared.DesignTokens

/**
 * Material 3 Expressive theme. Min SDK is 31, so the system's dynamic color scheme
 * (wallpaper-derived) is always available; [DesignTokens.SEED_COLOR_ARGB] is the
 * fallback seed used to keep the Desktop build visually consistent.
 */
@Composable
fun DomusTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val context = LocalContext.current
    val colorScheme = if (darkTheme) {
        dynamicDarkColorScheme(context)
    } else {
        dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
