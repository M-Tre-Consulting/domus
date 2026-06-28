package dev.domus.desktop.ui.screens

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import dev.domus.desktop.ui.components.StateHistorySection
import dev.domus.shared.DesignTokens
import dev.domus.shared.data.HaSession
import dev.domus.shared.model.HaServiceCall
import dev.domus.shared.model.changedBy
import dev.domus.shared.model.friendlyName
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LockDetailScreen(session: HaSession, entityId: String, onBack: () -> Unit) {
    val entities by session.repository.entities.collectAsState()
    val entity = entities[entityId]
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    fun callService(service: String) {
        scope.launch {
            try {
                session.repository.callService(HaServiceCall(domain = "lock", service = service, entityId = entityId))
            } catch (e: Exception) {
                snackbarHostState.showSnackbar("Couldn't update: ${e.message}")
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(entity?.friendlyName ?: entityId, maxLines = 1, overflow = TextOverflow.Ellipsis) },
                navigationIcon = {
                    IconButton(onClick = onBack) { Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back") }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { padding ->
        if (entity == null) {
            Box(Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) { CircularProgressIndicator() }
            return@Scaffold
        }

        val state = entity.state.lowercase()
        val isLocked = state == "locked"
        val isUnlocked = state == "unlocked"
        val isTransitioning = state == "locking" || state == "unlocking"

        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.TopCenter) {
            Column(
                modifier = Modifier
                    .widthIn(max = 640.dp)
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = DesignTokens.Spacing.lg.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))

                AnimatedLockButton(
                    isLocked = isLocked,
                    isTransitioning = isTransitioning,
                    onClick = {
                        when {
                            isLocked -> callService("unlock")
                            isUnlocked -> callService("lock")
                        }
                    },
                )

                Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))

                Text(
                    text = when (state) {
                        "locked" -> "Locked"; "unlocked" -> "Unlocked"
                        "locking" -> "Locking…"; "unlocking" -> "Unlocking…"
                        "jammed" -> "Jammed"
                        else -> state.replaceFirstChar { it.uppercase() }
                    },
                    style = MaterialTheme.typography.headlineSmall,
                )

                entity.changedBy?.takeIf { it.isNotBlank() }?.let {
                    Text("by $it", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(top = 4.dp))
                }

                if (isTransitioning) {
                    Spacer(Modifier.height(DesignTokens.Spacing.sm.dp))
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(DesignTokens.Spacing.sm.dp)) {
                        CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp)
                        Text(if (state == "locking") "Securing…" else "Opening…", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
                HorizontalDivider()
                Spacer(Modifier.height(DesignTokens.Spacing.lg.dp))
                StateHistorySection(session = session, entityId = entityId, entityName = entity.friendlyName, modifier = Modifier.fillMaxWidth())
                Spacer(Modifier.height(DesignTokens.Spacing.xl.dp))
            }
        }
    }
}

@Composable
private fun AnimatedLockButton(isLocked: Boolean, isTransitioning: Boolean, onClick: () -> Unit) {
    val shackleProgress by animateFloatAsState(
        targetValue = if (isLocked) 0f else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "shackle",
    )
    val lockColor by animateColorAsState(
        targetValue = when { isTransitioning -> MaterialTheme.colorScheme.outline; isLocked -> MaterialTheme.colorScheme.primary; else -> MaterialTheme.colorScheme.error },
        animationSpec = tween(400), label = "lockColor",
    )
    val bgColor by animateColorAsState(
        targetValue = when { isTransitioning -> MaterialTheme.colorScheme.surfaceVariant; isLocked -> MaterialTheme.colorScheme.primaryContainer; else -> MaterialTheme.colorScheme.errorContainer },
        animationSpec = tween(400), label = "bgColor",
    )

    Surface(onClick = onClick, modifier = Modifier.size(180.dp), shape = CircleShape, color = bgColor, enabled = !isTransitioning) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val bodyW = size.width * 0.48f; val bodyH = size.height * 0.38f
            val bodyL = (size.width - bodyW) / 2f; val bodyT = size.height * 0.50f
            val sw = bodyW * 0.52f; val sl = (size.width - sw) / 2f; val archH = sw * 0.85f
            val maxRaise = bodyT * 0.32f
            val shackleT = bodyT - archH - maxRaise * (1f - shackleProgress)
            val strokeW = 6.dp.toPx()

            drawArc(color = lockColor, startAngle = 180f, sweepAngle = 180f, useCenter = false,
                topLeft = Offset(sl, shackleT), size = Size(sw, archH), style = Stroke(width = strokeW, cap = StrokeCap.Round))
            drawLine(color = lockColor, start = Offset(sl, shackleT + archH * 0.5f), end = Offset(sl, bodyT + bodyH * 0.1f), strokeWidth = strokeW, cap = StrokeCap.Round)
            val rightEnd = bodyT + bodyH * 0.1f - shackleProgress * (bodyH * 0.45f)
            drawLine(color = lockColor, start = Offset(sl + sw, shackleT + archH * 0.5f), end = Offset(sl + sw, rightEnd), strokeWidth = strokeW, cap = StrokeCap.Round)
            drawRoundRect(color = lockColor, topLeft = Offset(bodyL, bodyT), size = Size(bodyW, bodyH), cornerRadius = CornerRadius(8.dp.toPx()))
            val khX = size.width / 2f; val khY = bodyT + bodyH * 0.38f
            drawCircle(bgColor, radius = 6.dp.toPx(), center = Offset(khX, khY))
            drawRoundRect(color = bgColor, topLeft = Offset(khX - 3.dp.toPx(), khY + 3.dp.toPx()), size = Size(6.dp.toPx(), 9.dp.toPx()), cornerRadius = CornerRadius(3.dp.toPx()))
        }
    }
}
