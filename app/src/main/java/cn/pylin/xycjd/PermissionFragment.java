package cn.pylin.xycjd;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class PermissionFragment extends Fragment {

    private Context mContext;
    private Activity mActivity;
    private NestedScrollView scrollView;
    private Handler permissionCheckHandler;
    private Runnable permissionCheckRunnable;
    private static final int PERMISSION_CHECK_INTERVAL = 1000; // 1秒检查一次

    // UI组件
    private androidx.cardview.widget.CardView permissionItemNotification;
    private TextView notificationStatus;
    private Button notificationBtn;
    private androidx.cardview.widget.CardView permissionItemOverlay;
    private TextView overlayStatus;
    private Button overlayBtn;

    private androidx.cardview.widget.CardView permissionItemBatteryOptimization;
    private TextView batteryOptimizationStatus;
    private Button batteryOptimizationBtn;

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
        mActivity = getActivity();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_permission, container, false);
        initViews(view);
        setListeners();
        
        // 初始化定时器
        initPermissionCheckTimer();
        
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        // 销毁定时器
        stopPermissionCheckTimer();
    }
    
    /**
     * 初始化权限检查定时器
     */
    private void initPermissionCheckTimer() {
        permissionCheckHandler = new Handler(Looper.getMainLooper());
        permissionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && mContext != null) {
                    updatePermissionStatus();
                    // 1秒后再次执行
                    permissionCheckHandler.postDelayed(this, PERMISSION_CHECK_INTERVAL);
                }
            }
        };
        
        // 立即执行一次，然后每秒执行一次
        permissionCheckHandler.post(permissionCheckRunnable);
    }
    
    /**
     * 停止权限检查定时器
     */
    private void stopPermissionCheckTimer() {
        if (permissionCheckHandler != null && permissionCheckRunnable != null) {
            permissionCheckHandler.removeCallbacks(permissionCheckRunnable);
            permissionCheckHandler = null;
            permissionCheckRunnable = null;
        }
    }

    /**
     * 初始化UI组件
     */
    private void initViews(View view) {
        scrollView = view.findViewById(R.id.nested_scroll_view);
        permissionItemNotification = view.findViewById(R.id.permission_item_notification);
        notificationStatus = view.findViewById(R.id.notification_status);
        notificationBtn = view.findViewById(R.id.notification_btn);
        permissionItemOverlay = view.findViewById(R.id.permission_item_overlay);
        overlayStatus = view.findViewById(R.id.overlay_status);
        overlayBtn = view.findViewById(R.id.overlay_btn);

        permissionItemBatteryOptimization = view.findViewById(R.id.permission_item_battery_optimization);
        batteryOptimizationStatus = view.findViewById(R.id.battery_optimization_status);
        batteryOptimizationBtn = view.findViewById(R.id.battery_optimization_btn);
    }

    /**
     * 设置监听器
     */
    private void setListeners() {
        setPermissionClickListener(permissionItemNotification, notificationBtn, this::openNotificationListenerSettings);
        setPermissionClickListener(permissionItemOverlay, overlayBtn, this::openOverlaySettings);
        setPermissionClickListener(permissionItemBatteryOptimization, batteryOptimizationBtn, this::openBatteryOptimizationSettings);
    }
    
    /**
     * 设置权限项的点击监听器
     */
    private void setPermissionClickListener(androidx.cardview.widget.CardView cardView, Button button, Runnable action) {
        View.OnClickListener listener = v -> action.run();
        cardView.setOnClickListener(listener);
        button.setOnClickListener(listener);
    }

    /**
     * 更新权限状态
     */
    void updatePermissionStatus() {
        if (!isAdded() || mContext == null || 
            notificationStatus == null || overlayStatus == null || 
            batteryOptimizationStatus == null) {
            return;
        }
        
        PermissionChecker.PermissionStatus status = PermissionChecker.checkAllPermissions(mContext);
        
        updatePermissionUI(notificationStatus, notificationBtn, status.hasNotificationPermission);
        updatePermissionUI(overlayStatus, overlayBtn, status.hasOverlayPermission);
        updatePermissionUI(batteryOptimizationStatus, batteryOptimizationBtn, status.hasBatteryOptimizationDisabled);
    }
    
    /**
     * 更新单个权限的UI状态
     */
    private void updatePermissionUI(TextView statusView, Button buttonView, boolean isGranted) {
        if (isGranted) {
            statusView.setText(R.string.permission_status_granted);
            statusView.setTextColor(getResources().getColor(R.color.colorPrimary));
            buttonView.setVisibility(View.GONE);
        } else {
            statusView.setText(R.string.permission_status_denied);
            statusView.setTextColor(getResources().getColor(R.color.red));
            buttonView.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 打开通知监听权限设置页面
     */
    private void openNotificationListenerSettings() {
        PermissionChecker.openNotificationListenerSettings(mActivity);
    }

    /**
     * 打开悬浮窗权限设置页面
     */
    private void openOverlaySettings() {
        PermissionChecker.openOverlaySettings(mActivity);
    }



    /**
     * 打开电池优化设置页面
     */
    private void openBatteryOptimizationSettings() {
        PermissionChecker.openBatteryOptimizationSettings(mActivity);
    }
}
