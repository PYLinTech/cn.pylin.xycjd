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
import androidx.fragment.app.Fragment;

public class AboutFragment extends Fragment {

    private TextView tvVersion;
    private TextView tvWebsite;
    private TextView tvGithub;
    private TextView tvQqGroup;

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
        tvVersion = view.findViewById(R.id.tv_version);
        tvWebsite = view.findViewById(R.id.tv_website);
        tvGithub = view.findViewById(R.id.tv_github);
        tvQqGroup = view.findViewById(R.id.tv_qq_group);
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
        // 官网地址点击事件
        tvWebsite.setOnClickListener(v -> {
            openUrl(getString(R.string.website_url));
        });
        
        // 开源地址点击事件
        tvGithub.setOnClickListener(v -> {
            openUrl(getString(R.string.github_url));
        });
        
        // QQ交流群点击事件
        tvQqGroup.setOnClickListener(v -> {
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
        // 此Flag可根据具体产品需要自定义，如设置，则在加群界面按返回，返回手Q主界面，不设置，按返回会返回到呼起产品界面    //intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) 
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