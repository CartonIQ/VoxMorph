package com.cartoniq.voxmorph.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ui.theme.Neon

@Composable
fun RecordButton(recording: Boolean, busy: Boolean, enabled: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
        1f, 1.12f, infiniteRepeatable(tween(700), RepeatMode.Reverse), label = "scale"
    )
    val cd = stringResource(if (recording) R.string.cd_stop else R.string.cd_record)
    Box(modifier.size(128.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier.size(120.dp).scale(if (recording) pulse else 1f)
                .border(3.dp, Brush.sweepGradient(listOf(Neon.Cyan, Neon.Purple, Neon.Cyan)), CircleShape)
        )
        Column(
            Modifier.size(100.dp).clip(CircleShape)
                .background(Brush.linearGradient(listOf(Color(0xFF0A3A8A), Color(0xFF5B2BB0))))
                .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
                .semantics { contentDescription = cd; role = Role.Button },
            horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
        ) {
            if (busy) {
                CircularProgressIndicator(Modifier.size(32.dp), color = Neon.Cyan, strokeWidth = 3.dp)
            } else {
                Icon(
                    painterResource(if (recording) R.drawable.ic_stop else R.drawable.ic_mic), null,
                    tint = if (enabled) Color.White else Neon.Muted, modifier = Modifier.size(38.dp)
                )
                Text(
                    stringResource(if (recording) R.string.stop else R.string.record),
                    color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Bold
                )
            }
        }
    }
}
