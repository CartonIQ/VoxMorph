package com.cartoniq.voxmorph.audio

import com.cartoniq.voxmorph.domain.AppError
import com.cartoniq.voxmorph.domain.AppException
import com.cartoniq.voxmorph.domain.EffectType
import com.cartoniq.voxmorph.domain.VoiceProfile
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

/**
 * Offline voice engine in plain Kotlin on float PCM (no network, no native code).
 *
 * Chain: rumble filter -> PSOLA pitch + formant shift -> tone tilt -> effect preset
 *        -> gentle compressor -> fade + peak normalisation (never clips).
 *
 * Pitch and formants are separate controls, so a "woman" voice raises pitch AND moves the vocal-tract
 * colour up, instead of just sounding like a sped-up tape.
 */
object VoiceProcessor {
    private const val TARGET_PEAK = 0.89f // about -1 dBFS
    private const val ROBOT_HZ = 110f

    suspend fun process(
        input: ShortArray, sr: Int, profile: VoiceProfile, extraPitch: Float, extraFormant: Float = 0f
    ): ShortArray {
        var x = FloatArray(input.size) { input[it] / 32768f }
        var peak = 0f
        for (v in x) peak = max(peak, abs(v))
        if (x.isEmpty() || peak < 0.004f) throw AppException(AppError.SILENCE)
        currentCoroutineContext().ensureActive()

        x = rumbleFilter(x, sr)
        val pitch = (profile.pitchSemitones + extraPitch).coerceIn(-16f, 16f)
        val formant = (profile.formantSemitones + extraFormant).coerceIn(-12f, 12f)
        val mono = if (profile.effect == EffectType.ROBOT) ROBOT_HZ * 2f.pow(pitch / 12f) else 0f
        x = Psola.process(x, sr, pitch, formant, mono)
        currentCoroutineContext().ensureActive()
        x = tilt(x, profile.tilt, sr)
        x = effect(x, profile.effect, sr)
        currentCoroutineContext().ensureActive()
        x = compress(x, sr)
        return finish(x, sr)
    }

    // ---------- filters / effects ----------

    private fun rumbleFilter(x: FloatArray, sr: Int): FloatArray {
        val hp = Biquad(false, sr, 60.0)
        return FloatArray(x.size) { hp.run(x[it]) }
    }

    private fun filter(x: FloatArray, lowpass: Boolean, sr: Int, hz: Double): FloatArray {
        val bq = Biquad(lowpass, sr, hz)
        return FloatArray(x.size) { bq.run(x[it]) }
    }

    private fun bandpass(x: FloatArray, sr: Int, lo: Double, hi: Double, passes: Int = 1): FloatArray {
        var y = x
        repeat(passes) { y = filter(filter(y, false, sr, lo), true, sr, hi) }
        return y
    }

    private fun tilt(x: FloatArray, t: Float, sr: Int): FloatArray {
        if (abs(t) < 0.01f) return x
        val a = 1f - exp(-2.0 * PI * 1500.0 / sr).toFloat()
        var lp = 0f
        return FloatArray(x.size) {
            lp += a * (x[it] - lp)
            if (t > 0) x[it] + t * (x[it] - lp) else x[it] * (1 + t) + (-t) * lp
        }
    }

    private fun ringMod(x: FloatArray, sr: Int, hz: Double, mix: Float): FloatArray {
        val w = 2 * PI * hz / sr
        return FloatArray(x.size) { x[it] * ((1 - mix) + mix * sin(w * it).toFloat()) }
    }

    private fun pad(x: FloatArray, sr: Int, seconds: Float) = x.copyOf(x.size + (sr * seconds).toInt())

    /** Feedback delay: y[i] = x[i] + fb * y[i-d]. */
    private fun feedback(x: FloatArray, sr: Int, ms: Float, fb: Float): FloatArray {
        val d = max(1, (sr * ms / 1000f).toInt())
        val y = FloatArray(x.size)
        for (i in x.indices) y[i] = x[i] + if (i >= d) y[i - d] * fb else 0f
        return y
    }

