# TotPocket — Agent Guide

TotPocket is a toddler "pretend phone" app: pretend calling, a picture-and-sound gallery, and a
shape-matching game. It's a Kotlin Multiplatform + Compose Multiplatform project. Android is the
product (target devices: Tecno C5, Redmi Note 14). iOS compiles but isn't a v1 goal.

**The source of truth for design, screens, interactions and ViewModel rules is
[docs/plans/2026-09-18-totpocket-v1-plan.md](docs/plans/2026-09-18-totpocket-v1-plan.md).** Read it
before planning or writing any feature.

## Layout

| Path                                     | Contents                                                                                                   |
| ---------------------------------------- | ---------------------------------------------------------------------------------------------------------- |
| `shared/src/commonMain/.../totpocket/`   | All UI and logic, organised by feature package (`home/`, `calls/`, `gallery/`, `games/`, `parent/`, `navigation/`, `audio/`, `ui/theme`, `ui/icons`, `ui/components`) |
| `shared/src/androidMain/`                | `actual`s for Android platform services (`PlatformBackHandler`, `SoundPlayer`, …)                          |
| `shared/src/iosMain/`                    | `actual`s for iOS (mostly no-ops)                                                                          |
| `androidApp/`                            | Thin shell: `MainActivity` (immersive mode, cutout, lock task). No UI lives here.                          |
| `wiki/`                                  | Generic Kotlin, Compose-performance and testing references used by the agents                             |
| `.claude/agents/`, `.claude/skills/`     | Agents and skills (also exposed to other tools via `.agents/skills/` symlinks)                            |

## Non-negotiables

1. **Toddler UX (plan §1):** no scrolling or feeds, no autoplay, targets ≥ `TotPocketDimens.MinTouchTarget`
   (96 dp), and no text that's needed to use the app. System Back never exits the app.
1. **Tokens only:** colours and dimensions come from `TotPocketColors` / `TotPocketDimens`, and
   glyphs from `TotPocketIcons`. Don't use raw `Color(0x…)` or ad-hoc `.dp` in feature code; add a
   token instead.
1. **ViewModel rules (plan §5.2):** one immutable `UiState` `StateFlow`, one `onAction(Action)`,
   and a `Channel` for one-shot effects. No platform types in ViewModels. Screen/content split.
1. **No new dependencies without a reason.** v1 has no navigation library, no DI framework and no
   icons artifact.
1. **Offline for the child, private:** no analytics, no ads, no accounts. The only network use is
   the PIN-gated grown-ups' "Add photos" screen (Wikimedia Commons, at most 5 photos a day, counted
   on the phone); nothing the child can reach goes online.

## Commands

Only run Gradle when the task needs verification or the user asks:

- `./gradlew :androidApp:assembleDebug` — build the Android app
- `tools/build_release.sh [--install]` — R8 release APK, verified, into `dist/` ([docs/install.md](docs/install.md))
- `./gradlew :shared:testAndroidHostTest` — shared unit tests (count the results; don't trust a
  green `./gradlew test`)
- `./gradlew -PenableIos=true :shared:iosSimulatorArm64Test` — iOS tests (optional). iOS targets are off by default (`enableIos=false` in `gradle.properties`) so Android builds stay fast.

There is no detekt/ktlint setup yet. Follow [wiki/detekt-rules.md](wiki/detekt-rules.md) by hand.

## Agents

| Agent                 | Use for                                                                    |
| --------------------- | -------------------------------------------------------------------------- |
| `team_lead`           | Multi-step feature work across the roles below                             |
| `product_manager`     | Ambiguous requirements, toddler-UX acceptance criteria                     |
| `engineer`            | Implementing an approved plan                                              |
| `senior_engineer`     | Critiquing a plan or reviewing code (reports findings, doesn't fix)        |
| `toddler-ux-reviewer` | Child and parent perspective review: safety, sensory load, anti-compulsion |
| `design-system`       | Tokens, icons, shared components in `ui/`                                  |
| `media-assets`        | Converting, placing, crediting and auditing sounds and images             |
| `tester`              | Unit and Compose UI tests                                                  |
| `compose-performance` | Recomposition, jank, startup on low-end devices                            |
| `kotlin`              | Language-level Kotlin/coroutines/Flow work                                 |
| `commit`              | Crafting a Conventional Commit for staged changes                          |

Project skills: `totpocket-feature-scaffold`, `toddler-ux-checklist`, `toddler-interactions`,
`toddler-audio-and-assets`, `android-kiosk-mode`. See [.claude/SKILLS_REFERENCE.md](.claude/SKILLS_REFERENCE.md) for which skill each agent uses.
