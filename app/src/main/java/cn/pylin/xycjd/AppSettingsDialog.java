package cn.pylin.xycjd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

public class AppSettingsDialog {
    
    private Context mContext;
    private AlertDialog mDialog;
    
    private TextView appNameTextView;
    private TextView packageNameTextView;
    private TextView enableStatusTextView;
    private Switch modelFilterSwitch;
    private Switch autoExpandSwitch;
    private Switch notificationVibrationSwitch;
    private Switch notificationSoundSwitch;
    private Button saveButton;
    private Button cancelButton;
    
    private String packageName;
    private String appName;
    private boolean isEnabled;
    
    private SharedPreferences modelFilterPrefs;
    private SharedPreferences autoExpandPrefs;
    private SharedPreferences notificationVibrationPrefs;
    private SharedPreferences notificationSoundPrefs;
    
    public AppSettingsDialog(@NonNull Context context, String packageName, String appName, boolean isEnabled) {
        mContext = context;
        this.packageName = packageName;
        this.appName = appName;
        this.isEnabled = isEnabled;
        
        // 初始化SharedPreferences
        modelFilterPrefs = mContext.getSharedPreferences("app_model_filter", Context.MODE_PRIVATE);
        autoExpandPrefs = mContext.getSharedPreferences("app_auto_expand", Context.MODE_PRIVATE);
        notificationVibrationPrefs = mContext.getSharedPreferences("app_notification_vibration", Context.MODE_PRIVATE);
        notificationSoundPrefs = mContext.getSharedPreferences("app_notification_sound", Context.MODE_PRIVATE);
        
        initDialog();
    }
    
    private void initDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(mContext);
        View view = LayoutInflater.from(mContext).inflate(R.layout.dialog_app_settings, null);
        builder.setView(view);
        mDialog = builder.create();
        
        if (mDialog.getWindow() != null) {
            mDialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }
        
        // 获取视图组件
        appNameTextView = view.findViewById(R.id.dialog_app_name);
        packageNameTextView = view.findViewById(R.id.dialog_package_name);
        enableStatusTextView = view.findViewById(R.id.dialog_enable_status);
        modelFilterSwitch = view.findViewById(R.id.switch_model_filter);
        autoExpandSwitch = view.findViewById(R.id.switch_auto_expand);
        notificationVibrationSwitch = view.findViewById(R.id.switch_notification_vibration);
        notificationSoundSwitch = view.findViewById(R.id.switch_notification_sound);
        saveButton = view.findViewById(R.id.btn_save);
        cancelButton = view.findViewById(R.id.btn_cancel);
        
        // 设置应用信息
        appNameTextView.setText(appName);
        packageNameTextView.setText(packageName);
        
        // 设置启用状态显示
        if (isEnabled) {
            enableStatusTextView.setText("状态：已启用");
            enableStatusTextView.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            enableStatusTextView.setText("状态：未启用");
            enableStatusTextView.setTextColor(mContext.getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // 加载当前设置状态
        boolean isModelFilterChecked = modelFilterPrefs.getBoolean(packageName, false);
        boolean isAutoExpandChecked = autoExpandPrefs.getBoolean(packageName, false);
        boolean isNotificationVibrationChecked = notificationVibrationPrefs.getBoolean(packageName, false);
        boolean isNotificationSoundChecked = notificationSoundPrefs.getBoolean(packageName, false);
        
        modelFilterSwitch.setChecked(isModelFilterChecked);
        autoExpandSwitch.setChecked(isAutoExpandChecked);
        notificationVibrationSwitch.setChecked(isNotificationVibrationChecked);
        notificationSoundSwitch.setChecked(isNotificationSoundChecked);
        
        // 设置按钮点击事件
        saveButton.setOnClickListener(v -> saveSettings());
        cancelButton.setOnClickListener(v -> mDialog.dismiss());
        
        mDialog.setCancelable(true);
        mDialog.setCanceledOnTouchOutside(true);
    }
    
    private void saveSettings() {
        // 保存模型过滤设置
        boolean isModelFilterChecked = modelFilterSwitch.isChecked();
        modelFilterPrefs.edit().putBoolean(packageName, isModelFilterChecked).apply();
        
        // 保存自动展开设置
        boolean isAutoExpandChecked = autoExpandSwitch.isChecked();
        autoExpandPrefs.edit().putBoolean(packageName, isAutoExpandChecked).apply();
        
        // 保存通知震动设置
        boolean isNotificationVibrationChecked = notificationVibrationSwitch.isChecked();
        notificationVibrationPrefs.edit().putBoolean(packageName, isNotificationVibrationChecked).apply();
        
        // 保存通知声音设置
        boolean isNotificationSoundChecked = notificationSoundSwitch.isChecked();
        notificationSoundPrefs.edit().putBoolean(packageName, isNotificationSoundChecked).apply();
        
        Toast.makeText(mContext, R.string.save, Toast.LENGTH_SHORT).show();
        mDialog.dismiss();
    }
    
    public void show() {
        if (mDialog != null && !mDialog.isShowing()) {
            mDialog.show();
        }
    }
    
    public void dismiss() {
        if (mDialog != null && mDialog.isShowing()) {
            mDialog.dismiss();
        }
    }
}
