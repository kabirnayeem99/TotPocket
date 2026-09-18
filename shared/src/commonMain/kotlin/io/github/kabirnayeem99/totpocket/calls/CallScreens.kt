package io.github.kabirnayeem99.totpocket.calls

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.ui.components.Emoji
import io.github.kabirnayeem99.totpocket.ui.components.RoundIconButton
import io.github.kabirnayeem99.totpocket.ui.components.SectionCard
import io.github.kabirnayeem99.totpocket.ui.components.SectionGlyph
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerGrid
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerScaffold
import io.github.kabirnayeem99.totpocket.ui.components.isLandscape
import io.github.kabirnayeem99.totpocket.ui.icons.TotPocketIcons
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketColors
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketDimens
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

// ---------------------------------------------------------------- contacts

private sealed interface ContactsTile {
    data class Person(val contact: Contact) : ContactsTile
    data object Keypad : ContactsTile
}

/** Five familiar faces and a keypad tile, filling the screen. */
@Composable
fun CallContactsScreen(
    onHome: () -> Unit,
    onCall: (ContactId) -> Unit,
    onOpenKeypad: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val tiles = remember { CallContacts.favourites.map(ContactsTile::Person) + ContactsTile.Keypad }
    ToddlerScaffold(onHome = onHome, modifier = modifier, background = TotPocketColors.RedTint) {
        ToddlerGrid(items = tiles, rows = 3) { tile, cellModifier ->
            when (tile) {
                is ContactsTile.Person -> SectionCard(
                    onClick = { onCall(tile.contact.id) },
                    label = tile.contact.name,
                    color = TotPocketColors.Surface,
                    contentColor = TotPocketColors.OnLight,
                    modifier = cellModifier,
                ) { Emoji(tile.contact.emoji) }

                ContactsTile.Keypad -> SectionCard(
                    onClick = onOpenKeypad,
                    label = "Keypad",
                    color = TotPocketColors.Red,
                    contentColor = TotPocketColors.OnDark,
                    modifier = cellModifier,
                ) { SectionGlyph(TotPocketIcons.Keypad, TotPocketColors.OnDark, TotPocketDimens.SmallIconSize) }
            }
        }
    }
}

// ---------------------------------------------------------------- keypad

@Composable
fun KeypadScreen(
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
    KeypadContent(state, viewModel::onAction, onHome, modifier)
}

@Composable
fun KeypadContent(
    state: KeypadUiState,
    onAction: (KeypadAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(
        onHome = onHome,
        modifier = modifier,
        background = TotPocketColors.RedTint,
        accessory = {
            RoundIconButton(
                onClick = { onAction(KeypadAction.CallPressed) },
                icon = TotPocketIcons.Phone,
                contentDescription = "Call",
                color = if (state.canCall) TotPocketColors.Green else TotPocketColors.Surface,
                iconTint = if (state.canCall) TotPocketColors.OnDark else TotPocketColors.Green,
                size = TotPocketDimens.HomeButtonSize,
            )
        },
    ) {
        Column(Modifier.fillMaxSize()) {
            DialledDots(count = state.dialled)
            Spacer(Modifier.height(TotPocketDimens.TightSpacing))
            ToddlerGrid(
                items = KeypadKey.entries,
                portraitColumns = 3,
                landscapeColumns = 6,
                spacing = TotPocketDimens.TightSpacing,
                modifier = Modifier.weight(1f),
            ) { key, cellModifier ->
                ToddlerButton(
                    onClick = { onAction(KeypadAction.KeyPressed(key)) },
                    contentDescription = key.label,
                    modifier = cellModifier,
                    shape = RoundedCornerShape(TotPocketDimens.TileCorner),
                    playTapSound = false,
                    debounce = kotlin.time.Duration.ZERO,
                ) {
                    Text(key.label, fontSize = TotPocketDimens.KeySize, fontWeight = FontWeight.Black, color = TotPocketColors.OnLight)
                }
            }
        }
    }
}

/** Big dots instead of digits: shows that something was dialled without numbers to read. */
@Composable
private fun DialledDots(count: Int) {
    Row(
        modifier = Modifier.fillMaxWidth().height(48.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        repeat(count) {
            Box(Modifier.size(24.dp).clip(CircleShape).background(TotPocketColors.Red))
        }
    }
}

// ---------------------------------------------------------------- call

@Composable
fun CallScreen(
    contactId: String,
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
    CallContent(state, viewModel::onAction, onHome, modifier)
}

@Composable
fun CallContent(
    state: CallUiState,
    onAction: (CallAction) -> Unit,
    onHome: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ToddlerScaffold(onHome = onHome, modifier = modifier, background = TotPocketColors.RedTint) {
        BoxWithConstraints(Modifier.fillMaxSize()) {
            val landscape = isLandscape(maxWidth, maxHeight)
            val avatarSize = minOf(if (landscape) maxHeight * 0.75f else maxWidth * 0.7f, 280.dp)
            val caller: @Composable () -> Unit = {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CallerAvatar(state, avatarSize)
                    Spacer(Modifier.height(TotPocketDimens.TightSpacing))
                    Text(state.contact.name, fontSize = 32.sp, fontWeight = FontWeight.Black, color = TotPocketColors.OnLight)
                }
            }
            val buttons: @Composable () -> Unit = { CallButtons(state.phase, onAction) }

            if (landscape) {
                Row(Modifier.fillMaxSize(), verticalAlignment = Alignment.CenterVertically) {
                    Box(Modifier.weight(1f).fillMaxHeight(), contentAlignment = Alignment.Center) { caller() }
                    Spacer(Modifier.width(TotPocketDimens.CardSpacing))
                    Box(Modifier.width(TotPocketDimens.RoundActionSize * 2 + TotPocketDimens.CardSpacing)) { buttons() }
                }
            } else {
                Column(Modifier.fillMaxSize(), horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) { caller() }
                    Spacer(Modifier.height(TotPocketDimens.CardSpacing))
                    buttons()
                }
            }
        }
    }
}

