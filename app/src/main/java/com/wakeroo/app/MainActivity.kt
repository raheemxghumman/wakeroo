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
import com.wakeroo.app.data.RingPrefs
import com.wakeroo.app.data.Settings
import com.wakeroo.app.feature.main.MainTab
import androidx.compose.foundation.isSystemInDarkTheme
import com.wakeroo.app.ui.navigation.WakerooNavHost
import com.wakeroo.app.ui.theme.WakerooTheme
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val settings = MutableStateFlow<Settings?>(null)
    /** Null until checked; true when this launch follows a restore onto a new phone. */
    private val restored = MutableStateFlow<Boolean?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        val splash = installSplashScreen()
        super.onCreate(savedInstanceState)
        splash.setKeepOnScreenCondition { settings.value == null || restored.value == null }
        // The UI is dark-first, so keep light status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        lifecycleScope.launch {
            container.settings.settings.collect {
                settings.value = it
                container.directBoot.ringPrefs = RingPrefs(it.alwaysSpeaker, it.alsoHeadphones, it.missionTimeLimit, it.ringScene)
            }
        }
        lifecycleScope.launch {
            // Restore detection must run before the first sync writes the direct-boot mirror.
            val store = container.directBoot
            val known = store.deviceKnown()
            store.markDeviceKnown()
            restored.value = !known && (container.settings.current().onboardingDone || container.repository.count() > 0)
            // Bring Room and AlarmManager back in line on every launch, and recheck Pro with Play.
            container.repository.reconcileAfterUnlock()
            container.billing.start()
        }
        val openHealth = intent.getBooleanExtra(EXTRA_OPEN_HEALTH, false)
        val initialTab = intent.getStringExtra(EXTRA_TAB)?.let { runCatching { MainTab.valueOf(it) }.getOrNull() } ?: MainTab.Alarms

        setContent {
            val current by settings.collectAsStateWithLifecycle()
            val s = current ?: return@setContent
            val restoreMode by restored.collectAsStateWithLifecycle()
            val restore = restoreMode ?: return@setContent
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
                WakerooNavHost(onboardingDone = s.onboardingDone, restored = restore, openHealth = openHealth, initialTab = initialTab)
            }
            if (!s.pro && s.onboardingDone) LaunchedEffect(Unit) { Ads.start(this@MainActivity) }
        }
    }

    companion object {
        const val EXTRA_OPEN_HEALTH = "open_health"
        const val EXTRA_TAB = "tab"
    }
}
