package com.crush.admob

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.lifecycle.lifecycleScope
import com.library.admob.Admob
import com.library.admob.activity.AdmobActivity
import com.library.admob.utlis.FirebaseUtils
import kotlinx.coroutines.launch

class SplashActivity : AdmobActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_splash)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        Admob.disableAppResumeWithActivity(this::class.java)
        lifecycleScope.launch {
            FirebaseUtils.initializeApp(this@SplashActivity,  R.xml.remote_config_defaults)
            showInterstitialAd() {
                startActivity(Intent(this@SplashActivity, MainActivity::class.java))
            }
        }
    }

}