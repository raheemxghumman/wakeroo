package com.wakeroo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.wakeroo.app.billing.Ads
import com.wakeroo.app.data.AppTheme
import com.wakeroo.app.data.Settings
import com.wakeroo.app.feature.main.MainTab
import androidx.compose.foundation.isSystemInDarkTheme
import com.wakeroo.app.ui.navigation.WakerooNavHost
import com.wakeroo.app.ui.theme.WakerooTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settings = MutableStateFlow<Settings?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { settings.value == null }
        // The UI is dark-first, so keep light status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        lifecycleScope.launch { container.settings.settings.collect { settings.value = it } }
        lifecycleScope.launch {
            // Bring Room and AlarmManager back in line on every launch, and recheck Pro with Play.
            container.repository.reconcileAfterUnlock()
            container.billing.start()
        }
        val openHealth = intent.getBooleanExtra(EXTRA_OPEN_HEALTH, false)
        val initialTab = intent.getStringExtra(EXTRA_TAB)?.let { runCatching { MainTab.valueOf(it) }.getOrNull() } ?: MainTab.Alarms

        setContent {
            val current by settings.collectAsStateWithLifecycle()
            val s = current ?: return@setContent
            val dark = when (s.appTheme) {
                AppTheme.Dark -> true
                AppTheme.Light -> false
                AppTheme.System -> isSystemInDarkTheme()
            }
            LaunchedEffect(dark) {
                // Status and navigation bar icons follow the app theme.
                val style = if (dark) SystemBarStyle.dark(Color.TRANSPARENT) else SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT)
                enableEdgeToEdge(statusBarStyle = style, navigationBarStyle = style)
            }
            WakerooTheme(dark = dark, motion = s.motion) {
                WakerooNavHost(onboardingDone = s.onboardingDone, openHealth = openHealth, initialTab = initialTab)
            }
            if (!s.pro && s.onboardingDone) LaunchedEffect(Unit) { Ads.start(this@MainActivity) }
        }
    }

    companion object {
        const val EXTRA_OPEN_HEALTH = "open_health"
        const val EXTRA_TAB = "tab"
    }
}
