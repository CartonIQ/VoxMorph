package com.cartoniq.voxmorph.viewmodel

import android.app.Activity
import android.app.Application
import android.database.sqlite.SQLiteException
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ads.AdManager
import com.cartoniq.voxmorph.audio.*
import com.cartoniq.voxmorph.data.*
import com.cartoniq.voxmorph.domain.*
import com.cartoniq.voxmorph.util.DownloadHelper
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class Screen { STUDIO, HISTORY, SETTINGS, TTS, ABOUT, PRIVACY }
enum class Phase { IDLE, RECORDING, STOPPING, PROCESSING, READY }
enum class TtsPhase { IDLE, GENERATING, READY }
enum class HistoryFilter { ALL, RECORDING, TTS }
enum class TtsLang(val locale: Locale) { EN(Locale.US), HI(Locale("hi", "IN")) }

data class Preview(
    val file: File, val durationMs: Long, val profile: VoiceProfile,
    val source: SourceType, val sampleRate: Int, val saved: Boolean = false
)

data class UiMessage(val res: Int, val stamp: Long = System.nanoTime())

data class UiState(
    val screen: Screen = Screen.STUDIO,
    val category: VoiceCategory = VoiceCategory.MALE,
    val selectedVoiceId: String = "man_1",
    val phase: Phase = Phase.IDLE,
    val levels: List<Float> = emptyList(),
    val elapsedMs: Long = 0,
    val pitch: Float = 0f,
    val formant: Float = 0f,
    val hasRaw: Boolean = false,
    val preview: Preview? = null,
    val playingPath: String? = null,
    val rewardRemainingMs: Long = 0,
    val message: UiMessage? = null,
    // TTS
    val ttsText: String = "",
    val ttsCategory: VoiceCategory = VoiceCategory.MALE,
    val ttsVoiceId: String = "man_1",
    val ttsPitch: Float = 0f,
    val ttsFormant: Float = 0f,
    val ttsSpeed: Float = 1f,
    val ttsLang: TtsLang = TtsLang.EN,
    val ttsAutoSave: Boolean = true,
    val ttsPhase: TtsPhase = TtsPhase.IDLE,
    // History / settings
    val historyFilter: HistoryFilter = HistoryFilter.ALL,
    val history: List<RecordingEntity> = emptyList(),
    val storageBytes: Long = 0,
    val storageItems: Int = 0
)

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModel(app: Application) : AndroidViewModel(app) {
    private val appContext: Application = app
    private var rawPcm: WavIO.Pcm? = null

    companion object {
        const val MAX_RECORD_MS = 60_000L
        const val MIN_RECORD_MS = 500L
        const val TTS_LIMIT = 500
        private const val MAX_LEVELS = 64
    }

    private val _state = MutableStateFlow(UiState())
    val state: StateFlow<UiState> = _state.asStateFlow()

    val ads = AdManager(app)
    private val rewards = RewardStore(app)
    private val repo = RecordingRepository(app, AppDatabase.get(app).dao())
    private val engine = AudioEngine()
    private val tts = TtsManager(app)
    private val player = PlaybackController()
    private val tempDir = File(app.cacheDir, "temp")
    private var recordJob: Job? = null
    private var ttsJob: Job? = null
    private val filterFlow = MutableStateFlow(HistoryFilter.ALL)

    init {
        tempDir.listFiles()?.forEach { it.delete() }
        viewModelScope.launch {
            runCatching { repo.reconcile() }
            refreshStorage()
        }
        viewModelScope.launch {
            filterFlow.flatMapLatest {
                when (it) {
                    HistoryFilter.ALL -> repo.observeAll()
                    HistoryFilter.RECORDING -> repo.observeByType(SourceType.RECORDING)
                    HistoryFilter.TTS -> repo.observeByType(SourceType.TTS)
                }
            }.catch { say(R.string.err_db) }
                .collect { list -> _state.update { it.copy(history = list) }; refreshStorage() }
        }
        viewModelScope.launch {
            while (true) {
                val rem = rewards.remainingMs() / 1000 * 1000
                _state.update { s ->
                    var n = s.copy(rewardRemainingMs = rem)
                    if (rem == 0L) {
                        if (!VoiceCatalog.get(n.selectedVoiceId).isFree)
                            n = n.copy(selectedVoiceId = VoiceCatalog.defaultFree(n.category).id)
                        if (!VoiceCatalog.get(n.ttsVoiceId).isFree)
                            n = n.copy(ttsVoiceId = VoiceCatalog.defaultFree(n.ttsCategory).id)
                    }
                    n
                }
                delay(1000)
            }
        }
    }

    // ---------- helpers ----------
    private fun say(res: Int) = _state.update { it.copy(message = UiMessage(res)) }
    fun consumeMessage(m: UiMessage) = _state.update { if (it.message == m) it.copy(message = null) else it }
    private fun entitled(p: VoiceProfile) = p.isFree || rewards.remainingMs() > 0
    private fun toError(t: Throwable): AppError = when (t) {
        is AppException -> t.error
        is SQLiteException -> AppError.DB
        is IOException -> AppError.STORAGE
        is OutOfMemoryError -> AppError.DSP
        else -> AppError.GENERIC
    }
    private fun newTemp(prefix: String) = File(tempDir, "${prefix}_${System.currentTimeMillis()}.wav").also { tempDir.mkdirs() }
    private suspend fun refreshStorage() {
        val (b, n) = repo.storageStats()
        _state.update { it.copy(storageBytes = b, storageItems = n) }
    }

    // ---------- navigation / selection ----------
    fun navigate(s: Screen) { stopPlayback(); _state.update { it.copy(screen = s) } }
    fun selectCategory(c: VoiceCategory) = _state.update { it.copy(category = c) }
    fun selectVoice(id: String) {
        val p = VoiceCatalog.get(id)
        if (!entitled(p)) { say(R.string.locked_voice); return }
        _state.update { it.copy(selectedVoiceId = id, category = p.category) }
    }
    fun setPitch(v: Float) = _state.update { it.copy(pitch = v) }
    fun setFormant(v: Float) = _state.update { it.copy(formant = v) }

    // ---------- recording ----------
    fun onRecordTapped() {
        val s = _state.value
        if (s.ttsPhase == TtsPhase.GENERATING) return
        when (s.phase) {
            Phase.RECORDING -> stopRecording()
            Phase.IDLE, Phase.READY -> startRecording()
            else -> Unit
        }
    }

    private fun startRecording() {
        val profile = VoiceCatalog.get(_state.value.selectedVoiceId)
        if (!entitled(profile)) {
            _state.update { it.copy(selectedVoiceId = VoiceCatalog.defaultFree(profile.category).id) }
            say(R.string.locked_voice); return
        }
        stopPlayback()
        discardPreview()
        engine.prepare()
        _state.update { it.copy(phase = Phase.RECORDING, levels = emptyList(), elapsedMs = 0) }
        recordJob = viewModelScope.launch {
            val tmp = newTemp("rec")
            val out = newTemp("preview")
            try {
                val cap = engine.capture(tmp, MAX_RECORD_MS) { level, elapsed ->
                    _state.update { s -> s.copy(levels = (s.levels + level).takeLast(MAX_LEVELS), elapsedMs = elapsed) }
                }
                _state.update { it.copy(phase = Phase.PROCESSING) }
                val pcm = withContext(Dispatchers.IO) { WavIO.read(tmp) } ?: throw AppException(AppError.RECORD_FAILED)
                if (pcm.samples.size * 1000L / pcm.sampleRate < MIN_RECORD_MS) throw AppException(AppError.TOO_SHORT)
                rawPcm = pcm
                val processed = dsp(pcm, profile, _state.value.pitch, _state.value.formant)
                withContext(Dispatchers.IO) { WavIO.write(out, processed, cap.sampleRate) }
                val p = Preview(out, processed.size * 1000L / cap.sampleRate, profile, SourceType.RECORDING, cap.sampleRate)
                _state.update { it.copy(phase = Phase.READY, preview = p, hasRaw = true) }
            } catch (e: CancellationException) {
                out.delete(); throw e
            } catch (t: Throwable) {
                out.delete()
                rawPcm = null
                _state.update { it.copy(phase = Phase.IDLE, hasRaw = false) }
                say(toError(t).res)
            } finally {
                tmp.delete()
            }
        }
    }

    fun stopRecording() {
        if (_state.value.phase != Phase.RECORDING) return
        _state.update { it.copy(phase = Phase.STOPPING) }
        engine.requestStop()
    }

    /** Called when the app leaves the foreground: recording ends and is processed, playback stops. */
    fun onBackgrounded() {
        stopRecording()
        stopPlayback()
    }

    private suspend fun dsp(pcm: WavIO.Pcm, profile: VoiceProfile, extraPitch: Float, extraFormant: Float): ShortArray =
        withContext(Dispatchers.Default) {
            try {
                VoiceProcessor.process(pcm.samples, pcm.sampleRate, profile, extraPitch, extraFormant)
            } catch (e: AppException) { throw e }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { throw AppException(AppError.DSP, e) }
        }

    // ---------- preview / save ----------
    fun discardPreview() {
        val p = _state.value.preview ?: return
        if (_state.value.playingPath == p.file.path) stopPlayback()
        p.file.delete()
        rawPcm = null
        _state.update { it.copy(preview = null, hasRaw = false, phase = if (it.phase == Phase.READY) Phase.IDLE else it.phase,
            ttsPhase = if (it.ttsPhase == TtsPhase.READY) TtsPhase.IDLE else it.ttsPhase) }
    }

    fun savePreview() {
        val p = _state.value.preview ?: return
        if (p.saved) return
        viewModelScope.launch { persist(p) }
    }

    private suspend fun persist(p: Preview) {
        try {
            val name = englishName(p.profile)
            repo.saveFromFile(p.file, p.profile, p.source, p.durationMs, p.sampleRate, name)
            _state.update { s -> s.copy(preview = s.preview?.takeIf { it.file == p.file }?.copy(saved = true) ?: s.preview) }
            say(R.string.saved_ok)
        } catch (e: CancellationException) { throw e }
        catch (t: Throwable) { say(toError(t).res) }
    }

    /** Re-applies the currently selected voice to the same recording, so you do not have to record again. */
    fun reprocess() {
        val raw = rawPcm ?: return
        val s = _state.value
        if (s.phase != Phase.READY) return
        val profile = VoiceCatalog.get(s.selectedVoiceId)
        if (!entitled(profile)) { say(R.string.locked_voice); return }
        stopPlayback()
        val old = s.preview
        _state.update { it.copy(phase = Phase.PROCESSING) }
        recordJob = viewModelScope.launch {
            val out = newTemp("preview")
            try {
                val processed = dsp(raw, profile, _state.value.pitch, _state.value.formant)
                withContext(Dispatchers.IO) { WavIO.write(out, processed, raw.sampleRate) }
                old?.file?.delete()
                val p = Preview(out, processed.size * 1000L / raw.sampleRate, profile, SourceType.RECORDING, raw.sampleRate)
                _state.update { it.copy(phase = Phase.READY, preview = p) }
            } catch (e: CancellationException) {
                out.delete(); throw e
            } catch (t: Throwable) {
                out.delete()
                _state.update { it.copy(phase = Phase.READY) }
                say(toError(t).res)
            }
        }
    }

    // ---------- download ----------
    fun downloadName(voice: String, stamp: Long): String {
        val safe = voice.replace(Regex("[^A-Za-z0-9]+"), "_").trim('_').ifEmpty { "Voice" }
        val time = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(Date(stamp))
        return "VoxMorph_${safe}_$time.wav"
    }

    /** Android 10+: one-tap copy into Downloads/VoxMorph. */
    fun saveToDownloads(file: File, name: String) {
        if (!file.exists()) { say(R.string.err_file_missing); return }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { DownloadHelper.saveToDownloads(appContext, file, name) }
            say(if (ok) R.string.download_ok else R.string.download_failed)
        }
    }

    /** Android 7-9: copy to the location the user chose in the system "save as" screen. */
    fun saveToUri(file: File, uri: Uri) {
        if (!file.exists()) { say(R.string.err_file_missing); return }
        viewModelScope.launch {
            val ok = withContext(Dispatchers.IO) { DownloadHelper.copyToUri(appContext, file, uri) }
            say(if (ok) R.string.saved_file_ok else R.string.download_failed)
        }
    }

    fun englishName(p: VoiceProfile): String = when (p.category) {
        VoiceCategory.MALE -> "Man ${p.index}"
        VoiceCategory.FEMALE -> "Woman ${p.index}"
        VoiceCategory.EFFECTS -> p.id.replaceFirstChar { it.uppercase() }
    }

    // ---------- playback ----------
    fun togglePlay(file: File) {
        if (_state.value.playingPath == file.path) { stopPlayback(); return }
        if (!file.exists()) { say(R.string.err_file_missing); return }
        _state.update { it.copy(playingPath = file.path) }
        player.play(file,
            onComplete = { _state.update { s -> s.copy(playingPath = null) } },
            onError = { _state.update { s -> s.copy(playingPath = null) }; say(R.string.err_playback) })
    }

    fun stopPlayback() {
        player.stop()
        _state.update { if (it.playingPath != null) it.copy(playingPath = null) else it }
    }

    fun fileOf(e: RecordingEntity): File = repo.fileOf(e)

    // ---------- TTS ----------
    fun setTtsText(t: String) = _state.update { it.copy(ttsText = t.take(TTS_LIMIT)) }
    fun setTtsPitch(v: Float) = _state.update { it.copy(ttsPitch = v) }
    fun setTtsFormant(v: Float) = _state.update { it.copy(ttsFormant = v) }
    fun setTtsSpeed(v: Float) = _state.update { it.copy(ttsSpeed = v) }
    fun setTtsLang(l: TtsLang) = _state.update { it.copy(ttsLang = l) }
    fun setTtsAutoSave(b: Boolean) = _state.update { it.copy(ttsAutoSave = b) }
    fun selectTtsCategory(c: VoiceCategory) = _state.update { it.copy(ttsCategory = c) }
    fun selectTtsVoice(id: String) {
        val p = VoiceCatalog.get(id)
        if (!entitled(p)) { say(R.string.locked_voice); return }
        _state.update { it.copy(ttsVoiceId = id, ttsCategory = p.category) }
    }

    fun generateTts() {
        val s = _state.value
        if (s.ttsPhase == TtsPhase.GENERATING || s.phase != Phase.IDLE && s.phase != Phase.READY) return
        val text = s.ttsText.trim()
        if (text.isEmpty()) { say(R.string.err_text_empty); return }
        val profile = VoiceCatalog.get(s.ttsVoiceId)
        if (!entitled(profile)) { say(R.string.locked_voice); return }
        stopPlayback()
        discardPreview()
        _state.update { it.copy(ttsPhase = TtsPhase.GENERATING) }
        ttsJob = viewModelScope.launch {
            val raw = newTemp("tts_raw")
            val out = newTemp("preview")
            var ok = false
            try {
                tts.synthesize(text, s.ttsLang.locale, s.ttsSpeed, raw)
                val pcm = withContext(Dispatchers.IO) { AudioDecoder.decodeToMono(raw) }
                if (pcm.samples.size < pcm.sampleRate / 20) throw AppException(AppError.TTS_FAILED)
                val processed = dsp(pcm, profile, s.ttsPitch, s.ttsFormant)
                withContext(Dispatchers.IO) { WavIO.write(out, processed, pcm.sampleRate) }
                val p = Preview(out, processed.size * 1000L / pcm.sampleRate, profile, SourceType.TTS, pcm.sampleRate)
                _state.update { it.copy(preview = p, ttsPhase = TtsPhase.READY) }
                ok = true
                if (s.ttsAutoSave) persist(p)
            } catch (e: CancellationException) {
                throw e
            } catch (t: Throwable) {
                say(toError(t).res)
            } finally {
                raw.delete()
                if (!ok) out.delete()
                if (!ok) _state.update { it.copy(ttsPhase = TtsPhase.IDLE) }
            }
        }
    }

    // ---------- history ----------
    fun setFilter(f: HistoryFilter) { filterFlow.value = f; _state.update { it.copy(historyFilter = f) } }

    fun deleteRecording(e: RecordingEntity) {
        viewModelScope.launch {
            if (_state.value.playingPath == repo.fileOf(e).path) stopPlayback()
            try { if (!repo.delete(e)) say(R.string.err_delete_failed) }
            catch (c: CancellationException) { throw c }
            catch (t: Throwable) { say(toError(t).res) }
        }
    }

    fun clearHistory() {
        viewModelScope.launch {
            stopPlayback()
            try {
                if (repo.clearAll() > 0) say(R.string.err_delete_failed) else say(R.string.cleared_ok)
            } catch (c: CancellationException) { throw c }
            catch (t: Throwable) { say(toError(t).res) }
        }
    }

    // ---------- ads / rewards ----------
    fun watchAd(activity: Activity) {
        val r = ads.showRewarded(activity,
            onEarned = { rewards.grant(); _state.update { it.copy(rewardRemainingMs = rewards.remainingMs()) }; say(R.string.reward_granted) },
            onClosedWithoutReward = { say(R.string.reward_not_earned) })
        when (r) {
            AdManager.Availability.NOT_READY -> say(R.string.ad_unavailable)
            AdManager.Availability.NO_CONSENT -> say(R.string.ad_consent_needed)
            AdManager.Availability.OK -> Unit
        }
    }

    override fun onCleared() {
        recordJob?.cancel(); ttsJob?.cancel()
        engine.requestStop()
        player.stop()
        tts.shutdown()
        super.onCleared()
    }
}
