package app.fuckclip.hook.dialog

import android.graphics.Color

/**
 * Color palette configuration adapted for the clipboard request dialog.
 *
 * 剪贴板权限申请弹窗的配色方案，支持深色模式与自定义种子色调节。
 */
internal class DialogThemePalette(isNight: Boolean, keyColor: Int = 0) {
    /**
     * Dialog window surface background color.
     *
     * 对话框窗口表面背景色。
     */
    val surfaceBg: Int = if (isNight) 0xFF1C1D24.toInt() else 0xFFFFFFFF.toInt()

    /**
     * Dialog window outer border outline color.
     *
     * 对话框窗口外边框轮廓色。
     */
    val surfaceBorder: Int = if (isNight) 0xFF2E323D.toInt() else 0xFFE2E6ED.toInt()

    /**
     * Primary emphasis text color.
     *
     * 主要文字高亮色。
     */
    val textPrimary: Int = if (isNight) 0xFFF3F4F6.toInt() else 0xFF111827.toInt()

    /**
     * Secondary emphasis text color.
     *
     * 次要说明文字颜色。
     */
    val textSecondary: Int = if (isNight) 0xFF9CA3AF.toInt() else 0xFF6B7280.toInt()

    /**
     * Tertiary emphasis text color.
     *
     * 第三级弱化提示文字颜色。
     */
    val textTertiary: Int = if (isNight) 0xFF6B7280.toInt() else 0xFF9CA3AF.toInt()

    /**
     * Application icon container background color.
     *
     * 应用图标容器背景色。
     */
    val iconBg: Int = if (isNight) 0xFF272932.toInt() else 0xFFF1F5F9.toInt()

    /**
     * Application icon container border color.
     *
     * 应用图标容器边框颜色。
     */
    val iconBorder: Int = if (isNight) 0xFF383C48.toInt() else 0xFFE2E8F0.toInt()

    /**
     * Clipboard content preview box background color.
     *
     * 剪贴板内容预览区域背景色。
     */
    val previewBg: Int = if (isNight) 0xFF23252E.toInt() else 0xFFF8FAFC.toInt()

    /**
     * Clipboard content preview box border outline color.
     *
     * 剪贴板内容预览区域边框颜色。
     */
    val previewBorder: Int = if (isNight) 0xFF323642.toInt() else 0xFFE5E7EB.toInt()

    /**
     * Background highlight color when the remember checkbox row is pressed.
     *
     * 点击“记住选择”行时的按压高亮背景色。
     */
    val rememberPressed: Int = if (isNight) 0xFF2E313D.toInt() else 0xFFEDF2F7.toInt()

    /**
     * Write action badge background color.
     *
     * 写入操作类型徽标背景色。
     */
    val badgeWriteBg: Int = if (isNight) 0xFF1E2D2F.toInt() else 0xFFE0F2F1.toInt()

    /**
     * Write action badge text/icon color.
     *
     * 写入操作类型徽标文本与图标前景色。
     */
    val badgeWriteFg: Int = if (isNight) 0xFF4DB6AC.toInt() else 0xFF00695C.toInt()

    /**
     * Write action badge border outline color.
     *
     * 写入操作类型徽标边框颜色。
     */
    val badgeWriteBorder: Int = if (isNight) 0xFF264A4E.toInt() else 0xFF80CBC4.toInt()

    /**
     * Read action badge background color.
     *
     * 读取操作类型徽标背景色。
     */
    val badgeReadBg: Int = if (isNight) 0xFF28233D.toInt() else 0xFFEDE7F6.toInt()

    /**
     * Read action badge text/icon color.
     *
     * 读取操作类型徽标文本与图标前景色。
     */
    val badgeReadFg: Int = if (isNight) 0xFFB39DDB.toInt() else 0xFF512DA8.toInt()

    /**
     * Read action badge border outline color.
     *
     * 读取操作类型徽标边框颜色。
     */
    val badgeReadBorder: Int = if (isNight) 0xFF3D3263.toInt() else 0xFFD1C4E9.toInt()

    /**
     * Deny action button background color.
     *
     * 拒绝操作按钮默认背景色。
     */
    val denyBtnBg: Int = if (isNight) 0xFF381E22.toInt() else 0xFFFEE2E2.toInt()

    /**
     * Deny action button pressed state color.
     *
     * 拒绝操作按钮按压状态反馈颜色。
     */
    val denyBtnPressed: Int = if (isNight) 0xFF4A252B.toInt() else 0xFFFECACA.toInt()

    /**
     * Deny action button border outline color.
     *
     * 拒绝操作按钮边框轮廓颜色。
     */
    val denyBtnBorder: Int = if (isNight) 0xFF5E272E.toInt() else 0xFFFCA5A5.toInt()

    /**
     * Deny action button label text color.
     *
     * 拒绝操作按钮文字颜色。
     */
    val denyBtnFg: Int = if (isNight) 0xFFF87171.toInt() else 0xFFDC2626.toInt()

    /**
     * Allow action button background color.
     *
     * 允许操作按钮默认背景色。
     */
    val allowBtnBg: Int = if (keyColor != 0) keyColor else (if (isNight) 0xFF2563EB.toInt() else 0xFF1D4ED8.toInt())

    /**
     * Allow action button pressed state color.
     *
     * 允许操作按钮按压状态反馈颜色。
     */
    val allowBtnPressed: Int = if (keyColor != 0) adjustBrightness(keyColor, 0.85f) else (if (isNight) 0xFF1D4ED8.toInt() else 0xFF1E40AF.toInt())

    /**
     * Allow action button label text color.
     *
     * 允许操作按钮文字颜色。
     */
    val allowBtnFg: Int = 0xFFFFFFFF.toInt()

    /**
     * Primary accent color used for checkboxes and active indicators.
     *
     * 用于复选框与焦点指示的主要强调色。
     */
    val accentColor: Int = if (keyColor != 0) keyColor else (if (isNight) 0xFF3B82F6.toInt() else 0xFF2563EB.toInt())

    private fun adjustBrightness(color: Int, factor: Float): Int {
        val a = Color.alpha(color)
        val r = (Color.red(color) * factor).toInt().coerceIn(0, 255)
        val g = (Color.green(color) * factor).toInt().coerceIn(0, 255)
        val b = (Color.blue(color) * factor).toInt().coerceIn(0, 255)
        return Color.argb(a, r, g, b)
    }
}
