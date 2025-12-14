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
        
        // 检查该应用是否被选中监听
        if (isAppSelected(packageName)) {
            // 处理选中的应用通知
            handleNotification(sbn);
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        
        String packageName = sbn.getPackageName();
        
        // 检查该应用是否被选中监听
        if (isAppSelected(packageName)) {
            // 处理选中的应用通知移除
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
        
        Log.d(TAG, "收到通知 - 包名: " + packageName + ", 标题: " + title + ", 内容: " + text);
        
        // 这里可以添加更多处理逻辑，例如：
        // 1. 显示悬浮窗
        // 2. 保存通知历史
        // 3. 发送广播通知其他组件
        // 4. 根据通知内容执行特定操作
    }
    
    /**
     * 处理选中的应用通知移除
     * @param sbn 状态栏通知
     */
    private void handleNotificationRemoved(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        String title = sbn.getNotification().extras.getString("android.title");
        
        Log.d(TAG, "通知已移除 - 包名: " + packageName + ", 标题: " + title);
        
        // 这里可以添加通知移除后的处理逻辑
    }
}