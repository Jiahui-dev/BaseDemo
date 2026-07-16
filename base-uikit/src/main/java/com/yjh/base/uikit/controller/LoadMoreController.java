package com.yjh.base.uikit.controller;

import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.uikit.R;
import com.yjh.base.uikit.adapter.BaseRecyclerAdapter;

/**
 * Created by jiahui on 2026/01/28
 */
public class LoadMoreController implements Lifecycle {

    private RecyclerView mRecyclerView;
    private BaseRecyclerAdapter<?> mAdapter;
    private OnLoadMoreListener mListener;
    private View mFooterView;
    private View mPbLoading;
    private TextView mTvLoading;
    private boolean isLoading=false;//是否正在加载
    private boolean hasMore=true;//是否还有更多数据

    // 预加载阈值：倒数第几个条目可见时触发加载
    private int mPreloadThreshold = 1;

    private RecyclerView.OnScrollListener mScrollListener;

    @Override
    public void onLifecycleChanged(LifecycleEvent event) {
        switch (event) {
            case ON_VIEW_CREATED:
                // 视图创建完毕，自动在内部完成监听器绑定
                initListener();
                break;
            case ON_DESTROY:
                // 页面销毁，自动解绑并释放内存，防止泄露
                if (mRecyclerView != null && mScrollListener != null) {
                    mRecyclerView.removeOnScrollListener(mScrollListener);
                }
                mRecyclerView = null;
                mAdapter = null;
                mListener = null;
                mFooterView = null;
                mPbLoading = null;
                mTvLoading = null;
                mScrollListener = null;
                break;
            default:
                break;
        }
    }

    public interface OnLoadMoreListener{
        void onLoadMore();
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.mListener = listener;
    }

    public LoadMoreController(RecyclerView recyclerView, BaseRecyclerAdapter<?> adapter){
        this.mRecyclerView=recyclerView;
        this.mAdapter=adapter;
        initFooter();
    }

    private void initFooter(){
        mFooterView= LayoutInflater.from(mRecyclerView.getContext())
                .inflate(R.layout.uikit_view_load_more,mRecyclerView,false);
        mTvLoading=mFooterView.findViewById(R.id.tv_loading);
        mPbLoading=mFooterView.findViewById(R.id.pb_loading);

        mPbLoading.setVisibility(View.GONE);
        if (mTvLoading != null) {
            mTvLoading.setVisibility(View.GONE);
        }
        mAdapter.addFooterView(mFooterView);
    }

    private void initListener() {
        // 防止重复绑定监听器
        if (mRecyclerView == null || mScrollListener != null) return;

        mScrollListener = new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                // 只有向下滚动、当前未在加载、且还有更多数据时才进行判定
                if (dy <= 0 || isLoading || !hasMore) {
                    return;
                }

                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                if (layoutManager == null) return;

                int totalItemCount = layoutManager.getItemCount();
                int lastVisibleItemPosition = findLastVisibleItemPosition(layoutManager);

                // 判断是否到达预加载阈值
                if (lastVisibleItemPosition >= totalItemCount - mPreloadThreshold) {
                    mRecyclerView.post(() -> {
                        // 双重检查，防止异步 post 触发时页面已被销毁或正在加载
                        if (mRecyclerView != null && !isLoading && hasMore) {
                            startLoadMore();
                        }
                    });
                }
            }
        };
        mRecyclerView.addOnScrollListener(mScrollListener);
    }

    /**
     * 获取最后一个可见 Item 的位置
     */
    private int findLastVisibleItemPosition(RecyclerView.LayoutManager layoutManager) {
        if (layoutManager instanceof LinearLayoutManager) {
            return ((LinearLayoutManager) layoutManager).findLastVisibleItemPosition();
        } else if (layoutManager instanceof StaggeredGridLayoutManager) {
            StaggeredGridLayoutManager staggeredGridLayoutManager = (StaggeredGridLayoutManager) layoutManager;
            int[] lastVisibleItemPositions = staggeredGridLayoutManager.findLastVisibleItemPositions(null);
            return findMax(lastVisibleItemPositions);
        }
        return -1;
    }

    private int findMax(int[] lastVisibleItemPositions) {
        if (lastVisibleItemPositions == null || lastVisibleItemPositions.length == 0) {
            return -1;
        }
        int max = lastVisibleItemPositions[0];
        for (int value : lastVisibleItemPositions) {
            if (value > max) {
                max = value;
            }
        }
        return max;
    }

    private void startLoadMore() {
        isLoading = true;
        mPbLoading.setVisibility(View.VISIBLE);
        mTvLoading.setVisibility(View.VISIBLE);
        mTvLoading.setText("正在加载...");

        mFooterView.setOnClickListener(null);

        // 回调给 Activity
        if (mListener != null) {
            mListener.onLoadMore();
        }
    }

    public void loadMoreSuccess(boolean hasMoreData) {
        this.isLoading = false;
        this.hasMore = hasMoreData;

        if (hasMoreData) {
            // 还有更多：隐藏加载提示，等待下一次滚动触发
            mPbLoading.setVisibility(View.GONE);
            mTvLoading.setVisibility(View.GONE);
            mFooterView.setOnClickListener(null);
        } else {
            // 已经到底了：显示到底提示
            mPbLoading.setVisibility(View.GONE);
            mTvLoading.setVisibility(View.VISIBLE);
            mTvLoading.setText("已经到底啦");
            mFooterView.setOnClickListener(null);
        }
    }

    public void loadMoreFail() {
        this.isLoading = false;

        if (mPbLoading != null) {
            mPbLoading.setVisibility(View.GONE);
        }
        if (mTvLoading != null) {
            mTvLoading.setVisibility(View.VISIBLE);
            mTvLoading.setText("加载失败, 点击重试");
        }
        if (mFooterView != null) {
            mFooterView.setOnClickListener(v -> {
                startLoadMore();
            });
        }
    }

    /**
     * 重置控制器状态
     */
    public void reset(boolean hasMoreData, String endFooterText) {
        this.isLoading = false;
        this.hasMore = hasMoreData;

        if (hasMoreData) {
            // 如果刷新后还有数据，重置底部状态为不可见状态
            if (mPbLoading != null) mPbLoading.setVisibility(View.GONE);
            if (mTvLoading != null) mTvLoading.setVisibility(View.GONE);
            mFooterView.setOnClickListener(null);
        } else {
            // 如果刷新完直接没数据了，展示到底提示
            if (mPbLoading != null) mPbLoading.setVisibility(View.GONE);
            if (mTvLoading != null) {
                mTvLoading.setVisibility(View.VISIBLE);
                mTvLoading.setText(endFooterText != null ? endFooterText : "已经到底啦");
            }
            mFooterView.setOnClickListener(null);
        }
    }
    /**
     * 触发一次加载更多，供业务层重试使用
     */
    public void retryLoadMore() {
        if (!isLoading) {
            startLoadMore();
        }
    }
}
