package com.cartoniq.voxmorph.domain

import androidx.annotation.StringRes
import com.cartoniq.voxmorph.R

enum class AppError(@StringRes val res: Int) {
    MIC_UNAVAILABLE(R.string.err_mic_unavailable),
    RECORD_FAILED(R.string.err_record_failed),
    STORAGE(R.string.err_storage),
    SILENCE(R.string.err_silence),
    TOO_SHORT(R.string.err_short),
    DSP(R.string.err_dsp),
    TTS_INIT(R.string.err_tts_init),
    TTS_UNSUPPORTED(R.string.err_tts_unsupported),
    TTS_MISSING_DATA(R.string.err_tts_missing_data),
    TTS_FAILED(R.string.err_tts_failed),
    DECODE(R.string.err_decode),
    DB(R.string.err_db),
    FILE_MISSING(R.string.err_file_missing),
    PLAYBACK(R.string.err_playback),
    TEXT_EMPTY(R.string.err_text_empty),
    DELETE_FAILED(R.string.err_delete_failed),
    GENERIC(R.string.err_generic)
}

class AppException(val error: AppError, cause: Throwable? = null) : Exception(error.name, cause)
