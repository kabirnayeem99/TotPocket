---
name: toddler-ux-checklist
description: Use when designing, implementing, reviewing or testing any TotPocket screen, interaction, sound or animation — the measurable toddler-safety and anti-"brain rot" checklist (touch targets, no scrolling, no autoplay, no compulsive loops, sensory limits, can't-get-lost navigation, parent gate) with Compose test assertions for each check.
---

# Toddler UX checklist

The audience is 1–4-year-olds: pre-readers with developing fine motor control and short attention
spans, and often sensitive to sensory overload. A parent hands them the phone. Every check below
traces to a principle in `docs/plans/2026-09-18-totpocket-v1-plan.md` §1 (P1–P7).

Run it on every new or changed screen. A ❌ on any **blocker** means the change doesn't ship.

## Blockers

| #   | Check                                                                                         | Why (principle)                | How to verify                                                           |
| --- | --------------------------------------------------------------------------------------------- | ------------------------------ | ----------------------------------------------------------------------- |
| B1  | Every tappable element is ≥ 96 × 96 dp, with ≥ 24 dp between targets                          | Fat fingers (P5)               | `assertWidthIsAtLeast(96.dp)` / `assertHeightIsAtLeast(96.dp)`          |
| B2  | No scrollable containers: no `LazyColumn`, `verticalScroll`, `HorizontalPager` swipe or feed  | Finite screens (P2)            | `onAllNodes(hasScrollAction()).assertCountEquals(0)`                    |
| B3  | Nothing plays, moves or advances unless the child did something (except the call ringtone, capped at 10 s) | Calm (P1)                      | ViewModel test: create it, advance 60 s, assert `FakeSoundPlayer.played` is empty |
| B4  | At most one content sound at a time; a new tap stops the previous one                         | Sensory (P1)                   | ViewModel test: `stop()` recorded before each `play()`                  |
| B5  | System Back never finishes the activity; Home button present on every non-Home screen         | Can't get lost (P3)            | Navigator test plus a manual check on device                            |
| B6  | Nothing flashes or changes colour faster than 3 Hz; colour transitions ≥ 300 ms               | Photosensitivity               | Code review of animation specs                                          |
| B7  | No score, streak, timer countdown, "one more?", variable reward or level counter visible to the child | Anti-compulsion (P1)           | `UiState` has no such fields; review                                    |
| B8  | Settings, exit and unpin are reachable only through the parent gate                           | Parent control (P6)            | Review; UI test that the gate content isn't rendered without a 3 s hold |
| B9  | No network, analytics, ads or external links                                                  | Privacy (P7)                   | Manifest has no `INTERNET`; no `openUri` outside the parent area        |

## Majors

| #   | Check                                                                                             | How to verify                                                           |
| --- | ------------------------------------------------------------------------------------------------- | ----------------------------------------------------------------------- |
| M1  | Text is never needed to use a screen: every action has an icon or picture, and labels are secondary | Cover the labels; can a pre-reader still use it?                        |
| M2  | Every tappable element has `Role.Button` and a `contentDescription` (for parents and TalkBack)    | `onNodeWithContentDescription("…").assertHasClickAction()`              |
| M3  | Wrong moves have no negative feedback: no buzzer, no ✗, no shake; things gently spring back        | Review; engine test that a wrong drop emits no sound                   |
| M4  | Every activity has a natural end (5 rounds, a 60 s call, a finite page count) that leads to a calm "done" state | Engine and ViewModel tests                                              |
| M5  | Taps are debounced (≥ 400 ms) on anything that navigates or starts a sound                        | UI test: double tap produces one `Action`                               |
| M6  | ≤ 3 saturated colours per screen, and each section keeps its colour (Calls red, Gallery blue, Games yellow) | Review against `TotPocketColors`                                        |
| M7  | Content sound ≤ 3 s, boops ≤ 400 ms, volume under the parent ceiling                              | Asset check (`toddler-audio-and-assets` skill)                          |
| M8  | Layout fills the viewport with no off-screen content in both portrait and landscape               | Previews at 360×780 and 780×360                                         |

## Minors

- Press feedback on every target: a 0.92 bounce plus a haptic tick
- Screen transitions are a crossfade or scale (≤ 250 ms), not directional slides
- Icons are filled and chunky, with no thin outline glyphs

## Compose test snippet

```kotlin
@Test
fun soundGrid_meetsToddlerTargets() = runComposeUiTest {
    setContent { TotPocketTheme { SoundGridContent(state = sampleState(), onAction = {}) } }

    onAllNodes(hasClickAction()).fetchSemanticsNodes().forEach { node ->
        val size = node.size // px
        val minPx = with(density) { 96.dp.roundToPx() }
        assertTrue(size.width >= minPx && size.height >= minPx, "Target too small: ${node.config}")
    }
    onAllNodes(hasScrollAction()).assertCountEquals(0)
}
```

## Review output format

List each failed check as `B#/M#: file:line (what's wrong) → (suggested fix)`. End with
`Blockers: n, Majors: n`.
