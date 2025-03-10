package com.library.admob.utlis

/**
 * Đối tượng Constant chứa các hằng số sử dụng trong ứng dụng.
 * Bao gồm các ID quảng cáo cho từng loại quảng cáo và số lần tối đa thử tải quảng cáo.
 */
object Constant {
    // ID cho quảng cáo Rewarded, dùng để tải quảng cáo có phần thưởng.
    const val ID_REWARD_AD = "ca-app-pub-3940256099942544/5224354917"

    // ID cho quảng cáo Interstitial, dùng để tải quảng cáo xen kẽ giữa các màn hình.
    const val ID_INTERSTITIAL_AD = "ca-app-pub-3940256099942544/1033173712"

    // ID cho quảng cáo App Open, dùng để tải quảng cáo khi mở ứng dụng.
    const val ID_APP_OPEN_AD = "ca-app-pub-3940256099942544/9257395921"

    // Số lần tối đa thử tải quảng cáo trước khi dừng quá trình tải.
    const val MAX_LOADING_ADS = 3
}
