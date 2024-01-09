package com.crush.ads.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.crush.ads.admob.callback.InterCallback
import com.crush.ads.admob.callback.NativeCallback
import com.crush.ads.admob.callback.RewardCallback
import com.crush.ads.admob.dialog.LoadingAdsDialog
import com.crush.ads.admob.util.Util
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.formats.NativeAdOptions
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class Admob private constructor() {

    private var context: WeakReference<Context>? = null
    private var dialogLoadingAds: LoadingAdsDialog? = null

    companion object {
        private var INSTANCE: Admob? = null

        fun getInstance(): Admob {
            if (INSTANCE == null) {
                INSTANCE = Admob()
            }
            return INSTANCE as Admob
        }

        fun initAdmob(context: Context) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                val processName = Application.getProcessName()
                val packageName = context.packageName
                if (packageName != processName) {
                    WebView.setDataDirectorySuffix(processName)
                }
            }
            MobileAds.initialize(context) {
                Log.e("Admob", "Admob ready!")
            }

            this.INSTANCE?.context = WeakReference(context)
        }
    }



    private fun show(context: Context) {
        dialogLoadingAds = LoadingAdsDialog(context)
        dialogLoadingAds?.show()
    }

    private fun dismiss() {
        dialogLoadingAds?.dismiss()
        dialogLoadingAds?.cancel()
    }

    // ---------------------------------------- Start load and show inter splash ----------------------------------------
    private var interstitialAdSplash: InterstitialAd? = null
    private var isTimeoutSplash = false
    private var isShowInterSplash = false
    private var adsJobTimeOutSplash: Job? = null
    private val adsCoroutineScopeTimeOutIOSplash = CoroutineScope(Dispatchers.IO)
    private var adsJobMainSplash: Job? = null
    private val adsCoroutineScopeMainSplash = CoroutineScope(Dispatchers.Main)
    private var adsJobShowFailSplash: Job? = null
    private val adsCoroutineScopeShowFailSplash = CoroutineScope(Dispatchers.Main)
    fun loadAndShowInterSplash(
        context: Context,
        listIdSplash: MutableList<String>,
        timeOut: Long,
        interCallback: InterCallback?
    ) {
        isTimeoutSplash = false
        AppOpenManager.getInstance().isUserDismissAppOpen = false
        if (!Util.isNetworkAvailable(context) || listIdSplash.size == 0) {
            interCallback?.onAdClosed()
            interCallback?.onNextAction()
            AppOpenManager.getInstance().isUserDismissAppOpen = true
        } else {
            if (timeOut > 0) {
                adsJobTimeOutSplash = adsCoroutineScopeTimeOutIOSplash.launch {
                    jobTimeOutSplash(context, timeOut, interCallback, listIdSplash)
                }
            }

            loadInterSplash(context, listIdSplash, object : InterCallback() {
                override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
                    super.onAdLoadSuccess(interstitialAd)
                    if (!isTimeoutSplash) {
                        interstitialAdSplash = interstitialAd
                        showInterSplash(context, interCallback, listIdSplash, timeOut)
                    }
                }

                override fun onAdFailedToLoad(i: LoadAdError?) {
                    super.onAdFailedToLoad(i)
                    if (!isTimeoutSplash) {
                        interCallback?.let {
                            adsJobTimeOutSplash?.cancel()
                            interCallback.onAdFailedToLoad(i)
                            interCallback.onNextAction()
                        }
                    }
                }
            })

        }
    }

    private fun loadInterSplash(context: Context, listIdSplash: MutableList<String>, interCallback: InterCallback?) {
        InterstitialAd.load(context, listIdSplash[0], Util.getAdRequest(), object : InterstitialAdLoadCallback() {
            override fun onAdLoaded(interstitialAd: InterstitialAd) {
                super.onAdLoaded(interstitialAd)
                interCallback?.onAdLoadSuccess(interstitialAd)
                //tracking adjust
                interstitialAd.onPaidEventListener =
                    OnPaidEventListener { adValue: AdValue ->
                        interCallback?.onEarnRevenue(adValue.valueMicros.toDouble())
                    }
            }

            override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                super.onAdFailedToLoad(loadAdError)
                listIdSplash.removeAt(0)
                if(listIdSplash.size == 0){
                    interCallback?.onAdFailedToLoad(loadAdError)
                }else{
                    loadInterSplash(context, listIdSplash, interCallback)
                }
            }
        })
    }

    private fun splashStart(interCallback: InterCallback?){
        interCallback?.onAdClosed()
        interCallback?.onNextAction()
    }

    private fun splashSuccess(interCallback: InterCallback?){
        splashStart(interCallback)
        AppOpenManager.getInstance().isUserDismissAppOpen = true
        dismiss()
    }

    private fun showInterSplash(
        context: Context,
        interCallback: InterCallback?,
        listIdSplash: MutableList<String>,
        timeOut: Long
    ) {
        if (interstitialAdSplash == null) {
            splashStart(interCallback)
        } else {
            adsJobTimeOutSplash?.cancel()
            interstitialAdSplash?.setOnPaidEventListener { adValue ->
                interCallback?.onEarnRevenue(adValue.valueMicros.toDouble())
            }

            interstitialAdSplash?.fullScreenContentCallback = object : FullScreenContentCallback() {

                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    splashSuccess(interCallback)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)

                    when (adError.code) {
                        3, 0 -> {
                            isShowInterSplash = true
                            return
                        }
                        1 -> {
                            adsJobTimeOutSplash?.cancel()
                            getInstance().loadAndShowInterSplash(
                                context,
                                listIdSplash,
                                timeOut,
                                interCallback
                            )
                        }
                        else -> {
                            splashSuccess(interCallback)
                        }
                    }
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AppOpenManager.getInstance().isUserClickAds = true
                }

            }

            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                adsJobMainSplash?.cancel()
                adsJobMainSplash = adsCoroutineScopeMainSplash.launch {
                    if (dialogLoadingAds?.isShowing != true) {
                        dismiss()
                        show(context)
                    }
                    delay(500)
                    isShowInterSplash = false
                    interstitialAdSplash?.show(context as Activity)
                }
            } else {
                isShowInterSplash = true
            }

        }
    }

    private suspend fun jobTimeOutSplash(
        context: Context,
        timeOut: Long,
        interCallback: InterCallback?,
        listIdSplash: MutableList<String>
    ) {
        delay(timeOut)
        isTimeoutSplash = true
        if (interstitialAdSplash != null) {
            showInterSplash(context, interCallback,listIdSplash, timeOut)
            return
        }
        splashStart(interCallback)
        AppOpenManager.getInstance().isUserDismissAppOpen = true
    }

    fun failInterSplash(
        context: Context,
        listIdSplash: MutableList<String>,
        timeOut: Long,
        interCallback: InterCallback?,
        timeDelay: Long
    ) {
        adsJobShowFailSplash?.cancel()
        if (Util.isNetworkAvailable(context)) {
            adsJobShowFailSplash = adsCoroutineScopeShowFailSplash.launch {
                delay(timeDelay)
                if (interstitialAdSplash != null && isShowInterSplash) {
                    showInterSplash(context, interCallback, listIdSplash, timeOut)
                }
            }
        }
    }
    // ---------------------------------------- End load and show inter splash --------------------------------------------

    // ---------------------------------------- Start load and show banner ----------------------------------------

    private fun getAdSize(activity: Activity): AdSize {
        val display: Display = activity.windowManager.defaultDisplay
        val outMetrics = DisplayMetrics()
        display.getMetrics(outMetrics)

        val widthPixels: Float = outMetrics.widthPixels.toFloat()
        val density: Float = outMetrics.density

        val adWidth: Int = (widthPixels / density).toInt()
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    fun loadBanner(activity: Activity, listIdBanner: MutableList<String>) {
        try {
            val bannerContainer: FrameLayout = activity.findViewById(R.id.bannerContainer)
            val bannerContainerShimmer: ShimmerFrameLayout = activity.findViewById(R.id.bannerContainerShimmer)

            if (listIdBanner.size == 0 || !Util.isNetworkAvailable(activity)) {
                bannerContainerShimmer.stopShimmer()
                bannerContainer.visibility = View.GONE
                bannerContainerShimmer.visibility = View.GONE
            } else {
                bannerContainer.visibility = View.VISIBLE
                bannerContainerShimmer.startShimmer()
                val adView = AdView(activity)
                adView.adUnitId = listIdBanner[0]
                bannerContainer.addView(adView)
                val adSize: AdSize = getAdSize(activity)
                val adHeight = adSize.height
                bannerContainerShimmer.layoutParams.height = (adHeight * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
                adView.setAdSize(adSize)
                adView.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

                adView.adListener = object : AdListener() {
                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        listIdBanner.removeAt(0)
                        if (listIdBanner.size > 0) {
                            loadBanner(activity, listIdBanner)
                        } else {
                            bannerContainerShimmer.stopShimmer()
                            bannerContainer.visibility = View.GONE
                            bannerContainerShimmer.visibility = View.GONE
                        }
                    }

                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        bannerContainerShimmer.stopShimmer()
                        bannerContainerShimmer.visibility = View.GONE

                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        AppOpenManager.getInstance().isUserClickAds = true
                    }
                }
                adView.loadAd(Util.getAdRequest())
            }
        } catch (e: Exception) {
            Log.e("Admob", "Load banner: ${e.message}")
        }
    }
    // ---------------------------------------- End load and show banner --------------------------------------------

    // ---------------------------------------- Start load and show inter ----------------------------------------

    var isOpenActivityAfterShowInterAds = true

    fun loadInter(context: Context, listIdInter: MutableList<String>, interCallback: InterCallback?){
        if (listIdInter.size == 0 || !Util.isNetworkAvailable(context)) {
            interCallback?.onAdFailedToLoad(null)
            interCallback?.onNextAction()
        }else{
            InterstitialAd.load(context, listIdInter[0], Util.getAdRequest(), object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    super.onAdLoaded(interstitialAd)
                    interCallback?.onAdLoadSuccess(interstitialAd)
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    listIdInter.removeAt(0)
                    if (listIdInter.size > 0) {
                        loadInter(context, listIdInter, interCallback)
                    } else {
                        interCallback?.onAdFailedToLoad(loadAdError)
                        interCallback?.onNextAction()
                    }
                }
            })
        }
    }

    fun showInter(context: Context, interstitialAd: InterstitialAd?, interCallback: InterCallback?){
        if(interstitialAd == null){
            interCallback?.onAdClosed()
            interCallback?.onNextAction()
        }else{

            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    if(!isOpenActivityAfterShowInterAds){
                        interCallback?.onAdClosed()
                        interCallback?.onNextAction()
                    }
                    dismiss()
                    AppOpenManager.getInstance().isUserDismissAppOpen = true
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    if(!isOpenActivityAfterShowInterAds){
                        interCallback?.onAdClosed()
                        interCallback?.onNextAction()
                    }
                    AppOpenManager.getInstance().isUserDismissAppOpen = true
                    dismiss()
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AppOpenManager.getInstance().isUserClickAds = true
                }
            }

            if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {

                AppOpenManager.getInstance().isUserDismissAppOpen = false
                if (dialogLoadingAds?.isShowing != true) {
                    dismiss()
                    show(context)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    if(isOpenActivityAfterShowInterAds){
                        interCallback?.onAdClosed()
                        interCallback?.onNextAction()
                    }

                    Handler(Looper.getMainLooper()).postDelayed({
                        dismiss()
                    }, 500)

                    interstitialAd.show(context as Activity)
                }, 500)
            } else {
                interCallback?.onAdClosed()
                interCallback?.onNextAction()
                dismiss()
            }

        }
    }
    // ---------------------------------------- End load and show inter ----------------------------------------

    // ---------------------------------------- Start load and show reward ----------------------------------------

    private var rewardedAd: RewardedAd? = null
    fun loadReward(context: Context, listIdReward:MutableList<String>){
        if (listIdReward.size == 0 || !Util.isNetworkAvailable(context)) {
            rewardedAd = null
        }else{
            RewardedAd.load(context, listIdReward[0], Util.getAdRequest(), object : RewardedAdLoadCallback() {
                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    super.onAdLoaded(rewardedAd)
                    getInstance().rewardedAd = rewardedAd
                    getInstance().rewardedAd?.setOnPaidEventListener {  }
                }

                override fun onAdFailedToLoad(p0: LoadAdError) {
                    super.onAdFailedToLoad(p0)
                    listIdReward.removeAt(0)
                    if (listIdReward.size > 0) {
                        loadReward(context, listIdReward)
                    }else{
                        rewardedAd = null
                    }
                }
            })
        }
    }

    fun showReward(context: Context, listIdReward:MutableList<String>,rewardCallback: RewardCallback?){
        if (!Util.isNetworkAvailable(context)) {
            rewardCallback?.onAdClosed()
        }else{
            rewardedAd?.fullScreenContentCallback = object :FullScreenContentCallback(){
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                    rewardCallback?.onAdClosed()
                    AppOpenManager.getInstance().isUserDismissAppOpen = true
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                    AppOpenManager.getInstance().isUserDismissAppOpen = false
                    rewardedAd = null
                    loadReward(context, listIdReward)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    super.onAdFailedToShowFullScreenContent(adError)
                    rewardCallback?.onAdFailedToShow(adError.code)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AppOpenManager.getInstance().isUserClickAds = true
                }
            }

            rewardedAd?.show(context as Activity) { rewardItem ->
                rewardCallback?.onEarnedReward(rewardItem)
            }
        }
    }
    // ---------------------------------------- End load and show reward ----------------------------------------

    // ---------------------------------------- Start load and show native ----------------------------------------

    fun loadNative(context: Context, listIdNative:MutableList<String>, nativeCallback: NativeCallback?){
        if (listIdNative.size == 0 || !Util.isNetworkAvailable(context)) {
            nativeCallback?.onAdFailedToLoad()
        }else{
            val videoOptions = VideoOptions.Builder()
                .setStartMuted(true)
                .build()

            val adOptions = NativeAdOptions.Builder()
                .setVideoOptions(videoOptions)
                .build()

            val adLoader = AdLoader.Builder(context, listIdNative[0])
                .forNativeAd { nativeAd ->
                    nativeCallback?.onNativeAdLoaded(nativeAd)
                    nativeAd.setOnPaidEventListener { adValue ->
                        nativeCallback?.onEarnRevenue(adValue.valueMicros.toDouble())
                    }
                }
                .withAdListener(object : AdListener() {
                    override fun onAdFailedToLoad(error: LoadAdError) {
                        listIdNative.removeAt(0)
                        if (listIdNative.size > 0) {
                            loadNative(context, listIdNative, nativeCallback)
                        }else{
                            nativeCallback?.onAdFailedToLoad()
                        }
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        nativeCallback?.onAdClicked()
                    }
                })
                .withNativeAdOptions(adOptions)
                .build()
            adLoader.loadAd(Util.getAdRequest())
        }
    }

    // ---------------------------------------- End load and show native ----------------------------------------

}
