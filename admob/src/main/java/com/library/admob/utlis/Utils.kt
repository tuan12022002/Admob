package com.library.admob.utlis

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkInfo
import com.library.admob.ump.AdsConsentManager

object Utils {
    private fun haveNetworkConnection(context: Context): Boolean {
        try {
            var haveConnectedWifi = false
            var haveConnectedMobile = false

            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val netInfo: Array<NetworkInfo> = cm.allNetworkInfo
            for (ni in netInfo) {
                if (ni.typeName.equals("WIFI", ignoreCase = true))
                    if (ni.isConnected)
                        haveConnectedWifi = true
                if (ni.typeName.equals("MOBILE", ignoreCase = true))
                    if (ni.isConnected)
                        haveConnectedMobile = true
            }
            return haveConnectedWifi || haveConnectedMobile
        } catch (e: Exception) {
            e.printStackTrace()
            return true
        }

    }

    fun isReadyLoadAndShow(context: Context, remoteConfigKey: String): Boolean {
        return AdsConsentManager.getConsentResult(context)
                && haveNetworkConnection(context)
                && getBoolean(context, remoteConfigKey)
    }

    fun getBoolean(context: Context, remoteConfigKey: String): Boolean {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(Constant.REMOTE_CONFIG, Context.MODE_PRIVATE)
        return sharedPreferences.getBoolean(remoteConfigKey, true)
    }

    fun setBoolean(context: Context, remoteConfigKey: String, value: Boolean) {
        val sharedPreferences: SharedPreferences =
            context.getSharedPreferences(Constant.REMOTE_CONFIG, Context.MODE_PRIVATE)
        sharedPreferences.edit().putBoolean(remoteConfigKey, value).apply()
    }

}