package com.yjh.base.uikit.controller;

import android.view.View;

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
 * 独立的分页控制器
 * Created by jiahui on 2026/08/11
 */
public class PagingController implements Lifecycle {

    /**
     * 分页配置与加载回调接口
     */
    public interface OnPagingListener {

        int getPageSize();

        /**
         * 滑动到底部触发加载更多
         *
         * @param page     下一次要请求的页码
         * @param pageSize 每页数量
         */
        void onLoadMore(int page, int pageSize);

        default int getStartPage() {
            return 1;
        }
    }

    private final BaseRecyclerActivity<?, ?> mActivity;
    private final OnPagingListener mListener;

    private final int mStartPage;
    private final int mPageSize;
    private int mCurrentPage;

    private RecyclerView mRecyclerView;
    private SimpleAdapter<?, ?> mAdapter;
    private UikitViewLoadMoreBinding mFooterBinding;
    private RecyclerView.OnScrollListener mScrollListener;

    private boolean mIsLoading = false;
    private boolean mHasMore = true;
    private int mPreloadThreshold = 1;

    /**
     * 构造方法：强约束使用环境与配置
     */
    public PagingController(@NonNull BaseRecyclerActivity<?, ?> activity, @NonNull OnPagingListener listener) {
        this.mActivity = activity;
        this.mListener = listener;

        this.mPageSize = listener.getPageSize();
        if (this.mPageSize <= 0) {
            throw new IllegalArgumentException("【" + activity.getClass().getSimpleName() + "】getPageSize() 的返回值必须大于 0！");
        }

        this.mStartPage = listener.getStartPage();
        this.mCurrentPage = mStartPage;
    }

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_VIEW_CREATED:
                // View 创建完成后绑定，避免 NPE
                bindRecyclerViewAndAdapter();
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

    private void bindRecyclerViewAndAdapter() {
        this.mRecyclerView = mActivity.getRecyclerView();
        this.mAdapter = mActivity.getAdapter();

        if (mAdapter != null && mRecyclerView != null) {
            mAdapter.setFooterView(UikitViewLoadMoreBinding::inflate, mRecyclerView);
            mFooterBinding = mAdapter.getFooterBinding();
            if (mFooterBinding != null) {
                mFooterBinding.pbLoading.setVisibility(View.GONE);
                mFooterBinding.tvLoading.setVisibility(View.GONE);
            }
        }
    }

    public <T> void handleRefreshSuccess(List<T> list) {
        this.mCurrentPage = mStartPage;
        this.mIsLoading = false;
        if (mAdapter != null) {
            mAdapter.setList(list);
        }
        checkHasMoreAndUpdateUi(list);
    }

    /**
     * 加载更多成功时调用：自动页码 +1、追加数据到 Adapter、更新 Footer 状态
     */
    public <T> void handleLoadMoreSuccess(List<T> list) {
        this.mIsLoading = false;
        this.mCurrentPage++;
        if (mAdapter != null) {
            mAdapter.addList(list);
        }
        checkHasMoreAndUpdateUi(list);
    }

    /**
     * 加载更多失败时调用
     */
    public void handleLoadMoreFailed() {
        this.mIsLoading = false;
        if (mFooterBinding == null) return;
        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
        mFooterBinding.tvLoading.setText("加载失败, 点击重试");
        mFooterBinding.getRoot().setOnClickListener(v -> startLoadMore());
    }

    private <T> void checkHasMoreAndUpdateUi(List<T> data) {
        this.mHasMore = (data != null && data.size() >= mPageSize);
        updateFooterUi();
    }

    private void startLoadMore() {
        mIsLoading = true;
        if (mFooterBinding != null) {
            mFooterBinding.pbLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setText("正在加载...");
            mFooterBinding.getRoot().setOnClickListener(null);
        }
        if (mListener != null) {
            // 将下一次要请求的页码抛给上层
            mListener.onLoadMore(mCurrentPage + 1, mPageSize);
        }
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

    private void updateFooterUi() {
        if (mFooterBinding == null) return;
        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.getRoot().setOnClickListener(null);

        if (mHasMore) {
            mFooterBinding.tvLoading.setVisibility(View.GONE);
        } else {
            if (mRecyclerView != null) {
                mRecyclerView.postDelayed(() -> {
                    if (mFooterBinding == null || mRecyclerView == null) return;
                    RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
                    int totalItem = lm != null ? lm.getItemCount() : 0;
                    if (totalItem <= mPreloadThreshold + 2) {
                        mFooterBinding.tvLoading.setVisibility(View.GONE);
                    } else {
                        mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
                        mFooterBinding.tvLoading.setText("已经到底啦");
                    }
                }, 150);
            }
        }
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

    public int getCurrentPage() {
        return mCurrentPage;
    }

    public int getPageSize() {
        return mPageSize;
    }

}