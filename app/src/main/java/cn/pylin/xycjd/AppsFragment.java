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
import android.widget.ListView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class AppsFragment extends Fragment {
    
    private Context mContext;
    private ListView listView;
    private ImageButton menuButton;
    private List<AppInfo> allAppsList;
    private List<AppInfo> filteredAppsList;
    private AppInfoAdapter adapter;
    private int currentFilterType = 1; // 默认为用户应用
    
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        mContext = context;
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_apps, container, false);
        
        initViews(view);
        loadApps();
        
        return view;
    }
    
    private void initViews(View view) {
        listView = view.findViewById(R.id.apps_list);
        menuButton = view.findViewById(R.id.menu_button);
        
        // 设置菜单按钮点击事件
        menuButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showPopupMenu(v);
            }
        });
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