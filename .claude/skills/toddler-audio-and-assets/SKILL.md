---
name: toddler-audio-and-assets
description: Use when adding or changing TotPocket sounds, images or the SoundPlayer — asset specs (OGG/WebP sizes, durations, loudness), ffmpeg/cwebp conversion commands, composeResources layout and naming, licensing/credits, and the SoundPlayer contract (one content clip at a time, preloaded boops, parent volume ceiling, stop on background).
---

# Audio and media assets

## 1. Asset specs

| Kind                                     | Format                 | Limits                                              | Location (under `shared/src/commonMain/composeResources/`) |
| ---------------------------------------- | ---------------------- | --------------------------------------------------- | ---------------------------------------------------------- |
| Gallery photo                            | WebP, quality 80       | ≤ 512 px on the long edge, ≤ 60 KB                  | `drawable/gallery_<category>_<id>.webp`                    |
| Gallery sound                            | OGG Vorbis, mono, 64 kbps | ≤ 3 s, fade out over the last 200 ms, −20 LUFS    | `files/gallery/<category>/<id>.ogg`                        |
| UI boop / pop / chime                    | OGG mono               | ≤ 400 ms (chime ≤ 1.5 s)                            | `files/ui/<name>.ogg`                                      |
| Call voice lines                         | OGG mono               | ≤ 4 s each, calm adult voice                        | `files/calls/<contact>/<line>.ogg`                         |
| Ringtone                                 | OGG mono               | 2–3 s loop, soft marimba-like (no harsh bell)       | `files/calls/ringtone.ogg`                                 |
| Contact avatars                          | WebP                   | 384 px square                                       | `drawable/contact_<id>.webp`                               |

- IDs are `lower_snake_case`. A photo and its sound share the id (`gallery_animals_cow.webp` ↔
  `files/gallery/animals/cow.ogg`).
- Drawables must be flat in `drawable/` (no subfolders), so the category goes in the file-name
  prefix.
- Watch the budget: the whole APK should stay under 40 MB. Check with `ls -la` after adding assets.

## 2. Conversion commands

```bash
# Sound: mono, trim to 3s, loudness-normalise, fade out, Vorbis 64k
ffmpeg -i in.wav -t 3 -ac 1 -ar 44100 \
  -af "loudnorm=I=-20:TP=-3:LRA=7,afade=t=out:st=2.8:d=0.2" \
  -c:a libvorbis -b:a 64k out.ogg

# Photo: fit within 512px, WebP q80, strip metadata
cwebp -q 80 -resize 512 0 -metadata none in.jpg -o out.webp   # landscape
cwebp -q 80 -resize 0 512 -metadata none in.jpg -o out.webp   # portrait

# Check loudness of an existing clip
ffmpeg -i clip.ogg -af loudnorm=print_format=summary -f null - 2>&1 | grep "Input Integrated"
```

## 3. Content rules

- Use real, recognisable sounds (a cow moo, rain, birdsong). No cartoon sound effects, no screaming
  or startling sounds, and no sudden loud onsets (fade in over 20 ms if the attack is harsh).
- Photos show a single clear subject centred on a plain background. No text or watermark in the
  image, and nothing scary (no predators baring teeth, no insects close up).
- Each gallery category has 6–12 items (1–2 pages at 6 per page, and never more than 4 pages).

## 4. Licensing

Use only CC0/public-domain or self-made assets (freesound.org filtered to CC0, Pixabay, or your
own recordings). Record every asset in `docs/assets/CREDITS.md`:

```markdown
| File | Source URL | Author | License |
```

Don't add an asset without a credits row.

## 5. SoundPlayer contract

```kotlin
interface SoundPlayer {
    /** Preload short UI sounds; call once from AppContainer. */
    fun preload(sounds: List<SoundRef>)

    /** Plays a content clip, stopping any clip already playing. UI boops mix over it. */
    fun play(sound: SoundRef)

    fun stop()

    /** Parent volume ceiling, 0f..1f. Applied as player volume, never as system volume. */
    fun setCeiling(fraction: Float)

    fun release()
}

@JvmInline
value class SoundRef(val path: String) // e.g. "files/gallery/animals/cow.ogg"
```

Invariants (enforce and test them with `FakeSoundPlayer`):

1. **One content clip at a time.** `play` always stops the previous clip first.
1. **Nothing is chained automatically.** When a clip finishes, nothing else starts.
1. **Stop on background.** The Android implementation observes `ProcessLifecycleOwner` (or the
   Activity's `onStop`) and calls `stop()`.
1. **Stop on leave.** Every ViewModel that plays audio calls `player.stop()` in `onCleared`.

## 6. Android implementation notes

- **Boops:** `SoundPool` (maxStreams 4), loaded once at startup. Play by sound ID, and never
  decode on tap.
- **Content clips:** a single reused `MediaPlayer` (`reset()` → `setDataSource` → `prepareAsync`).
- **Resolving compose resources on Android:** `Res.getUri("files/…")` returns an
  asset-backed URI. Open it through `context.assets.openFd(assetPath)` for both SoundPool and
  MediaPlayer. OGG isn't compressed in the APK, so `openFd` works. Check the exact asset path
  prefix the first time you implement this (log `Res.getUri` output); don't hard-code a guess.
- **Audio focus:** request `AUDIOFOCUS_GAIN_TRANSIENT` with `USAGE_GAME`, and stop on focus loss.
- iOS gets a stub `actual` / no-op implementation until iOS is in scope.
