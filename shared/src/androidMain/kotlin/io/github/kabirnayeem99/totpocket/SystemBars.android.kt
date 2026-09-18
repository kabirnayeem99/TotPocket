package io.github.kabirnayeem99.totpocket

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.statusBars
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat

@Composable
actual fun KeepSystemBarsHidden() {
    val view = LocalView.current
    val density = LocalDensity.current
    val direction = LocalLayoutDirection.current
    // A hidden bar has zero inset; any size means something revealed it.
    val statusBarShown = WindowInsets.statusBars.getTop(density) > 0
    val navigationBar = WindowInsets.navigationBars
    val navigationBarShown = navigationBar.getBottom(density) > 0 ||
        navigationBar.getLeft(density, direction) > 0 ||
        navigationBar.getRight(density, direction) > 0
    LaunchedEffect(statusBarShown, navigationBarShown) {
        if (statusBarShown || navigationBarShown) {
            val window = view.context.findActivity()?.window ?: return@LaunchedEffect
            WindowCompat.getInsetsController(window, view).hide(WindowInsetsCompat.Type.systemBars())
        }
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
