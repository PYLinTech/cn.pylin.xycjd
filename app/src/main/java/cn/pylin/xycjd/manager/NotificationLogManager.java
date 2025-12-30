package cn.pylin.xycjd.manager;

import android.content.Context;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
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
    private Context context;
    private static final String LOG_FILE_NAME = "notification_logs.txt";
    private static final String PREF_LOG_RECORDING = "notification_log_recording";

    public interface LogListener {
        void onLogAdded(String log);
    }

    private NotificationLogManager(Context context) {
        this.context = context.getApplicationContext();
        loadLogsFromFile(); // 启动时加载已有日志
    }

    public static synchronized NotificationLogManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationLogManager(context);
        }
        return instance;
    }

    public void log(String message) {
        // 检查是否开启记录（从SharedPreferences实时读取）
        if (!isRecording()) {
            return;
        }
        
        String timestamp = dateFormat.format(new Date());
        String logEntry = "[" + timestamp + "] " + message + "\n\n";
        
        synchronized (logs) {
            logs.add(logEntry);
        }
        
        // 保存到文件
        saveLogToFile(logEntry);
        
        // 通知UI更新
        synchronized (listeners) {
            if (!listeners.isEmpty()) {
                notifyListeners(logEntry);
            }
        }
    }

    public void clearLogs() {
        synchronized (logs) {
            logs.clear();
        }
        // 清除文件内容
        clearLogFile();
    }

    public void startRecording() {
        SharedPreferencesManager.getInstance(context).setNotificationLogRecording(true);
    }

    public void stopRecording() {
        SharedPreferencesManager.getInstance(context).setNotificationLogRecording(false);
    }

    public boolean isRecording() {
        return SharedPreferencesManager.getInstance(context).isNotificationLogRecording();
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

    /**
     * 保存日志到文件
     */
    private void saveLogToFile(String logEntry) {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file, true); // 追加模式
            fos.write(logEntry.getBytes());
            fos.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 从文件加载日志
     */
    private void loadLogsFromFile() {
        synchronized (logs) {
            logs.clear();
            try {
                File file = new File(context.getFilesDir(), LOG_FILE_NAME);
                if (!file.exists()) {
                    return;
                }
                
                FileInputStream fis = new FileInputStream(file);
                BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
                StringBuilder lineBuilder = new StringBuilder();
                String line;
                
                while ((line = reader.readLine()) != null) {
                    lineBuilder.append(line).append("\n");
                    // 如果遇到空行，说明一个完整的日志条目结束
                    if (line.trim().isEmpty()) {
                        String logEntry = lineBuilder.toString();
                        if (!logEntry.trim().isEmpty()) {
                            logs.add(logEntry);
                        }
                        lineBuilder = new StringBuilder();
                    }
                }
                
                reader.close();
                fis.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }

    /**
     * 清除日志文件
     */
    private void clearLogFile() {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 导出日志到下载目录的.log文件
     * @return 导出的文件路径，失败返回null
     */
    public String exportLogsToLogFile() {
        try {
            // 获取下载目录路径
            File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            );
            
            if (downloadDir == null) {
                return null;
            }
            
            // 确保下载目录存在
            if (!downloadDir.exists()) {
                downloadDir.mkdirs();
            }
            
            // 创建子文件夹结构：Download/cn.pylin.xycjd/log/
            File appDir = new File(downloadDir, "cn.pylin.xycjd");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            File logDir = new File(appDir, "log");
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 生成文件名，使用时间戳
            String timestamp = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date());
            String fileName = "Notification_logs_" + timestamp + ".log";
            File exportFile = new File(logDir, fileName);
            
            // 读取当前所有日志
            List<String> logs = getLogs();
            if (logs.isEmpty()) {
                return null;
            }
            
            // 写入到导出文件
            FileOutputStream fos = new FileOutputStream(exportFile);
            for (String log : logs) {
                fos.write(log.getBytes());
            }
            fos.close();
            
            return exportFile.getAbsolutePath();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * 获取导出目录的路径（下载目录/cn.pylin.xycjd/log/）
     * @return 导出目录路径
     */
    public String getExportDirectoryPath() {
        File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
            android.os.Environment.DIRECTORY_DOWNLOADS
        );
        if (downloadDir == null) {
            return null;
        }
        
        // 返回子文件夹路径：Download/cn.pylin.xycjd/log/
        File appDir = new File(downloadDir, "cn.pylin.xycjd");
        File logDir = new File(appDir, "log");
        return logDir.getAbsolutePath();
    }
}
