package com.cartoniq.voxmorph.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ui.components.NeonCard
import com.cartoniq.voxmorph.ui.components.NeonIconButton
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.util.LinkHelper
import com.cartoniq.voxmorph.viewmodel.MainViewModel
import com.cartoniq.voxmorph.viewmodel.Screen

@Composable
fun AboutScreen(vm: MainViewModel) {
    val ctx = LocalContext.current
    val subject = stringResource(R.string.about_email_subject)
    Column(Modifier.fillMaxSize()) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 4.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
            NeonIconButton(R.drawable.ic_back, stringResource(R.string.back), { vm.navigate(Screen.SETTINGS) }, tint = Color.White)
            Text(stringResource(R.string.settings_about_title), color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
        }
        Column(
            Modifier.weight(1f).verticalScroll(rememberScrollState()).padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            NeonCard(Modifier.fillMaxWidth()) {
                Text(stringResource(R.string.about_title), style = TextStyle(brush = Neon.Gradient, fontSize = 26.sp, fontWeight = FontWeight.ExtraBold))
                Spacer(Modifier.height(4.dp))
                Text(stringResource(R.string.about_tagline), color = Neon.Cyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.height(10.dp))
                Body(R.string.about_p1)
                Spacer(Modifier.height(8.dp))
                Body(R.string.about_p2)
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Heading(R.string.about_dev_title)
                Spacer(Modifier.height(6.dp))
                Text(stringResource(R.string.about_dev_name), color = Neon.Cyan, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(8.dp))
                Body(R.string.about_dev_p1)
                Spacer(Modifier.height(8.dp))
                Body(R.string.about_dev_p2)
                Spacer(Modifier.height(8.dp))
                Body(R.string.about_dev_p3)
            }
            NeonCard(Modifier.fillMaxWidth()) {
                Heading(R.string.about_contact_title)
                Spacer(Modifier.height(6.dp))
                Body(R.string.about_contact_intro)
                Spacer(Modifier.height(6.dp))
                ContactRow("\uD83D\uDCE7", stringResource(R.string.about_email_label), LinkHelper.SUPPORT_EMAIL) {
                    LinkHelper.email(ctx, subject)
                }
                ContactRow("\uD83C\uDF10", stringResource(R.string.about_github_label), "github.com/CartonIQ") {
                    LinkHelper.open(ctx, LinkHelper.GITHUB_URL)
                }
            }
            HorizontalDivider(color = Neon.Border.copy(alpha = 0.5f))
            Text(
                stringResource(R.string.about_footer), color = Neon.Muted, fontSize = 13.sp,
                textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth().padding(bottom = 12.dp)
            )
        }
    }
}

@Composable
private fun Heading(res: Int) {
    Text(stringResource(res), color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.Bold)
}

@Composable
private fun Body(res: Int) {
    Text(stringResource(res), color = Neon.Muted, fontSize = 15.sp, lineHeight = 22.sp)
}

@Composable
private fun ContactRow(emoji: String, label: String, value: String, onClick: () -> Unit) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 52.dp).clickable(role = Role.Button, onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(emoji, fontSize = 22.sp)
        Spacer(Modifier.width(12.dp))
        Column {
            Text(label, color = Neon.Muted, fontSize = 13.sp)
            Text(value, color = Neon.Cyan, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
        }
    }
}
