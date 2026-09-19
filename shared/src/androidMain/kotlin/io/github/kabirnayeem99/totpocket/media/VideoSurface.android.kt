package io.github.kabirnayeem99.totpocket.media

import android.net.Uri
import android.widget.VideoView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView

@Composable
actual fun VideoSurface(uri: String, playKey: Int, paused: Boolean, onFinished: () -> Unit, modifier: Modifier) {
    // A fresh VideoView and AndroidView per video. AndroidView calls its factory only once, so
    // without the key a new video's view was never attached: the old, stopped one stayed on screen
    // and play/pause went to a view nobody could see.
    key(uri) { VideoPlayer(uri, playKey, paused, onFinished, modifier) }
}

@Composable
private fun VideoPlayer(uri: String, playKey: Int, paused: Boolean, onFinished: () -> Unit, modifier: Modifier) {
    val context = LocalContext.current
    val currentOnFinished by rememberUpdatedState(onFinished)
    val videoView = remember {
        VideoView(context).apply {
            setOnCompletionListener { currentOnFinished() }
            setOnErrorListener { _, _, _ ->
                currentOnFinished()
                true
            }
        }
    }
    DisposableEffect(playKey) {
        videoView.setVideoURI(Uri.parse(uri))
        videoView.start()
        onDispose { videoView.stopPlayback() }
    }
    LaunchedEffect(paused) {
        if (paused) videoView.pause() else videoView.start()
    }
    AndroidView(factory = { videoView }, modifier = modifier)
}
