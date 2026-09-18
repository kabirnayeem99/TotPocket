> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Compose Performance Rules (canonical)

The complete, numbered Compose performance rule set for this repo — CR-1..7 (critical), HR-1..10
(high), MR-1..9 (medium), LR-1..4 (low) — plus the anti-pattern catalogue that illustrates them and
the pre-completion checklist. Load this when you are writing, reviewing or fixing Compose code and
need the actual rule text, the forbidden/correct code pair, or a rule ID to cite in a review.

**This file is the single canonical copy of these rule IDs.** Both
[`.claude/agents/compose-performance.md`](../.claude/agents/compose-performance.md) and
[`.claude/agents/design-system.md`](../.claude/agents/design-system.md) cite these IDs, and they
must mean the same thing in both. The numbering here is authoritative: never renumber an existing
rule, never reuse a retired number, and add a new rule only by appending the next free number in its
severity band. If you find a rule stated in an agent doc that is not here, add it here and link to
it from there — do not restate it locally.

For severity-level definitions, the three-phases model and the symptom → rule triage table, see the
router: [`.claude/agents/compose-performance.md`](../.claude/agents/compose-performance.md).

______________________________________________________________________

## STRICT PERFORMANCE RULES

These rules are **NON-NEGOTIABLE**. Every composable, every state holder, every modifier MUST
comply. Violations cause jank, ANRs, and degraded UX. Severity levels are defined in the router.

______________________________________________________________________

## CRITICAL RULES (Violations Block PR)

### CR-1: NEVER Write to State After Reading It (Backwards Writes)

**Severity: CRITICAL** - Causes infinite recomposition loops.

```kotlin
// FORBIDDEN - Backwards write causes infinite recomposition
@Composable
fun BadComposable() {
    var count by remember { mutableIntStateOf(0) }
    Text("$count")
    count++  // FORBIDDEN! Writing after reading = infinite loop
}

// CORRECT - State changes ONLY in event handlers or LaunchedEffect
@Composable
fun GoodComposable() {
    var count by remember { mutableIntStateOf(0) }
    Button(onClick = { count++ }) { Text("$count") }
}
```

### CR-2: NEVER Perform Heavy Work on the Main Thread During Composition

**Severity: CRITICAL** - Causes ANR if >5s, jank if >16ms per frame.

```kotlin
// FORBIDDEN - Blocking the main thread
@Composable
fun BadScreen() {
    val data = database.querySync()  // FORBIDDEN! Blocking I/O in composition
    Text(data.toString())
}

// CORRECT - Offload to coroutine, observe result
@Composable
fun GoodScreen(viewModel: MyViewModel) {
    val data by viewModel.data.collectAsState()
    Text(data.toString())
}
```

### CR-3: NEVER Allocate Objects Inside Composition Without `remember`

**Severity: CRITICAL** - Creates GC pressure on every recomposition, causes frame drops.

```kotlin
// FORBIDDEN - New allocation every recomposition
@Composable
fun BadDraw() {
    val paint = Paint().apply { color = Color.Blue }  // New object every recomposition!
    Canvas(modifier = Modifier.size(100.dp)) {
        drawRect(color = Color.Blue)
    }
}

// CORRECT - Cached with remember
@Composable
fun GoodDraw() {
    val paint = remember { Paint().apply { color = Color.Blue } }
    Canvas(modifier = Modifier.size(100.dp)) {
        drawRect(color = Color.Blue)
    }
}
```

### CR-4: ALWAYS Profile in Release Mode

**Severity: CRITICAL** - Debug mode disables runtime optimizations, hides real performance.

- **NEVER** trust performance metrics from debug builds
- **ALWAYS** enable R8/ProGuard for performance testing
- **ALWAYS** use `benchmark` build type or release with debuggable signing
- Target **16ms frame budget** (60 FPS) — every frame that exceeds this causes visible jank

### CR-5: ALWAYS Provide Stable Keys in Lazy Layouts

**Severity: CRITICAL** - Without keys, Compose recreates items on every list change instead of
moving them.

