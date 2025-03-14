package com.library.admob.ump

import android.app.Activity
import android.content.Context
import android.util.Log
import com.google.android.ump.ConsentDebugSettings
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import java.util.concurrent.atomic.AtomicBoolean

class AdsConsentManager(private val activity: Activity) {
    private val TAG = "AdsConsentManager"
    private val auAtomicBoolean = AtomicBoolean(false)

    interface UMPResultListener {
        fun onCheckUMPSuccess(result: Boolean)
    }

    init {
        auAtomicBoolean.set(false)
    }

    fun requestUMP(umpResultListener: UMPResultListener) {
        requestUMP(false, "", false, umpResultListener)
    }

    companion object {
        fun getConsentResult(context: Context): Boolean {
            val consentResult = context.getSharedPreferences(context.packageName + "_preferences", 0)
                .getString("IABTCF_PurposeConsents", "")
            return consentResult?.isEmpty() == true || consentResult?.get(0).toString() == "1"
        }
    }

    fun requestUMP(enableDebug: Boolean, testDevice: String, resetData: Boolean, umpResultListener: UMPResultListener) {
        val debugSettings = ConsentDebugSettings.Builder(activity)
            .setDebugGeography(ConsentDebugSettings.DebugGeography.DEBUG_GEOGRAPHY_EEA)
            .addTestDeviceHashedId(testDevice)
            .build()

        val params = ConsentRequestParameters.Builder()
        params.setTagForUnderAgeOfConsent(false)
        if (enableDebug) {
            params.setConsentDebugSettings(debugSettings)
        }
        val consentRequestParameters = params.build()
        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        if (resetData) {
            consentInformation.reset()
        }

        val onConsentInfoUpdateSuccessListener = ConsentInformation.OnConsentInfoUpdateSuccessListener {
            UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { loadAndShowError ->
                if (loadAndShowError != null)
                    Log.e(TAG, "onConsentInfoUpdateSuccess: " + loadAndShowError.message)
                if (!auAtomicBoolean.getAndSet(true)) {
                    umpResultListener.onCheckUMPSuccess(getConsentResult(activity))
                }
            }
        }

        val onConsentInfoUpdateFailureListener =
            ConsentInformation.OnConsentInfoUpdateFailureListener { formError ->
                if (!auAtomicBoolean.getAndSet(true)) {
                    Log.e(TAG, "onConsentInfoUpdateFailure: " + formError.message)
                    umpResultListener.onCheckUMPSuccess(getConsentResult(activity))
                }
            }

        consentInformation.requestConsentInfoUpdate(
            activity,
            consentRequestParameters,
            onConsentInfoUpdateSuccessListener,
            onConsentInfoUpdateFailureListener
        )

        if (consentInformation.canRequestAds() && !auAtomicBoolean.getAndSet(true)) {
            umpResultListener.onCheckUMPSuccess(getConsentResult(activity))
            Log.d(TAG, "requestUMP: ")
        }
    }

    fun showPrivacyOption(activity: Activity, umpResultListener: UMPResultListener) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            Log.d(TAG, "showPrivacyOption: " + getConsentResult(activity))
            umpResultListener.onCheckUMPSuccess(getConsentResult(activity))
        }
    }
}
