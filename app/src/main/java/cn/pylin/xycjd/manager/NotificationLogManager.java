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
    private static final int MAX_LOGS_IN_MEMORY = 100; // 内存中保存的日志数量（队列大小）
    private static final int MAX_LOGS_IN_FILE = 500; // 文件中保存的日志数量
    
    // 文件写入队列
    private final List<String> fileWriteQueue = new ArrayList<>();
    private boolean isWritingToFile = false;
    private static final int BATCH_WRITE_SIZE = 50; // 批量写入大小

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
            // 使用队列方式：当内存中日志数量超过限制时，移除最旧的日志
            if (logs.size() > MAX_LOGS_IN_MEMORY) {
                logs.remove(0);
            }
        }
        
        // 保存到文件（异步队列方式）
        saveLogToFileAsync(logEntry);
        
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
     * 异步保存日志到文件（队列方式）
     */
    private void saveLogToFileAsync(String logEntry) {
        synchronized (fileWriteQueue) {
            fileWriteQueue.add(logEntry);
            // 如果文件写入队列过大，移除最旧的
            if (fileWriteQueue.size() > MAX_LOGS_IN_FILE) {
                fileWriteQueue.subList(0, fileWriteQueue.size() - MAX_LOGS_IN_FILE).clear();
            }
        }
        
        // 如果没有正在进行的文件写入，启动批量写入
        if (!isWritingToFile) {
            new Thread(() -> {
                processFileWriteQueue();
            }).start();
        }
    }
    
    /**
     * 处理文件写入队列（批量写入）
     */
    private void processFileWriteQueue() {
        isWritingToFile = true;
        
        try {
            while (true) {
                List<String> batchToWrite = new ArrayList<>();
                
                // 从队列中取出一批日志
                synchronized (fileWriteQueue) {
                    if (fileWriteQueue.isEmpty()) {
                        break;
                    }
                    
                    int batchSize = Math.min(BATCH_WRITE_SIZE, fileWriteQueue.size());
                    batchToWrite.addAll(fileWriteQueue.subList(0, batchSize));
                    fileWriteQueue.subList(0, batchSize).clear();
                }
                
                // 批量写入文件
                if (!batchToWrite.isEmpty()) {
                    writeBatchToFile(batchToWrite);
                }
                
                // 短暂休眠，避免过于频繁的写入
                Thread.sleep(100);
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            isWritingToFile = false;
        }
    }
    
    /**
     * 批量写入日志到文件
     */
    private void writeBatchToFile(List<String> batchLogs) {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE_NAME);
            FileOutputStream fos = new FileOutputStream(file, true); // 追加模式
            
            for (String logEntry : batchLogs) {
                fos.write(logEntry.getBytes());
            }
            fos.close();
            
            // 检查文件大小，如果过大则清理
            if (file.length() > 10 * 1024 * 1024) { // 10MB
                cleanupOldLogs();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清理文件中的旧日志，保持文件大小可控
     */
    private void cleanupOldLogs() {
        try {
            File file = new File(context.getFilesDir(), LOG_FILE_NAME);
            if (!file.exists()) {
                return;
            }
            
            // 读取所有日志
            List<String> allLogs = new ArrayList<>();
            FileInputStream fis = new FileInputStream(file);
            BufferedReader reader = new BufferedReader(new InputStreamReader(fis));
            StringBuilder lineBuilder = new StringBuilder();
            String line;
            
            while ((line = reader.readLine()) != null) {
                lineBuilder.append(line).append("\n");
                if (line.trim().isEmpty()) {
                    String logEntry = lineBuilder.toString();
                    if (!logEntry.trim().isEmpty()) {
                        allLogs.add(logEntry);
                    }
                    lineBuilder = new StringBuilder();
                }
            }
            
            reader.close();
            fis.close();
            
            // 如果日志数量超过限制，只保留最新的
            if (allLogs.size() > MAX_LOGS_IN_FILE) {
                List<String> recentLogs = allLogs.subList(allLogs.size() - MAX_LOGS_IN_FILE, allLogs.size());
                
                // 重写文件
                FileOutputStream fos = new FileOutputStream(file, false);
                for (String log : recentLogs) {
                    fos.write(log.getBytes());
                }
                fos.close();
            }
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
                int logCount = 0;
                
                while ((line = reader.readLine()) != null) {
                    lineBuilder.append(line).append("\n");
                    // 如果遇到空行，说明一个完整的日志条目结束
                    if (line.trim().isEmpty()) {
                        String logEntry = lineBuilder.toString();
                        if (!logEntry.trim().isEmpty()) {
                            logs.add(logEntry);
                            logCount++;
                            // 限制从文件加载的日志数量，避免启动时内存溢出
                            if (logCount >= MAX_LOGS_IN_MEMORY) {
                                break;
                            }
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
}
