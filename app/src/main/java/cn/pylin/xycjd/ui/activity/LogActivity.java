package cn.pylin.xycjd.ui.activity;

import android.app.AlertDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import java.io.File;

import cn.pylin.xycjd.manager.NotificationLogManager;
import cn.pylin.xycjd.R;

public class LogActivity extends AppCompatActivity implements NotificationLogManager.LogListener {

    private TextView tvLog;
    private ScrollView scrollView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_log);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        tvLog = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scrollView);

        Button btnReturn = findViewById(R.id.btn_return);
        Button btnExit = findViewById(R.id.btn_exit);
        Button btnExport = findViewById(R.id.btn_export);

        btnReturn.setOnClickListener(v -> {
            // 只销毁Activity，不停止记录
            finish();
        });
        
        btnExit.setOnClickListener(v -> {
            // 停止记录、清除日志、销毁Activity
            NotificationLogManager.getInstance(this).stopRecording();
            NotificationLogManager.getInstance(this).clearLogs();
            finish();
        });

        btnExport.setOnClickListener(v -> {
            exportLogs();
        });

        // 进入Activity时开启记录
        NotificationLogManager.getInstance(this).startRecording();

        // 加载已有日志
        StringBuilder sb = new StringBuilder();
        for (String log : NotificationLogManager.getInstance(this).getLogs()) {
            sb.append(log);
        }
        tvLog.setText(sb.toString());

        // 滚动到底部
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        NotificationLogManager.getInstance(this).addListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationLogManager.getInstance(this).removeListener(this);
        // 不再自动清除日志，让记录继续
    }

    @Override
    public void onLogAdded(String log) {
        runOnUiThread(() -> {
            tvLog.append(log);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }

    /**
     * 导出日志到.log文件
     */
    private void exportLogs() {
        NotificationLogManager logManager = NotificationLogManager.getInstance(this);
        
        // 检查是否有日志
        if (logManager.getLogs().isEmpty()) {
            Toast.makeText(this, R.string.log_export_empty, Toast.LENGTH_SHORT).show();
            return;
        }

        // 执行导出
        String exportedPath = logManager.exportLogsToLogFile();
        
        if (exportedPath != null) {
            // 导出成功，显示成功对话框
            showExportSuccessDialog(exportedPath);
        } else {
            // 导出失败
            Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 显示导出成功对话框
     */
    private void showExportSuccessDialog(String exportedPath) {
        String message = getString(R.string.log_export_success, exportedPath);
        
        new AlertDialog.Builder(this)
            .setTitle(R.string.log_export_success_title)
            .setMessage(message)
            .setPositiveButton(R.string.log_share_file, (dialog, which) -> {
                shareLogFile(exportedPath);
            })
            .setCancelable(true)
            .show();
    }

    /**
     * 分享导出的日志文件
     */
    private void shareLogFile(String exportedPath) {
        try {
            File logFile = new File(exportedPath);
            
            // 检查文件是否存在
            if (!logFile.exists()) {
                Toast.makeText(this, R.string.log_export_failed, Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 使用FileProvider获取文件的URI
            Uri uri = androidx.core.content.FileProvider.getUriForFile(
                this,
                "cn.pylin.xycjd.fileprovider",
                logFile
            );
            
            // 创建分享Intent
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            // 使用字符串资源作为分享标题
            String shareTitle = getString(R.string.log_share_file);
            startActivity(Intent.createChooser(intent, shareTitle));
            
        } catch (Exception e) {
            // 如果分享失败，显示错误提示
            Toast.makeText(this, R.string.log_share_failed, Toast.LENGTH_SHORT).show();
        }
    }
}
