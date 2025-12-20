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
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 腾讯混元模型管理器
 * 实现OpenAI接口规范的调用
 */
public class HunyuanModelManager {
    
    // 从 BuildConfig 获取 API Key
    private static final String API_KEY = BuildConfig.Hunyuan_KEY;
    private static HunyuanModelManager instance;
    private final ExecutorService executor;
    private final Handler mainHandler;
    private final Context context;

    // 腾讯混元 API Endpoint (OpenAI 兼容接口)
    private static final String API_URL = "https://api.hunyuan.cloud.tencent.com/v1/chat/completions"; 
    // 模型名称
    private static final String MODEL_NAME = "hunyuan-lite"; 

    public interface FilterCallback {
        void onResult(boolean shouldFilter, float score);
    }

    private HunyuanModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.mainHandler = new Handler(Looper.getMainLooper());
    }

    public static synchronized HunyuanModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new HunyuanModelManager(context);
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
            // 获取在线过滤程度配置
            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
            float filteringDegree = prefs.getFloat("pref_online_filtering_degree", 5.0f);

            float score = callHunyuanApi(title, content, filteringDegree);
            
            boolean shouldFilter = score <= filteringDegree;
            
            mainHandler.post(() -> {
                if (callback != null) {
                    callback.onResult(shouldFilter, score);
                }
            });
        });
    }

    private float callHunyuanApi(String title, String content, float filteringDegree) {

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

            // 计算动态阈值
            float spamEnd = filteringDegree * 0.4f;
            float lowEnd = filteringDegree;
            float normalEnd = filteringDegree + (10f - filteringDegree) * 0.4f;
            float importantEnd = filteringDegree + (10f - filteringDegree) * 0.8f;

            String systemPrompt = String.format(Locale.US, "你是智能通知评分系统，每次评估通知时仅输出一个0.0到10.0之间的数字，0.0–%.1f表示骚扰或垃圾内容(如广告、诱导点击、虚假信息)，%.1f–%.1f为低价值通知，即可能没用，%.1f–%.1f为普通有用通知，%.1f–%.1f为重要通知(如支付信息等)，%.1f–10.0为关键通知(如聊天等)",
                    spamEnd,
                    spamEnd + 0.1f, lowEnd,
                    lowEnd + 0.1f, normalEnd,
                    normalEnd + 0.1f, importantEnd,
                    importantEnd + 0.1f);

            // 系统提示词
            JSONObject systemMessage = new JSONObject();
            systemMessage.put("role", "system");
            systemMessage.put("content", systemPrompt);
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
                            Log.e("HunyuanModelManager", "Failed to parse score: " + contentResult);
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
