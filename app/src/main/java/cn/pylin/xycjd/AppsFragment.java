package cn.pylin.xycjd;

import android.content.Context;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.util.ArrayList;
import java.util.List;

public class AppsFragment extends Fragment {
    
    private Context mContext;
    private ListView listView;
    private Spinner filterSpinner;
    private List<AppInfo> allAppsList;
    private List<AppInfo> filteredAppsList;
    private AppInfoAdapter adapter;
    
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
        setupFilterSpinner();
        // 设置默认选择用户应用（索引为1）
        filterSpinner.setSelection(1);
        loadApps();
        
        return view;
    }
    
    private void initViews(View view) {
        listView = view.findViewById(R.id.apps_list);
        filterSpinner = view.findViewById(R.id.filter_spinner);
    }
    
    private void setupFilterSpinner() {
        // 直接定义过滤选项，不使用数组资源
        String[] filterOptions;
        if (isChineseLanguage()) {
            filterOptions = new String[]{"所有应用", "用户应用", "系统应用"};
        } else {
            filterOptions = new String[]{"All Apps", "User Apps", "System Apps"};
        }
        
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                mContext,
                android.R.layout.simple_spinner_item,
                filterOptions
        );
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        filterSpinner.setAdapter(spinnerAdapter);
        
        filterSpinner.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                filterApps(position);
            }
            
            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    /**
     * 检查当前是否为中文语言
     */
    private boolean isChineseLanguage() {
        java.util.Locale locale = mContext.getResources().getConfiguration().locale;
        return locale.getLanguage().equals("zh");
    }
    
    private void loadApps() {
        // 检查权限
        if (!AppUtils.hasQueryAllPackagesPermission(mContext)) {
            Toast.makeText(mContext, R.string.query_all_packages_permission_required, Toast.LENGTH_LONG).show();
            return;
        }
        
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
            filterApps(filterSpinner.getSelectedItemPosition());
        }
    }
}