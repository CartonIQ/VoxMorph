package com.cartoniq.voxmorph.audio

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlin.math.*

/**
 * Pitch-synchronous overlap-add (TD-PSOLA) voice engine.
 *
 * Pitch and formants are controlled independently:
 *  - formant shift  = resample by the formant ratio (moves pitch and formants together), then
 *  - PSOLA restores the original duration and corrects the pitch to the requested value.
 * Voiced speech is re-synthesised period by period, so the vocal-tract colour (formants) is kept
 * while the pitch moves. Unvoiced parts (s, f, breath) keep their natural timing.
 */
internal object Psola {
    private class Track(val period: IntArray, val hop: Int)
    private class Marks(val pos: IntArray, val per: IntArray, val voiced: BooleanArray)

    /**
     * @param monotoneHz when above 0 every voiced period is forced to this pitch (robot voice).
     */
    suspend fun process(x: FloatArray, sr: Int, pitchSemis: Float, formantSemis: Float, monotoneHz: Float): FloatArray {
        val noPitch = abs(pitchSemis) < 0.05f && monotoneHz <= 0f
        val noFormant = abs(formantSemis) < 0.05f
        if (noPitch && noFormant) return x

        val beta = 2.0.pow(pitchSemis / 12.0)
        val phi = if (noFormant) 1.0 else 2.0.pow(formantSemis / 12.0)
        var y = x
        if (phi != 1.0) {
            if (phi > 1.0) y = Dsp.lowpass(y, sr, 0.45 * sr / phi)
            y = Dsp.resample(y, phi)
        }
        val mono = if (monotoneHz > 0f) max(1, (sr / monotoneHz).roundToInt()) else 0
        return synth(y, sr, beta / phi, phi, mono)
    }

    // ---------- analysis ----------

    private suspend fun track(x: FloatArray, sr: Int): Track {
        val n = x.size
        val d = max(1, sr / 11025)
        val nd = n / d
        val xd = FloatArray(nd)
        for (i in 0 until nd) {
            var s = 0f
            for (j in 0 until d) s += x[i * d + j]
            xd[i] = s / d
        }
        val sd = sr / d
        val minLag = max(2, sd / 400)
        val maxLag = sd / 70
        val w = (sd * 0.04).toInt()
        val hop = max(1, sr / 100)
        val nF = n / hop + 1
        val period = IntArray(nF)
        if (nd < w + maxLag + 2) return Track(period, hop)

        var gpeak = 0f
        for (v in x) gpeak = max(gpeak, abs(v))
        val thr = max(0.003f, 0.015f * gpeak)
        val r = FloatArray(maxLag + 2)

        for (f in 0 until nF) {
            if (f % 32 == 0) currentCoroutineContext().ensureActive()
            val c = (f * hop) / d
            val a = (c - w / 2).coerceIn(0, nd - w - maxLag - 1)
            var e0 = 0f
            for (i in a until a + w) e0 += xd[i] * xd[i]
            if (sqrt(e0 / w) < thr) continue

            var best = 0f
            for (lag in minLag..maxLag) {
                var sxy = 0f
                var e1 = 0f
                for (i in a until a + w) {
                    val y = xd[i + lag]
                    sxy += xd[i] * y
                    e1 += y * y
                }
                val den = sqrt(e0 * e1)
                val v = if (den > 1e-9f) sxy / den else 0f
                r[lag] = v
                if (v > best) best = v
            }
            if (best < 0.5f) continue

            // Smallest strong peak: avoids picking a multiple of the real period.
            var lag0 = -1
            for (lag in (minLag + 1) until maxLag) {
                if (r[lag] >= 0.85f * best && r[lag] >= r[lag - 1] && r[lag] >= r[lag + 1]) {
                    lag0 = lag
                    break
                }
            }
            if (lag0 < 0) continue
            period[f] = refine(x, f * hop, lag0 * d, d, w * d)
        }

        val out = period.copyOf()
        for (f in 1 until nF - 1) {
            val p0 = period[f - 1]
            val p1 = period[f]
            val p2 = period[f + 1]
            if (p0 > 0 && p1 > 0 && p2 > 0) out[f] = max(min(p0, p1), min(max(p0, p1), p2))
        }
        return Track(out, hop)
    }

