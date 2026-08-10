package com.yjh.base.uikit.widget.flow;

import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.view.ViewGroup;

import java.util.ArrayList;
import java.util.List;

/**
 * 流式布局控件
 * Created by youjiahui on 2026/08/10
 */
public class FlowLayout extends ViewGroup {

    private int mHorizontalSpacing = 24; // 标签左右间距 (px)
    private int mVerticalSpacing = 24;   // 标签上下间距 (px)

    private final List<List<View>> mAllViews = new ArrayList<>();
    private final List<Integer> mLineHeight = new ArrayList<>();

    public FlowLayout(Context context) {
        this(context, null);
    }

    public FlowLayout(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public FlowLayout(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
    }

    /**
     * 设置标签间距
     */
    public void setSpacing(int horizontalSpacing, int verticalSpacing) {
        this.mHorizontalSpacing = horizontalSpacing;
        this.mVerticalSpacing = verticalSpacing;
        requestLayout();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        mAllViews.clear();
        mLineHeight.clear();

        int sizeWidth = MeasureSpec.getSize(widthMeasureSpec);
        int modeWidth = MeasureSpec.getMode(widthMeasureSpec);
        int sizeHeight = MeasureSpec.getSize(heightMeasureSpec);
        int modeHeight = MeasureSpec.getMode(heightMeasureSpec);

        int width = 0;
        int height = 0;
        int lineWidth = 0;
        int lineHeight = 0;

        List<View> lineViews = new ArrayList<>();
        int count = getChildCount();

        for (int i = 0; i < count; i++) {
            View child = getChildAt(i);
            if (child.getVisibility() == GONE) continue;

            measureChild(child, widthMeasureSpec, heightMeasureSpec);
            MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

            int childWidth = child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin;
            int childHeight = child.getMeasuredHeight() + lp.topMargin + lp.bottomMargin;

            // 换行判断
            if (lineWidth + childWidth + (lineViews.isEmpty() ? 0 : mHorizontalSpacing) > sizeWidth - getPaddingLeft() - getPaddingRight()) {
                width = Math.max(width, lineWidth);
                height += lineHeight + mVerticalSpacing;

                mAllViews.add(lineViews);
                mLineHeight.add(lineHeight);

                lineViews = new ArrayList<>();
                lineWidth = 0;
                lineHeight = childHeight;
            }

            lineWidth += childWidth + (lineViews.isEmpty() ? 0 : mHorizontalSpacing);
            lineHeight = Math.max(lineHeight, childHeight);
            lineViews.add(child);
        }

        mAllViews.add(lineViews);
        mLineHeight.add(lineHeight);

        width = Math.max(width, lineWidth);
        height += lineHeight;

        setMeasuredDimension(
                modeWidth == MeasureSpec.EXACTLY ? sizeWidth : width + getPaddingLeft() + getPaddingRight(),
                modeHeight == MeasureSpec.EXACTLY ? sizeHeight : height + getPaddingTop() + getPaddingBottom()
        );
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        int left = getPaddingLeft();
        int top = getPaddingTop();

        int lineNum = mAllViews.size();
        for (int i = 0; i < lineNum; i++) {
            List<View> lineViews = mAllViews.get(i);
            int lineHeight = mLineHeight.get(i);

            for (int j = 0; j < lineViews.size(); j++) {
                View child = lineViews.get(j);
                if (child.getVisibility() == GONE) continue;

                MarginLayoutParams lp = (MarginLayoutParams) child.getLayoutParams();

                int lc = left + lp.leftMargin;
                int tc = top + lp.topMargin;
                int rc = lc + child.getMeasuredWidth();
                int bc = tc + child.getMeasuredHeight();

                child.layout(lc, tc, rc, bc);

                left += child.getMeasuredWidth() + lp.leftMargin + lp.rightMargin + mHorizontalSpacing;
            }
            left = getPaddingLeft();
            top += lineHeight + mVerticalSpacing;
        }
    }

    @Override
    public LayoutParams generateLayoutParams(AttributeSet attrs) {
        return new MarginLayoutParams(getContext(), attrs);
    }

    @Override
    protected LayoutParams generateDefaultLayoutParams() {
        return new MarginLayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected LayoutParams generateLayoutParams(LayoutParams p) {
        return new MarginLayoutParams(p);
    }
}
