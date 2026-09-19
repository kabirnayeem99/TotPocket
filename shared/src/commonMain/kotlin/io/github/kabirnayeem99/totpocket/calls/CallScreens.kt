package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.ui.layout.ContentScale
import org.jetbrains.compose.resources.painterResource
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyles
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold
import io.github.kabirnayeem99.totpocket.ui.components.AppTopBar
import io.github.kabirnayeem99.totpocket.ui.components.CallControl
import io.github.kabirnayeem99.totpocket.ui.components.NavigationButtons
import io.github.kabirnayeem99.totpocket.ui.components.StatusStrip
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.launcher.BrandColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme
import kotlinx.coroutines.delay
import kotlin.time.Duration

private val HangUpRed = Color(0xFFF23B3B)
private val DarkText = Color(0xFF1B1B1B)
private val GreyText = Color(0xFF8A8A8E)

// ---------------------------------------------------------------- contacts

/**
 * The app's contact list, as the real app shows it: a row per person with their picture, name
 * and a call (or video) icon. Five rows fit on screen, so there's nothing to scroll. The Phone
 * app adds the green dialer button.
 */
@Composable
fun CallContactsScreen(
    app: CallApp,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onCall: (ContactId) -> Unit,
    onOpenKeypad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = app.style()
    if (app == CallApp.WhatsApp) {
        WhatsAppCallsList(style, onBack, onHome, onCall, modifier)
        return
    }
    AppScaffold(title = style.listTitle, style = style.listChrome, onBack = onBack, onHome = onHome, modifier = modifier) {
        Column(Modifier.fillMaxSize().padding(top = 8.dp)) {
            CallContacts.favourites.forEach { contact ->
                ContactRow(contact, style, isVideo = style.isVideo, onClick = { onCall(contact.id) })
            }
        }
        if (app.hasKeypad) {
            ToddlerButton(
                onClick = onOpenKeypad,
                contentDescription = "Keypad",
                modifier = Modifier.align(Alignment.BottomEnd).padding(24.dp).size(TotPocketDimens.MinTouchTarget),
                shape = CircleShape,
                color = BrandColors.Phone,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
            ) {
                Icon(TotPocketIcons.Keypad, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
            }
        }
    }
}

/**
 * WhatsApp's Calls tab, after the WhatsApp clone's design: teal bar with the title, search and
 * menu, the CHATS · STATUS · CALLS tabs with CALLS selected, then one row per person with a
 * 56dp picture and a green video-call icon.
 */
@Composable
private fun WhatsAppCallsList(
    style: CallAppStyle,
    onBack: () -> Unit,
    onHome: () -> Unit,
    onCall: (ContactId) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier.fillMaxSize().background(Color.White)) {
        Column(Modifier.fillMaxWidth().background(WhatsAppBar)) {
            AppTopBar(title = "WhatsApp", style = style.listChrome, onBack = onBack) {
                Icon(TotPocketIcons.Search, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
                Spacer(Modifier.width(18.dp))
                Icon(TotPocketIcons.MoreVert, contentDescription = null, tint = Color.White, modifier = Modifier.size(26.dp))
            }
            Row(Modifier.fillMaxWidth()) {
                listOf("CHATS", "STATUS", "CALLS").forEach { tab ->
                    val selected = tab == "CALLS"
                    Column(Modifier.weight(1f), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            tab,
                            color = if (selected) Color.White else Color.White.copy(alpha = 0.7f),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(vertical = 12.dp),
                        )
                        Box(Modifier.fillMaxWidth().height(2.5.dp).background(if (selected) Color(0xFFE0E0E0) else Color.Transparent))
                    }
                }
            }
        }
        Column(Modifier.weight(1f).fillMaxWidth().padding(top = 6.dp)) {
            CallContacts.favourites.forEach { contact ->
                ContactRow(contact, style, isVideo = true, onClick = { onCall(contact.id) }, subtitle = "↗ Today, 10:1${contact.name.length % 10}")
            }
        }
        NavigationButtons(color = style.listChrome.gestureBar, onHome = onHome)
    }
}

