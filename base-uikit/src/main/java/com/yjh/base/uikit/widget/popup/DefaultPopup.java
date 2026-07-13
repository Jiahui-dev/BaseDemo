package com.yjh.base.uikit.widget.popup;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.view.Gravity;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;
import com.yjh.base.uikit.R;
import java.util.List;

public class DefaultPopup extends BaseCustomPopup<String> {

    public DefaultPopup(Context context, List<String> items) {
        super(context, items);
        // 样式设置
        container.setBackgroundResource(R.drawable.uikit_bg_popup_floating);
        container.setElevation(dip2px(2));
        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
    }

    @Override
    protected void renderItems() {
        for (int i = 0; i < menuItems.size(); i++) {
            String text = menuItems.get(i);
            final int position = i;  // 关键：将 i 赋值给 final 变量
            TextView tv = new TextView(context);
            tv.setText(text);
            tv.setTextColor(Color.BLACK);   // 默认黑色，你可以改成自定义颜色

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
            if (i < menuItems.size() - 1) {
                lp.bottomMargin = dip2px(8);
            }
            tv.setLayoutParams(lp);

            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
            tv.setPaddingRelative(dip2px(12), dip2px(12), dip2px(12), dip2px(12));

            tv.setOnClickListener(v -> {
                if (listener != null) listener.onItemClick(text, position);  // 传入 position
                dismiss();
            });

            container.addView(tv);
        }
    }
}