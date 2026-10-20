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

/**
 * Short, upbeat phrases in the app's language: easy to type with one thumb, impossible to type
 * asleep. The keypad is Latin a-z, so accents are optional ("dia" types "día") and Hindi and
 * Urdu use the romanised spelling most people already text in. Arabic falls back to English.
 */
object TypingPhrases {
    private val escapes = mapOf(
        "en" to "i am awake", "es" to "estoy despierto", "pt" to "estou acordado", "fr" to "je suis debout",
        "id" to "aku sudah bangun", "tr" to "uyandım ben", "hi" to "main jaag gaya", "ur" to "main jaag gaya",
    )

    private val phrases = mapOf(
        "en" to listOf(
            "today is going to be good", "my feet are on the floor", "the early bird gets the worm",
            "i am up and i am ready", "coffee first then the world", "small steps every morning",
            "rise and shine sleepy head", "i choose to start strong", "open the curtains and breathe",
            "one more day of progress",
        ),
        "es" to listOf(
            "hoy va a ser un gran día", "mis pies ya están en el suelo", "a quien madruga dios le ayuda",
            "estoy listo para empezar", "primero el café y luego el mundo", "pasos pequeños cada mañana",
            "arriba que el día espera", "hoy elijo empezar con fuerza", "abre las cortinas y respira",
            "un día más de progreso",
        ),
        "pt" to listOf(
            "hoje vai ser um bom dia", "meus pés estão no chão", "deus ajuda quem cedo madruga",
            "estou de pé e estou pronto", "primeiro o café depois o mundo", "pequenos passos toda manhã",
            "bom dia flor do dia", "hoje escolho começar forte", "abra as cortinas e respire",
            "mais um dia de progresso",
        ),
        "fr" to listOf(
            "aujourd hui sera une belle journée", "mes pieds sont sur le sol", "le monde appartient à ceux qui se lèvent tôt",
            "je suis debout et je suis prêt", "un café puis le monde", "des petits pas chaque matin",
            "debout la journée commence", "je choisis de bien commencer", "ouvre les rideaux et respire",
            "encore un jour de progrès",
        ),
        "id" to listOf(
            "hari ini akan jadi hari yang baik", "kakiku sudah menyentuh lantai", "bangun pagi rezeki tidak dipatok ayam",
            "aku sudah bangun dan siap", "kopi dulu baru dunia", "langkah kecil setiap pagi",
            "ayo bangun dan bersinar", "aku memilih mulai dengan kuat", "buka gorden dan tarik napas",
            "satu hari lagi untuk maju",
        ),
        "tr" to listOf(
            "bugün güzel bir gün olacak", "ayaklarım yere bastı", "erken kalkan yol alır",
            "kalktım ve hazırım", "önce kahve sonra dünya", "her sabah küçük adımlar",
            "günaydın uykucu", "güçlü başlamayı seçiyorum", "perdeleri aç ve nefes al",
            "ilerlemek için bir gün daha",
        ),
        "hi" to listOf(
            "aaj ka din accha hoga", "mere pair zameen par hain", "jaldi uthne wale aage badhte hain",
            "main uth gaya aur taiyaar hoon", "pehle chai phir duniya", "har subah chhote kadam",
            "utho aur chamko", "aaj main zor se shuru karunga", "parde kholo aur saans lo",
            "aage badhne ka ek aur din",
        ),
        "ur" to listOf(
            "aaj ka din acha hoga", "mere paon zameen par hain", "jaldi uthne wale aage barhte hain",
            "main uth gaya aur tayyar hoon", "pehle chai phir dunya", "har subah chhote qadam",
            "utho aur chamko", "aaj main zor se shuru karunga", "parde kholo aur saans lo",
            "aage barhne ka aik aur din",
        ),
    )

    private fun language() = java.util.Locale.getDefault().language.let { if (it == "in") "id" else it }

    /** Typed to leave through the emergency exit, in the app's language. */
    val ESCAPE: String get() = escapes[language()] ?: escapes.getValue("en")

    fun pick(count: Int, random: Random = Random.Default, language: String = language()): List<String> {
        val pool = phrases[language] ?: phrases.getValue("en")
        return pool.shuffled(random).take(count.coerceIn(1, pool.size))
    }

    /** Case, repeated spaces and accents don't matter. */
    fun matches(target: String, typed: String) = fold(normalize(target)) == fold(normalize(typed))

    fun normalize(s: String) = s.trim().lowercase().replace(Regex("""\s+"""), " ")

    /** "día" → "dia", "günaydın" → "gunaydin": one letter in, one letter out, so positions line up. */
    fun fold(s: String): String = buildString(s.length) {
        for (c in s) {
            val base = java.text.Normalizer.normalize(c.toString(), java.text.Normalizer.Form.NFD).first()
            append(if (c == 'ı') 'i' else base)
        }
    }
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
