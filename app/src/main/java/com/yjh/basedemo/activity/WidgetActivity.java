package com.yjh.basedemo.activity;

import android.view.Gravity;
import android.view.LayoutInflater;

import com.yjh.base.uikit.activity.BaseActivity;
import com.yjh.base.uikit.widget.dialog.bottom.PagedGridDialog;
import com.yjh.base.uikit.widget.popup.BaseCustomPopup;
import com.yjh.base.uikit.widget.popup.DefaultPopup;
import com.yjh.basedemo.databinding.AcWidgetBinding;
import com.yjh.basedemo.model.dict.ProductIconDict;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class WidgetActivity extends BaseActivity<AcWidgetBinding> {

    @Override
    protected AcWidgetBinding initBinding(LayoutInflater inflater) {
        return AcWidgetBinding.inflate(inflater);
    }

    @Override
    protected void initView() {
        super.initView();
    }

    @Override
    protected void initListener() {
        setClick(v->{
            List<ProductIconDict> menus = Arrays.asList(ProductIconDict.values());

            PagedGridDialog.newInstance(
                    3, 4, menus,
                    (binding, data, position) -> {
                        binding.tvItemName.setText(data.getTitle());
                        binding.ivItemIcon.setImageResource(data.getIconRes());
                    },
                    (data, globalPosition) -> {

                    }

            ).showTitle(true).show(getSupportFragmentManager(), "dialog_select_product_icon");
        },binding.tvPagedGridDialog);
        setClick(v->{
            List<String> items = new ArrayList<>();
            items.add("编辑");
            items.add("删除");
            items.add("分享");
            DefaultPopup popup = new DefaultPopup(this, items);
            popup.setOnItemClickListener(new BaseCustomPopup.OnItemClickListener<String>() {
                @Override
                public void onItemClick(String text, int position) {
                    // position 是点击项的位置（从0开始）
                    switch (position) {
                        case 0:
                        case 1:
                        case 2:
                            binding.tvDefaultPopup.setText(text);
                            break;
                    }
                }
            });
            popup.showAsDropDown(binding.tvDefaultPopup, Gravity.BOTTOM,0,0);  // 在锚点下方显示
        },binding.tvDefaultPopup);
    }
}
