package com.cartoniq.voxmorph.ui.screens

import android.app.Activity
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.domain.SourceType
import com.cartoniq.voxmorph.domain.VoiceCatalog
import com.cartoniq.voxmorph.domain.VoiceCategory
import com.cartoniq.voxmorph.ui.components.*
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.viewmodel.*
import kotlin.math.round

@Composable
fun TtsScreen(vm: MainViewModel, s: UiState) {
    val activity = LocalContext.current as Activity
    val generating = s.ttsPhase == TtsPhase.GENERATING
    val unlocked = s.rewardRemainingMs > 0
    Column(Modifier.fillMaxSize().imePadding()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconButton(R.drawable.ic_back, stringResource(R.string.back), { vm.navigate(Screen.STUDIO) }, tint = Color.White)
            Column(Modifier.weight(1f)) {
                Text(stringResource(R.string.tts_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.tts_subtitle), color = Neon.Muted, fontSize = 13.sp)
            }
        }
        Column(Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(horizontal = 12.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedTextField(
                value = s.ttsText, onValueChange = vm::setTtsText, enabled = !generating,
                modifier = Modifier.fillMaxWidth().heightIn(min = 120.dp),
                placeholder = { Text(stringResource(R.string.tts_hint), color = Neon.Muted) },
                trailingIcon = {
                    if (s.ttsText.isNotEmpty()) NeonIconButton(R.drawable.ic_close, stringResource(R.string.tts_clear), { vm.setTtsText("") }, tint = Neon.Muted)
                },
                supportingText = {
                    Text(stringResource(R.string.tts_counter, s.ttsText.length, MainViewModel.TTS_LIMIT),
                        color = Neon.Muted, modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.End)
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = Neon.Cyan, unfocusedBorderColor = Neon.Border,
                    focusedTextColor = Color.White, unfocusedTextColor = Color.White, cursorColor = Neon.Cyan
                )
            )

            Text(stringResource(R.string.tts_select_voice), color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            SegmentedTabs(categoryLabels(), s.ttsCategory.ordinal, { vm.selectTtsCategory(VoiceCategory.entries[it]) })
            LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                items(VoiceCatalog.of(s.ttsCategory), key = { it.id }) { p ->
                    VoiceCard(p, p.id == s.ttsVoiceId, !p.isFree && !unlocked, { vm.selectTtsVoice(p.id) }, Modifier.width(80.dp))
                }
            }

            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.advanced), color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                Spacer(Modifier.height(8.dp))
                val pitchCd = stringResource(R.string.cd_pitch_slider)
                LabeledSlider(stringResource(R.string.pitch_label), stringResource(R.string.pitch_value, s.ttsPitch)) {
                    Slider(s.ttsPitch, { vm.setTtsPitch(round(it)) }, valueRange = -12f..12f, steps = 23,
                        enabled = !generating, modifier = Modifier.semantics { contentDescription = pitchCd })
                }
                val formantCd = stringResource(R.string.cd_formant_slider)
                LabeledSlider(stringResource(R.string.formant_label), stringResource(R.string.pitch_value, s.ttsFormant)) {
                    Slider(s.ttsFormant, { vm.setTtsFormant(round(it)) }, valueRange = -6f..6f, steps = 11,
                        enabled = !generating, modifier = Modifier.semantics { contentDescription = formantCd })
                }
                val speedCd = stringResource(R.string.cd_speed_slider)
                LabeledSlider(stringResource(R.string.tts_speed), String.format(java.util.Locale.US, "%.1fx", s.ttsSpeed)) {
                    Slider(s.ttsSpeed, { vm.setTtsSpeed(round(it * 10) / 10f) }, valueRange = 0.5f..2f, steps = 14,
                        enabled = !generating, modifier = Modifier.semantics { contentDescription = speedCd })
                }
                Text(stringResource(R.string.tts_language), color = Neon.Muted, fontSize = 14.sp)
                Spacer(Modifier.height(4.dp))
                SegmentedTabs(
                    listOf(stringResource(R.string.lang_en), stringResource(R.string.lang_hi)),
                    s.ttsLang.ordinal, { vm.setTtsLang(TtsLang.entries[it]) }
                )
                Row(Modifier.fillMaxWidth().heightIn(min = 48.dp), verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.tts_save_history), color = Color.White, fontSize = 15.sp, modifier = Modifier.weight(1f))
                    Switch(s.ttsAutoSave, vm::setTtsAutoSave)
                }
            }

            GradientButton(
                stringResource(if (generating) R.string.tts_generating else R.string.tts_generate),
                onClick = vm::generateTts, enabled = !generating && s.phase != Phase.RECORDING && s.phase != Phase.PROCESSING && s.phase != Phase.STOPPING,
                modifier = Modifier.fillMaxWidth(), iconRes = if (generating) null else R.drawable.ic_mic
            )
            if (generating) LinearProgressIndicator(Modifier.fillMaxWidth(), color = Neon.Cyan)

            val pv = s.preview
            if (pv != null && pv.source == SourceType.TTS) PreviewBar(vm, s, pv)
            RewardBanner(s.rewardRemainingMs, onWatch = { vm.watchAd(activity) }, modifier = Modifier.padding(bottom = 12.dp))
        }
    }
}

@Composable
private fun LabeledSlider(label: String, value: String, slider: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(label, color = Neon.Muted, fontSize = 14.sp, modifier = Modifier.weight(1f))
            Text(value, color = Neon.Cyan, fontSize = 14.sp, fontWeight = FontWeight.Bold)
        }
        slider()
    }
}
