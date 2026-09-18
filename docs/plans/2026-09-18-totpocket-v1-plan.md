# TotPocket v1 — Implementation Plan

- **Date:** 2026-09-18
- **Status:** Draft. Home screen and immersive shell are done; everything else is planned.
- **Targets:** Tecno C5-class (low-end, 720p, ~3 GB RAM) and Xiaomi Redmi Note 14 (HyperOS)
- **Stack:** Kotlin Multiplatform + Compose Multiplatform 1.12. UI lives in `shared/commonMain`;
  `androidApp` is a thin shell. iOS compiles but is not a v1 goal.

______________________________________________________________________

## 1. Product principles (these override everything else)

| #   | Principle                     | What it means in practice                                                                                                                              |
| --- | ----------------------------- | ------------------------------------------------------------------------------------------------------------------------------------------------------ |
| P1  | **Calm, not compulsive**      | No autoplay, no streaks, no rewards the child has to chase, no "one more" prompts. Every sound plays only because the child tapped something.         |
| P2  | **Finite screens**            | No scrolling, no endless feeds. Every screen shows everything it has. Paging happens only through big arrow buttons, and page counts are small (≤ 4). |
| P3  | **Can't get lost**            | At most 2 levels below Home. A Home button sits in the same corner on every screen. System Back always goes toward Home and never exits the app.      |
| P4  | **Nothing to read**           | Icons and colours do the work. Text appears only as a secondary label for adults and is never needed to use the app.                                  |
| P5  | **Forgiving input**           | Big targets, taps debounced, no failure states, no penalties. A wrong move in a game gently snaps back.                                               |
| P6  | **Parent in control**         | Everything outside the app, plus settings, sits behind a parent gate that a toddler can't operate by chance.                                           |
| P7  | **Offline, private**          | No network permission, no analytics, no ads, no accounts. All media is bundled.                                                                        |

______________________________________________________________________

## 2. Design system

The code lives in `shared/commonMain/.../ui/theme/TotPocketTheme.kt` (`TotPocketColors`, `TotPocketDimens`).

### 2.1 Colour

| Token        | Hex       | Use                                   | Content colour | Contrast |
| ------------ | --------- | ------------------------------------- | -------------- | -------- |
| `Background` | `#FFF8E7` | Every screen's background (warm, not bright white) | `Outline`      | ≥ 15:1   |
| `Red`        | `#D32F2F` | Calls                                 | White          | ≈ 5.0:1  |
| `Blue`       | `#1565C0` | Gallery                               | White          | ≈ 5.7:1  |
| `Yellow`     | `#FFD600` | Games                                 | `Outline`      | ≈ 13:1   |
| `Green`      | `#2E7D32` | Home / "go" / accept call             | White          | ≈ 5.1:1  |
| `Outline`    | `#1B1B1B` | 6 dp border around every tappable shape | —              | —        |

Rules:

- Each section keeps its colour everywhere: Calls is always red, Gallery blue, Games yellow. The
  colour is how a pre-reader finds their way.
- Never put more than **3 saturated colours** on one screen, not counting photos.
- No gradients, no flashing, and no colour animation faster than 300 ms (photosensitivity safety).

### 2.2 Shape, size, type

| Token            | Value  | Notes                                                   |
| ---------------- | ------ | ------------------------------------------------------- |
| `MinTouchTarget` | 96 dp  | About twice Material's 48 dp. No tappable element is smaller. |
| `CardCorner`     | 48 dp  | Big, friendly radii. Buttons are circles.               |
| `CardOutline`    | 6 dp   | A thick near-black outline marks something as tappable. |
| `ScreenPadding`  | 24 dp  | Keeps targets away from curved edges and gesture zones. |
| `CardSpacing`    | 24 dp  | Gap between targets, so a fat-finger tap can't hit two. |
| `IconSize`       | 120 dp | Primary glyphs.                                         |
| `LabelSize`      | 36 sp  | Adult-facing labels only, `FontWeight.Black`.           |

Iconography: filled, chunky glyphs built in code as `ImageVector`s (`ui/icons/TotPocketIcons.kt`),
so no icon dependency is needed. Add a glyph there; don't pull in `material-icons-extended`.

### 2.3 Motion and feedback

