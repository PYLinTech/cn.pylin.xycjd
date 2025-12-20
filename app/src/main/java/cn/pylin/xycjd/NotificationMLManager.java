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

    private static NotificationMLManager instance;
    private static final String MODEL_FILE_NAME = "ml_weights.json";
    private static final float DEFAULT_WEIGHT = 8.0f;
    // 学习率
    private static final float LEARNING_RATE = 0.12f;
    private final Context context;
    private final Map<String, Float> wordWeights;
    private final ExecutorService executor;
    private boolean isDirty = false;

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
     * 预测文本分数 (0-10)
     * @param text 输入文本
     * @return 分数
     */
    public float predict(String text) {
        if (text == null || text.trim().isEmpty()) {
            return DEFAULT_WEIGHT;
        }

        String[] tokens = tokenize(text);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }

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
     * 训练模型
     * @param text 输入文本
     * @param targetScore 目标分数 (0 或 10)
     */
    public void train(String text, float targetScore) {
        if (text == null || text.trim().isEmpty()) return;

        executor.execute(() -> {
            String[] tokens = tokenize(text);
            if (tokens.length == 0) return;

            // 1. 计算当前预测值
            float currentScore = predict(text);
            
            // 2. 计算误差
            float error = targetScore - currentScore;
            
            // 3. 更新权重
            // 使用简单的梯度下降更新规则: w = w + lr * error
            for (String token : tokens) {
                if (isAscii(token) && token.length() < 2) {
                    continue;
                }

                float oldWeight = wordWeights.containsKey(token) ? wordWeights.get(token) : DEFAULT_WEIGHT;
                
                // 更新公式
                float newWeight = oldWeight + LEARNING_RATE * error;
                
                // 限制权重范围 [0, 10]
                newWeight = Math.max(0, Math.min(10, newWeight));
                
                wordWeights.put(token, newWeight);
            }
            
            isDirty = true;
            saveModel();
        });
    }

    /**
     * 根据用户操作更新模型
     * @param text 文本
     * @param isClick 是否是点击操作（false为删除）
     */
    public void updateModel(String text, boolean isClick) {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
        float learningDegree = prefs.getFloat("pref_learning_degree", 3.0f);
        float filteringDegree = prefs.getFloat("pref_filtering_degree", 5.0f);

        float targetScore;
        if (isClick) {
            // 点击操作：目标分数 = 过滤程度 + 学习度 / 2.0f，确保不高于10分
            targetScore = Math.min(10.0f, filteringDegree + learningDegree / 2.0f);
        } else {
            // 删除操作：目标分数 = 过滤程度 - 学习度 / 2.0f，确保不低于0分
            targetScore = Math.max(0.0f, filteringDegree - learningDegree / 2.0f);
        }
        train(text, targetScore);
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
            
            File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
            if (file.exists()) {
                file.delete();
            }
        });
    }

    private void loadModel() {
        File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
        if (!file.exists()) return;

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
