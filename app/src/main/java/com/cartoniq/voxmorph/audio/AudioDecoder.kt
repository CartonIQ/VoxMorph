package com.cartoniq.voxmorph.audio

import android.media.MediaCodec
import android.media.MediaExtractor
import android.media.MediaFormat
import com.cartoniq.voxmorph.domain.AppError
import com.cartoniq.voxmorph.domain.AppException
import java.io.File
import java.nio.ByteOrder

/** Turns whatever the TTS engine wrote into mono PCM16. WAV is parsed directly, anything else goes through MediaCodec. */
object AudioDecoder {
    fun decodeToMono(file: File): WavIO.Pcm {
        WavIO.read(file)?.let { return it }
        return try {
            decodeWithCodec(file)
        } catch (e: AppException) {
            throw e
        } catch (e: Exception) {
            throw AppException(AppError.DECODE, e)
        }
    }

    private fun decodeWithCodec(file: File): WavIO.Pcm {
        val ex = MediaExtractor()
        var codec: MediaCodec? = null
        try {
            ex.setDataSource(file.absolutePath)
            var track = -1
            var fmt: MediaFormat? = null
            for (i in 0 until ex.trackCount) {
                val f = ex.getTrackFormat(i)
                if (f.getString(MediaFormat.KEY_MIME)?.startsWith("audio/") == true) { track = i; fmt = f; break }
            }
            if (track < 0 || fmt == null) throw AppException(AppError.DECODE)
            ex.selectTrack(track)
            var sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE)
            var ch = fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
            codec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME)!!)
            codec.configure(fmt, null, null, 0)
            codec.start()

            val info = MediaCodec.BufferInfo()
            var samples = ShortArray(sr * 10)
            var count = 0
            var inputDone = false
            var outputDone = false
            while (!outputDone) {
                if (!inputDone) {
                    val ii = codec.dequeueInputBuffer(10_000)
                    if (ii >= 0) {
                        val ib = codec.getInputBuffer(ii)!!
                        val n = ex.readSampleData(ib, 0)
                        if (n < 0) {
                            codec.queueInputBuffer(ii, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM)
                            inputDone = true
                        } else {
                            codec.queueInputBuffer(ii, 0, n, ex.sampleTime, 0)
                            ex.advance()
                        }
                    }
                }
                val oi = codec.dequeueOutputBuffer(info, 10_000)
                if (oi >= 0) {
                    val ob = codec.getOutputBuffer(oi)!!
                    ob.position(info.offset)
                    ob.limit(info.offset + info.size)
                    val sb = ob.order(ByteOrder.LITTLE_ENDIAN).asShortBuffer()
                    val frames = sb.remaining() / ch
                    if (count + frames > samples.size) samples = samples.copyOf(maxOf(samples.size * 2, count + frames))
                    for (f in 0 until frames) {
                        var sum = 0
                        for (c in 0 until ch) sum += sb.get()
                        samples[count++] = (sum / ch).toShort()
                    }
                    codec.releaseOutputBuffer(oi, false)
                    if (info.flags and MediaCodec.BUFFER_FLAG_END_OF_STREAM != 0) outputDone = true
                } else if (oi == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    val of = codec.outputFormat
                    sr = of.getInteger(MediaFormat.KEY_SAMPLE_RATE)
                    ch = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT)
                }
            }
            if (count == 0) throw AppException(AppError.DECODE)
            return WavIO.Pcm(samples.copyOf(count), sr)
        } finally {
            runCatching { codec?.stop() }
            runCatching { codec?.release() }
            ex.release()
        }
    }
}
