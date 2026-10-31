package com.cartoniq.voxmorph.ui.screens

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.data.RecordingEntity
import com.cartoniq.voxmorph.domain.SourceType
import com.cartoniq.voxmorph.domain.VoiceCatalog
import com.cartoniq.voxmorph.ui.components.*
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.util.ShareHelper
import com.cartoniq.voxmorph.viewmodel.*
import java.text.DateFormat
import java.util.Date

@Composable
fun HistoryScreen(vm: MainViewModel, s: UiState, canShowBanner: Boolean) {
    val ctx = LocalContext.current
    val download = rememberDownloader(vm)
    var toDelete by remember { mutableStateOf<RecordingEntity?>(null) }
    var confirmClear by remember { mutableStateOf(false) }
    val locale = LocalConfiguration.current.locales[0]
    val fmt = remember(locale) { DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, locale) }

    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(start = 16.dp, end = 4.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.history_title), color = Color.White, fontSize = 26.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.history_subtitle), color = Neon.Muted, fontSize = 14.sp)
            }
            NeonIconButton(R.drawable.ic_delete, stringResource(R.string.clear_all), { confirmClear = true },
                enabled = s.history.isNotEmpty(), tint = Neon.Danger)
        }
        SegmentedTabs(
            listOf(stringResource(R.string.filter_all), stringResource(R.string.filter_rec), stringResource(R.string.filter_tts)),
            s.historyFilter.ordinal, { vm.setFilter(HistoryFilter.entries[it]) }, Modifier.padding(12.dp)
        )
        if (s.history.isEmpty()) {
            Box(Modifier.weight(1f).fillMaxWidth().padding(32.dp), contentAlignment = Alignment.Center) {
                Text(stringResource(R.string.history_empty), color = Neon.Muted, fontSize = 15.sp, textAlign = TextAlign.Center)
            }
        } else {
            LazyColumn(Modifier.weight(1f), contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)) {
                items(s.history, key = { it.id }) { e ->
                    val profile = VoiceCatalog.get(e.voiceId)
                    val file = vm.fileOf(e)
                    val playing = s.playingPath == file.path
                    val type = stringResource(if (e.sourceType == SourceType.TTS.name) R.string.type_tts else R.string.type_recording)
                    NeonCard(Modifier.fillMaxWidth(), borderColor = if (playing) Neon.Cyan else Neon.Border) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            VoiceAvatar(profile, Modifier.size(56.dp))
                            Spacer(Modifier.width(12.dp))
                            Column(Modifier.weight(1f)) {
                                Text(stringResource(R.string.row_title, voiceName(profile), type), color = Color.White,
                                    fontSize = 16.sp, fontWeight = FontWeight.Bold)
                                Text(fmt.format(Date(e.createdAtEpochMs)), color = Neon.Muted, fontSize = 13.sp)
                                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                    Text(fmtDuration(e.durationMs), color = Neon.Cyan, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                                    Pill(stringResource(if (profile.isFree) R.string.free else R.string.premium), profile.isFree)
                                }
                            }
                        }
                        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                            NeonIconButton(if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                                stringResource(if (playing) R.string.pause else R.string.play), { vm.togglePlay(file) }, ring = true)
                            NeonIconButton(R.drawable.ic_download, stringResource(R.string.download), {
                                download(file, vm.downloadName(e.voiceDisplayName, e.createdAtEpochMs))
                            })
                            NeonIconButton(R.drawable.ic_share, stringResource(R.string.share), {
                                if (!ShareHelper.share(ctx, file, ctx.getString(R.string.share_title))) vm.togglePlay(file)
                            })
                            NeonIconButton(R.drawable.ic_delete, stringResource(R.string.delete), { toDelete = e }, tint = Neon.Danger)
                        }
                    }
                }
            }
        }
        if (canShowBanner && s.phase == Phase.IDLE) BannerAd(Modifier.padding(bottom = 4.dp))
    }

    toDelete?.let { e ->
        AlertDialog(
            onDismissRequest = { toDelete = null },
            title = { Text(stringResource(R.string.delete_title)) },
            text = { Text(stringResource(R.string.delete_text)) },
            confirmButton = { TextButton(onClick = { vm.deleteRecording(e); toDelete = null }) { Text(stringResource(R.string.delete), color = Neon.Danger) } },
            dismissButton = { TextButton(onClick = { toDelete = null }) { Text(stringResource(R.string.cancel)) } }
        )
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
