package com.cartoniq.voxmorph.ads

import android.app.Activity
import android.app.Application
import android.content.Context
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** UMP consent + Mobile Ads. Nothing here may block startup or offline features. TEST IDs only. */
class AdManager(private val app: Application) {
    enum class Availability { OK, NOT_READY, NO_CONSENT }

    private val _canRequestAds = MutableStateFlow(false)
    val canRequestAds: StateFlow<Boolean> = _canRequestAds
    private val _privacyOptionsRequired = MutableStateFlow(false)
    val privacyOptionsRequired: StateFlow<Boolean> = _privacyOptionsRequired

    private var initialized = false
    private var rewarded: RewardedAd? = null
    private var loading = false

    /** Safe to call on every launch; shows the consent form only when UMP says it is required. */
    fun gatherConsent(activity: Activity) {
        val info = UserMessagingPlatform.getConsentInformation(activity)
        if (info.canRequestAds()) startAds()
        info.requestConsentInfoUpdate(
            activity, ConsentRequestParameters.Builder().build(),
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) {
                    refresh(activity)
                }
            },
            { refresh(activity) }
        )
    }

    private fun refresh(context: Context) {
        val info = UserMessagingPlatform.getConsentInformation(context)
        _privacyOptionsRequired.value =
            info.privacyOptionsRequirementStatus == ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED
        if (info.canRequestAds()) startAds()
    }

    fun showPrivacyOptions(activity: Activity) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { refresh(activity) }
    }

    private fun startAds() {
        if (initialized) return
        initialized = true
        _canRequestAds.value = true
        CoroutineScope(Dispatchers.IO).launch { MobileAds.initialize(app) {} }
        loadRewarded()
    }

    fun loadRewarded() {
        if (!initialized || loading || rewarded != null) return
        loading = true
        RewardedAd.load(app, REWARDED_ID, AdRequest.Builder().build(), object : RewardedAdLoadCallback() {
            override fun onAdLoaded(ad: RewardedAd) { rewarded = ad; loading = false }
            override fun onAdFailedToLoad(error: LoadAdError) { rewarded = null; loading = false }
        })
    }

    /**
     * [onEarned] fires ONLY from the SDK's user-earned-reward callback. Load, show, dismiss and failure never grant.
     * [onClosedWithoutReward] fires when the ad closes (or fails to show) without a reward.
     */
    fun showRewarded(activity: Activity, onEarned: () -> Unit, onClosedWithoutReward: () -> Unit): Availability {
        if (!initialized) return Availability.NO_CONSENT
        val ad = rewarded
        if (ad == null) { loadRewarded(); return Availability.NOT_READY }
        var earned = false
        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                rewarded = null
                loadRewarded()
                if (!earned) onClosedWithoutReward()
            }
            override fun onAdFailedToShowFullScreenContent(error: AdError) {
                rewarded = null
                loadRewarded()
                onClosedWithoutReward()
            }
        }
        ad.show(activity) { earned = true; onEarned() }
        return Availability.OK
    }

    companion object {
        // Google sample/test IDs. Replace with production IDs before release.
        const val BANNER_ID = "ca-app-pub-9771662959477951/5924228096"
        const val REWARDED_ID = "ca-app-pub-9771662959477951/3106493064"
    }
}
