package cn.pylin.xycjd;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private NestedScrollView scrollView;
    private CardView cardPermission;
    private RadioGroup radioGroupLanguage;
    private RadioButton radioBtnChinese;
    private RadioButton radioBtnEnglish;
    
    private RadioGroup radioGroupTheme;
    private RadioButton radioBtnLight;
    private RadioButton radioBtnDark;
    private RadioButton radioBtnSystem;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 初始化控件
        initViews(view);
        
        // 设置语言选择状态
        setLanguageSelection();
        
        // 设置主题选择状态
        setThemeSelection();
        
        // 设置点击事件
        setClickListeners();
        
        // 设置平滑滚动
        setupSmoothScrolling();
        
        return view;
    }

    private void initViews(View view) {
        scrollView = view.findViewById(R.id.nested_scroll_view);
        cardPermission = view.findViewById(R.id.card_permission);
        radioGroupLanguage = view.findViewById(R.id.radio_group_language);
        radioBtnChinese = view.findViewById(R.id.radio_btn_chinese);
        radioBtnEnglish = view.findViewById(R.id.radio_btn_english);
        
        radioGroupTheme = view.findViewById(R.id.radio_group_theme);
        radioBtnLight = view.findViewById(R.id.radio_btn_light);
        radioBtnDark = view.findViewById(R.id.radio_btn_dark);
        radioBtnSystem = view.findViewById(R.id.radio_btn_system);
    }

    private void setupSmoothScrolling() {
        if (scrollView != null) {
            scrollView.setNestedScrollingEnabled(true);
        }
    }

    private void setLanguageSelection() {
        // 获取SharedPreferences中保存的语言设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String savedLanguage = preferences.getString("language", "zh");
        
        // 设置选中状态
        if (savedLanguage.equals("en")) {
            radioBtnEnglish.setChecked(true);
        } else {
            radioBtnChinese.setChecked(true);
        }
    }
    
    private void setThemeSelection() {
        // 获取SharedPreferences中保存的主题设置
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        int savedTheme = preferences.getInt("theme", AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
        
        // 设置选中状态
        switch (savedTheme) {
            case AppCompatDelegate.MODE_NIGHT_NO:
                radioBtnLight.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_YES:
                radioBtnDark.setChecked(true);
                break;
            case AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM:
                radioBtnSystem.setChecked(true);
                break;
        }
    }

    private void setClickListeners() {
        // 权限设置卡片点击事件
        cardPermission.setOnClickListener(v -> {
            // 跳转到权限设置页面
            Intent intent = new Intent(requireContext(), IntroActivity.class);
            // 设置标志位，直接跳转到权限设置页面
            intent.putExtra("direct_to_permission", true);
            startActivity(intent);
        });
        
        // 语言选择变化监听
        radioGroupLanguage.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_chinese) {
                // 切换到中文
                changeLanguage("zh");
            } else if (checkedId == R.id.radio_btn_english) {
                // 切换到英文
                changeLanguage("en");
            }
        });
        
        // 主题选择变化监听
        radioGroupTheme.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_light) {
                // 切换到浅色模式
                changeTheme(AppCompatDelegate.MODE_NIGHT_NO);
            } else if (checkedId == R.id.radio_btn_dark) {
                // 切换到深色模式
                changeTheme(AppCompatDelegate.MODE_NIGHT_YES);
            } else if (checkedId == R.id.radio_btn_system) {
                // 跟随系统
                changeTheme(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
            }
        });
    }

    private void changeLanguage(String languageCode) {
        // 保存语言设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", languageCode);
        editor.apply();
        
        // 显示切换成功提示
        Toast.makeText(requireContext(), getString(R.string.language_changed), Toast.LENGTH_SHORT).show();
        
        // 重启应用以应用新语言设置
        restartApp();
    }
    
    private void changeTheme(int themeMode) {
        // 保存主题设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("theme", themeMode);
        editor.apply();
        
        // 应用主题设置
        AppCompatDelegate.setDefaultNightMode(themeMode);
        
        // 显示切换成功提示
        Toast.makeText(requireContext(), getString(R.string.theme_changed), Toast.LENGTH_SHORT).show();
    }

    private void restartApp() {
        // 重启当前活动
        requireActivity().recreate();
    }
}
