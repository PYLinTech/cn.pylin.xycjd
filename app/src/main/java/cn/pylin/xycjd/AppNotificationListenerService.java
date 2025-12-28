package cn.pylin.xycjd;

import android.content.Intent;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

/**
 * 应用通知监听服务
 * 重构后：主要职责是接收通知并委托给 NotificationProcessor 处理
 */
public class AppNotificationListenerService extends NotificationListenerService {
    
    private static AppNotificationListenerService instance;
    private NotificationProcessor processor;
    private NotificationServiceHeartbeat heartbeat;
    
    // 心跳上报定时器（被检测部分）
    private Handler heartbeatHandler;
    private Runnable heartbeatRunnable;
    private static final long HEARTBEAT_REPORT_INTERVAL_MS = 20000; // 20秒上报一次心跳

    public static AppNotificationListenerService getInstance() {
        return instance;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        // 初始化处理器
        processor = new NotificationProcessor(this);
        
        // 获取心跳管理器
        heartbeat = NotificationServiceHeartbeat.getInstance();
        
        // 启动心跳上报定时器（被检测部分：不断上报心跳）
        startHeartbeatReporter();
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        // 停止心跳上报
        stopHeartbeatReporter();
        instance = null;
        processor = null;
        heartbeat = null;
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        // 委托给处理器处理
        if (processor != null) {
            processor.processNotification(sbn);
        }
        
        // 记录通知日志（保留原有功能）
        logNotification(sbn);
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        // 处理通知移除（如果需要）
        handleNotificationRemoved(sbn);
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
        String packageName = sbn.getPackageName();
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        String mode = manager.getNotificationMode();
        
        // 如果是超级岛模式且是监听器主动取消，跳过处理
        if ("mode_super_island_only".equals(mode) && reason == 10) {
            return;
        }
        
        super.onNotificationRemoved(sbn, rankingMap, reason);
    }
    
    /**
     * 处理通知移除
     */
    private void handleNotificationRemoved(StatusBarNotification sbn) {
        // 从超级岛移除通知
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.removeNotification(sbn.getKey());
        }
    }

    /**
     * 启动心跳上报定时器（被检测部分：不断上报心跳）
     */
    private void startHeartbeatReporter() {
        if (heartbeatHandler == null) {
            heartbeatHandler = new Handler(Looper.getMainLooper());
        }
        
        heartbeatRunnable = new Runnable() {
            @Override
            public void run() {
                // 上报心跳
                if (heartbeat != null) {
                    heartbeat.reportHeartbeat();
                }
                
                // 记录心跳日志
                logHeartbeat();
                
                // 继续下一次上报
                if (heartbeatHandler != null && heartbeatRunnable != null) {
                    heartbeatHandler.postDelayed(this, HEARTBEAT_REPORT_INTERVAL_MS);
                }
            }
        };
        
        // 立即开始第一次上报，然后按间隔继续
        heartbeatHandler.postDelayed(heartbeatRunnable, HEARTBEAT_REPORT_INTERVAL_MS);
    }
    
    /**
     * 记录心跳日志
     */
    private void logHeartbeat() {
        try {
            String logMessage = getString(R.string.heartbeat_log_heartbeat_normal);
            NotificationLogManager.getInstance(this).log(logMessage);
            
            // 心跳正常时，重置重启计数
            if (heartbeat != null) {
                heartbeat.resetRestartCount();
            }
        } catch (Exception e) {
            // 日志记录失败不影响心跳
        }
    }
    
    /**
     * 停止心跳上报定时器
     */
    private void stopHeartbeatReporter() {
        if (heartbeatHandler != null && heartbeatRunnable != null) {
            heartbeatHandler.removeCallbacks(heartbeatRunnable);
        }
        heartbeatHandler = null;
        heartbeatRunnable = null;
    }
    
    /**
     * 记录通知日志（保留原有功能）
     */
    private void logNotification(StatusBarNotification sbn) {
        // 检查应用是否被选中
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        boolean isSelected = manager.isAppEnabled(sbn.getPackageName());
        
        StringBuilder sb = new StringBuilder();
        sb.append("\n").append(getString(R.string.log_notification_label)).append("\n");
        sb.append(getString(R.string.log_package_name)).append(" ").append(sbn.getPackageName()).append("\n");
        sb.append(getString(R.string.log_notification_id)).append(" ").append(sbn.getId()).append("\n");
        sb.append(getString(R.string.log_tag)).append(" ").append(sbn.getTag()).append("\n");
        sb.append(getString(R.string.log_time)).append(" ").append(new java.util.Date(sbn.getPostTime())).append("\n");
        sb.append(getString(R.string.log_clearable)).append(" ").append(sbn.isClearable()).append("\n");
        sb.append(getString(R.string.log_ongoing)).append(" ").append(sbn.isOngoing()).append("\n");
        sb.append(getString(R.string.log_key)).append(" ").append(sbn.getKey()).append("\n");
        sb.append(getString(R.string.log_group_key)).append(" ").append(sbn.getGroupKey()).append("\n");
        sb.append(getString(R.string.log_override_group_key)).append(" ").append(sbn.getOverrideGroupKey()).append("\n");

        android.app.Notification notification = sbn.getNotification();
        if (notification != null) {
            sb.append(getString(R.string.log_channel_id)).append(" ").append(notification.getChannelId()).append("\n");
            sb.append(getString(R.string.log_category)).append(" ").append(notification.category).append("\n");
            sb.append(getString(R.string.log_ticker_text)).append(" ").append(notification.tickerText).append("\n");
            sb.append(getString(R.string.log_content_intent)).append(" ").append(notification.contentIntent).append("\n");           
            sb.append(getString(R.string.log_time)).append(" ").append(new java.util.Date(notification.when)).append("\n");
            sb.append(getString(R.string.log_flags)).append(" ").append(notification.flags).append("\n");
            sb.append(getString(R.string.log_priority)).append(" ").append(notification.priority).append("\n");
            
            if (notification.extras != null) {
                sb.append("\n").append(getString(R.string.log_extras_header)).append("\n");
                for (String key : notification.extras.keySet()) {
                    Object value = notification.extras.get(key);
                    sb.append(key).append(": ").append(value).append("\n");
                }
                sb.append(getString(R.string.log_extras_footer)).append("\n");
            }
        }

        NotificationLogManager.getInstance(this).log(sb.toString());
    }
}
