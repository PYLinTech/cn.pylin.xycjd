package cn.pylin.xycjd.ui.view;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.Random;

public class AudioVisualizerView extends View {
    private Paint barPaint;
    private Path clipPath;
    private RectF clipRect;
    private float[] barHeights;
    private int barCount = 6;
    private float barWidth;
    private float barGap;
    private ValueAnimator animator;
    private Random random;

    public AudioVisualizerView(Context context) {
        super(context);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public AudioVisualizerView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        barPaint.setColor(0xFFFFFFFF);
        barPaint.setStyle(Paint.Style.FILL);

        clipPath = new Path();
        clipRect = new RectF();

        barHeights = new float[barCount];
        random = new Random();

        setVisibility(GONE);
    }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);

        barGap = w * 0.1f;
        float totalGap = (barCount - 1) * barGap;
        barWidth = (w - totalGap) / barCount;

        clipRect.set(0, 0, w, h);
        clipPath.reset();
        clipPath.addOval(clipRect, Path.Direction.CW);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (getVisibility() != VISIBLE) {
            return;
        }

        canvas.save();
        canvas.clipPath(clipPath);

        float centerY = getHeight() / 2f;
        float maxBarHeight = getHeight() * 0.8f;
        float cornerRadius = barWidth * 0.4f;

        for (int i = 0; i < barCount; i++) {
            float barHeight = barHeights[i] * maxBarHeight;
            float left = i * (barWidth + barGap);
            float right = left + barWidth;
            float top = centerY - barHeight / 2f;
            float bottom = centerY + barHeight / 2f;

            RectF barRect = new RectF(left, top, right, bottom);
            canvas.drawRoundRect(barRect, cornerRadius, cornerRadius, barPaint);
        }

        canvas.restore();
    }

    public void startAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
        }

        animator = ValueAnimator.ofFloat(0, 1);
        animator.setDuration(3600);
        animator.setRepeatCount(ValueAnimator.INFINITE);
        animator.setRepeatMode(ValueAnimator.RESTART);
        animator.setInterpolator(new LinearInterpolator());

        animator.addUpdateListener(animation -> {
            float value = (float) animation.getAnimatedValue();
            float time = (float) (value * Math.PI * 4);
            
            for (int i = 0; i < barCount; i++) {
                float barPos = i / (float) (barCount - 1);
                float centerDist = Math.abs(barPos - 0.5f) * 2f;
                float edgeAttenuation = 1f - centerDist * 0.85f;
                edgeAttenuation = Math.max(0.1f, edgeAttenuation);
                
                float wave = (float) Math.sin(time * 2f + barPos * 3f);
                
                barHeights[i] = 0.1f + (wave * 0.5f + 0.5f) * edgeAttenuation;
                barHeights[i] = Math.max(0.1f, Math.min(1f, barHeights[i]));
            }
            invalidate();
        });

        animator.start();
    }

    public void stopAnimation() {
        if (animator != null && animator.isRunning()) {
            animator.cancel();
            animator = null;
        }
    }

    public void setPausedState() {
        stopAnimation();
        for (int i = 0; i < barCount; i++) {
            barHeights[i] = 0.1f;
        }
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopAnimation();
    }
}
