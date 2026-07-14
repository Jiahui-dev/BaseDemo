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

    private void startLoadMore(){
        isLoading=true;
        mPbLoading.setVisibility(View.VISIBLE);
        mTvLoading.setVisibility(View.VISIBLE);
        mTvLoading.setText("正在加载...");

        //移除点击事件 (防止正在加载时用户狂点)
        mFooterView.setOnClickListener(null);

        mAdapter.addFooterView(mFooterView);
        //回调给 Activity
        if(mListener!=null){
            mListener.onLoadMore();
        }
    }

    public void loadMoreSuccess(boolean hasMoreData){
        this.isLoading=false;
        this.hasMore=hasMoreData;
        mAdapter.removeFooterView();
        if(!hasMoreData){
            mPbLoading.setVisibility(View.GONE);
            mTvLoading.setText("已经到底啦");
            mAdapter.addFooterView(mFooterView);
        }
    }

    public void loadMoreFail() {
        this.isLoading = false;

        if(mPbLoading!=null){
            mPbLoading.setVisibility(View.GONE);
        }
        if(mTvLoading!=null){
            mTvLoading.setVisibility(View.VISIBLE);
            mTvLoading.setText("加载失败,点击重试");
        }
        if(mFooterView!=null){
            mFooterView.setOnClickListener(v->{
                startLoadMore();
            });
        }
    }

    /**
     * 重置控制器状态
     */
    public void reset(boolean hasMoreData,String endFooterText) {
        this.isLoading = false;
        this.hasMore = hasMoreData;
        mAdapter.removeFooterView();

        // 如果刷新完直接就判定没有更多数据了（例如单页列表，或者不支持加载更多的页面）
        if (!hasMoreData) {
            if (mPbLoading != null) mPbLoading.setVisibility(View.GONE);
            if (mTvLoading != null) {
                mTvLoading.setVisibility(View.VISIBLE);
                mTvLoading.setText(endFooterText!=null? endFooterText :"已经到底啦");
            }
            //没有更多数据时，清除掉点击事件
            mFooterView.setOnClickListener(null);
            mAdapter.addFooterView(mFooterView);
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
