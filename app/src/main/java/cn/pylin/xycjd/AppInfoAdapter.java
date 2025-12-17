package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

public class AppInfoAdapter extends BaseAdapter {
    
    private Context mContext;
    private List<AppInfo> mAppInfoList;
    private LayoutInflater mInflater;
    private SharedPreferences mPrefs;
    private SharedPreferences mAutoExpandPrefs;
    
    public AppInfoAdapter(Context context, List<AppInfo> appInfoList) {
        mContext = context;
        mAppInfoList = appInfoList;
        mInflater = LayoutInflater.from(context);
        mPrefs = context.getSharedPreferences("app_checkboxes", Context.MODE_PRIVATE);
        mAutoExpandPrefs = context.getSharedPreferences("app_auto_expand", Context.MODE_PRIVATE);
    }
    
    @Override
    public int getCount() {
        return mAppInfoList.size();
    }
    
    @Override
    public Object getItem(int position) {
        return mAppInfoList.get(position);
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
            holder.appAutoExpandCheckbox = convertView.findViewById(R.id.app_auto_expand_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }
        
        AppInfo appInfo = mAppInfoList.get(position);
        
        // 设置应用名称
        holder.appName.setText(appInfo.getAppName());
        
        // 设置包名
        holder.packageName.setText(appInfo.getPackageName());
        
        // 设置是否为系统应用
        if (appInfo.isSystemApp()) {
            holder.systemApp.setText(R.string.system_app);
            // 使用主题颜色属性，系统应用使用警告色调
            holder.systemApp.setTextColor(mContext.getResources().getColor(android.R.color.holo_orange_dark));
        } else {
            holder.systemApp.setText(R.string.user_app);
            // 使用主题颜色属性，用户应用使用成功色调
            holder.systemApp.setTextColor(mContext.getResources().getColor(android.R.color.holo_green_dark));
        }
        
        // 设置勾选框状态
        String packageName = appInfo.getPackageName();
        boolean isChecked = mPrefs.getBoolean(packageName, false);
        appInfo.setChecked(isChecked);

        boolean isAutoExpandChecked = mAutoExpandPrefs.getBoolean(packageName, false);
        appInfo.setAutoExpandChecked(isAutoExpandChecked);
        
        // 先移除之前的监听器，避免在设置状态时触发
        holder.appCheckbox.setOnCheckedChangeListener(null);
        // 设置勾选框状态
        holder.appCheckbox.setChecked(isChecked);
        
        // 设置勾选框点击事件
        holder.appCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                // 保存状态到SharedPreferences
                mPrefs.edit().putBoolean(packageName, isChecked).apply();
                // 更新AppInfo对象状态
                appInfo.setChecked(isChecked);
            }
        });

        holder.appAutoExpandCheckbox.setOnCheckedChangeListener(null);
        holder.appAutoExpandCheckbox.setChecked(isAutoExpandChecked);
        holder.appAutoExpandCheckbox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                mAutoExpandPrefs.edit().putBoolean(packageName, isChecked).apply();
                appInfo.setAutoExpandChecked(isChecked);
            }
        });
        
        // 异步加载应用图标
        new LoadIconTask(holder.appIcon).execute(appInfo.getPackageName());
        
        return convertView;
    }
    
    /**
     * 更新列表数据
     * @param newList 新的应用列表
     */
    public void updateList(List<AppInfo> newList) {
        mAppInfoList = newList;
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
        CheckBox appAutoExpandCheckbox;
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
