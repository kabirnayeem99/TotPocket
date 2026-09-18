> **Ported from SyarahOPSRedesigned.** The rules are generic. Paths such as `core/` and `features/`, `Syarah*` names and custom detekt rules are examples from that codebase and don't exist in TotPocket. TotPocket-specific rules live in `docs/plans/` and `AGENTS.md`.

# Compose Lazy Layout and Animation Performance

The long-form patterns for the two places jank actually shows up: lazy layouts (cache window
configuration on Compose 1.9+, and a complete optimized `LazyColumn`) and animation (the
`graphicsLayer` properties that render without recomposition, and a complete animation example).
Load this when building or fixing a scrolling list or a running animation and you want the full
worked pattern rather than the one-line rule.

The rules these patterns satisfy — CR-5, HR-5, HR-8, HR-10, MR-3, MR-4, MR-5, LR-3 — are in
[`compose-performance-rules.md`](compose-performance-rules.md). For choosing an animation API in the
first place, see the [`compose-animations`](../.claude/skills/compose-animations/SKILL.md) skill.

______________________________________________________________________

## Lazy Layout Optimization (Advanced)

### Cache Window Configuration (Compose 1.9+)

Pre-compose items before they enter the viewport to reduce scroll jank:

```kotlin
LazyColumn(
    // Pre-compose items beyond visible area
    beyondBoundsItemCount = 5  // Pre-compose 5 items beyond viewport
) {
    items(items = jobs, key = { it.id }) { job ->
        JobCard(job)
    }
}
```

### Complete Optimized Lazy Layout Pattern

```kotlin
@Composable
fun OptimizedJobList(
    jobs: ImmutableList<Job>,  // HR-2: Immutable collection
    onJobClick: (Job) -> Unit,
    modifier: Modifier = Modifier
) {
    val listState = rememberLazyListState()

    // HR-3: derivedStateOf for scroll-dependent state
    val showScrollToTop by remember {
        derivedStateOf { listState.firstVisibleItemIndex > 5 }
    }

    Box(modifier = modifier) {
        LazyColumn(
            state = listState,
            modifier = Modifier.fillMaxSize()
        ) {
            items(
                items = jobs,
                key = { it.id },  // CR-5: Stable keys
                contentType = { "job" }  // HR-8: Content type
            ) { job ->
                JobCard(
                    job = job,
                    onClick = { onJobClick(job) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .animateItem()  // Smooth item animations
                )
            }
        }

        // Show/hide based on derived state
        AnimatedVisibility(
            visible = showScrollToTop,
            modifier = Modifier.align(Alignment.BottomEnd)
        ) {
            ScrollToTopButton(listState)
        }
    }
}
```

______________________________________________________________________

## Animation Performance

### graphicsLayer Properties (Zero Recomposition)

| Property                              | Use Case            |
| ------------------------------------- | ------------------- |
| `alpha`                               | Fade animations     |
| `scaleX`, `scaleY`                    | Scale animations    |
| `rotationX`, `rotationY`, `rotationZ` | Rotation animations |
| `translationX`, `translationY`        | Move animations     |
| `shadowElevation`                     | Shadow animations   |
| `cameraDistance`                      | 3D perspective      |

```kotlin
@Composable
fun AnimatedCard(
    alphaProvider: () -> Float,
    scaleProvider: () -> Float,
    translationYProvider: () -> Float
) {
    Card(
        modifier = Modifier.graphicsLayer {
            alpha = alphaProvider()
            scaleX = scaleProvider()
            scaleY = scaleProvider()
            translationY = translationYProvider()
        }
    ) {
        // content
    }
}
```

### Complete Animation Example

```kotlin
@Composable
fun PressableButton(isPressed: Boolean) {
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "scale"
    )

    Button(
        modifier = Modifier.graphicsLayer {  // HR-5: graphicsLayer for transforms
            scaleX = scale
            scaleY = scale
        }
    ) {
        Text("Press Me")
    }
}
```
