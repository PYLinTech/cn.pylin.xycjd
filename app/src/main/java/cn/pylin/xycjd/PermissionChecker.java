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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
            intent.setData(Uri.parse("package:" + activity.getPackageName()));
            activity.startActivity(intent);
        }
    }
}