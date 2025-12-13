package cn.pylin.xycjd;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private static final int PAGE_SETTINGS = 0;
    private static final int PAGE_APPS = 1;
    private static final int PAGE_ABOUT = 2;

    private int currentPage = PAGE_SETTINGS;

    private Button btnSettings;
    private Button btnApps;
    private Button btnAbout;
    private FrameLayout contentFrame;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnSettings = findViewById(R.id.btn_settings);
        btnApps = findViewById(R.id.btn_apps);
        btnAbout = findViewById(R.id.btn_about);
        contentFrame = findViewById(R.id.content_frame);

        // 设置初始页面
        loadPage(currentPage);
        updateButtonStates();

        // 设置按钮点击事件
        btnSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_SETTINGS) {
                    currentPage = PAGE_SETTINGS;
                    loadPage(currentPage);
                    updateButtonStates();
                }
            }
        });

        btnApps.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_APPS) {
                    currentPage = PAGE_APPS;
                    loadPage(currentPage);
                    updateButtonStates();
                }
            }
        });

        btnAbout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (currentPage != PAGE_ABOUT) {
                    currentPage = PAGE_ABOUT;
                    loadPage(currentPage);
                    updateButtonStates();
                }
            }
        });
    }

    private void loadPage(int page) {
        // 清除之前的页面内容
        contentFrame.removeAllViews();
        
        // 使用if-else逻辑切换页面
        if (page == PAGE_SETTINGS) {
            getLayoutInflater().inflate(R.layout.fragment_settings, contentFrame);
        } else if (page == PAGE_APPS) {
            getLayoutInflater().inflate(R.layout.fragment_apps, contentFrame);
        } else if (page == PAGE_ABOUT) {
            getLayoutInflater().inflate(R.layout.fragment_about, contentFrame);
        }
    }

    private void updateButtonStates() {
        // 更新按钮状态，当前页面按钮显示不同颜色
        if (currentPage == PAGE_SETTINGS) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnApps.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnAbout.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else if (currentPage == PAGE_APPS) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnApps.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
            btnAbout.setTextColor(getResources().getColor(android.R.color.darker_gray));
        } else if (currentPage == PAGE_ABOUT) {
            btnSettings.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnApps.setTextColor(getResources().getColor(android.R.color.darker_gray));
            btnAbout.setTextColor(getResources().getColor(android.R.color.holo_blue_dark));
        }
    }
}