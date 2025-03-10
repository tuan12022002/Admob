package com.library.admob.type

/**
 * Enum class định nghĩa các loại quảng cáo Admob hỗ trợ trong ứng dụng.
 *
 * ADMOB_APP_OPEN: Quảng cáo App Open, thường xuất hiện khi ứng dụng mở ra.
 * ADMOB_INTER: Quảng cáo Interstitial, xuất hiện giữa các màn hình hoặc hoạt động.
 * ADMOB_REWARD: Quảng cáo Rewarded, cho phép người dùng nhận phần thưởng sau khi xem quảng cáo.
 */
enum class AdmobType {
    ADMOB_APP_OPEN, // Quảng cáo hiển thị khi mở ứng dụng.
    ADMOB_INTER,    // Quảng cáo xen kẽ giữa các màn hình.
    ADMOB_REWARD    // Quảng cáo có phần thưởng dành cho người dùng.
}
