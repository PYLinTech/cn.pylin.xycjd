package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.util.Log;

public class AppNotificationListenerService extends NotificationListenerService {
    
    private static final String TAG = "AppNotificationListener";
    private static final String PREFS_NAME = "app_checkboxes";
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        String packageName = sbn.getPackageName();
        
        if (isAppSelected(packageName)) {
            handleNotification(sbn);
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        
        String packageName = sbn.getPackageName();
        
        if (isAppSelected(packageName)) {
            handleNotificationRemoved(sbn);
        }
    }
    
    /**
     * 检查应用是否被选中监听
     * @param packageName 应用包名
     * @return 是否被选中
     */
    private boolean isAppSelected(String packageName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(packageName, false);
    }
    
    /**
     * 处理选中的应用通知
     * @param sbn 状态栏通知
     */
    private void handleNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        String text = sbn.getNotification().extras.getString("android.text");
        
        Log.d(TAG, "Notification received - Package: " + packageName + ", Title: " + title + ", Content: " + text);
        
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.updateNotificationData(packageName, title, text);
            service.showThreeCircleIsland(packageName);
        }
    }
    
    /**
     * 处理选中的应用通知移除
     * @param sbn 状态栏通知
     */
    private void handleNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        
        Log.d(TAG, "Notification removed - Package: " + packageName + ", Title: " + title);
        
        // 隐藏灵动岛样式的悬浮窗
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.hideNotificationIsland();
            // 隐藏三圆灵动岛悬浮窗
            service.hideThreeCircleIsland();
        }
    }
}
