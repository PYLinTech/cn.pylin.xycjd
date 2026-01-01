package cn.pylin.xycjd.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;

/**
 * 悬浮窗背景生成工具类
 * 用于代码方式创建各种悬浮窗背景，替代XML drawable
 */
public class FloatingWindowBackgroundHelper {

    /**
     * 创建基础悬浮窗的圆角矩形背景
     * @param context 上下文
     * @return GradientDrawable 圆角矩形背景
     */
    public static GradientDrawable createBasicFloatingWindowBackground(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        drawable.setCornerRadius(dpToPx(context, 20)); // 20dp 圆角
        return drawable;
    }

    /**
     * 创建三个圆圈悬浮窗的背景（胶囊形状）
     * @param context 上下文
     * @return GradientDrawable 胶囊形状背景
     */
    public static GradientDrawable createIslandBackground(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        drawable.setCornerRadius(dpToPx(context, 100)); // 100dp 圆角，形成胶囊形状
        return drawable;
    }

    /**
     * 创建信息卡片的圆角矩形背景
     * @param context 上下文
     * @return GradientDrawable 卡片背景
     */
    public static GradientDrawable createCardBackground(Context context) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        drawable.setCornerRadius(dpToPx(context, 32)); // 32dp 圆角
        return drawable;
    }

    /**
     * 创建带边框的圆角矩形背景（可选）
     * @param context 上下文
     * @param cornerRadius 圆角半径（dp）
     * @param backgroundColor 背景色
     * @param strokeColor 边框色
     * @param strokeWidth 边框宽度（dp）
     * @return GradientDrawable 带边框的圆角矩形
     */
    public static GradientDrawable createBoundedRoundedBackground(
            Context context,
            float cornerRadius,
            int backgroundColor,
            int strokeColor,
            int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(backgroundColor);
        drawable.setCornerRadius(dpToPx(context, cornerRadius));
        drawable.setStroke(dpToPx(context, strokeWidth), strokeColor);
        return drawable;
    }

    /**
     * dp转px
     */
    private static int dpToPx(Context context, float dp) {
        return (int) TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP,
                dp,
                context.getResources().getDisplayMetrics());
    }
}