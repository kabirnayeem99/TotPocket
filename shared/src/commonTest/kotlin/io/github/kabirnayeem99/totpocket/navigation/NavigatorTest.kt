package io.github.kabirnayeem99.totpocket.navigation

import androidx.compose.runtime.saveable.SaverScope
import io.github.kabirnayeem99.totpocket.calls.CallApp
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class NavigatorTest {

    @Test
    fun `starts at home`() {
        assertEquals(Route.Home, Navigator().current.route)
    }

    @Test
    fun `back at home is swallowed and never empties the stack`() {
        val navigator = Navigator()
        assertFalse(navigator.pop())
        assertEquals(listOf(Route.Home), navigator.backStack.map { it.route })
    }

    @Test
    fun `push then pop returns to the previous screen and reports the removed entry`() {
        val navigator = Navigator()
        val removed = mutableListOf<Route>()
        navigator.onEntryRemoved = { removed += it.route }

        navigator.push(Route.Gallery.Categories)
        navigator.push(Route.Gallery.Grid("animals"))
        assertTrue(navigator.pop())

        assertEquals(Route.Gallery.Categories, navigator.current.route)
        assertEquals(listOf<Route>(Route.Gallery.Grid("animals")), removed)
    }

    @Test
    fun `pushing the current route again is ignored`() {
        val navigator = Navigator()
        navigator.push(Route.Calls.Contacts(CallApp.Phone))
        navigator.push(Route.Calls.Contacts(CallApp.Phone))
        assertEquals(2, navigator.backStack.size)
    }

    @Test
    fun `popToHome clears everything above home`() {
        val navigator = Navigator()
        val removed = mutableListOf<Route>()
        navigator.onEntryRemoved = { removed += it.route }
        navigator.push(Route.Calls.Contacts(CallApp.Phone))
        navigator.push(Route.Calls.Active("mum", CallApp.Phone))

        navigator.popToHome()

        assertEquals(listOf(Route.Home), navigator.backStack.map { it.route })
        assertEquals(listOf<Route>(Route.Calls.Active("mum", CallApp.Phone), Route.Calls.Contacts(CallApp.Phone)), removed)
    }

    @Test
    fun `replace swaps the top entry`() {
        val navigator = Navigator()
        navigator.push(Route.Parent.Gate)
        navigator.replace(Route.Parent.Settings)
        assertEquals(listOf(Route.Home, Route.Parent.Settings), navigator.backStack.map { it.route })
    }

    @Test
    fun `entries get unique ids`() {
        val navigator = Navigator()
        navigator.push(Route.Games.Picker)
        navigator.pop()
        navigator.push(Route.Games.Picker)
        assertEquals(navigator.backStack.map { it.id }.distinct().size, navigator.backStack.size)
        assertEquals("2", navigator.current.id)
    }

    @Test
    fun `every route survives encoding`() {
        val routes = listOf(
            Route.Home, Route.Calls.Contacts(CallApp.Phone), Route.Calls.Contacts(CallApp.Imo), Route.Calls.Keypad, Route.Calls.Active("grandma", CallApp.WhatsApp),
            Route.Gallery.Categories, Route.Gallery.Grid("flowers"), Route.YouTube, Route.Games.Picker,
            Route.Games.ShapeMatch, Route.Parent.Gate, Route.Parent.Settings,
        )
        routes.forEach { assertEquals(it, Route.decode(Route.encode(it))) }
    }

    @Test
    fun `back stack survives save and restore`() {
        val navigator = Navigator()
        navigator.push(Route.Gallery.Categories)
        navigator.push(Route.Gallery.Grid("nature"))

        val saved = with(Navigator.Saver) { SaverScope { true }.save(navigator) }!!
        val restored = Navigator.Saver.restore(saved)!!

        assertEquals(navigator.backStack.toList(), restored.backStack.toList())
        restored.push(Route.Home)
        assertEquals("3", restored.current.id)
    }
}
