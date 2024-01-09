package com.crush.ads.admob

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import android.widget.Toast
import com.crush.ads.admob.callback.InterCallback
import com.crush.ads.admob.callback.RewardCallback
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.rewarded.RewardItem

class MainActivity : AppCompatActivity() {

    lateinit var listInterSplash : MutableList<String>
    var interstitialAd2:InterstitialAd?= null
    private val interCallback =  object :InterCallback(){
        override fun onNextAction() {
            super.onNextAction()
            Log.e("kh45", "Start")
            startActivity(Intent(this@MainActivity, MainActivity2::class.java))
        }
    }
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        listInterSplash = mutableListOf()
        listInterSplash.addAll(resources.getStringArray(R.array.ads_splash).toMutableList())
        Admob.initAdmob(this)
        //Admob.getInstance().loadAndShowInterSplash(this@MainActivity, "ca-app-pub-3940256099942544/1033173712", 25000,interCallback)

        findViewById<TextView>(R.id.showAds).setOnClickListener {
            //Admob.getInstance().loadAndShowInterSplash(this@MainActivity, listInterSplash, 25000, interCallback)
//            Admob.getInstance().showInter(this@MainActivity,interstitialAd2, interCallback )
//            interstitialAd2 = null
//            loadInter()

            Admob.getInstance().showReward(this@MainActivity, resources.getStringArray(R.array.ads_rewarded).toMutableList(), object : RewardCallback() {
                override fun onAdClosed() {
                    super.onAdClosed()
                }

                override fun onEarnedReward(rewardItem: RewardItem?) {
                    super.onEarnedReward(rewardItem)
                }

                override fun onAdFailedToShow(codeError: Int) {
                    super.onAdFailedToShow(codeError)
                }
            })
        }

        Admob.getInstance().loadBanner(this, resources.getStringArray(R.array.ads_banner).toMutableList())

        AppOpenManager.getInstance().initAppOpenManager(application, "ca-app-pub-3940256099942544/9257395921")

        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
        loadInter()
        Admob.getInstance().loadReward(this,  resources.getStringArray(R.array.ads_rewarded).toMutableList())
    }

    override fun onResume() {
        super.onResume()
        Admob.getInstance().failInterSplash(this, listInterSplash,25000 ,interCallback, 1000L)
    }

    fun loadInter(){
        Admob.getInstance().loadInter(this, resources.getStringArray(R.array.ads_inter).toMutableList(), object :
            InterCallback() {
            override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
                super.onAdLoadSuccess(interstitialAd)
                interstitialAd2 = interstitialAd
                Toast.makeText(this@MainActivity, "Success", Toast.LENGTH_SHORT).show()
            }
        })
    }
}