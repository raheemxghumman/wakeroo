package com.wakeroo.app.missions

import kotlin.random.Random

/**
 * Something to find around the home. [labels] are the ML Kit base-model labels that count as
 * this object (the bundled model has no "book" or "bottle", so only labels it knows are used),
 * and [minConfidence] is how sure the model must be.
 */
data class HuntObject(
    val id: String,
    val name: String,
    val emoji: String,
    val labels: Set<String>,
    val minConfidence: Float = 0.7f,
    /** Easy objects are in almost every home and the model spots them from any angle. */
    val easy: Boolean = false,
)

object HuntObjects {
    /** Nothing that lives in or next to the bed: the point is to get away from it. */
    val all = listOf(
        HuntObject("cup", "a cup or mug", "☕", setOf("Cup", "Coffee", "Cappuccino", "Saucer"), 0.7f, easy = true),
        HuntObject("sink", "a sink", "🚰", setOf("Sink"), 0.7f, easy = true),
        HuntObject("shoe", "a shoe", "👟", setOf("Shoe", "Sneakers"), 0.7f, easy = true),
        HuntObject("chair", "a chair", "🪑", setOf("Chair", "Armrest"), 0.72f, easy = true),
        HuntObject("plant", "a plant", "🪴", setOf("Plant", "Flowerpot", "Flora", "Flower"), 0.7f, easy = true),
        HuntObject("couch", "a sofa", "🛋️", setOf("Couch", "Loveseat"), 0.72f, easy = true),
        HuntObject("tv", "a TV", "📺", setOf("Television"), 0.72f, easy = true),
        HuntObject("curtain", "the curtains", "🪟", setOf("Curtain"), 0.68f, easy = true),
        HuntObject("tableware", "a plate or bowl", "🍽️", setOf("Tableware", "Saucer"), 0.7f, easy = true),
        HuntObject("cutlery", "a fork or spoon", "🍴", setOf("Cutlery"), 0.7f),
        HuntObject("cookware", "a pot or pan", "🍳", setOf("Cookware and bakeware"), 0.68f),
        HuntObject("counter", "the kitchen counter", "🧑‍🍳", setOf("Countertop", "Kitchen"), 0.72f, easy = true),
        HuntObject("computer", "a laptop or computer", "💻", setOf("Computer"), 0.72f),
        HuntObject("clock", "a clock", "🕰️", setOf("Clock"), 0.65f),
        HuntObject("glasses", "glasses or sunglasses", "👓", setOf("Glasses", "Sunglasses", "Goggles"), 0.65f),
        HuntObject("jacket", "a jacket or coat", "🧥", setOf("Jacket", "Outerwear", "Blazer"), 0.7f),
        HuntObject("jeans", "jeans", "👖", setOf("Jeans", "Denim"), 0.7f),
        HuntObject("hat", "a hat or cap", "🧢", setOf("Hat", "Cap", "Beanie"), 0.68f),
        HuntObject("bag", "a bag", "👜", setOf("Handbag", "Bag"), 0.7f),
        HuntObject("umbrella", "an umbrella", "☂️", setOf("Umbrella"), 0.7f),
        HuntObject("lamp", "a lamp", "💡", setOf("Lampshade"), 0.65f),
        HuntObject("drawer", "a drawer or cupboard", "🗄️", setOf("Drawer", "Cabinetry"), 0.7f),
        HuntObject("shelf", "a shelf", "📚", setOf("Shelf"), 0.7f),
        HuntObject("desk", "a desk", "🖥️", setOf("Desk"), 0.7f),
        HuntObject("poster", "a poster or picture", "🖼️", setOf("Poster"), 0.68f),
        HuntObject("toy", "a toy or teddy", "🧸", setOf("Stuffed toy", "Plush", "Toy", "Lego"), 0.7f),
        HuntObject("scarf", "a scarf", "🧣", setOf("Scarf"), 0.7f),
        HuntObject("bread", "some bread", "🍞", setOf("Bread"), 0.72f),
        HuntObject("fruit", "a piece of fruit", "🍎", setOf("Fruit"), 0.72f),
        HuntObject("tablecloth", "a table mat or cloth", "🧺", setOf("Tablecloth", "Placemat"), 0.7f),
        HuntObject("stairs", "the stairs", "🪜", setOf("Stairs", "Handrail"), 0.72f),
        HuntObject("paper", "a newspaper or sheet of paper", "📰", setOf("Newspaper", "Paper"), 0.72f),
    )

    fun byId(id: String) = all.firstOrNull { it.id == id }

    /** Picks [count] different objects; easy difficulty only uses objects found in nearly every home. */
    fun pick(count: Int, difficulty: Int, random: Random = Random.Default, exclude: Set<String> = emptySet()): List<HuntObject> {
        val pool = (if (difficulty <= 1) all.filter { it.easy } else all).filter { it.id !in exclude }
        return pool.shuffled(random).take(count.coerceIn(1, pool.size))
    }

    /**
     * Best confidence for [target] in one frame's labels, or 0. A label counts at the object's
     * own threshold; Hard asks a little more of the model.
     */
    fun score(target: HuntObject, labels: List<Pair<String, Float>>): Float =
        labels.filter { (text, _) -> text in target.labels }.maxOfOrNull { it.second } ?: 0f

    fun matches(target: HuntObject, labels: List<Pair<String, Float>>, difficulty: Int): Boolean =
        score(target, labels) >= target.minConfidence + if (difficulty >= 3) 0.05f else 0f
}

/** A match must hold for several analysed frames in a row, so one lucky frame can't pass. */
class MatchStreak(private val needed: Int = 3) {
    private var run = 0
    fun offer(matched: Boolean): Boolean {
        run = if (matched) run + 1 else 0
        return run >= needed
    }
    fun reset() { run = 0 }
}
