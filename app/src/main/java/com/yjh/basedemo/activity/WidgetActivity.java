package com.yjh.basedemo.activity;

import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;

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
        setClick(v -> {
            List<String> items = Arrays.asList("编辑", "删除", "分享");
            DefaultPopup popup = new DefaultPopup(this, items);

            popup.setOnItemClickListener((text, position) -> {
                binding.tvDefaultPopup.setText(text);
            });
            popup.showAtAnchorCenter(binding.tvDefaultPopup);

        }, binding.tvDefaultPopup);
    }
}
