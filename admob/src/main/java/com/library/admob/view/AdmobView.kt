package com.library.admob.view

import android.content.Context
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
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
        // Thêm một đường line kích thước 2dp ngay duới frAds
        val lineBottomView = View(context).apply {
            id = R.id.viewBottomLine
            setBackgroundColor(ContextCompat.getColor(this@AdmobView.context, R.color.color_E0E0E0))
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, 1).apply {
                    bottomToBottom = LayoutParams.PARENT_ID
                    startToStart = LayoutParams.PARENT_ID
                    endToEnd = LayoutParams.PARENT_ID
                }
            elevation = 0f
        }
        lineBottomView.visibility = View.GONE
        addView(lineBottomView)

        // Thêm một FrameLayout ở dưới cùng
        val frAds = FrameLayout(context).apply {
            id = R.id.frAds
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT).apply {
                    bottomToBottom = lineBottomView.id
                    startToStart = LayoutParams.PARENT_ID
                    endToEnd = LayoutParams.PARENT_ID
                }
            elevation = 0f
        }
        addView(frAds)

        // Thêm một đường line kích thước 2dp ngay trên frAds
        val lineTopView = View(context).apply {
            id = R.id.viewTopLine
            setBackgroundColor(ContextCompat.getColor(this@AdmobView.context, R.color.color_E0E0E0))
            layoutParams =
                LayoutParams(LayoutParams.MATCH_PARENT, 1).apply {
                    bottomToTop = frAds.id
                    startToStart = LayoutParams.PARENT_ID
                    endToEnd = LayoutParams.PARENT_ID
                }
            elevation = 0f
        }
        lineTopView.visibility = View.GONE
        addView(lineTopView)

        // Inflate layout từ file R.layout.admob_loading và thêm vào AdmobView hiện tại.
        inflate(context, R.layout.admob_loading, this)
    }
}

