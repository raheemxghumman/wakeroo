package com.wakeroo.app.data

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.i18n.n
import android.content.Context
import com.wakeroo.app.data.db.MoodEntity
import com.wakeroo.app.data.db.RoutineItemEntity
import com.wakeroo.app.data.db.RoutineLogEntity
import com.wakeroo.app.data.db.SavedQuoteEntity
import com.wakeroo.app.data.db.WakerooDatabase
import com.wakeroo.app.data.model.WakerooJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.LocalDate
import java.util.Locale

@Serializable
data class Quote(
    val id: String,
    @SerialName("t") val text: String,
    @SerialName("a") val author: String? = null,
    @SerialName("th") val theme: String,
)

enum class QuoteTheme(val id: String, private val titleEn: String) {
    Positivity("positivity", n("Positivity")),
    Productivity("productivity", n("Productivity")),
    Calm("calm", n("Calm")),
    HardTimes("hard_times", n("Hard times")),
    Health("health", n("Health")),
    Learning("learning", n("Learning"));

    val title: String get() = tr(titleEn)
}

/** How you felt on waking. Ordered from worst to best. */
enum class Mood(private val titleEn: String) {
    Exhausted(n("Exhausted")), Tired(n("Tired")), Okay(n("Okay")), Calm(n("Calm")), Refreshed(n("Refreshed"));

    val title: String get() = tr(titleEn)
}

/** The bundled quotes for the device language, falling back to English. */
object QuoteBook {
    @Volatile private var cache: Pair<String, List<Quote>>? = null

    suspend fun all(context: Context): List<Quote> = withContext(Dispatchers.IO) {
        val lang = Locale.getDefault().language
        cache?.takeIf { it.first == lang }?.let { return@withContext it.second }
        val text = runCatching { context.assets.open("quotes/$lang.json") }.getOrNull()
            ?: context.assets.open("quotes/en.json")
        val quotes = text.bufferedReader().use { WakerooJson.decodeFromString<List<Quote>>(it.readText()) }
        cache = lang to quotes
        quotes
    }

    /**
     * The same quote all day, a different one tomorrow: the day number picks an offset into
     * the quotes for the chosen themes. [skip] walks forward through today's deck.
     */
    fun forDay(quotes: List<Quote>, date: LocalDate, themes: Set<String>, skip: Int = 0): Quote? {
        val pool = quotes.filter { themes.isEmpty() || it.theme in themes }.ifEmpty { quotes }
        if (pool.isEmpty()) return null
        // A fixed stride that's coprime with most pool sizes, so consecutive days feel varied.
        val index = ((date.toEpochDay() * 37 + skip) % pool.size).toInt().let { if (it < 0) it + pool.size else it }
        return pool[index]
    }
}

class MorningRepository(private val db: Lazy<WakerooDatabase>) {
    private val dao get() = db.value.morning()

    val moods: Flow<List<MoodEntity>> get() = dao.observeMoods()
    val routine: Flow<List<RoutineItemEntity>> get() = dao.observeRoutine()
    val routineLog: Flow<List<RoutineLogEntity>> get() = dao.observeRoutineLog()
    val savedQuotes: Flow<List<SavedQuoteEntity>> get() = dao.observeSavedQuotes()

    suspend fun setMood(date: LocalDate, mood: Mood) = dao.setMood(MoodEntity(date.toString(), mood.ordinal, System.currentTimeMillis()))

    /** Three gentle defaults the first time; the user can change them. */
    suspend fun ensureDefaultRoutine() {
        if (dao.routineCount() > 0) return
        listOf("Drink a glass of water", "Open the curtains", "Stretch for 2 minutes").forEachIndexed { i, t ->
            dao.upsertRoutine(RoutineItemEntity(title = t, position = i))
        }
    }

    suspend fun addRoutine(title: String, position: Int) = dao.upsertRoutine(RoutineItemEntity(title = title.trim().take(40), position = position))

    suspend fun deleteRoutine(item: RoutineItemEntity) {
        dao.deleteRoutine(item)
        dao.clearRoutineLog(item.id)
    }

    suspend fun setDone(date: LocalDate, itemId: Long, done: Boolean) =
        if (done) dao.tick(RoutineLogEntity(date.toString(), itemId, System.currentTimeMillis())) else dao.untick(date.toString(), itemId)

    suspend fun setSaved(quoteId: String, saved: Boolean) =
        if (saved) dao.saveQuote(SavedQuoteEntity(quoteId, System.currentTimeMillis())) else dao.unsaveQuote(quoteId)
}
