package cn.pylin.xycjd.manager;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

/**
 * SharedPreferences管理类
 * 统一管理应用中所有的SharedPreferences读写操作
 * 
 * 全局设置：应用启动时一次性读取，保存在内存中
 * 应用包特定设置：按需读取，实时同步
 */
public class SharedPreferencesManager {
    
    private static SharedPreferencesManager instance;
    
    // 全局SharedPreferences
    private SharedPreferences globalPrefs;
    private SharedPreferences.Editor globalEditor;
    
    // 应用包特定SharedPreferences
    private SharedPreferences appEnabledPrefs;      // 应用启用状态
    private SharedPreferences appModelFilterPrefs;  // 模型过滤设置
    private SharedPreferences appAutoExpandPrefs;   // 自动展开设置
    private SharedPreferences appVibrationPrefs;    // 通知震动设置
    private SharedPreferences appSoundPrefs;        // 通知声音设置
    
    // 全局设置缓存（启动时一次性读取）
    private String language;
    private int theme;
    private String notificationMode;
    private boolean modelFilteringEnabled;
    private String filterModel;
    private float filteringDegree;
    private float learningDegree;
    private float onlineFilteringDegree;
    private float temperature;
    private String onlineApiUrl;
    private String onlineApiKey;
    private String onlineModelName;
    private String onlineModelPrompt;
    private float animationSpeed;
    private int floatingSize;
    private int floatingX;
    private int floatingY;
    private int introVersion;
    
    // 常量定义
    private static final String PREF_LANGUAGE = "language";
    private static final String PREF_THEME = "theme";
    private static final String PREF_NOTIFICATION_MODE = "pref_notification_mode";
    private static final String PREF_MODEL_FILTERING_ENABLED = "pref_model_filtering_enabled";
    private static final String PREF_FILTER_MODEL = "pref_filter_model";
    private static final String PREF_FILTERING_DEGREE = "pref_filtering_degree";
    private static final String PREF_LEARNING_DEGREE = "pref_learning_degree";
    private static final String PREF_ONLINE_FILTERING_DEGREE = "pref_online_filtering_degree";
    private static final String PREF_TEMPERATURE = "pref_temperature";
    private static final String PREF_ONLINE_API_URL = "pref_online_api_url";
    private static final String PREF_ONLINE_API_KEY = "pref_online_api_key";
    private static final String PREF_ONLINE_MODEL_NAME = "pref_online_model_name";
    private static final String PREF_ONLINE_MODEL_PROMPT = "pref_online_model_prompt";
    private static final String PREF_ANIMATION_SPEED = "pref_animation_speed";
    private static final String PREF_FLOATING_SIZE = "floating_size";
    private static final String PREF_FLOATING_X = "floating_x";
    private static final String PREF_FLOATING_Y = "floating_y";
    private static final String PREF_INTRO_VERSION = "intro_version";
    
    // 应用包特定SharedPreferences名称
    private static final String PREF_APP_ENABLED = "app_checkboxes";
    private static final String PREF_APP_MODEL_FILTER = "app_model_filter";
    private static final String PREF_APP_AUTO_EXPAND = "app_auto_expand";
    private static final String PREF_APP_VIBRATION = "app_notification_vibration";
    private static final String PREF_APP_SOUND = "app_notification_sound";
    
    // 通知日志记录状态
    private static final String PREF_NOTIFICATION_LOG_RECORDING = "notification_log_recording";
    private boolean notificationLogRecording;
    
    // 悬浮窗开关状态
    private static final String PREF_FLOATING_WINDOW_ENABLED = "floating_window_enabled";
    private boolean floatingWindowEnabled;
    
    // 声音与震动全局开关
    private static final String PREF_SOUND_ENABLED = "sound_enabled";
    private boolean soundEnabled;
    
    private static final String PREF_VIBRATION_ENABLED = "vibration_enabled";
    private boolean vibrationEnabled;
    
    // 震动强度
    private static final String PREF_VIBRATION_INTENSITY = "vibration_intensity";
    private int vibrationIntensity;
    
