package cn.pylin.xycjd.ui.fragment;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.cardview.widget.CardView;
import androidx.core.app.NotificationCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import cn.pylin.xycjd.model.local.LocalModelManager;
import cn.pylin.xycjd.manager.FilteredNotificationManager;
import cn.pylin.xycjd.manager.FloatingWindowPermissionManager;
import cn.pylin.xycjd.model.online.OnlineModelManager;
import cn.pylin.xycjd.utils.PermissionChecker;
import cn.pylin.xycjd.R;
import cn.pylin.xycjd.manager.SharedPreferencesManager;
import cn.pylin.xycjd.service.FloatingWindowService;
import cn.pylin.xycjd.ui.activity.IntroActivity;
import cn.pylin.xycjd.ui.activity.MainActivity;

public class SettingsFragment extends Fragment {

    private NestedScrollView scrollView;
    private CardView cardPermission;
    private TextView tvPermissionStatus;
    private Button btnPermissionAction;
    private RadioGroup radioGroupLanguage;
    private RadioButton radioBtnChinese;
    private RadioButton radioBtnEnglish;
    private RadioButton radioBtnZhTw;
    
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

    // 模型过滤相关控件
    private CardView cardModelFiltering;
    private TextView tvModelFilteringStatus;
    private Button btnModelFilteringToggle;

    // 过滤模型控件
    private RadioGroup radioGroupFilterModel;
    private RadioButton radioBtnModelLocal;
    private RadioButton radioBtnModelOnline;

    // 学习配置相关控件
    private CardView cardLearningConfig;
    private TextView tvFilteringDegreeValue;
    private SeekBar seekBarFilteringDegree;
    private TextView tvLearningDegreeValue;
    private SeekBar seekBarLearningDegree;
    private Button btnResetLearningConfig;
    private Button btnClearLearningModel;

    // 在线模型配置相关控件
    private CardView cardOnlineModelConfig;
    private TextView tvOnlineFilteringDegreeValue;
    private SeekBar seekBarOnlineFilteringDegree;
    private TextView tvTemperatureValue;
    private SeekBar seekBarTemperature;
    private Button btnResetOnlineFilteringDegreeConfig;
    private Button btnApiConfig;
    
    // 在线模型流程控制相关控件
    private RadioGroup radioGroupOnlineModelProcess;
    private RadioButton radioBtnShowFirst;
    private RadioButton radioBtnCheckFirst;

    // 测试通知相关控件
    private CardView cardTestNotification;
    private Button btnSendTestNotification;
    
    // 声音与震动相关控件
    private CardView cardSoundVibration;
    private TextView tvSoundStatus;
    private Button btnSoundToggle;
    private TextView tvVibrationStatus;
    private Button btnVibrationToggle;
    private TextView tvVibrationIntensityValue;
    private SeekBar seekBarVibrationIntensity;
    private ImageButton btnVibrationIntensityDecrease;
    private ImageButton btnVibrationIntensityIncrease;
    
    // 动画速率相关控件
    private CardView cardAnimationSpeed;
    private TextView tvSpeedValue;
    private SeekBar seekBarSpeed;
    private ImageButton btnSpeedDecrease;
    private ImageButton btnSpeedIncrease;
    private Button btnResetSpeed;
    
    private boolean isFloatingWindowEnabled = false;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final String TEST_NOTIFICATION_CHANNEL_ID = "test_notification_channel";
    private static final String PREF_MODEL_FILTERING_ENABLED = "pref_model_filtering_enabled";
    private static final String PREF_FILTERING_DEGREE = "pref_filtering_degree";
    private static final String PREF_LEARNING_DEGREE = "pref_learning_degree";
    private static final String PREF_ONLINE_FILTERING_DEGREE = "pref_online_filtering_degree";
    private static final String PREF_ONLINE_MODEL_PROMPT = "pref_online_model_prompt";
    private static final String PREF_TEMPERATURE = "pref_temperature";
    private static final String PREF_NOTIFICATION_MODE = "pref_notification_mode";
    public static final String MODE_SUPER_ISLAND_ONLY = "mode_super_island_only";
    public static final String MODE_NOTIFICATION_BAR_ONLY = "mode_notification_bar_only";
    public static final String MODE_BOTH = "mode_both";
    
    private static final String PREF_FILTER_MODEL = "pref_filter_model";
    public static final String MODEL_LOCAL = "model_local";
    public static final String MODEL_ONLINE = "model_online";

    private static final String PREF_SETTINGS_SCROLL_Y = "pref_settings_scroll_y";
    private static final String PREF_ANIMATION_SPEED = "pref_animation_speed";

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

