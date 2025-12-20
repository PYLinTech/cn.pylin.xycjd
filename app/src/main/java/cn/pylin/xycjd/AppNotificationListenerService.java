package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;

public class AppNotificationListenerService extends NotificationListenerService {
    
    private static final String PREFS_NAME = "app_checkboxes";
    private static final String PREFS_MODEL_FILTER_NAME = "app_model_filter";
    private static final String PREFS_AUTO_EXPAND_NAME = "app_auto_expand";
    
    private static final String PREF_NOTIFICATION_MODE = "pref_notification_mode";
    private static final String MODE_SUPER_ISLAND_ONLY = "mode_super_island_only";
    private static final String MODE_NOTIFICATION_BAR_ONLY = "mode_notification_bar_only";
    private static final String MODE_BOTH = "mode_both";

    private static AppNotificationListenerService instance;

    public static AppNotificationListenerService getInstance() {
        return instance;
    }

    @Override
    public void onListenerConnected() {
        super.onListenerConnected();
        instance = this;
    }

    @Override
    public void onListenerDisconnected() {
        super.onListenerDisconnected();
        instance = null;
    }
    
    @Override
    public void onNotificationPosted(StatusBarNotification sbn) {
        super.onNotificationPosted(sbn);
        
        String packageName = sbn.getPackageName();
        boolean isSelected = isAppSelected(packageName);
        
        logNotification(sbn, isSelected);
        
        if (isSelected) {
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
            String mode = prefs.getString(PREF_NOTIFICATION_MODE, MODE_SUPER_ISLAND_ONLY);

            if (MODE_NOTIFICATION_BAR_ONLY.equals(mode)) {
                return;
            }

            if (MODE_SUPER_ISLAND_ONLY.equals(mode)) {
                cancelNotification(sbn.getKey());
            }

            handleNotification(sbn);

            if (isAppAutoExpandSelected(packageName)) {
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    FloatingWindowService service = FloatingWindowService.getInstance();
                    if (service != null) {
                        service.performThreeCircleClick();
                    }
                }, 300);
            }
        }
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn) {
        super.onNotificationRemoved(sbn);
        handleNotificationRemoved(sbn);
    }
    
    @Override
    public void onNotificationRemoved(StatusBarNotification sbn, NotificationListenerService.RankingMap rankingMap, int reason) {
        String packageName = sbn.getPackageName();
        if (isAppSelected(packageName)) {
             SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
             String mode = prefs.getString(PREF_NOTIFICATION_MODE, MODE_SUPER_ISLAND_ONLY);
             
             if (MODE_SUPER_ISLAND_ONLY.equals(mode)) {
                 // REASON_LISTENER_CANCEL = 10 (Listener cancelled it)
                 if (reason == 10) {
                     return;
                 }
             }
        }
        super.onNotificationRemoved(sbn, rankingMap, reason);
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

    private boolean isAppModelFilterSelected(String packageName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_MODEL_FILTER_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(packageName, false);
    }

    private boolean isAppAutoExpandSelected(String packageName) {
        SharedPreferences prefs = getSharedPreferences(PREFS_AUTO_EXPAND_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(packageName, false);
    }
    
    /**
     * 处理选中的应用通知
     * @param sbn 状态栏通知
     */
    private void handleNotification(StatusBarNotification sbn) {
        String packageName = sbn.getPackageName();
        android.app.Notification notification = sbn.getNotification();
        android.os.Bundle extras = notification.extras;

        String title = extras.getString("android.title");
        String text = extras.getString("android.text");

        // 智能过滤：使用本地机器学习模型评分
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        boolean isLocalLearningEnabled = prefs.getBoolean("pref_local_learning_enabled", false);

        if (isLocalLearningEnabled && isAppModelFilterSelected(packageName)) {
            String predictionText = (text != null ? text : "");
            float score = NotificationMLManager.getInstance(this).predict(title, predictionText);
            
            // 获取过滤程度设置
            float filteringDegree = prefs.getFloat("pref_filtering_degree", 5.0f);
            
            if (score < filteringDegree) {
                // 将过滤的通知存入列表
                FilteredNotificationManager.getInstance(this).addNotification(
                    sbn.getKey(), 
                    packageName, 
                    title, 
                    text
                );
                return;
            }
        }

        // 检查是否为媒体通知
        String template = extras.getString(android.app.Notification.EXTRA_TEMPLATE);
        android.media.session.MediaSession.Token token = null;
        if (template != null && (template.contains("MediaStyle") || template.contains("media"))) {
            token = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION);
        }
        
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.addNotification(sbn.getKey(), packageName, title, text, notification.contentIntent, token);
        }
    }
    
    /**
     * 处理选中的应用通知移除
     * @param sbn 状态栏通知
     */
    private void handleNotificationRemoved(StatusBarNotification sbn) {
        // 移除通知
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.removeNotification(sbn.getKey());
        }
    }

    private void logNotification(StatusBarNotification sbn, boolean isSelected) {
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(sbn.getPackageName()).append("\n");
        sb.append("ID: ").append(sbn.getId()).append("\n");
        sb.append("Tag: ").append(sbn.getTag()).append("\n");
        sb.append("PostTime: ").append(new java.util.Date(sbn.getPostTime())).append("\n");
        sb.append("IsClearable: ").append(sbn.isClearable()).append("\n");
        sb.append("IsOngoing: ").append(sbn.isOngoing()).append("\n");
        sb.append("Selected: ").append(isSelected).append("\n");
        sb.append("Key: ").append(sbn.getKey()).append("\n");
        sb.append("GroupKey: ").append(sbn.getGroupKey()).append("\n");
        sb.append("OverrideGroupKey: ").append(sbn.getOverrideGroupKey()).append("\n");

        android.app.Notification notification = sbn.getNotification();
        if (notification != null) {
            sb.append("ChannelId: ").append(notification.getChannelId()).append("\n");
            sb.append("Category: ").append(notification.category).append("\n");
            sb.append("Ticker: ").append(notification.tickerText).append("\n");
            sb.append("ContentIntent: ").append(notification.contentIntent).append("\n");           
            sb.append("When: ").append(new java.util.Date(notification.when)).append("\n");
            sb.append("Flags: ").append(notification.flags).append("\n");
            sb.append("Priority: ").append(notification.priority).append("\n");
            
            if (notification.extras != null) {
                sb.append("\n--- Extras ---\n");
                for (String key : notification.extras.keySet()) {
                    Object value = notification.extras.get(key);
                    sb.append(key).append(": ").append(value).append("\n");
                }
                sb.append("----------------\n");
            }
        }

        NotificationLogManager.getInstance().log(sb.toString());
    }
}
