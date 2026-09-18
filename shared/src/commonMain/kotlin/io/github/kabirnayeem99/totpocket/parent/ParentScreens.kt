package io.github.kabirnayeem99.totpocket.parent

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.LocalAppContainer
import io.github.kabirnayeem99.totpocket.settings.ParentSettings
import io.github.kabirnayeem99.totpocket.ui.components.AppChromeStyle
import io.github.kabirnayeem99.totpocket.ui.components.AppScaffold
import io.github.kabirnayeem99.totpocket.ui.components.ToddlerButton
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme
import kotlin.math.roundToInt

/** HyperOS Settings: light grey page, white rounded groups. */
private val SettingsChrome = AppChromeStyle(Color(0xFFF5F5F7), Color(0xFFF5F5F7), Color(0xFF1B1B1B), Color(0xFF1B1B1B))
private val Accent = Color(0xFF3482FF)
private val DarkText = Color(0xFF1B1B1B)
private val GreyText = Color(0xFF8A8A8E)

// ---------------------------------------------------------------- gate

@Composable
fun ParentGateScreen(onBack: () -> Unit, onHome: () -> Unit, onUnlocked: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ParentGateViewModel(container.random) }
    val question by viewModel.question.collectAsStateWithLifecycle()
    val currentOnUnlocked by rememberUpdatedState(onUnlocked)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                ParentGateEffect.Unlocked -> currentOnUnlocked()
            }
        }
    }
    ParentGateContent(question, viewModel::onAction, onBack, onHome)
}

@Composable
fun ParentGateContent(
    question: GateQuestion,
    onAction: (ParentGateAction) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    AppScaffold(title = "Grown-ups only", style = SettingsChrome, onBack = onBack, onHome = onHome) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("To open settings, answer:", color = GreyText, fontSize = 16.sp)
            Spacer(Modifier.height(12.dp))
            Text("${question.a} + ${question.b} = ?", color = DarkText, fontSize = 44.sp, fontWeight = FontWeight.SemiBold)
            Spacer(Modifier.height(32.dp))
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                question.answers.forEach { answer ->
                    ToddlerButton(
                        onClick = { onAction(ParentGateAction.AnswerPicked(answer)) },
                        contentDescription = answer.toString(),
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        color = Color.White,
                        outline = Color.Transparent,
                        outlineWidth = 1.dp,
                        playTapSound = false,
                        pressedScale = 0.96f,
                    ) {
                        Text(answer.toString(), color = DarkText, fontSize = 30.sp)
                    }
                }
            }
        }
    }
}

// ---------------------------------------------------------------- settings

@Composable
fun ParentSettingsScreen(onBack: () -> Unit, onHome: () -> Unit) {
    val container = LocalAppContainer.current
    val viewModel = viewModel { ParentSettingsViewModel(container.settingsStore, container.device) }
    val state by viewModel.state.collectAsStateWithLifecycle()
    // Pinning is confirmed by a system dialog; re-read it whenever we come back to the front.
    LifecycleResumeEffect(viewModel) {
        viewModel.onAction(ParentSettingsAction.Refresh)
        onPauseOrDispose { }
    }
    ParentSettingsContent(state, viewModel::onAction, onBack, onHome)
}

@Composable
fun ParentSettingsContent(
    state: ParentSettingsUiState,
    onAction: (ParentSettingsAction) -> Unit,
    onBack: () -> Unit,
    onHome: () -> Unit,
) {
    AppScaffold(title = "TotPocket settings", style = SettingsChrome, onBack = onBack, onHome = onHome) {
        Column(Modifier.fillMaxSize().padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Group {
                Label("Volume limit", "${(state.settings.volumeCeiling * 100).roundToInt()}%")
                Slider(
                    value = state.settings.volumeCeiling,
                    onValueChange = { onAction(ParentSettingsAction.VolumeChanged(it)) },
                    valueRange = ParentSettings.MIN_VOLUME..1f,
                    colors = SliderDefaults.colors(thumbColor = Accent, activeTrackColor = Accent),
                )
            }
            Group {
                Label("Play time", "Bedtime screen when time is up")
                Spacer(Modifier.height(12.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    ParentSettings.PlayLimitChoices.forEach { minutes ->
                        Choice(
                            text = if (minutes == 0) "Off" else "$minutes min",
                            selected = state.settings.playLimitMinutes == minutes,
                            onClick = { onAction(ParentSettingsAction.PlayLimitChosen(minutes)) },
                            modifier = Modifier.weight(1f),
                        )
                    }
                }
            }
            Group {
                Label(
                    if (state.isPinned) "TotPocket is pinned" else "Pin TotPocket",
                    if (state.isPinned) {
                        "Home and Recents are blocked. Unpin here, or hold Back + Recents."
                    } else {
                        "Keeps your child inside TotPocket. Turn on Screen pinning in the phone's security settings first."
                    },
                )
                Spacer(Modifier.height(12.dp))
                WideButton(
                    text = if (state.isPinned) "Unpin" else "Pin",
                    background = Accent,
                    onClick = { onAction(ParentSettingsAction.PinToggled) },
                )
            }
            Group {
                WideButton(text = "Exit TotPocket", background = Color(0xFFF23B3B), onClick = { onAction(ParentSettingsAction.ExitApp) })
            }
        }
    }
}

@Composable
private fun Group(content: @Composable ColumnScope.() -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(Color.White, RoundedCornerShape(20.dp)).padding(20.dp),
        content = content,
    )
}

@Composable
private fun Label(title: String, subtitle: String) {
    Text(title, color = DarkText, fontSize = 17.sp, fontWeight = FontWeight.Medium)
    Spacer(Modifier.height(2.dp))
    Text(subtitle, color = GreyText, fontSize = 13.sp)
}

@Composable
private fun Choice(text: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = text,
        modifier = modifier,
        shape = RoundedCornerShape(14.dp),
        color = if (selected) Accent else Color(0xFFF2F3F5),
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.96f,
    ) {
        Text(text, color = if (selected) Color.White else DarkText, fontSize = 15.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun WideButton(text: String, background: Color, onClick: () -> Unit) {
    ToddlerButton(
        onClick = onClick,
        contentDescription = text,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = background,
        outline = Color.Transparent,
        outlineWidth = 1.dp,
        playTapSound = false,
        pressedScale = 0.97f,
    ) {
        Text(text, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Medium)
    }
}

@Preview
@Composable
private fun ParentSettingsPreview() {
    TotPocketTheme {
        ParentSettingsContent(ParentSettingsUiState(ParentSettings(playLimitMinutes = 15), isPinned = false), onAction = {}, onBack = {}, onHome = {})
    }
}
