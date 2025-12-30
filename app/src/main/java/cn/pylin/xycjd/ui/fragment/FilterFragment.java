package cn.pylin.xycjd.ui.fragment;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

import cn.pylin.xycjd.manager.AppInfoManager;
import cn.pylin.xycjd.ui.adapter.AppInfoAdapter;
import cn.pylin.xycjd.manager.FilteredNotificationManager;
import cn.pylin.xycjd.model.local.LocalModelManager;
import cn.pylin.xycjd.R;
import cn.pylin.xycjd.manager.SharedPreferencesManager;
import cn.pylin.xycjd.ui.adapter.FilteredNotificationAdapter;
import cn.pylin.xycjd.utils.AppUtils;

public class FilterFragment extends Fragment {
    
    private Context mContext;
    private ListView listView;
    private ListView noticeListView;
    private ImageButton menuButtonApps, menuButtonNotice;
    private View headerApps, headerNotice;
    private View contentApps, contentNotice;
    private View cardApps, cardNotice;
    private ImageButton arrowApps, arrowNotice;
    
    private List<AppInfoManager> allAppsList;
    private List<AppInfoManager> filteredAppsList;
    private AppInfoAdapter adapter;
    private FilteredNotificationAdapter noticeAdapter;
    private List<FilteredNotificationManager.FilteredNotification> filteredNotificationList;
    private int currentFilterType = 1; // 默认为用户应用
    
