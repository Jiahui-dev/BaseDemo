package com.yjh.basedemo.adapter;

import android.content.Context;
import com.yjh.base.uikit.adapter.BaseRecyclerAdapter;
import com.yjh.base.uikit.adapter.holder.BaseViewHolder;
import com.yjh.basedemo.R;
import com.yjh.basedemo.model.bean.CollectionBean;

public class MyCollectionAdapter extends BaseRecyclerAdapter<CollectionBean> {

    public MyCollectionAdapter(Context context) {
        super(context);
    }

    @Override
    protected int getItemLayoutId(int viewType) {
        return R.layout.item_collection;
    }

    @Override
    protected void bindData(BaseViewHolder holder, CollectionBean data, int position) {
        holder.setText(R.id.tv_collection_name,data.getName());
    }
}
