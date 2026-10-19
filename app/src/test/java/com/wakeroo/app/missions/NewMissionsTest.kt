package com.wakeroo.app.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.sin
import kotlin.random.Random

class NewMissionsTest {
    @Test fun memoryPatternsAreDistinctAndGrowEachRound() {
        repeat(50) { seed ->
            val p = MemoryPatterns.generate(3, MemoryPatterns.lengthFor(seed % 4, 3), Random(seed))
            assertEquals(p.size, p.distinct().size)
            assertTrue(p.all { it in 0 until 9 })
        }
        assertEquals(3, MemoryPatterns.lengthFor(0, 3))
        assertEquals(4, MemoryPatterns.lengthFor(1, 3))
        assertEquals(4, MemoryPatterns.lengthFor(0, 4))
    }

    @Test fun memoryPracticeAndRealRunsDiffer() {
        assertTrue(MemoryPatterns.generate(4, 6, Random(1)) != MemoryPatterns.generate(4, 6, Random(2)))
    }

    @Test fun realPlansPass() {
        listOf(
            "make coffee and go for a walk",
            "finish the essay before lunch",
            "gym then shower then work",
            "call mum and pay the rent",
            "estudiar para el examen de historia",
        ).forEach { assertNull(it, PlanValidator.check(it)) }
    }

    @Test fun shortPlansAreTooShort() {
        assertEquals(PlanValidator.Problem.TooShort, PlanValidator.check("run"))
        assertEquals(PlanValidator.Problem.TooShort, PlanValidator.check("go to gym"))
    }

    @Test fun mashingIsRejected() {
        listOf("asdf asdf asdf asdf", "qwerty uiop asdfg", "aaaaaaa bbbbbbb ccccccc", "xxxx zzzz kkkk jjjj", "jkl jkl jkl jkl jkl").forEach {
            assertEquals(it, PlanValidator.Problem.NotWords, PlanValidator.check(it))
        }
    }

    @Test fun huntNeedsTheRightLabelAtTheRightConfidence() {
        val cup = HuntObjects.byId("cup")!!
        assertTrue(HuntObjects.matches(cup, listOf("Cup" to 0.82f), 2))
        assertTrue(HuntObjects.matches(cup, listOf("Table" to 0.9f, "Coffee" to 0.75f), 2))
        assertFalse(HuntObjects.matches(cup, listOf("Cup" to 0.55f), 2))
        assertFalse(HuntObjects.matches(cup, listOf("Shoe" to 0.95f), 2))
        // Hard asks for a little more certainty.
        assertFalse(HuntObjects.matches(cup, listOf("Cup" to 0.72f), 3))
    }

    @Test fun easyHuntOnlyUsesEverydayObjects() {
        repeat(30) { seed ->
            HuntObjects.pick(3, 1, Random(seed)).forEach { assertTrue(it.easy) }
        }
        val picked = HuntObjects.pick(3, 2, Random(4))
        assertEquals(3, picked.map { it.id }.distinct().size)
        assertTrue(HuntObjects.pick(1, 2, exclude = HuntObjects.all.drop(1).map { it.id }.toSet()).single().id == HuntObjects.all.first().id)
    }

    @Test fun oneLuckyFrameIsNotEnough() {
        val streak = MatchStreak(3)
        assertFalse(streak.offer(true))
        assertFalse(streak.offer(false))
        assertFalse(streak.offer(true))
        assertFalse(streak.offer(true))
        assertTrue(streak.offer(true))
    }

    /** Synthetic accelerometer trace: gravity plus a bounce at [hz] with [amplitude] m/s². */
    private fun trace(seconds: Int, hz: Float, amplitude: Float): Int {
        val h = StepHeuristic()
        var steps = 0
        val rate = 50
        for (i in 0 until seconds * rate) {
            val t = i / rate.toFloat()
            val z = 9.80665f + amplitude * sin(2 * PI * hz * t).toFloat()
            if (h.onSample(0f, 0f, z, (t * 1000).toLong())) steps++
        }
        return steps
    }

    @Test fun walkingCadenceCounts() {
        val steps = trace(10, 1.8f, 3f) // brisk walk, 18 steps
        assertTrue("got $steps", steps in 15..19)
    }

    @Test fun violentShakingDoesNotCount() {
        val steps = trace(10, 4f, 20f)
        assertTrue("got $steps", steps < 5)
    }

    @Test fun lyingStillDoesNotCount() {
        assertEquals(0, trace(10, 0.5f, 0.3f))
    }
}