    private boolean isAppsExpanded = false;
    private boolean isNoticeExpanded = false;
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_filter, container, false);
        
        initViews(view);
        updateUIState();
        loadApps();
        loadNotices();
        
        return view;
    }
    
    private void initViews(View view) {
        listView = view.findViewById(R.id.apps_list);
        noticeListView = view.findViewById(R.id.notice_list);
        menuButtonApps = view.findViewById(R.id.menu_button_apps);
        menuButtonNotice = view.findViewById(R.id.menu_button_notice);
        
        cardApps = view.findViewById(R.id.card_apps);
        cardNotice = view.findViewById(R.id.card_notice);
        headerApps = view.findViewById(R.id.header_apps);
        headerNotice = view.findViewById(R.id.header_notice);
        contentApps = view.findViewById(R.id.content_apps);
        contentNotice = view.findViewById(R.id.content_notice);
        arrowApps = view.findViewById(R.id.arrow_apps);
        arrowNotice = view.findViewById(R.id.arrow_notice);
        
        headerApps.setOnClickListener(v -> toggleAppsSection());
        headerNotice.setOnClickListener(v -> toggleNoticeSection());
        
        // 设置菜单按钮点击事件
        menuButtonApps.setOnClickListener(v -> showPopupMenuApps(v));
        menuButtonNotice.setOnClickListener(v -> showPopupMenuNotice(v));
    }
    
    private void toggleAppsSection() {
        isAppsExpanded = !isAppsExpanded;
        // 如果展开应用列表，建议折叠通知设置，以获得更多空间
        if (isAppsExpanded) {
            isNoticeExpanded = false;
        }
        updateUIState();
    }
    
    private void toggleNoticeSection() {
        isNoticeExpanded = !isNoticeExpanded;
        // 如果展开通知设置，建议折叠应用列表
        if (isNoticeExpanded) {
            isAppsExpanded = false;
        }
        updateUIState();
    }
    
    private void updateUIState() {
        // 更新应用列表板块状态
        contentApps.setVisibility(isAppsExpanded ? View.VISIBLE : View.GONE);
        arrowApps.setRotation(isAppsExpanded ? 180 : 0);
        menuButtonApps.setVisibility(isAppsExpanded ? View.VISIBLE : View.GONE);
        menuButtonNotice.setVisibility(isNoticeExpanded ? View.VISIBLE : View.GONE);
        
        LinearLayout.LayoutParams paramsApps = (LinearLayout.LayoutParams) cardApps.getLayoutParams();
        if (isAppsExpanded) {
            paramsApps.height = 0;
            paramsApps.weight = 1;
        } else {
            paramsApps.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            paramsApps.weight = 0;
        }
        cardApps.setLayoutParams(paramsApps);
        
        // 更新通知设置板块状态
        contentNotice.setVisibility(isNoticeExpanded ? View.VISIBLE : View.GONE);
        arrowNotice.setRotation(isNoticeExpanded ? 180 : 0);
        
        LinearLayout.LayoutParams paramsNotice = (LinearLayout.LayoutParams) cardNotice.getLayoutParams();
        if (isNoticeExpanded) {
            paramsNotice.height = 0;
            paramsNotice.weight = 1;
        } else {
            paramsNotice.height = LinearLayout.LayoutParams.WRAP_CONTENT;
            paramsNotice.weight = 0;
        }
        cardNotice.setLayoutParams(paramsNotice);
    }
    
    private void showPopupMenuApps(View anchorView) {
        PopupMenu popup = new PopupMenu(mContext, anchorView);
        popup.getMenuInflater().inflate(R.menu.apps_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            
            // 全选操作
            if (id == R.id.action_select_all_enable) {
                batchUpdateApps(true, 0);
                return true;
            } else if (id == R.id.action_select_all_model_filter) {
                batchUpdateApps(true, 1);
                return true;
            } else if (id == R.id.action_select_all_auto_expand) {
                batchUpdateApps(true, 2);
                return true;
            } else if (id == R.id.action_select_all_notification_vibration) {
                batchUpdateApps(true, 3);
                return true;
            } else if (id == R.id.action_select_all_notification_sound) {
                batchUpdateApps(true, 4);
                return true;
            } 
            // 全不选操作
            else if (id == R.id.action_deselect_all_enable) {
                batchUpdateApps(false, 0);
                return true;
            } else if (id == R.id.action_deselect_all_model_filter) {
                batchUpdateApps(false, 1);
                return true;
            } else if (id == R.id.action_deselect_all_auto_expand) {
                batchUpdateApps(false, 2);
                return true;
            } else if (id == R.id.action_deselect_all_notification_vibration) {
                batchUpdateApps(false, 3);
                return true;
            } else if (id == R.id.action_deselect_all_notification_sound) {
                batchUpdateApps(false, 4);
                return true;
            }
            // 过滤操作
            else if (id == R.id.action_filter_all) {
                currentFilterType = 0;
                filterApps(currentFilterType);
                return true;
            } else if (id == R.id.action_filter_user) {
                currentFilterType = 1;
                filterApps(currentFilterType);
                return true;
            } else if (id == R.id.action_filter_system) {
                currentFilterType = 2;
                filterApps(currentFilterType);
                return true;
            }
            return false;
        });
        
        popup.show();
    }
    
    private void showPopupMenuNotice(View anchorView) {
        PopupMenu popup = new PopupMenu(mContext, anchorView);
        popup.getMenuInflater().inflate(R.menu.notice_menu, popup.getMenu());
        
        // 从SharedPreferences管理器检查当前使用的模型
        String modelType = SharedPreferencesManager.getInstance(mContext).getFilterModel();
        
        // 如果是在线模型，隐藏"需要"选项，因为在线模型不支持用户反馈学习
        if (SettingsFragment.MODEL_ONLINE.equals(modelType)) {
            popup.getMenu().findItem(R.id.notice_need).setVisible(false);
        }
        
        popup.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.notice_need) {
                noticeNeed();
                return true;
            } else if (id == R.id.notice_delete) {
                noticeDelete();
                return true;
            } else if (id == R.id.notice_clear_all) {
                noticeClearAll();
                return true;
            }
            return false;
        });
        
        popup.show();
    }

    private void noticeNeed() {
        if (noticeAdapter == null || filteredNotificationList == null) return;
        
        List<FilteredNotificationManager.FilteredNotification> toRemove = new ArrayList<>();
        FilteredNotificationManager manager = FilteredNotificationManager.getInstance(mContext);
        LocalModelManager mlManager = LocalModelManager.getInstance(mContext);
        
        // 检查是否启用了模型过滤
        boolean modelFilteringEnabled = SharedPreferencesManager.getInstance(mContext).isModelFilteringEnabled();
        String filterModel = SharedPreferencesManager.getInstance(mContext).getFilterModel();
        
        for (FilteredNotificationManager.FilteredNotification notification : filteredNotificationList) {
            if (notification.isChecked) {
                    // 只有在启用模型过滤且使用本地模型时才进行训练
                    if (modelFilteringEnabled && filterModel.equals("model_local")) {
                        // 正向反馈到本地模型 - 使用新的分离接口
                        String trainingText = (notification.content != null ? notification.content : "");
                        mlManager.processPositive(notification.title, trainingText);
                    }
                toRemove.add(notification);
            }
        }
        
        for (FilteredNotificationManager.FilteredNotification notification : toRemove) {
            manager.removeNotification(notification);
        }
        
        loadNotices();
        Toast.makeText(mContext, getString(R.string.notice_need_feedback), Toast.LENGTH_SHORT).show();
    }

    private void noticeDelete() {
        if (noticeAdapter == null || filteredNotificationList == null) return;
        
        List<FilteredNotificationManager.FilteredNotification> toRemove = new ArrayList<>();
        FilteredNotificationManager manager = FilteredNotificationManager.getInstance(mContext);
        
        for (FilteredNotificationManager.FilteredNotification notification : filteredNotificationList) {
            if (notification.isChecked) {
                toRemove.add(notification);
            }
        }
        
        for (FilteredNotificationManager.FilteredNotification notification : toRemove) {
            manager.removeNotification(notification);
        }
        
        loadNotices();
    }

    private void noticeClearAll() {
        FilteredNotificationManager.getInstance(mContext).clearAll();
        loadNotices();
    }
    
    private void loadNotices() {
        filteredNotificationList = FilteredNotificationManager.getInstance(mContext).getAllNotifications();
        if (noticeAdapter == null) {
            noticeAdapter = new FilteredNotificationAdapter(mContext, filteredNotificationList);
            noticeListView.setAdapter(noticeAdapter);
        } else {
            noticeAdapter.updateList(filteredNotificationList);
        }
    }
    
    private void loadApps() {
        // 使用AsyncTask在后台加载应用列表
        new LoadAppsTask().execute();
    }
    
    private void filterApps(int filterType) {
        if (allAppsList == null) return;
        
        filteredAppsList = new ArrayList<>();
        
        switch (filterType) {
            case 0: // 所有应用
                filteredAppsList.addAll(allAppsList);
                break;
            case 1: // 用户应用
                for (AppInfoManager app : allAppsList) {
                    if (!app.isSystemApp()) {
                        filteredAppsList.add(app);
                    }
                }
                break;
            case 2: // 系统应用
                for (AppInfoManager app : allAppsList) {
                    if (app.isSystemApp()) {
                        filteredAppsList.add(app);
                    }
                }
                break;
        }
        
        // 更新列表
        if (adapter != null) {
            adapter.updateList(filteredAppsList);
        }
    }
    
    private void batchUpdateApps(boolean isChecked, int type) {
        if (filteredAppsList == null) return;
        
        // 获取所有应用包名列表
        List<String> packageNames = new ArrayList<>();
        for (AppInfoManager app : filteredAppsList) {
            packageNames.add(app.getPackageName());
        }
        
        // 使用SharedPreferencesManager批量更新
        SharedPreferencesManager manager = SharedPreferencesManager.getInstance(mContext);
        manager.batchUpdateMultipleApps(packageNames, type, isChecked);
        
        // 更新AppInfo对象状态（用于UI显示）
        for (AppInfoManager app : filteredAppsList) {
            if (type == 0) {
                app.setChecked(isChecked);
            } else if (type == 1) {
                app.setModelFilterChecked(isChecked);
            } else if (type == 2) {
                app.setAutoExpandChecked(isChecked);
            } else if (type == 3) {
                app.setNotificationVibrationEnabled(isChecked);
            } else if (type == 4) {
                app.setNotificationSoundEnabled(isChecked);
            }
        }
        
        // 通知适配器更新
        if (adapter != null) {
            adapter.notifyDataSetChanged();
        }
    }
    
    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfoManager>> {
        @Override
        protected List<AppInfoManager> doInBackground(Void... voids) {
            // 获取所有应用（包括系统应用）
            return AppUtils.getInstalledApps(mContext, true);
        }
        
        @Override
        protected void onPostExecute(List<AppInfoManager> appInfoManagerList) {
            allAppsList = appInfoManagerList;
            filteredAppsList = new ArrayList<>(allAppsList);
            
            // 初始化适配器
            adapter = new AppInfoAdapter(mContext, filteredAppsList);
            listView.setAdapter(adapter);
            
            // 应用当前选择的过滤器
            filterApps(currentFilterType);
        }
    }
}
