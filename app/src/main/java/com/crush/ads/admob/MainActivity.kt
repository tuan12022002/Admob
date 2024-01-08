package com.crush.ads.admob

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import com.crush.ads.admob.callback.InterCallback
import com.google.android.gms.ads.interstitial.InterstitialAd

class MainActivity : AppCompatActivity() {

    lateinit var listInterSplash : MutableList<String>

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
        listInterSplash.addAll(resources.getStringArray(R.array.splash).toMutableList())
        Admob.initAdmob(this)
        //Admob.getInstance().loadAndShowInterSplash(this@MainActivity, "ca-app-pub-3940256099942544/1033173712", 25000,interCallback)

        findViewById<TextView>(R.id.showAds).setOnClickListener {
            Admob.getInstance().loadAndShowInterSplash(this@MainActivity, listInterSplash, 25000, interCallback)
        }

        Admob.getInstance().loadBanner(this, resources.getStringArray(R.array.banner).toMutableList())

        var interstitialAd2:InterstitialAd?= null
        Admob.getInstance().loadInter(this, resources.getStringArray(R.array.banner).toMutableList(), object :
            InterCallback() {
            override fun onAdLoadSuccess(interstitialAd: InterstitialAd?) {
                super.onAdLoadSuccess(interstitialAd)
                interstitialAd2 = interstitialAd
            }
        })

        AppOpenManager.getInstance().initAppOpenManager(application, "ca-app-pub-3940256099942544/9257395921")

        AppOpenManager.getInstance().disableAppResumeWithActivity(MainActivity::class.java)
    }

    override fun onResume() {
        super.onResume()
        Admob.getInstance().failInterSplash(this, listInterSplash,25000 ,interCallback, 1000L)
    }
}