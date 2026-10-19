package com.wakeroo.app

import android.graphics.Color
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.wakeroo.app.ui.designsystem.DesignSystemShowcase
import com.wakeroo.app.ui.theme.WakerooTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // The UI is dark-first, so keep light status and navigation bar icons.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(Color.TRANSPARENT),
        )
        setContent {
            WakerooTheme {
                DesignSystemShowcase(onTestAlarm = {
                    container.scheduler.scheduleTest(System.currentTimeMillis() + 10_000)
                })
            }
        }
    }

    companion object {
        const val EXTRA_OPEN_HEALTH = "open_health"
    }
}
