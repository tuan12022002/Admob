package com.library.admob.appliction

import android.app.Activity
import android.app.Application
import android.os.Bundle
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.library.admob.Admob
import com.library.admob.activity.AdmobActivity
import java.lang.ref.WeakReference

/**
 * Lớp AdmobApplication mở rộng từ Application, đồng thời triển khai các giao diện
 * ActivityLifecycleCallbacks và DefaultLifecycleObserver để theo dõi vòng đời của Activity và Application.
 *
 * Lớp này chịu trách nhiệm khởi tạo MobileAds, theo dõi các sự kiện vòng đời của Activity,
 * cũng như hiển thị quảng cáo App Open khi ứng dụng chuyển từ background về foreground.
 */
open class AdmobApplication : Application(),
    Application.ActivityLifecycleCallbacks,
    DefaultLifecycleObserver {

    // Lưu trữ tham chiếu yếu đến Activity hiện tại, nhằm tránh rò rỉ bộ nhớ.
    private var activity: WeakReference<Activity>? = null

    /**
     * Hàm onCreate() của Application.
     * - Khởi tạo MobileAds.
     * - Đăng ký lắng nghe các sự kiện vòng đời của Activity.
     * - Thêm observer vào lifecycle của ProcessLifecycleOwner để theo dõi vòng đời của ứng dụng.
     */
    override fun onCreate() {
        super<Application>.onCreate()
        // Khởi tạo MobileAds SDK
        registerActivityLifecycleCallbacks(this)  // Đăng ký lắng nghe vòng đời của Activity
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)  // Đăng ký observer cho lifecycle của ứng dụng
    }

    /**
     * Callback onStart từ DefaultLifecycleObserver, được gọi khi ứng dụng chuyển sang foreground.
     * Kiểm tra xem có Activity hiện tại nào không và liệu điều kiện để hiển thị quảng cáo App Open đã sẵn sàng chưa.
     * Nếu đủ điều kiện, đặt cờ hiển thị quảng cáo và gọi hàm showAppOpenAd() của AdmobActivity.
     *
     * @param owner LifecycleOwner của ProcessLifecycleOwner.
     */
    @Synchronized
    override fun onStart(owner: LifecycleOwner) {
        try {
            // Lấy Activity hiện tại từ WeakReference nếu có
            activity?.get()?.let { activity ->
                // Kiểm tra xem có nên hiển thị quảng cáo App Open khi ứng dụng resume không
                if (Admob.isReadyShowAppOpenResume(activity)) {
                    Admob.setOpenActivityFirstToDisplayAd(true) // Đặt cờ để hiển thị quảng cáo khi mở activity
                    val admobActivity = activity as AdmobActivity
                    admobActivity.showAppOpenAd() // Gọi hiển thị quảng cáo App Open
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    /**
     * Callback khi Activity được tạo.
     *
     * @param activity Activity được tạo.
     * @param savedInstanceState Bundle chứa trạng thái lưu trước đó (nếu có).
     */
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        // Chưa cần thực hiện gì trong hàm này
    }

    /**
     * Callback khi Activity bắt đầu.
     * Lưu lại tham chiếu yếu đến Activity hiện tại.
     *
     * @param activity Activity bắt đầu.
     */
    override fun onActivityStarted(activity: Activity) {
        this.activity = WeakReference(activity)
    }

    /**
     * Callback khi Activity được resume.
     *
     * @param activity Activity đang được resume.
     */
    override fun onActivityResumed(activity: Activity) {
        // Chưa cần thực hiện gì trong hàm này
    }

    /**
     * Callback khi Activity bị pause.
     *
     * @param activity Activity bị pause.
     */
    override fun onActivityPaused(activity: Activity) {
        // Chưa cần thực hiện gì trong hàm này
    }

    /**
     * Callback khi Activity dừng.
     *
     * @param activity Activity dừng.
     */
    override fun onActivityStopped(activity: Activity) {
        // Chưa cần thực hiện gì trong hàm này
    }

    /**
     * Callback khi hệ thống lưu trạng thái của Activity.
     *
     * @param activity Activity cần lưu trạng thái.
     * @param outState Bundle chứa trạng thái lưu.
     */
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {
        // Chưa cần thực hiện gì trong hàm này
    }

    /**
     * Callback khi Activity bị hủy.
     *
     * @param activity Activity bị hủy.
     */
    override fun onActivityDestroyed(activity: Activity) {
        // Chưa cần thực hiện gì trong hàm này
    }
}

