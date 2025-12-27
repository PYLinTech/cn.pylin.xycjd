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
    private static final String PREFS_NOTIFICATION_VIBRATION_NAME = "app_notification_vibration";
    private static final String PREFS_NOTIFICATION_SOUND_NAME = "app_notification_sound";
    
    private static final String PREF_NOTIFICATION_MODE = "pref_notification_mode";
    private static final String MODE_SUPER_ISLAND_ONLY = "mode_super_island_only";
    private static final String MODE_NOTIFICATION_BAR_ONLY = "mode_notification_bar_only";
    private static final String MODE_BOTH = "mode_both";
    
    // 用于跟踪已提醒过的通知key
    private static java.util.Set<String> remindedKeys = new java.util.HashSet<>();

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
            String mode = SharedPreferencesManager.getInstance(this).getNotificationMode();

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
             String mode = SharedPreferencesManager.getInstance(this).getNotificationMode();
             
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
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        return manager.isAppEnabled(packageName);
    }

    private boolean isAppModelFilterSelected(String packageName) {
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        return manager.isAppModelFilterEnabled(packageName);
    }

    private boolean isAppAutoExpandSelected(String packageName) {
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        return manager.isAppAutoExpandEnabled(packageName);
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
        String predictionText = (text != null ? text : "");

        // 1. 准备基础环境
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        String mode = manager.getNotificationMode();
        
        // 检查是否为媒体通知
        String template = extras.getString(android.app.Notification.EXTRA_TEMPLATE);
        boolean isMediaNotification = template != null && (template.contains("MediaStyle") || template.contains("media"));
        android.media.session.MediaSession.Token token = null;
        if (isMediaNotification) {
            token = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION);
        }
        final android.media.session.MediaSession.Token finalToken = token;

        // 2. 处理震动和声音（不再限制仅超级岛模式）
        handleVibrationAndSound(sbn);

        // 3. 确定模型策略 (根据模型类型优先判断)
        boolean isModelFilteringEnabled = manager.isModelFilteringEnabled();
        boolean isSelectedApp = isAppModelFilterSelected(packageName);
        String modelType = manager.getFilterModel();
        
        // 定义具体的策略标志
        boolean useLocalModel = isModelFilteringEnabled && isSelectedApp && !isMediaNotification && SettingsFragment.MODEL_LOCAL.equals(modelType);
        boolean useOnlineModel = isModelFilteringEnabled && isSelectedApp && !isMediaNotification && SettingsFragment.MODEL_ONLINE.equals(modelType);

        // 4. 执行分流策略
        if (useLocalModel) {
            // 策略 A: 本地模型 -> 阻断式检查 (Check then Show)
            // 防止闪烁，只有检查通过后才显示
            new Thread(() -> {
                float score = NotificationMLManager.getInstance(this).predict(title, predictionText);
                float filteringDegree = manager.getFilteringDegree();
                
                boolean shouldFilter = score <= filteringDegree;
                String resultStr = shouldFilter ? getString(R.string.log_result_filtered) : getString(R.string.log_result_allowed);
                String logMsg = getString(R.string.log_local_check, title, score, filteringDegree, resultStr, predictionText);
                NotificationLogManager.getInstance().log(logMsg);
                
                if (shouldFilter) {
                    new Handler(Looper.getMainLooper()).post(() -> applyFilter(sbn, title, predictionText));
                } else {
                    // 允许显示
                    showNotificationInIsland(sbn, packageName, title, text, notification.contentIntent, finalToken, mode);
                }
            }).start();

        } else if (useOnlineModel) {
            // 策略 B: 在线模型 -> 乐观展示 + 异步检查 (Show then Check)
            // 网络请求较慢，为了体验先显示，后续如果判断为垃圾则移除
            
            // 先显示
            showNotificationInIsland(sbn, packageName, title, text, notification.contentIntent, finalToken, mode);

            // 后检查
            OnlineModelManager.getInstance(this).checkFilter(title, predictionText, (shouldFilter, score) -> {
                // 获取过滤程度用于日志记录
                float filteringDegree = manager.getOnlineFilteringDegree();
                String resultStr = shouldFilter ? getString(R.string.log_result_filtered) : getString(R.string.log_result_allowed);
                String logMsg = getString(R.string.log_online_check, title, score, filteringDegree, resultStr, predictionText);
                NotificationLogManager.getInstance().log(logMsg);

                if (shouldFilter) {
                    applyFilter(sbn, title, predictionText);
                }
            });

        } else {
            // 策略 C: 无模型/媒体/未启用 -> 直接显示 (Direct Show)
            showNotificationInIsland(sbn, packageName, title, text, notification.contentIntent, finalToken, mode);
        }
    }

    /**
     * 处理震动和声音
     * 对于同一key只提醒第一次
     * 只使用应用级别的设置
     */
    private void handleVibrationAndSound(StatusBarNotification sbn) {
        String key = sbn.getKey();
        String packageName = sbn.getPackageName();
        
        // 检查是否已经提醒过
        if (remindedKeys.contains(key)) {
            return;
        }
        
        // 获取应用级别的震动和声音设置
        boolean appVibration = isAppVibrationEnabled(packageName);
        boolean appSound = isAppSoundEnabled(packageName);
        
        // 检查是否有任何需要执行的提醒
        if (appVibration || appSound) {
            // 标记为已提醒
            remindedKeys.add(key);
            
            // 在主线程执行震动和声音
            new Handler(Looper.getMainLooper()).post(() -> {
                android.media.AudioManager audioManager = (android.media.AudioManager) getSystemService(Context.AUDIO_SERVICE);
                
                if (appVibration) {
                    // 震动
                    android.os.Vibrator vibrator = (android.os.Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
                    if (vibrator != null && vibrator.hasVibrator()) {
                        vibrator.vibrate(200); // 震动200毫秒
                    }
                }
                
                if (appSound) {
                    // 播放默认通知声音
                    if (audioManager != null && audioManager.getRingerMode() == android.media.AudioManager.RINGER_MODE_NORMAL) {
                        try {
                            android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(
                                this, 
                                android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                            );
                            if (ringtone != null) {
                                ringtone.play();
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                }
            });
        }
    }

    /**
     * 检查应用是否启用了震动
     */
    private boolean isAppVibrationEnabled(String packageName) {
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        return manager.isAppNotificationVibrationEnabled(packageName);
    }

    /**
     * 检查应用是否启用了声音
     */
    private boolean isAppSoundEnabled(String packageName) {
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(this);
        return manager.isAppNotificationSoundEnabled(packageName);
    }

    /**
     * 辅助方法：显示通知到灵动岛
     */
    private void showNotificationInIsland(StatusBarNotification sbn, String packageName, String title, String text, android.app.PendingIntent contentIntent, android.media.session.MediaSession.Token token, String mode) {
        if (!MODE_NOTIFICATION_BAR_ONLY.equals(mode)) {
            new Handler(Looper.getMainLooper()).post(() -> {
                FloatingWindowService service = FloatingWindowService.getInstance();
                if (service != null) {
                    service.addNotification(sbn.getKey(), packageName, title, text, contentIntent, token);
                }
            });
        }
    }
    
    private void applyFilter(StatusBarNotification sbn, String title, String text) {
        // 将过滤的通知存入列表
        FilteredNotificationManager.getInstance(this).addNotification(
            sbn.getKey(), 
            sbn.getPackageName(), 
            title, 
            text
        );
        
        // 从超级岛移除
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.removeNotification(sbn.getKey());
        }
        
        // 从系统通知栏移除
        cancelNotification(sbn.getKey());
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
