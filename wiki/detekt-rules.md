> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Detekt & ktlint Rules Reference

This document lists **all active rules** with project-specific thresholds for the Syarahops
codebase. Agents must follow these rules when writing code to avoid post-generation violations.

______________________________________________________________________

## 1. Tool Separation (No Overlap)

| Concern                               | Handled By                              | Config Location            | Auto-fixable?           |
| ------------------------------------- | --------------------------------------- | -------------------------- | ----------------------- |
| Formatting, indentation, spacing      | **ktlint**                              | `.editorconfig`            | Yes (`ktlintFormatAll`) |
| Import ordering, wildcard imports     | **ktlint**                              | `.editorconfig`            | Yes                     |
| Line length (140 chars)               | **ktlint**                              | `.editorconfig`            | Yes                     |
| Trailing commas, function signatures  | **ktlint** (disabled)                   | `.editorconfig`            | N/A                     |
| Complexity, naming, bugs, performance | **Detekt**                              | `config/detekt/detekt.yml` | No                      |
| Coroutine rules                       | **Detekt**                              | `config/detekt/detekt.yml` | No                      |
| Compose rules (21 rules)              | **Detekt** (via `compose-rules-detekt`) | `config/detekt/detekt.yml` | No                      |
| Custom Syarah rules (28 rules)        | **Detekt** (via `detekt-rules` module)  | `config/detekt/detekt.yml` | No                      |

**Key takeaway for agents:** Formatting issues (indentation, spacing, imports, line length) are
auto-fixed by `ktlintFormatAll`. You don't need to worry about those. But **logic rules**
(complexity, naming, Compose, coroutines) are NOT auto-fixable and must be followed when writing
code.

______________________________________________________________________

## 2. Syarah Custom Rules

These custom rules live in the `detekt-rules/` module and target patterns unique to this project —
Compose idioms, layer boundaries and module structure. `SyarahRuleSetProvider` is the registry; the
rules below are the ones most often hit. Each is configured under `syarah-rules:` in
`config/detekt/detekt.yml`.

> **Adding a rule? Stop the Gradle daemon before you trust the result.** Detekt rulesets are loaded
> into a classloader that the daemon caches, so a newly added rule class is silently absent from
> every run until `./gradlew --stop`. `--rerun-tasks`, `--no-configuration-cache` and deleting
> `build/` do **not** dislodge it, and the symptom is a rule that passes its own unit tests, appears
> in `SyarahRuleSetProvider().instance().rules`, and still reports nothing repo-wide.

### NoCrossFeatureImportRule

**What it detects:** a file under `features/<X>/` importing `app.syarah.features.<Y>.*` for any
other feature `<Y>`.

**Why:** `feature-module-ownership.md` §2 bans it outright, and the whole shared-code rule rests on
that ban — the ONLY reason a type is allowed into `core/` is that leaving it in its feature would
force exactly this import. Remove the ban and the rule keeping `core/` honest loses its
justification.

**Why it is separate from `FeatureModuleLayoutRule`:** the two check different things and neither
implies the other. That one checks *layout* — where a file sits. This one checks *direction* — who
may reference whom. The feature-ownership migration reached `FeatureModuleLayoutRule == 0` while
three banned edges had appeared in the inspection cluster, mutually coupling `features/inspections`
and `features/inspectionreport`. Gradle accepted them because the edges landed on different
sub-modules (`data→data` one way, `data→domain` the other), so there was no task-graph cycle and
nothing failed. Nothing caught it until it was read by hand.

**Not findings** — the two legitimate ways to share:

- a domain type both features need moves to `core/` (§2), or
- shared domain UI moves to a capability module such as `features/inspectionreport`'s
  `presentation/ui` (§4c).

**Exempt:** `features/shared`, in both directions — it exists to be depended upon. Note the
exemption lives in the rule, not in `excludes`: excluding its own files would be the wrong
direction, since the point is that *other* features may import it.

`composeApp` and `androidApp` are unaffected — the rule only fires on files whose own path is under
`features/`, and the app shell is expected to depend on every feature.

______________________________________________________________________

### FeatureModuleLayoutRule

**What it detects:** A `.kt` file sitting directly in `features/<name>/src` instead of inside the
feature's `domain/`, `data/` or `presentation/` sub-module.

