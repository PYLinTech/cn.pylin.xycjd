package cn.pylin.xycjd;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地机器学习文本回归管理器
 * 实现简单的词袋模型 + 在线学习
 * 优化：支持中文 Unigram + Bigram 分词，提升中文语义理解能力
 */
public class NotificationMLManager {
    private static final String TAG = "NotificationMLManager";

    private static NotificationMLManager instance;
    private static final String MODEL_FILE_NAME = "ml_weights.json";
    private static final float DEFAULT_WEIGHT = 10.00f;
    private final Context context;
    private final Map<String, Float> wordWeights;
    private final ExecutorService executor;
    private boolean isDirty = false;
    private volatile boolean isLoaded = false;

    private NotificationMLManager(Context context) {
        this.context = context.getApplicationContext();
        this.wordWeights = new ConcurrentHashMap<>();
        this.executor = Executors.newSingleThreadExecutor();
        loadModel();
    }

    public static synchronized NotificationMLManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationMLManager(context);
        }
        return instance;
    }

    /**
     * 核心处理方法：模型输入文本和反馈类型；自动读取配置的学习率。
     * @param title 输入标题
     * @param text 输入文本
     * @param isPositive 反馈类型 (true: 正向/保留, false: 负向/删除)
     * @return 新的分数
     */
    public float process(String title, String text, boolean isPositive) {
        float learningDegree = SharedPreferencesManager.getInstance(context).getLearningDegree();
        return process(title, text, isPositive, learningDegree);
    }

    /**
     * 核心处理方法：模型输入文本、反馈类型和学习率；模型输出新的分数。
     * @param title 输入标题
     * @param text 输入文本
     * @param isPositive 反馈类型 (true: 正向/保留, false: 负向/删除)
     * @param learningRate 学习率 (0.0 - 10.0)
     * @return 新的分数
     */
    public float process(String title, String text, boolean isPositive, float learningRate) {
        ensureLoaded();
        String combinedText = (title == null ? "" : title) + " " + (text == null ? "" : text);
        if (combinedText.trim().isEmpty()) {
            return DEFAULT_WEIGHT;
        }

        String[] tokens = tokenize(combinedText);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }

        // 正向反馈学习分数为10.0，负向为0.0
        float targetScore = isPositive ? 10.0f : 0.0f;
        
        // 计算当前分数
        float currentScore = calculateScore(tokens);
        float error = targetScore - currentScore;

        // 将 0-10 的学习率映射到 0-1
        // 用户输入的 learningRate 是 0.0-10.0
        float actualLearningRate = Math.max(0f, Math.min(10f, learningRate)) / 10.0f;

        // 同步更新权重
        for (String token : tokens) {
            if (isAscii(token) && token.length() < 2) {
                continue;
            }

            float oldWeight = wordWeights.containsKey(token) ? wordWeights.get(token) : DEFAULT_WEIGHT;
            
            // 更新公式: w = w + lr * error
            float newWeight = oldWeight + actualLearningRate * error;
            
            // 限制权重范围 [0, 10]
            newWeight = Math.max(0, Math.min(10, newWeight));
            
            wordWeights.put(token, newWeight);
        }

        isDirty = true;
        
        // 异步保存模型
        executor.execute(this::saveModel);

        // 返回新的分数
        return calculateScore(tokens);
    }

    /**
     * 预测文本分数 (0-10)
     * @param title 输入标题
     * @param text 输入文本
     * @return 分数
     */
    public float predict(String title, String text) {
        ensureLoaded();
        String combinedText = (title == null ? "" : title) + " " + (text == null ? "" : text);
        if (combinedText.trim().isEmpty()) {
            return DEFAULT_WEIGHT;
        }
        String[] tokens = tokenize(combinedText);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }
        return calculateScore(tokens);
    }

    /**
     * 检查是否需要过滤 - 与在线模型保持一致的接口
     * @param title 输入标题
     * @param text 输入文本
     * @param callback 回调接口，接收(是否过滤, 分数)
     */
    public void checkFilter(String title, String text, OnlineModelManager.FilterCallback callback) {
        executor.execute(() -> {
            try {
                ensureLoaded();
                String combinedText = (title == null ? "" : title) + " " + (text == null ? "" : text);
                float score;
                
                if (combinedText.trim().isEmpty()) {
                    score = DEFAULT_WEIGHT;
                } else {
                    String[] tokens = tokenize(combinedText);
                    if (tokens.length == 0) {
                        score = DEFAULT_WEIGHT;
                    } else {
                        score = calculateScore(tokens);
                    }
                }
                
                // 获取过滤阈值
                float filteringDegree = SharedPreferencesManager.getInstance(context).getFilteringDegree();
                boolean shouldFilter = score <= filteringDegree;
                
                if (callback != null) {
                    callback.onResult(shouldFilter, score);
                }
            } catch (Exception e) {
                // 出错时不过滤，返回高分
                if (callback != null) {
                    callback.onResult(false, 10.0f);
                }
            }
        });
    }

    /**
     * 内部计算分数方法
     */
    private float calculateScore(String[] tokens) {
        float totalWeight = 0;
        int count = 0;

        for (String token : tokens) {
            // 过滤太短的纯英文/数字词
            // Unigram (中文) 长度为1，必须保留。
            if (isAscii(token) && token.length() < 2) {
                continue;
            }
            
            float weight = wordWeights.containsKey(token) ? wordWeights.get(token) : DEFAULT_WEIGHT;
            totalWeight += weight;
            count++;
        }

        if (count == 0) return DEFAULT_WEIGHT;

        float score = totalWeight / count;
        return Math.max(0, Math.min(10.00f, score));
    }

    /**
     * 改进的分词器：
     * 1. 英文/数字：按连续字符切分
     * 2. 中文：Unigram (单字) + Bigram (双字)
     */
    private String[] tokenize(String text) {
        if (text == null) return new String[0];
        
        List<String> tokens = new ArrayList<>();
        StringBuilder buffer = new StringBuilder();
        String lowerText = text.toLowerCase();
        char[] chars = lowerText.toCharArray();

        for (int i = 0; i < chars.length; i++) {
            char c = chars[i];

            if (isChinese(c)) {
                // 如果缓冲区有内容（英文/数字），先作为词加入
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString());
                    buffer.setLength(0);
                }

                // 1. Unigram: 单字
                tokens.add(String.valueOf(c));

                // 2. Bigram: 如果下一个也是汉字，添加双字词
                if (i + 1 < chars.length && isChinese(chars[i + 1])) {
                    tokens.add(String.valueOf(c) + chars[i + 1]);
                }

            } else if (Character.isLetterOrDigit(c)) {
                // 英文或数字，累积到缓冲区
                buffer.append(c);
            } else {
                // 标点符号或其他，截断缓冲区
                if (buffer.length() > 0) {
                    tokens.add(buffer.toString());
                    buffer.setLength(0);
                }
            }
        }

        // 处理结尾
        if (buffer.length() > 0) {
            tokens.add(buffer.toString());
        }

        return tokens.toArray(new String[0]);
    }

    /**
     * 判断是否为汉字 (基本范围)
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    /**
     * 判断是否为纯ASCII字符（用于过滤短英文单词）
     */
    private boolean isAscii(String str) {
        for (char c : str.toCharArray()) {
            if (c > 127) return false;
        }
        return true;
    }

    /**
     * 清空学习模型
     * 清除内存中的权重并删除本地模型文件
     */
    public void clearModel() {
        executor.execute(() -> {
            wordWeights.clear();
            isDirty = false;
            isLoaded = true;
            File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        });
    }

    /**
     * 释放内存
     * 保存模型并清空内存中的权重
     */
    public void releaseMemory() {
        executor.execute(() -> {
            synchronized (this) {
                saveModel();
                wordWeights.clear();
                isLoaded = false;
            }
        });
    }

    private void ensureLoaded() {
        if (!isLoaded) {
            synchronized (this) {
                if (!isLoaded) {
                    loadModel();
                }
            }
        }
    }

    private synchronized void loadModel() {
        if (isLoaded) return;
        File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
        if (!file.exists()) {
            isLoaded = true;
            return;
        }

        try (FileInputStream fis = new FileInputStream(file);
             BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
            
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            
            JSONObject json = new JSONObject(sb.toString());
            Iterator<String> keys = json.keys();
            while (keys.hasNext()) {
                String key = keys.next();
                float value = (float) json.getDouble(key);
                wordWeights.put(key, value);
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
        isLoaded = true;
    }

    private void saveModel() {
        if (!isDirty) return;
        
        try {
            JSONObject json = new JSONObject();
            for (Map.Entry<String, Float> entry : wordWeights.entrySet()) {
                // 只保存偏离默认值较大的权重，以减小文件体积
                if (Math.abs(entry.getValue() - DEFAULT_WEIGHT) > 0.08) {
                    json.put(entry.getKey(), entry.getValue());
                }
            }
            
            File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(json.toString().getBytes());
            }
            
            isDirty = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
