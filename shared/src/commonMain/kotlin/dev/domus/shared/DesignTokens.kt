package dev.domus.shared

/**
 * Platform-agnostic Material 3 Expressive design tokens. Android and Desktop both
 * build their `ColorScheme`/`Typography` from these values so the two UIs stay
 * visually consistent without sharing Compose code directly.
 */
object DesignTokens {
    // Seed color for Material 3 dynamic/expressive color scheme generation.
    const val SEED_COLOR_ARGB = 0xFF3D5AFE.toInt()

    object Spacing {
        const val xs = 4
        const val sm = 8
        const val md = 16
        const val lg = 24
        const val xl = 32
    }

    object Shape {
        const val cornerSmall = 12
        const val cornerMedium = 20
        const val cornerLarge = 28
    }
}
