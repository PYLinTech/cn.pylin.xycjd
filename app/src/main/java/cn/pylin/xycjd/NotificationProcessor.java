package cn.pylin.xycjd;

import android.app.PendingIntent;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.service.notification.StatusBarNotification;
import android.util.Log;

/**
 * 通知处理器 - 重构版本
 * 逻辑流程：
 * 1. 接收通知
 * 2. 前置检查（空消息、媒体、已经在超级岛的重复通知）
 * 3. 逻辑匹配（显示逻辑与模型过滤逻辑）
 * 4. 执行方法（自动展开、声音、震动）
 */
public class NotificationProcessor {
    private static final String TAG = "NotificationProcessor";
    
    private Context context;
    private SharedPreferencesManager prefsManager;
    private Handler mainHandler;
    
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
        
        // 步骤1：接收通知并构建上下文
        NotificationContext notificationContext = buildContext(sbn);
        if (notificationContext == null) {
            // 前置检查失败：空消息
            return;
        }
        
        // 步骤2：前置检查
        if (!preCheck(notificationContext)) {
            return;
        }
        
        // 步骤3：逻辑匹配（显示逻辑与模型过滤逻辑）
        executeLogicMatching(notificationContext);
    }
    
    /**
     * 步骤1：构建通知上下文
     */
    private NotificationContext buildContext(StatusBarNotification sbn) {
        android.app.Notification notification = sbn.getNotification();
        android.os.Bundle extras = notification.extras;
        
        String title = extras.getString("android.title");
        String text = extras.getString("android.text");
        
        // 前置检查：空消息
        if ((title == null || title.isEmpty()) && (text == null || text.isEmpty())) {
            return null;
        }
        
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
     * 填充应用配置
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
     * 步骤2：前置检查
     * 返回true继续处理，false终止处理
     */
    private boolean preCheck(NotificationContext context) {
        // 1. 检查是否启用
        if (!context.config.enabled) {
            return false;
        }
        
        // 2. 重复通知检查
        if (context.config.isExisting) {
            updateExistingNotification(context);
            return false;
        }
        
        // 3. 媒体通知特殊处理
        if (context.config.isMedia) {
            // 媒体通知：直接显示到超级岛并执行行为，然后终止处理
            if (context.config.appMode.equals("mode_super_island_only")) {
                showInIsland(context);
                executeBehaviors(context);
            }
            return false;
        }
        
        return true;
    }
    
    /**
     * 步骤3：逻辑匹配（先匹配显示逻辑，再执行过滤）
     */
    private void executeLogicMatching(NotificationContext context) {
        String mode = context.config.appMode;
        
        // 根据显示模式执行不同的过滤逻辑
        if (mode.equals("mode_super_island_only")) {
            executeSuperIslandModeWithFilter(context);
        } else if (mode.equals("mode_notification_bar_only")) {
            executeNotificationBarModeWithFilter(context);
        } else if (mode.equals("mode_both")) {
            executeBothModeWithFilter(context);
        }
    }
    
    /**
     * 模式：仅超级岛 + 过滤逻辑
     * 逻辑：先移除系统通知，再过滤决定是否显示
     */
    private void executeSuperIslandModeWithFilter(NotificationContext context) {
        // 1. 先移除系统通知
        removeFromSystem(context);
        
        // 2. 再过滤决定是否显示
        if (context.config.needsModelFiltering()) {
            applyFiltering(context, true); // true = 需要显示超级岛
        } else {
            showInIsland(context);
            executeBehaviors(context);
        }
    }
    
    /**
     * 模式：仅通知栏 + 过滤逻辑
     * 逻辑：过滤决定是否保留系统通知（不执行行为）
     */
    private void executeNotificationBarModeWithFilter(NotificationContext context) {
        // 1. 过滤决定是否保留
        if (context.config.needsModelFiltering()) {
            applyFiltering(context, false); // false = 不显示超级岛
        }
    }
    
    /**
     * 模式：双显 + 过滤逻辑
     * 逻辑：过滤决定是否移除还是显示
     */
    private void executeBothModeWithFilter(NotificationContext context) {
        // 1. 过滤决定是否显示
        if (context.config.needsModelFiltering()) {
            applyFiltering(context, true); // true = 需要显示超级岛
        } else {
            showInIsland(context);
            executeBehaviors(context);
        }
    }
    
    /**
     * 统一的过滤应用方法
     * @param context 通知上下文
     * @param shouldShowIsland 根据显示逻辑决定是否需要在超级岛显示
     */
    private void applyFiltering(NotificationContext context, boolean shouldShowIsland) {
        if (context.config.modelType.equals("model_local")) {
            // 本地模型：异步执行
            applyLocalModelFilteringAsync(context, shouldShowIsland);
        }
        else if (context.config.modelType.equals("model_online")) {
            // 在线模型：根据流程设置决定
            String processMode = prefsManager.getOnlineModelProcessMode();
            if (SharedPreferencesManager.PROCESS_MODE_CHECK_FIRST.equals(processMode)) {
                // 先检查再显示：异步执行，检查后再决定
                applyOnlineModelFilteringSync(context, shouldShowIsland);
            } else {
                // 先显示再检查（默认）：异步执行，先显示，检查后决定是否移除
                applyOnlineModelFilteringAsync(context, shouldShowIsland);
            }
        }
    }
    
    /**
     * 本地模型过滤 - 异步执行
     */
    private void applyLocalModelFilteringAsync(NotificationContext context, boolean shouldShowIsland) {
        // 使用与在线模型一致的接口
        NotificationMLManager.getInstance(this.context).checkFilter(
            context.title, 
            context.content, 
            (shouldFilter, score) -> {
                context.modelScore = score;
                context.shouldFilter = shouldFilter;

                // 只记录日志，不做判断（由调用方根据shouldFilter决定后续操作）
                float filteringDegree = prefsManager.getFilteringDegree();
                String result = shouldFilter ? this.context.getString(R.string.log_result_filtered) : this.context.getString(R.string.log_result_allowed);
                logModelCheck(context, this.context.getString(R.string.log_model_local), score, filteringDegree, result);
                
                // 在主线程中根据结果执行相应逻辑
                mainHandler.post(() -> {
                    if (shouldFilter) {
                        // 需要过滤：移除通知
                        handleFilteredNotification(context);
                    } else {
                        // 不需要过滤：显示通知
                        if (shouldShowIsland) {
                            showInIsland(context);
                            executeBehaviors(context);
                        }
                    }
                });
            }
        );
    }
    
    /**
     * 在线模型过滤 - 先检查再显示（异步）
     */
    private void applyOnlineModelFilteringSync(NotificationContext context, boolean shouldShowIsland) {
        // 异步执行检查，不阻塞主线程
        OnlineModelManager.getInstance(this.context).checkFilter(
            context.title, 
            context.content, 
            (shouldFilter, score) -> {
                context.modelScore = score;
                context.shouldFilter = shouldFilter;
                
                float filteringDegree = prefsManager.getOnlineFilteringDegree();
                String result = shouldFilter ? this.context.getString(R.string.log_result_filtered) : this.context.getString(R.string.log_result_allowed);
                logModelCheck(context, this.context.getString(R.string.log_model_online), score, filteringDegree, result);
                
                // 在主线程中根据检查结果执行相应逻辑
                mainHandler.post(() -> {
                    if (shouldFilter) {
                        // 需要过滤：移除通知
                        handleFilteredNotification(context);
                    } else {
                        // 不需要过滤：显示通知
                        if (shouldShowIsland) {
                            showInIsland(context);
                            executeBehaviors(context);
                        }
                    }
                });
            }
        );
    }
    
    /**
     * 在线模型过滤 - 先显示再检查（异步）
     */
    private void applyOnlineModelFilteringAsync(NotificationContext context, boolean shouldShowIsland) {
        // 先执行显示逻辑
        if (shouldShowIsland) {
            showInIsland(context);
            executeBehaviors(context);
        }
        // 异步检查，如果需要过滤则移除
        OnlineModelManager.getInstance(this.context).checkFilter(
            context.title, 
            context.content, 
            (shouldFilter, score) -> {
                context.modelScore = score;
                context.shouldFilter = shouldFilter;
                
                float filteringDegree = prefsManager.getOnlineFilteringDegree();
                String result = shouldFilter ? this.context.getString(R.string.log_result_filtered) : this.context.getString(R.string.log_result_allowed);
                logModelCheck(context, this.context.getString(R.string.log_model_online), score, filteringDegree, result);
                
                // 如果检查结果是需要过滤，则移除通知
                if (shouldFilter) {
                    handleFilteredNotification(context);
                }
            }
        );
    }
    
    /**
     * 步骤4：执行方法（自动展开、声音、震动）
     */
    private void executeBehaviors(NotificationContext context) {
        // 检查是否有任何行为需要执行
        if (context.config.vibration || context.config.sound || context.config.autoExpand) {
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
     */
    private void updateExistingNotification(NotificationContext context) {
        // 更新超级岛中的内容（无动画）
        updateIslandContentNoAnimation(context);
        // 如果是超级岛模式，删除系统通知
        if (context.config.appMode.equals("mode_super_island_only")) {
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
        // 检查全局震动开关
        if (!prefsManager.isVibrationEnabled()) {
            return;
        }
        
        android.os.Vibrator vibrator = (android.os.Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        if (vibrator != null && vibrator.hasVibrator()) {
            int intensity = prefsManager.getVibrationIntensity();
            vibrator.vibrate(intensity); // 使用设置的震动强度
        }
    }
    
    /**
     * 播放通知声音
     */
    private void playNotificationSound() {
        // 检查全局声音开关
        if (!prefsManager.isSoundEnabled()) {
            return;
        }
        
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
