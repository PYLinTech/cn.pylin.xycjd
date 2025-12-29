package cn.pylin.xycjd;

import android.content.Context;
import org.json.JSONObject;
import org.json.JSONArray;
import java.io.File;
import java.io.FileOutputStream;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 本地机器学习文本回归管理器 - v3.0 增强版
 * 
 * 优化目标：
 * 1. 保持外部接口完全不变
 * 2. 引入TF-IDF权重和特征工程
 * 3. 实现决策树集成提升非线性表达
 * 4. 自适应学习率和时间衰减
 * 5. 显著提升准确率和收敛速度
 */
public class NotificationMLManager {
    private static final String TAG = "NotificationMLManager";

    private static NotificationMLManager instance;
    private static final String MODEL_FILE_NAME = "ml_weights_v3.json";
    private static final float DEFAULT_WEIGHT = 10.00f;
    private static final float MIN_WEIGHT = 0.0f;
    private static final float MAX_WEIGHT = 10.0f;
    
    // 学习强度配置
    private static final float AUTO_LEARNING_MULTIPLIER = 1.0f;
    private static final float MANUAL_POSITIVE_MULTIPLIER = 2.5f; // 增强手动反馈
    
    private final Context context;
    private final Map<String, Float> wordWeights;      // 词权重
    private final Map<String, Integer> wordCounts;     // 词频统计
    private final Map<String, Float> tfIdfWeights;     // TF-IDF权重
    private final Map<String, Long> lastAccessTime;    // 最后访问时间
    private final List<DecisionTree> decisionTrees;    // 决策树集成
    private final ExecutorService executor;
    private volatile boolean isLoaded = false;
    private volatile boolean isDirty = false;
    
    // 模型元数据
    private long totalLearnCount = 0;
    private long totalDocumentCount = 0;           // 总文档数（用于TF-IDF）
    private long lastCleanupTime = 0;
    
    // 特征统计
    private final Map<String, Integer> termDocCount;  // 词出现的文档数（用于IDF）

    private NotificationMLManager(Context context) {
        this.context = context.getApplicationContext();
        this.wordWeights = new ConcurrentHashMap<>();
        this.wordCounts = new ConcurrentHashMap<>();
        this.tfIdfWeights = new ConcurrentHashMap<>();
        this.lastAccessTime = new ConcurrentHashMap<>();
        this.termDocCount = new ConcurrentHashMap<>();
        this.decisionTrees = new ArrayList<>();
        this.executor = Executors.newSingleThreadExecutor();
        
        // 初始化决策树
        initializeDecisionTrees();
        this.isLoaded = true;
    }

    public static synchronized NotificationMLManager getInstance(Context context) {
        if (instance == null) {
            instance = new NotificationMLManager(context);
        }
        return instance;
    }

    /**
     * 初始化决策树集成 - v3.1 增强版
     * 增加到5个决策树，覆盖更多维度
     */
    private void initializeDecisionTrees() {
        // 决策树1：基于关键词权重的修正
        decisionTrees.add(new DecisionTree() {
            @Override
            public float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights) {
                // 如果包含高权重负面词，降低分数
                float negativeBoost = 0;
                float positiveBoost = 0;
                
                for (String token : tokens) {
                    Float weight = weights.get(token);
                    if (weight != null) {
                        if (weight < 3.0f) {
                            negativeBoost += (3.0f - weight) * 0.1f;
                        } else if (weight > 7.0f) {
                            positiveBoost += (weight - 7.0f) * 0.1f;
                        }
                    }
                }
                
                return baseScore - negativeBoost + positiveBoost;
            }
        });
        
