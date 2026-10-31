package com.cartoniq.voxmorph.ui.screens

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.domain.SourceType
import com.cartoniq.voxmorph.domain.VoiceCatalog
import com.cartoniq.voxmorph.domain.VoiceCategory
import com.cartoniq.voxmorph.ui.components.*
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.util.ShareHelper
import com.cartoniq.voxmorph.viewmodel.*
import kotlin.math.round

private enum class PermDialog { RATIONALE, PERMANENT }

@Composable
fun StudioScreen(vm: MainViewModel, s: UiState) {
    val ctx = LocalContext.current
    val activity = ctx as Activity
    var permDialog by remember { mutableStateOf<PermDialog?>(null) }
    var showPitch by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) vm.onRecordTapped()
        else permDialog = if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO))
            PermDialog.RATIONALE else PermDialog.PERMANENT
    }
    fun onRecord() {
        if (s.phase == Phase.RECORDING ||
            ContextCompat.checkSelfPermission(ctx, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        ) vm.onRecordTapped()
        else if (ActivityCompat.shouldShowRequestPermissionRationale(activity, Manifest.permission.RECORD_AUDIO))
            permDialog = PermDialog.RATIONALE
        else launcher.launch(Manifest.permission.RECORD_AUDIO)
    }

    val profile = VoiceCatalog.get(s.selectedVoiceId)
    val busy = s.phase == Phase.STOPPING || s.phase == Phase.PROCESSING
    val unlocked = s.rewardRemainingMs > 0

    Column(Modifier.fillMaxSize()) {
        StudioHeader(onTts = { vm.navigate(Screen.TTS) }, onSettings = { vm.navigate(Screen.SETTINGS) })
        LazyVerticalGrid(
            columns = GridCells.Adaptive(76.dp),
            modifier = Modifier.weight(1f),
            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            item(span = { GridItemSpan(maxLineSpan) }) {
                SegmentedTabs(categoryLabels(), s.category.ordinal, { vm.selectCategory(VoiceCategory.entries[it]) })
            }
            items(VoiceCatalog.of(s.category), key = { it.id }) { p ->
                VoiceCard(p, selected = p.id == s.selectedVoiceId, locked = !p.isFree && !unlocked,
                    onClick = { vm.selectVoice(p.id) })
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Column(Modifier.padding(top = 8.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                    Waveform(if (s.phase == Phase.RECORDING) s.levels else emptyList())
                    Text(
                        when (s.phase) {
                            Phase.RECORDING -> stringResource(R.string.rec_recording, fmtDuration(s.elapsedMs))
                            Phase.STOPPING -> stringResource(R.string.rec_stopping)
                            Phase.PROCESSING -> stringResource(R.string.rec_processing)
                            Phase.READY -> stringResource(R.string.rec_ready)
                            Phase.IDLE -> stringResource(R.string.hint_tap_record)
                        },
                        color = Neon.Muted, fontSize = 14.sp, textAlign = TextAlign.Center
                    )
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    SideCard(R.drawable.ic_headset, stringResource(R.string.voice_label), voiceName(profile), Modifier.weight(1f), onClick = null)
                    RecordButton(
                        recording = s.phase == Phase.RECORDING, busy = busy,
                        enabled = !busy && s.ttsPhase != TtsPhase.GENERATING, onClick = { onRecord() }
                    )
                    SideCard(
                        R.drawable.ic_settings, stringResource(R.string.pitch_label),
                        stringResource(R.string.pitch_value, s.pitch), Modifier.weight(1f),
                        onClick = { if (s.phase == Phase.IDLE || s.phase == Phase.READY) showPitch = true }
                    )
                }
            }
            val pv = s.preview
            if (pv != null && pv.source == SourceType.RECORDING) item(span = { GridItemSpan(maxLineSpan) }) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    PreviewBar(vm, s, pv)
                    if (s.hasRaw && s.phase == Phase.READY) {
                        GradientButton(
                            stringResource(R.string.apply_voice), onClick = { vm.reprocess() },
                            modifier = Modifier.fillMaxWidth(), iconRes = R.drawable.ic_headset
                        )
                    }
                }
            }
            item(span = { GridItemSpan(maxLineSpan) }) {
                RewardBanner(s.rewardRemainingMs, onWatch = { vm.watchAd(activity) }, modifier = Modifier.padding(top = 4.dp, bottom = 8.dp))
            }
        }
    }

    if (showPitch) {
        AlertDialog(
            onDismissRequest = { showPitch = false },
            confirmButton = { TextButton(onClick = { showPitch = false }) { Text(stringResource(R.string.ok)) } },
            title = { Text(stringResource(R.string.adjust_title)) },
            text = {
                Column {
                    Text(stringResource(R.string.pitch_label) + "  " + stringResource(R.string.pitch_value, s.pitch),
                        color = Neon.Cyan, fontWeight = FontWeight.Bold)
                    val cd = stringResource(R.string.cd_pitch_slider)
                    Slider(
                        value = s.pitch, onValueChange = { vm.setPitch(round(it)) }, valueRange = -12f..12f, steps = 23,
                        modifier = Modifier.semantics { contentDescription = cd }
                    )
                    Text(stringResource(R.string.pitch_dialog_hint), color = Neon.Muted, fontSize = 14.sp)
                    Spacer(Modifier.height(12.dp))
                    Text(stringResource(R.string.formant_label) + "  " + stringResource(R.string.pitch_value, s.formant),
                        color = Neon.Cyan, fontWeight = FontWeight.Bold)
                    val cdF = stringResource(R.string.cd_formant_slider)
                    Slider(
                        value = s.formant, onValueChange = { vm.setFormant(round(it)) }, valueRange = -6f..6f, steps = 11,
                        modifier = Modifier.semantics { contentDescription = cdF }
                    )
                    Text(stringResource(R.string.formant_hint), color = Neon.Muted, fontSize = 14.sp)
                }
            }
        )
    }

    permDialog?.let { d ->
        AlertDialog(
            onDismissRequest = { permDialog = null },
            title = { Text(stringResource(R.string.perm_title)) },
            text = { Text(stringResource(if (d == PermDialog.RATIONALE) R.string.perm_rationale else R.string.perm_denied_forever)) },
            confirmButton = {
                TextButton(onClick = {
                    permDialog = null
                    if (d == PermDialog.RATIONALE) launcher.launch(Manifest.permission.RECORD_AUDIO)
                    else ctx.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", ctx.packageName, null)))
                }) { Text(stringResource(if (d == PermDialog.RATIONALE) R.string.allow else R.string.open_app_settings)) }
            },
            dismissButton = { TextButton(onClick = { permDialog = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
}

@Composable
fun StudioHeader(onTts: () -> Unit, onSettings: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(stringResource(R.string.app_name), style = TextStyle(brush = Neon.Gradient, fontSize = 28.sp, fontWeight = FontWeight.ExtraBold))
            Text(stringResource(R.string.tagline), color = Neon.Muted, fontSize = 12.sp)
        }
        Row(
            Modifier.heightIn(min = 48.dp).clip(RoundedCornerShape(50)).border(1.5.dp, Neon.Cyan, RoundedCornerShape(50))
                .clickable(onClick = onTts).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(painterResource(R.drawable.ic_chat), null, tint = Neon.Cyan, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(6.dp))
            Text(stringResource(R.string.tts_action), color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
        }
        NeonIconButton(R.drawable.ic_settings, stringResource(R.string.settings), onSettings)
    }
}

@Composable
private fun SideCard(icon: Int, label: String, value: String, modifier: Modifier, onClick: (() -> Unit)?) {
    Column(
        modifier.heightIn(min = 64.dp).clip(RoundedCornerShape(14.dp)).background(Neon.Surface)
            .border(1.dp, Neon.Border.copy(alpha = 0.6f), RoundedCornerShape(14.dp))
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier).padding(8.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), null, tint = Neon.Cyan, modifier = Modifier.size(20.dp))
        Text(label, color = Neon.Muted, fontSize = 12.sp, maxLines = 1)
        Text(value, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.Bold, maxLines = 1, textAlign = TextAlign.Center)
    }
}

@Composable
fun PreviewBar(vm: MainViewModel, s: UiState, pv: Preview) {
    val ctx = LocalContext.current
    val download = rememberDownloader(vm)
    val playing = s.playingPath == pv.file.path
    NeonCard(Modifier.fillMaxWidth(), borderColor = Neon.Cyan) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            NeonIconButton(
                if (playing) R.drawable.ic_pause else R.drawable.ic_play,
                stringResource(if (playing) R.string.pause else R.string.play), { vm.togglePlay(pv.file) }, ring = true
            )
            Spacer(Modifier.width(8.dp))
            Column(Modifier.weight(1f)) {
                Text(voiceName(pv.profile), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 15.sp, maxLines = 1)
                Text(fmtDuration(pv.durationMs), color = Neon.Muted, fontSize = 13.sp)
            }
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.End) {
            if (!pv.saved) TextButton(onClick = { vm.savePreview() }) { Text(stringResource(R.string.save), color = Neon.Cyan, fontSize = 15.sp) }
            NeonIconButton(R.drawable.ic_download, stringResource(R.string.download), {
                download(pv.file, vm.downloadName(vm.englishName(pv.profile), System.currentTimeMillis()))
            })
            NeonIconButton(R.drawable.ic_share, stringResource(R.string.share), {
                if (!ShareHelper.share(ctx, pv.file, ctx.getString(R.string.share_title))) vm.togglePlay(pv.file)
            })
            NeonIconButton(R.drawable.ic_delete, stringResource(R.string.discard), { vm.discardPreview() }, tint = Neon.Danger)
        }
    }
}
