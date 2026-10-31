package com.cartoniq.voxmorph.audio

import android.content.Context
import android.os.Bundle
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import com.cartoniq.voxmorph.domain.AppError
import com.cartoniq.voxmorph.domain.AppException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeout
import java.io.File
import java.util.Locale
import java.util.UUID
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/** Wraps android.speech.tts.TextToSpeech. Always synthesises to a file; speaker output is never recorded. */
class TtsManager(private val context: Context) {
    private var tts: TextToSpeech? = null
    private var ready = false

    private suspend fun ensureReady(): TextToSpeech {
        val existing = tts
        if (existing != null && ready) return existing
        return suspendCancellableCoroutine { cont ->
            val holder = arrayOfNulls<TextToSpeech>(1)
            holder[0] = TextToSpeech(context.applicationContext) { status ->
                val engine = holder[0]
                if (status == TextToSpeech.SUCCESS && engine != null) {
                    tts = engine
                    ready = true
                    if (cont.isActive) cont.resume(engine)
                } else {
                    engine?.shutdown()
                    if (cont.isActive) cont.resumeWithException(AppException(AppError.TTS_INIT))
                }
            }
            cont.invokeOnCancellation { if (!ready) holder[0]?.shutdown() }
        }
    }

    suspend fun synthesize(text: String, locale: Locale, speechRate: Float, out: File) {
        val engine = ensureReady()
        when (engine.isLanguageAvailable(locale)) {
            TextToSpeech.LANG_MISSING_DATA -> throw AppException(AppError.TTS_MISSING_DATA)
            TextToSpeech.LANG_NOT_SUPPORTED -> throw AppException(AppError.TTS_UNSUPPORTED)
        }
        val r = engine.setLanguage(locale)
        if (r == TextToSpeech.LANG_MISSING_DATA) throw AppException(AppError.TTS_MISSING_DATA)
        if (r == TextToSpeech.LANG_NOT_SUPPORTED) throw AppException(AppError.TTS_UNSUPPORTED)
        engine.setSpeechRate(speechRate)

        out.parentFile?.mkdirs()
        val utteranceId = "vm_" + UUID.randomUUID()
        val done = CompletableDeferred<Boolean>()
        engine.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(id: String?) {}
            override fun onDone(id: String?) { if (id == utteranceId) done.complete(true) }
            @Deprecated("Deprecated in Java")
            override fun onError(id: String?) { if (id == utteranceId) done.complete(false) }
            override fun onError(id: String?, errorCode: Int) { if (id == utteranceId) done.complete(false) }
        })
        if (engine.synthesizeToFile(text, Bundle(), out, utteranceId) != TextToSpeech.SUCCESS) {
            throw AppException(AppError.TTS_FAILED)
        }
        val ok = try {
            withTimeout(60_000) { done.await() }
        } catch (e: TimeoutCancellationException) {
            engine.stop()
            throw AppException(AppError.TTS_FAILED, e)
        } catch (e: CancellationException) {
            engine.stop()
            throw e
        }
        if (!ok || !out.exists() || out.length() == 0L) throw AppException(AppError.TTS_FAILED)
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        tts = null
        ready = false
    }
}
