package com.cartoniq.voxmorph.ui

import android.app.Activity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.cartoniq.voxmorph.R
import com.cartoniq.voxmorph.ui.screens.*
import com.cartoniq.voxmorph.ui.theme.Neon
import com.cartoniq.voxmorph.viewmodel.MainViewModel
import com.cartoniq.voxmorph.viewmodel.Screen
import kotlinx.coroutines.delay

@Composable
fun VoxMorphRoot(vm: MainViewModel) {
    val s by vm.state.collectAsStateWithLifecycle()
    val canRequestAds by vm.ads.canRequestAds.collectAsStateWithLifecycle()
    val privacyRequired by vm.ads.privacyOptionsRequired.collectAsStateWithLifecycle()
    val ctx = LocalContext.current
    val snack = remember { SnackbarHostState() }
    var splash by rememberSaveable { mutableStateOf(true) }

    LaunchedEffect(Unit) { delay(900); splash = false }
    LaunchedEffect(s.message) {
        s.message?.let { m ->
            snack.showSnackbar(ctx.getString(m.res))
            vm.consumeMessage(m)
        }
    }
    BackHandler(enabled = !splash && s.screen != Screen.STUDIO) {
        vm.navigate(if (s.screen == Screen.ABOUT || s.screen == Screen.PRIVACY) Screen.SETTINGS else Screen.STUDIO)
    }

    Box(Modifier.fillMaxSize().background(Neon.Bg)) {
        Column(Modifier.fillMaxSize().systemBarsPadding().displayCutoutPadding()) {
            Box(Modifier.weight(1f)) {
                when (s.screen) {
                    Screen.STUDIO -> StudioScreen(vm, s)
                    Screen.HISTORY -> HistoryScreen(vm, s, canRequestAds)
                    Screen.TTS -> TtsScreen(vm, s)
                    Screen.SETTINGS -> SettingsScreen(vm, s, privacyRequired)
                    Screen.ABOUT -> AboutScreen(vm)
                    Screen.PRIVACY -> PrivacyScreen(vm)
                }
                SnackbarHost(snack, Modifier.align(Alignment.BottomCenter))
            }
            if (s.screen == Screen.STUDIO || s.screen == Screen.HISTORY) {
                Row(Modifier.fillMaxWidth().background(Neon.Surface)) {
                    NavItem(R.drawable.ic_headset, stringResource(R.string.nav_studio), s.screen == Screen.STUDIO, Modifier.weight(1f)) { vm.navigate(Screen.STUDIO) }
                    NavItem(R.drawable.ic_history, stringResource(R.string.nav_history), s.screen == Screen.HISTORY, Modifier.weight(1f)) { vm.navigate(Screen.HISTORY) }
                }
            }
        }
        if (splash) SplashScreen()
    }
}

@Composable
private fun NavItem(icon: Int, label: String, selected: Boolean, modifier: Modifier, onClick: () -> Unit) {
    val c = if (selected) Neon.Cyan else Neon.Muted
    Column(
        modifier.heightIn(min = 56.dp).clickable(onClick = onClick)
            .semantics { role = Role.Tab; this.selected = selected }.padding(vertical = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(icon), null, tint = c, modifier = Modifier.size(24.dp))
        Text(label, color = c, fontSize = 13.sp, fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium)
    }
}

@Composable
private fun SplashScreen() {
    Column(
        Modifier.fillMaxSize().background(Neon.Bg).clickable(enabled = false) {}.systemBarsPadding().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center
    ) {
        Icon(painterResource(R.drawable.ic_mic), null, tint = Neon.Cyan, modifier = Modifier.size(96.dp))
        Spacer(Modifier.height(16.dp))
        Text(stringResource(R.string.app_name), style = TextStyle(brush = Neon.Gradient, fontSize = 44.sp, fontWeight = FontWeight.ExtraBold))
        Text(stringResource(R.string.tagline), color = Neon.Muted, fontSize = 15.sp)
        Spacer(Modifier.height(32.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
            listOf(R.string.splash_offline, R.string.splash_private, R.string.splash_fast).forEach {
                Text(stringResource(it), color = Color.White, fontSize = 13.sp)
            }
        }
        Spacer(Modifier.height(32.dp))
        LinearProgressIndicator(Modifier.width(160.dp), color = Neon.Cyan)
        Spacer(Modifier.height(8.dp))
        Text(stringResource(R.string.loading), color = Neon.Muted, fontSize = 13.sp)
    }
}
