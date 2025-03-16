package com.library.admob.activity

import android.app.Activity
import android.content.res.Resources
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.WindowInsets
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.activity.result.ActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.facebook.shimmer.ShimmerFrameLayout
import com.google.ads.mediation.admob.AdMobAdapter
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdLoader
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.VideoOptions
import com.google.android.gms.ads.appopen.AppOpenAd
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.nativead.NativeAd
import com.google.android.gms.ads.nativead.NativeAdOptions
import com.google.android.gms.ads.nativead.NativeAdView
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import com.google.android.material.card.MaterialCardView
import com.library.admob.Admob
import com.library.admob.R
import com.library.admob.type.AdmobType
import com.library.admob.utlis.Constant
import com.library.admob.utlis.Utils

/**
 * Lớp cơ sở cho hoạt động Admob, xử lý các thao tác tải và hiển thị quảng cáo của nhiều loại khác nhau.
 */
open class AdmobActivity : AppCompatActivity() {
    /**
     * Biến chứa quảng cáo InterstitialAd
     */
    @Volatile
    private var interstitialAd: InterstitialAd? = null

    /**
     * Biến chứa quảng cáo RewardedAd
     */
    @Volatile
    private var rewardedAd: RewardedAd? = null

    /**
     * Biến chứa quảng cáo AppOpenAd
     */
    @Volatile
    private var appOpenAd: AppOpenAd? = null

    /**
     * Xác định loại quảng cáo đang được yêu cầu hiển thị.
     */
    @Volatile
    private var admobType: AdmobType? = null

    /**
     * Cờ kiểm tra xem Activity có ở trạng thái onResume để cho phép hiển thị quảng cáo hay không.
     */
    @Volatile
    private var isAdReadyShow = false

    /**
     * Biến đếm số lần tải quảng cáo, nhằm giới hạn số lần thử tải.
     */
    @Volatile
    private var countLoadingAds = 0

    /**
     * View hiển thị loading trước khi quảng cáo được tải xong.
     */
    @Volatile
    private var loadingView: MaterialCardView? = null

    /**
     * Handler để điều khiển việc delay ẩn loading view.
     */
    @Volatile
    private var handleLoadingView: Handler? = null

    /**
     * Callback được gọi sau khi quảng cáo hiển thị xong hoặc khi có sự kiện cần thực hiện sau quảng cáo.
     */
    @Volatile
    private var callback: (() -> Unit)? = null

    /**
     * ID quảng cáo InterstitialAd.
     */
    @Volatile
    private var idInterstitialAd = Constant.ID_INTERSTITIAL_AD

    /**
     * ID quảng cáo RewardedAd.
     */
    @Volatile
    private var idRewardAd = Constant.ID_REWARD_AD

    /**
     * ID quảng cáo AppOpenAd.
     */
    @Volatile
    private var idAppOpenAd = Constant.ID_APP_OPEN_AD

    private var adView: AdView? = null

    val launcherActivity =
        registerForActivityResult(ActivityResultContracts.StartActivityForResult()) { activityResult ->
            onActivityResult(activityResult)
        }

