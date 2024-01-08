package com.crush.ads.admob

import android.app.Activity
import android.app.Application
import android.os.Bundle
import android.util.Log
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import com.crush.ads.admob.dialog.LoadingAdsDialog
import com.crush.ads.admob.util.Util
import com.google.android.gms.ads.AdActivity
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.appopen.AppOpenAd.AppOpenAdLoadCallback
import kotlinx.coroutines.*
import java.lang.ref.WeakReference

@Suppress("DEPRECATION")
class AppOpenManager : Application.ActivityLifecycleCallbacks, LifecycleObserver {

    private var application: Application? = null
    private var id: String = ""

    private var activity: WeakReference<Activity>? = null


    private var loadCallback: AppOpenAdLoadCallback? = null

    private var appResumeAd: AppOpenAd? = null
    var isUserDismissAppOpen = true
    var isUserClickAds = false

    private var dialogLoadingAds: LoadingAdsDialog? = null

    private var adsJobMainAppOpen: Job? = null
    private val adsCoroutineScopeMainAppOpen = CoroutineScope(Dispatchers.Main)

    private val listActivityOffAdResume: MutableList<Class<*>> = mutableListOf()
    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        listActivityOffAdResume.add(activityClass)
    }

    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        listActivityOffAdResume.remove(activityClass)
    }

    companion object {
        private var INSTANCE: AppOpenManager? = null

        private var checkLoadResume = false


        fun getInstance(): AppOpenManager {
            if (INSTANCE == null) {
                INSTANCE = AppOpenManager()
            }
            return INSTANCE as AppOpenManager
        }
    }

    fun initAppOpenManager(application: Application, id: String) {
        this.application = application
        this.id = id
        this.application?.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
    }


    override fun onActivityCreated(activity: Activity, bundle: Bundle?) {

    }

    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    override fun onActivityResumed(activity: Activity) {
        this.activity = WeakReference(activity)
        if (activity::class.java.name != AdActivity::class.java.name) {
            Log.e("kh45", "Show")
            loadAdsAppOpenManager()
        }
    }

    override fun onActivityPaused(activity: Activity) {

    }

    override fun onActivityStopped(activity: Activity) {

    }

    override fun onActivitySaveInstanceState(activity: Activity, p1: Bundle) {

    }

    override fun onActivityDestroyed(activity: Activity) {
        this.activity = null
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onResume() {
        if(!checkShowAppOpen()){
            ads()
        }else{
            isUserClickAds = false
        }

    }

    private fun checkUserOnClickAds():Boolean = isUserClickAds

    private fun checkUserDismissAppOpen():Boolean{
        for (activityClass in listActivityOffAdResume) {
            activity?.get()?.let { activity ->
                if (activityClass.name ==activity.javaClass.name) {
                    return true
                }
            }

        }
        return false
    }

    private fun checkShowAppOpen():Boolean{
        return checkUserDismissAppOpen() || checkUserOnClickAds() || !isUserDismissAppOpen
    }

    private fun loadAdsAppOpenManager() {
        if (!checkLoadResume) {
            checkLoadResume = true

            loadCallback = object : AppOpenAdLoadCallback() {
                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    super.onAdLoaded(appOpenAd)
                    getInstance().appResumeAd = appOpenAd
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    super.onAdFailedToLoad(loadAdError)
                    checkLoadResume = false
                    dismiss()
                }
            }

            application?.let { application ->
                loadCallback?.let { loadCallback ->
                    AppOpenAd.load(application, id, Util.getAdRequest(), AppOpenAd.APP_OPEN_AD_ORIENTATION_PORTRAIT, loadCallback)
                }

            }


        }
    }

    fun ads(){
        activity.let {
            appResumeAd?.let { appResumeAd ->
                if (ProcessLifecycleOwner.get().lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED)) {
                    show()
                    appResumeAd.fullScreenContentCallback = object :FullScreenContentCallback(){
                        override fun onAdDismissedFullScreenContent() {
                            super.onAdDismissedFullScreenContent()

                            getInstance().appResumeAd = null
                            checkLoadResume = false
                            loadAdsAppOpenManager()
                            dismiss()
                        }

                        override fun onAdShowedFullScreenContent() {
                            super.onAdShowedFullScreenContent()
                            getInstance().appResumeAd = null
                            checkLoadResume = false
                        }

                        override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                            super.onAdFailedToShowFullScreenContent(adError)
                            getInstance().appResumeAd = null
                            checkLoadResume = false
                            loadAdsAppOpenManager()
                            dismiss()
                        }
                    }
                    activity?.get()?.let { activity ->
                        adsJobMainAppOpen = adsCoroutineScopeMainAppOpen.launch {
                            delay(500)
                            appResumeAd.show(activity)
                        }

                    }

                }
            }
        }
    }

    private fun show(){
        dismiss()
        activity?.get()?.let { activity ->
            dialogLoadingAds = LoadingAdsDialog(activity)
            dialogLoadingAds?.show()
        }

    }

    fun dismiss() {
        dialogLoadingAds?.dismiss()
        dialogLoadingAds?.cancel()
    }

}