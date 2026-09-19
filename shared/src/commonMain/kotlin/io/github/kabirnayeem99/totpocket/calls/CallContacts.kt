package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.runtime.Immutable
import androidx.compose.ui.graphics.Color
import io.github.kabirnayeem99.totpocket.audio.SoundRef
import org.jetbrains.compose.resources.DrawableResource
import totpocket.shared.generated.resources.Res
import totpocket.shared.generated.resources.avatar_dad
import totpocket.shared.generated.resources.avatar_grandpa
import totpocket.shared.generated.resources.avatar_unknown
import kotlin.jvm.JvmInline

@JvmInline
value class ContactId(val value: String)

/** What a contact's round avatar shows. No emoji faces. */
@Immutable
sealed interface ContactFace {
    /** A drawn portrait; [fillsCircle] when the artwork is already a full disc. */
    data class Picture(val resource: DrawableResource, val fillsCircle: Boolean) : ContactFace

    /** Just the first letter, as a phone shows a contact with no photo. Used for every woman. */
    data class Initial(val letter: String) : ContactFace
}

/**
 * Someone the child can pretend to call. Their voice lines are expected at
 * `files/calls/<id>/greeting.ogg`, `filler_1..3.ogg` and `bye.ogg`; missing lines are silent.
 */
@Immutable
data class Contact(
    val id: ContactId,
    val name: String,
    val face: ContactFace,
    /** The avatar's background; every contact has its own. */
    val color: Color,
    /** The letter's colour on [color], for [ContactFace.Initial]. */
    val ink: Color = Color(0xFF1B1B1B),
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
    // No pictures of women: Mum and Grandma show their first letter.
    val Mum = Contact(ContactId("mum"), "Mum", ContactFace.Initial("M"), color = Color(0xFFF8BBD0), ink = Color(0xFFAD1457))
    val Dad = Contact(ContactId("dad"), "Dad", ContactFace.Picture(Res.drawable.avatar_dad, fillsCircle = false), color = Color(0xFFBBDEFB))
    val Grandma = Contact(ContactId("grandma"), "Grandma", ContactFace.Initial("G"), color = Color(0xFFD1C4E9), ink = Color(0xFF4527A0))
    val Grandpa = Contact(ContactId("grandpa"), "Grandpa", ContactFace.Picture(Res.drawable.avatar_grandpa, fillsCircle = true), color = Color(0xFFC8E6C9))

    /** Who answers when the child dials a number on the keypad: an unknown caller. */
    val UnknownCaller = Contact(ContactId("unknown"), "Unknown", ContactFace.Picture(Res.drawable.avatar_unknown, fillsCircle = true), color = Color(0xFFFFF59D))

    /** Shown on the contacts screen, in this order. */
    val favourites: List<Contact> = listOf(Mum, Dad, Grandma, Grandpa)

    fun find(id: String): Contact = (favourites + UnknownCaller).firstOrNull { it.id.value == id } ?: Mum
}
