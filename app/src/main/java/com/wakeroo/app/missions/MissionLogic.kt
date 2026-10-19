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

/** Patterns for the memory-tiles mission. Each round lights one more tile than the last. */
object MemoryPatterns {
    fun lengthFor(round: Int, gridSize: Int): Int = (if (gridSize >= 4) 4 else 3) + round

    /** Distinct tile indices on a [gridSize]×[gridSize] board. */
    fun generate(gridSize: Int, length: Int, random: Random = Random.Default): List<Int> =
        (0 until gridSize * gridSize).shuffled(random).take(length.coerceIn(1, gridSize * gridSize))
}

/**
 * Checks a typed morning plan reads like a real intention ("make coffee and stretch"), not
 * thumb-mashing ("asdfgh jkl"). Simple rules, generous for real words in any Latin script.
 */
object PlanValidator {
    const val MIN_WORDS = 3
    const val MIN_LETTERS = 12
    private val ROWS = listOf("qwertyuiop", "asdfghjkl", "zxcvbnm")

    enum class Problem { TooShort, NotWords }

    fun check(text: String): Problem? {
        val words = TypingPhrases.normalize(text).split(' ').filter { it.isNotEmpty() }
        val letters = words.sumOf { w -> w.count { it.isLetter() } }
        if (words.size < MIN_WORDS || letters < MIN_LETTERS) return Problem.TooShort
        val joined = words.joinToString("")
        val vowels = joined.count { it in "aeiouyáéíóúàèìòùâêîôûäëïöüãõ" }
        val ratio = vowels / joined.length.toFloat()
        if (ratio < 0.18f || ratio > 0.75f) return Problem.NotWords
        if (Regex("""(.)\1\1\1""").containsMatchIn(joined)) return Problem.NotWords
        if (words.distinct().size < 2) return Problem.NotWords
        // Runs of four or more neighbouring keys, like "asdf" or "wert".
        val mashed = words.count { w -> w.length >= 4 && ROWS.any { row -> (0..w.length - 4).any { i -> w.substring(i, i + 4) in row } } }
        if (mashed * 2 >= words.size) return Problem.NotWords
        val realWords = words.count { w -> w.length >= 2 && w.any { it in "aeiouy" } }
        if (realWords < MIN_WORDS) return Problem.NotWords
        return null
    }
}