**Why:** A feature owns its whole stack
([`feature-module-ownership.md`](architecture/feature-module-ownership.md) §1). A flat feature
module has nowhere to put a repository implementation, so the stack ends up in `core/data` and
`core/network` — which is how `core/` accumulated fourteen features' worth of DTOs, DataSources and
repository impls. The layout is upstream of the ownership rule, so the layout is what gets checked.

```
features/tasklisting/src/commonMain/.../TaskListingViewModel.kt   FLAGGED  (flat)
features/addcar/presentation/src/commonMain/.../AddCarScreen.kt   ok       (sub-module)
features/addcar/src/commonMain/.../di/AddCarFeatureModule.kt      ok       (root aggregator)
```

This is a **layout** check, not an import check. A shared type legitimately living in `core/model`,
`core/domain` or a capability module per §2 of the ownership doc is not a finding, and a feature
importing one is normal.

**Config:** `allowedLayers` (default `domain`, `data`, `presentation`), `exemptRootDirectories`
(default `di` — the root aggregator's Koin module is the one file whose job is to see all three
sub-modules at once).

**Excludes:** `features/shared` — a namespace holder with no stack of its own.

**Expect it to be green.** It is scoped to every feature and reports **0** findings repo-wide, down
from 415 at the 2026-09-14 baseline. It got there by migrating, never by adding per-feature
`excludes` — keep it that way. A new finding is a real defect, not tracked debt.

______________________________________________________________________

### ManualEffectHandlingRule

**What it detects:** Direct use of `LaunchedEffect` to collect UI effect flows manually.

**Why:** The project has a `LaunchedUiEffectHandler` composable that encapsulates the correct
pattern. Manual `LaunchedEffect` for UI effects is error-prone and inconsistent.

```kotlin
// WRONG - Manual LaunchedEffect for UI effects
LaunchedEffect(Unit) {
    viewModel.uiEffect.collect { effect ->
        when (effect) { ... }
    }
}

// CORRECT - Use the project's handler
LaunchedUiEffectHandler(viewModel.uiEffect) { effect ->
    when (effect) { ... }
}
```

**Excludes:** `LaunchedUiEffectHandler.kt` itself.

______________________________________________________________________

### ImmutableStateCollectionRule

**What it detects:** Using standard `List`, `Set`, or `Map` properties in classes annotated with
`@Immutable`.

**Why:** Standard collections are always unstable in Compose, preventing recomposition skipping. Use
`kotlinx.collections.immutable` types instead.

```kotlin
// WRONG - List is unstable in @Immutable class
@Immutable
data class MyUiState(
    val items: List<Item> = emptyList()
)

// CORRECT - ImmutableList is stable
@Immutable
data class MyUiState(
    val items: ImmutableList<Item> = persistentListOf()
)
```

**Mapping:** `List` -> `ImmutableList`, `Set` -> `ImmutableSet`, `Map` -> `ImmutableMap`

______________________________________________________________________

### ComputedPropertyInImmutableStateRule

**What it detects:** Computed `get()` properties inside `@Immutable` data classes.

**Why:** Computed properties run on every read during recomposition, defeating `@Immutable`'s
purpose. They also don't participate in `equals()` checks, so Compose can't skip based on them. Use
the `withDerivedState()` pattern instead.

```kotlin
// WRONG - Computed get() in @Immutable class
@Immutable
data class LoginUiState(
    val username: String = "",
    val password: String = "",
) {
    val isLoginEnabled: Boolean
        get() = username.isNotBlank() && password.isNotBlank()  // Runs every read!
}

// CORRECT - Pre-computed stored val + withDerivedState()
@Immutable
data class LoginUiState(
    val username: String = "",
    val password: String = "",
    val isLoginEnabled: Boolean = false,  // Stored val
)

fun LoginUiState.withDerivedState(): LoginUiState = copy(
    isLoginEnabled = username.isNotBlank() && password.isNotBlank(),
)
```

______________________________________________________________________

### NoInitInViewModelRule

**What it detects:** Any `init { }` block on a class whose name ends in `ViewModel`.

