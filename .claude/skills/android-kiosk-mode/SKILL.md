---
name: android-kiosk-mode
description: Use when working on keeping a toddler inside TotPocket on Android — immersive full-screen (hiding system bars), display cutout, back-press swallowing, screen pinning via startLockTask, device-owner lock task (dpm set-device-owner, setLockTaskPackages), keep-screen-on, and OEM quirks on Xiaomi HyperOS and Tecno HiOS. Also use when asked about Accessibility Service or launcher-replacement approaches.
---

# Android kiosk mode (keeping the child in the app)

There are three layers, from least to most invasive. v1 ships layers 1 and 2; layer 3 is an opt-in
for dedicated devices.

## Layer 1: Immersive plus Back (✅ implemented)

Implemented in `androidApp/.../MainActivity.kt` and `shared/.../App.kt`:

```kotlin
private val insetsController by lazy { WindowCompat.getInsetsController(window, window.decorView) }

override fun onCreate(savedInstanceState: Bundle?) {
    enableEdgeToEdge()
    super.onCreate(savedInstanceState)
    window.attributes = window.attributes.apply {
        layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
    }
    insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    insetsController.hide(WindowInsetsCompat.Type.systemBars())
    setContent { App() }
}

override fun onWindowFocusChanged(hasFocus: Boolean) {
    super.onWindowFocusChanged(hasFocus)
    if (hasFocus) insetsController.hide(WindowInsetsCompat.Type.systemBars())
}
```

- Re-hiding in `onWindowFocusChanged` matters. The bars reappear after the pinning prompt, the
  notification shade, dialogs and OEM gesture hints.
- **Back:** `PlatformBackHandler(enabled = true)` at the root always consumes Back. It pops the
  in-app stack and does nothing on Home. **Never** call `finish()` from a child-reachable path.
- This layer **doesn't** block Home or Recents. That's layer 2's job.

## Layer 2: Screen pinning (v1, parent-initiated)

```kotlin
// From ParentSettings (behind the parent gate) — needs an Activity.
fun Activity.pinApp() {
    val am = getSystemService(ActivityManager::class.java)
    if (am.lockTaskModeState == ActivityManager.LOCK_TASK_MODE_NONE) startLockTask()
}

fun Activity.unpinApp() {
    val am = getSystemService(ActivityManager::class.java)
    if (am.lockTaskModeState != ActivityManager.LOCK_TASK_MODE_NONE) stopLockTask()
}
```

- Without device owner, `startLockTask()` shows a system confirmation and uses **screen pinning**.
  Home and Recents are blocked. The system unpins on Back+Recents held together, and asks for the
  device PIN if "Ask for PIN before unpinning" is on (tell parents to turn it on).
- If screen pinning is disabled in settings, the call may do nothing. Detect that
  (`lockTaskModeState` is still `NONE` after ~500 ms) and deep-link the parent:
  `startActivity(Intent(Settings.ACTION_SECURITY_SETTINGS))`.
- Expose the pin action to `commonMain` as an interface (`KioskController { pin(); unpin(); isPinned }`)
  implemented in `androidApp` or `androidMain` and supplied through `AppContainer`. The ViewModel
  never touches an `Activity`.

**Where OEMs keep the setting:**

| Device                 | Path                                                                      |
| ---------------------- | ------------------------------------------------------------------------- |
| Xiaomi Note 14 (HyperOS) | Settings → Additional settings → Privacy → Screen pinning               |
| Tecno C5 (HiOS)        | Settings → Security → Screen pinning (sometimes "App pinning")            |
| Stock / Pixel          | Settings → Security & privacy → More security settings → App pinning     |

HyperOS may also kill the app in the background. Tell parents to set battery saver to
"No restrictions" for TotPocket, or pinning drops after a reboot.

## Layer 3: Device-owner lock task (v2 opt-in, dedicated devices only)

This gives a true kiosk: no prompt, no escape gesture, and Home and Recents are gone.

1. Add a `DeviceAdminReceiver`:

   ```kotlin
   class TotPocketAdminReceiver : DeviceAdminReceiver()
   ```

   ```xml
   <!-- AndroidManifest.xml -->
   <receiver
       android:name=".TotPocketAdminReceiver"
       android:exported="true"
       android:permission="android.permission.BIND_DEVICE_ADMIN">
       <meta-data android:name="android.app.device_admin" android:resource="@xml/device_admin" />
       <intent-filter>
           <action android:name="android.app.action.DEVICE_ADMIN_ENABLED" />
       </intent-filter>
   </receiver>
   <!-- on MainActivity -->
   android:lockTaskMode="if_whitelisted"
   ```

   ```xml
   <!-- res/xml/device_admin.xml -->
   <device-admin><uses-policies /></device-admin>
   ```

1. Provision it on a **freshly reset device with no accounts added**:

   ```bash
   adb shell dpm set-device-owner io.github.kabirnayeem99.totpocket/.TotPocketAdminReceiver
   ```

   On Xiaomi, also enable Developer options → "USB debugging (Security settings)" first.

1. Allow-list the app and lock:

   ```kotlin
   val dpm = getSystemService(DevicePolicyManager::class.java)
   val admin = ComponentName(this, TotPocketAdminReceiver::class.java)
   if (dpm.isDeviceOwnerApp(packageName)) {
       dpm.setLockTaskPackages(admin, arrayOf(packageName))
       dpm.setLockTaskFeatures(admin, DevicePolicyManager.LOCK_TASK_FEATURE_NONE)
       startLockTask() // no prompt now
   }
   ```

1. **Always ship an exit.** The parent settings screen must offer "Remove kiosk"
   (`stopLockTask()` followed by `dpm.clearDeviceOwnerApp(packageName)`). Otherwise the only way out
   is a factory reset. Test this path before handing the device over.

## Rejected approaches

| Approach                                          | Why not                                                                                                                       |
| ------------------------------------------------- | ----------------------------------------------------------------------------------------------------------------------------- |
| AccessibilityService that relaunches the app      | Play policy restricts accessibility APIs to real accessibility use. It can't block Home anyway, only react afterwards, which flickers. |
| Replacing the launcher (`CATEGORY_HOME`)          | Hijacks the parent's phone, and parents get confused when the app is the home screen.                                        |
| `SYSTEM_ALERT_WINDOW` overlays blocking edges     | Needs a scary permission, is fragile on HyperOS, and hurts accessibility.                                                     |
| Swallowing `KEYCODE_HOME` in `onKeyDown`          | Hasn't worked since Android 4.                                                                                                |

## Related

- `FLAG_KEEP_SCREEN_ON`: set it only while a pretend call is active, and clear it afterwards.
- The `edge-to-edge` skill covers insets. TotPocket draws full-bleed and pads with
  `TotPocketDimens.ScreenPadding` rather than system insets, because bars are hidden.
