package com.wakeroo.app.feature.review

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ReviewPolicyTest {
    private val day = 24L * 3_600_000
    private val now = 1_800_000_000_000L
    private val installed = now - 10 * day

    @Test fun asksAfterAStreakOnceSettledIn() {
        assertTrue(ReviewPolicy.shouldAsk(streak = 3, prompts = 0, lastAskedAt = 0, installedAt = installed, now = now))
    }

    @Test fun waitsForAStreak() {
        assertFalse(ReviewPolicy.shouldAsk(streak = 2, prompts = 0, lastAskedAt = 0, installedAt = installed, now = now))
    }

    @Test fun neverInTheFirstDays() {
        assertFalse(ReviewPolicy.shouldAsk(streak = 5, prompts = 0, lastAskedAt = 0, installedAt = now - 2 * day, now = now))
    }

    @Test fun leavesTwoMonthsBetweenAsks() {
        assertFalse(ReviewPolicy.shouldAsk(streak = 7, prompts = 1, lastAskedAt = now - 30 * day, installedAt = installed - 90 * day, now = now))
        assertTrue(ReviewPolicy.shouldAsk(streak = 7, prompts = 1, lastAskedAt = now - 61 * day, installedAt = installed - 90 * day, now = now))
    }

    @Test fun stopsAfterThreeAsks() {
        assertFalse(ReviewPolicy.shouldAsk(streak = 30, prompts = 3, lastAskedAt = now - 365 * day, installedAt = installed - 400 * day, now = now))
    }
}
