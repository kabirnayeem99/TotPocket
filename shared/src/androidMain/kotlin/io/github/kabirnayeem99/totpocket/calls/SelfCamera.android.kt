package io.github.kabirnayeem99.totpocket.calls

import android.Manifest
import android.content.pm.PackageManager
import android.util.Log
import androidx.camera.core.CameraSelector
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LocalLifecycleOwner

@Composable
actual fun SelfCamera(modifier: Modifier, fallback: @Composable () -> Unit) {
    val context = LocalContext.current
    val hasCamera = context.checkSelfPermission(Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED &&
        context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_FRONT)
    if (!hasCamera) {
        fallback()
        return
    }
    val lifecycleOwner = LocalLifecycleOwner.current
    val previewView = remember {
        PreviewView(context).apply {
            scaleType = PreviewView.ScaleType.FILL_CENTER
            // TextureView, so the preview clips to rounded corners and overlaps other UI.
            implementationMode = PreviewView.ImplementationMode.COMPATIBLE
        }
    }
    DisposableEffect(lifecycleOwner, previewView) {
        val future = ProcessCameraProvider.getInstance(context)
        val preview = Preview.Builder().build().also { it.setSurfaceProvider(previewView.surfaceProvider) }
        future.addListener(
            {
                try {
                    future.get().bindToLifecycle(lifecycleOwner, CameraSelector.DEFAULT_FRONT_CAMERA, preview)
                } catch (e: Exception) {
                    Log.w("TotPocketCamera", "Front camera unavailable", e)
                }
            },
            ContextCompat.getMainExecutor(context),
        )
        onDispose {
            if (future.isDone) runCatching { future.get().unbind(preview) }
        }
    }
    AndroidView(factory = { previewView }, modifier = modifier)
}
