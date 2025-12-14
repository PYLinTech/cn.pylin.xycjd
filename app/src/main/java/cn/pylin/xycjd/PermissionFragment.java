package cn.pylin.xycjd;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.provider.Settings;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class PermissionFragment extends Fragment {

    private Context mContext;
    private Activity mActivity;
    private NestedScrollView scrollView;

    // UI组件
    private androidx.cardview.widget.CardView permissionItemNotification;
    private TextView notificationStatus;
    private Button notificationBtn;
    private androidx.cardview.widget.CardView permissionItemOverlay;
    private TextView overlayStatus;
    private Button overlayBtn;
    private androidx.cardview.widget.CardView permissionItemQueryAllPackages;
    private TextView queryAllPackagesStatus;
    private Button queryAllPackagesBtn;
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
        updatePermissionStatus();
        setListeners();
        setupSmoothScrolling();
        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        // 每次回到该页面时更新权限状态
        updatePermissionStatus();
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
        permissionItemQueryAllPackages = view.findViewById(R.id.permission_item_query_all_packages);
        queryAllPackagesStatus = view.findViewById(R.id.query_all_packages_status);
        queryAllPackagesBtn = view.findViewById(R.id.query_all_packages_btn);
        permissionItemBatteryOptimization = view.findViewById(R.id.permission_item_battery_optimization);
        batteryOptimizationStatus = view.findViewById(R.id.battery_optimization_status);
        batteryOptimizationBtn = view.findViewById(R.id.battery_optimization_btn);
    }

    /**
     * 设置平滑滚动
     */
    private void setupSmoothScrolling() {
        if (scrollView != null) {
            // 启用平滑滚动
            scrollView.setNestedScrollingEnabled(true);
            
            // 设置滚动速度
            scrollView.setVerticalScrollbarPosition(View.SCROLLBAR_POSITION_RIGHT);
        }
    }

    /**
     * 设置监听器
     */
    private void setListeners() {
        // 通知监听权限项点击事件
        permissionItemNotification.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationListenerSettings();
            }
        });

        // 通知监听权限按钮点击事件
        notificationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openNotificationListenerSettings();
            }
        });

        // 悬浮窗权限项点击事件
        permissionItemOverlay.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        });

        // 悬浮窗权限按钮点击事件
        overlayBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openOverlaySettings();
            }
        });

        // 查询所有应用包权限项点击事件
        permissionItemQueryAllPackages.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppSettings();
            }
        });

        // 查询所有应用包权限按钮点击事件
        queryAllPackagesBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openAppSettings();
            }
        });

        // 电池优化权限项点击事件
        permissionItemBatteryOptimization.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatteryOptimizationSettings();
            }
        });

        // 电池优化权限按钮点击事件
        batteryOptimizationBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openBatteryOptimizationSettings();
            }
        });
    }

    /**
     * 更新权限状态
     */
    private void updatePermissionStatus() {
        // 更新通知监听权限状态
        if (isNotificationListenerEnabled()) {
            notificationStatus.setText(R.string.permission_status_granted);
            notificationStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
            notificationBtn.setVisibility(View.GONE);
        } else {
            notificationStatus.setText(R.string.permission_status_denied);
            notificationStatus.setTextColor(getResources().getColor(R.color.red));
            notificationBtn.setVisibility(View.VISIBLE);
        }

        // 更新悬浮窗权限状态
        if (Settings.canDrawOverlays(mContext)) {
            overlayStatus.setText(R.string.permission_status_granted);
            overlayStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
            overlayBtn.setVisibility(View.GONE);
        } else {
            overlayStatus.setText(R.string.permission_status_denied);
            overlayStatus.setTextColor(getResources().getColor(R.color.red));
            overlayBtn.setVisibility(View.VISIBLE);
        }

        // 更新查询所有应用包权限状态
        if (AppUtils.hasQueryAllPackagesPermission(mContext)) {
            queryAllPackagesStatus.setText(R.string.permission_status_granted);
            queryAllPackagesStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
            queryAllPackagesBtn.setVisibility(View.GONE);
        } else {
            queryAllPackagesStatus.setText(R.string.permission_status_denied);
            queryAllPackagesStatus.setTextColor(getResources().getColor(R.color.red));
            queryAllPackagesBtn.setVisibility(View.VISIBLE);
        }

        // 更新电池优化状态
        if (isIgnoringBatteryOptimizations()) {
            batteryOptimizationStatus.setText(R.string.permission_status_granted);
            batteryOptimizationStatus.setTextColor(getResources().getColor(R.color.colorPrimary));
            batteryOptimizationBtn.setVisibility(View.GONE);
        } else {
            batteryOptimizationStatus.setText(R.string.permission_status_denied);
            batteryOptimizationStatus.setTextColor(getResources().getColor(R.color.red));
            batteryOptimizationBtn.setVisibility(View.VISIBLE);
        }
    }

    /**
     * 检查通知监听权限是否已授权
     */
    private boolean isNotificationListenerEnabled() {
        return NotificationListenerManager.isNotificationListenerEnabled(mContext);
    }

    /**
     * 打开通知监听权限设置页面
     */
    private void openNotificationListenerSettings() {
        NotificationListenerManager.openNotificationListenerSettings(mActivity);
    }

    /**
     * 打开悬浮窗权限设置页面
     */
    private void openOverlaySettings() {
        Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION);
        intent.setData(android.net.Uri.parse("package:" + mContext.getPackageName()));
        mActivity.startActivity(intent);
    }

    /**
     * 打开应用详情页面（用于查询所有应用包权限）
     */
    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        intent.setData(android.net.Uri.parse("package:" + mContext.getPackageName()));
        mActivity.startActivity(intent);
    }



    /**
     * 检查是否忽略电池优化（Android 11+）
     */
    private boolean isIgnoringBatteryOptimizations() {
        PowerManager powerManager = (PowerManager) mContext.getSystemService(Context.POWER_SERVICE);
        if (powerManager != null) {
            return powerManager.isIgnoringBatteryOptimizations(mContext.getPackageName());
        }
        return false;
    }

    /**
     * 打开电池优化设置页面（Android 11+）
     */
    private void openBatteryOptimizationSettings() {
        try {
            Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
            mActivity.startActivity(intent);
        } catch (Exception e) {
            // 如果失败，回退到应用详情页面
            Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
            intent.setData(android.net.Uri.parse("package:" + mContext.getPackageName()));
            mActivity.startActivity(intent);
        }
    }
}
