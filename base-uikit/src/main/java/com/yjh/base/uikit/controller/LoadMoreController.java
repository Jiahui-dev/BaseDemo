package com.yjh.base.uikit.controller;

import android.view.View;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;
import androidx.viewbinding.ViewBinding;
import com.yjh.base.core.lifecycle.Lifecycle;
import com.yjh.base.core.lifecycle.LifecycleEvent;
import com.yjh.base.uikit.adapter.SimpleAdapter;
import com.yjh.base.uikit.databinding.UikitViewLoadMoreBinding;

/**
 * Created by jiahui on 2026/01/28
 */
public class LoadMoreController implements Lifecycle {

    private RecyclerView mRecyclerView;
    private SimpleAdapter<?, ? extends ViewBinding> mAdapter;
    private OnLoadMoreListener mListener;
    // 直接持有加载更多布局的强类型 Binding 实例，废弃原生 View 和 TextView 的声明
    private UikitViewLoadMoreBinding mFooterBinding;
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
                mFooterBinding = null; // 释放内存
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

    public LoadMoreController(RecyclerView recyclerView, SimpleAdapter<?, ? extends ViewBinding> adapter) {
        this.mRecyclerView = recyclerView;
        this.mAdapter = adapter;
        initFooter();
    }

    private void initFooter() {
        if (mAdapter == null || mRecyclerView == null) return;

        // 直接调用我们为 SimpleAdapter 设计的 setFooterView，
        mAdapter.setFooterView(UikitViewLoadMoreBinding::inflate, mRecyclerView);

        // 从适配器拿到强类型的加载布局 Binding 实例
        mFooterBinding = mAdapter.getFooterBinding();

        if (mFooterBinding != null) {
            // 初始化为隐藏状态
            mFooterBinding.pbLoading.setVisibility(View.GONE);
            mFooterBinding.tvLoading.setVisibility(View.GONE);
        }
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

        if (mFooterBinding != null) {
            mFooterBinding.pbLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
            mFooterBinding.tvLoading.setText("正在加载...");
            mFooterBinding.getRoot().setOnClickListener(null);
        }

        if (mListener != null) {
            mListener.onLoadMore();
        }
    }

    public void loadMoreSuccess(boolean hasMoreData) {
        this.isLoading = false;
        this.hasMore = hasMoreData;

        if (mFooterBinding == null) return;

        // 1. 无论是否还有更多，立刻隐藏转圈的 ProgressBar
        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.getRoot().setOnClickListener(null);

        if (hasMoreData) {
            // 还有更多数据，直接隐藏文字提示，等待下次滑动
            mFooterBinding.tvLoading.setVisibility(View.GONE);
        } else {
            // 【核心修复】：最后一页到底了
            // 不要立刻改成“已经到底啦”！先让它保持现状（或者暂时 GONE）
            // 延迟 150ms 变字，确保 15 条新数据已经完全被渲染并把 Footer 挤到了屏幕下方
            if (mRecyclerView != null) {
                mRecyclerView.postDelayed(() -> {
                    // 双重校验环境安全
                    if (mFooterBinding == null || mRecyclerView == null) return;

                    RecyclerView.LayoutManager lm = mRecyclerView.getLayoutManager();
                    int totalItem = lm != null ? lm.getItemCount() : 0;

                    // 健壮性防错：如果总条数很少（不够一屏），直接隐藏
                    if (totalItem <= mPreloadThreshold + 2) {
                        mFooterBinding.tvLoading.setVisibility(View.GONE);
                    } else {
                        // 此时新数据已上屏，Footer 已经被挤到屏幕外面了
                        // 在这里静默把文字改成“已经到底啦”，用户在滑到最底部时才能看到，视觉上完美无缝
                        mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
                        mFooterBinding.tvLoading.setText("已经到底啦");
                    }
                }, 150); // 150 毫秒足够绝大多数手机完成一帧的 RecyclerView 绘制
            }
        }
    }

    public void loadMoreFail() {
        this.isLoading = false;

        if (mFooterBinding == null) return;

        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
        mFooterBinding.tvLoading.setText("加载失败, 点击重试");

        // 绑定点击重试事件到根布局上
        mFooterBinding.getRoot().setOnClickListener(v -> startLoadMore());
    }

    /**
     * 更新控制器状态
     */
    public void updateLoadingState(boolean hasMoreData, String endFooterText) {
        this.isLoading = false;
        this.hasMore = hasMoreData;

        if (mFooterBinding == null) return;

        mFooterBinding.pbLoading.setVisibility(View.GONE);
        mFooterBinding.getRoot().setOnClickListener(null);

        if (hasMoreData) {
            mFooterBinding.tvLoading.setVisibility(View.GONE);
        } else {
            RecyclerView.LayoutManager lm = mRecyclerView != null ? mRecyclerView.getLayoutManager() : null;
            if (lm != null && lm.getItemCount() <= 5) { // 不足满屏数据时不展示到底提示
                mFooterBinding.tvLoading.setVisibility(View.GONE);
            } else {
                mFooterBinding.tvLoading.setVisibility(View.VISIBLE);
                mFooterBinding.tvLoading.setText(endFooterText != null ? endFooterText : "已经到底啦");
            }
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
