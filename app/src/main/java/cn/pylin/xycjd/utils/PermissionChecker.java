package cn.pylin.xycjd.utils;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.Manifest;

import cn.pylin.xycjd.manager.NotificationListenerManager;
import cn.pylin.xycjd.service.AppAccessibilityService;

public class PermissionChecker {
    
    public static class PermissionStatus {
        public boolean hasNotificationPermission;
        public boolean hasNotificationPostPermission;
        public boolean hasOverlayPermission;
        public boolean hasAccessibilityPermission;
        public boolean hasBatteryOptimizationDisabled;
        public boolean hasShizukuPermission;
        public int deniedCount;
        
        public PermissionStatus(boolean notification, boolean notificationPost, boolean overlay, boolean accessibility, boolean battery, boolean shizuku) {
            this.hasNotificationPermission = notification;
            this.hasNotificationPostPermission = notificationPost;
            this.hasOverlayPermission = overlay;
            this.hasAccessibilityPermission = accessibility;
            this.hasBatteryOptimizationDisabled = battery;
            this.hasShizukuPermission = shizuku;
            
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
            if (!accessibility) {
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
        boolean hasAccessibility = isAccessibilitySettingsOn(context);
        boolean hasBattery = isIgnoringBatteryOptimizations(context);
        boolean hasShizuku = checkShizukuPermission();
        
        return new PermissionStatus(hasNotification, hasNotificationPost, hasOverlay, hasAccessibility, hasBattery, hasShizuku);
    }

    public static boolean checkShizukuPermission() {
        try {
            if (rikka.shizuku.Shizuku.isPreV11()) {
                return false;
            }
            if (rikka.shizuku.Shizuku.pingBinder()) {
                return rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean requestShizukuPermission(int requestCode) {
        try {
            if (rikka.shizuku.Shizuku.isPreV11()) {
                return false;
            }
            if (rikka.shizuku.Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
                return true;
            } else if (rikka.shizuku.Shizuku.shouldShowRequestPermissionRationale()) {
                // Should show rationale, but we just request for now or let caller handle UI
                rikka.shizuku.Shizuku.requestPermission(requestCode);
                return true;
            } else {
                rikka.shizuku.Shizuku.requestPermission(requestCode);
                return true;
            }
        } catch (Throwable ignored) {
        }
        return false;
    }

    public static boolean isAccessibilitySettingsOn(Context context) {
        int accessibilityEnabled = 0;
        final String service = context.getPackageName() + "/" + AppAccessibilityService.class.getCanonicalName();
        try {
            accessibilityEnabled = Settings.Secure.getInt(
                    context.getApplicationContext().getContentResolver(),
                    android.provider.Settings.Secure.ACCESSIBILITY_ENABLED);
        } catch (Settings.SettingNotFoundException e) {
            // Ignored
        }
        android.text.TextUtils.SimpleStringSplitter mStringColonSplitter = new android.text.TextUtils.SimpleStringSplitter(':');

        if (accessibilityEnabled == 1) {
            String settingValue = Settings.Secure.getString(
                    context.getApplicationContext().getContentResolver(),
                    Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES);
            if (settingValue != null) {
                mStringColonSplitter.setString(settingValue);
                while (mStringColonSplitter.hasNext()) {
                    String accessibilityService = mStringColonSplitter.next();
                    if (accessibilityService.equalsIgnoreCase(service)) {
                        return true;
                    }
                }
            }
        }
        return false;
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

    public static void openAccessibilitySettings(Activity activity) {
        Intent intent = new Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS);
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
