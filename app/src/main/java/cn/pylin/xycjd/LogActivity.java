package cn.pylin.xycjd;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

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

        btnReturn.setOnClickListener(v -> {
            // 只销毁Activity，不停止记录
            finish();
        });
        btnExit.setOnClickListener(v -> {
            // 停止记录、清除日志、销毁Activity
            NotificationLogManager.getInstance().stopRecording();
            NotificationLogManager.getInstance().clearLogs();
            finish();
        });

        // 确保记录处于开启状态
        NotificationLogManager.getInstance().startRecording();

        // Load existing logs
        StringBuilder sb = new StringBuilder();
        for (String log : NotificationLogManager.getInstance().getLogs()) {
            sb.append(log);
        }
        tvLog.setText(sb.toString());

        // Scroll to bottom
        scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));

        NotificationLogManager.getInstance().addListener(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        NotificationLogManager.getInstance().removeListener(this);
        // 不再自动清除日志，让记录继续
    }

    @Override
    public void onLogAdded(String log) {
        runOnUiThread(() -> {
            tvLog.append(log);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}