- **Press:** scale to 0.92 with a bouncy spring, read in `graphicsLayer {}` (a deferred read, so no
  recomposition per frame). No ripple on large cards.
- **Tap:** haptic tick (`LongPress` type; `TextHandleMove` is too weak on low-end vibrators).
- **Screen change:** `AnimatedContent` crossfade plus a slight scale, 250 ms. No slides, because
  direction means nothing to a toddler.
- **Sound:** every tap target has a short (≤ 400 ms) soft "boop". Content audio (animal sounds) is
  capped at 3 s. The master volume ceiling is enforced in-app (§6.3).

### 2.4 Layout

- Full-screen immersive: system bars hidden, content drawn under the display cutout (`MainActivity`).
- Every screen splits the whole viewport between its targets using `weight(1f)`. It never scrolls
  and never lays out anything off-screen.
- Adapt by aspect ratio (`BoxWithConstraints`): stack in portrait, go side by side in landscape.
  Grids are 2×3 in portrait and 3×2 in landscape.

______________________________________________________________________

## 3. Screens and navigation

### 3.1 Route map

```text
Home ─┬─ Calls ──── Contacts ──► Ringing ──► InCall ──► (auto) Contacts
      ├─ Gallery ── Categories ──► SoundGrid (paged, ≤4 pages)
      ├─ Games ──── GamePicker ──► ShapeMatch
      └─ (parent gate) ──► ParentSettings
```

| Route (`sealed interface Route`) | Screen composable          | ViewModel                 |
| -------------------------------- | -------------------------- | ------------------------- |
| `Route.Home`                     | `TotPocketHomeScreen` ✅   | — (stateless)             |
| `Route.Calls.Contacts`           | `CallContactsScreen`       | `CallsViewModel`          |
| `Route.Calls.Active(contactId)`  | `CallScreen` (Ringing → InCall → Ended phases) | `CallViewModel`           |
| `Route.Gallery.Categories`       | `GalleryCategoriesScreen`  | — (static list)           |
| `Route.Gallery.Grid(category)`   | `SoundGridScreen`          | `SoundGridViewModel`      |
| `Route.Games.Picker`             | `GamePickerScreen`         | — (static list)           |
| `Route.Games.ShapeMatch`         | `ShapeMatchScreen`         | `ShapeMatchViewModel`     |
| `Route.Parent.Settings`          | `ParentSettingsScreen`     | `ParentSettingsViewModel` |

Ringing, InCall and Ended are **phases of one route**, not separate routes. That keeps Back simple:
one pop always leaves the call.

### 3.2 Navigator

- A hand-rolled `Navigator` in `commonMain/navigation/` holds a `SnapshotStateList<Route>` back
  stack that starts as `[Home]`. It exposes `push(route)`, `pop()` and `popToHome()`. It is saved
  across process death with a `Saver`, since `Route`s are serialisable.
- Don't add a navigation library for v1. The graph is 8 routes and at most 3 deep.
- System Back goes through `PlatformBackHandler` (expect/actual): `pop()` when the stack has more
  than one entry, otherwise swallow. **The app never finishes on Back.**
- The Home button (top-start, 120 dp circle, green house) calls `popToHome()`. It appears on every
  route except Home.

______________________________________________________________________

## 4. Screen specs and interactions

### 4.1 Home (`TotPocketHomeScreen`) — ✅ implemented

- Three cards (Calls red phone, Gallery blue paw, Games yellow puzzle) share the whole screen.
- Tap: bounce, haptic, boop, then push the section route.
- **Parent gate entry:** hold a finger on an invisible 96 dp zone in the top-end corner for
  3 s. A ring fills during the hold, so adults can see the progress.

### 4.2 Calls

**`CallContactsScreen`:** a 2×2 grid of "people" cards (Mum, Dad, Grandma, Doggo). Each shows a big
round photo or illustrated avatar. Parents can swap the photos later (v2).

- Tap a contact to push `Calls.Active(id)`. The screen opens in the Ringing phase.

**`CallScreen`**, driven by its `phase`:

