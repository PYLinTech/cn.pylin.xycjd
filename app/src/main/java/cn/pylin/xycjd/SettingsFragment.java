package cn.pylin.xycjd;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
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
import androidx.core.app.NotificationCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class SettingsFragment extends Fragment {

    private NestedScrollView scrollView;
    private CardView cardPermission;
    private TextView tvPermissionStatus;
    private Button btnPermissionAction;
    private RadioGroup radioGroupLanguage;
    private RadioButton radioBtnChinese;
    private RadioButton radioBtnEnglish;
    
    private RadioGroup radioGroupTheme;
    private RadioButton radioBtnLight;
    private RadioButton radioBtnDark;
    private RadioButton radioBtnSystem;

    // 通知模式相关控件
    private RadioGroup radioGroupNotificationMode;
    private RadioButton radioBtnSuperIslandOnly;
    private RadioButton radioBtnNotificationBarOnly;
    private RadioButton radioBtnModeBoth;
    
    // 悬浮窗相关控件
    private CardView cardFloatingWindow;
    private LinearLayout layoutPlaceholder;
    private LinearLayout layoutFloatingContent;
    private SeekBar seekBarSize;
    private SeekBar seekBarX;
    private SeekBar seekBarY;
    private TextView tvSizeValue;
    private TextView tvXValue;
    private TextView tvYValue;
    private ImageButton btnSizeDecrease;
    private ImageButton btnSizeIncrease;
    private ImageButton btnXDecrease;
    private ImageButton btnXIncrease;
    private ImageButton btnYDecrease;
    private ImageButton btnYIncrease;
    private Button btnResetPosition;
    
    // 服务状态相关控件
    private CardView cardServiceStatus;
    private TextView tvServiceStatus;
    private Button btnServiceToggle;

    // 本地学习相关控件
    private CardView cardLocalLearning;
    private TextView tvLocalLearningStatus;
    private Button btnLocalLearningToggle;

    // 学习配置相关控件
    private CardView cardLearningConfig;
    private TextView tvFilteringDegreeValue;
    private SeekBar seekBarFilteringDegree;
    private TextView tvLearningDegreeValue;
    private SeekBar seekBarLearningDegree;
    private Button btnResetLearningConfig;
    private Button btnClearLearningModel;
    
    // 测试通知相关控件
    private CardView cardTestNotification;
    private Button btnSendTestNotification;
    
    private boolean isFloatingWindowEnabled = false;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final String TEST_NOTIFICATION_CHANNEL_ID = "test_notification_channel";
    private static final String PREF_LOCAL_LEARNING_ENABLED = "pref_local_learning_enabled";
    private static final String PREF_FILTERING_DEGREE = "pref_filtering_degree";
    private static final String PREF_LEARNING_DEGREE = "pref_learning_degree";
    private static final String PREF_NOTIFICATION_MODE = "pref_notification_mode";
    public static final String MODE_SUPER_ISLAND_ONLY = "mode_super_island_only";
    public static final String MODE_NOTIFICATION_BAR_ONLY = "mode_notification_bar_only";
    public static final String MODE_BOTH = "mode_both";
    private static final String PREF_SETTINGS_SCROLL_Y = "pref_settings_scroll_y";

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

        // 设置通知模式选择状态
        setupNotificationModeControls();
        
        // 设置点击事件
        setClickListeners();
        
        // 设置权限状态相关
        setupPermissionControls();
        
        // 设置悬浮窗相关
        setupFloatingWindowControls();
        
        // 设置服务状态相关
        setupServiceStatusControls();

        // 设置本地学习相关
        setupLocalLearningControls();
        
        // 设置学习配置相关
        setupLearningConfigControls();
        
        // 设置测试通知相关
        setupTestNotificationControls();
        
        return view;
    }

    private void initViews(View view) {
        // 初始化控件
        scrollView = view.findViewById(R.id.nested_scroll_view);
        cardPermission = view.findViewById(R.id.card_permission);
        tvPermissionStatus = view.findViewById(R.id.tv_permission_status);
        btnPermissionAction = view.findViewById(R.id.btn_permission_action);
        radioGroupLanguage = view.findViewById(R.id.radio_group_language);
        radioBtnChinese = view.findViewById(R.id.radio_btn_chinese);
        radioBtnEnglish = view.findViewById(R.id.radio_btn_english);
        
        radioGroupTheme = view.findViewById(R.id.radio_group_theme);
        radioBtnLight = view.findViewById(R.id.radio_btn_light);
        radioBtnDark = view.findViewById(R.id.radio_btn_dark);
        radioBtnSystem = view.findViewById(R.id.radio_btn_system);

        // 初始化通知模式控件
        radioGroupNotificationMode = view.findViewById(R.id.radio_group_notification_mode);
        radioBtnSuperIslandOnly = view.findViewById(R.id.radio_btn_super_island_only);
        radioBtnNotificationBarOnly = view.findViewById(R.id.radio_btn_notification_bar_only);
        radioBtnModeBoth = view.findViewById(R.id.radio_btn_mode_both);
        
        // 初始化悬浮窗相关控件
        cardFloatingWindow = view.findViewById(R.id.card_floating_window);
        layoutPlaceholder = view.findViewById(R.id.layout_placeholder);
        layoutFloatingContent = view.findViewById(R.id.layout_floating_content);
        seekBarSize = view.findViewById(R.id.seekbar_size);
        seekBarX = view.findViewById(R.id.seekbar_x);
        seekBarY = view.findViewById(R.id.seekbar_y);
        tvSizeValue = view.findViewById(R.id.tv_size_value);
        tvXValue = view.findViewById(R.id.tv_x_value);
        tvYValue = view.findViewById(R.id.tv_y_value);
        btnSizeDecrease = view.findViewById(R.id.btn_size_decrease);
        btnSizeIncrease = view.findViewById(R.id.btn_size_increase);
        btnXDecrease = view.findViewById(R.id.btn_x_decrease);
        btnXIncrease = view.findViewById(R.id.btn_x_increase);
        btnYDecrease = view.findViewById(R.id.btn_y_decrease);
        btnYIncrease = view.findViewById(R.id.btn_y_increase);
        btnResetPosition = view.findViewById(R.id.btn_reset_position);
        
        // 初始化服务状态相关控件
        cardServiceStatus = view.findViewById(R.id.card_service_status);
        tvServiceStatus = view.findViewById(R.id.tv_service_status);
        btnServiceToggle = view.findViewById(R.id.btn_service_toggle);

        // 初始化本地学习相关控件
        cardLocalLearning = view.findViewById(R.id.card_local_learning);
        tvLocalLearningStatus = view.findViewById(R.id.tv_local_learning_status);
        btnLocalLearningToggle = view.findViewById(R.id.btn_local_learning_toggle);
        
        // 初始化学习配置相关控件
        cardLearningConfig = view.findViewById(R.id.card_learning_config);
        tvFilteringDegreeValue = view.findViewById(R.id.tv_filtering_degree_value);
        seekBarFilteringDegree = view.findViewById(R.id.seekbar_filtering_degree);
        tvLearningDegreeValue = view.findViewById(R.id.tv_learning_degree_value);
        seekBarLearningDegree = view.findViewById(R.id.seekbar_learning_degree);
        btnResetLearningConfig = view.findViewById(R.id.btn_reset_learning_config);
        btnClearLearningModel = view.findViewById(R.id.btn_clear_learning_model);
        // 初始化测试通知相关控件
        cardTestNotification = view.findViewById(R.id.card_test_notification);
        btnSendTestNotification = view.findViewById(R.id.btn_send_test_notification);
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

    private void setupNotificationModeControls() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        String mode = preferences.getString(PREF_NOTIFICATION_MODE, MODE_SUPER_ISLAND_ONLY);
        
        // 设置选中状态
        switch (mode) {
            case MODE_SUPER_ISLAND_ONLY:
                radioBtnSuperIslandOnly.setChecked(true);
                break;
            case MODE_NOTIFICATION_BAR_ONLY:
                radioBtnNotificationBarOnly.setChecked(true);
                break;
            case MODE_BOTH:
                radioBtnModeBoth.setChecked(true);
                break;
        }

        // 设置监听器
        radioGroupNotificationMode.setOnCheckedChangeListener((group, checkedId) -> {
            SharedPreferences.Editor editor = preferences.edit();
            if (checkedId == R.id.radio_btn_super_island_only) {
                editor.putString(PREF_NOTIFICATION_MODE, MODE_SUPER_ISLAND_ONLY);
            } else if (checkedId == R.id.radio_btn_notification_bar_only) {
                editor.putString(PREF_NOTIFICATION_MODE, MODE_NOTIFICATION_BAR_ONLY);
            } else if (checkedId == R.id.radio_btn_mode_both) {
                editor.putString(PREF_NOTIFICATION_MODE, MODE_BOTH);
            }
            editor.apply();
        });
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
        // 保存当前滚动位置
        saveScrollPosition();
        
        // 保存语言设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", languageCode);
        editor.apply();
        
        // 重启应用以应用新语言设置
        restartApp();
    }
    
    private void changeTheme(int themeMode) {
        // 保存当前滚动位置
        saveScrollPosition();
        
        // 保存主题设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("theme", themeMode);
        editor.apply();
        
        // 应用主题设置
        AppCompatDelegate.setDefaultNightMode(themeMode);
    }

    private void restartApp() {
        // 重启当前活动
        requireActivity().recreate();
    }
    
    private void setupFloatingWindowControls() {
        // 获取当前悬浮窗状态
        isFloatingWindowEnabled = FloatingWindowService.isServiceRunning(requireContext());
        
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
        
        // 设置占位符和内容布局的可见性
        updateFloatingWindowLayoutVisibility(isFloatingWindowEnabled);
        
        // 设置加减按钮点击事件
        setupButtonListeners();
        
        // 设置还原默认位置按钮点击事件
        btnResetPosition.setOnClickListener(v -> {
            // 设置默认值
            seekBarSize.setProgress(100); // 默认大小
            seekBarX.setProgress(500); // 默认水平位置(0)
            seekBarY.setProgress(100); // 默认垂直位置(-100)
            
            // 更新显示值
            tvSizeValue.setText("100dp");
            tvXValue.setText("0dp");
            tvYValue.setText("-100dp");
            
            // 如果悬浮窗已启用，立即更新
            if (isFloatingWindowEnabled) {
                updateFloatingWindow();
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
        // 悬浮窗控件始终启用，不受服务状态影响
        seekBarSize.setEnabled(true);
        seekBarX.setEnabled(true);
        seekBarY.setEnabled(true);
        btnSizeDecrease.setEnabled(true);
        btnSizeIncrease.setEnabled(true);
        btnXDecrease.setEnabled(true);
        btnXIncrease.setEnabled(true);
        btnYDecrease.setEnabled(true);
        btnYIncrease.setEnabled(true);
        btnResetPosition.setEnabled(true);
    }
    
    private void setupButtonListeners() {
        // 大小加减按钮
        btnSizeDecrease.setOnClickListener(v -> {
            int currentProgress = seekBarSize.getProgress();
            if (currentProgress > 1) { // 设置最小值为1
                seekBarSize.setProgress(currentProgress - 1);
                tvSizeValue.setText((currentProgress - 1) + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
        
        btnSizeIncrease.setOnClickListener(v -> {
            int currentProgress = seekBarSize.getProgress();
            if (currentProgress < seekBarSize.getMax()) { // 确保不超过最大值
                seekBarSize.setProgress(currentProgress + 1);
                tvSizeValue.setText((currentProgress + 1) + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
        
        // 水平位置加减按钮
        btnXDecrease.setOnClickListener(v -> {
            int currentProgress = seekBarX.getProgress();
            if (currentProgress > 0) { // 设置最小值为0
                seekBarX.setProgress(currentProgress - 1);
                int xValue = (currentProgress - 1) - 500; // 调整范围，使0在中间
                tvXValue.setText(xValue + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
        
        btnXIncrease.setOnClickListener(v -> {
            int currentProgress = seekBarX.getProgress();
            if (currentProgress < seekBarX.getMax()) { // 确保不超过最大值
                seekBarX.setProgress(currentProgress + 1);
                int xValue = (currentProgress + 1) - 500; // 调整范围，使0在中间
                tvXValue.setText(xValue + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
        
        // 垂直位置加减按钮
        btnYDecrease.setOnClickListener(v -> {
            int currentProgress = seekBarY.getProgress();
            if (currentProgress > 0) { // 设置最小值为0
                seekBarY.setProgress(currentProgress - 1);
                int y = (currentProgress - 1) - 200; // 调整范围，使-200在0位置
                tvYValue.setText(y + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
        
        btnYIncrease.setOnClickListener(v -> {
            int currentProgress = seekBarY.getProgress();
            if (currentProgress < seekBarY.getMax()) { // 确保不超过最大值
                seekBarY.setProgress(currentProgress + 1);
                int y = (currentProgress + 1) - 200; // 调整范围，使-200在0位置
                tvYValue.setText(y + "dp");
                if (isFloatingWindowEnabled) {
                    updateFloatingWindow();
                }
            }
        });
    }
    
    private void startFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().startService(intent);
        isFloatingWindowEnabled = true;
    }
    
    private void stopFloatingWindowService() {
        Intent intent = new Intent(requireContext(), FloatingWindowService.class);
        requireContext().stopService(intent);
        isFloatingWindowEnabled = false;
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
                startFloatingWindowService();
                updateServiceStatus();
            } else {
                // 权限被拒绝
                FloatingWindowPermissionManager.showPermissionDeniedMessage(requireContext());
                updateServiceStatus();
            }
        }
    }
    
    private void setupServiceStatusControls() {
        // 初始更新服务状态
        updateServiceStatus();
        // 悬浮窗控件始终启用
        updateFloatingWindowControlsState(true);
        
        // 设置服务切换按钮点击事件
        btnServiceToggle.setOnClickListener(v -> {
            if (isFloatingWindowEnabled) {
                // 检查权限状态
                PermissionChecker.PermissionStatus status = PermissionChecker.checkAllPermissions(requireContext());
                if (!status.hasOverlayPermission || !status.hasAccessibilityPermission) {
                    // 跳转到权限设置页面
                    Intent intent = new Intent(requireContext(), IntroActivity.class);
                    intent.putExtra("direct_to_permission", true);
                    startActivity(intent);
                } else {
                    // 当前服务已开启且权限正常，执行关闭操作
                    stopFloatingWindowService();
                }
            } else {
                // 当前服务未开启，执行开启操作
                // 检查悬浮窗权限
                if (FloatingWindowPermissionManager.hasPermission(requireContext())) {
                    startFloatingWindowService();
                } else {
                    // 请求权限
                    FloatingWindowPermissionManager.requestPermission(requireActivity(), REQUEST_OVERLAY_PERMISSION);
                }
            }
            // 更新服务状态显示
            updateServiceStatus();
        });
    }
    
    private void updateServiceStatus() {
        // 检测当前悬浮窗服务状态
        isFloatingWindowEnabled = FloatingWindowService.isServiceRunning(requireContext());
        
        if (isFloatingWindowEnabled) {
            // 服务已开启，检查权限
            PermissionChecker.PermissionStatus status = PermissionChecker.checkAllPermissions(requireContext());
            if (!status.hasOverlayPermission || !status.hasAccessibilityPermission) {
                // 服务异常，关键权限未授予
                tvServiceStatus.setText(getString(R.string.service_abnormal_permission_missing));
                tvServiceStatus.setTextColor(getResources().getColor(R.color.colorWarning, null));
                btnServiceToggle.setText(getString(R.string.grant_permission));
                btnServiceToggle.setBackgroundResource(R.drawable.btn_warning_background);
            } else {
                // 服务正常运行
                tvServiceStatus.setText(getString(R.string.service_running));
                tvServiceStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
                btnServiceToggle.setText(getString(R.string.stop_service));
                btnServiceToggle.setBackgroundResource(R.drawable.btn_error_background);
            }
        } else {
            // 服务未开启
            tvServiceStatus.setText(getString(R.string.service_stopped));
            tvServiceStatus.setTextColor(getResources().getColor(R.color.colorError, null));
            btnServiceToggle.setText(getString(R.string.start_service));
            btnServiceToggle.setBackgroundResource(R.drawable.btn_primary_background);
        }
        
        // 更新悬浮窗布局可见性
        updateFloatingWindowLayoutVisibility(isFloatingWindowEnabled);
    }
    
    private void updateFloatingWindowLayoutVisibility(boolean isServiceRunning) {
        if (isServiceRunning) {
            // 服务已启动，显示设置选项，隐藏占位符
            layoutPlaceholder.setVisibility(View.GONE);
            layoutFloatingContent.setVisibility(View.VISIBLE);
        } else {
            // 服务未启动，显示占位符，隐藏设置选项
            layoutPlaceholder.setVisibility(View.VISIBLE);
            layoutFloatingContent.setVisibility(View.GONE);
        }
    }

    private void setupLocalLearningControls() {
        // 更新UI状态
        updateLocalLearningUI();

        // 设置点击事件
        btnLocalLearningToggle.setOnClickListener(v -> {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            boolean isEnabled = preferences.getBoolean(PREF_LOCAL_LEARNING_ENABLED, false);
            
            // 切换状态
            SharedPreferences.Editor editor = preferences.edit();
            editor.putBoolean(PREF_LOCAL_LEARNING_ENABLED, !isEnabled);
            editor.apply();
            
            // 更新UI
            updateLocalLearningUI();
        });
    }

    private void updateLocalLearningUI() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        boolean isEnabled = preferences.getBoolean(PREF_LOCAL_LEARNING_ENABLED, false);
        
        if (isEnabled) {
            // 获取已过滤数量
            int filteredCount = FilteredNotificationManager.getInstance(requireContext()).getAllNotifications().size();
            tvLocalLearningStatus.setText(getString(R.string.local_learning_running, filteredCount));
            tvLocalLearningStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            
            btnLocalLearningToggle.setText(getString(R.string.stop_learning));
            btnLocalLearningToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            tvLocalLearningStatus.setText(getString(R.string.local_learning_stopped));
            tvLocalLearningStatus.setTextColor(getResources().getColor(R.color.colorError, null));
            
            btnLocalLearningToggle.setText(getString(R.string.start_learning));
            btnLocalLearningToggle.setBackgroundResource(R.drawable.btn_primary_background);
        }
    }
    
    private void setupLearningConfigControls() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        
        // 获取保存的值，如果不存在则使用默认值
        float filteringDegree = preferences.getFloat(PREF_FILTERING_DEGREE, 5.0f);
        float learningDegree = preferences.getFloat(PREF_LEARNING_DEGREE, 3.0f);
        
        // 设置初始值
        // SeekBar范围是0-100，对应0.0-10.0
        seekBarFilteringDegree.setProgress((int) (filteringDegree * 10));
        tvFilteringDegreeValue.setText(String.format("%.1f", filteringDegree));
        
        seekBarLearningDegree.setProgress((int) (learningDegree * 10));
        tvLearningDegreeValue.setText(String.format("%.1f", learningDegree));
        
        // 设置监听器
        seekBarFilteringDegree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 10.0f;
                tvFilteringDegreeValue.setText(String.format("%.1f", value));
                
                if (fromUser) {
                    // 保存设置
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putFloat(PREF_FILTERING_DEGREE, value);
                    editor.apply();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        seekBarLearningDegree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 10.0f;
                tvLearningDegreeValue.setText(String.format("%.1f", value));
                
                if (fromUser) {
                    // 保存设置
                    SharedPreferences.Editor editor = preferences.edit();
                    editor.putFloat(PREF_LEARNING_DEGREE, value);
                    editor.apply();
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 重置按钮点击事件
        btnResetLearningConfig.setOnClickListener(v -> {
            // 恢复默认值
            float defaultFilteringDegree = 5.0f;
            float defaultLearningDegree = 3.0f;

            // 更新 SharedPreferences
            SharedPreferences.Editor editor = preferences.edit();
            editor.putFloat(PREF_FILTERING_DEGREE, defaultFilteringDegree);
            editor.putFloat(PREF_LEARNING_DEGREE, defaultLearningDegree);
            editor.apply();

            // 更新 UI
            seekBarFilteringDegree.setProgress((int) (defaultFilteringDegree * 10));
            tvFilteringDegreeValue.setText(String.format("%.1f", defaultFilteringDegree));

            seekBarLearningDegree.setProgress((int) (defaultLearningDegree * 10));
            tvLearningDegreeValue.setText(String.format("%.1f", defaultLearningDegree));
        });

        // 清空学习模型按钮点击事件
        btnClearLearningModel.setOnClickListener(v -> {
            NotificationMLManager.getInstance(requireContext()).clearModel();
            Toast.makeText(requireContext(), getString(R.string.clear_learning_model_success), Toast.LENGTH_SHORT).show();
        });
    }
    
    private void setupPermissionControls() {
        // 初始更新权限状态
        updatePermissionStatus();
        
        // 设置权限操作按钮点击事件
        btnPermissionAction.setOnClickListener(v -> {
            // 跳转到权限设置页面
            Intent intent = new Intent(requireContext(), IntroActivity.class);
            // 设置标志位，直接跳转到权限设置页面
            intent.putExtra("direct_to_permission", true);
            startActivity(intent);
        });
    }
    

    
    private void updatePermissionStatus() {
        // 使用PermissionChecker检查各项权限状态
        PermissionChecker.PermissionStatus status = PermissionChecker.checkAllPermissions(requireContext());
        
        if (status.deniedCount == 0) {
            // 全部权限已开启
            tvPermissionStatus.setText(getString(R.string.permission_all_granted));
            btnPermissionAction.setText(getString(R.string.configure_permission));
            btnPermissionAction.setBackgroundResource(R.drawable.btn_secondary_background);
            btnPermissionAction.setTextColor(getResources().getColor(android.R.color.white, null));
            btnPermissionAction.setVisibility(View.VISIBLE);
        } else {
            // 有权限未开启
            tvPermissionStatus.setText(String.format(getString(R.string.permission_some_denied), status.deniedCount));
            btnPermissionAction.setText(getString(R.string.go_to_permission));
            btnPermissionAction.setBackgroundResource(R.drawable.btn_primary_background);
            btnPermissionAction.setTextColor(getResources().getColor(android.R.color.white, null));
            btnPermissionAction.setVisibility(View.VISIBLE);
        }
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 更新权限状态
        updatePermissionStatus();
        // 更新服务状态
        updateServiceStatus();
        // 恢复滚动位置
        restoreScrollPosition();
    }
    
    private void setupTestNotificationControls() {
        // 创建通知通道（Android 8.0+）
        createNotificationChannel();
        
        // 设置发送测试通知按钮点击事件
        btnSendTestNotification.setOnClickListener(v -> {
            if (!isFloatingWindowEnabled) {
                if (FloatingWindowPermissionManager.hasPermission(requireContext())) {
                    startFloatingWindowService();
                    updateServiceStatus();
                } else {
                    FloatingWindowPermissionManager.requestPermission(requireActivity(), REQUEST_OVERLAY_PERMISSION);
                    return;
                }
            }
            sendTestNotification();
        });
    }
    
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
            
            NotificationChannel channel = new NotificationChannel(
                TEST_NOTIFICATION_CHANNEL_ID,
                getString(R.string.test_notification_channel_name),
                NotificationManager.IMPORTANCE_DEFAULT
            );
            channel.setDescription(getString(R.string.test_notification_channel_desc));
            channel.enableLights(true);
            channel.enableVibration(true);
            
            notificationManager.createNotificationChannel(channel);
        }
    }
    
    private void sendTestNotification() {
        // 创建点击通知时跳转的Intent
        Intent intent = new Intent(requireContext(), MainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(requireContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), TEST_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.test_notification_title))
            .setContentText(getString(R.string.test_notification_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setContentIntent(pendingIntent) // 设置点击跳转Intent
            .setAutoCancel(true);
            
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        // 使用当前时间戳作为唯一的通知ID，确保每次发送都是新的通知
        int uniqueNotificationId = (int) System.currentTimeMillis();
        notificationManager.notify(uniqueNotificationId, builder.build());
        
        Toast.makeText(requireContext(), getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show();
    }

    private void saveScrollPosition() {
        if (scrollView != null) {
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
            preferences.edit().putInt(PREF_SETTINGS_SCROLL_Y, scrollView.getScrollY()).apply();
        }
    }

    private void restoreScrollPosition() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        if (preferences.contains(PREF_SETTINGS_SCROLL_Y)) {
            final int scrollY = preferences.getInt(PREF_SETTINGS_SCROLL_Y, 0);
            if (scrollView != null) {
                scrollView.post(() -> scrollView.scrollTo(0, scrollY));
            }
            preferences.edit().remove(PREF_SETTINGS_SCROLL_Y).apply();
        }
    }
}
