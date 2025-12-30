package cn.pylin.xycjd.ui.view;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Matrix;
import android.graphics.Shader;
import android.graphics.drawable.Drawable;
import android.util.AttributeSet;
import androidx.appcompat.widget.AppCompatImageView;

public class CircleImageView extends AppCompatImageView {
    private final Paint paint;
    private final Paint backgroundPaint;
    private final Matrix matrix;
    private Bitmap lastBitmap;
    private BitmapShader lastShader;

    public CircleImageView(Context context) {
        super(context);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(android.graphics.Color.WHITE);
        matrix = new Matrix();
    }

    public CircleImageView(Context context, AttributeSet attrs) {
        super(context, attrs);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(android.graphics.Color.WHITE);
        matrix = new Matrix();
    }

    public CircleImageView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        backgroundPaint.setColor(android.graphics.Color.WHITE);
        matrix = new Matrix();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        Drawable drawable = getDrawable();
        if (drawable == null) {
            return;
        }

        final int width = getWidth();
        final int height = getHeight();
        final float radius = Math.min(width, height) / 2f;
        final float centerX = width / 2f;
        final float centerY = height / 2f;
        
        canvas.drawCircle(centerX, centerY, radius, backgroundPaint);
        
        // 创建或复用bitmap
        final int drawableWidth = drawable.getIntrinsicWidth();
        final int drawableHeight = drawable.getIntrinsicHeight();

        if (drawableWidth <= 0 || drawableHeight <= 0) {
            return;
        }

        Bitmap bitmap = lastBitmap;
        
        if (bitmap == null || bitmap.isRecycled() || bitmap.getWidth() != drawableWidth || bitmap.getHeight() != drawableHeight) {
            if (bitmap != null && !bitmap.isRecycled()) {
                bitmap.recycle();
            }
            bitmap = Bitmap.createBitmap(drawableWidth, drawableHeight, Bitmap.Config.ARGB_8888);
            lastBitmap = bitmap;
            lastShader = null;
        }
        
        // 将drawable绘制到bitmap上
        Canvas bitmapCanvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, drawableWidth, drawableHeight);
        drawable.draw(bitmapCanvas);
        
        // 创建或复用shader
        BitmapShader shader = lastShader;
        if (shader == null) {
            shader = new BitmapShader(bitmap, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
            lastShader = shader;
        }
        
        paint.setShader(shader);
        
        // 计算并应用变换矩阵
        final float scale = Math.min(width / (float) bitmap.getWidth(), 
                                   height / (float) bitmap.getHeight());
        
        matrix.reset();
        matrix.setScale(scale, scale);
        matrix.postTranslate(centerX - bitmap.getWidth() * scale / 2f, 
                           centerY - bitmap.getHeight() * scale / 2f);
        
        shader.setLocalMatrix(matrix);
        canvas.drawCircle(centerX, centerY, radius, paint);
    }
}