| Phase   | Visual                                                                   | Interaction                                                                                        |
| ------- | ------------------------------------------------------------------------ | -------------------------------------------------------------------------------------------------- |
| Ringing | The avatar pulses gently (scale 1.0↔1.05, 1 s period); soft ringtone loop capped at 10 s | A big green **accept** circle (bottom-start) and a big red **hang-up** circle (bottom-end). No swipe-to-answer; toddlers tap. |
| InCall  | Avatar plus a wiggly "voice" bar; a timer shown as a filling ring, with no digits | A pre-recorded friendly greeting plays once ("Hi sweetie! What are you doing?"), then silence with occasional "mm-hmm" responses on a 4–8 s random cadence. Red hang-up button. Auto-ends after 60 s. |
| Ended   | Avatar waves, "bye-bye" audio                                            | After 2 s, automatically returns to Contacts.                                                      |

- Also add a **pretend dialer** as the 5th tile on Contacts: a 3×4 keypad of huge coloured digits.
  Each press plays that digit's tone and shows a big dot (not the number) in the display. The call
  button connects to a "silly voice" contact.

### 4.3 Gallery

**`GalleryCategoriesScreen`:** three cards (Animals 🐮, Nature 🌧️, Flowers 🌼), using the same card
component as Home.

**`SoundGridScreen`:**

- A 2×3 grid of photos, with at most 4 pages per category. Big left/right arrow buttons sit on the
  sides in landscape or the bottom in portrait, and they're hidden at the first and last page. The
  page indicator is dots.
- Tap a tile: the tile grows to 1.1× and a 6 dp white glow ring appears, and its clip plays. Tapping
  another tile **stops the current clip** and plays the new one; sounds never overlap. Tapping the
  same tile again restarts it.
- Clips don't chain automatically. After a clip ends, nothing happens until the next tap (P1).
- Assets live in `shared/commonMain/composeResources/{drawable,files}/gallery/<category>/<id>.{webp,ogg}`.
  Images are WebP at no more than 512 px on the long edge; audio is OGG mono at 64 kbps.

### 4.4 Games

**`GamePickerScreen`:** v1 has one game tile, plus up to 2 "coming soon" tiles that stay hidden
until a game ships.

**`ShapeMatchScreen`:**

- The top half has 3 outlined **holes** (circle, square, star). The bottom half has the 3 matching
  **filled shapes**, shuffled. Each shape keeps its colour across rounds.
- Drag a shape. Dropped within 60 dp of its matching hole, it snaps in with a spring, a "pop" sound
  and a haptic. Dropped anywhere else, it springs back to where it started, silently: no buzzer,
  no ✗ (P5).
- When all 3 are placed, a 1.5 s calm celebration plays (the shapes bounce once and a gentle chime
  sounds; no confetti storm). The next round then starts with a new shape set, taken from a pool of
  6 shapes.
- There is no score, timer or level counter. After 5 rounds the game shows a "well done, all done"
  screen with only a Home button, as a natural stopping point.
- Drag uses `Modifier.pointerInput { detectDragGestures }` with the offset held in an
  `Animatable<Offset>`, read in `graphicsLayer`/`offset {}` (lambda form only).

### 4.5 Parent area

**Gate:** the 3 s hold on the Home corner opens a simple arithmetic challenge (for example,
"7 + 5 = ?") with 3 answer buttons. A toddler can't solve it; an adult can in a second.

**`ParentSettingsScreen`:**

- Volume ceiling slider (default 60 %)
- Session limit: off, 10, 15 or 20 min (§6.2)
- Pin / unpin the app (lock task, §6.1)
- Exit app

______________________________________________________________________

## 5. Architecture and ViewModel rules

### 5.1 Layout

```text
shared/src/commonMain/kotlin/io/github/kabirnayeem99/totpocket/
├── App.kt                      # root: theme + Navigator + route switch
├── PlatformBackHandler.kt      # expect
├── navigation/                 # Route, Navigator
├── ui/theme/  ui/icons/  ui/components/   # tokens, glyphs, shared cards/buttons
├── audio/                      # SoundPlayer interface (expect/actual or injected)
├── home/                       # TotPocketHomeScreen
├── calls/                      # CallContactsScreen, CallScreen, CallViewModel, …
├── gallery/                    # GalleryCategoriesScreen, SoundGridScreen, SoundGridViewModel
├── games/shapematch/           # ShapeMatchScreen, ShapeMatchViewModel, ShapeMatchEngine
└── parent/                     # ParentGate, ParentSettingsScreen, ParentSettingsViewModel
```

