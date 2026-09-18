> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Compose Profiling and Baseline Profiles

How to measure Compose performance rather than guess at it: baseline profiles for startup, enabling
the Compose compiler metrics/reports, reading the generated reports, Layout Inspector recomposition
counts, the benchmark build type, and frame timing. Load this when you need evidence — before and
after numbers, a compiler report, or a recomposition count — not when you already know which rule
was broken.

Profile in release/benchmark builds only (CR-4 in
[`compose-performance-rules.md`](compose-performance-rules.md)). For an end-to-end recomposition
investigation, start with the
[`compose-recomposition-performance`](../.claude/skills/compose-recomposition-performance/SKILL.md)
skill.

______________________________________________________________________

## Baseline Profiles

Improve startup and runtime performance by pre-compiling critical code paths (AOT):

```kotlin
// benchmark/src/main/java/BaselineProfileGenerator.kt
@ExperimentalBaselineProfilesApi
@RunWith(AndroidJUnit4::class)
class BaselineProfileGenerator {
    @get:Rule
    val baselineProfileRule = BaselineProfileRule()

    @Test
    fun startup() = baselineProfileRule.collectBaselineProfile(
        packageName = "io.github.kabirnayeem99.totpocket"
    ) {
        startActivityAndWait()
        // Add critical user flows here
    }
}
```

**Impact:** Baseline Profiles can improve app startup by 20-40% and reduce frame jank in critical
flows.

______________________________________________________________________

## Profiling and Debugging

### 1. Enable Compose Compiler Reports

```kotlin
// build.gradle.kts
composeCompiler {
    reportsDestination = layout.buildDirectory.dir("compose_reports")
    metricsDestination = layout.buildDirectory.dir("compose_metrics")
}
```

### 2. Analyze Generated Reports

Check for:

- **Unstable classes** in `*-classes.txt` — fix with `@Immutable`/`@Stable` or immutable collections
- **Non-skippable composables** in `*-composables.txt` — ensure all params are stable
- **Runtime stability** in `*-composables.csv` — identify hot recomposition paths

### 3. Use Layout Inspector

1. Enable "Show Recomposition Counts" in Layout Inspector
1. Look for composables with high recomposition counts
1. Compare recomposition count vs skip count — high recompose with low skip = problem

### 4. Benchmark Build Type

```kotlin
// In app/build.gradle.kts
buildTypes {
    create("benchmark") {
        initWith(getByName("release"))
        signingConfig = signingConfigs.getByName("debug")
        matchingFallbacks.add("release")
        isDebuggable = false
    }
}
```

### 5. Frame Timing

- **Target: 16ms per frame** (60 FPS)
- Use **Profile GPU Rendering** on-device (Developer Options) to visualize frame render times
- Use **Perfetto / Systrace** for system-level trace of the rendering pipeline
- Use **Macrobenchmark** to automate performance testing for critical user journeys