        // 设置模型过滤相关
        setupModelFilteringControls();
        
        // 设置过滤模型相关
        setupFilterModelControls();
        
        // 设置学习配置相关
        setupLearningConfigControls();

        // 设置在线模型配置相关
        setupOnlineModelConfigControls();
        
        // 设置测试通知相关
        setupTestNotificationControls();
        
        // 设置动画速率相关
        setupAnimationSpeedControls();
        
        // 设置声音与震动相关
        setupSoundVibrationControls();
        
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
        radioBtnZhTw = view.findViewById(R.id.radio_btn_zh_tw);
        
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

        // 初始化模型过滤相关控件
        cardModelFiltering = view.findViewById(R.id.card_model_filtering);
        tvModelFilteringStatus = view.findViewById(R.id.tv_model_filtering_status);
        btnModelFilteringToggle = view.findViewById(R.id.btn_model_filtering_toggle);
        
        // 初始化过滤模型控件
        radioGroupFilterModel = view.findViewById(R.id.radio_group_filter_model);
        radioBtnModelLocal = view.findViewById(R.id.radio_btn_model_local);
        radioBtnModelOnline = view.findViewById(R.id.radio_btn_model_online);

        // 初始化学习配置相关控件
        cardLearningConfig = view.findViewById(R.id.card_learning_config);
        tvFilteringDegreeValue = view.findViewById(R.id.tv_filtering_degree_value);
        seekBarFilteringDegree = view.findViewById(R.id.seekbar_filtering_degree);
        tvLearningDegreeValue = view.findViewById(R.id.tv_learning_degree_value);
        seekBarLearningDegree = view.findViewById(R.id.seekbar_learning_degree);
        btnResetLearningConfig = view.findViewById(R.id.btn_reset_learning_config);
        btnClearLearningModel = view.findViewById(R.id.btn_clear_learning_model);

        // 初始化在线模型配置相关控件
        cardOnlineModelConfig = view.findViewById(R.id.card_online_model_config);
        tvOnlineFilteringDegreeValue = view.findViewById(R.id.tv_online_filtering_degree_value);
        seekBarOnlineFilteringDegree = view.findViewById(R.id.seekbar_online_filtering_degree);
        tvTemperatureValue = view.findViewById(R.id.tv_temperature_value);
        seekBarTemperature = view.findViewById(R.id.seekbar_temperature);
        btnResetOnlineFilteringDegreeConfig = view.findViewById(R.id.btn_reset_online_filtering_degree_config);
        btnApiConfig = view.findViewById(R.id.btn_api_config);

        // 初始化测试通知相关控件
        cardTestNotification = view.findViewById(R.id.card_test_notification);
        btnSendTestNotification = view.findViewById(R.id.btn_send_test_notification);

        // 初始化动画速率控件
        cardAnimationSpeed = view.findViewById(R.id.card_animation_speed);
        tvSpeedValue = view.findViewById(R.id.tv_speed_value);
        seekBarSpeed = view.findViewById(R.id.seekbar_speed);
        btnSpeedDecrease = view.findViewById(R.id.btn_speed_decrease);
        btnSpeedIncrease = view.findViewById(R.id.btn_speed_increase);
        btnResetSpeed = view.findViewById(R.id.btn_reset_speed);
        
        // 初始化在线模型流程控制相关控件
        radioGroupOnlineModelProcess = view.findViewById(R.id.radio_group_online_model_process);
        radioBtnShowFirst = view.findViewById(R.id.radio_btn_show_first);
        radioBtnCheckFirst = view.findViewById(R.id.radio_btn_check_first);
        
