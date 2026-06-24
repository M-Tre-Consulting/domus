package dev.domus.android.data

import dev.domus.shared.data.HaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * In-memory holder for the active [HaSession], shared across screens via navigation. Does
 * not survive process death.
 *
 * The initial state refresh and the WebSocket realtime subscription are started here, in a
 * process-lifetime scope, instead of inside any one screen's `LaunchedEffect` — a screen
 * (e.g. the dashboard) gets disposed and its coroutines cancelled as soon as you navigate
 * away from it, which previously killed the WebSocket connection while viewing, say, the
 * climate detail screen, so live updates silently stopped until you navigated back.
 */
object HaSessionHolder {
    var session: HaSession? = null
        private set

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var realtimeJob: Job? = null

    fun connect(newSession: HaSession) {
        realtimeJob?.cancel()
        session = newSession
        realtimeJob = scope.launch {
            runCatching { newSession.repository.refresh() }
            newSession.repository.startRealtimeUpdates(this)
        }
    }

    fun disconnect() {
        realtimeJob?.cancel()
        realtimeJob = null
        session = null
    }
}
