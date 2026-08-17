package dev.domus.android.data

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dev.domus.android.R
import dev.domus.android.ShortcutToggleActivity
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.friendlyName

private val SHORTCUT_ELIGIBLE_DOMAINS = setOf("light", "switch", "fan", "input_boolean", "siren")

/** Publishes long-press launcher shortcuts for the user's favorite toggleable entities. */
object DomusShortcuts {
    fun update(context: Context, favorites: List<HaEntityState>) {
        val maxShortcuts = ShortcutManagerCompat.getMaxShortcutCountPerActivity(context)
            .takeIf { it > 0 } ?: 4
        val icon = IconCompat.createWithResource(context, R.mipmap.ic_launcher)
        val shortcuts = favorites
            .filter { it.domain in SHORTCUT_ELIGIBLE_DOMAINS }
            .take(maxShortcuts)
            .map { entity ->
                val intent = Intent(context, ShortcutToggleActivity::class.java).apply {
                    action = Intent.ACTION_VIEW
                    putExtra(ShortcutToggleActivity.EXTRA_ENTITY_ID, entity.entityId)
                    putExtra(ShortcutToggleActivity.EXTRA_LABEL, entity.friendlyName)
                }
                ShortcutInfoCompat.Builder(context, "toggle_${entity.entityId}")
                    .setShortLabel(entity.friendlyName)
                    .setLongLabel(entity.friendlyName)
                    .setIcon(icon)
                    .setIntent(intent)
                    .build()
            }
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcuts)
    }
}
