package com.cartoniq.voxmorph.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ads.AdManager
import com.cartoniq.voxmorph.ui.theme.Neon
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun RewardBanner(remainingMs: Long, onWatch: () -> Unit, modifier: Modifier = Modifier) {
    val active = remainingMs > 0
    Row(
        modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(Neon.GradientSoft)
            .border(1.dp, Neon.Purple.copy(alpha = 0.7f), RoundedCornerShape(16.dp)).padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(painterResource(R.drawable.ic_crown), null, tint = Color(0xFFFFC857), modifier = Modifier.size(32.dp))
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            if (active) {
                Text(stringResource(R.string.reward_active, fmtDuration(remainingMs)), color = Color.White,
                    fontSize = 14.sp, fontWeight = FontWeight.Bold)
            } else {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.ad_label), color = Neon.Muted, fontSize = 11.sp,
                        modifier = Modifier.border(1.dp, Neon.Muted, RoundedCornerShape(4.dp)).padding(horizontal = 4.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(stringResource(R.string.reward_title), color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                }
                Text(stringResource(R.string.reward_sub), color = Neon.Muted, fontSize = 13.sp)
            }
        }
        if (!active) {
            Spacer(Modifier.width(8.dp))
            GradientButton(stringResource(R.string.watch_ad), onWatch, iconRes = R.drawable.ic_play)
        }
    }
}

/** Banner is created only after consent allows ads; it never appears during recording or processing. */
@Composable
fun BannerAd(modifier: Modifier = Modifier) {
    AndroidView(
        modifier = modifier.fillMaxWidth(),
        factory = { ctx ->
            AdView(ctx).apply {
                setAdSize(AdSize.BANNER)
                adUnitId = AdManager.BANNER_ID
                loadAd(AdRequest.Builder().build())
            }
        },
        onRelease = { it.destroy() }
    )
}
