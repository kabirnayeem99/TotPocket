package io.github.kabirnayeem99.totpocket

import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat

class MainActivity : ComponentActivity() {

    private val app: TotPocketApplication
        get() = application as TotPocketApplication

    private val container: AppContainer
        get() = app.container

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
        app.deviceController.attach(this)

        setContent { App(container) }
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

    private fun hideSystemBars() {
        insetsController.hide(WindowInsetsCompat.Type.systemBars())
    }
}
