package com.cartoniq.voxmorph.audio

import java.io.File
import java.io.FileOutputStream
import java.io.RandomAccessFile
import java.nio.ByteBuffer
import java.nio.ByteOrder

object WavIO {
    class Pcm(val samples: ShortArray, val sampleRate: Int)

    fun header(dataBytes: Long, sampleRate: Int, channels: Int = 1): ByteArray {
        val bb = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN)
        bb.put("RIFF".toByteArray()).putInt((36 + dataBytes).toInt()).put("WAVE".toByteArray())
        bb.put("fmt ".toByteArray()).putInt(16).putShort(1).putShort(channels.toShort())
        bb.putInt(sampleRate).putInt(sampleRate * channels * 2).putShort((channels * 2).toShort()).putShort(16)
        bb.put("data".toByteArray()).putInt(dataBytes.toInt())
        return bb.array()
    }

    fun patchHeader(raf: RandomAccessFile, dataBytes: Long, sampleRate: Int) {
        raf.seek(0)
        raf.write(header(dataBytes, sampleRate))
    }

    fun write(file: File, samples: ShortArray, sampleRate: Int) {
        file.parentFile?.mkdirs()
        FileOutputStream(file).use { out ->
            out.write(header(samples.size * 2L, sampleRate))
            val bb = ByteBuffer.allocate(samples.size * 2).order(ByteOrder.LITTLE_ENDIAN)
            bb.asShortBuffer().put(samples)
            out.write(bb.array())
        }
    }

    /** Reads 16-bit PCM WAV; multi-channel is down-mixed to mono. Returns null if unsupported. */
    fun read(file: File): Pcm? {
        if (!file.exists() || file.length() < 44) return null
        val bytes = file.readBytes()
        if (String(bytes, 0, 4) != "RIFF" || String(bytes, 8, 4) != "WAVE") return null
        val bb = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN)
        var pos = 12
        var fmtOk = false
        var ch = 1
        var sr = 0
        var bits = 16
        var tag = 1
        while (pos + 8 <= bytes.size) {
            val id = String(bytes, pos, 4)
            val size = bb.getInt(pos + 4)
            val body = pos + 8
            if (id == "fmt " && body + 16 <= bytes.size) {
                tag = bb.getShort(body).toInt() and 0xFFFF
                ch = bb.getShort(body + 2).toInt()
                sr = bb.getInt(body + 4)
                bits = bb.getShort(body + 14).toInt()
                fmtOk = true
            } else if (id == "data") {
                if (!fmtOk || bits != 16 || ch < 1 || sr <= 0 || (tag != 1 && tag != 0xFFFE)) return null
                val avail = bytes.size - body
                val len = if (size <= 0 || size > avail) avail else size
                val frames = len / (2 * ch)
                val out = ShortArray(frames)
                for (f in 0 until frames) {
                    if (ch == 1) {
                        out[f] = bb.getShort(body + f * 2)
                    } else {
                        var sum = 0
                        for (c in 0 until ch) sum += bb.getShort(body + (f * ch + c) * 2)
                        out[f] = (sum / ch).toShort()
                    }
                }
                return Pcm(out, sr)
            }
            if (size < 0) return null
            pos = body + size + (size and 1)
        }
        return null
    }

    fun isValid(file: File): Boolean = read(file)?.let { it.samples.isNotEmpty() } == true
}
