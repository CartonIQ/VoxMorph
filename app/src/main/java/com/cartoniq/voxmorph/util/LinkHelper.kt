package com.cartoniq.voxmorph.util

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri

object LinkHelper {
    const val SUPPORT_EMAIL = "cartoniq.business@gmail.com"
    const val GITHUB_URL = "https://github.com/CartonIQ"

    fun email(context: Context, subject: String) {
        val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:$SUPPORT_EMAIL")).putExtra(Intent.EXTRA_SUBJECT, subject)
        try {
            context.startActivity(intent)
        } catch (e: ActivityNotFoundException) {
            // No mail app installed: nothing to open.
        }
    }

    fun open(context: Context, url: String) {
        try {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
        } catch (e: ActivityNotFoundException) {
            // No browser installed: nothing to open.
        }
    }
}
