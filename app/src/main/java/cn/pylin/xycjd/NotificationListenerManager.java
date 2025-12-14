package cn.pylin.xycjd;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.provider.Settings;
import android.text.TextUtils;

import java.lang.reflect.Method;
import java.util.Set;

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
            Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
            activity.startActivity(intent);
        } catch (Exception e) {
            // 如果标准方法失败，尝试反射方法
            try {
                Intent intent = new Intent();
                intent.setAction("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
                activity.startActivity(intent);
            } catch (Exception ex) {
                // 如果都失败，打开应用设置页面
                try {
                    Intent intent = new Intent();
                    intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                    intent.setData(android.net.Uri.parse("package:" + activity.getPackageName()));
                    activity.startActivity(intent);
                } catch (Exception exc) {
                    // 最后的尝试
                    try {
                        Intent intent = new Intent(Settings.ACTION_SETTINGS);
                        activity.startActivity(intent);
                    } catch (Exception exce) {
                        // 所有方法都失败
                    }
                }
            }
        }
    }
    
    /**
     * 获取所有启用的通知监听器
     * @param context 上下文
     * @return 启用的监听器集合
     */
    public static Set<String> getEnabledListeners(Context context) {
        String enabledListeners = Settings.Secure.getString(context.getContentResolver(), "enabled_notification_listeners");
        if (enabledListeners != null) {
            String[] listeners = android.text.TextUtils.split(enabledListeners, ":");
            Set<String> listenerSet = new java.util.HashSet<>();
            for (String listener : listeners) {
                listenerSet.add(listener);
            }
            return listenerSet;
        }
        return new java.util.HashSet<>();
    }
    
    /**
     * 检查特定包名的通知监听器是否启用
     * @param context 上下文
     * @param packageName 包名
     * @return 是否启用
     */
    public static boolean isListenerEnabled(Context context, String packageName) {
        Set<String> enabledListeners = getEnabledListeners(context);
        for (String listener : enabledListeners) {
            if (listener.contains(packageName)) {
                return true;
            }
        }
        return false;
    }
}