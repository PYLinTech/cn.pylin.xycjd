package cn.pylin.xycjd;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class NotificationLogManager {
    private static NotificationLogManager instance;
    private final List<String> logs = new ArrayList<>();
    private final List<LogListener> listeners = new ArrayList<>();
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault());

    public interface LogListener {
        void onLogAdded(String log);
    }

    public static synchronized NotificationLogManager getInstance() {
        if (instance == null) {
            instance = new NotificationLogManager();
        }
        return instance;
    }

    public void log(String message) {
        // 如果没有监听器，则不记录日志
        synchronized (listeners) {
            if (listeners.isEmpty()) {
                return;
            }
        }
        
        String timestamp = dateFormat.format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n\n";
        synchronized (logs) {
            logs.add(logEntry);
        }
        notifyListeners(logEntry);
    }

    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
    }

    public List<String> getLogs() {
        synchronized (logs) {
            return new ArrayList<>(logs);
        }
    }

    public void addListener(LogListener listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }

    public void removeListener(LogListener listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }

    private void notifyListeners(String log) {
        synchronized (listeners) {
            for (LogListener listener : listeners) {
                listener.onLogAdded(log);
            }
        }
    }
}
