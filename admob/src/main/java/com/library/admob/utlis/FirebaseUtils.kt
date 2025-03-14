package com.library.admob.utlis

import android.app.Activity
import com.google.android.gms.ads.MobileAds
import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.google.firebase.remoteconfig.FirebaseRemoteConfigSettings
import com.library.admob.ump.AdsConsentManager
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object FirebaseUtils {

    private suspend fun initRemoteConfig(remoteConfigDefaults: Int): Boolean {
        return suspendCancellableCoroutine { continuation ->
            FirebaseRemoteConfig.getInstance().reset()
            val configSettings = FirebaseRemoteConfigSettings.Builder()
                .setMinimumFetchIntervalInSeconds(3600)
                .build()

            FirebaseRemoteConfig.getInstance().setConfigSettingsAsync(configSettings)
            FirebaseRemoteConfig.getInstance().setDefaultsAsync(remoteConfigDefaults)
            FirebaseRemoteConfig.getInstance().fetchAndActivate().addOnCompleteListener { task ->
                continuation.resume(task.isSuccessful)
            }
        }
    }

    private suspend fun initUmp(context: Activity): Boolean {
        return suspendCancellableCoroutine { continuation ->
            MobileAds.initialize(context) {
                val reset = !AdsConsentManager.getConsentResult(context)
                val adsConsentManager = AdsConsentManager(context)
                adsConsentManager.requestUMP(
                    false,
                    "",
                    reset,
                    object : AdsConsentManager.UMPResultListener {
                        override fun onCheckUMPSuccess(result: Boolean) {
                            continuation.resume(result)
                        }
                    })
            }
        }
    }

    suspend fun initializeApp(activity: Activity, remoteConfigDefaults: Int) = coroutineScope {
        val remoteConfigDeferred = async { initRemoteConfig(remoteConfigDefaults) }
        val umpDeferred = async { initUmp(activity) }

        val remoteConfigSuccess = remoteConfigDeferred.await()
        umpDeferred.await()

        if (remoteConfigSuccess) {
            setRemoteConfigBoolean(activity)
        }
    }

    private fun setRemoteConfigBoolean(activity: Activity) {
        val listRemoteConfigBoolean = mutableListOf(
            Constant.REMOTE_CONFIG_BANNER,
            Constant.REMOTE_CONFIG_COLLAPSIBLE,
            Constant.REMOTE_CONFIG_NATIVE,
            Constant.REMOTE_CONFIG_INTER,
            Constant.REMOTE_CONFIG_APP_OPEN,
            Constant.REMOTE_CONFIG_REWARD,
        )
        listRemoteConfigBoolean.forEach { remoteConfig ->
            val value = getRemoteConfigBoolean(remoteConfig)
            Utils.setBoolean(activity, remoteConfig, value)
        }
    }

    private fun getRemoteConfigBoolean(adUnitId: String): Boolean {
        val mFirebaseRemoteConfig = FirebaseRemoteConfig.getInstance()
        return mFirebaseRemoteConfig.getBoolean(adUnitId)
    }
}
