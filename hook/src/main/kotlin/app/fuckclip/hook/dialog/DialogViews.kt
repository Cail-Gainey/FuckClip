package app.fuckclip.hook.dialog

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
import android.util.TypedValue
import android.view.Gravity
import android.widget.Button
import android.widget.ScrollView

/**
 * ScrollView with a maximum height boundary.
 *
 * 带有最大高度限制的滚动视图。
 */
internal class MaxHeightScrollView(context: Context, private val maxHeightPx: Int) : ScrollView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val customSpec = MeasureSpec.makeMeasureSpec(maxHeightPx, MeasureSpec.AT_MOST)
        super.onMeasure(widthMeasureSpec, customSpec)
    }
}

/**
 * Creates a rounded card background drawable.
 *
 * 创建圆角卡片背景 Drawable。
 */
internal fun createCardBackground(
    fillColor: Int,
    strokeColor: Int,
    radiusPx: Int,
    strokeWidthPx: Int = 1
): GradientDrawable {
    return GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusPx.toFloat()
        setColor(fillColor)
        if (strokeColor != 0) {
            setStroke(strokeWidthPx.coerceAtLeast(1), strokeColor)
        }
    }
}

/**
 * Creates a material-styled ripple drawable with rounded corners.
 *
 * 创建带有圆角遮罩的 Material 水波纹点击反馈 Drawable。
 */
internal fun createRippleDrawable(
    normalColor: Int,
    pressedColor: Int,
    radiusPx: Int,
    strokeColor: Int = 0,
    strokeWidthPx: Int = 1
): Drawable {
    val normal = createCardBackground(normalColor, strokeColor, radiusPx, strokeWidthPx)
    val mask = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radiusPx.toFloat()
        setColor(Color.WHITE)
    }
    return RippleDrawable(ColorStateList.valueOf(pressedColor), normal, mask)
}

/**
 * Creates a customized action button for dialog responses.
 *
 * 创建弹窗操作按钮（允许/拒绝）。
 */
@SuppressLint("SetTextI18n")
internal fun createStyledButton(
    context: Context,
    bgColor: Int,
    pressedColor: Int,
    strokeColor: Int,
    textColor: Int,
    label: String,
    radiusPx: Int,
    strokeWidthPx: Int = 1
): Button {
    return Button(context).apply {
        minHeight = 0
        minimumHeight = 0
        minWidth = 0
        minimumWidth = 0
        setPadding(0, 0, 0, 0)
        stateListAnimator = null
        isAllCaps = false
        text = label
        setTextColor(textColor)
        setTextSize(TypedValue.COMPLEX_UNIT_SP, 14.5f)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        background = createRippleDrawable(bgColor, pressedColor, radiusPx, strokeColor, strokeWidthPx)
    }
}
