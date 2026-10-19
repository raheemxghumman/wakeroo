package com.wakeroo.app.billing

import android.app.Activity
import android.content.Context
import com.google.android.gms.ads.MobileAds
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Consent first, then the ads SDK. Only ever started from the main app screen: the alarm,
 * missions and onboarding never load or show ads.
 */
object Ads {
    private val started = AtomicBoolean(false)
    private val _ready = MutableStateFlow(false)
    val ready: StateFlow<Boolean> = _ready

    // Google's sample IDs; replace with the real AdMob app and banner IDs before release.
    const val BANNER_UNIT_ID = "ca-app-pub-3940256099942544/9214589741"

    fun start(activity: Activity) {
        if (!started.compareAndSet(false, true)) return
        val consent = UserMessagingPlatform.getConsentInformation(activity)
        consent.requestConsentInfoUpdate(activity, ConsentRequestParameters.Builder().build(), {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { initIfAllowed(activity, consent) }
        }, { initIfAllowed(activity, consent) })
        initIfAllowed(activity, consent)
    }

    private fun initIfAllowed(context: Context, consent: ConsentInformation) {
        if (!consent.canRequestAds() || _ready.value) return
        Thread {
            MobileAds.initialize(context.applicationContext) { _ready.value = true }
        }.start()
    }

    fun privacyOptionsRequired(context: Context) =
        UserMessagingPlatform.getConsentInformation(context).privacyOptionsRequirementStatus ==
            ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

    fun showPrivacyOptions(activity: Activity) = UserMessagingPlatform.showPrivacyOptionsForm(activity) { }
}