```kotlin
// FORBIDDEN - No keys
LazyColumn {
    items(jobs) { job -> JobCard(job) }
}

// CORRECT - Stable unique keys
LazyColumn {
    items(items = jobs, key = { job -> job.id }) { job ->
        JobCard(job)
    }
}
```

### CR-6: ALWAYS Move Work Invoked From UI/Composables Off Main — Unless Main Is Strictly Required

**Severity: CRITICAL** - Any function reachable from a composable body, a UI event callback
(`onClick`, `onValueChange`, `onLongClick`, drag/gesture handlers, etc.), or a
ViewModel/state-holder method triggered by one of those events MUST do its actual work (I/O, file
access, network calls, CPU-bound computation, non-trivial collection processing) on a background
dispatcher — `Dispatchers.IO`/`Dispatchers.Default` or the project's injected
`ioDispatcher`/`defaultDispatcher` — never on Main.

Stay on Main **only** when one of these holds:

- The call synchronously writes Compose `State`/`mutableStateOf` that other code in the same frame
  depends on.
- It calls a platform API that requires Main (launching an `Activity`/`Intent`, a system `Toast`,
  showing a picker/camera launcher).
- Moving the read/write off Main would introduce a race — e.g. a non-thread-safe mutable field
  read-then-written without synchronization is actually safer left inline on the single-threaded
  Main dispatcher than "fixed" by a naive background dispatch that now races with itself.

**Prefer self-contained dispatching over relying on the caller.** Wrap the callee itself in
`withContext(dispatcher)` so the function is correct regardless of who calls it, rather than
assuming every call site already runs inside the right coroutine/dispatcher:

```kotlin
// FORBIDDEN - runs wherever the caller happens to be, likely Main from a UI callback
private fun resolveDealerById(id: Long): FilterItem<Long>? =
    allEnrichedUserDealers.find { it.id == id }
        ?: allFirestoreDealers.find { it.id == id }?.let { toFilterItem(it) }

// CORRECT - guaranteed off Main no matter who calls it
suspend fun findDealerById(id: Long): FilterItem<Long>? = withContext(defaultDispatcher) {
    allEnrichedUserDealers.find { it.id == id }
        ?: allFirestoreDealers.find { it.id == id }?.let { toFilterItem(it) }
}
```

```kotlin
// FORBIDDEN - onClick runs the mapping/lookup synchronously on Main
Button(onClick = {
    val filtered = hugeList.filter { it.matches(query) }.map(::toUiModel)
    viewModel.onResult(filtered)
})

// CORRECT - state holder absorbs the UI event, work happens off Main
Button(onClick = viewModel::onSearchClicked)

fun onSearchClicked() {
    viewModelScope.launch(ioDispatcher) {
        val filtered = hugeList.filter { it.matches(query) }.map(::toUiModel)
        updateState { it.copy(results = filtered) }
    }
}
```

This is broader than CR-2 (which only covers blocking calls made *directly inside a composable body
during composition*): CR-6 also covers work that a UI event **triggers** — a ViewModel/handler
function called from `onClick`, a drag callback, a picker's `onResult` — even though that function
itself isn't running inside composition. See `kotlin-coroutines-structured-concurrency` for
scope-ownership rules once the dispatch is in place, and `viewmodel-main-thread-audit` for the
concrete audit method when reviewing/fixing an existing ViewModel or
`features/*/handlers/*Handler.kt` delegate class against this rule — including the two ways a naive
"wrap everything" pass breaks things (classes that are deliberately synchronous by design, and tests
that pin down synchronous timing via manual coroutine-scheduler stepping).

### CR-7: NEVER Use `init { }` on a ViewModel — Defer to First Observation, Surface Readiness via `UiState`