@Composable
private fun ContactRow(
    contact: Contact,
    style: CallAppStyle,
    isVideo: Boolean,
    onClick: () -> Unit,
    subtitle: String = style.rowSubtitle,
) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = "Call ${contact.name}",
        modifier = Modifier.fillMaxWidth().height(TotPocketDimens.MinTouchTarget),
        shape = RoundedCornerShape(0.dp),
        color = Color.Transparent,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        pressedScale = 0.97f,
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 20.dp), verticalAlignment = Alignment.CenterVertically) {
            Avatar(contact, size = 60.dp)
            Spacer(Modifier.width(16.dp))
            Column(Modifier.weight(1f)) {
                Text(contact.name, color = DarkText, fontSize = 19.sp, fontWeight = FontWeight.Medium)
                Text(subtitle, color = GreyText, fontSize = 14.sp)
            }
            Icon(
                imageVector = if (isVideo) TotPocketIcons.Video else TotPocketIcons.Phone,
                contentDescription = null,
                tint = style.accent,
                modifier = Modifier.size(30.dp),
            )
        }
    }
}

@Composable
private fun Avatar(contact: Contact, size: Dp, modifier: Modifier = Modifier) {
    Box(
        modifier.size(size).clip(CircleShape).background(contact.avatarColor()),
        contentAlignment = Alignment.Center,
    ) {
        ContactFaceContent(contact, size)
    }
}

/** The portrait or letter inside a contact's circle of [size]. */
@Composable
private fun ContactFaceContent(contact: Contact, size: Dp) {
    when (val face = contact.face) {
        is ContactFace.Picture -> Image(
            painter = painterResource(face.resource),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize().padding(if (face.fillsCircle) 0.dp else size * 0.12f),
        )
        is ContactFace.Initial -> Text(face.letter, color = contact.ink, fontSize = (size.value * 0.45f).sp, fontWeight = FontWeight.Medium)
    }
}

// ---------------------------------------------------------------- keypad

@Composable
fun KeypadScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onCall: (ContactId) -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { KeypadViewModel(container.soundPlayer) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnCall by rememberUpdatedState(onCall)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                is KeypadEffect.StartCall -> currentOnCall(effect.contactId)
            }
        }
    }
    KeypadContent(state, viewModel::onAction, onBack, onHome, modifier)
}

/** The HyperOS dialer: the number on top, round grey keys with letters, a green call button. */
@Composable
fun KeypadContent(
    state: KeypadUiState,
    onAction: (KeypadAction) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AppScaffold(title = "", style = AppChromeStyles.System, onBack = onBack, onHome = onHome, modifier = modifier) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = isLandscape(maxWidth, maxHeight)
            val display: @Composable (Modifier) -> Unit = { displayModifier ->
                Box(displayModifier, contentAlignment = Alignment.Center) {
                    Text(state.dialled, color = DarkText, fontSize = 40.sp, fontWeight = FontWeight.Light, maxLines = 1)
                }
            }
            val callButton: @Composable () -> Unit = {
                ToddlerButton(
                    onClick = { onAction(KeypadAction.CallPressed) },
                    contentDescription = "Call",
                    modifier = Modifier.size(TotPocketDimens.MinTouchTarget),
                    shape = CircleShape,
                    color = BrandColors.Phone,
                    outline = Color.Transparent,
                    outlineWidth = 1.dp,
                ) {
                    Icon(TotPocketIcons.Phone, contentDescription = null, tint = Color.White, modifier = Modifier.size(40.dp))
                }
            }
            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Column(Modifier.weight(1f).fillMaxHeight(), horizontalAlignment = Alignment.CenterHorizontally) {
                        display(Modifier.weight(1f).fillMaxWidth())
                        callButton()
                        Spacer(Modifier.height(12.dp))
                    }
                    Keys(onAction, columns = 6, modifier = Modifier.weight(2f).fillMaxHeight())
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    display(Modifier.weight(1f).fillMaxWidth())
                    Keys(onAction, columns = 3, modifier = Modifier.fillMaxWidth())
                    Spacer(Modifier.height(16.dp))
                    callButton()
                    Spacer(Modifier.height(16.dp))
                }
            }
        }
    }
}

