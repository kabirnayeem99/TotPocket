# Skills Reference

`.claude/skills/` holds the generic Compose, Kotlin, KMP and Android skills ported from
`~/Office/Projects/SyarahOPSRedesigned`. `.agents/skills/` symlinks to them for Codex and OpenCode.

## TotPocket-specific skills (these outrank the generic ones)

| Skill                        | Use for                                                                                  |
| ---------------------------- | ---------------------------------------------------------------------------------------- |
| `totpocket-feature-scaffold` | Any new screen: Route, UiState/Action/Effect, ViewModel, Screen/Content, wiring, tests  |
| `toddler-ux-checklist`       | Blockers B1–B9 and majors M1–M8, with Compose test assertions, for any child-facing change |
| `toddler-interactions`       | Press bounce, debounce, drag-and-snap, hold-to-activate, pulse, haptics                   |
| `toddler-audio-and-assets`   | Asset specs, ffmpeg/cwebp, composeResources layout, credits, the `SoundPlayer` contract   |
| `android-kiosk-mode`         | Immersive mode, Back, screen pinning, device-owner lock task, OEM quirks                 |

These weren't ported: the BMAD workflow skills, and the SyarahOPS-specific
`design-system-catalogue`, `viewmodel-main-thread-audit`, `core-logger-structured-logging` and
`sy-code-review-mobile`. `jetpack-compose-m3` was ported and then removed, because it's
Wear OS–only.

## Companion skills by agent

| Agent                 | Reach for                                                                                                                                                |
| --------------------- | -------------------------------------------------------------------------------------------------------------------------------------------------------- |
| `team_lead`           | `totpocket-feature-scaffold`, `kotlin-multiplatform-expect-actual`, `compose-state-holder-ui-split`                                                      |
| `product_manager`     | `toddler-ux-checklist`                                                                                                                                   |
| `engineer`            | `totpocket-feature-scaffold`, `toddler-interactions`, `toddler-ux-checklist`, `toddler-audio-and-assets`, `android-kiosk-mode`, plus the generic Compose/Flow skills |
| `senior_engineer`     | `totpocket-feature-scaffold`, `toddler-ux-checklist`, `compose-stability-diagnostics`, `compose-state-deferred-reads`, `kotlin-coroutines-structured-concurrency` |
| `toddler-ux-reviewer` | `toddler-ux-checklist`                                                                                                                                   |
| `design-system`       | `toddler-interactions`, `toddler-ux-checklist`, `compose-slot-api-pattern`, `compose-modifier-and-layout-style`, `compose-animations`                    |
| `media-assets`        | `toddler-audio-and-assets`                                                                                                                               |
| `tester`              | `toddler-ux-checklist`, `compose-ui-testing-patterns`, `testing-setup`                                                                                   |
| `compose-performance` | `toddler-interactions`, `compose-recomposition-performance`, `compose-stability-diagnostics`, `compose-state-deferred-reads`, `perfetto-trace-analysis`, `perfetto-sql` |
| `kotlin`              | `kotlin-coroutines-structured-concurrency`, `kotlin-flow-state-event-modeling`, `kotlin-multiplatform-expect-actual`, `kotlin-types-value-class`         |

Other skills: `compose-slot-api-pattern` and `styles` for shared `ui/components`; `edge-to-edge` for
insets and immersive work; `compose-focus-navigation` for accessibility focus; `r8-analyzer` for
release shrinking; `android-cli` for SDK and device tasks.
