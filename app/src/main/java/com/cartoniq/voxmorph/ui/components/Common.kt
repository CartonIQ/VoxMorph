package com.cartoniq.voxmorph.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.domain.VoiceCategory
import com.cartoniq.voxmorph.domain.VoiceProfile
import com.cartoniq.voxmorph.ui.theme.Neon
import java.util.Locale

@Composable
fun voiceName(p: VoiceProfile): String =
    if (p.category == VoiceCategory.EFFECTS) stringResource(p.nameRes) else stringResource(p.nameRes, p.index)

fun fmtDuration(ms: Long): String {
    val s = ms / 1000
    return String.format(Locale.US, "%d:%02d", s / 60, s % 60)
}

@Composable
fun NeonIconButton(
    icon: Int, description: String, onClick: () -> Unit,
    modifier: Modifier = Modifier, enabled: Boolean = true, tint: Color = Neon.Cyan, ring: Boolean = false
) {
    IconButton(
        onClick = onClick, enabled = enabled,
        modifier = modifier.then(if (ring) Modifier.border(1.5.dp, tint.copy(alpha = if (enabled) 1f else 0.3f), CircleShape) else Modifier)
    ) {
        Icon(painterResource(icon), contentDescription = description, tint = if (enabled) tint else tint.copy(alpha = 0.35f))
    }
}

@Composable
fun Pill(text: String, free: Boolean, modifier: Modifier = Modifier) {
    val c = if (free) Neon.Green else Neon.Purple
    Text(
        text.uppercase(), color = c, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1,
        modifier = modifier
            .border(1.dp, c, RoundedCornerShape(50))
            .background(c.copy(alpha = 0.12f), RoundedCornerShape(50))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    )
}

@Composable
fun GradientButton(
    text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true, iconRes: Int? = null
) {
    Row(
        modifier = modifier
            .heightIn(min = 52.dp)
            .clip(RoundedCornerShape(50))
            .background(if (enabled) Neon.Gradient else androidx.compose.ui.graphics.SolidColor(Neon.SurfaceHi))
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically
    ) {
        if (iconRes != null) {
            Icon(painterResource(iconRes), null, tint = Color.Black, modifier = Modifier.size(20.dp))
            Spacer(Modifier.width(8.dp))
        }
        Text(text, color = if (enabled) Color.Black else Neon.Muted, fontWeight = FontWeight.Bold, fontSize = 16.sp)
    }
}

@Composable
fun SegmentedTabs(
    labels: List<String>, selectedIndex: Int, onSelect: (Int) -> Unit, modifier: Modifier = Modifier
) {
    Row(
        modifier.fillMaxWidth()
            .clip(RoundedCornerShape(50))
            .background(Neon.Surface)
            .border(1.dp, Neon.Border.copy(alpha = 0.6f), RoundedCornerShape(50))
            .padding(3.dp)
    ) {
        labels.forEachIndexed { i, label ->
            val sel = i == selectedIndex
            Box(
                Modifier.weight(1f).heightIn(min = 48.dp)
                    .clip(RoundedCornerShape(50))
                    .then(if (sel) Modifier.background(Neon.Gradient) else Modifier)
                    .clickable { onSelect(i) }
                    .semantics { role = Role.Tab; selected = sel },
                contentAlignment = Alignment.Center
            ) {
                Text(
                    label, color = if (sel) Color.Black else Color.White,
                    fontWeight = if (sel) FontWeight.Bold else FontWeight.Medium, fontSize = 15.sp, maxLines = 1
                )
            }
        }
    }
}

@Composable
fun categoryLabels(): List<String> =
    listOf(stringResource(R.string.cat_male), stringResource(R.string.cat_female), stringResource(R.string.cat_effects))

@Composable
fun NeonCard(modifier: Modifier = Modifier, borderColor: Color = Neon.Border, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier.clip(RoundedCornerShape(16.dp)).background(Neon.Surface)
            .border(BorderStroke(1.dp, borderColor.copy(alpha = 0.6f)), RoundedCornerShape(16.dp))
            .padding(12.dp),
        content = content
    )
}
