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
            Intent intent = new Intent(this, MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            startActivity(intent);
        });
        btnExit.setOnClickListener(v -> finish());

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
