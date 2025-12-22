package cn.pylin.xycjd;
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
            new UpdateManager(this).checkForUpdates(false);
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
        Locale locale;
        if (language.equals("zh-rTW")) {
            // 繁体中文使用台湾地区
            locale = new Locale("zh", "TW");
        } else {
            locale = new Locale(language);
        }
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
}
