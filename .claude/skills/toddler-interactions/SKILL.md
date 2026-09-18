---
name: toddler-interactions
description: Use when implementing TotPocket touch interactions in Compose Multiplatform — press-bounce cards, tap debouncing, drag-and-snap (ShapeMatch), hold-to-activate (parent gate), gentle pulse (ringing call), and haptics. Gives performance-safe patterns (deferred reads, Animatable, rememberUpdatedState) tuned for toddlers and low-end devices.
---

# Toddler interaction patterns

Toddlers tap with the whole palm, mash, drag badly and let go early. These patterns are forgiving
by default. All frame-rate state is read only in layout or draw lambdas (see the
`compose-state-deferred-reads` skill), so they stay smooth on a Tecno C5.

Reference implementation: `HomeCard` in `shared/.../home/TotPocketHomeScreen.kt`.

## 1. Press bounce (every tappable card or button)

```kotlin
val interactionSource = remember { MutableInteractionSource() }
val isPressed by interactionSource.collectIsPressedAsState()
val scale by animateFloatAsState(
    targetValue = if (isPressed) 0.92f else 1f,
    animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
    label = "pressScale",
)
Modifier
    .graphicsLayer { scaleX = scale; scaleY = scale } // deferred read: no recomposition per frame
    .clickable(interactionSource = interactionSource, indication = null, role = Role.Button) { … }
```

- Use `indication = null` on large cards, because a ripple across a whole card is visual noise.
  Keep the ripple on small circular buttons.
- Put the haptic on the tap, not on press-down, so a palm resting on the screen doesn't buzz.

## 2. Debounced taps (anything that navigates or starts a sound)

```kotlin
@Composable
fun rememberDebouncedClick(
    onClick: () -> Unit,
    window: Duration = 400.milliseconds,
): () -> Unit {
    val latest by rememberUpdatedState(onClick)
    return remember(window) {
        var last: TimeSource.Monotonic.ValueTimeMark? = null
        {
            val mark = last
            if (mark == null || mark.elapsedNow() >= window) {
                last = TimeSource.Monotonic.markNow()
                latest()
            }
        }
    }
}
```

`rememberUpdatedState` is needed because a `remember`ed lambda would otherwise capture a stale
`onClick` forever (HR-11 in `wiki/compose-performance-rules.md`).

## 3. Drag and snap (ShapeMatch)

The **engine** decides the match; the UI only converts coordinates.

```kotlin
@Composable
fun DraggableShape(
    shape: GameShape,
    onDrop: (shapeId: ShapeId, dropCenterInRoot: Offset) -> Offset?, // returns snap target (relative) or null
    modifier: Modifier = Modifier,
) {
    val offset = remember(shape.id) { Animatable(Offset.Zero, Offset.VectorConverter) }
    val scope = rememberCoroutineScope()
    val currentOnDrop by rememberUpdatedState(onDrop)
    var centerInRoot by remember { mutableStateOf(Offset.Zero) } // written in layout, read only in gesture callbacks

    Box(
        modifier
            .onGloballyPositioned { centerInRoot = it.boundsInRoot().center }
            .offset { offset.value.round() } // lambda form: layout-phase read
            .pointerInput(shape.id) {
                detectDragGestures(
                    onDrag = { change, amount ->
                        change.consume()
                        scope.launch { offset.snapTo(offset.value + amount) }
                    },
                    onDragEnd = {
                        scope.launch {
                            val target = currentOnDrop(shape.id, centerInRoot)
                            offset.animateTo(
                                targetValue = target ?: Offset.Zero, // no match → spring home, silently
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                            )
                        }
                    },
                    onDragCancel = { scope.launch { offset.animateTo(Offset.Zero) } },
                )
            },
    )
}
```

- The snap tolerance is generous (60 dp) and lives in `ShapeMatchEngine` as a named constant.
- Hole positions come from `onGloballyPositioned { boundsInRoot() }` and are passed to the engine
  as plain `Rect`s.
- A wrong drop gets no sound, no shake and no red. It just springs back (toddler-ux-checklist M3).
- Only one shape can be dragged at a time. Ignore a second pointer (take the first down, consume
  others).

## 4. Hold to activate (parent gate)

```kotlin
@Composable
fun HoldToActivate(
    onActivated: () -> Unit,
    holdFor: Duration = 3.seconds,
    modifier: Modifier = Modifier,
) {
    val progress = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val currentOnActivated by rememberUpdatedState(onActivated)

    Box(
        modifier
            .size(TotPocketDimens.MinTouchTarget)
            .pointerInput(holdFor) {
                awaitEachGesture {
                    awaitFirstDown(requireUnconsumed = false)
                    val job = scope.launch {
                        progress.animateTo(1f, tween(holdFor.inWholeMilliseconds.toInt(), easing = LinearEasing))
                        currentOnActivated()
                    }
                    waitForUpOrCancellation()
                    job.cancel()
                    scope.launch { progress.snapTo(0f) }
                }
            }
            .drawBehind {
                // Ring is visible to adults only while holding; invisible at rest.
                if (progress.value > 0f) drawArc(
                    color = TotPocketColors.Outline,
                    startAngle = -90f,
                    sweepAngle = 360f * progress.value,
                    useCenter = false,
                    style = Stroke(width = 8.dp.toPx()),
                )
            },
    )
}
```

After the hold, show the arithmetic challenge (plan §4.5). The hold alone isn't the gate.

## 5. Gentle pulse (ringing avatar)

```kotlin
val transition = rememberInfiniteTransition(label = "ringPulse")
val pulse by transition.animateFloat(
    initialValue = 1f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(tween(500, easing = FastOutSlowInEasing), RepeatMode.Reverse),
    label = "pulse",
)
Modifier.graphicsLayer { scaleX = pulse; scaleY = pulse }
```

A 1 s full cycle is 1 Hz, well under the 3 Hz flash limit. Use only small amplitude (≤ 5 %), and
never animate colour this way.

## 6. Haptics

- `LocalHapticFeedback.current.performHapticFeedback(HapticFeedbackType.LongPress)` for taps and
  snaps. The lighter types are imperceptible on low-end vibrators.
- No haptic on wrong drops. There's no continuous vibration anywhere.
- Needs only the `VIBRATE` permission, which Compose declares through its manifest merge.

## Anti-patterns

- `Modifier.offset(x = state.dp)` or `scale(state)` with animated values: this reads in
  composition and recomposes every frame.
- `pointerInput(Unit)` capturing a changing callback without `rememberUpdatedState`.
- `combinedClickable(onLongClick = …)` for the parent gate: the ~500 ms long-press is too easy for
  a toddler to trigger by accident.
- Swipe-to-answer or any swipe-only action, which toddlers can't do reliably.
