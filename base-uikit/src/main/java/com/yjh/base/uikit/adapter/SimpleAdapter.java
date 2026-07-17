package com.yjh.base.uikit.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.uikit.adapter.holder.BaseViewHolder;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by youjiahui on 2026/07/17.
 */
public class SimpleAdapter<T, VB extends ViewBinding> extends RecyclerView.Adapter<BaseViewHolder<ViewBinding>> {

    protected Context mContext;
    protected List<T> mList;

    public static final int TYPE_CONTENT = 0;
    public static final int TYPE_FOOTER = 1;

    private ViewBinding mFooterBinding;

    // 条目布局创建器与数据绑定器
    private final Creator<VB> mCreator;
    private final Binder<T, VB> mBinder;

    // 条目点击事件监听
    private OnItemClickListener<T> mOnItemClickListener;

    public SimpleAdapter(Context context, Creator<VB> creator, Binder<T, VB> binder) {
        this.mContext = context;
        this.mCreator = creator;
        this.mBinder = binder;
        this.mList = new ArrayList<>();
    }

    @Override
    public int getItemViewType(int position) {
        if (mFooterBinding != null && position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_CONTENT;
    }

    @NonNull
    @Override
    public BaseViewHolder<ViewBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mContext);

        // 直接返回已经构造好的 mFooterBinding 容器
        if (viewType == TYPE_FOOTER) {
            return new BaseViewHolder<>(mFooterBinding);
        }

        VB contentBinding = mCreator.create(inflater, parent, false);
        return new BaseViewHolder<>(contentBinding);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ViewBinding> holder, int position) {
        if (getItemViewType(position) == TYPE_FOOTER) {
            return; // Footer 布局通常为静态的加载状态提示，不需要绑定动态列表数据
        }

        final T item = mList.get(position);

        // 设置全行点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, v.getId(), position, item);
            }
        });

        // 【平铺赋值核心】强转为内容区域的具体 ViewBinding 并回调出去
        if (mBinder != null) {
            mBinder.bind((VB) holder.binding, item, position);
        }
    }

    @Override
    public int getItemCount() {
        return mList.size() + (mFooterBinding == null ? 0 : 1);
    }

    public int getFooterLayoutCount() {
        return mFooterBinding == null ? 0 : 1;
    }

    /**
     * 设置底部的 Footer 布局（例如：加载更多视图、没有更多数据视图）
     * @param footerCreator 传入底部布局的 ViewBinding::inflate 静态引用
     * @param parent 传入 RecyclerView 实例作为布局父容器
     */
    public <FB extends ViewBinding> void setFooterView(@NonNull Creator<FB> footerCreator, @NonNull ViewGroup parent) {
        FB footerBinding = footerCreator.create(LayoutInflater.from(mContext), parent, false);
        if (mFooterBinding == footerBinding) return;
        mFooterBinding = footerBinding;
        notifyItemInserted(getItemCount() - 1);
    }

    /**
     * 获取当前 Footer 布局的具体 ViewBinding 实例，方便在外部直接修改文本或状态图标
     */
    @SuppressWarnings("unchecked")
    public <FB extends ViewBinding> FB getFooterBinding() {
        return (FB) mFooterBinding;
    }

    /**
     * 移除底部的 Footer 布局
     */
    public void removeFooterView() {
        if (mFooterBinding != null) {
            mFooterBinding = null;
            notifyItemRemoved(getItemCount() - 1);
        }
    }

    /**
     * 刷新并重置整个列表数据
     */
    public void setList(List<T> list) {
        this.mList = list == null ? new ArrayList<>() : list;
        notifyDataSetChanged();
    }

    /**
     * 往列表末尾追加分页数据
     */
    public void addList(List<T> list) {
        if (list != null && !list.isEmpty()) {
            int startPos = mList.size();
            this.mList.addAll(list);
            notifyItemRangeInserted(startPos, list.size());
        }
    }

    /**
     * 获取当前列表的全部数据集
     */
    public List<T> getList() {
        return mList;
    }

    /**
     * 函数接口：完美承接 ViewBinding 自动生成的静态 inflate 方法
     */
    public interface Creator<VB extends ViewBinding> {
        VB create(LayoutInflater inflater, ViewGroup parent, boolean attachToParent);
    }

    /**
     * 函数接口：对外暴露具体的强类型数据绑定逻辑
     */
    public interface Binder<T, VB extends ViewBinding> {
        void bind(VB binding, T data, int position);
    }

    /**
     * 列表条目点击事件接口
     */
    public interface OnItemClickListener<T> {
        void onItemClick(View view, int viewId, int position, T data);
    }

    public void setOnItemClickListener(OnItemClickListener<T> listener) {
        this.mOnItemClickListener = listener;
    }
}