        // 决策树2：基于词频和一致性的修正
        decisionTrees.add(new DecisionTree() {
            @Override
            public float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights) {
                if (tokens.length == 0) return baseScore;
                
                // 计算权重方差
                float sum = 0, sumSq = 0;
                int count = 0;
                for (String token : tokens) {
                    Float weight = weights.get(token);
                    if (weight != null) {
                        sum += weight;
                        sumSq += weight * weight;
                        count++;
                    }
                }
                
                if (count < 2) return baseScore;
                
                float mean = sum / count;
                float variance = (sumSq / count) - (mean * mean);
                
                // 方差大说明意见不一致，降低置信度
                float confidencePenalty = Math.min(2.0f, variance * 0.1f);
                return baseScore - confidencePenalty;
            }
        });
        
        // 决策树3：基于位置和长度的修正
        decisionTrees.add(new DecisionTree() {
            @Override
            public float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights) {
                // 长文本倾向于中性，短文本更极端
                float lengthFactor = Math.min(1.0f, tokens.length / 10.0f);
                float lengthAdjust = (1.0f - lengthFactor) * 0.5f;
                
                // 如果权重集中在少数词，说明特征明确
                float weightSum = 0;
                float maxWeight = 0;
                for (String token : tokens) {
                    Float weight = weights.get(token);
                    if (weight != null) {
                        weightSum += weight;
                        maxWeight = Math.max(maxWeight, weight);
                    }
                }
                
                float concentrationFactor = (maxWeight > 0) ? (maxWeight / (weightSum + 1)) : 0;
                float concentrationAdjust = (concentrationFactor - 0.3f) * 2.0f;
                
                return baseScore + lengthAdjust + concentrationAdjust;
            }
        });
        
        // 决策树4：基于词长和类型的修正（v3.1新增）
        decisionTrees.add(new DecisionTree() {
            @Override
            public float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights) {
                float longWordBoost = 0;
                float chineseBoost = 0;
                float emojiPenalty = 0;
                
                for (String token : tokens) {
                    if (shouldSkipToken(token)) continue;
                    
                    // 长词加分
                    if (token.length() >= 5) {
                        longWordBoost += 0.1f;
                    }
                    
                    // 中文词加分
                    if (isChinese(token.charAt(0))) {
                        chineseBoost += 0.05f;
                    }
                    
                    // Emoji/符号减分
                    if (token.startsWith("EMOJI_") || token.startsWith("PUNCT_")) {
                        emojiPenalty += 0.08f;
                    }
                }
                
                return baseScore + longWordBoost + chineseBoost - emojiPenalty;
            }
        });
        
        // 决策树5：基于TF-IDF特征的修正（v3.1新增）
        decisionTrees.add(new DecisionTree() {
            @Override
            public float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights) {
                if (tokens.length == 0) return baseScore;
                
                // 计算平均TF-IDF
                float totalTfIdf = 0;
                int count = 0;
                for (String token : tokens) {
                    if (!shouldSkipToken(token)) {
                        float tfIdf = tfIdfWeights.getOrDefault(token, 0.5f);
                        totalTfIdf += tfIdf;
                        count++;
                    }
                }
                
                if (count == 0) return baseScore;
                
                float avgTfIdf = totalTfIdf / count;
                
                // TF-IDF越高，说明特征越突出，分数越极端
                float adjustment = (avgTfIdf - 0.5f) * 2.0f;
                
                return baseScore + adjustment;
            }
        });
    }

    /**
     * 自动学习处理方法 - v3.0 增强版
     */
    public float process(String title, String text, boolean isPositive) {
        ensureLoaded();
        
        String combinedText = combineText(title, text);
        if (combinedText.isEmpty()) {
            return DEFAULT_WEIGHT;
        }

        String[] tokens = tokenize(combinedText);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }

        // 计算位置权重（标题更重要）
        Map<String, Float> positionWeights = calculatePositionWeights(title, text, tokens);
        
        // 获取自适应学习率
        float baseLearningRate = getLearningRate();
        float adaptiveRate = calculateAdaptiveLearningRate(tokens, isPositive);
        float learningRate = baseLearningRate * adaptiveRate * AUTO_LEARNING_MULTIPLIER;
        
        // 计算当前分数和误差
        float currentScore = calculateScore(tokens, positionWeights);
        float targetScore = isPositive ? MAX_WEIGHT : MIN_WEIGHT;
        float error = targetScore - currentScore;

        // 更新权重（带时间衰减）
        updateWeightsV3(tokens, error, learningRate, isPositive, positionWeights);

        // 更新统计和TF-IDF
        updateStatsV3(tokens);
        isDirty = true;
        
        // 定期清理低频词
        scheduleCleanup();
        
        // 异步保存
        executor.execute(this::throttledSaveModel);

        return calculateScore(tokens, positionWeights);
    }

    /**
     * 手动正向反馈 - v3.0 增强版
     */
    public float processPositive(String title, String text) {
        ensureLoaded();
        
        String combinedText = combineText(title, text);
        if (combinedText.isEmpty()) {
            return DEFAULT_WEIGHT;
        }

        String[] tokens = tokenize(combinedText);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }

        // 计算位置权重
        Map<String, Float> positionWeights = calculatePositionWeights(title, text, tokens);
        
        // 手动反馈学习率更强
        float baseLearningRate = getLearningRate();
        float adaptiveRate = calculateAdaptiveLearningRate(tokens, true);
        float learningRate = baseLearningRate * adaptiveRate * MANUAL_POSITIVE_MULTIPLIER;
        
        // 计算当前分数和误差
        float currentScore = calculateScore(tokens, positionWeights);
        float error = MAX_WEIGHT - currentScore;

        // 更新权重（只做正向，更强）
        updateWeightsManualV3(tokens, error, learningRate, positionWeights);

        // 更新统计
        updateStatsV3(tokens);
        isDirty = true;
        
        // 定期清理
        scheduleCleanup();
        
        // 异步保存
        executor.execute(this::throttledSaveModel);

        return calculateScore(tokens, positionWeights);
    }

    /**
     * 预测分数 - v3.0 增强版
     */
    public float predict(String title, String text) {
        ensureLoaded();
        String combinedText = combineText(title, text);
        if (combinedText.isEmpty()) {
            return DEFAULT_WEIGHT;
        }
        String[] tokens = tokenize(combinedText);
        if (tokens.length == 0) {
            return DEFAULT_WEIGHT;
        }
        
        Map<String, Float> positionWeights = calculatePositionWeights(title, text, tokens);
        return calculateScore(tokens, positionWeights);
    }

    /**
     * 检查是否需要过滤 - 保持接口完全兼容
     */
    public void checkFilter(String title, String text, OnlineModelManager.FilterCallback callback) {
        executor.execute(() -> {
            try {
                ensureLoaded();
                float score = predict(title, text);
                
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

    // ==================== v3.0 核心算法实现 ====================

    /**
     * 计算位置权重 - 标题权重更高
     */
    private Map<String, Float> calculatePositionWeights(String title, String text, String[] tokens) {
        Map<String, Float> positionWeights = new HashMap<>();
        
        Set<String> titleTokens = new HashSet<>();
        if (title != null && !title.isEmpty()) {
            String[] titleTokensArray = tokenize(title.toLowerCase());
            Collections.addAll(titleTokens, titleTokensArray);
        }
        
        for (String token : tokens) {
            float weight = titleTokens.contains(token) ? 1.5f : 1.0f;
            positionWeights.put(token, weight);
        }
        
        return positionWeights;
    }

    /**
     * 自适应学习率计算 - v3.1 增强版
     * 增加动态学习率衰减
     */
    private float calculateAdaptiveLearningRate(String[] tokens, boolean isPositive) {
        // 误差越大，学习率越大
        float currentScore = calculateScore(tokens, null);
        float targetScore = isPositive ? MAX_WEIGHT : MIN_WEIGHT;
        float error = Math.abs(targetScore - currentScore);
        
        // 自适应因子：误差大时加速学习
        float errorFactor = Math.min(2.0f, 1.0f + error * 0.2f);
        
        // 动态学习率衰减（v3.1新增）
        // 随着训练次数增加，学习率逐渐减小，防止过拟合
        float dynamicDecay = 1.0f / (1.0f + totalLearnCount * 0.001f);
        
        return errorFactor * dynamicDecay;
    }

    /**
     * v3.1 权重更新 - 带时间衰减、正则化和智能冷启动
     */
    private void updateWeightsV3(String[] tokens, float error, float learningRate, 
                                 boolean isPositive, Map<String, Float> positionWeights) {
        long currentTime = System.currentTimeMillis();
        
        for (String token : tokens) {
            if (shouldSkipToken(token)) continue;

            int count = wordCounts.getOrDefault(token, 0);
            long lastTime = lastAccessTime.getOrDefault(token, currentTime);
            
            // 智能冷启动：为新词提供更好的初始值
            float oldWeight;
            if (count == 0) {
                oldWeight = calculateSmartInitialWeight(token, isPositive);
            } else {
                oldWeight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
            }
            
            // 时间衰减因子（指数衰减）
            float timeDecay = calculateTimeDecay(lastTime, currentTime);
            
            // 稳定性因子（出现次数越多越稳定）
            float stabilityFactor = 1.0f / (1.0f + count * 0.05f);
            
            // 位置权重
            float posWeight = positionWeights != null ? positionWeights.getOrDefault(token, 1.0f) : 1.0f;
            
            // 方向性因子（正向/负向学习的不对称性）
            float directionalFactor = isPositive ? 1.3f : 0.7f;
            
            // L2正则化（防止权重过大）
            float regularization = 0.99f; // 每次更新轻微收缩
            
            // 综合学习率
            float effectiveRate = learningRate * stabilityFactor * directionalFactor * posWeight * timeDecay;
            
            // 权重更新
            float newWeight = oldWeight * regularization + effectiveRate * error;
            
            // 边界保护
            if (isPositive) {
                newWeight = Math.min(MAX_WEIGHT, newWeight);
            } else {
                newWeight = Math.max(MIN_WEIGHT, newWeight);
            }
            
            wordWeights.put(token, newWeight);
            lastAccessTime.put(token, currentTime);
        }
    }

    /**
     * 智能冷启动初始值计算 - v3.1 新增
     * 为新词提供基于统计特性的智能初始值
     */
    private float calculateSmartInitialWeight(String token, boolean isPositive) {
        // 基于词的特性计算初始权重
        float baseWeight = DEFAULT_WEIGHT;
        
        // 1. 长度因素（长词通常更重要）
        if (token.length() >= 5) {
            baseWeight += 1.0f;
        }
        if (token.length() >= 8) {
            baseWeight += 0.5f;
        }
        
        // 2. 类型因素
        if (isChinese(token.charAt(0))) {
            // 中文词：中性偏正
            baseWeight += 0.5f;
        } else if (token.startsWith("EMOJI_")) {
            // Emoji：通常表示情感，偏向两极
            baseWeight = isPositive ? 8.0f : 2.0f;
        } else if (token.startsWith("PUNCT_")) {
            // 特殊符号：中性偏低
            baseWeight -= 1.0f;
        } else if (isAscii(token)) {
            // 英文词：根据长度调整
            if (token.length() >= 4) {
                baseWeight += 0.3f;
            }
        }
        
        // 3. 特殊词模式
        if (token.matches(".*\\d{4,}.*")) {
            // 包含长数字（年份、验证码等）
            baseWeight += 0.5f;
        }
        
        // 4. 学习方向调整
        if (!isPositive) {
            // 负向学习时，新词初始值偏低
            baseWeight = Math.max(MIN_WEIGHT, baseWeight - 1.0f);
        }
        
        // 5. 边界保护
        return Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, baseWeight));
    }

    /**
     * 手动正向权重更新 - v3.0
     */
    private void updateWeightsManualV3(String[] tokens, float error, float learningRate, 
                                       Map<String, Float> positionWeights) {
        long currentTime = System.currentTimeMillis();
        
        for (String token : tokens) {
            if (shouldSkipToken(token)) continue;

            float oldWeight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
            int count = wordCounts.getOrDefault(token, 0);
            long lastTime = lastAccessTime.getOrDefault(token, currentTime);
            
            // 时间衰减
            float timeDecay = calculateTimeDecay(lastTime, currentTime);
            
            // 稳定性因子
            float stabilityFactor = 1.0f / (1.0f + count * 0.03f);
            
            // 位置权重
            float posWeight = positionWeights != null ? positionWeights.getOrDefault(token, 1.0f) : 1.0f;
            
            // 手动正向更强
            float directionalFactor = 2.0f;
            
            // L2正则化
            float regularization = 0.98f;
            
            // 综合学习率
            float effectiveRate = learningRate * stabilityFactor * directionalFactor * posWeight * timeDecay;
            
            // 权重更新（只允许上升）
            float newWeight = oldWeight * regularization + effectiveRate * error;
            newWeight = Math.min(MAX_WEIGHT, newWeight);
            
            wordWeights.put(token, newWeight);
            lastAccessTime.put(token, currentTime);
        }
    }

    /**
     * 时间衰减计算 - 指数衰减
     */
    private float calculateTimeDecay(long lastTime, long currentTime) {
        long elapsedDays = (currentTime - lastTime) / (1000L * 60 * 60 * 24);
        if (elapsedDays <= 1) return 1.0f;
        
        // 半衰期为30天
        float halfLife = 30.0f;
        return (float) Math.pow(0.5, elapsedDays / halfLife);
    }

    /**
     * 更新统计和TF-IDF - v3.0
     */
    private void updateStatsV3(String[] tokens) {
        Set<String> uniqueTokens = new HashSet<>();
        
        for (String token : tokens) {
            if (shouldSkipToken(token)) continue;
            
            // 更新词频
            wordCounts.put(token, wordCounts.getOrDefault(token, 0) + 1);
            
            // 记录本次文档中出现的词（用于TF-IDF）
            uniqueTokens.add(token);
        }
        
        // 更新文档统计（每个文档只计一次）
        for (String token : uniqueTokens) {
            termDocCount.put(token, termDocCount.getOrDefault(token, 0) + 1);
        }
        
        totalDocumentCount++;
        totalLearnCount++;
        
        // 更新TF-IDF权重
        updateTfIdfWeights();
    }

    /**
     * 更新TF-IDF权重
     */
    private void updateTfIdfWeights() {
        if (totalDocumentCount < 10) return; // 文档数太少时不计算TF-IDF
        
        for (String token : wordCounts.keySet()) {
            int tf = wordCounts.getOrDefault(token, 0);
            int df = termDocCount.getOrDefault(token, 1);
            
            // TF: 词频
            float tfValue = (float) Math.log(tf + 1);
            
            // IDF: 逆文档频率
            float idfValue = (float) Math.log((double) totalDocumentCount / df);
            
            // TF-IDF
            float tfIdf = tfValue * idfValue;
            
            // 归一化到0-1范围
            float normalizedTfIdf = Math.min(1.0f, tfIdf / 5.0f);
            
            tfIdfWeights.put(token, normalizedTfIdf);
        }
    }

    /**
     * 计算分数 - v3.1 全面优化版
     * 增加特征重要性加权
     */
    private float calculateScore(String[] tokens, Map<String, Float> positionWeights) {
        if (tokens.length == 0) return DEFAULT_WEIGHT;

        float totalWeightedScore = 0;
        float totalConfidence = 0;
        int validCount = 0;

        for (String token : tokens) {
            if (shouldSkipToken(token)) continue;

            // 基础权重
            float baseWeight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
            
            // TF-IDF权重
            float tfIdf = tfIdfWeights.getOrDefault(token, 0.5f);
            
            // 位置权重
            float posWeight = positionWeights != null ? positionWeights.getOrDefault(token, 1.0f) : 1.0f;
            
            // 特征重要性权重（v3.1新增）
            float importanceWeight = calculateFeatureImportance(token);
            
            // 置信度
            float confidence = calculateTokenConfidence(token);
            
            // 综合权重：基础权重 * (1 + TF-IDF) * 位置权重 * 特征重要性
            float finalWeight = baseWeight * (1.0f + tfIdf * 0.3f) * posWeight * importanceWeight;
            
            totalWeightedScore += finalWeight * confidence;
            totalConfidence += confidence;
            validCount++;
        }

        if (validCount == 0) return DEFAULT_WEIGHT;

        // 基础平均分
        float baseScore = totalWeightedScore / validCount;
        
        // 决策树集成修正
        float adjustedScore = applyDecisionTrees(baseScore, tokens, wordWeights);
        
        // 边界保护
        float finalScore = Math.max(MIN_WEIGHT, Math.min(MAX_WEIGHT, adjustedScore));
        
        return finalScore;
    }

    /**
     * 计算特征重要性 - v3.1 新增
     * 根据词的统计特性分配重要性权重
     */
    private float calculateFeatureImportance(String token) {
        int count = wordCounts.getOrDefault(token, 0);
        float weight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
        
        // 1. 长度重要性（长词通常更重要）
        float lengthFactor = 1.0f;
        if (token.length() >= 5) {
            lengthFactor = 1.2f;
        } else if (token.length() >= 8) {
            lengthFactor = 1.4f;
        }
        
        // 2. 偏离度重要性（偏离默认值越大越重要）
        float deviationFactor = 1.0f + Math.abs(weight - DEFAULT_WEIGHT) / DEFAULT_WEIGHT * 0.3f;
        
        // 3. 稀有度重要性（罕见词可能更重要）
        float rarityFactor = 1.0f;
        if (count > 0 && count < 5) {
            rarityFactor = 1.1f;
        } else if (count == 0) {
            rarityFactor = 1.15f; // 新词
        }
        
        // 4. 类型重要性（中文词通常比符号重要）
        float typeFactor = 1.0f;
        if (isChinese(token.charAt(0))) {
            typeFactor = 1.1f;
        } else if (token.startsWith("EMOJI_") || token.startsWith("PUNCT_")) {
            typeFactor = 0.8f; // 符号重要性较低
        }
        
        // 5. 综合重要性（限制在合理范围）
        float importance = lengthFactor * deviationFactor * rarityFactor * typeFactor;
        return Math.min(1.5f, Math.max(0.7f, importance));
    }

    /**
     * 应用决策树集成
     */
    private float applyDecisionTrees(float baseScore, String[] tokens, Map<String, Float> weights) {
        float adjustedScore = baseScore;
        
        for (DecisionTree tree : decisionTrees) {
            adjustedScore = tree.adjustScore(adjustedScore, tokens, weights);
        }
        
        return adjustedScore;
    }

    /**
     * 计算Token置信度 - v3.0
     */
    private float calculateTokenConfidence(String token) {
        int count = wordCounts.getOrDefault(token, 0);
        
        if (count == 0) return 0.6f; // 新词中等置信度
        
        // 基于词频的置信度
        float freqConfidence = Math.min(1.0f, (float) Math.log(count + 1) / 2.0f);
        
        // 基于权重偏离度的置信度
        float weight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
        float weightDeviation = Math.abs(weight - DEFAULT_WEIGHT) / DEFAULT_WEIGHT;
        float stabilityConfidence = 1.0f - Math.min(0.6f, weightDeviation * 0.5f);
        
        // 基于TF-IDF的置信度（特征越突出越可信）
        float tfIdf = tfIdfWeights.getOrDefault(token, 0.5f);
        float featureConfidence = 0.5f + tfIdf * 0.5f;
        
        // 综合置信度
        return Math.max(0.3f, (freqConfidence * 0.4f + stabilityConfidence * 0.4f + featureConfidence * 0.2f));
    }

    /**
     * 学习率获取 - v3.0
     */
    private float getLearningRate() {
        float learningDegree = SharedPreferencesManager.getInstance(context).getLearningDegree();
        return Math.max(0f, Math.min(10f, learningDegree)) / 10.0f;
    }

    /**
     * Token过滤 - v3.0 增强版
     */
    private boolean shouldSkipToken(String token) {
        if (token == null || token.isEmpty()) return true;
        
        // 纯数字（但保留有意义的数字模式）
        if (token.matches("^\\d+$")) {
            int length = token.length();
            // 保留4位以上数字（可能是年份、验证码等）
            if (length < 4) return true;
        }
        
        // 短ASCII词（但保留常见缩写）
        if (isAscii(token) && token.length() < 3) {
            // 保留常见缩写
            Set<String> keepSet = new HashSet<>(Arrays.asList("ok", "no", "yes", "app", "msg", "tip"));
            if (!keepSet.contains(token)) return true;
        }
        
        return false;
    }

    /**
     * 文本预处理 - 保持不变
     */
    private String combineText(String title, String text) {
        StringBuilder sb = new StringBuilder();
        if (title != null && !title.trim().isEmpty()) {
            sb.append(title.trim());
        }
        if (text != null && !text.trim().isEmpty()) {
            if (sb.length() > 0) sb.append(" ");
            sb.append(text.trim());
        }
        return sb.toString();
    }

    /**
     * 分词算法 - v3.1 全面优化版
     * 增强N-gram、语义特征、特殊符号处理
     */
    private String[] tokenize(String text) {
        if (text == null || text.isEmpty()) {
            return new String[0];
        }

        List<String> tokens = new ArrayList<>();
        StringBuilder englishBuffer = new StringBuilder();
        String lowerText = text.toLowerCase();
        int length = lowerText.length();

        for (int i = 0; i < length; i++) {
            char c = lowerText.charAt(i);

            if (isChinese(c)) {
                // 处理英文缓冲区
                flushEnglishBuffer(englishBuffer, tokens);
                
                // 中文分词：单字 + 双字 + 三字 + 四字 + 五字
                tokens.add(String.valueOf(c));
                
                if (i + 1 < length && isChinese(lowerText.charAt(i + 1))) {
                    String bi = String.valueOf(c) + lowerText.charAt(i + 1);
                    tokens.add(bi);
                    
                    if (i + 2 < length && isChinese(lowerText.charAt(i + 2))) {
                        String tri = bi + lowerText.charAt(i + 2);
                        tokens.add(tri);
                        
                        if (i + 3 < length && isChinese(lowerText.charAt(i + 3))) {
                            String quad = tri + lowerText.charAt(i + 3);
                            tokens.add(quad);
                            
                            if (i + 4 < length && isChinese(lowerText.charAt(i + 4))) {
                                String pent = quad + lowerText.charAt(i + 4);
                                tokens.add(pent);
                            }
                        }
                    }
                }

            } else if (Character.isLetterOrDigit(c)) {
                // 英文/数字累积
                englishBuffer.append(c);
            } else {
                // 分隔符，处理英文缓冲区
                flushEnglishBuffer(englishBuffer, tokens);
                
                // 语义特征：特殊符号和emoji
                addSemanticFeatures(c, tokens);
            }
        }

        // 处理结尾
        flushEnglishBuffer(englishBuffer, tokens);

        return tokens.toArray(new String[0]);
    }

    /**
     * 添加语义特征 - v3.1 新增
     * 处理emoji、特殊符号、标点模式
     */
    private void addSemanticFeatures(char c, List<String> tokens) {
        // Emoji范围检测
        if ((c >= 0x2600 && c <= 0x26FF) || // 杂项符号
            (c >= 0x2700 && c <= 0x27BF) || // 装饰符号
            (c >= 0x1F300 && c <= 0x1F9FF) || // 新增emoji
            (c >= 0xFE00 && c <= 0xFE0F)) {   // 变体选择器
            tokens.add("EMOJI_" + c);
            return;
        }
        
        // 特殊标点模式
        if (c == '!' || c == '?' || c == '！' || c == '？') {
            // 检查连续重复次数
            int count = 1;
            // 这里简化处理，实际可以统计连续重复
            tokens.add("PUNCT_EXCLAIM");
        } else if (c == '.' || c == '。') {
            tokens.add("PUNCT_DOT");
        } else if (c == '#' || c == '＃') {
            tokens.add("PUNCT_HASH");
        } else if (c == '$' || c == '￥') {
            tokens.add("PUNCT_MONEY");
        }
    }

    /**
     * 英文分词优化 - v3.0
     */
    private void flushEnglishBuffer(StringBuilder buffer, List<String> tokens) {
        if (buffer.length() == 0) return;
        
        String english = buffer.toString();
        
        // 短词直接加入
        if (english.length() <= 3) {
            tokens.add(english);
        } else {
            // 长词：整词 + 前缀(4) + 后缀(4) + 词根(4)
            tokens.add(english);
            tokens.add(english.substring(0, Math.min(4, english.length())));
            if (english.length() >= 4) {
                tokens.add(english.substring(english.length() - Math.min(4, english.length())));
            }
            if (english.length() >= 5) {
                // 词根：中间部分
                int midStart = english.length() / 2 - 2;
                if (midStart > 0 && midStart + 4 <= english.length()) {
                    tokens.add(english.substring(midStart, midStart + 4));
                }
            }
        }
        
        buffer.setLength(0);
    }

    /**
     * 工具方法
     */
    private boolean isChinese(char c) {
        return c >= 0x4E00 && c <= 0x9FA5;
    }

    private boolean isAscii(String str) {
        for (char c : str.toCharArray()) {
            if (c > 127) return false;
        }
        return true;
    }

    // ==================== 持久化管理 ====================

    /**
     * 确保模型加载
     */
    private void ensureLoaded() {
        if (!isLoaded) {
            synchronized (this) {
                if (!isLoaded) {
                    loadModel();
                    isLoaded = true;
                }
            }
        }
    }

    /**
     * 节流保存
     */
    private long lastSaveTime = 0;
    private void throttledSaveModel() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastSaveTime > 5000) {
            lastSaveTime = currentTime;
            saveModel();
        }
    }

    /**
     * 定期清理低频词
     */
    private void scheduleCleanup() {
        long currentTime = System.currentTimeMillis();
        if (currentTime - lastCleanupTime > 60000) { // 每分钟检查一次
            lastCleanupTime = currentTime;
            
            // 如果数据量过大，清理低频低权重词
            if (wordCounts.size() > 5000) {
                executor.execute(this::cleanupLowValueTerms);
            }
        }
    }

    /**
     * 清理低价值词条
     */
    private void cleanupLowValueTerms() {
        Iterator<Map.Entry<String, Integer>> iterator = wordCounts.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, Integer> entry = iterator.next();
            String token = entry.getKey();
            int count = entry.getValue();
            float weight = wordWeights.getOrDefault(token, DEFAULT_WEIGHT);
            
            // 清理条件：低频且权重接近默认值
            if (count < 3 && Math.abs(weight - DEFAULT_WEIGHT) < 0.5f) {
                iterator.remove();
                wordWeights.remove(token);
                tfIdfWeights.remove(token);
                lastAccessTime.remove(token);
                termDocCount.remove(token);
            }
        }
    }

    /**
     * 保存模型 - v3.0 格式
     */
    private synchronized void saveModel() {
        if (!isDirty) return;
        
        try {
            JSONObject json = new JSONObject();
            json.put("totalLearnCount", totalLearnCount);
            json.put("totalDocumentCount", totalDocumentCount);
            json.put("saveTime", System.currentTimeMillis());
            
            // 保存权重（只保存偏离默认值较多的）
            JSONObject weightsJson = new JSONObject();
            for (Map.Entry<String, Float> entry : wordWeights.entrySet()) {
                float weight = entry.getValue();
                if (Math.abs(weight - DEFAULT_WEIGHT) > 0.05f) {
                    weightsJson.put(entry.getKey(), weight);
                }
            }
            json.put("weights", weightsJson);
            
            // 保存词频
            JSONObject countsJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : wordCounts.entrySet()) {
                if (entry.getValue() > 0) {
                    countsJson.put(entry.getKey(), entry.getValue());
                }
            }
            json.put("counts", countsJson);
            
            // 保存TF-IDF权重
            JSONObject tfIdfJson = new JSONObject();
            for (Map.Entry<String, Float> entry : tfIdfWeights.entrySet()) {
                if (entry.getValue() > 0.1f) {
                    tfIdfJson.put(entry.getKey(), entry.getValue());
                }
            }
            json.put("tfidf", tfIdfJson);
            
            // 保存时间戳
            JSONObject timeJson = new JSONObject();
            for (Map.Entry<String, Long> entry : lastAccessTime.entrySet()) {
                timeJson.put(entry.getKey(), entry.getValue());
            }
            json.put("accessTime", timeJson);
            
            // 保存文档统计
            JSONObject docJson = new JSONObject();
            for (Map.Entry<String, Integer> entry : termDocCount.entrySet()) {
                if (entry.getValue() > 1) {
                    docJson.put(entry.getKey(), entry.getValue());
                }
            }
            json.put("termDocCount", docJson);
            
            // 写入文件
            File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
            try (FileOutputStream fos = new FileOutputStream(file)) {
                fos.write(json.toString().getBytes());
            }
            
            isDirty = false;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 加载模型 - v3.0 格式
     * 不校验版本号，直接加载
     */
    private void loadModel() {
        try {
            File file = new File(context.getFilesDir(), MODEL_FILE_NAME);
            if (!file.exists()) return;
            
            Scanner scanner = new Scanner(file);
            StringBuilder sb = new StringBuilder();
            while (scanner.hasNextLine()) {
                sb.append(scanner.nextLine());
            }
            scanner.close();
            
            JSONObject json = new JSONObject(sb.toString());
            
            // 直接加载数据，不校验版本
            totalLearnCount = json.optLong("totalLearnCount", 0);
            totalDocumentCount = json.optLong("totalDocumentCount", 0);
            
            // 加载权重
            if (json.has("weights")) {
                JSONObject weightsJson = json.getJSONObject("weights");
                Iterator<String> keys = weightsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    wordWeights.put(key, (float) weightsJson.getDouble(key));
                }
            }
            
            // 加载词频
            if (json.has("counts")) {
                JSONObject countsJson = json.getJSONObject("counts");
                Iterator<String> keys = countsJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    wordCounts.put(key, countsJson.getInt(key));
                }
            }
            
            // 加载TF-IDF
            if (json.has("tfidf")) {
                JSONObject tfIdfJson = json.getJSONObject("tfidf");
                Iterator<String> keys = tfIdfJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    tfIdfWeights.put(key, (float) tfIdfJson.getDouble(key));
                }
            }
            
            // 加载时间戳
            if (json.has("accessTime")) {
                JSONObject timeJson = json.getJSONObject("accessTime");
                Iterator<String> keys = timeJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    lastAccessTime.put(key, timeJson.getLong(key));
                }
            }
            
            // 加载文档统计
            if (json.has("termDocCount")) {
                JSONObject docJson = json.getJSONObject("termDocCount");
                Iterator<String> keys = docJson.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    termDocCount.put(key, docJson.getInt(key));
                }
            }
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 清空模型 - 保持接口不变
     */
    public void clearModel() {
        executor.execute(() -> {
            wordWeights.clear();
            wordCounts.clear();
            tfIdfWeights.clear();
            lastAccessTime.clear();
            termDocCount.clear();
            totalLearnCount = 0;
            totalDocumentCount = 0;
            isDirty = false;
            isLoaded = true;
            
            // 删除所有版本的模型文件
            File[] files = context.getFilesDir().listFiles();
            if (files != null) {
                for (File file : files) {
                    if (file.getName().startsWith("ml_weights")) {
                        file.delete();
                    }
                }
            }
        });
    }

    /**
     * 释放内存 - 保持接口不变
     */
    public void releaseMemory() {
        executor.execute(() -> {
            synchronized (this) {
                saveModel();
                wordWeights.clear();
                wordCounts.clear();
                tfIdfWeights.clear();
                lastAccessTime.clear();
                termDocCount.clear();
                isLoaded = false;
            }
        });
    }


    /**
     * 决策树接口
     */
    private interface DecisionTree {
        float adjustScore(float baseScore, String[] tokens, Map<String, Float> weights);
    }
}
