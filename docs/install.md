# Building, installing and setting up TotPocket

This is the whole path from source code to a phone a child can use: build a fast, R8-shrunk release
APK, check it, install it (with or without a computer), set it up as a grown-up, and add photos from
Wikimedia Commons under supervision. It ends with updating, removing and troubleshooting.

Target phones: **Tecno C5 (HiOS)** and **Xiaomi Redmi Note 14 (HyperOS)**. Any Android 10+ (API 29+)
phone works.

---

## 1. What you need on the computer (once)

| Tool | Why | Check |
| --- | --- | --- |
| JDK 17 or newer | Gradle and the Android build | `java -version` |
| Android SDK (via Android Studio or command-line tools) with build-tools and platform-tools | Compiling, `apksigner`, `adb` | `adb version` |
| Git + **Git LFS** | The photo pack and source photos are stored in LFS | `git lfs version` |
| Python 3 + Pillow (`pip install Pillow`) | Only to rebuild the photo pack or fetch Commons photos | `python3 -c "import PIL"` |

Get the code with its large files:

```bash
git clone https://github.com/kabirnayeem99/TotPocket.git
cd TotPocket
git lfs install
git lfs pull          # downloads shared/.../pack.zip and tools/photos/*.webp
```

If `pack.zip` is only a few hundred bytes, LFS didn't run. Photos will be empty in the app. Run
`git lfs pull` again.

Tell Gradle where the SDK is, in `local.properties` (Android Studio writes this for you):

```properties
sdk.dir=/Users/<you>/Library/Android/sdk
```

---

## 2. Create a release signing key (once, then keep it forever)

Android only installs an **update** over an existing app if both are signed with the same key. Use
the debug key and you'll have to uninstall to update, which wipes the PIN and any photos a grown-up
added. So make a real key once:

```bash
keytool -genkeypair -v \
  -keystore totpocket-release.jks \
  -alias totpocket \
  -keyalg RSA -keysize 4096 -validity 36500
```

It asks for a password and your name. Then create `keystore.properties` in the project root:

```properties
storeFile=totpocket-release.jks
storePassword=<the store password>
keyAlias=totpocket
keyPassword=<the key password>
```

Both files are in `.gitignore`. **Never commit them.** Back up the `.jks` file and both passwords
somewhere safe, such as a password manager and an offline copy. Losing them means every later
version needs an uninstall first.

Without `keystore.properties` the release build still works, but it's signed with the debug key.
That's fine for testing, not for a phone you'll keep updating.

---

## 3. Build the release APK

### The one command

```bash
tools/build_release.sh              # build, verify, copy to dist/
tools/build_release.sh --install    # …and install on the phone connected over adb
PACK=1 tools/build_release.sh       # rebuild the photo pack first (after adding photos)
```

The script:

1. Runs `./gradlew :androidApp:assembleRelease`. That is:
   - **R8 in full mode**: removes unused code, inlines and optimises the rest, and shortens names.
   - **Resource shrinking**: drops unused Android resources.
   - **Log stripping**: `Log.v/d/i` calls are removed; warnings and errors stay.
   - **Baseline profiles** from Compose, installed on first launch by `profileinstaller`. A
     sideloaded APK never gets Play Store cloud profiles, so without this the first runs would be
     interpreted and JIT-compiled, and noticeably slower on a Tecno C5.
2. Checks the signature with `apksigner` and prints the certificate. Make sure it's **your** key
   and not `CN=Android Debug`.
3. Checks the APK really contains the photo pack and the baseline profile.
4. Copies three files to `dist/`:
   - `totpocket-<version>-<date>.apk`: the file to install.
   - `…-mapping.txt`: **keep this with the APK**. R8 renames classes, so a crash trace from this
     build is only readable with its mapping:
     `$ANDROID_HOME/cmdline-tools/latest/bin/retrace dist/…-mapping.txt crash.txt`
   - `….apk.sha256`: a checksum, so you can confirm the file wasn't damaged in transfer.

Sizes for comparison: debug APK about 119 MB, release about 41–43 MB. About 39 MB of that is the
photo pack, which is already WebP and stored uncompressed.

