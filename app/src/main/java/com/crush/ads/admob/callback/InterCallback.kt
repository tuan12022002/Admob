package com.crush.ads.admob.callback

import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.interstitial.InterstitialAd

open class InterCallback {
    open fun onAdClosed() {}
    open fun onAdFailedToLoad(i: LoadAdError?) {}
    open fun onAdFailedToShow(adError: AdError?) {}
    open fun onAdLeftApplication() {}
    open fun onAdLoaded() {}
    open fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {}
    open fun onAdClicked() {}
    open fun onAdImpression() {}
    open fun onAdClosedByUser() {}
    open fun onNextAction() {}
    open fun onEarnRevenue(Revenue: Double?) {}
}