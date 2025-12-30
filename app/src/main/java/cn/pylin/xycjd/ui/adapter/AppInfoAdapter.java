package cn.pylin.xycjd.ui.adapter;

import android.content.Context;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;

import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cn.pylin.xycjd.manager.AppInfoManager;
import cn.pylin.xycjd.R;
import cn.pylin.xycjd.manager.SharedPreferencesManager;
import cn.pylin.xycjd.ui.dialog.AppSettingsDialog;
import cn.pylin.xycjd.utils.AppUtils;

public class AppInfoAdapter extends BaseAdapter {
    
    private Context mContext;
    private List<AppInfoManager> mAppInfoManagerList;
    private LayoutInflater mInflater;
    private SharedPreferencesManager mManager;
    
    public AppInfoAdapter(Context context, List<AppInfoManager> appInfoManagerList) {
        mContext = context;
        mAppInfoManagerList = appInfoManagerList;
        mInflater = LayoutInflater.from(context);
        mManager = SharedPreferencesManager.getInstance(context);
    }
    
    @Override
    public int getCount() {
        return mAppInfoManagerList.size();
    }
    
    @Override
    public Object getItem(int position) {
        return mAppInfoManagerList.get(position);
    }
    
    @Override
    public long getItemId(int position) {
        return position;
    }
    
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.app_info_item, parent, false);
            holder = new ViewHolder();
            holder.appIcon = convertView.findViewById(R.id.app_icon);
            holder.appName = convertView.findViewById(R.id.app_name);
            holder.packageName = convertView.findViewById(R.id.package_name);
            holder.systemApp = convertView.findViewById(R.id.system_app);
            holder.appCheckbox = convertView.findViewById(R.id.app_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        AppInfoManager appInfoManager = mAppInfoManagerList.get(position);
        
        // 设置应用名称
        holder.appName.setText(appInfoManager.getAppName());
        
        // 设置包名
        holder.packageName.setText(appInfoManager.getPackageName());
        
        // 设置是否为系统应用
        if (appInfoManager.isSystemApp()) {
            holder.systemApp.setText(R.string.system_app);
            // 使用主题颜色属性，系统应用使用警告色调
            holder.systemApp.setTextColor(mContext.getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.systemApp.setText(R.string.user_app);
            // 使用主题颜色属性，用户应用使用成功色调
            holder.systemApp.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // 设置勾选框状态
        String packageName = appInfoManager.getPackageName();
        boolean currentChecked = mManager.isAppEnabled(packageName);
        appInfoManager.setChecked(currentChecked);
        
        // 先移除之前的监听器，避免在设置状态时触发
        holder.appCheckbox.setOnCheckedChangeListener(null);
        // 设置勾选框状态
        holder.appCheckbox.setChecked(currentChecked);
        
        // 设置勾选框点击事件
        holder.appCheckbox.setOnCheckedChangeListener((buttonView, newChecked) -> {
            // 使用SharedPreferencesManager保存状态
            mManager.setAppEnabled(packageName, newChecked);
            // 更新AppInfo对象状态
            appInfoManager.setChecked(newChecked);
        });
        
        // 设置整个item的点击事件，弹出Dialog
        convertView.setOnClickListener(v -> {
            // 弹出设置Dialog
            AppSettingsDialog dialog = new AppSettingsDialog(
                mContext,
                appInfoManager.getPackageName(),
                appInfoManager.getAppName(),
                appInfoManager.isChecked()
            );
            dialog.show();
        });
        
        // 异步加载应用图标
        new LoadIconTask(holder.appIcon).execute(appInfoManager.getPackageName());
        
        return convertView;
    }
    
    /**
     * 更新列表数据
     * @param newList 新的应用列表
     */
    public void updateList(List<AppInfoManager> newList) {
        mAppInfoManagerList = newList;
        notifyDataSetChanged();
    }
    
    /**
     * ViewHolder模式优化列表性能
     */
    private static class ViewHolder {
        ImageView appIcon;
        TextView appName;
        TextView packageName;
        TextView systemApp;
        CheckBox appCheckbox;
    }
    
    /**
     * 异步加载应用图标的任务
     */
    private class LoadIconTask extends android.os.AsyncTask<String, Void, Drawable> {
        private ImageView imageView;
        
        public LoadIconTask(ImageView imageView) {
            this.imageView = imageView;
        }
        
        @Override
        protected Drawable doInBackground(String... params) {
            String packageName = params[0];
            return AppUtils.loadAppIcon(mContext, packageName);
        }
        
        @Override
        protected void onPostExecute(Drawable drawable) {
            if (drawable != null && imageView != null) {
                imageView.setImageDrawable(drawable);
            }
        }
    }
}
