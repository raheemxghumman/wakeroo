package com.wakeroo.app.weather

import com.wakeroo.app.ui.i18n.tr
import com.wakeroo.app.ui.i18n.n
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.wakeroo.app.MainActivity
import com.wakeroo.app.R
import com.wakeroo.app.feature.main.MainTab
import com.wakeroo.app.reminders.Reminders
import java.time.LocalDate
import com.wakeroo.app.container
import com.wakeroo.app.data.City
import com.wakeroo.app.data.TempUnit
import com.wakeroo.app.data.model.WakerooJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.time.LocalDateTime

/** What the sky is doing, from WMO weather codes. */
enum class Condition(private val labelEn: String) {
    Clear(n("Clear")), PartlyCloudy(n("Partly cloudy")), Cloudy(n("Cloudy")), Fog(n("Foggy")),
    Drizzle(n("Drizzle")), Rain(n("Rain")), Snow(n("Snow")), Storm(n("Thunderstorms"));

    val label: String get() = tr(labelEn)

    val wet: Boolean get() = this == Drizzle || this == Rain || this == Storm

    companion object {
        fun fromCode(code: Int): Condition = when (code) {
            0 -> Clear
            1, 2 -> PartlyCloudy
            3 -> Cloudy
            45, 48 -> Fog
            in 51..57 -> Drizzle
            in 61..67, in 80..82 -> Rain
            in 71..77, 85, 86 -> Snow
            in 95..99 -> Storm
            else -> Cloudy
        }
    }
}

@Serializable
data class HourWeather(val time: String, val temp: Double, val rainChance: Int, val code: Int) {
    val hour: Int get() = LocalDateTime.parse(time).hour
    val condition: Condition get() = Condition.fromCode(code)
}

@Serializable
data class Forecast(
    val city: String,
    val fetchedAt: Long,
    val unit: String,
    val currentTemp: Double,
    val currentCode: Int,
    val isDay: Boolean,
    val high: Double,
    val low: Double,
    val rainChance: Int,
    /** The next 24 hours, starting with the current hour. */
    val hours: List<HourWeather>,
) {
    val condition: Condition get() = Condition.fromCode(currentCode)

    /**
     * A one-line heads-up if today's weather is worth a notification, else null: likely
     * rain later today, or a really cold or hot day.
     */
    fun notableToday(now: LocalDateTime = LocalDateTime.now()): String? {
        val today = hours.filter { LocalDateTime.parse(it.time).toLocalDate() == now.toLocalDate() && LocalDateTime.parse(it.time) >= now.withMinute(0) }
        val rainy = today.firstOrNull { it.rainChance >= 60 && it.condition.wet }
        val celsius = if (unit == "fahrenheit") (high - 32) * 5 / 9 else high
        val celsiusLow = if (unit == "fahrenheit") (low - 32) * 5 / 9 else low
        return when {
            rainy != null -> "Rain likely around ${hourLabel(rainy.hour)}. Take an umbrella."
            celsiusLow <= 2 -> "Cold one today, down to ${low.toInt()}°. Wrap up warm."
            celsius >= 35 -> "Hot day ahead, up to ${high.toInt()}°. Drink plenty of water."
            else -> null
        }
    }
}

fun hourLabel(hour: Int): String = when {
    hour == 0 -> tr("midnight")
    hour == 12 -> tr("noon")
    hour < 12 -> "$hour AM"
    else -> "${hour - 12} PM"
}

/**
 * Weather from Open-Meteo: free, keyless, and sent nothing but the chosen city's
 * coordinates. Forecasts are cached for three hours so the Morning tab and the wake-up
 * screen never wait on the network; offline, the weather card simply hides.
 */
class WeatherClient(private val context: Context) {
    private val cache get() = File(context.filesDir, "weather.json")

    fun cached(): Forecast? = runCatching { WakerooJson.decodeFromString<Forecast>(cache.readText()) }.getOrNull()

    /** Returns a fresh-enough forecast, fetching only if the cache is stale or for another city. */
    suspend fun forecast(city: City, unit: TempUnit, maxAgeMs: Long = 3 * 3_600_000L): Forecast? = withContext(Dispatchers.IO) {
        val old = cached()
        val unitName = if (unit == TempUnit.Fahrenheit) "fahrenheit" else "celsius"
        if (old != null && old.city == city.name && old.unit == unitName && System.currentTimeMillis() - old.fetchedAt < maxAgeMs) return@withContext old
        val fresh = runCatching { fetch(city, unitName) }.getOrNull()
        if (fresh != null) runCatching { cache.writeText(WakerooJson.encodeToString(fresh)) }
        fresh ?: old?.takeIf { it.city == city.name }
    }

    private fun fetch(city: City, unit: String): Forecast {
        val url = "https://api.open-meteo.com/v1/forecast?latitude=${city.latitude}&longitude=${city.longitude}" +
            "&current=temperature_2m,weather_code,is_day" +
            "&hourly=temperature_2m,precipitation_probability,weather_code" +
            "&daily=temperature_2m_max,temperature_2m_min,precipitation_probability_max" +
            "&timezone=auto&forecast_days=2&temperature_unit=$unit"
        return parseForecast(get(url), city.name, unit, System.currentTimeMillis())
    }

