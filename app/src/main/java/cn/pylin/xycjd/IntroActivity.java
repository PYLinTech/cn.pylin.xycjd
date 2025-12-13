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
import androidx.viewpager2.widget.ViewPager2;

import java.util.Locale;

public class IntroActivity extends AppCompatActivity {

    // 引导页面版本号
    public static final int INTRO_VERSION = 251213;
    
    private ViewPager2 viewPager;
    private Button btnNext;
    private IntroAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 设置应用语言（只在引导页面执行）
        setAppLanguage();
        
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_intro);

        // 初始化控件
        viewPager = findViewById(R.id.view_pager);
        btnNext = findViewById(R.id.btn_next);

        // 初始化适配器
        adapter = new IntroAdapter(this);
        
        // 添加Fragment
        adapter.addFragment(new WelcomeFragment());
        adapter.addFragment(new AgreementFragment());
        adapter.addFragment(new PrivacyFragment());
        adapter.addFragment(new PermissionFragment());
        
        // 设置适配器
        viewPager.setAdapter(adapter);
        
        // 监听页面变化
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 更新按钮文本
                updateButtonText(position);
            }
        });
        
        // 设置按钮点击事件
        btnNext.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                int currentPosition = viewPager.getCurrentItem();
                if (currentPosition < adapter.getItemCount() - 1) {
                    // 跳转到下一页
                    viewPager.setCurrentItem(currentPosition + 1);
                } else {
                    // 完成引导，跳转到主页面
                    finishIntro();
                }
            }
        });
    }
    
    /**
     * 更新按钮文本
     */
    private void updateButtonText(int position) {
        switch (position) {
            case 0:
                btnNext.setText(R.string.intro_next);
                break;
            case 1:
                btnNext.setText(R.string.intro_agree);
                break;
            case 2:
                btnNext.setText(R.string.intro_agree);
                break;
            case 3:
                btnNext.setText(R.string.intro_finish);
                break;
        }
    }
    
    /**
     * 设置应用语言
     */
    private void setAppLanguage() {
        // 获取系统语言
        String systemLanguage = Locale.getDefault().getLanguage();
        
        // 检查支持的语言
        String appLanguage = systemLanguage;
        if (!"zh".equals(systemLanguage) && !"en".equals(systemLanguage)) {
            // 默认使用中文
            appLanguage = "zh";
        }
        
        // 保存语言设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", appLanguage);
        editor.apply();
        
        // 更新应用语言设置
        Locale locale = new Locale(appLanguage);
        Locale.setDefault(locale);
        
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());
    }
    
    /**
     * 完成引导
     */
    private void finishIntro() {
        // 保存引导页面版本号
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("intro_version", INTRO_VERSION);
        editor.apply();
        
        // 跳转到主页面
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }
}