package cn.pylin.xycjd;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;

/**
 * 通知监听服务心跳检查管理器（后台服务）
 * 重构后：简化计数逻辑，按要求实现心跳检测和重启机制
 */
public class NotificationServiceHeartbeat extends Service {
    
    private static final String TAG = "NotificationServiceHeartbeat";
    private static final long HEARTBEAT_CHECK_INTERVAL_MS = 30000; // 30秒检查间隔
    private static final long HEARTBEAT_TIMEOUT_MS = 25000;  // 25秒超时时间（比心跳间隔稍长）
    private static final int MAX_FAILURE_COUNT = 2;  // 最大失败次数，超过则重启
    private static final int MAX_RESTART_COUNT = 2;  // 最大重启次数，超过则通知
    
    // 内存中存储的心跳状态（无需IO存储）
    private static volatile long lastHeartbeatTime = 0;
    private static volatile int failureCount = 0;  // 心跳失败计数
    private static volatile int restartCount = 0;  // 重启服务计数
    
    private static NotificationServiceHeartbeat instance;
    private Handler handler;
    private Runnable heartbeatCheckRunnable;
    private boolean isChecking = false;
    
    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        handler = new Handler(Looper.getMainLooper());
        
        // 心跳检查任务（检测部分）
        this.heartbeatCheckRunnable = new Runnable() {
            @Override
            public void run() {
                checkServiceHealth();
                if (isChecking) {
                    handler.postDelayed(this, HEARTBEAT_CHECK_INTERVAL_MS);
                }
            }
        };
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (!isChecking) {
            startHeartbeatCheck();
        }
        return START_STICKY; // 如果系统杀死服务，尝试重启
    }
    
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopHeartbeatCheck();
        instance = null;
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized NotificationServiceHeartbeat getInstance() {
        return instance;
    }
    
    /**
     * 上报心跳（由NotificationListenerService的定时器调用，20秒一次）
     */
    public void reportHeartbeat() {
        lastHeartbeatTime = System.currentTimeMillis();
        // 成功收到心跳，重置失败计数
        failureCount = 0;
    }
    
    /**
     * 开始心跳检查（检测部分，30秒检查一次）
     */
    public void startHeartbeatCheck() {
        if (isChecking) {
            return;
        }
        isChecking = true;
        handler.postDelayed(heartbeatCheckRunnable, HEARTBEAT_CHECK_INTERVAL_MS);
    }
    
    /**
     * 停止心跳检查
     */
    public void stopHeartbeatCheck() {
        isChecking = false;
        handler.removeCallbacks(heartbeatCheckRunnable);
    }
    
    /**
     * 检查服务健康状态（检测逻辑）
     */
    private void checkServiceHealth() {
        long currentTime = System.currentTimeMillis();
        long timeSinceLastHeartbeat = currentTime - lastHeartbeatTime;
        
        // 如果超过超时时间没有心跳，说明服务可能已死亡
        if (timeSinceLastHeartbeat > HEARTBEAT_TIMEOUT_MS) {
            // 心跳异常，失败计数+1
            failureCount++;
            logServiceDeath();
            
            // 当不正常计数大于2时进入重启阶段
            if (failureCount > MAX_FAILURE_COUNT) {
                attemptRestartService();
            }
        }
    }
    
    /**
     * 记录服务死亡事件
     */
    private void logServiceDeath() {
        try {
            String logMessage = getString(R.string.heartbeat_log_service_death);
            NotificationLogManager.getInstance(this).log(logMessage);
        } catch (Exception e) {
            // 日志记录失败不影响重启
        }
    }
    
    /**
     * 尝试重启通知监听服务
     */
    private void attemptRestartService() {
        // 每一次重启服务计数+1
        restartCount++;

        if (restartCount > MAX_RESTART_COUNT) {
            // 重启服务计数超过2时通知提示
            sendPermissionReminderNotification();
            // 重置重启计数，避免重复发送通知
            restartCount = 0;
            return;
        }
        
        // 使用重启助手进行重启
        NotificationServiceRestartHelper.restartNotificationListenerService(this);
    }
    
    /**
     * 发送权限提醒通知（API 30+）
     */
    private void sendPermissionReminderNotification() {
        try {
            android.app.NotificationManager notificationManager = 
                (android.app.NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            
            // 创建通知渠道
            android.app.NotificationChannel channel = new android.app.NotificationChannel(
                "heartbeat_permission_channel",
                getString(R.string.heartbeat_channel_name),
                android.app.NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.heartbeat_channel_desc));
            notificationManager.createNotificationChannel(channel);
            
            // 创建Intent，点击后直接打开小雨超级岛的通知监听权限页面
            Intent intent = new Intent(android.provider.Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 创建PendingIntent
            android.app.PendingIntent pendingIntent;
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
                pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_IMMUTABLE
                );
            } else {
                pendingIntent = android.app.PendingIntent.getActivity(
                    this, 0, intent, android.app.PendingIntent.FLAG_UPDATE_CURRENT
                );
            }
            
            // 构建通知
            android.app.Notification.Builder builder;
            builder = new android.app.Notification.Builder(this, "heartbeat_permission_channel");
            
            builder.setContentTitle(getString(R.string.heartbeat_notification_title))
                   .setContentText(getString(R.string.heartbeat_notification_content))
                   .setSmallIcon(android.R.drawable.ic_dialog_alert)
                   .setPriority(android.app.Notification.PRIORITY_HIGH)
                   .setAutoCancel(true)
                   .setContentIntent(pendingIntent);
            
            // 发送通知
            notificationManager.notify(1001, builder.build());
        } catch (Exception e) {
            // 通知发送失败，静默处理
        }
    }
}
