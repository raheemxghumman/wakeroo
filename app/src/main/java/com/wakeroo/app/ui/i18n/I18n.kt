package com.wakeroo.app.ui.i18n

import android.content.Context
import com.wakeroo.app.data.model.WakerooJson
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

/**
 * Wakeroo's translations. The English text in the code is the key, so screens read naturally
 * and nothing is ever missing: an untranslated string simply shows in English. Each language
 * is a JSON file in assets/i18n, checked for gaps by tools/i18n.py.
 *
 * Placeholders are numbered: tr("Rings in {0}", until). Plural forms are separate keys.
 */
object I18n {
    /** The languages Wakeroo ships, as BCP-47 tags, with their own names. */
    val languages = listOf(
        "en" to "English", "hi" to "हिन्दी", "ur" to "اردو", "es" to "Español", "pt-BR" to "Português (Brasil)",
        "id" to "Bahasa Indonesia", "ar" to "العربية", "tr" to "Türkçe", "fr" to "Français",
    )

    private lateinit var appContext: Context
    private val tables = ConcurrentHashMap<String, Map<String, String>>()

    fun init(context: Context) {
        appContext = context.applicationContext
    }

    /** The catalogue file for the current locale: "pt" covers Brazilian Portuguese. */
    private fun language(): String = Locale.getDefault().language.let { if (it == "in") "id" else it }

    private fun table(lang: String): Map<String, String> = tables.getOrPut(lang) {
        if (lang == "en" || !::appContext.isInitialized) emptyMap()
        else runCatching {
            appContext.assets.open("i18n/$lang.json").bufferedReader().use { WakerooJson.decodeFromString<Map<String, String>>(it.readText()) }
        }.getOrDefault(emptyMap())
    }

    fun translate(key: String, args: Array<out Any?>): String {
        var text = table(language())[key] ?: key
        args.forEachIndexed { i, a -> text = text.replace("{$i}", a?.toString() ?: "") }
        return text
    }
}

/** Translates [text] (the English source) into the app's language, filling {0}, {1}… from [args]. */
fun tr(text: String, vararg args: Any?): String = I18n.translate(text, args)

/**
 * Marks English text for the catalogue without translating it yet. Use it where text is
 * stored once (enum entries, constant lists) and translate it when read with [tr], so a
 * language change applies straight away.
 */
fun n(text: String): String = text
