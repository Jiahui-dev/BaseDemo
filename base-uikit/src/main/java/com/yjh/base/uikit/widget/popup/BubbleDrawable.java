package com.yjh.base.uikit.widget.popup;

import android.graphics.Canvas;
import android.graphics.ColorFilter;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.drawable.Drawable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class BubbleDrawable extends Drawable {

    private final Paint fillPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint strokePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();

    private final float cornerRadius; // 圆角大小
    private final float arrowWidth;   // 箭头宽度
    private final float arrowHeight;  // 箭头高度

    public BubbleDrawable(int fillColor, int strokeColor, float strokeWidth,
                          float cornerRadius, float arrowWidth, float arrowHeight) {
        this.cornerRadius = cornerRadius;
        this.arrowWidth = arrowWidth;
        this.arrowHeight = arrowHeight;

        fillPaint.setStyle(Paint.Style.FILL);
        fillPaint.setColor(fillColor);

        strokePaint.setStyle(Paint.Style.STROKE);
        strokePaint.setColor(strokeColor);
        strokePaint.setStrokeWidth(strokeWidth);
    }

    @Override
    protected void onBoundsChange(Rect bounds) {
        super.onBoundsChange(bounds);
        buildPath(bounds);
    }

    private void buildPath(Rect bounds) {
        path.reset();

        // 避开描边画笔宽度的一半，防止边框被裁切
        float halfStroke = strokePaint.getStrokeWidth() / 2f;
        float left = bounds.left + halfStroke;
        float top = bounds.top + halfStroke + arrowHeight; // 顶部留出箭头的高度
        float right = bounds.right - halfStroke;
        float bottom = bounds.bottom - halfStroke;

        RectF rect = new RectF(left, top, right, bottom);

        // 1. 从左上角圆角终点开始画（顺时针）
        path.moveTo(rect.left + cornerRadius, rect.top);

        // ================= 关键修改：计算水平居中的箭头位置 =================
        // rect.width() 是气泡主体宽度，其中心点加上 rect.left，再减去箭头一半宽度
        float arrowLeft = rect.left + (rect.width() - arrowWidth) / 2f;

        // 确保箭头不会超过圆角区域
        if (arrowLeft > rect.left + cornerRadius && (arrowLeft + arrowWidth) < rect.right - cornerRadius) {
            path.lineTo(arrowLeft, rect.top);
            path.lineTo(arrowLeft + arrowWidth / 2f, bounds.top + halfStroke); // 顶点
            path.lineTo(arrowLeft + arrowWidth, rect.top);
        }
        // ===================================================================

        // 3. 右上角
        path.lineTo(rect.right - cornerRadius, rect.top);
        path.arcTo(new RectF(rect.right - 2 * cornerRadius, rect.top, rect.right, rect.top + 2 * cornerRadius), -90, 90);

        // 4. 右下角
        path.lineTo(rect.right, rect.bottom - cornerRadius);
        path.arcTo(new RectF(rect.right - 2 * cornerRadius, rect.bottom - 2 * cornerRadius, rect.right, rect.bottom), 0, 90);

        // 5. 左下角
        path.lineTo(rect.left + cornerRadius, rect.bottom);
        path.arcTo(new RectF(rect.left, rect.bottom - 2 * cornerRadius, rect.left + 2 * cornerRadius, rect.bottom), 90, 90);

        // 6. 左上角
        path.lineTo(rect.left, rect.top + cornerRadius);
        path.arcTo(new RectF(rect.left, rect.top, rect.left + 2 * cornerRadius, rect.top + 2 * cornerRadius), 180, 90);

        path.close();
    }

    @Override
    public void draw(@NonNull Canvas canvas) {
        // 先画背景填充
        canvas.drawPath(path, fillPaint);
        // 再画边框描边
        if (strokePaint.getStrokeWidth() > 0) {
            canvas.drawPath(path, strokePaint);
        }
    }

    @Override
    public void setAlpha(int alpha) {
        fillPaint.setAlpha(alpha);
        strokePaint.setAlpha(alpha);
    }

    @Override
    public void setColorFilter(@Nullable ColorFilter colorFilter) {
        fillPaint.setColorFilter(colorFilter);
        strokePaint.setColorFilter(colorFilter);
    }

    @Override
    public int getOpacity() {
        return PixelFormat.TRANSLUCENT;
    }
}