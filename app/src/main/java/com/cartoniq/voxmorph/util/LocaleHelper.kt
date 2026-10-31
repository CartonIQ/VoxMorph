package com.cartoniq.voxmorph.util

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/** In-app English/Hindi switch without extra libraries: wraps the Activity context with the saved locale. */
object LocaleHelper {
    private const val PREFS = "vox_prefs"
    private const val KEY = "lang"

    fun saved(context: Context): String? =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, null)

    fun save(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, tag).apply()
    }

    fun wrap(base: Context): Context {
        val tag = saved(base) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val cfg = Configuration(base.resources.configuration)
        cfg.setLocale(locale)
        return base.createConfigurationContext(cfg)
    }
}
