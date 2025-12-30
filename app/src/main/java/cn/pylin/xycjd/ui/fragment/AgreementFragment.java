package cn.pylin.xycjd.ui.fragment;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import cn.pylin.xycjd.R;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;

public class AgreementFragment extends Fragment {

    private static final String AGREEMENT_URL = "https://api.pylin.cn/xycjd_agreement.json";

    private OkHttpClient client;
    private Handler mainHandler;

    private View loadingContainer;
    private TextView tvLoadError;
    private ScrollView scrollContent;
    private TextView tvContent;
    
    // 定义接口用于通知滚动到底部
    public interface OnScrollToBottomListener {
        void onScrollToBottom();
    }
    
    private OnScrollToBottomListener scrollToBottomListener;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_intro_agreement, container, false);
        
        // 初始化控件
        initViews(view);
        
        // 初始化OkHttp客户端和Handler
        client = new OkHttpClient();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 加载用户协议内容
        loadAgreementContent();
        
        return view;
    }
    
    private void initViews(View view) {
        loadingContainer = view.findViewById(R.id.loading_container);
        tvLoadError = view.findViewById(R.id.tv_load_error);
        scrollContent = view.findViewById(R.id.scroll_content);
        tvContent = view.findViewById(R.id.tv_content);
    }
    
    private void loadAgreementContent() {
        // 显示加载中
        showLoading();
        
        // 创建请求
        Request request = new Request.Builder()
                .url(AGREEMENT_URL)
                .build();
        
        // 异步请求
        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(@NonNull Call call, @NonNull IOException e) {
                // 请求失败
                mainHandler.post(() -> showError());
            }
            
            @Override
            public void onResponse(@NonNull Call call, @NonNull Response response) throws IOException {
                // 请求成功
                if (response.isSuccessful() && response.body() != null) {
                    String content = response.body().string();
                    mainHandler.post(() -> showContent(content));
                } else {
                    mainHandler.post(() -> showError());
                }
            }
        });
    }
    
    private void showLoading() {
        loadingContainer.setVisibility(View.VISIBLE);
        tvLoadError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.GONE);
    }
    
    private void showError() {
        loadingContainer.setVisibility(View.GONE);
        tvLoadError.setVisibility(View.VISIBLE);
        scrollContent.setVisibility(View.GONE);
    }
    
    // 设置滚动到底部监听器
    public void setOnScrollToBottomListener(OnScrollToBottomListener listener) {
        this.scrollToBottomListener = listener;
    }
    
    private void showContent(String content) {
        loadingContainer.setVisibility(View.GONE);
        tvLoadError.setVisibility(View.GONE);
        scrollContent.setVisibility(View.VISIBLE);
        tvContent.setText(content);
        
        // 添加滚动监听
        scrollContent.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {
                // 检查是否滚动到底部
                if (scrollY >= (scrollContent.getChildAt(0).getHeight() - scrollContent.getHeight())) {
                    // 滚动到底部，通知Activity
                    if (scrollToBottomListener != null) {
                        scrollToBottomListener.onScrollToBottom();
                    }
                }
            }
        });
    }
}
