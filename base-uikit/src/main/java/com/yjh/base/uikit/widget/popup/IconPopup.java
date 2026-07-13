package com.yjh.base.uikit.widget.popup;//package com.leben.base.widget.popup;
//
//import android.content.Context;
//import android.graphics.Color;
//import android.graphics.drawable.ColorDrawable;
//import android.view.Gravity;
//import android.view.ViewGroup;
//import android.widget.LinearLayout;
//import android.widget.TextView;
//
//import com.leben.base.R;
//import com.leben.base.model.bean.PopupEntity;
//import java.util.List;
//
//public class IconPopup extends BaseCustomPopup {
//    public IconPopup(Context context, List<PopupEntity> items) {
//        super(context, items);
//        // 1. 设置背景资源（带圆角）
//        container.setBackgroundResource(R.drawable.bg_popup_floating);
//
//        // 2. 设置阴影高度（API 21+）
//        // 注意：必须先设置背景，阴影才会生效
//        container.setElevation(dip2px(2));
//
//        // 3. 关键：解决阴影显示不全的问题
//        // PopupWindow 默认的 WindowContentOverlay 可能会挡住阴影
//        setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
//
//        setWidth(ViewGroup.LayoutParams.WRAP_CONTENT);
//        setHeight(ViewGroup.LayoutParams.WRAP_CONTENT);
//    }
//
//    @Override
//    protected void renderItems() {
//        for (int i = 0; i < menuItems.size(); i++) {
//            PopupEntity item = menuItems.get(i);
//            TextView tv = new TextView(context);
//            tv.setText(item.text);
//            tv.setTextColor(item.textColor);
//            // 1. 统一布局参数：确保宽度 MATCH_PARENT 才能让点击整行有效，高度 WRAP_CONTENT
//            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
//                    ViewGroup.LayoutParams.MATCH_PARENT,
//                    ViewGroup.LayoutParams.WRAP_CONTENT);
//
//            // 2. 统一项间距：与 DefaultPopup 保持一致的 8dp
//            if (i < menuItems.size() - 1) {
//                lp.bottomMargin = dip2px(8);
//            }
//            tv.setLayoutParams(lp);
//
//            // 3. 统一对齐方式与内边距：使用 PaddingRelative 保证 Start 对齐
//            tv.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
//            // 这里建议 Padding 与 DefaultPopup 统一，比如 15dp 或者 12dp
//            tv.setPaddingRelative(dip2px(15), dip2px(12), dip2px(15), dip2px(12));
//
//            // 4. 设置图标逻辑（保持不变，但确保图标不破坏对齐）
//            if (item.iconRes != 0) {
//                tv.setCompoundDrawablesWithIntrinsicBounds(item.iconRes, 0, 0, 0);
//                tv.setCompoundDrawablePadding(dip2px(10));
//            }
//
//            tv.setOnClickListener(v -> {
//                if (listener != null) listener.onItemClick(item);
//                dismiss();
//            });
//            container.addView(tv);
//        }
//    }
//}
