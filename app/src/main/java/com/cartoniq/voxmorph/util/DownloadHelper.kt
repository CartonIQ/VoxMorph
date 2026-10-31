package com.cartoniq.voxmorph.util

import android.content.ContentValues
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import androidx.annotation.RequiresApi
import java.io.File
import java.io.IOException

/** Copies an audio file out of the app's private storage into the user's public Downloads folder. */
object DownloadHelper {
    /**
     * Android 10+: writes to Downloads/VoxMorph through MediaStore (no storage permission needed).
     * Returns false on older Android (use the system "save as" picker there) or on any failure.
     */
    fun saveToDownloads(context: Context, src: File, displayName: String): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.Q) return false
        return insertIntoDownloads(context, src, displayName)
    }

    @RequiresApi(Build.VERSION_CODES.Q)
    private fun insertIntoDownloads(context: Context, src: File, displayName: String): Boolean {
        val resolver = context.contentResolver
        val values = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, displayName)
            put(MediaStore.MediaColumns.MIME_TYPE, "audio/wav")
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/VoxMorph")
            put(MediaStore.MediaColumns.IS_PENDING, 1)
        }
        val uri = resolver.insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values) ?: return false
        return try {
            val out = resolver.openOutputStream(uri) ?: throw IOException("no output stream")
            out.use { o -> src.inputStream().use { it.copyTo(o) } }
            val done = ContentValues()
            done.put(MediaStore.MediaColumns.IS_PENDING, 0)
            resolver.update(uri, done, null, null)
            true
        } catch (e: Exception) {
            runCatching { resolver.delete(uri, null, null) }
            false
        }
    }

    /** Writes the file to a location the user picked with the system "save as" screen. */
    fun copyToUri(context: Context, src: File, uri: Uri): Boolean = try {
        val out = context.contentResolver.openOutputStream(uri) ?: throw IOException("no output stream")
        out.use { o -> src.inputStream().use { it.copyTo(o) } }
        true
    } catch (e: Exception) {
        false
    }
}
