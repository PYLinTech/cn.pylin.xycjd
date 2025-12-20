package cn.pylin.xycjd;

import android.animation.ValueAnimator;
import android.app.ActivityManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.PixelFormat;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.preference.PreferenceManager;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AnticipateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.OvershootInterpolator;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.Shader;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.SimpleItemAnimator;

import java.util.List;

public class FloatingWindowService extends Service {

    private static FloatingWindowService instance;
    private WindowManager windowManager;
    private View floatingView;
    private WindowManager.LayoutParams params;
    
    private View floatingIslandView;
    private WindowManager.LayoutParams islandParams;
    
    private View floatingThreeCircleView;
    private WindowManager.LayoutParams threeCircleParams;
    private NotificationAdapter notificationAdapter;
    
    private boolean isClosingIsland = false;

    public static class NotificationInfo {
        String key;
        String packageName;
        String title;
        String content;
        PendingIntent pendingIntent;
        android.media.session.MediaSession.Token mediaToken;

        public NotificationInfo(String key, String packageName, String title, String content, PendingIntent pendingIntent) {
            this(key, packageName, title, content, pendingIntent, null);
        }

        public NotificationInfo(String key, String packageName, String title, String content, PendingIntent pendingIntent, android.media.session.MediaSession.Token mediaToken) {
            this.key = key;
            this.packageName = packageName;
            this.title = title;
            this.content = content;
            this.pendingIntent = pendingIntent;
            this.mediaToken = mediaToken;
        }
    }

    private java.util.LinkedList<NotificationInfo> notificationQueue = new java.util.LinkedList<>();

    private String lastNotificationPackageName;
    private String lastNotificationTitle;
    private String lastNotificationContent;
    
    private SharedPreferences preferences;
    private SharedPreferences modelFilterPrefs;
    
    private static final String PREF_FLOATING_SIZE = "floating_size";
    private static final String PREF_FLOATING_X = "floating_x";
    private static final String PREF_FLOATING_Y = "floating_y";
    
    private static final int DEFAULT_SIZE = 100;
    private static final int DEFAULT_X = 0;
    private static final int DEFAULT_Y = -100;
    
    // 第二个悬浮窗与第一个悬浮窗的垂直间距（dp）
    private static final int ISLAND_MARGIN_TOP = 20;

    @Override
    public void onCreate() {
        super.onCreate();
        instance = this;
        preferences = PreferenceManager.getDefaultSharedPreferences(this);
        modelFilterPrefs = getSharedPreferences("app_model_filter", Context.MODE_PRIVATE);
    }
    
