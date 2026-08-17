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

    /** Material 3 Expressive-style shape scale: bigger jumps between steps than the classic
     *  scale, used intentionally rather than one radius everywhere, for visual rhythm. */
    object Shape {
        const val cornerExtraSmall = 8
        const val cornerSmall = 12
        const val cornerMedium = 20
        const val cornerLarge = 28
        const val cornerExtraLarge = 36
    }
}