@Composable
private fun Keys(onAction: (KeypadAction) -> Unit, columns: Int, modifier: Modifier = Modifier) {
    Column(modifier, verticalArrangement = Arrangement.spacedBy(10.dp, Alignment.CenterVertically)) {
        KeypadKey.entries.chunked(columns).forEach { row ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                row.forEach { key ->
                    ToddlerButton(
                        onClick = { onAction(KeypadAction.KeyPressed(key)) },
                        contentDescription = key.label,
                        modifier = Modifier.size(TotPocketDimens.MinTouchTarget),
                        shape = CircleShape,
                        color = Color(0xFFF2F3F5),
                        outline = Color.Transparent,
                        outlineWidth = 1.dp,
                        playTapSound = false,
                        debounce = Duration.ZERO,
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(key.label, color = DarkText, fontSize = 34.sp)
                            if (key.letters.isNotEmpty()) Text(key.letters, color = GreyText, fontSize = 11.sp)
                        }
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- call

@Composable
fun CallScreen(
    contactId: String,
    app: CallApp,
    onHome: () -> Unit,
    onFinished: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val container = LocalAppContainer.current
    val viewModel = viewModel(key = "call-$contactId") {
        CallViewModel(CallContacts.find(contactId), container.soundPlayer, container.device, container.random)
    }
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnFinished by rememberUpdatedState(onFinished)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                CallEffect.Finished -> currentOnFinished()
            }
        }
    }
    CallContent(state, app, viewModel::onAction, onHome, modifier)
}

/**
 * The in-call screen, dressed as the app the call came from: HyperOS Phone (dark, name on top),
 * WhatsApp (dark teal, "WhatsApp voice call", encryption note) or imo (a bright "video" with a
 * small self-view). Mute and speaker toggle like the real buttons; the red button hangs up.
 */
@Composable
fun CallContent(
    state: CallUiState,
    app: CallApp,
    onAction: (CallAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val style = app.style()
    if (style.isVideo) {
        VideoCallContent(state, app, style, onAction, onHome, modifier)
        return
    }
    Column(modifier.fillMaxSize().background(style.callBackground)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusStrip(contentColor = Color.White)
                Spacer(Modifier.height(16.dp))
                style.callHeader?.let { header ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app == CallApp.WhatsApp) {
                            Icon(TotPocketIcons.Lock, contentDescription = null, tint = LightText, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(header, color = LightText, fontSize = 15.sp)
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(state.contact.name, color = Color.White, fontSize = 34.sp, fontWeight = FontWeight.Normal)
                Spacer(Modifier.height(8.dp))
                CallStatus(state.phase)
                Spacer(Modifier.height(40.dp))
                CallerAvatar(state)
                Spacer(Modifier.weight(1f))
                CallControls(state.phase, onAction)
                Spacer(Modifier.height(24.dp))
            }
        }
        NavigationButtons(color = Color.White, onHome = onHome)
    }
}

private val LightText = Color.White.copy(alpha = 0.75f)

/** "Calling…", then a running mm:ss timer, then "Call ended" — like every real dialer. */
@Composable
private fun CallStatus(phase: CallPhase) {
    var seconds by remember { mutableIntStateOf(0) }
    LaunchedEffect(phase) {
        if (phase == CallPhase.InCall) {
            while (true) {
                delay(1_000)
                seconds++
            }
        }
    }
    val text = when (phase) {
        CallPhase.Calling -> "Calling…"
        CallPhase.InCall -> "${(seconds / 60).toString().padStart(2, '0')}:${(seconds % 60).toString().padStart(2, '0')}"
        CallPhase.Ended -> "Call ended"
    }
    Text(text, color = LightText, fontSize = 18.sp)
}

/** Gentle 1 Hz pulse while calling; a small wobble while the caller speaks. Draw-phase reads only. */
@Composable
private fun CallerAvatar(state: CallUiState) {
    val transition = rememberInfiniteTransition(label = "caller")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "callingPulse",
    )
    val wobble by transition.animateFloat(
        initialValue = 0.98f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "talkWobble",
    )
    val phase = state.phase
    val talking = state.isTalking
    Avatar(
        contact = state.contact,
        size = 150.dp,
        modifier = Modifier.graphicsLayer {
            val scale = when {
                phase == CallPhase.Calling -> pulse
                talking -> wobble
                else -> 1f
            }
            scaleX = scale
            scaleY = scale
        },
    )
}

/**
 * A video call as WhatsApp and imo show it. While it rings, the child's own front camera fills
 * the screen; once the other person picks up they fill the screen and the child moves to a small
 * self-view window. The camera button hides and shows the self-view.
 */
@Composable
private fun VideoCallContent(
    state: CallUiState,
    app: CallApp,
    style: CallAppStyle,
    onAction: (CallAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var cameraOn by remember { mutableStateOf(true) }
    var muted by remember { mutableStateOf(false) }
    val selfFallback: @Composable () -> Unit = {
        Box(Modifier.fillMaxSize().background(Color(0xFF263238)), contentAlignment = Alignment.Center) {
            Text("🙂", fontSize = 48.sp)
        }
    }
    Column(modifier.fillMaxSize().background(Color.Black)) {
        Box(Modifier.weight(1f).fillMaxWidth()) {
            when {
                state.phase == CallPhase.Calling && cameraOn -> SelfCamera(Modifier.fillMaxSize(), selfFallback)
                state.phase == CallPhase.Calling -> Box(Modifier.fillMaxSize().background(style.callBackground))
                else -> CallerVideo(state, style, Modifier.fillMaxSize())
            }
            // Header over the picture, like the real apps.
            Column(
                Modifier.fillMaxWidth().background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.55f), Color.Transparent)))
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
            ) {
                StatusStrip(contentColor = Color.White)
                Spacer(Modifier.height(8.dp))
                style.callHeader?.let { header ->
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (app == CallApp.WhatsApp) {
                            Icon(TotPocketIcons.Lock, contentDescription = null, tint = LightText, modifier = Modifier.size(13.dp))
                            Spacer(Modifier.width(6.dp))
                        }
                        Text(if (app == CallApp.WhatsApp) "End-to-end encrypted" else header, color = LightText, fontSize = 13.sp)
                    }
                    Spacer(Modifier.height(10.dp))
                }
                Text(state.contact.name, color = Color.White, fontSize = 28.sp)
                Spacer(Modifier.height(4.dp))
                CallStatus(state.phase)
            }
            if (state.phase != CallPhase.Calling && cameraOn) {
                SelfCamera(
                    Modifier.align(Alignment.BottomEnd).padding(end = 16.dp, bottom = 16.dp)
                        .size(width = 110.dp, height = 160.dp).clip(RoundedCornerShape(14.dp)),
                    selfFallback,
                )
            }
        }
        // Controls on a dark panel (WhatsApp) or a coloured strip (imo).
        Row(
            Modifier.fillMaxWidth()
                .background(
                    if (app == CallApp.WhatsApp) Color(0xFF1F2C33) else BrandColors.Imo,
                    RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
                )
                .padding(vertical = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (state.phase == CallPhase.Ended) {
                Spacer(Modifier.size(TotPocketDimens.MinTouchTarget))
            } else {
                RoundControl(TotPocketIcons.Video, "Camera", active = !cameraOn) { cameraOn = !cameraOn }
                RoundControl(TotPocketIcons.Mic, "Mute", active = muted) { muted = !muted }
                ToddlerButton(
                    onClick = { onAction(CallAction.HangUp) },
                    contentDescription = "End call",
                    modifier = Modifier.size(80.dp),
                    shape = CircleShape,
                    color = HangUpRed,
                    outline = Color.Transparent,
                    outlineWidth = 1.dp,
                    minSize = 72.dp,
                ) {
                    Icon(TotPocketIcons.HangUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(38.dp))
                }
            }
        }
        NavigationButtons(color = Color.White, onHome = onHome, modifier = Modifier.background(if (app == CallApp.WhatsApp) Color(0xFF1F2C33) else BrandColors.Imo))
    }
}

