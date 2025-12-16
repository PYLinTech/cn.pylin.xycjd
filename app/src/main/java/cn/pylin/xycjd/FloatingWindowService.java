package cn.pylin.xycjd;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.List;

public class FloatingWindowService extends Service {

    private static FloatingWindowService instance;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    private View floatingIslandView;
    private WindowManager.LayoutParams islandParams;
    
    private View floatingThreeCircleView;
    private WindowManager.LayoutParams threeCircleParams;
    
    public static class NotificationInfo {
        String key;
        String packageName;
        String title;
        String content;

        public NotificationInfo(String key, String packageName, String title, String content) {
            this.key = key;
            this.packageName = packageName;
            this.title = title;
            this.content = content;
        }
    }

    private java.util.LinkedList<NotificationInfo> notificationQueue = new java.util.LinkedList<>();

    private String lastNotificationPackageName;
    private String lastNotificationTitle;
    private String lastNotificationContent;
    
    private SharedPreferences preferences;
    
    private static final String PREF_FLOATING_SIZE = "floating_size";
    private static final String PREF_FLOATING_X = "floating_x";
    private static final String PREF_FLOATING_Y = "floating_y";
    
    private static final int DEFAULT_SIZE = 100;
    private static final int DEFAULT_X = 0;
    private static final int DEFAULT_Y = -100;
    
