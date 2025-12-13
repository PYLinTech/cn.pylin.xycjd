package cn.pylin.xycjd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.View;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;

import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SETTINGS = 0;
    private static final int PAGE_APPS = 1;
    private static final int PAGE_ABOUT = 2;

    private int currentPage = PAGE_SETTINGS;

    private Button btnSettings;
    private Button btnApps;
    private Button btnAbout;
    
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
        btnApps = findViewById(R.id.btn_apps);
        btnAbout = findViewById(R.id.btn_about);

        // 设置初始页面
        loadPage(currentPage);
        updateButtonStates();

        // 设置按钮点击事件
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_SETTINGS) {
                    currentPage = PAGE_SETTINGS;
                    loadPage(currentPage);
                    updateButtonStates();
                }
            }
        });

        btnApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_APPS) {
                    currentPage = PAGE_APPS;
                    loadPage(currentPage);
                    updateButtonStates();
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
                }
            }
        });
    }

    private void loadPage(int page) {
        Fragment fragment = null;
        
        // 根据页面类型创建对应的Fragment
        if (page == PAGE_SETTINGS) {
            fragment = new SettingsFragment();
        } else if (page == PAGE_APPS) {
            // 目前Apps页面还没有Fragment，暂时使用空白Fragment
            fragment = new Fragment();
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
        // 更新按钮状态，当前页面按钮显示不同颜色
        if (currentPage == PAGE_SETTINGS) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnApps.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnAbout.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else if (currentPage == PAGE_APPS) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnApps.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnAbout.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else if (currentPage == PAGE_ABOUT) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnApps.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnAbout.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }
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
    public void onBackPressed() {
        long currentTime = System.currentTimeMillis();
        // 如果两次按返回键的时间间隔小于2秒，则退出应用
        if (currentTime - lastBackPressTime < DOUBLE_BACK_PRESS_INTERVAL) {
            super.onBackPressed();
        } else {
            // 第一次按返回键，提示用户再次按返回键退出应用
            lastBackPressTime = currentTime;
            // 显示提示信息
            android.widget.Toast.makeText(this, "再按一次退出应用", android.widget.Toast.LENGTH_SHORT).show();
        }
    }
}