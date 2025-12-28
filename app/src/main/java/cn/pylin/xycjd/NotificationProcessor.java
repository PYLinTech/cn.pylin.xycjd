package cn.pylin.xycjd;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * 通知处理器
 * 负责所有通知的接收、配置读取、过滤和执行逻辑
 */
public class NotificationProcessor {
    private static final String TAG = "NotificationProcessor";
    
    private Context context;
    private SharedPreferencesManager prefsManager;
    private Handler mainHandler;
    
    // 用于跟踪已提醒过的通知key（避免重复震动/声音）
    private static java.util.Set<String> remindedKeys = new java.util.HashSet<>();
    
    public NotificationProcessor(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = SharedPreferencesManager.getInstance(context);
        this.mainHandler = new Handler(Looper.getMainLooper());
    }
    
    /**
     * 主入口：处理通知
     */
    public void processNotification(StatusBarNotification sbn) {
        if (sbn == null) return;
        
        String packageName = sbn.getPackageName();
        
        // 1. 读取配置并构建上下文
        NotificationContext context = buildContext(sbn);
        
        // 2. 预处理检查
        if (!preProcessCheck(context)) {
            return;
        }
        
        // 3. 执行模式逻辑
        executeByMode(context);
    }
    
    /**
     * 步骤1：构建处理上下文
     */
    private NotificationContext buildContext(StatusBarNotification sbn) {
        android.app.Notification notification = sbn.getNotification();
        android.os.Bundle extras = notification.extras;
        
        String title = extras.getString("android.title");
        String text = extras.getString("android.text");
        String packageName = sbn.getPackageName();
        
        // 获取PendingIntent和媒体Token
        PendingIntent pendingIntent = notification.contentIntent;
        android.media.session.MediaSession.Token mediaToken = null;
        
        // 检查是否为媒体通知
        String template = extras.getString(android.app.Notification.EXTRA_TEMPLATE);
        boolean isMedia = template != null && (template.contains("MediaStyle") || template.contains("media"));
        if (isMedia) {
            mediaToken = extras.getParcelable(android.app.Notification.EXTRA_MEDIA_SESSION);
        }
        
        // 创建上下文
        NotificationContext context = new NotificationContext(sbn, title, text, pendingIntent, mediaToken);
        
        // 填充配置信息
        fillAppConfig(context, packageName);
        
        return context;
    }
    
    /**
     * 步骤2：填充应用配置
     */
    private void fillAppConfig(NotificationContext context, String packageName) {
        AppNotificationConfig config = context.config;
        
        // 基础配置
        config.enabled = prefsManager.isAppEnabled(packageName);
        config.isMedia = context.isMediaNotification();
        
        // 检查是否已存在（通过队列检查）
        config.isExisting = checkIfNotificationExists(context.key);
        
        // 模式配置
        config.appMode = prefsManager.getNotificationMode();
        
        // 模型过滤配置
        config.modelFilterEnabled = prefsManager.isModelFilteringEnabled() && 
                                   prefsManager.isAppModelFilterEnabled(packageName);
        config.modelType = prefsManager.getFilterModel();
        
        // 行为配置
        config.autoExpand = prefsManager.isAppAutoExpandEnabled(packageName);
        config.vibration = prefsManager.isAppNotificationVibrationEnabled(packageName);
        config.sound = prefsManager.isAppNotificationSoundEnabled(packageName);
    }
    
    /**
     * 步骤3：预处理检查
     * 返回true继续处理，false终止处理
     */
    private boolean preProcessCheck(NotificationContext context) {
        // 1. 检查是否启用
        if (!context.config.enabled) {
            return false;
        }
        
        // 2. 检查是否已存在（存在则更新，不向后执行）
        if (context.config.isExisting) {
            updateExistingNotification(context);
            return false;
        }
        
        // 3. 媒体通知特殊处理（媒体通知不进行模型过滤，但不执行行为）
        if (context.config.isMedia) {
            // 媒体通知只更新内容，不执行行为
            updateExistingNotification(context);
            return false;
        }
        
        return true;
    }
    
