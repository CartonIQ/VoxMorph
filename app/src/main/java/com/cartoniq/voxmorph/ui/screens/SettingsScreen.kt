package com.cartoniq.voxmorph.ui.screens

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ui.components.*
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.util.LocaleHelper
import com.cartoniq.voxmorph.viewmodel.*
import java.util.Locale

@Composable
fun SettingsScreen(vm: MainViewModel, s: UiState, privacyOptionsRequired: Boolean) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    val currentLang = LocaleHelper.saved(ctx) ?: activity.resources.configuration.locales[0].language
    var confirmClear by remember { mutableStateOf(false) }
    val version = remember { runCatching { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName }.getOrNull() ?: "" }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconButton(R.drawable.ic_back, stringResource(R.string.back), { vm.navigate(Screen.STUDIO) }, tint = Color.White)
            Text(stringResource(R.string.settings), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_language), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                SegmentedTabs(
                    listOf(stringResource(R.string.lang_en), stringResource(R.string.lang_hi)),
                    if (currentLang.startsWith("hi")) 1 else 0,
                    { i ->
                        val tag = if (i == 1) "hi" else "en"
                        if (tag != currentLang) { LocaleHelper.save(ctx, tag); activity.recreate() }
                    }
                )
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_privacy), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_privacy_text), color = Neon.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { vm.navigate(Screen.PRIVACY) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.settings_privacy_full))
                }
                Spacer(Modifier.height(8.dp))
                if (privacyOptionsRequired) {
                    OutlinedButton(onClick = { vm.ads.showPrivacyOptions(activity) }, modifier = Modifier.heightIn(min = 48.dp)) {
                        Text(stringResource(R.string.settings_consent))
                    }
                } else {
                    Text(stringResource(R.string.settings_consent) + ": " + stringResource(R.string.settings_consent_none),
                        color = Neon.Muted, fontSize = 14.sp)
                }
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_perm_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_perm_text), color = Neon.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = { ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null))) },
                    modifier = Modifier.heightIn(min = 48.dp)
                ) { Text(stringResource(R.string.open_app_settings)) }
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_storage), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.settings_storage_value,
                    String.format(Locale.US, "%.1f", s.storageBytes / 1_048_576.0), s.storageItems),
                    color = Neon.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { confirmClear = true }, enabled = s.storageItems > 0, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.clear_all), color = if (s.storageItems > 0) Neon.Danger else Neon.Muted)
                }
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.settings_about_title), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_tagline), color = Neon.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = { vm.navigate(Screen.ABOUT) }, modifier = Modifier.heightIn(min = 48.dp)) {
                    Text(stringResource(R.string.settings_about_open))
                }
            }
            Text(stringResource(R.string.settings_version) + ": " + version, color = Neon.Muted, fontSize = 14.sp,
                modifier = Modifier.padding(4.dp))
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text(stringResource(R.string.clear_all)) },
            text = { Text(stringResource(R.string.clear_all_text)) },
            confirmButton = { TextButton(onClick = { vm.clearHistory(); confirmClear = false }) { Text(stringResource(R.string.delete), color = Neon.Danger) } },
            dismissButton = { TextButton(onClick = { confirmClear = false }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}
