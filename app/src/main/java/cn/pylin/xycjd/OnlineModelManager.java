package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 在线模型管理器
 * 实现OpenAI接口规范的调用
 */
public class OnlineModelManager {
    
    // 从 BuildConfig 获取 API Key
    private static final String API_KEY = BuildConfig.Online_KEY;
    private static OnlineModelManager instance;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context context;

    // 在线 API Endpoint (OpenAI 兼容接口)
    private static final String API_URL = "https://api.online.cloud.tencent.com/v1/chat/completions"; 
    // 模型名称
    private static final String MODEL_NAME = "online-lite"; 

    public interface FilterCallback {
        void onResult(boolean shouldFilter, float score);
    }

    private OnlineModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized OnlineModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new OnlineModelManager(context);
        }
        return instance;
    }

    /**
     * 异步检查是否需要过滤
     * @param title 通知标题
     * @param content 通知内容
     * @param callback 回调接口
     */
    public void checkFilter(String title, String content, FilterCallback callback) {
        executor.execute(() -> {
            float score = callOnlineApi(title, content);

            // 获取在线过滤程度配置
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            float filteringDegree = prefs.getFloat("pref_online_filtering_degree", 5.0f);

            boolean shouldFilter = score <= filteringDegree;
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(shouldFilter, score);
                }
            });
        });
    }

    private float callOnlineApi(String title, String content) {

        try {
            URL url = new URL(API_URL);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty("Authorization", "Bearer " + API_KEY);
            conn.setDoOutput(true);

            // 截断标题和内容
            String safeTitle = title != null ? title : "";
            if (safeTitle.length() > 10) {
                safeTitle = safeTitle.substring(0, 10);
            }
            String safeContent = content != null ? content : "";
            if (safeContent.length() > 20) {
                safeContent = safeContent.substring(0, 20);
            }

            // 构建提示词
            String prompt = String.format("标题：%s\n内容：%s", 
                    safeTitle, 
                    safeContent);

            // 构建请求体 (OpenAI 格式)
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("model", MODEL_NAME);
            
            JSONArray messages = new JSONArray();

            // 系统提示词
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            
            // 获取自定义提示词
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            String defaultPrompt = context.getString(R.string.default_prompt_content);
            String systemContent = prefs.getString("pref_online_model_prompt", defaultPrompt);
            
            systemMessage.put("content", systemContent);
            messages.put(systemMessage);
            
            JSONObject userMessage = new JSONObject();
            userMessage.put("role", "user");
            userMessage.put("content", prompt);
            messages.put(userMessage);

            jsonBody.put("messages", messages);
            jsonBody.put("temperature", 0.5);

            // 发送请求
            try (OutputStream os = conn.getOutputStream()) {
                byte[] input = jsonBody.toString().getBytes("utf-8");
                os.write(input, 0, input.length);
            }

            // 读取响应
            int responseCode = conn.getResponseCode();
            if (responseCode == HttpURLConnection.HTTP_OK) {
                try (BufferedReader br = new BufferedReader(new InputStreamReader(conn.getInputStream(), "utf-8"))) {
                    StringBuilder response = new StringBuilder();
                    String responseLine;
                    while ((responseLine = br.readLine()) != null) {
                        response.append(responseLine.trim());
                    }
                    
                    // 解析响应
                    JSONObject jsonResponse = new JSONObject(response.toString());
                    JSONArray choices = jsonResponse.getJSONArray("choices");
                    if (choices.length() > 0) {
                        JSONObject choice = choices.getJSONObject(0);
                        JSONObject message = choice.getJSONObject("message");
                        String contentResult = message.getString("content").trim();
                        
                        // 尝试解析数字
                        try {
                            // 提取字符串中的数字（防止AI返回"Score: 8.5"等情况）
                            String numberStr = contentResult.replaceAll("[^0-9.]", "");
                            if (!numberStr.isEmpty()) {
                                float score = Float.parseFloat(numberStr);
                                // 限制范围在0-10之间
                                return Math.max(0f, Math.min(10f, score));
                            }
                        } catch (NumberFormatException e) {
                            Log.e("OnlineModelManager", "Failed to parse score: " + contentResult);
                        }
                    }
                }
            } 
        } catch (Exception e) {
            e.printStackTrace();
        }
        // 默认返回满分（放行），避免因网络错误误杀
        return 10.0f;
    }
}