    /**
     * 步骤4：根据模式执行逻辑
     */
    private void executeByMode(NotificationContext context) {
        String mode = context.config.appMode;
        
        // 执行模型过滤（如果需要）
        if (context.config.needsModelFiltering()) {
            applyModelFiltering(context);
        }
        
        // 如果被过滤，则不执行显示逻辑，但仍可能执行行为（根据需求调整）
        if (context.shouldFilter) {
            handleFilteredNotification(context);
            return;
        }
        
        // 根据模式执行不同逻辑
        if (mode.equals("mode_super_island_only")) {
            executeSuperIslandMode(context);
        } else if (mode.equals("mode_notification_bar_only")) {
            executeNotificationBarMode(context);
        } else if (mode.equals("mode_both")) {
            executeBothMode(context);
        }
    }
    
    /**
     * 模式：仅超级岛
     */
    private void executeSuperIslandMode(NotificationContext context) {
        // 显示到超级岛
        if (context.config.shouldShowIsland()) {
            showInIsland(context);
        }
        
        // 删除系统通知
        if (context.config.shouldRemoveSystemNotification()) {
            removeFromSystem(context);
        }
        
        // 执行行为
        executeBehaviors(context);
    }
    
    /**
     * 模式：仅通知栏
     */
    private void executeNotificationBarMode(NotificationContext context) {
        // 不显示超级岛，仅保留系统通知
        // 执行行为（如果需要）
        executeBehaviors(context);
    }
    
    /**
     * 模式：双显
     */
    private void executeBothMode(NotificationContext context) {
        // 显示到超级岛
        if (context.config.shouldShowIsland()) {
            showInIsland(context);
        }
        
        // 不删除系统通知（默认行为）
        
        // 执行行为
        executeBehaviors(context);
    }
    
    /**
     * 步骤5：应用模型过滤
     */
    private void applyModelFiltering(NotificationContext context) {
        if (context.config.modelType.equals("model_local")) {
            // 本地模型：同步执行
            float score = NotificationMLManager.getInstance(this.context).predict(
                context.title, context.content);
            context.modelScore = score;
            
            float filteringDegree = prefsManager.getFilteringDegree();
            context.shouldFilter = score <= filteringDegree;
            
            String result = context.shouldFilter ? this.context.getString(R.string.log_result_filtered) : this.context.getString(R.string.log_result_allowed);
            logModelCheck(context, this.context.getString(R.string.log_model_local), score, filteringDegree, result);
            
        } else if (context.config.modelType.equals("model_online")) {
            // 在线模型：异步执行
            applyOnlineModelFiltering(context);
        }
    }
    
    /**
     * 在线模型过滤（异步）
     */
    private void applyOnlineModelFiltering(NotificationContext context) {
        OnlineModelManager.getInstance(this.context).checkFilter(
            context.title, 
            context.content, 
            (shouldFilter, score) -> {
                context.modelScore = score;
                context.shouldFilter = shouldFilter;
                
                float filteringDegree = prefsManager.getOnlineFilteringDegree();
                String result = shouldFilter ? this.context.getString(R.string.log_result_filtered) : this.context.getString(R.string.log_result_allowed);
                logModelCheck(context, this.context.getString(R.string.log_model_online), score, filteringDegree, result);
                
                // 在线模型过滤完成后，根据结果执行后续逻辑
                if (shouldFilter) {
                    handleFilteredNotification(context);
                } else {
                    // 继续执行显示逻辑
                    continueExecutionAfterOnlineFilter(context);
                }
            }
        );
    }
    
