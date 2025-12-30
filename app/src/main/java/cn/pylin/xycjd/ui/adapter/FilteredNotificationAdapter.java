package cn.pylin.xycjd.ui.adapter;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.List;

import cn.pylin.xycjd.manager.FilteredNotificationManager;
import cn.pylin.xycjd.R;

public class FilteredNotificationAdapter extends BaseAdapter {
    private Context context;
    private List<FilteredNotificationManager.FilteredNotification> notificationList;
    private PackageManager packageManager;

    public FilteredNotificationAdapter(Context context, List<FilteredNotificationManager.FilteredNotification> notificationList) {
        this.context = context;
        this.notificationList = notificationList;
        this.packageManager = context.getPackageManager();
    }

    public void updateList(List<FilteredNotificationManager.FilteredNotification> list) {
        this.notificationList = list;
        notifyDataSetChanged();
    }

    @Override
    public int getCount() {
        return notificationList == null ? 0 : notificationList.size();
    }

    @Override
    public Object getItem(int position) {
        return notificationList.get(position);
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = LayoutInflater.from(context).inflate(R.layout.notice_item, parent, false);
            holder = new ViewHolder();
            holder.icon = convertView.findViewById(R.id.notice_icon);
            holder.title = convertView.findViewById(R.id.notice_name);
            holder.content = convertView.findViewById(R.id.notice_content);
            holder.checkBox = convertView.findViewById(R.id.notice_checkbox);
            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        FilteredNotificationManager.FilteredNotification notification = notificationList.get(position);

        holder.title.setText(notification.title);
        holder.content.setText(notification.content);
        
        // Remove listener before setting check state to avoid triggering it
        holder.checkBox.setOnCheckedChangeListener(null);
        holder.checkBox.setChecked(notification.isChecked);
        holder.checkBox.setOnCheckedChangeListener((buttonView, isChecked) -> {
            notification.isChecked = isChecked;
        });

        // Load app icon
        try {
            Drawable icon = packageManager.getApplicationIcon(notification.packageName);
            holder.icon.setImageDrawable(icon);
        } catch (PackageManager.NameNotFoundException e) {
            holder.icon.setImageResource(android.R.drawable.sym_def_app_icon);
        }

        return convertView;
    }

    static class ViewHolder {
        ImageView icon;
        TextView title;
        TextView content;
        CheckBox checkBox;
    }
}
