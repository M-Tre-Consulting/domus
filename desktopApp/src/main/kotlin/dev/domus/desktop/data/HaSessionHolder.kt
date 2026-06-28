package dev.domus.desktop.data

import dev.domus.shared.data.HaSession
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/**
 * In-memory holder for the active [HaSession], shared across screens via the navigation stack.
 * Does not survive process death. Starting refresh + WebSocket here (not in individual screens)
 * keeps the connection alive while navigating between detail views.
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
