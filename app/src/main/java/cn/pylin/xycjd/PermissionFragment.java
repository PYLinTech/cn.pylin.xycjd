package cn.pylin.xycjd;

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.Manifest;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class PermissionFragment extends Fragment {

    private Context mContext;
    private Activity mActivity;
    private NestedScrollView scrollView;
    private Handler permissionCheckHandler;
    private Runnable permissionCheckRunnable;
    private static final int PERMISSION_CHECK_INTERVAL = 1000;
    private static final int REQUEST_POST_NOTIFICATIONS = 1001;

    private androidx.cardview.widget.CardView permissionItemNotification;
    private TextView notificationStatus;
    private Button notificationBtn;
    private androidx.cardview.widget.CardView permissionItemNotificationPost;
    private TextView notificationPostStatus;
    private Button notificationPostBtn;
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
        initPermissionCheckTimer();
        return view;
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        stopPermissionCheckTimer();
    }
    
    private void initPermissionCheckTimer() {
        permissionCheckHandler = new Handler(Looper.getMainLooper());
        permissionCheckRunnable = new Runnable() {
            @Override
            public void run() {
                if (isAdded() && mContext != null) {
                    updatePermissionStatus();
                    permissionCheckHandler.postDelayed(this, PERMISSION_CHECK_INTERVAL);
                }
            }
        };
        permissionCheckHandler.post(permissionCheckRunnable);
    }
    
    private void stopPermissionCheckTimer() {
        if (permissionCheckHandler != null && permissionCheckRunnable != null) {
            permissionCheckHandler.removeCallbacks(permissionCheckRunnable);
            permissionCheckHandler = null;
            permissionCheckRunnable = null;
        }
    }

    private void initViews(View view) {
        scrollView = view.findViewById(R.id.nested_scroll_view);
        permissionItemNotification = view.findViewById(R.id.permission_item_notification);
        notificationStatus = view.findViewById(R.id.notification_status);
        notificationBtn = view.findViewById(R.id.notification_btn);
        permissionItemNotificationPost = view.findViewById(R.id.permission_item_notification_post);
        notificationPostStatus = view.findViewById(R.id.notification_post_status);
        notificationPostBtn = view.findViewById(R.id.notification_post_btn);
        permissionItemOverlay = view.findViewById(R.id.permission_item_overlay);
        overlayStatus = view.findViewById(R.id.overlay_status);
        overlayBtn = view.findViewById(R.id.overlay_btn);

        permissionItemBatteryOptimization = view.findViewById(R.id.permission_item_battery_optimization);
        batteryOptimizationStatus = view.findViewById(R.id.battery_optimization_status);
        batteryOptimizationBtn = view.findViewById(R.id.battery_optimization_btn);
    }

    private void setListeners() {
        setPermissionClickListener(permissionItemNotification, notificationBtn, this::openNotificationListenerSettings);
        setPermissionClickListener(permissionItemNotificationPost, notificationPostBtn, this::requestNotificationPermission);
        setPermissionClickListener(permissionItemOverlay, overlayBtn, this::openOverlaySettings);
        setPermissionClickListener(permissionItemBatteryOptimization, batteryOptimizationBtn, this::openBatteryOptimizationSettings);
    }
    
    private void setPermissionClickListener(androidx.cardview.widget.CardView cardView, Button button, Runnable action) {
        View.OnClickListener listener = v -> action.run();
        cardView.setOnClickListener(listener);
        button.setOnClickListener(listener);
    }

    void updatePermissionStatus() {
        if (!isAdded() || mContext == null || 
            notificationStatus == null || notificationPostStatus == null ||
            overlayStatus == null || 
            batteryOptimizationStatus == null) {
            return;
        }
        
        PermissionChecker.PermissionStatus status = PermissionChecker.checkAllPermissions(mContext);
        
        updatePermissionUI(notificationStatus, notificationBtn, status.hasNotificationPermission);
        updatePermissionUI(notificationPostStatus, notificationPostBtn, status.hasNotificationPostPermission);
        updatePermissionUI(overlayStatus, overlayBtn, status.hasOverlayPermission);
        updatePermissionUI(batteryOptimizationStatus, batteryOptimizationBtn, status.hasBatteryOptimizationDisabled);
    }
    
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

    private void requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && mActivity != null) {
            if (ContextCompat.checkSelfPermission(mActivity, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, REQUEST_POST_NOTIFICATIONS);
            }
        }
    }

    private void openNotificationListenerSettings() {
        PermissionChecker.openNotificationListenerSettings(mActivity);
    }

    private void openOverlaySettings() {
        PermissionChecker.openOverlaySettings(mActivity);
    }

    private void openBatteryOptimizationSettings() {
        PermissionChecker.openBatteryOptimizationSettings(mActivity);
    }
}
