package com.cartoniq.voxmorph.data

import android.content.Context

/** Persists the rewarded-unlock expiry as epoch millis so it survives process death. */
class RewardStore(context: Context) {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    fun remainingMs(now: Long = System.currentTimeMillis()): Long {
        val expiry = prefs.getLong(KEY_EXPIRY, 0L)
        val rem = expiry - now
        // A clock moved far back must not give a huge window.
        return if (rem in 1..DURATION_MS) rem else 0L
    }

    fun grant(now: Long = System.currentTimeMillis()) {
        prefs.edit().putLong(KEY_EXPIRY, now + DURATION_MS).apply()
    }

    companion object {
        const val PREFS = "vox_prefs"
        private const val KEY_EXPIRY = "reward_expiry_epoch_ms"
        const val DURATION_MS = 10 * 60 * 1000L
    }
}
