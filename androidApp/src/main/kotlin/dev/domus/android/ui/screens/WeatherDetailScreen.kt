package dev.domus.android.ui.screens

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.R
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.weatherAttribution
import dev.domus.shared.model.weatherHumidity
import dev.domus.shared.model.weatherPressure
import dev.domus.shared.model.weatherPressureUnit
import dev.domus.shared.model.weatherTemperature
import dev.domus.shared.model.weatherTemperatureUnit
import dev.domus.shared.model.weatherWindSpeed
import dev.domus.shared.model.weatherWindSpeedUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WeatherDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = entity?.friendlyName ?: entityId,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.common_back))
                    }
                },
            )
        },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val humidityLabel = stringResource(R.string.climate_humidity)
        val windLabel = stringResource(R.string.weather_wind)
        val pressureLabel = stringResource(R.string.weather_pressure)
        val rows = buildList {
            entity.weatherHumidity?.let { add(humidityLabel to "${it.toInt()}%") }
            entity.weatherWindSpeed?.let { add(windLabel to "%.1f %s".format(it, entity.weatherWindSpeedUnit.orEmpty())) }
            entity.weatherPressure?.let { add(pressureLabel to "%.0f %s".format(it, entity.weatherPressureUnit.orEmpty())) }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))

            Surface(
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(96.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = iconForWeatherCondition(entity.state),
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(56.dp),
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.md.dp))

            entity.weatherTemperature?.let { temp ->
                Text(
                    text = "%.1f%s".format(temp, entity.weatherTemperatureUnit ?: "°"),
                    style = MaterialTheme.typography.displaySmall,
                )
            }
            Text(
                text = entity.state.toDisplayLabel(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = DesignTokens.Spacing.xs.dp),
            )

            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
            InfoCard(rows = rows)

            entity.weatherAttribution?.let { attribution ->
                Text(
                    text = attribution,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.md.dp),
                )
            }
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
        }
    }
}
