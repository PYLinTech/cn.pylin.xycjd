package cn.pylin.xycjd;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    private NestedScrollView scrollView;
    private TextView tvVersion;
    private TextView tvWebsite;
    private TextView tvGithub;
    private TextView tvQqGroup;
    
    // 布局容器
    private ConstraintLayout layoutWebsite;
    private ConstraintLayout layoutGithub;
    private ConstraintLayout layoutQqGroup;

    private final long[] mHits = new long[5];

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_about, container, false);
        
        // 初始化控件
        initViews(view);
        
        // 设置版本号
        setVersionInfo();
        
        // 设置点击事件
        setClickListeners();
        
        return view;
    }

    private void initViews(View view) {
        scrollView = view.findViewById(R.id.nested_scroll_view);
        tvVersion = view.findViewById(R.id.tv_version);
        tvWebsite = view.findViewById(R.id.tv_website);
        tvGithub = view.findViewById(R.id.tv_github);
        tvQqGroup = view.findViewById(R.id.tv_qq_group);
        
        // 初始化布局容器
        layoutWebsite = view.findViewById(R.id.layout_website);
        layoutGithub = view.findViewById(R.id.layout_github);
        layoutQqGroup = view.findViewById(R.id.layout_qq_group);
    }

    private void setVersionInfo() {
        try {
            PackageInfo packageInfo = requireActivity().getPackageManager().getPackageInfo(
                    requireActivity().getPackageName(), 0);
            String versionName = packageInfo.versionName;
            int versionCode = packageInfo.versionCode;
            
            String versionText = getString(R.string.app_version, versionName, versionCode);
            tvVersion.setText(versionText);
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
            tvVersion.setText(getString(R.string.app_version_error));
        }
    }

    private void setClickListeners() {
        tvVersion.setOnClickListener(v -> {
            System.arraycopy(mHits, 1, mHits, 0, mHits.length - 1);
            mHits[mHits.length - 1] = android.os.SystemClock.uptimeMillis();
            if (mHits[0] >= (android.os.SystemClock.uptimeMillis() - 10000)) {
                Intent intent = new Intent(requireContext(), LogActivity.class);
                startActivity(intent);
                java.util.Arrays.fill(mHits, 0);
            }
        });

        // 官网地址点击事件
        layoutWebsite.setOnClickListener(v -> {
            openUrl(getString(R.string.website_url));
        });
        
        // 开源地址点击事件
        layoutGithub.setOnClickListener(v -> {
            openUrl(getString(R.string.github_url));
        });
        
        // QQ交流群点击事件
        layoutQqGroup.setOnClickListener(v -> {
            // 使用官网生成的key发起加群请求
            boolean result = joinQQGroup(getString(R.string.qq_group_key));
            if (!result) {
                Toast.makeText(requireContext(), getString(R.string.qq_join_failed), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void openUrl(String url) {
        try {
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            startActivity(intent);
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(requireContext(), getString(R.string.open_url_failed), Toast.LENGTH_SHORT).show();
        }
    }

     public boolean joinQQGroup(String key) { 
         Intent intent = new Intent(); 
         intent.setData(Uri.parse(getString(R.string.qq_group_url_prefix) + key)); 
         try { 
             startActivity(intent); 
             return true; 
         } catch (Exception e) {
             // 复制群号到剪贴板
             copyToClipboard(getString(R.string.qq_group_number));
             return false; 
         } 
     }

     private void copyToClipboard(String text) {
         if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.HONEYCOMB) {
             android.content.ClipboardManager clipboardManager = (android.content.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
             android.content.ClipData clipData = android.content.ClipData.newPlainText(getString(R.string.clipboard_label), text);
             clipboardManager.setPrimaryClip(clipData);
         } else {
             // 适配旧版本
             android.text.ClipboardManager clipboardManager = (android.text.ClipboardManager) requireContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
             clipboardManager.setText(text);
         }
     }
}