    open fun onActivityResult(activityResult: ActivityResult?) {

    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        this.onBackPressedDispatcher.addCallback(onBackPressedCallback = object :
            OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                onBackPressedCallback()
            }
        })
    }

    open fun onBackPressedCallback() {

    }

    /**
     * Hàm onResume() của Activity.
     * Khi Activity trở lại, đánh dấu rằng quảng cáo đã sẵn sàng hiển thị và kiểm tra hiển thị quảng cáo nếu có.
     */
    override fun onResume() {
        super.onResume()
        isAdReadyShow = true
        showAdByFromBackground()
    }

    /**
     * Hàm onStop() của Activity.
     * Khi Activity chuyển sang trạng thái dừng, đánh dấu rằng không nên hiển thị quảng cáo.
     */
    override fun onStop() {
        super.onStop()
        isAdReadyShow = false
    }

    /**
     * Kiểm tra và hiển thị quảng cáo nếu có quảng cáo đang chờ được hiển thị khi người dùng quay lại app.
     * Dựa trên giá trị của admobType để gọi hàm hiển thị tương ứng.
     */
    private fun showAdByFromBackground() {
        when (admobType) {
            AdmobType.ADMOB_INTER -> {
                showInterstitialAd(callback)
            }

            AdmobType.ADMOB_REWARD -> {
                showRewardedAd(callback)
            }

            AdmobType.ADMOB_APP_OPEN -> {
                showAppOpenAd(callback)
            }

            null -> {}
        }
    }

    /**
     * Tải quảng cáo Interstitial.
     * Nếu tải thành công, kiểm tra điều kiện để hiển thị quảng cáo.
     * Nếu tải thất bại, gọi handleLoadAdError để xử lý và thử lại.
     *
     * @param callback Hành động sẽ được thực hiện sau khi quảng cáo hiển thị hoặc khi có lỗi.
     */
    @Synchronized
    private fun loadAdInterstitialAd(callback: (() -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            this,
            idInterstitialAd,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdFailedToLoad(adError: LoadAdError) {
                    interstitialAd = null
                    handleLoadAdError(callback)
                }

                override fun onAdLoaded(interstitialAd: InterstitialAd) {
                    this@AdmobActivity.interstitialAd = interstitialAd
                    if (isAdReadyShow) {
                        showInterstitialAd(callback)
                    }
                }
            })
    }

    /**
     * Hiển thị quảng cáo Interstitial nếu các điều kiện sau được đáp ứng:
     * - Ứng dụng đang ở trạng thái có thể hiển thị quảng cáo (isAdReadyShow = true)
     * - Quảng cáo đã được tải thành công (interstitialAd khác null)
     * - Không có quảng cáo nào đang hiển thị (Admob.getAdShowing() trả về false)
     *
     * Thiết lập các callback để xử lý các sự kiện của quảng cáo như: hiển thị, ấn tượng, click, đóng, lỗi hiển thị.
     *
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc khi có lỗi.
     */
    @Synchronized
    private fun showInterstitialAd(callback: (() -> Unit)? = null) {
        if (isAdReadyShow && interstitialAd != null && !Admob.getAdShowing()) {
            interstitialAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    onInterstitialAdShowedFullScreenContent()
                }

                override fun onAdDismissedFullScreenContent() {
                    onInterstitialAdDismissedFullScreenContent()
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    if (!Admob.getOpenActivityFirstToDisplayAd()) {
                        hiddenLoadingView()
                        callback?.invoke()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    onInterstitialAdFailedToShowFullScreenContent(adError)
                    if (!Admob.getOpenActivityFirstToDisplayAd()) {
                        hiddenLoadingView()
                        callback?.invoke()
                    }
                }

                override fun onAdImpression() {
                    onInterstitialAdImpression()
                }

                override fun onAdClicked() {
                    onInterstitialAdClicked()
                }
            }
            if (Admob.getOpenActivityFirstToDisplayAd()) {
                hiddenLoadingView()
                callback?.invoke()
            }
            Admob.setAdShowing(true)
            admobType = null
            interstitialAd?.show(this)
        }
    }

    /**
     * Tải quảng cáo Rewarded.
     * Nếu tải thành công, kiểm tra và hiển thị quảng cáo nếu ứng dụng đã sẵn sàng.
     * Nếu tải thất bại, gọi handleLoadAdError để xử lý lỗi.
     *
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc khi có lỗi.
     */
    @Synchronized
    fun loadRewardedAd(callback: (() -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        RewardedAd.load(
            this,
            idRewardAd,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    rewardedAd = null
                    handleLoadAdError(callback)
                }

                override fun onAdLoaded(rewardedAd: RewardedAd) {
                    this@AdmobActivity.rewardedAd = rewardedAd
                    if (isAdReadyShow) {
                        showRewardedAd(callback)
                    }
                }
            })
    }

    /**
     * Hiển thị quảng cáo Rewarded nếu các điều kiện cho phép:
     * - Ứng dụng đang sẵn sàng hiển thị (isAdReadyShow = true)
     * - Quảng cáo Rewarded đã được tải thành công (rewardedAd khác null)
     * - Không có quảng cáo nào đang được hiển thị (Admob.getAdShowing() = false)
     *
     * Thiết lập callback để xử lý các sự kiện khi quảng cáo được hiển thị, gặp lỗi hoặc bị đóng.
     *
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc khi có lỗi.
     */
    @Synchronized
    fun showRewardedAd(callback: (() -> Unit)? = null) {
        if (isAdReadyShow && rewardedAd != null && !Admob.getAdShowing()) {
            rewardedAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    onRewardedAdShowedFullScreenContent()
                }

                override fun onAdDismissedFullScreenContent() {
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    onRewardedAdDismissedFullScreenContent()
                    hiddenLoadingView(0)
                    callback?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    onRewardedAdFailedToShowFullScreenContent(adError)
                    hiddenLoadingView(0)
                    callback?.invoke()
                }

                override fun onAdImpression() {
                    onRewardedAdImpression()
                }

                override fun onAdClicked() {
                    onRewardedAdClicked()
                }
            }

            Admob.setAdShowing(true)
            admobType = null
            rewardedAd?.show(this) {
                // Callback khi người dùng nhận thưởng có thể được xử lý tại đây nếu cần.
            }
        }
    }

    /**
     * Tải quảng cáo App Open.
     * Nếu tải thành công và ứng dụng sẵn sàng hiển thị, tiến hành hiển thị quảng cáo.
     * Nếu tải thất bại, gọi handleLoadAdError để thử lại.
     *
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc gặp lỗi.
     */
    @Synchronized
    private fun loadAppOpenAd(callback: (() -> Unit)? = null) {
        val adRequest = AdRequest.Builder().build()
        AppOpenAd.load(
            application,
            idAppOpenAd,
            adRequest,
            object : AppOpenAd.AppOpenAdLoadCallback() {
                override fun onAdFailedToLoad(error: LoadAdError) {
                    appOpenAd = null
                    handleLoadAdError(callback)
                }

                override fun onAdLoaded(appOpenAd: AppOpenAd) {
                    this@AdmobActivity.appOpenAd = appOpenAd
                    if (isAdReadyShow) {
                        showAppOpenAd(callback)
                    }
                }
            })
    }

    /**
     * Hiển thị quảng cáo App Open nếu thỏa mãn các điều kiện:
     * - Ứng dụng sẵn sàng hiển thị quảng cáo (isAdReadyShow = true)
     * - Quảng cáo App Open đã được tải thành công (appOpenAd khác null)
     * - Không có quảng cáo nào đang hiển thị (Admob.getAdShowing() = false)
     *
     * Thiết lập các callback để xử lý các sự kiện của quảng cáo.
     *
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc gặp lỗi.
     */
    @Synchronized
    private fun showAppOpenAd(callback: (() -> Unit)? = null) {
        if (isAdReadyShow && appOpenAd != null && !Admob.getAdShowing()) {
            appOpenAd?.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdShowedFullScreenContent() {
                    onAppOpenAdShowedFullScreenContent()
                }

                override fun onAdDismissedFullScreenContent() {
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    onAppOpenAdDismissedFullScreenContent()
                    if (!Admob.getOpenActivityFirstToDisplayAd()) {
                        hiddenLoadingView()
                        callback?.invoke()
                    }
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Admob.setAdShowing(false)
                    Admob.setAdLoading(false)
                    onAppOpenAdFailedToShowFullScreenContent(adError)
                    if (!Admob.getOpenActivityFirstToDisplayAd()) {
                        hiddenLoadingView()
                        callback?.invoke()
                    }
                }

                override fun onAdImpression() {
                    onAppOpenAdImpression()
                }

                override fun onAdClicked() {
                    onAppOpenAdClicked()
                }
            }
            if (Admob.getOpenActivityFirstToDisplayAd()) {
                hiddenLoadingView()
                callback?.invoke()
            }
            Admob.setAdShowing(true)
            admobType = null
            appOpenAd?.show(this)
        }
    }

    /**
     * Xử lý lỗi khi tải quảng cáo:
     * - Tăng bộ đếm số lần tải quảng cáo.
     * - Nếu số lần tải vượt quá giới hạn, ẩn loading view và gọi callback.
     * - Nếu chưa vượt quá giới hạn, thử tải lại quảng cáo theo loại đã được yêu cầu.
     *
     * @param callback Hành động được thực hiện khi đã đạt giới hạn tải hoặc chuyển sang tải lại quảng cáo.
     */
    @Synchronized
    private fun handleLoadAdError(callback: (() -> Unit)? = null) {
        countLoadingAds++
        if (countLoadingAds >= Constant.MAX_LOADING_ADS) {
            if (!isFinishing && !isDestroyed) {
                hiddenLoadingView()
                callback?.invoke()
            }
            return
        }
        when (admobType) {
            AdmobType.ADMOB_INTER -> {
                loadAdInterstitialAd(callback)
            }

            AdmobType.ADMOB_REWARD -> {
                loadRewardedAd(callback)
            }

            AdmobType.ADMOB_APP_OPEN -> {
                loadAppOpenAd(callback)
            }

            else -> {
                callback?.invoke()
            }
        }
    }

    /**
     * Ẩn loading view sau một khoảng thời gian delay nhất định (mặc định 1 giây).
     * Hủy bỏ các callback cũ của handler và thiết lập một callback mới để ẩn loading view.
     *
     * @param time Thời gian delay (mili giây) trước khi ẩn loading view.
     */
    @Synchronized
    private fun hiddenLoadingView(time: Long = 1000) {
        handleLoadingView?.removeCallbacksAndMessages(null)
        handleLoadingView = Handler(Looper.getMainLooper())
        handleLoadingView?.postDelayed({
            loadingView?.visibility = View.GONE
        }, time)
    }

    /**
     * Hiển thị loading view.
     * Tìm kiếm view có ID R.id.loadingView, đặt nó hiển thị và đưa nó lên phía trên.
     */
    @Synchronized
    private fun showLoadingView() {
        loadingView = findViewById(R.id.loadingView)
        loadingView?.visibility = View.VISIBLE
        loadingView?.bringToFront()
    }

    /**
     * Hiển thị quảng cáo Interstitial cho người dùng.
     *
     * @param idInterstitialAd ID quảng cáo Interstitial.
     * @param isShowAds Cờ xác định có nên hiển thị quảng cáo hay không.
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc bị hủy.
     */
    @Synchronized
    open fun showInterstitialAd(
        idInterstitialAd: String = Constant.ID_INTERSTITIAL_AD,
        callback: (() -> Unit)? = null
    ) {
        if (Utils.isReadyLoadAndShow(this, Constant.REMOTE_CONFIG_INTER)) {
            Admob.setAdLoading(true)
            this.idInterstitialAd = idInterstitialAd
            interstitialAd = null
            this.callback = callback
            admobType = AdmobType.ADMOB_INTER
            showLoadingView()
            loadAdInterstitialAd(callback)
        } else {
            hiddenLoadingView()
            callback?.invoke()
        }
    }

    /**
     * Hiển thị quảng cáo Rewarded cho người dùng.
     *
     * @param idRewardAd ID quảng cáo Rewarded.
     * @param isShowAds Cờ xác định có nên hiển thị quảng cáo hay không.
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc bị hủy.
     */
    @Synchronized
    open fun showRewardedAd(
        idRewardAd: String = Constant.ID_REWARD_AD,
        callback: (() -> Unit)? = null
    ) {
        if (Utils.isReadyLoadAndShow(this, Constant.REMOTE_CONFIG_REWARD)) {
            Admob.setAdLoading(true)
            this.idRewardAd = idRewardAd
            rewardedAd = null
            this.callback = callback
            admobType = AdmobType.ADMOB_REWARD
            showLoadingView()
            loadRewardedAd(callback)
        } else {
            hiddenLoadingView()
            callback?.invoke()
        }
    }

    /**
     * Hiển thị quảng cáo App Open cho người dùng.
     *
     * @param idAppOpenAd ID quảng cáo App Open.
     * @param isShowAds Cờ xác định có nên hiển thị quảng cáo hay không.
     * @param callback Hành động được thực hiện sau khi quảng cáo hiển thị hoặc bị hủy.
     */
    @Synchronized
    open fun showAppOpenAd(
        idAppOpenAd: String = Constant.ID_APP_OPEN_AD,
        callback: (() -> Unit)? = null
    ) {
        if (Utils.isReadyLoadAndShow(this, Constant.REMOTE_CONFIG_APP_OPEN)) {
            Admob.setAdLoading(true)
            this.idAppOpenAd = idAppOpenAd
            appOpenAd = null
            this.callback = callback
            admobType = AdmobType.ADMOB_APP_OPEN
            showLoadingView()
            loadAppOpenAd(callback)
        } else {
            hiddenLoadingView()
            callback?.invoke()
        }
    }

    /**
     * Callback được gọi khi quảng cáo Interstitial hiển thị thành công.
     */
    open fun onInterstitialAdShowedFullScreenContent() {
        Log.d("Admob Activity", "InterstitialAd is shown.")
    }

    /**
     * Callback được gọi khi quảng cáo Interstitial bị đóng.
     */
    open fun onInterstitialAdDismissedFullScreenContent() {
        Log.d("Admob Activity", "InterstitialAd was dismissed.")
    }

    /**
     * Callback được gọi khi quảng cáo Interstitial gặp lỗi khi hiển thị.
     *
     * @param adError Thông tin lỗi của quảng cáo.
     */
    open fun onInterstitialAdFailedToShowFullScreenContent(adError: AdError) {
        Log.e("Admob Activity", "InterstitialAd failed to show: ${adError.message}")
    }

    /**
     * Callback được gọi khi quảng cáo Interstitial ghi nhận ấn tượng.
     */
    open fun onInterstitialAdImpression() {
        Log.d("Admob Activity", "InterstitialAd impression recorded.")
    }

    /**
     * Callback được gọi khi quảng cáo Interstitial được click.
     */
    open fun onInterstitialAdClicked() {
        Log.d("Admob Activity", "InterstitialAd clicked.")
    }

    /**
     * Callback được gọi khi quảng cáo Rewarded hiển thị thành công.
     */
    open fun onRewardedAdShowedFullScreenContent() {
        Log.d("Admob Activity", "RewardedAd is shown.")
    }

    /**
     * Callback được gọi khi quảng cáo Rewarded bị đóng.
     */
    open fun onRewardedAdDismissedFullScreenContent() {
        Log.d("Admob Activity", "RewardedAd was dismissed.")
    }

    /**
     * Callback được gọi khi quảng cáo Rewarded gặp lỗi khi hiển thị.
     *
     * @param adError Thông tin lỗi của quảng cáo.
     */
    open fun onRewardedAdFailedToShowFullScreenContent(adError: AdError) {
        Log.e("Admob Activity", "RewardedAd failed to show: ${adError.message}")
    }

    /**
     * Callback được gọi khi quảng cáo Rewarded ghi nhận ấn tượng.
     */
    open fun onRewardedAdImpression() {
        Log.d("Admob Activity", "RewardedAd impression recorded.")
    }

    /**
     * Callback được gọi khi quảng cáo Rewarded được click.
     */
    open fun onRewardedAdClicked() {
        Log.d("Admob Activity", "RewardedAd clicked.")
    }

    /**
     * Callback được gọi khi quảng cáo App Open hiển thị thành công.
     */
    open fun onAppOpenAdShowedFullScreenContent() {
        Log.d("Admob Activity", "AppOpenAd is shown.")
    }

    /**
     * Callback được gọi khi quảng cáo App Open bị đóng.
     */
    open fun onAppOpenAdDismissedFullScreenContent() {
        Log.d("Admob Activity", "AppOpenAd was dismissed.")
    }

    /**
     * Callback được gọi khi quảng cáo App Open gặp lỗi khi hiển thị.
     *
     * @param adError Thông tin lỗi của quảng cáo.
     */
    open fun onAppOpenAdFailedToShowFullScreenContent(adError: AdError) {
        Log.e("Admob Activity", "AppOpenAd failed to show: ${adError.message}")
    }

    /**
     * Callback được gọi khi quảng cáo App Open ghi nhận ấn tượng.
     */
    open fun onAppOpenAdImpression() {
        Log.d("Admob Activity", "AppOpenAd impression recorded.")
    }

    /**
     * Callback được gọi khi quảng cáo App Open được click.
     */
    open fun onAppOpenAdClicked() {
        Log.d("Admob Activity", "AppOpenAd clicked.")
    }

    /**
     * Giải phóng tài nguyên khi Activity bị hủy.
     */
    override fun onDestroy() {
        handleLoadingView?.removeCallbacksAndMessages(null)
        loadingView = null
        interstitialAd = null
        handleLoadingView = null
        adView?.destroy()
        adView = null
        super.onDestroy()
    }


    /**
     * Callback cho Banner Ad
     */
    open fun onBannerAdLoaded() {
        // Xử lý khi banner ad được load thành công
    }

    open fun onBannerAdFailedToLoad(adError: LoadAdError) {
        // Xử lý khi banner ad load thất bại, ví dụ: log lỗi hoặc hiển thị thông báo
    }

    open fun onBannerAdClicked() {
        // Xử lý khi người dùng click vào banner ad
    }

    open fun onBannerAdOpened() {
        // Xử lý khi banner ad được mở rộng
    }

    open fun onBannerAdImpression() {
        // Xử lý khi banner ad ghi nhận 1 lượt hiển thị (impression)
    }

    open fun onBannerAdClosed() {
        // Xử lý khi banner ad bị đóng
    }

    /**
     * Tính toán kích thước quảng cáo theo chiều rộng màn hình (adaptive banner).
     * Dùng cho Android 11 trở lên và các phiên bản cũ hơn.
     */
    @Suppress("DEPRECATION")
    private fun getAdSize(activity: Activity): AdSize {
        val adWidth: Int = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            // Dành cho Android 11 (API 30) trở lên
            val windowMetrics = activity.windowManager.currentWindowMetrics
            // Lấy các insets của hệ thống (status bar, navigation bar, ...)
            val insets =
                windowMetrics.windowInsets.getInsetsIgnoringVisibility(WindowInsets.Type.systemBars())
            // Tính chiều rộng khả dụng của màn hình
            val widthPixels = windowMetrics.bounds.width() - insets.left - insets.right
            (widthPixels / activity.resources.displayMetrics.density).toInt()
        } else {
            // Dành cho các phiên bản Android cũ hơn
            val displayMetrics = DisplayMetrics()
            activity.windowManager.defaultDisplay.getMetrics(displayMetrics)
            (displayMetrics.widthPixels / displayMetrics.density).toInt()
        }
        return AdSize.getCurrentOrientationAnchoredAdaptiveBannerAdSize(activity, adWidth)
    }

    /**
     * Load banner ad dạng collapsible với hiệu ứng shimmer.
     *
     * @param idBannerAd Mã quảng cáo banner.
     * @param isCollapsible Xác định banner có dạng collapsible hay không.
     */
    fun loadBannerOfCollapsible(
        idBannerAd: String = Constant.ID_BANNER_AD,
        isCollapsible: Boolean = false
    ) {
        try {
            adView?.destroy()
            adView = null
            // Lấy container quảng cáo và các view đường viền, trả về nếu không tìm thấy
            val frAds = findViewById<FrameLayout>(R.id.frAds) ?: return
            val viewTopLine = findViewById<View>(R.id.viewTopLine) ?: return
            val viewBottomLine = findViewById<View>(R.id.viewBottomLine) ?: return

            // Hiển thị container và các đường viền
            listOf(frAds, viewTopLine, viewBottomLine).forEach { it.visibility = View.VISIBLE }

            // Inflate view hiệu ứng shimmer khi đang tải quảng cáo
            val shimmerView = LayoutInflater.from(this)
                .inflate(R.layout.ads_banner_shimmer, null) as ShimmerFrameLayout
            shimmerView.startShimmer()
            frAds.removeAllViews()
            frAds.addView(shimmerView)

            // Kiểm tra điều kiện từ remote config để load banner ad
            if (Utils.isReadyLoadAndShow(this, Constant.REMOTE_CONFIG_BANNER)) {
                // Tạo AdView và cấu hình
                adView = AdView(this).apply {
                    adUnitId = idBannerAd
                    setLayerType(View.LAYER_TYPE_SOFTWARE, null)
                }
                frAds.addView(adView)

                // Tính toán kích thước quảng cáo và cập nhật chiều cao cho shimmer
                val adSize = getAdSize(this)
                shimmerView.layoutParams.height =
                    (adSize.height * Resources.getSystem().displayMetrics.density + 0.5f).toInt()
                adView?.setAdSize(adSize)

                // Cấu hình callback cho banner ad
                adView?.adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        super.onAdLoaded()
                        shimmerView.stopShimmer()
                        shimmerView.visibility = View.GONE
                        onBannerAdLoaded()  // Gọi callback khi load thành công
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        super.onAdFailedToLoad(loadAdError)
                        shimmerView.stopShimmer()
                        hideBanner(frAds, viewTopLine, viewBottomLine)
                        onBannerAdFailedToLoad(loadAdError)  // Gọi callback khi load thất bại
                    }

                    override fun onAdClicked() {
                        super.onAdClicked()
                        onBannerAdClicked()  // Gọi callback khi quảng cáo được click
                    }

                    override fun onAdOpened() {
                        super.onAdOpened()
                        onBannerAdOpened()  // Gọi callback khi quảng cáo mở rộng
                    }

                    override fun onAdImpression() {
                        super.onAdImpression()
                        onBannerAdImpression()  // Gọi callback khi ghi nhận impression
                    }

                    override fun onAdClosed() {
                        super.onAdClosed()
                        onBannerAdClosed()  // Gọi callback khi quảng cáo đóng
                    }
                }

                // Xây dựng AdRequest, thêm extra nếu banner dạng collapsible
                val adRequest = if (isCollapsible) {
                    val extras = Bundle().apply { putString("collapsible", "bottom") }
                    AdRequest.Builder()
                        .addNetworkExtrasBundle(AdMobAdapter::class.java, extras)
                        .build()
                } else {
                    AdRequest.Builder().build()
                }
                adView?.loadAd(adRequest)
            } else {
                // Nếu điều kiện load không thỏa mãn thì dừng shimmer và ẩn container
                shimmerView.stopShimmer()
                hideBanner(frAds, viewTopLine, viewBottomLine)
            }
        } catch (e: Exception) {
            // Xử lý exception (có thể log lỗi tại đây)
        }
    }

    /**
     * Hàm ẩn container quảng cáo và các đường viền.
     */
    private fun hideBanner(frAds: FrameLayout, viewTopLine: View, viewBottomLine: View) {
        frAds.removeAllViews()
        listOf(frAds, viewTopLine, viewBottomLine).forEach { it.visibility = View.GONE }
    }

    /**
     * Load native ad với hiệu ứng shimmer, sau đó bind dữ liệu quảng cáo vào view.
     *
     * @param idNativeAd Mã quảng cáo native.
     * @param nativeLayout Layout resource cho NativeAdView.
     * @param nativeShimmer Layout resource cho hiệu ứng shimmer.
     */
    fun loadNative(idNativeAd: String, nativeLayout: Int, nativeShimmer: Int) {
        // Lấy container chứa quảng cáo, trả về nếu không tìm thấy
        val frAds = findViewById<FrameLayout>(R.id.frAds) ?: return
        frAds.visibility = View.VISIBLE
        // Ẩn các đường viền khi hiển thị native ad
        findViewById<View>(R.id.viewTopLine)?.visibility = View.GONE
        findViewById<View>(R.id.viewBottomLine)?.visibility = View.GONE

        // Inflate view shimmer để hiển thị hiệu ứng khi tải quảng cáo
        val shimmerView = LayoutInflater.from(this)
            .inflate(nativeShimmer, null) as ShimmerFrameLayout
        shimmerView.startShimmer()
        frAds.removeAllViews()
        frAds.addView(shimmerView)

        // Kiểm tra điều kiện từ remote config để load native ad
        if (Utils.isReadyLoadAndShow(this, Constant.REMOTE_CONFIG_NATIVE)) {
            // Tạo AdLoader để tải native ad
            val adLoader = AdLoader.Builder(this, idNativeAd)
                .forNativeAd { nativeAd ->
                    // Kiểm tra activity còn hợp lệ để cập nhật UI
                    if (isFinishing || isDestroyed) {
                        nativeAd.destroy()
                        return@forNativeAd
                    }
                    // Inflate layout native ad và ép kiểu về NativeAdView
                    val adView = LayoutInflater.from(this)
                        .inflate(nativeLayout, null) as? NativeAdView
                    if (adView == null) {
                        nativeAd.destroy()
                        return@forNativeAd
                    }
                    // Dừng hiệu ứng shimmer và cập nhật UI với NativeAdView
                    shimmerView.stopShimmer()
                    frAds.removeAllViews()
                    frAds.addView(adView)
                    // Bind dữ liệu quảng cáo vào NativeAdView
                    bindNativeAdToView(nativeAd, adView)
                }
                .withAdListener(object : AdListener() {
                    override fun onAdLoaded() {
                        onNativeAdLoaded()  // Gọi callback native ad loaded
                    }

                    override fun onAdFailedToLoad(adError: LoadAdError) {
                        onNativeAdFailedToLoad(adError)  // Gọi callback native ad failed
                        shimmerView.stopShimmer()
                        frAds.removeAllViews()
                        frAds.visibility = View.GONE
                    }

                    override fun onAdOpened() {
                        onNativeAdOpened()  // Gọi callback native ad opened
                    }

                    override fun onAdClicked() {
                        onNativeAdClicked()  // Gọi callback native ad clicked
                    }

                    override fun onAdImpression() {
                        onNativeAdImpression()  // Gọi callback native ad impression
                    }

                    override fun onAdClosed() {
                        onNativeAdClosed()  // Gọi callback native ad closed
                    }
                })
                .withNativeAdOptions(
                    NativeAdOptions.Builder()
                        .setVideoOptions(VideoOptions.Builder().setStartMuted(true).build())
                        .build()
                )
                .build()

            // Load native ad với AdRequest
            adLoader.loadAd(AdRequest.Builder().build())
        } else {
            // Nếu không thỏa mãn điều kiện load, dừng shimmer và ẩn container
            shimmerView.stopShimmer()
            frAds.removeAllViews()
            frAds.visibility = View.GONE
        }
    }

    /**
     * Bind dữ liệu của NativeAd vào NativeAdView.
     *
     * @param nativeAd Đối tượng chứa dữ liệu quảng cáo.
     * @param nativeAdView View hiển thị quảng cáo native.
     */
    private fun bindNativeAdToView(nativeAd: NativeAd, nativeAdView: NativeAdView) {
        // Ánh xạ các thành phần của native ad tới view tương ứng
        nativeAdView.mediaView = nativeAdView.findViewById(R.id.ad_media)
        nativeAdView.headlineView = nativeAdView.findViewById(R.id.ad_headline)
        nativeAdView.bodyView = nativeAdView.findViewById(R.id.ad_body)
        nativeAdView.callToActionView = nativeAdView.findViewById(R.id.ad_call_to_action)
        nativeAdView.iconView = nativeAdView.findViewById(R.id.ad_app_icon)
        nativeAdView.advertiserView = nativeAdView.findViewById(R.id.ad_advertiser)

        // Cập nhật tiêu đề quảng cáo
        (nativeAdView.headlineView as? TextView)?.text = nativeAd.headline

        // Cập nhật nội dung quảng cáo, ẩn view nếu không có dữ liệu
        (nativeAdView.bodyView as? TextView)?.apply {
            text = nativeAd.body ?: ""
            visibility = if (nativeAd.body.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }

        // Cập nhật nút Call-to-Action, ẩn nếu không có dữ liệu
        (nativeAdView.callToActionView as? Button)?.apply {
            text = nativeAd.callToAction ?: ""
            visibility = if (nativeAd.callToAction.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }

        // Cập nhật icon quảng cáo, ẩn nếu không có
        (nativeAdView.iconView as? ImageView)?.apply {
            if (nativeAd.icon == null) {
                visibility = View.GONE
            } else {
                visibility = View.VISIBLE
                setImageDrawable(nativeAd.icon?.drawable)
            }
        }

        // Cập nhật thông tin advertiser, ẩn nếu không có dữ liệu
        (nativeAdView.advertiserView as? TextView)?.apply {
            text = nativeAd.advertiser ?: ""
            visibility = if (nativeAd.advertiser.isNullOrEmpty()) View.INVISIBLE else View.VISIBLE
        }

        // Đăng ký NativeAd với NativeAdView
        nativeAdView.setNativeAd(nativeAd)
    }

    /**
     * Các callback cho Native Ad (có thể tùy chỉnh xử lý tương ứng).
     */
    open fun onNativeAdLoaded() {
        // Xử lý khi native ad load thành công
    }

    open fun onNativeAdFailedToLoad(adError: LoadAdError) {
        // Xử lý khi native ad load thất bại
    }

    open fun onNativeAdOpened() {
        // Xử lý khi native ad được mở rộng
    }

    open fun onNativeAdClicked() {
        // Xử lý khi người dùng click vào native ad
    }

    open fun onNativeAdImpression() {
        // Xử lý khi native ad ghi nhận 1 lượt hiển thị (impression)
    }

    open fun onNativeAdClosed() {
        // Xử lý khi native ad bị đóng
    }


}
