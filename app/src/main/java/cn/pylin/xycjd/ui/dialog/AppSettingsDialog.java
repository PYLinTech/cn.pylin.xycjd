package cn.pylin.xycjd.ui.dialog;

import android.app.AlertDialog;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import cn.pylin.xycjd.R;
import cn.pylin.xycjd.manager.SharedPreferencesManager;

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
    
    private SharedPreferencesManager manager;
    
    public AppSettingsDialog(@NonNull Context context, String packageName, String appName, boolean isEnabled) {
        mContext = context;
        this.packageName = packageName;
        this.appName = appName;
        this.isEnabled = isEnabled;
        
        // 初始化SharedPreferences管理器
        manager = SharedPreferencesManager.getInstance(context);
        
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
            enableStatusTextView.setText(R.string.appsettings_app_enabled);
            enableStatusTextView.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        } else {
            enableStatusTextView.setText(R.string.appsettings_app_disabled);
            enableStatusTextView.setTextColor(mContext.getResources().getColor(android.R.color.holo_red_dark));
        }
        
        // 从SharedPreferences管理器加载当前设置状态
        boolean isModelFilterChecked = manager.isAppModelFilterEnabled(packageName);
        boolean isAutoExpandChecked = manager.isAppAutoExpandEnabled(packageName);
        boolean isNotificationVibrationChecked = manager.isAppNotificationVibrationEnabled(packageName);
        boolean isNotificationSoundChecked = manager.isAppNotificationSoundEnabled(packageName);
        
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
        // 使用SharedPreferences管理器保存所有设置
        manager.batchUpdateAppSettings(
            packageName,
            null,  // 启用状态不通过此对话框修改
            modelFilterSwitch.isChecked(),
            autoExpandSwitch.isChecked(),
            notificationVibrationSwitch.isChecked(),
            notificationSoundSwitch.isChecked()
        );
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
