package com.yjh.base.uikit.adapter;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewbinding.ViewBinding;

import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.holder.BaseViewHolder;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by youjiahui on 2026/07/17.
 */
public class SimpleAdapter<T, VB extends ViewBinding> extends RecyclerView.Adapter<BaseViewHolder<ViewBinding>> {

    private static final String TAG = "SimpleAdapter";

    public static final int TYPE_CONTENT = 0;
    public static final int TYPE_FOOTER = 1;
    public static final int TYPE_EMPTY = 2;
    public static final int TYPE_ERROR = 3;

    protected Context mContext;
    protected List<T> mList;

    private ViewBinding mFooterBinding;
    private View mEmptyView;
    private View mErrorView;

    // 当前缺省页状态：0-正常显示列表, 1-显示 Empty, 2-显示 Error
    private int mPageState = 0;

    // 条目布局创建器与数据绑定器
    private final Creator<VB> mCreator;
    private final Binder<T, VB> mBinder;

    private String mCustomErrorMsg;

    // 条目点击事件监听
    private OnItemClickListener<T> mOnItemClickListener;
    // 重试回调
    private OnRetryListener mOnRetryListener;

    public interface OnRetryListener {
        void onRetry();
    }

    public void setOnRetryListener(OnRetryListener listener) {
        this.mOnRetryListener = listener;
    }

    public SimpleAdapter(Context context, Creator<VB> creator, Binder<T, VB> binder) {
        this.mContext = context;
        this.mCreator = creator;
        this.mBinder = binder;
        this.mList = new ArrayList<>();
    }

    // 设置空状态 View
    public void setEmptyView(View emptyView) {
        this.mEmptyView = emptyView;
        Log.d(TAG, "setEmptyView: emptyView 是否为空 -> " + (emptyView == null));
    }

    // 设置错误状态 View
    public void setErrorView(View errorView) {
        this.mErrorView = errorView;
    }

    // 切换为显示空页面
    public void showEmpty() {
        this.mPageState = 1;
        Log.d(TAG, "showEmpty() 被调用，当前 mPageState 已置为 1, mEmptyView 是否存在 -> " + (mEmptyView != null));
        notifyDataSetChanged();
    }

    public void showError() {
        showError(null);
    }

    public void showError(String errorMsg) {
        this.mPageState = 2;
        this.mCustomErrorMsg = errorMsg;
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        if (mList.isEmpty()) {
            if (mPageState == 1 && mEmptyView != null) {
                return TYPE_EMPTY;
            }
            if (mPageState == 2 && mErrorView != null) {
                return TYPE_ERROR;
            }
        }
        if (mFooterBinding != null && position == getItemCount() - 1) {
            return TYPE_FOOTER;
        }
        return TYPE_CONTENT;
    }

    @NonNull
    @Override
    public BaseViewHolder<ViewBinding> onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(mContext);

        if (viewType == TYPE_EMPTY) {
            setFullMatchLayout(mEmptyView,parent);
            return new BaseViewHolder<>(mEmptyView);
        }

        if (viewType == TYPE_ERROR) {
            setFullMatchLayout(mErrorView,parent);
            mErrorView.setOnClickListener(v -> {
                if (mOnRetryListener != null) {
                    mOnRetryListener.onRetry();
                }
            });
            return new BaseViewHolder<>(mErrorView);
        }

        if (viewType == TYPE_FOOTER) {
            return new BaseViewHolder<>(mFooterBinding);
        }

        VB contentBinding = mCreator.create(inflater, parent, false);
        return new BaseViewHolder<>(contentBinding);
    }

    @SuppressWarnings("unchecked")
    @Override
    public void onBindViewHolder(@NonNull BaseViewHolder<ViewBinding> holder, int position) {

        int viewType = getItemViewType(position);

        if (viewType == TYPE_ERROR) {

            TextView tvErrorMsg = holder.itemView.findViewById(R.id.uikit_tv_error_msg);
            View btnRetry =  holder.itemView.findViewById(R.id.uikit_btn_retry);

            if (tvErrorMsg != null && !TextUtils.isEmpty(mCustomErrorMsg)) {
                tvErrorMsg.setText(mCustomErrorMsg);
            }

            if (btnRetry != null) {
                btnRetry.setOnClickListener(v -> {
                    if (mOnRetryListener != null) {
                        mOnRetryListener.onRetry();
                    }
                });
            }
            return;
        }

        if (viewType == TYPE_EMPTY || viewType == TYPE_FOOTER) {
            return; // 静态 View 不绑定业务逻辑
        }

        final T item = mList.get(position);

        // 设置全行点击事件
        holder.itemView.setOnClickListener(v -> {
            if (mOnItemClickListener != null) {
                mOnItemClickListener.onItemClick(v, v.getId(), position, item);
            }
        });

        // 强转为内容区域的具体 ViewBinding 并回调出去
        if (mBinder != null) {
            mBinder.bind((VB) holder.binding, item, position);
        }
    }

    @Override
    public int getItemCount() {
        if (mList.isEmpty()) {
            if (mPageState == 1 && mEmptyView != null) {
                Log.d(TAG, "getItemCount: mList 为空, mPageState=1, 返回 Item 数量 = 1 (EmptyView)");
                return 1;
            }
            if (mPageState == 2 && mErrorView != null) {
                Log.d(TAG, "getItemCount: mList 为空, mPageState=2, 返回 Item 数量 = 1 (ErrorView)");
                return 1;
            }
            Log.d(TAG, "getItemCount: mList 为空，但未满足空/错误状态条件, mPageState = " + mPageState + ", 返回 Item 数量 = 0");
            return 0;
        }
        int count = mList.size() + (mFooterBinding == null ? 0 : 1);
        Log.d(TAG, "getItemCount: 列表有数据，返回 Item 数量 = " + count);
        return count;
    }

    public int getFooterLayoutCount() {
        return mFooterBinding == null ? 0 : 1;
    }

    /**
     * 设置底部的 Footer 布局（例如：加载更多视图、没有更多数据视图）
     *
     * @param footerCreator 传入底部布局的 ViewBinding::inflate 静态引用
     * @param parent        传入 RecyclerView 实例作为布局父容器
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

    public void resetState() {
        this.mPageState = 0; // 重置状态为普通列表
        notifyDataSetChanged(); // 刷新 UI，清除出现的 Stub View
    }

    /**
     * 刷新并重置整个列表数据
     */
    @SuppressWarnings("unchecked")
    public void setList(List<?> list) {
        this.mList = list == null ? new ArrayList<>() : (List<T>) list;
        if (!this.mList.isEmpty()) {
            this.mPageState = 0;
        }
        notifyDataSetChanged();
    }

    /**
     * 往列表末尾追加分页数据
     */
    @SuppressWarnings("unchecked")
    public void addList(List<?> list) {
        if (list != null && !list.isEmpty()) {
            int startPos = mList.size();
            this.mList.addAll((List<T>) list);
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

    private void setFullMatchLayout(View view, ViewGroup parent) {
        if (view == null) return;

        // 强制把 View 的高度设置为 RecyclerView 的测量高度，防止 wrap_content 导致塌陷
        int height = parent.getMeasuredHeight();
        if (height <= 0) {
            height = ViewGroup.LayoutParams.MATCH_PARENT;
        }

        ViewGroup.LayoutParams params = view.getLayoutParams();
        if (params == null) {
            params = new RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, height);
        } else {
            params.width = ViewGroup.LayoutParams.MATCH_PARENT;
            params.height = height;
        }
        view.setLayoutParams(params);
    }

}