Organise by **feature package, not layer**. v1 stays a single `shared` module; split into Gradle
modules only when build time demands it.

### 5.2 ViewModel rules

1. **Add a ViewModel only when a screen has state that must survive recomposition or config change
   *and* has logic.** Static screens (Home, Categories, GamePicker) are plain composables that take
   callbacks.
1. **One `StateFlow<XxxUiState>` per ViewModel.** `XxxUiState` is an `@Immutable data class`, with
   `ImmutableList` or plain `List` behind `@Immutable` for collections. Back it with a private
   `MutableStateFlow` and mutate only through `_state.update { it.copy(...) }`, never
   `_state.value = _state.value.copy(...)`.
1. **One entry point for input:** `fun onAction(action: XxxAction)`, where `XxxAction` is a
   `sealed interface`. Don't give the UI a public method per button.
1. **One-shot effects (navigate, play a sound) go through a `Channel<XxxEffect>(BUFFERED)` exposed
   as `receiveAsFlow()`.** Don't use `SharedFlow(replay = 0)` for effects (it drops them with no
   collector), and don't encode effects as state flags that the UI has to reset.
1. **Keep platform types out of a ViewModel:** no `Context`, `MediaPlayer` or `Uri`. Platform
   services come in as constructor interfaces (`SoundPlayer`, `Clock`, `Random`), so every
   ViewModel is unit-testable in `commonTest`.
1. **Put game and call rules in plain classes** (`ShapeMatchEngine`, `CallScript`) with pure
   functions. The ViewModel orchestrates; the engine decides. Engines get exhaustive unit tests.
1. **Coroutines** run only in `viewModelScope`. Inject a `CoroutineDispatcher` for anything heavy,
   and never use `GlobalScope` or `runBlocking`. Timers (the call auto-end, the in-call "mm-hmm"
   cadence) are `delay` loops in `viewModelScope`, cancelled by phase changes.
1. **Screen/content split:** `XxxScreen(viewModel)` collects state and effects, then calls a
   stateless `XxxContent(state, onAction)`. Previews and UI tests target `XxxContent` only.
1. **Collect with `collectAsStateWithLifecycle()`** so audio timers pause in the background.
1. **Construction** uses the `viewModel { XxxViewModel(deps…) }` factory lambda with dependencies
   from a hand-written `AppContainer`, supplied at the root through a `CompositionLocal`. v1 has no
   DI framework.

Example shape:

```kotlin
@Immutable
data class SoundGridUiState(
    val category: GalleryCategory,
    val page: Int,
    val pageCount: Int,
    val tiles: List<SoundTile>,
    val playingTileId: SoundTileId?,
)

sealed interface SoundGridAction {
    data class TileTapped(val id: SoundTileId) : SoundGridAction
    data object NextPage : SoundGridAction
    data object PreviousPage : SoundGridAction
}

class SoundGridViewModel(
    category: GalleryCategory,
    private val repository: GalleryRepository,
    private val player: SoundPlayer,
) : ViewModel() {
    private val _state = MutableStateFlow(repository.initialState(category))
    val state: StateFlow<SoundGridUiState> = _state.asStateFlow()

    fun onAction(action: SoundGridAction) { /* when (action) … */ }

    override fun onCleared() = player.stop()
}
```

`SoundTileId`, `ContactId` and similar are `@JvmInline value class` wrappers around `String`.

### 5.3 Audio

- `interface SoundPlayer { fun play(sound: SoundRef); fun stop(); fun setCeiling(fraction: Float) }`
- The Android implementation uses `SoundPool` for boops (≤ 400 ms, preloaded) and a single
  `MediaPlayer` for content clips. That enforces one content clip at a time.
- The volume ceiling is applied as the player's own volume, never by changing system volume.

______________________________________________________________________

## 6. Safety and device integration

### 6.1 Staying inside the app

