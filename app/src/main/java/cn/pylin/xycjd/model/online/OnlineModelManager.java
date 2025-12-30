package cn.pylin.xycjd.model.online;

import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import cn.pylin.xycjd.R;
import cn.pylin.xycjd.manager.SharedPreferencesManager;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 在线模型管理器 - 重构版
 * 实现OpenAI接口规范的调用
 */
public class OnlineModelManager {
    
    private static OnlineModelManager instance;
    private final ExecutorService executor;
    private final Context context;
    private final OkHttpClient client;

    private OnlineModelManager(Context context) {
        this.context = context.getApplicationContext();
        this.executor = Executors.newSingleThreadExecutor();
        this.client = new OkHttpClient();
    }

    public static synchronized OnlineModelManager getInstance(Context context) {
        if (instance == null) {
            instance = new OnlineModelManager(context);
        }
        return instance;
    }

    public interface FilterCallback {
        void onResult(boolean shouldFilter, float score);
    }

    /**
     * 异步检查是否需要过滤
     */
    public void checkFilter(String title, String content, FilterCallback callback) {
        executor.execute(() -> {
            float score = executeApiCall(title, content);
            float filteringDegree = SharedPreferencesManager.getInstance(context).getOnlineFilteringDegree();
            boolean shouldFilter = score <= filteringDegree;
            
            if (callback != null) {
                callback.onResult(shouldFilter, score);
            }
        });
    }

    /**
     * 执行API调用并返回分数（实际使用的方法）
     * 失败时返回10.0f
     */
    public float executeApiCall(String title, String content) {
        try {
            return callOnlineApi(title, content, 
                SharedPreferencesManager.getInstance(context));
        } catch (Exception e) {
            Log.e("OnlineModelManager", "API call failed: " + e.getMessage());
            return 10.0f;
        }
    }

    /**
     * 核心API调用方法 - 使用配置管理器
     */
    private float callOnlineApi(String title, String content, SharedPreferencesManager manager) throws Exception {
        return callOnlineApi(title, content, 
            manager.getOnlineApiUrl(),
            manager.getOnlineApiKey(),
            manager.getOnlineModelName(),
            manager.getOnlineModelPrompt(),
            manager.getTemperature());
    }

    /**
     * 测试API连接方法
     * @return true 如果测试成功返回0.0f-10.0f的分数
     */
    public boolean testApiConnection(String apiUrl, String apiKey, String modelName, 
                                   String systemPrompt, float temperature) {
        try {
            float score = callOnlineApi("测试标题", "测试内容", 
                apiUrl, apiKey, modelName, systemPrompt, temperature);
            return score >= 0.0f && score <= 10.0f;
        } catch (Exception e) {
            Log.e("OnlineModelManager", "Test failed: " + e.getMessage());
            return false;
        }
    }

    /**
     * 核心API调用方法 - 使用OkHttp
     */
    private float callOnlineApi(String title, String content, 
                               String apiUrl, String apiKey, String modelName,
                               String systemPrompt, float temperature) throws Exception {

        // 构建URL
        String finalUrl = apiUrl.replaceAll("/+$", "");
        if (!finalUrl.endsWith("/chat/completions")) {
            finalUrl += "/chat/completions";
        }

        // 构建请求体
        JSONObject requestBody = buildRequestBody(title, content, modelName, systemPrompt, temperature);
        
        // 创建请求
        Request request = new Request.Builder()
                .url(finalUrl)
                .addHeader("Content-Type", "application/json")
                .addHeader("Authorization", "Bearer " + apiKey)
                .post(RequestBody.create(requestBody.toString(), MediaType.get("application/json; charset=utf-8")))
                .build();

        // 执行请求并解析响应
        try (Response response = client.newCall(request).execute()) {
            if (response.isSuccessful() && response.body() != null) {
                // 直接使用 OkHttp 的 string() 方法获取响应内容
                String responseString = response.body().string();
                return parseScoreFromResponse(responseString);
            } else {
                throw new Exception("HTTP error: " + response.code());
            }
        }
    }

    /**
     * 构建请求体
     */
    private JSONObject buildRequestBody(String title, String content, String modelName, 
                                      String systemPrompt, float temperature) throws Exception {
        // 截断内容
        String safeTitle = title != null ? title.substring(0, Math.min(50, title.length())) : "";
        String safeContent = content != null ? content.substring(0, Math.min(200, content.length())) : "";

        // 获取系统提示词
        if (systemPrompt == null || systemPrompt.isEmpty()) {
            systemPrompt = context.getString(R.string.default_prompt_content);
        }

        // 构建消息
        JSONObject systemMessage = new JSONObject();
        systemMessage.put("role", "system");
        systemMessage.put("content", systemPrompt);

        JSONObject userMessage = new JSONObject();
        userMessage.put("role", "user");
        userMessage.put("content", String.format("标题：%s\n内容：%s", safeTitle, safeContent));

        JSONArray messages = new JSONArray();
        messages.put(systemMessage);
        messages.put(userMessage);

        // 构建请求体
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", modelName);
        requestBody.put("messages", messages);
        requestBody.put("temperature", temperature);

        return requestBody;
    }

    /**
     * 从API响应中解析分数
     */
    private float parseScoreFromResponse(String responseString) throws Exception {
        JSONObject jsonResponse = new JSONObject(responseString);
        JSONArray choices = jsonResponse.getJSONArray("choices");
        
        if (choices.length() > 0) {
            String contentResult = choices.getJSONObject(0)
                .getJSONObject("message")
                .getString("content")
                .trim();
            
            // 提取数字
            String numberStr = contentResult.replaceAll("[^0-9.]", "");
            if (!numberStr.isEmpty()) {
                float score = Float.parseFloat(numberStr);
                return Math.max(0f, Math.min(10f, score));
            }
        }
        
        throw new Exception("No valid score found in response");
    }
}
