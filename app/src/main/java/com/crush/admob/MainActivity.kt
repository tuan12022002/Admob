package com.crush.admob

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.android.gms.ads.MobileAds
import com.library.admob.Admob
import com.library.admob.activity.AdmobActivity
import com.library.admob.utlis.Constant

class MainActivity : AdmobActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_main)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        MobileAds.initialize(this)
        findViewById<View>(R.id.tvTest).setOnClickListener {
            showInterstitialAd(isShowAds = true){
                startActivity(Intent(this, SecondActivity::class.java))
                //finish()
            }
        }

        Admob.disableAppResumeWithActivity(this::class.java)
        loadNative(
            idNativeAd = Constant.ID_NATIVE_AD,
            nativeLayout = R.layout.ads_native_large,
            nativeShimmer = R.layout.ads_native_large_shimmer
        )
    }

}