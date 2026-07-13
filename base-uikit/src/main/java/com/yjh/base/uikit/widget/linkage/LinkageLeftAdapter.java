package com.yjh.base.uikit.widget.linkage;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public abstract class LinkageLeftAdapter<T extends ILinkageCategory, VH extends RecyclerView.ViewHolder> extends RecyclerView.Adapter<VH> {

    protected List<T> mData;
    private int mSelectedPosition = 0; // 当前选中的索引
    private OnItemClickListener mListener;

    public LinkageLeftAdapter(List<T> data) {
        this.mData = data;
    }

    // 设置选中位置并刷新
    public void setSelectedIndex(int position) {
        if (mSelectedPosition != position) {
            int oldPos = mSelectedPosition;
            mSelectedPosition = position;
            notifyItemChanged(oldPos);
            notifyItemChanged(mSelectedPosition);
        }
    }

    public int getSelectedIndex() {
        return mSelectedPosition;
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.mListener = listener;
    }

    @Override
    public void onBindViewHolder(@NonNull VH holder, int position) {
        boolean isSelected = (position == mSelectedPosition);
        // 回调给子类去设置颜色、背景
        onBind(holder, mData.get(position), isSelected);

        holder.itemView.setOnClickListener(v -> {
            if (mListener != null) {
                mListener.onItemClick(position);
            }
        });
    }

    @Override
    public int getItemCount() {
        return mData == null ? 0 : mData.size();
    }

    // 子类实现具体的数据绑定逻辑
    protected abstract void onBind(VH holder, T item, boolean isSelected);

    public interface OnItemClickListener {
        void onItemClick(int position);
    }

    public int getCheckedPosition() { return mSelectedPosition; }

    public T getItem(int position) {
        if (position >= 0 && position < mData.size()) {
            return mData.get(position);
        }
        return null;
    }
}