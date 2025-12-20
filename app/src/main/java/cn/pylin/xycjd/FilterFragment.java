package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
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

public class FilterFragment extends Fragment {
    
    private Context mContext;
    private ListView listView;
    private ListView noticeListView;
    private ImageButton menuButtonApps, menuButtonNotice;
    private View headerApps, headerNotice;
    private View contentApps, contentNotice;
    private View cardApps, cardNotice;
    private ImageButton arrowApps, arrowNotice;
    
    private List<AppInfo> allAppsList;
    private List<AppInfo> filteredAppsList;
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
        
        headerApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleAppsSection();
            }
        });
        
        headerNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                toggleNoticeSection();
            }
        });
        
        // 设置菜单按钮点击事件
        menuButtonApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenuApps(v);
            }
        });
        
        menuButtonNotice.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenuNotice(v);
            }
        });
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
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                int id = item.getItemId();
                if (id == R.id.action_select_all) {
                    selectAllApps();
                    return true;
                } else if (id == R.id.action_deselect_all) {
                    deselectAllApps();
                    return true;
                } else if (id == R.id.action_filter_all) {
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
            }
        });
        
        popup.show();
    }
    
    private void showPopupMenuNotice(View anchorView) {
        PopupMenu popup = new PopupMenu(mContext, anchorView);
        popup.getMenuInflater().inflate(R.menu.notice_menu, popup.getMenu());
        
        popup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
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
            }
        });
        
        popup.show();
    }

    private void noticeNeed() {
        if (noticeAdapter == null || filteredNotificationList == null) return;
        
        List<FilteredNotificationManager.FilteredNotification> toRemove = new ArrayList<>();
        FilteredNotificationManager manager = FilteredNotificationManager.getInstance(mContext);
        NotificationMLManager mlManager = NotificationMLManager.getInstance(mContext);
        
        for (FilteredNotificationManager.FilteredNotification notification : filteredNotificationList) {
            if (notification.isChecked) {
                // 正向反馈到模型 (分数 10)
                // 只使用内容进行训练
                String trainingText = (notification.content != null ? notification.content : "");
                mlManager.train(trainingText, 10f);
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
                for (AppInfo app : allAppsList) {
                    if (!app.isSystemApp()) {
                        filteredAppsList.add(app);
                    }
                }
                break;
            case 2: // 系统应用
                for (AppInfo app : allAppsList) {
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
    
    private void selectAllApps() {
        if (adapter != null && filteredAppsList != null) {
            SharedPreferences prefs = mContext.getSharedPreferences("app_checkboxes", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            for (AppInfo app : filteredAppsList) {
                app.setChecked(true);
                editor.putBoolean(app.getPackageName(), true);
            }
            editor.apply();
            adapter.notifyDataSetChanged();
        }
    }
    
    private void deselectAllApps() {
        if (adapter != null && filteredAppsList != null) {
            SharedPreferences prefs = mContext.getSharedPreferences("app_checkboxes", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = prefs.edit();
            
            for (AppInfo app : filteredAppsList) {
                app.setChecked(false);
                editor.putBoolean(app.getPackageName(), false);
            }
            editor.apply();
            adapter.notifyDataSetChanged();
        }
    }
    
    private class LoadAppsTask extends AsyncTask<Void, Void, List<AppInfo>> {
        @Override
        protected List<AppInfo> doInBackground(Void... voids) {
            // 获取所有应用（包括系统应用）
            return AppUtils.getInstalledApps(mContext, true);
        }
        
        @Override
        protected void onPostExecute(List<AppInfo> appInfoList) {
            allAppsList = appInfoList;
            filteredAppsList = new ArrayList<>(allAppsList);
            
            // 初始化适配器
            adapter = new AppInfoAdapter(mContext, filteredAppsList);
            listView.setAdapter(adapter);
            
            // 应用当前选择的过滤器
            filterApps(currentFilterType);
        }
    }
}