**Why:** `init { }` runs synchronously on ViewModel construction (Compose's `koinViewModel { }` call
site, on the composing thread), competing with the first frame even though the work itself is
async/off-Main. Every ViewModel instead defers startup work to first observation of `uiState` via
`stateInWhileSubscribed`
(`core/common/src/commonMain/kotlin/app/syarah/common/flow/StateFlowExtensions.kt`).

```kotlin
// WRONG - init{} runs at construction time
class FooViewModel(...) : ViewModel() {
    val uiState: StateFlow<FooUiState> = _uiState.asStateFlow()

    init {
        loadData()
    }
}

// CORRECT - deferred to first real subscriber
class FooViewModel(...) : ViewModel() {
    @Volatile
    private var initialized = false

    val uiState: StateFlow<FooUiState> = _uiState
        .onStart { initializeOnFirstObservation() }
        .stateInWhileSubscribed(scope = viewModelScope, initialValue = _uiState.value)

    private fun initializeOnFirstObservation() {
        if (initialized) return
        initialized = true
        loadData()
    }
}
```

See `PostListingViewModel` for a full worked example, and the `viewmodel-main-thread-audit` skill
for the underlying rationale.

______________________________________________________________________

## 3. Compose Rules (via `io.nlopez.compose.rules:detekt`)

