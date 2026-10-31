package com.cartoniq.voxmorph.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import com.cartoniq.voxmorph.domain.AppError
import com.cartoniq.voxmorph.domain.AppException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import java.io.File
import java.io.RandomAccessFile
import kotlin.math.abs
import kotlin.math.max

/** Captures mono PCM16 with AudioRecord into a temporary WAV file on a worker thread. */
class AudioEngine {
    class Capture(val sampleRate: Int, val sampleCount: Long)

    @Volatile private var stopRequested = false

    /** Call before starting a capture job, so a stop tapped right after Record is never lost. */
    fun prepare() { stopRequested = false }

    fun requestStop() { stopRequested = true }

    private fun pickRate(): Pair<Int, Int>? {
        for (rate in intArrayOf(44100, 48000, 22050, 16000)) {
            val min = AudioRecord.getMinBufferSize(rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT)
            if (min > 0) return rate to min
        }
        return null
    }

    /**
     * Records until [requestStop], cancellation or [maxMs]. [onLevel] receives a 0..1 peak level and elapsed ms,
     * roughly 20 times per second. The microphone is always released in the finally block.
     */
    suspend fun capture(file: File, maxMs: Long, onLevel: (Float, Long) -> Unit): Capture =
        withContext(Dispatchers.IO) { captureBlocking(this, file, maxMs, onLevel) }

    private fun captureBlocking(scope: CoroutineScope, file: File, maxMs: Long, onLevel: (Float, Long) -> Unit): Capture {
        val (rate, minBuf) = pickRate() ?: throw AppException(AppError.MIC_UNAVAILABLE)
        val bufBytes = max(minBuf, rate / 5 * 2)
        val rec = try {
            AudioRecord(MediaRecorder.AudioSource.MIC, rate, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, bufBytes)
        } catch (e: SecurityException) {
            throw AppException(AppError.MIC_UNAVAILABLE, e)
        } catch (e: IllegalArgumentException) {
            throw AppException(AppError.MIC_UNAVAILABLE, e)
        }
        if (rec.state != AudioRecord.STATE_INITIALIZED) {
            rec.release()
            throw AppException(AppError.MIC_UNAVAILABLE)
        }
        file.parentFile?.mkdirs()
        var written = 0L
        try {
            RandomAccessFile(file, "rw").use { raf ->
                raf.setLength(0)
                raf.write(ByteArray(44)) // placeholder header, patched when finished
                rec.startRecording()
                if (rec.recordingState != AudioRecord.RECORDSTATE_RECORDING) throw AppException(AppError.MIC_UNAVAILABLE)
                val buf = ShortArray(bufBytes / 2)
                val bytes = ByteArray(buf.size * 2)
                val maxSamples = rate * maxMs / 1000
                val chunk = max(1, rate / 20) // ~50 ms level windows
                while (!stopRequested && scope.isActive && written < maxSamples) {
                    val n = rec.read(buf, 0, buf.size)
                    if (n < 0) throw AppException(AppError.RECORD_FAILED)
                    if (n == 0) continue
                    for (i in 0 until n) {
                        val v = buf[i].toInt()
                        bytes[i * 2] = (v and 0xFF).toByte()
                        bytes[i * 2 + 1] = ((v shr 8) and 0xFF).toByte()
                    }
                    raf.write(bytes, 0, n * 2)
                    var start = 0
                    while (start < n) {
                        val end = minOf(n, start + chunk)
                        var peak = 0
                        for (i in start until end) peak = max(peak, abs(buf[i].toInt()))
                        val elapsed = (written + end) * 1000 / rate
                        onLevel(peak / 32768f, elapsed)
                        start = end
                    }
                    written += n
                }
                WavIO.patchHeader(raf, written * 2, rate)
            }
        } finally {
            runCatching { rec.stop() }
            rec.release()
        }
        return Capture(rate, written)
    }
}
