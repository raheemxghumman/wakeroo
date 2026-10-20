package com.wakeroo.app.ui.i18n

import android.app.Activity
import android.app.LocaleManager
import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app's own language, separate from the phone's. Android 13+ keeps it in the system's
 * per-app language setting (so it also shows in system Settings); older phones keep it here
 * and apply it to each activity.
 */
object AppLocale {
    private const val PREFS = "app_locale"
    private const val KEY = "tag"

    /** The chosen language tag, or null to follow the phone. */
    fun current(context: Context): String? =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.getSystemService(LocaleManager::class.java).applicationLocales.takeIf { !it.isEmpty }?.get(0)?.toLanguageTag()
        } else prefs(context).getString(KEY, null)

    fun set(activity: Activity, tag: String?) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            // The system restarts the activity in the new language.
            activity.getSystemService(LocaleManager::class.java).applicationLocales =
                if (tag == null) LocaleList.getEmptyLocaleList() else LocaleList.forLanguageTags(tag)
        } else {
            prefs(activity).edit().apply { if (tag == null) remove(KEY) else putString(KEY, tag) }.commit()
            applyDefault(activity)
            activity.recreate()
        }
    }

    /** Older phones: make the chosen language the process default, so formatting follows it too. */
    fun applyDefault(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return
        val tag = prefs(context).getString(KEY, null) ?: return
        Locale.setDefault(Locale.forLanguageTag(tag))
    }

    /** Older phones: an activity's base context in the chosen language (layout direction included). */
    fun wrap(base: Context): Context {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) return base
        val tag = prefs(base).getString(KEY, null) ?: return base
        val locale = Locale.forLanguageTag(tag)
        Locale.setDefault(locale)
        val config = Configuration(base.resources.configuration).apply { setLocale(locale); setLayoutDirection(locale) }
        return base.createConfigurationContext(config)
    }

    // Device-protected: the process can start before the first unlock after a reboot (an alarm
    // ringing), and credential-encrypted preferences would throw then.
    private fun prefs(context: Context) =
        context.applicationContext.createDeviceProtectedStorageContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE)
}
