package com.yjh.base.uikit.widget.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import com.yjh.base.uikit.R;
import java.util.List;

public abstract class BaseCustomPopup<T> extends PopupWindow {

    protected Context context;
    protected LinearLayout container;
    protected List<T> menuItems;          // 泛型数据列表

    public BaseCustomPopup(Context context, List<T> items) {
        super(context);
        this.context = context;
        this.menuItems = items;
        init();
    }

    private void init() {
        container = new LinearLayout(context);
        container.setOrientation(LinearLayout.VERTICAL);
        container.setBackgroundResource(R.drawable.uikit_shape_radius_12); // 你的默认背景

        container.setPadding(0, 0, 0, 0);

        // 由子类实现具体布局填充
        renderItems();

        setContentView(container);
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setElevation(10f);
    }

    // 子类必须实现此方法，负责向 container 中添加视图
    protected abstract void renderItems();

    protected int dip2px(float dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density + 0.5f);
    }

    // 泛型回调接口
    public interface OnItemClickListener<T> {
        void onItemClick(T item, int position);  // 增加 position
    }

    protected OnItemClickListener<T> listener;

    public BaseCustomPopup<T> setOnItemClickListener(OnItemClickListener<T> l) {
        this.listener = l;
        return this;
    }

    /**
     * 在锚点控件（Anchor）下方水平居中显示
     * @param anchor 目标控件
     */
    public void showAtAnchorCenter(View anchor) {
        showAtAnchorCenter(anchor, 0);
    }

    /**
     * 在锚点控件（Anchor）下方水平居中显示，并支持设置 Y 轴偏移量
     * @param anchor 目标控件
     * @param yoff Y 轴偏移量（单位：px）
     */
    public void showAtAnchorCenter(View anchor, int yoff) {
        if (anchor == null) return;

        // 1. 强制测量 ContentView，确保获取到精准的 Popup 宽度
        getContentView().measure(
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        );
        int popupWidth = getContentView().getMeasuredWidth();

        // 2. 获取锚点控件的宽度
        int anchorWidth = anchor.getWidth();

        // 3. 计算水平居中的 X 轴偏移量
        int xoff = (anchorWidth - popupWidth) / 2;

        // 4. 调用原生方法展示
        showAsDropDown(anchor, xoff, yoff);
    }

}