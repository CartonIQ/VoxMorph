package com.cartoniq.voxmorph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cartoniq.voxmorph.domain.VoiceCategory
import com.cartoniq.voxmorph.domain.VoiceProfile

private val skinTones = listOf(0xFFF2C6A0, 0xFFD9A37A, 0xFFB67B52, 0xFF8D5A3B, 0xFFE8B896).map { Color(it) }
private val hairTones = listOf(0xFF1B1B1F, 0xFF4A2C1A, 0xFF7A4A24, 0xFFC9A24A, 0xFF8A8A92, 0xFFB03A6E, 0xFF2F4FBF).map { Color(it) }

/** Original, locally drawn avatar art. No remote or third-party images. */
@Composable
fun VoiceAvatar(profile: VoiceProfile, modifier: Modifier = Modifier) {
    val a = Color.hsv(profile.hue % 360f, 0.75f, 0.75f)
    val b = Color.hsv((profile.hue + 40f) % 360f, 0.85f, 0.32f)
    BoxWithConstraints(
        modifier.clip(RoundedCornerShape(12.dp)).background(Brush.linearGradient(listOf(b, a.copy(alpha = 0.75f)))),
        contentAlignment = Alignment.Center
    ) {
        if (profile.category == VoiceCategory.EFFECTS) {
            val sp = with(LocalDensity.current) { (maxWidth * 0.5f).toSp() }
            Text(profile.glyph, fontSize = sp, modifier = Modifier.clearAndSetSemantics {})
        } else {
            val female = profile.category == VoiceCategory.FEMALE
            val skin = skinTones[profile.index % skinTones.size]
            val hair = hairTones[(profile.index * 3 + if (female) 2 else 0) % hairTones.size]
            Canvas(Modifier.fillMaxSize().clearAndSetSemantics {}) {
                val w = size.width
                val h = size.height
                drawOval(Color(0xFF0E1428), Offset(w * 0.1f, h * 0.68f), Size(w * 0.8f, h * 0.7f))
                drawOval(a.copy(alpha = 0.9f), Offset(w * 0.1f, h * 0.68f), Size(w * 0.8f, h * 0.7f), style = Stroke(w * 0.03f))
                if (female) drawRoundRect(hair, Offset(w * 0.27f, h * 0.2f), Size(w * 0.46f, h * 0.58f), CornerRadius(w * 0.2f))
                drawRect(skin, Offset(w * 0.44f, h * 0.55f), Size(w * 0.12f, h * 0.16f))
                drawCircle(skin, w * 0.2f, Offset(w * 0.5f, h * 0.42f))
                drawArc(hair, 165f, 210f, true, Offset(w * 0.29f, h * 0.2f), Size(w * 0.42f, h * 0.4f))
                drawCircle(Color(0xFF14141C), w * 0.018f, Offset(w * 0.43f, h * 0.45f))
                drawCircle(Color(0xFF14141C), w * 0.018f, Offset(w * 0.57f, h * 0.45f))
                if (profile.index % 3 == 0) {
                    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.06f, Offset(w * 0.43f, h * 0.45f), style = Stroke(w * 0.012f))
                    drawCircle(Color.White.copy(alpha = 0.85f), w * 0.06f, Offset(w * 0.57f, h * 0.45f), style = Stroke(w * 0.012f))
                }
                if (profile.index % 4 == 1) {
                    drawArc(Color(0xFF00E5FF), 180f, 180f, false, Offset(w * 0.26f, h * 0.2f), Size(w * 0.48f, h * 0.44f), style = Stroke(w * 0.035f))
                    drawCircle(Color(0xFF00E5FF), w * 0.05f, Offset(w * 0.26f, h * 0.43f))
                    drawCircle(Color(0xFF00E5FF), w * 0.05f, Offset(w * 0.74f, h * 0.43f))
                }
            }
        }
    }
}
