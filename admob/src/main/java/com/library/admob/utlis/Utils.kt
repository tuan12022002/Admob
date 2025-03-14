package com.library.admob.utlis

import android.content.Context
import android.content.SharedPreferences
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import com.library.admob.ump.AdsConsentManager

object Utils {
    private fun haveNetworkConnection(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false
        // Kiểm tra xem kết nối có phải là Wi-Fi, Cellular, Ethernet hay Bluetooth không
        return networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) ||
                networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_BLUETOOTH)
    }

    fun isReadyLoadAndShow(context: Context, remoteConfigKey: String): Boolean {
        return AdsConsentManager.getConsentResult(context)
                && haveNetworkConnection(context)
                && getBoolean(context, remoteConfigKey)
    }

    private fun getBoolean(context: Context, remoteConfigKey: String): Boolean {
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