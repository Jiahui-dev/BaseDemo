package com.yjh.base.uikit.widget.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
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
        container.setBackgroundResource(R.drawable.uikit_bg_popup_white); // 你的默认背景

        int p = dip2px(8);
        container.setPadding(p, p, p, p);

        // 由子类实现具体布局填充
        renderItems();

        setContentView(container);
        setWidth(dip2px(150));
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
        setFocusable(true);
        setOutsideTouchable(true);
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            setElevation(10f);
        }
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
}