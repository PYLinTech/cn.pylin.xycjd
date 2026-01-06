package cn.pylin.xycjd.service;

import android.accessibilityservice.AccessibilityService;
import android.accessibilityservice.AccessibilityServiceInfo;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.view.accessibility.AccessibilityEvent;
import androidx.core.app.NotificationCompat;

import cn.pylin.xycjd.R;
import cn.pylin.xycjd.ui.activity.IntroActivity;

public class AppAccessibilityService extends AccessibilityService {

    private static AppAccessibilityService instance;

    // 待执行的PendingIntent信息
    private PendingIntent pendingIntentToExecute;
    private String pendingIntentKey;
    private String pendingIntentPackage;

    // 超时机制
    private Handler timeoutHandler;
    private Runnable timeoutRunnable;
    private static final long TIMEOUT_MS = 2000; // 2秒超时

    public static AppAccessibilityService getInstance() {
        return instance;
    }

    /**
     * 准备PendingIntent，待检测到目标APP在前台时执行
     * 如果2秒内没有检测到窗口变化，将强制执行PendingIntent作为兜底
     */
    public void preparePendingIntent(String key, PendingIntent pendingIntent, String packageName) {
        this.pendingIntentKey = key;
        this.pendingIntentToExecute = pendingIntent;
        this.pendingIntentPackage = packageName;

        // 设置超时任务：2秒后强制执行PendingIntent
        if (timeoutHandler != null) {
            // 取消之前的超时任务（如果有）
            if (timeoutRunnable != null) {
                timeoutHandler.removeCallbacks(timeoutRunnable);
            }

            // 创建新的超时任务
            timeoutRunnable = new Runnable() {
                @Override
                public void run() {
                    executePendingIntentWithTimeout();
                }
            };

            // 2秒后执行
            timeoutHandler.postDelayed(timeoutRunnable, TIMEOUT_MS);
        }
    }

    @Override
    protected void onServiceConnected() {
        super.onServiceConnected();
        instance = this;

        // 初始化Handler用于超时机制
        timeoutHandler = new Handler(Looper.getMainLooper());

        // 配置监听窗口状态变化
        AccessibilityServiceInfo info = new AccessibilityServiceInfo();
        info.eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED;
        info.feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC;
        info.notificationTimeout = 100;
        setServiceInfo(info);

        // 如果悬浮窗服务正在运行，通知其重新创建悬浮窗以提升层级
        FloatingWindowService floatingService = FloatingWindowService.getInstance();
        if (floatingService != null) {
            floatingService.recreateWindow();
        }
    }

    @Override
    public boolean onUnbind(android.content.Intent intent) {
        instance = null;
        // 权限被关闭时，通知悬浮窗服务降级
        FloatingWindowService floatingService = FloatingWindowService.getInstance();
        if (floatingService != null) {
            floatingService.recreateWindow();
        }
        // 发送通知提醒用户
        sendDowngradeNotification();
        return super.onUnbind(intent);
    }
    
    private void sendDowngradeNotification() {
        String channelId = "accessibility_status_channel";
        NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                channelId,
                getString(R.string.accessibility_channel_name),
                NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription(getString(R.string.accessibility_channel_desc));
            notificationManager.createNotificationChannel(channel);
        }

        Intent intent = new Intent(this, IntroActivity.class);
        intent.putExtra("direct_to_permission", true);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.accessibility_downgraded_title))
            .setContentText(getString(R.string.accessibility_downgraded_content))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true);

        notificationManager.notify(1001, builder.build());
    }

    @Override
    public void onAccessibilityEvent(AccessibilityEvent event) {
        if (event.getEventType() == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
            String packageName = event.getPackageName().toString();

            // 检查是否跳转到目标APP
            if (packageName.equals(pendingIntentPackage) && pendingIntentToExecute != null) {
                // 取消超时任务
                if (timeoutHandler != null && timeoutRunnable != null) {
                    timeoutHandler.removeCallbacks(timeoutRunnable);
                }

                // 执行PendingIntent
                try {
                    pendingIntentToExecute.send();
                    // 成功：从超级岛移除通知
                    FloatingWindowService.getInstance().removeNotification(pendingIntentKey);
                } catch (PendingIntent.CanceledException e) {
                    // 失败：保留通知在超级岛中
                }

                // 清理
                pendingIntentToExecute = null;
                pendingIntentKey = null;
                pendingIntentPackage = null;
                timeoutRunnable = null;
            }
        }
    }

    /**
     * 超时后强制执行PendingIntent（兜底机制）
     */
    private void executePendingIntentWithTimeout() {
        if (pendingIntentToExecute != null) {
            try {
                pendingIntentToExecute.send();
                // 成功：从超级岛移除通知
                FloatingWindowService.getInstance().removeNotification(pendingIntentKey);
            } catch (PendingIntent.CanceledException e) {
                // 失败：保留通知在超级岛中
            }

            // 清理
            pendingIntentToExecute = null;
            pendingIntentKey = null;
            pendingIntentPackage = null;
        }
        timeoutRunnable = null;
    }

    @Override
    public void onInterrupt() {
        // 清理待执行的PendingIntent
        pendingIntentToExecute = null;
        pendingIntentKey = null;
        pendingIntentPackage = null;

        // 清理超时任务
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;

        // 清理超时任务
        if (timeoutHandler != null && timeoutRunnable != null) {
            timeoutHandler.removeCallbacks(timeoutRunnable);
            timeoutHandler = null;
        }
    }
}