    /**
     * 在线模型过滤后继续执行
     */
    private void continueExecutionAfterOnlineFilter(NotificationContext context) {
        mainHandler.post(() -> {
            // 重新执行模式逻辑（跳过过滤步骤）
            String mode = context.config.appMode;
            if (mode.equals("mode_super_island_only")) {
                executeSuperIslandMode(context);
            } else if (mode.equals("mode_both")) {
                executeBothMode(context);
            }
            // mode_notification_bar_only 不需要特殊处理
        });
    }
    
    /**
     * 步骤6：执行行为（震动、声音、自动展开）
     */
    private void executeBehaviors(NotificationContext context) {
        // 避免重复提醒
        if (remindedKeys.contains(context.key)) {
            return;
        }
        
        // 检查是否有任何行为需要执行
        if (context.config.vibration || context.config.sound || context.config.autoExpand) {
            remindedKeys.add(context.key);
            
            mainHandler.post(() -> {
                // 震动
                if (context.config.vibration) {
                    performVibration();
                    logBehavior(context, this.context.getString(R.string.log_behavior_vibration));
                }
                
                // 声音
                if (context.config.sound) {
                    playNotificationSound();
                    logBehavior(context, this.context.getString(R.string.log_behavior_sound));
                }
                
                // 自动展开
                if (context.config.autoExpand) {
                    performAutoExpand();
                    logBehavior(context, this.context.getString(R.string.log_behavior_expand));
                }
            });
        }
    }
    
    /**
     * 处理被过滤的通知
     */
    private void handleFilteredNotification(NotificationContext context) {
        // 添加到过滤列表
        FilteredNotificationManager.getInstance(this.context).addNotification(
            context.key, context.packageName, context.title, context.content);
        
        // 从超级岛移除（如果已显示）
        removeFromIsland(context);
        
        // 从系统通知栏移除
        removeFromSystem(context);
    }
    
    /**
     * 更新已存在的通知
     * 只更新内容，不执行任何动画或行为
     */
    private void updateExistingNotification(NotificationContext context) {
        // 更新超级岛中的内容（无动画）
        updateIslandContentNoAnimation(context);
        
        // 如果是超级岛模式，删除系统通知
        if (context.config.shouldRemoveSystemNotification()) {
            removeFromSystem(context);
        }
    }
    
