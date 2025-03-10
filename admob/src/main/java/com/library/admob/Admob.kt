package com.library.admob

import android.app.Activity

/**
 * Đối tượng Admob quản lý trạng thái và thao tác liên quan đến quảng cáo trong ứng dụng.
 * Nó bao gồm các cờ trạng thái cho việc hiển thị và tải quảng cáo, cũng như danh sách các Activity cần loại trừ
 * khỏi việc hiển thị quảng cáo App Open khi ứng dụng chuyển từ background về foreground.
 */
object Admob {
    // Cờ cho biết có quảng cáo đang được hiển thị hay không.
    @Volatile
    private var isAdShowing = false

    // Cờ cho biết có quảng cáo đang được tải hay không.
    @Volatile
    private var isAdLoading = false

    // Cờ chỉ định Activity đầu tiên có hiển thị quảng cáo App Open hay không.
    @Volatile
    private var isOpenActivityFirstToDisplayAd = true

    // Danh sách các lớp Activity mà khi chuyển về (resume) sẽ không hiển thị quảng cáo App Open.
    @Volatile
    private var listActivityOffAdResume: MutableList<Class<*>> = mutableListOf()

    /**
     * Vô hiệu hóa hiển thị quảng cáo App Open khi Activity cụ thể được chuyển resume.
     * Thêm lớp Activity đó vào danh sách loại trừ.
     *
     * @param activityClass Lớp của Activity cần loại trừ khỏi việc hiển thị quảng cáo.
     */
    @Synchronized
    fun disableAppResumeWithActivity(activityClass: Class<*>) {
        listActivityOffAdResume.add(activityClass)
    }

    /**
     * Cho phép hiển thị quảng cáo App Open khi Activity cụ thể được chuyển resume.
     * Loại bỏ lớp Activity đó khỏi danh sách loại trừ.
     *
     * @param activityClass Lớp của Activity cần loại bỏ khỏi danh sách loại trừ.
     */
    @Synchronized
    fun enableAppResumeWithActivity(activityClass: Class<*>) {
        listActivityOffAdResume.remove(activityClass)
    }

    /**
     * Đặt trạng thái để xác định Activity đầu tiên có hiển thị quảng cáo App Open hay không.
     *
     * @param isOpenActivityFirstToDisplayAd Giá trị boolean xác định trạng thái hiển thị (mặc định là true).
     */
    @Synchronized
    fun setOpenActivityFirstToDisplayAd(isOpenActivityFirstToDisplayAd: Boolean = true) {
        Admob.isOpenActivityFirstToDisplayAd = isOpenActivityFirstToDisplayAd
    }

    /**
     * Lấy trạng thái cho biết Activity đầu tiên có hiển thị quảng cáo App Open hay không.
     *
     * @return true nếu Activity đầu tiên được phép hiển thị quảng cáo App Open, false nếu không.
     */
    @Synchronized
    fun getOpenActivityFirstToDisplayAd(): Boolean {
        return isOpenActivityFirstToDisplayAd
    }

    /**
     * Đặt trạng thái cho biết quảng cáo có đang được hiển thị hay không.
     *
     * @param isAdShowing Giá trị boolean xác định trạng thái hiển thị quảng cáo.
     */
    @Synchronized
    fun setAdShowing(isAdShowing: Boolean) {
        Admob.isAdShowing = isAdShowing
    }

    /**
     * Lấy trạng thái cho biết quảng cáo có đang được hiển thị hay không.
     *
     * @return true nếu có quảng cáo đang hiển thị, false nếu không.
     */
    @Synchronized
    fun getAdShowing(): Boolean = isAdShowing

    /**
     * Kiểm tra xem có thể hiển thị quảng cáo App Open khi Activity chuyển resume hay không.
     * Điều kiện để hiển thị là:
     *  - Activity không nằm trong danh sách loại trừ (listActivityOffAdResume).
     *  - Không có quảng cáo nào đang được hiển thị.
     *  - Không có quảng cáo nào đang được tải.
     *
     * @param activity Activity cần kiểm tra.
     * @return true nếu đáp ứng điều kiện hiển thị quảng cáo, false nếu không.
     */
    fun isReadyShowAppOpenResume(activity: Activity): Boolean {
        listActivityOffAdResume.forEach { activityClass ->
            if (activityClass.name == activity.javaClass.name) {
                return false
            }
        }
        return !getAdShowing() && !getAdLoading()
    }

    /**
     * Lấy trạng thái cho biết quảng cáo có đang được tải hay không.
     *
     * @return true nếu quảng cáo đang được tải, false nếu không.
     */
    fun getAdLoading() = isAdLoading

    /**
     * Đặt trạng thái cho biết quảng cáo có đang được tải hay không.
     *
     * @param isAdLoading Giá trị boolean xác định trạng thái tải quảng cáo.
     */
    fun setAdLoading(isAdLoading: Boolean) {
        Admob.isAdLoading = isAdLoading
    }
}
