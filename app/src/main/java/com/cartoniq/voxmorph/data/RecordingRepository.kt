package com.cartoniq.voxmorph.data

import android.content.Context
import com.cartoniq.voxmorph.audio.WavIO
import com.cartoniq.voxmorph.domain.AppError
import com.cartoniq.voxmorph.domain.AppException
import com.cartoniq.voxmorph.domain.SourceType
import com.cartoniq.voxmorph.domain.VoiceProfile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.util.UUID

class RecordingRepository(context: Context, private val dao: RecordingDao) {
    val dir = File(context.filesDir, "recordings")

    fun observeAll(): Flow<List<RecordingEntity>> = dao.observeAll()
    fun observeByType(type: SourceType): Flow<List<RecordingEntity>> = dao.observeByType(type.name)
    fun fileOf(e: RecordingEntity) = File(dir, e.fileName)

    /** Copies a validated WAV into private storage, then inserts metadata. Removes the file if the insert fails. */
    suspend fun saveFromFile(
        src: File, profile: VoiceProfile, source: SourceType, durationMs: Long, sampleRate: Int, englishName: String
    ): RecordingEntity = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val dest = File(dir, "vm_${UUID.randomUUID()}.wav") // never built from user text
        try {
            src.copyTo(dest, overwrite = false)
            if (!WavIO.isValid(dest)) throw AppException(AppError.DECODE)
            val suffix = if (source == SourceType.TTS) "TTS" else "Recording"
            val e = RecordingEntity(
                title = "$englishName - $suffix", voiceId = profile.id, voiceDisplayName = englishName,
                category = profile.category.name, sourceType = source.name, fileName = dest.name,
                durationMs = durationMs, createdAtEpochMs = System.currentTimeMillis(),
                sampleRate = sampleRate, channels = 1, format = "WAV_PCM16", fileSizeBytes = dest.length()
            )
            e.copy(id = dao.insert(e))
        } catch (t: Throwable) {
            dest.delete()
            throw t
        }
    }

    /** Returns false when the file exists but could not be deleted (the row is then kept, not silently lost). */
    suspend fun delete(e: RecordingEntity): Boolean = withContext(Dispatchers.IO) {
        val f = fileOf(e)
        if (f.exists() && !f.delete()) return@withContext false
        dao.deleteById(e.id)
        true
    }

    suspend fun clearAll(): Int = withContext(Dispatchers.IO) {
        var failed = 0
        for (e in dao.getAll()) {
            val f = fileOf(e)
            if (f.exists() && !f.delete()) failed++ else dao.deleteById(e.id)
        }
        failed
    }

    /** Removes rows whose file vanished and files that have no row. */
    suspend fun reconcile() = withContext(Dispatchers.IO) {
        val rows = dao.getAll()
        rows.filter { !fileOf(it).exists() }.forEach { dao.deleteById(it.id) }
        val known = rows.map { it.fileName }.toSet()
        dir.listFiles()?.filter { it.name !in known }?.forEach { it.delete() }
    }

    suspend fun storageStats(): Pair<Long, Int> = withContext(Dispatchers.IO) {
        val files = dir.listFiles().orEmpty()
        files.sumOf { it.length() } to files.size
    }
}
