package cn.pylin.xycjd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;

public class PermissionChecker {
    
    public static class PermissionStatus {
        public boolean hasNotificationPermission;
        public boolean hasOverlayPermission;
        public boolean hasBatteryOptimizationDisabled;
        public int deniedCount;
        
        public PermissionStatus(boolean notification, boolean overlay, boolean battery) {
            this.hasNotificationPermission = notification;
            this.hasOverlayPermission = overlay;
            this.hasBatteryOptimizationDisabled = battery;
            
            this.deniedCount = 0;
            if (!notification) deniedCount++;
            if (!overlay) deniedCount++;
            if (!battery) deniedCount++;
        }
        
        public boolean areAllPermissionsGranted() {
            return deniedCount == 0;
        }
    }
    
    public static PermissionStatus checkAllPermissions(Context context) {
        boolean hasNotification = NotificationListenerManager.isNotificationListenerEnabled(context);
        boolean hasOverlay = Settings.canDrawOverlays(context);
        boolean hasBattery = isIgnoringBatteryOptimizations(context);
        
        return new PermissionStatus(hasNotification, hasOverlay, hasBattery);
    }
    
    public static boolean isIgnoringBatteryOptimizations(Context context) {
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            return powerManager.isIgnoringBatteryOptimizations(context.getPackageName());
        }
        return false;
    }
    
    public static void openNotificationListenerSettings(Activity activity) {
        NotificationListenerManager.openNotificationListenerSettings(activity);
    }
    
    public static void openOverlaySettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(Uri.parse("package:" + activity.getPackageName()));
        activity.startActivity(intent);
    }
    

    
    public static void openBatteryOptimizationSettings(Activity activity) {
        try {
            // Android 11+ 使用更通用的电池优化设置页面
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);
            return;
        } catch (Exception e) {
            // 如果第一个方法失败，尝试打开电池优化设置页面
            try {
                //直接请求方式
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                return;
            } catch (Exception e2) {
                // 如果第二个方法失败，尝试打开应用详情页
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                return;
            }
        }
    }
}