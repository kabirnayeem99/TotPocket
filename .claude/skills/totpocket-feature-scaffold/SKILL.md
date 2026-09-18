---
name: totpocket-feature-scaffold
description: Use when adding a new TotPocket screen or section (Calls, Gallery, Games, Parent, or a new game) — creates the Route, UiState/Action/Effect, ViewModel, Screen/Content split, AppContainer wiring and test skeleton following the plan's ViewModel rules. Also use when reviewing whether an existing TotPocket screen follows that structure.
---

# TotPocket feature scaffold

The source of truth is `docs/plans/2026-09-18-totpocket-v1-plan.md`, §3 (routes) and §5
(architecture and ViewModel rules). This skill turns those rules into concrete files.

## 0. Does it need a ViewModel?

Give a screen a ViewModel **only** if it has logic *and* state that must survive recomposition or a
config change. Otherwise it's a stateless composable taking callbacks:

| Screen                                                   | ViewModel?                                              |
| -------------------------------------------------------- | ------------------------------------------------------- |
| Home, GalleryCategories, GamePicker                      | No: a static list of cards and an `onXxxClick` callback |
| SoundGrid, Call, ShapeMatch, CallContacts, ParentSettings | Yes                                                     |

## 1. Files

For feature `gallery`, screen `SoundGrid`, under `shared/src/commonMain/kotlin/io/github/kabirnayeem99/totpocket/`:

```text
gallery/
├── SoundGridContract.kt     # UiState, Action, Effect, value-class IDs
├── SoundGridViewModel.kt
├── SoundGridScreen.kt       # SoundGridScreen (stateful) + SoundGridContent (stateless) + @Preview
└── GalleryRepository.kt     # static/bundled data only; no I/O on Main
navigation/Route.kt          # add Route.Gallery.Grid(category)
App.kt                       # add the when-branch for the route
AppContainer.kt              # provide new dependencies
```

Tests live in `shared/src/commonTest/.../gallery/SoundGridViewModelTest.kt` (see the `tester` agent).

## 2. Contract

```kotlin
@JvmInline
value class SoundTileId(val value: String)

@Immutable
data class SoundGridUiState(
    val category: GalleryCategory,
    val page: Int = 0,
    val pageCount: Int = 1,
    val tiles: List<SoundTile> = emptyList(),
    val playingTileId: SoundTileId? = null,
) {
    val hasPrevious: Boolean get() = page > 0
    val hasNext: Boolean get() = page < pageCount - 1
}

sealed interface SoundGridAction {
    data class TileTapped(val id: SoundTileId) : SoundGridAction
    data object NextPage : SoundGridAction
    data object PreviousPage : SoundGridAction
}

sealed interface SoundGridEffect {
    data object NavigateHome : SoundGridEffect
}
```

Rules:

- Derived values (`hasNext`) are computed properties on the state, not separate fields that could
  drift out of sync.
- Keep scores, streaks and counters shown to the child out of `UiState` (plan P1).
- IDs are `@JvmInline value class`es, never raw `String`s.

## 3. ViewModel

```kotlin
class SoundGridViewModel(
    category: GalleryCategory,
    private val repository: GalleryRepository,
    private val player: SoundPlayer,
) : ViewModel() {

    private val _state = MutableStateFlow(repository.initialState(category))
    val state: StateFlow<SoundGridUiState> = _state.asStateFlow()

    private val _effects = Channel<SoundGridEffect>(Channel.BUFFERED)
    val effects: Flow<SoundGridEffect> = _effects.receiveAsFlow()

    fun onAction(action: SoundGridAction) {
        when (action) {
            is SoundGridAction.TileTapped -> playTile(action.id)
            SoundGridAction.NextPage -> changePage(+1)
            SoundGridAction.PreviousPage -> changePage(-1)
        }
    }

    private fun playTile(id: SoundTileId) {
        val tile = _state.value.tiles.firstOrNull { it.id == id } ?: return
        player.stop() // one clip at a time, always
        player.play(tile.sound)
        _state.update { it.copy(playingTileId = id) }
    }

    private fun changePage(delta: Int) {
        player.stop()
        _state.update { repository.page(it, it.page + delta) }
    }

    override fun onCleared() {
        player.stop()
    }
}
```

Checklist:

- [ ] A private `MutableStateFlow`, mutated only with `update { }`
- [ ] One public `onAction`, with no other public mutators
- [ ] One-shot effects go through `Channel(BUFFERED).receiveAsFlow()`, never
      `SharedFlow(replay = 0)` or a boolean in the state
- [ ] No `Context`, `MediaPlayer`, `Uri` or `android.*` imports; platform services are constructor
      interfaces
- [ ] Rules worth unit-testing live in a pure engine class, not here
- [ ] Timers are `viewModelScope.launch { delay(...) }` jobs held in a `Job?` and cancelled on phase
      change
- [ ] `onCleared` stops audio

## 4. Screen / Content split

```kotlin
@Composable
fun SoundGridScreen(
    category: GalleryCategory,
    onHome: () -> Unit,
    container: AppContainer = LocalAppContainer.current,
    viewModel: SoundGridViewModel = viewModel(key = "grid-${category.name}") {
        SoundGridViewModel(category, container.galleryRepository, container.soundPlayer)
    },
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val currentOnHome by rememberUpdatedState(onHome)
    LaunchedEffect(viewModel) {
        viewModel.effects.collect { effect ->
            when (effect) {
                SoundGridEffect.NavigateHome -> currentOnHome()
            }
        }
    }
    SoundGridContent(state = state, onAction = viewModel::onAction)
}

@Composable
fun SoundGridContent(
    state: SoundGridUiState,
    onAction: (SoundGridAction) -> Unit,
    modifier: Modifier = Modifier,
) {
    // Pure rendering: tokens from TotPocketColors/TotPocketDimens, no scrolling, targets ≥ 96 dp.
}

@Preview
@Composable
private fun SoundGridContentPreview() {
    TotPocketTheme { SoundGridContent(state = previewState(), onAction = {}) }
}
```

- `XxxContent` gets the preview and the UI tests. `XxxScreen` never does.
- The `viewModel { }` factory needs a `key` whenever a route argument varies, or two categories
  will share one ViewModel.

## 5. Wiring

- **Route:** `@Serializable sealed interface Route`, with nested sections. Pass arguments in the
  route (`Grid(category)`), never through a global.
- **App.kt:** add a `when` branch. The Home button calls `navigator.popToHome()`.
- **AppContainer:** a plain class that constructs singletons (`SoundPlayer`, repositories), provided
  through `LocalAppContainer` at the root. There's no DI framework.

## 6. Done when

- `./gradlew :androidApp:assembleDebug` passes
- The `toddler-ux-checklist` skill passes for the new screen
- The ViewModel and any engine have `commonTest` tests (`tester` agent)
- The plan doc is updated if the implementation diverged from it
