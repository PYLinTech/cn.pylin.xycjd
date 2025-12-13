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
    
    // 保存Fragment引用
    private WelcomeFragment welcomeFragment;
    private AgreementFragment agreementFragment;
    private PrivacyFragment privacyFragment;
    private PermissionFragment permissionFragment;

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
        
        // 创建Fragment实例
        welcomeFragment = new WelcomeFragment();
        agreementFragment = new AgreementFragment();
        privacyFragment = new PrivacyFragment();
        permissionFragment = new PermissionFragment();
        
        // 添加Fragment
        adapter.addFragment(welcomeFragment);
        adapter.addFragment(agreementFragment);
        adapter.addFragment(privacyFragment);
        adapter.addFragment(permissionFragment);
        
        // 设置适配器
        viewPager.setAdapter(adapter);
        
        // 禁用左右滑动切换
        viewPager.setUserInputEnabled(false);
        
        // 设置滚动监听器
        setScrollListeners();
        
        // 监听页面变化
        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // 更新按钮文本
                updateButtonText(position);
            }
        });
        
        // 检查是否需要直接跳转到权限设置页面
        boolean directToPermission = getIntent().getBooleanExtra("direct_to_permission", false);
        if (directToPermission) {
            // 直接跳转到权限设置页面
            viewPager.setCurrentItem(3, false);
        }

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
        // 根据页面位置设置按钮文本
        switch (position) {
            case 0:
                btnNext.setText(R.string.intro_next);
                break;
            case 1:
                btnNext.setText(R.string.intro_slide_to_read);
                break;
            case 2:
                btnNext.setText(R.string.intro_slide_to_read);
                break;
            case 3:
                btnNext.setText(R.string.intro_finish);
                break;
        }
        
        // 直接根据页面位置设置按钮状态：用户协议和隐私政策页面为灰色不可点击
        if (position == 1 || position == 2) {
            btnNext.setEnabled(false);
            btnNext.setTextColor(getResources().getColor(R.color.white));
            btnNext.setBackgroundTintList(getResources().getColorStateList(R.color.gray));
        } else {
            btnNext.setEnabled(true);
            btnNext.setTextColor(getResources().getColor(R.color.white));
            btnNext.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
        }
    }
    
    /**
     * 设置滚动监听器
     */
    private void setScrollListeners() {
        // 设置用户协议页面滚动监听器
        agreementFragment.setOnScrollToBottomListener(new AgreementFragment.OnScrollToBottomListener() {
            @Override
            public void onScrollToBottom() {
                // 滚动到底部，修改按钮文本和状态
                if (viewPager.getCurrentItem() == 1) {
                    btnNext.setText(R.string.intro_agree);
                    btnNext.setEnabled(true);
                    btnNext.setTextColor(getResources().getColor(R.color.white));
                    btnNext.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                }
            }
        });
        
        // 设置隐私政策页面滚动监听器
        privacyFragment.setOnScrollToBottomListener(new PrivacyFragment.OnScrollToBottomListener() {
            @Override
            public void onScrollToBottom() {
                // 滚动到底部，修改按钮文本和状态
                if (viewPager.getCurrentItem() == 2) {
                    btnNext.setText(R.string.intro_agree);
                    btnNext.setEnabled(true);
                    btnNext.setTextColor(getResources().getColor(R.color.white));
                    btnNext.setBackgroundTintList(getResources().getColorStateList(R.color.colorPrimary));
                }
            }
        });
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
        editor.commit(); // 使用commit()而不是apply()确保同步保存
        
        // 跳转到主页面
        Intent intent = new Intent(this, MainActivity.class);
        // 检查主界面是否已经存在，如果存在则直接切换过去而不是创建新实例
        intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(intent);
        finish();
    }
}