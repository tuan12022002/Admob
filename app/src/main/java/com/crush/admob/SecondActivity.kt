package com.crush.admob

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.library.admob.activity.AdmobActivity
import com.library.admob.utlis.Constant

class SecondActivity : AdmobActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_second)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        findViewById<View>(R.id.tvTest).setOnClickListener {
            finish()
        }

        loadNative(
            idNativeAd = BuildConfig.NATIVE,
            nativeLayout = R.layout.ads_native_large,
            nativeShimmer = R.layout.ads_native_large_shimmer
        )
    }
}