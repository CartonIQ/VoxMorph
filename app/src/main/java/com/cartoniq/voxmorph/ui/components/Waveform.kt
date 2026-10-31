package com.cartoniq.voxmorph.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.dp
import com.cartoniq.voxmorph.ui.theme.Neon
import kotlin.math.max
import kotlin.math.sqrt

/** Draws real captured amplitude only. With no samples it shows a flat baseline, not fake activity. */
@Composable
fun Waveform(levels: List<Float>, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxWidth().height(72.dp).clearAndSetSemantics {}) {
        val bars = 48
        val slot = size.width / bars
        val barW = slot * 0.55f
        val minH = 3.dp.toPx()
        for (i in 0 until bars) {
            val idx = levels.size - bars + i
            val lv = if (idx >= 0) sqrt(levels[idx].coerceIn(0f, 1f)) else 0f
            val h = max(minH, lv * size.height)
            drawRoundRect(
                lerp(Neon.Cyan, Neon.Purple, i / (bars - 1f)),
                Offset(i * slot + (slot - barW) / 2, (size.height - h) / 2),
                Size(barW, h), CornerRadius(barW / 2)
            )
        }
    }
}
