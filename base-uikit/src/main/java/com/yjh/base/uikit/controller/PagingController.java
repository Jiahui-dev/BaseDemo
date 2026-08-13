package com.yjh.base.uikit.controller;

import android.view.View;

import androidx.annotation.ColorInt;
import androidx.annotation.ColorRes;
import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.uikit.activity.BaseRecyclerActivity;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.databinding.UikitViewLoadMoreBinding;

import java.util.List;

/**
 * 独立的分页与加载更多控制器
 * Created by jiahui on 2026/08/11
 */
public class PagingController implements Lifecycle {

    private static final String TAG = "PagingController";
    private final BaseRecyclerActivity<?, ?> mActivity;
    private final OnPagingListener mListener;

    private RecyclerView mRecyclerView;
    private SimpleAdapter<?, ?> mAdapter;
    private UikitViewLoadMoreBinding mFooterBinding;
    private RecyclerView.OnScrollListener mScrollListener;

    private boolean mIsLoading = false;
    private boolean mHasMore = true;
    private int mPreloadThreshold = 1;

    public PagingController(@NonNull BaseRecyclerActivity<?, ?> activity, @NonNull OnPagingListener listener) {
        this.mActivity = activity;
        this.mListener = listener;
    }

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_VIEW_CREATED:
                this.mRecyclerView = mActivity.getRecyclerView();
                this.mAdapter = mActivity.getAdapter();

                if (mAdapter != null && mRecyclerView != null) {
                    mAdapter.setFooterView(UikitViewLoadMoreBinding::inflate, mRecyclerView);
                    mFooterBinding = mAdapter.getFooterBinding();
                    hideFooter();
                }
                initListener();
                break;
            case ON_DESTROY:
                if (mRecyclerView != null && mScrollListener != null) {
                    mRecyclerView.removeOnScrollListener(mScrollListener);
                }
                mRecyclerView = null;
                mAdapter = null;
                mFooterBinding = null;
                mScrollListener = null;
                break;
            default:
                break;
        }
    }

    /**
     * 下拉刷新/首次加载成功：刷新列表，更新 Footer
     */
    public <T> void refreshSuccess(List<T> list, boolean hasMore) {
        this.mIsLoading = false;
        this.mHasMore = hasMore;
        if (mAdapter != null) {
            mAdapter.setList(list);
        }
        updateFooter(list);
    }

    /**
     * 加载更多成功：自动页码 +1、追加数据到 Adapter、更新 Footer 状态
     */
    public <T> void loadMoreSuccess(List<T> list, boolean hasMore) {
        this.mIsLoading = false;
        this.mHasMore = hasMore;
        if (mAdapter != null) {
            mAdapter.addList(list);
        }
        updateFooter(mAdapter != null ? mAdapter.getList() : list);
    }

    /**
     * 加载更多失败
     */
    public void loadMoreFailed() {
        this.mIsLoading = false;
        if (mFooterBinding == null) return;
        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
        mFooterBinding.tvLoading.setText("加载失败, 点击重试");
        mFooterBinding.getRoot().setOnClickListener(v -> startLoadMore());
    }

    private void updateFooter(List<?> list) {
        if (mFooterBinding == null) return;

        if (list == null || list.isEmpty()) {
            mFooterBinding.getRoot().setVisibility(View.GONE);
            return;
        }

        mFooterBinding.getRoot().setVisibility(View.VISIBLE);
        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.getRoot().setOnClickListener(null);

        if (mHasMore) {
            mFooterBinding.tvLoading.setVisibility(View.GONE);
        } else {
            mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setText(mListener.getEndFooterText());
        }
    }

    private void startLoadMore() {
        mIsLoading = true;
        if (mFooterBinding != null) {
            mFooterBinding.getRoot().setVisibility(View.VISIBLE);
            mFooterBinding.pbLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setText("正在加载...");
            mFooterBinding.getRoot().setOnClickListener(null);
        }
        // 通知业务层去加载更多
        mListener.onLoadMore();
    }

    private void initListener() {
        if (mRecyclerView == null || mScrollListener != null) return;
        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if (dy <= 0 || mIsLoading || !mHasMore) return;

                RecyclerView.LayoutManager lm = recyclerView.getLayoutManager();
                if (lm == null) return;

                int totalItemCount = lm.getItemCount();
                int lastVisible = findLastVisibleItemPosition(lm);

                if (lastVisible >= totalItemCount - mPreloadThreshold) {
                    mRecyclerView.post(() -> {
                        if (mRecyclerView != null && !mIsLoading && mHasMore) {
                            startLoadMore();
                        }
                    });
                }
            }
        };
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    private int findLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggered = (StaggeredGridLayoutManager) layoutManager;
            int[] lastPositions = staggered.findLastVisibleItemPositions(null);
            return findMax(lastPositions);
        }
        return -1;
    }

    private int findMax(int[] positions) {
        if (positions == null || positions.length == 0) return -1;
        int max = positions[0];
        for (int value : positions) {
            if (value > max) max = value;
        }
        return max;
    }

    // 设置 Footer 背景色
    public void setFooterBackgroundColorRes(@ColorRes int colorRes) {
        if (mFooterBinding != null) {
            mFooterBinding.getRoot().setBackgroundResource(colorRes);
        }
    }

    public void setFooterBackgroundColor(@ColorInt int color) {
        if (mFooterBinding != null) {
            mFooterBinding.getRoot().setBackgroundColor(color);
        }
    }

    public void setFooterBackgroundResource(@DrawableRes int resId) {
        if (mFooterBinding != null) {
            mFooterBinding.getRoot().setBackgroundResource(resId);
        }
    }

    /**
     * 隐藏并重置 Footer（在下拉刷新/重新搜索/发起请求时调用）
     */
    public void hideFooter() {
        this.mIsLoading = false;
        if (mFooterBinding != null) {
            mFooterBinding.pbLoading.setVisibility(View.GONE);
            mFooterBinding.tvLoading.setVisibility(View.GONE);
            mFooterBinding.getRoot().setVisibility(View.GONE);
        }
    }

    /**
     * 分页配置与加载回调接口
     */
    public interface OnPagingListener {

        /**
         * 滑动到底部触发加载更多
         */
        void onLoadMore();

        default String getEndFooterText() {
            return "已经到底啦";
        }
    }
}