    /** Re-measures the period at full sample rate around the coarse estimate. */
    private fun refine(x: FloatArray, center: Int, lagF: Int, d: Int, wf: Int): Int {
        val n = x.size
        val maxL = lagF + d
        if (n < wf + maxL + 2) return lagF
        val a = (center - wf / 2).coerceIn(0, n - wf - maxL - 1)
        var e0 = 0f
        for (i in a until a + wf) e0 += x[i] * x[i]
        var bestLag = lagF
        var bestV = -2f
        for (lag in (lagF - d)..(lagF + d)) {
            var sxy = 0f
            var e1 = 0f
            for (i in a until a + wf) {
                val y = x[i + lag]
                sxy += x[i] * y
                e1 += y * y
            }
            val den = sqrt(e0 * e1)
            val v = if (den > 1e-9f) sxy / den else 0f
            if (v > bestV) {
                bestV = v
                bestLag = lag
            }
        }
        return bestLag
    }

    private fun marks(x: FloatArray, sr: Int, t: Track): Marks {
        val n = x.size
        val uT = max(8, sr / 100)
        val mp = ArrayList<Int>()
        val mt = ArrayList<Int>()
        val mv = ArrayList<Boolean>()
        var pos = 0
        while (pos < n) {
            val f = min(pos / t.hop, t.period.size - 1)
            val tp = t.period[f]
            if (tp == 0) {
                var p = pos
                if (mp.isNotEmpty() && p <= mp[mp.size - 1]) p = mp[mp.size - 1] + 1
                mp.add(p)
                mt.add(uT)
                mv.add(false)
                pos = p + uT
            } else {
                val lo = max(0, pos - tp / 4)
                val hi = min(n - 1, pos + tp / 4)
                var p = lo
                for (i in lo..hi) if (x[i] > x[p]) p = i
                if (mp.isNotEmpty() && p <= mp[mp.size - 1]) p = mp[mp.size - 1] + 1
                mp.add(p)
                mt.add(tp)
                mv.add(true)
                pos = p + tp
            }
        }
        return Marks(mp.toIntArray(), mt.toIntArray(), mv.toBooleanArray())
    }

    // ---------- synthesis ----------

    private suspend fun synth(x: FloatArray, sr: Int, pitchRatio: Double, timeScale: Double, monoPeriod: Int): FloatArray {
        val n = x.size
        if (n < sr / 20) return x
        val m = marks(x, sr, track(x, sr))
        val cnt = m.pos.size
        if (cnt == 0) return x

        val outLen = max(1, (n * timeScale).roundToInt())
        val out = FloatArray(outLen)
        val ws = FloatArray(outLen)
        var ts = 0
        var idx = 0
        var guard = 0
        while (ts < outLen) {
            if (guard++ % 256 == 0) currentCoroutineContext().ensureActive()
            val ta = (ts / timeScale).toInt()
            while (idx + 1 < cnt && abs(m.pos[idx + 1] - ta) <= abs(m.pos[idx] - ta)) idx++
            val center = m.pos[idx]
            val per = m.per[idx]
            val step = if (m.voiced[idx]) {
                if (monoPeriod > 0) monoPeriod else max(1, (per / pitchRatio).roundToInt())
            } else {
                per
            }
            val half = max(per, step)
            val len = 2 * half
            for (k in 0 until len) {
                val si = center - half + k
                val di = ts - half + k
                if (si < 0 || si >= n || di < 0 || di >= outLen) continue
                val w = (0.5 - 0.5 * cos(2.0 * PI * (k + 0.5) / len)).toFloat()
                out[di] += x[si] * w
                ws[di] += w
            }
            ts += max(1, step)
        }
        return FloatArray(outLen) { if (ws[it] > 1e-3f) out[it] / ws[it] else 0f }
    }
}
