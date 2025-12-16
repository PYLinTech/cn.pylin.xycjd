package cn.pylin.xycjd;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.Manifest;

public class PermissionChecker {
    
    public static class PermissionStatus {
        public boolean hasNotificationPermission;
        public boolean hasNotificationPostPermission;
        public boolean hasOverlayPermission;
        public boolean hasBatteryOptimizationDisabled;
        public int deniedCount;
        
        public PermissionStatus(boolean notification, boolean notificationPost, boolean overlay, boolean battery) {
            this.hasNotificationPermission = notification;
            this.hasNotificationPostPermission = notificationPost;
            this.hasOverlayPermission = overlay;
            this.hasBatteryOptimizationDisabled = battery;
            
            this.deniedCount = 0;
            if (!notification) {
                deniedCount++;
            }
            if (!notificationPost) {
                deniedCount++;
            }
            if (!overlay) {
                deniedCount++;
            }
            if (!battery) {
                deniedCount++;
            }
        }
        
        public boolean areAllPermissionsGranted() {
            return deniedCount == 0;
        }
    }

    public static PermissionStatus checkAllPermissions(Context context) {
        boolean hasNotification = NotificationListenerManager.isNotificationListenerEnabled(context);
        boolean hasNotificationPost = true;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            hasNotificationPost = context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        }
        boolean hasOverlay = Settings.canDrawOverlays(context);
        boolean hasBattery = isIgnoringBatteryOptimizations(context);
        
        return new PermissionStatus(hasNotification, hasNotificationPost, hasOverlay, hasBattery);
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
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            activity.startActivity(intent);
            return;
        } catch (Exception e) {
            try {
                Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                return;
            } catch (Exception e2) {
                Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
                intent.setData(Uri.parse("package:" + activity.getPackageName()));
                activity.startActivity(intent);
                return;
            }
        }
    }
}
