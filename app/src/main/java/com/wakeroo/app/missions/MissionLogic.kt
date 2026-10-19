package com.wakeroo.app.missions

import kotlin.random.Random

data class MathProblem(val text: String, val answer: Int)

/**
 * Problems hard enough to need a working brain but never needing paper. Answers are always
 * positive whole numbers so the keypad never needs a minus sign or decimals.
 */
object MathProblems {
    fun generate(difficulty: Int, random: Random = Random.Default): MathProblem = when (difficulty) {
        1 -> {
            val a = random.nextInt(11, 50); val b = random.nextInt(3, 10)
            MathProblem("$a + $b", a + b)
        }
        2 -> {
            val a = random.nextInt(12, 30); val b = random.nextInt(3, 9); val c = random.nextInt(11, 60)
            MathProblem("$a × $b + $c", a * b + c)
        }
        else -> {
            val a = random.nextInt(13, 40); val b = random.nextInt(6, 13); val c = random.nextInt(101, 400)
            if (random.nextBoolean()) MathProblem("$a × $b + $c", a * b + c)
            else MathProblem("$c − $a + ${b * 7}", c - a + b * 7)
        }
    }
}

/** Short, upbeat phrases: easy to type with one thumb, impossible to type asleep. */
object TypingPhrases {
    private val phrases = listOf(
        "today is going to be good",
        "my feet are on the floor",
        "the early bird gets the worm",
        "i am up and i am ready",
        "coffee first then the world",
        "small steps every morning",
        "rise and shine sleepy head",
        "i choose to start strong",
        "open the curtains and breathe",
        "one more day of progress",
    )

    fun pick(count: Int, random: Random = Random.Default): List<String> = phrases.shuffled(random).take(count.coerceIn(1, phrases.size))

    /** Case and repeated spaces don't matter. */
    fun matches(target: String, typed: String) = normalize(target) == normalize(typed)

    fun normalize(s: String) = s.trim().lowercase().replace(Regex("\\s+"), " ")
}

/** 64-bit difference hash: robust to lighting and small framing changes, cheap to compute. */
object ImageHash {
    /** [luma] is a row-major grayscale buffer of [width] x [height]. */
    fun dHash(luma: (x: Int, y: Int) -> Int, width: Int, height: Int): Long {
        var hash = 0L
        var bit = 0
        for (row in 0 until 8) {
            for (col in 0 until 8) {
                val left = cell(luma, width, height, col, row)
                val right = cell(luma, width, height, col + 1, row)
                if (left > right) hash = hash or (1L shl bit)
                bit++
            }
        }
        return hash
    }

    /** Average luminance of one cell in a 9 x 8 grid, sampled sparsely for speed. */
    private fun cell(luma: (Int, Int) -> Int, width: Int, height: Int, cx: Int, cy: Int): Int {
        val x0 = cx * width / 9; val x1 = (cx + 1) * width / 9
        val y0 = cy * height / 8; val y1 = (cy + 1) * height / 8
        var sum = 0L; var n = 0
        var y = y0
        while (y < y1) {
            var x = x0
            while (x < x1) { sum += luma(x, y); n++; x += 4 }
            y += 4
        }
        return if (n == 0) 0 else (sum / n).toInt()
    }

    fun distance(a: Long, b: Long): Int = java.lang.Long.bitCount(a xor b)
}
