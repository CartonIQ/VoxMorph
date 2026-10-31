package com.cartoniq.voxmorph.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ui.components.NeonCard
import com.cartoniq.voxmorph.ui.components.NeonIconButton
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.util.LinkHelper
import com.cartoniq.voxmorph.viewmodel.MainViewModel
import com.cartoniq.voxmorph.viewmodel.Screen

private val sections = listOf(
    R.string.priv_h_summary to R.string.priv_b_summary,
    R.string.priv_h_collect to R.string.priv_b_collect,
    R.string.priv_h_mic to R.string.priv_b_mic,
    R.string.priv_h_tts to R.string.priv_b_tts,
    R.string.priv_h_files to R.string.priv_b_files,
    R.string.priv_h_ads to R.string.priv_b_ads,
    R.string.priv_h_perm to R.string.priv_b_perm,
    R.string.priv_h_children to R.string.priv_b_children,
    R.string.priv_h_security to R.string.priv_b_security,
    R.string.priv_h_changes to R.string.priv_b_changes,
    R.string.priv_h_contact to R.string.priv_b_contact
)

@Composable
fun PrivacyScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val subject = stringResource(R.string.privacy_email_subject)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconButton(R.drawable.ic_back, stringResource(R.string.back), { vm.navigate(Screen.SETTINGS) }, tint = Color.White)
            Column {
                Text(stringResource(R.string.priv_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                Text(stringResource(R.string.priv_updated), color = Neon.Muted, fontSize = 13.sp)
            }
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            sections.forEach { (head, body) ->
                NeonCard(Modifier.fillMaxWidth()) {
                    Text(stringResource(head), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text(stringResource(body), color = Neon.Muted, fontSize = 15.sp, lineHeight = 22.sp)
                }
            }
            OutlinedButton(
                onClick = { LinkHelper.email(ctx, subject) },
                modifier = Modifier.fillMaxWidth().heightIn(min = 48.dp)
            ) { Text(LinkHelper.SUPPORT_EMAIL) }
            Spacer(Modifier.height(8.dp))
        }
    }
}
