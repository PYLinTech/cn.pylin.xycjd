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
    private OnSeekBarChangeListener externalListener;

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
        setProgressDrawableColor();
        
        super.setOnSeekBarChangeListener(new OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                if (externalListener != null) {
                    externalListener.onProgressChanged(seekBar, progress, fromUser);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                isDragging = true;
                if (externalListener != null) {
                    externalListener.onStartTrackingTouch(seekBar);
                }
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                isDragging = false;
                if (externalListener != null) {
                    externalListener.onStopTrackingTouch(seekBar);
                }
            }
        });
    }

    private void setProgressDrawableColor() {
        android.graphics.drawable.Drawable progressDrawable = getProgressDrawable();
        if (progressDrawable != null) {
            progressDrawable.setTint(getResources().getColor(R.color.colorSurface));
        }
        
        android.graphics.drawable.Drawable background = getBackground();
        if (background != null) {
            background.setTint(getResources().getColor(R.color.colorOnSurfaceSecondary));
        }
        
        android.graphics.drawable.Drawable thumb = getThumb();
        if (thumb != null) {
            thumb.setTint(getResources().getColor(R.color.colorSurface));
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (getParent() != null) {
                    getParent().requestDisallowInterceptTouchEvent(false);
                }
                break;
        }

        return super.onTouchEvent(event);
    }

    @Override
    public void setOnSeekBarChangeListener(OnSeekBarChangeListener listener) {
        this.externalListener = listener;
    }

    public boolean isDragging() {
        return isDragging;
    }
}
