package com.wakeroo.app.missions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.random.Random

class MissionLogicTest {
    @Test fun mathAnswersArePositiveAndCorrect() {
        val rnd = Random(42)
        for (difficulty in 1..3) repeat(500) {
            val p = MathProblems.generate(difficulty, rnd)
            assertTrue("answer must be positive: $p", p.answer > 0)
            assertEquals(p.text, evaluate(p.text), p.answer)
        }
    }

    @Test fun typingIgnoresCaseAndSpacing() {
        assertTrue(TypingPhrases.matches("my feet are on the floor", "  My Feet  are on THE floor "))
    }

    @Test fun hashOfSameImageIsIdentical() {
        val img = { x: Int, y: Int -> (x * 3 + y * 5) % 256 }
        assertEquals(0, ImageHash.distance(ImageHash.dHash(img, 320, 240), ImageHash.dHash(img, 320, 240)))
    }

    @Test fun hashSurvivesBrightnessChange() {
        val img = { x: Int, y: Int -> if (x < 160) 60 + y / 4 else 200 - y / 8 }
        val brighter = { x: Int, y: Int -> (img(x, y) + 40).coerceAtMost(255) }
        assertTrue(ImageHash.distance(ImageHash.dHash(img, 320, 240), ImageHash.dHash(brighter, 320, 240)) <= 4)
    }

    @Test fun hashDiffersForDifferentScenes() {
        val a = { x: Int, _: Int -> x % 256 }
        val b = { x: Int, _: Int -> 255 - x % 256 }
        assertTrue(ImageHash.distance(ImageHash.dHash(a, 256, 256), ImageHash.dHash(b, 256, 256)) > 40)
    }

    /** Tiny evaluator for "a × b + c" and "a − b + c" (left to right, × first). */
    private fun evaluate(expr: String): Int {
        val tokens = expr.split(" ")
        val values = mutableListOf(tokens[0].toInt())
        val ops = mutableListOf<String>()
        var i = 1
        while (i < tokens.size) {
            val op = tokens[i]; val v = tokens[i + 1].toInt()
            if (op == "×") values[values.lastIndex] = values.last() * v else { ops += op; values += v }
            i += 2
        }
        var result = values[0]
        ops.forEachIndexed { k, op -> result = if (op == "+") result + values[k + 1] else result - values[k + 1] }
        return result
    }
}
