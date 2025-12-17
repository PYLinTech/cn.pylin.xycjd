package cn.pylin.xycjd;

import android.os.Bundle;
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
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        toolbar.setNavigationOnClickListener(v -> finish());

        tvLog = findViewById(R.id.tv_log);
        scrollView = findViewById(R.id.scrollView);

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
        NotificationLogManager.getInstance().clearLogs();
    }

    @Override
    public void onLogAdded(String log) {
        runOnUiThread(() -> {
            tvLog.append(log);
            scrollView.post(() -> scrollView.fullScroll(ScrollView.FOCUS_DOWN));
        });
    }
}
