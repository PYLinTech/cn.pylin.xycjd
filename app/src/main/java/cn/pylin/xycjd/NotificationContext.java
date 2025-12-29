package cn.pylin.xycjd;

import android.app.PendingIntent;
import android.service.notification.StatusBarNotification;

/**
 * 通知处理上下文类
 * 封装单次通知处理的所有相关信息
 */
public class NotificationContext {
    // 原始数据
    public StatusBarNotification sbn;
    public String packageName;
    public String key;
    public String title;
    public String content;
    public PendingIntent pendingIntent;
    public android.media.session.MediaSession.Token mediaToken;
    
    // 配置信息
    public AppNotificationConfig config;
    
    // 处理结果
    public boolean shouldFilter;          // 是否应该过滤
    public float modelScore;              // 模型评分
    public String filterReason;           // 过滤原因（用于日志）
    
    public NotificationContext(StatusBarNotification sbn, String title, String content, 
                              PendingIntent pendingIntent, android.media.session.MediaSession.Token mediaToken) {
        this.sbn = sbn;
        this.packageName = sbn.getPackageName();
        this.key = sbn.getKey();
        this.title = title;
        this.content = content;
        this.pendingIntent = pendingIntent;
        this.mediaToken = mediaToken;
        this.config = new AppNotificationConfig();
        this.shouldFilter = false;
        this.modelScore = 10.0f; // 默认满分
        this.filterReason = "";
    }

    /**
     * 判断是否为媒体通知
     */
    public boolean isMediaNotification() {
        return config.isMedia;
    }
}
