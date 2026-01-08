package cn.pylin.xycjd.handler;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import java.util.List;
import cn.pylin.xycjd.manager.SharedPreferencesManager;
import cn.pylin.xycjd.model.local.LocalModelManager;
import cn.pylin.xycjd.service.AppAccessibilityService;
import cn.pylin.xycjd.service.FloatingWindowService;
import cn.pylin.xycjd.utils.ShizukuShellHelper;
import androidx.annotation.NonNull;

/**
 * 通知卡片点击处理器
 * 统一处理通知卡片点击后的所有逻辑
 */
public class NotificationClickHandler {

    private final Context context;
    private final SharedPreferencesManager prefsManager;

    public NotificationClickHandler(Context context) {
        this.context = context.getApplicationContext();
        this.prefsManager = SharedPreferencesManager.getInstance(context);
    }

    /**
     * 处理通知卡片点击
     *
     * @param notificationInfo 通知信息
     * @param isDraggingMediaProgress 是否正在拖拽媒体进度条
     */
    public void handleCardClick(NotificationClickInfo notificationInfo, boolean isDraggingMediaProgress) {
        // 如果正在拖拽进度条，不响应卡片点击
        if (isDraggingMediaProgress) {
            return;
        }

        // 处理模型训练反馈（如果启用）
        handleModelFeedback(notificationInfo);

        // 从超级岛移除通知
        FloatingWindowService.getInstance().removeNotification(notificationInfo.getKey());

        // 启动目标APP
        launchTargetApp(notificationInfo);
    }

    /**
     * 处理模型反馈（正向反馈到本地模型）
     */
    private void handleModelFeedback(NotificationClickInfo notificationInfo) {
        // 条件：总过滤开启 + 包名过滤开启 + 是本地模型
        if (prefsManager.isModelFilteringEnabled()
                && isModelFilterEnabled(notificationInfo.getPackageName())
                && prefsManager.getFilterModel().equals("model_local")) {

            // 正向反馈到本地模型 - 参数true表示正向
            String trainingText = notificationInfo.getTitle() != null ? notificationInfo.getContent() : "";
            LocalModelManager.getInstance(context).process(
                    notificationInfo.getTitle(),
                    trainingText,
                    true
            );
        }
    }

    /**
     * 启动目标APP，根据点击响应模式选择不同的启动方式
     */
    private void launchTargetApp(NotificationClickInfo notificationInfo) {
        String clickMode = prefsManager.getClickResponseMode();

        if (SharedPreferencesManager.CLICK_MODE_COMPATIBILITY.equals(clickMode)) {
            // 兼容模式：先打开APP，无障碍检测页面，再跳转（默认）
            launchTargetAppCompatibility(notificationInfo);
        } else if (SharedPreferencesManager.CLICK_MODE_SHIZUKU.equals(clickMode)) {
            // Shizuku模式：使用Shizuku方式启动
            launchTargetAppShizuku(notificationInfo);
        } else {
            // 标准模式：直接跳转，不经过无障碍
            launchTargetAppStandard(notificationInfo);
        }
    }

    /**
     * 标准模式：直接执行PendingIntent或拉起对应APP
     */
    private void launchTargetAppStandard(NotificationClickInfo notificationInfo) {
        // 直接执行PendingIntent
        try {
            if (notificationInfo.getPendingIntent() != null) {
                notificationInfo.getPendingIntent().send();
            }
        } catch (PendingIntent.CanceledException e) {
            // 如果PendingIntent失效，尝试直接启动APP
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(notificationInfo.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
        }
    }

    /**
     * 兼容模式：普通方法打开APP，无障碍检测是否成功拉起，再执行PendingIntent
     */
    private void launchTargetAppCompatibility(NotificationClickInfo notificationInfo) {
        try {
            // 1. 检查目标APP是否已在前台
            if (isAppInForeground(notificationInfo.getPackageName()) && notificationInfo.getPendingIntent() != null) {
                // APP已在前台，直接执行PendingIntent
                notificationInfo.getPendingIntent().send();
            } else {
                // 2. APP不在前台，启动APP并通知AccessibilityService准备执行PendingIntent
                Intent launchIntent = context.getPackageManager()
                        .getLaunchIntentForPackage(notificationInfo.getPackageName());

                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(launchIntent);

                    // 通知AccessibilityService准备执行PendingIntent（带超时机制）
                    AppAccessibilityService accessibilityService = AppAccessibilityService.getInstance();
                    if (accessibilityService != null) {
                        accessibilityService.preparePendingIntent(
                                notificationInfo.getKey(),
                                notificationInfo.getPendingIntent(),
                                notificationInfo.getPackageName()
                        );
                    }
                }
            }
        } catch (Exception e) {
            // 执行失败，降级标准方法
            launchTargetAppStandard(notificationInfo);
        }
    }

    /**
     * 检查指定包名的APP是否在前台
     */
    private boolean isAppInForeground(String packageName) {
        ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        if (activityManager == null) {
            return false;
        }

        List<ActivityManager.RunningAppProcessInfo> runningProcesses =
                activityManager.getRunningAppProcesses();

        if (runningProcesses == null) {
            return false;
        }

        for (ActivityManager.RunningAppProcessInfo processInfo : runningProcesses) {
            if (processInfo.processName.equals(packageName)
                    && processInfo.importance == ActivityManager.RunningAppProcessInfo.IMPORTANCE_FOREGROUND) {
                return true;
            }
        }

        return false;
    }


    /**
     * Shizuku模式：使用Shizuku执行Monkey方法拉起应用程序之后执行PendingIntent
     */

    private void launchTargetAppShizuku(NotificationClickInfo info) {
        // 构造 Monkey 命令
        String cmd = "monkey -p " + info.getPackageName() + " -c android.intent.category.LAUNCHER 1";

        ShizukuShellHelper.getInstance(context).execCommand(cmd, 5000, new ShizukuShellHelper.Callback() {
            @Override
            public void onResult(@NonNull String result) {
                // 命令执行成功，执行 PendingIntent 并移除通知
                PendingIntent pi = info.getPendingIntent();
                if (pi != null) {
                    try {
                        pi.send();
                    } catch (PendingIntent.CanceledException e) {
                        //PendingIntent 失效
                    }
                }
            }

            @Override
            public void onError(@NonNull String error) {
                // 显示 Toast
                Toast.makeText(context.getApplicationContext(), error, Toast.LENGTH_SHORT).show();
                // 执行失败 → 降级到兼容方式
                launchTargetAppCompatibility(info);
            }
        });
    }

    /**
     * 检查指定包名的模型过滤是否启用
     */
    private boolean isModelFilterEnabled(String packageName) {
        return prefsManager.isAppModelFilterEnabled(packageName);
    }

    /**
     * 通知点击信息封装类
     */
    public static class NotificationClickInfo {
        private final String key;
        private final String packageName;
        private final String title;
        private final String content;
        private final PendingIntent pendingIntent;

        public NotificationClickInfo(String key, String packageName, String title,
                                      String content, PendingIntent pendingIntent) {
            this.key = key;
            this.packageName = packageName;
            this.title = title;
            this.content = content;
            this.pendingIntent = pendingIntent;
        }

        public String getKey() {
            return key;
        }

        public String getPackageName() {
            return packageName;
        }

        public String getTitle() {
            return title;
        }

        public String getContent() {
            return content;
        }

        public PendingIntent getPendingIntent() {
            return pendingIntent;
        }
    }
}
