package com.wakeroo.app.feature.about

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import com.wakeroo.app.BuildConfig
import java.util.Locale

const val DEVELOPER_NAME = "Qubiq Studios"

/** Feedback by email, with the details we'd otherwise have to ask for. */
fun sendFeedback(context: Context) {
    val body = buildString {
        append("\n\n\n---\n")
        append("Wakeroo ${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})\n")
        append("${Build.MANUFACTURER} ${Build.MODEL}\n")
        append("Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n")
        append("Language: ${Locale.getDefault().toLanguageTag()}\n")
    }
    val intent = Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:"))
        .putExtra(Intent.EXTRA_EMAIL, arrayOf(BuildConfig.SUPPORT_EMAIL))
        .putExtra(Intent.EXTRA_SUBJECT, "Wakeroo feedback")
        .putExtra(Intent.EXTRA_TEXT, body)
        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    runCatching { context.startActivity(intent) }
}

/** Wakeroo's page in the Play Store app, or on the web if the Store isn't there. */
fun openStoreListing(context: Context) {
    val id = context.packageName
    try {
        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    } catch (_: ActivityNotFoundException) {
        runCatching {
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://play.google.com/store/apps/details?id=$id")).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
    }
}
