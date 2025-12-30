package cn.pylin.xycjd.manager;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.Settings;
import android.text.TextUtils;

import cn.pylin.xycjd.service.AppNotificationListenerService;

public class NotificationListenerManager {
    
    private static final String TAG = "NotificationListenerManager";
    
    /**
     * 检查通知监听权限是否已授予
     * @param context 上下文
     * @return 是否有权限
     */
    public static boolean isNotificationListenerEnabled(Context context) {
        ComponentName componentName = new ComponentName(context, AppNotificationListenerService.class);
        String flat = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        return !TextUtils.isEmpty(flat) && flat.contains(componentName.flattenToString());
    }
    
    /**
     * 打开通知监听权限设置页面
     * @param activity 活动
     */
    public static void openNotificationListenerSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            activity.startActivity(intent);
        } catch (Exception e) {
            tryOpenAppDetailsSettings(activity);
        }
    }
    
    /**
     * 尝试打开应用详情页面作为备选方案
     */
    private static void tryOpenAppDetailsSettings(Activity activity) {
        try {
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        } catch (Exception e) {
            // 所有方法都失败，无法打开设置页面
        }
    }
}