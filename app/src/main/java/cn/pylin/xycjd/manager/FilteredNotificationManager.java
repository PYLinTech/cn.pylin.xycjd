package cn.pylin.xycjd.manager;

import android.content.Context;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class FilteredNotificationManager {
    private static FilteredNotificationManager instance;
    private static final String FILE_NAME = "filtered_notifications.json";
    private Context context;
    private List<FilteredNotification> notificationList;

    public static class FilteredNotification {
        public String key;
        public String packageName;
        public String title;
        public String content;
        public long timestamp;
        public boolean isChecked;

        public FilteredNotification(String key, String packageName, String title, String content, long timestamp) {
            this.key = key;
            this.packageName = packageName;
            this.title = title;
            this.content = content;
            this.timestamp = timestamp;
            this.isChecked = false;
        }
    }

    private FilteredNotificationManager(Context context) {
        this.context = context.getApplicationContext();
        this.notificationList = new CopyOnWriteArrayList<>();
        loadNotifications();
    }

    public static synchronized FilteredNotificationManager getInstance(Context context) {
        if (instance == null) {
            instance = new FilteredNotificationManager(context);
        }
        return instance;
    }

    public void addNotification(String key, String packageName, String title, String content) {
        FilteredNotification notification = new FilteredNotification(key, packageName, title, content, System.currentTimeMillis());
        notificationList.add(0, notification); // Add to top
        saveNotifications();
    }

    public void removeNotification(FilteredNotification notification) {
        notificationList.remove(notification);
        saveNotifications();
    }

    public void clearAll() {
        notificationList.clear();
        saveNotifications();
    }

    public List<FilteredNotification> getAllNotifications() {
        return notificationList;
    }

    private void loadNotifications() {
        File file = new File(context.getFilesDir(), FILE_NAME);
        if (!file.exists()) return;

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JSONArray jsonArray = new JSONArray(sb.toString());
            for (int i = 0; i < jsonArray.length(); i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                FilteredNotification notification = new FilteredNotification(
                    obj.optString("key"),
                    obj.optString("packageName"),
                    obj.optString("title"),
                    obj.optString("content"),
                    obj.optLong("timestamp")
                );
                notificationList.add(notification);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void saveNotifications() {
        try {
            JSONArray jsonArray = new JSONArray();
            for (FilteredNotification notification : notificationList) {
                JSONObject obj = new JSONObject();
                obj.put("key", notification.key);
                obj.put("packageName", notification.packageName);
                obj.put("title", notification.title);
                obj.put("content", notification.content);
                obj.put("timestamp", notification.timestamp);
                jsonArray.put(obj);
            }
            
            File file = new File(context.getFilesDir(), FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(jsonArray.toString().getBytes());
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