    // 第二个悬浮窗与第一个悬浮窗的垂直间距（dp）
    private static final int ISLAND_MARGIN_TOP = 20;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (windowManager != null) {
            if (floatingView != null && floatingView.getParent() != null) {
                windowManager.removeView(floatingView);
            }
            if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                windowManager.removeView(floatingIslandView);
            }
            if (floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
                windowManager.removeView(floatingThreeCircleView);
            }
        }
    }
    
    public static FloatingWindowService getInstance() {
        return instance;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("update", false)) {
            // 更新悬浮窗
            int size = intent.getIntExtra("size", DEFAULT_SIZE);
            int x = intent.getIntExtra("x", DEFAULT_X);
            int y = intent.getIntExtra("y", DEFAULT_Y);
            updateFloatingWindow(size, x, y);
        } else {
            // 创建新的悬浮窗
            createFloatingWindow();
        }
        return START_STICKY;
    }
    
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingWindowService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    private void createFloatingWindow() {
        windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingView = inflater.inflate(R.layout.floating_window_layout, null);
        
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        
        params = new WindowManager.LayoutParams(
                size,
                size,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = x;
        params.y = y;
        
        windowManager.addView(floatingView, params);
    }
    
    public void updateFloatingWindow(int size, int x, int y) {
        if (windowManager != null && floatingView != null && params != null) {
            params.width = size;
            params.height = size;
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.x = x;
            params.y = y;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            windowManager.updateViewLayout(floatingView, params);
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(PREF_FLOATING_SIZE, size);
            editor.putInt(PREF_FLOATING_X, x);
            editor.putInt(PREF_FLOATING_Y, y);
            editor.apply();
        }
    }
    
    public static int getSize(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
    }
    
    public static int getX(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
    }
    
    public static int getY(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
    }



    public void showNotificationIsland(String packageName, String title, String content) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        
        if (floatingIslandView == null) {
            createFloatingIsland();
        }
        
        updateNotificationContent(packageName, title, content);
        
        if (floatingIslandView.getParent() == null) {
            // 在添加视图前设置初始状态，防止闪烁
            View islandContainer = floatingIslandView.findViewById(R.id.island_container);
            if (islandContainer != null) {
                islandContainer.setAlpha(0f);
                islandContainer.setScaleX(0.8f);
                islandContainer.setScaleY(0.8f);
                islandContainer.setTranslationY(-dpToPx(50));
            }
            
            windowManager.addView(floatingIslandView, islandParams);
            floatingIslandView.post(this::startIslandEnterAnimation);
        }
    }
    
    /**
     * 执行标准灵动岛的入场动画
     */
    private void startIslandEnterAnimation() {
        if (floatingIslandView == null) return;
        
        View islandContainer = floatingIslandView.findViewById(R.id.island_container);
        if (islandContainer == null) return;
        
        // 确保初始状态（虽然addView前设置过，这里再次确保逻辑完整）
        islandContainer.setAlpha(0f);
        islandContainer.setScaleX(0.8f);
        islandContainer.setScaleY(0.8f);
        islandContainer.setTranslationY(-dpToPx(50));
        
        // 组合动画
        islandContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f)) // 轻微弹跳
                .start();
    }
    
    public void addNotification(String key, String packageName, String title, String content) {
        // 移除已存在的同key通知（更新情况）
        removeNotificationInternal(key);
        
        // 添加到头部（最新的）
        notificationQueue.addFirst(new NotificationInfo(key, packageName, title, content));
        
        // 更新最近的通知变量
        if (!notificationQueue.isEmpty()) {
            NotificationInfo latest = notificationQueue.getFirst();
            lastNotificationPackageName = latest.packageName;
            lastNotificationTitle = latest.title;
            lastNotificationContent = latest.content;
        }

        // 显示或更新三圆悬浮窗
        showThreeCircleIsland();
    }

    public void removeNotification(String key) {
        removeNotificationInternal(key);
        
        if (notificationQueue.isEmpty()) {
            hideThreeCircleIsland();
            hideNotificationIsland();
            lastNotificationPackageName = null;
        } else {
            // 更新UI显示最新的通知
            NotificationInfo latest = notificationQueue.getFirst();
            lastNotificationPackageName = latest.packageName;
            lastNotificationTitle = latest.title;
            lastNotificationContent = latest.content;
            
            updateThreeCircleContent();
            // 如果展开的悬浮窗正在显示，也更新它
            if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                updateNotificationContent(latest.packageName, latest.title, latest.content);
            }
        }
    }

    private void removeNotificationInternal(String key) {
        if (key == null) return;
        for (int i = 0; i < notificationQueue.size(); i++) {
            if (key.equals(notificationQueue.get(i).key)) {
                notificationQueue.remove(i);
                break;
            }
        }
    }

    public void updateNotificationData(String packageName, String title, String content) {
        // 兼容旧方法，但建议使用addNotification
        addNotification(packageName + title, packageName, title, content);
    }
    
    /**
     * 隐藏通知悬浮窗
     */
    public void hideNotificationIsland() {
        if (windowManager != null && floatingIslandView != null && floatingIslandView.getParent() != null) {
            View islandContainer = floatingIslandView.findViewById(R.id.island_container);
            if (islandContainer != null) {
                // 执行退出动画
                islandContainer.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .translationY(-dpToPx(50))
                        .setDuration(330)
                        .setInterpolator(new AnticipateInterpolator(1f))
                        .withEndAction(() -> {
                            // 动画结束后移除视图
                            if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                                windowManager.removeView(floatingIslandView);
                                floatingIslandView = null;
                            }
                        })
                        .start();
            } else {
                // 如果找不到容器，直接移除
                windowManager.removeView(floatingIslandView);
                floatingIslandView = null;
            }
        }
    }
    
    /**
     * 创建灵动岛样式的悬浮窗
     */
    private void createFloatingIsland() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingIslandView = inflater.inflate(R.layout.floating_window_island, null);
        
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        
        int islandY = y + size + dpToPx(ISLAND_MARGIN_TOP);

        View islandContainer = floatingIslandView.findViewById(R.id.island_container);
        ViewGroup.MarginLayoutParams containerParams = (ViewGroup.MarginLayoutParams) islandContainer.getLayoutParams();
        containerParams.topMargin = islandY;
        islandContainer.setLayoutParams(containerParams);
        islandContainer.setTranslationX(x);
        islandContainer.setClickable(true);

        View islandRoot = floatingIslandView.findViewById(R.id.island_root);
        islandRoot.setOnClickListener(v -> hideNotificationIsland());

        islandContainer.setOnClickListener(v -> {
            if (lastNotificationPackageName != null) {
                Intent launchIntent = getPackageManager().getLaunchIntentForPackage(lastNotificationPackageName);
                if (launchIntent != null) {
                    launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(launchIntent);
                    hideNotificationIsland();
                }
            }
        });
        
        islandParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        islandParams.gravity = Gravity.TOP | Gravity.START;
        islandParams.x = 0;
        islandParams.y = 0;
    }
    
    /**
     * 更新通知内容
     * @param packageName 应用包名
     * @param title 通知标题
     * @param content 通知内容
     */
    private void updateNotificationContent(String packageName, String title, String content) {
        if (floatingIslandView == null) {
            return;
        }
        
        // 获取应用图标
        ImageView appIcon = floatingIslandView.findViewById(R.id.app_icon);
        try {
            // 使用PackageManager获取应用图标
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
        } catch (Exception e) {
            // 如果获取失败，使用默认图标
            appIcon.setImageResource(android.R.mipmap.sym_def_app_icon);
        }
        
        // 设置通知标题和内容
        TextView notificationTitle = floatingIslandView.findViewById(R.id.notification_title);
        TextView notificationContent = floatingIslandView.findViewById(R.id.notification_content);
        
        notificationTitle.setText(title != null ? title : "");
        notificationContent.setText(content != null ? content : "");
    }
    
    /**
     * dp转换为px
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    /**
     * 显示三圆灵动岛悬浮窗
     */
    public void showThreeCircleIsland() {
        if (notificationQueue.isEmpty()) return;
        
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        
        if (floatingThreeCircleView == null) {
            createThreeCircleIsland();
        }
        
        updateThreeCircleContent();
        
        if (floatingThreeCircleView.getParent() == null) {
            windowManager.addView(floatingThreeCircleView, threeCircleParams);
            floatingThreeCircleView.post(this::startThreeCircleAnimation);
        }
    }

    public void showThreeCircleIsland(String packageName) {
        showThreeCircleIsland();
    }

    /**
     * 更新三圆灵动岛内容
     */
    private void updateThreeCircleContent() {
        if (floatingThreeCircleView == null || notificationQueue.isEmpty()) {
            return;
        }
        
        NotificationInfo info = notificationQueue.getFirst();
        String packageName = info.packageName;
        
        // 获取应用图标
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        try {
            // 使用PackageManager获取应用图标
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
        } catch (Exception e) {
            // 如果获取失败，使用默认图标
            appIcon.setImageResource(android.R.mipmap.sym_def_app_icon);
        }
        
        // 更新数量
        TextView countText = floatingThreeCircleView.findViewById(R.id.circle_black2);
        if (countText != null) {
            int count = notificationQueue.size();
            countText.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    /**
     * 隐藏三圆灵动岛悬浮窗
     */
    public void hideThreeCircleIsland() {
        if (windowManager != null && floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
            startThreeCircleExitAnimation(() -> {
                if (windowManager != null && floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
                    windowManager.removeView(floatingThreeCircleView);
                    floatingThreeCircleView = null;
                }
            });
        }
    }
    
    /**
     * 执行三圆灵动岛的退出动画
     * @param onAnimationEnd 动画结束回调
     */
    private void startThreeCircleExitAnimation(Runnable onAnimationEnd) {
        if (floatingThreeCircleView == null) {
            if (onAnimationEnd != null) onAnimationEnd.run();
            return;
        }

        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int circleSize = size - dpToPx(4);
        int targetSpacerWidth = dpToPx(10);
        int targetMargin = dpToPx(2);
        // 最终宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth
        int totalContentWidth = 3 * circleSize + 6 * targetMargin + 2 * targetSpacerWidth;
        
        // 偏移量 = circleSize + margin + spacer + margin = circleSize + 2*margin + spacer
        int offset = circleSize + 2 * targetMargin + targetSpacerWidth;

        final View background = floatingThreeCircleView.findViewById(R.id.island_background);
        final View circle1 = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        final View circle3 = floatingThreeCircleView.findViewById(R.id.circle_black2);

        // 倒放：从1f到0f
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(400); // 退出动画稍微快一点
        animator.setInterpolator(new AnticipateInterpolator(1.2f)); // 使用Anticipate实现倒放的"回弹"效果
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // 1. 背景收缩
            int currentWidth = (int) (circleSize + (totalContentWidth - circleSize) * fraction);
            ViewGroup.LayoutParams bgParams = background.getLayoutParams();
            bgParams.width = currentWidth;
            background.setLayoutParams(bgParams);

            // 2. 圆形回归
            float currentOffset = offset * fraction;
            
            // Circle 1 (Left) moves back to center
            circle1.setTranslationX(-currentOffset);
            
            // Circle 3 (Right) moves back to center
            circle3.setTranslationX(currentOffset);
        });
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });
        
        animator.start();
    }

    /**
     * 创建三圆灵动岛样式的悬浮窗
     */
    private void createThreeCircleIsland() {
        LayoutInflater inflater = (LayoutInflater) getSystemService(LAYOUT_INFLATER_SERVICE);
        floatingThreeCircleView = inflater.inflate(R.layout.floating_window_island_three, null);
        
        // 获取第一个悬浮窗的位置和大小
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        
        // 计算第三个小岛的圆形大小，比第一个悬浮窗小4dp
        int circleSize = size - dpToPx(4); // 减去4dp，使圆形小一点
        
        // 动态设置圆形大小
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        View blackCircle = floatingThreeCircleView.findViewById(R.id.circle_black);
        View blackCircle2 = floatingThreeCircleView.findViewById(R.id.circle_black2);
        
        // 设置圆形大小
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) appIcon.getLayoutParams();
        layoutParams.width = circleSize;
        layoutParams.height = circleSize;
        appIcon.setLayoutParams(layoutParams);
        
        FrameLayout.LayoutParams blackLayoutParams = (FrameLayout.LayoutParams) blackCircle.getLayoutParams();
        blackLayoutParams.width = circleSize;
        blackLayoutParams.height = circleSize;
        blackCircle.setLayoutParams(blackLayoutParams);
        
        FrameLayout.LayoutParams blackLayoutParams2 = (FrameLayout.LayoutParams) blackCircle2.getLayoutParams();
        blackLayoutParams2.width = circleSize;
        blackLayoutParams2.height = circleSize;
        blackCircle2.setLayoutParams(blackLayoutParams2);

        // 初始化背景
        View background = floatingThreeCircleView.findViewById(R.id.island_background);
        ViewGroup.LayoutParams bgParams = background.getLayoutParams();
        bgParams.width = circleSize; // 初始只显示一个圆的大小
        background.setLayoutParams(bgParams);

        // 计算三圆悬浮岛的位置，使其中心与第一个悬浮窗的中心对齐
        int targetSpacerWidth = dpToPx(10);
        int margin = dpToPx(2);
        // 三圆悬浮岛的总宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth + 2 * padding
        int totalWidth = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth + dpToPx(20);
        int totalHeight = size; 
        
        // 创建悬浮窗布局参数
        threeCircleParams = new WindowManager.LayoutParams(
                totalWidth,
                totalHeight,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        // 设置位置，使其中心与第一个悬浮窗的中心对齐
        threeCircleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        threeCircleParams.x = x;
        threeCircleParams.y = y;
        
        floatingThreeCircleView.setOnClickListener(view -> {
            if (!notificationQueue.isEmpty()) {
                NotificationInfo info = notificationQueue.getFirst();
                showNotificationIsland(info.packageName, info.title, info.content);
            }
        });
    }

    /**
     * 执行三圆灵动岛的入场动画
     */
    private void startThreeCircleAnimation() {
        if (floatingThreeCircleView == null) return;

        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int circleSize = size - dpToPx(4);
        int targetSpacerWidth = dpToPx(10);
        int targetMargin = dpToPx(2);
        // 最终宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth
        int totalContentWidth = 3 * circleSize + 6 * targetMargin + 2 * targetSpacerWidth;
        
        // 偏移量 = circleSize + margin + spacer + margin = circleSize + 2*margin + spacer
        int offset = circleSize + 2 * targetMargin + targetSpacerWidth;

        final View background = floatingThreeCircleView.findViewById(R.id.island_background);
        final View circle1 = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        final View circle3 = floatingThreeCircleView.findViewById(R.id.circle_black2);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // 1. 背景展开
            int currentWidth = (int) (circleSize + (totalContentWidth - circleSize) * fraction);
            ViewGroup.LayoutParams bgParams = background.getLayoutParams();
            bgParams.width = currentWidth;
            background.setLayoutParams(bgParams);

            // 2. 圆形移动
            float currentOffset = offset * fraction;
            
            // Circle 1 (Left) moves left (negative X)
            circle1.setTranslationX(-currentOffset);
            
            // Circle 3 (Right) moves right (positive X)
            circle3.setTranslationX(currentOffset);
        });
        animator.start();
    }
    
    /**
     * 更新三圆灵动岛内容
     * @param packageName 应用包名
     */
    private void updateThreeCircleContent(String packageName) {
        if (floatingThreeCircleView == null) {
            return;
        }
        
        // 获取应用图标
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        try {
            // 使用PackageManager获取应用图标
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
        } catch (Exception e) {
            // 如果获取失败，使用默认图标
            appIcon.setImageResource(android.R.mipmap.sym_def_app_icon);
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