    private static final String PREF_ONLINE_MODEL_PROCESS_MODE = "online_model_process_mode";
    private String onlineModelProcessMode;
    public static final String PROCESS_MODE_SHOW_FIRST = "show_first"; // 先显示再检查
    public static final String PROCESS_MODE_CHECK_FIRST = "check_first"; // 先检查再显示
    
    // 悬浮窗圆角百分比存储值（0-100）
    private static final String PREF_FLOATING_CORNER_RADIUS_1 = "floating_corner_radius_1";
    private static final String PREF_FLOATING_CORNER_RADIUS_2 = "floating_corner_radius_2";
    private static final String PREF_FLOATING_CORNER_RADIUS_3 = "floating_corner_radius_3";
    private int floatingCornerRadius1;
    private int floatingCornerRadius2;
    private int floatingCornerRadius3;
    
    // 超大岛列表相对距离存储值（0-200dp）
    private static final String PREF_ISLAND_LIST_DISTANCE = "island_list_distance";
    private int islandListDistance;
    
    // 超大岛列表水平相对距离存储值（0-400，映射为-200到200dp）
    private static final String PREF_ISLAND_LIST_HORIZONTAL_DISTANCE = "island_list_horizontal_distance";
    private int islandListHorizontalDistance;
    
    // 透明度存储值（0-100%）
    private static final String PREF_OPACITY = "pref_opacity";
    private static final String PREF_MEDIUM_OPACITY = "pref_medium_opacity";
    private static final String PREF_LARGE_OPACITY = "pref_large_opacity";
    private int opacity;
    private int mediumOpacity;
    private int largeOpacity;
    
    private SharedPreferencesManager(Context context) {
        // 初始化全局SharedPreferences
        globalPrefs = PreferenceManager.getDefaultSharedPreferences(context);
        globalEditor = globalPrefs.edit();
        
        // 初始化应用包特定SharedPreferences
        appEnabledPrefs = context.getSharedPreferences(PREF_APP_ENABLED, Context.MODE_PRIVATE);
        appModelFilterPrefs = context.getSharedPreferences(PREF_APP_MODEL_FILTER, Context.MODE_PRIVATE);
        appAutoExpandPrefs = context.getSharedPreferences(PREF_APP_AUTO_EXPAND, Context.MODE_PRIVATE);
        appVibrationPrefs = context.getSharedPreferences(PREF_APP_VIBRATION, Context.MODE_PRIVATE);
        appSoundPrefs = context.getSharedPreferences(PREF_APP_SOUND, Context.MODE_PRIVATE);
        
        // 一次性读取所有全局设置到内存
        loadGlobalSettings();
    }
    
    /**
     * 获取单例实例
     */
    public static synchronized SharedPreferencesManager getInstance(Context context) {
        if (instance == null) {
            instance = new SharedPreferencesManager(context.getApplicationContext());
        }
        return instance;
    }
    
