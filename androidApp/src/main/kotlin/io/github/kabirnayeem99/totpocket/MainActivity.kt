package io.github.kabirnayeem99.totpocket

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.result.contract.ActivityResultContracts
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val app: TotPocketApplication
        get() = application as TotPocketApplication

    private val container: AppContainer
        get() = app.container

    // Read-only access to the phone's photos and videos (Photos, YouTube), and the front camera
    // for the self-view in pretend video calls.
    private val mediaPermissions = registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) {
        container.mediaLibrary.refresh()
    }

    private val insetsController: WindowInsetsControllerCompat by lazy {
        WindowCompat.getInsetsController(window, window.decorView)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)

        // Draw under the camera cutout too (Tecno/Xiaomi punch-holes) — no letterbox strip.
        window.attributes = window.attributes.apply {
            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS
        }
        // "Transient" bars (shown by an edge swipe such as the Back gesture) never tell the app
        // they're up, so they'd linger for seconds. With the default behaviour a revealed bar
        // changes the window insets, and KeepSystemBarsHidden (in the shared UI) hides it again.
        insetsController.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_DEFAULT
        hideSystemBars()
        app.deviceController.setSystemBarsAllowed(container.settingsStore.settings.value.showSystemBars)
        app.deviceController.attach(this)
        // Restores the home-app choice after "Exit TotPocket" withdrew it.
        app.deviceController.setHomeApp(container.settingsStore.settings.value.homeApp, askToChoose = false)
        keepPinnedWhileVisible()
        requestMediaAccess()

        setContent { App(container) }
    }

    // With "Keep pinned" on, TotPocket pins itself whenever it's on screen and, if something
    // unpins it (e.g. the swipe-up-and-hold gesture), asks to pin again a moment later.
    private fun keepPinnedWhileVisible() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.RESUMED) {
                while (true) {
                    // No window focus means the system pin prompt (or another dialog) is showing.
                    if (hasWindowFocus() && container.settingsStore.settings.value.keepPinned && !app.deviceController.isPinned) {
                        app.deviceController.pin()
                    }
                    delay(PIN_CHECK_INTERVAL_MS)
                }
            }
        }
    }

    override fun onDestroy() {
        app.deviceController.detach(this)
        super.onDestroy()
    }

    // Bars come back after dialogs, the pinning prompt, or an edge swipe; re-hide them whenever
    // we regain focus.
    override fun onWindowFocusChanged(hasFocus: Boolean) {
        super.onWindowFocusChanged(hasFocus)
        if (hasFocus) hideSystemBars()
    }

    // Nothing keeps playing once the app is out of sight.
    override fun onStop() {
        super.onStop()
        container.soundPlayer.stop()
    }

    private fun requestMediaAccess() {
        val wanted = if (Build.VERSION.SDK_INT >= 33) {
            listOf(Manifest.permission.READ_MEDIA_IMAGES, Manifest.permission.READ_MEDIA_VIDEO, Manifest.permission.CAMERA)
        } else {
            listOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.CAMERA)
        }
        val missing = wanted.filter { checkSelfPermission(it) != PackageManager.PERMISSION_GRANTED }
        if (missing.isNotEmpty()) mediaPermissions.launch(missing.toTypedArray())
    }

    private companion object {
        const val PIN_CHECK_INTERVAL_MS = 3_000L
    }

    private fun hideSystemBars() {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