    @Override
    public void onDestroy() {
        super.onDestroy();
        instance = null;
        if (windowManager != null) {
            if (floatingView != null && floatingView.getParent() != null) {
                windowManager.removeView(floatingView);
            }
            if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                windowManager.removeView(floatingIslandView);
            }
            if (floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
                windowManager.removeView(floatingThreeCircleView);
            }
        }
    }
    
    public static FloatingWindowService getInstance() {
        return instance;
    }
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null && intent.getBooleanExtra("update", false)) {
            // 更新悬浮窗
            int size = intent.getIntExtra("size", DEFAULT_SIZE);
            int x = intent.getIntExtra("x", DEFAULT_X);
            int y = intent.getIntExtra("y", DEFAULT_Y);
            updateFloatingWindow(size, x, y);
        } else {
            // 创建新的悬浮窗
            createFloatingWindow();
        }
        return START_STICKY;
    }
    
    public static boolean isServiceRunning(Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (FloatingWindowService.class.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }

    public void recreateWindow() {
        if (windowManager != null) {
            try {
                if (floatingView != null && floatingView.getParent() != null) {
                    windowManager.removeView(floatingView);
                }
                if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                    windowManager.removeView(floatingIslandView);
                }
                if (floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
                    windowManager.removeView(floatingThreeCircleView);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
            // 强制置空WindowManager，以便在ensureWindowManager中重新获取正确的实例
            windowManager = null;
        }
        
        floatingView = null;
        floatingIslandView = null;
        floatingThreeCircleView = null;
        
        // 延迟重建窗口，确保旧窗口已完全移除，且系统状态已更新
        // 解决降级时悬浮窗可能不显示的问题
        new android.os.Handler(android.os.Looper.getMainLooper()).postDelayed(() -> {
            try {
                // 再次检查服务是否存活
                if (instance == null) return;
                
                createFloatingWindow();
                
                if (!notificationQueue.isEmpty()) {
                    showThreeCircleIsland();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }, 200);
    }
    
    private int getWindowType() {
        if (AppAccessibilityService.getInstance() != null) {
            return WindowManager.LayoutParams.TYPE_ACCESSIBILITY_OVERLAY;
        }
        return Build.VERSION.SDK_INT >= Build.VERSION_CODES.O ? 
                WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY : 
                WindowManager.LayoutParams.TYPE_PHONE;
    }
    
    private void ensureWindowManager() {
        AppAccessibilityService service = AppAccessibilityService.getInstance();
        if (service != null) {
            windowManager = (WindowManager) service.getSystemService(WINDOW_SERVICE);
        } else {
            if (windowManager == null) {
                windowManager = (WindowManager) getSystemService(WINDOW_SERVICE);
            }
        }
    }

    private void createFloatingWindow() {
        ensureWindowManager();
        
        // 使用ContextThemeWrapper应用AppCompat主题，解决CircleImageView报错问题
        AppAccessibilityService service = AppAccessibilityService.getInstance();
        Context context = new android.view.ContextThemeWrapper(service != null ? service : this, R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        floatingView = inflater.inflate(R.layout.floating_window_layout, null);
        
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        
        params = new WindowManager.LayoutParams(
                size,
                size,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        params.x = x;
        params.y = y;
        
        windowManager.addView(floatingView, params);
    }
    
    public void updateFloatingWindow(int size, int x, int y) {
        if (windowManager != null && floatingView != null && params != null) {
            params.width = size;
            params.height = size;
            params.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
            params.x = x;
            params.y = y;
            params.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            windowManager.updateViewLayout(floatingView, params);
            
            SharedPreferences.Editor editor = preferences.edit();
            editor.putInt(PREF_FLOATING_SIZE, size);
            editor.putInt(PREF_FLOATING_X, x);
            editor.putInt(PREF_FLOATING_Y, y);
            editor.apply();
        }
    }
    
    public static int getSize(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
    }
    
    public static int getX(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
    }
    
    public static int getY(Context context) {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        return preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
    }

    private boolean isModelFilterEnabled(String packageName) {
        if (modelFilterPrefs == null || packageName == null) return false;
        return modelFilterPrefs.getBoolean(packageName, false);
    }

    public void showNotificationIsland(String packageName, String title, String content) {
        ensureWindowManager();
        
        // 确保状态复位
        isClosingIsland = false;

        if (floatingIslandView == null) {
            createFloatingIsland();
        }
        
        updateNotificationContent(packageName, title, content);
        
        if (floatingIslandView.getParent() == null) {
            // 在添加视图前设置初始状态，防止闪烁
            View islandContainer = floatingIslandView.findViewById(R.id.notification_recycler_view);
            if (islandContainer != null) {
                islandContainer.setAlpha(0f);
                islandContainer.setScaleX(0.8f);
                islandContainer.setScaleY(0.8f);
                islandContainer.setTranslationY(-dpToPx(50));
            }
            
            windowManager.addView(floatingIslandView, islandParams);
            floatingIslandView.post(this::startIslandEnterAnimation);
        }
    }
    
    /**
     * 执行标准灵动岛的入场动画
     */
    private void startIslandEnterAnimation() {
        if (floatingIslandView == null) return;
        
        View islandContainer = floatingIslandView.findViewById(R.id.notification_recycler_view);
        if (islandContainer == null) return;
        
        // 确保初始状态（虽然addView前设置过，这里再次确保逻辑完整）
        islandContainer.setAlpha(0f);
        islandContainer.setScaleX(0.8f);
        islandContainer.setScaleY(0.8f);
        islandContainer.setTranslationY(-dpToPx(50));
        
        // 组合动画
        islandContainer.animate()
                .alpha(1f)
                .scaleX(1f)
                .scaleY(1f)
                .translationY(0f)
                .setDuration(500)
                .setInterpolator(new OvershootInterpolator(1.2f)) // 轻微弹跳
                .start();
    }
    
    public void addNotification(String key, String packageName, String title, String content, PendingIntent pendingIntent) {
        addNotification(key, packageName, title, content, pendingIntent, null);
    }

    public void addNotification(String key, String packageName, String title, String content, PendingIntent pendingIntent, android.media.session.MediaSession.Token mediaToken) {
        new Handler(Looper.getMainLooper()).post(() -> {
            // 忽略标题为空的通知
            if (title == null || title.trim().isEmpty()) {
                return;
            }

            // 模型过滤逻辑：如果启用了模型过滤，且预测分数低于阈值(4.0)，则不显示
            if (isModelFilterEnabled(packageName)) {
                float score = NotificationMLManager.getInstance(this).predict(content != null ? content : "");
                if (score < 4.0f) {
                    return;
                }
            }

            boolean isIslandVisible = floatingIslandView != null && floatingIslandView.getParent() != null;

            // 查找是否已存在相同key的通知
            int existingIndex = -1;
            for (int i = 0; i < notificationQueue.size(); i++) {
                if (notificationQueue.get(i).key.equals(key)) {
                    existingIndex = i;
                    break;
                }
            }

            if (existingIndex != -1) {
                NotificationInfo info = notificationQueue.get(existingIndex);
                
                // 检查内容是否有变化 (避免重复刷新)
                boolean isTitleSame = (info.title == null ? title == null : info.title.equals(title));
                boolean isContentSame = (info.content == null ? content == null : info.content.equals(content));
                boolean isTokenSame = (info.mediaToken == null ? mediaToken == null : info.mediaToken.equals(mediaToken));

                // 如果内容完全一致且已经在队首，仅更新Intent（无需刷新UI）
                if (isTitleSame && isContentSame && isTokenSame && existingIndex == 0) {
                    info.pendingIntent = pendingIntent;
                    return;
                }

                // 如果已存在，直接更新内容
                info.title = title;
                info.content = content;
                info.pendingIntent = pendingIntent;
                info.mediaToken = mediaToken;
                
                // 将更新后的通知移动到队首
                if (existingIndex != 0) {
                    notificationQueue.remove(existingIndex);
                    notificationQueue.addFirst(info);
                    if (isIslandVisible && notificationAdapter != null) {
                        notificationAdapter.notifyItemMoved(existingIndex, 0);
                        notificationAdapter.notifyItemChanged(0);
                    }
                } else {
                    // 已经在队首，直接刷新
                    if (isIslandVisible && notificationAdapter != null) {
                        notificationAdapter.notifyItemChanged(0);
                    }
                }
            } else {
                // 如果不存在，添加到头部（最新的）
                notificationQueue.addFirst(new NotificationInfo(key, packageName, title, content, pendingIntent, mediaToken));
                
                // 如果展开的悬浮窗正在显示，插入新项
                if (isIslandVisible && notificationAdapter != null) {
                    notificationAdapter.notifyItemInserted(0);
                    // 确保列表滚动到顶部以显示最新通知
                    RecyclerView rv = floatingIslandView.findViewById(R.id.notification_recycler_view);
                    if (rv != null) {
                        rv.scrollToPosition(0);
                    }
                }
            }
            
            // 更新最近的通知变量
            if (!notificationQueue.isEmpty()) {
                NotificationInfo latest = notificationQueue.getFirst();
                lastNotificationPackageName = latest.packageName;
                lastNotificationTitle = latest.title;
                lastNotificationContent = latest.content;
            }

            // 显示或更新三圆悬浮窗
            showThreeCircleIsland();
        });
    }

    public void removeNotification(String key) {
        new Handler(Looper.getMainLooper()).post(() -> {
            removeNotificationInternal(key);
            
            if (notificationQueue.isEmpty()) {
                hideThreeCircleIsland();
                hideNotificationIsland();
                lastNotificationPackageName = null;
            } else {
                // 更新UI显示最新的通知
                NotificationInfo latest = notificationQueue.getFirst();
                lastNotificationPackageName = latest.packageName;
                lastNotificationTitle = latest.title;
                lastNotificationContent = latest.content;
                
                updateThreeCircleContent();
                // 如果展开的悬浮窗正在显示，也更新它
                if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                    updateNotificationContent(latest.packageName, latest.title, latest.content);
                }
            }
        });
    }

    private int removeNotificationInternal(String key) {
        if (key == null) return -1;
        for (int i = 0; i < notificationQueue.size(); i++) {
            if (key.equals(notificationQueue.get(i).key)) {
                notificationQueue.remove(i);
                return i;
            }
        }
        return -1;
    }

    /**
     * 隐藏通知悬浮窗
     */
    public void hideNotificationIsland() {
        if (isClosingIsland) return;
        
        if (windowManager != null && floatingIslandView != null && floatingIslandView.getParent() != null) {
            isClosingIsland = true;
            
            // 禁用点击事件，防止重复触发
            View islandRoot = floatingIslandView.findViewById(R.id.island_root);
            if (islandRoot != null) {
                islandRoot.setClickable(false);
                islandRoot.setOnClickListener(null);
            }

            // 更新Flags，移除焦点和触摸事件，防止动画过程中点击造成卡顿
            islandParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCHABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS;
            windowManager.updateViewLayout(floatingIslandView, islandParams);

            View islandContainer = floatingIslandView.findViewById(R.id.notification_recycler_view);
            if (islandContainer != null) {
                // 执行退出动画
                islandContainer.animate()
                        .alpha(0f)
                        .scaleX(0.8f)
                        .scaleY(0.8f)
                        .translationY(-dpToPx(50))
                        .setDuration(330)
                        .setInterpolator(new AnticipateInterpolator(1f))
                        .withEndAction(() -> {
                            // 动画结束后移除视图
                            if (floatingIslandView != null && floatingIslandView.getParent() != null) {
                                windowManager.removeView(floatingIslandView);
                                floatingIslandView = null;
                                notificationAdapter = null; // 清除引用
                            }
                            isClosingIsland = false;
                        })
                        .start();
            } else {
                // 如果找不到容器，直接移除
                windowManager.removeView(floatingIslandView);
                floatingIslandView = null;
                notificationAdapter = null; // 清除引用
                isClosingIsland = false;
            }
        }
    }
    
    /**
     * Fading Edge ItemDecoration
     */
    private class FadingEdgeDecoration extends RecyclerView.ItemDecoration {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final int fadeLength;
        private final int estimatedCardHeight;
        private LinearGradient topGradient;
        private LinearGradient bottomGradient;
        private int lastWidth = -1;
        private int lastHeight = -1;

        public FadingEdgeDecoration(int fadeLength, int estimatedCardHeight) {
            this.fadeLength = fadeLength;
            this.estimatedCardHeight = estimatedCardHeight;
            paint.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        }

        @Override
        public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDraw(c, parent, state);
            // 在绘制子View之前更新缩放，确保动画平滑
            // 这里使用 updateCardScales 更新 View 的 scale 属性
            updateCardScales(parent, estimatedCardHeight);
        }

        @Override
        public void onDrawOver(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
            super.onDrawOver(c, parent, state);
            
            int width = parent.getWidth();
            int height = parent.getHeight();
            
            if (width <= 0 || height <= 0) return;

            if (width != lastWidth || height != lastHeight) {
                lastWidth = width;
                lastHeight = height;
                
                // Top gradient: Transparent -> Black (Opaque)
                topGradient = new LinearGradient(0, 0, 0, fadeLength,
                        0x00000000, 0xFF000000, Shader.TileMode.CLAMP);
                
                // Bottom gradient: Black (Opaque) -> Transparent
                bottomGradient = new LinearGradient(0, height - fadeLength, 0, height,
                        0xFF000000, 0x00000000, Shader.TileMode.CLAMP);
            }

            // Draw top fade
            paint.setShader(topGradient);
            c.drawRect(0, 0, width, fadeLength, paint);

            // Draw bottom fade
            paint.setShader(bottomGradient);
            c.drawRect(0, height - fadeLength, width, height, paint);
        }
    }

    private void updateCardScales(RecyclerView recyclerView, int estimatedCardHeight) {
        float recyclerViewCenterY = recyclerView.getHeight() / 2f;
        int minScaleDistance = estimatedCardHeight / 2 + dpToPx(20);

        for (int i = 0; i < recyclerView.getChildCount(); i++) {
            View child = recyclerView.getChildAt(i);
            // 使用视觉位置 (Top + TranslationY) 计算中心点
            // 这样在 ItemAnimator 移动动画过程中也能获取正确的实时位置
            float childCenterY = child.getY() + child.getHeight() / 2f;
            float absDistance = Math.abs(childCenterY - recyclerViewCenterY);

            float scale;
            if (absDistance < minScaleDistance) {
                float fraction = absDistance / minScaleDistance;
                scale = 1f - 0.2f * fraction * fraction;
            } else {
                scale = 0.8f;
            }

            child.setScaleX(scale);
            child.setScaleY(scale);
        }
    }

    /**
     * 创建灵动岛样式的悬浮窗
     */
    private void createFloatingIsland() {
        // 使用ContextThemeWrapper应用AppCompat主题
        AppAccessibilityService service = AppAccessibilityService.getInstance();
        Context context = new android.view.ContextThemeWrapper(service != null ? service : this, R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        floatingIslandView = inflater.inflate(R.layout.floating_window_island, null);
        
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        
        int islandY = y + size + dpToPx(ISLAND_MARGIN_TOP);

        RecyclerView recyclerView = floatingIslandView.findViewById(R.id.notification_recycler_view);
        
        // 开启硬件加速 Layer 以支持 PorterDuff.Mode.DST_IN
        recyclerView.setLayerType(View.LAYER_TYPE_HARDWARE, null);
        
        // 设置 RecyclerView
        LinearLayoutManager layoutManager = new LinearLayoutManager(this) {
            @Override
            protected int getExtraLayoutSpace(RecyclerView.State state) {
                // 增加额外的布局空间，确保屏幕外的 item 也能参与布局和动画
                // 这样当删除卡片时，下方的卡片（即使原本不可见）也能平滑地移入
                return 1000;
            }
        };
        recyclerView.setLayoutManager(layoutManager);
        notificationAdapter = new NotificationAdapter();
        recyclerView.setAdapter(notificationAdapter);
        
        // 显式设置 DefaultItemAnimator 并配置参数
        // 必须使用 DefaultItemAnimator 以确保 Move 动画生效
        androidx.recyclerview.widget.DefaultItemAnimator animator = new androidx.recyclerview.widget.DefaultItemAnimator();
        animator.setSupportsChangeAnimations(false);
        // 优化动画时长：移除要快，补位移动要慢且清晰
        animator.setRemoveDuration(200);
        animator.setMoveDuration(300);
        animator.setAddDuration(300);
        recyclerView.setItemAnimator(animator);
        
        // 添加间距装饰器
        int itemSpacing = dpToPx(16);
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull android.graphics.Rect outRect, @NonNull View view, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                outRect.bottom = itemSpacing; // 卡片之间的间距
            }
        });
        
        // 添加自定义模糊边界装饰器 (在代码中实现，不使用XML)
        // 估算高度：Card(height) + Spacing(16dp)
        int cardHeight = getResources().getDimensionPixelSize(R.dimen.notification_card_height);
        int estimatedCardHeight = cardHeight + dpToPx(16); 
        recyclerView.addItemDecoration(new FadingEdgeDecoration(dpToPx(50), estimatedCardHeight)); // 50dp fade length

        // 1. 略微增加可见高度为1张卡片高度的1.6倍
        int visibleHeight = (int) (estimatedCardHeight * 1.8f);

        // 设置 RecyclerView 的 padding，使得第一个 item 能居中（配合 LinearSnapHelper）
        // 计算方式：(ViewHeight - ItemHeight) / 2
        // ItemHeight 约为 estimatedCardHeight (实际可能会有细微差别，这里取估算值)
        int paddingVertical = (visibleHeight - estimatedCardHeight) / 2;
        recyclerView.setPadding(0, paddingVertical, 0, paddingVertical);
        recyclerView.setClipToPadding(false);

        // 3. 最佳位置吸附 (使用自定义的SpringSnapHelper实现物理弹簧效果)
        new SpringSnapHelper().attachToRecyclerView(recyclerView);

        // 6. 左滑右滑移除
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public float getSwipeThreshold(@NonNull RecyclerView.ViewHolder viewHolder) {
                // 降低滑动阈值，只需滑动 25% 即可触发移除，提升跟手感
                return 0.25f;
            }

            @Override
            public float getSwipeEscapeVelocity(float defaultValue) {
                // 降低逃逸速度要求，让轻微的甩动也能触发移除
                return defaultValue * 0.5f;
            }

            @Override
            public long getAnimationDuration(@NonNull RecyclerView recyclerView, int animationType, float animateDx, float animateDy) {
                // 缩短动画时间，提升响应速度
                return (long) (super.getAnimationDuration(recyclerView, animationType, animateDx, animateDy) * 0.6f);
            }

            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                int position = viewHolder.getAdapterPosition();
                if (position != RecyclerView.NO_POSITION && position < notificationQueue.size()) {
                        NotificationInfo removedInfo = notificationQueue.remove(position);

                        // 只使用内容进行训练
                        String trainingText = (removedInfo.content != null ? removedInfo.content : "");
                        if (preferences.getBoolean("pref_local_learning_enabled", false) && isModelFilterEnabled(removedInfo.packageName)) {
                            NotificationMLManager.getInstance(FloatingWindowService.this).process(trainingText, false);
                        }

                        notificationAdapter.notifyItemRemoved(position);
                        // 通知后续数据位置发生变化，确保动画连贯
                        notificationAdapter.notifyItemRangeChanged(position, notificationQueue.size() - position);
                    
                        // 尝试从系统通知栏移除通知
                    AppNotificationListenerService listenerService = AppNotificationListenerService.getInstance();
                    if (listenerService != null) {
                        try {
                            listenerService.cancelNotification(removedInfo.key);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                    }
                    
                    // 如果队列为空，关闭悬浮窗
                    if (notificationQueue.isEmpty()) {
                        hideNotificationIsland();
                        lastNotificationPackageName = null;
                        hideThreeCircleIsland();
                    } else if (position == 0) {
                        // 如果移除了第一个（最新的），更新 lastNotification 为新的头部
                        NotificationInfo latest = notificationQueue.getFirst();
                        lastNotificationPackageName = latest.packageName;
                        lastNotificationTitle = latest.title;
                        lastNotificationContent = latest.content;
                        // 更新三圆岛显示
                        updateThreeCircleContent();
                    }
                }
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {
                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                
                // 实现非线性动画效果：先快后慢
                if (actionState == ItemTouchHelper.ACTION_STATE_SWIPE) {
                    float width = (float) viewHolder.itemView.getWidth();
                    float absDx = Math.abs(dX);
                    float fraction = absDx / width;
                    
                    // 限制最大缩放和透明度变化
                    // 使用 sin 函数实现先快后慢的效果 (0 -> 1 的过程变化率逐渐减小)
                    float nonlinearFraction = (float) Math.sin(Math.min(1f, fraction) * Math.PI / 2);
                    
                    // 缩放效果：从1.0减小到0.9
                    float scale = 1.0f - 0.1f * nonlinearFraction;
                    scale = Math.max(0.9f, scale);
                    
                    // 透明度效果：从1.0减小到0.2
                    float alpha = 1.0f - 0.8f * nonlinearFraction;
                    alpha = Math.max(0.2f, alpha);
                    
                    viewHolder.itemView.setScaleX(scale);
                    viewHolder.itemView.setScaleY(scale);
                    viewHolder.itemView.setAlpha(alpha);
                    
                    // 必须调用 super 否则没有平移效果
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                } else {
                    super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
                }
            }

            @Override
            public void clearView(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder) {
                super.clearView(recyclerView, viewHolder);
                // 恢复视图状态，防止复用问题
                viewHolder.itemView.setScaleX(1.0f);
                viewHolder.itemView.setScaleY(1.0f);
                viewHolder.itemView.setAlpha(1.0f);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        ViewGroup.MarginLayoutParams containerParams = (ViewGroup.MarginLayoutParams) recyclerView.getLayoutParams();
        containerParams.topMargin = islandY;
        containerParams.height = visibleHeight; // 设置 RecyclerView 高度
        recyclerView.setLayoutParams(containerParams);
        recyclerView.setTranslationX(x);
        
        // 5. 点击卡片以外的区域收起
        // 将 Window 设置为全屏，并监听根布局点击事件
        View islandRoot = floatingIslandView.findViewById(R.id.island_root);
        islandRoot.setOnClickListener(v -> hideNotificationIsland());
        
        islandParams = new WindowManager.LayoutParams(
                WindowManager.LayoutParams.MATCH_PARENT,
                WindowManager.LayoutParams.MATCH_PARENT, // 全屏以捕获点击
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        islandParams.gravity = Gravity.TOP | Gravity.START;
        islandParams.x = 0;
        islandParams.y = 0;
        
        // 初始触发一次滚动监听以设置初始缩放
        recyclerView.post(() -> {
            if (recyclerView.getLayoutManager() != null) {
                // 模拟滚动以触发 onScrolled
                recyclerView.scrollBy(0, 0);
            }
        });
    }
    
    /**
     * 更新通知内容
     * @param packageName 应用包名
     * @param title 通知标题
     * @param content 通知内容
     */
    private void updateNotificationContent(String packageName, String title, String content) {
        if (notificationAdapter != null) {
            notificationAdapter.notifyDataSetChanged();
        }
    }

    private class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

        NotificationAdapter() {
            setHasStableIds(true);
        }

        @Override
        public long getItemId(int position) {
            // 使用 key 的 hashCode 作为唯一 ID，确保动画正确执行
            return notificationQueue.get(position).key.hashCode();
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification_card, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            NotificationInfo info = notificationQueue.get(position);
            holder.currentPackageName = info.packageName;
            
            holder.title.setText(info.title != null ? info.title : "");
            holder.content.setText(info.content != null ? info.content : "");
            
            // 绑定媒体控制器（内部会处理图标显示）
            holder.bindMediaController(info.mediaToken);
            // 如果没有 token，bindMediaController 可能直接返回了，需要在这里确保图标被设置
            if (info.mediaToken == null) {
                updateIcon(holder.icon, null, info.packageName);
            }

            holder.container.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.95f).scaleY(0.95f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1f).scaleY(1f).setDuration(100).start();
                        if (event.getAction() == MotionEvent.ACTION_UP) {
                            v.performClick();
                        }
                        break;
                }
                return true;
            });

            holder.container.setOnClickListener(v -> {
                // 只使用内容进行训练
                String trainingText = (info.content != null ? info.content : "");
                if (preferences.getBoolean("pref_local_learning_enabled", false) && isModelFilterEnabled(info.packageName)) {
                    NotificationMLManager.getInstance(FloatingWindowService.this).process(trainingText, true);
                }
                try {
                    // 优先尝试使用 PendingIntent (响应通知事件)
                    if (info.pendingIntent != null) {
                        info.pendingIntent.send();
                        // 成功响应后，尝试消除系统通知
                        AppNotificationListenerService listenerService = AppNotificationListenerService.getInstance();
                        if (listenerService != null) {
                            listenerService.cancelNotification(info.key);
                        }
                    } else {
                        // 如果没有 PendingIntent，则回退到打开应用
                        throw new PendingIntent.CanceledException();
                    }
                } catch (PendingIntent.CanceledException e) {
                    // 如果 PendingIntent 发送失败，尝试直接打开应用
                    Intent launchIntent = getPackageManager().getLaunchIntentForPackage(info.packageName);
                    if (launchIntent != null) {
                        launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(launchIntent);
                        // 打开应用后也尝试消除系统通知（可选，视需求而定，这里保持一致性也消除）
                        AppNotificationListenerService listenerService = AppNotificationListenerService.getInstance();
                        if (listenerService != null) {
                            listenerService.cancelNotification(info.key);
                        }
                    }
                }
                
                // 点击后从APP列表中移除该通知
                removeNotification(info.key);
                
                hideNotificationIsland();
            });
        }

        @Override
        public int getItemCount() {
            return notificationQueue.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            View container;
            ImageView icon;
            TextView title;
            TextView content;
            LinearLayout mediaControls;
            ImageView btnPrev;
            ImageView btnPlay;
            ImageView btnNext;
            android.media.session.MediaController mediaController;
            android.media.session.MediaController.Callback mediaCallback;
            String currentPackageName;

            ViewHolder(View itemView) {
                super(itemView);
                container = itemView.findViewById(R.id.island_container);
                icon = itemView.findViewById(R.id.app_icon);
                title = itemView.findViewById(R.id.notification_title);
                content = itemView.findViewById(R.id.notification_content);
                mediaControls = itemView.findViewById(R.id.media_controls);
                btnPrev = itemView.findViewById(R.id.btn_prev);
                btnPlay = itemView.findViewById(R.id.btn_play);
                btnNext = itemView.findViewById(R.id.btn_next);
            }

            void bindMediaController(android.media.session.MediaSession.Token token) {
                // 清理旧的Controller
                if (mediaController != null && mediaCallback != null) {
                    mediaController.unregisterCallback(mediaCallback);
                }
                
                if (token == null) {
                    mediaControls.setVisibility(View.GONE);
                    mediaController = null;
                    return;
                }

                mediaControls.setVisibility(View.VISIBLE);
                content.setMaxLines(1); // 媒体通知限制为1行
                mediaController = new android.media.session.MediaController(FloatingWindowService.this, token);
                
                // 设置初始状态
                updateMediaState(mediaController.getPlaybackState());
                updateMediaMetadata(mediaController.getMetadata());

                mediaCallback = new android.media.session.MediaController.Callback() {
                    @Override
                    public void onPlaybackStateChanged(android.media.session.PlaybackState state) {
                        // 在主线程更新UI
                        new Handler(Looper.getMainLooper()).post(() -> updateMediaState(state));
                    }

                    @Override
                    public void onMetadataChanged(android.media.MediaMetadata metadata) {
                        new Handler(Looper.getMainLooper()).post(() -> updateMediaMetadata(metadata));
                    }
                    
                    @Override
                    public void onSessionDestroyed() {
                        new Handler(Looper.getMainLooper()).post(() -> {
                            mediaControls.setVisibility(View.GONE);
                        });
                    }
                };
                mediaController.registerCallback(mediaCallback);

                // 绑定按钮点击事件
                btnPrev.setOnClickListener(v -> {
                    if (mediaController != null) {
                        mediaController.getTransportControls().skipToPrevious();
                    }
                });

                btnNext.setOnClickListener(v -> {
                    if (mediaController != null) {
                        mediaController.getTransportControls().skipToNext();
                    }
                });

                btnPlay.setOnClickListener(v -> {
                    if (mediaController != null) {
                        android.media.session.PlaybackState state = mediaController.getPlaybackState();
                        if (state != null) {
                            if (state.getState() == android.media.session.PlaybackState.STATE_PLAYING) {
                                mediaController.getTransportControls().pause();
                            } else {
                                mediaController.getTransportControls().play();
                            }
                        }
                    }
                });
            }

            private void updateMediaState(android.media.session.PlaybackState state) {
                if (state == null) return;
                
                boolean isPlaying = state.getState() == android.media.session.PlaybackState.STATE_PLAYING;
                btnPlay.setImageResource(isPlaying ? R.drawable.ic_media_pause : R.drawable.ic_media_play);
            }

            private void updateMediaMetadata(android.media.MediaMetadata metadata) {
                // 使用外部通用方法更新图标
                updateIcon(icon, metadata, currentPackageName);
            }
        }
    }
    
    /**
     * dp转换为px
     * @param dp dp值
     * @return px值
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }
    
    /**
     * 显示三圆灵动岛悬浮窗
     */
    public void showThreeCircleIsland() {
        if (notificationQueue.isEmpty()) return;
        
        ensureWindowManager();
        
        if (floatingThreeCircleView == null) {
            createThreeCircleIsland();
        }
        
        updateThreeCircleContent();
        
        if (floatingThreeCircleView.getParent() == null) {
            windowManager.addView(floatingThreeCircleView, threeCircleParams);
            floatingThreeCircleView.post(this::startThreeCircleAnimation);
        }
    }

    public void showThreeCircleIsland(String packageName) {
        showThreeCircleIsland();
    }

    public void performThreeCircleClick() {
        if (notificationQueue.isEmpty()) return;
        NotificationInfo info = notificationQueue.getFirst();
        showNotificationIsland(info.packageName, info.title, info.content);
    }

        /**
     * 更新图标显示（通用方法）
     * @param iconView 目标ImageView
     * @param metadata 媒体元数据（可选）
     * @param packageName 应用包名
     */
    private void updateIcon(ImageView iconView, android.media.MediaMetadata metadata, String packageName) {
        if (iconView == null) return;
        
        boolean iconSet = false;
        
        // 尝试从媒体元数据获取封面
        if (metadata != null) {
            android.graphics.Bitmap art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ALBUM_ART);
            if (art == null) {
                art = metadata.getBitmap(android.media.MediaMetadata.METADATA_KEY_ART);
            }
            if (art != null) {
                iconView.setImageBitmap(art);
                iconSet = true;
            }
        }
        
        // 如果没有封面，显示应用图标
        if (!iconSet && packageName != null) {
            try {
                iconView.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
            } catch (Exception e) {
                iconView.setImageResource(android.R.mipmap.sym_def_app_icon);
            }
        }
    }

    /**
     * 更新三圆灵动岛内容
     */
    private void updateThreeCircleContent() {
        if (floatingThreeCircleView == null || notificationQueue.isEmpty()) {
            return;
        }
        
        NotificationInfo info = notificationQueue.getFirst();
        String packageName = info.packageName;
        
        // 获取应用图标 View
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        
        // 获取媒体元数据（如果有）
        android.media.MediaMetadata metadata = null;
        if (info.mediaToken != null) {
            try {
                android.media.session.MediaController controller = new android.media.session.MediaController(this, info.mediaToken);
                metadata = controller.getMetadata();
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        
        // 更新图标
        updateIcon(appIcon, metadata, packageName);
        
        // 更新数量
        TextView countText = floatingThreeCircleView.findViewById(R.id.circle_black2);
        if (countText != null) {
            int count = notificationQueue.size();
            countText.setText(count > 9 ? "9+" : String.valueOf(count));
        }
    }

    /**
     * 隐藏三圆灵动岛悬浮窗
     */
    public void hideThreeCircleIsland() {
        if (windowManager != null && floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
            startThreeCircleExitAnimation(() -> {
                if (windowManager != null && floatingThreeCircleView != null && floatingThreeCircleView.getParent() != null) {
                    windowManager.removeView(floatingThreeCircleView);
                    floatingThreeCircleView = null;
                }
            });
        }
    }
    
    /**
     * 执行三圆灵动岛的退出动画
     * @param onAnimationEnd 动画结束回调
     */
    private void startThreeCircleExitAnimation(Runnable onAnimationEnd) {
        if (floatingThreeCircleView == null) {
            if (onAnimationEnd != null) onAnimationEnd.run();
            return;
        }

        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int circleSize = size - dpToPx(4);
        int targetSpacerWidth = dpToPx(10);
        int targetMargin = dpToPx(2);
        // 最终宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth
        int totalContentWidth = 3 * circleSize + 6 * targetMargin + 2 * targetSpacerWidth;
        
        // 偏移量 = circleSize + margin + spacer + margin = circleSize + 2*margin + spacer
        int offset = circleSize + 2 * targetMargin + targetSpacerWidth;

        final View background = floatingThreeCircleView.findViewById(R.id.island_background);
        final View circle1 = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        final View circle3 = floatingThreeCircleView.findViewById(R.id.circle_black2);

        // 倒放：从1f到0f
        ValueAnimator animator = ValueAnimator.ofFloat(1f, 0f);
        animator.setDuration(400); // 退出动画稍微快一点
        animator.setInterpolator(new AnticipateInterpolator(1.2f)); // 使用Anticipate实现倒放的"回弹"效果
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // 1. 背景收缩
            int currentWidth = (int) (circleSize + (totalContentWidth - circleSize) * fraction);
            ViewGroup.LayoutParams bgParams = background.getLayoutParams();
            bgParams.width = currentWidth;
            background.setLayoutParams(bgParams);

            // 2. 圆形回归
            float currentOffset = offset * fraction;
            
            // Circle 1 (Left) moves back to center
            circle1.setTranslationX(-currentOffset);
            
            // Circle 3 (Right) moves back to center
            circle3.setTranslationX(currentOffset);
        });
        
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }
        });
        
        animator.start();
    }

    /**
     * 创建三圆灵动岛样式的悬浮窗
     */
    private void createThreeCircleIsland() {
        AppAccessibilityService service = AppAccessibilityService.getInstance();
        Context context = new android.view.ContextThemeWrapper(service != null ? service : this, R.style.AppTheme);
        LayoutInflater inflater = LayoutInflater.from(context);
        floatingThreeCircleView = inflater.inflate(R.layout.floating_window_island_three, null);
        
        // 获取第一个悬浮窗的位置和大小
        int x = preferences.getInt(PREF_FLOATING_X, DEFAULT_X);
        int y = preferences.getInt(PREF_FLOATING_Y, DEFAULT_Y);
        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        
        // 计算第三个小岛的圆形大小，比第一个悬浮窗小4dp
        int circleSize = size - dpToPx(4); // 减去4dp，使圆形小一点
        
        // 动态设置圆形大小
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        View blackCircle = floatingThreeCircleView.findViewById(R.id.circle_black);
        View blackCircle2 = floatingThreeCircleView.findViewById(R.id.circle_black2);
        
        // 设置圆形大小
        FrameLayout.LayoutParams layoutParams = (FrameLayout.LayoutParams) appIcon.getLayoutParams();
        layoutParams.width = circleSize;
        layoutParams.height = circleSize;
        appIcon.setLayoutParams(layoutParams);
        
        FrameLayout.LayoutParams blackLayoutParams = (FrameLayout.LayoutParams) blackCircle.getLayoutParams();
        blackLayoutParams.width = circleSize;
        blackLayoutParams.height = circleSize;
        blackCircle.setLayoutParams(blackLayoutParams);
        
        FrameLayout.LayoutParams blackLayoutParams2 = (FrameLayout.LayoutParams) blackCircle2.getLayoutParams();
        blackLayoutParams2.width = circleSize;
        blackLayoutParams2.height = circleSize;
        blackCircle2.setLayoutParams(blackLayoutParams2);

        // 初始化背景
        View background = floatingThreeCircleView.findViewById(R.id.island_background);
        ViewGroup.LayoutParams bgParams = background.getLayoutParams();
        bgParams.width = circleSize; // 初始只显示一个圆的大小
        background.setLayoutParams(bgParams);

        // 计算三圆悬浮岛的位置，使其中心与第一个悬浮窗的中心对齐
        int targetSpacerWidth = dpToPx(10);
        int margin = dpToPx(2);
        // 三圆悬浮岛的总宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth + 2 * padding
        int totalWidth = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth + dpToPx(20);
        int totalHeight = size; 
        
        // 创建悬浮窗布局参数
        threeCircleParams = new WindowManager.LayoutParams(
                totalWidth,
                totalHeight,
                getWindowType(),
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,
                PixelFormat.TRANSLUCENT
        );
        
        // 设置位置，使其中心与第一个悬浮窗的中心对齐
        threeCircleParams.gravity = Gravity.TOP | Gravity.CENTER_HORIZONTAL;
        threeCircleParams.x = x;
        threeCircleParams.y = y;
        
        floatingThreeCircleView.setOnClickListener(view -> {
            if (!notificationQueue.isEmpty()) {
                NotificationInfo info = notificationQueue.getFirst();
                showNotificationIsland(info.packageName, info.title, info.content);
            }
        });
    }

    /**
     * 执行三圆灵动岛的入场动画
     */
    private void startThreeCircleAnimation() {
        if (floatingThreeCircleView == null) return;

        int size = preferences.getInt(PREF_FLOATING_SIZE, DEFAULT_SIZE);
        int circleSize = size - dpToPx(4);
        int targetSpacerWidth = dpToPx(10);
        int targetMargin = dpToPx(2);
        // 最终宽度 = 3 * circleSize + 6 * margin + 2 * targetSpacerWidth
        int totalContentWidth = 3 * circleSize + 6 * targetMargin + 2 * targetSpacerWidth;
        
        // 偏移量 = circleSize + margin + spacer + margin = circleSize + 2*margin + spacer
        int offset = circleSize + 2 * targetMargin + targetSpacerWidth;

        final View background = floatingThreeCircleView.findViewById(R.id.island_background);
        final View circle1 = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        final View circle3 = floatingThreeCircleView.findViewById(R.id.circle_black2);

        ValueAnimator animator = ValueAnimator.ofFloat(0f, 1f);
        animator.setDuration(600);
        animator.setInterpolator(new DecelerateInterpolator(2f));
        animator.addUpdateListener(animation -> {
            float fraction = (float) animation.getAnimatedValue();

            // 1. 背景展开
            int currentWidth = (int) (circleSize + (totalContentWidth - circleSize) * fraction);
            ViewGroup.LayoutParams bgParams = background.getLayoutParams();
            bgParams.width = currentWidth;
            background.setLayoutParams(bgParams);

            // 2. 圆形移动
            float currentOffset = offset * fraction;
            
            // Circle 1 (Left) moves left (negative X)
            circle1.setTranslationX(-currentOffset);
            
            // Circle 3 (Right) moves right (positive X)
            circle3.setTranslationX(currentOffset);
        });
        animator.start();
    }
    
    /**
     * 更新三圆灵动岛内容
     * @param packageName 应用包名
     */
    private void updateThreeCircleContent(String packageName) {
        if (floatingThreeCircleView == null) {
            return;
        }
        
        // 获取应用图标
        CircleImageView appIcon = floatingThreeCircleView.findViewById(R.id.circle_app_icon);
        try {
            // 使用PackageManager获取应用图标
            appIcon.setImageDrawable(getPackageManager().getApplicationIcon(packageName));
        } catch (Exception e) {
            // 如果获取失败，使用默认图标
            appIcon.setImageResource(android.R.mipmap.sym_def_app_icon);
        }
    }
    
    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
