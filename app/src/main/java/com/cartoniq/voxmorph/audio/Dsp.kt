package com.cartoniq.voxmorph.audio

import kotlin.math.*

/** Second-order (RBJ) low-pass / high-pass filter. */
internal class Biquad(lowpass: Boolean, sr: Int, freq: Double, q: Double = 0.7071) {
    private val b0: Double
    private val b1: Double
    private val b2: Double
    private val a1: Double
    private val a2: Double
    private var z1 = 0.0
    private var z2 = 0.0

    init {
        val fc = freq.coerceIn(20.0, sr * 0.45)
        val w = 2 * PI * fc / sr
        val c = cos(w)
        val al = sin(w) / (2 * q)
        val a0 = 1 + al
        if (lowpass) {
            b0 = (1 - c) / 2 / a0
            b1 = (1 - c) / a0
            b2 = (1 - c) / 2 / a0
        } else {
            b0 = (1 + c) / 2 / a0
            b1 = -(1 + c) / a0
            b2 = (1 + c) / 2 / a0
        }
        a1 = -2 * c / a0
        a2 = (1 - al) / a0
    }

    fun run(x: Float): Float {
        val y = b0 * x + z1
        z1 = b1 * x - a1 * y + z2
        z2 = b2 * x - a2 * y
        return y.toFloat()
    }
}

internal object Dsp {
    private fun at(x: FloatArray, k: Int): Float = x[k.coerceIn(0, x.size - 1)]

    /** Catmull-Rom cubic interpolation at a fractional position. */
    fun cubic(x: FloatArray, pos: Double): Float {
        val i = floor(pos).toInt()
        val t = (pos - i).toFloat()
        val p0 = at(x, i - 1)
        val p1 = at(x, i)
        val p2 = at(x, i + 1)
        val p3 = at(x, i + 2)
        return p1 + 0.5f * t * (p2 - p0 + t * (2f * p0 - 5f * p1 + 4f * p2 - p3 + t * (3f * (p1 - p2) + p3 - p0)))
    }

    /** Reads the signal [factor] times faster (factor 2 = one octave up and half the length). */
    fun resample(x: FloatArray, factor: Double): FloatArray {
        val n = max(1, (x.size / factor).toInt())
        return FloatArray(n) { cubic(x, it * factor) }
    }

    fun lowpass(x: FloatArray, sr: Int, hz: Double): FloatArray {
        val a = Biquad(true, sr, hz)
        val b = Biquad(true, sr, hz)
        return FloatArray(x.size) { b.run(a.run(x[it])) }
    }
}
