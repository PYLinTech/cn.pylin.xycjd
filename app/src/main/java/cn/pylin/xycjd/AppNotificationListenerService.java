package cn.pylin.xycjd;

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

    public static AppNotificationListenerService getInstance() {
        return instance;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
        // 初始化处理器
        processor = new NotificationProcessor(this);
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        instance = null;
        processor = null;
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
