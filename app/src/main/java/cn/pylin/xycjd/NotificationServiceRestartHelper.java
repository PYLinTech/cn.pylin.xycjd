package cn.pylin.xycjd;

import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.Looper;

/**
 * 通知监听服务重启助手
 * 提供多种方式尝试重启NotificationListenerService
 */
public class NotificationServiceRestartHelper {
    
    private static final String TAG = "NotificationServiceRestartHelper";
    
    /**
     * 尝试重启通知监听服务
     */
    public static void restartNotificationListenerService(Context context) {
        // 检查权限
        if (!NotificationListenerManager.isNotificationListenerEnabled(context)) {
            return;
        }
        
        Handler handler = new Handler(Looper.getMainLooper());
        
        // 先停止服务
        stopService(context);
        
        // 延迟后重启服务
        handler.postDelayed(() -> {
            // 直接启动服务
            startServiceDirectly(context);
            
            // 记录重启完成
            logRestartEvent(context);
        }, 1000);
    }
    
    /**
     * 直接停止服务
     */
    private static void stopService(Context context) {
        try {
            Intent intent = new Intent(context, AppNotificationListenerService.class);
            context.stopService(intent);
        } catch (Exception e) {
            // 忽略停止失败
        }
    }
    
    /**
     * 直接启动服务
     */
    private static void startServiceDirectly(Context context) {
        try {
            Intent intent = new Intent(context, AppNotificationListenerService.class);
            context.startService(intent);
        } catch (Exception e) {
            // 启动失败，可能是系统限制
        }
    }
    
    /**
     * 记录重启事件到日志系统
     */
    private static void logRestartEvent(Context context) {
        try {          
            String logMessage = context.getString(R.string.heartbeat_log_restart_completed);
            NotificationLogManager.getInstance(context).log(logMessage);
        } catch (Exception e) {
            // 日志记录失败，不影响重启功能
        }
    }
}
