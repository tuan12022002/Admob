package com.crush.admob

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.ActivityResult
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.crush.admob.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds
import com.library.admob.activity.AdmobActivity

class MainActivity : AdmobActivity() {
    private var binding: ActivityMainBinding? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding?.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        MobileAds.initialize(this)

        binding?.btnInter?.setOnClickListener {
            showInterstitialAd(idInterstitialAd = BuildConfig.INTER) {
                launcherActivity.launch(Intent(this, SecondActivity::class.java))
            }
        }

        binding?.btnAppOpen?.setOnClickListener {
            showAppOpenAd(idAppOpenAd = BuildConfig.APP_OPEN) {
                launcherActivity.launch(Intent(this, SecondActivity::class.java))
            }
        }

        binding?.btnReward?.setOnClickListener {
            showRewardedAd(idRewardAd = BuildConfig.REWARD) {
                launcherActivity.launch(Intent(this, SecondActivity::class.java))
            }
        }

        loadBannerOfCollapsible(idBannerAd = BuildConfig.COLLAPSIBLE, true)
    }

    override fun onActivityResult(activityResult: ActivityResult?) {
        super.onActivityResult(activityResult)
        loadBannerOfCollapsible(idBannerAd = BuildConfig.COLLAPSIBLE, true)
    }

    override fun onBackPressedCallback() {
        super.onBackPressedCallback()
        finish()
    }

}