package cn.pylin.xycjd;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.net.Uri;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.core.content.FileProvider;
import org.json.JSONObject;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.concurrent.TimeUnit;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class UpdateManager {
    private static final String UPDATE_API_URL = "https://api.pylin.cn/xycjd_config.json";
    private final Context context;
    private boolean isManualCheck = false;
    
    public interface UpdateCallback {
        void onUpdateCheckComplete();
        void onError(String message);
    }
    
    public UpdateManager(Context context) {
        this.context = context;
    }
    
    public void checkForUpdates(boolean isManualCheck) {
        checkForUpdates(null, isManualCheck);
    }
    
    public void checkForUpdates(UpdateCallback callback, boolean isManualCheck) {
        this.isManualCheck = isManualCheck;
        
        if (isManualCheck) {
            SharedPreferences prefs = context.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
            prefs.edit().remove("ignored_version_code").apply();
        }
        
        performUpdateCheck(callback);
    }
    
    private void performUpdateCheck(UpdateCallback callback) {
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .build();
        
        Request request = new Request.Builder()
                .url(UPDATE_API_URL)
                .build();
        
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                e.printStackTrace();
                new Handler(Looper.getMainLooper()).post(() -> {
                    if (callback != null) {
                        callback.onError(context.getString(R.string.update_check_failed));
                    }
                });
            }
            
            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String responseBody = response.body().string();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        try {
                            parseAndHandleResponse(responseBody, callback);
                        } catch (Exception e) {
                            e.printStackTrace();
                            if (callback != null) {
                                callback.onError(context.getString(R.string.update_parse_failed) + ": " + e.getMessage());
                            }
                        }
                    });
                } else {
                    new Handler(Looper.getMainLooper()).post(() -> {
                        if (callback != null) {
                            callback.onError(context.getString(R.string.update_check_failed));
                        }
                    });
                }
            }
        });
    }
    
    private void parseAndHandleResponse(String responseBody, UpdateCallback callback) throws Exception {
        JSONObject jsonResponse = new JSONObject(responseBody);
        int latestVersionCode = jsonResponse.getInt("latestVersionCode");
        String downloadUrl = jsonResponse.getString("directlink");
        String updateDescription = jsonResponse.getString("updateDescription");
        
        PackageInfo packageInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
        int currentVersionCode = packageInfo.versionCode;
    
        if (latestVersionCode > currentVersionCode) {
            if (!isManualCheck) {
                SharedPreferences prefs = context.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
                int ignoredVersionCode = prefs.getInt("ignored_version_code", -1);
                
                if (latestVersionCode == ignoredVersionCode) {
                    if (callback != null) {
                        callback.onUpdateCheckComplete();
                    }
                    return;
                }
            }
            
            showUpdateDialog(downloadUrl, updateDescription, latestVersionCode);
        } else {
            cleanupOldApkFiles();
            
            if (isManualCheck) { //手动检查更新时，显示提示信息
                Toast.makeText(context, context.getString(R.string.update_no_update), Toast.LENGTH_SHORT).show(); //显示没有更新的提示信息
            }
        }
        
        if (callback != null) {
            callback.onUpdateCheckComplete();
        }
    }

    private void showUpdateDialog(String downloadUrl, String updateDescription, int currentVersionCode) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_update, null);
        builder.setView(view);
        AlertDialog dialog = builder.create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
        }

        TextView tvUpdateDescription = view.findViewById(R.id.tv_update_description);
        LinearLayout layoutUpdateActions = view.findViewById(R.id.layout_update_actions);
        Button btnUpdateNow = view.findViewById(R.id.btn_update_now);
        TextView tvUpdateLater = view.findViewById(R.id.tv_update_later);
        TextView tvIgnoreUpdate = view.findViewById(R.id.tv_ignore_update);
        
        LinearLayout layoutDownloadProgress = view.findViewById(R.id.layout_download_progress);
        TextView tvDownloadStatus = view.findViewById(R.id.tv_download_status);
        ProgressBar progressDownload = view.findViewById(R.id.progress_download);
        TextView tvProgressPercent = view.findViewById(R.id.tv_progress_percent);
        Button btnCancelDownload = view.findViewById(R.id.btn_cancel_download);

        tvUpdateDescription.setText(updateDescription);

        btnUpdateNow.setOnClickListener(v -> {
            layoutDownloadProgress.setVisibility(View.VISIBLE);
            layoutUpdateActions.setVisibility(View.GONE);
            
            new ApkDownloader(context, dialog, tvDownloadStatus, progressDownload, tvProgressPercent, btnCancelDownload).downloadAndInstall(downloadUrl);
        });

        tvUpdateLater.setOnClickListener(v -> dialog.dismiss());

        tvIgnoreUpdate.setOnClickListener(v -> {
            SharedPreferences prefs = context.getSharedPreferences("UpdatePrefs", Context.MODE_PRIVATE);
            prefs.edit().putInt("ignored_version_code", currentVersionCode).apply();
            dialog.dismiss();
        });

        dialog.setCancelable(false);
        dialog.show();
    }
    
    public void cleanupOldApkFiles() {
        try {
            // 清理新的下载目录中的旧APK文件
            File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            );
            
            if (downloadDir != null && downloadDir.exists()) {
                File appDir = new File(downloadDir, "cn.pylin.xycjd");
                File updateDir = new File(appDir, "update");
                
                if (updateDir.exists()) {
                    File[] apkFiles = updateDir.listFiles();
                    if (apkFiles != null) {
                        for (File file : apkFiles) {
                            if (file.isFile() && file.getName().endsWith(".apk")) {
                                file.delete();
                            }
                        }
                    }
                }
            }
            
            // 同时清理旧的私有目录（向后兼容）
            File oldApkDir = new File(context.getFilesDir(), "apk");
            if (oldApkDir.exists()) {
                File[] oldApkFiles = oldApkDir.listFiles();
                if (oldApkFiles != null) {
                    for (File file : oldApkFiles) {
                        if (file.isFile() && file.getName().endsWith(".apk")) {
                            file.delete();
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private static class ApkDownloader {
        private final Context context;
        private final AlertDialog dialog;
        private final TextView statusText;
        private final TextView progressPercent;
        private final ProgressBar progressBar;
        private final Button cancelButton;
        private final OkHttpClient client;
        private Call currentCall;
    
        public ApkDownloader(Context context, AlertDialog dialog, TextView statusText, ProgressBar progressBar, TextView progressPercent, Button cancelButton) {
            this.context = context;
            this.dialog = dialog;
            this.statusText = statusText;
            this.progressBar = progressBar;
            this.progressPercent = progressPercent;
            this.cancelButton = cancelButton;
            
            this.client = new OkHttpClient.Builder()
                    .connectTimeout(30, TimeUnit.SECONDS)
                    .readTimeout(60, TimeUnit.SECONDS)
                    .writeTimeout(60, TimeUnit.SECONDS)
                    .build();
        }
    
        public void downloadAndInstall(String downloadUrl) {
            new Handler(Looper.getMainLooper()).post(() -> {
                statusText.setText(R.string.update_downloading);
                progressBar.setProgress(0);
                progressPercent.setText("0%");
            });
            
            Request request = new Request.Builder()
                    .url(downloadUrl)
                    .build();
            
            currentCall = client.newCall(request);

            cancelButton.setOnClickListener(v -> {
                if (currentCall != null && !currentCall.isCanceled()) {
                    currentCall.cancel();
                }
                dialog.dismiss();
            });

            currentCall.enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    if (call.isCanceled() || "Canceled".equals(e.getMessage())) {
                        return;
                    }
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(() -> {
                        statusText.setText(R.string.update_download_failed);
                    });
                }
                
                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (!response.isSuccessful()) {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            statusText.setText(R.string.update_download_failed);
                        });
                        return;
                    }
                    
                    try {
                        downloadFileWithProgress(response);
                    } catch (Exception e) {
                        if (call.isCanceled() || "Canceled".equals(e.getMessage()) || (e instanceof IOException && "Socket closed".equals(e.getMessage()))) {
                            return;
                        }
                        e.printStackTrace();
                        new Handler(Looper.getMainLooper()).post(() -> {
                            statusText.setText(R.string.update_download_failed);
                        });
                    }
                }
            });
        }
        
        private void downloadFileWithProgress(Response response) throws IOException {
            long fileLength = response.body().contentLength();
            InputStream input = response.body().byteStream();
            
            // 获取下载目录路径
            File downloadDir = android.os.Environment.getExternalStoragePublicDirectory(
                android.os.Environment.DIRECTORY_DOWNLOADS
            );
            
            // 创建子文件夹结构：Download/cn.pylin.xycjd/update/
            File appDir = new File(downloadDir, "cn.pylin.xycjd");
            if (!appDir.exists()) {
                appDir.mkdirs();
            }
            
            File updateDir = new File(appDir, "update");
            if (!updateDir.exists()) {
                updateDir.mkdirs();
            }
            
            File outputFile = new File(updateDir, "update.apk");
            
            FileOutputStream output = new FileOutputStream(outputFile);
            byte[] data = new byte[4096];
            long total = 0;
            int count;
            
            try {
                while ((count = input.read(data)) != -1) {
                    total += count;
                    output.write(data, 0, count);
                    
                    if (fileLength > 0) {
                        final int progress = (int) (total * 100 / fileLength);
                        new Handler(Looper.getMainLooper()).post(() -> {
                            progressBar.setProgress(progress);
                            statusText.setText(R.string.update_downloading);
                            progressPercent.setText(progress + "%");
                        });
                    }
                }
                
                new Handler(Looper.getMainLooper()).post(() -> {
                    statusText.setText(R.string.update_installing);
                    dialog.dismiss();
                    installApk(outputFile);
                });
                
            } finally {
                try {
                    output.close();
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    
        private void installApk(File apkFile) {
            try {
                Intent intent = new Intent(Intent.ACTION_VIEW);
                Uri uri = FileProvider.getUriForFile(
                        context,
                        "cn.pylin.xycjd.fileprovider",
                        apkFile);
    
                intent.setDataAndType(uri, "application/vnd.android.package-archive");
                intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
