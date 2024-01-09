package com.crush.ads.admob.callback

import com.google.android.gms.ads.nativead.NativeAd

open class NativeCallback {
    open fun onNativeAdLoaded(nativeAd: NativeAd?) {}
    open fun onAdFailedToLoad() {}
    open fun onEarnRevenue(Revenue: Double?) {}
    open fun onAdClicked() {}
}