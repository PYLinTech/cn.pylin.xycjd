package cn.pylin.xycjd;

import android.app.Activity;
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
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;
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
    
    // 悬浮窗相关控件
    private CardView cardFloatingWindow;
    private Switch switchFloatingWindow;
    private SeekBar seekBarSize;
    private SeekBar seekBarX;
    private SeekBar seekBarY;
    private TextView tvSizeValue;
    private TextView tvXValue;
    private TextView tvYValue;
    
    private FloatingWindowService floatingWindowService;
    private boolean isFloatingWindowEnabled = false;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;

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
        
        // 设置悬浮窗相关
        setupFloatingWindowControls();
        
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
        
        // 初始化悬浮窗相关控件
        cardFloatingWindow = view.findViewById(R.id.card_floating_window);
        switchFloatingWindow = view.findViewById(R.id.switch_floating_window);
        seekBarSize = view.findViewById(R.id.seekbar_size);
        seekBarX = view.findViewById(R.id.seekbar_x);
        seekBarY = view.findViewById(R.id.seekbar_y);
        tvSizeValue = view.findViewById(R.id.tv_size_value);
        tvXValue = view.findViewById(R.id.tv_x_value);
        tvYValue = view.findViewById(R.id.tv_y_value);
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
    
    private void setupFloatingWindowControls() {
        // 获取当前悬浮窗状态
        isFloatingWindowEnabled = FloatingWindowService.isServiceRunning(requireContext());
        switchFloatingWindow.setChecked(isFloatingWindowEnabled);
        
        // 设置初始值
        int size = FloatingWindowService.getSize(requireContext());
        int x = FloatingWindowService.getX(requireContext());
        int y = FloatingWindowService.getY(requireContext());
        
        seekBarSize.setProgress(size);
        seekBarX.setProgress(x + 500); // 调整范围，使0在中间
        seekBarY.setProgress(y + 200); // 调整范围，使-200在0位置
        
        tvSizeValue.setText(size + "dp");
        tvXValue.setText(x + "dp");
        tvYValue.setText(y + "dp");
        
        // 设置控件启用状态
        updateFloatingWindowControlsState(isFloatingWindowEnabled);
        
        // 设置监听器
        switchFloatingWindow.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                // 检查悬浮窗权限
                if (FloatingWindowPermissionManager.hasPermission(requireContext())) {
                    startFloatingWindowService();
                } else {
                    // 请求权限
                    FloatingWindowPermissionManager.requestPermission(requireActivity(), REQUEST_OVERLAY_PERMISSION);
                    // 暂时关闭开关，等待权限结果
                    switchFloatingWindow.setChecked(false);
                }
            } else {
                stopFloatingWindowService();
            }
        });
        
        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSizeValue.setText(progress + "dp");
                if (fromUser && isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarX.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int xValue = progress - 500; // 调整范围，使0在中间
                tvXValue.setText(xValue + "dp");
                if (fromUser && isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarY.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int y = progress - 200; // 调整范围，使-200在0位置
                tvYValue.setText(y + "dp");
                if (fromUser && isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
    }
    
    private void updateFloatingWindowControlsState(boolean enabled) {
        seekBarSize.setEnabled(enabled);
        seekBarX.setEnabled(enabled);
        seekBarY.setEnabled(enabled);
    }
    
    private void startFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().startService(intent);
        isFloatingWindowEnabled = true;
        updateFloatingWindowControlsState(true);
        Toast.makeText(requireContext(), getString(R.string.floating_window_enabled), Toast.LENGTH_SHORT).show();
    }
    
    private void stopFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().stopService(intent);
        isFloatingWindowEnabled = false;
        updateFloatingWindowControlsState(false);
        Toast.makeText(requireContext(), getString(R.string.floating_window_disabled), Toast.LENGTH_SHORT).show();
    }
    
    private void updateFloatingWindow() {
        int size = seekBarSize.getProgress();
        int x = seekBarX.getProgress() - 500; // 调整范围，使0在中间
        int y = seekBarY.getProgress() - 200; // 调整范围，使-200在0位置
        
        FloatingWindowService service = FloatingWindowService.getInstance();
        if (service != null) {
            service.updateFloatingWindow(size, x, y);
        } else {
            // 如果服务实例不存在，通过Intent更新
            Intent intent = new Intent(requireContext(), FloatingWindowService.class);
            intent.putExtra("update", true);
            intent.putExtra("size", size);
            intent.putExtra("x", x);
            intent.putExtra("y", y);
            requireContext().startService(intent);
        }
    }
    
    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_OVERLAY_PERMISSION) {
            if (FloatingWindowPermissionManager.hasPermission(requireContext())) {
                // 权限已授予，开启悬浮窗
                switchFloatingWindow.setChecked(true);
                startFloatingWindowService();
            } else {
                // 权限被拒绝
                FloatingWindowPermissionManager.showPermissionDeniedMessage(requireContext());
            }
        }
    }
}
