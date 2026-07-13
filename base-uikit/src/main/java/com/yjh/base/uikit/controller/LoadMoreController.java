package com.yjh.base.uikit.controller;

import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.leben.base.Lifecycle;
import com.leben.base.R;
import com.leben.base.ui.adapter.BaseRecyclerAdapter;

public class LoadMoreController implements Lifecycle {

    private RecyclerView mRecyclerView;
    private BaseRecyclerAdapter<?> mAdapter;
    private OnLoadMoreListener mListener;
    private View mFooterView;
    private View mPbLoading;
    private TextView mTvLoading;
    private boolean isLoading=false;//是否正在加载
    private boolean hasMore=true;//是否还有更多数据

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
                .inflate(R.layout.view_load_more,mRecyclerView,false);
        mTvLoading=mFooterView.findViewById(R.id.tv_loading);
        mPbLoading=mFooterView.findViewById(R.id.pb_loading);
    }

    @Override
    public void onInit() {

    }

    @Override
    public void initView() {

    }

    @Override
    public void initListener() {
        mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                if(dy<=0||isLoading||!hasMore){
                    return;
                }
                RecyclerView.LayoutManager layoutManager= recyclerView.getLayoutManager();
                if(layoutManager instanceof LinearLayoutManager){
                    LinearLayoutManager lm=(LinearLayoutManager) layoutManager;
                    int totalItemCount=lm.getItemCount();
                    int lastVisibleItem=lm.findLastVisibleItemPosition();

                    //如果看到了最后一条，触发加载
                    if(lastVisibleItem>=totalItemCount-1){
                        mRecyclerView.post(() -> startLoadMore());
                    }
                }
            }
        });
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


    @Override
    public void initData() {

    }

    @Override
    public void onStart() {

    }

    @Override
    public void onStop() {

    }

    @Override
    public void onResume() {

    }

    @Override
    public void onPause() {

    }

    @Override
    public void onDestroy() {

    }

    @Override
    public void onRetry() {

    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {

    }
}
