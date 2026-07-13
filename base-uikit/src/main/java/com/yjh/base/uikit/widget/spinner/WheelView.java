package com.yjh.base.uikit.widget.spinner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import android.widget.Scroller;
import java.util.ArrayList;
import java.util.List;

public class WheelView extends View {
    private List<String> data = new ArrayList<>();
    private Paint paint;
    private int scrollY = 0;
    private Scroller scroller;
    private VelocityTracker velocityTracker;
    private OnItemSelectedListener listener;
    private int itemHeight; // 不要在这里写死 120

    public interface OnItemSelectedListener {
        void onSelected(int index);
    }

    public WheelView(Context context, AttributeSet attrs) {
        super(context, attrs);
        // 将 50dp 转换为当前设备的像素值，这样在所有手机上看起来高度都差不多
        float density = context.getResources().getDisplayMetrics().density;
        itemHeight = (int) (50 * density); // 50dp 是比较标准的滚轮高度

        paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        paint.setTextAlign(Paint.Align.CENTER);
        paint.setColor(Color.BLACK);
        scroller = new Scroller(context);
    }

    public void setData(List<String> data) {
        this.data = data;
        invalidate();
    }

    public void setOnItemSelectedListener(OnItemSelectedListener listener) {
        this.listener = listener;
    }

    public int getSelectedIndex() {
        if (data == null || data.isEmpty()) return 0;
        int index = Math.round((float) scrollY / itemHeight);
        return Math.max(0, Math.min(index, data.size() - 1));
    }

    public String getSelectedItem() {
        if (data == null || data.isEmpty()) return "";
        return data.get(getSelectedIndex());
    }

    @Override
    protected void onDraw(Canvas canvas) {
        if (data == null || data.isEmpty()) return;

        int centerY = getHeight() / 2;

        // --- 1. 绘制中间的选中框横线 ---
        // 创建一个新的画笔或者重用已有画笔并修改属性
        paint.setAlpha(255); // 确保横线不透明
        paint.setColor(Color.parseColor("#EEEEEE")); // 设置横线的颜色，建议用浅灰色
        paint.setStrokeWidth(2f); // 设置横线的粗细

        // 计算上下横线的 Y 坐标
        float lineYTop = centerY - itemHeight / 2f;
        float lineYBottom = centerY + itemHeight / 2f;

        // 绘制上横线
        canvas.drawLine(0, lineYTop, getWidth(), lineYTop, paint);
        // 绘制下横线
        canvas.drawLine(0, lineYBottom, getWidth(), lineYBottom, paint);

        // 恢复画笔颜色，以免影响后面文字的绘制
        paint.setColor(Color.BLACK);

        for (int i = 0; i < data.size(); i++) {
            int itemCenterY = centerY + (i * itemHeight - scrollY);
            int distance = Math.abs(itemCenterY - centerY);

            if (distance > getHeight() / 2 + itemHeight) continue;

            // 缩放逻辑：距离中心越远越小
            float scale = 1.0f - (distance / (float) (itemHeight * 3.0f));
            scale = Math.max(0.6f, scale);

            // 将 0.4f 降到 0.3f，如果还觉得大，就改到 0.25f
            float baseTextSize = itemHeight * 0.45f;
            paint.setTextSize(baseTextSize * scale);

            // 透明度处理
            float alphaRatio = 1.0f - (distance / (float) (itemHeight * 2.0f));
            int alpha = (int) (255 * Math.pow(Math.max(0, alphaRatio), 2));
            paint.setAlpha(Math.max(40, alpha));

            // 绘制文字
            canvas.drawText(data.get(i), getWidth() / 2f, itemCenterY + paint.getTextSize() / 3, paint);
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (velocityTracker == null) velocityTracker = VelocityTracker.obtain();
        velocityTracker.addMovement(event);
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (!scroller.isFinished()) scroller.abortAnimation();
                break;
            case MotionEvent.ACTION_MOVE:
                scrollY -= (int) event.getHistorySize() > 0 ? (event.getY() - event.getHistoricalY(0)) : 0;
                invalidate();
                break;
            case MotionEvent.ACTION_UP:
                velocityTracker.computeCurrentVelocity(1000);
                float vY = velocityTracker.getYVelocity();
                scroller.fling(0, scrollY, 0, (int) -vY, 0, 0, 0, Math.max(0, (data.size() - 1) * itemHeight));
                justifyPosition();
                break;
        }
        return true;
    }

    private void justifyPosition() {
        int targetY = Math.round((float) scrollY / itemHeight) * itemHeight;
        targetY = Math.max(0, Math.min(targetY, Math.max(0, (data.size() - 1) * itemHeight)));
        scroller.startScroll(0, scrollY, 0, targetY - scrollY);
        invalidate();
        if (listener != null) listener.onSelected(getSelectedIndex());
    }

    @Override
    public void computeScroll() {
        if (scroller.computeScrollOffset()) {
            scrollY = scroller.getCurrY();
            invalidate();
        }
    }

    public void setSelected(int index) {
        if (data == null || data.isEmpty()) return;
        int safeIndex = Math.max(0, Math.min(index, data.size() - 1));
        scrollY = safeIndex * itemHeight; // 立即修正数值，防止空白
        post(() -> {
            invalidate();
        });
    }
}