21 active rules from the [compose-rules](https://mrmans0n.github.io/compose-rules/) library. These
are the most commonly violated rules by agents.

### Modifier Rules

| Rule                       | Description                                                  | Quick Fix                                                      |
| -------------------------- | ------------------------------------------------------------ | -------------------------------------------------------------- |
| **ModifierMissing**        | Public composables must accept a `Modifier` parameter        | Add `modifier: Modifier = Modifier` parameter                  |
| **ModifierWithoutDefault** | `Modifier` parameter must have `= Modifier` default          | Add `= Modifier` default value                                 |
| **ModifierNotUsedAtRoot**  | The `modifier` parameter must be applied to the root element | Move `modifier` to the outermost composable                    |
| **ModifierReused**         | Don't pass the same `modifier` to multiple children          | Apply `modifier` only to root, use new `Modifier` for children |
| **ModifierComposable**     | Don't create `@Composable` modifier factory functions        | Use `Modifier.Node` or `composed {}` instead                   |

```kotlin
// WRONG - ModifierMissing
@Composable
fun MyCard(title: String) { ... }

// CORRECT
@Composable
fun MyCard(title: String, modifier: Modifier = Modifier) { ... }

// WRONG - ModifierNotUsedAtRoot
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Column {
        Text("Title", modifier = modifier)  // Not root!
    }
}

// CORRECT
@Composable
fun MyCard(modifier: Modifier = Modifier) {
    Column(modifier = modifier) {
        Text("Title")
    }
}
```

### State & Remember Rules

| Rule                       | Description                                                             | Quick Fix                                               |
| -------------------------- | ----------------------------------------------------------------------- | ------------------------------------------------------- |
| **RememberMissing**        | State created in composition must use `remember`                        | Wrap with `remember { }`                                |
| **RememberContentMissing** | `movableContentOf` / `movableContentWithReceiverOf` must use `remember` | Wrap with `remember { }`                                |
| **MutableStateParam**      | Don't pass `MutableState<T>` as composable parameter                    | Pass the value + callback instead                       |
| **MutableStateAutoboxing** | Use `mutableIntStateOf`/`mutableFloatStateOf` etc. for primitives       | Replace `mutableStateOf(0)` with `mutableIntStateOf(0)` |

```kotlin
// WRONG - MutableStateParam
@Composable
fun Counter(count: MutableState<Int>) { ... }

// CORRECT - Value + callback (state hoisting)
@Composable
fun Counter(count: Int, onCountChange: (Int) -> Unit) { ... }

// WRONG - MutableStateAutoboxing
var count by remember { mutableStateOf(0) }  // Boxes Int to Integer

// CORRECT - No boxing
var count by remember { mutableIntStateOf(0) }
```

### ViewModel Rules

| Rule                    | Description                                               | Quick Fix                                        |
| ----------------------- | --------------------------------------------------------- | ------------------------------------------------ |
| **ViewModelInjection**  | Only inject ViewModels at the top-level screen composable | Move `koinViewModel()` to the screen entry point |
| **ViewModelForwarding** | Don't pass ViewModel instances down the composable tree   | Pass state + callbacks, not the ViewModel        |

```kotlin
// WRONG - ViewModelForwarding
@Composable
fun ParentScreen(viewModel: MyViewModel = koinViewModel()) {
    ChildContent(viewModel = viewModel)  // Don't forward!
}

// CORRECT - Pass state + callbacks
@Composable
fun ParentScreen(viewModel: MyViewModel = koinViewModel()) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ChildContent(
        uiState = uiState,
        onAction = viewModel::onEvent
    )
}
```

### Function Rules

| Rule                     | Description                                                                    | Quick Fix                 |
| ------------------------ | ------------------------------------------------------------------------------ | ------------------------- |
| **ComposableNaming**     | Composables returning `Unit` use PascalCase; returning values use camelCase    | Rename function           |
| **ComposableParamOrder** | Required params first, `modifier` second, optional params, content lambda last | Reorder parameters        |
| **DefaultsVisibility**   | Default values objects (`*Defaults`) should be internal or private             | Add `internal` visibility |
| **PreviewNaming**        | Preview functions should be prefixed/suffixed with "Preview"                   | Rename to `*Preview`      |
| **PreviewPublic**        | Preview functions should be `private`                                          | Add `private` modifier    |

```kotlin
// CORRECT parameter order
@Composable
fun MyButton(
    onClick: () -> Unit,           // 1. Required
    modifier: Modifier = Modifier, // 2. Modifier
    enabled: Boolean = true,       // 3. Optional with default
    content: @Composable () -> Unit // 4. Content lambda last
)

// CORRECT preview naming
@Preview
@Composable
private fun MyButtonPreview() { ... }
```

### Content Rules

| Rule                              | Description                                                                          | Quick Fix                                        |
| --------------------------------- | ------------------------------------------------------------------------------------ | ------------------------------------------------ |
| **ContentEmitterReturningValues** | Composables that emit UI content should not return values                            | Separate content emission from value computation |
| **MultipleContentEmitters**       | A composable should not call multiple content-emitting composables at the same level | Wrap in a layout (`Column`, `Row`, `Box`)        |

### Stability Rules

| Rule                    | Description                                           | Quick Fix                                         |
| ----------------------- | ----------------------------------------------------- | ------------------------------------------------- |
| **UnstableCollections** | Don't use `List`/`Set`/`Map` in composable parameters | Use `ImmutableList`/`ImmutableSet`/`ImmutableMap` |

### Effect Rules

| Rule                                   | Description                                                                  | Quick Fix                                 |
| -------------------------------------- | ---------------------------------------------------------------------------- | ----------------------------------------- |
| **LambdaParameterInRestartableEffect** | Don't pass lambda parameters directly to `LaunchedEffect`/`DisposableEffect` | Use `rememberUpdatedState` for the lambda |

```kotlin
// WRONG - Lambda captured directly in effect
@Composable
fun TimedContent(onTimeout: () -> Unit) {
    LaunchedEffect(Unit) {
        delay(5000)
        onTimeout()  // Stale lambda if parent recomposes!
    }
}

// CORRECT - Use rememberUpdatedState
@Composable
fun TimedContent(onTimeout: () -> Unit) {
    val currentOnTimeout by rememberUpdatedState(onTimeout)
    LaunchedEffect(Unit) {
        delay(5000)
        currentOnTimeout()  // Always latest lambda
    }
}
```

### Material Rules

| Rule          | Description                  | Quick Fix                                       |
| ------------- | ---------------------------- | ----------------------------------------------- |
| **Material2** | Don't import Material 2 APIs | Use Material 3 (`androidx.compose.material3.*`) |

```kotlin
// WRONG
import androidx.compose.material.Button

// CORRECT
import androidx.compose.material3.Button
```

______________________________________________________________________

## 4. Complexity Rules

These have **project-specific thresholds** that differ from detekt defaults.

| Rule                                 | Project Limit | Default | Quick Fix                            |
| ------------------------------------ | ------------- | ------- | ------------------------------------ |
| **CyclomaticComplexMethod**          | **18**        | 14      | Extract helper functions             |
| **LongParameterList** (functions)    | **8**         | 5       | Use data class or builder            |
| **LongParameterList** (constructors) | **10**        | 6       | Group related params into data class |
| **LongMethod**                       | **60 lines**  | 60      | Extract smaller functions            |
| **TooManyFunctions** (file)          | **35**        | 11      | Split into multiple files            |
| **TooManyFunctions** (class)         | **35**        | 11      | Extract collaborators                |
| **TooManyFunctions** (interface)     | **35**        | 11      | Split interface                      |
| **TooManyFunctions** (object/enum)   | **10**        | 11      | Split object                         |
| **NestedBlockDepth**                 | **5**         | 4       | Flatten with early returns / `when`  |
| **ComplexCondition**                 | **4**         | 3       | Extract named boolean variables      |

> **On the 35-function thresholds:** these are deliberately high so that cohesive 1:1 mirrors of an
> external contract (a Firestore collection wrapper, the Remote Config key surface, a Room DAO's
> full CRUD surface) don't each need a per-file exclude or `@Suppress`. They are **not** a licence
> to let a class grow unboundedly — a class approaching 35 functions almost certainly has more than
> one responsibility. Treat ~15 as the review-time smell threshold and split before you reach the
> detekt limit.

**Special configurations:**

- `LongParameterList`: `ignoreDefaultParameters: true`, `ignoreAnnotatedParameter: ['Composable']`
- `LongMethod`: `ignoreAnnotated: ['Composable']` (Composable functions exempt)
- `CyclomaticComplexMethod`: Composable functions are NOT exempt
- Several complexity rules are switched off entirely for test source sets — see
  [Rule-Level Path Excludes](#13-rule-level-path-excludes)

```kotlin
// WRONG - ComplexCondition (5 conditions, limit is 4)
if (a && b || c && d && e) { ... }

// CORRECT - Extract named booleans
val isUserValid = a && b
val hasPermission = c && d && e
if (isUserValid || hasPermission) { ... }
```

______________________________________________________________________

## 5. Naming Rules

| Rule                       | Configuration                          | Notes                                                        |
| -------------------------- | -------------------------------------- | ------------------------------------------------------------ |
| **FunctionNaming**         | `ignoreAnnotated: ['Composable']`      | Allows PascalCase for `@Composable` functions                |
| **TopLevelPropertyNaming** | `constantPattern: '[A-Z][A-Za-z0-9]*'` | PascalCase for Compose constants (e.g., `LocalSyarahColors`) |

______________________________________________________________________

## 6. Style Rules

| Rule                      | Status       | Configuration                                              | Notes                                                                         |
| ------------------------- | ------------ | ---------------------------------------------------------- | ----------------------------------------------------------------------------- |
| **MaxLineLength**         | **Disabled** | N/A                                                        | ktlint handles (140 chars)                                                    |
| **WildcardImport**        | **Disabled** | N/A                                                        | ktlint handles                                                                |
| **MagicNumber**           | Active       | See ignores below                                          | Extensive ignore list                                                         |
| **ReturnCount**           | Active       | `max: 3`                                                   | `excludeGuardClauses: true`, `excludeLabeled: true`                           |
| **ForbiddenComment**      | Active       | Only `STOPSHIP:` blocked                                   | `TODO`/`FIXME`/`HACK` allowed                                                 |
| **UnusedPrivateFunction** | Active       | `ignoreAnnotated: ['Preview']`                             | Preview functions exempt                                                      |
| **UnusedPrivateProperty** | Active       | `allowedNames: '(_\|ignored\|expected\|serialVersionUID)'` |                                                                               |
| **UseCheckOrError**       | Active       |                                                            | Use `check()` instead of `if (!condition) throw IllegalStateException()`      |
| **UseRequire**            | Active       |                                                            | Use `require()` instead of `if (!condition) throw IllegalArgumentException()` |

### MagicNumber Ignores

MagicNumber is active but with extensive ignores to reduce noise:

- `ignoreNumbers: [-1, 0, 1, 2, 0.0, 1.0]`
- `ignoreHashCodeFunction: true`
- `ignorePropertyDeclaration: true`
- `ignoreLocalVariableDeclaration: true`
- `ignoreConstantDeclaration: true`
- `ignoreCompanionObjectPropertyDeclaration: true`
- `ignoreAnnotation: true`
- `ignoreNamedArgument: true`
- `ignoreEnums: true`
- `ignoreAnnotated: ['Composable']`

**In practice:** MagicNumber triggers mostly in **non-property, non-argument, non-composable**
contexts. Use named constants or annotate the containing function.

```kotlin
// WRONG - Magic number in regular function
fun calculateDelay(retryCount: Int): Long {
    return retryCount * 5000L  // 5000 is a magic number
}

// CORRECT - Named constant
private const val RETRY_DELAY_MS = 5000L
fun calculateDelay(retryCount: Int): Long {
    return retryCount * RETRY_DELAY_MS
}

// OK - Named argument (ignored)
padding(horizontal = 16.dp)

// OK - Property declaration (ignored)
val maxRetries = 3

// OK - Inside @Composable (ignored)
@Composable
fun MyComponent() {
    Spacer(modifier = Modifier.height(24.dp))
}
```

______________________________________________________________________

## 7. Potential Bugs

All 6 rules are explicitly active:

| Rule                                          | Description                                                          | Quick Fix                                                            |
| --------------------------------------------- | -------------------------------------------------------------------- | -------------------------------------------------------------------- |
| **DoubleMutabilityForCollection**             | `var` + mutable collection type (e.g., `var list = mutableListOf()`) | Use `val` with mutable collection OR `var` with immutable collection |
| **UnnecessaryNotNullOperator**                | Using `!!` on a non-nullable type                                    | Remove `!!`                                                          |
| **UnnecessarySafeCall**                       | Using `?.` on a non-nullable type                                    | Remove `?`                                                           |
| **UselessPostfixExpression**                  | Postfix `++`/`--` where result is unused                             | Use prefix `++`/`--` or statement form                               |
| **CastToNullableType**                        | Casting to nullable type (e.g., `as String?`)                        | Use `as?` for safe cast or verify type                               |
| **IteratorNotThrowingNoSuchElementException** | Custom `Iterator.next()` not throwing `NoSuchElementException`       | Add proper exception                                                 |

______________________________________________________________________

## 8. Performance

| Rule                                  | Description                                               | Quick Fix                                                    |
| ------------------------------------- | --------------------------------------------------------- | ------------------------------------------------------------ |
| **SpreadOperator**                    | Avoid `*` spread operator on varargs (creates array copy) | Pass array directly or use `toTypedArray()` only when needed |
| **UnnecessaryPartOfBinaryExpression** | Redundant parts in binary expressions (e.g., `x && true`) | Simplify the expression                                      |

```kotlin
// WRONG - SpreadOperator
fun logAll(vararg items: String) { ... }
val list = listOf("a", "b")
logAll(*list.toTypedArray())  // Creates unnecessary copy

// CORRECT - Accept List directly or redesign
fun logAll(items: List<String>) { ... }
logAll(list)
```

______________________________________________________________________

## 9. Coroutines

| Rule                             | Description                                                      | Quick Fix                                                            |
| -------------------------------- | ---------------------------------------------------------------- | -------------------------------------------------------------------- |
| **GlobalCoroutineUsage**         | Don't use `GlobalScope`                                          | Use `viewModelScope`, `lifecycleScope`, or injected `CoroutineScope` |
| **RedundantSuspendModifier**     | `suspend` function that doesn't call any suspending functions    | Remove `suspend` modifier or add suspending call                     |
| **SuspendFunWithFlowReturnType** | `suspend` function returning `Flow` (should be regular function) | Remove `suspend` — Flow is cold and doesn't need it                  |

```kotlin
// WRONG - SuspendFunWithFlowReturnType
suspend fun observeJobs(): Flow<List<Job>> = flow { ... }

// CORRECT - Flow is cold, no suspend needed
fun observeJobs(): Flow<List<Job>> = flow { ... }

// WRONG - GlobalCoroutineUsage
GlobalScope.launch { saveData() }

// CORRECT - Structured concurrency
viewModelScope.launch { saveData() }
```

______________________________________________________________________

## 10. Empty Blocks

All empty block rules are active by default. These flag empty bodies in:

- `catch`, `finally`, `if`/`else`, `when`, `for`, `while`, `do/while`
- `init`, `function body`, `class body`, `secondary constructor`
- `default value` parameter

**Quick fix:** Add a comment explaining why the block is empty, or remove the empty block.

```kotlin
// WRONG
try { riskyOperation() } catch (e: Exception) { }

// CORRECT
try { riskyOperation() } catch (e: Exception) {
    // Intentionally swallowed: operation is best-effort
}
```

______________________________________________________________________

## 11. Exceptions

Default exception rules are active. Key ones:

- Don't catch generic `Exception` unless re-throwing `CancellationException`
- Don't throw generic `Exception` — use specific types
- Don't swallow exceptions silently

______________________________________________________________________

## 12. ktlint Rules

### Active Rules (auto-fixed by `ktlintFormatAll`)

ktlint uses `ktlint_official` code style with these settings:

- **Max line length:** 140
- **Indent:** 4 spaces
- **No wildcard imports** (enforced via `ij_kotlin_packages_to_use_import_on_demand = unset`)
- **Final newline:** required
- **Trailing whitespace:** trimmed

### Disabled ktlint Rules

These rules are **intentionally disabled** in `.editorconfig`. Agents should NOT try to follow these
patterns:

| Disabled Rule                        | Why                                            |
| ------------------------------------ | ---------------------------------------------- |
| `trailing-comma-on-call-site`        | Project style: no trailing commas              |
| `trailing-comma-on-declaration-site` | Project style: no trailing commas              |
| `multiline-expression-wrapping`      | Conflicts with project formatting              |
| `no-blank-line-in-list`              | Project allows blank lines in parameter lists  |
| `string-template-indent`             | Conflicts with project style                   |
| `function-signature`                 | Project allows flexible function signatures    |
| `function-naming`                    | Compose `@Composable` functions use PascalCase |

**Important:** Do NOT add trailing commas to function calls or declarations. This is the most common
mistake.

______________________________________________________________________

## 13. Rule-Level Path Excludes

Some rules are disabled by path in `config/detekt/detekt.yml` rather than by `@Suppress` at each
declaration, so new files matching the pattern don't have to remember to add an annotation. **Code
inside these paths is exempt — but the underlying guidance still applies; use the exemption, don't
lean on it.**

| Rule                                | Excluded paths                                                                                                                 | Why                                                                                                                                                          |
| ----------------------------------- | ------------------------------------------------------------------------------------------------------------------------------ | ------------------------------------------------------------------------------------------------------------------------------------------------------------ |
| `LongParameterList`                 | `**/src/*Test/**`, `**/src/test/**`                                                                                            | Test-fixture builders (`setXContent(...)`) mirror a component's full parameter surface with all-default values                                               |
| `TooManyFunctions`                  | `**/src/*Test/**`, `**/src/test/**`, `**/*ViewModel.kt`                                                                        | One `@Test` per scenario; a ViewModel legitimately covers a screen's whole lifecycle. Matched by naming convention, so every `*ViewModel.kt` is exempt       |
| `LargeClass`                        | `**/src/*Test/**`, `**/src/test/**`, `**/photographer/PhotographerAddCarViewModel.kt`, `**/photographer/BulkAutoTagHandler.kt` | Same rationale; the two named files were previously per-declaration `@Suppress("LargeClass")`                                                                |
| `InjectDispatcher`                  | `**/src/*Test/**`, `**/src/test/**`                                                                                            | Tests legitimately wire real dispatchers (Robolectric, `setQueryCoroutineContext`, `runTest`). Production DI seams are outside these paths and still flagged |
| `Compose.CompositionLocalAllowlist` | `**/designsystem/theme/Theme.kt`                                                                                               | The five `LocalSyarahX` statics are the framework-mandated theme pattern for a CMP design system                                                             |

**Note on `*ViewModel.kt`:** because `TooManyFunctions` is excluded by filename pattern, a ViewModel
has no function-count ceiling at all. Keep ViewModels thin by delegating to `handlers/*Handler.kt` —
see `features/Rules.md`.

`config/detekt/detekt.yml` is authoritative; this table drifts. If you change a threshold or add an
exclude there, update this section in the same commit.
