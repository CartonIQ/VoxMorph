package com.cartoniq.voxmorph.audio

import android.media.MediaPlayer
import java.io.File

/** One MediaPlayer at a time. Released on completion, error, stop and by the ViewModel on exit. */
class PlaybackController {
    private var player: MediaPlayer? = null

    fun play(file: File, onComplete: () -> Unit, onError: () -> Unit) {
        stop()
        val mp = MediaPlayer()
        player = mp
        try {
            mp.setDataSource(file.absolutePath)
            mp.setOnPreparedListener { if (player === it) it.start() }
            mp.setOnCompletionListener { release(it); onComplete() }
            mp.setOnErrorListener { p, _, _ -> release(p); onError(); true }
            mp.prepareAsync()
        } catch (e: Exception) {
            release(mp)
            onError()
        }
    }

    fun stop() {
        player?.let { release(it) }
    }

    private fun release(mp: MediaPlayer) {
        if (player === mp) player = null
        runCatching { mp.setOnCompletionListener(null); mp.setOnErrorListener(null); mp.stop() }
        runCatching { mp.release() }
    }
}
