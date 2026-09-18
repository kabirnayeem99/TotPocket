package io.github.kabirnayeem99.totpocket.media

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoSurface(uri: String, playKey: Int, onFinished: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val videoView = remember(uri) {
        VideoView(context).apply {
            setOnCompletionListener { currentOnFinished() }
            setOnErrorListener { _, _, _ ->
                currentOnFinished()
                true
            }
        }
    }
    DisposableEffect(videoView, playKey) {
        videoView.setVideoURI(Uri.parse(uri))
        videoView.start()
        onDispose { videoView.stopPlayback() }
    }
    AndroidView(factory = { videoView }, modifier = modifier)
}
