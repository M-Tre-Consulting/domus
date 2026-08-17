package dev.domus.android.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.GlanceTheme
import androidx.glance.action.actionParametersOf
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.action.actionRunCallback
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import dev.domus.android.R
import dev.domus.android.data.ConnectionStore
import dev.domus.android.data.FavoritesStore
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaConnectionConfig
import dev.domus.shared.model.HaEntityState
import dev.domus.shared.model.friendlyName
import kotlinx.coroutines.flow.first

private val WIDGET_ELIGIBLE_DOMAINS = setOf("light", "switch", "fan", "input_boolean", "siren")
private const val MAX_WIDGET_ROWS = 5

/** Home-screen widget listing favorite toggleable entities. Tapping a row toggles it. */
class DomusWidget : GlanceAppWidget() {
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val entities = loadFavoriteEntities(context)
        val strings = WidgetStrings(
            title = context.getString(R.string.app_name),
            empty = context.getString(R.string.widget_empty),
            on = context.getString(R.string.widget_on),
            off = context.getString(R.string.widget_off),
        )
        provideContent {
            GlanceTheme {
                WidgetContent(entities, strings)
            }
        }
    }

    private suspend fun loadFavoriteEntities(context: Context): List<HaEntityState> {
        val favoriteIds = FavoritesStore(context).favoriteEntityIds.first()
        if (favoriteIds.isEmpty()) return emptyList()
        val connectionStore = ConnectionStore(context)
        val config = connectionStore.read() ?: return emptyList()
        val session = HaSession(config) { refreshed ->
            connectionStore.save(HaConnectionConfig(config.baseUrl, refreshed))
        }
        return runCatching {
            session.restApi.getStates()
                .filter { it.entityId in favoriteIds && it.domain in WIDGET_ELIGIBLE_DOMAINS }
                .sortedBy { it.friendlyName }
                .take(MAX_WIDGET_ROWS)
        }.getOrElse { emptyList() }
    }

    companion object {
        suspend fun updateAll(context: Context) {
            val widget = DomusWidget()
            GlanceAppWidgetManager(context).getGlanceIds(DomusWidget::class.java).forEach { id ->
                widget.update(context, id)
            }
        }
    }
}

private data class WidgetStrings(val title: String, val empty: String, val on: String, val off: String)

@Composable
private fun WidgetContent(entities: List<HaEntityState>, strings: WidgetStrings) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(GlanceTheme.colors.background)
            .padding(12.dp),
    ) {
        Text(
            text = strings.title,
            style = TextStyle(fontWeight = FontWeight.Bold, color = GlanceTheme.colors.onBackground),
        )
        if (entities.isEmpty()) {
            Text(
                text = strings.empty,
                style = TextStyle(color = GlanceTheme.colors.onBackground),
                modifier = GlanceModifier.padding(top = 8.dp),
            )
        } else {
            entities.forEach { entity ->
                Row(
                    modifier = GlanceModifier
                        .fillMaxWidth()
                        .padding(top = 8.dp)
                        .clickable(
                            actionRunCallback<ToggleEntityAction>(
                                actionParametersOf(entityIdParamKey to entity.entityId),
                            ),
                        ),
                    verticalAlignment = Alignment.Vertical.CenterVertically,
                ) {
                    val isOn = entity.state.equals("on", ignoreCase = true)
                    Text(
                        text = entity.friendlyName,
                        style = TextStyle(
                            color = if (isOn) GlanceTheme.colors.primary else GlanceTheme.colors.onBackground,
                        ),
                        modifier = GlanceModifier.defaultWeight(),
                    )
                    Text(
                        text = if (isOn) strings.on else strings.off,
                        style = TextStyle(color = GlanceTheme.colors.onBackground),
                    )
                }
            }
        }
    }
}