    private fun vibrato(x: FloatArray, sr: Int, depthMs: Float, rateHz: Double): FloatArray {
        val depth = sr * depthMs / 1000f
        val w = 2 * PI * rateHz / sr
        return FloatArray(x.size) { i ->
            val d = depth * (1f + sin(w * i).toFloat())
            val p = i - d
            if (p < 0) 0f else {
                val i0 = p.toInt()
                val fr = p - i0
                val a = x[i0]
                val b = if (i0 + 1 < x.size) x[i0 + 1] else a
                a + (b - a) * fr
            }
        }
    }

    private fun tremolo(x: FloatArray, sr: Int, rateHz: Double, depth: Float): FloatArray {
        val w = 2 * PI * rateHz / sr
        return FloatArray(x.size) { x[it] * (1f - depth + depth * 0.5f * (1f + sin(w * it).toFloat())) }
    }

    private fun softClip(x: FloatArray, drive: Float): FloatArray {
        val n = tanh(drive)
        return FloatArray(x.size) { tanh(drive * x[it]) / n }
    }

    private fun sampleHold(x: FloatArray, factor: Int): FloatArray =
        if (factor <= 1) x else FloatArray(x.size) { x[it - it % factor] }

    private fun effect(x: FloatArray, e: EffectType, sr: Int): FloatArray = when (e) {
        EffectType.NONE -> x
        // The pitch is already locked to a single note by the engine; ring-mod and comb add the metal.
        EffectType.ROBOT -> feedback(ringMod(x, sr, 70.0, 0.35f), sr, 6f, 0.45f)
        EffectType.ALIEN -> ringMod(vibrato(x, sr, 2.5f, 6.5), sr, 420.0, 0.3f)
        EffectType.GHOST -> {
            val wet = tremolo(vibrato(pad(x, sr, 1.6f), sr, 1.5f, 4.0), sr, 5.0, 0.35f)
            filter(feedback(wet, sr, 330f, 0.5f), true, sr, 4000.0)
        }
        EffectType.ECHO -> feedback(pad(x, sr, 1.5f), sr, 280f, 0.45f)
        EffectType.MEGAPHONE -> softClip(bandpass(x, sr, 500.0, 3200.0).let { y -> FloatArray(y.size) { y[it] * 2.5f } }, 3f)
        EffectType.CHIPMUNK -> x
        EffectType.MONSTER -> filter(softClip(ringMod(x, sr, 28.0, 0.35f), 2f), true, sr, 4500.0)
        EffectType.DEEP -> filter(x, true, sr, 5000.0)
        EffectType.RADIO -> softClip(bandpass(x, sr, 300.0, 4500.0).let { y -> FloatArray(y.size) { y[it] * 1.8f } }, 1.8f)
        EffectType.TELEPHONE -> softClip(bandpass(sampleHold(x, max(1, sr / 8000)), sr, 300.0, 3400.0, 2), 1.5f)
    }

    /** Smooths level differences so quiet words are not lost next to loud ones. */
    private fun compress(x: FloatArray, sr: Int): FloatArray {
        val threshold = 0.1f
        val ratio = 3f
        val attack = exp(-1.0 / (0.005 * sr)).toFloat()
        val release = exp(-1.0 / (0.12 * sr)).toFloat()
        var env = 0f
        return FloatArray(x.size) {
            val a = abs(x[it])
            env = if (a > env) attack * env + (1f - attack) * a else release * env + (1f - release) * a
            val g = if (env > threshold) (threshold + (env - threshold) / ratio) / env else 1f
            x[it] * g
        }
    }

    // ---------- output ----------

    private fun finish(x: FloatArray, sr: Int): ShortArray {
        val fade = min(x.size / 2, (sr * 0.005).toInt())
        for (i in 0 until fade) {
            val g = i / fade.toFloat()
            x[i] *= g
            x[x.size - 1 - i] *= g
        }
        var peak = 0f
        for (v in x) peak = max(peak, abs(v))
        val gain = if (peak > 1e-6f) min(TARGET_PEAK / peak, 8f) else 1f
        return ShortArray(x.size) { (x[it] * gain * 32767f).roundToInt().coerceIn(-32767, 32767).toShort() }
    }
}
