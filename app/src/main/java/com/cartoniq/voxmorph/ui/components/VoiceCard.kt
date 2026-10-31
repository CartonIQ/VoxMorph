package com.cartoniq.voxmorph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.*
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.domain.VoiceProfile
import com.cartoniq.voxmorph.ui.theme.Neon

@Composable
fun VoiceCard(profile: VoiceProfile, selected: Boolean, locked: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val name = voiceName(profile)
    val tier = stringResource(if (profile.isFree) R.string.free else R.string.premium)
    val state = stringResource(if (selected) R.string.state_selected else if (locked) R.string.state_locked else R.string.state_available)
    val cd = stringResource(R.string.cd_voice_card, name, tier, state)
    val shape = RoundedCornerShape(14.dp)
    Column(
        modifier.clip(shape).background(Neon.Surface)
            .border(if (selected) 2.dp else 1.dp, if (selected) Neon.Cyan else Neon.Border.copy(alpha = 0.5f), shape)
            .clickable(onClick = onClick)
            .semantics(mergeDescendants = true) { contentDescription = cd; this.selected = selected; role = Role.Button }
            .padding(5.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box {
            VoiceAvatar(profile, Modifier.fillMaxWidth().aspectRatio(1f))
            if (selected || locked) {
                Box(
                    Modifier.align(Alignment.TopEnd).padding(3.dp).size(20.dp).clip(CircleShape)
                        .background(if (selected) Neon.Cyan else Color.Black.copy(alpha = 0.6f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painterResource(if (selected) R.drawable.ic_check else R.drawable.ic_lock), null,
                        tint = if (selected) Color.Black else Color.White, modifier = Modifier.size(13.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(4.dp))
        Text(name, color = Color.White, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, maxLines = 1,
            overflow = TextOverflow.Ellipsis, textAlign = TextAlign.Center)
        Spacer(Modifier.height(3.dp))
        Pill(tier, profile.isFree)
    }
}
