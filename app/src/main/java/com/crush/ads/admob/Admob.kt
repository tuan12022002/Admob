package com.crush.ads.admob

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.res.Resources
import android.os.Build
import android.util.DisplayMetrics
import android.util.Log
import android.view.Display
import android.view.View
import android.webkit.WebView
import android.widget.FrameLayout
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ProcessLifecycleOwner
import com.crush.ads.admob.callback.InterCallback
import com.crush.ads.admob.dialog.LoadingAdsDialog
import com.crush.ads.admob.util.Util
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.android.gms.ads.*
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
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
            interCallback?.onNextAction()
        }else{

            interstitialAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    super.onAdDismissedFullScreenContent()
                }

                override fun onAdShowedFullScreenContent() {
                    super.onAdShowedFullScreenContent()
                }

                override fun onAdFailedToShowFullScreenContent(p0: AdError) {
                    super.onAdFailedToShowFullScreenContent(p0)
                }

                override fun onAdClicked() {
                    super.onAdClicked()
                    AppOpenManager.getInstance().isUserClickAds = true
                }
            }
        }
    }
}
