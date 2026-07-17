package com.yjh.base.uikit.adapter.holder;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

/**
 * 专为 ViewBinding 设计的通用 ViewHolder
 * Created by jiahui on 2026/07/17
 */
public class BaseViewHolder<VB extends ViewBinding> extends RecyclerView.ViewHolder {

    // 强类型的 ViewBinding 实例，直接向外暴露给 Adapter 使用
    public final VB binding;

    public BaseViewHolder(@NonNull VB binding) {
        super(binding.getRoot());
        this.binding=binding;
    }
}
