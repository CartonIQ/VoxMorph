package com.cartoniq.voxmorph.ui.components

import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.cartoniq.voxmorph.viewmodel.MainViewModel
import java.io.File

/**
 * Returns a "download" action: (file, suggested name) -> saves a copy to the public Downloads folder.
 * Android 10+ saves in one tap; Android 7-9 opens the system "save as" picker (no storage permission).
 */
@Composable
fun rememberDownloader(vm: MainViewModel): (File, String) -> Unit {
    var pending by remember { mutableStateOf<File?>(null) }
    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("audio/wav")) { uri ->
        val f = pending
        pending = null
        if (uri != null && f != null) vm.saveToUri(f, uri)
    }
    return remember(vm) {
        { file: File, name: String ->
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                vm.saveToDownloads(file, name)
            } else {
                pending = file
                launcher.launch(name)
            }
        }
    }
}
