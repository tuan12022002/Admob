package com.crush.ads.admob.dialog

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import com.crush.ads.admob.R

class LoadingAdsDialog(context: Context) : Dialog(context, R.style.LoadingAds) {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.dialog_loading_ads)
    }
}