    /**
     * 一次性加载所有全局设置到内存
     */
    private void loadGlobalSettings() {
        language = globalPrefs.getString(PREF_LANGUAGE, "zh");
        theme = globalPrefs.getInt(PREF_THEME, 0); // 0 = UI_MODE_NIGHT_FOLLOW_SYSTEM
        notificationMode = globalPrefs.getString(PREF_NOTIFICATION_MODE, "mode_super_island_only");
        modelFilteringEnabled = globalPrefs.getBoolean(PREF_MODEL_FILTERING_ENABLED, false);
        filterModel = globalPrefs.getString(PREF_FILTER_MODEL, "model_local");
        filteringDegree = globalPrefs.getFloat(PREF_FILTERING_DEGREE, 5.0f);
        learningDegree = globalPrefs.getFloat(PREF_LEARNING_DEGREE, 3.0f);
        onlineFilteringDegree = globalPrefs.getFloat(PREF_ONLINE_FILTERING_DEGREE, 5.0f);
        temperature = globalPrefs.getFloat(PREF_TEMPERATURE, 0.5f);
        onlineApiUrl = globalPrefs.getString(PREF_ONLINE_API_URL, "");
        onlineApiKey = globalPrefs.getString(PREF_ONLINE_API_KEY, "");
        onlineModelName = globalPrefs.getString(PREF_ONLINE_MODEL_NAME, "");
        onlineModelPrompt = globalPrefs.getString(PREF_ONLINE_MODEL_PROMPT, "");
        animationSpeed = globalPrefs.getFloat(PREF_ANIMATION_SPEED, 1.0f);
        floatingSize = globalPrefs.getInt(PREF_FLOATING_SIZE, 100);
        floatingX = globalPrefs.getInt(PREF_FLOATING_X, 0);
        floatingY = globalPrefs.getInt(PREF_FLOATING_Y, -100);
        introVersion = globalPrefs.getInt(PREF_INTRO_VERSION, 0);
        notificationLogRecording = globalPrefs.getBoolean(PREF_NOTIFICATION_LOG_RECORDING, false);
        floatingWindowEnabled = globalPrefs.getBoolean(PREF_FLOATING_WINDOW_ENABLED, false);
        onlineModelProcessMode = globalPrefs.getString(PREF_ONLINE_MODEL_PROCESS_MODE, PROCESS_MODE_SHOW_FIRST);
        
        // 加载声音与震动设置（默认开启）
        soundEnabled = globalPrefs.getBoolean(PREF_SOUND_ENABLED, true);
        vibrationEnabled = globalPrefs.getBoolean(PREF_VIBRATION_ENABLED, true);
        vibrationIntensity = globalPrefs.getInt(PREF_VIBRATION_INTENSITY, 160);
        
        // 加载悬浮窗圆角百分比（默认值：第一个100%，第二个50%，第三个50%）
        floatingCornerRadius1 = globalPrefs.getInt(PREF_FLOATING_CORNER_RADIUS_1, 100);
        floatingCornerRadius2 = globalPrefs.getInt(PREF_FLOATING_CORNER_RADIUS_2, 100);
        floatingCornerRadius3 = globalPrefs.getInt(PREF_FLOATING_CORNER_RADIUS_3, 45);
        
        // 加载超大岛列表相对距离（默认值：0dp）
        islandListDistance = globalPrefs.getInt(PREF_ISLAND_LIST_DISTANCE, 0);
        
        // 加载超大岛列表水平相对距离（默认值：200，映射为0dp居中）
        islandListHorizontalDistance = globalPrefs.getInt(PREF_ISLAND_LIST_HORIZONTAL_DISTANCE, 200);
        
        // 加载透明度（默认值：0% - 不透明，100% - 完全透明，但默认值为0表示不透明，符合用户要求的默认0）
        opacity = globalPrefs.getInt(PREF_OPACITY, 0);
        mediumOpacity = globalPrefs.getInt(PREF_MEDIUM_OPACITY, 0);
        largeOpacity = globalPrefs.getInt(PREF_LARGE_OPACITY, 0);
    }
    
