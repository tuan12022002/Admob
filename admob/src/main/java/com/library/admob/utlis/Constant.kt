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


    const val ID_NATIVE_AD = "ca-app-pub-3940256099942544/2247696110"

    const val ID_BANNER_AD = "ca-app-pub-3940256099942544/6300978111"

    // Số lần tối đa thử tải quảng cáo trước khi dừng quá trình tải.
    const val MAX_LOADING_ADS = 3

    const val REMOTE_CONFIG = "remote_config"
    const val REMOTE_CONFIG_INTER = "inter"
    const val REMOTE_CONFIG_NATIVE = "native"
    const val REMOTE_CONFIG_BANNER = "banner"
    const val REMOTE_CONFIG_APP_OPEN = "app_open"
    const val REMOTE_CONFIG_REWARD = "reward"
    const val REMOTE_CONFIG_COLLAPSIBLE = "collapsible"
}
