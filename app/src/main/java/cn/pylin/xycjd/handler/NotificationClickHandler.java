package cn.pylin.xycjd.handler;

import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
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
        if (notificationInfo.getPendingIntent() != null) {
            try {
                notificationInfo.getPendingIntent().send();
            } catch (PendingIntent.CanceledException e) {
                throw new RuntimeException(e);
            }
        }
    }

    /**
     * 兼容模式：普通方法打开APP，再执行PendingIntent
     */
    private void launchTargetAppCompatibility(NotificationClickInfo notificationInfo) {
        try {
            // 1. 启动APP
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(notificationInfo.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
            }
            // 2. 直接执行PendingIntent
            if (notificationInfo.getPendingIntent() != null) {
                notificationInfo.getPendingIntent().send();
            }
        } catch (Exception e) {
            // 执行失败，降级标准方法
            launchTargetAppStandard(notificationInfo);
        }
    }

    /**
     * Shizuku模式：使用Shizuku执行Monkey方法拉起应用程序之后执行PendingIntent
     */

    private void launchTargetAppShizuku(NotificationClickInfo info) {
        // 1. 获取 launcher Activity
        String packageName = info.getPackageName();
        String launcherActivity = getLauncherActivity(packageName);
        if (launcherActivity == null) {
            // 找不到 Launcher Activity → 降级
            launchTargetAppCompatibility(info);
            return;
        }

        // 2. 构造 am start 命令
        String cmd = "am start -a " + android.content.Intent.ACTION_MAIN +
                " -c " + android.content.Intent.CATEGORY_LAUNCHER +
                " -n " + packageName + "/" + launcherActivity;

        // 3. 使用 ShizukuShellHelper 执行
        ShizukuShellHelper.getInstance(context).execCommand(cmd, 3000, new ShizukuShellHelper.Callback() {
            @Override
            public void onResult(@NonNull String result) {
                // 命令执行成功，执行 PendingIntent
                if (info.getPendingIntent() != null) {
                    try {
                        info.getPendingIntent().send();
                    } catch (PendingIntent.CanceledException e) {
                        // PendingIntent 失效
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
     * 获取目标应用的 launcher Activity
     */
    private String getLauncherActivity(String packageName) {
        PackageManager pm = context.getPackageManager();
        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);
        intent.setPackage(packageName);

        List<ResolveInfo> resolveInfos = pm.queryIntentActivities(intent, 0);
        if (resolveInfos == null || resolveInfos.isEmpty()) return null;

        // 返回第一个匹配的 Activity 的全类名
        ResolveInfo info = resolveInfos.get(0);
        return info.activityInfo.name;
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
