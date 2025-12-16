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
import android.view.animation.DecelerateInterpolator;
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
            windowManager.addView(floatingIslandView, islandParams);
        }
    }
    
    public void updateNotificationData(String packageName, String title, String content) {
        lastNotificationPackageName = packageName;
        lastNotificationTitle = title;
        lastNotificationContent = content;
        
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        
        if (floatingIslandView == null) {
            createFloatingIsland();
        }
        
        updateNotificationContent(packageName, title, content);
    }
    
    /**
     * 隐藏通知悬浮窗
     */
    public void hideNotificationIsland() {
        if (windowManager != null && floatingIslandView != null && floatingIslandView.getParent() != null) {
            windowManager.removeView(floatingIslandView);
            floatingIslandView = null;
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
     * @param packageName 应用包名
     */
    public void showThreeCircleIsland(String packageName) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        
        if (floatingThreeCircleView == null) {
            createThreeCircleIsland();
        }
        
        updateThreeCircleContent(packageName);
        
        if (floatingThreeCircleView.getParent() == null) {
            windowManager.addView(floatingThreeCircleView, threeCircleParams);
            floatingThreeCircleView.post(this::startThreeCircleAnimation);
        }
    }

    /**
     * 隐藏三圆灵动岛悬浮窗
     */
    public void hideThreeCircleIsland() {
        if (windowManager != null && floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
            windowManager.removeView(floatingThreeCircleView);
            floatingThreeCircleView = null;
        }
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
        LinearLayout.LayoutParams layoutParams = (LinearLayout.LayoutParams) appIcon.getLayoutParams();
        layoutParams.width = circleSize;
        layoutParams.height = circleSize;
        // 初始设置负margin实现重叠
        layoutParams.setMarginEnd(-circleSize);
        appIcon.setLayoutParams(layoutParams);
        
        LinearLayout.LayoutParams blackLayoutParams = (LinearLayout.LayoutParams) blackCircle.getLayoutParams();
        blackLayoutParams.width = circleSize;
        blackLayoutParams.height = circleSize;
        // 初始设置负margin实现重叠
        blackLayoutParams.setMarginEnd(-circleSize);
        blackCircle.setLayoutParams(blackLayoutParams);
        
        LinearLayout.LayoutParams blackLayoutParams2 = (LinearLayout.LayoutParams) blackCircle2.getLayoutParams();
        blackLayoutParams2.width = circleSize;
        blackLayoutParams2.height = circleSize;
        blackCircle2.setLayoutParams(blackLayoutParams2);

        // 初始化背景和占位符
        View background = floatingThreeCircleView.findViewById(R.id.island_background);
        ViewGroup.LayoutParams bgParams = background.getLayoutParams();
        bgParams.width = circleSize; // 初始只显示一个圆的大小
        background.setLayoutParams(bgParams);

        View spacerLeft = floatingThreeCircleView.findViewById(R.id.spacer_left);
        ViewGroup.LayoutParams spacerParams = spacerLeft.getLayoutParams();
        spacerParams.width = 0;
        spacerLeft.setLayoutParams(spacerParams);

        View spacerRight = floatingThreeCircleView.findViewById(R.id.spacer_right);
        ViewGroup.LayoutParams spacerParams2 = spacerRight.getLayoutParams();
        spacerParams2.width = 0;
        spacerRight.setLayoutParams(spacerParams2);

        // 初始化容器宽度
        View circlesContainer = floatingThreeCircleView.findViewById(R.id.circles_container);
        ViewGroup.LayoutParams containerParams = circlesContainer.getLayoutParams();
        containerParams.width = circleSize; // 初始宽度为一个圆的大小
        circlesContainer.setLayoutParams(containerParams);
        
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
            if (lastNotificationPackageName != null) {
                showNotificationIsland(lastNotificationPackageName, lastNotificationTitle, lastNotificationContent);
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

        final View background = floatingThreeCircleView.findViewById(R.id.island_background);
        final View spacerLeft = floatingThreeCircleView.findViewById(R.id.spacer_left);
        final View spacerRight = floatingThreeCircleView.findViewById(R.id.spacer_right);
        final View circle1 = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        final View circle2 = floatingThreeCircleView.findViewById(R.id.circle_black);
        final View circlesContainer = floatingThreeCircleView.findViewById(R.id.circles_container);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(800);
        animator.setInterpolator(new DecelerateInterpolator(1.8f));
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // 1. 背景展开
            int currentWidth = (int) (circleSize + (totalContentWidth - circleSize) * fraction);
            ViewGroup.LayoutParams bgParams = background.getLayoutParams();
            bgParams.width = currentWidth;
            background.setLayoutParams(bgParams);

            // 2. 容器宽度同步展开
            ViewGroup.LayoutParams containerParams = circlesContainer.getLayoutParams();
            containerParams.width = currentWidth;
            circlesContainer.setLayoutParams(containerParams);

            // 3. 占位符变宽
            int currentSpacerWidth = (int) (0 + targetSpacerWidth * fraction);
            ViewGroup.LayoutParams sp1 = spacerLeft.getLayoutParams();
            sp1.width = currentSpacerWidth;
            spacerLeft.setLayoutParams(sp1);
            
            ViewGroup.LayoutParams sp2 = spacerRight.getLayoutParams();
            sp2.width = currentSpacerWidth;
            spacerRight.setLayoutParams(sp2);

            // 4. Margin分离
            int startMargin = -circleSize;
            int currentMargin = (int) (startMargin + (targetMargin - startMargin) * fraction);
            
            LinearLayout.LayoutParams lp1 = (LinearLayout.LayoutParams) circle1.getLayoutParams();
            lp1.setMarginEnd(currentMargin);
            circle1.setLayoutParams(lp1);

            LinearLayout.LayoutParams lp2 = (LinearLayout.LayoutParams) circle2.getLayoutParams();
            lp2.setMarginEnd(currentMargin);
            circle2.setLayoutParams(lp2);
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
