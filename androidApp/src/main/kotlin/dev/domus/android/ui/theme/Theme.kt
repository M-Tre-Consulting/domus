package dev.domus.android.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import com.materialkolor.rememberDynamicColorScheme

/**
 * Material 3 Expressive theme. Min SDK is 31, so the system's dynamic color scheme
 * (wallpaper-derived) is always available as the default.
 *
 * When [seedColorArgb] is non-zero, a full M3 tonal palette is generated from that seed
 * using material-kolor instead of the wallpaper palette.
 *
 * [themeMode] overrides the system dark/light preference when set to "light" or "dark".
 */
@Composable
fun DomusTheme(
    themeMode: String = "system",
    seedColorArgb: Int = 0,
    content: @Composable () -> Unit,
) {
    val isDark = when (themeMode) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }

    val context = LocalContext.current
    val colorScheme = if (seedColorArgb != 0) {
        rememberDynamicColorScheme(Color(seedColorArgb), isDark, isAmoled = false)
    } else {
        if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}
