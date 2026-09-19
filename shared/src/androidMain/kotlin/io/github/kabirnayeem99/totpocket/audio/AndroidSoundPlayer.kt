package io.github.kabirnayeem99.totpocket.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.media.MediaPlayer
import android.media.SoundPool
import android.util.Log
import io.github.kabirnayeem99.totpocket.SwallowBackgroundErrors
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import org.jetbrains.compose.resources.ExperimentalResourceApi
import totpocket.shared.generated.resources.Res
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.cancellation.CancellationException

/**
 * [SoundPlayer] backed by a [SoundPool] for short effects and one [MediaPlayer] for content clips.
 *
 * Clips are copied out of compose resources into `cacheDir` once, then played from the file path.
 * A clip that isn't bundled yet is skipped silently (and remembered, so it isn't retried).
 * Call [play]/[stop] from the main thread.
 */
class AndroidSoundPlayer(context: Context) : SoundPlayer {

    private val appContext = context.applicationContext
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO + SwallowBackgroundErrors)
    private val cacheLock = Mutex()
    private val missing = ConcurrentHashMap.newKeySet<SoundRef>()

    private val attributes = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()

    private val audioManager = appContext.getSystemService(AudioManager::class.java)
    private val focusRequest = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        .setAudioAttributes(attributes)
        .setOnAudioFocusChangeListener { change ->
            if (change == AudioManager.AUDIOFOCUS_LOSS || change == AudioManager.AUDIOFOCUS_LOSS_TRANSIENT) stop()
        }
        .build()

    private val soundPool = SoundPool.Builder().setMaxStreams(4).setAudioAttributes(attributes).build()
    private val effectIds = ConcurrentHashMap<SoundRef, Int>()

    private var mediaPlayer: MediaPlayer? = null
    private var playToken = 0

    @Volatile
    private var ceiling = 0.6f

    private val _playing = MutableStateFlow<SoundRef?>(null)
    override val playing: StateFlow<SoundRef?> = _playing.asStateFlow()

    override fun preload(effects: List<SoundRef>) {
        effects.forEach { ref ->
            scope.launch {
                val file = cachedFile(ref) ?: return@launch
                effectIds[ref] = soundPool.load(file.path, 1)
            }
        }
    }

    override fun playEffect(sound: SoundRef) {
        val id = effectIds[sound]
        if (id == null) {
            if (sound !in missing) preload(listOf(sound))
            return
        }
        soundPool.play(id, ceiling, ceiling, 1, 0, 1f)
    }

    override fun play(sound: SoundRef, loop: Boolean) {
        val token = ++playToken
        releaseMediaPlayer()
        _playing.value = sound
        scope.launch {
            val file = cachedFile(sound)
            withContext(Dispatchers.Main) {
                if (token != playToken) return@withContext
                if (file == null) {
                    _playing.value = null
                    return@withContext
                }
                startMediaPlayer(file, loop, token)
            }
        }
    }

    private fun startMediaPlayer(file: File, loop: Boolean, token: Int) {
        audioManager.requestAudioFocus(focusRequest)
        mediaPlayer = MediaPlayer().apply {
            setAudioAttributes(attributes)
            setDataSource(file.path)
            isLooping = loop
            setVolume(ceiling, ceiling)
            setOnPreparedListener { if (token == playToken) it.start() }
            setOnCompletionListener {
                if (token == playToken) {
                    _playing.value = null
                    releaseMediaPlayer()
                }
            }
            setOnErrorListener { _, what, extra ->
                Log.w(TAG, "MediaPlayer error $what/$extra for ${file.name}")
                if (token == playToken) _playing.value = null
                true
            }
            prepareAsync()
        }
    }

    override fun stop() {
        playToken++
        releaseMediaPlayer()
        _playing.value = null
    }

    override fun setCeiling(fraction: Float) {
        ceiling = fraction.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(ceiling, ceiling)
    }

    override fun release() {
        stop()
        soundPool.release()
        scope.cancel()
    }

    private fun releaseMediaPlayer() {
        mediaPlayer?.let {
            it.setOnCompletionListener(null)
            it.release()
            audioManager.abandonAudioFocusRequest(focusRequest)
        }
        mediaPlayer = null
    }

    @OptIn(ExperimentalResourceApi::class)
    private suspend fun cachedFile(ref: SoundRef): File? {
        if (ref in missing) return null
        val file = File(appContext.cacheDir, "sounds/${ref.path}")
        if (file.exists()) return file
        return cacheLock.withLock {
            if (file.exists()) return@withLock file
            try {
                val bytes = Res.readBytes(ref.path)
                file.parentFile?.mkdirs()
                val tmp = File(file.path + ".tmp")
                tmp.writeBytes(bytes)
                tmp.renameTo(file)
                file
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                Log.i(TAG, "Sound not bundled yet: ${ref.path} (${e::class.simpleName})")
                missing += ref
                null
            }
        }
    }

    private companion object {
        const val TAG = "TotPocketSound"
    }
}
