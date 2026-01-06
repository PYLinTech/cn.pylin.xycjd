package cn.pylin.xycjd.handler;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;

import java.util.List;

import cn.pylin.xycjd.manager.SharedPreferencesManager;
import cn.pylin.xycjd.model.local.LocalModelManager;
import cn.pylin.xycjd.service.AppAccessibilityService;
import cn.pylin.xycjd.service.FloatingWindowService;

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
     * 标准模式：直接跳转，不经过无障碍服务
     */
    private void launchTargetAppStandard(NotificationClickInfo notificationInfo) {
        // 直接执行PendingIntent，不通过AccessibilityService
        try {
            if (notificationInfo.getPendingIntent() != null) {
                notificationInfo.getPendingIntent().send();
                // 成功：从超级岛移除通知
                FloatingWindowService.getInstance().removeNotification(notificationInfo.getKey());
            }
        } catch (PendingIntent.CanceledException e) {
            e.printStackTrace();
            // 如果PendingIntent失效，尝试直接启动APP
            Intent launchIntent = context.getPackageManager()
                    .getLaunchIntentForPackage(notificationInfo.getPackageName());
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(launchIntent);
                //从超级岛移除通知
                FloatingWindowService.getInstance().removeNotification(notificationInfo.getKey());
            }
        }
    }

    /**
     * 兼容模式：先打开APP，无障碍检测页面，再跳转
     */
    private void launchTargetAppCompatibility(NotificationClickInfo notificationInfo) {
        // 1. 检查目标APP是否已在前台
        if (isAppInForeground(notificationInfo.getPackageName())) {
            // APP已在前台，直接执行PendingIntent
            try {
                if (notificationInfo.getPendingIntent() != null) {
                    notificationInfo.getPendingIntent().send();
                    // 成功：从超级岛移除通知
                    FloatingWindowService.getInstance().removeNotification(notificationInfo.getKey());
                }
            } catch (PendingIntent.CanceledException e) {
                e.printStackTrace();
                // 执行失败，保留通知
            }
            return;
        }

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
            } else {
                // 无障碍服务未运行，记录日志
                android.util.Log.w("NotificationClickHandler", 
                    "AppAccessibilityService is null, pendingIntent will not be auto-executed. " +
                    "Target app has been launched, user needs to manually complete the action.");
            }
        }
    }

    /**
     * 通过反射获取PendingIntent内部的IIntentSender对象
     * 尝试多种方式以绕过Hidden API限制
     */
    private Object getPendingIntentTarget(PendingIntent pendingIntent) {
        if (pendingIntent == null) return null;
        
        // 尝试1: getTarget() 方法
        try {
            @SuppressLint("SoonBlockedPrivateApi") java.lang.reflect.Method method = PendingIntent.class.getDeclaredMethod("getTarget");
            method.setAccessible(true);
            return method.invoke(pendingIntent);
        } catch (Exception e) {
            // 忽略，尝试下一方法
        }

        // 尝试2: mTarget 字段
        try {
            @SuppressLint("SoonBlockedPrivateApi") java.lang.reflect.Field field = PendingIntent.class.getDeclaredField("mTarget");
            field.setAccessible(true);
            return field.get(pendingIntent);
        } catch (Exception e) {
            // 忽略
        }

        return null;
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
     * Shizuku模式：使用Shizuku方式启动
     */
    private void launchTargetAppShizuku(NotificationClickInfo notificationInfo) {
        if (notificationInfo.getPendingIntent() == null) return;

        try {
            // 检查Shizuku权限，如果没有权限，降级处理
            if (cn.pylin.xycjd.utils.PermissionChecker.checkShizukuPermission()) {
                // 1. 获取 PendingIntent 的 IIntentSender (Hidden API)
                Object intentSender = getPendingIntentTarget(notificationInfo.getPendingIntent());

                if (intentSender == null) {
                    launchTargetAppStandard(notificationInfo);
                    return;
                }

                // 2. 获取 IActivityManager (Shizuku Binder)
                // 使用 Shizuku 获取 ActivityManager 的 Binder，并包装
                // 注意：Shizuku.getSystemService可能不可用，使用反射调用ServiceManager.getService
                Class<?> serviceManagerClass = Class.forName("android.os.ServiceManager");
                java.lang.reflect.Method getServiceMethod = serviceManagerClass.getMethod("getService", String.class);
                android.os.IBinder originalBinder = (android.os.IBinder) getServiceMethod.invoke(null, "activity");

                android.os.IBinder binder = new rikka.shizuku.ShizukuBinderWrapper(originalBinder);

                // IActivityManager iam = IActivityManager.Stub.asInterface(binder);
                Class<?> stubClass = Class.forName("android.app.IActivityManager$Stub");
                java.lang.reflect.Method asInterfaceMethod = stubClass.getMethod("asInterface", android.os.IBinder.class);
                Object iActivityManager = asInterfaceMethod.invoke(null, binder);

                // 3. 调用 sendIntentSender
                // 签名通常为: int sendIntentSender(IIntentSender target, IBinder whitelistToken, int code, Intent intent,
                //         String resolvedType, IIntentReceiver finishedReceiver, String requiredPermission, Bundle options)
                Class<?> iIntentSenderClass = Class.forName("android.content.IIntentSender");
                Class<?> iIntentReceiverClass = Class.forName("android.content.IIntentReceiver");

                java.lang.reflect.Method sendMethod = iActivityManager.getClass().getMethod("sendIntentSender",
                        iIntentSenderClass,             // target
                        android.os.IBinder.class,       // whitelistToken
                        int.class,                      // code
                        Intent.class,                   // intent
                        String.class,                   // resolvedType
                        iIntentReceiverClass,           // finishedReceiver
                        String.class,                   // requiredPermission
                        android.os.Bundle.class         // options
                );

                sendMethod.invoke(iActivityManager,
                        intentSender,                   // target
                        null,                           // whitelistToken
                        0,                              // code
                        null,                           // intent (fillInIntent)
                        null,                           // resolvedType
                        null,                           // finishedReceiver
                        null,                           // requiredPermission
                        null                            // options
                );

                // 成功后移除通知
                FloatingWindowService.getInstance().removeNotification(notificationInfo.getKey());
            } else {
                // 无 Shizuku 权限，回退
                launchTargetAppStandard(notificationInfo);
            }
        } catch (Exception e) {
            e.printStackTrace();
            // 发生异常（如反射失败），回退到标准模式
            launchTargetAppStandard(notificationInfo);
        }
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
