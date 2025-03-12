package com.library.admob.view

import android.content.Context
import android.util.AttributeSet
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import com.library.admob.R

/**
 * Lớp AdmobView kế thừa từ ConstraintLayout, dùng để hiển thị layout loading cho quảng cáo Admob.
 *
 * @param context Ngữ cảnh của ứng dụng, cần thiết để truy cập tài nguyên và các dịch vụ.
 * @param attrs (Tùy chọn) Các thuộc tính XML được áp dụng cho view này khi khai báo trong layout.
 * @param defStyleAttr (Tùy chọn) Giá trị style mặc định áp dụng cho view.
 */
class AdmobView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : ConstraintLayout(context, attrs, defStyleAttr) {

    init {
        // Inflate layout từ file R.layout.admob_loading và thêm vào AdmobView hiện tại.
        inflate(context, R.layout.admob_loading, this)

        // Thêm một FrameLayout ở dưới cùng
        val frAds = FrameLayout(context).apply {
            id = R.id.frAds
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomToBottom = LayoutParams.PARENT_ID
                    startToStart = LayoutParams.PARENT_ID
                    endToEnd = LayoutParams.PARENT_ID
                }
        }
        addView(frAds)
    }
}

