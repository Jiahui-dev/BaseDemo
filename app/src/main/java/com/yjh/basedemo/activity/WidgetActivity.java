package com.yjh.basedemo.activity;

import android.view.LayoutInflater;

import com.yjh.base.uikit.activity.BaseActivity;
import com.yjh.base.uikit.widget.dialog.bottom.PagedGridDialog;
import com.yjh.basedemo.databinding.AcWidgetBinding;
import com.yjh.basedemo.model.dict.ProductIconDict;

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
    }
}
