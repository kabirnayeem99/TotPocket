package io.github.kabirnayeem99.totpocket.navigation

import androidx.compose.runtime.Immutable
import io.github.kabirnayeem99.totpocket.calls.CallApp

/** Every place a child (or parent) can be. Encoded to a string so the back stack survives process death. */
@Immutable
sealed interface Route {
    data object Home : Route

    sealed interface Calls : Route {
        data class Contacts(val app: CallApp) : Calls
        data object Keypad : Calls
        data class Active(val contactId: String, val app: CallApp) : Calls
    }

    sealed interface Gallery : Route {
        data object Categories : Gallery
        data class Grid(val categoryId: String) : Gallery
    }

    /** The pretend YouTube: animal "videos". */
    data object YouTube : Route

    sealed interface Games : Route {
        data object Picker : Games
        data object ShapeMatch : Games
    }

    sealed interface Parent : Route {
        data object Gate : Parent
        data object Settings : Parent
    }

    companion object {
        fun encode(route: Route): String = when (route) {
            Home -> "home"
            is Calls.Contacts -> "calls/${route.app.id}"
            Calls.Keypad -> "calls/keypad"
            is Calls.Active -> "calls/active/${route.app.id}/${route.contactId}"
            Gallery.Categories -> "gallery"
            is Gallery.Grid -> "gallery/grid/${route.categoryId}"
            YouTube -> "youtube"
            Games.Picker -> "games"
            Games.ShapeMatch -> "games/shape-match"
            Parent.Gate -> "parent/gate"
            Parent.Settings -> "parent/settings"
        }

        fun decode(value: String): Route = when {
            value == "calls/keypad" -> Calls.Keypad
            value.startsWith("calls/active/") -> value.removePrefix("calls/active/").split("/", limit = 2).let {
                Calls.Active(contactId = it.getOrElse(1) { "" }, app = CallApp.fromId(it[0]))
            }
            value.startsWith("calls/") -> Calls.Contacts(CallApp.fromId(value.removePrefix("calls/")))
            value == "gallery" -> Gallery.Categories
            value.startsWith("gallery/grid/") -> Gallery.Grid(value.removePrefix("gallery/grid/"))
            value == "youtube" -> YouTube
            value == "games" -> Games.Picker
            value == "games/shape-match" -> Games.ShapeMatch
            value == "parent/gate" -> Parent.Gate
            value == "parent/settings" -> Parent.Settings
            else -> Home
        }
    }
}