@Composable
private fun RoundControl(icon: ImageVector, label: String, active: Boolean, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = label,
        modifier = Modifier.size(72.dp),
        shape = CircleShape,
        color = if (active) Color.White else Color.White.copy(alpha = 0.18f),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        minSize = 64.dp,
    ) {
        Icon(icon, contentDescription = null, tint = if (active) DarkText else Color.White, modifier = Modifier.size(30.dp))
    }
}

/** The other person "on camera": their picture large on a soft background, moving gently, talking when they talk. */
@Composable
private fun CallerVideo(state: CallUiState, style: CallAppStyle, modifier: Modifier = Modifier) {
    val transition = rememberInfiniteTransition(label = "video")
    val sway by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "sway",
    )
    val talking = state.isTalking
    Box(modifier.background(Brush.verticalGradient(listOf(state.contact.avatarColor(), Color(0xFF2B2F3A)))), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .graphicsLayer {
                    translationX = sway * 10.dp.toPx()
                    rotationZ = if (talking) sway * 3f else 0f
                    val breathe = if (talking) 1f + sway * 0.02f else 1f
                    scaleX = breathe
                    scaleY = breathe
                }
                .size(260.dp)
                .clip(CircleShape)
                .background(state.contact.avatarColor()),
            contentAlignment = Alignment.Center,
        ) {
            if (state.phase == CallPhase.Ended) Text("👋", fontSize = 140.sp) else ContactFaceContent(state.contact, 260.dp)
        }
    }
}

