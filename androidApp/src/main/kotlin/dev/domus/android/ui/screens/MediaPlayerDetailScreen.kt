package dev.domus.android.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeOff
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Repeat
import androidx.compose.material.icons.filled.RepeatOne
import androidx.compose.material.icons.filled.Shuffle
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.android.ui.components.StateHistorySection
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.friendlyName
import dev.domus.shared.model.isVolumeMuted
import dev.domus.shared.model.isShuffle
import dev.domus.shared.model.mediaAlbum
import dev.domus.shared.model.mediaArtist
import dev.domus.shared.model.mediaDuration
import dev.domus.shared.model.mediaPosition
import dev.domus.shared.model.mediaPositionUpdatedAt
import dev.domus.shared.model.mediaSource
import dev.domus.shared.model.mediaTitle
import dev.domus.shared.model.repeatMode
import dev.domus.shared.model.sourceList
import dev.domus.shared.model.volumeLevel
import androidx.compose.runtime.rememberCoroutineScope
import dev.domus.android.ui.LocalRefreshIntervalSeconds
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.time.Clock
import kotlin.time.Instant
import kotlinx.serialization.json.JsonPrimitive
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MediaPlayerDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }
    val haptic = LocalHapticFeedback.current
    val refreshInterval = LocalRefreshIntervalSeconds.current

    LaunchedEffect(entityId, refreshInterval) {
        while (true) {
            delay(refreshInterval.toLong() * 1000L)
            try { session.repository.refreshEntities(setOf(entityId)) } catch (_: Exception) {}
        }
    }

    fun callService(call: HaServiceCall) {
        haptic.performHapticFeedback(HapticFeedbackType.LongPress)
        scope.launch {
            try { session.repository.callService(call) } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't update: ${e.message}")
            }
        }
    }

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
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            return@Scaffold
        }

        val isPlaying = entity.state.equals("playing", ignoreCase = true)
        val duration = entity.mediaDuration
        val hasDuration = duration != null && duration > 0.0

        // Real-time position estimate for playing state
        var displayPositionSec by remember { mutableDoubleStateOf(entity.mediaPosition ?: 0.0) }
        LaunchedEffect(entity.entityId, entity.state, entity.mediaPosition, entity.mediaPositionUpdatedAt) {
            val pos = entity.mediaPosition ?: return@LaunchedEffect
            if (!isPlaying) {
                displayPositionSec = pos
                return@LaunchedEffect
            }
            val updatedAt = try {
                entity.mediaPositionUpdatedAt?.let { Instant.parse(it) } ?: return@LaunchedEffect
            } catch (_: Exception) { return@LaunchedEffect }
            while (true) {
                val elapsed = (Clock.System.now() - updatedAt).inWholeSeconds.toDouble()
                displayPositionSec = (pos + elapsed).coerceIn(0.0, duration ?: Double.MAX_VALUE)
                delay(1_000.milliseconds)
            }
        }

        // Local volume for smooth slider feel
        var displayVolume by remember { mutableFloatStateOf(entity.volumeLevel ?: 0.5f) }
        LaunchedEffect(entity.volumeLevel) {
            displayVolume = entity.volumeLevel ?: 0.5f
        }

        // Local seek position while dragging
        var seekFraction by remember { mutableFloatStateOf(0f) }
        var isSeeking by remember { mutableStateOf(false) }
        val positionFraction = if (hasDuration) {
            if (isSeeking) seekFraction else (displayPositionSec / duration).toFloat().coerceIn(0f, 1f)
        } else 0f

        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = DesignTokens.Spacing.lg.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))

            // Album art placeholder
            Surface(
                shape = RoundedCornerShape(DesignTokens.Shape.cornerLarge.dp),
                color = MaterialTheme.colorScheme.primaryContainer,
                modifier = Modifier.size(180.dp),
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        imageVector = Icons.Filled.MusicNote,
                        contentDescription = null,
                        modifier = Modifier.size(72.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

            val title = entity.mediaTitle
            val artist = entity.mediaArtist
            val album = entity.mediaAlbum

            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            val subtitle = listOfNotNull(artist, album).joinToString(" · ")
            if (subtitle.isNotEmpty()) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }

            // State pill
            if (entity.state !in setOf("unavailable", "unknown")) {
                Surface(
                    shape = CircleShape,
                    color = if (isPlaying) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.padding(top = DesignTokens.Spacing.sm.dp),
                ) {
                    Text(
                        text = entity.state.toDisplayLabel(),
                        style = MaterialTheme.typography.labelSmall,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                        color = if (isPlaying) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

            // Seek bar
            if (hasDuration) {
                Slider(
                    value = positionFraction,
                    onValueChange = { frac ->
                        isSeeking = true
                        seekFraction = frac
                    },
                    onValueChangeFinished = {
                        val seekTo = seekFraction * duration
                        displayPositionSec = seekTo
                        isSeeking = false
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "media_seek",
                                entityId = entity.entityId,
                                data = mapOf("seek_position" to JsonPrimitive(seekTo)),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Text(formatMediaDuration(displayPositionSec), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatMediaDuration(duration), style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
            }

            // Transport controls
            Row(
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.md.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                FilledTonalIconButton(
                    onClick = {
                        callService(HaServiceCall("media_player", "media_previous_track", entity.entityId))
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.SkipPrevious, contentDescription = "Previous", modifier = Modifier.size(28.dp))
                }
                FilledIconButton(
                    onClick = {
                        callService(HaServiceCall("media_player", "media_play_pause", entity.entityId))
                    },
                    modifier = Modifier.size(72.dp),
                ) {
                    Icon(
                        imageVector = if (isPlaying) Icons.Filled.Pause else Icons.Filled.PlayArrow,
                        contentDescription = if (isPlaying) "Pause" else "Play",
                        modifier = Modifier.size(40.dp),
                    )
                }
                FilledTonalIconButton(
                    onClick = {
                        callService(HaServiceCall("media_player", "media_next_track", entity.entityId))
                    },
                    modifier = Modifier.size(48.dp),
                ) {
                    Icon(Icons.Filled.SkipNext, contentDescription = "Next", modifier = Modifier.size(28.dp))
                }
            }

            Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

            // Volume
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(
                    onClick = {
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "volume_mute",
                                entityId = entity.entityId,
                                data = mapOf("is_volume_muted" to JsonPrimitive(!entity.isVolumeMuted)),
                            ),
                        )
                    },
                ) {
                    Icon(
                        imageVector = if (entity.isVolumeMuted) Icons.AutoMirrored.Filled.VolumeOff else Icons.AutoMirrored.Filled.VolumeUp,
                        contentDescription = if (entity.isVolumeMuted) "Unmute" else "Mute",
                    )
                }
                Slider(
                    value = displayVolume,
                    onValueChange = { displayVolume = it },
                    onValueChangeFinished = {
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "volume_set",
                                entityId = entity.entityId,
                                data = mapOf("volume_level" to JsonPrimitive(displayVolume.toDouble())),
                            ),
                        )
                    },
                    modifier = Modifier.weight(1f),
                )
                Spacer(Modifier.width(DesignTokens.Spacing.sm.dp))
                Text(
                    text = "${(displayVolume * 100).toInt()}%",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.width(36.dp),
                )
            }

            // Source selector
            val sources = entity.sourceList
            if (sources.isNotEmpty()) {
                Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
                SourceSelector(
                    currentSource = entity.mediaSource,
                    sources = sources,
                    onSelect = { source ->
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "select_source",
                                entityId = entity.entityId,
                                data = mapOf("source" to JsonPrimitive(source)),
                            ),
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                )
            }

            Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))

            // Shuffle and Repeat
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp),
            ) {
                val shuffleActive = entity.isShuffle
                IconButton(
                    onClick = {
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "shuffle_set",
                                entityId = entity.entityId,
                                data = mapOf("shuffle" to JsonPrimitive(!shuffleActive)),
                            ),
                        )
                    },
                    colors = if (shuffleActive) IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
                ) {
                    Icon(Icons.Filled.Shuffle, contentDescription = "Shuffle")
                }

                val repeat = entity.repeatMode
                IconButton(
                    onClick = {
                        val next = when (repeat) { "off" -> "all"; "all" -> "one"; else -> "off" }
                        callService(
                            HaServiceCall(
                                domain = "media_player",
                                service = "repeat_set",
                                entityId = entity.entityId,
                                data = mapOf("repeat" to JsonPrimitive(next)),
                            ),
                        )
                    },
                    colors = if (repeat != "off") IconButtonDefaults.filledTonalIconButtonColors() else IconButtonDefaults.iconButtonColors(),
                ) {
                    Icon(
                        imageVector = if (repeat == "one") Icons.Filled.RepeatOne else Icons.Filled.Repeat,
                        contentDescription = "Repeat: $repeat",
                    )
                }
            }

            HorizontalDivider(modifier = Modifier.padding(vertical = DesignTokens.Spacing.lg.dp))

            StateHistorySection(session = session, entityId = entityId, entityName = entity.friendlyName)

            Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
        }
    }
}

@Composable
private fun SourceSelector(
    currentSource: String?,
    sources: List<String>,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    var expanded by remember { mutableStateOf(false) }
    Box(modifier = modifier) {
        Surface(
            shape = RoundedCornerShape(DesignTokens.Shape.cornerLarge.dp),
            color = MaterialTheme.colorScheme.surfaceVariant,
            modifier = Modifier.fillMaxWidth().clickable { expanded = true },
        ) {
            Row(
                modifier = Modifier.padding(horizontal = DesignTokens.Spacing.md.dp, vertical = DesignTokens.Spacing.sm.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Column {
                    Text("Source", style = MaterialTheme.typography.labelSmall)
                    Text(currentSource ?: "—", style = MaterialTheme.typography.bodyMedium)
                }
                Text("▾", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        DropdownMenu(expanded = expanded, onDismissRequest = { expanded = false }) {
            sources.forEach { source ->
                DropdownMenuItem(
                    text = { Text(source) },
                    onClick = { expanded = false; onSelect(source) },
                )
            }
        }
    }
}

private fun formatMediaDuration(seconds: Double): String {
    val total = seconds.toLong().coerceAtLeast(0L)
    val min = total / 60
    val sec = total % 60
    return "$min:%02d".format(sec)
}
