package cn.pylin.xycjd.ui.view;

import android.content.Context;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.widget.SeekBar;

import androidx.appcompat.widget.AppCompatSeekBar;

import cn.pylin.xycjd.R;

/**
 * 自定义媒体进度条组件
 * 解决拖拽进度条与卡片滑动的冲突问题
 */
public class MediaSeekBar extends AppCompatSeekBar {

    private boolean isDragging = false;
    private boolean isTouchDown = false;  // 新增：记录触摸是否按下
    private OnSeekBarChangeListener externalListener;
    private int pendingProgress = -1;  // 新增：待应用的进度值
    private boolean hasPendingSeek = false;  // 新增：是否有待应用的 seek

    public MediaSeekBar(Context context) {
        super(context);
        init();
    }

    public MediaSeekBar(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public MediaSeekBar(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // 设置进度条颜色为白色
        setProgressDrawableColor();
        
        // 设置自定义监听器来处理拖拽状态
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (externalListener != null) {
                    externalListener.onProgressChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isTouchDown = true;  // 触摸按下
                isDragging = true;
                if (externalListener != null) {
                    externalListener.onStartTrackingTouch(seekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isDragging = false;
                isTouchDown = false;  // 触摸释放
                // 清除待处理状态（不调用 externalListener，让外部自行处理）
                hasPendingSeek = false;
                pendingProgress = -1;
                if (externalListener != null) {
                    externalListener.onStopTrackingTouch(seekBar);
                }
            }
        });
    }

    /**
     * 设置进度条颜色为白色
     */
    private void setProgressDrawableColor() {
        // 获取系统默认的seekbar样式
        android.graphics.drawable.Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            // 设置进度条颜色为白色
            progressDrawable.setTint(getResources().getColor(R.color.colorSurface));
        }
        
        // 设置背景颜色为半透明白色
        android.graphics.drawable.Drawable background = getBackground();
        if (background != null) {
            background.setTint(getResources().getColor(R.color.colorOnSurfaceSecondary));
        }
        
        // 设置拇指（拖动按钮）颜色为白色
        android.graphics.drawable.Drawable thumb = getThumb();
        if (thumb != null) {
            thumb.setTint(getResources().getColor(R.color.colorSurface));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // 通知父容器不要拦截触摸事件，确保拖拽流畅
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                // 恢复父容器的事件拦截
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        // 保存外部监听器，内部处理拖拽状态
        this.externalListener = listener;
    }

    /**
     * 获取当前是否正在拖拽
     */
    public boolean isDragging() {
        return isDragging;
    }

    /**
     * 设置拖拽状态（用于外部控制）
     */
    public void setDragging(boolean dragging) {
        this.isDragging = dragging;
    }

    /**
     * 检查触摸是否按下
     */
    public boolean isTouchDown() {
        return isTouchDown;
    }

    /**
     * 设置待应用的进度值（用于批量处理拖动时的进度更新）
     * @param progress 进度值
     */
    public void setPendingProgress(int progress) {
        this.pendingProgress = progress;
        this.hasPendingSeek = true;
    }

    /**
     * 获取待应用的进度值
     * @return 待应用的进度值，如果没有返回 -1
     */
    public int getPendingProgress() {
        return pendingProgress;
    }

    /**
     * 检查是否有待应用的 seek
     */
    public boolean hasPendingSeek() {
        return hasPendingSeek;
    }

    /**
     * 清除待应用的 seek
     */
    public void clearPendingSeek() {
        hasPendingSeek = false;
        pendingProgress = -1;
    }
}
