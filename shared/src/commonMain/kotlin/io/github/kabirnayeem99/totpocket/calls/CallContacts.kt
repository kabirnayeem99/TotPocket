package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import kotlin.jvm.JvmInline

@JvmInline
value class ContactId(val value: String)

/**
 * Someone the child can pretend to call. Their voice lines are expected at
 * `files/calls/<id>/greeting.ogg`, `filler_1..3.ogg` and `bye.ogg`; missing lines are silent.
 */
@Immutable
data class Contact(
    val id: ContactId,
    val name: String,
    val emoji: String,
) {
    val greeting: SoundRef get() = line("greeting")
    val fillers: List<SoundRef> get() = (1..FILLER_COUNT).map { line("filler_$it") }
    val bye: SoundRef get() = line("bye")

    private fun line(name: String) = SoundRef("files/calls/${id.value}/$name.ogg")

    private companion object {
        const val FILLER_COUNT = 3
    }
}

object CallContacts {
    val Mum = Contact(ContactId("mum"), "Mum", "👩")
    val Dad = Contact(ContactId("dad"), "Dad", "👨")
    val Grandma = Contact(ContactId("grandma"), "Grandma", "👵")
    val Grandpa = Contact(ContactId("grandpa"), "Grandpa", "👴")
    val Doggo = Contact(ContactId("doggo"), "Doggo", "🐶")

    /** Who answers when the child dials a number on the keypad. */
    val SillyMonkey = Contact(ContactId("monkey"), "Silly Monkey", "🐵")

    /** Shown on the contacts screen, in this order. */
    val favourites: List<Contact> = listOf(Mum, Dad, Grandma, Grandpa, Doggo)

    fun find(id: String): Contact = (favourites + SillyMonkey).firstOrNull { it.id.value == id } ?: Mum
}