@Composable
private fun CallControls(phase: CallPhase, onAction: (CallAction) -> Unit) {
    var muted by remember { mutableStateOf(false) }
    var speaker by remember { mutableStateOf(false) }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        if (phase == CallPhase.InCall) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
                CallControl(
                    icon = TotPocketIcons.Mic,
                    caption = "Mute",
                    background = if (muted) Color.White else Color.White.copy(alpha = 0.18f),
                    iconTint = if (muted) DarkText else Color.White,
                    captionColor = LightText,
                    onClick = { muted = !muted },
                )
                CallControl(
                    icon = TotPocketIcons.Speaker,
                    caption = "Speaker",
                    background = if (speaker) Color.White else Color.White.copy(alpha = 0.18f),
                    iconTint = if (speaker) DarkText else Color.White,
                    captionColor = LightText,
                    onClick = { speaker = !speaker },
                )
            }
            Spacer(Modifier.height(28.dp))
        }
        if (phase != CallPhase.Ended) {
            ToddlerButton(
                onClick = { onAction(CallAction.HangUp) },
                contentDescription = "End call",
                modifier = Modifier.size(TotPocketDimens.MinTouchTarget),
                shape = CircleShape,
                color = HangUpRed,
                outline = Color.Transparent,
                outlineWidth = 1.dp,
            ) {
                Icon(TotPocketIcons.HangUp, contentDescription = null, tint = Color.White, modifier = Modifier.size(44.dp))
            }
        } else {
            Spacer(Modifier.size(TotPocketDimens.MinTouchTarget))
        }
    }
}

@Preview
@Composable
private fun WhatsAppCallPreview() {
    TotPocketTheme {
        CallContent(CallUiState(CallContacts.Grandma, CallPhase.InCall, isTalking = true), CallApp.WhatsApp, onAction = {}, onHome = {})
    }
}

@Preview
@Composable
private fun PhoneContactsPreview() {
    TotPocketTheme { CallContactsScreen(CallApp.Phone, onBack = {}, onHome = {}, onCall = {}, onOpenKeypad = {}) }
}

@Preview
@Composable
private fun KeypadContentPreview() {
    TotPocketTheme { KeypadContent(KeypadUiState(dialled = "0171"), onAction = {}, onBack = {}, onHome = {}) }
}
