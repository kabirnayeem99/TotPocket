package io.github.kabirnayeem99.totpocket.navigation

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.listSaver

@Immutable
data class BackStackEntry(val id: String, val route: Route)

/**
 * TotPocket's back stack. Always starts at — and can never pop past — [Route.Home], so Back can
 * never leave the app. [onEntryRemoved] lets the host release per-entry ViewModels and saved state.
 */
@Stable
class Navigator internal constructor(
    initial: List<BackStackEntry>,
    private var nextId: Long,
) {
    constructor() : this(listOf(BackStackEntry("0", Route.Home)), nextId = 1)

    private val entries = mutableStateListOf<BackStackEntry>().apply { addAll(initial) }

    var onEntryRemoved: (BackStackEntry) -> Unit = {}

    val backStack: List<BackStackEntry> get() = entries

    val current: BackStackEntry get() = entries.last()

    fun push(route: Route) {
        if (route == current.route) return
        entries.add(newEntry(route))
    }

    /** Swaps the current entry for [route] — e.g. the parent gate for the settings it protects. */
    fun replace(route: Route) {
        if (entries.size == 1) {
            push(route)
            return
        }
        val removed = entries.removeAt(entries.lastIndex)
        entries.add(newEntry(route))
        onEntryRemoved(removed)
    }

    /** Returns `false` (and does nothing) at Home. */
    fun pop(): Boolean {
        if (entries.size == 1) return false
        onEntryRemoved(entries.removeAt(entries.lastIndex))
        return true
    }

    fun popToHome() {
        while (pop()) Unit
    }

    private fun newEntry(route: Route) = BackStackEntry((nextId++).toString(), route)

    companion object {
        val Saver: Saver<Navigator, Any> = listSaver(
            save = { navigator ->
                buildList {
                    add(navigator.nextId.toString())
                    navigator.entries.forEach { add("${it.id}|${Route.encode(it.route)}") }
                }
            },
            restore = { saved ->
                val entries = saved.drop(1).map {
                    val (id, route) = it.split("|", limit = 2)
                    BackStackEntry(id, Route.decode(route))
                }
                Navigator(entries.ifEmpty { listOf(BackStackEntry("0", Route.Home)) }, saved.first().toLong())
            },
        )
    }
}
