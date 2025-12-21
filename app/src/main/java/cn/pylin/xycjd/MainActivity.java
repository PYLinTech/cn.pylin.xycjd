package cn.pylin.xycjd;
import android.app.Activity;
import android.app.ActivityManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
import androidx.dynamicanimation.animation.DynamicAnimation;
import androidx.dynamicanimation.animation.SpringAnimation;
import androidx.dynamicanimation.animation.SpringForce;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SETTINGS = 0;
    private static final int PAGE_FILTER = 1;
    private static final int PAGE_ABOUT = 2;

    private int currentPage = PAGE_SETTINGS;

    private Button btnSettings;
    private Button btnFilter;
    private Button btnAbout;
    private View navigationIndicator;
    private SpringAnimation indicatorSpringAnimation;
    
    // 上次按返回键的时间
    private long lastBackPressTime = 0;
    // 双击返回键的时间间隔（毫秒）
    private static final long DOUBLE_BACK_PRESS_INTERVAL = 2000;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 检查是否需要显示引导页面
        if (shouldShowIntro()) {
            // 需要显示引导页面，跳转到引导页面
            Intent intent = new Intent(this, IntroActivity.class);
            startActivity(intent);
            finish();
            return;
        }
        
        // 加载保存的语言设置
        loadLanguageSetting();
        
        // 加载保存的主题设置
        loadThemeSetting();
        
        setContentView(R.layout.activity_main);

        btnSettings = findViewById(R.id.btn_settings);
        btnFilter = findViewById(R.id.btn_filter);
        btnAbout = findViewById(R.id.btn_about);
        navigationIndicator = findViewById(R.id.navigation_indicator);

        // 设置初始页面
        loadPage(currentPage);
        updateButtonStates();
        
        // 初始化指示器位置
        updateIndicatorLayout();

        // 检查更新
        getWindow().getDecorView().post(() -> {
            if (!isFinishing() && !isDestroyed()) { //确保活动未被销毁或完成销毁后再执行检查更新操作
                new UpdateManager(this).checkForUpdates(false);
            }
        });

        // 设置按钮点击事件
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_SETTINGS) {
                    currentPage = PAGE_SETTINGS;
                    loadPage(currentPage);
                    updateButtonStates();
                    updateIndicatorPosition(currentPage, true);
                }
            }
        });

        btnFilter.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_FILTER) {
                    currentPage = PAGE_FILTER;
                    loadPage(currentPage);
                    updateButtonStates();
                    updateIndicatorPosition(currentPage, true);
                }
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_ABOUT) {
                    currentPage = PAGE_ABOUT;
                    loadPage(currentPage);
                    updateButtonStates();
                    updateIndicatorPosition(currentPage, true);
                }
            }
        });
    }

    private void loadPage(int page) {
        Fragment fragment = null;
        
        // 根据页面类型创建对应的Fragment
        if (page == PAGE_SETTINGS) {
            fragment = new SettingsFragment();
        } else if (page == PAGE_FILTER) {
            // 使用FilterFragment
            fragment = new FilterFragment();
        } else if (page == PAGE_ABOUT) {
            fragment = new AboutFragment();
        }
        
        if (fragment != null) {
            FragmentTransaction transaction = getSupportFragmentManager().beginTransaction();
            transaction.replace(R.id.content_frame, fragment);
            transaction.commit();
        }
    }

    private void updateButtonStates() {
        // 获取反色
        int inverseColor = getResources().getColor(R.color.colorPrimaryInverse, getTheme());
        int defaultColor = getResources().getColor(R.color.colorOnSurface, getTheme());
        
        // 更新按钮状态，当前页面按钮显示不同颜色
        if (currentPage == PAGE_SETTINGS) {
            btnSettings.setTextColor(inverseColor);
            btnFilter.setTextColor(defaultColor);
            btnAbout.setTextColor(defaultColor);
        } else if (currentPage == PAGE_FILTER) {
            btnSettings.setTextColor(defaultColor);
            btnFilter.setTextColor(inverseColor);
            btnAbout.setTextColor(defaultColor);
        } else if (currentPage == PAGE_ABOUT) {
            btnSettings.setTextColor(defaultColor);
            btnFilter.setTextColor(defaultColor);
            btnAbout.setTextColor(inverseColor);
        }
    }
    
    private void updateIndicatorPosition(int page, boolean animate) {
        if (navigationIndicator == null) return;
        
        int navigationBarWidth = findViewById(R.id.navigation_bar).getWidth();
        int indicatorWidth = navigationIndicator.getWidth();
        int buttonWidth = navigationBarWidth / 3;
        
        // 计算每个按钮的中心位置，然后减去指示器宽度的一半
        int targetX = 0;
        switch (page) {
            case PAGE_SETTINGS:
                targetX = buttonWidth / 2 - indicatorWidth / 2;
                break;
            case PAGE_FILTER:
                targetX = buttonWidth + buttonWidth / 2 - indicatorWidth / 2;
                break;
            case PAGE_ABOUT:
                targetX = buttonWidth * 2 + buttonWidth / 2 - indicatorWidth / 2;
                break;
        }
        
        if (animate) {
            if (indicatorSpringAnimation == null) {
                // 初始化弹簧动画，使用低刚度和低阻尼实现流畅的物理效果
                indicatorSpringAnimation = new SpringAnimation(navigationIndicator, DynamicAnimation.X);
                SpringForce spring = new SpringForce();
                spring.setDampingRatio(SpringForce.DAMPING_RATIO_LOW_BOUNCY);
                spring.setStiffness(SpringForce.STIFFNESS_LOW);
                indicatorSpringAnimation.setSpring(spring);
            }
            indicatorSpringAnimation.animateToFinalPosition(targetX);
        } else {
            if (indicatorSpringAnimation != null && indicatorSpringAnimation.isRunning()) {
                indicatorSpringAnimation.cancel();
            }
            navigationIndicator.setX(targetX);
        }
    }

    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        updateIndicatorLayout();
    }

    private void updateIndicatorLayout() {
        final View navBar = findViewById(R.id.navigation_bar);
        if (navBar == null) return;
        
        navBar.getViewTreeObserver().addOnGlobalLayoutListener(new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                // 移除监听器，避免重复调用
                navBar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                
                // 调整指示器宽度为按钮宽度的80%
                int navigationBarWidth = navBar.getWidth();
                // 避免宽度为0的情况
                if (navigationBarWidth <= 0) return;
                
                int buttonWidth = navigationBarWidth / 3;
                int indicatorWidth = (int)(buttonWidth * 0.8f);
                
                if (navigationIndicator != null) {
                    FrameLayout.LayoutParams params = (FrameLayout.LayoutParams) navigationIndicator.getLayoutParams();
                    params.width = indicatorWidth;
                    navigationIndicator.setLayoutParams(params);
                    
                    // 确保布局完成后再设置初始位置
                    navigationIndicator.post(new Runnable() {
                        @Override
                        public void run() {
                            // 设置初始位置
                            updateIndicatorPosition(currentPage, false);
                        }
                    });
                }
            }
        });
    }
    
    /**
     * 加载保存的语言设置
     */
    private void loadLanguageSetting() {
        // 获取SharedPreferences中保存的语言设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        String language = preferences.getString("language", "zh");
        
        // 更新应用语言设置
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
    
    /**
     * 加载保存的主题设置
     */
    private void loadThemeSetting() {
        // 获取SharedPreferences中保存的主题设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int themeMode = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        // 应用主题设置
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }
    
    /**
     * 检查是否需要显示引导页面
     */
    private boolean shouldShowIntro() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        int savedVersion = preferences.getInt("intro_version", 0);
        return savedVersion < IntroActivity.INTRO_VERSION;
    }
    
    @Override
    protected void onUserLeaveHint() {
        super.onUserLeaveHint();
        // 当用户离开应用（如按下Home键或切换任务）时，释放资源并从最近任务列表中隐藏
        hideFromRecents();
    }

    @Override
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        // 如果两次按返回键的时间间隔小于2秒，则释放资源并从最近任务列表中排除
        if (currentTime - lastBackPressTime < DOUBLE_BACK_PRESS_INTERVAL) {
            // 从最近任务列表中排除应用并释放资源
            hideFromRecents();
            // 移动到后台
            moveTaskToBack(true);
        } else {
            // 第一次按返回键，提示用户再次按返回键隐藏到后台
            lastBackPressTime = currentTime;
            // 显示提示信息
            android.widget.Toast.makeText(this, getString(R.string.double_back_to_exit), android.widget.Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 从最近任务列表中隐藏应用并释放Activity资源
     */
    private void hideFromRecents() {
        try {
            ActivityManager activityManager = (ActivityManager) getSystemService(Activity.ACTIVITY_SERVICE);
            if (activityManager != null) {
                // 获取当前应用的任务列表
                for (ActivityManager.AppTask task : activityManager.getAppTasks()) {
                    // 设置从最近任务中排除
                    task.setExcludeFromRecents(true);
                    // 完成任务并移除，释放Activity资源
                    task.finishAndRemoveTask();
                }
            }
        } catch (Exception e) {
            // 处理异常
            e.printStackTrace();
        }
    }
}