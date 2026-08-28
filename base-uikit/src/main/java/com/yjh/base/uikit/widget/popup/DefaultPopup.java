package com.yjh.base.uikit.widget.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.yjh.base.uikit.R;

import java.util.List;

public class DefaultPopup extends BaseCustomPopup<String> {

    private final int arrowHeightDp = 8; // 三角形高度

    public DefaultPopup(Context context, List<String> items) {
        super(context, items);

        // 构造自定义的气泡背景
        BubbleDrawable bubbleBackground = new BubbleDrawable(
                ContextCompat.getColor(context, R.color.uikit_white),      // 填充底色
                ContextCompat.getColor(context, R.color.uikit_grey_200),   // 边框线颜色
                dip2px(1),                                             // 边框粗细 1dp
                dip2px(8),                                             // 矩形圆角 8dp
                dip2px(12),                                            // 三角形宽度 12dp
                dip2px(arrowHeightDp)                                      // 三角形高度 8dp
        );

        container.setBackground(bubbleBackground);

        setElevation(0);
        container.setElevation(0);

        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void renderItems() {
        // 容器顶部 Padding 需加上三角形的高度 (arrowHeightDp) + 原来的边距，避免文字挡住三角形
        int topPadding = dip2px(8 + arrowHeightDp);
        int normalPadding = dip2px(8);
        container.setPadding(0, topPadding, 0, normalPadding);

        for (int i = 0; i < menuItems.size(); i++) {
            String text = menuItems.get(i);
            final int position = i;
            TextView tv = new TextView(context);
            tv.setText(text);
            tv.setTextColor(ContextCompat.getColor(context, R.color.uikit_text_primary));

            //波纹效果
            TypedValue outValue = new TypedValue();
            context.getTheme().resolveAttribute(android.R.attr.selectableItemBackground, outValue, true);
            tv.setBackgroundResource(outValue.resourceId);

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < menuItems.size() - 1) {
                lp.bottomMargin = dip2px(2);
            }
            tv.setLayoutParams(lp);

            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setPaddingRelative(dip2px(14), dip2px(6), dip2px(14), dip2px(6));

            tv.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(text, position);
                dismiss();
            });

            container.addView(tv);
        }
    }
}