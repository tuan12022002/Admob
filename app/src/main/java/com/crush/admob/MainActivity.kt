package com.crush.admob

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.crush.admob.databinding.ActivityMainBinding
import com.google.android.gms.ads.MobileAds
import com.library.admob.activity.AdmobActivity
import com.library.admob.utlis.Constant

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
            showInterstitialAd(idInterstitialAd = Constant.ID_INTERSTITIAL_AD) {
                startActivity(Intent(this, SecondActivity::class.java))
            }
        }

        binding?.btnAppOpen?.setOnClickListener {
            showAppOpenAd(idAppOpenAd = Constant.ID_APP_OPEN_AD) {
                startActivity(Intent(this, SecondActivity::class.java))
            }
        }

        binding?.btnReward?.setOnClickListener {
            showRewardedAd(idRewardAd = Constant.ID_REWARD_AD) {
                startActivity(Intent(this, SecondActivity::class.java))
            }
        }



        loadBanner()
    }

    override fun onBackPressedCallback() {
        super.onBackPressedCallback()
        finish()
    }

}