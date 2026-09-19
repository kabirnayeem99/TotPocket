package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyle
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.launcher.BrandColors

/** Everything that makes a pretend call look like the real app it came from. */
data class CallAppStyle(
    val listTitle: String,
    val listChrome: AppChromeStyle,
    /** Colour of the call/video icon at the end of each contact row. */
    val accent: Color,
    /** The line under each contact's name in the list. */
    val rowSubtitle: String,
    val callBackground: Brush,
    /** Small header shown above the caller on the call screen, if the real app has one. */
    val callHeader: String?,
    val isVideo: Boolean,
)

/** Colours from the WhatsApp clone's design system (GREEN500 bar, GREEN450 icons). */
val WhatsAppBar = Color(0xFF19887A)
val WhatsAppIconGreen = Color(0xFF1AA05B)

fun CallApp.style(): CallAppStyle = when (this) {
    CallApp.Phone -> CallAppStyle(
        listTitle = "Contacts",
        listChrome = AppChromeStyles.System,
        accent = BrandColors.Phone,
        rowSubtitle = "Mobile",
        callBackground = Brush.verticalGradient(listOf(Color(0xFF3A3D4F), Color(0xFF15161D))),
        callHeader = null,
        isVideo = false,
    )
    CallApp.WhatsApp -> CallAppStyle(
        listTitle = "WhatsApp",
        listChrome = AppChromeStyle(Color.White, WhatsAppBar, Color.White, Color(0xFF1B1B1B)),
        accent = WhatsAppIconGreen,
        rowSubtitle = "Video call",
        callBackground = Brush.verticalGradient(listOf(Color(0xFF1F2C33), Color(0xFF0B141A))),
        callHeader = "WhatsApp video call",
        isVideo = true,
    )
    CallApp.Imo -> CallAppStyle(
        listTitle = "imo",
        listChrome = AppChromeStyle(Color.White, BrandColors.Imo, Color.White, Color(0xFF1B1B1B)),
        accent = BrandColors.Imo,
        rowSubtitle = "Video call",
        callBackground = Brush.verticalGradient(listOf(Color(0xFF3AA0FF), Color(0xFF0B4DA8))),
        callHeader = "imo video call",
        isVideo = true,
    )
}

/** The contact's own avatar colour — each one differs, so the child can tell them apart. */
fun Contact.avatarColor(): Color = color
