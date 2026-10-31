package com.cartoniq.voxmorph.util

import android.content.ClipData
import android.content.Context
import android.content.Intent
import androidx.core.content.FileProvider
import java.io.File

object ShareHelper {
    /** Shares via ACTION_SEND with a content:// URI and a read grant. Returns false if the file is missing. */
    fun share(context: Context, file: File, chooserTitle: String): Boolean {
        if (!file.exists()) return false
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", file)
        val send = Intent(Intent.ACTION_SEND).apply {
            type = "audio/wav"
            putExtra(Intent.EXTRA_STREAM, uri)
            clipData = ClipData.newRawUri("", uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        context.startActivity(Intent.createChooser(send, chooserTitle))
        return true
    }
}
