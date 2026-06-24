package dev.domus.android.data

import dev.domus.shared.data.HaSession

/**
 * In-memory holder for the active [HaSession], shared between [ConnectScreen] and
 * [DashboardScreen] via navigation. Does not survive process death — token persistence
 * (so re-launching the app skips the connect screen) is still TODO.
 */
object HaSessionHolder {
    var session: HaSession? = null
}
