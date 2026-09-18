> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Compose Stability Reference

How the Compose compiler decides whether a type is stable, what makes a composable skippable versus
restartable, how strong skipping mode changes those answers, and how to use a stability
configuration file for types you do not own. Load this when a composable will not skip, when a
compiler report marks a parameter unstable, or before adding `@Stable`/`@Immutable` to a type.

Rules that reference stability (HR-1, HR-2, HR-7) live in
[`compose-performance-rules.md`](compose-performance-rules.md). For diagnosing a specific unstable
parameter, the
[`compose-stability-diagnostics`](../.claude/skills/compose-stability-diagnostics/SKILL.md) skill is
the shorter path.

______________________________________________________________________

## Stability Deep Dive

### How the Compose Compiler Determines Stability

The compiler infers stability by analyzing types:

1. **Primitives** (`Int`, `String`, `Boolean`, `Float`, `Long`, `Double`, `Char`) → Always
   **stable**
1. **Function types** (`() -> Unit`, `(Int) -> String`) → Always **stable**
1. **Enum classes** → Always **stable**
1. **Data classes with all `val` immutable properties** → Inferred as **stable**
1. **Data classes with `var` properties** → **UNSTABLE** (cannot track mutations)
1. **Standard collections** (`List`, `Set`, `Map`) → Always **UNSTABLE** (implementation may be
   mutable)
1. **Types from external modules without Compose compiler** → **UNSTABLE** (no stability inference)

### Skippable vs Restartable Functions

| Tag             | Meaning                                                             |
| --------------- | ------------------------------------------------------------------- |
| **Restartable** | Function can be the entry point for recomposition (has state reads) |
| **Skippable**   | Function can be skipped if all parameters are stable and unchanged  |

A composable that is **restartable but NOT skippable** is a performance problem — it will always
recompose when its parent recomposes.

### Strong Skipping Mode

Strong skipping mode (default in Kotlin 2.0.20+) makes **all restartable composables skippable**,
even with unstable parameters. Unstable parameters are compared using instance equality (`===`)
instead of structural equality (`==`).

**Enable for earlier Kotlin versions:**

```kotlin
// build.gradle.kts
composeCompiler {
    enableStrongSkippingMode = true
}
```

**Opt-out for specific composables:**

```kotlin
@NonSkippableComposable
@Composable
fun AlwaysRecompose() {
    // This composable will never be skipped
}
```

**Important:** Even with strong skipping, proper stability annotations are preferred because `===`
comparison for unstable types means new instances always cause recomposition, while stable types use
`==` which compares actual content.

### Stability Configuration File

For types from external libraries that you know are immutable but the compiler cannot verify:

```
// stability_config.conf
// Classes from external libraries to consider stable
java.time.Instant
java.time.LocalDate
java.time.LocalDateTime
kotlinx.datetime.Instant
kotlinx.datetime.LocalDate
kotlinx.collections.immutable.*
```

Configure in build:

```kotlin
// build.gradle.kts
composeCompiler {
    stabilityConfigurationFile = rootProject.layout.projectDirectory.file("stability_config.conf")
}
```

______________________________________________________________________
