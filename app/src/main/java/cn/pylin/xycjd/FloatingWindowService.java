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

import androidx.annotation.Nullable;

import java.util.List;

public class FloatingWindowService extends Service {

    private static FloatingWindowService instance;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    private SharedPreferences preferences;
    
    private static final String PREF_FLOATING_SIZE = "floating_size";
    private static final String PREF_FLOATING_X = "floating_x";
    private static final String PREF_FLOATING_Y = "floating_y";
    
    private static final int DEFAULT_SIZE = 100;
    private static final int DEFAULT_X = 0;
    private static final int DEFAULT_Y = -100;

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
        if (windowManager != null && floatingView != null) {
            windowManager.removeView(floatingView);
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



    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}