package io.github.kabirnayeem99.totpocket.navigation

import androidx.compose.runtime.Immutable

/** Every place a child (or parent) can be. Encoded to a string so the back stack survives process death. */
@Immutable
sealed interface Route {
    data object Home : Route

    sealed interface Calls : Route {
        data object Contacts : Calls
        data object Keypad : Calls
        data class Active(val contactId: String) : Calls
    }

    sealed interface Gallery : Route {
        data object Categories : Gallery
        data class Grid(val categoryId: String) : Gallery
    }

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
            Calls.Contacts -> "calls"
            Calls.Keypad -> "calls/keypad"
            is Calls.Active -> "calls/active/${route.contactId}"
            Gallery.Categories -> "gallery"
            is Gallery.Grid -> "gallery/grid/${route.categoryId}"
            Games.Picker -> "games"
            Games.ShapeMatch -> "games/shape-match"
            Parent.Gate -> "parent/gate"
            Parent.Settings -> "parent/settings"
        }

        fun decode(value: String): Route = when {
            value == "calls" -> Calls.Contacts
            value == "calls/keypad" -> Calls.Keypad
            value.startsWith("calls/active/") -> Calls.Active(value.removePrefix("calls/active/"))
            value == "gallery" -> Gallery.Categories
            value.startsWith("gallery/grid/") -> Gallery.Grid(value.removePrefix("gallery/grid/"))
            value == "games" -> Games.Picker
            value == "games/shape-match" -> Games.ShapeMatch
            value == "parent/gate" -> Parent.Gate
            value == "parent/settings" -> Parent.Settings
            else -> Home
        }
    }
}
