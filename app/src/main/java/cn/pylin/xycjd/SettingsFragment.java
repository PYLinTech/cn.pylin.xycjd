package cn.pylin.xycjd;

import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
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
    
    // 测试通知相关控件
    private CardView cardTestNotification;
    private Button btnSendTestNotification;
    
    private boolean isFloatingWindowEnabled = false;
    private static final int REQUEST_OVERLAY_PERMISSION = 1001;
    private static final String TEST_NOTIFICATION_CHANNEL_ID = "test_notification_channel";
    private static final int TEST_NOTIFICATION_ID = 1001;

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
        
        // 设置权限状态相关
        setupPermissionControls();
        
        // 设置悬浮窗相关
        setupFloatingWindowControls();
        
        // 设置服务状态相关
        setupServiceStatusControls();
        
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
        // 保存语言设置到SharedPreferences
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(requireContext());
        SharedPreferences.Editor editor = preferences.edit();
        editor.putString("language", languageCode);
        editor.apply();
        
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
                // 当前服务已开启，执行关闭操作
                stopFloatingWindowService();
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
            // 服务已开启
            tvServiceStatus.setText(getString(R.string.service_running));
            btnServiceToggle.setText(getString(R.string.stop_service));
            btnServiceToggle.setBackgroundResource(R.drawable.btn_error_background);
        } else {
            // 服务未开启
            tvServiceStatus.setText(getString(R.string.service_stopped));
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
    }
    
    private void setupTestNotificationControls() {
        // 创建通知通道（Android 8.0+）
        createNotificationChannel();
        
        // 设置发送测试通知按钮点击事件
        btnSendTestNotification.setOnClickListener(v -> {
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
        NotificationCompat.Builder builder = new NotificationCompat.Builder(requireContext(), TEST_NOTIFICATION_CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(getString(R.string.test_notification_title))
            .setContentText(getString(R.string.test_notification_content))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true);
            
        NotificationManager notificationManager = (NotificationManager) requireContext().getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(TEST_NOTIFICATION_ID, builder.build());
        
        Toast.makeText(requireContext(), getString(R.string.test_notification_sent), Toast.LENGTH_SHORT).show();
    }
}
