package com.wakeroo.app.feature.review

import android.app.Activity
import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import com.google.android.play.core.review.ReviewManagerFactory
import com.google.android.play.core.ktx.launchReview
import com.google.android.play.core.ktx.requestReview
import com.wakeroo.app.container
import com.wakeroo.app.data.Settings
import kotlinx.coroutines.delay

/**
 * When to ask for a Play rating: only after a run of won mornings, never in the first days,
 * never on the alarm or mission screens, and rarely. Google's own quota may still skip it.
 */
object ReviewPolicy {
    const val MIN_STREAK = 3
    const val MAX_PROMPTS = 3
    const val MIN_GAP_MS = 60L * 24 * 3_600_000
    const val MIN_INSTALL_AGE_MS = 4L * 24 * 3_600_000

    fun shouldAsk(streak: Int, prompts: Int, lastAskedAt: Long, installedAt: Long, now: Long): Boolean =
        streak >= MIN_STREAK &&
            prompts < MAX_PROMPTS &&
            now - installedAt >= MIN_INSTALL_AGE_MS &&
            (lastAskedAt == 0L || now - lastAskedAt >= MIN_GAP_MS)
}

/** On Home, after a good streak: shows Google's in-app rating sheet over the app. */
@Composable
fun ReviewPromptEffect(streak: Int, settings: Settings) {
    val activity = LocalActivity.current ?: return
    LaunchedEffect(streak) {
        val installedAt = runCatching {
            activity.packageManager.getPackageInfo(activity.packageName, 0).firstInstallTime
        }.getOrDefault(System.currentTimeMillis())
        val now = System.currentTimeMillis()
        if (!ReviewPolicy.shouldAsk(streak, settings.reviewPrompts, settings.lastReviewPromptAt, installedAt, now)) return@LaunchedEffect
        // Let Home settle first; a sheet over a half-drawn screen feels pushy.
        delay(2_500)
        askForReview(activity)
        activity.container.settings.update { it.copy(reviewPrompts = it.reviewPrompts + 1, lastReviewPromptAt = now) }
    }
}

private suspend fun askForReview(activity: Activity) {
    runCatching {
        val manager = ReviewManagerFactory.create(activity)
        manager.launchReview(activity, manager.requestReview())
    }
}
