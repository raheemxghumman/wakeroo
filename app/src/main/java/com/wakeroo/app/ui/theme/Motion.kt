package com.wakeroo.app.ui.theme

import android.content.Context
import android.provider.Settings
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.spring
import androidx.compose.runtime.staticCompositionLocalOf

/**
 * Motion rules: springs instead of fixed durations, calm at night and bouncy in the morning,
 * one hero motion per screen, and never animate information out of view.
 */
object WakerooMotion {
    /** Bedtime and settings screens: settles without overshoot. */
    fun <T> calm(): SpringSpec<T> = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMediumLow)

    /** Morning and success moments: a visible, friendly bounce. */
    fun <T> bouncy(): SpringSpec<T> = spring(dampingRatio = 0.55f, stiffness = Spring.StiffnessLow)

    /** Small controls such as switches and chips. */
    fun <T> snappy(): SpringSpec<T> = spring(dampingRatio = 0.8f, stiffness = Spring.StiffnessMedium)
}

/** True when the user turned animations off in system settings or in Wakeroo. */
val LocalReducedMotion = staticCompositionLocalOf { false }

fun systemAnimationsOff(context: Context): Boolean =
    Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f) == 0f