    // ==================== 全局设置读写方法 ====================
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
        globalEditor.putString(PREF_LANGUAGE, language).apply();
    }
    
    public int getTheme() {
        return theme;
    }
    
    public void setTheme(int theme) {
        this.theme = theme;
        globalEditor.putInt(PREF_THEME, theme).apply();
    }
    
    public String getNotificationMode() {
        return notificationMode;
    }
    
    public void setNotificationMode(String notificationMode) {
        this.notificationMode = notificationMode;
        globalEditor.putString(PREF_NOTIFICATION_MODE, notificationMode).apply();
    }
    
    public boolean isModelFilteringEnabled() {
        return modelFilteringEnabled;
    }
    
    public void setModelFilteringEnabled(boolean enabled) {
        this.modelFilteringEnabled = enabled;
        globalEditor.putBoolean(PREF_MODEL_FILTERING_ENABLED, enabled).apply();
    }
    
    public String getFilterModel() {
        return filterModel;
    }
    
    public void setFilterModel(String filterModel) {
        this.filterModel = filterModel;
        globalEditor.putString(PREF_FILTER_MODEL, filterModel).apply();
    }
    
    public float getFilteringDegree() {
        return filteringDegree;
    }
    
    public void setFilteringDegree(float filteringDegree) {
        this.filteringDegree = filteringDegree;
        globalEditor.putFloat(PREF_FILTERING_DEGREE, filteringDegree).apply();
    }
    
    public float getLearningDegree() {
        return learningDegree;
    }
    
    public void setLearningDegree(float learningDegree) {
        this.learningDegree = learningDegree;
        globalEditor.putFloat(PREF_LEARNING_DEGREE, learningDegree).apply();
    }
    
    public float getOnlineFilteringDegree() {
        return onlineFilteringDegree;
    }
    
    public void setOnlineFilteringDegree(float onlineFilteringDegree) {
        this.onlineFilteringDegree = onlineFilteringDegree;
        globalEditor.putFloat(PREF_ONLINE_FILTERING_DEGREE, onlineFilteringDegree).apply();
    }
    
    public float getTemperature() {
        return temperature;
    }
    
    public void setTemperature(float temperature) {
        this.temperature = temperature;
        globalEditor.putFloat(PREF_TEMPERATURE, temperature).apply();
    }
    
    public String getOnlineApiUrl() {
        return onlineApiUrl;
    }
    
    public void setOnlineApiUrl(String onlineApiUrl) {
        this.onlineApiUrl = onlineApiUrl;
        globalEditor.putString(PREF_ONLINE_API_URL, onlineApiUrl).apply();
    }
    
    public String getOnlineApiKey() {
        return onlineApiKey;
    }
    
    public void setOnlineApiKey(String onlineApiKey) {
        this.onlineApiKey = onlineApiKey;
        globalEditor.putString(PREF_ONLINE_API_KEY, onlineApiKey).apply();
    }
    
    public String getOnlineModelName() {
        return onlineModelName;
    }
    
    public void setOnlineModelName(String onlineModelName) {
        this.onlineModelName = onlineModelName;
        globalEditor.putString(PREF_ONLINE_MODEL_NAME, onlineModelName).apply();
    }
    
    public String getOnlineModelPrompt() {
        return onlineModelPrompt;
    }
    
    public void setOnlineModelPrompt(String onlineModelPrompt) {
        this.onlineModelPrompt = onlineModelPrompt;
        globalEditor.putString(PREF_ONLINE_MODEL_PROMPT, onlineModelPrompt).apply();
    }
    
    public float getAnimationSpeed() {
        return animationSpeed;
    }
    
    public void setAnimationSpeed(float animationSpeed) {
        this.animationSpeed = animationSpeed;
        globalEditor.putFloat(PREF_ANIMATION_SPEED, animationSpeed).apply();
    }
    
    public int getFloatingSize() {
        return floatingSize;
    }
    
    public void setFloatingSize(int floatingSize) {
        this.floatingSize = floatingSize;
        globalEditor.putInt(PREF_FLOATING_SIZE, floatingSize).apply();
    }
    
    public int getFloatingX() {
        return floatingX;
    }
    
    public void setFloatingX(int floatingX) {
        this.floatingX = floatingX;
        globalEditor.putInt(PREF_FLOATING_X, floatingX).apply();
    }
    
    public int getFloatingY() {
        return floatingY;
    }
    
    public void setFloatingY(int floatingY) {
        this.floatingY = floatingY;
        globalEditor.putInt(PREF_FLOATING_Y, floatingY).apply();
    }
    
    public int getIntroVersion() {
        return introVersion;
    }
    
    public void setIntroVersion(int introVersion) {
        this.introVersion = introVersion;
        globalEditor.putInt(PREF_INTRO_VERSION, introVersion).apply();
    }
    
    // ==================== 通知日志记录状态方法 ====================
    
    public boolean isNotificationLogRecording() {
        return notificationLogRecording;
    }
    
    public void setNotificationLogRecording(boolean recording) {
        this.notificationLogRecording = recording;
        globalEditor.putBoolean(PREF_NOTIFICATION_LOG_RECORDING, recording).apply();
    }
    
    // ==================== 悬浮窗开关状态方法 ====================
    
    public boolean isFloatingWindowEnabled() {
        return floatingWindowEnabled;
    }
    
    public void setFloatingWindowEnabled(boolean enabled) {
        this.floatingWindowEnabled = enabled;
        globalEditor.putBoolean(PREF_FLOATING_WINDOW_ENABLED, enabled).apply();
    }
    
    // ==================== 声音与震动全局开关方法 ====================
    
    public boolean isSoundEnabled() {
        return soundEnabled;
    }
    
    public void setSoundEnabled(boolean enabled) {
        this.soundEnabled = enabled;
        globalEditor.putBoolean(PREF_SOUND_ENABLED, enabled).apply();
    }
    
    public boolean isVibrationEnabled() {
        return vibrationEnabled;
    }
    
    public void setVibrationEnabled(boolean enabled) {
        this.vibrationEnabled = enabled;
        globalEditor.putBoolean(PREF_VIBRATION_ENABLED, enabled).apply();
    }
    
    public int getVibrationIntensity() {
        return vibrationIntensity;
    }
    
    public void setVibrationIntensity(int intensity) {
        this.vibrationIntensity = intensity;
        globalEditor.putInt(PREF_VIBRATION_INTENSITY, intensity).apply();
    }
    
    // ==================== 在线模型处理流程方法 ====================
    
    public String getOnlineModelProcessMode() {
        return onlineModelProcessMode != null ? onlineModelProcessMode : PROCESS_MODE_SHOW_FIRST;
    }
    
    public void setOnlineModelProcessMode(String processMode) {
        this.onlineModelProcessMode = processMode;
        globalEditor.putString(PREF_ONLINE_MODEL_PROCESS_MODE, processMode).apply();
    }
    
    public boolean isShowFirstMode() {
        return PROCESS_MODE_SHOW_FIRST.equals(getOnlineModelProcessMode());
    }
    
    public boolean isCheckFirstMode() {
        return PROCESS_MODE_CHECK_FIRST.equals(getOnlineModelProcessMode());
    }
    
    // ==================== 应用包特定设置读写方法 ====================
    
    /**
     * 获取应用启用状态
     */
    public boolean isAppEnabled(String packageName) {
        return appEnabledPrefs.getBoolean(packageName, false);
    }
    
    /**
     * 设置应用启用状态
     */
    public void setAppEnabled(String packageName, boolean enabled) {
        appEnabledPrefs.edit().putBoolean(packageName, enabled).apply();
    }
    
    /**
     * 获取应用模型过滤状态
     */
    public boolean isAppModelFilterEnabled(String packageName) {
        return appModelFilterPrefs.getBoolean(packageName, false);
    }
    
    /**
     * 设置应用模型过滤状态
     */
    public void setAppModelFilterEnabled(String packageName, boolean enabled) {
        appModelFilterPrefs.edit().putBoolean(packageName, enabled).apply();
    }
    
    /**
     * 获取应用自动展开状态
     */
    public boolean isAppAutoExpandEnabled(String packageName) {
        return appAutoExpandPrefs.getBoolean(packageName, false);
    }
    
    /**
     * 设置应用自动展开状态
     */
    public void setAppAutoExpandEnabled(String packageName, boolean enabled) {
        appAutoExpandPrefs.edit().putBoolean(packageName, enabled).apply();
    }
    
    /**
     * 获取应用通知震动状态
     */
    public boolean isAppNotificationVibrationEnabled(String packageName) {
        return appVibrationPrefs.getBoolean(packageName, false);
    }
    
    /**
     * 设置应用通知震动状态
     */
    public void setAppNotificationVibrationEnabled(String packageName, boolean enabled) {
        appVibrationPrefs.edit().putBoolean(packageName, enabled).apply();
    }
    
    /**
     * 获取应用通知声音状态
     */
    public boolean isAppNotificationSoundEnabled(String packageName) {
        return appSoundPrefs.getBoolean(packageName, false);
    }
    
    /**
     * 设置应用通知声音状态
     */
    public void setAppNotificationSoundEnabled(String packageName, boolean enabled) {
        appSoundPrefs.edit().putBoolean(packageName, enabled).apply();
    }
    
    /**
     * 批量更新应用设置
     */
    public void batchUpdateAppSettings(String packageName, Boolean enabled, Boolean modelFilter, 
                                      Boolean autoExpand, Boolean vibration, Boolean sound) {
        if (enabled != null) {
            setAppEnabled(packageName, enabled);
        }
        if (modelFilter != null) {
            setAppModelFilterEnabled(packageName, modelFilter);
        }
        if (autoExpand != null) {
            setAppAutoExpandEnabled(packageName, autoExpand);
        }
        if (vibration != null) {
            setAppNotificationVibrationEnabled(packageName, vibration);
        }
        if (sound != null) {
            setAppNotificationSoundEnabled(packageName, sound);
        }
    }
    
    /**
     * 批量更新多个应用的同一设置
     */
    public void batchUpdateMultipleApps(java.util.List<String> packageNames, int settingType, boolean value) {
        SharedPreferences prefs;
        switch (settingType) {
            case 0: // 启用状态
                prefs = appEnabledPrefs;
                break;
            case 1: // 模型过滤
                prefs = appModelFilterPrefs;
                break;
            case 2: // 自动展开
                prefs = appAutoExpandPrefs;
                break;
            case 3: // 通知震动
                prefs = appVibrationPrefs;
                break;
            case 4: // 通知声音
                prefs = appSoundPrefs;
                break;
            default:
                return;
        }
        
        SharedPreferences.Editor editor = prefs.edit();
        for (String packageName : packageNames) {
            editor.putBoolean(packageName, value);
        }
        editor.apply();
    }
    
    // ==================== 悬浮窗圆角百分比方法 ====================
    
    public int getFloatingCornerRadius1() {
        return floatingCornerRadius1;
    }
    
    public void setFloatingCornerRadius1(int radius) {
        this.floatingCornerRadius1 = radius;
        globalEditor.putInt(PREF_FLOATING_CORNER_RADIUS_1, radius).apply();
    }
    
    public int getFloatingCornerRadius2() {
        return floatingCornerRadius2;
    }
    
    public void setFloatingCornerRadius2(int radius) {
        this.floatingCornerRadius2 = radius;
        globalEditor.putInt(PREF_FLOATING_CORNER_RADIUS_2, radius).apply();
    }
    
    public int getFloatingCornerRadius3() {
        return floatingCornerRadius3;
    }
    
    public void setFloatingCornerRadius3(int radius) {
        this.floatingCornerRadius3 = radius;
        globalEditor.putInt(PREF_FLOATING_CORNER_RADIUS_3, radius).apply();
    }
    
    // ==================== 超大岛列表相对距离方法 ====================
    
    public int getIslandListDistance() {
        return islandListDistance;
    }
    
    public void setIslandListDistance(int distance) {
        this.islandListDistance = distance;
        globalEditor.putInt(PREF_ISLAND_LIST_DISTANCE, distance).apply();
    }
    
    // ==================== 超大岛列表水平相对距离方法 ====================
    
    public int getIslandListHorizontalDistance() {
        return islandListHorizontalDistance;
    }
    
    public void setIslandListHorizontalDistance(int distance) {
        this.islandListHorizontalDistance = distance;
        globalEditor.putInt(PREF_ISLAND_LIST_HORIZONTAL_DISTANCE, distance).apply();
    }
    
    // ==================== 透明度方法 ====================
    
    public int getOpacity() {
        return opacity;
    }
    
    public void setOpacity(int opacity) {
        this.opacity = opacity;
        globalEditor.putInt(PREF_OPACITY, opacity).apply();
    }
    
    public int getMediumOpacity() {
        return mediumOpacity;
    }
    
    public void setMediumOpacity(int mediumOpacity) {
        this.mediumOpacity = mediumOpacity;
        globalEditor.putInt(PREF_MEDIUM_OPACITY, mediumOpacity).apply();
    }
    
    public int getLargeOpacity() {
        return largeOpacity;
    }
    
    public void setLargeOpacity(int largeOpacity) {
        this.largeOpacity = largeOpacity;
        globalEditor.putInt(PREF_LARGE_OPACITY, largeOpacity).apply();
    }
}