    /**
     * 更新超级岛内容（无动画版本）
     */
    private void updateIslandContentNoAnimation(NotificationContext context) {
        mainHandler.post(() -> {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                // 直接更新队列中的数据
                java.util.LinkedList<FloatingWindowService.NotificationInfo> queue = service.getNotificationQueue();
                for (FloatingWindowService.NotificationInfo info : queue) {
                    if (info.key.equals(context.key)) {
                        info.title = context.title;
                        info.content = context.content;
                        info.pendingIntent = context.pendingIntent;
                        info.mediaToken = context.mediaToken;
                        break;
                    }
                }
                
                // 如果超级岛正在显示，直接更新UI内容（无动画）
                if (service.floatingIslandView != null && service.floatingIslandView.getParent() != null) {
                    // 通过反射调用私有方法 updateNotificationContent
                    try {
                        java.lang.reflect.Method updateMethod = FloatingWindowService.class.getDeclaredMethod(
                            "updateNotificationContent", String.class, String.class, String.class);
                        updateMethod.setAccessible(true);
                        updateMethod.invoke(service, context.packageName, context.title, context.content);
                    } catch (Exception e) {
                        // 如果反射失败，使用adapter.notifyDataSetChanged()
                        if (service.notificationAdapter != null) {
                            service.notificationAdapter.notifyDataSetChanged();
                        }
                    }
                }
            }
        });
    }
    
    // ==================== 辅助方法 ====================
    
    /**
     * 检查通知是否已存在
     */
    private boolean checkIfNotificationExists(String key) {
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            java.util.LinkedList<FloatingWindowService.NotificationInfo> queue = service.getNotificationQueue();
            for (FloatingWindowService.NotificationInfo info : queue) {
                if (info.key.equals(key)) {
                    return true;
                }
            }
        }
        return false;
    }
    
    /**
     * 显示到超级岛
     */
    private void showInIsland(NotificationContext context) {
        mainHandler.post(() -> {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                service.addNotification(
                    context.key, 
                    context.packageName, 
                    context.title, 
                    context.content, 
                    context.pendingIntent, 
                    context.mediaToken);
            }
        });
    }
    
    /**
     * 从超级岛移除
     */
    private void removeFromIsland(NotificationContext context) {
        mainHandler.post(() -> {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                service.removeNotification(context.key);
            }
        });
    }
    
    /**
     * 更新超级岛内容
     */
    private void updateIslandContent(NotificationContext context) {
        mainHandler.post(() -> {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                // 先移除再添加，实现更新
                service.removeNotification(context.key);
                service.addNotification(
                    context.key, 
                    context.packageName, 
                    context.title, 
                    context.content, 
                    context.pendingIntent, 
                    context.mediaToken);
            }
        });
    }
    
    /**
     * 从系统通知栏移除
     */
    private void removeFromSystem(NotificationContext context) {
        AppNotificationListenerService listenerService = AppNotificationListenerService.getInstance();
        if (listenerService != null) {
            try {
                listenerService.cancelNotification(context.key);
            } catch (Exception e) {
                Log.e(TAG, "Failed to cancel system notification", e);
            }
        }
    }
    
    /**
     * 执行震动
     */
    private void performVibration() {
        android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            vibrator.vibrate(200); // 震动200毫秒
        }
    }
    
    /**
     * 播放通知声音
     */
    private void playNotificationSound() {
        android.media.AudioManager audioManager = (android.media.AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        if (audioManager != null && audioManager.getRingerMode() == android.media.AudioManager.RINGER_MODE_NORMAL) {
            try {
                android.media.Ringtone ringtone = android.media.RingtoneManager.getRingtone(
                    context, 
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION)
                );
                if (ringtone != null) {
                    ringtone.play();
                }
            } catch (Exception e) {
                Log.e(TAG, "Failed to play notification sound", e);
            }
        }
    }
    
    /**
     * 执行自动展开
     */
    private void performAutoExpand() {
        mainHandler.postDelayed(() -> {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                service.performThreeCircleClick();
            }
        }, 300);
    }
    
    /**
     * 日志：行为执行
     */
    private void logBehavior(NotificationContext context, String behavior) {
        StringBuilder sb = new StringBuilder();
        sb.append("[").append(context.packageName).append("] ").append(behavior).append("\n");
        NotificationLogManager.getInstance(this.context).log(sb.toString());
    }

    /**
     * 日志：模型检查
     */
    private void logModelCheck(NotificationContext context, String modelType, float score, float threshold, String result) {
        StringBuilder sb = new StringBuilder();
        String modelTypeName = modelType.equals(this.context.getString(R.string.log_model_local)) ? this.context.getString(R.string.log_model_local) : this.context.getString(R.string.log_model_online);
        sb.append("\n").append(String.format(this.context.getString(R.string.log_model_check_header), modelTypeName)).append("\n");
        sb.append(this.context.getString(R.string.log_package_name_colon)).append(" ").append(context.packageName).append("\n");
        sb.append(this.context.getString(R.string.log_score_colon)).append(" ").append(String.format("%.2f", score)).append("\n");
        sb.append(this.context.getString(R.string.log_threshold_colon)).append(" ").append(String.format("%.2f", threshold)).append("\n");
        sb.append(this.context.getString(R.string.log_result_colon)).append(" ").append(result).append("\n");
        sb.append(this.context.getString(R.string.log_title_colon)).append(" ").append(context.title).append("\n");
        sb.append(this.context.getString(R.string.log_content_colon)).append(" ").append(context.content).append("\n");
        sb.append(this.context.getString(R.string.log_extras_footer)).append("\n");
        
        NotificationLogManager.getInstance(this.context).log(sb.toString());
    }
}