### Before a real release

- Bump `versionCode` (by +1) and `versionName` in `androidApp/build.gradle.kts`. Android refuses to
  install an update whose `versionCode` isn't higher than the installed one.
- `./gradlew :shared:testAndroidHostTest` and count the results.

---

## 4. Performance and main-thread checks (before handing it over)

TotPocket has to be smooth on a low-end phone. These are the checks, from cheapest to most thorough.

### 4.1 Compose stability (no phone needed)

```bash
./gradlew :androidApp:assembleRelease -PcomposeReports=true
R=shared/build/compose-reports/TotPocket:shared-composables.txt
grep -c '^restartable' "$R"                   # all restartable composables
grep -c '^restartable skippable' "$R"         # must be the same number
grep -n ' unstable ' "$R"                     # must print nothing
```

`compose-stability.conf` (root) tells the compiler that Kotlin's read-only collections and
`Duration` are stable, which lets every screen skip recomposition when its inputs didn't change. At
the time of writing, all 96 restartable composables are skippable and none takes an unstable
parameter. A new `unstable` line means a new parameter type needs `@Immutable`/`@Stable`, or an
entry in that file. See `wiki/compose-performance-stability.md`.

### 4.2 Nothing on the main thread (debug build + phone)

Debug builds turn on **StrictMode**, which logs every disk read/write and network call made on the
main thread:

```bash
./gradlew :androidApp:installDebug
adb logcat -c
adb shell am start -n io.github.kabirnayeem99.totpocket/.MainActivity
# use every app for a minute: Photos, YouTube, calls, games, grown-up settings, Add photos
adb logcat -d | grep -E "StrictMode|D/StrictMode" | grep -v "at android\." | head -50
```

What is allowed, and why:

- The settings file (`SharedPreferences`) is read once at start-up, because the very first frame
  needs the PIN, the pinned state and the bar settings. Android loads that file on a background
  thread; the main thread only waits for the tiny file.

Everything else runs on `Dispatchers.IO`, and any StrictMode line pointing at TotPocket code is a
bug to fix. That covers:

- unpacking the photo pack
- reading MediaStore
- decoding images
- caching sounds (`MediaPlayer` uses `prepareAsync`)
- Commons searches
- downloads
- the download index and quota

### 4.3 Start-up time (release build + phone)

```bash
adb shell am force-stop io.github.kabirnayeem99.totpocket
adb shell am start -W -n io.github.kabirnayeem99.totpocket/.MainActivity | grep -E "TotalTime|WaitTime"
```

Run it 5 times and ignore the first run, which is compiling the profile. On a Tecno C5 a cold start
should stay well under a second.

### 4.4 Smooth frames (release build + phone)

```bash
adb shell dumpsys gfxinfo io.github.kabirnayeem99.totpocket reset
# now use the app: scroll an album, swipe photos with the arrows, play a YouTube slideshow, drag shapes
adb shell dumpsys gfxinfo io.github.kabirnayeem99.totpocket | \
  grep -E "Total frames rendered|Janky frames|50th percentile|90th percentile|95th percentile|99th percentile"
```

Aim for fewer than 5% janky frames and a 90th percentile under 16 ms on a 60 Hz screen (on 90/120 Hz
screens, under 11/8 ms). For a frame-by-frame look, record a trace with the phone's Developer
options → System Tracing, or Android Studio's profiler, and read it with the `perfetto-trace-analysis`
skill.

### 4.5 APK contents

```bash
$ANDROID_HOME/cmdline-tools/latest/bin/apkanalyzer apk summary dist/totpocket-*.apk
unzip -l dist/totpocket-*.apk | sort -n | tail -10     # biggest files
```

---

## 5. Install on the phone

Pick **A** if you have a computer and USB cable, **B** if you're sending the APK file to the phone.

### A. With a computer (adb over USB)

1. **Turn on Developer options** on the phone:
   - **Tecno (HiOS):** Settings → About phone → tap **Build number** 7 times.
   - **Xiaomi (HyperOS):** Settings → About phone → tap **OS version** 7 times.
