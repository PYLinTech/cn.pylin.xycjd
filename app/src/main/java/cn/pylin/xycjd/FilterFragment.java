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
    private ImageButton menuButton;
    private View headerApps, headerNotice;
    private View contentApps, contentNotice;
    private View cardApps, cardNotice;
    private ImageButton arrowApps, arrowNotice;
    
    private List<AppInfo> allAppsList;
    private List<AppInfo> filteredAppsList;
    private AppInfoAdapter adapter;
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
        
        return view;
    }
    
    private void initViews(View view) {
        listView = view.findViewById(R.id.apps_list);
        menuButton = view.findViewById(R.id.menu_button);
        
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
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
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
        menuButton.setVisibility(isAppsExpanded ? View.VISIBLE : View.GONE);
        
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
    
    private void showPopupMenu(View anchorView) {
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
