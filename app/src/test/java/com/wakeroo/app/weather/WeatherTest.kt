package com.wakeroo.app.weather

import com.wakeroo.app.data.Quote
import com.wakeroo.app.data.QuoteBook
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.time.LocalDateTime

class WeatherTest {
    private fun fixture(rainAt: Int = -1, low: Double = 12.0, high: Double = 24.0): String {
        val hours = (0 until 48).map { i -> "2026-10-${20 + i / 24}T%02d:00".format(i % 24) }
        val temps = hours.indices.map { 14 + it % 10 }
        val rain = hours.indices.map { if (it == rainAt) 80 else 5 }
        val codes = hours.indices.map { if (it == rainAt) 63 else 1 }
        return """
        {"current":{"time":"2026-10-20T06:30","temperature_2m":13.4,"weather_code":2,"is_day":1},
         "hourly":{"time":${hours.joinToString(",", "[", "]") { "\"$it\"" }},
                   "temperature_2m":$temps,"precipitation_probability":$rain,"weather_code":$codes},
         "daily":{"temperature_2m_max":[$high,20.0],"temperature_2m_min":[$low,9.0],"precipitation_probability_max":[40,10]}}
        """.trimIndent()
    }

    @Test fun parsesCurrentDailyAndNext24Hours() {
        val f = WeatherClient.parseForecast(fixture(), "Lisbon", "celsius", 1L)
        assertEquals(13.4, f.currentTemp, 0.01)
        assertEquals(Condition.PartlyCloudy, f.condition)
        assertEquals(24.0, f.high, 0.01)
        assertEquals(40, f.rainChance)
        assertEquals(24, f.hours.size)
        assertEquals("2026-10-20T06:00", f.hours.first().time)
    }

    @Test fun rainLaterTodayIsNotable() {
        val f = WeatherClient.parseForecast(fixture(rainAt = 17), "Lisbon", "celsius", 1L)
        val note = f.notableToday(LocalDateTime.of(2026, 10, 20, 6, 40))
        assertTrue(note!!.contains("5 PM"))
    }

    @Test fun ordinaryDayIsNotNotable() {
        val f = WeatherClient.parseForecast(fixture(), "Lisbon", "celsius", 1L)
        assertNull(f.notableToday(LocalDateTime.of(2026, 10, 20, 6, 40)))
    }

    @Test fun freezingDayIsNotable() {
        val f = WeatherClient.parseForecast(fixture(low = -3.0, high = 4.0), "Oslo", "celsius", 1L)
        assertTrue(f.notableToday(LocalDateTime.of(2026, 10, 20, 6, 40))!!.contains("Cold"))
    }

    @Test fun fahrenheitThresholdsConvert() {
        // 95°F is 35°C: a hot day in Fahrenheit counts too.
        val f = WeatherClient.parseForecast(fixture(low = 70.0, high = 99.0), "Phoenix", "fahrenheit", 1L)
        assertTrue(f.notableToday(LocalDateTime.of(2026, 10, 20, 6, 40))!!.contains("Hot"))
    }

    @Test fun weatherCodesMap() {
        assertEquals(Condition.Clear, Condition.fromCode(0))
        assertEquals(Condition.Fog, Condition.fromCode(45))
        assertEquals(Condition.Rain, Condition.fromCode(81))
        assertEquals(Condition.Snow, Condition.fromCode(73))
        assertEquals(Condition.Storm, Condition.fromCode(95))
    }

    @Test fun citiesParse() {
        val json = """{"results":[{"name":"Lahore","latitude":31.55,"longitude":74.34,"country":"Pakistan","admin1":"Punjab"}]}"""
        val c = WeatherClient.parseCities(json).single()
        assertEquals("Lahore", c.name)
        assertEquals("Punjab, Pakistan", c.region)
        assertTrue(WeatherClient.parseCities("""{"generationtime_ms":0.2}""").isEmpty())
    }

    @Test fun quoteOfTheDayIsStableAndChangesDaily() {
        val quotes = (1..40).map { Quote("q$it", "text $it", theme = if (it % 2 == 0) "calm" else "health") }
        val day = LocalDate.of(2026, 10, 20)
        assertEquals(QuoteBook.forDay(quotes, day, emptySet()), QuoteBook.forDay(quotes, day, emptySet()))
        assertNotEquals(QuoteBook.forDay(quotes, day, emptySet()), QuoteBook.forDay(quotes, day.plusDays(1), emptySet()))
        assertEquals("calm", QuoteBook.forDay(quotes, day, setOf("calm"))!!.theme)
        assertNotEquals(QuoteBook.forDay(quotes, day, emptySet(), 0), QuoteBook.forDay(quotes, day, emptySet(), 1))
    }
}