2. **Turn on USB debugging:**
   - **Tecno:** Settings → System → Developer options → **USB debugging** on.
   - **Xiaomi:** Settings → Additional settings → Developer options → **USB debugging** on, **and**
     **Install via USB** on. Xiaomi asks you to sign in to a Mi account and have a SIM inserted for
     this.
3. Connect the cable. On the phone, accept **"Allow USB debugging?"** (tick "Always allow from this
   computer").
4. Check the phone is seen, then install:

   ```bash
   adb devices                      # shows one line ending in "device"
   adb install -r dist/totpocket-1.0-<date>.apk
   ```

   `-r` replaces an existing install and keeps its data. Xiaomi shows an on-screen "Install via USB"
   prompt; tap **Install** within 10 seconds.
5. `Success` means it's installed. `INSTALL_FAILED_UPDATE_INCOMPATIBLE` means the phone has a copy
   signed with another key; see [Updating](#8-updating).

### B. Without a computer (APK file)

1. Send the APK to the phone by USB file transfer, a memory card, Google Drive, Telegram or
   Bluetooth.
2. (Optional) Check the file: compare its SHA-256 with the `.sha256` file, using any "hash checker"
   app or, on a computer, `shasum -a 256 <apk>`.
3. Open the APK from the phone's **Files** app (or the app that received it).
4. Android asks you to **allow installing unknown apps** for that app (Files, Chrome, Telegram and so
   on). Tap **Settings**, turn on **Allow from this source**, then go back.
5. Tap **Install**.
   - **Xiaomi** runs a security scan and may say the app is "not verified". Tap **Install anyway** /
     **Continue**. If it's blocked, turn off Settings → Apps → **Security** app → settings →
     "Scan apps before installing".
   - **Tecno** or **Google Play Protect** may say "Unsafe app blocked". Tap **More details** →
     **Install anyway**.
6. Afterwards, turn **Allow from this source** back off for that app.

---

## 6. First-time setup by a grown-up

Do this before giving the phone to the child.

1. **Open TotPocket.** It asks for **Photos and videos** and **Camera**. Allow both if you want:
   - the phone's own photos and videos in Photos and YouTube, and
   - the child's face in pretend video calls.

   Both are read-only: TotPocket never changes, uploads or shares them. If you decline, TotPocket
   still works with its own photos.
2. **Open the grown-up area:** on the home screen, **press and hold the top-right corner for 3
   seconds**. The first time, it asks you to create a **4-digit PIN**. After that it asks for the
   PIN each time. Don't use a PIN the child has seen.
3. In **TotPocket settings**:

   | Setting | What it does | Suggested |
   | --- | --- | --- |
   | Volume limit | Caps every sound TotPocket makes | 50–60% |
   | Play time | Shows a calm bedtime screen when time's up | 10–20 min |
   | Add photos | Search Wikimedia Commons and add photos ([section 7](#7-adding-photos-from-wikimedia-commons-grown-ups-only)) | — |
   | Pin / Keep pinned | Locks the phone to TotPocket (screen pinning) | On |
   | Use as home app | Home always comes back to TotPocket | Optional |
   | Show status bar | Shows the real status bar and notifications | Off |
   | Change PIN / Exit TotPocket | Change the PIN, or leave the app | — |

4. **Turn on screen pinning** in the phone's settings first, or "Pin" can't work:
   - **Tecno:** Settings → Security → **Screen pinning** (sometimes "App pinning") → on, and turn on
     **Ask for PIN before unpinning**.
   - **Xiaomi:** Settings → Additional settings → Privacy → **Screen pinning** → on, and turn on
     **Ask for PIN before unpinning**.

   Then press **Pin** in TotPocket settings and confirm. With "Keep pinned" on, TotPocket re-pins
   itself when it opens.
5. **Stop the phone from closing TotPocket in the background.** Otherwise pinning can drop after a
   while:
   - **Xiaomi:** Settings → Apps → TotPocket → **Battery saver → No restrictions**, and **Autostart**
     on.
   - **Tecno:** Settings → Battery → App launch / Power management → TotPocket → **Manage manually**,
     all switches on.
6. **Hand-over check:** press Home, Back and Recents; swipe from the edges; try the three buttons at
   the bottom of each app. The child should stay inside TotPocket every time. Then exit with your
   PIN to make sure *you* can get out.

**Device-owner mode (optional, advanced).** This is a stronger lock with no escape gesture. It needs
a factory-reset phone with no accounts, and
`adb shell dpm set-device-owner io.github.kabirnayeem99.totpocket/.TotPocketAdminReceiver`. TotPocket
has **no button to undo it**: removing it means a **factory reset**. Only use it on a phone that is
TotPocket's alone. Screen pinning is enough for most families.

---

## 7. Adding photos from Wikimedia Commons (grown-ups only)

TotPocket ships with about 430 photos. A grown-up can add more from
[Wikimedia Commons](https://commons.wikimedia.org), a free library of openly licensed photos, right
from the phone.

### How to add a photo

1. Hold the top-right corner for 3 seconds → enter the PIN → **TotPocket settings**.
2. Under **Add photos**, tap **Search photos**.
3. Type what you want in **English**, e.g. `elephant`, `mango tree`, `rickshaw`, `mosque`, and tap
   **Search**.
4. Tap a result to see it **full size**, with its title, photographer and licence. Look at it
   properly; see the safety notes below.
5. Tap **Add to Photos**. It's downloaded, saved on the phone, and appears in the child's **Photos**
   app in a **"New photos"** album at the top, with the photographer's credit under it.
6. To take one away: in **Add photos**, scroll to **Added photos**, tap it, then **Remove from
   Photos**.

### The daily limit: 5 photos a day

- At most **5 photos can be added per calendar day**. The screen shows how many are left
  ("3 of 5 photos left today").
- The limit is counted **on the phone**; there's no account or server. It resets when the date
  moves to the next day. **Setting the clock back does not reset it**: the count only resets when
  the date moves forward.
- A download only counts when the photo is actually saved. A failed download costs nothing.
- **Removing** a photo does not give a download back.
- It's meant to keep adding photos a small, thoughtful job, not a feed.

### Safety: what's checked and what isn't

- Commons has **no safe-search**. TotPocket leaves out results matching words that usually mean
  unsuitable pictures (nudity, violence, weapons, blood and similar). That filter is **not perfect**.
  **You** are the real filter: every photo is shown to you full size before it's added, and
  nothing is added automatically.
- Search where the child can't see the results screen.
- The child **can't reach** this screen: it's behind the hidden corner hold and your PIN. None of
  the child's apps (Photos, YouTube, calls, games) ever go online.

### What goes over the internet

Only when a grown-up searches or adds a photo, and only to `commons.wikimedia.org` and
`upload.wikimedia.org` over HTTPS:

- the **search words** you type
- requests for the **picture files** you look at or add
- a `User-Agent` naming the app (`TotPocket/1.0 (https://github.com/kabirnayeem99/TotPocket)`), which
  Wikimedia asks every app to send

No account, no personal data, no analytics, no ads. Wikimedia's own privacy policy covers their
servers. With no internet, "Add photos" says it couldn't connect, and everything else works as usual.

### Where the photos live, and the licences

- Saved in the app's private storage (`files/media/downloads/`) as a 1080-pixel WebP plus a small
  thumbnail, listed in `index.json`. Other apps can't see them, and they're **not** in the phone's
  gallery.
- Uninstalling TotPocket deletes them. Updating keeps them.
- Commons photos are free to use under their licence: public domain, CC0, CC BY or CC BY-SA. CC BY
  and CC BY-SA require **crediting the author**, which is why every added photo shows "Photo:
  <author> · <licence>" under it.

### Under the hood (for developers)

The search is one call to the MediaWiki Action API:

```text
GET https://commons.wikimedia.org/w/api.php
    ?action=query&format=json
    &generator=search&gsrnamespace=6&gsrlimit=30
    &gsrsearch=<words> filetype:bitmap -nude -naked … (excluded words)
    &prop=imageinfo&iiprop=url|mime|extmetadata&iiurlwidth=1280
    &iiextmetadatafilter=LicenseShortName|Artist
```

- Each result's `thumburl` (1280 px) is what gets downloaded. Its `330px-` variant fills the results
  grid.
- `extmetadata` supplies the author and licence.
- Code:
  - `shared/src/androidMain/.../online/CommonsPhotoSearch.kt`: the search.
  - `AndroidPhotoDownloads.kt`: download, WebP files, index and quota. All on `Dispatchers.IO`, with a
    lock so two taps can't pass the limit.
  - `commonMain/.../online/PhotoDownloads.kt`: `DownloadQuota`, the day-limit rule.
  - `parent/AddPhotosViewModel.kt` and `parent/AddPhotosScreen.kt`: the screen.

Docs: [Commons API](https://commons.wikimedia.org/wiki/Commons:API),
[API:Search](https://www.mediawiki.org/wiki/API:Search),
[API:Imageinfo](https://www.mediawiki.org/wiki/API:Imageinfo),
[User-Agent policy](https://foundation.wikimedia.org/wiki/Policy:User-Agent_policy).

**Adding photos to the bundled pack** (for everyone who installs the APK) is a separate, build-time
process, `tools/fetch_commons_photos.py`:

1. `add` / `file` fetches candidates.
2. You review the contact sheet, then `approve` / `reject`.
3. `PACK=1 tools/build_release.sh` builds the pack into the next APK.

See the script's header for the commands.

---

## 8. Updating

1. Build the new version with a **higher `versionCode`** and the **same signing key**.
2. Install over the top: `adb install -r dist/totpocket-<new>.apk`, or open the new APK on the phone
   and tap **Update**.
3. The PIN, settings and added photos are kept.

If you see `INSTALL_FAILED_UPDATE_INCOMPATIBLE` / "App not installed as package conflicts", the phone
has a copy signed with a different key (often the debug key). The only fix is to uninstall the old
one first (see below). That loses the PIN and added photos, so set them up again afterwards.

If the phone is pinned to TotPocket, unpin first (TotPocket settings → **Unpin**), or the installer
can't come to the front.

---

## 9. Removing TotPocket

1. Open the grown-up area → **Unpin**, turn off **Use as home app** (then pick the phone's own
   launcher when Android asks), and **Exit TotPocket**.
2. Uninstall: Settings → Apps → TotPocket → **Uninstall**, or
   `adb uninstall io.github.kabirnayeem99.totpocket`.
3. If you used device-owner mode, the phone must be **factory reset** (see section 6).

---

## 10. Troubleshooting

| Problem | Fix |
| --- | --- |
| Photos app is empty | The APK was built without LFS files. `git lfs pull`, rebuild. The APK should be ~40 MB, not ~2 MB. |
| "App not installed" | Signed with a different key than the installed copy → uninstall first; or not enough storage (needs ~200 MB free for install + unpacking). |
| Xiaomi: `INSTALL_FAILED_USER_RESTRICTED` over adb | Turn on Developer options → **Install via USB** (needs Mi account + SIM), then tap Install on the phone within 10 s. |
| "Pin" does nothing | Screen pinning is off in the phone's settings (section 6, step 4). |
| Pinning drops after a while | Battery saver is closing the app: set **No restrictions** / **Autostart** (section 6, step 5). |
| Can't get out | Hold the top-right corner 3 s → PIN → Exit. Forgotten PIN: hold **Back + Recents** to unpin (asks for the phone's lock PIN), then clear the app's data in Settings → Apps → TotPocket → Storage (this also removes added photos). |
| "Add photos" says it couldn't connect | No internet, or Wikimedia is busy. Try again later; nothing was counted. |
| "That's today's photos" | 5 photos were added today. Try again tomorrow (changing the clock back won't help). |
| A bad photo got added | Grown-up area → Add photos → Added photos → tap it → **Remove from Photos**. |
| Slow or janky on a low-end phone | Make sure it's the **release** APK (`dist/…apk`), not debug. Let it run a minute after the first install while the baseline profile compiles. Then run the checks in section 4. |
| A crash in a release build | `adb logcat -d > crash.txt`, then `retrace` it with the matching `…-mapping.txt` (section 3). |
