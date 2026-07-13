package com.yjh.base.uikit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.yjh.base.uikit.adapter.holder.BaseViewHolder;
import java.util.ArrayList;
import java.util.List;

/**
 * 通用 RecyclerView 适配器基类
 * Created by jiahui on 2026/01/28
 */

public abstract class BaseRecyclerAdapter<T> extends RecyclerView.Adapter<BaseViewHolder> {

    protected Context mContext;
    protected List<T> mList;
    protected OnItemClickListener<T> mOnItemClickListener;
    protected OnItemLongClickListener<T> mOnItemLongClickListener;
    public static final int TYPE_CONTENT = 0;
    public static final int TYPE_FOOTER = 1;
    private View mFooterView;//底部布局

    // 用于防抖处理
    private long lastClickTime = 0;

    public BaseRecyclerAdapter(Context context) {
        this.mContext = context;
        this.mList = new ArrayList<>();
    }

    /**
     * 添加底部布局
     */
    public void addFooterView(View footerView) {
        //去重
        if (mFooterView == footerView) {
            return;
        }
        mFooterView = footerView;
        notifyItemInserted(getItemCount() - 1);
    }

    /**
     * 移除底部布局
     */
    public void removeFooterView() {
        if (mFooterView != null) {
            mFooterView = null;
            notifyItemRemoved(getItemCount() - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mFooterView != null && position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_CONTENT;
    }

    @NonNull
    @Override
    public BaseViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_FOOTER) {
            //如果是 Footer，直接返回持有 FooterView 的 Holder
            return new BaseViewHolder(mFooterView);
        }
        // 这里的 getLayoutId(viewType) 由子类实现
        View view = LayoutInflater.from(mContext).inflate(getItemLayoutId(viewType), parent, false);
        return new BaseViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder holder, int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            // Footer 不需要绑定数据
            return;
        }

        final T item = getItem(position);

        // 1. 整行点击：传递当前 itemView 的 ID
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null)
                mOnItemClickListener.onItemClick(v, v.getId(), position, item);
        });

        // 2. 整行长按
        holder.itemView.setOnLongClickListener(v -> {
            if (mOnItemLongClickListener != null) {
                mOnItemLongClickListener.onItemLongClick(v, position, item);
                return true;
            }
            return false;
        });
        bindData(holder, item, position);
    }

    @Override
    public int getItemCount() {
        // 如果有 Footer，总数 + 1
        return mList.size() + (mFooterView == null ? 0 : 1);
    }

    /**
     * 获取底部视图数量
     * LoadMoreController 会调用这个方法来判断当前有没有 Footer
     *
     * @return 如果有 Footer 返回 1，否则返回 0
     */
    public int getFooterLayoutCount() {
        return mFooterView == null ? 0 : 1;
    }

    public T getItem(int position) {
        return mList.get(position);
    }

    public void setList(List<T> list) {
        this.mList = list == null ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }

    public void addList(List<T> list) {
        if (list != null && !list.isEmpty()) {
            int startPos = mList.size();
            this.mList.addAll(list);
            notifyItemRangeInserted(startPos, list.size());
        }
    }

    public List<T> getList() {
        return mList;
    }

    /**
     * 子类提供布局 ID
     */
    protected abstract int getItemLayoutId(int viewType);

    /**
     * 子类绑定数据
     */
    protected abstract void bindData(BaseViewHolder holder, T data, int position);

    public interface OnItemClickListener<T> {
        void onItemClick(View view, int viewId, int position, T data);
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mOnItemClickListener = listener;
    }

    /**
     * 在集合末尾添加单个数据
     * 常用于聊天室实时收到新消息
     */
    public void addData(T data) {
        if (data != null) {
            int position = mList.size(); // 获取插入前的位置
            mList.add(data);
            // 通知适配器在内容列表的末尾插入了数据
            // 注意：如果你有 Footer，新数据是插在 Footer 之前的
            notifyItemInserted(position);
        }
    }

    /**
     * 清除所有数据
     */
    public void clear() {
        mList.clear();
        notifyDataSetChanged();
    }

    public interface OnItemLongClickListener<T> {
        void onItemLongClick(View view, int position, T entity);
    }

    public void setOnItemLongClickListener(OnItemLongClickListener<T> listener) {
        this.mOnItemLongClickListener = listener;
    }
}