@Composable
private fun CallButtons(phase: CallPhase, onAction: (CallAction) -> Unit) {
    val answer: @Composable () -> Unit = {
        RoundIconButton(
            onClick = { onAction(CallAction.Answer) },
            icon = TotPocketIcons.Phone,
            contentDescription = "Answer",
            color = TotPocketColors.Green,
            iconTint = TotPocketColors.OnDark,
        )
    }
    val hangUp: @Composable () -> Unit = {
        RoundIconButton(
            onClick = { onAction(CallAction.HangUp) },
            icon = TotPocketIcons.HangUp,
            contentDescription = "Hang up",
            color = TotPocketColors.Red,
            iconTint = TotPocketColors.OnDark,
        )
    }
    Row(
        modifier = Modifier.fillMaxWidth().height(TotPocketDimens.RoundActionSize),
        horizontalArrangement = when (phase) {
            CallPhase.Ringing -> Arrangement.SpaceBetween
            else -> Arrangement.Center
        },
        verticalAlignment = Alignment.CenterVertically,
    ) {
        when (phase) {
            CallPhase.Ringing -> {
                answer()
                hangUp()
            }
            CallPhase.InCall -> hangUp()
            // Nothing to press while saying goodbye; the screen closes by itself.
            CallPhase.Ended -> Unit
        }
    }
}

/**
 * The caller's face. Ringing: a gentle 1 Hz pulse. In the call: a ring fills slowly over the
 * call's length and a "voice" wobble shows while the caller speaks. Ended: a waving hand.
 * Every animated value is read in the draw phase only.
 */
@Composable
private fun CallerAvatar(state: CallUiState, size: Dp) {
    val transition = rememberInfiniteTransition(label = "caller")
    val pulse by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "ringPulse",
    )
    val wobble by transition.animateFloat(
        initialValue = 0.97f,
        targetValue = 1.03f,
        animationSpec = infiniteRepeatable(tween(220, easing = LinearEasing), RepeatMode.Reverse),
        label = "talkWobble",
    )
    val callProgress = remember { Animatable(0f) }
    LaunchedEffect(state.phase) {
        if (state.phase == CallPhase.InCall) {
            callProgress.animateTo(
                targetValue = 1f,
                animationSpec = tween(CallScript.CallLength.inWholeMilliseconds.toInt(), easing = LinearEasing),
            )
        }
    }
    val phase = state.phase
    val talking = state.isTalking

    Box(
        modifier = Modifier
            .size(size)
            .drawBehind {
                if (phase == CallPhase.InCall) {
                    val stroke = 12.dp.toPx()
                    drawArc(
                        color = TotPocketColors.Green,
                        startAngle = -90f,
                        sweepAngle = 360f * callProgress.value,
                        useCenter = false,
                        topLeft = Offset(stroke / 2, stroke / 2),
                        size = Size(this.size.width - stroke, this.size.height - stroke),
                        style = Stroke(width = stroke, cap = StrokeCap.Round),
                    )
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Box(
            modifier = Modifier
                .size(size - 36.dp)
                .graphicsLayer {
                    val scale = when {
                        phase == CallPhase.Ringing -> pulse
                        talking -> wobble
                        else -> 1f
                    }
                    scaleX = scale
                    scaleY = scale
                }
                .clip(CircleShape)
                .background(TotPocketColors.Surface)
                .border(TotPocketDimens.CardOutline, TotPocketColors.Outline, CircleShape),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = if (phase == CallPhase.Ended) "👋" else state.contact.emoji,
                fontSize = (size.value * 0.4f).sp,
            )
        }
    }
}

@Preview
@Composable
private fun CallContentPreview() {
    TotPocketTheme {
        CallContent(CallUiState(CallContacts.Grandma, CallPhase.Ringing, isTalking = false), onAction = {}, onHome = {})
    }
}

@Preview
@Composable
private fun KeypadContentPreview() {
    TotPocketTheme { KeypadContent(KeypadUiState(dialled = 3), onAction = {}, onHome = {}) }
}
