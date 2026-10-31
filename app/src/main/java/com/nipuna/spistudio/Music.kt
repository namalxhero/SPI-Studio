package com.nipuna.spistudio

import android.content.Context
import android.media.MediaPlayer

/**
 * Plays res/raw/intro.mp3 in a loop (if the file exists). Looked up by name at runtime,
 * so the project still builds and runs without the mp3.
 */
class MusicPlayer(private val ctx: Context) {
    private var mp: MediaPlayer? = null

    private fun resId(): Int = ctx.resources.getIdentifier("intro", "raw", ctx.packageName)

    val available: Boolean
        get() = resId() != 0

    fun start() {
        if (mp != null) return
        val id = resId()
        if (id == 0) return
        mp = try {
            MediaPlayer.create(ctx, id)?.apply {
                isLooping = true
                setVolume(0f, 0f)
                start()
            }
        } catch (e: Exception) {
            null
        }
    }

    fun volume(v: Float) {
        mp?.setVolume(v, v)
    }

    fun pause() {
        val p = mp ?: return
        if (p.isPlaying) p.pause()
    }

    fun resume() {
        val p = mp ?: return
        if (!p.isPlaying) p.start()
    }

    fun release() {
        mp?.release()
        mp = null
    }
}
