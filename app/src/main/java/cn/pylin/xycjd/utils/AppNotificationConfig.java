package cn.pylin.xycjd.utils;

/**
 * 应用通知配置类
 * 封装应用的完整通知处理配置
 */
public class AppNotificationConfig {
    // 基础配置
    public boolean enabled;              // 是否启用通知监听
    public boolean isExisting;           // 是否已存在的通知
    public boolean isMedia;              // 是否媒体通知
    
    // 模式配置
    public String appMode;               // APP模式：mode_super_island_only, mode_notification_bar_only, mode_both
    
    // 模型过滤配置
    public boolean modelFilterEnabled;   // 模型过滤是否开启
    public String modelType;             // 模型类型：model_local, model_online
    
    // 行为配置
    public boolean autoExpand;           // 自动展开
    public boolean vibration;            // 震动
    public boolean sound;                // 声音
    
    public AppNotificationConfig() {
        // 默认值
        enabled = false;
        isExisting = false;
        isMedia = false;
        appMode = "mode_super_island_only";
        modelFilterEnabled = false;
        modelType = "model_local";
        autoExpand = false;
        vibration = false;
        sound = false;
    }

    /**
     * 检查是否需要模型过滤
     */
    public boolean needsModelFiltering() {
        return modelFilterEnabled && !isMedia;
    }
}
