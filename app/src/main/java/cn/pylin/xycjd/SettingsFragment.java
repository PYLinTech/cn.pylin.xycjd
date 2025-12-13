package cn.pylin.xycjd;

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
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private RadioGroup radioGroupLanguage;
    private RadioButton radioBtnChinese;
    private RadioButton radioBtnEnglish;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        // 初始化控件
        initViews(view);
        
        // 设置语言选择状态
        setLanguageSelection();
        
        // 设置点击事件
        setClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        radioGroupLanguage = view.findViewById(R.id.radio_group_language);
        radioBtnChinese = view.findViewById(R.id.radio_btn_chinese);
        radioBtnEnglish = view.findViewById(R.id.radio_btn_english);
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

    private void setClickListeners() {
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

    private void restartApp() {
        // 重启当前活动
        requireActivity().recreate();
    }
}