        // 初始化声音与震动相关控件
        cardSoundVibration = view.findViewById(R.id.card_sound_vibration);
        tvSoundStatus = view.findViewById(R.id.tv_sound_status);
        btnSoundToggle = view.findViewById(R.id.btn_sound_toggle);
        tvVibrationStatus = view.findViewById(R.id.tv_vibration_status);
        btnVibrationToggle = view.findViewById(R.id.btn_vibration_toggle);
        tvVibrationIntensityValue = view.findViewById(R.id.tv_vibration_intensity_value);
        seekBarVibrationIntensity = view.findViewById(R.id.seekbar_vibration_intensity);
        btnVibrationIntensityDecrease = view.findViewById(R.id.btn_vibration_intensity_decrease);
        btnVibrationIntensityIncrease = view.findViewById(R.id.btn_vibration_intensity_increase);
        
    }

    private void setLanguageSelection() {
        // 从SharedPreferences管理器获取语言设置
        String savedLanguage = SharedPreferencesManager.getInstance(requireContext()).getLanguage();
        
        // 设置选中状态，如果没有设置过语言，默认选中中文
        if (savedLanguage.equals("en")) {
            radioBtnEnglish.setChecked(true);
        } else if (savedLanguage.equals("zh-rTW")) {
            radioBtnZhTw.setChecked(true);
        } else {
            // 默认值（"zh"）或未设置时选中中文
            radioBtnChinese.setChecked(true);
        }
    }
    
    private void setThemeSelection() {
        // 从SharedPreferences管理器获取主题设置
        int savedTheme = SharedPreferencesManager.getInstance(requireContext()).getTheme();
        
        // 设置选中状态，如果没有设置过主题，默认选中"跟随系统"
        if (savedTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            radioBtnLight.setChecked(true);
        } else if (savedTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            radioBtnDark.setChecked(true);
        } else {
            // 默认值（0）或 MODE_NIGHT_FOLLOW_SYSTEM 都选中"跟随系统"
            radioBtnSystem.setChecked(true);
        }
    }

    private void setupNotificationModeControls() {
        // 从SharedPreferences管理器获取通知模式设置
        String mode = SharedPreferencesManager.getInstance(requireContext()).getNotificationMode();
        
        // 设置选中状态，如果没有设置过通知模式，默认选中"仅超级岛"
        if (mode == null || mode.isEmpty() || mode.equals(MODE_SUPER_ISLAND_ONLY)) {
            radioBtnSuperIslandOnly.setChecked(true);
        } else if (mode.equals(MODE_NOTIFICATION_BAR_ONLY)) {
            radioBtnNotificationBarOnly.setChecked(true);
        } else if (mode.equals(MODE_BOTH)) {
            radioBtnModeBoth.setChecked(true);
        } else {
            // 未知模式，默认选中"仅超级岛"
            radioBtnSuperIslandOnly.setChecked(true);
        }

        // 设置监听器
        radioGroupNotificationMode.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_super_island_only) {
                SharedPreferencesManager.getInstance(requireContext()).setNotificationMode(MODE_SUPER_ISLAND_ONLY);
            } else if (checkedId == R.id.radio_btn_notification_bar_only) {
                SharedPreferencesManager.getInstance(requireContext()).setNotificationMode(MODE_NOTIFICATION_BAR_ONLY);
            } else if (checkedId == R.id.radio_btn_mode_both) {
                SharedPreferencesManager.getInstance(requireContext()).setNotificationMode(MODE_BOTH);
            }
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
            } else if (checkedId == R.id.radio_btn_zh_tw) {
                // 切换到繁体中文
                changeLanguage("zh-rTW");
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
        
        // 使用SharedPreferences管理器保存语言设置
        SharedPreferencesManager.getInstance(requireContext()).setLanguage(languageCode);
        
        // 重启应用以应用新语言设置
        restartApp();
    }
    
    private void changeTheme(int themeMode) {
        // 保存当前滚动位置
        saveScrollPosition();
        
        // 使用SharedPreferences管理器保存主题设置
        SharedPreferencesManager.getInstance(requireContext()).setTheme(themeMode);
        
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
        
        tvSizeValue.setText(getString(R.string.value_dp, size));
        tvXValue.setText(getString(R.string.value_dp, x));
        tvYValue.setText(getString(R.string.value_dp, y));
        
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
            tvSizeValue.setText(getString(R.string.default_size));
            tvXValue.setText(getString(R.string.default_x));
            tvYValue.setText(getString(R.string.default_y));
            
            // 如果悬浮窗已启用，立即更新
            if (isFloatingWindowEnabled) {
                updateFloatingWindow();
            }
        });
        
        seekBarSize.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvSizeValue.setText(getString(R.string.value_dp, progress));
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
                tvXValue.setText(getString(R.string.value_dp, xValue));
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
                tvYValue.setText(getString(R.string.value_dp, y));
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
                tvYValue.setText(getString(R.string.value_dp, y));
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
                tvYValue.setText(getString(R.string.value_dp, y));
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
                // 当前服务已开启，执行关闭操作
                stopFloatingWindowService();
                // 保存用户主动关闭的状态
                SharedPreferencesManager.getInstance(requireContext()).setFloatingWindowEnabled(false);
            } else {
                // 当前服务未开启，执行开启操作
                // 检查悬浮窗权限
                if (FloatingWindowPermissionManager.hasPermission(requireContext())) {
                    startFloatingWindowService();
                    // 保存用户主动开启的状态
                    SharedPreferencesManager.getInstance(requireContext()).setFloatingWindowEnabled(true);
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
            // 服务正在运行
            tvServiceStatus.setText(getString(R.string.service_running));
            tvServiceStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnServiceToggle.setText(getString(R.string.stop_service));
            btnServiceToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            // 服务已停止
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

    private void setupModelFilteringControls() {
        // 更新UI状态
        updateModelFilteringUI();

        // 设置点击事件
        btnModelFilteringToggle.setOnClickListener(v -> {
            // 使用SharedPreferences管理器切换模型过滤状态
            boolean currentState = SharedPreferencesManager.getInstance(requireContext()).isModelFilteringEnabled();
            SharedPreferencesManager.getInstance(requireContext()).setModelFilteringEnabled(!currentState);
            
            // 更新UI
            updateModelFilteringUI();
        });
    }

    private void updateModelFilteringUI() {
        // 从SharedPreferences管理器获取模型过滤状态
        boolean isEnabled = SharedPreferencesManager.getInstance(requireContext()).isModelFilteringEnabled();
        
        if (isEnabled) {
            // 获取已过滤数量
            int filteredCount = FilteredNotificationManager.getInstance(requireContext()).getAllNotifications().size();
            tvModelFilteringStatus.setText(getString(R.string.model_filtering_running, filteredCount));
            tvModelFilteringStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            
            btnModelFilteringToggle.setText(getString(R.string.stop_filtering));
            btnModelFilteringToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            tvModelFilteringStatus.setText(getString(R.string.model_filtering_stopped));
            tvModelFilteringStatus.setTextColor(getResources().getColor(R.color.colorError, null));
            
            btnModelFilteringToggle.setText(getString(R.string.start_filtering));
            btnModelFilteringToggle.setBackgroundResource(R.drawable.btn_primary_background);
        }
    }
    
    private void setupFilterModelControls() {
        // 从SharedPreferences管理器获取过滤模型设置
        String model = SharedPreferencesManager.getInstance(requireContext()).getFilterModel();
        
        // 设置选中状态
        if (MODEL_ONLINE.equals(model)) {
            radioBtnModelOnline.setChecked(true);
            cardLearningConfig.setVisibility(View.GONE);
            cardOnlineModelConfig.setVisibility(View.VISIBLE);
        } else {
            radioBtnModelLocal.setChecked(true);
            cardLearningConfig.setVisibility(View.VISIBLE);
            cardOnlineModelConfig.setVisibility(View.GONE);
        }
        
        // 设置监听器
        radioGroupFilterModel.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_model_local) {
                SharedPreferencesManager.getInstance(requireContext()).setFilterModel(MODEL_LOCAL);
                cardLearningConfig.setVisibility(View.VISIBLE);
                cardOnlineModelConfig.setVisibility(View.GONE);
            } else if (checkedId == R.id.radio_btn_model_online) {
                // 显示警告弹窗，暂不保存设置
                showOnlineWarningDialog();
            }
        });
    }

    private void showOnlineWarningDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_warning, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        Button btnConfirm = view.findViewById(R.id.btn_confirm);
        String confirmText = getString(R.string.confirm);
        
        // 初始禁用确定按钮
        btnConfirm.setEnabled(false);
        btnConfirm.setAlpha(0.5f);
        
        // 创建倒计时，总时长10秒，间隔1秒
        android.os.CountDownTimer timer = new android.os.CountDownTimer(10000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
                // 显示倒计时 10-1
                long seconds = (millisUntilFinished / 1000) + 1;
                btnConfirm.setText(confirmText + " (" + seconds + ")");
            }

            @Override
            public void onFinish() {
                // 倒计时结束，恢复文本并启用
                btnConfirm.setText(confirmText);
                btnConfirm.setEnabled(true);
                btnConfirm.setAlpha(1.0f);
            }
        };
        timer.start();
        
        // 确保dialog消失时取消timer
        dialog.setOnDismissListener(d -> timer.cancel());

        view.findViewById(R.id.btn_cancel).setOnClickListener(v -> {
            dialog.dismiss();
            // 恢复到本地模型
            radioBtnModelLocal.setChecked(true);
        });

        btnConfirm.setOnClickListener(v -> {
            dialog.dismiss();
            // 确认切换到在线模型
            SharedPreferencesManager.getInstance(requireContext()).setFilterModel(MODEL_ONLINE);
            
            cardLearningConfig.setVisibility(View.GONE);
            cardOnlineModelConfig.setVisibility(View.VISIBLE);
            
            // 释放本地模型内存
            LocalModelManager.getInstance(requireContext()).releaseMemory();
        });
        
        dialog.setCancelable(false);
        dialog.show();
    }

    private void setupOnlineModelConfigControls() {
        // 从SharedPreferences管理器获取在线模型配置
        float filteringDegree = SharedPreferencesManager.getInstance(requireContext()).getOnlineFilteringDegree();
        float temperature = SharedPreferencesManager.getInstance(requireContext()).getTemperature();
        
        // 设置初始值
        // SeekBar范围是0-100，对应0.0-10.0
        seekBarOnlineFilteringDegree.setProgress((int) (filteringDegree * 10));
        tvOnlineFilteringDegreeValue.setText(String.format("%.1f", filteringDegree));
        
        // 温度滑块范围是0-10，对应0.0-1.0
        seekBarTemperature.setProgress((int) (temperature * 10));
        tvTemperatureValue.setText(String.format("%.1f", temperature));
        
        // 设置过滤程度监听器
        seekBarOnlineFilteringDegree.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 10.0f;
                tvOnlineFilteringDegreeValue.setText(String.format("%.1f", value));
                
                if (fromUser) {
                    // 使用管理器保存设置
                    SharedPreferencesManager.getInstance(requireContext()).setOnlineFilteringDegree(value);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 设置温度监听器
        seekBarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 10.0f;
                tvTemperatureValue.setText(String.format("%.1f", value));
                
                if (fromUser) {
                    // 使用管理器保存设置
                    SharedPreferencesManager.getInstance(requireContext()).setTemperature(value);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // 重置按钮点击事件
        btnResetOnlineFilteringDegreeConfig.setOnClickListener(v -> {
            // 恢复默认值
            float defaultFilteringDegree = 5.0f;
            float defaultTemperature = 0.5f;

            // 使用管理器重置API配置
            SharedPreferencesManager manager = SharedPreferencesManager.getInstance(requireContext());
            manager.setOnlineFilteringDegree(defaultFilteringDegree);
            manager.setTemperature(defaultTemperature);
            manager.setOnlineApiUrl("");
            manager.setOnlineApiKey("");
            manager.setOnlineModelName("");
            manager.setOnlineModelPrompt("");

            // 更新 UI
            seekBarOnlineFilteringDegree.setProgress((int) (defaultFilteringDegree * 10));
            tvOnlineFilteringDegreeValue.setText(String.format("%.1f", defaultFilteringDegree));
            seekBarTemperature.setProgress((int) (defaultTemperature * 10));
            tvTemperatureValue.setText(String.format("%.1f", defaultTemperature));
        });

        // API配置按钮点击事件
        btnApiConfig.setOnClickListener(v -> showApiConfigDialog());
        
        // 设置在线模型流程控制
        setupOnlineModelProcessControls();
    }
    
    private void setupOnlineModelProcessControls() {
        // 从SharedPreferences管理器获取在线模型处理流程设置
        String processMode = SharedPreferencesManager.getInstance(requireContext()).getOnlineModelProcessMode();
        
        // 设置选中状态，默认是"先显示再检查"
        if (SharedPreferencesManager.PROCESS_MODE_CHECK_FIRST.equals(processMode)) {
            radioBtnCheckFirst.setChecked(true);
        } else {
            radioBtnShowFirst.setChecked(true); // 默认值
        }
        
        // 设置监听器
        radioGroupOnlineModelProcess.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_btn_show_first) {
                SharedPreferencesManager.getInstance(requireContext()).setOnlineModelProcessMode(
                    SharedPreferencesManager.PROCESS_MODE_SHOW_FIRST);
            } else if (checkedId == R.id.radio_btn_check_first) {
                SharedPreferencesManager.getInstance(requireContext()).setOnlineModelProcessMode(
                    SharedPreferencesManager.PROCESS_MODE_CHECK_FIRST);
            }
        });
    }
    
    private void showApiConfigDialog() {
        android.app.AlertDialog.Builder builder = new android.app.AlertDialog.Builder(requireContext());
        View view = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_api_config, null);
        builder.setView(view);
        android.app.AlertDialog dialog = builder.create();
        
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        EditText etApiUrl = view.findViewById(R.id.et_api_url);
        EditText etApiKey = view.findViewById(R.id.et_api_key);
        EditText etModelName = view.findViewById(R.id.et_model_name);
        EditText etSystemPrompt = view.findViewById(R.id.et_system_prompt);
        SeekBar seekBarTemperature = view.findViewById(R.id.et_temperature);
        TextView tvTemperatureValue = view.findViewById(R.id.tv_temperature_value);
        Button btnCancel = view.findViewById(R.id.btn_cancel);
        Button btnSave = view.findViewById(R.id.btn_save);
        
        // 新增：进度条相关控件
        LinearLayout layoutProgress = view.findViewById(R.id.layout_progress);
        TextView tvProgressText = view.findViewById(R.id.tv_progress_text);
        LinearLayout layoutButtons = view.findViewById(R.id.layout_buttons);

        // 从SharedPreferences管理器读取当前保存的配置
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(requireContext());
        String currentApiUrl = manager.getOnlineApiUrl();
        String currentApiKey = manager.getOnlineApiKey();
        String currentModelName = manager.getOnlineModelName();
        String currentSystemPrompt = manager.getOnlineModelPrompt();
        if (currentSystemPrompt.isEmpty()) {
            currentSystemPrompt = getString(R.string.default_prompt_content);
        }
        float currentTemperature = manager.getTemperature();

        // 设置当前值到输入框
        etApiUrl.setText(currentApiUrl);
        etApiKey.setText(currentApiKey);
        etModelName.setText(currentModelName);
        etSystemPrompt.setText(currentSystemPrompt);
        
        // 设置温度滑块
        seekBarTemperature.setProgress((int) (currentTemperature * 10));
        tvTemperatureValue.setText(String.format("%.1f", currentTemperature));
        
        // 设置温度滑块监听器
        seekBarTemperature.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float value = progress / 10.0f;
                tvTemperatureValue.setText(String.format("%.1f", value));
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 取消
        btnCancel.setOnClickListener(v -> dialog.dismiss());

        // 保存 - 先测试API，成功后再保存到本地
        btnSave.setOnClickListener(v -> {
            String apiUrl = etApiUrl.getText().toString().trim();
            String apiKey = etApiKey.getText().toString().trim();
            String modelName = etModelName.getText().toString().trim();
            String systemPrompt = etSystemPrompt.getText().toString().trim();
            float temperature = seekBarTemperature.getProgress() / 10.0f;
            
            // 验证输入
            if (apiUrl.isEmpty() || apiKey.isEmpty() || modelName.isEmpty()) {
                Toast.makeText(requireContext(), R.string.api_config_required, Toast.LENGTH_SHORT).show();
                return;
            }

            // 显示进度条，隐藏按钮
            layoutProgress.setVisibility(View.VISIBLE);
            layoutButtons.setVisibility(View.GONE);
            
            // 调用测试API连接方法（不先保存到本地）
            testApiConnection(apiUrl, apiKey, modelName, systemPrompt, temperature, 
                new ApiTestCallback() {
                    @Override
                    public void onSuccess() {
                        // 测试成功，保存配置到本地并关闭弹窗
                        requireActivity().runOnUiThread(() -> {
                            // 成功后再保存到SharedPreferences
                            manager.setOnlineApiUrl(apiUrl);
                            manager.setOnlineApiKey(apiKey);
                            manager.setOnlineModelName(modelName);
                            manager.setOnlineModelPrompt(systemPrompt);
                            manager.setTemperature(temperature);
                            
                            Toast.makeText(requireContext(), R.string.api_test_success, Toast.LENGTH_SHORT).show();
                            
                            // 更新主页面的温度显示
                            if (tvTemperatureValue != null) {
                                tvTemperatureValue.setText(String.format("%.1f", temperature));
                            }
                            if (seekBarTemperature != null) {
                                seekBarTemperature.setProgress((int) (temperature * 10));
                            }
                            
                            dialog.dismiss();
                        });
                    }
                    
                    @Override
                    public void onFailure() {
                        // 测试失败，隐藏进度条，显示按钮，显示错误
                        requireActivity().runOnUiThread(() -> {
                            layoutProgress.setVisibility(View.GONE);
                            layoutButtons.setVisibility(View.VISIBLE);
                            
                            Toast.makeText(requireContext(), R.string.api_test_failed, Toast.LENGTH_SHORT).show();
                        });
                    }
                });
        });

        dialog.show();
    }

    // API测试回调接口
    private interface ApiTestCallback {
        void onSuccess();
        void onFailure();
    }

    // 测试API连接方法
    private void testApiConnection(String apiUrl, String apiKey, String modelName, 
                                   String systemPrompt, float temperature, ApiTestCallback callback) {
        // 使用OnlineModelManager的testApiConnection方法进行测试
        new Thread(() -> {
            try {
                OnlineModelManager manager = OnlineModelManager.getInstance(requireContext());
                boolean success = manager.testApiConnection(apiUrl, apiKey, modelName, systemPrompt, temperature);
                
                requireActivity().runOnUiThread(() -> {
                    if (success) {
                        callback.onSuccess();
                    } else {
                        callback.onFailure();
                    }
                });
            } catch (Exception e) {
                requireActivity().runOnUiThread(() -> {
                    callback.onFailure();
                });
            }
        }).start();
    }

    private void setupLearningConfigControls() {
        // 从SharedPreferences管理器获取学习配置
        float filteringDegree = SharedPreferencesManager.getInstance(requireContext()).getFilteringDegree();
        float learningDegree = SharedPreferencesManager.getInstance(requireContext()).getLearningDegree();
        
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
                    // 使用管理器保存设置
                    SharedPreferencesManager.getInstance(requireContext()).setFilteringDegree(value);
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
                    // 使用管理器保存设置
                    SharedPreferencesManager.getInstance(requireContext()).setLearningDegree(value);
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

            // 使用管理器重置设置
            SharedPreferencesManager manager = SharedPreferencesManager.getInstance(requireContext());
            manager.setFilteringDegree(defaultFilteringDegree);
            manager.setLearningDegree(defaultLearningDegree);

            // 更新 UI
            seekBarFilteringDegree.setProgress((int) (defaultFilteringDegree * 10));
            tvFilteringDegreeValue.setText(String.format("%.1f", defaultFilteringDegree));

            seekBarLearningDegree.setProgress((int) (defaultLearningDegree * 10));
            tvLearningDegreeValue.setText(String.format("%.1f", defaultLearningDegree));
        });

        // 清空学习模型按钮点击事件
        btnClearLearningModel.setOnClickListener(v -> {
            LocalModelManager.getInstance(requireContext()).clearModel();
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
            tvPermissionStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnPermissionAction.setText(getString(R.string.configure_permission));
            btnPermissionAction.setBackgroundResource(R.drawable.btn_secondary_background);
            btnPermissionAction.setTextColor(getResources().getColor(R.color.colorOnSurfaceSecondary, null));
            btnPermissionAction.setVisibility(View.VISIBLE);
        } else {
            // 有权限未开启 - 设置黄色
            tvPermissionStatus.setText(String.format(getString(R.string.permission_some_denied), status.deniedCount));
            tvPermissionStatus.setTextColor(getResources().getColor(R.color.colorWarning, null));
            btnPermissionAction.setText(getString(R.string.go_to_permission));
            btnPermissionAction.setBackgroundResource(R.drawable.btn_warning_background);
            btnPermissionAction.setTextColor(getResources().getColor(R.color.white, null));
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

    private void setupAnimationSpeedControls() {
        // 从SharedPreferences管理器获取动画速度设置
        float speed = SharedPreferencesManager.getInstance(requireContext()).getAnimationSpeed();
        int progress = Math.max(0, Math.min(29, (int) (speed * 10) - 1));
        
        seekBarSpeed.setProgress(progress);
        tvSpeedValue.setText(getString(R.string.value_speed, (progress + 1) / 10.0f));
        
        // 统一监听逻辑（拖动+按钮点击都会触发 onProgressChanged）
        seekBarSpeed.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                float newSpeed = (progress + 1) / 10.0f;
                tvSpeedValue.setText(getString(R.string.value_speed, newSpeed));
                saveAnimationSpeed(newSpeed);
            }
            
            @Override public void onStartTrackingTouch(SeekBar seekBar) {}
            @Override public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 按钮直接操作进度，复用监听器逻辑
        btnSpeedDecrease.setOnClickListener(v -> seekBarSpeed.setProgress(seekBarSpeed.getProgress() - 1));
        btnSpeedIncrease.setOnClickListener(v -> seekBarSpeed.setProgress(seekBarSpeed.getProgress() + 1));
        btnResetSpeed.setOnClickListener(v -> seekBarSpeed.setProgress(9)); // 1.0x 对应 progress 9
    }

    private void saveAnimationSpeed(float speed) {
        // 使用管理器保存动画速度设置
        SharedPreferencesManager.getInstance(requireContext()).setAnimationSpeed(speed);
        
        // 通知服务更新动画配置
        if (FloatingWindowService.isServiceRunning(requireContext())) {
            FloatingWindowService service = FloatingWindowService.getInstance();
            if (service != null) {
                service.updateAnimationConfiguration();
            }
        }
    }

    private void saveScrollPosition() {
        if (scrollView != null) {
            // 使用自定义SharedPreferences保存滚动位置（不通过管理器，因为这是临时状态）
            SharedPreferences preferences = requireContext().getSharedPreferences("app_settings_scroll", Context.MODE_PRIVATE);
            preferences.edit().putInt("scroll_y", scrollView.getScrollY()).apply();
        }
    }

    private void restoreScrollPosition() {
        // 使用自定义SharedPreferences恢复滚动位置
        SharedPreferences preferences = requireContext().getSharedPreferences("app_settings_scroll", Context.MODE_PRIVATE);
        if (preferences.contains("scroll_y")) {
            final int scrollY = preferences.getInt("scroll_y", 0);
            if (scrollView != null) {
                scrollView.post(() -> scrollView.scrollTo(0, scrollY));
            }
            preferences.edit().remove("scroll_y").apply();
        }
    }
    
    /**
     * 设置声音与震动相关控件
     */
    private void setupSoundVibrationControls() {
        // 从SharedPreferences管理器获取声音与震动设置
        boolean soundEnabled = SharedPreferencesManager.getInstance(requireContext()).isSoundEnabled();
        boolean vibrationEnabled = SharedPreferencesManager.getInstance(requireContext()).isVibrationEnabled();
        int vibrationIntensity = SharedPreferencesManager.getInstance(requireContext()).getVibrationIntensity();
        
        // 更新声音状态显示
        updateSoundUI(soundEnabled);
        
        // 更新震动状态显示
        updateVibrationUI(vibrationEnabled);
        
        // 设置震动强度初始值
        seekBarVibrationIntensity.setProgress(vibrationIntensity);
        tvVibrationIntensityValue.setText(getString(R.string.vibration_intensity_value, vibrationIntensity));
        
        // 声音开关点击事件
        btnSoundToggle.setOnClickListener(v -> {
            boolean currentState = SharedPreferencesManager.getInstance(requireContext()).isSoundEnabled();
            SharedPreferencesManager.getInstance(requireContext()).setSoundEnabled(!currentState);
            updateSoundUI(!currentState);
        });
        
        // 震动开关点击事件
        btnVibrationToggle.setOnClickListener(v -> {
            boolean currentState = SharedPreferencesManager.getInstance(requireContext()).isVibrationEnabled();
            SharedPreferencesManager.getInstance(requireContext()).setVibrationEnabled(!currentState);
            updateVibrationUI(!currentState);
        });
        
        // 震动强度滑块监听器
        seekBarVibrationIntensity.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                tvVibrationIntensityValue.setText(getString(R.string.vibration_intensity_value, progress));
                if (fromUser) {
                    SharedPreferencesManager.getInstance(requireContext()).setVibrationIntensity(progress);
                }
            }
            
            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}
            
            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });
        
        // 震动强度减小按钮
        btnVibrationIntensityDecrease.setOnClickListener(v -> {
            int currentProgress = seekBarVibrationIntensity.getProgress();
            if (currentProgress > 30) { // 设置最小值为30毫秒
                seekBarVibrationIntensity.setProgress(currentProgress - 1);
                tvVibrationIntensityValue.setText(getString(R.string.vibration_intensity_value, currentProgress - 1));
                SharedPreferencesManager.getInstance(requireContext()).setVibrationIntensity(currentProgress - 1);
            }
        });
        
        // 震动强度增加按钮
        btnVibrationIntensityIncrease.setOnClickListener(v -> {
            int currentProgress = seekBarVibrationIntensity.getProgress();
            if (currentProgress < 1000) { // 设置最大值为1000毫秒
                seekBarVibrationIntensity.setProgress(currentProgress + 1);
                tvVibrationIntensityValue.setText(getString(R.string.vibration_intensity_value, currentProgress + 1));
                SharedPreferencesManager.getInstance(requireContext()).setVibrationIntensity(currentProgress + 1);
            }
        });
    }
    
    /**
     * 更新声音UI状态
     */
    private void updateSoundUI(boolean enabled) {
        if (enabled) {
            tvSoundStatus.setText(getString(R.string.sound_enabled));
            tvSoundStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnSoundToggle.setText(getString(R.string.disable_sound));
            btnSoundToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            tvSoundStatus.setText(getString(R.string.sound_disabled));
            tvSoundStatus.setTextColor(getResources().getColor(R.color.colorError, null));
            btnSoundToggle.setText(getString(R.string.enable_sound));
            btnSoundToggle.setBackgroundResource(R.drawable.btn_primary_background);
        }
    }
    
    /**
     * 更新震动UI状态
     */
    private void updateVibrationUI(boolean enabled) {
        if (enabled) {
            tvVibrationStatus.setText(getString(R.string.vibration_enabled));
            tvVibrationStatus.setTextColor(getResources().getColor(R.color.colorSuccess, null));
            btnVibrationToggle.setText(getString(R.string.disable_vibration));
            btnVibrationToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            tvVibrationStatus.setText(getString(R.string.vibration_disabled));
            tvVibrationStatus.setTextColor(getResources().getColor(R.color.colorError, null));
            btnVibrationToggle.setText(getString(R.string.enable_vibration));
            btnVibrationToggle.setBackgroundResource(R.drawable.btn_primary_background);
        }
    }
    
}
