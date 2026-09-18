package io.github.kabirnayeem99.totpocket.calls

/** Which pretend app a call is made from; each looks like the real app the child knows. */
enum class CallApp(val id: String) {
    Phone("phone"),
    WhatsApp("whatsapp"),
    Imo("imo");

    /** Only the Phone app has a keypad. */
    val hasKeypad: Boolean get() = this == Phone

    companion object {
        fun fromId(id: String): CallApp = entries.firstOrNull { it.id == id } ?: Phone
    }
}