    suspend fun searchCities(query: String): List<City> = withContext(Dispatchers.IO) {
        if (query.trim().length < 2) return@withContext emptyList()
        val q = URLEncoder.encode(query.trim(), "UTF-8")
        runCatching { parseCities(get("https://geocoding-api.open-meteo.com/v1/search?name=$q&count=8&language=en&format=json")) }.getOrDefault(emptyList())
    }

    private fun get(url: String): String {
        val conn = URL(url).openConnection() as HttpURLConnection
        conn.connectTimeout = 8000
        conn.readTimeout = 8000
        conn.setRequestProperty("User-Agent", "Wakeroo")
        return try {
            if (conn.responseCode != 200) error("HTTP ${conn.responseCode}")
            conn.inputStream.bufferedReader().use { it.readText() }
        } finally {
            conn.disconnect()
        }
    }

    companion object {
        fun parseForecast(json: String, city: String, unit: String, now: Long): Forecast {
            val root = WakerooJson.parseToJsonElement(json).jsonObject
            val current = root["current"]!!.jsonObject
            val hourly = root["hourly"]!!.jsonObject
            val daily = root["daily"]!!.jsonObject
            val times = hourly.arr("time").map { it.jsonPrimitive.content }
            val temps = hourly.arr("temperature_2m").map { it.jsonPrimitive.doubleOrNull ?: 0.0 }
            val rain = hourly.arr("precipitation_probability").map { it.jsonPrimitive.intOrNull ?: 0 }
            val codes = hourly.arr("weather_code").map { it.jsonPrimitive.intOrNull ?: 0 }
            val currentTime = current["time"]?.jsonPrimitive?.content?.let { LocalDateTime.parse(it) }
            val start = currentTime?.let { ct -> times.indexOfFirst { LocalDateTime.parse(it) >= ct.withMinute(0) } }?.coerceAtLeast(0) ?: 0
            val hours = (start until minOf(start + 24, times.size)).map { i -> HourWeather(times[i], temps[i], rain.getOrElse(i) { 0 }, codes.getOrElse(i) { 0 }) }
            return Forecast(
                city = city, fetchedAt = now, unit = unit,
                currentTemp = current["temperature_2m"]!!.jsonPrimitive.doubleOrNull ?: 0.0,
                currentCode = current["weather_code"]!!.jsonPrimitive.intOrNull ?: 0,
                isDay = (current["is_day"]?.jsonPrimitive?.intOrNull ?: 1) == 1,
                high = daily.arr("temperature_2m_max").first().jsonPrimitive.doubleOrNull ?: 0.0,
                low = daily.arr("temperature_2m_min").first().jsonPrimitive.doubleOrNull ?: 0.0,
                rainChance = daily.arr("precipitation_probability_max").first().jsonPrimitive.intOrNull ?: 0,
                hours = hours,
            )
        }

        fun parseCities(json: String): List<City> {
            val results = WakerooJson.parseToJsonElement(json).jsonObject["results"] as? JsonArray ?: return emptyList()
            return results.map { it.jsonObject }.mapNotNull { o ->
                val name = o["name"]?.jsonPrimitive?.content ?: return@mapNotNull null
                val region = listOfNotNull(o["admin1"]?.jsonPrimitive?.content, o["country"]?.jsonPrimitive?.content).joinToString(", ")
                City(name, region, o["latitude"]!!.jsonPrimitive.doubleOrNull ?: return@mapNotNull null, o["longitude"]!!.jsonPrimitive.doubleOrNull ?: return@mapNotNull null)
            }
        }

        private fun JsonObject.arr(key: String) = this[key]!!.jsonArray
    }
}

/**
 * The morning weather heads-up: after the first alarm of the day is dismissed, if the user
 * opted in and today has notable weather (rain later, a freezing or very hot day).
 */
object WeatherNotifier {
    private const val ID = 22

    suspend fun afterWakeUp(context: Context) {
        val app = context.container
        val settings = app.settings.current()
        if (!settings.weatherNotification) return
        val city = settings.weatherCity ?: return
        val prefs = context.getSharedPreferences("weather_notifier", Context.MODE_PRIVATE)
        val today = LocalDate.now().toString()
        if (prefs.getString("last", null) == today) return
        val text = app.weather.forecast(city, settings.tempUnit, maxAgeMs = 3_600_000L)?.notableToday() ?: return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        prefs.edit().putString("last", today).apply()
        val open = PendingIntent.getActivity(
            context, 60,
            Intent(context, MainActivity::class.java).putExtra(MainActivity.EXTRA_TAB, MainTab.Morning.name),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        val n = NotificationCompat.Builder(context, Reminders.CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_wakeroo)
            .setContentTitle("Weather in ${city.name}")
            .setContentText(text)
            .setContentIntent(open)
            .setAutoCancel(true)
            .build()
        NotificationManagerCompat.from(context).notify(ID, n)
    }
}
