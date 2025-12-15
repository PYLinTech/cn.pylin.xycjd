package cn.pylin.xycjd;

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
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.Nullable;

import java.util.List;

public class FloatingWindowService extends Service {

    private static FloatingWindowService instance;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    // 第二个悬浮窗（灵动岛）相关
    private View floatingIslandView;
    private WindowManager.LayoutParams islandParams;
    
    // 第三个悬浮窗（三圆灵动岛）相关
    private View floatingThreeCircleView;
    private WindowManager.LayoutParams threeCircleParams;
    
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
            if (floatingView != null) {
                windowManager.removeView(floatingView);
            }
            if (floatingIslandView != null) {
                windowManager.removeView(floatingIslandView);
            }
            if (floatingThreeCircleView != null) {
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



    /**
     * 显示通知悬浮窗
     * @param packageName 应用包名
     * @param title 通知标题
     * @param content 通知内容
     */
    public void showNotificationIsland(String packageName, String title, String content) {
        if (windowManager == null) {
            windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
        }
        
        // 如果悬浮岛视图不存在，创建它
        if (floatingIslandView == null) {
            createFloatingIsland();
        }
        
        // 设置通知内容
        updateNotificationContent(packageName, title, content);
        
        // 如果悬浮岛视图未添加到窗口管理器，添加它
        if (floatingIslandView.getParent() == null) {
            windowManager.addView(floatingIslandView, islandParams);
        }
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
        
        // 获取第一个悬浮窗的位置和大小
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        
        // 计算第二个悬浮窗的位置
        // 水平居中，垂直位置在第一个悬浮窗下方约30dp
        int islandY = y + size + dpToPx(ISLAND_MARGIN_TOP);
        
        // 创建悬浮窗布局参数
        islandParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.WRAP_CONTENT,
                WindowManager.LayoutParams.WRAP_CONTENT,
                Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                        WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                        WindowManager.LayoutParams.TYPE_PHONE,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        islandParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        islandParams.x = x;
        islandParams.y = islandY;
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
        
        // 如果三圆悬浮岛视图不存在，创建它
        if (floatingThreeCircleView == null) {
            createThreeCircleIsland();
        }
        
        // 设置应用图标
        updateThreeCircleContent(packageName);
        
        // 如果三圆悬浮岛视图未添加到窗口管理器，添加它
        if (floatingThreeCircleView.getParent() == null) {
            windowManager.addView(floatingThreeCircleView, threeCircleParams);
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
        android.view.ViewGroup.LayoutParams layoutParams = appIcon.getLayoutParams();
        layoutParams.width = circleSize;
        layoutParams.height = circleSize;
        appIcon.setLayoutParams(layoutParams);
        
        android.view.ViewGroup.LayoutParams blackLayoutParams = blackCircle.getLayoutParams();
        blackLayoutParams.width = circleSize;
        blackLayoutParams.height = circleSize;
        blackCircle.setLayoutParams(blackLayoutParams);
        
        android.view.ViewGroup.LayoutParams blackLayoutParams2 = blackCircle2.getLayoutParams();
        blackLayoutParams2.width = circleSize;
        blackLayoutParams2.height = circleSize;
        blackCircle2.setLayoutParams(blackLayoutParams2);
        
        // 计算三圆悬浮岛的位置，使其中心与第一个悬浮窗的中心对齐
        // 三圆悬浮岛的总宽度为 3 * circleSize + 4 * 2dp (每个圆的外边距) + 2 * 2dp (容器内边距)
        int totalWidth = 3 * circleSize + 4 * dpToPx(2) + 2 * dpToPx(2);
        int totalHeight = size; // 总高度等于第一个悬浮窗的大小，确保对齐
        
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