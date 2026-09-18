package io.github.kabirnayeem99.totpocket

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.home.TotPocketHomeScreen
import io.github.kabirnayeem99.totpocket.navigation.EntryViewModelStores
import io.github.kabirnayeem99.totpocket.navigation.Navigator
import io.github.kabirnayeem99.totpocket.navigation.Route
import io.github.kabirnayeem99.totpocket.ui.components.LocalTapSound
import io.github.kabirnayeem99.totpocket.ui.theme.TotPocketTheme

/** Root of the shared UI. */
@Composable
fun App(container: AppContainer) {
    val tapSound = remember(container) { { container.soundPlayer.playEffect(Sounds.Boop) } }
    CompositionLocalProvider(
        LocalAppContainer provides container,
        LocalTapSound provides tapSound,
    ) {
        TotPocketTheme {
            TotPocketNavHost()
        }
    }
}

@Composable
private fun TotPocketNavHost() {
    val navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val stores = viewModel { EntryViewModelStores() }
    val savedStates = rememberSaveableStateHolder()

    DisposableEffect(navigator, stores, savedStates) {
        navigator.onEntryRemoved = { entry ->
            stores.clear(entry.id)
            savedStates.removeState(entry.id)
        }
        onDispose { navigator.onEntryRemoved = {} }
    }

    // Always enabled: Back walks toward Home and is swallowed there — it never exits the app.
    PlatformBackHandler(enabled = true) { navigator.pop() }

    AnimatedContent(
        targetState = navigator.current,
        contentKey = { it.id },
        transitionSpec = {
            (fadeIn(tween(250)) + scaleIn(tween(250), initialScale = 0.96f)) togetherWith fadeOut(tween(200))
        },
        label = "screen",
    ) { entry ->
        savedStates.SaveableStateProvider(entry.id) {
            CompositionLocalProvider(LocalViewModelStoreOwner provides stores.ownerFor(entry.id)) {
                RouteContent(entry.route, navigator)
            }
        }
    }
}

@Composable
private fun RouteContent(route: Route, navigator: Navigator) {
    val onHome = navigator::popToHome
    when (route) {
        Route.Home -> TotPocketHomeScreen(onOpen = navigator::push)
        else -> ComingSoonScreen(route, onHome = onHome)
    }
}
