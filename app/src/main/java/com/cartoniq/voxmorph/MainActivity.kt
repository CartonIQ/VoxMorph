package com.cartoniq.voxmorph

import android.content.Context
import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.cartoniq.voxmorph.ui.VoxMorphRoot
import com.cartoniq.voxmorph.ui.theme.VoxMorphTheme
import com.cartoniq.voxmorph.util.LocaleHelper
import com.cartoniq.voxmorph.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val vm: MainViewModel by viewModels()

    override fun attachBaseContext(newBase: Context) {
        super.attachBaseContext(LocaleHelper.wrap(newBase))
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT)
        )
        super.onCreate(savedInstanceState)
        setContent { VoxMorphTheme { VoxMorphRoot(vm) } }

        lifecycle.addObserver(LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP && !isChangingConfigurations) vm.onBackgrounded()
        })
        // Non-blocking: free features work whether or not consent/ads ever become available.
        vm.ads.gatherConsent(this)
    }
}