**Severity: CRITICAL** - `init { }` runs synchronously on ViewModel construction, on the
composing/calling thread (`koinViewModel { }`'s call site). ANY work started there — not just a
literal blocking call — competes with whatever else is on-screen for the first frame, even if that
work is itself async/off-Main, and runs unconditionally even for a ViewModel that's constructed but
never actually observed.

`init { }` is banned outright on every `ViewModel` in this codebase — enforced automatically by the
`NoInitInViewModelRule` Detekt rule
(`detekt-rules/src/main/kotlin/app/syarah/detekt/rules/NoInitInViewModelRule.kt`), which flags any
`init { }` block on a class whose name ends in `ViewModel`. Startup work instead goes into a
`initializeOnFirstObservation()` method, fired via `uiState`'s `onStart { }` the moment it gains its
first real collector (Compose's `collectAsStateWithLifecycle()`, or a test's `.collect()`), using
`stateInWhileSubscribed` from `core/common`'s `StateFlowExtensions.kt` — a ViewModel that's
constructed but never observed never runs this work at all.

**If a screen genuinely cannot render anything meaningful until some value resolves**, expose that
as a loading flag on `UiState` (`isLoading: Boolean` / a sealed `UiState.Loading` variant / a
nullable "not yet ready" field) that the screen observes and renders a spinner/skeleton for, and
populate the real value asynchronously via
`viewModelScope.launch(ioDispatcher/defaultDispatcher) { ... ; updateState { it.copy(...) } }`
inside `initializeOnFirstObservation()` — never by blocking construction or gating the screen's
composition on it.

```kotlin
// FORBIDDEN - init{} is not allowed on a ViewModel at all
class ProfileViewModel(private val repo: ProfileRepository) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState = _uiState.asStateFlow()

    init {
        val profile = runBlocking { repo.getProfile() }  // blocks the composing thread
        _uiState.value = ProfileUiState(profile = profile)
    }
}

// CORRECT - startup work is deferred to the moment uiState gains its first real
// subscriber; the screen renders with isLoading = true and updates itself the moment
// the async load lands, via the same UiState the screen already observes
class ProfileViewModel(
    private val repo: ProfileRepository,
    private val ioDispatcher: CoroutineDispatcher,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ProfileUiState(isLoading = true))

    val uiState: StateFlow<ProfileUiState> = _uiState
        .onStart { initializeOnFirstObservation() }
        .stateInWhileSubscribed(scope = viewModelScope, initialValue = _uiState.value)

    @Volatile
    private var initialized = false

    private fun initializeOnFirstObservation() {
        if (initialized) return
        initialized = true

        viewModelScope.launch(ioDispatcher) {
            val profile = repo.getProfile()
            _uiState.update { it.copy(profile = profile, isLoading = false) }
        }
    }
}
```

See `PostListingViewModel` for a full worked example of this pattern (including the `initialized`
guard and its interaction with `WhileSubscribed`'s re-fire-on-resubscribe behavior). This is the
general form of the same "hide behind loading, link to the UI's own state observer instead of
blocking construction" principle applied at the navigation level in `AddCarFlow`'s
`AddCarDestination.Idle` sentinel (a spinner keyed to a real gap — camera/scanner warm-up — instead
of the screen freezing with nothing shown). A `viewModelScope.launch { }` inside
`initializeOnFirstObservation()` that does real, non-trivial computation before its first
`updateState { }` still needs the dispatcher from CR-6
(`launch(ioDispatcher)`/`launch(defaultDispatcher)`, not a bare `launch { }` which inherits
`Dispatchers.Main.immediate`).

______________________________________________________________________

## HIGH SEVERITY RULES (Must Fix Before Merge)

### HR-1: Mark All UI State Data Classes with `@Immutable`

All data classes used as composable parameters or in `StateFlow<UiState>` MUST be annotated.

```kotlin
// REQUIRED for all UiState classes
@Immutable
data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val isLoading: Boolean = false,
    val error: String? = null
)
```

**When to use which annotation:**

| Annotation   | Use When                                                                       | Contract                                             |
| ------------ | ------------------------------------------------------------------------------ | ---------------------------------------------------- |
| `@Immutable` | ALL properties are `val` with immutable types                                  | Properties NEVER change after construction           |
| `@Stable`    | Class has observable mutable state (`mutableStateOf`)                          | Changes are ALWAYS notified via Compose State system |
| None needed  | Primitives (`Int`, `String`, `Boolean`, `Float`), function types, enum classes | Already stable by default                            |

### HR-2: NEVER Use Standard Collections in State — Use Kotlinx Immutable Collections

Standard `List`, `Set`, `Map` are **ALWAYS unstable** because the Compose compiler cannot guarantee
their backing implementation is immutable. This prevents skipping.

```kotlin
// FORBIDDEN - List is unstable, prevents skipping
@Immutable
data class UiState(
    val items: List<Item>  // UNSTABLE! Compose cannot skip
)

// CORRECT - ImmutableList is stable
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.persistentListOf

@Immutable
data class UiState(
    val items: ImmutableList<Item> = persistentListOf()  // STABLE
)
```

**Collection mapping:**

| Standard (UNSTABLE) | Kotlinx Immutable (STABLE)                 |
| ------------------- | ------------------------------------------ |
| `List<T>`           | `ImmutableList<T>` / `PersistentList<T>`   |
| `Set<T>`            | `ImmutableSet<T>` / `PersistentSet<T>`     |
| `Map<K,V>`          | `ImmutableMap<K,V>` / `PersistentMap<K,V>` |

### HR-3: Use `derivedStateOf` for State Derived from Rapidly Changing Sources

When state depends on rapidly changing values (scroll position, animation progress) but only needs
to trigger updates when a condition changes:

```kotlin
// FORBIDDEN - Recomposes on EVERY scroll pixel
@Composable
fun BadScrollList() {
    val listState = rememberLazyListState()
    val showButton = listState.firstVisibleItemIndex > 0  // Recomposes constantly!
    // ...
}

// CORRECT - Only recomposes when boolean actually changes
@Composable
fun GoodScrollList() {
    val listState = rememberLazyListState()
    val showButton by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 0 }
    }
    // ...
}
```

### HR-4: Defer State Reads with Lambda Parameters

Pass state as `() -> T` lambdas instead of `T` values when the state changes frequently. This moves
the read from Composition phase to Layout/Drawing phase.

```kotlin
// FORBIDDEN - Parent recomposes on every scroll
@Composable
fun BadTitle(snack: Snack, scroll: Int) {
    val offset = with(LocalDensity.current) { scroll.toDp() }
    Column(modifier = Modifier.offset(y = offset)) { /* content */ }
}

// CORRECT - State read deferred to Layout phase
@Composable
fun GoodTitle(snack: Snack, scrollProvider: () -> Int) {
    Column(
        modifier = Modifier.offset {
            IntOffset(x = 0, y = scrollProvider())  // Read in Layout, not Composition
        }
    ) { /* content */ }
}
```

### HR-5: Use `graphicsLayer` for ALL Transform Animations

Transform animations (scale, rotation, translation, alpha) MUST use `graphicsLayer` to skip
Composition and Layout phases entirely.

```kotlin
// FORBIDDEN - Triggers recomposition on every animation frame
@Composable
fun BadAnimation(scale: Float) {
    Box(modifier = Modifier.scale(scale))  // Recomposes every frame!
}

// CORRECT - GPU-accelerated, zero recomposition
@Composable
fun GoodAnimation(scaleProvider: () -> Float) {
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = scaleProvider()
            scaleY = scaleProvider()
        }
    )
}
```

### HR-6: Use `remember` for ALL Expensive Calculations

```kotlin
// FORBIDDEN - Sorts on every recomposition
@Composable
fun BadList(contacts: List<Contact>, comparator: Comparator<Contact>) {
    LazyColumn {
        items(contacts.sortedWith(comparator)) { contact -> ContactRow(contact) }
    }
}

// CORRECT - Sorts only when inputs change
@Composable
fun GoodList(contacts: List<Contact>, comparator: Comparator<Contact>) {
    val sortedContacts = remember(contacts, comparator) {
        contacts.sortedWith(comparator)
    }
    LazyColumn {
        items(sortedContacts) { contact -> ContactRow(contact) }
    }
}
```

### HR-7: Ensure Lambda Stability

Lambdas that capture unstable references cause the composable to recompose. Fix with method
references or `remember`.

```kotlin
// PROBLEMATIC - New lambda every recomposition (if viewModel is unstable)
Button(onClick = { viewModel.doSomething() }) { Text("Click") }

// CORRECT - Method reference (always stable)
Button(onClick = viewModel::doSomething) { Text("Click") }

// CORRECT - Remembered lambda
val onClick = remember { { viewModel.doSomething() } }
Button(onClick = onClick) { Text("Click") }
```

### HR-8: Specify `contentType` for Heterogeneous Lazy Layouts

Enables Compose to reuse compositions between items of the same type, dramatically improving scroll
performance.

```kotlin
LazyColumn {
    items(
        items = items,
        key = { it.id },
        contentType = { item ->
            when (item) {
                is ListItem.Header -> "header"
                is ListItem.Job -> "job"
                is ListItem.Ad -> "ad"
            }
        }
    ) { item ->
        when (item) {
            is ListItem.Header -> HeaderRow(item)
            is ListItem.Job -> JobRow(item)
            is ListItem.Ad -> AdRow(item)
        }
    }
}
```

### HR-9: Pre-Compute Derived State with `withDerivedState()` Pattern

**Severity: HIGH** - Computed `get()` properties in `@Immutable` data classes run on every
recomposition read, defeating the purpose of `@Immutable`. Pre-computing ensures equality checks
work correctly and avoids redundant computation.

```kotlin
// FORBIDDEN - Computed get() in @Immutable UiState
@Immutable
data class VinUiState(
    val vin: String = "",
    val isLoading: Boolean = false,
    val isVinValidated: Boolean = false,
) {
    val isConfirmEnabled: Boolean
        get() = vin.length == 17 && !isLoading && isVinValidated  // Runs every read!
}

// CORRECT - Stored vals with withDerivedState() extension
@Immutable
data class VinUiState(
    val vin: String = "",
    val isLoading: Boolean = false,
    val isVinValidated: Boolean = false,
    val isConfirmEnabled: Boolean = false,  // Pre-computed, stored val
)

fun VinUiState.withDerivedState(): VinUiState = copy(
    isConfirmEnabled = vin.length == 17 && !isLoading && isVinValidated,
)

// ViewModel: updateState helper auto-applies withDerivedState()
private inline fun updateState(crossinline transform: (VinUiState) -> VinUiState) {
    _uiState.update { transform(it).withDerivedState() }
}
```

**When to use `withDerivedState()`:**

- UiState has 2+ properties derived from other properties in the same state
- Derived properties involve logic beyond simple field access
- State changes frequently (form input, loading states)

**When `get()` is OK:**

- Single trivial derivation (`val displayName get() = name.ifEmpty { "Unknown" }`)
- State class is NOT `@Immutable` annotated
- Property is rarely read

### HR-10: Use `derivedStateOf` for PagerState/ScrollState Boolean Conditions

**Severity: HIGH** - Reading `pagerState.currentPage` directly in composition causes recomposition
on every page swipe. When the result is a boolean condition, `derivedStateOf` limits recomposition
to only when the boolean actually changes.

```kotlin
// FORBIDDEN - Recomposes on EVERY page swipe (5 pages → 5 recompositions)
val isImageTab = pagerState.currentPage == 3 || pagerState.currentPage == 4

// CORRECT - Only recomposes when boolean actually changes (2 transitions)
val isImageTab by remember {
    derivedStateOf { pagerState.currentPage == 3 || pagerState.currentPage == 4 }
}
```

**Applies to ANY rapidly-changing Compose state read that produces a derived condition:**

- `pagerState.currentPage` → boolean tab type
- `lazyListState.firstVisibleItemIndex` → show/hide scroll-to-top
- `scrollState.value` → collapsed/expanded header
- `textFieldState.text.length` → enable/disable button

### HR-11: Use `rememberUpdatedState` for Long-Lived Lambdas

**Severity: HIGH** - A stale captured lambda produces a wrong-callback bug, not just a slow frame.

In a `LaunchedEffect`, a `DisposableEffect`, or any long-lived callback holder, read the lambda
through `rememberUpdatedState` so the effect always invokes the latest one without restarting.

```kotlin
@Composable
fun TimedContent(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(5000)
        currentOnTimeout()  // Always calls the latest lambda
    }
}
```

**Numbering note — severity conflict, resolved HIGH.** This rule was filed twice with opposite
merge-blocking advice: LR-4 (LOW, "recommended") in `.claude/agents/compose-performance.md` and HR-7
(HIGH, "must fix before merge") in `.claude/agents/design-system.md`. The HIGH reading wins, and it
lives here as **HR-11**; LR-4 is retired and its number is never reused. Note that HR-7 in this
canonical file is **lambda stability**, a different rule — do not conflate the two.

Reason: the failure mode is a *stale callback firing* — wrong navigation target, wrong item acted
on, an effect calling into a disposed screen's handler. That is a correctness defect that merely
happens to be fixed by a performance-adjacent API; the rest of the LR tier is genuine
micro-optimization (cache modifier chains, delegation syntax), and a rule whose failure mode is "the
wrong thing happens" does not belong beside them.

Evidence (111 call sites across `core/`, `features/` and `composeApp/`, with the design system's own
primitives depending on it for correctness rather than frame time):

1. `core/designsystem/.../util/ThrottledClick.kt#rememberThrottledOnClick` returns a `remember { }`
   closure with **no keys** — without `rememberUpdatedState` it captures the *first* `onClick` for
   the composable's whole lifetime and keeps invoking the stale one. Every `SyarahButton` /
   `SyarahChip` / `SyarahTag` click and every throttled navigation goes through it.
1. `core/designsystem/.../util/LaunchedUiEffectHandler.kt` is the same shape:
   `LaunchedEffect(effect)` is keyed only on `effect`, so a changed `onEffect`/`onConsumeEffect`
   would never be observed.
1. Systemic, not incidental: `composeApp/.../PendingDeepLinkEffect.kt` (6 uses),
   `core/care/.../ShakeDetectionEffect.android.kt`, `core/paging/.../ListingState.kt`, and
   `core/designsystem/.../SyarahHaptics.kt` — whose KDoc makes always-latest a **documented contract
   to callers** ("the latest [onClick] is always invoked (via [rememberUpdatedState]), so callers
   don't need to…"), so dropping the pattern silently breaks a published guarantee.
1. Detekt already enforces the shape: `LambdaParameterInRestartableEffect` is enabled in
   `config/detekt/detekt.yml`, which makes it CI-blocking regardless of what a doc said.

______________________________________________________________________

## MEDIUM SEVERITY RULES (Should Fix)

### MR-1: Flatten Layout Hierarchies

Deep nesting causes exponential measurement cost ("double taxation").

```kotlin
// AVOID - Deep nesting
Column {
    Row {
        Column {
            Row { Box { Text("Deeply nested") } }
        }
    }
}

// PREFER - Flat structure
Box(modifier = Modifier.fillMaxSize()) {
    Text("Flattened", modifier = Modifier.align(Alignment.Center))
}
```

### MR-2: Set Fixed Sizes on Image Composables

Allows image loaders (Coil, Glide) to downsample before hitting the UI thread.

```kotlin
// CORRECT - Fixed size allows downsampling
AsyncImage(
    model = imageUrl,
    contentDescription = null,
    modifier = Modifier.size(width = 120.dp, height = 80.dp),  // Fixed size!
    contentScale = ContentScale.Crop
)
```

### MR-3: Pre-size Lazy Layout Items When Possible

Fixed item sizes improve scroll performance by avoiding per-item measurement.

```kotlin
LazyColumn {
    items(items = jobs, key = { it.id }) { job ->
        JobCard(
            job = job,
            modifier = Modifier
                .fillMaxWidth()
                .height(120.dp)  // Fixed height improves scroll perf
        )
    }
}
```

### MR-4: Avoid Nested Scrollables Without Fixed Dimensions

```kotlin
// FORBIDDEN - Unbounded nested scrollable
LazyColumn {
    item { LazyRow { /* items */ } }  // No height constraint!
}

// CORRECT - Fixed height for nested scrollable
LazyColumn {
    item {
        LazyRow(modifier = Modifier.height(200.dp)) { /* items */ }
    }
}
```

### MR-5: Use Lambda-Based Modifiers for Frequently Changing State

Lambda modifiers (`Modifier.offset { }`, `Modifier.drawBehind { }`) skip the Composition phase.

```kotlin
// AVOID - Recomposes on every offset change
Box(modifier = Modifier.offset(y = offset))

// CORRECT - Skips composition, runs only Layout phase
Box(modifier = Modifier.offset { IntOffset(x = 0, y = offsetProvider()) })
```

### MR-6: Prefer `Modifier.Node` Over Composed Modifiers

For custom modifiers, `Modifier.Node` API (Compose 1.3+) is more performant than `composed {}`.

```kotlin
// More performant than composed modifiers
class CircleModifierNode : Modifier.Node(), DrawModifierNode {
    override fun ContentDrawScope.draw() {
        drawCircle(Color.Blue)
        drawContent()
    }
}
```

### MR-7: Use `SubcomposeLayout` Sparingly

`SubcomposeLayout` defers composition, which is powerful but expensive. Use only when you need to
measure children before deciding what to compose.

### MR-8: Avoid Intrinsic Measurements in Performance-Critical Paths

```kotlin
// Can be expensive for complex layouts
Modifier.width(IntrinsicSize.Max)
Modifier.height(IntrinsicSize.Min)
```

### MR-9: Modifier Order Matters

```kotlin
// Touch target INCLUDES padding (larger touch area)
Modifier.clickable { }.padding(16.dp)

// Touch target EXCLUDES padding (smaller touch area)
Modifier.padding(16.dp).clickable { }
```

______________________________________________________________________

## LOW SEVERITY RULES (Recommended)

### LR-1: Cache Reusable Modifier Chains

```kotlin
// AVOID - New Modifier chain on every recomposition
@Composable
fun BadModifier(onClick: () -> Unit) {
    Box(modifier = Modifier.size(48.dp).background(Color.Blue).clickable { onClick() })
}

// PREFER - Reuse static modifier chains
private val baseModifier = Modifier.size(48.dp).background(Color.Blue)

@Composable
fun GoodModifier(onClick: () -> Unit) {
    Box(modifier = baseModifier.clickable { onClick() })
}
```

### LR-2: Use `collectAsState()` with Delegation

```kotlin
// AVOID - Eagerly reads .value
val items = viewModel.items.collectAsState().value

// PREFER - Delegated read (defers access)
val items by viewModel.items.collectAsState()
```

### LR-3: Choose the Right Animation API

| Scenario             | API                            | Performance   |
| -------------------- | ------------------------------ | ------------- |
| Simple value change  | `animate*AsState()`            | Best          |
| Multiple coordinated | `updateTransition()`           | Great         |
| Infinite animations  | `rememberInfiniteTransition()` | Use sparingly |
| Gesture-driven       | `Animatable` + coroutines      | Best control  |

### LR-4: RETIRED — promoted to HR-11

"Use `rememberUpdatedState` for long-lived lambdas" was promoted from LOW to HIGH and now lives at
**HR-11**. The number LR-4 is retired and is never reused; see HR-11 for the rule text and for why
the severity conflict between the two agent docs was resolved this way.

______________________________________________________________________

## Common Anti-Patterns Reference

### Anti-Pattern 1: Reading State Too Early

```kotlin
// BAD
val items = viewModel.items.collectAsState().value  // Eagerly reads

// GOOD
val items by viewModel.items.collectAsState()  // Delegated, deferred
```

### Anti-Pattern 2: Creating Objects in Composition

Allocating inside a composable body without `remember` — a new object on every recomposition. See
**CR-3** above for the forbidden/correct pair; not repeated here.

### Anti-Pattern 3: Unstable Collections in State

A plain `List`/`Set`/`Map` in a UI state class, which keeps the composable from skipping. See
**HR-1** and **HR-2** above for the forbidden/correct pair; not repeated here.

### Anti-Pattern 4: Backwards Write in Click Throttling

```kotlin
// BAD - Backwards write causes recomposition
@Composable
fun BadButton(onClick: () -> Unit) {
    var lastClickTime by remember { mutableLongStateOf(0L) }
    val throttledClick = remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime > 300L) {
                lastClickTime = currentTime  // Backwards write!
                onClick()
            }
        }
    }
    Button(onClick = throttledClick) { Text("Click") }
}

// GOOD - Use reference to avoid backwards write
@Composable
fun GoodButton(onClick: () -> Unit) {
    val currentOnClick by rememberUpdatedState(onClick)
    val lastClickTimeRef = remember { mutableLongStateOf(0L) }
    val throttledClick = remember {
        {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTimeRef.longValue > 300L) {
                lastClickTimeRef.longValue = currentTime
                currentOnClick()
            }
        }
    }
    Button(onClick = throttledClick) { Text("Click") }
}
```

### Anti-Pattern 5: Using Direct Modifiers for Animations

`Modifier.scale`/`.alpha`/`.rotate` with an animated value, which recomposes every frame instead of
redrawing. See **HR-5** above for the forbidden/correct pair; not repeated here.

### Anti-Pattern 6: Missing Keys in LazyColumn with Reorderable Items

```kotlin
// BAD - Items recreated on reorder
items(jobs) { job -> JobCard(job) }

// GOOD - Items moved, not recreated
items(items = jobs, key = { it.id }) { job -> JobCard(job) }
```

### Anti-Pattern 7: Using `var` in Data Classes Passed to Composables

```kotlin
// BAD - var makes entire type unstable
data class Contact(var name: String, var number: String)

// GOOD - val ensures stability
data class Contact(val name: String, val number: String)
```

______________________________________________________________________

## Performance Checklist

Before completing any optimization task, verify ALL items:

### Recomposition (CRITICAL + HIGH)

- [ ] **CR-1:** No backwards writes in any composable
- [ ] **CR-3:** No unremembered object allocations in composition
- [ ] **HR-1:** All UiState data classes annotated with `@Immutable`
- [ ] **HR-2:** All collections use `kotlinx.collections.immutable` types (`ImmutableList`,
  `ImmutableMap`, `ImmutableSet`)
- [ ] **HR-3:** `derivedStateOf` used for derived state from rapidly changing sources
- [ ] **HR-4:** State reads deferred with lambda parameters where applicable
- [ ] **HR-6:** `remember` wraps all expensive calculations
- [ ] **HR-7:** Lambdas are stable (method references or remembered)
- [ ] **HR-9:** No computed `get()` properties in `@Immutable` UiState — use `withDerivedState()`
  pattern
- [ ] **HR-10:** `derivedStateOf` wraps PagerState/ScrollState boolean conditions

### Lazy Layouts (CRITICAL + HIGH)

- [ ] **CR-5:** Stable, unique keys provided for ALL lazy layout items
- [ ] **HR-8:** `contentType` specified for heterogeneous lists
- [ ] **MR-3:** Fixed item sizes where possible
- [ ] **MR-4:** No nested scrollables without fixed dimensions

### Animations (HIGH + MEDIUM)

- [ ] **HR-5:** `graphicsLayer` used for ALL transform animations
- [ ] **MR-5:** Lambda-based modifiers for frequently changing state
- [ ] **LR-3:** Correct animation API chosen for use case
- [ ] **HR-11:** `rememberUpdatedState` used for lambdas captured by long-lived effects/holders

### General (MEDIUM + LOW)

- [ ] **CR-2:** No heavy work on main thread during composition
- [ ] **CR-6:** Every function invoked from a composable/UI callback dispatches its actual work off
  Main unless Main is strictly required
- [ ] **CR-7:** No ViewModel `init { }` blocks (`runBlocking`/`Thread.sleep`) — readiness surfaced
  via `UiState`, not by delaying construction
- [ ] **CR-4:** Performance profiled in release mode
- [ ] **MR-1:** Layout hierarchy is as flat as possible
- [ ] **MR-2:** Fixed sizes on image composables
- [ ] **MR-9:** Modifier order is intentional
- [ ] Compose compiler reports analyzed for unstable types
- [ ] No `var` properties in data classes passed to composables
