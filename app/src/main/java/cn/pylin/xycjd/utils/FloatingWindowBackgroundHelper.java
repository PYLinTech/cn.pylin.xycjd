package cn.pylin.xycjd.utils;

import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.ShapeDrawable;
import android.graphics.drawable.shapes.RoundRectShape;
import android.util.TypedValue;
import androidx.core.content.ContextCompat;

import cn.pylin.xycjd.manager.SharedPreferencesManager;

/**
 * 悬浮窗背景生成工具类
 * 用于代码方式创建各种悬浮窗背景，替代XML drawable
 */
public class FloatingWindowBackgroundHelper {

    /**
     * 创建基础悬浮窗的圆角矩形背景
     * @param context 上下文
     * @param size 第一个悬浮窗的大小（px）
     * @return GradientDrawable 圆角矩形背景
     */
    public static GradientDrawable createBasicFloatingWindowBackground(Context context, int size) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        
        // 根据存储的百分比计算实际圆角值
        int radiusPercent = SharedPreferencesManager.getInstance(context).getFloatingCornerRadius1();
        float cornerRadius = calculateCornerRadius(context, size, radiusPercent);
        drawable.setCornerRadius(cornerRadius);
        
        return drawable;
    }

    /**
     * 创建三个圆圈悬浮窗的背景（胶囊形状）
     * @param context 上下文
     * @param size 第一个悬浮窗的大小（px）
     * @return GradientDrawable 胶囊形状背景
     */
    public static GradientDrawable createIslandBackground(Context context, int size) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        
        // 根据存储的百分比计算实际圆角值（使用第二个百分比）
        int radiusPercent = SharedPreferencesManager.getInstance(context).getFloatingCornerRadius2();
        float cornerRadius = calculateCornerRadius(context, size, radiusPercent);
        drawable.setCornerRadius(cornerRadius);
        
        return drawable;
    }

    /**
     * 创建信息卡片的圆角矩形背景
     * @param context 上下文
     * @param cardHeight 卡片高度（px）
     * @return GradientDrawable 卡片背景
     */
    public static GradientDrawable createCardBackground(Context context, int cardHeight) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(ContextCompat.getColor(context, android.R.color.black));
        
        // 根据存储的百分比计算实际圆角值（使用第三个百分比）
        int radiusPercent = SharedPreferencesManager.getInstance(context).getFloatingCornerRadius3();
        float cornerRadius = calculateCornerRadius(context, cardHeight, radiusPercent);
        drawable.setCornerRadius(cornerRadius);
        
        return drawable;
    }

    /**
     * 创建带边框的圆角矩形背景（可选）
     * @param context 上下文
     * @param size 第一个悬浮窗的大小（px）
     * @param radiusPercent 圆角百分比（0-100）
     * @param backgroundColor 背景色
     * @param strokeColor 边框色
     * @param strokeWidth 边框宽度（dp）
     * @return GradientDrawable 带边框的圆角矩形
     */
    public static GradientDrawable createBoundedRoundedBackground(
            Context context,
            int size,
            int radiusPercent,
            int backgroundColor,
            int strokeColor,
            int strokeWidth) {
        GradientDrawable drawable = new GradientDrawable();
        drawable.setShape(GradientDrawable.RECTANGLE);
        drawable.setColor(backgroundColor);
        
        float cornerRadius = calculateCornerRadius(context, size, radiusPercent);
        drawable.setCornerRadius(cornerRadius);
        drawable.setStroke(dpToPx(context, strokeWidth), strokeColor);
        return drawable;
    }

    /**
     * 根据悬浮窗大小和百分比计算实际圆角值
     * @param context 上下文
     * @param size 悬浮窗大小（px）
     * @param radiusPercent 圆角百分比（0-100）
     * @return 实际圆角值（px）
     */
    private static float calculateCornerRadius(Context context, int size, int radiusPercent) {
        // 限制百分比在0-100之间
        int percent = Math.max(0, Math.min(100, radiusPercent));
        
        // 计算实际圆角值：百分比 * (大小 / 2)
        // 100% 时圆角等于大小的一半，形成圆形
        // 0% 时圆角为0，形成直角矩形
        float cornerRadius = (size / 2f) * (percent / 100f);
        
        return cornerRadius;
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