package io.github.kabirnayeem99.totpocket

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.runtime.Composable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.kabirnayeem99.totpocket.parent.AddPhotosScreen
import io.github.kabirnayeem99.totpocket.parent.ParentGateScreen
import io.github.kabirnayeem99.totpocket.parent.ParentSettingsScreen
import io.github.kabirnayeem99.totpocket.session.BedtimeScreen
import io.github.kabirnayeem99.totpocket.session.SessionAction
import io.github.kabirnayeem99.totpocket.session.SessionViewModel
import io.github.kabirnayeem99.totpocket.ui.components.HoldToActivate
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kabirnayeem99.totpocket.audio.Sounds
import io.github.kabirnayeem99.totpocket.calls.CallApp
import io.github.kabirnayeem99.totpocket.calls.CallContactsScreen
import io.github.kabirnayeem99.totpocket.calls.CallScreen
import io.github.kabirnayeem99.totpocket.calls.KeypadScreen
import io.github.kabirnayeem99.totpocket.gallery.AlbumScreen
import io.github.kabirnayeem99.totpocket.gallery.PhotosAlbumsScreen
import io.github.kabirnayeem99.totpocket.gallery.YouTubeScreen
import io.github.kabirnayeem99.totpocket.games.GamePickerScreen
import io.github.kabirnayeem99.totpocket.games.ShapeMatchScreen
import io.github.kabirnayeem99.totpocket.home.LauncherHomeScreen
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
        val settings by container.settingsStore.settings.collectAsStateWithLifecycle()
        KeepSystemBarsHidden(allowed = settings.showSystemBars)
        TotPocketTheme {
            TotPocketNavHost()
        }
    }
}

@Composable
private fun TotPocketNavHost() {
    val container = LocalAppContainer.current
    val navigator = rememberSaveable(saver = Navigator.Saver) { Navigator() }
    val stores = viewModel { EntryViewModelStores() }
    val session = viewModel { SessionViewModel(container.settingsStore, container.soundPlayer) }
    val bedtime by session.bedtime.collectAsStateWithLifecycle()

    // Play time only counts while TotPocket is in front.
    LifecycleResumeEffect(session) {
        session.onAction(SessionAction.Resumed)
        onPauseOrDispose { session.onAction(SessionAction.Paused) }
    }
    // Bedtime closes whatever was open (stopping calls and sounds), then the lullaby plays.
    LaunchedEffect(bedtime) {
        if (bedtime) {
            navigator.popToHome()
            session.onAction(SessionAction.BedtimeShown)
        }
    }
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

    Box(Modifier.fillMaxSize()) {
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
                    RouteContent(entry.route, navigator, onParentUnlocked = { session.onAction(SessionAction.ParentUnlocked) })
                }
            }
        }
        if (bedtime && navigator.current.route !is Route.Parent) {
            PlatformBackHandler(enabled = true) {}
            BedtimeScreen(onParentHold = { navigator.push(Route.Parent.Gate) })
        }
    }
}

@Composable
private fun RouteContent(route: Route, navigator: Navigator, onParentUnlocked: () -> Unit) {
    val onHome = navigator::popToHome
    val onBack: () -> Unit = { navigator.pop() }
    when (route) {
        Route.Home -> LauncherHomeScreen(
            onOpen = navigator::push,
            overlay = {
                // The hidden way in for grown-ups: hold the top-right corner for three seconds. Kept
                // below the top edge, where a press would start the system's pull-down gesture.
                Box(Modifier.fillMaxSize().padding(top = 40.dp), contentAlignment = Alignment.TopEnd) {
                    HoldToActivate(onActivated = { navigator.push(Route.Parent.Gate) })
                }
            },
        )
        Route.Gallery.Categories -> PhotosAlbumsScreen(
            onBack = onBack,
            onHome = onHome,
            onOpenAlbum = { navigator.push(Route.Gallery.Grid(it)) },
        )
        is Route.Gallery.Grid -> AlbumScreen(albumId = route.categoryId, onBack = onBack, onHome = onHome)
        Route.YouTube -> YouTubeScreen(onBack = onBack, onHome = onHome)
        is Route.Calls.Contacts -> CallContactsScreen(
            app = route.app,
            onBack = onBack,
            onHome = onHome,
            onCall = { navigator.push(Route.Calls.Active(it.value, route.app)) },
            onOpenKeypad = { navigator.push(Route.Calls.Keypad) },
        )
        Route.Calls.Keypad -> KeypadScreen(
            onBack = onBack,
            onHome = onHome,
            onCall = { navigator.push(Route.Calls.Active(it.value, CallApp.Phone)) },
        )
        is Route.Calls.Active -> CallScreen(
            contactId = route.contactId,
            app = route.app,
            onHome = onHome,
            onFinished = onBack,
        )
        Route.Games.Picker -> GamePickerScreen(
            onBack = onBack,
            onHome = onHome,
            onOpenShapeMatch = { navigator.push(Route.Games.ShapeMatch) },
        )
        Route.Games.ShapeMatch -> ShapeMatchScreen(onBack = onBack, onHome = onHome)
        Route.Parent.Gate -> ParentGateScreen(
            onBack = onBack,
            onHome = onHome,
            onUnlocked = {
                onParentUnlocked()
                navigator.replace(Route.Parent.Settings)
            },
        )
        Route.Parent.Settings -> ParentSettingsScreen(
            onBack = onBack,
            onHome = onHome,
            onAddPhotos = { navigator.push(Route.Parent.AddPhotos) },
        )
        Route.Parent.AddPhotos -> AddPhotosScreen(onBack = onBack, onHome = onHome)
    }
}