| Layer                  | Mechanism                                                                                                            | Status  |
| ---------------------- | -------------------------------------------------------------------------------------------------------------------- | ------- |
| System bars            | `WindowInsetsControllerCompat.hide(systemBars())`, `BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE`, re-hidden in `onWindowFocusChanged` | ✅ done |
| Back                   | `PlatformBackHandler(enabled = true)` never lets Back finish the activity                                            | ✅ done |
| Home / Recents         | **Screen pinning**: `Activity.startLockTask()` from the parent settings. Android asks the parent to confirm once, and unpinning needs Back+Recents plus the device PIN. | v1      |
| Home / Recents (hard)  | **Device-owner lock task**: a `DeviceAdminReceiver` plus `dpm set-device-owner`, then `setLockTaskPackages`. Pinning then needs no prompt and can't be escaped. For dedicated hand-me-down devices only, since it needs a factory-reset device. | v2 opt-in |
| Accessibility service  | **Not used.** Google Play restricts `AccessibilityService` to real accessibility use, and it can't block Home anyway. | rejected |

OEM notes: on HyperOS (Note 14) screen pinning lives under *Settings → Additional settings →
Privacy → Screen pinning*, and on HiOS (Tecno) under *Settings → Security → Screen pinning*. The
parent settings screen should deep-link to `Settings.ACTION_SECURITY_SETTINGS` when
`startLockTask()` finds pinning disabled.

### 6.2 Session limits

- The optional session limit counts only foreground time. When it runs out, a "sleepy" wind-down
  screen appears: the palette dims, a lullaby plays for 10 s, and then a static "all done" moon
  screen stays up and only the parent gate dismisses it.
- No countdown is shown to the child.

### 6.3 Sensory limits

- In-app volume ceiling: default 60 %, set by the parent.
- No strobe: no flashes, and nothing moving faster than 3 Hz.
- `FLAG_KEEP_SCREEN_ON` only during an active call, so the device can sleep in other screens.

### 6.4 Permissions

Only `VIBRATE`. No `INTERNET`, no storage, no contacts, and `CALL_PHONE` is never requested.

______________________________________________________________________

## 7. Performance budget (Tecno C5-class)

- Cold start under 1.5 s. Add a baseline profile once there are 3 or more screens.
- Keep all images ≤ 512 px WebP, decoded at display size, and keep no more than 6 bitmaps live
  (one grid page).
- Keep RSS under 150 MB.
- Frame-rate state (drag offset, press scale, pulse) is only ever read in layout/draw lambdas.
- Enable Compose compiler stability reports in CI. Every `UiState` must be stable.

______________________________________________________________________

## 8. Testing

| Level               | What                                                                                   | Where                    |
| ------------------- | -------------------------------------------------------------------------------------- | ------------------------ |
| Unit                | `ShapeMatchEngine`, `CallScript`, `Navigator`, every ViewModel with a fake `SoundPlayer`, using Turbine for flows | `commonTest`             |
| Compose UI          | Each `XxxContent`: targets ≥ 96 dp (`assertWidthIsAtLeast`), content descriptions present, taps call `onAction` | `androidHostTest` (Robolectric) or device tests |
| Manual device check | Immersive survives the pinning prompt and a notification-shade swipe; Back never exits; audio stops on background | Tecno C5, Note 14        |

______________________________________________________________________

## 9. Milestones

| #   | Milestone     | Scope                                                                                     |
| --- | ------------- | ----------------------------------------------------------------------------------------- |
| M0  | Shell ✅      | Theme tokens, icons, `TotPocketHomeScreen`, immersive activity, back swallowing            |
| M1  | Navigation + audio | `Route`/`Navigator`, Home button, `SoundPlayer` (Android), `AppContainer`, tap boops |
| M2  | Gallery       | Categories, `SoundGridScreen` + ViewModel, bundled assets (3 × 6–12 items)                |
| M3  | Calls         | Contacts, `CallScreen` phases, `CallScript`, pretend dialer                               |
| M4  | Games         | `ShapeMatchEngine`, drag and snap, 5-round session, done screen                           |
| M5  | Parent        | Gate, settings, volume ceiling, lock task, session limit and wind-down                    |
| M6  | Polish        | Baseline profile, stability report pass, device QA on both targets                       |

## 10. Open questions

1. Contact avatars: bundled illustrations only, or parent-supplied photos? Photos need a picker and
   storage permission, so this plan defers them to v2.
1. Recorded call voice lines: which language or languages? Assets need to be localised per
   language.
1. Should iOS stay compiling (the expect/actual cost is low), or should `iosMain` be dropped for now?
